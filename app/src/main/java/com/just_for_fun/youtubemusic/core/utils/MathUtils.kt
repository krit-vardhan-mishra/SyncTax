package com.just_for_fun.youtubemusic.core.utils

import kotlin.math.*

object MathUtils {
    /**
     * Calculate cosine similarity between two vectors
     */
    fun cosineSimilarity(a: DoubleArray, b: DoubleArray): Double {
        require(a.size == b.size) { "Vectors must have same dimensions" }

        val dotProduct = a.zip(b).sumOf { (x, y) -> x * y }
        val magnitudeA = sqrt(a.sumOf { it * it })
        val magnitudeB = sqrt(b.sumOf { it * it })

        return if (magnitudeA == 0.0 || magnitudeB == 0.0) 0.0
        else dotProduct / (magnitudeA * magnitudeB)
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
     * Calculate time decay factor (exponential decay)
     */
    fun timeDecay(timestamp: Long, currentTime: Long, halfLife: Long = 7 * 24 * 60 * 60 * 1000): Double {
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
}