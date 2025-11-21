package com.just_for_fun.synctax.core.data.model

/**
 * Data class representing a single line of synchronized lyrics.
 *
 * @property timestamp The timestamp of the lyric line in milliseconds.
 * @property text The text content of the lyric line.
 */
data class LyricLine(
    val timestamp: Long,
    val text: String
)
