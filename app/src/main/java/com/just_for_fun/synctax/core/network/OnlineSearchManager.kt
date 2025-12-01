package com.just_for_fun.synctax.core.network

import android.util.Log
import com.just_for_fun.synctax.core.player.StreamUrlCache

/**
 * Online search manager using YouTube InnerTube API.
 * This is the same API used by official YouTube clients and apps like OuterTune/Spotube.
 * Much more reliable than third-party frontends like Piped/Invidious.
 */
class OnlineSearchManager {

    private val innerTubeClient = YouTubeInnerTubeClient()

    suspend fun search(query: String, limit: Int = 12, instances: List<String>? = null, apiKey: String? = null): List<OnlineSearchResult> {
        Log.d(TAG, "Searching YouTube Music for: $query")
        
        // Use InnerTube API for search only - stream URLs fetched on-demand
        return innerTubeClient.search(query, limit)
    }

    suspend fun getStreamUrl(videoId: String): String? {
        // Check cache first for faster access
        StreamUrlCache.get(videoId)?.let {
            Log.d(TAG, "Stream URL cache hit for: $videoId")
            return it
        }
        
        // Fetch from network
        val url = innerTubeClient.getStreamUrl(videoId)
        
        // Cache the result if successful
        if (url != null) {
            StreamUrlCache.put(videoId, url)
            Log.d(TAG, "Cached stream URL for: $videoId")
        }
        
        return url
    }

    companion object {
        private const val TAG = "OnlineSearchManager"
    }

}
