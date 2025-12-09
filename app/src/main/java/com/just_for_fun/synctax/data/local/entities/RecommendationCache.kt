package com.just_for_fun.synctax.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entity to cache recommendation results with expiry time.
 * Prevents excessive API calls by storing recommendations for 24 hours.
 */
@Entity(tableName = "recommendations_cache")
data class RecommendationCache(
    @PrimaryKey
    val cacheKey: String,           // e.g., "artist_based", "discovery", "similar_songs"
    val recommendationsJson: String, // JSON string of Song list
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long              // Cache validity timestamp
)
