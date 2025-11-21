package com.just_for_fun.synctax.core.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * LRCLIB API interface for fetching synchronized lyrics
 * API Documentation: https://lrclib.net/docs
 */
interface LrcLibApi {
    
    /**
     * Get lyrics by track information
     * This endpoint attempts to find the best match for the given track
     * 
     * @param trackName The title of the track (required)
     * @param artistName The name of the artist (required)
     * @param albumName The name of the album (optional)
     * @param duration The track's duration in seconds (optional but recommended for better matching)
     * @return LrcLibResponse containing synced and plain lyrics
     */
    @GET("api/get")
    suspend fun getLyrics(
        @Query("track_name") trackName: String,
        @Query("artist_name") artistName: String,
        @Query("album_name") albumName: String? = null,
        @Query("duration") duration: Int? = null
    ): LrcLibResponse
    
    /**
     * Search for lyrics records
     * Returns an array of lyrics records matching the search criteria
     * Maximum 20 results, no pagination
     * 
     * @param trackName Specific track name to search for
     * @param artistName Specific artist name to search for
     * @param query General search query
     * @return List of matching lyrics records
     */
    @GET("api/search")
    suspend fun searchLyrics(
        @Query("track_name") trackName: String? = null,
        @Query("artist_name") artistName: String? = null,
        @Query("q") query: String? = null
    ): List<LrcLibResponse>
}
