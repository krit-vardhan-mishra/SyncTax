package com.just_for_fun.synctax.core.ml.models

data class UserProfile(
    val totalPlays: Int,
    val favoriteGenres: Map<String, Double>,
    val favoriteArtists: Map<String, Double>,
    val preferredTimeSlots: Map<Int, Double>, // Hour -> preference weight
    val avgSessionLength: Long,
    val skipThreshold: Float = 0.3f,
    val isProfileTrained: Boolean = false
)