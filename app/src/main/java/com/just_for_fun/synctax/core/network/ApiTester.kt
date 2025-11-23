package com.just_for_fun.synctax.core.network

import com.just_for_fun.synctax.core.data.local.entities.Song
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.net.URL
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.util.*
import kotlinx.coroutines.*
import java.text.SimpleDateFormat

// Configure JSON parser to ignore fields not defined in our data classes
val json = Json { ignoreUnknownKeys = true }

@Serializable
data class RecordingResponse(
    val recordings: List<Recording> = emptyList()
)

@Serializable
data class Recording(
    val id: String,
    val title: String,
    val length: Long? = null, // Duration in milliseconds
    val releases: List<Release> = emptyList(), // Contains album details
    @SerialName("artist-credit")
    val artistCredit: List<ArtistCredit> = emptyList(),
    // NEW: Added genres field. Note: This field will only be present if &inc=genres is in the query.
    val genres: List<Genre> = emptyList()
)

@Serializable
data class ArtistCredit(
    val name: String
)

@Serializable
data class Release(
    val id: String, // MusicBrainz Release MBID - CRITICAL for Cover Art Archive
    val title: String,
    val date: String? = null // YYYY-MM-DD
)

// NEW: Data class for genre tags
@Serializable
data class Genre(
    val name: String
)

// --- 3. Core Logic for Fetching Metadata ---

fun fetchSongMetadata(songTitle: String, artistName: String): Song? {
    println("--- Starting Metadata Fetch for '$songTitle' by $artistName ---")

    // 1. Build the MusicBrainz Search URL
    // *** UPDATED: Added +genres to the &inc parameter ***
    val query = "recording:\"$songTitle\" AND artist:\"$artistName\""
    val encodedQuery = URL(query).toExternalForm().replace("file:/", "")
    val mbApiUrl = "https://musicbrainz.org/ws/2/recording/?query=$encodedQuery&fmt=json&limit=1&inc=releases+genres"

    println("MBBrainz URL: $mbApiUrl")

    try {
        // --- MUSICBRAINZ API CALL ---
        val jsonText = URL(mbApiUrl).readText()
        val response = json.decodeFromString<RecordingResponse>(jsonText)

        val recording = response.recordings.firstOrNull()
        if (recording == null) {
            println("Error: No recording found for the given search terms.")
            return null
        }

        // Extract basic metadata
        val recordingId = recording.id
        val primaryArtist = recording.artistCredit.firstOrNull()?.name ?: "Unknown Artist"
        val durationMs = recording.length ?: 0L

        // Extract Genre - MusicBrainz returns a list of user-submitted genres/tags. We take the first one.
        val primaryGenre = recording.genres.firstOrNull()?.name ?: "Unknown"

        // Extract Release (Album) details
        val primaryRelease = recording.releases.firstOrNull()
        val albumTitle = primaryRelease?.title
        val releaseMbid = primaryRelease?.id
        val releaseYear = primaryRelease?.date?.substringBefore('-')?.toIntOrNull()

        // --- COVER ART ARCHIVE (CAA) CALL ---
        var albumArtUri: String? = null
        if (releaseMbid != null) {
            albumArtUri = "https://coverartarchive.org/release/$releaseMbid/front"
            println("CAA Art URI: $albumArtUri (This URL redirects to the actual image file)")
        } else {
            println("Warning: No release MBID found. Cannot fetch album art from CAA.")
        }

        // Populate your Song object
        return Song(
            id = recordingId,
            title = recording.title,
            artist = primaryArtist,
            album = albumTitle,
            duration = durationMs,
            genre = primaryGenre,
            releaseYear = releaseYear,
            albumArtUri = albumArtUri,
            filePath = "",      // if any api found which can help to download audio file the file path will be place here for online songs
            addedTimestamp = System.currentTimeMillis()
        )

    } catch (e: IOException) {
        println("Network Error during API call: ${e.message}. (Ensure you are online and respect rate limits: max 1 request/second.)")
        return null
    } catch (e: Exception) {
        println("Parsing or Unknown Error: ${e.message}")
        return null
    }
}

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
                println("API: Error making API call ${e.message}")
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
}

@Serializable
data class TestResult(
    val songTitle: String,
    val artistName: String,
    val musicBrainzResult: Song?,
    val audioDBResult: SongFromAudioDB?
)

fun main() = runBlocking {
    val testSongs = listOf(
        // Indian songs
        Pair("Gulabo", "Arpit Bala"),
        Pair("295", "Sidhu Moose Wala"),
        Pair("Besharam Rang", "Vishal Dadlani"),
        // Foreign (Western)
        Pair("Shape of You", "Ed Sheeran"),
        Pair("Blinding Lights", "The Weeknd"),
        // Other countries
        Pair("Despacito", "Luis Fonsi"),
        Pair("Gangnam Style", "Psy")
    )

    val results = mutableListOf<TestResult>()

    val audioDB = TheAudioDB()

    for ((title, artist) in testSongs) {
        println("\n=== Testing: $title by $artist ===")

        val mbResult = fetchSongMetadata(title, artist)
        val adbResult = audioDB.fetchSongDetails("$title $artist") // Combine for search

        results.add(TestResult(title, artist, mbResult, adbResult))

        // Rate limit: 1 request per second for MB, AudioDB has limits too
        delay(1000)
    }

    // Write to file
    val outputDir = File("data/api_test_data")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    val outputFile = File(outputDir, "api_test_results.json")
    val jsonString = Json.encodeToString(results)
    outputFile.writeText(jsonString)

    println("\nResults saved to ${outputFile.absolutePath}")
}