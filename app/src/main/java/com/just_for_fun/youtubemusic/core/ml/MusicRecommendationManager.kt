package com.just_for_fun.youtubemusic.core.ml

import android.content.Context
import com.just_for_fun.youtubemusic.core.chaquopy.ChaquopyMusicAnalyzer
import com.just_for_fun.youtubemusic.core.data.local.MusicDatabase
import com.just_for_fun.youtubemusic.core.ml.agents.*
import com.just_for_fun.youtubemusic.core.ml.models.*
import com.just_for_fun.youtubemusic.core.utils.VectorDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.Calendar

/**
 * Manager that orchestrates the recommendation pipeline.
 * It coordinates several agents (Statistical, Collaborative, Fusion) and uses the ChaquopyPython
 * analyzer for additional ML-based scoring. Responsible for training models and generating
 * final quick picks presented in the UI.
 */
class MusicRecommendationManager(private val context: Context) {

    private val database = MusicDatabase.getDatabase(context)
    private val vectorDb = VectorDatabase()

    private val statisticalAgent = StatisticalAgent()
    private val collaborativeAgent = CollaborativeFilteringAgent(vectorDb)
    private val fusionAgent = FusionAgent()
    private val recommendationAgent = RecommendationAgent()
    private val chaquopyAnalyzer = ChaquopyMusicAnalyzer.getInstance(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * Generate quick picks for the home screen
     */
    suspend fun generateQuickPicks(count: Int = 20): QuickPicksResult {
        return withContext(Dispatchers.Default) {
            // Get all songs
            val allSongs = database.songDao().getAllSongs().first()
            if (allSongs.isEmpty()) {
                return@withContext QuickPicksResult(emptyList(), "1.0.0")
            }

            // Get user's listening history
            val recentHistory = database.listeningHistoryDao().getRecentHistory(100).first()
            val userPreferences = database.userPreferenceDao().getTopPreferences(50).first()

            // If user has no listening history yet, do not generate recommendations
            if (recentHistory.isEmpty()) {
                return@withContext QuickPicksResult(emptyList(), "1.0.0")
            }

            // Extract features for each song
            val songFeaturesList = allSongs.map { song ->
                extractSongFeatures(song.id, recentHistory, userPreferences)
            }

            // Process in parallel with all agents
            val results = songFeaturesList.map { songFeatures ->
                async {
                    processWithAgents(songFeatures, songFeaturesList)
                }
            }.awaitAll()

            // Generate final recommendations
            recommendationAgent.generateQuickPicks(results, count)
        }
    }

    /**
     * Train models based on user history
     */
    suspend fun trainModels() {
        withContext(Dispatchers.IO) {
            try {
                val history = database.listeningHistoryDao().getRecentHistory(200).first()
                val preferences = database.userPreferenceDao().getTopPreferences(100).first()

                if (history.isEmpty()) {
                    return@withContext
                }

                // Build song features from history
                val songFeaturesList = preferences.map { pref ->
                    extractSongFeatures(pref.songId, history, preferences)
                }

                // Train collaborative filtering
                collaborativeAgent.trainFromHistory(songFeaturesList)

                // Train Python ML model
                chaquopyAnalyzer.trainModel(songFeaturesList)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Clear model data (in-memory vectors, python model state, recent recommendations)
     * Call this when user clears app data to ensure all ML state is reset
     */
    suspend fun clearModelData() {
        withContext(Dispatchers.IO) {
            try {
                // Clear vector DB used by collaborative filtering
                vectorDb.clear()

                // Clear recommendation history used for diversity filtering
                recommendationAgent.clearRecommendationHistory()

                // Reset python ML model via Chaquopy analyzer
                val reset = chaquopyAnalyzer.resetModel()
                if (!reset) {
                    // Log if needed - for now, silently continue
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Process song with all agents and fuse results
     */
    private suspend fun processWithAgents(
        songFeatures: SongFeatures,
        userHistory: List<SongFeatures>
    ): RecommendationResult = coroutineScope {
        // Run agents in parallel
        val statisticalResult = async { statisticalAgent.analyze(songFeatures) }
        val collaborativeResult = async { collaborativeAgent.analyze(songFeatures, userHistory) }
        val pythonMlResult = async { chaquopyAnalyzer.getRecommendation(songFeatures) }

        // Fuse results
        fusionAgent.fuseRecommendations(
            statisticalResult.await(),
            collaborativeResult.await(),
            pythonMlResult.await()
        )
    }

    /**
     * Extract features from song based on listening history
     */
    private suspend fun extractSongFeatures(
        songId: String,
        history: List<com.just_for_fun.youtubemusic.core.data.local.entities.ListeningHistory>,
        preferences: List<com.just_for_fun.youtubemusic.core.data.local.entities.UserPreference>
    ): SongFeatures {
        val song = database.songDao().getSongById(songId)
        val pref = preferences.find { it.songId == songId }
        val songHistory = history.filter { it.songId == songId }

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return SongFeatures(
            songId = songId,
            playFrequency = (pref?.playCount ?: 0) / 100.0,
            avgCompletionRate = pref?.avgCompletionRate?.toDouble() ?: 0.5,
            skipRate = if (pref != null && pref.playCount > 0) {
                pref.skipCount.toDouble() / pref.playCount
            } else 0.0,
            recencyScore = calculateRecencyScore(pref?.lastPlayed ?: 0L),
            timeOfDayMatch = calculateTimeOfDayMatch(songHistory, currentHour),
            genreAffinity = calculateGenreAffinity(song?.genre, preferences),
            artistAffinity = calculateArtistAffinity(song?.artist, preferences),
            consecutivePlays = calculateConsecutivePlays(songId, history),
            sessionContext = calculateSessionContext(songId, history)
        )
    }

    private fun calculateRecencyScore(lastPlayed: Long): Double {
        if (lastPlayed == 0L) return 0.0
        val currentTime = System.currentTimeMillis()
        val daysSince = (currentTime - lastPlayed) / (24 * 60 * 60 * 1000)
        return 1.0 / (1.0 + daysSince / 7.0) // Decay over weeks
    }

    private fun calculateTimeOfDayMatch(
        songHistory: List<com.just_for_fun.youtubemusic.core.data.local.entities.ListeningHistory>,
        currentHour: Int
    ): Double {
        if (songHistory.isEmpty()) return 0.5

        val hourCounts = songHistory.groupBy { it.timeOfDay }
            .mapValues { it.value.size }

        val currentHourCount = hourCounts[currentHour] ?: 0
        val maxCount = hourCounts.values.maxOrNull() ?: 1

        return currentHourCount.toDouble() / maxCount
    }

    private suspend fun calculateGenreAffinity(
        genre: String?,
        preferences: List<com.just_for_fun.youtubemusic.core.data.local.entities.UserPreference>
    ): Double {
        if (genre == null) return 0.5

        // Count plays for same genre
        val genrePlays = preferences.sumOf { pref ->
            val song = database.songDao().getSongById(pref.songId)
            if (song?.genre == genre) pref.playCount else 0
        }

        val totalPlays = preferences.sumOf { it.playCount }
        return if (totalPlays > 0) genrePlays.toDouble() / totalPlays else 0.5
    }

    private suspend fun calculateArtistAffinity(
        artist: String?,
        preferences: List<com.just_for_fun.youtubemusic.core.data.local.entities.UserPreference>
    ): Double {
        if (artist == null) return 0.5

        val artistPlays = preferences.sumOf { pref ->
            val song = database.songDao().getSongById(pref.songId)
            if (song?.artist == artist) pref.playCount else 0
        }

        val totalPlays = preferences.sumOf { it.playCount }
        return if (totalPlays > 0) artistPlays.toDouble() / totalPlays else 0.5
    }

    private fun calculateConsecutivePlays(
        songId: String,
        history: List<com.just_for_fun.youtubemusic.core.data.local.entities.ListeningHistory>
    ): Double {
        var maxConsecutive = 0
        var currentConsecutive = 0

        history.sortedByDescending { it.playTimestamp }.forEach { item ->
            if (item.songId == songId) {
                currentConsecutive++
                maxConsecutive = maxOf(maxConsecutive, currentConsecutive)
            } else {
                currentConsecutive = 0
            }
        }

        return minOf(maxConsecutive / 5.0, 1.0)
    }

    private fun calculateSessionContext(
        songId: String,
        history: List<com.just_for_fun.youtubemusic.core.data.local.entities.ListeningHistory>
    ): Double {
        // Check if song was played in recent session (last 30 minutes)
        val thirtyMinutesAgo = System.currentTimeMillis() - 30 * 60 * 1000
        val recentSession = history.filter { it.playTimestamp > thirtyMinutesAgo }

        return if (recentSession.any { it.songId == songId }) 0.8 else 0.3
    }

    fun cleanup() {
        scope.cancel()
    }
}