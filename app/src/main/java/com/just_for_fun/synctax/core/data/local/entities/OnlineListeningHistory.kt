package com.just_for_fun.synctax.core.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to store the last 10 online listened songs
 * This will be displayed in the Quick Picks section on home screen
 */
@Entity(tableName = "online_listening_history")
data class OnlineListeningHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val watchUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)
