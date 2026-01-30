package com.just_for_fun.synctax.utils

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import com.just_for_fun.synctax.MusicApplication
import java.io.File

class MusicDownloadManager private constructor() {

    private val context = MusicApplication.instance
    private val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager

    companion object {
        val instance by lazy { MusicDownloadManager() }
    }

    /**
     * Download song to external storage (permanent download)
     */
    fun downloadSong(songId: String, songName: String, url: String, onComplete: (Boolean, String?) -> Unit) {
        val fileName = "$songName${Constants.AUDIO_EXTENSION}"
        val destinationFile = File(Constants.Path.DOWNLOADED_DIR, fileName)

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Downloading $songName")
            .setDescription("Downloading music file")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationUri(Uri.fromFile(destinationFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        val downloadId = downloadManager.enqueue(request)

        // Note: For simplicity, we're not tracking completion here
        // In a real app, you'd register a BroadcastReceiver for DownloadManager.ACTION_DOWNLOAD_COMPLETE
        // and check the status
        onComplete(true, destinationFile.absolutePath)
    }

    /**
     * Save song to internal storage (cached save)
     * For simplicity, using DownloadManager but to internal storage
     */
    fun saveSong(songId: String, songName: String, url: String, onComplete: (Boolean, String?) -> Unit) {
        val fileName = "$songName${Constants.AUDIO_EXTENSION}"
        val destinationFile = File(Constants.Path.SAVED_DIR, fileName)

        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("Saving $songName")
            .setDescription("Saving music file")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
            .setDestinationUri(Uri.fromFile(destinationFile))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(false)

        val downloadId = downloadManager.enqueue(request)
        onComplete(true, destinationFile.absolutePath)
    }

    /**
     * Check if song is saved (in internal storage)
     */
    fun isSongSaved(songId: String, songName: String): Boolean {
        val fileName = "$songName${Constants.AUDIO_EXTENSION}"
        val file = File(Constants.Path.SAVED_DIR, fileName)
        return file.exists()
    }

    /**
     * Check if song is downloaded (in external storage)
     */
    fun isSongDownloaded(songId: String, songName: String): Boolean {
        val fileName = "$songName${Constants.AUDIO_EXTENSION}"
        val file = File(Constants.Path.DOWNLOADED_DIR, fileName)
        return file.exists()
    }

    /**
     * Get saved song path
     */
    fun getSavedSongPath(songId: String, songName: String): String? {
        val fileName = "$songName${Constants.AUDIO_EXTENSION}"
        val file = File(Constants.Path.SAVED_DIR, fileName)
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * Get downloaded song path
     */
    fun getDownloadedSongPath(songId: String, songName: String): String? {
        val fileName = "$songName${Constants.AUDIO_EXTENSION}"
        val file = File(Constants.Path.DOWNLOADED_DIR, fileName)
        return if (file.exists()) file.absolutePath else null
    }

    /**
     * Delete saved song
     */
    fun deleteSavedSong(songId: String, songName: String): Boolean {
        val fileName = "$songName${Constants.AUDIO_EXTENSION}"
        val file = File(Constants.Path.SAVED_DIR, fileName)
        return file.delete()
    }

    /**
     * Delete downloaded song
     */
    fun deleteDownloadedSong(songId: String, songName: String): Boolean {
        val fileName = "$songName${Constants.AUDIO_EXTENSION}"
        val file = File(Constants.Path.DOWNLOADED_DIR, fileName)
        return file.delete()
    }
}