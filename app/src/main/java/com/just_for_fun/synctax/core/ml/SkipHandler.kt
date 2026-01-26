package com.just_for_fun.synctax.core.ml

import com.just_for_fun.synctax.core.ml.agents.SequenceRecommenderAgent
import com.just_for_fun.synctax.data.local.MusicDatabase
import com.just_for_fun.synctax.data.local.entities.ListeningHistory
import com.just_for_fun.synctax.data.local.entities.UserPreference
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlin.math.exp

/**
 * Handles skip behavior and real-time queue adaptation.
 * 
 * When a user skips a song, the system should:
 * 1. Record the skip in listening history
 * 2. Update transition weights (penalize the skip)
 * 3. Update user preference (increase skip score)
 * 4. Analyze skip pattern to adjust queue
 * 5. Optionally notify queue manager to adapt
 * 
 * Skip Classification:
 * - Early skip (< 10% of song): Strong negative signal
 * - Mid skip (10-50% of song): Moderate negative signal  
 * - Late skip (> 50% of song): Weak negative signal (maybe interrupted)
 * 
 * Mathematical Formulas:
 * - Skip penalty: penalty = e^(-listen_time / song_duration)
 * - Score update: skip_score += penalty
 * - Like score: like_score -= penalty
 */
class SkipHandler(
    private val database: MusicDatabase,
    private val sequenceAgent: SequenceRecommenderAgent
) {
    companion object {
        // Skip thresholds
        const val EARLY_SKIP_THRESHOLD = 0.10    // < 10% = early skip
        const val MID_SKIP_THRESHOLD = 0.50      // < 50% = mid skip
        
        // Penalty multipliers
        const val EARLY_SKIP_PENALTY = 1.0f      // Strong penalty
        const val MID_SKIP_PENALTY = 0.5f        // Moderate penalty
        const val LATE_SKIP_PENALTY = 0.2f       // Weak penalty
        
        // Skip score decay (to not over-penalize old behavior)
        const val SKIP_SCORE_DECAY = 0.95f
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Callback interface for queue adaptation
    var onSkipPatternDetected: ((SkipPattern) -> Unit)? = null

    /**
     * Process a skip event
     * @param skippedSongId The song that was skipped
     * @param previousSongId The song that played before (if any)
     * @param listenDuration How long the user listened (ms)
     * @param totalDuration Total song duration (ms)
     */
    suspend fun onSongSkipped(
        skippedSongId: String,
        previousSongId: String?,
        listenDuration: Long,
        totalDuration: Long
    ) {
        // Calculate completion rate
        val completionRate = if (totalDuration > 0) {
            (listenDuration.toFloat() / totalDuration).coerceIn(0f, 1f)
        } else 0f

        // Classify skip type
        val skipType = classifySkip(completionRate)
        
        // Calculate penalty based on skip type
        val penalty = calculateSkipPenalty(completionRate, skipType)

        // 1. Record in listening history
        recordSkipHistory(skippedSongId, listenDuration, completionRate)

        // 2. Update transition weights (if we know previous song)
        if (previousSongId != null) {
            sequenceAgent.recordSkip(previousSongId, skippedSongId)
        }

        // 3. Update user preference
        updateSkipPreference(skippedSongId, penalty)

        // 4. Analyze pattern and potentially adapt queue
        analyzeSkipPattern(skippedSongId, skipType, penalty)
    }

    /**
     * Classify the skip based on completion rate
     */
    private fun classifySkip(completionRate: Float): SkipType {
        return when {
            completionRate < EARLY_SKIP_THRESHOLD -> SkipType.EARLY
            completionRate < MID_SKIP_THRESHOLD -> SkipType.MID
            else -> SkipType.LATE
        }
    }

    /**
     * Calculate penalty using exponential decay formula
     * penalty = e^(-listen_time / song_duration) = e^(-completionRate)
     * Shorter listens = higher penalty
     */
    private fun calculateSkipPenalty(completionRate: Float, skipType: SkipType): Float {
        val basePenalty = exp(-completionRate.toDouble()).toFloat()
        
        val multiplier = when (skipType) {
            SkipType.EARLY -> EARLY_SKIP_PENALTY
            SkipType.MID -> MID_SKIP_PENALTY
            SkipType.LATE -> LATE_SKIP_PENALTY
        }
        
        return basePenalty * multiplier
    }

    /**
     * Record skip in listening history
     */
    private suspend fun recordSkipHistory(
        songId: String,
        listenDuration: Long,
        completionRate: Float
    ) {
        val currentTime = System.currentTimeMillis()
        val calendar = java.util.Calendar.getInstance()
        
        database.listeningHistoryDao().insertHistory(
            ListeningHistory(
                songId = songId,
                playTimestamp = currentTime,
                listenDuration = listenDuration,
                completionRate = completionRate,
                skipped = true,
                timeOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY),
                dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK)
            )
        )
    }

    /**
     * Update user preference with skip penalty
     */
    private suspend fun updateSkipPreference(songId: String, penalty: Float) {
        val existing = database.userPreferenceDao().getPreference(songId)
        
        if (existing != null) {
            // Update existing preference
            val newSkipCount = existing.skipCount + 1
            val totalInteractions = existing.playCount + newSkipCount
            
            // Recalculate like score with skip penalty
            // Formula: likeScore = (plays - skips) / total * completionRate
            val newLikeScore = if (totalInteractions > 0) {
                ((existing.playCount - newSkipCount).toFloat() / totalInteractions * 
                    existing.avgCompletionRate).coerceIn(-1f, 1f)
            } else 0f
            
            database.userPreferenceDao().updatePreference(
                existing.copy(
                    skipCount = newSkipCount,
                    likeScore = newLikeScore
                )
            )
        } else {
            // Create new preference with skip
            database.userPreferenceDao().insertPreference(
                UserPreference(
                    songId = songId,
                    playCount = 0,
                    skipCount = 1,
                    likeScore = -penalty,
                    lastPlayed = System.currentTimeMillis(),
                    avgCompletionRate = 0f
                )
            )
        }
    }

    // Track recent skips for pattern detection
    private val recentSkips = mutableListOf<SkipEvent>()
    private val MAX_RECENT_SKIPS = 10

    /**
     * Analyze skip patterns to detect user dissatisfaction
     */
    private fun analyzeSkipPattern(songId: String, skipType: SkipType, penalty: Float) {
        // Record this skip
        recentSkips.add(SkipEvent(songId, skipType, penalty, System.currentTimeMillis()))
        
        // Keep only recent skips
        if (recentSkips.size > MAX_RECENT_SKIPS) {
            recentSkips.removeAt(0)
        }
        
        // Detect patterns
        val pattern = detectPattern()
        
        if (pattern != SkipPattern.NONE) {
            // Notify listener (e.g., queue manager) to adapt
            onSkipPatternDetected?.invoke(pattern)
        }
    }

    /**
     * Detect skip patterns from recent behavior
     */
    private fun detectPattern(): SkipPattern {
        if (recentSkips.size < 3) return SkipPattern.NONE
        
        val fiveMinutesAgo = System.currentTimeMillis() - (5 * 60 * 1000)
        val recentFiveMin = recentSkips.filter { it.timestamp > fiveMinutesAgo }
        
        // Pattern: 3+ early skips in 5 minutes = user is frustrated
        val earlySkipsCount = recentFiveMin.count { it.type == SkipType.EARLY }
        if (earlySkipsCount >= 3) {
            return SkipPattern.FRUSTRATED
        }
        
        // Pattern: 5+ skips of any type in 5 minutes = searching for something
        if (recentFiveMin.size >= 5) {
            return SkipPattern.SEARCHING
        }
        
        // Pattern: Late skips might indicate interruption, not dislike
        val lateSkipsRatio = recentFiveMin.count { it.type == SkipType.LATE }.toFloat() / 
            recentFiveMin.size.coerceAtLeast(1)
        if (lateSkipsRatio > 0.7) {
            return SkipPattern.INTERRUPTED
        }
        
        return SkipPattern.NONE
    }

    /**
     * Get recommendations to avoid based on skip history
     * Returns song IDs that should be deprioritized
     */
    suspend fun getSongsToAvoid(limit: Int = 20): List<String> {
        // Get songs with high skip rates from preferences
        val preferences = database.userPreferenceDao().getTopPreferences(100).first()
        
        // Filter songs with high skip counts relative to play counts
        return preferences
            .filter { it.playCount > 0 && (it.skipCount.toFloat() / it.playCount) > 0.5f }
            .sortedByDescending { it.skipCount.toFloat() / it.playCount }
            .take(limit)
            .map { it.songId }
    }

    /**
     * Calculate adaptive skip threshold based on user behavior
     * Some users skip more, some less - adjust expectations
     */
    suspend fun getAdaptiveSkipThreshold(): Float {
        // Get average skip rate across all songs
        // If user generally skips a lot, be more lenient
        // If user rarely skips, even one skip is significant
        
        return EARLY_SKIP_THRESHOLD.toFloat() // Default for now
    }

    /**
     * Reset recent skip tracking (e.g., when user manually selects a song)
     */
    fun resetSkipTracking() {
        recentSkips.clear()
    }

    /**
     * Get skip statistics for analytics
     */
    fun getSkipStatistics(): SkipStatistics {
        val earlySkips = recentSkips.count { it.type == SkipType.EARLY }
        val midSkips = recentSkips.count { it.type == SkipType.MID }
        val lateSkips = recentSkips.count { it.type == SkipType.LATE }
        val avgPenalty = recentSkips.map { it.penalty }.average().toFloat()
        
        return SkipStatistics(
            totalRecentSkips = recentSkips.size,
            earlySkips = earlySkips,
            midSkips = midSkips,
            lateSkips = lateSkips,
            averagePenalty = avgPenalty,
            currentPattern = detectPattern()
        )
    }
}

/**
 * Skip classification
 */
enum class SkipType {
    EARLY,  // < 10% listened
    MID,    // 10-50% listened
    LATE    // > 50% listened
}

/**
 * Detected skip patterns
 */
enum class SkipPattern {
    NONE,           // Normal behavior
    FRUSTRATED,     // Multiple early skips - queue not matching mood
    SEARCHING,      // Many skips - looking for specific song/mood
    INTERRUPTED     // Late skips - external interruption, not dislike
}

/**
 * Skip event for pattern tracking
 */
data class SkipEvent(
    val songId: String,
    val type: SkipType,
    val penalty: Float,
    val timestamp: Long
)

/**
 * Skip statistics for analytics
 */
data class SkipStatistics(
    val totalRecentSkips: Int,
    val earlySkips: Int,
    val midSkips: Int,
    val lateSkips: Int,
    val averagePenalty: Float,
    val currentPattern: SkipPattern
)
