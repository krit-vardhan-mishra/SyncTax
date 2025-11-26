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
            try {
                val videoId = extractVideoId(currentYoutubeUrl)
                if (videoId.isNullOrEmpty()) {
                    withContext(Dispatchers.Main) { onError("Invalid YouTube URL") }
                    return@launch
                }

                val currentInfo = getVideoDetails(videoId)
                    ?: run { withContext(Dispatchers.Main) { onError("Failed to get current video info") }; return@launch }

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
        val url = URL("https://www.googleapis.com/youtube/v3/videos?part=snippet&id=$videoId&key=${BuildConfig.YOUTUBE_API_KEY}")
        val json = url.readText()
        val obj = JSONObject(json)

        if (obj.getJSONArray("items").length() == 0) return@withContext null

        val item = obj.getJSONArray("items").getJSONObject(0).getJSONObject("snippet")
        val title = item.getString("title")
        val channel = item.getString("channelTitle")
        val tags = try { item.optJSONArray("tags")?.let { (0 until it.length()).map { i -> it.getString(i).lowercase() } } ?: emptyList() } catch (e: Exception) { emptyList() }

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
        val url = URL("https://www.googleapis.com/youtube/v3/search?part=snippet&q=$encoded&type=video&videoCategoryId=10&maxResults=$limit&key=${BuildConfig.YOUTUBE_API_KEY}")

        try {
            val json = url.readText()
            val obj = JSONObject(json)
            val items = obj.getJSONArray("items")

            val results = mutableListOf<RecommendedSong>()
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val idObj = item.getJSONObject("id")
                if (idObj.getString("kind") != "youtube#video") continue

                val videoId = idObj.getString("videoId")
                val snippet = item.getJSONObject("snippet")
                val title = snippet.getString("title")

                results += RecommendedSong(
                    title = title,
                    artist = snippet.getString("channelTitle"),
                    videoId = videoId,
                    watchUrl = "https://www.youtube.com/watch?v=$videoId",
                    thumbnail = snippet.getJSONObject("thumbnails").getJSONObject("high").getString("url")
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

    // Helper for URL reading
    private fun URL.readText(): String {
        val connection = openConnection() as HttpURLConnection
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        return try {
            BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }
}