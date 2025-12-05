package com.just_for_fun.synctax.core.ml.models

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
    val sessionContext: Double,       // Context similarity score
    val durationScore: Double,        // Song duration preference match (0-1)
    val albumAffinity: Double,        // User's affinity to this album
    val releaseYearScore: Double,     // Preference for song release year
    val songPopularity: Double,       // How popular this song is for the user
    val tempoEnergy: Double           // Estimated tempo/energy level (0-1)
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
        sessionContext,
        durationScore,
        albumAffinity,
        releaseYearScore,
        songPopularity,
        tempoEnergy
    )
}
