package com.just_for_fun.synctax.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.synctax.core.data.cache.ListenAgainManager
import com.just_for_fun.synctax.core.data.cache.QuickAccessManager
import com.just_for_fun.synctax.core.data.cache.SpeedDialManager
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.core.data.repository.MusicRepository
import com.just_for_fun.synctax.core.ml.MusicRecommendationManager
import com.just_for_fun.synctax.core.ml.models.RecommendationResult
import com.just_for_fun.synctax.core.network.OnlineSearchManager
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.model.SearchFilterType
import com.just_for_fun.synctax.util.AlbumDetails
import com.just_for_fun.synctax.util.ArtistDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    private val speedDialManager = SpeedDialManager(repository)
    private val quickAccessManager = QuickAccessManager(repository)
    
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
        loadOnlineHistory()
        observeRecommendationsCount()
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

    private fun observeRecommendationsCount() {
        viewModelScope.launch {
            val userPreferences = com.just_for_fun.synctax.data.preferences.UserPreferences(getApplication())
            var isFirst = true
            userPreferences.recommendationsCount.collect { count ->
                if (isFirst) {
                    isFirst = false
                    return@collect
                }
                // Only regenerate if we have songs and not currently generating
                if (_uiState.value.allSongs.isNotEmpty() && !_uiState.value.isGeneratingRecommendations) {
                    generateQuickPicks()
                }
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

    fun searchOnline(query: String, filterType: SearchFilterType = SearchFilterType.ALL) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSearchingOnline = true)
            try {
                val results = mutableListOf<com.just_for_fun.synctax.core.network.OnlineSearchResult>()
                
                // Search for songs if filter is ALL or SONGS
                if (filterType == SearchFilterType.ALL ||
                    filterType == SearchFilterType.SONGS) {
                    
                    com.just_for_fun.synctax.util.YTMusicRecommender.searchSongs(
                        query = query,
                        limit = 15,
                        onResult = { songs ->
                            val songResults = songs.map { song ->
                                com.just_for_fun.synctax.core.network.OnlineSearchResult(
                                    id = song.videoId,
                                    title = song.title,
                                    author = song.artist,
                                    duration = 0L, // Duration from ytmusicapi is string, would need parsing
                                    thumbnailUrl = song.thumbnail,
                                    streamUrl = null,
                                    type = com.just_for_fun.synctax.core.network.OnlineResultType.SONG
                                )
                            }
                            results.addAll(songResults)
                        },
                        onError = { error ->
                            android.util.Log.e("HomeViewModel", "Song search failed: $error")
                        }
                    )
                    
                    // Wait a bit for async results
                    kotlinx.coroutines.delay(500)
                }
                
                // Search for albums if filter is ALL or ALBUMS
                if (filterType == com.just_for_fun.synctax.ui.model.SearchFilterType.ALL || 
                    filterType == com.just_for_fun.synctax.ui.model.SearchFilterType.ALBUMS) {
                    
                    com.just_for_fun.synctax.util.YTMusicRecommender.searchAlbums(
                        query = query,
                        limit = 10,
                        onResult = { albums ->
                            val albumResults = albums.map { album ->
                                com.just_for_fun.synctax.core.network.OnlineSearchResult(
                                    id = album.browseId,
                                    title = album.title,
                                    author = album.artist,
                                    duration = null,
                                    thumbnailUrl = album.thumbnail,
                                    streamUrl = null,
                                    type = com.just_for_fun.synctax.core.network.OnlineResultType.ALBUM,
                                    year = album.year,
                                    browseId = album.browseId
                                )
                            }
                            results.addAll(albumResults)
                        },
                        onError = { error ->
                            android.util.Log.e("HomeViewModel", "Album search failed: $error")
                        }
                    )
                    
                    // Wait a bit for async results
                    kotlinx.coroutines.delay(500)
                }
                
                // Search for artists if filter is ALL or ARTISTS
                if (filterType == com.just_for_fun.synctax.ui.model.SearchFilterType.ALL || 
                    filterType == com.just_for_fun.synctax.ui.model.SearchFilterType.ARTISTS) {
                    
                    com.just_for_fun.synctax.util.YTMusicRecommender.searchArtists(
                        query = query,
                        limit = 10,
                        onResult = { artists ->
                            val artistResults = artists.map { artist ->
                                com.just_for_fun.synctax.core.network.OnlineSearchResult(
                                    id = artist.browseId,
                                    title = artist.name,
                                    author = artist.subscribers,
                                    duration = null,
                                    thumbnailUrl = artist.thumbnail,
                                    streamUrl = null,
                                    type = com.just_for_fun.synctax.core.network.OnlineResultType.ARTIST,
                                    year = null,
                                    browseId = artist.browseId
                                )
                            }
                            results.addAll(artistResults)
                        },
                        onError = { error ->
                            android.util.Log.e("HomeViewModel", "Artist search failed: $error")
                        }
                    )
                    
                    // Wait a bit for async results
                    kotlinx.coroutines.delay(500)
                }
                
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
    
    /**
     * Fetch album details with songs list
     */
    fun fetchAlbumDetails(
        browseId: String,
        onResult: (AlbumDetails?) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingAlbumDetails = true)
            com.just_for_fun.synctax.util.YTMusicRecommender.getAlbumDetails(
                browseId = browseId,
                onResult = { albumDetails ->
                    _uiState.value = _uiState.value.copy(isLoadingAlbumDetails = false)
                    onResult(albumDetails)
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(isLoadingAlbumDetails = false)
                    onError(error)
                }
            )
        }
    }
    
    /**
     * Fetch artist details with top songs list
     */
    fun fetchArtistDetails(
        browseId: String,
        onResult: (ArtistDetails?) -> Unit,
        onError: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingArtistDetails = true)
            com.just_for_fun.synctax.util.YTMusicRecommender.getArtistDetails(
                browseId = browseId,
                onResult = { artistDetails ->
                    _uiState.value = _uiState.value.copy(isLoadingArtistDetails = false)
                    onResult(artistDetails)
                },
                onError = { error ->
                    _uiState.value = _uiState.value.copy(isLoadingArtistDetails = false)
                    onError(error)
                }
            )
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
            speedDialManager.setGenerating(true)

            try {
                val userPreferences = com.just_for_fun.synctax.data.preferences.UserPreferences(getApplication())
                val count = userPreferences.getRecommendationsCount()
                val result = recommendationManager.generateQuickPicks(count)

                // Map recommendations to songs
                val recommendedSongs = result.recommendations.mapNotNull { rec ->
                    _uiState.value.allSongs.find { it.id == rec.songId }
                }

                _uiState.value = _uiState.value.copy(
                    quickPicks = recommendedSongs.shuffled(),
                    recommendationScores = result.recommendations,
                    isGeneratingRecommendations = false
                )
                
                // Update Speed Dial with top 9 recommendations
                speedDialManager.updateRecommendations(recommendedSongs)
                speedDialManager.setGenerating(false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingRecommendations = false,
                    error = e.message
                )
                speedDialManager.setGenerating(false)
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

    fun setSelectedOnlineArtist(artistDetails: ArtistDetails) {
        _uiState.value = _uiState.value.copy(
            selectedOnlineArtist = artistDetails
        )
    }

    fun setSelectedAlbum(album: String, artist: String, songs: List<Song>) {
        _uiState.value = _uiState.value.copy(
            selectedAlbum = album,
            selectedAlbumArtist = artist,
            selectedAlbumSongs = songs
        )
    }

    fun setSelectedOnlineAlbum(albumDetails: AlbumDetails) {
        _uiState.value = _uiState.value.copy(
            selectedOnlineAlbum = albumDetails,
            selectedAlbum = albumDetails.title,
            selectedAlbumArtist = albumDetails.artist,
            selectedAlbumSongs = albumDetails.songs.map { song ->
                // Convert RecommendedSong to Song for compatibility
                Song(
                    id = song.videoId,
                    title = song.title,
                    artist = song.artist,
                    album = albumDetails.title,
                    duration = 0L, // Will be parsed from duration string if needed
                    filePath = song.watchUrl ?: "",
                    albumArtUri = song.thumbnail,
                    genre = null,
                    releaseYear = albumDetails.year.toIntOrNull()
                )
            }
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

                            // Check for various album art file patterns
                            val albumArtUri = repository.checkForLocalAlbumArt(audioFile)
                            
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
        // Speed Dial now uses ML recommendations - updated separately
        quickAccessManager.refresh()
    }
    
    /**
     * Update Speed Dial with ML recommendations
     */
    fun updateSpeedDialRecommendations(songs: List<Song>) {
        speedDialManager.updateRecommendations(songs)
    }
    
    /**
     * Add an online song to the listening history
     */
    fun addOnlineListeningHistory(videoId: String, title: String, artist: String, thumbnailUrl: String?, watchUrl: String) {
        viewModelScope.launch {
            try {
                val database = com.just_for_fun.synctax.core.data.local.MusicDatabase.getDatabase(getApplication())
                val onlineHistory = com.just_for_fun.synctax.core.data.local.entities.OnlineListeningHistory(
                    videoId = videoId,
                    title = title,
                    artist = artist,
                    thumbnailUrl = thumbnailUrl,
                    watchUrl = watchUrl
                )
                // Delete existing entry with same videoId to prevent duplicates
                database.onlineListeningHistoryDao().deleteByVideoId(videoId)
                // Insert new entry with updated timestamp
                database.onlineListeningHistoryDao().insertOnlineListening(onlineHistory)
                database.onlineListeningHistoryDao().trimOldRecords()
                
                // Refresh the online history in UI state
                loadOnlineHistory()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error adding online listening history", e)
            }
        }
    }
    
    /**
     * Load online listening history from database
     */
    private fun loadOnlineHistory() {
        viewModelScope.launch {
            try {
                val database = com.just_for_fun.synctax.core.data.local.MusicDatabase.getDatabase(getApplication())
                val userPreferences = com.just_for_fun.synctax.data.preferences.UserPreferences(getApplication())
                var isFirst = true
                userPreferences.onlineHistoryCount.collect { limit ->
                    if (isFirst) {
                        isFirst = false
                    }
                    database.onlineListeningHistoryDao().getRecentOnlineHistory(limit).collect { history ->
                        _uiState.value = _uiState.value.copy(onlineHistory = history)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error loading online listening history", e)
            }
        }
    }
    
    /**
     * Delete a song from online listening history
     */
    fun deleteOnlineHistory(videoId: String) {
        viewModelScope.launch {
            try {
                val database = com.just_for_fun.synctax.core.data.local.MusicDatabase.getDatabase(getApplication())
                database.onlineListeningHistoryDao().deleteByVideoId(videoId)
                android.util.Log.d("HomeViewModel", "Online history deleted: $videoId")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error deleting online history", e)
            }
        }
    }
    
    /**
     * Delete a song from the database and update UI
     */
    fun deleteSong(song: Song) {
        viewModelScope.launch {
            try {
                // Delete from database
                repository.deleteSong(song.id)
                
                // Update UI state by removing the song from the list
                val updatedSongs = _uiState.value.allSongs.filter { it.id != song.id }
                _uiState.value = _uiState.value.copy(allSongs = updatedSongs)
                
                // Cleanup ML data for this song
                repository.cleanupDeletedSongsData(listOf(song.id))
                
                // Refresh all sections
                refreshSections()
                
                // Regenerate quick picks if songs still available
                if (updatedSongs.isNotEmpty()) {
                    generateQuickPicks()
                }
                
                android.util.Log.d("HomeViewModel", "Song deleted successfully: ${song.title}")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error deleting song from database", e)
                _uiState.value = _uiState.value.copy(error = "Failed to delete song: ${e.message}")
            }
        }
    }

    // Search state management
    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun updateSelectedFilter(filter: SearchFilterType) {
        _uiState.value = _uiState.value.copy(selectedFilter = filter)
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
    val selectedOnlineArtist: ArtistDetails? = null,
    val selectedOnlineAlbum: AlbumDetails? = null,
    val selectedAlbum: String? = null,
    val selectedAlbumArtist: String? = null,
    val selectedAlbumSongs: List<Song>? = null,
    // Online search state (for SearchScreen)
    val isSearchingOnline: Boolean = false,
    val onlineSearchResults: List<com.just_for_fun.synctax.core.network.OnlineSearchResult> = emptyList(),
    val isLoadingArtistDetails: Boolean = false,
    val isLoadingAlbumDetails: Boolean = false,
    // Search state
    val searchQuery: String = "",
    val selectedFilter: SearchFilterType = SearchFilterType.ALL,
    // Section-specific song lists
    val listenAgain: List<Song> = emptyList(),
    val speedDialSongs: List<Song> = emptyList(), // Now shows ML recommendations
    val quickAccessSongs: List<Song> = emptyList(), // Now shows random songs
    val onlineHistory: List<com.just_for_fun.synctax.core.data.local.entities.OnlineListeningHistory> = emptyList(), // Last 10 online songs
    // Pagination state for all songs
    val isLoadingMore: Boolean = false,
    val hasMoreSongs: Boolean = true
)