package com.just_for_fun.synctax.core.ml.agents

import com.just_for_fun.synctax.core.ml.models.RecommendationResult
import com.just_for_fun.synctax.core.ml.models.SongFeatures
import com.just_for_fun.synctax.core.utils.MathUtils
import kotlin.math.pow

/**
 * Statistical-based recommendation using weighted scoring
 * Similar to Tier-0 Agent in OD-MAS
 */
class StatisticalAgent {

    private val featureWeights = doubleArrayOf(
        0.15, // playFrequency
        0.20, // avgCompletionRate
        0.10, // skipRate (inverted)
        0.15, // recencyScore
        0.10, // timeOfDayMatch
        0.10, // genreAffinity
        0.10, // artistAffinity
        0.05, // consecutivePlays
        0.05  // sessionContext
    )

    suspend fun analyze(songFeatures: SongFeatures): RecommendationResult {
        val featureVector = songFeatures.toVector()

        // Calculate weighted score
        val rawScore = featureVector.zip(featureWeights)
            .sumOf { (feature, weight) -> feature * weight }

        // Apply sigmoid for normalization
        val normalizedScore = MathUtils.sigmoid(rawScore * 2 - 1) * 100

        // Calculate confidence based on data completeness
        val confidence = calculateConfidence(songFeatures)

        return RecommendationResult(
            songId = songFeatures.songId,
            score = normalizedScore,
            confidence = confidence,
            reason = "Statistical pattern matching"
        )
    }

    private fun calculateConfidence(features: SongFeatures): Float {
        // Higher confidence with more play history
        val historyFactor = minOf(features.playFrequency / 10.0, 1.0)

        // Higher confidence with higher completion rate
        val completionFactor = features.avgCompletionRate

        // Lower confidence with high skip rate
        val skipPenalty = 1.0 - features.skipRate

        return ((historyFactor * 0.4 + completionFactor * 0.4 + skipPenalty * 0.2) * 100).toFloat()
    }
}