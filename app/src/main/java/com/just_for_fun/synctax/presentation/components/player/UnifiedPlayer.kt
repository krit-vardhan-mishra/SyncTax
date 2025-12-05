package com.just_for_fun.synctax.presentation.components.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.ui.theme.PlayerBackground

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
    downloadPercent: Int = 0,
) {
    val snackBarHostState = remember { SnackbarHostState() }
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

    BackHandler(enabled = isExpanded) {
        onExpandedChange(false)
    }

    // Main Container
    Box(
        modifier = Modifier
            .fillMaxWidth()
            // Calculate height precisely: Min 80dp, Max expands to full screen
            .height(if (isExpanded) (expansionProgress * 1000).dp else 80.dp)
            .background(PlayerBackground)
    ) {
        // --- 1. ALIVE BACKGROUND LAYER ---
        if (!song.albumArtUri.isNullOrEmpty()) {
            // Blur Logic: Less blur when mini (to see texture), more when expanded (ambient)
            val blurRadius = if (isExpanded) 50.dp else 20.dp

            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
                    .alpha(0.6f)
            )

            // Gradient Overlay for contrast
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.2f),
                                Color.Black.copy(alpha = 0.6f)
                            )
                        )
                    )
            )
        }

        // --- 2. CONTENT LAYER ---
        Surface(
            color = Color.Transparent, // Must be transparent for background to show
            tonalElevation = 0.dp,     // Must be 0 to prevent M3 surface tint
            shape = RectangleShape,
            modifier = Modifier.fillMaxSize()
        ) {
            if (!isExpanded) {
                MiniPlayerContent(
                    song = song,
                    isPlaying = isPlaying,
                    position = position,
                    duration = duration,
                    albumArtScale = albumArtMiniScale,
                    // Pass Transparent here to trigger the fix in MiniPlayerContent
                    backgroundColor = Color.Transparent,
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
                    snackbarHostState = snackBarHostState,
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
