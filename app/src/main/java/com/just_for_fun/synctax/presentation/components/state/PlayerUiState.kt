package com.just_for_fun.synctax.presentation.components.state

import androidx.compose.runtime.Stable
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.data.local.entities.Format

/**
 * UI state for the player, marked @Stable to reduce Compose recomposition overhead.
 */
@Stable
data class PlayerUiState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isLoadingSong: Boolean = false,
    val position: Long = 0L,
    val duration: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatEnabled: Boolean = false,
    val downloadPercent: Int = 0,
    val volume: Float = 1.0f,
    val downloadedSongs: Set<String> = emptySet(),
    val downloadingSongs: Set<String> = emptySet(),
    val downloadProgress: Map<String, Float> = emptyMap(), // songId to progress (0.0 to 1.0)
    val downloadMessage: String? = null, // Download location message for snackbar
    val showFormatDialog: Boolean = false,
    val availableFormats: List<Format> = emptyList(),  // Changed from AudioFormat to Format
    val isLoadingFormats: Boolean = false, // Loading indicator for format selection
    val selectedFormat: Format? = null,  // Currently selected format for download
    val upNextRecommendations: List<Song> = emptyList(), // YouTube-recommended songs for online player
    val isLoadingRecommendations: Boolean = false // Loading indicator for recommendations
)
