package com.just_for_fun.synctax.core.player

import android.content.Context
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.core.data.repository.MusicRepository
import com.just_for_fun.synctax.core.ml.MusicRecommendationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first

/**
 * Centralized queue manager for handling playback queue operations.
 * Manages queue state, song ordering, and dynamic queue refilling based on recommendations.
 */
class QueueManager(
    private val context: Context,
    private val repository: MusicRepository,
    private val recommendationManager: MusicRecommendationManager
) {
    private val _queueState = MutableStateFlow(QueueState())
    val queueState: StateFlow<QueueState> = _queueState.asStateFlow()

    /**
     * Initialize queue with a playlist
     */
    fun initializeQueue(playlist: List<Song>, startIndex: Int = 0) {
        _queueState.value = QueueState(
            currentPlaylist = playlist,
            currentIndex = startIndex,
            playHistory = emptyList()
        )
    }

    /**
     * Get current song
     */
    fun getCurrentSong(): Song? {
        val state = _queueState.value
        return if (state.currentIndex in state.currentPlaylist.indices) {
            state.currentPlaylist[state.currentIndex]
        } else {
            null
        }
    }

    /**
     * Get upcoming queue (songs after current)
     */
    fun getUpcomingQueue(): List<Song> {
        val state = _queueState.value
        return if (state.currentIndex < state.currentPlaylist.size - 1) {
            state.currentPlaylist.subList(state.currentIndex + 1, state.currentPlaylist.size)
        } else {
            emptyList()
        }
    }

    /**
     * Get play history
     */
    fun getPlayHistory(): List<Song> {
        return _queueState.value.playHistory
    }

    /**
     * Move to next song in queue
     * @return The next song, or null if queue is empty
     */
    suspend fun moveToNext(autoRefill: Boolean = true): Song? {
        val state = _queueState.value
        
        // Add current song to history
        getCurrentSong()?.let { currentSong ->
            val updatedHistory = state.playHistory.toMutableList().apply {
                add(currentSong)
                // Limit history size
                if (size > 50) removeAt(0)
            }
            _queueState.value = state.copy(playHistory = updatedHistory)
        }

        // Check if we can move to next song
        if (state.currentIndex < state.currentPlaylist.size - 1) {
            _queueState.value = state.copy(currentIndex = state.currentIndex + 1)
            return getCurrentSong()
        }

        // Queue is empty - attempt to refill if enabled
        if (autoRefill && state.currentPlaylist.isNotEmpty()) {
            val refilled = refillQueue()
            if (refilled) {
                return getCurrentSong()
            }
        }

        return null
    }

    /**
     * Move to previous song in queue or history
     */
    fun moveToPrevious(): Song? {
        val state = _queueState.value

        // If not at the start of playlist, just move back
        if (state.currentIndex > 0) {
            _queueState.value = state.copy(currentIndex = state.currentIndex - 1)
            return getCurrentSong()
        }

        // If at start of playlist but have history, restore from history
        if (state.playHistory.isNotEmpty()) {
            val lastHistorySong = state.playHistory.last()
            val updatedHistory = state.playHistory.dropLast(1)
            val updatedPlaylist = listOf(lastHistorySong) + state.currentPlaylist

            _queueState.value = state.copy(
                currentPlaylist = updatedPlaylist,
                currentIndex = 0,
                playHistory = updatedHistory
            )
            return getCurrentSong()
        }

        return null
    }

    /**
     * Play a specific song from the queue
     * Removes all songs before the selected song
     */
    fun playFromQueue(song: Song): Song? {
        val state = _queueState.value
        val songIndex = state.currentPlaylist.indexOf(song)

        if (songIndex == -1) return null

        // Add songs before selected song to history
        if (songIndex > 0) {
            val songsToHistory = state.currentPlaylist.subList(0, songIndex)
            val updatedHistory = (state.playHistory + songsToHistory).takeLast(50)
            
            // Remove songs before the selected song from queue
            val updatedPlaylist = state.currentPlaylist.subList(songIndex, state.currentPlaylist.size)

            _queueState.value = state.copy(
                currentPlaylist = updatedPlaylist,
                currentIndex = 0,
                playHistory = updatedHistory
            )
        } else {
            _queueState.value = state.copy(currentIndex = songIndex)
        }

        return getCurrentSong()
    }

    /**
     * Add song to play next (after current song)
     */
    fun placeNext(song: Song) {
        val state = _queueState.value
        val currentPosition = state.currentPlaylist.indexOf(song)
        
        val mutablePlaylist = state.currentPlaylist.toMutableList()

        // Remove song from current position if it exists
        if (currentPosition != -1) {
            mutablePlaylist.removeAt(currentPosition)
            // Adjust current index if necessary
            val adjustedIndex = if (currentPosition < state.currentIndex) {
                state.currentIndex - 1
            } else {
                state.currentIndex
            }
            
            // Insert at position after current song
            val insertPosition = adjustedIndex + 1
            mutablePlaylist.add(insertPosition, song)

            _queueState.value = state.copy(
                currentPlaylist = mutablePlaylist,
                currentIndex = adjustedIndex
            )
        } else {
            // Song not in queue, just add it after current
            val insertPosition = state.currentIndex + 1
            mutablePlaylist.add(insertPosition, song)
            _queueState.value = state.copy(currentPlaylist = mutablePlaylist)
        }
    }

    /**
     * Remove song from queue
     */
    fun removeFromQueue(song: Song) {
        val state = _queueState.value
        val songIndex = state.currentPlaylist.indexOf(song)

        if (songIndex == -1) return

        val mutablePlaylist = state.currentPlaylist.toMutableList()
        mutablePlaylist.removeAt(songIndex)

        // Adjust current index if necessary
        val newIndex = when {
            songIndex < state.currentIndex -> state.currentIndex - 1
            songIndex == state.currentIndex -> state.currentIndex // Will be handled by caller
            else -> state.currentIndex
        }

        _queueState.value = state.copy(
            currentPlaylist = mutablePlaylist,
            currentIndex = newIndex
        )
    }

    /**
     * Reorder songs in queue (for drag and drop)
     */
    fun reorderQueue(fromIndex: Int, toIndex: Int) {
        val state = _queueState.value

        if (fromIndex !in state.currentPlaylist.indices || toIndex !in state.currentPlaylist.indices) {
            return
        }

        val mutablePlaylist = state.currentPlaylist.toMutableList()
        val song = mutablePlaylist.removeAt(fromIndex)
        mutablePlaylist.add(toIndex, song)

        // Update current index if necessary
        val newIndex = when {
            fromIndex == state.currentIndex -> toIndex
            fromIndex < state.currentIndex && toIndex >= state.currentIndex -> state.currentIndex - 1
            fromIndex > state.currentIndex && toIndex <= state.currentIndex -> state.currentIndex + 1
            else -> state.currentIndex
        }

        _queueState.value = state.copy(
            currentPlaylist = mutablePlaylist,
            currentIndex = newIndex
        )
    }

    /**
     * Shuffle the queue, keeping current song at the front
     * This is simple random shuffle
     */
    fun shuffle() {
        val state = _queueState.value
        val currentSong = getCurrentSong() ?: return

        // Remove current song and shuffle remaining
        val songsToShuffle = state.currentPlaylist.toMutableList().apply {
            remove(currentSong)
        }.shuffled()

        // Place current song at the beginning
        val shuffledPlaylist = listOf(currentSong) + songsToShuffle

        _queueState.value = state.copy(
            currentPlaylist = shuffledPlaylist,
            currentIndex = 0
        )
    }

    /**
     * Smart shuffle based on user recommendations
     * Interleaves recommended songs with others for better discovery
     */
    suspend fun shuffleWithRecommendations(songs: List<Song>) {
        try {
            // Get recommendations
            val recommendations = recommendationManager.generateQuickPicks(50)
            val recommendedIds = recommendations.recommendations.map { it.songId }.toSet()
            
            // Split into recommended and others
            val recommended = songs.filter { it.id in recommendedIds }
            val others = songs.filter { it.id !in recommendedIds }
            
            // Shuffle each group
            val shuffledRecommended = recommended.shuffled()
            val shuffledOthers = others.shuffled()
            
            // Interleave: 2 recommended songs, then 1 other song
            val shuffledPlaylist = mutableListOf<Song>()
            var recIndex = 0
            var othIndex = 0
            
            while (recIndex < shuffledRecommended.size || othIndex < shuffledOthers.size) {
                // Add up to 2 recommended songs
                repeat(2) {
                    if (recIndex < shuffledRecommended.size) {
                        shuffledPlaylist.add(shuffledRecommended[recIndex++])
                    }
                }
                // Add 1 other song
                if (othIndex < shuffledOthers.size) {
                    shuffledPlaylist.add(shuffledOthers[othIndex++])
                }
            }
            
            _queueState.value = QueueState(
                currentPlaylist = shuffledPlaylist,
                currentIndex = 0,
                playHistory = emptyList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to regular shuffle
            setQueue(songs.shuffled(), 0)
        }
    }

    /**
     * Add songs to the end of the queue
     */
    fun addToQueue(songs: List<Song>) {
        val state = _queueState.value
        val updatedPlaylist = state.currentPlaylist + songs
        _queueState.value = state.copy(currentPlaylist = updatedPlaylist)
    }

    /**
     * Clear the entire queue and history
     */
    fun clearQueue() {
        _queueState.value = QueueState()
    }

    /**
     * Refill queue with recommended songs based on current/last played song
     */
    private suspend fun refillQueue(): Boolean {
        val state = _queueState.value
        
        // Get the last played song (either current or from history)
        val baseSong = getCurrentSong() ?: state.playHistory.lastOrNull() ?: return false

        try {
            // Generate recommendations based on the last song
            val recommendations = recommendationManager.generateQuickPicks(20)
            
            if (recommendations.recommendations.isEmpty()) {
                // Fallback: get songs from same genre/artist
                return refillWithSimilarSongs(baseSong)
            }

            // Map recommendations to songs
            val allSongs = repository.getAllSongs().first()
            val recommendedSongs = recommendations.recommendations.mapNotNull { rec ->
                allSongs.find { it.id == rec.songId }
            }.filter { it.id != baseSong.id } // Don't include the base song

            if (recommendedSongs.isEmpty()) {
                return refillWithSimilarSongs(baseSong)
            }

            // Update queue with recommendations
            _queueState.value = state.copy(
                currentPlaylist = listOf(baseSong) + recommendedSongs,
                currentIndex = 0
            )

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return refillWithSimilarSongs(baseSong)
        }
    }

    /**
     * Fallback refill: get songs from same genre or artist
     */
    private suspend fun refillWithSimilarSongs(baseSong: Song): Boolean {
        try {
            val allSongs = repository.getAllSongs().first()
            
            // Filter by genre first, then by artist
            val similarSongs = allSongs.filter { song ->
                song.id != baseSong.id && (
                    (baseSong.genre != null && song.genre == baseSong.genre) ||
                    song.artist == baseSong.artist
                )
            }.shuffled().take(20)

            if (similarSongs.isEmpty()) {
                // Last resort: random songs
                val randomSongs = allSongs.filter { it.id != baseSong.id }
                    .shuffled()
                    .take(20)
                
                if (randomSongs.isEmpty()) return false

                _queueState.value = _queueState.value.copy(
                    currentPlaylist = listOf(baseSong) + randomSongs,
                    currentIndex = 0
                )
                return true
            }

            _queueState.value = _queueState.value.copy(
                currentPlaylist = listOf(baseSong) + similarSongs,
                currentIndex = 0
            )
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    /**
     * Check if queue needs refilling (less than threshold songs remaining)
     */
    fun shouldRefillQueue(threshold: Int = 3): Boolean {
        val upcomingCount = getUpcomingQueue().size
        return upcomingCount < threshold
    }
}

/**
 * State representing the current queue
 */
data class QueueState(
    val currentPlaylist: List<Song> = emptyList(),
    val currentIndex: Int = 0,
    val playHistory: List<Song> = emptyList()
)
