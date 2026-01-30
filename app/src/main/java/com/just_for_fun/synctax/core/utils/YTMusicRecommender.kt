package com.just_for_fun.synctax.core.utils

import android.util.Log
import com.chaquo.python.PyException
import com.chaquo.python.Python
import com.just_for_fun.synctax.core.network.OnlineSearchResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import com.just_for_fun.synctax.core.network.OnlineResultType


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
     * Search for videos
     * @param query Search query
     * @param limit Maximum number of results (default 20)
     * @param onResult Callback with list of videos (as RecommendedSong)
     * @param onError Error callback
     */
    fun searchVideos(
        query: String,
        limit: Int = 20,
        onResult: (List<RecommendedSong>) -> Unit,
        onError: (String) -> Unit = { Log.e(TAG, it) }
    ) {
        scope.launch {
            try {
                Log.d(TAG, "searchVideos: query='$query', limit=$limit")
                
                val python = Python.getInstance()
                val module = python.getModule("ytmusic_recommender")
                val jsonResult = module.callAttr("search_videos", query, limit).toString()
                
                // We can reuse song parser as the Python function returns compatible JSON structure
                val videos = parseSongsFromJson(jsonResult)
                Log.d(TAG, "Found ${videos.size} videos for query: $query")
                
                withContext(Dispatchers.Main) {
                    onResult(videos)
                }
            } catch (e: PyException) {
                Log.e(TAG, "Python search_videos failed", e)
                withContext(Dispatchers.Main) {
                    onError("Video search failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "searchVideos failed", e)
                withContext(Dispatchers.Main) {
                    onError("Video search failed: ${e.message}")
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
     * Get all recommendations (songs + videos) based on a YouTube video ID
     * Does NOT filter by videoType, useful for music videos that need video recommendations
     * @param videoId YouTube video ID
     * @param limit Maximum number of recommendations (default 25)
     * @param onResult Callback with list of recommended songs/videos
     * @param onError Error callback
     */
    fun getAllRecommendations(
        videoId: String,
        limit: Int = 25,
        onResult: (List<RecommendedSong>) -> Unit,
        onError: (String) -> Unit = { Log.e(TAG, it) }
    ) {
        scope.launch {
            try {
                Log.d(TAG, "getAllRecommendations: videoId='$videoId', limit=$limit")
                
                val python = Python.getInstance()
                val module = python.getModule("ytmusic_recommender")
                val jsonResult = module.callAttr("get_all_recommendations", videoId, limit).toString()
                
                val recommendations = parseSongsFromJson(jsonResult)
                Log.d(TAG, "Found ${recommendations.size} all-type recommendations for videoId: $videoId")
                
                recommendations.forEachIndexed { idx, song ->
                    Log.d(TAG, "AllRec[$idx] id=${song.videoId} title='${song.title}' artist='${song.artist}'")
                }
                
                withContext(Dispatchers.Main) {
                    onResult(recommendations)
                }
            } catch (e: PyException) {
                Log.e(TAG, "Python get_all_recommendations failed", e)
                withContext(Dispatchers.Main) {
                    onError("All recommendations failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getAllRecommendations failed", e)
                withContext(Dispatchers.Main) {
                    onError("All recommendations failed: ${e.message}")
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
                Log.d(TAG, "Retrieved artist: ${artistDetails?.name} with ${artistDetails?.songs?.size} songs (hasMore: ${artistDetails?.hasMoreSongs})")
                
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
     * Get all songs from an artist using the songs browseId (for pagination/load more)
     * @param songsBrowseId The browseId from artist.songsBrowseId
     * @param limit Maximum number of songs to return (default 100)
     * @param onResult Callback with list of songs
     * @param onError Error callback
     */
    fun getArtistAllSongs(
        songsBrowseId: String,
        limit: Int = 100,
        onResult: (List<RecommendedSong>) -> Unit,
        onError: (String) -> Unit = { Log.e(TAG, it) }
    ) {
        scope.launch {
            try {
                Log.d(TAG, "getArtistAllSongs: songsBrowseId='$songsBrowseId', limit=$limit")
                
                val python = Python.getInstance()
                val module = python.getModule("ytmusic_recommender")
                val jsonResult = module.callAttr("get_artist_all_songs", songsBrowseId, limit).toString()
                
                val songs = parseSongsFromJson(jsonResult)
                Log.d(TAG, "Retrieved ${songs.size} songs from artist")
                
                withContext(Dispatchers.Main) {
                    onResult(songs)
                }
            } catch (e: PyException) {
                Log.e(TAG, "Python get_artist_all_songs failed", e)
                withContext(Dispatchers.Main) {
                    onError("Failed to load more songs: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getArtistAllSongs failed", e)
                withContext(Dispatchers.Main) {
                    onError("Failed to load more songs: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Get search suggestions for the given query
     * @param query Partial search query string
     * @param onResult Callback with list of suggestion strings
     * @param onError Error callback
     */
    fun getSearchSuggestions(
        query: String,
        onResult: (List<String>) -> Unit,
        onError: (String) -> Unit = { Log.e(TAG, it) }
    ) {
        scope.launch {
            try {
                Log.d(TAG, "getSearchSuggestions: query='$query'")
                
                val python = Python.getInstance()
                val module = python.getModule("ytmusic_recommender")
                val jsonResult = module.callAttr("get_search_suggestions", query).toString()
                
                val suggestions = parseSuggestionsFromJson(jsonResult)
                Log.d(TAG, "Found ${suggestions.size} suggestions for query: $query")
                
                withContext(Dispatchers.Main) {
                    onResult(suggestions)
                }
            } catch (e: PyException) {
                Log.e(TAG, "Python get_search_suggestions failed", e)
                withContext(Dispatchers.Main) {
                    onError("Search suggestions failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "getSearchSuggestions failed", e)
                withContext(Dispatchers.Main) {
                    onError("Search suggestions failed: ${e.message}")
                }
            }
        }
    }
    
    /**
     * Parse search suggestions from JSON string
     */
    private fun parseSuggestionsFromJson(jsonString: String): List<String> {
        val suggestions = mutableListOf<String>()
        try {
            val jsonArray = JSONArray(jsonString)
            for (i in 0 until jsonArray.length()) {
                suggestions.add(jsonArray.getString(i))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse suggestions JSON", e)
        }
        return suggestions
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
                description = jsonObject.optString("description", ""),
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
                songs = songs,
                songsBrowseId = jsonObject.optString("songsBrowseId", null),
                hasMoreSongs = jsonObject.optBoolean("hasMoreSongs", false),
                totalSongsAvailable = jsonObject.optInt("totalSongsAvailable", 0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse artist details JSON", e)
            return null
        }
    }
    /**
     * Search for all types of content (Songs, Videos, Albums, Artists, Podcasts)
     * @param query Search query
     * @param limit Maximum number of results (default 20)
     * @param onResult Callback with list of mixed results
     * @param onError Error callback
     */
    fun searchAll(
        query: String,
        limit: Int = 20,
        onResult: (List<OnlineSearchResult>) -> Unit,
        onError: (String) -> Unit = { Log.e(TAG, it) }
    ) {
        scope.launch {
            try {
                Log.d(TAG, "searchAll: query='$query', limit=$limit")

                val python = Python.getInstance()
                val module = python.getModule("ytmusic_recommender")
                val jsonResult = module.callAttr("search_all", query, limit).toString()

                val results = parseMixedResultsFromJson(jsonResult)
                Log.d(TAG, "Found ${results.size} mixed results for query: $query")

                withContext(Dispatchers.Main) {
                    onResult(results)
                }
            } catch (e: PyException) {
                Log.e(TAG, "Python search_all failed", e)
                withContext(Dispatchers.Main) {
                    onError("Search failed: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "searchAll failed", e)
                withContext(Dispatchers.Main) {
                    onError("Search failed: ${e.message}")
                }
            }
        }
    }

    private fun parseMixedResultsFromJson(jsonString: String): List<OnlineSearchResult> {
        val results = mutableListOf<OnlineSearchResult>()

        try {
            val jsonArray = JSONArray(jsonString)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val typeStr = obj.optString("resultType", "unknown")

                val type = when (typeStr) {
                    "song" -> OnlineResultType.SONG
                    "video" -> OnlineResultType.VIDEO
                    "album" -> OnlineResultType.ALBUM
                    "artist" -> OnlineResultType.ARTIST
                    "podcast" -> OnlineResultType.PODCAST
                    "episode" -> OnlineResultType.EPISODE
                    else -> continue // Skip unknown types
                }

                // ID handling depends on type
                val id = when (type) {
                    OnlineResultType.ALBUM, OnlineResultType.ARTIST, OnlineResultType.PODCAST -> obj.optString("browseId", "")
                    else -> obj.optString("videoId", "")
                }

                if (id.isEmpty()) continue

                val result = OnlineSearchResult(
                    id = id,
                    title = obj.optString("title", "Unknown Title"),
                    author = obj.optString("artist", "Unknown Artist"), // Normalized to 'artist' in Python
                    duration = if (type == OnlineResultType.SONG || type == OnlineResultType.VIDEO || type == OnlineResultType.EPISODE) {
                        parseDuration(obj.optString("duration", "0:00"))
                    } else null,
                    thumbnailUrl = obj.optString("thumbnail", ""),
                    streamUrl = null,
                    type = type,
                    year = obj.optString("year", null as String?),
                    browseId = if (type == OnlineResultType.ALBUM || type == OnlineResultType.ARTIST || type == OnlineResultType.PODCAST) id else null
                )
                results.add(result)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse mixed results JSON", e)
        }
        return results
    }

    private fun parseDuration(durationStr: String): Long {
        try {
            val parts = durationStr.split(":")
            if (parts.size == 2) {
                val min = parts[0].toLongOrNull() ?: 0L
                val sec = parts[1].toLongOrNull() ?: 0L
                return (min * 60 + sec) * 1000
            } else if (parts.size == 3) {
                val hr = parts[0].toLongOrNull() ?: 0L
                val min = parts[1].toLongOrNull() ?: 0L
                val sec = parts[2].toLongOrNull() ?: 0L
                return (hr * 3600 + min * 60 + sec) * 1000
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing duration: $durationStr", e)
        }
        return 0L
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
    val description: String,
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
    val songs: List<RecommendedSong>,
    val songsBrowseId: String? = null,
    val hasMoreSongs: Boolean = false,
    val totalSongsAvailable: Int = 0
)
