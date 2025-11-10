package com.just_for_fun.youtubemusic.core.ml.models

data class SongFeatures(
    val songId: String,
    val playFrequency: Double,        // How often played
    val avgCompletionRate: Double,    // Average % of song completed
    val skipRate: Double,             // % of times skipped
    val recencyScore: Double,         // How recently played (0-1)
    val timeOfDayMatch: Double,       // Match with current time preferences
    val genreAffinity: Double,        // User's affinity to this genre
    val artistAffinity: Double,       // User's affinity to this artist
    val consecutivePlays: Double,     // How many times played in a row
    val sessionContext: Double        // Context similarity score
) {
    fun toVector(): DoubleArray = doubleArrayOf(
        playFrequency,
        avgCompletionRate,
        skipRate,
        recencyScore,
        timeOfDayMatch,
        genreAffinity,
        artistAffinity,
        consecutivePlays,
        sessionContext
    )
}