package com.just_for_fun.synctax.utils

import android.os.Environment
import com.just_for_fun.synctax.MusicApplication
import java.io.File

object Constants {
    // Paths for saved (cached) and downloaded songs
    object Path {
        // Internal storage for saved songs (deleted on app data clear)
        val SAVED_DIR: File by lazy {
            File(MusicApplication.instance.filesDir, "saved").apply { mkdirs() }
        }

        // External storage for downloaded songs (persistent)
        val DOWNLOADED_DIR: File by lazy {
            File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC), "SyncTax").apply { mkdirs() }
        }

        // Cache dir for temporary files
        val CACHE_DIR: File by lazy {
            File(MusicApplication.instance.cacheDir, "audio_cache").apply { mkdirs() }
        }
    }

    // File extensions
    const val AUDIO_EXTENSION = ".mp3"
    const val DOWNLOAD_SUFFIX = ".download"
}