package com.just_for_fun.synctax.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Junction table to associate songs with playlists
 * Supports many-to-many relationship between playlists and online songs
 */
@Entity(
    tableName = "playlist_songs",
    primaryKeys = ["playlistId", "onlineSongId"],
    foreignKeys = [
        ForeignKey(
            entity = Playlist::class,
            parentColumns = ["playlistId"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = OnlineSong::class,
            parentColumns = ["id"],
            childColumns = ["onlineSongId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["onlineSongId"])
    ]
)
data class PlaylistSong(
    val playlistId: Int,
    val onlineSongId: Int,
    val position: Int
)
