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

    fun startCollecting(songId: String) {
        currentSongId = songId
        playStartTime = System.currentTimeMillis()
        hasMarkedAsPlayed = false

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
                        onlineSongRepository?.markAsPlayed(videoId)
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
        
        // Check if online song was fully played (90%+ completion)
        if ((songId.startsWith("online:") || songId.startsWith("youtube:")) && completionRate >= 0.9f) {
            val videoId = songId.removePrefix("online:").removePrefix("youtube:")
            onlineSongRepository?.markAsFullyPlayed(videoId)
            onOnlineSongFullyPlayed?.invoke(videoId)
        }
    }
}
