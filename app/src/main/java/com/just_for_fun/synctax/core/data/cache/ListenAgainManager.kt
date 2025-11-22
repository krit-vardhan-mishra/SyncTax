package com.just_for_fun.synctax.core.data.cache

import android.content.Context
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.core.data.repository.MusicRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Maintains a merged "Listen Again" cache (recently played + most played).
 * Keeps up to [maxSize] entries, verifies files exist on device and removes missing entries.
 */
class ListenAgainManager(private val context: Context, private val maxSize: Int = 20) {

    private val repository = MusicRepository(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _listenAgain = MutableStateFlow<List<Song>>(emptyList())
    val listenAgain: StateFlow<List<Song>> = _listenAgain.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            try {
                // Get last 50 listened songs
                val recentHistory = repository.getRecentHistory(50).first()
                val recentSongIds = recentHistory.map { it.songId }.distinct()

                // Collect songs
                val allRecentSongs = mutableListOf<Song>()
                for (id in recentSongIds) {
                    val song = repository.getSongById(id)
                    if (song != null) allRecentSongs.add(song)
                }

                // If we have 20 or more songs, randomly pick 20
                // Otherwise, show all available songs
                val selectedSongs = if (allRecentSongs.size >= 20) {
                    allRecentSongs.shuffled().take(20)
                } else {
                    allRecentSongs
                }

                _listenAgain.value = selectedSongs
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /** Remove a song from cached list (for example if it doesn't exist on device). */
    fun remove(songId: String) {
        scope.launch {
            val current = _listenAgain.value.toMutableList()
            val idx = current.indexOfFirst { it.id == songId }
            if (idx != -1) {
                current.removeAt(idx)
                _listenAgain.value = current
            }
        }
    }
}
