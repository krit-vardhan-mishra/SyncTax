package com.just_for_fun.synctax.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.ForeignKey

@Entity(
    tableName = "listening_history"
    // Removed foreign key constraint to allow tracking online songs (online:videoId) 
    // that don't exist in the Songs table
)
data class ListeningHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val songId: String,  // Can be local song ID or "online:videoId" for streamed songs
    val playTimestamp: Long,
    val listenDuration: Long, // How long user listened in milliseconds
    val completionRate: Float, // 0.0 to 1.0
    val skipped: Boolean,
    val timeOfDay: Int, // Hour of day (0-23)
    val dayOfWeek: Int, // 1-7
    val repeatCount: Int = 0,
    val userRating: Int? = null // 1 = dislike (thumbs down), 2 = like (thumbs up), null = no rating
)
