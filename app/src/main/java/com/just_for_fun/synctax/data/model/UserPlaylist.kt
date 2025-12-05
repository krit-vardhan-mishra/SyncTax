package com.just_for_fun.synctax.data.model

data class UserPlaylist(
    val id: String,
    val name: String,
    val songCount: Int,
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
