package com.just_for_fun.synctax.core.data.repository

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.just_for_fun.synctax.core.data.local.MusicDatabase
import com.just_for_fun.synctax.core.data.local.entities.ListeningHistory
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.core.data.local.entities.UserPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Calendar

class MusicRepository(private val context: Context) {

    private val database = MusicDatabase.getDatabase(context)
    private val songDao = database.songDao()
    private val historyDao = database.listeningHistoryDao()
    private val preferenceDao = database.userPreferenceDao()

    companion object {
        const val APP_MUSIC_DIR = "SyncTax"
    }

    /**
     * Get the app's download directory (always scanned)
     * This is where the app stores downloaded songs
     */
    fun getAppMusicDirectory(): File {
        val downloadDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "SyncTax"
        )
        if (!downloadDir.exists()) {
            downloadDir.mkdirs()
        }
        Log.d("Directory Location", "App music directory: ${downloadDir.absolutePath}/")

        return downloadDir
    }

    /**
     * Scan device storage for music files from selected directories
     */
    suspend fun scanDeviceMusic(selectedPaths: List<String> = emptyList()): List<Song> =
        withContext(Dispatchers.IO) {
            val songs = mutableListOf<Song>()

            // Get app's download directory - always scan this
            val appMusicDir = getAppMusicDirectory()

            // First, scan app's download directory directly (not relying on MediaStore)
            scanDirectoryDirectly(appMusicDir, songs)

            // Then scan user-selected directories using SAF
            selectedPaths.forEach { uriString ->
                try {
                    val uri = Uri.parse(uriString)
                    scanDirectoryFromSAF(uri, songs)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Save to database
            songDao.insertSongs(songs)

            // Prune songs that are no longer present on device (by MediaStore id)
            val scannedIds = songs.map { it.id }
            if (scannedIds.isNotEmpty()) {
                songDao.deleteSongsNotIn(scannedIds)
            }

            songs
        }

    /**
     * Scan a directory directly using File API
     */
    private fun scanDirectoryDirectly(directory: File, songs: MutableList<Song>) {
        fun scanRecursive(folder: File) {
            folder.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    scanRecursive(file)
                } else if (file.isFile) {
                    val fileName = file.name ?: ""
                    // Check if it's an audio file and not a deleted/trash file
                    if (fileName.matches(Regex(".*\\.(mp3|opus|m4a|flac|wav|aac|ogg|wma|ape|alac)$", RegexOption.IGNORE_CASE)) &&
                        !fileName.startsWith(".") && !fileName.contains("trashed", ignoreCase = true)) {
                        try {
                            // Get metadata from MediaStore using file path
                            val projection = arrayOf(
                                MediaStore.Audio.Media._ID,
                                MediaStore.Audio.Media.TITLE,
                                MediaStore.Audio.Media.ARTIST,
                                MediaStore.Audio.Media.ALBUM,
                                MediaStore.Audio.Media.ALBUM_ID,
                                MediaStore.Audio.Media.DURATION,
                                MediaStore.Audio.Media.YEAR
                            )

                            val selection = "${MediaStore.Audio.Media.DATA} = ?"
                            val selectionArgs = arrayOf(file.absolutePath)

                            var songAdded = false
                            context.contentResolver.query(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                projection,
                                selection,
                                selectionArgs,
                                null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)).toString()
                                    val title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)) ?: fileName.removeSuffix(fileName.substringAfterLast(".", ""))
                                    val artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)) ?: "Unknown Artist"
                                    val album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                                    val albumId = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                                    val duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                                    val year = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR))

                                    // Get album art URI - check for local image file first
                                    var albumArtUri = ContentUris.withAppendedId(
                                        android.net.Uri.parse("content://media/external/audio/albumart"),
                                        albumId
                                    ).toString()

                                    // Check for local album art file with same base name
                                    val localAlbumArtUri = checkForLocalAlbumArt(file)
                                    if (localAlbumArtUri != null) {
                                        albumArtUri = localAlbumArtUri
                                    }

                                    val genre = detectGenre(file.absolutePath, artist, title)

                                    val song = Song(
                                        id = id,
                                        title = title,
                                        artist = artist,
                                        album = album,
                                        duration = duration,
                                        filePath = file.absolutePath,
                                        genre = genre,
                                        releaseYear = if (year > 0) year else null,
                                        albumArtUri = albumArtUri
                                    )

                                    songs.add(song)
                                    songAdded = true
                                    Log.d("Directory Location", "Added song from direct scan: ${file.absolutePath}")
                                }
                            }

                            // If not found in MediaStore, add basic info
                            if (!songAdded) {
                                val title = fileName.removeSuffix(fileName.substringAfterLast(".", ""))
                                val song = Song(
                                    id = file.absolutePath,
                                    title = title,
                                    artist = "Unknown Artist",
                                    album = null,
                                    duration = 0L,
                                    filePath = file.absolutePath,
                                    genre = detectGenre(file.absolutePath, "", title),
                                    releaseYear = null,
                                    albumArtUri = checkForLocalAlbumArt(file) // Check for local album art
                                )
                                songs.add(song)
                                Log.d("Directory Location", "Added song from direct scan (no MediaStore): ${file.absolutePath}")
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        scanRecursive(directory)
    }
    private fun scanDirectoryFromSAF(directoryUri: Uri, songs: MutableList<Song>) {
        val directory = DocumentFile.fromTreeUri(context, directoryUri) ?: return

        fun scanRecursive(folder: DocumentFile) {
            folder.listFiles().forEach { file ->
                if (file.isDirectory) {
                    scanRecursive(file)
                } else if (file.isFile) {
                    val mimeType = file.type
                    val fileName = file.name ?: ""
                    // Check if it's an audio file and not a deleted/trash file
                    if ((mimeType?.startsWith("audio/") == true ||
                        fileName.matches(
                            Regex(
                                ".*\\.(mp3|opus|m4a|flac|wav|aac|ogg|wma|ape|alac)$",
                                RegexOption.IGNORE_CASE
                            )
                        )) &&
                        !fileName.startsWith(".") && !fileName.contains("trashed", ignoreCase = true)
                    ) {

                        try {
                            // Get metadata from MediaStore using the document URI
                            val projection = arrayOf(
                                MediaStore.Audio.Media._ID,
                                MediaStore.Audio.Media.TITLE,
                                MediaStore.Audio.Media.ARTIST,
                                MediaStore.Audio.Media.ALBUM,
                                MediaStore.Audio.Media.ALBUM_ID,
                                MediaStore.Audio.Media.DURATION,
                                MediaStore.Audio.Media.DATA,
                                MediaStore.Audio.Media.YEAR
                            )

                            // Try to find this file in MediaStore
                            val displayName = file.name ?: "Unknown"
                            val selection = "${MediaStore.Audio.Media.DISPLAY_NAME} = ?"
                            val selectionArgs = arrayOf(displayName)

                            var songAdded = false
                            context.contentResolver.query(
                                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                                projection,
                                selection,
                                selectionArgs,
                                null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) {
                                    val id =
                                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID))
                                            .toString()
                                    val title =
                                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE))
                                            ?: displayName
                                    val artist =
                                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST))
                                            ?: "Unknown Artist"
                                    val album =
                                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM))
                                    val albumId =
                                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID))
                                    val duration =
                                        cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION))
                                    val filePath =
                                        cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA))
                                    val year =
                                        cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR))

                                    // Get album art URI - check for local image file first
                                    var albumArtUri = ContentUris.withAppendedId(
                                        android.net.Uri.parse("content://media/external/audio/albumart"),
                                        albumId
                                    ).toString()

                                    // Check for local album art file with same base name (if we have file path)
                                    if (filePath != null) {
                                        val audioFile = File(filePath)
                                        val localAlbumArtUri = checkForLocalAlbumArt(audioFile)
                                        if (localAlbumArtUri != null) {
                                            albumArtUri = localAlbumArtUri
                                        }
                                    }

                                    val genre = detectGenre(filePath, artist, title)

                                    val song = Song(
                                        id = id,
                                        title = title,
                                        artist = artist,
                                        album = album,
                                        duration = duration,
                                        filePath = filePath,
                                        genre = genre,
                                        releaseYear = if (year > 0) year else null,
                                        albumArtUri = albumArtUri
                                    )

                                    songs.add(song)
                                    songAdded = true
                                }
                            }

                            // If not found in MediaStore, add basic info
                            if (!songAdded) {
                                val song = Song(
                                    id = file.uri.toString(),
                                    title = displayName.removeSuffix(
                                        file.name?.substringAfterLast(".") ?: ""
                                    ),
                                    artist = "Unknown Artist",
                                    album = null,
                                    duration = 0L,
                                    filePath = file.uri.toString(),
                                    genre = "Other",
                                    releaseYear = null,
                                    albumArtUri = null
                                )
                                songs.add(song)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }

        scanRecursive(directory)
    }

    /**
     * Check for local album art file with same base name as audio file
     * Also checks for common album art file names like cover.jpg, folder.jpg, etc.
     * Supports multiple image formats: jpg, jpeg, png, bmp, gif
     */
    private fun checkForLocalAlbumArt(audioFile: File): String? {
        try {
            val directory = audioFile.parentFile ?: return null
            val baseName = audioFile.nameWithoutExtension
            
            // Common image extensions to check
            val imageExtensions = listOf("jpg", "jpeg", "png", "bmp", "gif")
            
            // 1. Check for file with same base name as audio file
            for (ext in imageExtensions) {
                val albumArtFile = File(directory, "$baseName.$ext")
                if (albumArtFile.exists() && albumArtFile.isFile) {
                    return albumArtFile.absolutePath
                }
            }
            
            // 2. Check for common album art file names in the directory
            val commonAlbumArtNames = listOf("cover", "folder", "album", "artwork", "front")
            for (name in commonAlbumArtNames) {
                for (ext in imageExtensions) {
                    val albumArtFile = File(directory, "$name.$ext")
                    if (albumArtFile.exists() && albumArtFile.isFile) {
                        return albumArtFile.absolutePath
                    }
                }
            }
        } catch (e: Exception) {
            // Silently ignore errors when checking for album art
        }
        return null
    }    private fun scanFromMediaStore(songs: MutableList<Song>, allowedPath: String) {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.YEAR
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
            val durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn).toString()
                val title = cursor.getString(titleColumn) ?: "Unknown"
                val artist = cursor.getString(artistColumn) ?: "Unknown Artist"
                val album = cursor.getString(albumColumn)
                val albumId = cursor.getLong(albumIdColumn)
                val duration = cursor.getLong(durationColumn)
                val filePath = cursor.getString(dataColumn)
                val year = cursor.getInt(yearColumn)

                // Check if file is in allowed directory
                if (!filePath.startsWith(allowedPath)) {
                    Log.d("Directory Location", "Skipping file not in allowed path: $filePath (allowed: $allowedPath)")
                    continue // Skip this song
                }

                // Skip deleted/trash files
                val fileName = File(filePath).name
                if (fileName.startsWith(".") || fileName.contains("trashed", ignoreCase = true)) {
                    Log.d("Directory Location", "Skipping deleted/trash file: $filePath")
                    continue // Skip this song
                }

                Log.d("Directory Location", "Found song in SyncTax: $filePath")

                // Get album art URI - check for local image file first
                var albumArtUri = ContentUris.withAppendedId(
                    android.net.Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

                // Check for local album art file with same base name
                val audioFile = File(filePath)
                val localAlbumArtUri = checkForLocalAlbumArt(audioFile)
                if (localAlbumArtUri != null) {
                    albumArtUri = localAlbumArtUri
                }

                // Detect genre from file path or metadata (simplified)
                val genre = detectGenre(filePath, artist, title)

                val song = Song(
                    id = id,
                    title = title,
                    artist = artist,
                    album = album,
                    duration = duration,
                    filePath = filePath,
                    genre = genre,
                    releaseYear = if (year > 0) year else null,
                    albumArtUri = albumArtUri
                )

                songs.add(song)
            }
        }
    }

    private fun detectGenre(filePath: String, artist: String, title: String): String {
        // Simple genre detection based on folder structure
        val path = filePath.lowercase()
        return when {
            path.contains("rock") -> "Rock"
            path.contains("pop") -> "Pop"
            path.contains("jazz") -> "Jazz"
            path.contains("classical") -> "Classical"
            path.contains("hip hop") || path.contains("rap") -> "Hip Hop"
            path.contains("electronic") || path.contains("edm") -> "Electronic"
            path.contains("country") -> "Country"
            path.contains("metal") -> "Metal"
            path.contains("blues") -> "Blues"
            path.contains("reggae") -> "Reggae"
            else -> "Other"
        }
    }

    /**
     * Get all songs
     */
    fun getAllSongs(): Flow<List<Song>> = songDao.getAllSongs()

    /**
     * Get songs with pagination
     */
    suspend fun getSongsPaginated(limit: Int, offset: Int): List<Song> =
        songDao.getSongsPaginated(limit, offset)

    /**
     * Get total song count
     */
    suspend fun getSongCount(): Int = songDao.getSongCount()

    /**
     * Get song by ID
     */
    suspend fun getSongById(songId: String): Song? = songDao.getSongById(songId)

    /**
     * Insert a single song into the database
     */
    suspend fun insertSong(song: Song) = withContext(Dispatchers.IO) {
        songDao.insertSongs(listOf(song))
    }

    /**
     * Get songs by genre
     */
    fun getSongsByGenre(genre: String): Flow<List<Song>> = songDao.getSongsByGenre(genre)

    /**
     * Get songs by artist
     */
    fun getSongsByArtist(artist: String): Flow<List<Song>> = songDao.getSongsByArtist(artist)

    /**
     * Record song play
     */
    suspend fun recordPlay(
        songId: String,
        listenDuration: Long,
        completionRate: Float,
        skipped: Boolean
    ) = withContext(Dispatchers.IO) {
        val calendar = Calendar.getInstance()
        val timeOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        val history = ListeningHistory(
            songId = songId,
            playTimestamp = System.currentTimeMillis(),
            listenDuration = listenDuration,
            completionRate = completionRate,
            skipped = skipped,
            timeOfDay = timeOfDay,
            dayOfWeek = dayOfWeek
        )

        historyDao.insertHistory(history)

        // Update user preferences
        val existingPref = preferenceDao.getPreference(songId)
        if (existingPref != null) {
            val newPlayCount = existingPref.playCount + 1
            val newSkipCount = if (skipped) existingPref.skipCount + 1 else existingPref.skipCount
            val newAvgCompletion =
                (existingPref.avgCompletionRate * existingPref.playCount + completionRate) / newPlayCount

            val updatedPref = existingPref.copy(
                playCount = newPlayCount,
                skipCount = newSkipCount,
                avgCompletionRate = newAvgCompletion,
                lastPlayed = System.currentTimeMillis(),
                likeScore = calculateLikeScore(newPlayCount, newSkipCount, newAvgCompletion)
            )

            preferenceDao.updatePreference(updatedPref)
        } else {
            val newPref = UserPreference(
                songId = songId,
                playCount = 1,
                skipCount = if (skipped) 1 else 0,
                avgCompletionRate = completionRate,
                lastPlayed = System.currentTimeMillis(),
                likeScore = calculateLikeScore(1, if (skipped) 1 else 0, completionRate)
            )

            preferenceDao.insertPreference(newPref)
        }
    }

    private fun calculateLikeScore(playCount: Int, skipCount: Int, avgCompletion: Float): Float {
        val playScore = minOf(playCount / 10f, 1f) * 40
        val skipPenalty = (skipCount.toFloat() / playCount.coerceAtLeast(1)) * 30
        val completionScore = avgCompletion * 30

        return (playScore - skipPenalty + completionScore).coerceIn(0f, 100f)
    }

    /**
     * Get listening history
     */
    fun getRecentHistory(limit: Int = 100): Flow<List<ListeningHistory>> =
        historyDao.getRecentHistory(limit)

    /**
     * Get user preferences
     */
    fun getTopPreferences(limit: Int = 50): Flow<List<UserPreference>> =
        preferenceDao.getTopPreferences(limit)

    /**
     * Clear old history (older than 90 days)
     */
    suspend fun clearOldHistory() = withContext(Dispatchers.IO) {
        val ninetyDaysAgo = System.currentTimeMillis() - (90 * 24 * 60 * 60 * 1000L)
        historyDao.deleteOldHistory(ninetyDaysAgo)
    }

    /**
     * Clean up listening history and preferences for deleted songs
     */
    suspend fun cleanupDeletedSongsData(deletedSongIds: List<String>) =
        withContext(Dispatchers.IO) {
            deletedSongIds.forEach { songId ->
                // Remove from listening history
                historyDao.deleteBySongId(songId)
                // Remove from user preferences
                preferenceDao.deleteBySongId(songId)
            }
        }
}