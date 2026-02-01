package com.just_for_fun.synctax.data.repository

import com.just_for_fun.synctax.data.local.dao.OnlineSongDao
import com.just_for_fun.synctax.data.local.entities.OnlineSong
import com.just_for_fun.synctax.utils.MusicDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class OnlineSongRepository(
    private val onlineSongDao: OnlineSongDao,
    private val downloadManager: MusicDownloadManager
) {

    fun getAllOnlineSongs(): Flow<List<OnlineSong>> = onlineSongDao.getAllOnlineSongs()

    fun getSavedSongs(): Flow<List<OnlineSong>> = onlineSongDao.getSavedSongs()

    fun getDownloadedSongs(): Flow<List<OnlineSong>> = onlineSongDao.getDownloadedSongs()
    
    fun getPlayedSongs(): Flow<List<OnlineSong>> = onlineSongDao.getPlayedSongs()
    
    fun getFullyPlayedSongs(): Flow<List<OnlineSong>> = onlineSongDao.getFullyPlayedSongs()

    suspend fun getOnlineSongById(id: Int): OnlineSong? = onlineSongDao.getOnlineSongById(id)

    suspend fun getOnlineSongByVideoId(videoId: String): OnlineSong? = onlineSongDao.getOnlineSongByVideoId(videoId)

    suspend fun insertOnlineSong(song: OnlineSong): Long = onlineSongDao.insertOnlineSong(song)

    suspend fun updateOnlineSong(song: OnlineSong) = onlineSongDao.updateOnlineSong(song)

    suspend fun deleteOnlineSong(song: OnlineSong) = onlineSongDao.deleteOnlineSong(song)
    
    /**
     * Mark an online song as played (when played for 5+ seconds)
     * This adds it to the online song history section
     * If the song doesn't exist in the database, creates a new entry with provided metadata
     */
    suspend fun markAsPlayed(
        videoId: String, 
        title: String? = null, 
        artist: String? = null, 
        thumbnailUrl: String? = null, 
        duration: Int? = null
    ) {
        // Check if song exists
        val existingSong = onlineSongDao.getOnlineSongByVideoId(videoId)
        if (existingSong != null) {
            // Update existing
            onlineSongDao.updatePlayedStatus(videoId, true)
        } else if (title != null) {
            // Create new entry with isPlayed = true
            val newSong = OnlineSong(
                videoId = videoId,
                title = title,
                artist = artist ?: "Unknown Artist",
                thumbnailUrl = thumbnailUrl,
                duration = duration,
                isPlayed = true
            )
            onlineSongDao.insertOnlineSong(newSong)
        }
    }
    
    /**
     * Mark an online song as fully played
     * If offline storage is enabled, this can trigger auto-save
     */
    suspend fun markAsFullyPlayed(videoId: String) {
        onlineSongDao.updateFullyPlayedStatus(videoId, true)
    }

    suspend fun pruneCache(limit: Int) {
        if (limit <= 0) return
        
        try {
            // Use first() to get current list from Flow - Order is DESC (Newest First) by default
            val savedSongs = onlineSongDao.getSavedSongs().first()
            
            if (savedSongs.size > limit) {
                // Calculate how many to delete
                val songsToDeleteCount = savedSongs.size - limit
                
                if (songsToDeleteCount > 0) {
                    // savedSongs is Newest -> Oldest
                    // We want to delete the OLDEST, which are at the END of the list
                    val songsToDelete = savedSongs.takeLast(songsToDeleteCount)
                    
                    songsToDelete.forEach { song: OnlineSong ->
                        removeSavedSong(song)
                    }
                }
            }
        } catch (e: Exception) {
           e.printStackTrace()
        }
    }

    /**
     * Delete saved songs that are older than the expiration timestamp
     * @param expirationTime Timestamp (ms) - songs added before this time will be deleted
     */
    suspend fun deleteExpiredSongs(expirationTime: Long) {
        try {
            val savedSongs = onlineSongDao.getSavedSongs().first()
            val expiredSongs = savedSongs.filter { it.addedAt < expirationTime }
            
            if (expiredSongs.isNotEmpty()) {
                expiredSongs.forEach { song: OnlineSong ->
                    removeSavedSong(song)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun saveSong(song: OnlineSong, url: String) {
        // Download to internal storage
        downloadManager.saveSong(song.videoId, song.title, url) { success, path ->
            if (success) {
                // Update database in coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    val updatedSong = song.copy(isSaved = true)
                    updateOnlineSong(updatedSong)
                    
                    // Prune cache if needed
                    // We need UserPreferences to get the limit. 
                    // But Repository doesn't have UserPreferences injected here.
                    // We pass the limit or handle it in ViewModel.
                    // Ideally ViewModel should call prune, but saveSong is called here.
                    // Let's rely on HomeViewModel calling prune periodically or passed via constructor.
                    // Since I cannot change constructor easily without updating DI, 
                    // I will leave pruning for `autoSaveOnlineSong` in PlayerViewModel which HAS UserPreferences.
                }
            }
        }
    }

    fun downloadSong(song: OnlineSong, url: String) {
        // Download to external storage
        downloadManager.downloadSong(song.videoId, song.title, url) { success, path ->
            if (success) {
                // Update database in coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    val updatedSong = song.copy(isDownloaded = true)
                    updateOnlineSong(updatedSong)
                }
            }
        }
    }

    suspend fun removeSavedSong(song: OnlineSong) {
        // Delete file and update database
        downloadManager.deleteSavedSong(song.videoId, song.title)
        val updatedSong = song.copy(isSaved = false)
        updateOnlineSong(updatedSong)
    }

    suspend fun removeDownloadedSong(song: OnlineSong) {
        // Delete file and update database
        downloadManager.deleteDownloadedSong(song.videoId, song.title)
        val updatedSong = song.copy(isDownloaded = false)
        updateOnlineSong(updatedSong)
    }

    fun isSongSaved(song: OnlineSong): Boolean = downloadManager.isSongSaved(song.videoId, song.title)

    fun isSongDownloaded(song: OnlineSong): Boolean = downloadManager.isSongDownloaded(song.videoId, song.title)

    fun getSavedSongPath(song: OnlineSong): String? = downloadManager.getSavedSongPath(song.videoId, song.title)

    fun getDownloadedSongPath(song: OnlineSong): String? = downloadManager.getDownloadedSongPath(song.videoId, song.title)
}