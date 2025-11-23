import com.just_for_fun.synctax.core.data.local.entities.Song
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import java.net.URL
import java.io.IOException

// --- 1. Data Class for Your Application (Matches your Room Entity) ---

// --- 2. Data Classes to Map the MusicBrainz JSON Response ---

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

// --- 4. Execution Example ---

fun main() {
    // Test songs from various countries
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

    val results = mutableListOf<Map<String, Any?>>()

    for ((title, artist) in testSongs) {
        println("\n=== Testing MusicBrainz for: $title by $artist ===")
        val songData = fetchSongMetadata(title, artist)
        val result = mapOf(
            "api" to "MusicBrainz",
            "songTitle" to title,
            "artistName" to artist,
            "title" to songData?.title,
            "artist" to songData?.artist,
            "album" to songData?.album,
            "duration" to songData?.duration,
            "genre" to songData?.genre,
            "releaseYear" to songData?.releaseYear,
            "albumArtUri" to songData?.albumArtUri,
            "filePath" to songData?.filePath,
            "success" to (songData != null)
        )
        results.add(result)
        // Rate limit
        Thread.sleep(1000)
    }

    // Write results to file
    val outputDir = java.io.File("data/api_test_data")
    if (!outputDir.exists()) {
        outputDir.mkdirs()
    }
    val outputFile = java.io.File(outputDir, "musicbrainz_test_results.json")
    val jsonString = json.encodeToString(results)
    outputFile.writeText(jsonString)

    println("\nMusicBrainz results saved to ${outputFile.absolutePath}")
}