package com.just_for_fun.synctax.core.ml.agents

import com.just_for_fun.synctax.core.ml.models.RecommendationResult
import com.just_for_fun.synctax.core.utils.MathUtils
import com.just_for_fun.synctax.data.local.dao.SongTransitionDao
import com.just_for_fun.synctax.data.local.entities.SongTransition
import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

/**
 * Sequence-based recommendation engine using Markov Chain.
 * 
 * This agent learns song-to-song transitions based on user behavior:
 * - Tracks which songs are typically played after others
 * - Adjusts weights based on completions (positive) and skips (negative)
 * - Uses probabilistic selection with exploration factor
 * 
 * Mathematical Model:
 * - Transition probability: P(B|A) = weight(A→B) / Σ(weights from A)
 * - Weight update on completion: w_new = w_old + α (α = learning rate)
 * - Weight update on skip: w_new = w_old * β (β = decay factor)
 * - Time decay: w_decayed = w * e^(-λ * days) (λ = decay constant)
 * 
 * Selection Strategy (ε-greedy):
 * - (1-ε) probability: select highest weight transition (exploitation)
 * - ε probability: select random transition (exploration)
 */
class SequenceRecommenderAgent(
    private val transitionDao: SongTransitionDao
) {
    companion object {
        // Learning parameters
        const val LEARNING_RATE = 0.1f          // α: weight increase on completion
        const val DECAY_FACTOR = 0.5f           // β: weight multiplier on skip
        const val TIME_DECAY_LAMBDA = 0.03      // λ: time decay constant (~23 day half-life)
        const val EXPLORATION_RATE = 0.2        // ε: exploration probability
        const val MIN_WEIGHT_THRESHOLD = 0.05f  // Minimum weight before pruning
        const val REPEAT_BONUS = 2.0f           // Extra weight for repeat transitions
    }

    /**
     * Record a successful transition (user played song B after song A)
     */
    suspend fun recordTransition(
        fromSongId: String,
        toSongId: String,
        completionRate: Float = 1.0f,
        isRepeat: Boolean = false
    ) {
        val existing = transitionDao.getTransition(fromSongId, toSongId)

        if (existing != null) {
            // Reinforce existing transition
            val learningRate = if (isRepeat) LEARNING_RATE * REPEAT_BONUS else LEARNING_RATE
            transitionDao.reinforceTransition(
                fromId = fromSongId,
                toId = toSongId,
                learningRate = learningRate,
                completionRate = completionRate
            )
        } else {
            // Create new transition
            transitionDao.insertTransition(
                SongTransition(
                    fromSongId = fromSongId,
                    toSongId = toSongId,
                    weight = 1.0f,
                    playCount = 1,
                    avgCompletionRate = completionRate
                )
            )
        }
    }

    /**
     * Record a skip (user skipped song B after song A)
     */
    suspend fun recordSkip(fromSongId: String, toSongId: String) {
        val existing = transitionDao.getTransition(fromSongId, toSongId)

        if (existing != null) {
            // Apply penalty to existing transition
            transitionDao.penalizeTransition(
                fromId = fromSongId,
                toId = toSongId,
                decayFactor = DECAY_FACTOR
            )
        } else {
            // Create new transition with low weight (already penalized)
            transitionDao.insertTransition(
                SongTransition(
                    fromSongId = fromSongId,
                    toSongId = toSongId,
                    weight = DECAY_FACTOR,
                    playCount = 0,
                    skipCount = 1,
                    avgCompletionRate = 0f
                )
            )
        }
    }

    /**
     * Get recommended next songs based on Markov chain transitions
     * Uses ε-greedy strategy for exploration vs exploitation
     */
    suspend fun getNextSongCandidates(
        currentSongId: String,
        excludeSongIds: Set<String> = emptySet(),
        limit: Int = 10
    ): List<RecommendationResult> {
        val transitions = transitionDao.getTransitionsFrom(currentSongId)

        if (transitions.isEmpty()) {
            return emptyList()
        }

        // Filter out excluded songs and apply time decay
        val currentTime = System.currentTimeMillis()
        val validTransitions = transitions
            .filter { it.toSongId !in excludeSongIds }
            .map { transition ->
                // Apply time decay to weight
                val daysSince = (currentTime - transition.lastOccurred) / (24 * 60 * 60 * 1000.0)
                val decayedWeight = transition.weight * exp(-TIME_DECAY_LAMBDA * daysSince).toFloat()
                transition.copy(weight = decayedWeight.coerceAtLeast(MIN_WEIGHT_THRESHOLD))
            }
            .filter { it.weight >= MIN_WEIGHT_THRESHOLD }

        if (validTransitions.isEmpty()) {
            return emptyList()
        }

        // Calculate transition probabilities
        val totalWeight = validTransitions.sumOf { it.weight.toDouble() }.toFloat()
        
        return validTransitions
            .sortedByDescending { it.weight }
            .take(limit)
            .map { transition ->
                val probability = transition.weight / totalWeight
                val confidence = calculateConfidence(transition)
                
                RecommendationResult(
                    songId = transition.toSongId,
                    score = (probability * 100).toDouble(),
                    confidence = confidence,
                    reason = "Sequential pattern: ${(probability * 100).toInt()}% after current"
                )
            }
    }

    /**
     * Select next song using ε-greedy strategy
     * (1-ε): select best transition (exploitation)
     * ε: select random transition (exploration)
     */
    suspend fun selectNextSong(
        currentSongId: String,
        excludeSongIds: Set<String> = emptySet()
    ): String? {
        val transitions = transitionDao.getTransitionsFrom(currentSongId)
            .filter { it.toSongId !in excludeSongIds }

        if (transitions.isEmpty()) return null

        return if (Random.nextDouble() < EXPLORATION_RATE) {
            // Exploration: random selection weighted by probability
            selectByRouletteWheel(transitions)
        } else {
            // Exploitation: select highest weight
            transitions.maxByOrNull { it.weight }?.toSongId
        }
    }

    /**
     * Roulette wheel selection (weighted random)
     * Each transition has probability proportional to its weight
     */
    private fun selectByRouletteWheel(transitions: List<SongTransition>): String? {
        if (transitions.isEmpty()) return null

        val totalWeight = transitions.sumOf { it.weight.toDouble() }
        var random = Random.nextDouble() * totalWeight
        
        for (transition in transitions) {
            random -= transition.weight
            if (random <= 0) {
                return transition.toSongId
            }
        }
        
        return transitions.last().toSongId
    }

    /**
     * Calculate confidence based on transition strength and play count
     */
    private fun calculateConfidence(transition: SongTransition): Float {
        // More plays = higher confidence
        val playFactor = minOf(transition.playCount / 10.0, 1.0)
        
        // Higher completion rate = higher confidence
        val completionFactor = transition.avgCompletionRate
        
        // Lower skip ratio = higher confidence
        val totalInteractions = transition.playCount + transition.skipCount
        val skipRatio = if (totalInteractions > 0) {
            transition.skipCount.toDouble() / totalInteractions
        } else 0.0
        val skipPenalty = 1.0 - skipRatio
        
        return ((playFactor * 0.3 + completionFactor * 0.4 + skipPenalty * 0.3) * 100).toFloat()
    }

    /**
     * Analyze song features based on transition patterns
     * Returns a score indicating how well this song fits in sequences
     */
    suspend fun analyzeSequenceStrength(songId: String): RecommendationResult {
        val incomingTransitions = transitionDao.getTransitionsTo(songId)
        val outgoingTransitions = transitionDao.getTransitionsFrom(songId)

        // Calculate sequence connectivity score
        val incomingScore = incomingTransitions.sumOf { it.weight.toDouble() }
        val outgoingScore = outgoingTransitions.sumOf { it.weight.toDouble() }
        
        // Songs that are well-connected in both directions are more valuable
        val connectivityScore = (incomingScore + outgoingScore) / 2.0
        
        val confidence = minOf(
            (incomingTransitions.size + outgoingTransitions.size) / 20f * 100f,
            100f
        )

        return RecommendationResult(
            songId = songId,
            score = minOf(connectivityScore * 10, 100.0),
            confidence = confidence,
            reason = "Sequence connectivity: ${incomingTransitions.size} incoming, ${outgoingTransitions.size} outgoing"
        )
    }

    /**
     * Get strongly connected song pairs (bidirectional high weight)
     * These are "magnetic tracks" that users love to play together
     */
    suspend fun getMagneticPairs(limit: Int = 20): List<Pair<String, String>> {
        return transitionDao.getStronglyConnectedTransitions(limit = limit)
            .map { it.fromSongId to it.toSongId }
    }

    /**
     * Periodic maintenance: prune weak transitions and apply time decay
     */
    suspend fun performMaintenance() {
        val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
        
        // Prune very weak and old transitions
        transitionDao.pruneWeakTransitions(
            minWeight = MIN_WEIGHT_THRESHOLD,
            beforeTimestamp = thirtyDaysAgo
        )
        
        // Apply time decay to old transitions
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        val weeklyDecay = exp(-TIME_DECAY_LAMBDA * 7).toFloat()
        transitionDao.applyTimeDecay(
            decayMultiplier = weeklyDecay,
            beforeTimestamp = sevenDaysAgo
        )
    }

    /**
     * Get transition statistics for debugging/analytics
     */
    suspend fun getStatistics(): TransitionStatistics {
        val count = transitionDao.getTransitionCount()
        return TransitionStatistics(
            totalTransitions = count,
            explorationRate = EXPLORATION_RATE,
            learningRate = LEARNING_RATE,
            decayFactor = DECAY_FACTOR
        )
    }
}

/**
 * Statistics about the transition graph
 */
data class TransitionStatistics(
    val totalTransitions: Int,
    val explorationRate: Double,
    val learningRate: Float,
    val decayFactor: Float
)
