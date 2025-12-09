package com.just_for_fun.synctax.core.service

import android.util.Log
import com.just_for_fun.synctax.core.dispatcher.AppDispatchers
import com.just_for_fun.synctax.core.network.OnlineSearchResult
import com.just_for_fun.synctax.core.network.YouTubeInnerTubeClient
import com.just_for_fun.synctax.data.local.dao.OnlineListeningHistoryDao
import com.just_for_fun.synctax.data.local.dao.RecommendationCacheDao
import com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory
import com.just_for_fun.synctax.data.local.entities.RecommendationCache
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Core service that generates recommendations using various algorithms.
 * All network operations run on AppDispatchers.Network.
 * All database operations run on AppDispatchers.Database.
 */
class RecommendationService(
    private val analytics: ListeningAnalyticsService,
    private val ytClient: YouTubeInnerTubeClient,
    private val historyDao: OnlineListeningHistoryDao,
    private val cacheDao: RecommendationCacheDao
) {
    companion object {
        private const val TAG = "RecommendationService"
        private const val CACHE_DURATION_MS = 24 * 60 * 60 * 1000L // 24 hours
    }
    
    private val json = Json { 
        ignoreUnknownKeys = true 
        encodeDefaults = true
    }
    
    /**
     * Serializable data class for caching recommendations.
     */
    @Serializable
    data class CachedSong(
        val id: String,
        val title: String,
        val artist: String?,
        val thumbnailUrl: String?,
        val duration: Long?
    )
    
    /**
     * Result containing all recommendation categories.
     */
    data class RecommendationResult(
        val artistBased: List<OnlineSearchResult>,
        val similarSongs: List<OnlineSearchResult>,
        val discovery: List<OnlineSearchResult>,
        val trending: List<OnlineSearchResult>
    )
    
    /**
     * Generates recommendations from cached data or fresh API calls.
     * Runs heavy operations on background dispatchers.
     */
    suspend fun generateRecommendations(forceRefresh: Boolean = false): RecommendationResult {
        // Check cache first (unless forcing refresh)
        if (!forceRefresh) {
            val cached = getCachedRecommendations()
            if (cached != null) {
                Log.d(TAG, "Returning cached recommendations")
                return cached
            }
        }
        
        Log.d(TAG, "Generating fresh recommendations")
        
        // Get user preferences on database dispatcher
        val preferences = analytics.getUserPreferences()
        val recentHistory = withContext(AppDispatchers.Database) {
            historyDao.getRecentHistory(50)
        }
        
        // Generate recommendations on network dispatcher
        val result = withContext(AppDispatchers.Network) {
            RecommendationResult(
                artistBased = filterSongsOnly(generateArtistBasedRecommendations(preferences.topArtists)),
                similarSongs = filterSongsOnly(generateSimilarSongRecommendations(recentHistory)),
                discovery = filterSongsOnly(generateDiscoveryRecommendations(recentHistory, preferences)),
                trending = filterSongsOnly(getTrendingRecommendations())
            )
        }
        
        // Cache results on database dispatcher
        cacheRecommendations(result)
        
        return result
    }

    /**
     * Generates personalized recommendations based on user inputs.
     * Takes precedence over cached recommendations for user-input-based results.
     */
    suspend fun generateUserInputRecommendations(
        userInputs: com.just_for_fun.synctax.presentation.screens.UserRecommendationInputs
    ): RecommendationResult {
        Log.d(TAG, "Generating user-input-based recommendations")

        // Generate recommendations on network dispatcher
        val result = withContext(AppDispatchers.Network) {
            RecommendationResult(
                artistBased = filterSongsOnly(generateUserArtistBasedRecommendations(userInputs.artists)),
                similarSongs = filterSongsOnly(generateUserSongBasedRecommendations(userInputs.songs)),
                discovery = filterSongsOnly(generateUserDiscoveryRecommendations(userInputs)),
                trending = filterSongsOnly(getTrendingRecommendations())
            )
        }

        // Don't cache user-input recommendations as they are personalized
        return result
    }
    
    /**
     * Filters results to only include songs (not videos, episodes, etc.).
     * Songs typically have a duration and don't have typical video/episode indicators.
     */
    private fun filterSongsOnly(results: List<OnlineSearchResult>): List<OnlineSearchResult> {
        return results.filter { result ->
            // Must have a duration (indicates it's playable content)
            val hasDuration = result.duration != null && result.duration > 0
            
            // Filter out common non-song indicators in title
            val title = result.title.lowercase()
            val isLikelyNotSong = title.contains("episode") ||
                    title.contains("podcast") ||
                    title.contains("full movie") ||
                    title.contains("official trailer") ||
                    title.contains("interview") ||
                    title.contains("documentary") ||
                    title.contains("live stream") ||
                    title.contains("reaction") ||
                    title.contains("tutorial") ||
                    title.contains("how to")
            
            // Duration check - songs are typically between 1-15 minutes
            val durationInMinutes = (result.duration ?: 0) / 60
            val hasReasonableDuration = durationInMinutes in 1..15
            
            hasDuration && !isLikelyNotSong && hasReasonableDuration
        }
    }
    
    /**
     * Generates recommendations based on user's top artists.
     */
    private suspend fun generateArtistBasedRecommendations(topArtists: List<String>): List<OnlineSearchResult> {
        val recommendations = mutableListOf<OnlineSearchResult>()
        
        for (artist in topArtists.take(5)) {
            try {
                // Search for artist's popular songs
                val searchResults = ytClient.search("$artist popular songs", limit = 5)
                recommendations.addAll(searchResults)
                Log.d(TAG, "Found ${searchResults.size} songs for artist: $artist")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get recommendations for artist: $artist", e)
            }
        }
        
        return recommendations.distinctBy { it.id }.take(20)
    }
    
    /**
     * Generates recommendations based on songs user listened to with high completion rate.
     */
    private suspend fun generateSimilarSongRecommendations(
        history: List<OnlineListeningHistory>
    ): List<OnlineSearchResult> {
        val recommendations = mutableListOf<OnlineSearchResult>()
        
        // Get songs with high completion rates (user liked them)
        val likedSongs = history
            .filter { it.completionRate > 0.6f && it.skipCount == 0 }
            .sortedByDescending { it.timestamp }
            .take(5)
        
        for (song in likedSongs) {
            try {
                // Search for similar songs
                val searchResults = ytClient.search(
                    "songs similar to ${song.title} by ${song.artist}", 
                    limit = 4
                )
                recommendations.addAll(searchResults)
                Log.d(TAG, "Found ${searchResults.size} similar songs for: ${song.title}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get similar songs for: ${song.title}", e)
            }
        }
        
        return recommendations.distinctBy { it.id }.take(20)
    }
    
    /**
     * Generates discovery recommendations - new artists based on user preferences.
     */
    private suspend fun generateDiscoveryRecommendations(
        history: List<OnlineListeningHistory>,
        preferences: ListeningAnalyticsService.UserPreferencesData
    ): List<OnlineSearchResult> {
        val recommendations = mutableListOf<OnlineSearchResult>()
        val listenedArtists = history.map { it.artist.lowercase() }.toSet()
        
        val topArtist = preferences.topArtists.firstOrNull()
        
        if (topArtist != null) {
            try {
                // Search for artists similar to top artist
                val searchResults = ytClient.search("artists like $topArtist music", limit = 10)
                
                // Filter out songs from artists user already listened to
                val newArtistSongs = searchResults.filter { result ->
                    result.author?.lowercase()?.let { it !in listenedArtists } ?: true
                }
                
                recommendations.addAll(newArtistSongs)
                Log.d(TAG, "Found ${newArtistSongs.size} discovery songs")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get discovery recommendations", e)
            }
        }
        
        // If no discovery results, fall back to trending
        if (recommendations.isEmpty()) {
            return getTrendingRecommendations().take(15)
        }
        
        return recommendations.distinctBy { it.id }.take(15)
    }
    
    /**
     * Gets trending music recommendations.
     */
    private suspend fun getTrendingRecommendations(): List<OnlineSearchResult> {
        return try {
            val results = ytClient.search("trending music 2024", limit = 15)
            Log.d(TAG, "Found ${results.size} trending songs")
            results
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get trending recommendations", e)
            emptyList()
        }
    }
    
    /**
     * Retrieves cached recommendations if still valid.
     */
    private suspend fun getCachedRecommendations(): RecommendationResult? = 
        withContext(AppDispatchers.Database) {
            try {
                val currentTime = System.currentTimeMillis()
                
                val artistBased = cacheDao.getValidCache("artist_based", currentTime)
                val similarSongs = cacheDao.getValidCache("similar_songs", currentTime)
                val discovery = cacheDao.getValidCache("discovery", currentTime)
                val trending = cacheDao.getValidCache("trending", currentTime)
                
                if (artistBased != null && similarSongs != null && 
                    discovery != null && trending != null) {
                    
                    return@withContext RecommendationResult(
                        artistBased = deserializeSongs(artistBased.recommendationsJson),
                        similarSongs = deserializeSongs(similarSongs.recommendationsJson),
                        discovery = deserializeSongs(discovery.recommendationsJson),
                        trending = deserializeSongs(trending.recommendationsJson)
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get cached recommendations", e)
            }
            null
        }
    
    /**
     * Caches recommendations to database.
     */
    private suspend fun cacheRecommendations(result: RecommendationResult) = 
        withContext(AppDispatchers.Database) {
            try {
                val expiryTime = System.currentTimeMillis() + CACHE_DURATION_MS
                
                // Clean up expired cache first
                cacheDao.deleteExpired()
                
                cacheDao.insert(RecommendationCache(
                    cacheKey = "artist_based",
                    recommendationsJson = serializeSongs(result.artistBased),
                    expiresAt = expiryTime
                ))
                
                cacheDao.insert(RecommendationCache(
                    cacheKey = "similar_songs",
                    recommendationsJson = serializeSongs(result.similarSongs),
                    expiresAt = expiryTime
                ))
                
                cacheDao.insert(RecommendationCache(
                    cacheKey = "discovery",
                    recommendationsJson = serializeSongs(result.discovery),
                    expiresAt = expiryTime
                ))
                
                cacheDao.insert(RecommendationCache(
                    cacheKey = "trending",
                    recommendationsJson = serializeSongs(result.trending),
                    expiresAt = expiryTime
                ))
                
                Log.d(TAG, "Cached recommendations successfully")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to cache recommendations", e)
            }
        }
    
    /**
     * Serializes songs to JSON for caching.
     */
    private fun serializeSongs(songs: List<OnlineSearchResult>): String {
        val cachedSongs = songs.map {
            CachedSong(
                id = it.id,
                title = it.title,
                artist = it.author,
                thumbnailUrl = it.thumbnailUrl,
                duration = it.duration
            )
        }
        return json.encodeToString(cachedSongs)
    }

    /**
     * Generates artist-based recommendations from user inputs.
     */
    private suspend fun generateUserArtistBasedRecommendations(userArtists: List<String>): List<OnlineSearchResult> {
        val recommendations = mutableListOf<OnlineSearchResult>()

        for (artist in userArtists.take(5)) {
            try {
                // Search for artist's popular songs
                val searchResults = ytClient.search("$artist popular songs", limit = 6)
                recommendations.addAll(searchResults)
                Log.d(TAG, "Found ${searchResults.size} songs for user-input artist: $artist")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get recommendations for user-input artist: $artist", e)
            }
        }

        return recommendations.distinctBy { it.id }.take(25)
    }

    /**
     * Generates song-based recommendations from user inputs.
     */
    private suspend fun generateUserSongBasedRecommendations(userSongs: List<String>): List<OnlineSearchResult> {
        val recommendations = mutableListOf<OnlineSearchResult>()

        for (song in userSongs.take(5)) {
            try {
                // Search for songs similar to user-input songs
                val searchResults = ytClient.search("songs similar to $song", limit = 5)
                recommendations.addAll(searchResults)
                Log.d(TAG, "Found ${searchResults.size} similar songs for user-input song: $song")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get similar songs for user-input song: $song", e)
            }
        }

        return recommendations.distinctBy { it.id }.take(25)
    }

    /**
     * Generates discovery recommendations based on user inputs (albums and genres).
     */
    private suspend fun generateUserDiscoveryRecommendations(
        userInputs: com.just_for_fun.synctax.presentation.screens.UserRecommendationInputs
    ): List<OnlineSearchResult> {
        val recommendations = mutableListOf<OnlineSearchResult>()

        // Use albums for discovery
        for (album in userInputs.albums.take(3)) {
            try {
                val searchResults = ytClient.search("songs from album $album", limit = 4)
                recommendations.addAll(searchResults)
                Log.d(TAG, "Found ${searchResults.size} songs from user-input album: $album")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get songs from user-input album: $album", e)
            }
        }

        // Use genres for discovery
        for (genre in userInputs.genres.take(3)) {
            try {
                val searchResults = ytClient.search("popular $genre songs", limit = 5)
                recommendations.addAll(searchResults)
                Log.d(TAG, "Found ${searchResults.size} songs in user-input genre: $genre")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get songs in user-input genre: $genre", e)
            }
        }

        return recommendations.distinctBy { it.id }.take(25)
    }
    
    /**
     * Deserializes songs from cached JSON.
     */
    private fun deserializeSongs(jsonString: String): List<OnlineSearchResult> {
        return try {
            val cachedSongs: List<CachedSong> = json.decodeFromString(jsonString)
            cachedSongs.map { 
                OnlineSearchResult(
                    id = it.id,
                    title = it.title,
                    author = it.artist,
                    thumbnailUrl = it.thumbnailUrl,
                    duration = it.duration,
                    streamUrl = null
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to deserialize cached songs", e)
            emptyList()
        }
    }
}
