package com.just_for_fun.synctax.core.data.repository

import android.content.Context
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.just_for_fun.synctax.core.data.local.MusicDatabase
import com.just_for_fun.synctax.core.data.local.entities.OnlineSong
import com.just_for_fun.synctax.core.data.local.entities.Playlist
import com.just_for_fun.synctax.core.data.local.entities.PlaylistSong
import kotlinx.coroutines.Dispatchers
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
    suspend fun getPlaylistById(playlistId: Int): Playlist? = playlistDao.getPlaylistById(playlistId)
    
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
     * Validate if URL is a valid YouTube/YouTube Music playlist URL
     */
    suspend fun validatePlaylistUrl(url: String): ValidationResult = withContext(Dispatchers.IO) {
        try {
            initPythonIfNeeded()
            
            val python = Python.getInstance()
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
     * Import a playlist from YouTube/YouTube Music URL
     * 
     * @param playlistUrl The full URL of the playlist
     * @return ImportResult indicating success or failure
     */
    suspend fun importPlaylist(playlistUrl: String): ImportResult = withContext(Dispatchers.IO) {
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
            val module = python.getModule("yt_playlist_importer")
            
            // Fetch playlist data from YouTube
            val resultJson = module.callAttr("fetch_playlist", playlistUrl).toString()
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
                platform = "YouTube",
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
                    sourcePlatform = "YouTube"
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
            
            Log.d(TAG, "Successfully imported playlist: ${savedPlaylist.name} with ${playlistSongs.size} songs")
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
    suspend fun deletePlaylist(playlistId: Int) = withContext(Dispatchers.IO) {
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
}
