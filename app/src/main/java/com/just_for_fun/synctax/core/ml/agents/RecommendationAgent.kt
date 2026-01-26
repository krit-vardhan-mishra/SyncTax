package com.just_for_fun.synctax.core.ml.agents

import com.just_for_fun.synctax.core.ml.models.QuickPicksResult
import com.just_for_fun.synctax.core.ml.models.RecommendationResult
import kotlin.math.exp

/**
 * Policy agent for final recommendation decisions
 */
class RecommendationAgent {

    private val recentRecommendations = mutableListOf<String>()
    private val maxRecentHistory = 50

    fun generateQuickPicks(
        candidateResults: List<RecommendationResult>,
        count: Int = 20
    ): QuickPicksResult {
        // Filter out recently recommended songs
        val filtered = candidateResults.filter { result ->
            result.songId !in recentRecommendations
        }

        // Apply diversity penalty to ensure variety
        val diversified = applyDiversityBoost(filtered)

        // Sort by score and take top recommendations
        val topRecommendations = diversified
            .sortedByDescending { it.score }
            .take(count)

        // Update recent recommendations history
        topRecommendations.forEach { result ->
            recentRecommendations.add(0, result.songId)
            if (recentRecommendations.size > maxRecentHistory) {
                recentRecommendations.removeLast()
            }
        }

        return QuickPicksResult(
            recommendations = topRecommendations,
            modelVersion = "1.0.0"
        )
    }

    private fun applyDiversityBoost(results: List<RecommendationResult>): List<RecommendationResult> {
        // Group by score ranges and ensure diversity
        val scoreGroups = results.groupBy { (it.score / 10).toInt() }

        return scoreGroups.flatMap { (_, group) ->
            // Slightly boost varied content
            group.mapIndexed { index, result ->
                val diversityBoost = exp(-index * 0.1) * 2
                result.copy(score = result.score + diversityBoost)
            }
        }
    }

    fun clearRecommendationHistory() {
        recentRecommendations.clear()
    }
}
