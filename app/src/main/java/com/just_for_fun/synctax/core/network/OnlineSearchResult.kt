package com.just_for_fun.synctax.core.network

data class OnlineSearchResult(
    val id: String,
    val title: String,
    val author: String?,
    val duration: Long?,
    val thumbnailUrl: String?,
    var streamUrl: String?, // direct playable audio url (fetched on-demand)
    val type: OnlineResultType = OnlineResultType.SONG, // Type of result (song/album/video)
    val year: String? = null, // Album year (for albums)
    val browseId: String? = null // Browse ID for albums to fetch album details
)
