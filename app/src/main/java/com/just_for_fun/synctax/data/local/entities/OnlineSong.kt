package com.just_for_fun.synctax.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a song from YouTube/YouTube Music
 * This is separate from local Song entity as online songs have different properties
 */
@Entity(
    tableName = "online_songs",
    indices = [
        Index(value = ["videoId"], unique = true),
        Index(value = ["title"]),
        Index(value = ["artist"])
    ]
)
data class OnlineSong(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val videoId: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Int? = null, // duration in seconds
    val sourcePlatform: String = "YouTube",
    val addedAt: Long = System.currentTimeMillis()
)
