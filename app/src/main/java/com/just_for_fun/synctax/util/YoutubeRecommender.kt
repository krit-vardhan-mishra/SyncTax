package com.just_for_fun.synctax.util

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

                val currentInfo = getVideoDetails(videoId)
                    ?: run { Log.w(TAG, "getVideoDetails returned null for videoId=$videoId"); withContext(Dispatchers.Main) { onError("Failed to get current video info") }; return@launch }

                val recommendations = mutableListOf<RecommendedSong>()

                // Priority 1: 4 songs from same genre (extracted from tags/description)
                recommendations += searchByKeywords(
                    keywords = currentInfo.genreKeywords,
                    excludeTitle = currentInfo.title,
                    limit = 10
                ).shuffled().take(4)

                // Priority 2: 3 songs from same artist
                recommendations += searchByKeywords(
                    keywords = listOf(currentInfo.artist),
                    excludeTitle = currentInfo.title,
                    limit = 8
                ).shuffled().take(3)

                // Priority 3: 2 songs from same album
                if (currentInfo.album.isNotEmpty()) {
                    recommendations += searchByKeywords(
                        keywords = listOf(currentInfo.album, currentInfo.artist),
                        excludeTitle = currentInfo.title,
                        limit = 6
                    ).take(2)
                }

                // Priority 4: 1 song from same year
                recommendations += searchByKeywords(
                    keywords = listOf(currentInfo.year.toString()),
                    excludeTitle = currentInfo.title,
                    limit = 5
                ).shuffled().take(1)

                // Remove duplicates by videoId, keep order
                val finalList = recommendations
                    .distinctBy { it.videoId }
                    .filter { it.videoId != videoId } // exclude current song
                    .take(10)

                Log.d(TAG, "Final recommendation list size=${finalList.size}")
                finalList.forEachIndexed { idx, rs ->
                    Log.d(TAG, "Rec["+idx+"] id=${rs.videoId} title=${rs.title} artist=${rs.artist} url=${rs.watchUrl}")
                }

                withContext(Dispatchers.Main) {
                    onResult(finalList)
                }

            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onError("Error: ${e.message}")
                }
            }
        }
    }

    private data class CurrentVideoInfo(
        val title: String,
        val artist: String,
        val album: String,
        val year: Int,
        val genreKeywords: List<String>
    )

    private suspend fun getVideoDetails(videoId: String): CurrentVideoInfo? = withContext(Dispatchers.IO) {
        // Check if API key is configured
        if (BuildConfig.YOUTUBE_API_KEY.isEmpty()) {
            Log.e(TAG, "❌ YOUTUBE_API_KEY is not configured in local.properties!")
            throw Exception("YouTube API key not configured. Please add YOUTUBE_API_KEY to local.properties")
        }
        
        val rawUrl = "https://www.googleapis.com/youtube/v3/videos?part=snippet&id=$videoId&key=${BuildConfig.YOUTUBE_API_KEY}"
        val safeUrl = rawUrl.replace(Regex("key=[^&]+"), "key=[REDACTED]")
        Log.d(TAG, "getVideoDetails request url: $safeUrl")
        val url = URL(rawUrl)
        val json = try {
            url.readText()
        } catch (e: Exception) {
            Log.e(TAG, "getVideoDetails failed to read URL", e)
            throw e
        }
        Log.d(TAG, "getVideoDetails response length=${json.length}")
        val obj = JSONObject(json)

        val itemsArr = obj.getJSONArray("items")
        if (itemsArr.length() == 0) {
            Log.w(TAG, "getVideoDetails: no items found for videoId=$videoId")
            return@withContext null
        }
        val item = itemsArr.getJSONObject(0).getJSONObject("snippet")
        val title = item.getString("title")
        val channel = item.getString("channelTitle")
        val tags = try { item.optJSONArray("tags")?.let { (0 until it.length()).map { i -> it.getString(i).lowercase() } } ?: emptyList() } catch (e: Exception) { emptyList() }

        Log.d(TAG, "getVideoDetails parsed title='${title}' channel='${channel}' tagsCount=${tags.size}")

        // Smart extraction
        val artist = extractArtist(title, channel)
        val album = extractAlbum(title)
        val year = extractYear(item.getString("publishedAt"))

        val genreKeywords = mutableListOf<String>()
        listOf("pop", "hip hop", "rap", "rock", "edm", "bollywood", "punjabi", "lofi", "remix", "sad", "love", "dance").forEach {
            if (tags.any { t -> t.contains(it) } || title.lowercase().contains(it)) {
                genreKeywords += it
            }
        }
        if (genreKeywords.isEmpty()) genreKeywords += "music"

        Log.d(TAG, "getVideoDetails extracted artist='$artist' album='$album' year=$year genreKeywords=${genreKeywords.joinToString(",")}")

        return@withContext CurrentVideoInfo(
            title = title,
            artist = artist,
            album = album,
            year = year,
            genreKeywords = genreKeywords
        )
    }

    private suspend fun searchByKeywords(
        keywords: List<String>,
        excludeTitle: String,
        limit: Int
    ): List<RecommendedSong> = withContext(Dispatchers.IO) {
        if (keywords.isEmpty()) return@withContext emptyList()

        val query = keywords.joinToString(" ") + " music"
        val encoded = URLEncoder.encode("$query -\"$excludeTitle\"", "UTF-8")
        val rawUrl = "https://www.googleapis.com/youtube/v3/search?part=snippet&q=$encoded&type=video&videoCategoryId=10&maxResults=$limit&key=${BuildConfig.YOUTUBE_API_KEY}"
        val safeUrl = rawUrl.replace(Regex("key=[^&]+"), "key=[REDACTED]")
        Log.d(TAG, "searchByKeywords query='$query' excludeTitle='$excludeTitle' limit=$limit requestUrl=$safeUrl")

        try {
            val url = URL(rawUrl)
            val json = url.readText()
            val obj = JSONObject(json)
            val items = obj.getJSONArray("items")

            Log.d(TAG, "searchByKeywords response items=${items.length()}")

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
            return@withContext results.shuffled() // randomize within category
        } catch (e: Exception) {
            Log.e(TAG, "Search failed for: $keywords", e)
            return@withContext emptyList()
        }
    }

    private fun extractVideoId(url: String): String? {
        return Regex("(?:youtube\\.com/watch\\?v=|youtu\\.be/|youtube\\.com/embed/)([^#\\&\\?]{11})")
            .find(url)?.groupValues?.getOrNull(1)
    }

    private fun extractArtist(title: String, channel: String): String {
        val lower = title.lowercase()
        return when {
            lower.contains("justin bieber") || channel.contains("Justin Bieber") -> "Justin Bieber"
            lower.contains("arijit singh") -> "Arijit Singh"
            lower.contains("badshah") -> "Badshah"
            else -> channel.split("-")[0].trim()
        }
    }

    private fun extractAlbum(title: String): String {
        return Regex("(?:from|album):?\\s*[\\\"']?([^\\\"'\\n]+)", RegexOption.IGNORE_CASE)
            .find(title)?.groupValues?.getOrNull(1)?.trim() ?: ""
    }

    private fun extractYear(publishedAt: String): Int {
        return publishedAt.substring(0, 4).toIntOrNull() ?: 2020
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