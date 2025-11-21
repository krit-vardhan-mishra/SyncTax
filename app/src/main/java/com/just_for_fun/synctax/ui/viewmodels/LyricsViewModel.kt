package com.just_for_fun.synctax.ui.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.core.data.model.LyricLine
import com.just_for_fun.synctax.core.network.LrcLibResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import com.just_for_fun.synctax.core.data.repository.LyricsRepository
import kotlin.collections.isNotEmpty


/**
 * ViewModel for managing lyrics fetching and state
 * Handles API calls to LRCLIB in the background
 */
class LyricsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val lyricsRepository = LyricsRepository()
    
    private val _lyricsState = MutableStateFlow(LyricsUiState())
    val lyricsState: StateFlow<LyricsUiState> = _lyricsState.asStateFlow()
    
    // Current lyrics for the loaded song
    private val _currentLyrics = MutableStateFlow<List<LyricLine>?>(null)
    val currentLyrics: StateFlow<List<LyricLine>?> = _currentLyrics.asStateFlow()
    
    // Search results for user selection
    private val _searchResults = MutableStateFlow<List<LrcLibResponse>>(emptyList())
    val searchResults: StateFlow<List<LrcLibResponse>> = _searchResults.asStateFlow()
    
    // Set of song IDs that failed to find lyrics
    private val _failedSongs = mutableSetOf<String>()
    
    init {
        // Initialize with empty lyrics
        _currentLyrics.value = null
    }
    
    /**
     * Parse LRC content into LyricLine objects
     */
    private fun parseLrcContent(content: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.+)")
        
        content.lines().forEach { line ->
            val match = regex.find(line.trim())
            if (match != null) {
                val minutes = match.groupValues[1].toInt()
                val seconds = match.groupValues[2].toInt()
                val centiseconds = match.groupValues[3].toInt()
                val text = match.groupValues[4].trim()
                
                val totalMillis = (minutes * 60 * 1000) + (seconds * 1000) + (centiseconds * 10)
                lines.add(LyricLine(totalMillis.toLong(), text))
            }
        }
        
        return lines.sortedBy { it.timestamp }
    }
    
    /**
     * Load lyrics for a song from local file
     */
    fun loadLyricsForSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val lyrics = try {
                // Try to find LRC file with the same name as the audio file
                val audioFile = File(song.filePath)
                val lrcFile = File(audioFile.parent, audioFile.nameWithoutExtension + ".lrc")
                
                if (lrcFile.exists()) {
                    val content = lrcFile.readText()
                    parseLrcContent(content)
                } else {
                    null
                }
            } catch (e: Exception) {
                null
            }
            _currentLyrics.value = lyrics
        }
    }
    
    /**
     * Fetch lyrics for a song from LRCLIB API
     * Shows loading indicator while fetching
     * 
     * @param song The song to fetch lyrics for
     */
    fun fetchLyricsForSong(song: Song) {
        // Don't fetch if already fetching
        if (_lyricsState.value.isFetching) return
        
        viewModelScope.launch {
            try {
                // Set loading state
                _lyricsState.value = _lyricsState.value.copy(
                    isFetching = true,
                    error = null
                )
                
                // Try exact match first
                val lyrics = lyricsRepository.fetchLyricsFromApi(song)
                
                if (lyrics != null) {
                    // Save and reload
                    lyricsRepository.saveLyricsToFile(song, lyrics)
                    loadLyricsForSong(song)
                    
                    _lyricsState.value = _lyricsState.value.copy(
                        isFetching = false,
                        lastFetchedSongId = song.id,
                        fetchSuccess = true,
                        error = null
                    )
                } else {
                    // No exact match, search for options
                    searchLyricsForSong(song)
                }
                
            } catch (e: Exception) {
                // Mark song as failed on any exception during fetch
                _failedSongs.add(song.id)
                _lyricsState.value = _lyricsState.value.copy(
                    isFetching = false,
                    error = "Failed to fetch lyrics: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Fetch lyrics for a song with custom search query from LRCLIB API
     * Shows loading indicator while fetching
     * 
     * @param song The song to fetch lyrics for
     * @param customSongName Custom song name to search for
     * @param customArtistName Custom artist name to search for
     */
    fun fetchLyricsForSongWithCustomQuery(song: Song, customSongName: String, customArtistName: String) {
        // Don't fetch if already fetching
        if (_lyricsState.value.isFetching) return
        
        viewModelScope.launch {
            try {
                // Set loading state
                _lyricsState.value = _lyricsState.value.copy(
                    isFetching = true,
                    error = null
                )
                
                // Try exact match first with custom query
                val lyrics = lyricsRepository.fetchLyricsFromApiWithCustomQuery(song, customSongName, customArtistName)
                
                if (lyrics != null) {
                    // Save and reload
                    lyricsRepository.saveLyricsToFile(song, lyrics)
                    loadLyricsForSong(song)
                    
                    _lyricsState.value = _lyricsState.value.copy(
                        isFetching = false,
                        lastFetchedSongId = song.id,
                        fetchSuccess = true,
                        error = null
                    )
                } else {
                    // No exact match, search for options with custom query
                    searchLyricsForSongWithCustomQuery(song, customSongName, customArtistName)
                }
                
            } catch (e: Exception) {
                // Mark song as failed on any exception during custom fetch
                _failedSongs.add(song.id)
                _lyricsState.value = _lyricsState.value.copy(
                    isFetching = false,
                    error = "Failed to fetch lyrics: ${e.message}"
                )
            }
        }
    }
    
    private fun searchLyricsForSong(song: Song) {
        viewModelScope.launch {
            try {
                val results = lyricsRepository.searchLyricsResults(song)
                
                if (results.isNotEmpty()) {
                    _searchResults.value = results
                    _lyricsState.value = _lyricsState.value.copy(
                        isFetching = false,
                        lastFetchedSongId = song.id,
                        fetchSuccess = false,
                        error = null,
                        hasSearchResults = true
                    )
                } else {
                    // No results found
                    _failedSongs.add(song.id)
                    _lyricsState.value = _lyricsState.value.copy(
                        isFetching = false,
                        lastFetchedSongId = song.id,
                        fetchSuccess = false,
                        error = "Could not find lyrics for this song"
                    )
                }
            } catch (e: Exception) {
                // Mark song as failed on any exception during search
                _failedSongs.add(song.id)
                _lyricsState.value = _lyricsState.value.copy(
                    isFetching = false,
                    error = "Failed to search lyrics: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Search for lyrics options when exact match fails (with custom query)
     */
    private fun searchLyricsForSongWithCustomQuery(song: Song, customSongName: String, customArtistName: String) {
        viewModelScope.launch {
            try {
                val results = lyricsRepository.searchLyricsResultsWithCustomQuery(song, customSongName, customArtistName)
                
                if (results.isNotEmpty()) {
                    _searchResults.value = results
                    _lyricsState.value = _lyricsState.value.copy(
                        isFetching = false,
                        lastFetchedSongId = song.id,
                        fetchSuccess = false,
                        error = null,
                        hasSearchResults = true
                    )
                } else {
                    // No results found
                    _failedSongs.add(song.id)
                    _lyricsState.value = _lyricsState.value.copy(
                        isFetching = false,
                        lastFetchedSongId = song.id,
                        fetchSuccess = false,
                        error = "Could not find lyrics for this song"
                    )
                }
            } catch (e: Exception) {
                // Mark song as failed on any exception during custom search
                _failedSongs.add(song.id)
                _lyricsState.value = _lyricsState.value.copy(
                    isFetching = false,
                    error = "Failed to search lyrics: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Select and download specific lyrics from search results
     */
    fun selectLyrics(trackId: Int, song: Song) {
        viewModelScope.launch {
            try {
                _lyricsState.value = _lyricsState.value.copy(isFetching = true, error = null)
                
                val success = lyricsRepository.fetchAndSaveLyricsById(trackId, song)
                
                if (success) {
                    loadLyricsForSong(song)
                    _searchResults.value = emptyList() // Clear search results
                    _lyricsState.value = _lyricsState.value.copy(
                        isFetching = false,
                        fetchSuccess = true,
                        error = null
                    )
                } else {
                    _lyricsState.value = _lyricsState.value.copy(
                        isFetching = false,
                        error = "Failed to download selected lyrics"
                    )
                }
            } catch (e: Exception) {
                // Mark song as failed if selection fails
                _failedSongs.add(song.id)
                _lyricsState.value = _lyricsState.value.copy(
                    isFetching = false,
                    error = "Failed to select lyrics: ${e.message}"
                )
            }
        }
    }
    
    /**
     * Clear search results
     */
    fun clearSearchResults() {
        _searchResults.value = emptyList()
        _lyricsState.value = _lyricsState.value.copy(hasSearchResults = false)
    }
    
    /**
     * Clear error message
     */
    fun clearError() {
        _lyricsState.value = _lyricsState.value.copy(error = null)
    }
    
    /**
     * Check if lyrics file exists for a song
     */
    fun hasLyricsFile(song: Song): Boolean {
        return lyricsRepository.hasLyricsFile(song)
    }
    
    /**
     * Check if we previously failed to find lyrics for this song
     */
    fun hasFailedToFindLyrics(songId: String): Boolean {
        return _failedSongs.contains(songId)
    }
}

/**
 * UI state for lyrics fetching
 */
data class LyricsUiState(
    val isFetching: Boolean = false,
    val lastFetchedSongId: String? = null,
    val fetchSuccess: Boolean = false,
    val error: String? = null,
    val hasSearchResults: Boolean = false
)
