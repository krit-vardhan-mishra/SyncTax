package com.just_for_fun.synctax.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import com.just_for_fun.synctax.core.network.NewPipeUtils
import com.just_for_fun.synctax.BuildConfig
import java.net.HttpURLConnection
import java.net.URL

/**
 * YouTube InnerTube API client for searching and streaming YouTube Music content.
 * Based on the implementation used by OuterTune and Spotube apps.
 * 
 * InnerTube is YouTube's internal API that powers their official clients.
 * It's more reliable than third-party frontends like Piped/Invidious.
 */
class YouTubeInnerTubeClient {

    companion object {
        private const val TAG = "YouTubeInnerTube"
        
        // WEB_REMIX for search (YouTube Music)
        private const val MUSIC_BASE_URL = "https://music.youtube.com/youtubei/v1"
        private val MUSIC_API_KEY = BuildConfig.MUSIC_API_KEY
        private const val MUSIC_CLIENT_NAME = "WEB_REMIX"
        private const val MUSIC_CLIENT_VERSION = "1.20240918.01.00"
        
        // WEB client for player (works with visitor_data, uses NewPipe for cipher)
        private const val PLAYER_BASE_URL = "https://www.youtube.com/youtubei/v1"
        private val PLAYER_API_KEY = BuildConfig.PLAYER_API_KEY
        private const val PLAYER_CLIENT_NAME = "WEB"
        private const val PLAYER_CLIENT_VERSION = "2.20241111.00.00"
        
        // Cache visitor data to avoid fetching on every request
        @Volatile
        private var cachedVisitorData: String? = null
        private var visitorDataExpiry: Long = 0
    }

    /**
     * Get visitor_data from YouTube to bypass bot detection
     */
    private suspend fun getVisitorData(): String? {
        // Return cached data if still valid (cache for 1 hour)
        val now = System.currentTimeMillis()
        if (cachedVisitorData != null && now < visitorDataExpiry) {
            return cachedVisitorData
        }
        
        return withContext(Dispatchers.IO) {
            try {
                val url = URL("https://www.youtube.com/")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "GET"
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                conn.connectTimeout = 10000
                conn.readTimeout = 10000
                
                val html = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                
                // Extract visitor_data from ytcfg.set or JSON
                val regex = """"VISITOR_DATA":"([^"]+)""".toRegex()
                val match = regex.find(html)
                
                if (match != null) {
                    val visitorData = match.groupValues[1]
                    cachedVisitorData = visitorData
                    visitorDataExpiry = now + 3600000 // 1 hour
                    Log.d(TAG, "Fetched visitor_data: ${visitorData.take(20)}...")
                    return@withContext visitorData
                }
                
                Log.w(TAG, "Could not extract visitor_data from YouTube")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch visitor_data", e)
                null
            }
        }
    }
    
    /**
     * Search for music on YouTube Music
     */
    suspend fun search(query: String, limit: Int = 12): List<OnlineSearchResult> {
        return withContext(Dispatchers.IO) {
            try {
                val results = mutableListOf<OnlineSearchResult>()
                
                // Build search request body
                val requestBody = buildSearchRequest(query)
                
                // Make API call to YouTube Music
                val url = "$MUSIC_BASE_URL/search?key=$MUSIC_API_KEY&prettyPrint=false"
                val response = makePostRequest(url, requestBody, forPlayer = false)
                
                // Parse response
                val jsonResponse = JSONObject(response)
                
                // Navigate to search results
                val contents = jsonResponse.optJSONObject("contents")
                    ?.optJSONObject("tabbedSearchResultsRenderer")
                    ?.optJSONArray("tabs")
                    ?.optJSONObject(0)
                    ?.optJSONObject("tabRenderer")
                    ?.optJSONObject("content")
                    ?.optJSONObject("sectionListRenderer")
                    ?.optJSONArray("contents")
                
                if (contents != null) {
                    // Find the music shelf with actual results
                    for (i in 0 until contents.length()) {
                        val section = contents.optJSONObject(i)
                        val musicShelf = section?.optJSONObject("musicShelfRenderer")
                        
                        if (musicShelf != null) {
                            val items = musicShelf.optJSONArray("contents")
                            if (items != null) {
                                for (j in 0 until minOf(items.length(), limit)) {
                                    val item = items.optJSONObject(j)
                                    val listItem = item?.optJSONObject("musicResponsiveListItemRenderer")
                                    
                                    if (listItem != null) {
                                        parseSearchResult(listItem)?.let { results.add(it) }
                                    }
                                }
                            }
                            break
                        }
                    }
                }
                
                Log.d(TAG, "Search completed: found ${results.size} results for '$query'")
                results
            } catch (e: Exception) {
                Log.e(TAG, "Search failed for query: $query", e)
                emptyList()
            }
        }
    }

    /**
     * Get playable stream URL for a video
     * 
     * YouTube InnerTube API now returns incomplete format objects (no URL, no cipher).
     * Using NewPipe extractor directly which implements YouTube's client logic fully.
     */
    suspend fun getStreamUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Getting stream URL for $videoId using NewPipe extractor")
                
                // Use NewPipe extractor - it handles all YouTube client logic internally
                val result = NewPipeUtils.getStreamUrl(videoId)
                
                if (result.isSuccess) {
                    val streamUrl = result.getOrNull()
                    if (streamUrl != null) {
                        Log.d(TAG, "✅ NewPipe got stream URL for $videoId: ${streamUrl.take(100)}...")
                        return@withContext streamUrl
                    }
                } else {
                    Log.e(TAG, "❌ NewPipe failed for $videoId", result.exceptionOrNull())
                }
                
                Log.w(TAG, "No audio stream found for $videoId")
                null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to get stream URL for $videoId", e)
                null
            }
        }
    }

    private fun buildSearchRequest(query: String): String {
        return """
        {
            "context": {
                "client": {
                    "clientName": "$MUSIC_CLIENT_NAME",
                    "clientVersion": "$MUSIC_CLIENT_VERSION",
                    "gl": "US",
                    "hl": "en"
                }
            },
            "query": "$query"
        }
        """.trimIndent()
    }

    private suspend fun buildPlayerRequest(videoId: String): String {
        // Use WEB client with visitor_data to bypass bot detection
        // Streams will be cipher-protected but NewPipe handles that
        val visitorData = getVisitorData()
        
        if (visitorData != null) {
            Log.d(TAG, "Using visitor_data for $videoId")
        } else {
            Log.w(TAG, "No visitor_data available for $videoId - may hit bot detection")
        }
        
        val clientContext = if (visitorData != null) {
            """
                "clientName": "$PLAYER_CLIENT_NAME",
                "clientVersion": "$PLAYER_CLIENT_VERSION",
                "hl": "en",
                "gl": "US",
                "visitorData": "$visitorData"
            """.trimIndent()
        } else {
            """
                "clientName": "$PLAYER_CLIENT_NAME",
                "clientVersion": "$PLAYER_CLIENT_VERSION",
                "hl": "en",
                "gl": "US"
            """.trimIndent()
        }
        
        return """
        {
            "context": {
                "client": {
                    $clientContext
                }
            },
            "videoId": "$videoId",
            "contentCheckOk": true,
            "racyCheckOk": true
        }
        """.trimIndent()
    }

    private fun makePostRequest(urlString: String, body: String, forPlayer: Boolean = false): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("Accept", "application/json")
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            
            if (forPlayer) {
                // WEB client headers for player endpoint
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36")
                conn.setRequestProperty("X-YouTube-Client-Name", "1")  // WEB = 1
                conn.setRequestProperty("X-YouTube-Client-Version", PLAYER_CLIENT_VERSION)
                conn.setRequestProperty("Origin", "https://www.youtube.com")
                conn.setRequestProperty("Referer", "https://www.youtube.com/")
            } else {
                // WEB_REMIX client headers for search
                conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                conn.setRequestProperty("X-Goog-Api-Format-Version", "1")
                conn.setRequestProperty("X-YouTube-Client-Name", "67")  // WEB_REMIX = 67
                conn.setRequestProperty("X-YouTube-Client-Version", MUSIC_CLIENT_VERSION)
                conn.setRequestProperty("Origin", "https://music.youtube.com")
                conn.setRequestProperty("Referer", "https://music.youtube.com/")
            }
            
            // Write request body
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            
            // Read response
            val responseCode = conn.responseCode
            if (responseCode == 200) {
                return conn.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorBody = conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
                throw Exception("HTTP $responseCode: $errorBody")
            }
        } finally {
            conn.disconnect()
        }
    }

    private fun parseSearchResult(listItem: JSONObject): OnlineSearchResult? {
        try {
            // Get video ID
            val playlistItemData = listItem.optJSONObject("playlistItemData")
            val videoId = playlistItemData?.optString("videoId") ?: return null
            
            // Check if this is a song (has WATCH endpoint, not BROWSE for albums/playlists)
            val navigationEndpoint = listItem.optJSONObject("overlay")
                ?.optJSONObject("musicItemThumbnailOverlayRenderer")
                ?.optJSONObject("content")
                ?.optJSONObject("musicPlayButtonRenderer")
                ?.optJSONObject("playNavigationEndpoint")
            
            // Only include items with watch endpoint (songs/videos)
            if (navigationEndpoint == null || !navigationEndpoint.has("watchEndpoint")) {
                Log.d(TAG, "Skipping non-song result: $videoId")
                return null
            }
            
            // Get title
            val flexColumns = listItem.optJSONArray("flexColumns")
            val title = flexColumns?.optJSONObject(0)
                ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                ?.optJSONObject("text")
                ?.optJSONArray("runs")
                ?.optJSONObject(0)
                ?.optString("text") ?: return null
            
            // Get artist and other metadata
            var artist: String? = null
            var duration: Long? = null
            
            if (flexColumns != null && flexColumns.length() > 1) {
                val metadataColumn = flexColumns.optJSONObject(1)
                    ?.optJSONObject("musicResponsiveListItemFlexColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")
                
                if (metadataColumn != null && metadataColumn.length() > 0) {
                    artist = metadataColumn.optJSONObject(0)?.optString("text")
                }
            }
            
            // Get thumbnail - use higher quality image (last thumbnail in array)
            val thumbnails = listItem.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
            
            var thumbnail: String? = null
            if (thumbnails != null && thumbnails.length() > 0) {
                // Get the highest quality thumbnail (last one in array)
                val thumbnailObj = thumbnails.optJSONObject(thumbnails.length() - 1)
                thumbnail = thumbnailObj?.optString("url")
                
                // Handle protocol-relative URLs (//i.ytimg.com/...)
                if (thumbnail?.startsWith("//") == true) {
                    thumbnail = "https:$thumbnail"
                }
                
                // Upgrade to higher quality thumbnail
                thumbnail = thumbnail?.replace("/default.jpg", "/hqdefault.jpg")
                    ?.replace("/mqdefault.jpg", "/hqdefault.jpg")
                    ?.replace("/sddefault.jpg", "/hqdefault.jpg")
            }
            
            // Get fixed run (duration)
            val fixedColumns = listItem.optJSONArray("fixedColumns")
            if (fixedColumns != null && fixedColumns.length() > 0) {
                val durationText = fixedColumns.optJSONObject(0)
                    ?.optJSONObject("musicResponsiveListItemFixedColumnRenderer")
                    ?.optJSONObject("text")
                    ?.optJSONArray("runs")
                    ?.optJSONObject(0)
                    ?.optString("text")
                
                if (durationText != null) {
                    duration = parseDuration(durationText)
                }
            }
            
            return OnlineSearchResult(
                id = videoId,
                title = title,
                author = artist,
                duration = duration,
                thumbnailUrl = thumbnail,
                streamUrl = null // Will be fetched when user tries to play
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse search result", e)
            return null
        }
    }

    private fun parseDuration(durationText: String): Long? {
        return try {
            val parts = durationText.split(":")
            when (parts.size) {
                2 -> { // MM:SS
                    val minutes = parts[0].toLongOrNull() ?: 0
                    val seconds = parts[1].toLongOrNull() ?: 0
                    (minutes * 60 + seconds)
                }
                3 -> { // HH:MM:SS
                    val hours = parts[0].toLongOrNull() ?: 0
                    val minutes = parts[1].toLongOrNull() ?: 0
                    val seconds = parts[2].toLongOrNull() ?: 0
                    (hours * 3600 + minutes * 60 + seconds)
                }
                else -> null
            }
        } catch (e: Exception) {
            null
        }
    }
}
