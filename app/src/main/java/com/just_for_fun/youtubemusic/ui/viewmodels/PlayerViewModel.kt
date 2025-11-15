package com.just_for_fun.youtubemusic.ui.viewmodels

import android.app.Application
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.ServiceConnection
import android.media.AudioManager
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.youtubemusic.core.data.local.entities.Song
import com.just_for_fun.youtubemusic.core.data.preferences.PlayerPreferences
import com.just_for_fun.youtubemusic.core.data.repository.MusicRepository
import com.just_for_fun.youtubemusic.core.player.MusicPlayer
import com.just_for_fun.youtubemusic.core.player.PlaybackCollector
import com.just_for_fun.youtubemusic.service.MusicService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import com.just_for_fun.youtubemusic.core.player.ChunkedStreamManager
import java.io.File

class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val player = MusicPlayer(application)
    private val playbackCollector = PlaybackCollector(repository, player)
    private val playerPreferences = PlayerPreferences(application)
    private val chunkedStreamManager = ChunkedStreamManager(application)
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    private var currentPlaylist: List<Song> = emptyList()
    private var currentIndex: Int = 0
    private var playHistory: MutableList<Song> = mutableListOf()

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
                    musicService?.updatePlaybackState(null, false, 0L, 0L)

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
                        currentPlaylist = playlist
                        currentIndex = savedIndex.coerceIn(0, playlist.size - 1)
                        // Ensure the current song is at the current index
                        if (currentIndex < playlist.size && playlist[currentIndex].id == song.id) {
                            // Good
                        } else {
                            // Find the song in playlist
                            val songIndex = playlist.indexOf(song)
                            if (songIndex != -1) {
                                currentIndex = songIndex
                            } else {
                                // Add it to playlist
                                currentPlaylist = playlist + song
                                currentIndex = playlist.size
                            }
                        }
                    } else {
                        currentPlaylist = listOf(song)
                        currentIndex = 0
                    }
                } else {
                    currentPlaylist = listOf(song)
                    currentIndex = 0
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
            playerPreferences.saveCurrentPlaylist(
                songIds = currentPlaylist.map { it.id },
                currentIndex = currentIndex
            )
        }
    }

    fun playSong(song: Song, playlist: List<Song> = listOf(song)) {
        viewModelScope.launch {
            // Stop any streaming downloads and remove cached chunks of previous song
            chunkedStreamManager.stopAndCleanup(removeFinalCache = true)
            // Stop current song
            if (_uiState.value.currentSong != null) {
                playbackCollector.stopCollecting(skipped = true)
                // Add current song to history
                _uiState.value.currentSong?.let { playHistory.add(it) }
            }

            currentPlaylist = playlist
            currentIndex = playlist.indexOf(song)

            player.prepare(song.filePath, song.id)
            player.play()

            playbackCollector.startCollecting(song.id)

            _uiState.value = _uiState.value.copy(
                currentSong = song,
                isPlaying = true
            )

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

            // Play the online URL
            currentPlaylist = listOf(onlineSong)
            currentIndex = 0
            player.prepare(url, onlineSong.id)
            player.play()

            playbackCollector.startCollecting(onlineSong.id)

            _uiState.value = _uiState.value.copy(
                currentSong = onlineSong,
                isPlaying = true
            )
            savePlaylistState()
            updateNotification()
        }
    }

    /** Play a remote stream using chunked progressive download for 30s segments.
     * This writes to a temp cache file and starts playback as soon as the first chunk is available.
     */
    fun playChunkedStream(videoId: String, streamUrl: String, title: String, artist: String? = null, durationMs: Long = 0L) {
        viewModelScope.launch {
            // Stop any existing streaming downloads
            chunkedStreamManager.stopAndCleanup()

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
                albumArtUri = null
            )

            // Stop current collecting and prepare new
            playbackCollector.stopCollecting(skipped = true)

            currentPlaylist = listOf(onlineSong)
            currentIndex = 0

            player.prepare(tempFile.absolutePath, onlineSong.id)
            player.play()

            playbackCollector.startCollecting(onlineSong.id)

            _uiState.value = _uiState.value.copy(
                currentSong = onlineSong,
                isPlaying = true,
                isBuffering = true
            )

            // Observe chunk download state and update UI; when completed swap file path to final cached file
            viewModelScope.launch {
                chunkedStreamManager.state.collect { st ->
                    _uiState.value = _uiState.value.copy(
                        isBuffering = !st.isComplete,
                        duration = if (durationMs > 0) durationMs else _uiState.value.duration
                        , downloadPercent = st.percent
                    )
                    if (st.isComplete) {
                        // replace currentSong filePath with final cached file if exists
                        val final = File(getApplication<Application>().cacheDir, "stream_${videoId}.cache")
                        if (final.exists()) {
                            _uiState.value.currentSong?.let { song ->
                                val updated = song.copy(filePath = final.absolutePath)
                                _uiState.value = _uiState.value.copy(currentSong = updated)
                            }
                        }
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
                        chunkedStreamManager.requestNextChunk(1)
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
        if (currentPlaylist.isEmpty()) return emptyList()
        return if (currentIndex < currentPlaylist.size - 1) {
            currentPlaylist.subList(currentIndex + 1, currentPlaylist.size)
        } else {
            emptyList()
        }
    }

    fun getPlayHistory(): List<Song> {
        return playHistory.toList()
    }

    fun togglePlayPause() {
        if (_uiState.value.isPlaying) {
            player.pause()
            musicService?.updatePlaybackState(
                _uiState.value.currentSong,
                false,
                _uiState.value.position,
                _uiState.value.duration
            )
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
                _uiState.value.duration
            )
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
            if (currentPlaylist.isEmpty()) return@launch

            // Simply move to next song in the playlist (whether shuffled or not)
            if (currentIndex < currentPlaylist.size - 1) {
                currentIndex++
                val nextSong = currentPlaylist[currentIndex]
                playSong(nextSong, currentPlaylist)
            }
        }
    }

    fun previous() {
        viewModelScope.launch {
            playbackCollector.stopCollecting(skipped = true)
            if (currentPlaylist.isEmpty()) return@launch

            // Simply move to previous song in the playlist (whether shuffled or not)
            if (currentIndex > 0) {
                currentIndex--
                val previousSong = currentPlaylist[currentIndex]
                playSong(previousSong, currentPlaylist)
            }
        }
    }

    private fun onSongEnded() {
        viewModelScope.launch {
            playbackCollector.stopCollecting(skipped = false)
            val repeat = _uiState.value.repeatEnabled
            if (currentPlaylist.isEmpty()) {
                _uiState.value = _uiState.value.copy(isPlaying = false)
                return@launch
            }

            if (repeat) {
                // Repeat current song
                _uiState.value.currentSong?.let { song ->
                    playSong(song, currentPlaylist)
                }
            } else {
                // Auto-play next song in order (whether playlist is shuffled or not)
                if (currentIndex < currentPlaylist.size - 1) {
                    currentIndex++
                    val nextSong = currentPlaylist[currentIndex]
                    playSong(nextSong, currentPlaylist)
                } else {
                    _uiState.value = _uiState.value.copy(isPlaying = false)
                }
            }
        }
    }

    fun toggleShuffle() {
        // Always reshuffle the playlist with current song at first position
        viewModelScope.launch {
            val currentSong = _uiState.value.currentSong
            
            if (currentSong != null && currentPlaylist.isNotEmpty()) {
                // Create a mutable list from current playlist
                val songsToShuffle = currentPlaylist.toMutableList()
                
                // Remove current song from the list
                songsToShuffle.remove(currentSong)
                
                // Shuffle remaining songs
                val shuffledOthers = songsToShuffle.shuffled()
                
                // Place current song at the beginning
                val newPlaylist = mutableListOf(currentSong).apply {
                    addAll(shuffledOthers)
                }
                
                // Update playlist and index
                currentPlaylist = newPlaylist
                currentIndex = 0 // Current song is now at index 0
                
                // Update UI state
                _uiState.value = _uiState.value.copy(shuffleEnabled = true)
                
                savePlaylistState()
            } else {
                // If no current song, just fetch and shuffle all songs
                val allSongs = repository.getAllSongs().first()
                if (allSongs.isNotEmpty()) {
                    val shuffledPlaylist = allSongs.shuffled()
                    currentPlaylist = shuffledPlaylist
                    currentIndex = 0
                    _uiState.value = _uiState.value.copy(shuffleEnabled = true)
                    playSong(shuffledPlaylist[currentIndex], shuffledPlaylist)
                }
            }
        }
    }

    fun toggleRepeat() {
        val enabled = !_uiState.value.repeatEnabled
        _uiState.value = _uiState.value.copy(repeatEnabled = enabled)
    }

    fun shufflePlay(playlist: List<Song>) {
        viewModelScope.launch {
            if (playlist.isEmpty()) return@launch
            
            // Shuffle the playlist
            val shuffledPlaylist = playlist.shuffled()
            currentPlaylist = shuffledPlaylist
            
            // Start playing from the first song in shuffled list
            currentIndex = 0
            _uiState.value = _uiState.value.copy(shuffleEnabled = true)
            playSong(shuffledPlaylist[currentIndex], shuffledPlaylist)
        }
    }

    fun removeFromQueue(song: Song) {
        viewModelScope.launch {
            val songIndex = currentPlaylist.indexOf(song)
            if (songIndex != -1) {
                // If removing current song, stop playback
                if (songIndex == currentIndex) {
                    player.pause()
                    playbackCollector.stopCollecting(skipped = true)
                    _uiState.value = _uiState.value.copy(
                        currentSong = null,
                        isPlaying = false
                    )
                } else if (songIndex < currentIndex) {
                    // If removing song before current index, adjust current index
                    currentIndex--
                }

                // Remove from playlist
                currentPlaylist = currentPlaylist.toMutableList().apply { removeAt(songIndex) }
                savePlaylistState()
            }
        }
    }

    fun placeNext(song: Song) {
        viewModelScope.launch {
            // Remove song from current position if it's already in the queue
            val currentPosition = currentPlaylist.indexOf(song)
            currentPlaylist = currentPlaylist.toMutableList().apply {
                if (currentPosition != -1) {
                    removeAt(currentPosition)
                    // Adjust current index if necessary
                    if (currentPosition < currentIndex) {
                        currentIndex--
                    } else if (currentPosition == currentIndex) {
                        // If we're moving the current song, keep it as current
                        currentIndex = 0
                    }
                }
            }

            // Insert song at position after current song
            val insertPosition = currentIndex + 1
            currentPlaylist = currentPlaylist.toMutableList().apply {
                add(insertPosition, song)
            }
            savePlaylistState()
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            if (fromIndex in currentPlaylist.indices && toIndex in currentPlaylist.indices) {
                val mutablePlaylist = currentPlaylist.toMutableList()
                val song = mutablePlaylist.removeAt(fromIndex)
                mutablePlaylist.add(toIndex, song)

                // Update current index if necessary
                when {
                    fromIndex == currentIndex -> currentIndex = toIndex
                    fromIndex < currentIndex && toIndex >= currentIndex -> currentIndex--
                    fromIndex > currentIndex && toIndex <= currentIndex -> currentIndex++
                }

                currentPlaylist = mutablePlaylist
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

    private fun updateNotification() {
        musicService?.updatePlaybackState(
            _uiState.value.currentSong,
            _uiState.value.isPlaying,
            _uiState.value.position,
            _uiState.value.duration
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
    val volume: Float = 1.0f
)