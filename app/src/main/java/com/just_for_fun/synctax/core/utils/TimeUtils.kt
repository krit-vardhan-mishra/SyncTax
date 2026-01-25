package com.just_for_fun.synctax.core.utils

/**
 * Utility functions for time formatting
 */
object TimeUtils {
    fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
}