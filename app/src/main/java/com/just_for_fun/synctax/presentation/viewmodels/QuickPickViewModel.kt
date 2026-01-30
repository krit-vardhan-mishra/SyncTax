package com.just_for_fun.synctax.presentation.viewmodels

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.just_for_fun.synctax.core.dispatcher.AppDispatchers
import com.just_for_fun.synctax.core.network.OnlineResultType
import com.just_for_fun.synctax.core.network.OnlineSearchResult
import com.just_for_fun.synctax.core.service.QuickPickService
import com.just_for_fun.synctax.data.local.entities.QuickPickSong
import com.just_for_fun.synctax.data.local.entities.Song
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Quick Pick screen.
 * Manages separate offline and online recommendation queues.
 */
class QuickPickViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "QuickPickViewModel"
    }
    
    private val quickPickService = QuickPickService(application)
    
    // UI State
    private val _uiState = MutableStateFlow(QuickPickUiState())
    val uiState: StateFlow<QuickPickUiState> = _uiState.asStateFlow()
    
    // Selected mode (Offline/Online)
    private val _selectedMode = MutableStateFlow("Offline")
    val selectedMode: StateFlow<String> = _selectedMode.asStateFlow()
    
    init {
        // Observe both queues
        observeOfflineQueue()
        observeOnlineQueue()
        
        // Load initial data
        loadInitialData()
    }
    
    /**
     * Observe offline queue changes
     */
    private fun observeOfflineQueue() {
        viewModelScope.launch {
            quickPickService.observeQueue(QuickPickService.SOURCE_OFFLINE).collect { queue ->
                val songs = quickPickService.toSongs(queue)
                _uiState.value = _uiState.value.copy(
                    offlineQueue = songs,
                    offlineQueueRaw = queue
                )
            }
        }
    }
    
    /**
     * Observe online queue changes
     */
    private fun observeOnlineQueue() {
        viewModelScope.launch {
            quickPickService.observeQueue(QuickPickService.SOURCE_ONLINE).collect { queue ->
                val onlineResults = queue.map { it.toOnlineSearchResult() }
                _uiState.value = _uiState.value.copy(
                    onlineQueue = onlineResults,
                    onlineQueueRaw = queue
                )
            }
        }
    }
    
    /**
     * Load initial data and check for seeds
     */
    private fun loadInitialData() {
        viewModelScope.launch(AppDispatchers.Database) {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // Check if seeds exist for each mode
                val hasOfflineSeeds = quickPickService.hasSeeds(QuickPickService.SOURCE_OFFLINE)
                val hasOnlineSeeds = quickPickService.hasSeeds(QuickPickService.SOURCE_ONLINE)
                
                _uiState.value = _uiState.value.copy(
                    hasOfflineSeeds = hasOfflineSeeds,
                    hasOnlineSeeds = hasOnlineSeeds,
                    isLoading = false
                )
                
                // Generate queues if seeds exist
                if (hasOfflineSeeds) {
                    quickPickService.generateOfflineQueue()
                }
                if (hasOnlineSeeds) {
                    quickPickService.generateOnlineQueue()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading initial data: ${e.message}", e)
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message
                )
            }
        }
    }
    
    /**
     * Set the selected mode (Offline/Online)
     */
    fun setMode(mode: String) {
        _selectedMode.value = mode
        
        // Refresh queue for the selected mode
        viewModelScope.launch {
            refreshQueue(mode)
        }
    }
    
    /**
     * Refresh the queue for a specific mode
     */
    private suspend fun refreshQueue(mode: String) {
        val source = if (mode == "Online") QuickPickService.SOURCE_ONLINE else QuickPickService.SOURCE_OFFLINE
        quickPickService.refreshQueue(source)
    }
    
    /**
     * Called when a song from the queue is played
     */
    fun onSongPlayed(song: QuickPickSong) {
        viewModelScope.launch(AppDispatchers.Database) {
            quickPickService.onSongPlayed(song)
        }
    }
    
    /**
     * Called when a song from the queue is played - without triggering queue refresh.
     * Used by QuickPickScreen to avoid cascading updates during playback.
     */
    fun onSongPlayedWithoutQueueRefresh(song: QuickPickSong) {
        viewModelScope.launch(AppDispatchers.Database) {
            quickPickService.recordSongPlayOnly(
                songId = song.songId,
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl,
                duration = song.duration,
                isOnline = song.source == QuickPickService.SOURCE_ONLINE,
                filePath = song.filePath
            )
        }
    }
    
    /**
     * Called when an offline song is played (from Song object)
     */
    fun onOfflineSongPlayed(song: Song) {
        viewModelScope.launch(AppDispatchers.Database) {
            quickPickService.recordSongPlay(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.albumArtUri,
                duration = song.duration,
                isOnline = false,
                genre = song.genre,
                album = song.album,
                filePath = song.filePath
            )
        }
    }
    
    /**
     * Called when an offline song is played - without triggering queue refresh.
     * Used by QuickPickScreen to avoid cascading updates during playback.
     */
    fun onOfflineSongPlayedWithoutQueueRefresh(song: Song) {
        viewModelScope.launch(AppDispatchers.Database) {
            quickPickService.recordSongPlayOnly(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.albumArtUri,
                duration = song.duration,
                isOnline = false,
                genre = song.genre,
                album = song.album,
                filePath = song.filePath
            )
        }
    }
    
    /**
     * Called when an online song is played
     */
    fun onOnlineSongPlayed(
        videoId: String,
        title: String,
        artist: String,
        thumbnailUrl: String?,
        duration: Long = 0
    ) {
        viewModelScope.launch(AppDispatchers.Database) {
            quickPickService.recordSongPlay(
                songId = videoId,
                title = title,
                artist = artist,
                thumbnailUrl = thumbnailUrl,
                duration = duration,
                isOnline = true
            )
        }
    }
    
    /**
     * Manually refresh the current queue
     */
    fun refresh() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            
            try {
                val source = if (_selectedMode.value == "Online") {
                    QuickPickService.SOURCE_ONLINE
                } else {
                    QuickPickService.SOURCE_OFFLINE
                }
                
                quickPickService.refreshQueue(source)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing queue: ${e.message}", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }
    
    /**
     * Shuffle - regenerates the queue with new recommendations.
     * For online mode: picks a new random song from history and gets fresh recommendations.
     * For offline mode: shuffles the offline queue.
     */
    fun shuffle() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            Log.d(TAG, "🔀 Shuffle requested for mode: ${_selectedMode.value}")
            
            try {
                val source = if (_selectedMode.value == "Online") {
                    QuickPickService.SOURCE_ONLINE
                } else {
                    QuickPickService.SOURCE_OFFLINE
                }
                
                // Regenerate queue (for online, this picks a new random seed song)
                quickPickService.refreshQueue(source)
                Log.d(TAG, "🔀 Shuffle complete - new queue generated")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error during shuffle: ${e.message}", e)
                _uiState.value = _uiState.value.copy(error = e.message)
            } finally {
                _uiState.value = _uiState.value.copy(isRefreshing = false)
            }
        }
    }
    
    /**
     * Preload stream URLs for all online songs in the queue.
     * This ensures instant playback when user swipes to next song.
     */
    fun preloadOnlineQueue() {
        if (_selectedMode.value != "Online") return
        
        viewModelScope.launch(AppDispatchers.Network) {
            val queue = _uiState.value.onlineQueueRaw
            if (queue.isEmpty()) return@launch
            
            Log.d(TAG, "🔄 Preloading ${queue.size} online songs...")
            
            queue.forEach { song ->
                quickPickService.preloadStreamUrl(song.songId)
            }
            
            Log.d(TAG, "✅ Preload initiated for ${queue.size} songs")
        }
    }
    
    /**
     * Get the current song list based on selected mode
     */
    fun getCurrentQueue(): List<Song> {
        return if (_selectedMode.value == "Online") {
            // Convert online queue to Song objects for MotionPlayerScreen
            _uiState.value.onlineQueueRaw.map { it.toSong() }
        } else {
            _uiState.value.offlineQueue
        }
    }
    
    /**
     * Get the current queue as QuickPickSong list
     */
    fun getCurrentQueueRaw(): List<QuickPickSong> {
        return if (_selectedMode.value == "Online") {
            _uiState.value.onlineQueueRaw
        } else {
            _uiState.value.offlineQueueRaw
        }
    }
    
    /**
     * Check if the current mode has seeds
     */
    fun hasCurrentModeSeeds(): Boolean {
        return if (_selectedMode.value == "Online") {
            _uiState.value.hasOnlineSeeds
        } else {
            _uiState.value.hasOfflineSeeds
        }
    }
    
    /**
     * Clear any error state
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

/**
 * UI State for Quick Pick screen
 */
data class QuickPickUiState(
    val offlineQueue: List<Song> = emptyList(),
    val onlineQueue: List<OnlineSearchResult> = emptyList(),
    val offlineQueueRaw: List<QuickPickSong> = emptyList(),
    val onlineQueueRaw: List<QuickPickSong> = emptyList(),
    val hasOfflineSeeds: Boolean = false,
    val hasOnlineSeeds: Boolean = false,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null
)

/**
 * Extension function to convert QuickPickSong to OnlineSearchResult
 */
fun QuickPickSong.toOnlineSearchResult(): OnlineSearchResult {
    return OnlineSearchResult(
        id = songId,
        title = title,
        author = artist,
        duration = duration,
        thumbnailUrl = thumbnailUrl,
        streamUrl = streamUrl,
        type = OnlineResultType.SONG
    )
}

/**
 * Extension function to convert QuickPickSong to Song for playback
 */
fun QuickPickSong.toSong(): Song {
    // For online songs, use "online:" prefix to match PlayerViewModel's format
    val effectiveId = if (source == "online") "online:$songId" else songId
    return Song(
        id = effectiveId,
        title = title,
        artist = artist,
        album = null,
        duration = duration,
        filePath = filePath ?: "online:$songId",  // Use online: prefix for streaming
        genre = null,
        releaseYear = null,
        albumArtUri = thumbnailUrl
    )
}
