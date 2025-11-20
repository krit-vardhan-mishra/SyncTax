package com.just_for_fun.synctax.core.player

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Global event bus for playback events
 * Allows different parts of the app to observe playback changes in real-time
 */
object PlaybackEventBus {
    private val _events = MutableSharedFlow<PlaybackEvent>(
        replay = 0,
        extraBufferCapacity = 10
    )

    /**
     * Flow of playback events that can be collected by any component
     */
    val events: SharedFlow<PlaybackEvent> = _events.asSharedFlow()

    /**
     * Emit a playback event to all subscribers
     */
    suspend fun emit(event: PlaybackEvent) {
        _events.emit(event)
    }

    /**
     * Emit a playback event without suspending (for use in non-coroutine contexts)
     */
    fun tryEmit(event: PlaybackEvent): Boolean {
        return _events.tryEmit(event)
    }
}
