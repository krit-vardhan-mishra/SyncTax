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
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.synctax.core.chaquopy.ChaquopyAudioDownloader
import com.just_for_fun.synctax.core.data.local.entities.OnlineSong
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.core.data.model.Format
import com.just_for_fun.synctax.core.data.preferences.PlayerPreferences
import com.just_for_fun.synctax.core.data.repository.MusicRepository
import com.just_for_fun.synctax.core.ml.MusicRecommendationManager
import com.just_for_fun.synctax.core.network.OnlineSearchManager
import com.just_for_fun.synctax.core.player.ChunkedStreamManager
import com.just_for_fun.synctax.core.player.MusicPlayer
import com.just_for_fun.synctax.core.player.PlaybackCollector
import com.just_for_fun.synctax.core.player.PlaybackEvent
import com.just_for_fun.synctax.core.player.PlaybackEventBus
import com.just_for_fun.synctax.core.player.QueueManager
import com.just_for_fun.synctax.core.utils.AudioProcessor
import com.just_for_fun.synctax.service.MusicService
import com.just_for_fun.synctax.util.FormatUtil
import com.just_for_fun.synctax.util.YTMusicRecommender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import com.just_for_fun.synctax.core.player.PreloadManager
import com.just_for_fun.synctax.core.player.StreamUrlCache
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.io.File
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch


class PlayerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val player = MusicPlayer(application)
    private val playerPreferences = PlayerPreferences(application)
    private val chunkedStreamManager = ChunkedStreamManager(application)
    private val audioManager = application.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    private val recommendationManager = MusicRecommendationManager(application)
    private val queueManager = QueueManager(application, repository, recommendationManager)
    private val audioProcessor = AudioProcessor(application)
    private val chaquopyDownloader = ChaquopyAudioDownloader.getInstance(application)
    private val onlineManager = OnlineSearchManager()
    
    // Preload manager for instant song switching
    private val preloadManager = PreloadManager(application)

    // Error messages flow for UI
    private val _errorMessages = MutableSharedFlow<String>()
    val errorMessages = _errorMessages.asSharedFlow()

    // Track active download jobs for cancellation support
    private val activeDownloadJobs = mutableMapOf<String, kotlinx.coroutines.Job>()

    // PlaybackCollector with cache invalidation callback
    private val playbackCollector = PlaybackCollector(
        repository = repository,
        player = player,
        scope = viewModelScope, // Use viewModelScope for lifecycle management
        onPlaybackRecorded = {
            // Invalidate Quick Picks cache when new playback is recorded
            recommendationManager.invalidateQuickPicksCache()
        }
    )

    // Track if we're currently handling song end to prevent multiple triggers
    private var isHandlingSongEnd = false
    private var currentLyrics: String? = null
    
    // Prefetching state (legacy - kept for compatibility)
    private var prefetchedSongId: String? = null
    private var isPrefetching = false
    private var prefetchJob: Job? = null
    
    // Stable recommendations - keep original list and only refetch when empty
    private var originalRecommendationVideoId: String? = null
    
    // Track played recommendations for previous navigation
    private val playedRecommendationsHistory = mutableListOf<Song>()

    private fun logSongDetails(song: Song) {
        // Simplified playback log; detailed metadata is logged when fetched via yt-dlp
        Log.d("PlayerViewModel", "🎵 Playing: ${song.title} — ${song.artist} (${song.album ?: "Unknown"})")
    }

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
                        getApplication<Application>().stopService(
                            Intent(
                                getApplication(),
                                MusicService::class.java
                            )
                        )
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

                // Prefetch next song when reaching 75% progress
                val duration = _uiState.value.duration
                if (duration > 0 && position >= (duration * 0.75)) {
                    prefetchNextSongIfNeeded()
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
        // Check if the last played song was an online song
        if (playerPreferences.isOnlineSong()) {
            restoreOnlineSong()
            return
        }
        
        val lastSongId = playerPreferences.getCurrentSongId() ?: return
        val lastPosition = playerPreferences.getLastPosition()
        val playlistIds = playerPreferences.getCurrentPlaylist()
        val savedIndex = playerPreferences.getCurrentIndex()

        try {
            val song = repository.getSongById(lastSongId)
            if (song != null) {
                // Clear any online song state since we're restoring a local song
                playerPreferences.clearOnlineSongState()
                
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
    
    /**
     * Restore online song state when app reopens
     */
    private suspend fun restoreOnlineSong() {
        val onlineState = playerPreferences.getOnlineSongState() ?: return
        
        try {
            Log.d("PlayerViewModel", "🔄 Restoring online song: ${onlineState.title}")
            
            // Create a Song object for the online song
            val onlineSong = Song(
                id = "online:${onlineState.videoId}",
                title = onlineState.title,
                artist = onlineState.artist,
                album = null,
                duration = 0L,
                filePath = onlineState.watchUrl,
                genre = null,
                releaseYear = null,
                albumArtUri = onlineState.thumbnailUrl
            )
            
            // Initialize queue with the online song
            queueManager.initializeQueue(listOf(onlineSong), 0)
            
            // Set the UI state with the restored song (don't auto-play)
            _uiState.value = _uiState.value.copy(
                currentSong = onlineSong,
                position = onlineState.position,
                isPlaying = false // Don't auto-play on restore
            )
            
            // Update notification
            updateNotification()
            
            // Fetch recommendations for the restored song
            if (onlineState.videoId.isNotEmpty()) {
                fetchRecommendationsForVideoId(onlineState.videoId)
            }
            
            Log.d("PlayerViewModel", "✅ Online song restored: ${onlineState.title}")
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "❌ Failed to restore online song: ${e.message}")
            e.printStackTrace()
        }
    }
    
    /**
     * Fetch recommendations for a video ID
     */
    private fun fetchRecommendationsForVideoId(videoId: String) {
        if (_uiState.value.upNextRecommendations.isEmpty() && playedRecommendationsHistory.isEmpty()) {
            _uiState.value = _uiState.value.copy(isLoadingRecommendations = true)
            originalRecommendationVideoId = videoId
            
            YTMusicRecommender.getRecommendations(
                videoId = videoId,
                limit = 50,
                onResult = { recommendations ->
                    Log.d("PlayerViewModel", "🎵 Got ${recommendations.size} YouTube recommendations for restored song")
                    // Convert RecommendedSong to Song
                    val recommendedSongs = recommendations.map { rec ->
                        Song(
                            id = "youtube:${rec.videoId}",
                            title = rec.title,
                            artist = rec.artist,
                            album = null,
                            duration = 0L,
                            filePath = rec.watchUrl,
                            genre = null,
                            releaseYear = null,
                            albumArtUri = rec.thumbnail
                        )
                    }
                    _uiState.value = _uiState.value.copy(
                        upNextRecommendations = recommendedSongs,
                        isLoadingRecommendations = false
                    )
                },
                onError = { error ->
                    Log.e("PlayerViewModel", "❌ Failed to fetch recommendations: $error")
                    _uiState.value = _uiState.value.copy(isLoadingRecommendations = false)
                }
            )
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
            
            // Clear online song state when switching to local songs
            playerPreferences.clearOnlineSongState()

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

            // Log current playing song details
            Log.d("Storage Scene", Environment.getExternalStorageDirectory().toString())
            logSongDetails(song)

            // Check if song is already downloaded
            checkIfSongDownloaded(song)

            // Emit playback events
            PlaybackEventBus.emit(PlaybackEvent.SongChanged(song))
            PlaybackEventBus.emit(PlaybackEvent.PlaybackStateChanged(true))
            PlaybackEventBus.emit(
                PlaybackEvent.QueueUpdated(
                    upcomingQueue = queueManager.getUpcomingQueue(),
                    playHistory = queueManager.getPlayHistory()
                )
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

    /**
     * Play a list of songs in shuffled order (for album/playlist shuffle playback)
     * Shuffles the songs first, then plays from the beginning
     */
    fun playSongShuffled(playlist: List<Song>) {
        if (playlist.isEmpty()) return
        val shuffledPlaylist = playlist.shuffled()
        playSong(shuffledPlaylist.first(), shuffledPlaylist)
    }

    fun playUrl(url: String, title: String, artist: String? = null, durationMs: Long = 0L, thumbnailUrl: String? = null, skipRecommendationFetch: Boolean = false) {
        viewModelScope.launch {
            // Extract video ID from YouTube URL
            val videoId = url.substringAfter("v=").substringBefore("&").substringAfter("youtu.be/").substringBefore("?")
            
            // Record online listening history for YouTube songs
            if (url.contains("youtube.com") || url.contains("youtu.be")) {
                val database = com.just_for_fun.synctax.core.data.local.MusicDatabase.getDatabase(getApplication())
                val finalThumbnailUrl = thumbnailUrl ?: "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
                val onlineHistory = com.just_for_fun.synctax.core.data.local.entities.OnlineListeningHistory(
                    videoId = videoId,
                    title = title,
                    artist = artist ?: "Unknown Artist",
                    thumbnailUrl = finalThumbnailUrl,
                    watchUrl = url
                )
                // Delete existing entry with same videoId to prevent duplicates
                database.onlineListeningHistoryDao().deleteByVideoId(videoId)
                // Insert new entry with updated timestamp
                database.onlineListeningHistoryDao().insertOnlineListening(onlineHistory)
                database.onlineListeningHistoryDao().trimOldRecords()
            }
            
            // Check if this is a YouTube URL that needs stream extraction
            if (url.contains("youtube.com") || url.contains("youtu.be")) {
                Log.d("PlayerViewModel", "🔄 Extracting stream URL for YouTube video: $videoId")
                
                // Check if stream URL is already cached (from preloading)
                val cachedUrl = StreamUrlCache.get(videoId)
                if (cachedUrl != null) {
                    Log.d("PlayerViewModel", "✅ Using cached stream URL for instant playback")
                    val finalThumbnailUrl = thumbnailUrl ?: "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
                    playChunkedStream(
                        videoId = videoId,
                        streamUrl = cachedUrl,
                        title = title,
                        artist = artist,
                        durationMs = durationMs,
                        thumbnailUrl = finalThumbnailUrl,
                        skipRecommendationFetch = skipRecommendationFetch
                    )
                    return@launch
                }
                
                // Set loading state for online songs
                _uiState.value = _uiState.value.copy(isLoadingSong = true)
                
                // Fetch the actual stream URL
                val streamUrl = onlineManager.getStreamUrl(videoId)
                
                if (streamUrl != null) {
                    Log.d("PlayerViewModel", "✅ Stream URL extracted, using chunked playback")
                    // Clear loading state when stream is ready
                    _uiState.value = _uiState.value.copy(isLoadingSong = false)
                    // Generate thumbnail URL from video ID if not provided
                    val finalThumbnailUrl = thumbnailUrl ?: "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
                    // Use chunked stream playback with the extracted URL
                    playChunkedStream(
                        videoId = videoId,
                        streamUrl = streamUrl,
                        title = title,
                        artist = artist,
                        durationMs = durationMs,
                        thumbnailUrl = finalThumbnailUrl,
                        skipRecommendationFetch = skipRecommendationFetch
                    )
                } else {
                    Log.e("PlayerViewModel", "❌ Failed to extract stream URL for: $videoId")
                    // Clear loading state on error
                    _uiState.value = _uiState.value.copy(isLoadingSong = false)
                    _errorMessages.emit("Failed to play song: Could not extract stream")
                }
                return@launch
            }
            
            // For non-YouTube URLs, play directly
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

            // Log song details
            logSongDetails(onlineSong)

            _uiState.value = _uiState.value.copy(
                currentSong = onlineSong,
                isPlaying = true
            )
            // Check if song is already downloaded
            checkIfSongDownloaded(onlineSong)
            savePlaylistState()
            updateNotification()

            // Fetch richer metadata via yt-dlp and update the UI song fields
            viewModelScope.launch {
                try {
                    val videoId = title.substringAfter("online:", "") // best-effort; if not available, fallback
                    val resolvedUrl = if (url.startsWith("http")) url else "https://www.youtube.com/watch?v=$videoId"
                    val info = try {
                        chaquopyDownloader.getVideoInfo(resolvedUrl, null)
                    } catch (e: Exception) { null }

                    if (info != null) {
                        val fetchedTitle = info.title.takeIf { it.isNotEmpty() && !it.contains("recommended", ignoreCase = true) && it != "video" } ?: onlineSong.title
                        val fetchedArtist = info.artist.takeIf { it.isNotEmpty() && it != "Unknown" } ?: onlineSong.artist
                        val fetchedAlbum = info.album.takeIf { it.isNotEmpty() } ?: onlineSong.album
                        
                        val updated = onlineSong.copy(
                            title = fetchedTitle,
                            artist = fetchedArtist,
                            album = fetchedAlbum
                        )
                        _uiState.value = _uiState.value.copy(currentSong = updated)
                        Log.d("PlayerViewModel", "📥 Metadata: Title=${updated.title}, Artist=${updated.artist}, Album=${updated.album}")
                        updateNotification()
                    }
                } catch (e: Exception) {
                    Log.w("PlayerViewModel", "📥 Metadata fetch failed for playUrl: ${e.message}")
                }
            }
            
            // Fetch YouTube Music recommendations based on current song (songs only, no videos/playlists)
            // Skip if explicitly requested (e.g., playing from existing recommendation list)
            if (!skipRecommendationFetch && (url.contains("youtube.com") || url.contains("youtu.be"))) {
                // Extract video ID from URL
                val videoId = url.substringAfter("v=").substringBefore("&").substringAfter("youtu.be/").substringBefore("?")
                
                // Only fetch if recommendations list is empty (all songs played) AND played history is also empty (fresh start)
                // This ensures recommendations stay stable during continuous playback
                val shouldFetchRecommendations = _uiState.value.upNextRecommendations.isEmpty() && 
                                                playedRecommendationsHistory.isEmpty() &&
                                                originalRecommendationVideoId != videoId
                
                if (shouldFetchRecommendations) {
                    // Clear history when fetching fresh recommendations (new search)
                    playedRecommendationsHistory.clear()
                    _uiState.value = _uiState.value.copy(isLoadingRecommendations = true)
                    originalRecommendationVideoId = videoId
                    
                    YTMusicRecommender.getRecommendations(
                        videoId = videoId,
                        limit = 50,
                        onResult = { recommendations ->
                            Log.d("PlayerViewModel", "🎵 Got ${recommendations.size} YouTube recommendations")
                            if (recommendations.isEmpty()) {
                                // No recommendations available - enable repeat mode to loop the current song
                                Log.d("PlayerViewModel", "🔁 No recommendations available, enabling repeat mode for continuous playback")
                                _uiState.value = _uiState.value.copy(
                                    repeatEnabled = true,
                                    upNextRecommendations = emptyList(),
                                    isLoadingRecommendations = false
                                )
                            } else {
                                // Convert RecommendedSong to Song objects for UI
                                val recommendedSongs = recommendations
                                    .filter { rec -> rec.videoId != videoId } // Filter out current song to avoid loop
                                    .map { rec ->
                                        Song(
                                            id = "youtube:${rec.videoId}",
                                            title = rec.title,
                                            artist = rec.artist,
                                            album = null,
                                            duration = 0L, // Duration unknown from search API
                                            filePath = rec.watchUrl,
                                            genre = null,
                                            releaseYear = null,
                                            albumArtUri = rec.thumbnail
                                        )
                                    }
                                Log.d("PlayerViewModel", "🎵 Filtered recommendations: ${recommendedSongs.size} songs (excluded current)")
                                _uiState.value = _uiState.value.copy(
                                    upNextRecommendations = recommendedSongs,
                                    isLoadingRecommendations = false,
                                    repeatEnabled = false  // Disable repeat when recommendations are available
                                )
                            }
                        },
                        onError = { error ->
                            Log.e("PlayerViewModel", "❌ YouTube recommendations failed: $error")
                            // On error, also enable repeat mode to loop the current song
                            Log.d("PlayerViewModel", "🔁 Recommendation fetch failed, enabling repeat mode for continuous playback")
                            _uiState.value = _uiState.value.copy(
                                upNextRecommendations = emptyList(),
                                isLoadingRecommendations = false,
                                repeatEnabled = true
                            )
                        }
                    )
                } else {
                    Log.d("PlayerViewModel", "🎵 Using existing recommendations, not refetching")
                }
            }
        }
    }
    
    // Function to play a recommended song from UpNext
    fun playRecommendedSong(song: Song) {
        viewModelScope.launch {
            // Add current song to history before playing next
            _uiState.value.currentSong?.let { current ->
                if (current.id.startsWith("online:") || current.id.startsWith("youtube:")) {
                    playedRecommendationsHistory.add(current)
                    // Keep history size limited to 50 songs
                    if (playedRecommendationsHistory.size > 50) {
                        playedRecommendationsHistory.removeAt(0)
                    }
                }
            }
            
            // Check if song is preloaded in ExoPlayer cache
            val videoId = song.id.removePrefix("online:").removePrefix("youtube:")
            val cachedStreamUrl = preloadManager.getCachedStreamUrl(song)
            val isPreloaded = preloadManager.isPreloaded(song)
            
            if (isPreloaded && cachedStreamUrl != null) {
                // Use cached playback for instant switching
                Log.d("PlayerViewModel", "🚀 Using preloaded cache for instant playback: ${song.title}")
                playWithCachedStream(song, videoId, cachedStreamUrl)
            } else {
                // Fall back to regular playback (will use ChunkedStreamManager)
                val youtubeUrl = song.filePath
                playUrl(url = youtubeUrl, title = song.title, artist = song.artist, durationMs = song.duration, 
                       thumbnailUrl = song.albumArtUri, skipRecommendationFetch = true)
            }
        }
    }
    
    /**
     * Play a song using ExoPlayer's cached stream for instant playback.
     * This is used for preloaded songs that have data in the cache.
     */
    private suspend fun playWithCachedStream(song: Song, videoId: String, streamUrl: String) {
        // Clean up previous stream's tmp file if it's safe
        chunkedStreamManager.cleanupTmpFile()
        // Stop any active chunked download but DON'T delete cache files that player might need
        chunkedStreamManager.stopAndCleanup(removeFinalCache = false)
        
        // Stop current song
        playbackCollector.stopCollecting(skipped = true)
        
        // Upgrade thumbnail URL to high quality
        val highQualityThumbnail = song.albumArtUri?.let { url ->
            when {
                url.contains("lh3.googleusercontent.com") -> url.replace(Regex("=w\\d+-h\\d+"), "=w544-h544")
                url.contains("ytimg.com") && url.contains("/default.jpg") -> url.replace("/default.jpg", "/maxresdefault.jpg")
                url.contains("ytimg.com") && url.contains("/mqdefault.jpg") -> url.replace("/mqdefault.jpg", "/maxresdefault.jpg")
                url.contains("ytimg.com") && url.contains("/hqdefault.jpg") -> url.replace("/hqdefault.jpg", "/maxresdefault.jpg")
                else -> url
            }
        } ?: "https://img.youtube.com/vi/$videoId/maxresdefault.jpg"
        
        val onlineSong = Song(
            id = "online:$videoId",
            title = song.title,
            artist = song.artist,
            album = null,
            duration = song.duration,
            filePath = streamUrl, // Use the stream URL directly
            genre = null,
            releaseYear = null,
            albumArtUri = highQualityThumbnail
        )
        
        // Initialize queue with this song
        queueManager.initializeQueue(listOf(onlineSong), 0)
        
        // Use prepareWithCacheKey to leverage ExoPlayer's cached data
        player.prepareWithCacheKey(streamUrl, onlineSong.id, videoId)
        player.play()
        
        playbackCollector.startCollecting(onlineSong.id)
        
        // Start preloading next songs
        triggerPreloading(onlineSong)
        
        logSongDetails(onlineSong)
        
        _uiState.value = _uiState.value.copy(
            currentSong = onlineSong,
            isPlaying = true,
            isBuffering = false // Preloaded, no buffering needed
        )
        
        checkIfSongDownloaded(onlineSong)
        savePlaylistState()
        updateNotification()
        
        // Fetch richer metadata
        viewModelScope.launch {
            try {
                val url = "https://www.youtube.com/watch?v=$videoId"
                val info = try {
                    chaquopyDownloader.getVideoInfo(url, null)
                } catch (e: Exception) { null }

                if (info != null) {
                    val fetchedTitle = info.title.takeIf { it.isNotEmpty() && !it.contains("recommended", ignoreCase = true) && it != "video" } ?: onlineSong.title
                    val fetchedArtist = info.artist.takeIf { it.isNotEmpty() && it != "Unknown" } ?: onlineSong.artist
                    val fetchedAlbum = info.album.takeIf { it.isNotEmpty() } ?: onlineSong.album
                    
                    val updated = onlineSong.copy(
                        title = fetchedTitle,
                        artist = fetchedArtist,
                        album = fetchedAlbum
                    )
                    _uiState.value = _uiState.value.copy(currentSong = updated)
                    Log.d("PlayerViewModel", "📥 Metadata: Title=${updated.title}, Artist=${updated.artist}, Album=${updated.album}")
                    updateNotification()
                }
            } catch (e: Exception) {
                Log.w("PlayerViewModel", "📥 Metadata fetch failed for cached playback: ${e.message}")
            }
        }
    }

    /**
     * Play a list of online songs in order (for album/playlist playback)
     * Sets the remaining songs as "Up Next" queue
     */
    fun playOnlinePlaylist(songs: List<Song>, startIndex: Int = 0) {
        viewModelScope.launch {
            if (songs.isEmpty()) return@launch
            
            // Clear any existing online recommendations and history
            playedRecommendationsHistory.clear()
            
            // Set the remaining songs as the up next queue (excluding the first song)
            val upNextSongs = songs.drop(startIndex + 1)
            _uiState.value = _uiState.value.copy(
                upNextRecommendations = upNextSongs
            )
            
            // Play the first song
            val firstSong = songs[startIndex]
            playUrl(
                url = firstSong.filePath,
                title = firstSong.title,
                artist = firstSong.artist,
                durationMs = firstSong.duration,
                thumbnailUrl = firstSong.albumArtUri,
                skipRecommendationFetch = true // Don't fetch recommendations, use the playlist
            )
        }
    }

    /**
     * Play a list of online songs in shuffled order
     * Shuffles the songs and sets them as "Up Next" queue
     */
    fun playOnlinePlaylistShuffled(songs: List<Song>) {
        val shuffledSongs = songs.shuffled()
        playOnlinePlaylist(shuffledSongs, 0)
    }

    /**
     * Play a list of RecommendedSong objects in order (for online artist/playlist playback)
     * Converts RecommendedSong to Song and sets remaining songs as "Up Next" queue
     * This is used for "Play All" - no recommendations will be fetched
     */
    fun playRecommendedSongsPlaylist(recommendedSongs: List<com.just_for_fun.synctax.util.RecommendedSong>, startIndex: Int = 0) {
        val songs = recommendedSongs.map { rec ->
            Song(
                id = "youtube:${rec.videoId}",
                title = rec.title,
                artist = rec.artist,
                album = null,
                duration = 0L,
                filePath = rec.watchUrl,
                genre = null,
                releaseYear = null,
                albumArtUri = rec.thumbnail
            )
        }
        playOnlinePlaylist(songs, startIndex)
    }

    /**
     * Play a list of RecommendedSong objects in shuffled order
     * This is used for "Shuffle" - no recommendations will be fetched
     */
    fun playRecommendedSongsShuffled(recommendedSongs: List<com.just_for_fun.synctax.util.RecommendedSong>) {
        val shuffled = recommendedSongs.shuffled()
        playRecommendedSongsPlaylist(shuffled, 0)
    }

    /**
     * Play a single online song with recommendations
     * This is used when user clicks on an individual song in artist/album/playlist detail screen
     * Recommendations WILL be fetched for continuous playback
     */
    fun playOnlineSongWithRecommendations(
        videoId: String,
        title: String,
        artist: String,
        thumbnailUrl: String?
    ) {
        viewModelScope.launch {
            // Clear existing queue and recommendations
            playedRecommendationsHistory.clear()
            _uiState.value = _uiState.value.copy(upNextRecommendations = emptyList())
            
            // Play the song - this will fetch recommendations automatically
            playUrl(
                url = "https://music.youtube.com/watch?v=$videoId",
                title = title,
                artist = artist,
                durationMs = 0L,
                thumbnailUrl = thumbnailUrl,
                skipRecommendationFetch = false // Fetch recommendations for individual song click
            )
        }
    }

    /**
     * Play a RecommendedSong with recommendations
     * This is used when user clicks on an individual song in online artist detail screen
     * Recommendations WILL be fetched for continuous playback
     */
    fun playRecommendedSongWithRecommendations(song: com.just_for_fun.synctax.util.RecommendedSong) {
        playOnlineSongWithRecommendations(
            videoId = song.videoId,
            title = song.title,
            artist = song.artist,
            thumbnailUrl = song.thumbnail
        )
    }

    /**
     * Play an OnlineSong with recommendations
     * This is used when user clicks on an individual song in playlist detail screen
     * Recommendations WILL be fetched for continuous playback
     */
    fun playOnlineSongEntityWithRecommendations(song: OnlineSong) {
        playOnlineSongWithRecommendations(
            videoId = song.videoId,
            title = song.title,
            artist = song.artist,
            thumbnailUrl = song.thumbnailUrl
        )
    }

    /**
     * Play a list of OnlineSong entities in order (for playlist detail "Play All")
     * Sets the remaining songs as "Up Next" queue - NO recommendations fetched
     */
    fun playOnlineSongEntitiesPlaylist(songs: List<OnlineSong>, startIndex: Int = 0) {
        val convertedSongs = songs.map { song ->
            Song(
                id = "youtube:${song.videoId}",
                title = song.title,
                artist = song.artist,
                album = song.album,
                duration = (song.duration ?: 0) * 1000L,
                filePath = "https://music.youtube.com/watch?v=${song.videoId}",
                genre = null,
                releaseYear = null,
                albumArtUri = song.thumbnailUrl
            )
        }
        playOnlinePlaylist(convertedSongs, startIndex)
    }

    /**
     * Play a list of OnlineSong entities in shuffled order (for playlist detail "Shuffle")
     * NO recommendations fetched
     */
    fun playOnlineSongEntitiesShuffled(songs: List<OnlineSong>) {
        val shuffledSongs = songs.shuffled()
        playOnlineSongEntitiesPlaylist(shuffledSongs, 0)
    }

    /** Play a remote stream using chunked progressive download for 30s segments.
     * This writes to a temp cache file and starts playback as soon as the first chunk is available.
     * @param skipRecommendationFetch If true, don't fetch recommendations (used for playlist/album playback)
     */
    fun playChunkedStream(
        videoId: String,
        streamUrl: String,
        title: String,
        artist: String? = null,
        durationMs: Long = 0L,
        thumbnailUrl: String? = null,
        skipRecommendationFetch: Boolean = false
    ) {
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
            
            // Start preloading adjacent songs for instant switching
            triggerPreloading(onlineSong)

            // Log song details
            logSongDetails(onlineSong)

            _uiState.value = _uiState.value.copy(
                currentSong = onlineSong,
                isPlaying = true,
                isBuffering = true
            )

            // Check if song is already downloaded
            checkIfSongDownloaded(onlineSong)
            
            // Save online song state for restoration when app reopens
            playerPreferences.saveOnlineSongState(
                videoId = videoId,
                title = title,
                artist = artist ?: "Unknown",
                thumbnailUrl = highQualityThumbnail,
                watchUrl = "https://www.youtube.com/watch?v=$videoId",
                position = 0L,
                isPlaying = true
            )

            // Observe chunk download state and update UI; when completed swap file path to final cached file
            viewModelScope.launch {
                chunkedStreamManager.state.collect { st ->
                    _uiState.value = _uiState.value.copy(
                        isBuffering = !st.isComplete,
                        duration = if (durationMs > 0) durationMs else _uiState.value.duration,
                        downloadPercent = st.percent
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
                    
                    // Update saved position periodically for online songs
                    _uiState.value.currentSong?.let { song ->
                        if (song.id.startsWith("online:")) {
                            val currentPosition = player.getCurrentPosition()
                            if (currentPosition % 5000 < 1000) { // Save every ~5 seconds
                                playerPreferences.saveOnlineSongState(
                                    videoId = videoId,
                                    title = title,
                                    artist = artist ?: "Unknown",
                                    thumbnailUrl = highQualityThumbnail,
                                    watchUrl = "https://www.youtube.com/watch?v=$videoId",
                                    position = currentPosition,
                                    isPlaying = _uiState.value.isPlaying
                                )
                            }
                        }
                    }
                }
            }

            savePlaylistState()
            updateNotification()

            // Fetch richer metadata via yt-dlp and update the UI song fields for full-screen player
            viewModelScope.launch {
                try {
                    val url = "https://www.youtube.com/watch?v=$videoId"
                    val info = try {
                        chaquopyDownloader.getVideoInfo(url, null)
                    } catch (e: Exception) { null }

                    if (info != null) {
                        val fetchedTitle = info.title.takeIf { it.isNotEmpty() && !it.contains("recommended", ignoreCase = true) && it != "video" } ?: onlineSong.title
                        val fetchedArtist = info.artist.takeIf { it.isNotEmpty() && it != "Unknown" } ?: onlineSong.artist
                        val fetchedAlbum = info.album.takeIf { it.isNotEmpty() } ?: onlineSong.album
                        
                        val updated = onlineSong.copy(
                            title = fetchedTitle,
                            artist = fetchedArtist,
                            album = fetchedAlbum
                        )
                        _uiState.value = _uiState.value.copy(currentSong = updated)
                        Log.d("PlayerViewModel", "📥 Metadata: Title=${updated.title}, Artist=${updated.artist}, Album=${updated.album}")
                        updateNotification()
                    }
                } catch (e: Exception) {
                    Log.w("PlayerViewModel", "📥 Metadata fetch failed for stream: ${e.message}")
                }
            }
            
            // Skip recommendation fetching if playing from a playlist/album (Play All or Shuffle)
            if (skipRecommendationFetch) {
                Log.d("PlayerViewModel", "🎵 Skipping recommendation fetch (playlist/album mode)")
                // Don't clear upNextRecommendations - they were already set by playOnlinePlaylist
                return@launch
            }
            
            // Fetch YouTube Music recommendations after stream extraction completes (songs only)
            // Clear existing recommendations when starting a new song to get fresh ones
            _uiState.value = _uiState.value.copy(upNextRecommendations = emptyList())
            playedRecommendationsHistory.clear()
            
            // Only fetch if recommendations list is completely empty (all songs played) and history is empty
            val shouldFetchRecommendations = originalRecommendationVideoId != videoId
            
            if (shouldFetchRecommendations) {
                Log.d("PlayerViewModel", "🎵 Stream extraction complete, fetching YouTube Music recommendations for videoId=$videoId")
                _uiState.value = _uiState.value.copy(isLoadingRecommendations = true)
                originalRecommendationVideoId = videoId
                
                YTMusicRecommender.getRecommendations(
                    videoId = videoId,
                    limit = 50,
                    onResult = { recommendations ->
                        Log.d("PlayerViewModel", "🎵 Got ${recommendations.size} YouTube recommendations for chunked stream")
                        if (recommendations.isEmpty()) {
                            // No recommendations available - enable repeat mode to loop the current song
                            Log.d("PlayerViewModel", "🔁 No recommendations available, enabling repeat mode for continuous playback")
                            _uiState.value = _uiState.value.copy(
                                repeatEnabled = true,
                                upNextRecommendations = emptyList(),
                                isLoadingRecommendations = false
                            )
                        } else {
                            // Convert RecommendedSong to Song objects for UI
                            val recommendedSongs = recommendations
                                .filter { rec -> rec.videoId != videoId } // Filter out current song to avoid loop
                                .map { rec ->
                                    Song(
                                        id = "youtube:${rec.videoId}",
                                        title = rec.title,
                                        artist = rec.artist,
                                        album = null,
                                        duration = 0L, // Duration unknown from search API
                                        filePath = rec.watchUrl,
                                        genre = null,
                                        releaseYear = null,
                                        albumArtUri = rec.thumbnail
                                    )
                                }
                            Log.d("PlayerViewModel", "🎵 Filtered chunked stream recommendations: ${recommendedSongs.size} songs (excluded current)")
                            _uiState.value = _uiState.value.copy(
                                upNextRecommendations = recommendedSongs,
                                isLoadingRecommendations = false,
                                repeatEnabled = false  // Disable repeat when recommendations are available
                            )
                        }
                    },
                    onError = { error ->
                        Log.e("PlayerViewModel", "❌ YouTube recommendations failed for chunked stream: $error")
                        // On error, also enable repeat mode to loop the current song
                        Log.d("PlayerViewModel", "🔁 Recommendation fetch failed, enabling repeat mode for continuous playback")
                        _uiState.value = _uiState.value.copy(
                            upNextRecommendations = emptyList(),
                            isLoadingRecommendations = false,
                            repeatEnabled = true
                        )
                    }
                )
            } else {
                Log.d("PlayerViewModel", "🎵 Using existing recommendations, not refetching")
            }
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
                val currentSong = _uiState.value.currentSong
                
                // Check if this is a restored online song that needs stream URL preparation
                if (currentSong != null && 
                    currentSong.id.startsWith("online:") && 
                    !player.isSourcePrepared()) {
                    
                    Log.d("PlayerViewModel", "🔄 Restored online song needs stream URL preparation")
                    
                    // Extract video ID and play using playUrl which handles stream extraction
                    val videoId = currentSong.id.removePrefix("online:")
                    val watchUrl = currentSong.filePath.ifEmpty { 
                        "https://www.youtube.com/watch?v=$videoId" 
                    }
                    
                    playUrl(
                        url = watchUrl,
                        title = currentSong.title,
                        artist = currentSong.artist,
                        durationMs = currentSong.duration,
                        thumbnailUrl = currentSong.albumArtUri,
                        skipRecommendationFetch = true // Already have recommendations from restore
                    )
                    return@launch
                }
                
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

    /**
     * Prefetch next song in queue when reaching 75% of current song
     * This prepares the stream URL and potentially starts downloading the first chunks
     */
    private fun prefetchNextSongIfNeeded() {
        val currentSong = _uiState.value.currentSong ?: return
        
        // For online songs, check recommendations first, then queue
        var nextSong: Song? = null
        if ((currentSong.id.startsWith("online:") || currentSong.id.startsWith("youtube:")) 
            && _uiState.value.upNextRecommendations.isNotEmpty()) {
            // Use first recommendation as next song
            nextSong = _uiState.value.upNextRecommendations.firstOrNull()
        } else {
            // Use queue manager for regular songs
            nextSong = queueManager.peekNext()
        }
        
        if (nextSong == null) return
        
        // Only prefetch if this is a different song and we're not already prefetching
        if (nextSong.id == prefetchedSongId || isPrefetching) {
            return
        }
        
        // Only prefetch online songs that need stream URL extraction
        if (!nextSong.id.startsWith("online:") && !nextSong.id.startsWith("youtube:")) {
            return
        }
        
        isPrefetching = true
        prefetchJob?.cancel()
        
        prefetchJob = viewModelScope.launch {
            try {
                Log.d("PlayerViewModel", "🔄 Prefetching next song: ${nextSong.title}")
                
                val videoId = nextSong.id.removePrefix("online:").removePrefix("youtube:")
                
                // Fetch stream URL for next song (this will cache it in OnlineManager)
                val streamUrl = onlineManager.getStreamUrl(videoId)
                
                if (streamUrl != null) {
                    Log.d("PlayerViewModel", "✅ Prefetched and cached stream URL for: ${nextSong.title}")
                    prefetchedSongId = nextSong.id
                    
                    // Optionally: Pre-download first chunk (1MB) for instant playback
                    // This improves the user experience by reducing initial buffering
                    try {
                        withContext(Dispatchers.IO) {
                            val cacheDir = getApplication<android.app.Application>().cacheDir
                            val prefetchFile = File(cacheDir, "prefetch_${videoId}.cache")
                            
                            // Only prefetch if file doesn't already exist
                            if (!prefetchFile.exists()) {
                                val connection = java.net.URL(streamUrl).openConnection() as java.net.HttpURLConnection
                                connection.setRequestProperty("Range", "bytes=0-1048575") // First 1MB
                                connection.connectTimeout = 10000
                                connection.readTimeout = 10000
                                connection.connect()
                                
                                if (connection.responseCode in 200..299 || connection.responseCode == 206) {
                                    prefetchFile.parentFile?.mkdirs()
                                    val inputStream = connection.inputStream
                                    val outputStream = java.io.FileOutputStream(prefetchFile)
                                    
                                    val buffer = ByteArray(8192)
                                    var bytesRead: Int
                                    var totalBytes = 0
                                    
                                    // Limit to 1MB max
                                    while (inputStream.read(buffer).also { bytesRead = it } != -1 && totalBytes < 1048576) {
                                        outputStream.write(buffer, 0, bytesRead)
                                        totalBytes += bytesRead
                                    }
                                    
                                    inputStream.close()
                                    outputStream.close()
                                    connection.disconnect()
                                    
                                    Log.d("PlayerViewModel", "✅ Pre-cached ${totalBytes / 1024}KB for instant playback: ${nextSong.title}")
                                }
                            }
                        }
                    } catch (e: Exception) {
                        Log.w("PlayerViewModel", "⚠️ Failed to pre-cache chunk: ${e.message}")
                        // Not critical, continue without pre-caching
                    }
                } else {
                    Log.w("PlayerViewModel", "⚠️ Failed to prefetch stream URL for: ${nextSong.title}")
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "❌ Error prefetching next song: ${e.message}")
            } finally {
                isPrefetching = false
            }
        }
    }

    fun next() {
        viewModelScope.launch {
            playbackCollector.stopCollecting(skipped = true)

            // Reset prefetch state when moving to next song
            prefetchedSongId = null
            prefetchJob?.cancel()

            val currentSong = _uiState.value.currentSong
            
            // For online songs with recommendations, play from recommendation list
            if (currentSong != null && 
                (currentSong.id.startsWith("online:") || currentSong.id.startsWith("youtube:")) && 
                _uiState.value.upNextRecommendations.isNotEmpty()) {
                
                val nextRecommendation = _uiState.value.upNextRecommendations.first()
                
                // Check if next song is preloaded for instant playback
                val isPreloaded = preloadManager.isPreloaded(nextRecommendation)
                Log.d("PlayerViewModel", "⏭️ Playing next recommendation: ${nextRecommendation.title} (preloaded: $isPreloaded)")
                
                // Remove the played recommendation from the list
                val remainingRecommendations = _uiState.value.upNextRecommendations.drop(1)
                _uiState.value = _uiState.value.copy(
                    upNextRecommendations = remainingRecommendations
                )
                
                // Play the recommended song (will use cache if preloaded)
                playRecommendedSong(nextRecommendation)
                return@launch
            }

            // Use queue manager to move to next song (with auto-refill) for offline songs
            val nextSong = queueManager.moveToNext(autoRefill = true)

            if (nextSong != null) {
                player.prepare(nextSong.filePath, nextSong.id)
                player.play()
                playbackCollector.startCollecting(nextSong.id)

                _uiState.value = _uiState.value.copy(
                    currentSong = nextSong,
                    isPlaying = true
                )

                // Log song details
                logSongDetails(nextSong)

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

            val currentSong = _uiState.value.currentSong
            
            // For online songs with played history, go back to previous recommendation
            if (currentSong != null && 
                (currentSong.id.startsWith("online:") || currentSong.id.startsWith("youtube:")) && 
                playedRecommendationsHistory.isNotEmpty()) {
                
                val previousRecommendation = playedRecommendationsHistory.last()
                Log.d("PlayerViewModel", "⏮️ Playing previous recommendation: ${previousRecommendation.title}")
                
                // Remove from history
                playedRecommendationsHistory.removeAt(playedRecommendationsHistory.size - 1)
                
                // Add current song back to the front of recommendations list
                val updatedRecommendations = listOf(currentSong) + _uiState.value.upNextRecommendations
                _uiState.value = _uiState.value.copy(
                    upNextRecommendations = updatedRecommendations
                )
                
                // Check if previous song is preloaded for instant playback
                val videoId = previousRecommendation.id.removePrefix("online:").removePrefix("youtube:")
                val cachedStreamUrl = preloadManager.getCachedStreamUrl(previousRecommendation)
                val isPreloaded = preloadManager.isPreloaded(previousRecommendation)
                
                if (isPreloaded && cachedStreamUrl != null) {
                    Log.d("PlayerViewModel", "🚀 Using preloaded cache for previous song: ${previousRecommendation.title}")
                    playWithCachedStream(previousRecommendation, videoId, cachedStreamUrl)
                } else {
                    // Fall back to regular playback
                    val youtubeUrl = previousRecommendation.filePath
                    playUrl(url = youtubeUrl, title = previousRecommendation.title, 
                           artist = previousRecommendation.artist, durationMs = previousRecommendation.duration, 
                           thumbnailUrl = previousRecommendation.albumArtUri, skipRecommendationFetch = true)
                }
                return@launch
            }

            // Use queue manager to move to previous song for offline songs
            val previousSong = queueManager.moveToPrevious()

            if (previousSong != null) {
                player.prepare(previousSong.filePath, previousSong.id)
                player.play()
                playbackCollector.startCollecting(previousSong.id)

                _uiState.value = _uiState.value.copy(
                    currentSong = previousSong,
                    isPlaying = true
                )

                // Log song details
                logSongDetails(previousSong)

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
                    // For online songs with recommendations, auto-play the first recommendation
                    if (currentSong.id.startsWith("online:") && _uiState.value.upNextRecommendations.isNotEmpty()) {
                        val nextRecommendation = _uiState.value.upNextRecommendations.first()
                        Log.d("PlayerViewModel", "🎵 Auto-playing next recommendation: ${nextRecommendation.title}")
                        
                        // Remove the played recommendation from the list
                        val remainingRecommendations = _uiState.value.upNextRecommendations.drop(1)
                        _uiState.value = _uiState.value.copy(
                            upNextRecommendations = remainingRecommendations
                        )
                        
                        // Play the recommended song
                        playRecommendedSong(nextRecommendation)
                    } else {
                        // Auto-play next song from queue with queue refill enabled
                        val nextSong = queueManager.moveToNext(autoRefill = true)

                        if (nextSong != null) {
                            player.prepare(nextSong.filePath, nextSong.id)
                            player.play()
                            playbackCollector.startCollecting(nextSong.id)

                            _uiState.value = _uiState.value.copy(
                                currentSong = nextSong,
                                isPlaying = true
                            )

                            // Log song details
                            logSongDetails(nextSong)

                            updateNotification()
                            savePlaylistState()
                        } else {
                            _uiState.value = _uiState.value.copy(isPlaying = false)
                        }
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
                PlaybackEventBus.emit(
                    PlaybackEvent.QueueUpdated(
                        upcomingQueue = queueManager.getUpcomingQueue(),
                        playHistory = queueManager.getPlayHistory()
                    )
                )
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
                PlaybackEventBus.emit(
                    PlaybackEvent.QueueUpdated(
                        upcomingQueue = queueManager.getUpcomingQueue(),
                        playHistory = queueManager.getPlayHistory()
                    )
                )
            }
        }
    }

    fun placeNext(song: Song) {
        viewModelScope.launch {
            queueManager.placeNext(song)
            savePlaylistState()
            PlaybackEventBus.emit(PlaybackEvent.SongPlacedNext(song))
            PlaybackEventBus.emit(
                PlaybackEvent.QueueUpdated(
                    upcomingQueue = queueManager.getUpcomingQueue(),
                    playHistory = queueManager.getPlayHistory()
                )
            )
        }
    }

    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            queueManager.reorderQueue(fromIndex, toIndex)
            savePlaylistState()
            PlaybackEventBus.emit(PlaybackEvent.QueueReordered(fromIndex, toIndex))
            PlaybackEventBus.emit(
                PlaybackEvent.QueueUpdated(
                    upcomingQueue = queueManager.getUpcomingQueue(),
                    playHistory = queueManager.getPlayHistory()
                )
            )
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

                // Log song details
                logSongDetails(selectedSong)

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
                // Use public Download directory
                val musicDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "SyncTax"
                )
                val safeTitle = song.title.replace(Regex("[^a-zA-Z0-9\\s-]"), "").trim()
                val safeArtist =
                    song.artist?.replace(Regex("[^a-zA-Z0-9\\s-]"), "")?.trim() ?: "Unknown"
                
                // Check for WebM (primary) or Opus (fallback)
                val webmFile = File(musicDir, "$safeArtist - $safeTitle.webm")
                val opusFile = File(musicDir, "$safeTitle - $safeArtist.opus")
                val audioFile = when {
                    webmFile.exists() -> webmFile
                    opusFile.exists() -> opusFile
                    else -> null
                }

                if (audioFile != null && audioFile.exists() && !_uiState.value.downloadedSongs.contains(song.id)) {
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
            // Use public Download directory
            val musicDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                "SyncTax"
            )

            val downloadedSongIds = mutableSetOf<String>()

            onlineSongs.forEach { song ->
                try {
                    val safeTitle = song.title.replace(Regex("[^a-zA-Z0-9\\s-]"), "").trim()
                    val safeArtist =
                        song.artist?.replace(Regex("[^a-zA-Z0-9\\s-]"), "")?.trim() ?: "Unknown"
                    
                    // Check for WebM (primary from NewPipe) or Opus (legacy)
                    val webmFile = File(musicDir, "$safeArtist - $safeTitle.webm")
                    val opusFile = File(musicDir, "$safeTitle - $safeArtist.opus")

                    if (webmFile.exists() || opusFile.exists()) {
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

    fun startDownloadProcess() {
        val currentSong = _uiState.value.currentSong ?: run {
            Log.w("PlayerViewModel", "📥 Download: No current song")
            return
        }

        Log.d("PlayerViewModel", "📥 Download: Starting download process for '${currentSong.title}'")

        // Only allow downloading online songs
        if (!currentSong.id.startsWith("online:")) {
            Log.w("PlayerViewModel", "📥 Download: Not an online song (id: ${currentSong.id})")
            return
        }

        // Check if already downloaded
        if (_uiState.value.downloadedSongs.contains(currentSong.id)) {
            Log.i("PlayerViewModel", "📥 Download: Song already downloaded")
            _uiState.value = _uiState.value.copy(
                downloadMessage = "Song already downloaded"
            )
            return
        }

        // Check if already downloading
        if (_uiState.value.downloadingSongs.contains(currentSong.id)) {
            Log.i("PlayerViewModel", "📥 Download: Song is already being downloaded")
            return
        }

        // Extract URL from song ID (format: "online:${videoId}")
        val videoId = currentSong.id.substringAfter("online:")
        val url = "https://www.youtube.com/watch?v=$videoId"

        Log.d("PlayerViewModel", "📥 Download: Video ID: $videoId")
        Log.d("PlayerViewModel", "📥 Download: URL: $url")

        viewModelScope.launch {
            try {
                Log.d("PlayerViewModel", "📥 Download: Fetching available formats...")

                // Show loading indicator
                _uiState.value = _uiState.value.copy(isLoadingFormats = true)

                // Get available formats using ChaquopyAudioDownloader (includes client fallback)
                val videoInfo = chaquopyDownloader.getVideoInfo(url, null)

                Log.d(
                    "PlayerViewModel",
                    "📥 Download: Video info - Success: ${videoInfo.success}, Message: ${videoInfo.message}"
                )
                Log.d("PlayerViewModel", "📥 Download: Found ${videoInfo.formats.size} formats")

                // Hide loading indicator
                _uiState.value = _uiState.value.copy(isLoadingFormats = false)

                if (videoInfo.success && videoInfo.formats.isNotEmpty()) {
                    // Use FormatUtil to sort and filter audio formats (YTDLNIS-style)
                    val formatUtil = FormatUtil(getApplication())
                    val audioFormats = formatUtil.sortAudioFormats(videoInfo.formats)

                    Log.d(
                        "PlayerViewModel",
                        "📥 Download: Sorted ${audioFormats.size} audio formats"
                    )

                    if (audioFormats.isNotEmpty()) {
                        // AUTO-SELECT BEST FORMAT: Prefer M4A for metadata embedding support
                        // M4A (AAC) supports metadata embedding via Mutagen without FFmpeg
                        val bestFormat = audioFormats.find { 
                            it.container.equals("m4a", ignoreCase = true) ||
                            it.acodec.contains("aac", ignoreCase = true) ||
                            it.acodec.contains("mp4a", ignoreCase = true)
                        } ?: audioFormats.first() // Fall back to best available
                        
                        Log.d("PlayerViewModel", "📥 Download: Auto-selected format: ${bestFormat.format_id} (${bestFormat.container})")
                        
                        // Download directly with best format (skip format selection dialog)
                        downloadWithFormat(bestFormat)
                    } else {
                        // No audio formats available - use direct download with ChaquopyAudioDownloader
                        Log.w(
                            "PlayerViewModel",
                            "📥 Download: No audio formats available, using direct download"
                        )
                        downloadDirectly(currentSong, url)
                    }
                } else {
                    // Video info failed or no formats - use direct download with ChaquopyAudioDownloader
                    Log.w(
                        "PlayerViewModel",
                        "📥 Download: Video info failed or no formats, using direct download"
                    )
                    downloadDirectly(currentSong, url)
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "📥 Download: Error getting formats: ${e.message}", e)
                Log.e("PlayerViewModel", "📥 Download: Stack trace:", e)

                // Hide loading indicator
                _uiState.value = _uiState.value.copy(isLoadingFormats = false)

                // Fall back to direct download if format extraction fails
                Log.d("PlayerViewModel", "📥 Download: Falling back to direct download")
                downloadDirectly(currentSong, url)
            }
        }
    }

    /**
     * Downloads a song directly without format selection using ChaquopyAudioDownloader.
     * This method embeds metadata and album art automatically.
     */
    private fun downloadDirectly(song: Song, url: String) {
        viewModelScope.launch {
            try {
                Log.d("PlayerViewModel", "📥 Direct Download: Starting for '${song.title}'")

                // Mark as downloading
                _uiState.value = _uiState.value.copy(
                    downloadingSongs = _uiState.value.downloadingSongs + song.id,
                    downloadProgress = _uiState.value.downloadProgress + (song.id to 0.0f),
                    downloadMessage = "Downloading ${song.title}..."
                )

                // Use ChaquopyAudioDownloader which handles metadata embedding
                // Use public Download directory for user access
                val downloadDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "SyncTax"
                ).apply {
                    mkdirs()
                }

                Log.d(
                    "PlayerViewModel",
                    "📥 Direct Download: Download directory: ${downloadDir.absolutePath}"
                )
                Log.d("PlayerViewModel", "📥 Direct Download: Calling ChaquopyAudioDownloader...")

                val result = chaquopyDownloader.downloadAudio(
                    url,
                    downloadDir.absolutePath,
                    null,
                    null
                )

                Log.d(
                    "PlayerViewModel",
                    "📥 Direct Download: Result - Success: ${result.success}, Message: ${result.message}"
                )

                if (result.success && result.filePath.isNotEmpty()) {
                    val downloadedFile = File(result.filePath)

                    if (downloadedFile.exists()) {
                        Log.d(
                            "PlayerViewModel",
                            "📥 Direct Download: File exists at: ${downloadedFile.absolutePath}"
                        )
                        Log.d(
                            "PlayerViewModel",
                            "📥 Direct Download: File size: ${downloadedFile.length() / 1024 / 1024}MB"
                        )

                        // Create downloaded song entry
                        val downloadedSong = Song(
                            id = downloadedFile.absolutePath,
                            title = result.title.ifEmpty { song.title },
                            artist = result.artist.ifEmpty { song.artist },
                            album = song.album,
                            duration = if (result.duration > 0) result.duration.toLong() else song.duration,
                            filePath = downloadedFile.absolutePath,
                            genre = song.genre,
                            releaseYear = song.releaseYear,
                            albumArtUri = result.thumbnailUrl.ifEmpty { song.albumArtUri }
                        )

                        // Insert into database if not already present
                        val existingSong = repository.getSongById(downloadedFile.absolutePath)
                        if (existingSong == null) {
                            repository.insertSong(downloadedSong)
                            Log.d(
                                "PlayerViewModel",
                                "📥 Direct Download: Song inserted into database"
                            )
                        } else {
                            Log.d(
                                "PlayerViewModel",
                                "📥 Direct Download: Song already exists in database"
                            )
                        }

                        // Update UI state
                        _uiState.value = _uiState.value.copy(
                            downloadingSongs = _uiState.value.downloadingSongs - song.id,
                            downloadedSongs = _uiState.value.downloadedSongs + song.id,
                            downloadProgress = _uiState.value.downloadProgress - song.id,
                            downloadMessage = "Downloaded to: ${downloadDir.absolutePath}"
                        )

                        Log.d(
                            "PlayerViewModel",
                            "📥 Direct Download: ✅ Download completed successfully"
                        )
                    } else {
                        Log.e(
                            "PlayerViewModel",
                            "📥 Direct Download: File does not exist: ${downloadedFile.absolutePath}"
                        )
                        throw Exception("Downloaded file not found")
                    }
                } else {
                    Log.e(
                        "PlayerViewModel",
                        "📥 Direct Download: Download failed - ${result.message}"
                    )
                    throw Exception(result.message)
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "📥 Direct Download: ❌ Error: ${e.message}", e)

                _uiState.value = _uiState.value.copy(
                    downloadingSongs = _uiState.value.downloadingSongs - song.id,
                    downloadProgress = _uiState.value.downloadProgress - song.id,
                    downloadMessage = "Download failed: ${e.message}"
                )
            }
        }
    }

    fun downloadWithFormat(format: Format) {
        val currentSong = _uiState.value.currentSong ?: run {
            Log.w("PlayerViewModel", "📥 Format Download: No current song")
            return
        }

//        Log.d(
//            "PlayerViewModel",
//            "📥 Format Download: Starting download with format: ${format.quality} (${format.bitrate})"
//        )
        Log.d("PlayerViewModel", "📥 Format Download: Format ID: ${format.format_id}")

        // Hide dialog
        _uiState.value = _uiState.value.copy(
            showFormatDialog = false,
            availableFormats = emptyList()
        )

        // Extract URL from song ID
        val videoId = currentSong.id.substringAfter("online:")
        val url = "https://www.youtube.com/watch?v=$videoId"

        Log.d("PlayerViewModel", "📥 Format Download: Video ID: $videoId")
        Log.d("PlayerViewModel", "📥 Format Download: URL: $url")

        // Cancel existing download for this song if any
        activeDownloadJobs[currentSong.id]?.cancel()

        val downloadJob = viewModelScope.launch {
            try {
                // Fetch detailed metadata from yt-dlp first
                Log.d("PlayerViewModel", "📥 Format Download: Fetching metadata from yt-dlp...")
                val videoInfo = try {
                    chaquopyDownloader.getVideoInfo(url, null)
                } catch (e: Exception) {
                    Log.w("PlayerViewModel", "📥 Format Download: yt-dlp metadata fetch failed: ${e.message}")
                    null
                }
                
                val metadataTitle = videoInfo?.title?.takeIf { it.isNotEmpty() } ?: currentSong.title
                val metadataArtist = videoInfo?.artist?.takeIf { it.isNotEmpty() } ?: currentSong.artist
                val metadataAlbum = videoInfo?.album?.takeIf { it.isNotEmpty() } ?: "YouTube Audio"
                
                Log.d("PlayerViewModel", "📥 Format Download: Metadata from yt-dlp:")
                Log.d("PlayerViewModel", "📥 Format Download: - Title: $metadataTitle")
                Log.d("PlayerViewModel", "📥 Format Download: - Artist: $metadataArtist")
                Log.d("PlayerViewModel", "📥 Format Download: - Album: $metadataAlbum")

                Log.d("PlayerViewModel", "📥 Format Download: Marking song as downloading...")
                // Mark as downloading
                _uiState.value = _uiState.value.copy(
                    downloadingSongs = _uiState.value.downloadingSongs + currentSong.id,
                    downloadProgress = _uiState.value.downloadProgress + (currentSong.id to 0.0f),
                    downloadMessage = "Downloading $metadataTitle (${format.format_note})..."
                )

                Log.d("PlayerViewModel", "📥 Format Download: Using NewPipe direct download (primary method)...")

                // Use public Download directory for user access
                val downloadDir = File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "SyncTax"
                ).apply {
                    mkdirs()
                }

                Log.d(
                    "PlayerViewModel",
                    "📥 Format Download: Download directory: ${downloadDir.absolutePath}"
                )

                // PRIMARY: Use NewPipe for direct download (reliable, no bot detection issues)
                val newPipeResult = try {
                    com.just_for_fun.synctax.core.download.NewPipeAudioDownloader.downloadAudio(
                        context = getApplication(), // Pass application context
                        videoId = videoId,
                        outputDir = downloadDir,
                        preferredFormat = null, // Let NewPipe choose best format
                        metadataTitle = metadataTitle,
                        metadataArtist = metadataArtist,
                        metadataAlbum = metadataAlbum,
                        progressCallback = { pct ->
                            // Dispatch UI updates to Main thread for safety and coalescing
                            try {
                                viewModelScope.launch(Dispatchers.Main) {
                                    _uiState.value = _uiState.value.copy(
                                        downloadProgress = _uiState.value.downloadProgress + (currentSong.id to (pct / 100.0f))
                                    )
                                }
                            } catch (_: Exception) {}
                        },
                        thumbnailProgressCallback = { tPct ->
                            // Optional: log thumbnail progress (could be used to show a separate UI)
                            Log.d("PlayerViewModel", "📷 Thumbnail download progress: $tPct% for ${currentSong.id}")
                        },
                        progressStep = 2 // reduce update frequency to 2% steps
                    )
                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "📥 Format Download: NewPipe failed", e)
                    com.just_for_fun.synctax.core.download.NewPipeAudioDownloader.DownloadResult(
                        success = false,
                        message = "NewPipe error: ${e.message}"
                    )
                }

                Log.d(
                    "PlayerViewModel",
                    "📥 Format Download: NewPipe result - Success: ${newPipeResult.success}, Message: ${newPipeResult.message}"
                )

                // FALLBACK: If NewPipe fails, try yt-dlp (commented out by default)
                val (finalSuccess, finalFilePath, finalMessage) = if (!newPipeResult.success) {
                    Log.d("PlayerViewModel", "📥 Format Download: NewPipe failed, trying yt-dlp fallback...")
                    try {
                        val result = chaquopyDownloader.downloadAudio(
                            url,
                            downloadDir.absolutePath,
                            format.format_id,
                            null
                        )
                        
                        if (result.success) {
                            Log.d("PlayerViewModel", "📥 Format Download: ✅ yt-dlp fallback successful!")
                            Triple(true, result.filePath, result.message)
                        } else {
                            Triple(newPipeResult.success, newPipeResult.filePath ?: "", newPipeResult.message)
                        }
                    } catch (e: Exception) {
                        Log.e("PlayerViewModel", "📥 Format Download: yt-dlp also failed", e)
                        Triple(newPipeResult.success, newPipeResult.filePath ?: "", newPipeResult.message)
                    }
                } else {
                    Log.d("PlayerViewModel", "📥 Format Download: ✅ NewPipe download successful!")
                    Triple(newPipeResult.success, newPipeResult.filePath ?: "", newPipeResult.message)
                }

                if (finalSuccess && finalFilePath.isNotEmpty()) {
                    val downloadedFile = File(finalFilePath)

                    if (downloadedFile.exists()) {
                        Log.d(
                            "PlayerViewModel",
                            "📥 Format Download: File exists at: ${downloadedFile.absolutePath}"
                        )
                        Log.d(
                            "PlayerViewModel",
                            "📥 Format Download: File size: ${downloadedFile.length() / 1024 / 1024}MB"
                        )

                        // Create downloaded song entry using the EXACT metadata embedded in the audio file
                        // This ensures database metadata matches what's physically in the file
                        val embeddedTitle = newPipeResult.title.takeIf { it.isNotEmpty() } ?: currentSong.title
                        val embeddedArtist = newPipeResult.artist.takeIf { it.isNotEmpty() } ?: currentSong.artist
                        val embeddedAlbum = newPipeResult.album.takeIf { it.isNotEmpty() } ?: "YouTube Audio"
                        
                        Log.d("PlayerViewModel", "📥 Format Download: Using embedded metadata from file - Title: $embeddedTitle, Artist: $embeddedArtist, Album: $embeddedAlbum")
                        
                        val downloadedSong = Song(
                            id = downloadedFile.absolutePath,
                            title = embeddedTitle,
                            artist = embeddedArtist,
                            album = embeddedAlbum,
                            duration = currentSong.duration, // Use original duration, will be updated on playback
                            filePath = downloadedFile.absolutePath,
                            genre = currentSong.genre,
                            releaseYear = currentSong.releaseYear,
                            albumArtUri = newPipeResult.thumbnailUrl.takeIf { it.isNotEmpty() } ?: currentSong.albumArtUri
                        )

                        // Insert into database if not already present
                        val existingSong = repository.getSongById(downloadedFile.absolutePath)
                        if (existingSong == null) {
                            repository.insertSong(downloadedSong)
                            Log.d(
                                "PlayerViewModel",
                                "📥 Format Download: Song inserted into database"
                            )
                        } else {
                            Log.d(
                                "PlayerViewModel",
                                "📥 Format Download: Song already exists in database"
                            )
                        }

                        _uiState.value = _uiState.value.copy(
                            downloadedSongs = _uiState.value.downloadedSongs + currentSong.id,
                            downloadingSongs = _uiState.value.downloadingSongs - currentSong.id,
                            downloadProgress = _uiState.value.downloadProgress - currentSong.id,
                            downloadMessage = "Downloaded to: ${downloadDir.absolutePath}"
                        )

                        Log.d(
                            "PlayerViewModel",
                            "📥 Format Download: ✅ Download completed successfully"
                        )
                    } else {
                        Log.e(
                            "PlayerViewModel",
                            "📥 Format Download: File does not exist: ${downloadedFile.absolutePath}"
                        )
                        throw Exception("Downloaded file not found")
                    }
                } else {
                    Log.e(
                        "PlayerViewModel",
                        "📥 Format Download: Download failed - $finalMessage"
                    )
                    throw Exception(finalMessage)
                }

            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) {
                    Log.w("PlayerViewModel", "📥 Format Download: Download cancelled by user")
                    _uiState.value = _uiState.value.copy(
                        downloadingSongs = _uiState.value.downloadingSongs - currentSong.id,
                        downloadProgress = _uiState.value.downloadProgress - currentSong.id,
                        downloadMessage = "Download cancelled"
                    )
                } else {
                    Log.e("PlayerViewModel", "📥 Format Download: ❌ Error: ${e.message}", e)
                    Log.e("PlayerViewModel", "📥 Format Download: Stack trace:", e)
                    _uiState.value = _uiState.value.copy(
                        downloadingSongs = _uiState.value.downloadingSongs - currentSong.id,
                        downloadProgress = _uiState.value.downloadProgress - currentSong.id,
                        downloadMessage = "Download failed: ${e.message}"
                    )
                }
            } finally {
                // Clean up job reference
                activeDownloadJobs.remove(currentSong.id)
            }
        }
        
        // Store the job for potential cancellation
        activeDownloadJobs[currentSong.id] = downloadJob
    }

    /**
     * Cancel an active download for a specific song
     */
    fun cancelDownload(songId: String) {
        activeDownloadJobs[songId]?.let { job ->
            Log.d("PlayerViewModel", "🛑 Cancelling download for song: $songId")
            job.cancel()
            activeDownloadJobs.remove(songId)
            
            // Update UI state
            _uiState.value = _uiState.value.copy(
                downloadingSongs = _uiState.value.downloadingSongs - songId,
                downloadProgress = _uiState.value.downloadProgress - songId,
                downloadMessage = "Download cancelled"
            )
        }
    }

    /**
     * Check if a song is currently being downloaded
     */
    fun isDownloading(songId: String): Boolean {
        return _uiState.value.downloadingSongs.contains(songId)
    }

    fun selectFormat(format: Format) {
        _uiState.value = _uiState.value.copy(selectedFormat = format)
    }

    fun dismissFormatDialog() {
        _uiState.value = _uiState.value.copy(
            showFormatDialog = false,
            availableFormats = emptyList()
        )
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
        preloadManager.release()
        player.release()
    }

    /**
     * Trigger preloading of adjacent songs for instant switching.
     * Called when a new song starts playing.
     */
    private fun triggerPreloading(currentSong: Song) {
        viewModelScope.launch {
            delay(1500) // Wait 1.5 seconds after current song starts to avoid competing for bandwidth
            
            val nextSongs = _uiState.value.upNextRecommendations.take(3)
            val previousSong = if (playedRecommendationsHistory.isNotEmpty()) {
                playedRecommendationsHistory.lastOrNull()
            } else null
            
            Log.d("PlayerViewModel", "🔄 Triggering preload: ${nextSongs.size} next songs, previous=${previousSong?.title}")
            preloadManager.preloadAround(currentSong, nextSongs, previousSong)
        }
    }

    fun setCurrentLyrics(lyrics: String?) {
        currentLyrics = lyrics
    }
}

data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLoadingSong: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatEnabled: Boolean = false,
    val downloadPercent: Int = 0,
    val volume: Float = 1.0f,
    val downloadedSongs: Set<String> = emptySet(),
    val downloadingSongs: Set<String> = emptySet(),
    val downloadProgress: Map<String, Float> = emptyMap(), // songId to progress (0.0 to 1.0)
    val downloadMessage: String? = null, // Download location message for snackbar
    val showFormatDialog: Boolean = false,
    val availableFormats: List<Format> = emptyList(),  // Changed from AudioFormat to Format
    val isLoadingFormats: Boolean = false, // Loading indicator for format selection
    val selectedFormat: Format? = null,  // Currently selected format for download
    val upNextRecommendations: List<Song> = emptyList(), // YouTube-recommended songs for online player
    val isLoadingRecommendations: Boolean = false // Loading indicator for recommendations
)