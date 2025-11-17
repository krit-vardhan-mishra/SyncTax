package com.just_for_fun.synctax.core.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
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