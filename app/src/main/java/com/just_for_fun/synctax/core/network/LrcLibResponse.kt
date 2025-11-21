package com.just_for_fun.synctax.core.network

import com.google.gson.annotations.SerializedName

/**
 * Response model for LRCLIB API
 * Represents synchronized and plain lyrics for a track
 */
data class LrcLibResponse(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("trackName")
    val trackName: String,
    
    @SerializedName("artistName")
    val artistName: String,
    
    @SerializedName("albumName")
    val albumName: String?,
    
    @SerializedName("duration")
    val duration: Int,
    
    @SerializedName("instrumental")
    val instrumental: Boolean,
    
    @SerializedName("plainLyrics")
    val plainLyrics: String?,
    
    @SerializedName("syncedLyrics")
    val syncedLyrics: String?
)
