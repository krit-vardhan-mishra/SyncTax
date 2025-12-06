package com.just_for_fun.synctax.core.player

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Extension functions for optimized Flow handling in player state
 * Based on OuterTune's approach to preventing unnecessary recompositions
 */

/**
 * Create a flow that only emits when isPlaying changes
 * Prevents recomposition on every state update
 */
fun StateFlow<PlayerState>.isPlayingFlow(): Flow<Boolean> =
    map { it.isPlaying }.distinctUntilChanged()

/**
 * Create a flow that only emits when isBuffering changes
 */
fun StateFlow<PlayerState>.isBufferingFlow(): Flow<Boolean> =
    map { it.isBuffering }.distinctUntilChanged()

/**
 * Create a flow that only emits when currentSongId changes
 */
fun StateFlow<PlayerState>.currentSongIdFlow(): Flow<String?> =
    map { it.currentSongId }.distinctUntilChanged()

/**
 * Create a flow that only emits when duration changes
 */
fun StateFlow<PlayerState>.durationFlow(): Flow<Long> =
    map { it.duration }.distinctUntilChanged()

/**
 * Create a flow that only emits when isEnded changes
 */
fun StateFlow<PlayerState>.isEndedFlow(): Flow<Boolean> =
    map { it.isEnded }.distinctUntilChanged()

/**
 * Throttle position updates to reduce recompositions
 * Only emit when position changes by at least the threshold
 */
fun Flow<Long>.throttlePosition(thresholdMs: Long = 1000): Flow<Long> {
    var lastEmitted = 0L
    return map { position ->
        if (kotlin.math.abs(position - lastEmitted) >= thresholdMs) {
            lastEmitted = position
            position
        } else {
            lastEmitted
        }
    }.distinctUntilChanged()
}
