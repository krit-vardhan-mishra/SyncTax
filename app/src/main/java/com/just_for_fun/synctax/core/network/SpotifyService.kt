package com.just_for_fun.synctax.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

data class SpotifyTrack(
    val title: String,
    val artist: String,
    val durationMs: Long,
    val spotifyUri: String
)

object SpotifyService {
    private val client = OkHttpClient()

    suspend fun fetchPlaylistTracks(playlistUrl: String): Pair<String, List<SpotifyTrack>>? = withContext(Dispatchers.IO) {
        val playlistId = extractPlaylistId(playlistUrl) ?: return@withContext null

        var offset = 0
        val limit = 100
        val tracks = mutableListOf<SpotifyTrack>()
        var playlistName: String? = null

        while (true) {
            val url = "https://api.spotify.com/v1/playlists/$playlistId/tracks" +
                    "?offset=$offset&limit=$limit" +
                    "&fields=items(track(name,artists(name),duration_ms,uri)),next"

            val request = Request.Builder().url(url).build()
            val response = client.newCall(request).execute()

            if (!response.isSuccessful) break

            val json = JSONObject(response.body?.string() ?: "")
            if (playlistName == null) {
                // Get name from first request (we can also fetch /playlists endpoint, but this works)
                playlistName = "Spotify Playlist • Imported"
            }

            val items = json.getJSONArray("items")
            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i).optJSONObject("track") ?: continue
                if (item.optBoolean("is_local", false)) continue // Skip local files

                val title = item.getString("name")
                val artists = item.getJSONArray("artists")
                val artist = if (artists.length() > 0) artists.getJSONObject(0).getString("name") else "Unknown"
                val duration = item.getLong("duration_ms")
                val uri = item.getString("uri")

                tracks.add(SpotifyTrack(title, artist, duration, uri))
            }

            if (json.isNull("next")) break
            offset += limit
        }

        if (tracks.isEmpty()) null else Pair(playlistName ?: "Imported from Spotify", tracks)
    }

    private fun extractPlaylistId(url: String): String? {
        return Regex("""/playlist/([a-zA-Z0-9]+)""").find(url)?.groupValues?.get(1)
    }

    fun isSpotifyPlaylistUrl(url: String): Boolean {
        return url.contains("open.spotify.com/playlist/") || url.contains("spotify.com/playlist/")
    }
}