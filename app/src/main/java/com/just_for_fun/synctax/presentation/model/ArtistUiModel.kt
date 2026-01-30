package com.just_for_fun.synctax.presentation.model

data class ArtistUiModel(
    val name: String,
    val songCount: Int = 0,
    val isOnline: Boolean = false,
    val imageUrl: String? = null,
    val browseId: String? = null,  // For fetching full artist details
    val subscribers: String? = null
)
