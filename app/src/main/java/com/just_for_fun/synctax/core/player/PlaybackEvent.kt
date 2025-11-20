package com.just_for_fun.synctax.core.player

import com.just_for_fun.synctax.core.data.local.entities.Song

/**
 * Events emitted by the playback system for global synchronization
 * Use these events to keep UI components in sync with playback state
 */
sealed class PlaybackEvent {
    /**
     * Emitted when a new song starts playing
     */
    data class SongChanged(val song: Song?) : PlaybackEvent()

    /**
     * Emitted when playback state changes (play/pause)
     */
    data class PlaybackStateChanged(val isPlaying: Boolean) : PlaybackEvent()

    /**
     * Emitted when the queue is updated
     */
    data class QueueUpdated(
        val upcomingQueue: List<Song>,
        val playHistory: List<Song>
    ) : PlaybackEvent()

    /**
     * Emitted when a song is added to play next
     */
    data class SongPlacedNext(val song: Song) : PlaybackEvent()

    /**
     * Emitted when a song is removed from queue
     */
    data class SongRemovedFromQueue(val song: Song) : PlaybackEvent()

    /**
     * Emitted when queue is reordered
     */
    data class QueueReordered(val fromIndex: Int, val toIndex: Int) : PlaybackEvent()

    /**
     * Emitted when queue is shuffled
     */
    object QueueShuffled : PlaybackEvent()

    /**
     * Emitted when queue is refilled with recommendations
     */
    data class QueueRefilled(val newSongs: List<Song>) : PlaybackEvent()

    /**
     * Emitted when playback position changes significantly
     */
    data class PositionChanged(val position: Long, val duration: Long) : PlaybackEvent()

    /**
     * Emitted when volume changes
     */
    data class VolumeChanged(val volume: Float) : PlaybackEvent()
}
