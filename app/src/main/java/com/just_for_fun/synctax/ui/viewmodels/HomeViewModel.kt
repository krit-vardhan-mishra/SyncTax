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

    companion object {
        const val PAGE_SIZE = 30  // Load 30 songs at a time
    }

    private val repository = MusicRepository(application)
    private val recommendationManager = MusicRecommendationManager(application)

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    
    private val onlineManager = OnlineSearchManager()
    private val listenAgainManager = ListenAgainManager(getApplication())
    private val speedDialManager = com.just_for_fun.synctax.core.data.cache.SpeedDialManager(repository)
    private val quickAccessManager = com.just_for_fun.synctax.core.data.cache.QuickAccessManager(repository)
    
    private var lastQuickPicksRefresh = 0L
    private val quickPicksRefreshInterval = 15 * 60 * 1000L // 15 minutes in milliseconds

    // Callback for refreshing downloaded songs check
    var onSongsRefreshed: ((List<Song>) -> Unit)? = null

    init {
        loadData()
        // Auto-scan on app open to keep library in sync with device
        scanMusic()
        observeListeningHistoryForTraining()
        observeListenAgain()
        // Start periodic refresh for deleted songs check
        startPeriodicRefresh()
        // Refresh album art for songs without embedded art
        refreshAlbumArtForSongs()
    }

    private fun observeListenAgain() {
        viewModelScope.launch {
            listenAgainManager.listenAgain.collect { list ->
                _uiState.value = _uiState.value.copy(listenAgain = list)
            }
        }
        viewModelScope.launch {
            speedDialManager.speedDialSongs.collect { list ->
                _uiState.value = _uiState.value.copy(speedDialSongs = list)
            }
        }
        viewModelScope.launch {
            quickAccessManager.quickAccessSongs.collect { list ->
                _uiState.value = _uiState.value.copy(quickAccessSongs = list)
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
                        // Refresh all sections
                        refreshSections()
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

    fun forceRefreshLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)

            try {
                // Get selected scan paths from UserPreferences
                val userPrefs = com.just_for_fun.synctax.data.preferences.UserPreferences(getApplication())
                val scanPaths = userPrefs.scanPaths.value
                
                // Force a complete rescan
                val songs = repository.scanDeviceMusic(scanPaths)
                
                // Update the state with fresh data
                _uiState.value = _uiState.value.copy(
                    allSongs = songs,
                    isScanning = false,
                    scanComplete = true
                )
                
                // Notify listeners that songs have been refreshed
                onSongsRefreshed?.invoke(songs)
                
                // Refresh album art for songs that might have new art files
                refreshAlbumArtForSongs()
                
                // Regenerate quick picks with the updated song list
                if (songs.isNotEmpty()) {
                    generateQuickPicks()
                }
                
                // Refresh all sections after scanning
                refreshSections()
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
                    quickPicks = recommendedSongs.shuffled(),
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

    fun scanMusic() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isScanning = true)

            try {
                // Get selected scan paths from UserPreferences
                val userPrefs = com.just_for_fun.synctax.data.preferences.UserPreferences(getApplication())
                val scanPaths = userPrefs.scanPaths.value
                val songs = repository.scanDeviceMusic(scanPaths)

                // Force refresh the song list by updating the state
                _uiState.value = _uiState.value.copy(
                    allSongs = songs,
                    isScanning = false,
                    scanComplete = true
                )

                // Notify listeners that songs have been refreshed
                onSongsRefreshed?.invoke(songs)

                // Regenerate quick picks with the updated song list
                if (songs.isNotEmpty()) {
                    generateQuickPicks()
                }

                // Refresh all sections after scanning
                refreshSections()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isScanning = false,
                    error = e.message
                )
            }
        }
    }

    private fun observeListeningHistoryForTraining() {
        viewModelScope.launch {
            var hasAutoTrained = false
            repository.getRecentHistory(1).collect { history ->
                // Update training data size
                val allHistory = repository.getRecentHistory(1000).first()
                _uiState.value = _uiState.value.copy(trainingDataSize = allHistory.size)

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
                            quickPicks = recommendedSongs.shuffled(),
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

    private fun refreshAlbumArtForSongs() {
        viewModelScope.launch {
            try {
                // Get all songs from database
                val allSongs = _uiState.value.allSongs
                
                // Find songs without album art (either null or MediaStore URI)
                val songsWithoutArt = allSongs.filter { song ->
                    song.albumArtUri.isNullOrEmpty() || 
                    song.albumArtUri?.startsWith("content://media/external/audio/albumart") == true
                }
                
                if (songsWithoutArt.isNotEmpty()) {
                    // Check for local image files for these songs
                    val updatedSongs = mutableListOf<Song>()
                    
                    songsWithoutArt.forEach { song ->
                        val audioFile = java.io.File(song.filePath)
                        if (audioFile.exists()) {
                            val directory = audioFile.parentFile ?: return@forEach
                            
                            // Check for various album art file patterns
                            val albumArtUri = findAlbumArtForSong(audioFile, directory)
                            
                            if (albumArtUri != null) {
                                // Found album art file, update the song
                                val updatedSong = song.copy(albumArtUri = albumArtUri)
                                updatedSongs.add(updatedSong)
                                
                                // Update in database
                                repository.insertSong(updatedSong)
                            }
                        }
                    }
                    
                    if (updatedSongs.isNotEmpty()) {
                        // Update the UI state with songs that now have album art
                        val currentSongs = _uiState.value.allSongs.toMutableList()
                        updatedSongs.forEach { updatedSong ->
                            val index = currentSongs.indexOfFirst { it.id == updatedSong.id }
                            if (index != -1) {
                                currentSongs[index] = updatedSong
                            }
                        }
                        
                        _uiState.value = _uiState.value.copy(allSongs = currentSongs)
                        
                        // Regenerate quick picks with updated album art
                        if (currentSongs.isNotEmpty()) {
                            generateQuickPicks()
                        }
                    }
                }
            } catch (e: Exception) {
                // Silently handle errors during album art refresh
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Find album art for a song by checking various file patterns
     */
    private fun findAlbumArtForSong(audioFile: java.io.File, directory: java.io.File): String? {
        val baseName = audioFile.nameWithoutExtension
        
        // Common image extensions to check
        val imageExtensions = listOf("jpg", "jpeg", "png", "bmp", "gif")
        
        // 1. Check for file with same base name as audio file
        for (ext in imageExtensions) {
            val albumArtFile = java.io.File(directory, "$baseName.$ext")
            if (albumArtFile.exists() && albumArtFile.isFile) {
                return albumArtFile.absolutePath
            }
        }
        
        // 2. Check for common album art file names in the directory
        val commonAlbumArtNames = listOf("cover", "folder", "album", "artwork", "front")
        for (name in commonAlbumArtNames) {
            for (ext in imageExtensions) {
                val albumArtFile = java.io.File(directory, "$name.$ext")
                if (albumArtFile.exists() && albumArtFile.isFile) {
                    return albumArtFile.absolutePath
                }
            }
        }
        
        return null
    }

    fun refreshAlbumArt() {
        refreshAlbumArtForSongs()
    }

    /**
     * Refresh all section managers
     */
    fun refreshSections() {
        listenAgainManager.refresh()
        speedDialManager.refresh()
        quickAccessManager.refresh()
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
    val trainingDataSize: Int = 0,
    val error: String? = null,
    val selectedArtist: String? = null,
    val selectedArtistSongs: List<Song>? = null,
    val selectedAlbum: String? = null,
    val selectedAlbumArtist: String? = null,
    val selectedAlbumSongs: List<Song>? = null,
    // Online search state (for SearchScreen)
    val isSearchingOnline: Boolean = false,
    val onlineSearchResults: List<com.just_for_fun.synctax.core.network.OnlineSearchResult> = emptyList(),
    // Section-specific song lists
    val listenAgain: List<Song> = emptyList(),
    val speedDialSongs: List<Song> = emptyList(),
    val quickAccessSongs: List<Song> = emptyList(),
    // Pagination state for all songs
    val isLoadingMore: Boolean = false,
    val hasMoreSongs: Boolean = true
)