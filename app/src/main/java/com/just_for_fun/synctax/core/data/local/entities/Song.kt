package com.just_for_fun.synctax.core.data.local.entities

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["addedTimestamp"]),
        Index(value = ["title"]),
        Index(value = ["artist"]),
        Index(value = ["album"]),
        Index(value = ["genre"])
    ]
)
data class Song(
    @PrimaryKey val id: String,
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long,
    val filePath: String,
    val genre: String?,
    val releaseYear: Int?,
    val albumArtUri: String? = null, // URI to album art
    val addedTimestamp: Long = System.currentTimeMillis()
)