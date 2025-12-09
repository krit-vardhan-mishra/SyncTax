package com.just_for_fun.synctax.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to track user interactions with recommendations.
 * Used to improve recommendation quality over time.
 */
@Entity(tableName = "recommendation_interactions")
data class RecommendationInteraction(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val recommendationId: String,    // Cache key that generated this recommendation
    val songId: String,              // Video ID of recommended song
    val action: String,              // "played", "skipped", "liked", "disliked"
    val timestamp: Long = System.currentTimeMillis(),
    val source: String               // "artist_based", "discovery", etc.
)
