package com.just_for_fun.synctax.ui.components.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
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
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.components.app.TooltipIconButton
import com.just_for_fun.synctax.ui.guide.GuideContent
import com.just_for_fun.synctax.ui.guide.GuideOverlay
import com.just_for_fun.synctax.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.max
import java.io.File
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.compositeOver

// Data class for LRC lyrics
data class LyricLine(
    val timestamp: Long, // in milliseconds
    val text: String
)

// Function to parse LRC file content
fun parseLrcContent(content: String): List<LyricLine> {
    val lines = mutableListOf<LyricLine>()
    val regex = Regex("\\[(\\d{2}):(\\d{2})\\.(\\d{2})\\](.+)")

    content.lines().forEach { line ->
        val match = regex.find(line.trim())
        if (match != null) {
            val minutes = match.groupValues[1].toInt()
            val seconds = match.groupValues[2].toInt()
            val centiseconds = match.groupValues[3].toInt()
            val text = match.groupValues[4].trim()

            val totalMillis = (minutes * 60 * 1000) + (seconds * 1000) + (centiseconds * 10)
            lines.add(LyricLine(totalMillis.toLong(), text))
        }
    }

    return lines.sortedBy { it.timestamp }
}

// Function to load lyrics for a song
fun loadLyricsForSong(song: Song): List<LyricLine>? {
    return try {
        // Try to find LRC file with the same name as the audio file
        val audioFile = File(song.filePath)
        val lrcFile = File(audioFile.parent, audioFile.nameWithoutExtension + ".lrc")

        if (lrcFile.exists()) {
            val content = lrcFile.readText()
            parseLrcContent(content)
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}

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

    // --- Local UI States ---
    var showPlayerMenu by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) } // State to control the lyrics overlay

    // Load lyrics for current song
    val lyrics by remember(song.id) {
        derivedStateOf { loadLyricsForSong(song) }
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

    // Get PlayerViewModel for download functionality
    val playerViewModel: PlayerViewModel = viewModel()
    val uiState by playerViewModel.uiState.collectAsState()

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

    // Show download message in snackbar
    LaunchedEffect(uiState.downloadMessage) {
        uiState.downloadMessage?.let { message ->
            snackbarHostState.showSnackbar(
                message = message,
                duration = SnackbarDuration.Long
            )
            // Clear the message after showing
            playerViewModel.dismissDownloadMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // Transparency enables the UnifiedPlayer blurred image to show through
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->

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

                TooltipIconButton(onClick = { showPlayerMenu = true }, tooltipText = "Options") {
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
                                                val target = (position - 10_000L).coerceAtLeast(0L)
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
                        val downloadProgress = uiState.downloadProgress[song.id] ?: 0f

                        AnimatedDownloadButton(
                            isDownloading = isDownloading,
                            isDownloaded = isDownloaded,
                            downloadProgress = downloadProgress,
                            onClick = { playerViewModel.downloadCurrentSong() }
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
                if (downloadPercent in 1..99) {
                    Text(
                        text = "Downloading $downloadPercent%",
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
            // UP NEXT BUTTON
            // =====================================================================
            if (!song.id.startsWith("online:")) {
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
                            text = "Up Next",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = PlayerTextSecondary
                        )
                        Spacer(modifier = Modifier.weight(1f))
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
                onDismiss = { showLyrics = false }
            )
        }

        // =====================================================================
        // BOTTOM SHEET (Up Next)
        // =====================================================================
        if (showUpNext && !song.id.startsWith("online:")) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
            ModalBottomSheet(
                onDismissRequest = { onShowUpNextChange(false) },
                sheetState = sheetState,
                // TRANSPARENT SHEET BACKGROUND
                containerColor = PlayerSurface.copy(alpha = 0.85f),
                scrimColor = Color.Black.copy(alpha = 0.6f)
            ) {
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
}

// Helper function for time formatting
private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}