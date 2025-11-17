package com.just_for_fun.synctax.core.network

data class OnlineSearchResult(
    val id: String,
    val title: String,
    val author: String?,
    val duration: Long?,
    val thumbnailUrl: String?,
    var streamUrl: String? // direct playable audio url (fetched on-demand)
)
