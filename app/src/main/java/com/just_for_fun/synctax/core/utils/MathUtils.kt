package com.just_for_fun.synctax.core.utils

import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Mathematical utilities for the recommendation system.
 * 
 * Formulas from RECOMMENDATION_LOGIC_NEW.md:
 * - Cosine similarity for vector comparison
 * - Time decay for relevance scoring
 * - Skip penalty calculation
 * - Confidence scoring with implicit feedback
 */
object MathUtils {
    /**
     * Calculate cosine similarity between two vectors.
     * Formula: cos(θ) = (A·B) / (|A||B|)
     */
    fun cosineSimilarity(a: DoubleArray, b: DoubleArray): Double {
        require(a.size == b.size) { "Vectors must have same dimensions" }

        val dotProduct = a.zip(b).sumOf { (x, y) -> x * y }
        val magnitudeA = sqrt(a.sumOf { it * it })
        val magnitudeB = sqrt(b.sumOf { it * it })

        return if (magnitudeA == 0.0 || magnitudeB == 0.0) 0.0
        else (dotProduct / (magnitudeA * magnitudeB)).coerceIn(0.0, 1.0)
    }

    /**
     * Calculate Euclidean distance between two vectors
     */
    fun euclideanDistance(a: DoubleArray, b: DoubleArray): Double {
        require(a.size == b.size) { "Vectors must have same dimensions" }
        return sqrt(a.zip(b).sumOf { (x, y) -> (x - y).pow(2) })
    }

    /**
     * Normalize vector to 0-1 range
     */
    fun normalizeVector(vector: DoubleArray): DoubleArray {
        val min = vector.minOrNull() ?: 0.0
        val max = vector.maxOrNull() ?: 1.0
        val range = max - min

        return if (range == 0.0) DoubleArray(vector.size) { 0.5 }
        else vector.map { (it - min) / range }.toDoubleArray()
    }

    /**
     * Calculate time decay factor (exponential decay).
     * Formula: relevance(t) = e^(-λ * days)
     * Default half-life: 23 days (λ ≈ 0.03)
     */
    fun timeDecay(timestamp: Long, currentTime: Long, halfLife: Long = 23 * 24 * 60 * 60 * 1000): Double {
        val elapsed = currentTime - timestamp
        return exp(-ln(2.0) * elapsed / halfLife)
    }

    /**
     * Sigmoid activation function
     */
    fun sigmoid(x: Double): Double = 1.0 / (1.0 + exp(-x))

    /**
     * Calculate weighted average
     */
    fun weightedAverage(values: List<Double>, weights: List<Double>): Double {
        require(values.size == weights.size) { "Values and weights must have same size" }
        val totalWeight = weights.sum()
        return if (totalWeight == 0.0) 0.0
        else values.zip(weights).sumOf { (v, w) -> v * w } / totalWeight
    }

    // ==================== NEW FORMULAS FROM RECOMMENDATION_LOGIC_NEW.md ====================

    /**
     * Calculate skip penalty using exponential decay.
     * Formula: penalty = e^(-listen_time / song_duration)
     * Shorter listens = bigger penalty
     */
    fun skipPenalty(listenDuration: Long, totalDuration: Long): Double {
        if (totalDuration <= 0) return 1.0
        val completionRate = listenDuration.toDouble() / totalDuration
        return exp(-completionRate)
    }

    /**
     * Calculate confidence score using implicit feedback.
     * Formula: confidence = α * completion_rate + β * (1 - skip_rate) + γ * normalized_play_count
     * 
     * @param completionRate Average completion rate (0-1)
     * @param skipRate Ratio of skips to total plays (0-1)
     * @param playCount Number of times played
     * @param alpha Weight for completion rate (default 0.5)
     * @param beta Weight for non-skip rate (default 0.3)
     * @param gamma Weight for play count (default 0.2)
     */
    fun confidenceScore(
        completionRate: Double,
        skipRate: Double,
        playCount: Int,
        alpha: Double = 0.5,
        beta: Double = 0.3,
        gamma: Double = 0.2
    ): Double {
        // Normalize play count (saturates around 100 plays)
        val normalizedPlays = minOf(playCount / 100.0, 1.0)
        
        return (alpha * completionRate + 
                beta * (1.0 - skipRate) + 
                gamma * normalizedPlays).coerceIn(0.0, 1.0)
    }

    /**
     * Calculate diversity penalty based on similarity to recent songs.
     * Higher penalty = song is too similar to recently played
     * 
     * Formula: penalty = Σ similarity(current, recent_i) / count
     */
    fun diversityPenalty(
        currentVector: DoubleArray,
        recentVectors: List<DoubleArray>,
        maxRecent: Int = 5
    ): Double {
        if (recentVectors.isEmpty()) return 0.0
        
        val recentToConsider = recentVectors.takeLast(maxRecent)
        val totalSimilarity = recentToConsider.sumOf { cosineSimilarity(currentVector, it) }
        
        return totalSimilarity / recentToConsider.size
    }

    /**
     * Calculate recommendation score combining multiple factors.
     * Formula: score = similarity + transition_weight - skip_penalty - recent_penalty + exploration_bonus
     */
    fun recommendationScore(
        similarity: Double,
        transitionWeight: Double,
        skipRate: Double,
        recentlyPlayed: Boolean,
        exploration: Boolean = false
    ): Double {
        var score = 0.5 // Base score
        
        score += similarity * 0.25
        score += transitionWeight * 0.35
        score -= skipRate * 0.5  // Skip penalty
        
        if (recentlyPlayed) {
            score -= 0.3  // Recent play penalty
        }
        
        if (exploration) {
            score += 0.1  // Exploration bonus
        }
        
        return score.coerceIn(0.0, 1.0)
    }

    /**
     * Softmax function for converting scores to probabilities.
     * Useful for weighted random selection.
     */
    fun softmax(scores: List<Double>, temperature: Double = 1.0): List<Double> {
        if (scores.isEmpty()) return emptyList()
        
        val maxScore = scores.maxOrNull() ?: 0.0
        val expScores = scores.map { exp((it - maxScore) / temperature) }
        val sumExp = expScores.sum()
        
        return if (sumExp == 0.0) {
            List(scores.size) { 1.0 / scores.size }
        } else {
            expScores.map { it / sumExp }
        }
    }
}
