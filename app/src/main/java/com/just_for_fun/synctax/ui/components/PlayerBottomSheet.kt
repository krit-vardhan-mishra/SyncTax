package com.just_for_fun.synctax.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.core.data.local.entities.Song
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBottomSheet(
    scaffoldState: BottomSheetScaffoldState, // Accept the hoisted state
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
    content: @Composable (PaddingValues) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    // Removed local scaffoldState, using the one from the parameters

    val miniPlayerHeight = 80.dp
    val isExpanded = scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded

    // --- THIS IS THE CHANGE ---
    // Dynamically set peek height. If no song, peek height is 0.
    val currentPeekHeight = if (song != null) miniPlayerHeight else 0.dp
    // --- END CHANGE ---

    BottomSheetScaffold(
        scaffoldState = scaffoldState, // Use the hoisted state
        sheetContent = {
            // Use UnifiedPlayer - single component that morphs between states
            // This 'if' check ensures we don't try to render UnifiedPlayer
            // when the sheet is animating closed after a song becomes null.
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
                    isExpanded = isExpanded,
                    onExpandedChange = { expand ->
                        coroutineScope.launch {
                            if (expand) {
                                scaffoldState.bottomSheetState.expand()
                            } else {
                                scaffoldState.bottomSheetState.partialExpand()
                            }
                        }
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
        // --- THIS IS THE CHANGE ---
        sheetPeekHeight = currentPeekHeight, // Use the dynamic height
        // --- END CHANGE ---
        sheetDragHandle = null,
        sheetShape = MaterialTheme.shapes.extraSmall
    ) {
        content(it)
    }
}