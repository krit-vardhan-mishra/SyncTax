package com.just_for_fun.youtubemusic.core.network

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
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
        private const val INNERTUBE_API_KEY = "AIzaSyC9XL3ZjWddXya6X74dJoCTL-WEYFDNX30"
        private const val BASE_URL = "https://music.youtube.com/youtubei/v1"
        
        // Client configuration for YouTube Music Web
        private const val CLIENT_NAME = "WEB_REMIX"
        private const val CLIENT_VERSION = "1.20240403.01.00"
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
                
                // Make API call
                val url = "$BASE_URL/search?key=$INNERTUBE_API_KEY&prettyPrint=false"
                val response = makePostRequest(url, requestBody)
                
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
     */
    suspend fun getStreamUrl(videoId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val requestBody = buildPlayerRequest(videoId)
                val url = "$BASE_URL/player?key=$INNERTUBE_API_KEY&prettyPrint=false"
                val response = makePostRequest(url, requestBody)
                
                val jsonResponse = JSONObject(response)
                
                // Check playability status
                val playabilityStatus = jsonResponse.optJSONObject("playabilityStatus")
                val status = playabilityStatus?.optString("status")
                
                if (status != "OK") {
                    Log.w(TAG, "Video $videoId not playable: $status - ${playabilityStatus?.optString("reason")}")
                    return@withContext null
                }
                
                // Get streaming data
                val streamingData = jsonResponse.optJSONObject("streamingData")
                val adaptiveFormats = streamingData?.optJSONArray("adaptiveFormats")
                
                if (adaptiveFormats != null) {
                    // Find best audio stream
                    var bestAudioUrl: String? = null
                    var bestBitrate = 0
                    
                    for (i in 0 until adaptiveFormats.length()) {
                        val format = adaptiveFormats.optJSONObject(i)
                        val mimeType = format?.optString("mimeType") ?: continue
                        
                        // Only consider audio formats
                        if (mimeType.startsWith("audio/")) {
                            val url = format.optString("url")
                            val bitrate = format.optInt("bitrate", 0)
                            
                            if (url.isNotEmpty() && bitrate > bestBitrate) {
                                bestAudioUrl = url
                                bestBitrate = bitrate
                            }
                        }
                    }
                    
                    if (bestAudioUrl != null) {
                        Log.d(TAG, "Found stream URL for $videoId (bitrate: $bestBitrate)")
                        return@withContext bestAudioUrl
                    }
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
                    "clientName": "$CLIENT_NAME",
                    "clientVersion": "$CLIENT_VERSION",
                    "gl": "US",
                    "hl": "en"
                }
            },
            "query": "$query"
        }
        """.trimIndent()
    }

    private fun buildPlayerRequest(videoId: String): String {
        return """
        {
            "context": {
                "client": {
                    "clientName": "$CLIENT_NAME",
                    "clientVersion": "$CLIENT_VERSION",
                    "gl": "US",
                    "hl": "en"
                }
            },
            "videoId": "$videoId"
        }
        """.trimIndent()
    }

    private fun makePostRequest(urlString: String, body: String): String {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        
        try {
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
            conn.setRequestProperty("Accept", "application/json")
            conn.setRequestProperty("X-Goog-Api-Format-Version", "1")
            conn.setRequestProperty("X-YouTube-Client-Name", "67")
            conn.setRequestProperty("X-YouTube-Client-Version", CLIENT_VERSION)
            conn.setRequestProperty("Origin", "https://music.youtube.com")
            conn.setRequestProperty("Referer", "https://music.youtube.com/")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            
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
            
            // Get thumbnail
            val thumbnail = listItem.optJSONObject("thumbnail")
                ?.optJSONObject("musicThumbnailRenderer")
                ?.optJSONObject("thumbnail")
                ?.optJSONArray("thumbnails")
                ?.optJSONObject(0)
                ?.optString("url")
            
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
