package com.just_for_fun.synctax.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity to store the generated Quick Pick song queue.
 * These are the recommended songs shown in the Quick Pick screen.
 * The queue is refreshed as the user plays songs.
 */
@Entity(
    tableName = "quick_pick_songs",
    indices = [
        Index(value = ["source"]),
        Index(value = ["queuePosition"]),
        Index(value = ["songId"])
    ]
)
data class QuickPickSong(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: String,           // Song ID (local) or videoId (online)
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val streamUrl: String? = null,// Stream URL for online songs (cached)
    val duration: Long = 0,       // Duration in milliseconds
    val source: String,           // "offline" or "online"
    val queuePosition: Int,       // Position in the queue (0, 1, 2 for next 3)
    val generatedAt: Long = System.currentTimeMillis(),
    val basedOnSongId: String? = null, // The seed song this recommendation came from
    val recommendationReason: String? = null, // Why this song was recommended
    val filePath: String? = null  // File path for offline songs
)
