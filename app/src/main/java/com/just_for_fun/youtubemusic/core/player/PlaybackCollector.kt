package com.just_for_fun.synctax.core.player

import com.just_for_fun.synctax.core.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

/**
 * Collects playback data for ML training
 */
class PlaybackCollector(
    private val repository: MusicRepository,
    private val player: MusicPlayer
) {

    private var collectJob: Job? = null
    private var playStartTime = 0L
    private var currentSongId: String? = null

    fun startCollecting(songId: String) {
        currentSongId = songId
        playStartTime = System.currentTimeMillis()

        collectJob?.cancel()
        // Use Main dispatcher to safely observe player state
        collectJob = CoroutineScope(Dispatchers.Main).launch {
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
        }
    }
}