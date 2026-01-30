package com.just_for_fun.synctax.core.service

import android.content.Context
import android.util.Log
import com.just_for_fun.synctax.core.dispatcher.AppDispatchers
import com.just_for_fun.synctax.core.ml.MusicRecommendationManager
import com.just_for_fun.synctax.core.network.OnlineResultType
import com.just_for_fun.synctax.core.network.OnlineSearchManager
import com.just_for_fun.synctax.core.network.OnlineSearchResult
import com.just_for_fun.synctax.core.player.StreamUrlCache
import com.just_for_fun.synctax.core.utils.YTMusicRecommender
import com.just_for_fun.synctax.data.local.MusicDatabase
import com.just_for_fun.synctax.data.local.dao.OnlineListeningHistoryDao
import com.just_for_fun.synctax.data.local.dao.QuickPickDao
import com.just_for_fun.synctax.data.local.dao.SongDao
import com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory
import com.just_for_fun.synctax.data.local.entities.QuickPick
import com.just_for_fun.synctax.data.local.entities.QuickPickSong
import com.just_for_fun.synctax.data.local.entities.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Service for managing Quick Pick recommendations.
 * Handles both offline (local songs) and online (YouTube Music) recommendations.
 * 
 * The service maintains separate queues for offline and online modes:
 * - Offline: Uses ML-based recommendations from local songs
 * - Online: Uses YouTube Music API for streaming recommendations
 */
class QuickPickService(private val context: Context) {
    
    companion object {
        private const val TAG = "QuickPickService"
        const val QUEUE_SIZE = 10  // Number of songs to keep in queue
        const val VISIBLE_QUEUE_SIZE = 3  // Number of "next" songs visible to user
        const val MAX_SEED_SONGS = 10  // Maximum seed songs to use for recommendations
        const val SOURCE_OFFLINE = "offline"
        const val SOURCE_ONLINE = "online"
    }
    
    private val database = MusicDatabase.getDatabase(context)
    private val quickPickDao: QuickPickDao = database.quickPickDao()
    private val songDao: SongDao = database.songDao()
    private val onlineHistoryDao: OnlineListeningHistoryDao = database.onlineListeningHistoryDao()
    private val recommendationManager = MusicRecommendationManager(context)
    
    // ==================== Seed Song Management ====================
    
    /**
     * Record a song play to update Quick Pick seeds.
     * This is called whenever a song is played to build the recommendation base.
     */
    suspend fun recordSongPlay(
        songId: String,
        title: String,
        artist: String,
        thumbnailUrl: String?,
        duration: Long = 0,
        isOnline: Boolean,
        genre: String? = null,
        album: String? = null,
        filePath: String? = null
    ) = withContext(AppDispatchers.Database) {
        try {
            val source = if (isOnline) SOURCE_ONLINE else SOURCE_OFFLINE
            val existingPick = quickPickDao.getQuickPickBySongId(songId)
            
            if (existingPick != null) {
                // Update existing seed
                quickPickDao.updatePlayCount(songId)
                Log.d(TAG, "Updated play count for seed: $title ($source)")
            } else {
                // Insert new seed
                val quickPick = QuickPick(
                    songId = songId,
                    title = title,
                    artist = artist,
                    thumbnailUrl = thumbnailUrl,
                    duration = duration,
                    source = source,
                    genre = genre,
                    album = album
                )
                quickPickDao.insertQuickPick(quickPick)
                Log.d(TAG, "Added new seed: $title ($source)")
            }
            
            // Regenerate queue after adding new seed
            if (isOnline) {
                generateOnlineQueue()
            } else {
                generateOfflineQueue()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error recording song play: ${e.message}", e)
        }
    }
    
    /**
     * Record a song play without regenerating the queue.
     * Used by QuickPickScreen to avoid cascading updates during continuous playback.
     */
    suspend fun recordSongPlayOnly(
        songId: String,
        title: String,
        artist: String,
        thumbnailUrl: String?,
        duration: Long = 0,
        isOnline: Boolean,
        genre: String? = null,
        album: String? = null,
        filePath: String? = null
    ) = withContext(AppDispatchers.Database) {
        try {
            val source = if (isOnline) SOURCE_ONLINE else SOURCE_OFFLINE
            val existingPick = quickPickDao.getQuickPickBySongId(songId)
            
            if (existingPick != null) {
                // Update existing seed
                quickPickDao.updatePlayCount(songId)
                Log.d(TAG, "Updated play count for seed (no refresh): $title ($source)")
            } else {
                // Insert new seed
                val quickPick = QuickPick(
                    songId = songId,
                    title = title,
                    artist = artist,
                    thumbnailUrl = thumbnailUrl,
                    duration = duration,
                    source = source,
                    genre = genre,
                    album = album
                )
                quickPickDao.insertQuickPick(quickPick)
                Log.d(TAG, "Added new seed (no refresh): $title ($source)")
            }
            // Note: Queue is NOT regenerated here to prevent cascading updates
        } catch (e: Exception) {
            Log.e(TAG, "Error recording song play (no refresh): ${e.message}", e)
        }
    }
    
    /**
     * Check if user has any seeds for a given source.
     * For online mode, also checks OnlineListeningHistory.
     */
    suspend fun hasSeeds(source: String): Boolean = withContext(AppDispatchers.Database) {
        if (source == SOURCE_ONLINE) {
            // For online, check listening history (primary source)
            val historyCount = onlineHistoryDao.getRecentHistory(1).size
            historyCount > 0 || quickPickDao.getQuickPickCount(source) > 0
        } else {
            quickPickDao.getQuickPickCount(source) > 0
        }
    }
    
    /**
     * Get all seeds for a source
     */
    suspend fun getSeeds(source: String): List<QuickPick> = withContext(AppDispatchers.Database) {
        quickPickDao.getQuickPicksBySource(source, MAX_SEED_SONGS)
    }
    
    // ==================== Offline Queue Generation ====================
    
    /**
     * Generate recommendations for offline mode using ML-based scoring.
     * Uses local songs and listening history to generate personalized recommendations.
     */
    suspend fun generateOfflineQueue() = withContext(AppDispatchers.Database) {
        try {
            Log.d(TAG, "Generating offline queue...")
            
            // Get seed songs
            val seeds = quickPickDao.getQuickPicksBySource(SOURCE_OFFLINE, MAX_SEED_SONGS)
            if (seeds.isEmpty()) {
                Log.d(TAG, "No offline seeds, generating random queue")
                generateRandomOfflineQueue()
                return@withContext
            }
            
            // Get all local songs
            val allSongs = songDao.getAllSongsList()
            if (allSongs.isEmpty()) {
                Log.d(TAG, "No local songs available")
                return@withContext
            }
            
            // Get seed song IDs to exclude from recommendations
            val seedIds = seeds.map { it.songId }.toSet()
            
            // Get current queue song IDs to exclude
            val currentQueue = quickPickDao.getQuickPickQueue(SOURCE_OFFLINE, QUEUE_SIZE)
            val queueIds = currentQueue.map { it.songId }.toSet()
            
            // Filter out seeds and already queued songs
            val candidateSongs = allSongs.filter { 
                it.id !in seedIds && it.id !in queueIds 
            }
            
            if (candidateSongs.isEmpty()) {
                Log.d(TAG, "No candidate songs after filtering")
                return@withContext
            }
            
            // Score songs based on similarity to seeds
            val scoredSongs = scoreSongsForOffline(candidateSongs, seeds)
            
            // Take top songs for queue
            val recommendedSongs = scoredSongs
                .sortedByDescending { it.second }
                .take(QUEUE_SIZE)
                .map { it.first }
            
            // Clear existing queue and insert new recommendations
            quickPickDao.clearQuickPickQueue(SOURCE_OFFLINE)
            
            val quickPickSongs = recommendedSongs.mapIndexed { index, song ->
                QuickPickSong(
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    thumbnailUrl = song.albumArtUri,
                    duration = song.duration,
                    source = SOURCE_OFFLINE,
                    queuePosition = index,
                    basedOnSongId = seeds.firstOrNull()?.songId,
                    filePath = song.filePath
                )
            }
            
            quickPickDao.insertQuickPickSongs(quickPickSongs)
            Log.d(TAG, "Generated ${quickPickSongs.size} offline recommendations")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating offline queue: ${e.message}", e)
        }
    }
    
    /**
     * Generate a random queue when no seeds exist
     */
    private suspend fun generateRandomOfflineQueue() {
        val allSongs = songDao.getAllSongsList()
        if (allSongs.isEmpty()) return
        
        val randomSongs = allSongs.shuffled().take(QUEUE_SIZE)
        
        quickPickDao.clearQuickPickQueue(SOURCE_OFFLINE)
        
        val quickPickSongs = randomSongs.mapIndexed { index, song ->
            QuickPickSong(
                songId = song.id,
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.albumArtUri,
                duration = song.duration,
                source = SOURCE_OFFLINE,
                queuePosition = index,
                recommendationReason = "Random pick",
                filePath = song.filePath
            )
        }
        
        quickPickDao.insertQuickPickSongs(quickPickSongs)
        Log.d(TAG, "Generated ${quickPickSongs.size} random offline songs")
    }
    
    /**
     * Score songs based on similarity to seed songs.
     * Uses genre, artist, and album matching.
     */
    private fun scoreSongsForOffline(
        candidates: List<Song>,
        seeds: List<QuickPick>
    ): List<Pair<Song, Float>> {
        val seedArtists = seeds.map { it.artist.lowercase() }.toSet()
        val seedGenres = seeds.mapNotNull { it.genre?.lowercase() }.toSet()
        val seedAlbums = seeds.mapNotNull { it.album?.lowercase() }.toSet()
        
        return candidates.map { song ->
            var score = 0f
            
            // Artist match (high weight)
            if (song.artist.lowercase() in seedArtists) {
                score += 0.4f
            }
            
            // Genre match (medium weight)
            if (song.genre?.lowercase() in seedGenres) {
                score += 0.3f
            }
            
            // Album match (medium weight)
            if (song.album?.lowercase() in seedAlbums) {
                score += 0.2f
            }
            
            // Add some randomness for variety
            score += (0..10).random() / 100f
            
            Pair(song, score)
        }
    }
    
    // ==================== Online Queue Generation ====================
    
    /**
     * Generate recommendations for online mode using YouTube Music API.
     * Picks a RANDOM song from online listening history as seed,
     * places it first in the queue, then fills with its recommendations.
     */
    suspend fun generateOnlineQueue() = withContext(AppDispatchers.Network) {
        try {
            Log.d(TAG, "Generating online queue from listening history...")
            
            // Get online listening history
            val history = withContext(AppDispatchers.Database) {
                onlineHistoryDao.getRecentHistory(15) // Get last 15 songs from history
            }
            
            if (history.isEmpty()) {
                Log.d(TAG, "No online listening history available")
                return@withContext
            }
            
            // Pick a RANDOM song from history as the seed
            val seedSong = history.random()
            Log.d(TAG, "Selected random seed from history: ${seedSong.title} (${seedSong.videoId})")
            
            // Get recommendations for the seed song
            val recommendations = try {
                getRecommendationsForVideoId(seedSong.videoId)
            } catch (e: Exception) {
                Log.e(TAG, "Error getting recommendations for ${seedSong.videoId}: ${e.message}")
                emptyList()
            }
            
            // Filter out the seed song from recommendations
            val filteredRecommendations = recommendations
                .filter { it.id != seedSong.videoId }
                .filter { it.type == OnlineResultType.SONG }
                .distinctBy { it.id }
                .take(QUEUE_SIZE - 1) // Leave room for the seed song at position 0
            
            Log.d(TAG, "Got ${filteredRecommendations.size} recommendations for ${seedSong.title}")
            
            // Build the queue: seed song first, then its recommendations
            withContext(AppDispatchers.Database) {
                quickPickDao.clearQuickPickQueue(SOURCE_ONLINE)
                
                val queueSongs = mutableListOf<QuickPickSong>()
                
                // Add seed song at position 0
                queueSongs.add(
                    QuickPickSong(
                        songId = seedSong.videoId,
                        title = seedSong.title,
                        artist = seedSong.artist,
                        thumbnailUrl = seedSong.thumbnailUrl,
                        streamUrl = null,
                        duration = seedSong.totalDuration * 1000, // Convert to ms
                        source = SOURCE_ONLINE,
                        queuePosition = 0,
                        basedOnSongId = null,
                        recommendationReason = "From your listening history"
                    )
                )
                
                // Add recommendations starting at position 1
                filteredRecommendations.forEachIndexed { index, result ->
                    queueSongs.add(
                        QuickPickSong(
                            songId = result.id,
                            title = result.title,
                            artist = result.author ?: "Unknown Artist",
                            thumbnailUrl = result.thumbnailUrl,
                            streamUrl = result.streamUrl,
                            duration = result.duration ?: 0L,
                            source = SOURCE_ONLINE,
                            queuePosition = index + 1,
                            basedOnSongId = seedSong.videoId,
                            recommendationReason = "Based on ${seedSong.title}"
                        )
                    )
                }
                
                quickPickDao.insertQuickPickSongs(queueSongs)
                Log.d(TAG, "Generated online queue: 1 seed + ${filteredRecommendations.size} recommendations")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating online queue: ${e.message}", e)
        }
    }
    
    /**
     * Get recommendations for a video ID using YouTube Music API
     */
    private suspend fun getRecommendationsForVideoId(videoId: String): List<OnlineSearchResult> {
        return suspendCancellableCoroutine { continuation ->
            YTMusicRecommender.getRecommendations(
                videoId = videoId,
                limit = 10,
                onResult = { songs ->
                    val results = songs.map { song ->
                        OnlineSearchResult(
                            id = song.videoId,
                            title = song.title,
                            author = song.artist,
                            duration = null,
                            thumbnailUrl = song.thumbnail,
                            streamUrl = null,
                            type = OnlineResultType.SONG
                        )
                    }
                    if (continuation.isActive) {
                        continuation.resume(results)
                    }
                },
                onError = { error ->
                    Log.e(TAG, "Error getting recommendations: $error")
                    if (continuation.isActive) {
                        continuation.resume(emptyList())
                    }
                }
            )
        }
    }
    
    // ==================== Queue Management ====================
    
    /**
     * Observe the Quick Pick queue for a source
     */
    fun observeQueue(source: String): Flow<List<QuickPickSong>> {
        return quickPickDao.observeQuickPickQueue(source, QUEUE_SIZE)
    }
    
    /**
     * Get the current queue for a source
     */
    suspend fun getQueue(source: String): List<QuickPickSong> = withContext(AppDispatchers.Database) {
        quickPickDao.getQuickPickQueue(source, QUEUE_SIZE)
    }
    
    /**
     * Called when a song from the queue is played.
     * Removes it from queue and refreshes recommendations.
     */
    suspend fun onSongPlayed(song: QuickPickSong) = withContext(AppDispatchers.Database) {
        try {
            // Record this play as a new seed
            recordSongPlay(
                songId = song.songId,
                title = song.title,
                artist = song.artist,
                thumbnailUrl = song.thumbnailUrl,
                duration = song.duration,
                isOnline = song.source == SOURCE_ONLINE,
                filePath = song.filePath
            )
            
            // Remove from queue and shift positions
            quickPickDao.removeFromQueueBySongId(song.songId, song.source)
            quickPickDao.shiftQueuePositions(song.source, song.queuePosition)
            
            // Regenerate queue to fill gap
            if (song.source == SOURCE_ONLINE) {
                generateOnlineQueue()
            } else {
                generateOfflineQueue()
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error handling song played: ${e.message}", e)
        }
    }
    
    /**
     * Refresh the queue for a source
     */
    suspend fun refreshQueue(source: String) {
        if (source == SOURCE_ONLINE) {
            generateOnlineQueue()
        } else {
            generateOfflineQueue()
        }
    }
    
    /**
     * Clear all Quick Pick data
     */
    suspend fun clearAllData() = withContext(AppDispatchers.Database) {
        quickPickDao.clearQuickPickQueue(SOURCE_OFFLINE)
        quickPickDao.clearQuickPickQueue(SOURCE_ONLINE)
        // Note: Seeds are not cleared to preserve user preferences
    }
    
    /**
     * Convert QuickPickSong to Song for playback (offline only)
     */
    suspend fun toSong(quickPickSong: QuickPickSong): Song? = withContext(AppDispatchers.Database) {
        if (quickPickSong.source == SOURCE_OFFLINE && quickPickSong.filePath != null) {
            Song(
                id = quickPickSong.songId,
                title = quickPickSong.title,
                artist = quickPickSong.artist,
                album = null,
                duration = quickPickSong.duration,
                filePath = quickPickSong.filePath,
                genre = null,
                releaseYear = null,
                albumArtUri = quickPickSong.thumbnailUrl
            )
        } else {
            // Try to get from database
            songDao.getSongById(quickPickSong.songId)
        }
    }
    
    /**
     * Convert QuickPickSong list to Song list for playback
     */
    suspend fun toSongs(quickPickSongs: List<QuickPickSong>): List<Song> {
        return quickPickSongs.mapNotNull { toSong(it) }
    }
    
    /**
     * Preload stream URL for an online song.
     * This fetches and caches the stream URL so playback is instant.
     */
    suspend fun preloadStreamUrl(videoId: String) = withContext(AppDispatchers.Network) {
        try {
            // Check if already cached
            if (StreamUrlCache.get(videoId) != null) {
                Log.d(TAG, "Stream URL already cached for: $videoId")
                return@withContext
            }
            
            // Fetch and cache the stream URL
            val streamUrl = OnlineSearchManager().getStreamUrl(videoId)
            if (streamUrl != null) {
                StreamUrlCache.put(videoId, streamUrl)
                Log.d(TAG, "✅ Preloaded stream URL for: $videoId")
            } else {
                Log.w(TAG, "⚠️ Failed to preload stream URL for: $videoId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading stream URL for $videoId: ${e.message}")
        }
    }
    
    /**
     * Preload stream URLs for multiple songs in parallel.
     */
    suspend fun preloadStreamUrls(videoIds: List<String>) = withContext(AppDispatchers.Network) {
        Log.d(TAG, "🔄 Preloading ${videoIds.size} stream URLs...")
        videoIds.forEach { videoId ->
            preloadStreamUrl(videoId)
        }
        Log.d(TAG, "✅ Preload complete for ${videoIds.size} songs")
    }
}
