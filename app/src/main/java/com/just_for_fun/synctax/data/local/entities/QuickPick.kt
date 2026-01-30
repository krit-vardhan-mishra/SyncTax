package com.just_for_fun.synctax.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity to store Quick Pick seed songs.
 * These are songs that the user has played which are used to generate recommendations.
 * Separate seeds are maintained for offline and online modes.
 */
@Entity(
    tableName = "quick_picks",
    indices = [
        Index(value = ["songId"], unique = true),
        Index(value = ["source"]),
        Index(value = ["lastPlayedAt"])
    ]
)
data class QuickPick(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: String,           // Song ID (local) or videoId (online)
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val duration: Long = 0,       // Duration in milliseconds
    val source: String,           // "offline" or "online"
    val playCount: Int = 1,       // How many times this song was played
    val lastPlayedAt: Long = System.currentTimeMillis(),
    val addedAt: Long = System.currentTimeMillis(),
    val genre: String? = null,    // Genre for better recommendations (offline mode)
    val album: String? = null     // Album for better recommendations
)
