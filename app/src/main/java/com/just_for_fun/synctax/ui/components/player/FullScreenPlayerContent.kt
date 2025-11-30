package com.just_for_fun.synctax.ui.components.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.core.data.model.LyricLine
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.components.FormatSelectionDialog
import com.just_for_fun.synctax.ui.components.app.TooltipIconButton
import com.just_for_fun.synctax.ui.guide.GuideContent
import com.just_for_fun.synctax.ui.guide.GuideOverlay
import com.just_for_fun.synctax.ui.theme.PlayerAccent
import com.just_for_fun.synctax.ui.theme.PlayerIconColor
import com.just_for_fun.synctax.ui.theme.PlayerOnAccent
import com.just_for_fun.synctax.ui.theme.PlayerSurface
import com.just_for_fun.synctax.ui.theme.PlayerSurfaceVariant
import com.just_for_fun.synctax.ui.theme.PlayerTextPrimary
import com.just_for_fun.synctax.ui.theme.PlayerTextSecondary
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel
import com.just_for_fun.synctax.ui.viewmodels.LyricsViewModel
import kotlinx.coroutines.delay
import java.io.File
import kotlin.math.abs
import io.github.fletchmckee.liquid.rememberLiquidState
import io.github.fletchmckee.liquid.liquefiable

// Function to get current lyric line based on position
fun getCurrentLyricLine(lyrics: List<LyricLine>, position: Long): Int {
    if (lyrics.isEmpty()) return -1

    // Find the lyric line that should be displayed at current position
    for (i in lyrics.indices) {
        // Check if the current position is less than the timestamp of the next lyric line
        // This means the current line is the one right before it.
        if (position < lyrics[i].timestamp) {
            return if (i > 0) i - 1 else 0
        }
    }
    // If we passed all timestamps, the last line is the current line
    return lyrics.size - 1
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullScreenPlayerContent(
    song: Song,
    isPlaying: Boolean,
    isBuffering: Boolean,
    position: Long,
    duration: Long,
    shuffleEnabled: Boolean,
    repeatEnabled: Boolean,
    volume: Float,
    upNext: List<Song>,
    playHistory: List<Song>,
    showUpNext: Boolean,
    snackbarHostState: SnackbarHostState,
    onShowUpNextChange: (Boolean) -> Unit,
    onSelectSong: (Song) -> Unit,
    onPlaceNext: (Song) -> Unit,
    onRemoveFromQueue: (Song) -> Unit,
    onReorderQueue: (Int, Int) -> Unit,
    onSetVolume: (Float) -> Unit,
    onPlayPauseClick: () -> Unit,
    onNextClick: () -> Unit,
    onPreviousClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onClose: () -> Unit,
    expansionProgress: Float,
    downloadPercent: Int = 0
) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current


    // Get LyricsViewModel for lyrics functionality
    val lyricsViewModel: LyricsViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()
    val lyricsState by lyricsViewModel.lyricsState.collectAsState()
    val searchResults by lyricsViewModel.searchResults.collectAsState()
    val lyrics by lyricsViewModel.currentLyrics.collectAsState()
    // Get PlayerViewModel for download functionality
    val uiState by playerViewModel.uiState.collectAsState()

    // Smoothly animate download progress changes for the current song
    val liveProgress = uiState.downloadProgress[song.id] ?: 0f
    val animatedProgress by animateFloatAsState(
        targetValue = liveProgress,
        animationSpec = tween(durationMillis = 300)
    )

    // Liquid state for shader effects
    val liquidState = rememberLiquidState()
    // --- Local UI States ---
    var showPlayerMenu by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) } // State to control the lyrics overlay
    var showCancelDownloadDialog by remember { mutableStateOf(false) } // State for download cancellation dialog

    // Load lyrics when song changes
    LaunchedEffect(song.id) {
        lyricsViewModel.loadLyricsForSong(song)
    }

    // Update playerViewModel with current lyrics
    LaunchedEffect(lyrics) {
        val lyricsText = lyrics?.joinToString("\n") { it.text }
        playerViewModel.setCurrentLyrics(lyricsText)
    }

    // Get current lyric index
    val currentLyricIndex by remember(position, lyrics) {
        derivedStateOf {
            lyrics?.let { getCurrentLyricLine(it, position) } ?: -1
        }
    }

    // Gesture Feedback States
    var seekDirection by remember { mutableStateOf<String?>(null) }
    var showPlayPause by remember { mutableStateOf(false) }
    var showVolume by remember { mutableStateOf(false) }
    var overlayVolume by remember { mutableFloatStateOf(volume) }

    // Swipe Gesture State
    var albumArtOffsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 150f

    // Preferences & Guide
    val context = LocalContext.current
    val userPreferences = remember(context) { UserPreferences(context) }
    var showGuide by remember { mutableStateOf(userPreferences.shouldShowGuide(UserPreferences.GUIDE_PLAYER)) }

    // Sync overlay volume
    LaunchedEffect(volume) {
        overlayVolume = volume
    }

    // Auto-hide overlays
    LaunchedEffect(seekDirection) {
        if (seekDirection != null) {
            delay(1500)
            seekDirection = null
        }
    }
    LaunchedEffect(showPlayPause) {
        if (showPlayPause) {
            delay(2000)
            showPlayPause = false
        }
    }
    LaunchedEffect(showVolume) {
        if (showVolume) {
            delay(2000)
            showVolume = false
        }
    }

    // Detect external play/pause changes for overlay feedback
    var previousIsPlaying by remember { mutableStateOf(isPlaying) }
    LaunchedEffect(isPlaying) {
        if (isPlaying != previousIsPlaying) {
            showPlayPause = true
            previousIsPlaying = isPlaying
        }
    }

    // Show download message in snackbar (but not for downloading progress)
    LaunchedEffect(uiState.downloadMessage) {
        uiState.downloadMessage?.let { message ->
            if (!message.startsWith("Downloading")) {
                snackbarHostState.showSnackbar(
                    message = message,
                    duration = SnackbarDuration.Short
                )
            }
            // Clear the message after processing
            playerViewModel.dismissDownloadMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Transparency enables the UnifiedPlayer blurred image to show through
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        bottomBar = {
            // Show download progress bar at bottom when downloading
            if (uiState.downloadingSongs.contains(song.id)) {
                val progress = animatedProgress
                LinearProgressIndicator(
                    progress = progress,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = Color(0xFF4CAF50), // Bright green color
                    trackColor = Color.White.copy(alpha = 0.3f),
                )
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .liquefiable(liquidState)
                .fillMaxSize()
        ) {

            // Main Content Column
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // =====================================================================
                // TOP BAR
                // =====================================================================
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TooltipIconButton(
                        onClick = onClose,
                        tooltipText = "Minimize Player"
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Minimize Player",
                            tint = PlayerIconColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    // Capsule indicating Source
                    Surface(
                        color = Color.Transparent,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            Text(
                                text = if (song.id.startsWith("online:")) "ONLINE" else "LIBRARY",
                                style = MaterialTheme.typography.labelMedium,
                                color = PlayerTextSecondary,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = MaterialTheme.typography.labelSmall.letterSpacing
                            )
                        }
                    }

                    TooltipIconButton(
                        onClick = { showPlayerMenu = true },
                        tooltipText = "Options"
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "Options",
                            tint = PlayerIconColor
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(0.5f))

                // =====================================================================
                // ALBUM ART & GESTURES (Central Box Design Stays the Same)
                // =====================================================================
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .offset(x = albumArtOffsetX.dp)
                        .pointerInput(song.id) {
                            var hasTriggered = false
                            detectHorizontalDragGestures(
                                onDragStart = { hasTriggered = false },
                                onDragEnd = {
                                    if (!hasTriggered && abs(albumArtOffsetX) > swipeThreshold) {
                                        hasTriggered = true
                                        if (albumArtOffsetX > 0) {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onPreviousClick()
                                        } else {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onNextClick()
                                        }
                                    }
                                    albumArtOffsetX = 0f
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    albumArtOffsetX += dragAmount
                                }
                            )
                        }
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .shadow(
                                elevation = 24.dp,
                                shape = RoundedCornerShape(24.dp),
                                spotColor = Color.Black,
                                ambientColor = Color.Black
                            ),
                        shape = RoundedCornerShape(24.dp),
                        color = PlayerSurfaceVariant // Keep this opaque for the card design
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Image Layer
                            if (song.albumArtUri.isNullOrEmpty()) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = PlayerTextSecondary.copy(alpha = 0.5f)
                                    )
                                }
                            } else {
                                val expandedImageScale by animateFloatAsState(
                                    targetValue = 1f + (expansionProgress * 0.08f),
                                    animationSpec = tween(
                                        durationMillis = 350,
                                        easing = FastOutSlowInEasing
                                    ),
                                    label = "expandedImageScale"
                                )

                                AsyncImage(
                                    model = song.albumArtUri,
                                    contentDescription = "Album Art",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .scale(expandedImageScale)
                                )
                            }

                            // Vertical Volume Drag Layer
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectVerticalDragGestures { change, dragAmount ->
                                            change.consume()
                                            val volumeChange = -dragAmount * 0.002f
                                            val newVolume =
                                                (overlayVolume + volumeChange).coerceIn(0.0f, 1.0f)
                                            overlayVolume = newVolume
                                            showVolume = true
                                            onSetVolume(newVolume)
                                        }
                                    }
                            )

                            // Tap Gesture Layer (Seek & Play/Pause & Lyrics Toggle)
                            // MODIFIED: Single tap now toggles lyrics. Double tap handles Play/Pause/Seek.
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Double Tap Left: Rewind
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    showLyrics = true // Single tap opens lyrics
                                                },
                                                onDoubleTap = {
                                                    val target =
                                                        (position - 10_000L).coerceAtLeast(0L)
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    seekDirection = "backward"
                                                    onSeek(target)
                                                }
                                            )
                                        }
                                )
                                // Double Tap Center: Play/Pause
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    showLyrics = true // Single tap opens lyrics
                                                },
                                                onDoubleTap = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    onPlayPauseClick()
                                                }
                                            )
                                        }
                                )
                                // Double Tap Right: Forward
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .pointerInput(Unit) {
                                            detectTapGestures(
                                                onTap = {
                                                    showLyrics = true // Single tap opens lyrics
                                                },
                                                onDoubleTap = {
                                                    val target =
                                                        (position + 10_000L).coerceAtMost(duration)
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    seekDirection = "forward"
                                                    onSeek(target)
                                                }
                                            )
                                        }
                                )
                            }

                            // Feedback Overlays (Seek, Play, Volume)
                            if (seekDirection != null || showPlayPause || showVolume) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    when {
                                        seekDirection != null -> {
                                            Icon(
                                                imageVector = if (seekDirection == "forward") Icons.Default.FastForward else Icons.Default.FastRewind,
                                                contentDescription = null,
                                                modifier = Modifier.size(64.dp),
                                                tint = Color.White
                                            )
                                        }

                                        showPlayPause -> {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                                contentDescription = null,
                                                modifier = Modifier.size(64.dp),
                                                tint = Color.White
                                            )
                                        }

                                        showVolume -> {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Rounded.VolumeUp,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(48.dp),
                                                    tint = Color.White
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text(
                                                    text = "${(overlayVolume * 100).toInt()}%",
                                                    style = MaterialTheme.typography.headlineMedium,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // =====================================================================
                // TRACK INFO
                // =====================================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title and Artist Section (75% width)
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(0.75f)
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PlayerTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = PlayerTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Start
                        )
                    }

                    // Download Button Section (25% width) - Only for online songs
                    if (song.id.startsWith("online:")) {
                        Box(
                            modifier = Modifier.weight(0.25f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            val isDownloaded = uiState.downloadedSongs.contains(song.id)
                            val isDownloading = uiState.downloadingSongs.contains(song.id)
                            val downloadProgress = animatedProgress // Already smoothly animated

                            AnimatedDownloadButton(
                                isDownloading = isDownloading,
                                isDownloaded = isDownloaded,
                                downloadProgress = downloadProgress,
                                onClick = { playerViewModel.startDownloadProcess() }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // =====================================================================
                // SLIDER & TIMES
                // =====================================================================
                WavyPlayerSlider(
                    position = position,
                    duration = duration,
                    onSeek = onSeek
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(position),
                        style = MaterialTheme.typography.labelSmall,
                        color = PlayerTextSecondary
                    )

                    // Optional: Download Progress for Online songs
                    val downloadPercentInt = (animatedProgress.times(100f).toInt()).coerceIn(0, 100)
                    if (downloadPercentInt in 1..99) {
                        Text(
                            text = "Downloading $downloadPercentInt%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Text(
                        text = formatTime(duration),
                        style = MaterialTheme.typography.labelSmall,
                        color = PlayerTextSecondary
                    )
                }

                Spacer(modifier = Modifier.weight(0.5f))

                // =====================================================================
                // CONTROLS
                // =====================================================================
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Shuffle
                    TooltipIconButton(onClick = onShuffleClick, tooltipText = "Shuffle") {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleEnabled) PlayerAccent else PlayerTextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    // Previous
                    TooltipIconButton(onClick = onPreviousClick, tooltipText = "Previous") {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = PlayerTextPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    // Play / Pause (Prominent)
                    FilledIconButton(
                        onClick = onPlayPauseClick,
                        modifier = Modifier.size(72.dp),
                        colors = IconButtonDefaults.filledIconButtonColors(
                            containerColor = PlayerAccent,
                            contentColor = PlayerOnAccent
                        )
                    ) {
                        if (isBuffering) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = PlayerOnAccent,
                                strokeWidth = 3.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }

                    // Next
                    TooltipIconButton(onClick = onNextClick, tooltipText = "Next") {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = PlayerTextPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                    }

                    // Repeat
                    TooltipIconButton(onClick = onRepeatClick, tooltipText = "Repeat") {
                        Icon(
                            imageVector = if (repeatEnabled) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                            contentDescription = "Repeat",
                            tint = if (repeatEnabled) PlayerAccent else PlayerTextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // =====================================================================
                // UP NEXT BUTTON (for both offline and online songs)
                // =====================================================================
                Surface(
                    onClick = { onShowUpNextChange(true) },
                    shape = RoundedCornerShape(12.dp),
                    // Make this slightly transparent too
                    color = PlayerSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlaylistPlay,
                            contentDescription = null,
                            tint = PlayerTextSecondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (song.id.startsWith("online:") || song.id.startsWith("youtube:")) 
                                "Recommended" else "Up Next",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = PlayerTextSecondary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        
                        // Show loading indicator if fetching recommendations
                        if (uiState.isLoadingRecommendations && (song.id.startsWith("online:") || song.id.startsWith("youtube:"))) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = PlayerTextSecondary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowUp,
                                contentDescription = null,
                                tint = PlayerTextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // =====================================================================
            // LYRICS OVERLAY (Using new composable and requested design)
            // =====================================================================
            if (showLyrics) {
                // NOTE: MaterialTheme.colorScheme.primary is used as a placeholder for the
                // song-dependent dominant color. Replace this with your actual color calculation.
                LyricsOverlay(
                    song = song,
                    lyrics = lyrics,
                    currentLyricIndex = currentLyricIndex,
                    songDominantColor = MaterialTheme.colorScheme.primary, // Placeholder for song color
                    onDismiss = {
                        showLyrics = false
                        lyricsViewModel.clearError()
                        lyricsViewModel.clearSearchResults()
                    },
                    onFetchLyrics = { lyricsViewModel.fetchLyricsForSong(song) },
                    onFetchLyricsWithCustomQuery = { customSong, customArtist ->
                        lyricsViewModel.fetchLyricsForSongWithCustomQuery(
                            song,
                            customSong,
                            customArtist
                        )
                    },
                    isFetchingLyrics = lyricsState.isFetching,
                    lyricsError = lyricsState.error,
                    hasFailedFetch = lyricsViewModel.hasFailedToFindLyrics(song.id),
                    searchResults = searchResults,
                    onSelectLyrics = { trackId -> lyricsViewModel.selectLyrics(trackId, song) },
                    hasSearchResults = lyricsState.hasSearchResults,
                    liquidState = liquidState
                )
            }

            // =====================================================================
            // BOTTOM SHEET (Up Next / Recommendations)
            // =====================================================================
            if (showUpNext) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                val isOnlineSong = song.id.startsWith("online:") || song.id.startsWith("youtube:")
                
                ModalBottomSheet(
                    onDismissRequest = { onShowUpNextChange(false) },
                    sheetState = sheetState,
                    // TRANSPARENT SHEET BACKGROUND
                    containerColor = PlayerSurface.copy(alpha = 0.85f),
                    scrimColor = Color.Black.copy(alpha = 0.6f)
                ) {
                    if (isOnlineSong) {
                        // YouTube Recommendations Sheet for online songs
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Recommended for You",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = PlayerTextPrimary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            Text(
                                text = "Based on current song (Genre → Artist → Album → Year)",
                                style = MaterialTheme.typography.bodySmall,
                                color = PlayerTextSecondary,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            if (uiState.isLoadingRecommendations) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = PlayerAccent)
                                }
                            } else if (uiState.upNextRecommendations.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No recommendations available",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PlayerTextSecondary
                                    )
                                }
                            } else {
                                // Display recommendations as a list
                                androidx.compose.foundation.lazy.LazyColumn(
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    items(uiState.upNextRecommendations.size) { index ->
                                        val recommendedSong = uiState.upNextRecommendations[index]
                                        RecommendedSongItem(
                                            song = recommendedSong,
                                            index = index + 1,
                                            onClick = {
                                                playerViewModel.playRecommendedSong(recommendedSong)
                                                onShowUpNextChange(false)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Normal Up Next sheet for offline songs
                        UpNextSheet(
                            upcomingItems = upNext,
                            historyItems = playHistory,
                            onSelect = onSelectSong,
                            onPlaceNext = onPlaceNext,
                            onRemoveFromQueue = onRemoveFromQueue,
                            onReorderQueue = onReorderQueue,
                            snackbarHostState = snackbarHostState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                            color = Color.Transparent
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // Player Menu
        if (showPlayerMenu) {
            SimplePlayerMenu(
                song = song,
                volume = volume,
                onVolumeChange = onSetVolume,
                onDismiss = { showPlayerMenu = false }
            )
        }

        // Guide Overlay
        if (showGuide) {
            GuideOverlay(
                steps = GuideContent.playerScreenGuide,
                onDismiss = {
                    userPreferences.setGuideShown(UserPreferences.GUIDE_PLAYER)
                    showGuide = false
                }
            )
        }

        // Loading indicator for format fetching
        if (uiState.isLoadingFormats) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Fetching best quality") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Starting the download in best available quality...")
                    }
                },
                confirmButton = { }
            )
        }
        
        // Format Selection Dialog
        if (uiState.showFormatDialog) {
            FormatSelectionDialog(
                formats = uiState.availableFormats,
                onFormatSelected = { format ->
                    playerViewModel.downloadWithFormat(format)
                },
                onDismiss = {
                    playerViewModel.dismissFormatDialog()
                },
                currentPoToken = uiState.poTokenData,
                onPoTokenChanged = { token ->
                    playerViewModel.updatePoTokenData(token)
                }
            )
        }
    }
}

// Helper function for time formatting
fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}

// Composable for displaying recommended songs
@Composable
fun RecommendedSongItem(
    song: Song,
    index: Int,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = PlayerSurfaceVariant.copy(alpha = 0.3f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index number
            Text(
                text = "$index",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = PlayerTextSecondary,
                modifier = Modifier.width(32.dp)
            )
            
            // Thumbnail
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(56.dp)
                    .shadow(4.dp, RoundedCornerShape(8.dp))
                    .background(PlayerSurfaceVariant, RoundedCornerShape(8.dp))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Song info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = PlayerTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = PlayerTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            // Play icon
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = "Play",
                tint = PlayerAccent,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}