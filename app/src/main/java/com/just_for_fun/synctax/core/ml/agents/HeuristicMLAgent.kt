package com.just_for_fun.synctax.core.ml.agents

import android.content.Context
import android.util.Log
import com.just_for_fun.synctax.core.ml.models.RecommendationResult
import com.just_for_fun.synctax.core.ml.models.SongFeatures
import com.just_for_fun.synctax.core.utils.MathUtils
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import java.io.File
import kotlin.math.sqrt

/**
 * Native Kotlin implementation of the heuristic ML logic from music_ml.py.
 * Replaces the need for Chaquopy for core ML scoring.
 */
class HeuristicMLAgent(private val context: Context) {
    private val TAG = "HeuristicMLAgent"
    private val modelFile = File(context.filesDir, "heuristic_ml_model.json")
    
    @Serializable
    data class ModelState(
        val isTrained: Boolean = false,
        val userMean: List<Double>? = null,
        val userStd: List<Double>? = null,
        val userProfileVector: List<Double>? = null,
        val centroids: List<List<Double>>? = null,
        val nClusters: Int = 5
    )

    private var state = ModelState()
    private val recentRecommendations = mutableListOf<DoubleArray>()
    private val json = Json { ignoreUnknownKeys = true }

    init {
        loadModel()
    }

    /**
     * Train the model on user listening history.
     */
    fun train(userHistory: List<SongFeatures>): Boolean {
        if (userHistory.size < 5) {
            Log.d(TAG, "Insufficient training data (${userHistory.size} samples)")
            return false
        }

        try {
            val featuresList = userHistory.map { it.toVector() }
            val nFeatures = featuresList[0].size

            // 1. Calculate mean and std for each feature (Z-score normalization basis)
            val means = mutableListOf<Double>()
            val stds = mutableListOf<Double>()

            for (i in 0 until nFeatures) {
                val values = featuresList.map { it[i] }
                val mean = values.average()
                val variance = values.map { (it - mean) * (it - mean) }.average()
                val std = if (variance > 0) sqrt(variance) else 1.0
                
                means.add(mean)
                stds.add(std)
            }

            // 2. Simple Clustering (K-means)
            val centroids = runKMeans(featuresList, state.nClusters)

            // 3. Update state
            state = ModelState(
                isTrained = true,
                userMean = means,
                userStd = stds,
                userProfileVector = means, // Profile is the average of history
                centroids = centroids,
                nClusters = state.nClusters
            )

            saveModel()
            Log.d(TAG, "Model trained successfully with ${userHistory.size} samples")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Training failed: ${e.message}")
            return false
        }
    }

    /**
     * Generate a recommendation score for a song.
     */
    fun analyze(songFeatures: SongFeatures): RecommendationResult {
        if (!state.isTrained || state.userMean == null || state.userStd == null) {
            return RecommendationResult(
                songId = songFeatures.songId,
                score = 50.0,
                confidence = 0.3f,
                reason = "Model not trained"
            )
        }

        val vector = songFeatures.toVector()
        
        // 1. Calculate base score using Z-scores and weights
        val similarityScores = mutableListOf<Double>()
        for (i in vector.indices) {
            val mean = state.userMean!![i]
            val std = state.userStd!![i]
            val zScore = Math.abs((vector[i] - mean) / std)
            val similarity = 1.0 / (1.0 + zScore)
            similarityScores.add(similarity)
        }

        // Feature weights (matching music_ml.py)
        val weights = listOf(
            0.10, 0.18, 0.12, 0.10, 0.08, 0.08, 0.08, 0.04, 0.04, 0.06, 0.05, 0.03, 0.02, 0.02
        )
        
        var weightedScore = 0.0
        for (i in similarityScores.indices) {
            val weight = if (i < weights.size) weights[i] else (1.0 / similarityScores.size)
            weightedScore += similarityScores[i] * weight
        }

        // 2. Content-based bonus (Cosine similarity to user profile)
        state.userProfileVector?.let { profile ->
            val cosSim = MathUtils.cosineSimilarity(vector, profile.toDoubleArray())
            weightedScore = weightedScore * 0.7 + cosSim * 0.3
        }

        // 3. Skip Penalty
        val skipRate = if (vector.size > 2) vector[2] else 0.0
        val skipPenalty = 1.0 - (skipRate * 0.5)
        weightedScore *= skipPenalty

        // 4. Diversity Penalty
        val diversityPenalty = MathUtils.diversityPenalty(vector, recentRecommendations)
        val diversityFactor = 0.3
        val finalScore = (weightedScore * (1.0 - diversityFactor * diversityPenalty)) * 100

        // 5. Confidence Score (Implicit Feedback)
        val completion = if (vector.size > 1) vector[1] else 0.5
        val playFreq = if (vector.size > 0) vector[0] else 0.0
        val confidence = MathUtils.confidenceScore(completion, skipRate, (playFreq * 100).toInt())

        // Track for diversity
        recentRecommendations.add(vector)
        if (recentRecommendations.size > 50) recentRecommendations.removeAt(0)

        // Cluster assignment (for informational purposes)
        val clusterId = predictCluster(vector)

        return RecommendationResult(
            songId = songFeatures.songId,
            score = finalScore.coerceIn(0.0, 100.0),
            confidence = confidence.toFloat(),
            reason = "Native Heuristic (Cluster $clusterId)"
        )
    }

    fun getStatus() = com.just_for_fun.synctax.core.chaquopy.ModelStatus(
        isTrained = state.isTrained,
        hasScorer = state.userMean != null,
        nClusters = state.centroids?.size ?: 0
    )

    fun reset(): Boolean {
        state = ModelState()
        if (modelFile.exists()) modelFile.delete()
        recentRecommendations.clear()
        return true
    }

    private fun runKMeans(data: List<DoubleArray>, k: Int): List<List<Double>> {
        if (data.size < k) return data.map { it.toList() }

        // Initialize centroids (simple sampling)
        var centroids = data.shuffled().take(k).map { it.toList() }

        repeat(10) { // 10 iterations
            val clusters = List(k) { mutableListOf<DoubleArray>() }

            // Assignment
            for (point in data) {
                val nearestIndex = centroids.indices.minByOrNull { i ->
                    MathUtils.euclideanDistance(point, centroids[i].toDoubleArray())
                } ?: 0
                clusters[nearestIndex].add(point)
            }

            // Update
            centroids = clusters.mapIndexed { i, cluster ->
                if (cluster.isEmpty()) centroids[i]
                else {
                    val nFeatures = cluster[0].size
                    DoubleArray(nFeatures) { featureIdx ->
                        cluster.map { it[featureIdx] }.average()
                    }.toList()
                }
            }
        }
        return centroids
    }

    private fun predictCluster(vector: DoubleArray): Int {
        val centroids = state.centroids ?: return -1
        return centroids.indices.minByOrNull { i ->
            MathUtils.euclideanDistance(vector, centroids[i].toDoubleArray())
        } ?: 0
    }

    private fun saveModel() {
        try {
            val jsonStr = json.encodeToString(state)
            modelFile.writeText(jsonStr)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save model: ${e.message}")
        }
    }

    private fun loadModel() {
        try {
            if (modelFile.exists()) {
                val jsonStr = modelFile.readText()
                state = json.decodeFromString(jsonStr)
                Log.d(TAG, "Model loaded successfully")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load model: ${e.message}")
        }
    }
}
