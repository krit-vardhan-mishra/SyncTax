package com.just_for_fun.synctax.util

import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.PyException
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * YouTube Music Recommender using ytmusicapi
 * Returns only songs (audio tracks), no music videos, playlists, or live streams
 */
object YTMusicRecommender {
    private const val TAG = "YTMusicRecommender"
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    @Volatile
    private var isInitialized = false
    
    /**
     * Initialize the Python ytmusicapi module
     * Call this once during app startup
     */
    fun initialize() {
        if (isInitialized) {
            Log.d(TAG, "Already initialized")
            return
        }
        
        scope.launch {
            try {
                val python = Python.getInstance()
                val module = python.getModule("ytmusic_recommender")
                val result = module.callAttr("initialize").toString()
                isInitialized = true
                Log.d(TAG, "Initialization result: $result")
            } catch (e: PyException) {
                Log.e(TAG, "Python initialization failed", e)
                isInitialized = false
            } catch (e: Exception) {
                Log.e(TAG, "Initialization failed", e)
                isInitialized = false
            }
        }
    }
    
    /**
     * Search for songs only (no videos, playlists, or albums)
     * @param query Search query
     * @param limit Maximum number of results (default 20)
     * @param onResult Callback with list of songs
     * @param onError Error callback
     */
    fun searchSongs(
        query: String,
        limit: Int = 20,
        onResult: (List<RecommendedSong>) -> Unit,
        onError: (String) -> Unit = { Log.e(TAG, it) }
    ) {
        scope.launch {
            try {
                Log.d(TAG, "searchSongs: query='$query', limit=$limit")
                
                val python = Python.getInstance()
                val module = python.getModule("ytmusic_recommender")
                val jsonResult = module.callAttr("search_songs", query, limit).toString()
                
                val songs = parseSongsFromJson(jsonResult)
                Log.d(TAG, "Found ${songs.size} songs for query: $query")
                
                withContext(Dispatchers.Main) {
                    onResult(songs)
                }
            } catch (e: PyException) {
                Log.e(TAG, "Python search_songs failed", e)
                withContext(Dispatchers.Main) {
                    onError("Search failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "searchSongs failed", e)
                withContext(Dispatchers.Main) {
                    onError("Search failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Search for albums
     * @param query Search query
     * @param limit Maximum number of results (default 20)
     * @param onResult Callback with list of albums
     * @param onError Error callback
     */
    fun searchAlbums(
        query: String,
        limit: Int = 20,
        onResult: (List<Album>) -> Unit,
        onError: (String) -> Unit = { Log.e(TAG, it) }
    ) {
        scope.launch {
            try {
                Log.d(TAG, "searchAlbums: query='$query', limit=$limit")
                
                val python = Python.getInstance()
                val module = python.getModule("ytmusic_recommender")
                val jsonResult = module.callAttr("search_albums", query, limit).toString()
                
                val albums = parseAlbumsFromJson(jsonResult)
                Log.d(TAG, "Found ${albums.size} albums for query: $query")
                
                withContext(Dispatchers.Main) {
                    onResult(albums)
                }
            } catch (e: PyException) {
                Log.e(TAG, "Python search_albums failed", e)
                withContext(Dispatchers.Main) {
                    onError("Album search failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "searchAlbums failed", e)
                withContext(Dispatchers.Main) {
                    onError("Album search failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Search for artists on YouTube Music
     * @param query Search query string
     * @param limit Maximum number of results (default 10)
     * @param onResult Callback with list of artists
     * @param onError Error callback
     */
    fun searchArtists(
        query: String,
        limit: Int = 10,
        onResult: (List<Artist>) -> Unit,
        onError: (String) -> Unit = { Log.e(TAG, it) }
    ) {
        scope.launch {
            try {
                Log.d(TAG, "searchArtists: query='$query', limit=$limit")
                
                val python = Python.getInstance()
                val module = python.getModule("ytmusic_recommender")
                val jsonResult = module.callAttr("search_artists", query, limit).toString()
                
                val artists = parseArtistsFromJson(jsonResult)
                Log.d(TAG, "Found ${artists.size} artists for query: $query")
                
                withContext(Dispatchers.Main) {
                    onResult(artists)
                }
            } catch (e: PyException) {
                Log.e(TAG, "Python search_artists failed", e)
                withContext(Dispatchers.Main) {
                    onError("Artist search failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "searchArtists failed", e)
                withContext(Dispatchers.Main) {
                    onError("Artist search failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Get song recommendations based on a YouTube video ID
     * Only returns audio tracks (songs), filters out music videos
     * @param videoId YouTube video ID of a song
     * @param limit Maximum number of recommendations (default 25)
     * @param onResult Callback with list of recommended songs
     * @param onError Error callback
     */
    fun getRecommendations(
        videoId: String,
        limit: Int = 25,
        onResult: (List<RecommendedSong>) -> Unit,
        onError: (String) -> Unit = { Log.e(TAG, it) }
    ) {
        scope.launch {
            try {
                Log.d(TAG, "getRecommendations: videoId='$videoId', limit=$limit")
                
                val python = Python.getInstance()
                val module = python.getModule("ytmusic_recommender")
                val jsonResult = module.callAttr("get_song_recommendations", videoId, limit).toString()
                
                val recommendations = parseSongsFromJson(jsonResult)
                Log.d(TAG, "Found ${recommendations.size} song recommendations for videoId: $videoId")
                
                recommendations.forEachIndexed { idx, song ->
                    Log.d(TAG, "Rec[$idx] id=${song.videoId} title='${song.title}' artist='${song.artist}'")
                }
                
                withContext(Dispatchers.Main) {
                    onResult(recommendations)
                }
            } catch (e: PyException) {
                Log.e(TAG, "Python get_song_recommendations failed", e)
                withContext(Dispatchers.Main) {
                    onError("Recommendations failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getRecommendations failed", e)
                withContext(Dispatchers.Main) {
                    onError("Recommendations failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Get song recommendations based on a search query
     * First searches for songs, then gets recommendations based on the first result
     * @param query Search query
     * @param limit Maximum number of recommendations (default 25)
     * @param onResult Callback with list of recommended songs
     * @param onError Error callback
     */
    fun getRecommendationsForQuery(
        query: String,
        limit: Int = 25,
        onResult: (List<RecommendedSong>) -> Unit,
        onError: (String) -> Unit = { Log.e(TAG, it) }
    ) {
        scope.launch {
            try {
                Log.d(TAG, "getRecommendationsForQuery: query='$query', limit=$limit")
                
                val python = Python.getInstance()
                val module = python.getModule("ytmusic_recommender")
                val jsonResult = module.callAttr("get_recommendations_for_query", query, limit).toString()
                
                val recommendations = parseSongsFromJson(jsonResult)
                Log.d(TAG, "Found ${recommendations.size} recommendations for query: $query")
                
                withContext(Dispatchers.Main) {
                    onResult(recommendations)
                }
            } catch (e: PyException) {
                Log.e(TAG, "Python get_recommendations_for_query failed", e)
                withContext(Dispatchers.Main) {
                    onError("Query recommendations failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getRecommendationsForQuery failed", e)
                withContext(Dispatchers.Main) {
                    onError("Query recommendations failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Get album details including songs list
     * @param browseId Album browseId from search results
     * @param onResult Callback with album details
     * @param onError Error callback
     */
    fun getAlbumDetails(
        browseId: String,
        onResult: (AlbumDetails?) -> Unit,
        onError: (String) -> Unit = { Log.e(TAG, it) }
    ) {
        scope.launch {
            try {
                Log.d(TAG, "getAlbumDetails: browseId='$browseId'")
                
                val python = Python.getInstance()
                val module = python.getModule("ytmusic_recommender")
                val jsonResult = module.callAttr("get_album_details", browseId).toString()
                
                val albumDetails = parseAlbumDetailsFromJson(jsonResult)
                Log.d(TAG, "Retrieved album: ${albumDetails?.title} with ${albumDetails?.songs?.size} songs")
                
                withContext(Dispatchers.Main) {
                    onResult(albumDetails)
                }
            } catch (e: PyException) {
                Log.e(TAG, "Python get_album_details failed", e)
                withContext(Dispatchers.Main) {
                    onError("Album details fetch failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getAlbumDetails failed", e)
                withContext(Dispatchers.Main) {
                    onError("Album details fetch failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Get artist details including top songs
     * @param browseId Artist browseId from search results
     * @param onResult Callback with artist details
     * @param onError Error callback
     */
    fun getArtistDetails(
        browseId: String,
        onResult: (ArtistDetails?) -> Unit,
        onError: (String) -> Unit = { Log.e(TAG, it) }
    ) {
        scope.launch {
            try {
                Log.d(TAG, "getArtistDetails: browseId='$browseId'")
                
                val python = Python.getInstance()
                val module = python.getModule("ytmusic_recommender")
                val jsonResult = module.callAttr("get_artist_details", browseId).toString()
                
                val artistDetails = parseArtistDetailsFromJson(jsonResult)
                Log.d(TAG, "Retrieved artist: ${artistDetails?.name} with ${artistDetails?.songs?.size} songs")
                
                withContext(Dispatchers.Main) {
                    onResult(artistDetails)
                }
            } catch (e: PyException) {
                Log.e(TAG, "Python get_artist_details failed", e)
                withContext(Dispatchers.Main) {
                    onError("Artist details fetch failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getArtistDetails failed", e)
                withContext(Dispatchers.Main) {
                    onError("Artist details fetch failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Parse songs from JSON string returned by Python
     */
    private fun parseSongsFromJson(jsonString: String): List<RecommendedSong> {
        val songs = mutableListOf<RecommendedSong>()
        
        try {
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                val videoId = jsonObject.optString("videoId", "")
                if (videoId.isEmpty()) continue
                
                val song = RecommendedSong(
                    title = jsonObject.optString("title", "Unknown Title"),
                    artist = jsonObject.optString("artist", "Unknown Artist"),
                    videoId = videoId,
                    watchUrl = "https://www.youtube.com/watch?v=$videoId",
                    thumbnail = jsonObject.optString("thumbnail", "")
                )
                
                songs.add(song)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse songs JSON", e)
        }
        
        return songs
    }
    
    /**
     * Parse albums from JSON string returned by Python
     */
    private fun parseAlbumsFromJson(jsonString: String): List<Album> {
        val albums = mutableListOf<Album>()
        
        try {
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                val browseId = jsonObject.optString("browseId", "")
                if (browseId.isEmpty()) continue
                
                val album = Album(
                    browseId = browseId,
                    title = jsonObject.optString("title", "Unknown Album"),
                    artist = jsonObject.optString("artist", "Unknown Artist"),
                    year = jsonObject.optString("year", ""),
                    thumbnail = jsonObject.optString("thumbnail", "")
                )
                
                albums.add(album)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse albums JSON", e)
        }
        
        return albums
    }
    
    private fun parseArtistsFromJson(jsonString: String): List<Artist> {
        val artists = mutableListOf<Artist>()
        
        try {
            val jsonArray = JSONArray(jsonString)
            
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                
                val browseId = jsonObject.optString("browseId", "")
                if (browseId.isEmpty()) continue
                
                val artist = Artist(
                    browseId = browseId,
                    name = jsonObject.optString("name", "Unknown Artist"),
                    thumbnail = jsonObject.optString("thumbnail", ""),
                    subscribers = jsonObject.optString("subscribers", "")
                )
                
                artists.add(artist)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse artists JSON", e)
        }
        
        return artists
    }
    
    private fun parseAlbumDetailsFromJson(jsonString: String): AlbumDetails? {
        try {
            if (jsonString == "null" || jsonString.isEmpty()) return null
            
            val jsonObject = JSONObject(jsonString)
            val songsArray = jsonObject.optJSONArray("songs") ?: JSONArray()
            
            val songs = mutableListOf<RecommendedSong>()
            for (i in 0 until songsArray.length()) {
                val songObj = songsArray.getJSONObject(i)
                val videoId = songObj.optString("videoId", "")
                val song = RecommendedSong(
                    videoId = videoId,
                    title = songObj.optString("title", "Unknown Title"),
                    artist = songObj.optString("artist", "Unknown Artist"),
                    thumbnail = songObj.optString("thumbnail", ""),
                    watchUrl = "https://www.youtube.com/watch?v=$videoId"
                )
                songs.add(song)
            }
            
            return AlbumDetails(
                browseId = jsonObject.optString("browseId", ""),
                title = jsonObject.optString("title", "Unknown Album"),
                artist = jsonObject.optString("artist", "Unknown Artist"),
                year = jsonObject.optString("year", ""),
                thumbnail = jsonObject.optString("thumbnail", ""),
                trackCount = jsonObject.optInt("trackCount", songs.size),
                duration = jsonObject.optString("duration", ""),
                songs = songs
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse album details JSON", e)
            return null
        }
    }
    
    private fun parseArtistDetailsFromJson(jsonString: String): ArtistDetails? {
        try {
            if (jsonString == "null" || jsonString.isEmpty()) return null
            
            val jsonObject = JSONObject(jsonString)
            val songsArray = jsonObject.optJSONArray("songs") ?: JSONArray()
            
            val songs = mutableListOf<RecommendedSong>()
            for (i in 0 until songsArray.length()) {
                val songObj = songsArray.getJSONObject(i)
                val videoId = songObj.optString("videoId", "")
                val song = RecommendedSong(
                    videoId = videoId,
                    title = songObj.optString("title", "Unknown Title"),
                    artist = songObj.optString("artist", "Unknown Artist"),
                    thumbnail = songObj.optString("thumbnail", ""),
                    watchUrl = "https://www.youtube.com/watch?v=$videoId"
                )
                songs.add(song)
            }
            
            return ArtistDetails(
                browseId = jsonObject.optString("browseId", ""),
                name = jsonObject.optString("name", "Unknown Artist"),
                description = jsonObject.optString("description", ""),
                thumbnail = jsonObject.optString("thumbnail", ""),
                subscribers = jsonObject.optString("subscribers", ""),
                songs = songs
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse artist details JSON", e)
            return null
        }
    }
}

/**
 * Data class for album information
 */
data class Album(
    val browseId: String,
    val title: String,
    val artist: String,
    val year: String,
    val thumbnail: String
)

/**
 * Data class for artist information
 */
data class Artist(
    val browseId: String,
    val name: String,
    val thumbnail: String,
    val subscribers: String
)

/**
 * Data class for album details with songs
 */
data class AlbumDetails(
    val browseId: String,
    val title: String,
    val artist: String,
    val year: String,
    val thumbnail: String,
    val trackCount: Int,
    val duration: String,
    val songs: List<RecommendedSong>
)

/**
 * Data class for artist details with songs
 */
data class ArtistDetails(
    val browseId: String,
    val name: String,
    val description: String,
    val thumbnail: String,
    val subscribers: String,
    val songs: List<RecommendedSong>
)
