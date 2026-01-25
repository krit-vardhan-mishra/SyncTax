package com.just_for_fun.synctax.presentation.components.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
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
import com.just_for_fun.synctax.presentation.ui.theme.PlayerSurface
import com.just_for_fun.synctax.presentation.ui.theme.PlayerTextSecondary

private const val TAG = "UnifiedPlayer"

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
    // Progress from AnchoredDraggable (0 = collapsed, 1 = expanded)
    progress: Float,
    miniControlsAlpha: Float,
    fullControlsAlpha: Float,
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

    var showUpNext by remember { mutableStateOf(false) }

    // Use progress directly from parent (AnchoredDraggable)
    val expansionProgress = progress
    
    // Hide mini player image immediately when dragging starts to show the morphing image instead
    val albumArtMiniScale = if (expansionProgress > 0.01f) {
        0f
    } else {
        PlayerSheetConstants.ALBUM_ART_MINI_SCALE
    }

    // Use alpha values from parent for smoother phasing with drag
    val miniPlayerAlpha = miniControlsAlpha
    val expandedAlpha = fullControlsAlpha

    BackHandler(enabled = isExpanded) {
        onExpandedChange(false)
    }

    // Main Container - uses full size since parent controls positioning
    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxSize()
            .background(PlayerBackground)
    ) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val maxWidth = maxWidth
        
        // --- 1. ENHANCED ANIMATED BACKGROUND LAYER ---
        if (!song.albumArtUri.isNullOrEmpty()) {
            // Dynamic blur radius interpolates smoothly based on expansion
            val blurRadius = (PlayerSheetConstants.MIN_BLUR_RADIUS_DP +
                    (expansionProgress * PlayerSheetConstants.BLUR_RADIUS_EXPANSION_DP)).dp

            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(blurRadius)
                    .alpha(
                        PlayerSheetConstants.MIN_BACKGROUND_OPACITY +
                                (expansionProgress * PlayerSheetConstants.BACKGROUND_OPACITY_EXPANSION)
                    )
                    .scale(
                        PlayerSheetConstants.MIN_BACKGROUND_SCALE +
                                (expansionProgress * PlayerSheetConstants.BACKGROUND_SCALE_EXPANSION)
                    )
            )

            // Animated gradient overlay for smooth contrast transitions
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(
                                    alpha = PlayerSheetConstants.MIN_GRADIENT_TOP_ALPHA +
                                            (expansionProgress * PlayerSheetConstants.GRADIENT_TOP_ALPHA_EXPANSION)
                                ),
                                Color.Black.copy(
                                    alpha = PlayerSheetConstants.MIN_GRADIENT_BOTTOM_ALPHA +
                                            (expansionProgress * PlayerSheetConstants.GRADIENT_BOTTOM_ALPHA_EXPANSION)
                                )
                            )
                        )
                    )
            )
        }

        // --- 2. CONTENT LAYER WITH ALPHA BLENDING ---
        Surface(
            color = Color.Transparent, // Must be transparent for background to show
            tonalElevation = 0.dp,     // Must be 0 to prevent M3 surface tint
            shape = RectangleShape,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Mini player content with fade-out animation
                if (miniPlayerAlpha > PlayerSheetConstants.ALPHA_RENDER_THRESHOLD) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(miniPlayerAlpha)
                    ) {
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
                            onClick = {
                                onExpandedChange(true)
                            },
                            onSwipeUp = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onExpandedChange(true)
                            }
                        )
                    }
                }

                // Expanded player content with fade-in animation
                if (expandedAlpha > PlayerSheetConstants.ALPHA_RENDER_THRESHOLD) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(expandedAlpha)
                    ) {
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
                            onClose = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onExpandedChange(false)
                            },
                            expansionProgress = expansionProgress,
                            downloadPercent = downloadPercent
                        )
                    }
                }
            }
        }
        
        // --- 3. MORPHING ALBUM ART LAYER ---
        // This renders the album art growing from mini-player position to full-player position
        if (progress > 0.01f && fullControlsAlpha < 1f) {
            val startSize = 48.dp
            val targetSize = 320.dp // Approximate full player size
            
            // Coordinates based on MiniPlayerContent layout
            // MiniPlayer: Vertically centered in 80dp (top ~16dp), Left padding 16dp + 8dp inner = 24dp
            val startTop = 16.dp 
            val startStart = 24.dp
            
            // FullPlayer: Centered horizontally, Top offset approx 100dp
            val targetTop = 100.dp
            val targetStart = (maxWidth - targetSize) / 2
            
            val currentSize = androidx.compose.ui.unit.lerp(startSize, targetSize, progress)
            val currentTop = androidx.compose.ui.unit.lerp(startTop, targetTop, progress)
            val currentStart = androidx.compose.ui.unit.lerp(startStart, targetStart, progress)
            val currentCorner = androidx.compose.ui.unit.lerp(8.dp, 16.dp, progress)
            
            // Fade out as the full player takes over (optional, currently stays opaque until full player is fully visible)
            // Using inverse of fullControlsAlpha to crossfade properly
            val morphAlpha = 1f - fullControlsAlpha

            Surface(
                modifier = Modifier
                    .offset(x = currentStart, y = currentTop)
                    .size(currentSize)
                    .alpha(morphAlpha),
                shape = RoundedCornerShape(currentCorner),
                color = PlayerSurface,
                shadowElevation = 8.dp * progress
            ) {
                if (song.albumArtUri.isNullOrEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                         Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(currentSize / 2),
                            tint = PlayerTextSecondary
                        )
                    }
                } else {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
