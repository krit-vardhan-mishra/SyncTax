package com.just_for_fun.synctax.core.player

import com.just_for_fun.synctax.data.repository.MusicRepository
import com.just_for_fun.synctax.data.repository.OnlineSongRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * Collects playback data for ML training and history tracking
 */
class PlaybackCollector(
    private val repository: MusicRepository,
    private val player: MusicPlayer,
    private val scope: CoroutineScope, // Accept scope from caller (ViewModel)
    private val onPlaybackRecorded: (() -> Unit)? = null,
    private val onlineSongRepository: OnlineSongRepository? = null,
    private val onOnlineSongPlayed: ((String) -> Unit)? = null, // Called when online song played 5+ seconds
    private val onOnlineSongFullyPlayed: ((String) -> Unit)? = null // Called when online song fully played
) {

    private var collectJob: Job? = null
    private var playStartTime = 0L
    private var currentSongId: String? = null
    private var hasMarkedAsPlayed = false // Track if we've already marked this song as played
    
    // Store current song metadata for marking as played
    private var currentSongTitle: String? = null
    private var currentSongArtist: String? = null
    private var currentSongThumbnail: String? = null
    private var currentSongDuration: Int? = null

    fun startCollecting(songId: String, title: String? = null, artist: String? = null, thumbnailUrl: String? = null, duration: Long? = null) {
        currentSongId = songId
        playStartTime = System.currentTimeMillis()
        hasMarkedAsPlayed = false
        
        // Store metadata for later use
        currentSongTitle = title
        currentSongArtist = artist
        currentSongThumbnail = thumbnailUrl
        currentSongDuration = duration?.div(1000)?.toInt()

        collectJob?.cancel()
        // Use provided scope for lifecycle management
        collectJob = scope.launch {
            // Monitor for 5 second play threshold for online songs
            if (songId.startsWith("online:") || songId.startsWith("youtube:")) {
                launch {
                    delay(5000) // Wait 5 seconds
                    if (!hasMarkedAsPlayed && currentSongId == songId) {
                        hasMarkedAsPlayed = true
                        val videoId = songId.removePrefix("online:").removePrefix("youtube:")
                        // Pass metadata to ensure song is created if not exists
                        onlineSongRepository?.markAsPlayed(
                            videoId = videoId,
                            title = currentSongTitle,
                            artist = currentSongArtist,
                            thumbnailUrl = currentSongThumbnail,
                            duration = currentSongDuration
                        )
                        onOnlineSongPlayed?.invoke(videoId)
                    }
                }
            }
            
            player.playerState.collect { state ->
                // Observe player state changes safely
                if (!state.isPlaying) {
                    // Handle pause or stop
                }
            }
        }
    }
    
    /**
     * Backward-compatible version without metadata
     */
    fun startCollecting(songId: String) {
        startCollecting(songId, null, null, null, null)
    }

    suspend fun stopCollecting(skipped: Boolean = false) {
        collectJob?.cancel()

        val songId = currentSongId ?: return
        val playEndTime = System.currentTimeMillis()
        val listenDuration = playEndTime - playStartTime
        
        // Get duration and position from StateFlow (thread-safe)
        val totalDuration = player.playerState.value.duration
        val currentPosition = player.getCurrentPosition()

        val completionRate = if (totalDuration > 0) {
            (currentPosition.toFloat() / totalDuration).coerceIn(0f, 1f)
        } else 0f

        // Only record if played for at least 3 seconds
        if (listenDuration >= 3000) {
            repository.recordPlay(
                songId = songId,
                listenDuration = listenDuration,
                completionRate = completionRate,
                skipped = skipped
            )
            // Notify that playback was recorded (to invalidate cache)
            onPlaybackRecorded?.invoke()
        }
        
        // Check if online song was fully played (80%+ completion)
        // Note: Actual auto-save is now handled in real-time by PlayerViewModel at 80% mark
        // This is kept just to trigger the "fully played" event for history/stats if needed
        if ((songId.startsWith("online:") || songId.startsWith("youtube:")) && completionRate >= 0.8f) {
            val videoId = songId.removePrefix("online:").removePrefix("youtube:")
            onlineSongRepository?.markAsFullyPlayed(videoId)
            // onOnlineSongFullyPlayed?.invoke(videoId) -- Removed to prevent double auto-save attempts
        }
        
        // Clear metadata
        currentSongTitle = null
        currentSongArtist = null
        currentSongThumbnail = null
        currentSongDuration = null
    }
}
