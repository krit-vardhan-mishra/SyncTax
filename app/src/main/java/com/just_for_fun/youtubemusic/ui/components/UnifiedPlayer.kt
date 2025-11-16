package com.just_for_fun.youtubemusic.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.just_for_fun.youtubemusic.core.data.local.entities.Song
import com.just_for_fun.youtubemusic.ui.components.player.FullScreenPlayerContent
import com.just_for_fun.youtubemusic.ui.components.player.MiniPlayerContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnifiedPlayer(
    song: Song,
    isPlaying: Boolean,
    isBuffering: Boolean,
    position: Long,
    duration: Long,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    volume: Float = 1.0f,
    upNext: List<Song> = emptyList(),
    playHistory: List<Song> = emptyList(),
    isExpanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelectSong: (Song) -> Unit = {},
    onPlaceNext: (Song) -> Unit = {},
    onRemoveFromQueue: (Song) -> Unit = {},
    onReorderQueue: (Int, Int) -> Unit = { _, _ -> },
    onSetVolume: (Float) -> Unit = {},
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSeek: (Long) -> Unit,
    downloadPercent: Int = 0
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    // Animation progress (0f = mini, 1f = expanded)
    val expansionProgress by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "expansion"
    )

    var showUpNext by remember { mutableStateOf(false) }

    val albumArtMiniScale by animateFloatAsState(
        targetValue = if (isExpanded) 1.0f else 0.85f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "albumArtMiniScale"
    )

    // Handle back button when player is expanded
    BackHandler(enabled = isExpanded) {
        onExpandedChange(false)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(if (isExpanded) (expansionProgress * 1000).dp else 80.dp)
    ) {
        // Background (only visible when expanded)
        if (expansionProgress > 0f) {
            if (!song.albumArtUri.isNullOrEmpty()) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur((80 * expansionProgress).dp)
                        .alpha(0.6f * expansionProgress)
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f * expansionProgress))
            )
        }

        Surface(
            color = if (isExpanded) Color.Transparent
            else MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = if (isExpanded) 0.dp else 3.dp,
            shape = if (isExpanded) RectangleShape else RectangleShape,
            modifier = Modifier.fillMaxSize()
        ) {
            if (!isExpanded) {
                // MINI PLAYER VIEW
                MiniPlayerContent(
                    song = song,
                    isPlaying = isPlaying,
                    position = position,
                    duration = duration,
                    albumArtScale = albumArtMiniScale,
                    onPlayPauseClick = onPlayPauseClick,
                    onNextClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNextClick()
                    },
                    onPreviousClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPreviousClick()
                    },
                    onClick = { onExpandedChange(true) },
                    onSwipeUp = { onExpandedChange(true) }
                )
            } else {
                // FULL SCREEN PLAYER VIEW
                FullScreenPlayerContent(
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
                    showUpNext = showUpNext,
                    snackbarHostState = snackbarHostState,
                    onShowUpNextChange = { showUpNext = it },
                    onSelectSong = onSelectSong,
                    onPlaceNext = onPlaceNext,
                    onRemoveFromQueue = onRemoveFromQueue,
                    onReorderQueue = onReorderQueue,
                    onSetVolume = onSetVolume,
                    onPlayPauseClick = onPlayPauseClick,
                    onNextClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNextClick()
                    },
                    onPreviousClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onPreviousClick()
                    },
                    onShuffleClick = onShuffleClick,
                    onRepeatClick = onRepeatClick,
                    onSeek = onSeek,
                    onClose = { onExpandedChange(false) },
                    expansionProgress = expansionProgress,
                    downloadPercent = downloadPercent
                )
            }
        }
    }
}
