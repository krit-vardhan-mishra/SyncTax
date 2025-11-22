package com.just_for_fun.synctax.ui.viewmodels

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.Environment
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.core.data.preferences.PlayerPreferences
import com.just_for_fun.synctax.core.data.repository.MusicRepository
import com.just_for_fun.synctax.core.ml.MusicRecommendationManager
import com.just_for_fun.synctax.core.player.ChunkedStreamManager
import com.just_for_fun.synctax.core.player.MusicPlayer
import com.just_for_fun.synctax.core.player.PlaybackCollector
import com.just_for_fun.synctax.core.player.PlaybackEvent
import com.just_for_fun.synctax.core.player.PlaybackEventBus
import com.just_for_fun.synctax.core.player.QueueManager
import com.just_for_fun.synctax.service.MusicService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.URL

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val player = MusicPlayer(application)
    private val playerPreferences = PlayerPreferences(application)
    private val chunkedStreamManager = ChunkedStreamManager(application)
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    private val recommendationManager = MusicRecommendationManager(application)
    private val queueManager = QueueManager(application, repository, recommendationManager)
    
    // PlaybackCollector with cache invalidation callback
    private val playbackCollector = PlaybackCollector(
        repository = repository,
        player = player,
        onPlaybackRecorded = {
            // Invalidate Quick Picks cache when new playback is recorded
            recommendationManager.invalidateQuickPicksCache()
        }
    )
        
    // Track if we're currently handling song end to prevent multiple triggers
    private var isHandlingSongEnd = false
    private var currentLyrics: String? = null

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    // Service binding
    private var musicService: MusicService? = null
    private var isServiceBound = false

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MusicService.MusicBinder
            musicService = binder.getService()
            isServiceBound = true
            // Update notification with current state
            updateNotification()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            musicService = null
            isServiceBound = false
        }
    }

    // Broadcast receiver for service actions
    private val serviceActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                MusicService.ACTION_PLAY -> {
                    if (!_uiState.value.isPlaying) {
                        togglePlayPause()
                    }
                }
                MusicService.ACTION_PAUSE -> {
                    if (_uiState.value.isPlaying) {
                        togglePlayPause()
                    }
                }
                MusicService.ACTION_NEXT -> next()
                MusicService.ACTION_PREVIOUS -> previous()
                MusicService.ACTION_SHUFFLE -> toggleShuffle()
                MusicService.ACTION_STOP -> {
                    // Stop playback completely and clear song state
                    viewModelScope.launch {
                        playbackCollector.stopCollecting(skipped = true)
                    }
                    player.pause()

                    _uiState.value = _uiState.value.copy(
                        currentSong = null,
                        isPlaying = false
                    )

                    // Update notification and stop the foreground service
                    musicService?.updatePlaybackState(null, false, 0L, 0L, null)

                    // Stop the service hosting playback
                    try {
                        getApplication<Application>().stopService(Intent(getApplication(), MusicService::class.java))
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
                MusicService.ACTION_SEEK_TO -> {
                    val position = intent.getLongExtra("position", 0L)
                    seekTo(position)
                }
            }
        }
    }

    init {
        // Bind to music service
        val intent = Intent(application, MusicService::class.java)
        application.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        application.startService(intent)

        // Register broadcast receiver - use EXPORTED to allow service to send broadcasts
        val filter = IntentFilter().apply {
            addAction(MusicService.ACTION_PLAY)
            addAction(MusicService.ACTION_PAUSE)
            addAction(MusicService.ACTION_NEXT)
            addAction(MusicService.ACTION_PREVIOUS)
            addAction(MusicService.ACTION_SHUFFLE)
            addAction(MusicService.ACTION_STOP)
            addAction(MusicService.ACTION_SEEK_TO)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(serviceActionReceiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            application.registerReceiver(serviceActionReceiver, filter)
        }

        viewModelScope.launch {
            player.playerState.collect { playerState ->
                _uiState.value = _uiState.value.copy(
                    isPlaying = playerState.isPlaying,
                    isBuffering = playerState.isBuffering,
                    duration = playerState.duration
                )

                // Update notification when playback state changes
                updateNotification()

                // Handle song end
                if (playerState.isEnded) {
                    onSongEnded()
                }
            }
        }

        // Collect position updates from player
        viewModelScope.launch {
            player.currentPosition.collect { position ->
                _uiState.value = _uiState.value.copy(
                    position = position
                )

                // Update notification periodically (every second)
                if (position % 1000 < 100) {
                    updateNotification()
                }

                // Save current state periodically
                _uiState.value.currentSong?.let { song ->
                    if (position % 5000 < 100) { // Save every ~5 seconds
                        playerPreferences.saveCurrentSong(
                            songId = song.id,
                            position = position,
                            isPlaying = _uiState.value.isPlaying
                        )
                    }
                }
            }
        }

        // Initialize volume from device and sync UI state
        _uiState.value = _uiState.value.copy(volume = getVolume())

        // Observe queue state changes
        viewModelScope.launch {
            queueManager.queueState.collect { queueState ->
                // Queue state is now managed by QueueManager
                // UI can observe this for updates
            }
        }

        // Restore last playing song
        viewModelScope.launch {
            restoreLastSong()
        }
    }

    private suspend fun restoreLastSong() {
        val lastSongId = playerPreferences.getCurrentSongId() ?: return
        val lastPosition = playerPreferences.getLastPosition()
        val playlistIds = playerPreferences.getCurrentPlaylist()
        val savedIndex = playerPreferences.getCurrentIndex()

        try {
            val song = repository.getSongById(lastSongId)
            if (song != null) {
                // Restore playlist if available
                if (playlistIds.isNotEmpty()) {
                    val playlist = playlistIds.mapNotNull { id -> repository.getSongById(id) }
                    if (playlist.isNotEmpty()) {
                        // Initialize queue manager with restored playlist
                        val actualIndex = savedIndex.coerceIn(0, playlist.size - 1)
                        
                        // Ensure the current song is at the current index
                        if (actualIndex < playlist.size && playlist[actualIndex].id == song.id) {
                            queueManager.initializeQueue(playlist, actualIndex)
                        } else {
                            // Find the song in playlist
                            val songIndex = playlist.indexOf(song)
                            if (songIndex != -1) {
                                queueManager.initializeQueue(playlist, songIndex)
                            } else {
                                // Add it to playlist
                                queueManager.initializeQueue(playlist + song, playlist.size)
                            }
                        }
                    } else {
                        queueManager.initializeQueue(listOf(song), 0)
                    }
                } else {
                    queueManager.initializeQueue(listOf(song), 0)
                }

                player.prepare(song.filePath, song.id)
                player.seekTo(lastPosition)

                _uiState.value = _uiState.value.copy(
                    currentSong = song,
                    position = lastPosition,
                    isPlaying = false // Don't auto-play on restore
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun savePlaylistState() {
        viewModelScope.launch {
            val queueState = queueManager.queueState.value
            playerPreferences.saveCurrentPlaylist(
                songIds = queueState.currentPlaylist.map { it.id },
                currentIndex = queueState.currentIndex
            )
        }
    }

    fun playSong(song: Song, playlist: List<Song> = listOf(song)) {
        viewModelScope.launch {
            // Clean up tmp file from previous stream if safe
            chunkedStreamManager.cleanupTmpFile()
            // Stop any streaming downloads and remove cached chunks of previous song
            chunkedStreamManager.stopAndCleanup(removeFinalCache = true)
            // Stop current song
            if (_uiState.value.currentSong != null) {
                playbackCollector.stopCollecting(skipped = true)
            }

            // Initialize queue with new playlist
            val songIndex = playlist.indexOf(song).coerceAtLeast(0)
            queueManager.initializeQueue(playlist, songIndex)

            player.prepare(song.filePath, song.id)
            player.play()

            playbackCollector.startCollecting(song.id)

            _uiState.value = _uiState.value.copy(
                currentSong = song,
                isPlaying = true
            )

            // Check if song is already downloaded
            checkIfSongDownloaded(song)

            // Emit playback events
            PlaybackEventBus.emit(PlaybackEvent.SongChanged(song))
            PlaybackEventBus.emit(PlaybackEvent.PlaybackStateChanged(true))
            PlaybackEventBus.emit(PlaybackEvent.QueueUpdated(
                upcomingQueue = queueManager.getUpcomingQueue(),
                playHistory = queueManager.getPlayHistory()
            ))

            // Update notification with new song
            updateNotification()

            // Save the new song state
            playerPreferences.saveCurrentSong(
                songId = song.id,
                position = 0L,
                isPlaying = true
            )

            // Save the playlist
            savePlaylistState()
        }
    }

    fun playUrl(url: String, title: String, artist: String? = null, durationMs: Long = 0L) {
        viewModelScope.launch {
            chunkedStreamManager.stopAndCleanup(removeFinalCache = true)
            // Create a temporary Song object for playback UI
            val onlineSong = Song(
                id = "online:${url.hashCode()}",
                title = title,
                artist = artist ?: "Unknown",
                album = null,
                duration = durationMs,
                filePath = url,
                genre = null,
                releaseYear = null,
                albumArtUri = null
            )

            // Initialize queue with online song
            queueManager.initializeQueue(listOf(onlineSong), 0)

            player.prepare(url, onlineSong.id)
            player.play()

            playbackCollector.startCollecting(onlineSong.id)

            _uiState.value = _uiState.value.copy(
                currentSong = onlineSong,
                isPlaying = true
            )
            // Check if song is already downloaded
            checkIfSongDownloaded(onlineSong)
            savePlaylistState()
            updateNotification()
        }
    }

    /** Play a remote stream using chunked progressive download for 30s segments.
     * This writes to a temp cache file and starts playback as soon as the first chunk is available.
     */
    fun playChunkedStream(videoId: String, streamUrl: String, title: String, artist: String? = null, durationMs: Long = 0L, thumbnailUrl: String? = null) {
        viewModelScope.launch {
            // Clean up tmp file from previous stream if it's complete
            chunkedStreamManager.cleanupTmpFile()
            // Stop any existing streaming downloads
            chunkedStreamManager.stopAndCleanup()

            // Upgrade thumbnail URL to 544x544 quality for better album art display
            val highQualityThumbnail = thumbnailUrl?.let { url ->
                when {
                    // Google Photos URLs (YouTube Music thumbnails)
                    url.contains("lh3.googleusercontent.com") -> {
                        url.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
                    }
                    // YouTube thumbnail URLs
                    url.contains("ytimg.com") && url.contains("/default.jpg") -> {
                        url.replace("/default.jpg", "/maxresdefault.jpg")
                    }
                    url.contains("ytimg.com") && url.contains("/mqdefault.jpg") -> {
                        url.replace("/mqdefault.jpg", "/maxresdefault.jpg")
                    }
                    url.contains("ytimg.com") && url.contains("/hqdefault.jpg") -> {
                        url.replace("/hqdefault.jpg", "/maxresdefault.jpg")
                    }
                    url.contains("ytimg.com") && url.contains("/sddefault.jpg") -> {
                        url.replace("/sddefault.jpg", "/maxresdefault.jpg")
                    }
                    // Return original if no pattern matches
                    else -> url
                }
            }

            // Create a Song object where filePath will point to growing tmp file so player can read it
            val tempFile = chunkedStreamManager.startStreaming(videoId, streamUrl, durationMs)
            // Request first 2 chunks to start playback and have a small buffer
            chunkedStreamManager.requestNextChunk(2)

            val onlineSong = Song(
                id = "online:${videoId}",
                title = title,
                artist = artist ?: "Unknown",
                album = null,
                duration = durationMs,
                filePath = tempFile.absolutePath,
                genre = null,
                releaseYear = null,
                albumArtUri = highQualityThumbnail  // Use high-quality thumbnail
            )

            // Stop current collecting and prepare new
            playbackCollector.stopCollecting(skipped = true)

            // Initialize queue with streaming song
            queueManager.initializeQueue(listOf(onlineSong), 0)

            player.prepare(tempFile.absolutePath, onlineSong.id)
            player.play()

            playbackCollector.startCollecting(onlineSong.id)

            _uiState.value = _uiState.value.copy(
                currentSong = onlineSong,
                isPlaying = true,
                isBuffering = true
            )

            // Check if song is already downloaded
            checkIfSongDownloaded(onlineSong)

            // Observe chunk download state and update UI; when completed swap file path to final cached file
            viewModelScope.launch {
                chunkedStreamManager.state.collect { st ->
                    _uiState.value = _uiState.value.copy(
                        isBuffering = !st.isComplete,
                        duration = if (durationMs > 0) durationMs else _uiState.value.duration
                        , downloadPercent = st.percent
                    )
                    if (st.isComplete) {
                        // Download complete - the tmp file now contains full content
                        // No need to switch file paths since tmp file is still being used by player
                        // The .cache file has been copied for future use
                        _uiState.value = _uiState.value.copy(isBuffering = false)
                    }
                }
            }

            // Monitor player position and request more chunks to keep at least 60s buffered
            viewModelScope.launch {
                while (true) {
                    val position = player.getCurrentPosition()
                    val bufferedSec = chunkedStreamManager.state.value.bufferedSeconds
                    val posSec = (position / 1000).toLong()
                    val threshold = 60L
                    if (bufferedSec - posSec < threshold && !chunkedStreamManager.state.value.isComplete) {
                        val prefetch = chunkedStreamManager.suggestedPrefetchCount()
                        chunkedStreamManager.requestNextChunk(prefetch)
                    }
                    // Stop if a different song is now playing or song removed
                    if (_uiState.value.currentSong == null || _uiState.value.currentSong?.id != onlineSong.id) {
                        break
                    }
                    kotlinx.coroutines.delay(1000)
                }
            }

            savePlaylistState()
            updateNotification()
        }
    }

    fun getUpcomingQueue(): List<Song> {
        return queueManager.getUpcomingQueue()
    }

    fun getPlayHistory(): List<Song> {
        return queueManager.getPlayHistory()
    }

    fun togglePlayPause() {
        viewModelScope.launch {
            if (_uiState.value.isPlaying) {
                player.pause()
                musicService?.updatePlaybackState(
                    _uiState.value.currentSong,
                    false,
                    _uiState.value.position,
                    _uiState.value.duration,
                    currentLyrics
                )
                PlaybackEventBus.emit(PlaybackEvent.PlaybackStateChanged(false))
            } else {
                player.play()

                _uiState.value.currentSong?.let { song ->
                    playbackCollector.startCollecting(song.id)
                }
                
                // Update notification immediately
                musicService?.updatePlaybackState(
                    _uiState.value.currentSong,
                    true,
                    _uiState.value.position,
                    _uiState.value.duration,
                    currentLyrics
                )
                PlaybackEventBus.emit(PlaybackEvent.PlaybackStateChanged(true))
            }
        }
    }

    fun seekTo(positionMs: Long) {
        player.seekTo(positionMs)
        _uiState.value = _uiState.value.copy(position = positionMs)
        
        // Update notification with new position
        updateNotification()
    }

    fun next() {
        viewModelScope.launch {
            playbackCollector.stopCollecting(skipped = true)
            
            // Use queue manager to move to next song (with auto-refill)
            val nextSong = queueManager.moveToNext(autoRefill = true)
            
            if (nextSong != null) {
                player.prepare(nextSong.filePath, nextSong.id)
                player.play()
                playbackCollector.startCollecting(nextSong.id)

                _uiState.value = _uiState.value.copy(
                    currentSong = nextSong,
                    isPlaying = true
                )

                updateNotification()
                savePlaylistState()
            } else {
                // No more songs available
                _uiState.value = _uiState.value.copy(isPlaying = false)
            }
        }
    }

    fun previous() {
        viewModelScope.launch {
            playbackCollector.stopCollecting(skipped = true)
            
            // Use queue manager to move to previous song
            val previousSong = queueManager.moveToPrevious()
            
            if (previousSong != null) {
                player.prepare(previousSong.filePath, previousSong.id)
                player.play()
                playbackCollector.startCollecting(previousSong.id)

                _uiState.value = _uiState.value.copy(
                    currentSong = previousSong,
                    isPlaying = true
                )

                updateNotification()
                savePlaylistState()
            }
        }
    }

    private fun onSongEnded() {
        // Prevent multiple simultaneous calls to onSongEnded
        if (isHandlingSongEnd) {
            return
        }
        
        isHandlingSongEnd = true
        
        viewModelScope.launch {
            try {
                playbackCollector.stopCollecting(skipped = false)
                val repeat = _uiState.value.repeatEnabled
                val currentSong = _uiState.value.currentSong

                if (currentSong == null) {
                    _uiState.value = _uiState.value.copy(isPlaying = false)
                    return@launch
                }

                if (repeat) {
                    // Handle repeat differently for online vs offline songs
                    if (currentSong.id.startsWith("online:")) {
                        // For online songs, just seek back to beginning and continue playing
                        seekTo(0L)
                        player.play()
                        playbackCollector.startCollecting(currentSong.id)
                    } else {
                        // For offline songs, restart the song normally
                        player.prepare(currentSong.filePath, currentSong.id)
                        player.play()
                        playbackCollector.startCollecting(currentSong.id)
                    }
                } else {
                    // Auto-play next song with queue refill enabled
                    val nextSong = queueManager.moveToNext(autoRefill = true)
                    
                    if (nextSong != null) {
                        player.prepare(nextSong.filePath, nextSong.id)
                        player.play()
                        playbackCollector.startCollecting(nextSong.id)

                        _uiState.value = _uiState.value.copy(
                            currentSong = nextSong,
                            isPlaying = true
                        )

                        updateNotification()
                        savePlaylistState()
                    } else {
                        _uiState.value = _uiState.value.copy(isPlaying = false)
                    }
                }
            } finally {
                // Reset the flag after a short delay to allow state to stabilize
                kotlinx.coroutines.delay(500)
                isHandlingSongEnd = false
            }
        }
    }

    fun toggleShuffle() {
        // Always reshuffle the playlist with current song at first position
        viewModelScope.launch {
            val currentSong = _uiState.value.currentSong
            val queueState = queueManager.queueState.value
            
            if (currentSong != null && queueState.currentPlaylist.isNotEmpty()) {
                // Use queue manager to shuffle
                queueManager.shuffle()
                
                // Update UI state
                _uiState.value = _uiState.value.copy(shuffleEnabled = true)
                
                savePlaylistState()
                
                PlaybackEventBus.emit(PlaybackEvent.QueueShuffled)
                PlaybackEventBus.emit(PlaybackEvent.QueueUpdated(
                    upcomingQueue = queueManager.getUpcomingQueue(),
                    playHistory = queueManager.getPlayHistory()
                ))
            } else {
                // If no current song, just fetch and shuffle all songs
                val allSongs = repository.getAllSongs().first()
                if (allSongs.isNotEmpty()) {
                    val shuffledPlaylist = allSongs.shuffled()
                    _uiState.value = _uiState.value.copy(shuffleEnabled = true)
                    playSong(shuffledPlaylist[0], shuffledPlaylist)
                }
            }
        }
    }

    fun toggleRepeat() {
        val enabled = !_uiState.value.repeatEnabled
        _uiState.value = _uiState.value.copy(repeatEnabled = enabled)
    }

    /**
     * Shuffle play with random shuffle (for Library screen)
     */
    fun shufflePlay(playlist: List<Song>) {
        viewModelScope.launch {
            if (playlist.isEmpty()) return@launch
            
            // Shuffle the playlist
            val shuffledPlaylist = playlist.shuffled()
            
            // Start playing from the first song in shuffled list
            _uiState.value = _uiState.value.copy(shuffleEnabled = true)
            playSong(shuffledPlaylist[0], shuffledPlaylist)
        }
    }

    /**
     * Smart shuffle play based on recommendations (for Home screen)
     */
    fun shufflePlayWithRecommendations(playlist: List<Song>) {
        viewModelScope.launch {
            if (playlist.isEmpty()) return@launch

            // Use smart shuffle with recommendations
            queueManager.shuffleWithRecommendations(playlist)

            val queueState = queueManager.queueState.value
            val firstSong = queueState.currentPlaylist.firstOrNull()

            if (firstSong != null) {
                _uiState.value = _uiState.value.copy(shuffleEnabled = true)
                playSong(firstSong, queueState.currentPlaylist)
            }
        }
    }

    fun removeFromQueue(song: Song) {
        viewModelScope.launch {
            val queueState = queueManager.queueState.value
            val songIndex = queueState.currentPlaylist.indexOf(song)
            
            if (songIndex != -1) {
                // If removing current song, stop playback
                if (songIndex == queueState.currentIndex) {
                    player.pause()
                    playbackCollector.stopCollecting(skipped = true)
                    _uiState.value = _uiState.value.copy(
                        currentSong = null,
                        isPlaying = false
                    )
                }

                // Remove from queue using queue manager
                queueManager.removeFromQueue(song)
                savePlaylistState()
                
                PlaybackEventBus.emit(PlaybackEvent.SongRemovedFromQueue(song))
                PlaybackEventBus.emit(PlaybackEvent.QueueUpdated(
                    upcomingQueue = queueManager.getUpcomingQueue(),
                    playHistory = queueManager.getPlayHistory()
                ))
            }
        }
    }

    fun placeNext(song: Song) {
        viewModelScope.launch {
            queueManager.placeNext(song)
            savePlaylistState()
            PlaybackEventBus.emit(PlaybackEvent.SongPlacedNext(song))
            PlaybackEventBus.emit(PlaybackEvent.QueueUpdated(
                upcomingQueue = queueManager.getUpcomingQueue(),
                playHistory = queueManager.getPlayHistory()
            ))
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            queueManager.reorderQueue(fromIndex, toIndex)
            savePlaylistState()
            PlaybackEventBus.emit(PlaybackEvent.QueueReordered(fromIndex, toIndex))
            PlaybackEventBus.emit(PlaybackEvent.QueueUpdated(
                upcomingQueue = queueManager.getUpcomingQueue(),
                playHistory = queueManager.getPlayHistory()
            ))
        }
    }

    /**
     * Play a song from the queue
     * This will remove all songs before it and make it the current song
     */
    fun playFromQueue(song: Song) {
        viewModelScope.launch {
            val selectedSong = queueManager.playFromQueue(song)
            
            if (selectedSong != null) {
                playbackCollector.stopCollecting(skipped = true)
                
                player.prepare(selectedSong.filePath, selectedSong.id)
                player.play()
                playbackCollector.startCollecting(selectedSong.id)

                _uiState.value = _uiState.value.copy(
                    currentSong = selectedSong,
                    isPlaying = true
                )

                updateNotification()
                savePlaylistState()
            }
        }
    }

    fun setVolume(volume: Float) {
        // Clamp volume between 0 and 1
        val clampedVolume = volume.coerceIn(0f, 1f)
        
        // Set system volume
        val volumeLevel = (clampedVolume * maxVolume).toInt().coerceIn(0, maxVolume)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeLevel, 0)
        
        // Update UI state with actual system volume
        val actualVolume = getVolume()
        _uiState.value = _uiState.value.copy(volume = actualVolume)
    }

    fun getVolume(): Float {
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        return currentVolume.toFloat() / maxVolume.toFloat()
    }

    private fun checkIfSongDownloaded(song: Song) {
        if (!song.id.startsWith("online:")) return
        
        viewModelScope.launch {
            try {
                val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "SyncTax")
                val safeTitle = song.title.replace(Regex("[^a-zA-Z0-9\\s-]"), "").trim()
                val safeArtist = song.artist?.replace(Regex("[^a-zA-Z0-9\\s-]"), "")?.trim() ?: "Unknown"
                val audioFilename = "$safeTitle - $safeArtist.opus"
                val audioFile = File(musicDir, audioFilename)
                
                if (audioFile.exists() && !_uiState.value.downloadedSongs.contains(song.id)) {
                    _uiState.value = _uiState.value.copy(
                        downloadedSongs = _uiState.value.downloadedSongs + song.id
                    )
                }
            } catch (e: Exception) {
                // Ignore errors when checking for existing downloads
            }
        }
    }

    fun refreshDownloadedSongsCheck(allSongs: List<Song>) {
        viewModelScope.launch {
            val onlineSongs = allSongs.filter { it.id.startsWith("online:") }
            val musicDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "SyncTax")
            
            val downloadedSongIds = mutableSetOf<String>()
            
            onlineSongs.forEach { song ->
                try {
                    val safeTitle = song.title.replace(Regex("[^a-zA-Z0-9\\s-]"), "").trim()
                    val safeArtist = song.artist?.replace(Regex("[^a-zA-Z0-9\\s-]"), "")?.trim() ?: "Unknown"
                    val audioFilename = "$safeTitle - $safeArtist.opus"
                    val audioFile = File(musicDir, audioFilename)
                    
                    if (audioFile.exists()) {
                        downloadedSongIds.add(song.id)
                    }
                } catch (e: Exception) {
                    // Ignore errors for individual songs
                }
            }
            
            _uiState.value = _uiState.value.copy(
                downloadedSongs = downloadedSongIds,
                downloadingSongs = emptySet() // Clear downloading state on refresh
            )
        }
    }

    fun dismissDownloadMessage() {
        _uiState.value = _uiState.value.copy(downloadMessage = null)
    }

    fun downloadCurrentSong() {
        val currentSong = _uiState.value.currentSong ?: return
        
        // Only allow downloading online songs
        if (!currentSong.id.startsWith("online:")) {
            return
        }

        // Check if already downloaded
        if (_uiState.value.downloadedSongs.contains(currentSong.id)) {
            return
        }

        // Check if already downloading
        if (_uiState.value.downloadingSongs.contains(currentSong.id)) {
            return
        }

        viewModelScope.launch {
            try {
                // Mark as downloading
                _uiState.value = _uiState.value.copy(
                    downloadingSongs = _uiState.value.downloadingSongs + currentSong.id
                )
                
                // Get the app's download directory (Download/SyncTax)
                val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "SyncTax")
                if (!downloadDir.exists()) {
                    downloadDir.mkdirs()
                }
                val musicDir = downloadDir // Keep variable name for compatibility
                
                // Create a safe filename with proper audio extension
                val safeTitle = currentSong.title.replace(Regex("[^a-zA-Z0-9\\s-]"), "").trim()
                val safeArtist = currentSong.artist?.replace(Regex("[^a-zA-Z0-9\\s-]"), "")?.trim() ?: "Unknown"
                val baseFilename = "$safeTitle - $safeArtist"
                // Use .opus format since that's what the cache files are
                val audioFilename = "$baseFilename.opus"
                val audioFile = File(musicDir, audioFilename)
                
                // Check if audio file already exists
                if (audioFile.exists()) {
                    // Mark as downloaded and return
                    _uiState.value = _uiState.value.copy(
                        downloadedSongs = _uiState.value.downloadedSongs + currentSong.id,
                        downloadingSongs = _uiState.value.downloadingSongs - currentSong.id
                    )
                    return@launch
                }
                
                // For online songs, find the cached file
                // Extract video ID from song ID (format: "online:${videoId}")
                val videoId = currentSong.id.substringAfter("online:")
                val cacheDir = getApplication<Application>().cacheDir
                val cacheFile = File(cacheDir, "stream_${videoId}.cache")
                
                var downloadSuccess = false
                
                if (cacheFile.exists() && cacheFile.length() > 0) {
                    withContext(Dispatchers.IO) {
                        FileInputStream(cacheFile).use { input ->
                            FileOutputStream(audioFile).use { output ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                var totalBytesRead = 0L
                                val totalBytes = cacheFile.length()
                                
                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    output.write(buffer, 0, bytesRead)
                                    totalBytesRead += bytesRead
                                    
                                    // Update progress
                                    val progress = totalBytesRead.toFloat() / totalBytes.toFloat()
                                    _uiState.value = _uiState.value.copy(
                                        downloadProgress = _uiState.value.downloadProgress + (currentSong.id to progress)
                                    )
                                }
                            }
                        }
                    }
                    downloadSuccess = true
                }
                
                // Download album art if available
                if (downloadSuccess && !currentSong.albumArtUri.isNullOrEmpty()) {
                    try {
                        val albumArtFilename = "$baseFilename.jpg"
                        val albumArtFile = File(musicDir, albumArtFilename)
                        
                        // Upgrade thumbnail URL to maxres quality if it's a YouTube thumbnail
                        val highQualityThumbnail = currentSong.albumArtUri
                            ?.replace("/hqdefault.jpg", "/maxresdefault.jpg")
                            ?.replace("/mqdefault.jpg", "/maxresdefault.jpg")
                            ?.replace("/sddefault.jpg", "/maxresdefault.jpg")
                            ?.replace("/default.jpg", "/maxresdefault.jpg")
                        
                        withContext(Dispatchers.IO) {
                            URL(highQualityThumbnail ?: currentSong.albumArtUri).openStream().use { input ->
                                FileOutputStream(albumArtFile).use { output ->
                                    input.copyTo(output)
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Album art download failed, but audio download succeeded
                        e.printStackTrace()
                    }
                }
                
                // Mark as downloaded if audio download was successful
                if (downloadSuccess) {
                    val downloadLocation = "Download/SyncTax/${audioFile.name}"
                    _uiState.value = _uiState.value.copy(
                        downloadedSongs = _uiState.value.downloadedSongs + currentSong.id,
                        downloadingSongs = _uiState.value.downloadingSongs - currentSong.id,
                        downloadProgress = _uiState.value.downloadProgress + (currentSong.id to 1.0f),
                        downloadMessage = "Downloaded to $downloadLocation"
                    )
                    
                    // Add the downloaded song to the database so it appears in library
                    val downloadedSong = Song(
                        id = audioFile.absolutePath, // Use file path as ID for local songs
                        title = currentSong.title,
                        artist = currentSong.artist,
                        album = currentSong.album,
                        duration = currentSong.duration,
                        filePath = audioFile.absolutePath,
                        genre = currentSong.genre,
                        releaseYear = currentSong.releaseYear,
                        albumArtUri = if (File(musicDir, "$baseFilename.jpg").exists()) {
                            File(musicDir, "$baseFilename.jpg").absolutePath
                        } else {
                            currentSong.albumArtUri
                        }
                    )
                    
                    // Insert into database if not already present
                    if (repository.getSongById(downloadedSong.id) == null) {
                        repository.insertSong(downloadedSong)
                    }
                } else {
                    // Remove from downloading if failed
                    _uiState.value = _uiState.value.copy(
                        downloadingSongs = _uiState.value.downloadingSongs - currentSong.id,
                        downloadProgress = _uiState.value.downloadProgress - currentSong.id
                    )
                }
                
            } catch (e: Exception) {
                e.printStackTrace()
                // Remove from downloading on error
                _uiState.value = _uiState.value.copy(
                    downloadingSongs = _uiState.value.downloadingSongs - currentSong.id,
                    downloadProgress = _uiState.value.downloadProgress - currentSong.id
                )
                // Handle download error - could emit an event or update UI state
            }
        }
    }

    private fun updateNotification() {
        musicService?.updatePlaybackState(
            _uiState.value.currentSong,
            _uiState.value.isPlaying,
            _uiState.value.position,
            _uiState.value.duration,
            currentLyrics
        )
    }

    override fun onCleared() {
        super.onCleared()
        getApplication<Application>().unregisterReceiver(serviceActionReceiver)
        if (isServiceBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isServiceBound = false
        }
        viewModelScope.launch {
            playbackCollector.stopCollecting()
        }
        chunkedStreamManager.stopAndCleanup()
        player.release()
    }

    fun setCurrentLyrics(lyrics: String?) {
        currentLyrics = lyrics
    }
}

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatEnabled: Boolean = false,
    val downloadPercent: Int = 0,
    val volume: Float = 1.0f,
    val downloadedSongs: Set<String> = emptySet(),
    val downloadingSongs: Set<String> = emptySet(),
    val downloadProgress: Map<String, Float> = emptyMap(), // songId to progress (0.0 to 1.0)
        val downloadMessage: String? = null // Download location message for snackbar
)