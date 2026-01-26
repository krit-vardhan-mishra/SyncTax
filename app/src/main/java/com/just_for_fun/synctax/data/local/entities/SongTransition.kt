package com.just_for_fun.synctax.data.local.entities

import androidx.room.Entity
import androidx.room.Index

/**
 * Represents a transition (edge) between two songs in the user's listening graph.
 * This is used for Markov Chain-based sequential recommendations.
 * 
 * The weight represents the probability/strength of the transition:
 * - Increased when user plays song B after song A (completion)
 * - Decreased when user skips song B after song A
 * - Reinforced when user repeats the same transition
 * 
 * Mathematical formula for weight update:
 * - On completion: new_weight = old_weight + α (learning rate, e.g., 0.1)
 * - On skip: new_weight = old_weight * β (decay factor, e.g., 0.5)
 * - On repeat: new_weight = old_weight + 2 * α
 * 
 * Probability calculation:
 * P(A → B) = weight(A→B) / Σ(all weights from A)
 */
@Entity(
    tableName = "song_transitions",
    primaryKeys = ["fromSongId", "toSongId"],
    indices = [
        Index(value = ["fromSongId"]),
        Index(value = ["toSongId"]),
        Index(value = ["weight"])
    ]
)
data class SongTransition(
    val fromSongId: String,           // Source song ID (can be local or "online:videoId")
    val toSongId: String,             // Target song ID
    val weight: Float = 1.0f,         // Transition weight/probability strength
    val playCount: Int = 1,           // Number of times this transition occurred
    val skipCount: Int = 0,           // Number of times user skipped after this transition
    val lastOccurred: Long = System.currentTimeMillis(), // Timestamp of last occurrence
    val avgCompletionRate: Float = 1.0f  // Average completion rate when this transition happens
)
