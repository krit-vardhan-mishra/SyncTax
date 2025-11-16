package com.just_for_fun.youtubemusic.core.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import com.just_for_fun.youtubemusic.core.data.local.MusicDatabase
import com.just_for_fun.youtubemusic.core.data.local.entities.ListeningHistory
import com.just_for_fun.youtubemusic.core.data.local.entities.Song
import com.just_for_fun.youtubemusic.core.data.local.entities.UserPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Calendar

class MusicRepository(private val context: Context) {

    private val database = MusicDatabase.getDatabase(context)
    private val songDao = database.songDao()
    private val historyDao = database.listeningHistoryDao()
    private val preferenceDao = database.userPreferenceDao()

    /**
     * Scan device storage for music files
     */
    suspend fun scanDeviceMusic(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

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

                // Get album art URI
                val albumArtUri = ContentUris.withAppendedId(
                    android.net.Uri.parse("content://media/external/audio/albumart"),
                    albumId
                ).toString()

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

        // Save to database
        songDao.insertSongs(songs)

        // Prune songs that are no longer present on device (by MediaStore id)
        val scannedIds = songs.map { it.id }
        if (scannedIds.isNotEmpty()) {
            songDao.deleteSongsNotIn(scannedIds)
        }

        songs
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
     * Get song by ID
     */
    suspend fun getSongById(songId: String): Song? = songDao.getSongById(songId)

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
            val newAvgCompletion = (existingPref.avgCompletionRate * existingPref.playCount + completionRate) / newPlayCount

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
}