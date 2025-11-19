package com.just_for_fun.synctax.ui.components.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
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
import coil.compose.AsyncImage
import com.just_for_fun.synctax.core.data.local.entities.Song
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.guide.GuideContent
import com.just_for_fun.synctax.ui.guide.GuideOverlay
import com.just_for_fun.synctax.ui.components.app.TooltipIconButton
import kotlinx.coroutines.delay
import kotlin.math.abs

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

    // Player menu state
    var showPlayerMenu by remember { mutableStateOf(false) }

    // Local overlay states
    var seekDirection by remember { mutableStateOf<String?>(null) }
    var showPlayPause by remember { mutableStateOf(false) }
    var showVolume by remember { mutableStateOf(false) }
    var overlayVolume by remember { mutableFloatStateOf(volume) }

    // Swipe gesture state - LOCAL to this composable
    var albumArtOffsetX by remember { mutableFloatStateOf(0f) }
    val swipeThreshold = 150f

    // Guide Overlay
    val context = LocalContext.current
    val userPreferences =
        remember(context) { UserPreferences(context) }
    var showGuide by remember { mutableStateOf(userPreferences.shouldShowGuide(UserPreferences.GUIDE_PLAYER)) }


    // Sync overlay volume with actual volume
    LaunchedEffect(volume) {
        overlayVolume = volume
    }

    // Hide overlays after delay
    LaunchedEffect(seekDirection) {
        if (seekDirection != null) {
            delay(1500)
            seekDirection = null
        }
    }

    LaunchedEffect(showPlayPause) {
        if (showPlayPause) {
            delay(3000)
            showPlayPause = false
        }
    }

    LaunchedEffect(showVolume) {
        if (showVolume) {
            delay(3000)
            showVolume = false
        }
    }

    // Detect play/pause changes
    var previousIsPlaying by remember { mutableStateOf(isPlaying) }
    LaunchedEffect(isPlaying) {
        if (isPlaying != previousIsPlaying) {
            showPlayPause = true
            previousIsPlaying = isPlaying
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TooltipIconButton(
                    onClick = onClose,
                    tooltipText = "Minimize player"
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Close Player",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = if (song.id.startsWith("online:")) "ONLINE MUSIC" else "OFFLINE MUSIC",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.weight(1f))
                TooltipIconButton(
                    onClick = { showPlayerMenu = true },
                    tooltipText = "Song details & options"
                ) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = "Player Menu",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // FIXED: Album art container with proper gesture hierarchy
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .offset(x = albumArtOffsetX.dp)
                    // HORIZONTAL SWIPE - outermost layer
                    .pointerInput(song.id) {
                        var hasTriggered = false
                        detectHorizontalDragGestures(
                            onDragStart = {
                                hasTriggered = false
                            },
                            onDragEnd = {
                                if (!hasTriggered && abs(albumArtOffsetX) > swipeThreshold) {
                                    hasTriggered = true
                                    when {
                                        albumArtOffsetX > 0 -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onPreviousClick()
                                        }

                                        albumArtOffsetX < 0 -> {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onNextClick()
                                        }
                                    }
                                }
                                albumArtOffsetX = 0f
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                // Don't consume here - let inner gestures handle their events
                                albumArtOffsetX += dragAmount
                            }
                        )
                    }
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Album Art Surface with SEPARATE gesture zones
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f)
                        // --- BORDER REMOVED ---
                        // The 'border = BorderStroke(...)' line was removed from here
                        // to create a softer, submerged look.
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Album Art Image
                            if (song.albumArtUri.isNullOrEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .wrapContentSize(Alignment.Center)
                                        .size(120.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
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

                            // SEPARATE LAYER: Volume control (vertical drag)
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(volume) {
                                        detectVerticalDragGestures { change, dragAmount ->
                                            change.consume() // Consume to prevent horizontal swipe
                                            val volumeChange = -dragAmount * 0.002f
                                            val newVolume =
                                                (overlayVolume + volumeChange).coerceIn(
                                                    0.0f,
                                                    1.0f
                                                )
                                            overlayVolume = newVolume
                                            showVolume = true
                                            onSetVolume(newVolume)
                                        }
                                    }
                            )

                            // SEPARATE LAYER: Tap zones for seek and play/pause
                            Row(modifier = Modifier.fillMaxSize()) {
                                // Left: Rewind (double-tap)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .pointerInput(position, duration) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    val target =
                                                        (position - 10_000L).coerceAtLeast(0L)
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    seekDirection = "backward"
                                                    onSeek(target)
                                                }
                                            )
                                        }
                                )

                                // Middle: Play/Pause (double-tap)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .pointerInput(isPlaying) {
                                            detectTapGestures(
                                                onDoubleTap = {
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    onPlayPauseClick()
                                                }
                                            )
                                        }
                                )

                                // Right: Forward (single tap)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .pointerInput(position, duration) {
                                            detectTapGestures(
                                                onTap = {
                                                    val target =
                                                        (position + 10_000L).coerceAtMost(
                                                            duration
                                                        )
                                                    haptic.performHapticFeedback(
                                                        HapticFeedbackType.LongPress
                                                    )
                                                    seekDirection = "forward"
                                                    onSeek(target)
                                                }
                                            )
                                        }
                                )
                            }

                            // Overlays
                            if (seekDirection != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (seekDirection == "forward") Icons.Default.Forward10 else Icons.Default.Replay10,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = Color.White
                                    )
                                }
                            }

                            if (showPlayPause) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = if (isPlaying) "Paused" else "Playing",
                                        modifier = Modifier.size(80.dp),
                                        tint = Color.White
                                    )
                                }
                            }

                            if (showVolume) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.VolumeUp,
                                            contentDescription = "Volume",
                                            modifier = Modifier.size(60.dp),
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "${(overlayVolume * 100).toInt()}%",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Song Info Container
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = Color.Transparent,
                        tonalElevation = 2.dp
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Wavy Slider
            WavyPlayerSlider(
                position = position,
                duration = duration,
                onSeek = onSeek
            )

            // Download progress for streaming online songs
            if (downloadPercent > 0 && downloadPercent < 100) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Downloading: $downloadPercent%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Playback Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TooltipIconButton(
                    onClick = onShuffleClick,
                    tooltipText = if (shuffleEnabled) "Shuffle is ON" else "Shuffle is OFF"
                ) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        modifier = Modifier.size(28.dp),
                        tint = if (shuffleEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                TooltipIconButton(
                    onClick = onPreviousClick,
                    tooltipText = "Previous song"
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipPrevious,
                        contentDescription = "Previous",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                FilledIconButton(
                    onClick = onPlayPauseClick,
                    modifier = Modifier.size(72.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.onBackground,
                        contentColor = MaterialTheme.colorScheme.background
                    )
                ) {
                    if (isBuffering) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = MaterialTheme.colorScheme.background,
                            strokeWidth = 3.dp
                        )
                    } else {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                TooltipIconButton(
                    onClick = onNextClick,
                    tooltipText = "Next song"
                ) {
                    Icon(
                        imageVector = Icons.Default.SkipNext,
                        contentDescription = "Next",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }

                TooltipIconButton(
                    onClick = onRepeatClick,
                    tooltipText = if (repeatEnabled) "Repeat is ON" else "Repeat is OFF"
                ) {
                    Icon(
                        imageVector = if (repeatEnabled) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        contentDescription = "Repeat",
                        modifier = Modifier.size(28.dp),
                        tint = if (repeatEnabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Up Next Button - only show for offline songs
            if (!song.id.startsWith("online:")) {
                TextButton(onClick = { onShowUpNextChange(true) }) {
                    Icon(
                        imageVector = Icons.Default.PlaylistPlay,
                        contentDescription = "Playlist",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "UP NEXT",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            // Up Next Bottom Sheet - only show for offline songs
            if (showUpNext && !song.id.startsWith("online:")) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                ModalBottomSheet(
                    onDismissRequest = { onShowUpNextChange(false) },
                    sheetState = sheetState,
                    // --- LIQUID GLASS CHANGE ---
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f)
                    // --- END LIQUID GLASS CHANGE ---
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
                        shape = MaterialTheme.shapes.medium,
                        color = Color.Transparent
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(48.dp))

    // Player Menu
    if (showPlayerMenu) {
        SimplePlayerMenu(
            song = song,
            volume = volume,
            onVolumeChange = onSetVolume,
            onDismiss = { }
        )
    }

    if (showGuide) {
        GuideOverlay(
            steps = GuideContent.playerScreenGuide,
            onDismiss = {
                userPreferences.setGuideShown(UserPreferences.GUIDE_PLAYER)
            }
        )
    }
}