package com.just_for_fun.synctax.core.utils

import android.util.Log
import com.just_for_fun.synctax.BuildConfig
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.URLEncoder
import java.net.HttpURLConnection
import java.net.URL
import java.io.BufferedReader
import java.io.InputStreamReader

data class RecommendedSong(
    val title: String,
    val artist: String,
    val videoId: String,
    val watchUrl: String,
    val thumbnail: String
)

object YoutubeRecommender {
    private const val TAG = "YoutubeRecommender"

    // API keys are provided via BuildConfig fields populated from `local.properties`

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Main public function - call this from your PlayerViewModel
    fun getRecommendations(
        currentYoutubeUrl: String,
        onResult: (List<RecommendedSong>) -> Unit,
        onError: (String) -> Unit = { Log.e(TAG, it) }
    ) {
        scope.launch {
            Log.d(TAG, "getRecommendations() called with url=$currentYoutubeUrl")
            try {
                val videoId = extractVideoId(currentYoutubeUrl)
                Log.d(TAG, "extractVideoId -> $videoId")
                if (videoId.isNullOrEmpty()) {
                    Log.w(TAG, "Invalid YouTube URL provided, can't extract videoId")
                    withContext(Dispatchers.Main) { onError("Invalid YouTube URL") }
                    return@launch
                }

                // Simple approach: Just get popular music videos, no filtering
                val recommendations = getSimpleRecommendations(videoId)

                Log.d(TAG, "Final recommendation list size=${recommendations.size}")
                recommendations.forEachIndexed { idx, rs ->
                    Log.d(TAG, "Rec["+idx+"] id=${rs.videoId} title=${rs.title} artist=${rs.artist} url=${rs.watchUrl}")
                }

                withContext(Dispatchers.Main) {
                    onResult(recommendations)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Error: ${e.message}")
                }
            }
        }
    }

    private suspend fun getSimpleRecommendations(excludeVideoId: String): List<RecommendedSong> = withContext(Dispatchers.IO) {
        // Simple approach: Just search for popular music videos, no filtering
        Log.d(TAG, "getSimpleRecommendations() - searching for popular music")

        val searchTerms = listOf("popular music", "trending music", "music hits")
        val allResults = mutableListOf<RecommendedSong>()

        for (term in searchTerms) {
            val results = searchSimple(term, limit = 5)
            allResults.addAll(results)
        }

        // Shuffle and deduplicate
        val shuffled = allResults.shuffled()
        val seenIds = mutableSetOf<String>()
        val uniqueRecommendations = mutableListOf<RecommendedSong>()

        for (rec in shuffled) {
            if (rec.videoId !in seenIds && rec.videoId != excludeVideoId) {
                uniqueRecommendations.add(rec)
                seenIds.add(rec.videoId)
                if (uniqueRecommendations.size >= 10) break
            }
        }

        Log.d(TAG, "getSimpleRecommendations found ${uniqueRecommendations.size} recommendations")
        return@withContext uniqueRecommendations
    }

    private suspend fun searchSimple(query: String, limit: Int): List<RecommendedSong> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, "UTF-8")
        val rawUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$encoded&type=video&videoCategoryId=10&maxResults=$limit&order=relevance&key=${BuildConfig.YOUTUBE_API_KEY}"
        val safeUrl = rawUrl.replace(Regex("key=[^&]+"), "key=[REDACTED]")
        Log.d(TAG, "searchSimple query='$query' limit=$limit requestUrl=$safeUrl")

        try {
            val url = URL(rawUrl)
            val json = url.readText()
            val obj = JSONObject(json)
            val items = obj.getJSONArray("items")

            Log.d(TAG, "searchSimple response items=${items.length()}")

            val results = mutableListOf<RecommendedSong>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val idObj = item.getJSONObject("id")
                if (idObj.getString("kind") != "youtube#video") continue

                val videoId = idObj.getString("videoId")
                val snippet = item.getJSONObject("snippet")
                val title = snippet.getString("title")
                val channelTitle = snippet.getString("channelTitle")
                val thumbnail = try { snippet.getJSONObject("thumbnails").getJSONObject("high").getString("url") } catch (e: Exception) { "" }

                Log.d(TAG, "search item index=$i videoId=$videoId title='$title' channel='$channelTitle' thumbnail='$thumbnail'")

                results += RecommendedSong(
                    title = title,
                    artist = channelTitle,
                    videoId = videoId,
                    watchUrl = "https://www.youtube.com/watch?v=$videoId",
                    thumbnail = thumbnail
                )
            }
            return@withContext results
        } catch (e: Exception) {
            Log.e(TAG, "Simple search failed for: $query", e)
            return@withContext emptyList()
        }
    }

    private fun extractVideoId(url: String): String? {
        return Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([^#\\&\\?]{11})")
            .find(url)?.groupValues?.getOrNull(1)
    }

    // Helper for URL reading with better error handling
    private fun URL.readText(): String {
        val connection = openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        return try {
            val responseCode = connection.responseCode
            Log.d(TAG, "HTTP Response Code: $responseCode for URL: ${this.toString().replace(Regex("key=[^&]+"), "key=[REDACTED]")}")

            if (responseCode == 200) {
                BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            } else {
                // Read error stream for more details
                val errorStream = connection.errorStream
                val errorMessage = if (errorStream != null) {
                    BufferedReader(InputStreamReader(errorStream)).use { it.readText() }
                } else {
                    "No error details available"
                }
                Log.e(TAG, "HTTP Error $responseCode: $errorMessage")
                throw Exception("HTTP $responseCode: $errorMessage")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network error: ${e.message}", e)
            throw e
        } finally {
            connection.disconnect()
        }
    }
}
