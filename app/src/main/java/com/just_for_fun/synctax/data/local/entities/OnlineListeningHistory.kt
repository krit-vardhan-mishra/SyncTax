package com.just_for_fun.synctax.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to store online listening history for Quick Picks and Recommendations.
 * Enhanced with fields for recommendation analytics.
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
    val timestamp: Long = System.currentTimeMillis(),
    
    // Enhanced fields for better recommendations
    val playDuration: Long = 0,        // How long user listened (seconds)
    val totalDuration: Long = 0,       // Song total duration (seconds)
    val completionRate: Float = 0f,    // playDuration/totalDuration
    val playCount: Int = 1,            // How many times played
    val skipCount: Int = 0,            // How many times skipped
    val genre: String? = null,         // Genre if available from metadata
    val source: String = "online"      // "online" or "local"
)
