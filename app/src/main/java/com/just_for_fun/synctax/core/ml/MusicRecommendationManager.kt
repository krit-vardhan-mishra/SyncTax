package com.just_for_fun.synctax.core.ml

import android.content.Context
import com.just_for_fun.synctax.core.chaquopy.ChaquopyMusicAnalyzer
import com.just_for_fun.synctax.core.data.local.MusicDatabase
import com.just_for_fun.synctax.core.ml.agents.*
import com.just_for_fun.synctax.core.ml.models.*
import com.just_for_fun.synctax.core.utils.VectorDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.schabi.newpipe.extractor.NewPipe.init
import java.io.File
import java.util.Calendar

/**
 * Manager that orchestrates the recommendation pipeline.
 * It coordinates several agents (Statistical, Collaborative, Fusion) and uses the ChaquopyPython
 * analyzer for additional ML-based scoring. Responsible for training models and generating
 * final quick picks presented in the UI.
 */
class MusicRecommendationManager(private val context: Context) {

    private val database = MusicDatabase.getDatabase(context)
    private val vectorDb = VectorDatabase(context)

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

            // If user has no songs, do not generate recommendations
            if (allSongs.isEmpty()) {
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

                // Delete persisted model files
                val modelFile = File(context.filesDir, "ml_model.json")
                if (modelFile.exists()) {
                    modelFile.delete()
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
        history: List<com.just_for_fun.synctax.core.data.local.entities.ListeningHistory>,
        preferences: List<com.just_for_fun.synctax.core.data.local.entities.UserPreference>
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
            sessionContext = calculateSessionContext(songId, history),
            durationScore = calculateDurationScore(song?.duration ?: 0L, preferences),
            albumAffinity = calculateAlbumAffinity(song?.album, preferences),
            releaseYearScore = calculateReleaseYearScore(song?.releaseYear, preferences),
            songPopularity = calculateSongPopularity(pref, preferences),
            tempoEnergy = estimateTempoEnergy(song?.duration ?: 0L, song?.title ?: "")
        )
    }

    private fun calculateRecencyScore(lastPlayed: Long): Double {
        if (lastPlayed == 0L) return 0.0
        val currentTime = System.currentTimeMillis()
        val daysSince = (currentTime - lastPlayed) / (24 * 60 * 60 * 1000)
        return 1.0 / (1.0 + daysSince / 7.0) // Decay over weeks
    }

    private fun calculateTimeOfDayMatch(
        songHistory: List<com.just_for_fun.synctax.core.data.local.entities.ListeningHistory>,
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
        preferences: List<com.just_for_fun.synctax.core.data.local.entities.UserPreference>
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
        preferences: List<com.just_for_fun.synctax.core.data.local.entities.UserPreference>
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
        history: List<com.just_for_fun.synctax.core.data.local.entities.ListeningHistory>
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
        history: List<com.just_for_fun.synctax.core.data.local.entities.ListeningHistory>
    ): Double {
        // Check if song was played in recent session (last 30 minutes)
        val thirtyMinutesAgo = System.currentTimeMillis() - 30 * 60 * 1000
        val recentSession = history.filter { it.playTimestamp > thirtyMinutesAgo }

        return if (recentSession.any { it.songId == songId }) 0.8 else 0.3
    }

    private suspend fun calculateDurationScore(
        duration: Long,
        preferences: List<com.just_for_fun.synctax.core.data.local.entities.UserPreference>
    ): Double {
        if (duration == 0L || preferences.isEmpty()) return 0.5
        
        // Calculate average preferred duration from user's most played songs
        val topSongs = preferences.sortedByDescending { it.playCount }.take(20)
        val durations = topSongs.mapNotNull { pref ->
            database.songDao().getSongById(pref.songId)?.duration
        }
        
        if (durations.isEmpty()) return 0.5
        
        val avgDuration = durations.average()
        val stdDev = kotlin.math.sqrt(durations.map { (it - avgDuration).let { diff -> diff * diff } }.average())
        
        // Calculate how close this song's duration is to user's preference
        val difference = kotlin.math.abs(duration - avgDuration)
        return kotlin.math.max(0.0, 1.0 - (difference / (stdDev * 2)))
    }

    private suspend fun calculateAlbumAffinity(
        album: String?,
        preferences: List<com.just_for_fun.synctax.core.data.local.entities.UserPreference>
    ): Double {
        if (album.isNullOrEmpty()) return 0.5

        val albumPlays = preferences.sumOf { pref ->
            val song = database.songDao().getSongById(pref.songId)
            if (song?.album == album) pref.playCount else 0
        }

        val totalPlays = preferences.sumOf { it.playCount }
        return if (totalPlays > 0) albumPlays.toDouble() / totalPlays else 0.5
    }

    private suspend fun calculateReleaseYearScore(
        releaseYear: Int?,
        preferences: List<com.just_for_fun.synctax.core.data.local.entities.UserPreference>
    ): Double {
        if (releaseYear == null || preferences.isEmpty()) return 0.5
        
        // Analyze user's preferred eras
        val yearCounts = mutableMapOf<Int, Int>()
        preferences.forEach { pref ->
            val song = database.songDao().getSongById(pref.songId)
            song?.releaseYear?.let { year ->
                yearCounts[year] = yearCounts.getOrDefault(year, 0) + pref.playCount
            }
        }
        
        if (yearCounts.isEmpty()) return 0.5
        
        // Find the decade with most plays
        val decadeCounts = yearCounts.entries.groupBy { it.key / 10 * 10 }
            .mapValues { it.value.sumOf { entry -> entry.value } }
        
        val songDecade = releaseYear / 10 * 10
        val maxDecadePlays = decadeCounts.values.maxOrNull() ?: 1
        val songDecadePlays = decadeCounts[songDecade] ?: 0
        
        return songDecadePlays.toDouble() / maxDecadePlays
    }

    private fun calculateSongPopularity(
        pref: com.just_for_fun.synctax.core.data.local.entities.UserPreference?,
        allPreferences: List<com.just_for_fun.synctax.core.data.local.entities.UserPreference>
    ): Double {
        if (pref == null || allPreferences.isEmpty()) return 0.0
        
        val maxPlays = allPreferences.maxOfOrNull { it.playCount } ?: 1
        return pref.playCount.toDouble() / maxPlays
    }

    private fun estimateTempoEnergy(duration: Long, title: String): Double {
        // Estimate energy based on duration and title keywords
        // Shorter songs tend to be more energetic
        val durationSeconds = duration / 1000.0
        var energy = when {
            durationSeconds < 180 -> 0.8 // Short songs, likely energetic
            durationSeconds < 240 -> 0.6 // Medium
            durationSeconds < 300 -> 0.5 // Average
            else -> 0.4 // Longer songs, potentially slower
        }
        
        // Boost energy for certain keywords in title
        val energeticKeywords = listOf(
            "remix", "dance", "edm", "trap", "bass", "drop", "banger",
            "party", "club", "hype", "fire", "lit", "fast"
        )
        val calmKeywords = listOf(
            "acoustic", "piano", "ballad", "slow", "soft", "calm",
            "meditation", "sleep", "lo-fi", "lofi", "chill"
        )
        
        val lowerTitle = title.lowercase()
        if (energeticKeywords.any { lowerTitle.contains(it) }) {
            energy = minOf(1.0, energy + 0.2)
        }
        if (calmKeywords.any { lowerTitle.contains(it) }) {
            energy = maxOf(0.0, energy - 0.2)
        }
        
        return energy
    }

    fun cleanup() {
        scope.cancel()
    }

    /**
     * Check for deleted songs and remove them from database and ML models.
     * Should be called periodically.
     */
    suspend fun cleanupDeletedSongs() {
        withContext(Dispatchers.IO) {
            try {
                val allSongs = database.songDao().getAllSongs().first()
                val deletedSongs = mutableListOf<String>()

                allSongs.forEach { song ->
                    // Check if file exists
                    // Note: This assumes song.filePath is an absolute path or accessible URI
                    try {
                        val file = java.io.File(song.filePath)
                        if (!file.exists() && !song.id.startsWith("online:")) {
                            deletedSongs.add(song.id)
                        }
                    } catch (e: Exception) {
                        // If filePath is invalid or not a file path, skip
                    }
                }

                if (deletedSongs.isNotEmpty()) {
                    // Remove from database
                    database.songDao().deleteSongsByIds(deletedSongs)
                    
                    // Remove from listening history
                    database.listeningHistoryDao().deleteHistoryForSongs(deletedSongs)
                    
                    // Remove from user preferences
                    database.userPreferenceDao().deletePreferencesForSongs(deletedSongs)
                    
                    // Clear vector DB to ensure consistency
                    vectorDb.clear()
                    
                    // Re-train models to reflect changes
                    trainModels()
                }
                
                // Also scan assets for enhancement
                enhanceWithAssetSongs()
                
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun enhanceWithAssetSongs() {
        // In a real implementation, we would use AssetManager to list files in "songs"
        // and extract metadata. Since we are in a pure class without direct AssetManager access 
        // (unless passed context), we'll use the context passed in constructor.
        try {
            val assetManager = context.assets
            val songs = assetManager.list("songs")
            
            songs?.forEach { filename ->
                // Logic to parse filename and enhance training
                // e.g. "Artist - Title.mp3"
                // This is a placeholder for the requested feature
                val parts = filename.split(" - ")
                if (parts.size >= 2) {
                    val artist = parts[0]
                    // We could boost artist affinity for these "featured" songs
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    init {
        // Start periodic cleanup job
        scope.launch {
            while (isActive) {
                cleanupDeletedSongs()
                delay(15 * 60 * 1000) // 15 minutes
            }
        }
    }
}