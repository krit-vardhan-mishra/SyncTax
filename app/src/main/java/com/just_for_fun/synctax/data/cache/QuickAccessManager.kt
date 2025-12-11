package com.just_for_fun.synctax.data.cache

import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manages Quick Access section - random songs for quick discovery
 * This shows randomly selected songs from the user's library (previously in Speed Dial)
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
