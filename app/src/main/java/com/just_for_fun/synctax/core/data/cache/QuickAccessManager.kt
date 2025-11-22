package com.just_for_fun.synctax.core.data.cache

import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.core.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manages Quick Access section - random 9 songs
 */
class QuickAccessManager(
    private val repository: MusicRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _quickAccessSongs = MutableStateFlow<List<Song>>(emptyList())
    val quickAccessSongs: StateFlow<List<Song>> = _quickAccessSongs.asStateFlow()

    fun refresh() {
        scope.launch {
            try {
                val allSongs = repository.getAllSongs().first()
                
                // Get random 9 songs
                val randomSongs = allSongs.shuffled().take(9)
                
                _quickAccessSongs.value = randomSongs
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
