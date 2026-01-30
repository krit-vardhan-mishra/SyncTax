package com.just_for_fun.synctax.core.ml.agents

import com.just_for_fun.synctax.core.ml.models.RecommendationResult

/**
 * Fuses results from multiple agents.
 * 
 * Enhanced with sequence-based and content-based recommendations (from RECOMMENDATION_LOGIC_NEW.md):
 * - Statistical Agent: Feature-based scoring (behavior patterns)
 * - Collaborative Agent: Vector similarity (user profile matching)
 * - Sequence Agent: Markov chain transitions (song-to-song patterns)
 * - Content-Based Agent: Metadata similarity (artist, album, genre)
 * - Python ML Agent: Ensemble learning (bonus when confident)
 */
class FusionAgent {

    fun fuseRecommendations(
        statisticalResult: RecommendationResult,
        collaborativeResult: RecommendationResult,
        pythonMlResult: RecommendationResult? = null,
        sequenceResult: RecommendationResult? = null,
        contentBasedResult: RecommendationResult? = null
    ): RecommendationResult {

        // If Python ML is available and confident, give it more weight
        return if (pythonMlResult != null && pythonMlResult.confidence > 80f) {
            fuseWithPythonML(statisticalResult, collaborativeResult, pythonMlResult, sequenceResult, contentBasedResult)
        } else if (sequenceResult != null && sequenceResult.confidence > 60f) {
            fuseWithSequence(statisticalResult, collaborativeResult, sequenceResult, contentBasedResult)
        } else if (contentBasedResult != null && contentBasedResult.confidence > 50f) {
            fuseWithContentBased(statisticalResult, collaborativeResult, contentBasedResult)
        } else {
            fuseKotlinAgents(statisticalResult, collaborativeResult)
        }
    }

    private fun fuseWithPythonML(
        statistical: RecommendationResult,
        collaborative: RecommendationResult,
        pythonMl: RecommendationResult,
        sequence: RecommendationResult?,
        contentBased: RecommendationResult?
    ): RecommendationResult {
        // Python ML gets 35%, sequence 15%, content 10%, others split remaining
        val sequenceScore = sequence?.score ?: 0.0
        val sequenceWeight = if (sequence != null) 0.15 else 0.0
        val contentScore = contentBased?.score ?: 0.0
        val contentWeight = if (contentBased != null) 0.10 else 0.0
        
        val fusedScore = pythonMl.score * 0.35 +
                statistical.score * 0.15 +
                collaborative.score * 0.25 +
                sequenceScore * sequenceWeight +
                contentScore * contentWeight

        val avgConfidence = listOfNotNull(
            pythonMl.confidence,
            statistical.confidence,
            collaborative.confidence,
            sequence?.confidence,
            contentBased?.confidence
        ).average().toFloat()

        val reason = buildString {
            append("Multi-agent fusion with ML")
            if (sequence != null) append(" + Sequential")
            if (contentBased != null) append(" + Content")
        }

        return RecommendationResult(
            songId = statistical.songId,
            score = fusedScore,
            confidence = avgConfidence,
            reason = reason
        )
    }

    private fun fuseWithSequence(
        statistical: RecommendationResult,
        collaborative: RecommendationResult,
        sequence: RecommendationResult,
        contentBased: RecommendationResult?
    ): RecommendationResult {
        // Sequence gets 30%, Content 15% (if available), Statistical 20%, Collaborative 35%
        val contentScore = contentBased?.score ?: 0.0
        val contentWeight = if (contentBased != null) 0.15 else 0.0
        val statWeight = if (contentBased != null) 0.20 else 0.25
        
        val fusedScore = statistical.score * statWeight + 
                collaborative.score * 0.35 + 
                sequence.score * 0.30 +
                contentScore * contentWeight

        val avgConfidence = listOfNotNull(
            statistical.confidence, 
            collaborative.confidence, 
            sequence.confidence,
            contentBased?.confidence
        ).average().toFloat()

        val reason = buildString {
            append("Statistical + Collaborative + Sequential")
            if (contentBased != null) append(" + Content")
            append(" fusion")
        }

        return RecommendationResult(
            songId = statistical.songId,
            score = fusedScore,
            confidence = avgConfidence,
            reason = reason
        )
    }
    
    private fun fuseWithContentBased(
        statistical: RecommendationResult,
        collaborative: RecommendationResult,
        contentBased: RecommendationResult
    ): RecommendationResult {
        // Content gets 25%, Statistical 30%, Collaborative 45%
        val fusedScore = statistical.score * 0.30 + 
                collaborative.score * 0.45 + 
                contentBased.score * 0.25

        val avgConfidence = (statistical.confidence + collaborative.confidence + contentBased.confidence) / 3

        return RecommendationResult(
            songId = statistical.songId,
            score = fusedScore,
            confidence = avgConfidence,
            reason = "Statistical + Collaborative + Content fusion"
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
