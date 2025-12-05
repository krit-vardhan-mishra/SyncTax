package com.just_for_fun.synctax.data.repository

import android.util.Log
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.data.model.LyricLine
import com.just_for_fun.synctax.core.network.LrcLibClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import  com.just_for_fun.synctax.core.network.LrcLibResponse

/**
 * Repository for managing lyrics fetching and caching
 * Handles API calls to LRCLIB and local file storage
 */
class LyricsRepository {
    
    private val api = LrcLibClient.api
    
    /**
     * Fetch lyrics for a song from LRCLIB API
     * Returns synced lyrics if available, otherwise plain lyrics
     * 
     * @param song The song to fetch lyrics for
     * @return Lyrics string in LRC format, or null if not found
     */
    suspend fun fetchLyricsFromApi(song: Song): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("LyricsRepository", "Fetching lyrics for: ${song.title} by ${song.artist}")
            
            // Convert duration from milliseconds to seconds
            val durationSeconds = if (song.duration > 0) {
                (song.duration / 1000).toInt()
            } else null
            
            val response = api.getLyrics(
                trackName = song.title,
                artistName = song.artist,
                albumName = song.album,
                duration = durationSeconds
            )
            
            // Prefer synced lyrics, fallback to plain lyrics
            val lyrics = response.syncedLyrics ?: response.plainLyrics
            
            if (lyrics != null) {
                Log.d("LyricsRepository", "Successfully fetched lyrics (${if (response.syncedLyrics != null) "synced" else "plain"})")
            } else {
                Log.w("LyricsRepository", "No lyrics found for song")
            }
            
            lyrics
        } catch (e: Exception) {
            Log.e("LyricsRepository", "Failed to fetch lyrics from API", e)
            null
        }
    }
    
    /**
     * Fetch lyrics for a song from LRCLIB API using custom query parameters
     * Returns synced lyrics if available, otherwise plain lyrics
     * 
     * @param song The song to fetch lyrics for
     * @param customSongName Custom song name to search for
     * @param customArtistName Custom artist name to search for
     * @return Lyrics string in LRC format, or null if not found
     */
    suspend fun fetchLyricsFromApiWithCustomQuery(song: Song, customSongName: String, customArtistName: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("LyricsRepository", "Fetching lyrics with custom query: $customSongName by $customArtistName")
            
            // Convert duration from milliseconds to seconds
            val durationSeconds = if (song.duration > 0) {
                (song.duration / 1000).toInt()
            } else null
            // If artist name is blank, fall back to the more flexible search endpoint
            if (customArtistName.isBlank()) {
                Log.d("LyricsRepository", "Artist blank in custom query, using search endpoint for: $customSongName")
                val results = api.searchLyrics(
                    trackName = customSongName,
                    artistName = null,
                    query = customSongName
                )

                if (results.isNotEmpty()) {
                    val best = results.first()
                    val lyrics = best.syncedLyrics ?: best.plainLyrics
                    if (lyrics != null) {
                        Log.d("LyricsRepository", "Found lyrics via search for custom query (synced=${best.syncedLyrics != null})")
                        return@withContext lyrics
                    }
                }

                Log.w("LyricsRepository", "No lyrics found with custom query (search)")
                null
            } else {
                val response = api.getLyrics(
                    trackName = customSongName,
                    artistName = customArtistName,
                    albumName = song.album,
                    duration = durationSeconds
                )

                // Prefer synced lyrics, fallback to plain lyrics
                val lyrics = response.syncedLyrics ?: response.plainLyrics

                if (lyrics != null) {
                    Log.d("LyricsRepository", "Successfully fetched lyrics with custom query (${if (response.syncedLyrics != null) "synced" else "plain"})")
                } else {
                    Log.w("LyricsRepository", "No lyrics found with custom query")
                }

                lyrics
            }
        } catch (e: Exception) {
            Log.e("LyricsRepository", "Failed to fetch lyrics with custom query from API", e)
            null
        }
    }
    
    /**
     * Search for lyrics using a general query
     * Useful as a fallback when exact match fails
     * 
     * @param song The song to search lyrics for
     * @return Lyrics string in LRC format, or null if not found
     */
    suspend fun searchLyrics(song: Song): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("LyricsRepository", "Searching lyrics for: ${song.title} by ${song.artist}")
            
            val results = api.searchLyrics(
                trackName = song.title,
                artistName = song.artist
            )
            
            if (results.isNotEmpty()) {
                val bestMatch = results.first()
                val lyrics = bestMatch.syncedLyrics ?: bestMatch.plainLyrics
                
                if (lyrics != null) {
                    Log.d("LyricsRepository", "Found lyrics from search results")
                }
                
                lyrics
            } else {
                Log.w("LyricsRepository", "No search results found")
                null
            }
        } catch (e: Exception) {
            Log.e("LyricsRepository", "Failed to search lyrics", e)
            null
        }
    }
    
    /**
     * Search for lyrics and return multiple results for user selection
     * 
     * @param song The song to search lyrics for
     * @return List of LrcLibResponse containing potential matches
     */
    suspend fun searchLyricsResults(song: Song): List<LrcLibResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("LyricsRepository", "Searching for lyrics results: ${song.title} by ${song.artist}")
            
            val results = api.searchLyrics(
                trackName = song.title,
                artistName = song.artist
            )
            
            Log.d("LyricsRepository", "Found ${results.size} potential matches")
            results
        } catch (e: Exception) {
            Log.e("LyricsRepository", "Failed to search lyrics results", e)
            emptyList()
        }
    }
    
    /**
     * Search for lyrics and return multiple results for user selection (with custom query)
     * 
     * @param song The song to search lyrics for
     * @param customSongName Custom song name to search for
     * @param customArtistName Custom artist name to search for
     * @return List of LrcLibResponse containing potential matches
     */
    suspend fun searchLyricsResultsWithCustomQuery(song: Song, customSongName: String, customArtistName: String): List<LrcLibResponse> = withContext(Dispatchers.IO) {
        try {
            Log.d("LyricsRepository", "Searching for lyrics results with custom query: $customSongName by $customArtistName")
            
            val results = api.searchLyrics(
                trackName = customSongName,
                artistName = customArtistName
            )
            
            Log.d("LyricsRepository", "Found ${results.size} potential matches with custom query")
            results
        } catch (e: Exception) {
            Log.e("LyricsRepository", "Failed to search lyrics results with custom query", e)
            emptyList()
        }
    }
    
    /**
     * Fetch and save specific lyrics by ID
     * 
     * @param trackId The LRCLIB track ID
     * @param song The song to save lyrics for
     * @return Parsed lyrics as LyricLine objects, or null if failed
     */
    suspend fun fetchAndSaveLyricsById(trackId: Int, song: Song): List<LyricLine>? = withContext(Dispatchers.IO) {
        try {
            Log.d("LyricsRepository", "Fetching lyrics by ID: $trackId")
            
            // Note: LRCLIB API doesn't have a get by ID endpoint, so we'll use search and filter
            // This is a workaround since the API doesn't provide direct ID access
            val results = api.searchLyrics(trackName= "${song.artist}", artistName= "${song.title}")
            val selectedResult = results.firstOrNull { it.id == trackId }
            
            if (selectedResult != null) {
                val lyrics = selectedResult.syncedLyrics ?: selectedResult.plainLyrics
                if (lyrics != null) {
                    val parsedLyrics = saveLyricsToFile(song, lyrics)
                    Log.d("LyricsRepository", "Saved selected lyrics for: ${selectedResult.trackName}")
                    parsedLyrics
                } else {
                    Log.w("LyricsRepository", "Selected result has no lyrics content")
                    null
                }
            } else {
                Log.w("LyricsRepository", "Could not find lyrics with ID: $trackId")
                null
            }
        } catch (e: Exception) {
            Log.e("LyricsRepository", "Failed to fetch lyrics by ID", e)
            null
        }
    }
    
    /**
     * Fetch and save lyrics for a song
     * Tries exact match first, then falls back to search
     * Saves lyrics as .lrc file next to the audio file
     * 
     * @param song The song to fetch and save lyrics for
     * @return True if lyrics were successfully fetched and saved
     */
    suspend fun fetchAndSaveLyrics(song: Song): Boolean = withContext(Dispatchers.IO) {
        try {
            // Try exact match first
            var lyrics = fetchLyricsFromApi(song)
            
            // Fallback to search if exact match fails
            if (lyrics == null) {
                Log.d("LyricsRepository", "Exact match failed, trying search...")
                lyrics = searchLyrics(song)
            }
            
            if (lyrics != null) {
                // Save lyrics to file
                saveLyricsToFile(song, lyrics)
                true
            } else {
                Log.w("LyricsRepository", "Could not find lyrics for song")
                false
            }
        } catch (e: Exception) {
            Log.e("LyricsRepository", "Failed to fetch and save lyrics", e)
            false
        }
    }
    
    /**
     * Parse LRC content into LyricLine objects
     */
    private fun parseLrcContent(content: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()
        val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.+)")
        
        content.lines().forEach { line ->
            val match = regex.find(line.trim())
            if (match != null) {
                val minutes = match.groupValues[1].toInt()
                val seconds = match.groupValues[2].toInt()
                val centiseconds = match.groupValues[3].toInt()
                val text = match.groupValues[4].trim()
                
                val totalMillis = (minutes * 60 * 1000) + (seconds * 1000) + (centiseconds * 10)
                lines.add(LyricLine(totalMillis.toLong(), text))
            }
        }
        
        return lines.sortedBy { it.timestamp }
    }
    
    /**
     * Save lyrics to a .lrc file next to the audio file or cache for online songs
     * 
     * @param song The song to save lyrics for
     * @param lyrics The lyrics content in LRC format
     * @return Parsed lyrics as LyricLine objects, or null if parsing failed
     */
    fun saveLyricsToFile(song: Song, lyrics: String): List<LyricLine>? {
        try {
            // Parse the lyrics content
            val parsedLyrics = parseLrcContent(lyrics)
            
            if (song.id.startsWith("online:")) {
                // For online songs, lyrics are stored in memory cache (handled by ViewModel)
                Log.d("LyricsRepository", "Parsed lyrics for online song: ${song.id}")
                return parsedLyrics
            } else {
                // For local songs, save to file
                val audioFile = File(song.filePath)
                if (!audioFile.exists()) {
                    Log.w("LyricsRepository", "Audio file does not exist: ${song.filePath}")
                    return null
                }
                
                val lrcFile = File(audioFile.parent, "${audioFile.nameWithoutExtension}.lrc")
                lrcFile.writeText(lyrics)
                
                Log.d("LyricsRepository", "Saved lyrics to: ${lrcFile.absolutePath}")
                return parsedLyrics
            }
        } catch (e: Exception) {
            Log.e("LyricsRepository", "Failed to save lyrics", e)
            return null
        }
    }
    
    /**
     * Check if lyrics file exists for a song
     * 
     * @param song The song to check
     * @return True if .lrc file exists
     */
    fun hasLyricsFile(song: Song): Boolean {
        if (song.id.startsWith("online:")) return false
        
        val audioFile = File(song.filePath)
        val lrcFile = File(audioFile.parent, "${audioFile.nameWithoutExtension}.lrc")
        return lrcFile.exists()
    }
}
