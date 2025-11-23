package com.just_for_fun.synctax.core.network.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat

// Data class for AudioDB-specific song data (includes lyrics)
data class SongFromAudioDB(
    val title: String,
    val artist: String,
    val album: String?,
    val duration: Long,
    val genre: String?,
    val releaseYear: Int?,
    val lyrics: String
)

class TheAudioDB {

    private val apiUrl = "https://www.theaudiodb.com/api/v1/json/123/"

    // Fetch song details from TheAudioDB API by song name
    suspend fun fetchSongDetails(songName: String): SongFromAudioDB? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient()
                val request = Request.Builder()
                    .url("$apiUrl/searchtrack.php?s=${songName.replace(" ", "+")}")
                    .build()

                val response: Response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val jsonResponse = response.body?.string()
                    parseSongDetails(jsonResponse)
                } else {
                    null
                }
            } catch (e: IOException) {
                Log.e("API", "Error making API call", e)
                null
            }
        }
    }

    // Parse song details from JSON response
    private fun parseSongDetails(jsonResponse: String?): SongFromAudioDB? {
        if (jsonResponse == null) return null

        val jsonObject = JSONObject(jsonResponse)
        val trackArray = jsonObject.optJSONArray("track") ?: return null
        val trackObject = trackArray.optJSONObject(0) ?: return null

        val title = trackObject.optString("strTrack")
        val artist = trackObject.optString("strArtist")
        val album = trackObject.optString("strAlbum")
        val genre = trackObject.optString("strGenre")
        val releaseYear = trackObject.optInt("intYearReleased")
        val duration = trackObject.optLong("intDuration")
        val lyrics = trackObject.optString("strTrackLyrics")

        return SongFromAudioDB(title, artist, album, duration, genre, releaseYear, lyrics)
    }

    // Convert song lyrics to LRC file format
    fun convertLyricsToLRC(lyrics: String, songTitle: String, outputPath: String): Boolean {
        val timestampedLyrics = generateLRCContentFromLyrics(lyrics)

        // Write the content to an LRC file
        return try {
            val file = File(outputPath)
            file.writeText(timestampedLyrics)
            Log.d("LRC", "LRC file written successfully")
            true
        } catch (e: IOException) {
            Log.e("LRC", "Error writing LRC file", e)
            false
        }
    }

    // Generate LRC content with dummy timestamps for each lyric line
    private fun generateLRCContentFromLyrics(lyrics: String): String {
        val lines = lyrics.split("\n")
        val lrcContent = StringBuilder()

        // Example of dummy timestamps (you would replace this with actual timestamps)
        var timeInSeconds = 0

        for (line in lines) {
            // Generate timestamp [mm:ss.xx]
            val minutes = timeInSeconds / 60
            val seconds = timeInSeconds % 60
            val hundredths = (timeInSeconds * 100) % 100
            val timestamp = String.format("[%02d:%02d.%02d]", minutes, seconds, hundredths)

            // Append the timestamp and lyric line
            lrcContent.append("$timestamp $line\n")

            // Increment time (in seconds) for the next line (dummy logic)
            timeInSeconds += 4  // Assume each lyric line is 4 seconds long
        }

        return lrcContent.toString()
    }
}

fun main() = runBlocking {
    val testSongs = listOf(
        // Indian songs
        "Gulabo Arpit Bala",
        "295 Sidhu Moose Wala",
        "Besharam Rang Vishal Dadlani",
        // Foreign (Western)
        "Shape of You Ed Sheeran",
        "Blinding Lights The Weeknd",
        // Other countries
        "Despacito Luis Fonsi",
        "Gangnam Style Psy"
    )

    val audioDB = TheAudioDB()
    val results = mutableListOf<Map<String, Any?>>()

    for (query in testSongs) {
        println("\n=== Testing TheAudioDB for: $query ===")
        val songData = audioDB.fetchSongDetails(query)
        val result = mapOf(
            "api" to "TheAudioDB",
            "query" to query,
            "title" to songData?.title,
            "artist" to songData?.artist,
            "album" to songData?.album,
            "duration" to songData?.duration,
            "genre" to songData?.genre,
            "releaseYear" to songData?.releaseYear,
            "lyrics" to songData?.lyrics,
            "success" to (songData != null)
        )
        results.add(result)
        // Rate limit
        delay(1000)
    }

    // Write results to file
    val outputDir = File("data/api_test_data")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    val outputFile = File(outputDir, "audiodb_test_results.json")
    // Since no kotlinx.serialization here, use simple JSON or just print
    // For simplicity, write as string
    val jsonString = results.joinToString("\n") { it.toString() }
    outputFile.writeText(jsonString)

    println("\nTheAudioDB results saved to ${outputFile.absolutePath}")
}
