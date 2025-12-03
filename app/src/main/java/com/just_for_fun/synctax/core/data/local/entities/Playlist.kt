package com.just_for_fun.synctax.core.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Entity representing a playlist imported from YouTube/YouTube Music
 */
@Entity(
    tableName = "playlists",
    indices = [
        Index(value = ["playlistUrl"], unique = true),
        Index(value = ["createdAt"])
    ]
)
data class Playlist(
    @PrimaryKey(autoGenerate = true) val playlistId: Int = 0,
    val name: String,
    val description: String? = null,
    val platform: String = "YouTube",
    val playlistUrl: String,
    val thumbnailUrl: String? = null,
    val songCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
