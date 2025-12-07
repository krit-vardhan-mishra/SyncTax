package com.just_for_fun.synctax.data.repository

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.just_for_fun.synctax.core.dispatcher.AppDispatchers
import com.just_for_fun.synctax.data.local.MusicDatabase
import com.just_for_fun.synctax.data.local.entities.OnlineSong
import com.just_for_fun.synctax.data.local.entities.Playlist
import com.just_for_fun.synctax.data.local.entities.PlaylistSong
import com.just_for_fun.synctax.data.local.entities.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * Repository for handling playlist import and management operations
 */
class PlaylistRepository(private val context: Context) {

    private val database = MusicDatabase.getDatabase(context)
    private val playlistDao = database.playlistDao()
    private val onlineSongDao = database.onlineSongDao()
    private val playlistSongDao = database.playlistSongDao()

    companion object {
        private const val TAG = "PlaylistRepository"
    }

    /**
     * Result class for playlist import operation
     */
    sealed class ImportResult {
        data class Success(
            val playlist: Playlist,
            val songCount: Int
        ) : ImportResult()

        data class Error(val message: String) : ImportResult()
    }

    /**
     * Validation result for playlist URL
     */
    data class ValidationResult(
        val isValid: Boolean,
        val platform: String?,
        val playlistId: String?
    )

    /**
     * Get all playlists as Flow
     */
    fun getAllPlaylists(): Flow<List<Playlist>> = playlistDao.getAllPlaylists()

    /**
     * Get playlist by ID
     */
    suspend fun getPlaylistById(playlistId: Int): Playlist? =
        playlistDao.getPlaylistById(playlistId)

    /**
     * Get songs for a playlist
     */
    fun getSongsForPlaylist(playlistId: Int): Flow<List<OnlineSong>> =
        playlistSongDao.getSongsForPlaylist(playlistId)

    /**
     * Get songs for a playlist synchronously
     */
    suspend fun getSongsForPlaylistSync(playlistId: Int): List<OnlineSong> =
        playlistSongDao.getSongsForPlaylistSync(playlistId)

    /**
     * Validate if URL is a valid YouTube/YouTube Music or Spotify playlist URL
     */
    suspend fun validatePlaylistUrl(url: String): ValidationResult = withContext(AppDispatchers.Network) {
        try {
            initPythonIfNeeded()

            val python = Python.getInstance()

            // Check for Spotify
            if (url.contains("spotify.com") || url.contains("spotify:")) {
                val module = python.getModule("spotify_playlist_importer")
                val resultJson = module.callAttr("validate_spotify_url", url).toString()
                val result = JSONObject(resultJson)

                return@withContext ValidationResult(
                    isValid = result.optBoolean("isValid", false),
                    platform = result.optString("platform", null),
                    playlistId = result.optString("playlistId", null)
                )
            }

            // Default to YouTube
            val module = python.getModule("yt_playlist_importer")

            val resultJson = module.callAttr("validate_playlist_url", url).toString()
            val result = JSONObject(resultJson)

            ValidationResult(
                isValid = result.optBoolean("isValid", false),
                platform = result.optString("platform", null),
                playlistId = result.optString("playlistId", null)
            )
        } catch (e: Exception) {
            Log.e(TAG, "URL validation failed", e)
            ValidationResult(isValid = false, platform = null, playlistId = null)
        }
    }

    /**
     * Import a playlist from YouTube/YouTube Music or Spotify URL
     *
     * @param playlistUrl The full URL of the playlist
     * @return ImportResult indicating success or failure
     */
    suspend fun importPlaylist(playlistUrl: String): ImportResult = withContext(AppDispatchers.Network) {
        try {
            Log.d(TAG, "Starting playlist import: $playlistUrl")

            // Check if playlist already exists
            val existingPlaylist = playlistDao.getPlaylistByUrl(playlistUrl)
            if (existingPlaylist != null) {
                // Update existing playlist
                return@withContext refreshPlaylist(existingPlaylist.playlistId, playlistUrl)
            }

            initPythonIfNeeded()

            val python = Python.getInstance()
            val isSpotify = playlistUrl.contains("spotify.com") || playlistUrl.contains("spotify:")
            val module = if (isSpotify) {
                python.getModule("spotify_playlist_importer")
            } else {
                python.getModule("yt_playlist_importer")
            }

            val functionName = if (isSpotify) "fetch_spotify_playlist" else "fetch_playlist"

            // Fetch playlist data
            val resultJson = module.callAttr(functionName, playlistUrl).toString()
            val result = JSONObject(resultJson)

            if (!result.optBoolean("success", false)) {
                val error = result.optString("error", "Unknown error occurred")
                Log.e(TAG, "Playlist fetch failed: $error")
                return@withContext ImportResult.Error(error)
            }

            // Parse playlist data
            val title = result.optString("title", "Unknown Playlist")
            val description = result.optString("description", "")
            val thumbnail = result.optString("thumbnail", "")
            val tracks = result.optJSONArray("tracks")

            if (tracks == null || tracks.length() == 0) {
                return@withContext ImportResult.Error("Playlist is empty or contains no valid tracks")
            }

            // Create playlist entity
            val playlist = Playlist(
                name = title,
                description = description.takeIf { it.isNotEmpty() },
                platform = if (isSpotify) "Spotify" else "YouTube",
                playlistUrl = playlistUrl,
                thumbnailUrl = thumbnail.takeIf { it.isNotEmpty() },
                songCount = tracks.length()
            )

            // Insert playlist and get ID
            val playlistId = playlistDao.insertPlaylist(playlist).toInt()
            Log.d(TAG, "Created playlist with ID: $playlistId")

            // Process and insert songs
            val playlistSongs = mutableListOf<PlaylistSong>()

            for (i in 0 until tracks.length()) {
                val track = tracks.getJSONObject(i)

                val onlineSong = OnlineSong(
                    videoId = track.getString("videoId"),
                    title = track.optString("title", "Unknown Title"),
                    artist = track.optString("artist", "Unknown Artist"),
                    album = track.optString("album", null),
                    thumbnailUrl = track.optString("thumbnail", null),
                    duration = if (track.isNull("duration")) null else track.optInt("duration"),
                    sourcePlatform = if (isSpotify) "Spotify" else "YouTube"
                )

                // Get or insert the song
                val songId = onlineSongDao.getOrInsertByVideoId(onlineSong)

                // Create junction entry
                playlistSongs.add(
                    PlaylistSong(
                        playlistId = playlistId,
                        onlineSongId = songId,
                        position = track.optInt("position", i)
                    )
                )
            }

            // Batch insert playlist-song associations
            playlistSongDao.insertPlaylistSongs(playlistSongs)

            // Return the saved playlist
            val savedPlaylist = playlistDao.getPlaylistById(playlistId)
                ?: return@withContext ImportResult.Error("Failed to save playlist")

            Log.d(
                TAG,
                "Successfully imported playlist: ${savedPlaylist.name} with ${playlistSongs.size} songs"
            )
            ImportResult.Success(savedPlaylist, playlistSongs.size)

        } catch (e: Exception) {
            Log.e(TAG, "Playlist import failed", e)
            ImportResult.Error("Import failed: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Refresh an existing playlist with latest data from YouTube
     */
    private suspend fun refreshPlaylist(playlistId: Int, playlistUrl: String): ImportResult {
        try {
            val python = Python.getInstance()
            val module = python.getModule("yt_playlist_importer")

            val resultJson = module.callAttr("fetch_playlist", playlistUrl).toString()
            val result = JSONObject(resultJson)

            if (!result.optBoolean("success", false)) {
                val error = result.optString("error", "Unknown error occurred")
                return ImportResult.Error(error)
            }

            val title = result.optString("title", "Unknown Playlist")
            val description = result.optString("description", "")
            val thumbnail = result.optString("thumbnail", "")
            val tracks = result.optJSONArray("tracks")

            if (tracks == null) {
                return ImportResult.Error("Playlist contains no valid tracks")
            }

            // Clear existing songs from playlist
            playlistSongDao.clearPlaylist(playlistId)

            // Process and insert songs
            val playlistSongs = mutableListOf<PlaylistSong>()

            for (i in 0 until tracks.length()) {
                val track = tracks.getJSONObject(i)

                val onlineSong = OnlineSong(
                    videoId = track.getString("videoId"),
                    title = track.optString("title", "Unknown Title"),
                    artist = track.optString("artist", "Unknown Artist"),
                    album = track.optString("album", null),
                    thumbnailUrl = track.optString("thumbnail", null),
                    duration = if (track.isNull("duration")) null else track.optInt("duration"),
                    sourcePlatform = "YouTube"
                )

                val songId = onlineSongDao.getOrInsertByVideoId(onlineSong)

                playlistSongs.add(
                    PlaylistSong(
                        playlistId = playlistId,
                        onlineSongId = songId,
                        position = track.optInt("position", i)
                    )
                )
            }

            playlistSongDao.insertPlaylistSongs(playlistSongs)

            // Update playlist metadata
            val existingPlaylist = playlistDao.getPlaylistById(playlistId)
                ?: return ImportResult.Error("Playlist not found")

            val updatedPlaylist = existingPlaylist.copy(
                name = title,
                description = description.takeIf { it.isNotEmpty() },
                thumbnailUrl = thumbnail.takeIf { it.isNotEmpty() },
                songCount = playlistSongs.size,
                updatedAt = System.currentTimeMillis()
            )

            playlistDao.updatePlaylist(updatedPlaylist)

            Log.d(TAG, "Successfully refreshed playlist: $title with ${playlistSongs.size} songs")
            return ImportResult.Success(updatedPlaylist, playlistSongs.size)

        } catch (e: Exception) {
            Log.e(TAG, "Playlist refresh failed", e)
            return ImportResult.Error("Refresh failed: ${e.message ?: "Unknown error"}")
        }
    }

    /**
     * Delete a playlist and its song associations
     */
    suspend fun deletePlaylist(playlistId: Int) = withContext(AppDispatchers.Database) {
        playlistDao.deletePlaylistById(playlistId)
        // PlaylistSong entries are deleted automatically via CASCADE
    }

    /**
     * Initialize Python if not already started
     */
    private fun initPythonIfNeeded() {
        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(context))
        }
    }

    /**
     * Save an album as a playlist (user's saved albums)
     *
     * @param albumName The name of the album
     * @param artistName The artist name
     * @param thumbnailUrl The album thumbnail URL
     * @param songs List of songs in the album
     * @return The saved playlist ID, or null if failed
     */
    suspend fun saveAlbumAsPlaylist(
        albumName: String,
        artistName: String,
        thumbnailUrl: String?,
        songs: List<Song>
    ): Int? = withContext(AppDispatchers.Database) {
        try {
            // Create a unique URL for saved albums to identify them
            val albumUrl = "saved_album://$artistName/$albumName"

            // Check if album is already saved
            val existingPlaylist = playlistDao.getPlaylistByUrl(albumUrl)
            if (existingPlaylist != null) {
                Log.d(TAG, "Album already saved: $albumName")
                return@withContext existingPlaylist.playlistId
            }

            // Create playlist entity for the album
            val playlist = Playlist(
                name = albumName,
                description = "Album by $artistName",
                platform = "Saved Album",
                playlistUrl = albumUrl,
                thumbnailUrl = thumbnailUrl,
                songCount = songs.size
            )

            // Insert playlist and get ID
            val playlistId = playlistDao.insertPlaylist(playlist).toInt()
            Log.d(TAG, "Created saved album playlist with ID: $playlistId")

            // Process and insert songs
            val playlistSongs = mutableListOf<PlaylistSong>()

            songs.forEachIndexed { index, song ->
                val videoId = song.id.removePrefix("youtube:").removePrefix("online:")

                val onlineSong = OnlineSong(
                    videoId = videoId,
                    title = song.title,
                    artist = song.artist,
                    album = albumName,
                    thumbnailUrl = song.albumArtUri,
                    duration = song.duration.toInt(),
                    sourcePlatform = "Saved Album"
                )

                // Get or insert the song
                val songId = onlineSongDao.getOrInsertByVideoId(onlineSong)

                // Create junction entry
                playlistSongs.add(
                    PlaylistSong(
                        playlistId = playlistId,
                        onlineSongId = songId,
                        position = index
                    )
                )
            }

            // Batch insert playlist-song associations
            playlistSongDao.insertPlaylistSongs(playlistSongs)

            Log.d(TAG, "Successfully saved album: $albumName with ${playlistSongs.size} songs")
            playlistId

        } catch (e: Exception) {
            Log.e(TAG, "Failed to save album as playlist", e)
            null
        }
    }

    /**
     * Remove a saved album playlist
     *
     * @param albumName The name of the album
     * @param artistName The artist name
     * @return true if deleted, false otherwise
     */
    suspend fun unsaveAlbum(albumName: String, artistName: String): Boolean =
        withContext(AppDispatchers.Database) {
            try {
                val albumUrl = "saved_album://$artistName/$albumName"
                val playlist = playlistDao.getPlaylistByUrl(albumUrl)

                if (playlist != null) {
                    playlistDao.deletePlaylistById(playlist.playlistId)
                    Log.d(TAG, "Removed saved album: $albumName")
                    return@withContext true
                }
                false
            } catch (e: Exception) {
                Log.e(TAG, "Failed to unsave album", e)
                false
            }
        }

    /**
     * Create a manually created playlist (Offline or Online)
     */
    suspend fun createPlaylist(
        name: String,
        offlineSongs: List<Song>,
        onlineSongs: List<OnlineSong>
    ): Int? = withContext(AppDispatchers.Database) {
        try {
            val timestamp = System.currentTimeMillis()
            // Generate a unique URL for manual playlists
            val uniqueId = "${timestamp}_${name.hashCode()}"
            val playlistUrl = "user_playlist://$uniqueId"

            val isOffline = offlineSongs.isNotEmpty()
            val platform =
                if (isOffline) "Offline" else "Online" // Using "Online" generic for manually created online playlists

            // Determine thumbnail
            val thumbnail = if (isOffline) {
                offlineSongs.firstOrNull()?.albumArtUri
            } else {
                onlineSongs.firstOrNull()?.thumbnailUrl
            }

            val totalSongs = offlineSongs.size + onlineSongs.size

            val playlist = Playlist(
                name = name,
                description = "Created by user",
                platform = platform,
                playlistUrl = playlistUrl,
                thumbnailUrl = thumbnail,
                songCount = totalSongs,
                createdAt = timestamp,
                updatedAt = timestamp
            )

            val playlistId = playlistDao.insertPlaylist(playlist).toInt()
            Log.d(TAG, "Created user playlist: $name (ID: $playlistId)")

            val playlistSongs = mutableListOf<PlaylistSong>()

            // Process offline songs
            offlineSongs.forEachIndexed { index, song ->
                // Create a unique videoId for local files to store in online_songs table
                // Format: local:{md5_or_hash_of_path} or just use filePath if length allows.
                // Using a prefix to distinguish.
                val localId = "local:${song.id}"

                val onlineSong = OnlineSong(
                    videoId = localId,
                    title = song.title,
                    artist = song.artist,
                    album = song.album,
                    thumbnailUrl = song.albumArtUri,
                    duration = song.duration.toInt(),
                    sourcePlatform = "Local"
                )

                // Insert/Get ID
                val onlineSongId = onlineSongDao.getOrInsertByVideoId(onlineSong)

                playlistSongs.add(PlaylistSong(playlistId, onlineSongId, index))
            }

            // Process online songs
            onlineSongs.forEachIndexed { index, song ->
                val onlineSongId = onlineSongDao.getOrInsertByVideoId(song)
                // Adjust position for online songs if appending to offline (though typically it's one or the other)
                val position = if (isOffline) offlineSongs.size + index else index
                playlistSongs.add(PlaylistSong(playlistId, onlineSongId, position))
            }

            playlistSongDao.insertPlaylistSongs(playlistSongs)
            return@withContext playlistId

        } catch (e: Exception) {
            Log.e(TAG, "Failed to create playlist", e)
            return@withContext null
        }
    }


    /**
     * Check if an album is saved as a playlist
     *
     * @param albumName The name of the album
     * @param artistName The artist name
     * @return true if album is saved, false otherwise
     */
    suspend fun isAlbumSaved(albumName: String, artistName: String): Boolean =
        withContext(AppDispatchers.Database) {
            try {
                val albumUrl = "saved_album://$artistName/$albumName"
                playlistDao.getPlaylistByUrl(albumUrl) != null
            } catch (e: Exception) {
                Log.e(TAG, "Failed to check if album is saved", e)
                false
            }
        }
}
