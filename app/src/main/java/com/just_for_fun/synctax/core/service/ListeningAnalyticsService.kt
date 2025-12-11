package com.just_for_fun.synctax.core.service

import com.just_for_fun.synctax.core.dispatcher.AppDispatchers
import com.just_for_fun.synctax.data.local.dao.OnlineListeningHistoryDao
import com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory
import kotlinx.coroutines.withContext
import java.util.Calendar

/**
 * Service that analyzes user listening patterns from history data.
 * All database operations run on AppDispatchers.Database to avoid main thread blocking.
 */
class ListeningAnalyticsService(
    private val historyDao: OnlineListeningHistoryDao
) {
    
    /**
     * Data class representing user preferences extracted from listening history.
     */
    data class UserPreferencesData(
        val topArtists: List<String>,
        val favoriteGenres: List<String>,
        val listeningPatterns: Map<String, Any>,
        val completionRates: Map<String, Float>,
        val skipRates: Map<String, Float>
    )
    
    /**
     * Analyzes user's listening history to generate preferences.
     * Runs on AppDispatchers.Database.
     */
    suspend fun getUserPreferences(): UserPreferencesData = withContext(AppDispatchers.Database) {
        val history = historyDao.getAllHistory()
        
        UserPreferencesData(
            topArtists = analyzeTopArtists(history),
            favoriteGenres = analyzeGenres(history),
            listeningPatterns = analyzeTimePatterns(history),
            completionRates = analyzeCompletionRates(history),
            skipRates = analyzeSkipRates(history)
        )
    }
    
    /**
     * Analyzes top artists based on play count and completion rate.
     * Artists are scored by: playCount + (completionRate * 10)
     */
    private fun analyzeTopArtists(history: List<OnlineListeningHistory>): List<String> {
        return history
            .groupBy { it.artist }
            .mapValues { (_, songs) ->
                songs.sumOf { it.playCount } +
                songs.sumOf { (it.completionRate * 10).toInt() }
            }
            .entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }
    }
    
    /**
     * Analyzes favorite genres from listening history.
     */
    private fun analyzeGenres(history: List<OnlineListeningHistory>): List<String> {
        return history
            .filter { it.genre != null }
            .groupBy { it.genre!! }
            .mapValues { it.value.size }
            .entries
            .sortedByDescending { it.value }
            .take(5)
            .map { it.key }
    }
    
    /**
     * Analyzes listening patterns by hour of day.
     * Returns map containing peak listening hours.
     */
    private fun analyzeTimePatterns(history: List<OnlineListeningHistory>): Map<String, Any> {
        val hourPattern = history.groupBy {
            Calendar.getInstance().apply {
                timeInMillis = it.timestamp
            }.get(Calendar.HOUR_OF_DAY)
        }
        
        return mapOf(
            "peakHours" to hourPattern.entries
                .sortedByDescending { it.value.size }
                .take(3)
                .map { it.key }
        )
    }
    
    /**
     * Analyzes completion rates for each song.
     */
    private fun analyzeCompletionRates(history: List<OnlineListeningHistory>): Map<String, Float> {
        return history
            .filter { it.totalDuration > 0 }
            .associate { it.videoId to it.completionRate }
    }
    
    /**
     * Analyzes skip rates for each song.
     * Skip rate = skipCount / (playCount + skipCount)
     */
    private fun analyzeSkipRates(history: List<OnlineListeningHistory>): Map<String, Float> {
        return history
            .filter { it.skipCount > 0 }
            .associate { it.videoId to (it.skipCount.toFloat() / (it.playCount + it.skipCount)) }
    }
    
    /**
     * Gets the minimum listening history count required for recommendations.
     */
    suspend fun hasEnoughHistory(minCount: Int = 5): Boolean = withContext(AppDispatchers.Database) {
        historyDao.getAllHistory().size >= minCount
    }
}
