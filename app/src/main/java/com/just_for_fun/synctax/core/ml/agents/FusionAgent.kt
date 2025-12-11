package com.just_for_fun.synctax.core.ml.agents

import com.just_for_fun.synctax.core.ml.models.RecommendationResult

/**
 * Fuses results from multiple agents
 * Similar to Fusion Agent in OD-MAS
 */
class FusionAgent {

    fun fuseRecommendations(
        statisticalResult: RecommendationResult,
        collaborativeResult: RecommendationResult,
        pythonMlResult: RecommendationResult? = null
    ): RecommendationResult {

        // If Python ML is available and confident, give it more weight
        return if (pythonMlResult != null && pythonMlResult.confidence > 80f) {
            fusWithPythonML(statisticalResult, collaborativeResult, pythonMlResult)
        } else {
            fuseKotlinAgents(statisticalResult, collaborativeResult)
        }
    }

    private fun fusWithPythonML(
        statistical: RecommendationResult,
        collaborative: RecommendationResult,
        pythonMl: RecommendationResult
    ): RecommendationResult {
        // Python ML gets 50%, others split remaining 50%
        val fusedScore = pythonMl.score * 0.5 +
                statistical.score * 0.2 +
                collaborative.score * 0.3

        val avgConfidence = (pythonMl.confidence + statistical.confidence + collaborative.confidence) / 3

        return RecommendationResult(
            songId = statistical.songId,
            score = fusedScore,
            confidence = avgConfidence,
            reason = "Multi-agent fusion with ML"
        )
    }

    private fun fuseKotlinAgents(
        statistical: RecommendationResult,
        collaborative: RecommendationResult
    ): RecommendationResult {
        // Statistical: 40%, Collaborative: 60%
        val fusedScore = statistical.score * 0.4 + collaborative.score * 0.6

        val avgConfidence = (statistical.confidence + collaborative.confidence) / 2

        return RecommendationResult(
            songId = statistical.songId,
            score = fusedScore,
            confidence = avgConfidence,
            reason = "Statistical + Collaborative fusion"
        )
    }
}
