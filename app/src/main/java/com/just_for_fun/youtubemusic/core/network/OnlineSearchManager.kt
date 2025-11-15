package com.just_for_fun.youtubemusic.core.network

import android.util.Log

/**
 * Online search manager using YouTube InnerTube API.
 * This is the same API used by official YouTube clients and apps like OuterTune/Spotube.
 * Much more reliable than third-party frontends like Piped/Invidious.
 */
class OnlineSearchManager {

    private val innerTubeClient = YouTubeInnerTubeClient()

    suspend fun search(query: String, limit: Int = 12, instances: List<String>? = null, apiKey: String? = null): List<OnlineSearchResult> {
        Log.d(TAG, "Searching YouTube Music for: $query")
        
        // Use InnerTube API for search
        val results = innerTubeClient.search(query, limit)
        
        // Fetch stream URLs for results (do this in the background)
        results.forEach { result ->
            if (result.streamUrl == null) {
                try {
                    val streamUrl = innerTubeClient.getStreamUrl(result.id)
                    if (streamUrl != null) {
                        // Update the result with stream URL
                        result.streamUrl = streamUrl
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to get stream URL for ${result.id}: ${e.message}")
                }
            }
        }
        
        return results
    }

    suspend fun getStreamUrl(videoId: String): String? {
        return innerTubeClient.getStreamUrl(videoId)
    }

    companion object {
        private const val TAG = "OnlineSearchManager"
    }

}
