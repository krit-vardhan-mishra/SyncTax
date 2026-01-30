package com.just_for_fun.synctax.core.init

import android.content.Context
import android.util.Log
import com.just_for_fun.synctax.core.ml.MusicRecommendationManager
import com.just_for_fun.synctax.core.network.YouTubeInnerTubeClient
import com.just_for_fun.synctax.core.service.ListeningAnalyticsService
import com.just_for_fun.synctax.core.service.RecommendationService
import com.just_for_fun.synctax.data.local.MusicDatabase
import com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory
import com.just_for_fun.synctax.data.local.entities.OnlineSearchHistory
import com.just_for_fun.synctax.data.local.entities.Playlist
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.data.repository.MusicRepository
import com.just_for_fun.synctax.data.repository.PlaylistRepository
import com.just_for_fun.synctax.presentation.viewmodels.ModelTrainingStatus
import com.just_for_fun.synctax.presentation.viewmodels.SongPlayCount
import com.just_for_fun.synctax.presentation.viewmodels.TrainingStatistics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Singleton that manages app initialization during splash screen.
 * Pre-computes data normally loaded by HomeViewModel, so the home screen appears instantly responsive.
 */
object AppInitializer {
    
    private const val TAG = "AppInitializer"
    
    /**
     * Represents the current initialization progress
     */
    data class InitProgress(
        val phase: String,
        val progress: Float, // 0f to 1f
        val isComplete: Boolean = false
    )
    
    /**
     * Pre-computed data for HomeViewModel to consume
     */
    /**
     * Minimum splash screen duration in milliseconds to ensure smooth progress bar animation
     */
    private const val MIN_SPLASH_DURATION_MS = 2000L
    
    data class InitializedData(
        val songs: List<Song>,
        val mostPlayedSongs: List<Song>,
        val listenAgain: List<Song>,
        val quickAccessSongs: List<Song>,
        val favoriteSongs: List<Song>,
        val searchHistory: List<OnlineSearchHistory>,
        val onlineHistory: List<OnlineListeningHistory>,
        val savedPlaylists: List<Playlist>,
        val trainingStatistics: TrainingStatistics?,
        val modelStatus: ModelTrainingStatus?,
        val trainingDataSize: Int,
        // New fields for preloading
        val recommendations: RecommendationService.RecommendationResult?,
        val lastPlayedSong: UserPreferences.LastPlayedSong?,
        val lastPlayedStreamUrl: String?
    )
    
    private val _progress = MutableStateFlow(InitProgress("Initializing...", 0f))
    val progress: Flow<InitProgress> = _progress.asStateFlow()
    
    private var _initializedData: InitializedData? = null
    private var _isInitialized = false
    
    /**
     * Check if initialization has completed
     */
    fun isInitialized(): Boolean = _isInitialized
    
    /**
     * Get the pre-computed data. Returns null if not yet initialized.
     * After consuming, call clearData() to free memory.
     */
    fun getInitializedData(): InitializedData? = _initializedData
    
    /**
     * Clear the pre-computed data after HomeViewModel has consumed it.
     * This frees up memory since HomeViewModel will manage the data from then on.
     */
    fun clearData() {
        _initializedData = null
        Log.d(TAG, "Cleared pre-computed data")
    }
    
    /**
     * Reset the initializer state. Called if we need to re-initialize.
     */
    fun reset() {
        _initializedData = null
        _isInitialized = false
        _progress.value = InitProgress("Initializing...", 0f)
        Log.d(TAG, "Reset initializer state")
    }
    
    /**
     * Run the initialization process. This should be called from the splash screen.
     * Updates progress as each phase completes.
     * 
     * @param context Application context
     * @return Flow of InitProgress updates
     */
    suspend fun initialize(context: Context): Flow<InitProgress> {
        if (_isInitialized) {
            Log.d(TAG, "Already initialized, skipping")
            _progress.value = InitProgress("Ready", 1f, isComplete = true)
            return progress
        }
        
        withContext(Dispatchers.IO) {
            try {
                val startTime = System.currentTimeMillis()
                val repository = MusicRepository(context)
                val playlistRepository = PlaylistRepository(context)
                val userPreferences = UserPreferences(context)
                val recommendationManager = MusicRecommendationManager(context)
                
                // Phase 1: Scan and load songs (40% of progress)
                updateProgress("Scanning music library...", 0.05f)
                val scanPaths = userPreferences.scanPaths.value
                val songs = repository.scanDeviceMusic(scanPaths)
                updateProgress("Loaded ${songs.size} songs", 0.40f)
                Log.d(TAG, "Phase 1 complete: Loaded ${songs.size} songs")
                
                // Phase 2: Load most played songs (10%)
                updateProgress("Loading most played...", 0.45f)
                val mostPlayedSongs = repository.getMostPlayedSongs(10)
                Log.d(TAG, "Phase 2 complete: Loaded ${mostPlayedSongs.size} most played songs")
                
                // Phase 3: Load listen again (recent history) (10%)
                updateProgress("Loading recent history...", 0.50f)
                val recentHistory = repository.getRecentHistory(50).first()
                val recentSongIds = recentHistory.map { it.songId }.distinct()
                val listenAgainList = mutableListOf<Song>()
                for (id in recentSongIds) {
                    val song = repository.getSongById(id)
                    if (song != null) listenAgainList.add(song)
                }
                val listenAgain = if (listenAgainList.size >= 20) {
                    listenAgainList.shuffled().take(20)
                } else {
                    listenAgainList
                }
                Log.d(TAG, "Phase 3 complete: Loaded ${listenAgain.size} listen again songs")
                
                // Phase 4: Generate quick access (random songs) (5%)
                updateProgress("Preparing quick access...", 0.55f)
                val quickAccessSongs = songs.shuffled().take(9)
                Log.d(TAG, "Phase 4 complete: Selected ${quickAccessSongs.size} quick access songs")
                
                // Phase 5: Load favorites (5%)
                updateProgress("Loading favorites...", 0.60f)
                val favoriteSongs = repository.getFavoriteSongs().first()
                Log.d(TAG, "Phase 5 complete: Loaded ${favoriteSongs.size} favorite songs")
                
                // Phase 6: Load search history (5%)
                updateProgress("Loading search history...", 0.65f)
                val searchHistory: List<OnlineSearchHistory> = try {
                    val db = com.just_for_fun.synctax.data.local.MusicDatabase.getDatabase(context)
                    db.onlineSearchHistoryDao().getRecentSearches(50).first()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load search history: ${e.message}")
                    emptyList<OnlineSearchHistory>()
                }
                Log.d(TAG, "Phase 6 complete: Loaded ${searchHistory.size} search history items")
                
                // Phase 7: Load online listening history (5%)
                updateProgress("Loading online history...", 0.70f)
                val onlineHistory: List<OnlineListeningHistory> = try {
                    val db = com.just_for_fun.synctax.data.local.MusicDatabase.getDatabase(context)
                    db.onlineListeningHistoryDao().getRecentHistory(10)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load online history: ${e.message}")
                    emptyList<OnlineListeningHistory>()
                }
                Log.d(TAG, "Phase 7 complete: Loaded ${onlineHistory.size} online history items")
                
                // Phase 8: Load saved playlists (5%)
                updateProgress("Loading playlists...", 0.75f)
                val savedPlaylists = try {
                    playlistRepository.getAllPlaylists().first()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load playlists: ${e.message}")
                    emptyList<Playlist>()
                }
                Log.d(TAG, "Phase 8 complete: Loaded ${savedPlaylists.size} playlists")
                
                // Phase 9: Load training statistics (10%)
                updateProgress("Loading statistics...", 0.85f)
                val trainingStatistics = try {
                    val history = repository.getRecentHistory(1000).first()
                    val preferences = repository.getTopPreferences(200).first()
                    
                    val totalPlays = history.size
                    val uniqueSongsPlayed = history.distinctBy { it.songId }.size
                    val averageCompletionRate = if (history.isNotEmpty()) {
                        history.map { it.completionRate }.average().toFloat()
                    } else 0f
                    
                    val hourCounts = history.groupBy { it.timeOfDay }.mapValues { it.value.size }
                    val dayCounts = history.groupBy { it.dayOfWeek }.mapValues { it.value.size }
                    val mostActiveHour = hourCounts.maxByOrNull { it.value }?.key ?: 0
                    val mostActiveDay = dayCounts.maxByOrNull { it.value }?.key ?: 0
                    
                    val topSongs = preferences
                        .sortedByDescending { it.playCount }
                        .take(5)
                        .mapNotNull { pref ->
                            repository.getSongById(pref.songId)?.let { song ->
                                SongPlayCount(
                                    songId = pref.songId,
                                    title = song.title,
                                    artist = song.artist,
                                    playCount = pref.playCount
                                )
                            }
                        }
                    
                    val listeningPatterns = mutableMapOf<String, Int>()
                    history.forEach { entry ->
                        val key = "${entry.timeOfDay}:${entry.dayOfWeek}"
                        listeningPatterns[key] = listeningPatterns.getOrDefault(key, 0) + 1
                    }
                    
                    TrainingStatistics(
                        totalPlays = totalPlays,
                        uniqueSongsPlayed = uniqueSongsPlayed,
                        averageCompletionRate = averageCompletionRate,
                        mostActiveHour = mostActiveHour,
                        mostActiveDay = mostActiveDay,
                        topSongs = topSongs,
                        listeningPatterns = listeningPatterns
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load training statistics: ${e.message}")
                    null
                }
                Log.d(TAG, "Phase 9 complete: Loaded training statistics")
                
                // Phase 10: Load model status (3%)
                updateProgress("Checking models...", 0.70f)
                val modelStatus = try {
                    val status = recommendationManager.getModelStatus()
                    ModelTrainingStatus(
                        statisticalAgentTrained = true,
                        collaborativeAgentTrained = status.isTrained,
                        pythonModelTrained = status.isTrained,
                        fusionAgentReady = true,
                        lastTrainingTime = System.currentTimeMillis(),
                        modelVersion = "1.0.0"
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load model status: ${e.message}")
                    null
                }
                Log.d(TAG, "Phase 10 complete: Loaded model status")
                
                // Phase 11: Load online recommendations (15%)
                updateProgress("Loading recommendations...", 0.75f)
                val recommendations = try {
                    val db = MusicDatabase.getDatabase(context)
                    val historyDao = db.onlineListeningHistoryDao()
                    val cacheDao = db.recommendationCacheDao()
                    val analyticsService = ListeningAnalyticsService(historyDao)
                    val ytClient = YouTubeInnerTubeClient()
                    val recommendationService = RecommendationService(
                        analyticsService, ytClient, historyDao, cacheDao
                    )
                    
                    // Check if user has enough history before loading
                    val hasHistory = analyticsService.hasEnoughHistory(3)
                    if (hasHistory) {
                        recommendationService.generateRecommendations()
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to load recommendations: ${e.message}")
                    null
                }
                Log.d(TAG, "Phase 11 complete: Loaded recommendations (${recommendations != null})")
                
                // Phase 12: Preload last played song stream (10%)
                updateProgress("Preloading last song...", 0.90f)
                val playerPreferences = com.just_for_fun.synctax.data.preferences.PlayerPreferences(context)
                val onlineSongState = playerPreferences.getOnlineSongState()
                var lastPlayedStreamUrl: String? = null
                
                // Also get last played song info for InitializedData
                val lastPlayedSong = if (playerPreferences.isOnlineSong() && onlineSongState != null) {
                    UserPreferences.LastPlayedSong(
                        songId = "online:${onlineSongState.videoId}",
                        isOnline = true,
                        videoId = onlineSongState.videoId,
                        title = onlineSongState.title,
                        artist = onlineSongState.artist,
                        thumbnailUrl = onlineSongState.thumbnailUrl,
                        watchUrl = onlineSongState.watchUrl
                    )
                } else {
                    userPreferences.getLastPlayedSong()
                }
                
                if (onlineSongState != null && onlineSongState.videoId.isNotEmpty()) {
                    updateProgress("Fetching stream...", 0.92f)
                    lastPlayedStreamUrl = try {
                        val ytClient = YouTubeInnerTubeClient()
                        ytClient.getStreamUrl(onlineSongState.videoId)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to preload stream URL: ${e.message}")
                        null
                    }
                    Log.d(TAG, "Phase 12 complete: Preloaded stream URL for ${onlineSongState.title}")
                } else {
                    Log.d(TAG, "Phase 12 complete: No online song to preload")
                }
                
                // Store the pre-computed data
                _initializedData = InitializedData(
                    songs = songs,
                    mostPlayedSongs = mostPlayedSongs,
                    listenAgain = listenAgain,
                    quickAccessSongs = quickAccessSongs,
                    favoriteSongs = favoriteSongs,
                    searchHistory = searchHistory,
                    onlineHistory = onlineHistory,
                    savedPlaylists = savedPlaylists,
                    trainingStatistics = trainingStatistics,
                    modelStatus = modelStatus,
                    trainingDataSize = recentHistory.size,
                    recommendations = recommendations,
                    lastPlayedSong = lastPlayedSong,
                    lastPlayedStreamUrl = lastPlayedStreamUrl
                )

                // Phase 13: Schedule library update checks
                updateProgress("Setting up updates...", 0.95f)
                try {
                    com.just_for_fun.synctax.core.service.LibraryUpdateCheckWorker.schedulePeriodicCheck(context)
                    Log.d(TAG, "Phase 13 complete: Scheduled library update checks")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to schedule library update checks: ${e.message}")
                }

                // Phase 14: Ensure minimum splash duration for smooth progress bar
                updateProgress("Almost ready...", 0.97f)
                updateProgress("Almost ready...", 0.95f)
                val elapsed = System.currentTimeMillis() - startTime
                if (elapsed < MIN_SPLASH_DURATION_MS) {
                    val remaining = MIN_SPLASH_DURATION_MS - elapsed
                    // Animate progress smoothly during wait
                    val steps = (remaining / 100).toInt().coerceAtLeast(1)
                    val progressPerStep = (1f - 0.97f) / steps
                    repeat(steps) { i ->
                        delay(remaining / steps)
                        updateProgress("Almost ready...", 0.97f + (progressPerStep * (i + 1)))
                    }
                }
                
                _isInitialized = true
                updateProgress("Ready", 1f, isComplete = true)
                Log.d(TAG, "Initialization complete!")
                
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed: ${e.message}", e)
                // Even on failure, mark as complete so app can continue
                // HomeViewModel will fall back to its normal initialization
                _isInitialized = true
                updateProgress("Ready", 1f, isComplete = true)
            }
        }
        
        return progress
    }
    
    private fun updateProgress(phase: String, progress: Float, isComplete: Boolean = false) {
        _progress.value = InitProgress(phase, progress, isComplete)
    }
}
