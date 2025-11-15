package com.just_for_fun.youtubemusic.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.youtubemusic.core.data.local.entities.Song
import com.just_for_fun.youtubemusic.core.data.repository.MusicRepository
import com.just_for_fun.youtubemusic.core.ml.MusicRecommendationManager
import com.just_for_fun.youtubemusic.core.data.cache.ListenAgainManager
import com.just_for_fun.youtubemusic.core.ml.models.RecommendationResult
import com.just_for_fun.youtubemusic.core.network.OnlineSearchManager
import com.just_for_fun.youtubemusic.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MusicRepository(application)
    private val recommendationManager = MusicRecommendationManager(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val onlineManager = OnlineSearchManager()
    private val listenAgainManager = ListenAgainManager(getApplication())

    init {
        loadData()
        observeListeningHistoryForTraining()
        observeListenAgain()
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
    val onlineSearchResults: List<com.just_for_fun.youtubemusic.core.network.OnlineSearchResult> = emptyList()
    ,
    // Listen again cache exposed to UI
    val listenAgain: List<Song> = emptyList()
)