package com.just_for_fun.synctax.presentation.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.synctax.core.dispatcher.AppDispatchers
import com.just_for_fun.synctax.core.network.OnlineSearchResult
import com.just_for_fun.synctax.core.network.YouTubeInnerTubeClient
import com.just_for_fun.synctax.core.service.ListeningAnalyticsService
import com.just_for_fun.synctax.core.service.RecommendationService
import com.just_for_fun.synctax.data.local.MusicDatabase
import com.just_for_fun.synctax.data.local.entities.RecommendationInteraction
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min

/**
 * ViewModel for handling recommendation UI state and interactions.
 * All heavy operations run on appropriate background dispatchers.
 */
class RecommendationViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "RecommendationViewModel"
        private const val BATCH_SIZE = 15
    }
    
    // Database and services
    private val database = MusicDatabase.getDatabase(application)
    private val historyDao = database.onlineListeningHistoryDao()
    private val cacheDao = database.recommendationCacheDao()
    private val interactionDao = database.recommendationInteractionDao()
    
    private val ytClient = YouTubeInnerTubeClient()
    private val analyticsService = ListeningAnalyticsService(historyDao)
    private val recommendationService = RecommendationService(
        analyticsService, ytClient, historyDao, cacheDao
    )
    
    // UI State
    private val _recommendations = MutableStateFlow<RecommendationService.RecommendationResult?>(null)
    val recommendations: StateFlow<RecommendationService.RecommendationResult?> = _recommendations.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    private val _currentShuffleBatch = MutableStateFlow<List<OnlineSearchResult>>(emptyList())
    val currentShuffleBatch: StateFlow<List<OnlineSearchResult>> = _currentShuffleBatch.asStateFlow()
    
    private val _hasEnoughHistory = MutableStateFlow(false)
    val hasEnoughHistory: StateFlow<Boolean> = _hasEnoughHistory.asStateFlow()
    
    // Internal state for shuffle functionality
    private var allAvailableSongs = listOf<OnlineSearchResult>()
    private var currentBatchIndex = 0
    
    init {
        checkHistoryAndLoad()
    }
    
    /**
     * Checks if user has enough listening history and loads recommendations.
     */
    private fun checkHistoryAndLoad() {
        viewModelScope.launch(AppDispatchers.Database) {
            val hasHistory = analyticsService.hasEnoughHistory(3)
            _hasEnoughHistory.value = hasHistory
            
            if (hasHistory) {
                withContext(AppDispatchers.Network) {
                    loadRecommendations()
                }
            }
        }
    }
    
    /**
     * Loads recommendations from cache or generates new ones.
     * Runs on appropriate background dispatchers.
     */
    fun loadRecommendations() {
        if (_isLoading.value) return
        
        viewModelScope.launch(AppDispatchers.Network) {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d(TAG, "Loading recommendations...")
                val result = recommendationService.generateRecommendations()
                _recommendations.value = result
                
                // Prepare shuffle pool
                allAvailableSongs = (result.artistBased + result.similarSongs + 
                                    result.discovery + result.trending)
                    .distinctBy { it.id }
                    .shuffled()
                currentBatchIndex = 0
                _currentShuffleBatch.value = emptyList()
                
                Log.d(TAG, "Loaded ${allAvailableSongs.size} total recommendations")
            } catch (e: Exception) {
                _error.value = "Failed to load recommendations: ${e.message}"
                Log.e(TAG, "Error loading recommendations", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Forces a refresh of recommendations from API.
     */
    fun refreshRecommendations() {
        if (_isLoading.value) return
        
        viewModelScope.launch(AppDispatchers.Network) {
            _isLoading.value = true
            _error.value = null
            
            try {
                Log.d(TAG, "Refreshing recommendations...")
                val result = recommendationService.generateRecommendations(forceRefresh = true)
                _recommendations.value = result
                
                // Refresh shuffle pool
                allAvailableSongs = (result.artistBased + result.similarSongs + 
                                    result.discovery + result.trending)
                    .distinctBy { it.id }
                    .shuffled()
                currentBatchIndex = 0
                _currentShuffleBatch.value = emptyList()
                
                Log.d(TAG, "Refreshed ${allAvailableSongs.size} recommendations")
            } catch (e: Exception) {
                _error.value = "Failed to refresh recommendations: ${e.message}"
                Log.e(TAG, "Error refreshing recommendations", e)
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Generates personalized recommendations based on user inputs.
     */
    fun generateUserInputRecommendations(userInputs: com.just_for_fun.synctax.presentation.screens.UserRecommendationInputs) {
        if (_isLoading.value) return

        viewModelScope.launch(AppDispatchers.Network) {
            _isLoading.value = true
            _error.value = null

            try {
                Log.d(TAG, "Generating user-input-based recommendations...")
                val result = recommendationService.generateUserInputRecommendations(userInputs)
                _recommendations.value = result

                // Prepare shuffle pool with user-input recommendations
                allAvailableSongs = (result.artistBased + result.similarSongs +
                                    result.discovery + result.trending)
                    .distinctBy { it.id }
                    .shuffled()
                currentBatchIndex = 0
                _currentShuffleBatch.value = emptyList()

                Log.d(TAG, "Generated ${allAvailableSongs.size} user-input-based recommendations")
            } catch (e: Exception) {
                _error.value = "Failed to generate personalized recommendations: ${e.message}"
                Log.e(TAG, "Error generating user-input recommendations", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Shuffles the current batch of recommendations.
     */
    fun shuffleRecommendations() {
        if (allAvailableSongs.isEmpty()) {
            loadNextShuffleBatch()
        } else {
            _currentShuffleBatch.value = _currentShuffleBatch.value.shuffled()
        }
    }
    
    /**
     * Loads the next batch of shuffled songs.
     */
    fun loadNextShuffleBatch() {
        if (allAvailableSongs.isEmpty()) return
        
        val startIndex = currentBatchIndex
        val endIndex = min(startIndex + BATCH_SIZE, allAvailableSongs.size)
        
        if (startIndex >= allAvailableSongs.size) {
            // Wrap around - reshuffle and start over
            allAvailableSongs = allAvailableSongs.shuffled()
            currentBatchIndex = 0
            loadNextShuffleBatch()
            return
        }
        
        val newBatch = allAvailableSongs.subList(startIndex, endIndex)
        _currentShuffleBatch.value = newBatch
        currentBatchIndex = endIndex
    }
    
    /**
     * Tracks user interaction with a recommendation.
     * Runs on database dispatcher.
     */
    fun trackInteraction(songId: String, action: String, source: String) {
        viewModelScope.launch(AppDispatchers.Database) {
            try {
                val interaction = RecommendationInteraction(
                    recommendationId = source,
                    songId = songId,
                    action = action,
                    source = source
                )
                interactionDao.insert(interaction)
                Log.d(TAG, "Tracked interaction: $action for song $songId from $source")
            } catch (e: Exception) {
                Log.e(TAG, "Error tracking interaction", e)
            }
        }
    }
    
    /**
     * Clears any error state.
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Gets a mixed list of 9 recommendations for the home screen grid.
     */
    fun getGridRecommendations(): List<OnlineSearchResult> {
        val result = _recommendations.value ?: return emptyList()
        
        return (result.artistBased.take(3) + 
                result.similarSongs.take(3) + 
                result.discovery.take(3))
            .shuffled()
            .take(9)
    }
    
    /**
     * Gets the recommendation reason for a song based on which category it's from.
     */
    fun getRecommendationReason(song: OnlineSearchResult): String {
        val result = _recommendations.value ?: return "Recommended for you"
        
        return when {
            result.artistBased.any { it.id == song.id } -> "Based on artists you love"
            result.discovery.any { it.id == song.id } -> "Discover new music"
            result.similarSongs.any { it.id == song.id } -> "Similar to songs you've enjoyed"
            result.trending.any { it.id == song.id } -> "Trending now"
            else -> "Recommended for you"
        }
    }
}
