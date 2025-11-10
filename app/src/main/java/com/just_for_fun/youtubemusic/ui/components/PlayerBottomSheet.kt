package com.just_for_fun.youtubemusic.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.just_for_fun.youtubemusic.core.data.local.entities.Song
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerBottomSheet(
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
    content: @Composable (PaddingValues) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState(
        bottomSheetState = rememberStandardBottomSheetState(
            initialValue = SheetValue.PartiallyExpanded,
            skipHiddenState = true
        )
    )

    val miniPlayerHeight = 80.dp // Approximate height of the MiniPlayer

    val expansionFraction by animateFloatAsState(
        targetValue = if (scaffoldState.bottomSheetState.currentValue == SheetValue.Expanded) 1f else 0f,
        label = "expansion"
    )

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetContent = {
            Box(modifier = Modifier.fillMaxWidth()) {
                // MiniPlayer visible when collapsed
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = 1f - expansionFraction
                    }
                ) {
                    if (song != null) {
                        MiniPlayer(
                            song = song,
                            isPlaying = isPlaying,
                            position = position,
                            duration = duration,
                            onPlayPauseClick = onPlayPauseClick,
                            onNextClick = onNextClick,
                            onClick = {
                                coroutineScope.launch {
                                    scaffoldState.bottomSheetState.expand()
                                }
                            }
                        )
                    }
                }

                // FullScreenPlayer visible when expanded
                Box(
                    modifier = Modifier.graphicsLayer {
                        alpha = expansionFraction
                    }
                ) {
                    if (song != null) {
                        YTFullScreenPlayer(
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
                            onPlayPauseClick = onPlayPauseClick,
                            onNextClick = onNextClick,
                            onPreviousClick = onPreviousClick,
                            onShuffleClick = onShuffleClick,
                            onRepeatClick = onRepeatClick,
                            onSeek = onSeek,
                            onSelectSong = onSelectSong,
                            onPlaceNext = onPlaceNext,
                            onRemoveFromQueue = onRemoveFromQueue,
                            onReorderQueue = onReorderQueue,
                            onSetVolume = onSetVolume,
                            onClose = {
                                coroutineScope.launch {
                                    scaffoldState.bottomSheetState.partialExpand()
                                }
                            }
                        )
                    }
                }
            }
        },
        sheetPeekHeight = miniPlayerHeight,
        sheetDragHandle = null,
        sheetShape = MaterialTheme.shapes.extraSmall
    ) {
        content(it)
    }
}
