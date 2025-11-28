package com.just_for_fun.synctax.core.data.cache

import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.core.data.repository.MusicRepository
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
     * Update with ML-recommended songs (passed from HomeViewModel)
     */
    fun updateRecommendations(songs: List<Song>) {
        _speedDialSongs.value = songs.take(9) // Take first 9 recommendations
    }
    
    fun setGenerating(generating: Boolean) {
        _isGenerating.value = generating
    }
}
