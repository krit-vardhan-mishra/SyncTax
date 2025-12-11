package com.just_for_fun.synctax.presentation.components.player

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.BottomSheetScaffoldState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.local.entities.Song

private const val TAG = "PlayerBottomSheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBottomSheet(
    scaffoldState: BottomSheetScaffoldState, // For the visual animation only
    song: Song?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    position: Long,
    duration: Long,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    volume: Float = 1.0f,
    upNext: List<Song>,
    playHistory: List<Song> = emptyList(),
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onSelectSong: (Song) -> Unit,
    onPlaceNext: (Song) -> Unit,
    onRemoveFromQueue: (Song) -> Unit = {},
    onReorderQueue: (Int, Int) -> Unit = { _, _ -> },
    onSetVolume: (Float) -> Unit = {},
    downloadPercent: Int = 0,
    // NEW: Hoisted expansion state - completely controlled by MusicApp
    isExpanded: Boolean = false,
    onExpandedChange: (Boolean) -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val miniPlayerHeight = 80.dp
    
    val currentPeekHeight = if (song != null) miniPlayerHeight else 0.dp
    
    // Track previous song to detect when song goes from null to non-null
    var previousSongId by remember { mutableStateOf<String?>(null) }
    
    // Ensure mini-player is visible when a song is first set (only on initial song set)
    LaunchedEffect(song?.id) {
        if (song != null && previousSongId == null) {
            scaffoldState.bottomSheetState.partialExpand()
        }
        previousSongId = song?.id
    }

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            if (song != null) {
                UnifiedPlayer(
                    song = song,
                    isPlaying = isPlaying,
                    isBuffering = isBuffering,
                    position = position,
                    duration = duration,
                    shuffleEnabled = shuffleEnabled,
                    repeatEnabled = repeatEnabled,
                    volume = volume,
                    upNext = upNext,
                    playHistory = playHistory,
                    isExpanded = isExpanded, // Use the hoisted state directly
                    onExpandedChange = { expand ->
                        onExpandedChange(expand) // Just call the hoisted callback
                    },
                    onSelectSong = onSelectSong,
                    onPlaceNext = onPlaceNext,
                    onRemoveFromQueue = onRemoveFromQueue,
                    onReorderQueue = onReorderQueue,
                    onSetVolume = onSetVolume,
                    downloadPercent = downloadPercent,
                    onPlayPauseClick = onPlayPauseClick,
                    onNextClick = onNextClick,
                    onPreviousClick = onPreviousClick,
                    onShuffleClick = onShuffleClick,
                    onRepeatClick = onRepeatClick,
                    onSeek = onSeek
                )
            }
        },
        sheetPeekHeight = currentPeekHeight,
        sheetDragHandle = null,
        sheetShape = MaterialTheme.shapes.extraSmall
    ) {
        content(it)
    }
}
