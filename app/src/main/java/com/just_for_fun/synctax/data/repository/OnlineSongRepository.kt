package com.just_for_fun.synctax.data.repository

import com.just_for_fun.synctax.data.local.dao.OnlineSongDao
import com.just_for_fun.synctax.data.local.entities.OnlineSong
import com.just_for_fun.synctax.utils.MusicDownloadManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
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
     */
    suspend fun markAsPlayed(videoId: String) {
        onlineSongDao.updatePlayedStatus(videoId, true)
    }
    
    /**
     * Mark an online song as fully played
     * If offline storage is enabled, this can trigger auto-save
     */
    suspend fun markAsFullyPlayed(videoId: String) {
        onlineSongDao.updateFullyPlayedStatus(videoId, true)
    }

    fun saveSong(song: OnlineSong, url: String) {
        // Download to internal storage
        downloadManager.saveSong(song.videoId, song.title, url) { success, path ->
            if (success) {
                // Update database in coroutine
                CoroutineScope(Dispatchers.IO).launch {
                    val updatedSong = song.copy(isSaved = true)
                    updateOnlineSong(updatedSong)
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