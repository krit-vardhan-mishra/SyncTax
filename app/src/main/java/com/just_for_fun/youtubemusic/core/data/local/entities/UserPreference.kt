package com.just_for_fun.synctax.core.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_preferences")
data class UserPreference(
    @PrimaryKey val songId: String,
    val playCount: Int = 0,
    val skipCount: Int = 0,
    val likeScore: Float = 0f, // Calculated preference score
    val lastPlayed: Long = 0L,
    val avgCompletionRate: Float = 0f,
    val preferredTimeSlots: String = "" // JSON array of preferred hours
)