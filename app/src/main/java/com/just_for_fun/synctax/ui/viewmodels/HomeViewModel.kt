package com.just_for_fun.synctax.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.core.data.repository.MusicRepository
import com.just_for_fun.synctax.core.ml.MusicRecommendationManager
import com.just_for_fun.synctax.core.data.cache.ListenAgainManager
import com.just_for_fun.synctax.core.ml.models.RecommendationResult
import com.just_for_fun.synctax.core.network.OnlineSearchManager
import com.just_for_fun.synctax.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.schabi.newpipe.extractor.timeago.patterns.it

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val recommendationManager = MusicRecommendationManager(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val onlineManager = OnlineSearchManager()
    private val listenAgainManager = ListenAgainManager(getApplication())
    
    private var lastQuickPicksRefresh = 0L
    private val quickPicksRefreshInterval = 15 * 60 * 1000L // 15 minutes in milliseconds

    init {
        loadData()
        // Auto-scan on app open to keep library in sync with device
        scanMusic()
        observeListeningHistoryForTraining()
        observeListenAgain()
        // Start periodic refresh for deleted songs check
        startPeriodicRefresh()
    }

    private fun observeListenAgain() {
        viewModelScope.launch {
            listenAgainManager.listenAgain.collect { list ->
                _uiState.value = _uiState.value.copy(listenAgain = list)
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            try {
                // Collect songs
                repository.getAllSongs().collect { songs ->
                    _uiState.value = _uiState.value.copy(
                        allSongs = songs,
                        isLoading = false
                    )

                    // Generate recommendations if songs available
                    if (songs.isNotEmpty()) {
                        generateQuickPicks()
                    }
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }

    fun scanMusic() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)

            try {
                val songs = repository.scanDeviceMusic()
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    scanComplete = true
                )
                // Refresh listen again cache after scanning
                listenAgainManager.refresh()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = e.message
                )
            }
        }
    }

    fun searchOnline(query: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingOnline = true)
            try {
                val apiKey = UserPreferences(getApplication()).getYouTubeApiKey()
                val results = onlineManager.search(query, apiKey = apiKey)
                _uiState.value = _uiState.value.copy(
                    isSearchingOnline = false,
                    onlineSearchResults = results
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSearchingOnline = false,
                    error = e.message
                )
            }
        }
    }

    suspend fun fetchStreamUrl(videoId: String): String? {
        return try {
            onlineManager.getStreamUrl(videoId)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun generateQuickPicks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isGeneratingRecommendations = true)

            try {
                val result = recommendationManager.generateQuickPicks(20)

                // Map recommendations to songs
                val recommendedSongs = result.recommendations.mapNotNull { rec ->
                    _uiState.value.allSongs.find { it.id == rec.songId }
                }

                _uiState.value = _uiState.value.copy(
                    quickPicks = recommendedSongs,
                    recommendationScores = result.recommendations,
                    isGeneratingRecommendations = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingRecommendations = false,
                    error = e.message
                )
            }
        }
    }

    private fun observeListeningHistoryForTraining() {
        viewModelScope.launch {
            var hasAutoTrained = false
            repository.getRecentHistory(1).collect { history ->
                // If history becomes non-empty and we haven't auto-trained yet, train models
                if (!hasAutoTrained && history.isNotEmpty()) {
                    hasAutoTrained = true
                    _uiState.value = _uiState.value.copy(isTraining = true)
                    try {
                        recommendationManager.trainModels()
                        // After training, regenerate quick picks
                        val result = recommendationManager.generateQuickPicks(20)
                        val recommendedSongs = result.recommendations.mapNotNull { rec ->
                            _uiState.value.allSongs.find { it.id == rec.songId }
                        }
                        _uiState.value = _uiState.value.copy(
                            quickPicks = recommendedSongs,
                            recommendationScores = result.recommendations,
                            isTraining = false,
                            trainingComplete = true
                        )
                    } catch (e: Exception) {
                        _uiState.value = _uiState.value.copy(
                            isTraining = false,
                            error = e.message
                        )
                    }
                }
            }
        }
    }

    fun trainModels() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTraining = true)

            try {
                recommendationManager.trainModels()
                _uiState.value = _uiState.value.copy(
                    isTraining = false,
                    trainingComplete = true
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isTraining = false,
                    error = e.message
                )
            }
        }
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun setSelectedArtist(artist: String, songs: List<Song>) {
        _uiState.value = _uiState.value.copy(
            selectedArtist = artist,
            selectedArtistSongs = songs
        )
    }

    fun setSelectedAlbum(album: String, artist: String, songs: List<Song>) {
        _uiState.value = _uiState.value.copy(
            selectedAlbum = album,
            selectedAlbumArtist = artist,
            selectedAlbumSongs = songs
        )
    }

    private fun startPeriodicRefresh() {
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(quickPicksRefreshInterval)
                
                // Check for deleted songs and cleanup ML data
                cleanupDeletedSongs()
                
                // Refresh Quick Picks if there's listening history
                if (_uiState.value.allSongs.isNotEmpty()) {
                    generateQuickPicks()
                }
            }
        }
    }
    
    private fun cleanupDeletedSongs() {
        viewModelScope.launch {
            try {
                // Get all song IDs from database
                val dbSongIds = _uiState.value.allSongs.map { it.id }.toSet()
                
                // Get all song IDs from listening history and preferences
                val historyIds = repository.getRecentHistory(1000).first().map { it.songId }.toSet()
                val preferenceIds = repository.getTopPreferences(1000).first().map { it.songId }.toSet()
                
                // Find deleted song IDs (in history/preferences but not in songs table)
                val deletedIds = (historyIds + preferenceIds) - dbSongIds
                
                if (deletedIds.isNotEmpty()) {
                    // Remove deleted songs from listening history and preferences
                    repository.cleanupDeletedSongsData(deletedIds.toList())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recommendationManager.cleanup()
    }
}

data class HomeUiState(
    val allSongs: List<Song> = emptyList(),
    val quickPicks: List<Song> = emptyList(),
    val recommendationScores: List<RecommendationResult> = emptyList(),
    val isLoading: Boolean = false,
    val isScanning: Boolean = false,
    val isGeneratingRecommendations: Boolean = false,
    val isTraining: Boolean = false,
    val scanComplete: Boolean = false,
    val trainingComplete: Boolean = false,
    val error: String? = null,
    val selectedArtist: String? = null,
    val selectedArtistSongs: List<Song>? = null,
    val selectedAlbum: String? = null,
    val selectedAlbumArtist: String? = null,
    val selectedAlbumSongs: List<Song>? = null
    ,
    // Online search state (for SearchScreen)
    val isSearchingOnline: Boolean = false,
    val onlineSearchResults: List<com.just_for_fun.synctax.core.network.OnlineSearchResult> = emptyList()
    ,
    // Listen again cache exposed to UI
    val listenAgain: List<Song> = emptyList()
)