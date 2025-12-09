package com.just_for_fun.synctax.data.cache

import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Manages Speed Dial section - ML-recommended songs based on listening behavior
 * This now shows personalized recommendations (previously shown in Quick Picks)
 * 
 * Rules:
 * - No duplicate songs are allowed
 * - If a song is played again, it moves to the first position
 * - Maximum of 9 songs are displayed
 */
class SpeedDialManager(
    private val repository: MusicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _speedDialSongs = MutableStateFlow<List<Song>>(emptyList())
    val speedDialSongs: StateFlow<List<Song>> = _speedDialSongs.asStateFlow()
    
    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    /**
     * Update with ML-recommended songs (passed from HomeViewModel).
     * Ensures no duplicates - existing songs are moved to first position.
     */
    fun updateRecommendations(songs: List<Song>) {
        // Remove duplicates by keeping only the first occurrence of each song ID
        val uniqueSongs = songs.distinctBy { it.id }.take(9)
        _speedDialSongs.value = uniqueSongs
    }
    
    /**
     * Add a single song to speed dial (used when a song is played).
     * If song already exists, move it to first position.
     * Ensures maximum 9 songs.
     */
    fun addOrMoveToFirst(song: Song) {
        val currentList = _speedDialSongs.value.toMutableList()
        
        // Remove existing occurrence if present
        currentList.removeAll { it.id == song.id }
        
        // Add to first position
        currentList.add(0, song)
        
        // Keep only first 9
        _speedDialSongs.value = currentList.take(9)
    }
    
    fun setGenerating(generating: Boolean) {
        _isGenerating.value = generating
    }
}

