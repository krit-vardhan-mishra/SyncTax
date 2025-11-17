package com.just_for_fun.synctax.core.ml.agents

import com.just_for_fun.synctax.core.ml.models.RecommendationResult
import com.just_for_fun.synctax.core.ml.models.SongFeatures
import com.just_for_fun.synctax.core.utils.MathUtils
import com.just_for_fun.synctax.core.utils.VectorDatabase

/**
 * Collaborative filtering using vector similarity
 * Similar to Tier-1 Agent in OD-MAS
 */
class CollaborativeFilteringAgent(
    private val vectorDb: VectorDatabase
) {

    suspend fun analyze(
        songFeatures: SongFeatures,
        userListeningHistory: List<SongFeatures>
    ): RecommendationResult {
        // Create user profile vector (average of liked songs)
        val userProfileVector = createUserProfileVector(userListeningHistory)

        // Calculate similarity with current song
        val songVector = songFeatures.toVector()
        val similarity = MathUtils.cosineSimilarity(userProfileVector, songVector)

        // Find similar songs from vector database
        val similarSongs = vectorDb.findSimilar(songVector, topK = 5)
        val avgSimilarityScore = similarSongs.map { it.second }.average()

        // Combine direct similarity and neighborhood similarity
        val combinedScore = (similarity * 0.7 + avgSimilarityScore * 0.3) * 100

        val confidence = calculateConfidence(similarity, similarSongs.size)

        return RecommendationResult(
            songId = songFeatures.songId,
            score = combinedScore,
            confidence = confidence,
            reason = "Collaborative filtering"
        )
    }

    private fun createUserProfileVector(history: List<SongFeatures>): DoubleArray {
        if (history.isEmpty()) return DoubleArray(9) { 0.5 }

        val vectors = history.map { it.toVector() }
        val dimensions = vectors.first().size

        return DoubleArray(dimensions) { dim ->
            vectors.map { it[dim] }.average()
        }
    }

    private fun calculateConfidence(similarity: Double, neighborCount: Int): Float {
        val similarityFactor = similarity
        val neighborFactor = minOf(neighborCount / 5.0, 1.0)

        return ((similarityFactor * 0.6 + neighborFactor * 0.4) * 100).toFloat()
    }

    suspend fun trainFromHistory(history: List<SongFeatures>) {
        // Store song vectors in database
        history.forEach { features ->
            vectorDb.storeVector(features.songId, features.toVector())
        }
    }
}