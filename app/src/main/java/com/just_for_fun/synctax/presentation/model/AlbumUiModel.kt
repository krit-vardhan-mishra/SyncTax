package com.just_for_fun.synctax.presentation.model

/**
 * UI Model for displaying album information
 */
data class AlbumUiModel(
    val name: String,
    val artist: String,
    val songCount: Int = 0,
    val albumArtUri: String? = null,
    val isOnline: Boolean = false,
    val browseId: String? = null,  // For fetching full album details from online API
    val year: String? = null,
    val description: String? = null
)
