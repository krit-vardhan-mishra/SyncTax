package com.just_for_fun.synctax.presentation.components.player

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.core.utils.TimeUtils.formatTime
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.data.model.LyricLine
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.components.SnackbarUtils
import com.just_for_fun.synctax.presentation.components.app.TooltipIconButton
import com.just_for_fun.synctax.presentation.components.utils.FormatSelectionDialog
import com.just_for_fun.synctax.presentation.guide.GuideContent
import com.just_for_fun.synctax.presentation.guide.GuideOverlay
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import com.just_for_fun.synctax.presentation.ui.theme.PlayerAccent
import com.just_for_fun.synctax.presentation.ui.theme.PlayerIconColor
import com.just_for_fun.synctax.presentation.ui.theme.PlayerOnAccent
import com.just_for_fun.synctax.presentation.ui.theme.PlayerSurface
import com.just_for_fun.synctax.presentation.ui.theme.PlayerSurfaceVariant
import com.just_for_fun.synctax.presentation.ui.theme.PlayerTextPrimary
import com.just_for_fun.synctax.presentation.ui.theme.PlayerTextSecondary
import com.just_for_fun.synctax.presentation.viewmodels.LyricsViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlayerViewModel
import io.github.fletchmckee.liquid.liquefiable
import io.github.fletchmckee.liquid.rememberLiquidState

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

    // Combine queue for carousel - ensure current song is included
    val carouselQueue = remember(playHistory, song, upNext) {
        (playHistory.reversed() + song + upNext).distinctBy { it.id }
    }

    // ViewModels & state
    val lyricsViewModel: LyricsViewModel = viewModel()
    val playerViewModel: PlayerViewModel = viewModel()
    val lyricsState by lyricsViewModel.lyricsState.collectAsState()
    val searchResults by lyricsViewModel.searchResults.collectAsState()
    val lyrics by lyricsViewModel.currentLyrics.collectAsState()
    val uiState by playerViewModel.uiState.collectAsState()

    val liveProgress = uiState.downloadProgress[song.id] ?: 0f
    val animatedProgress by animateFloatAsState(
        targetValue = liveProgress,
        animationSpec = tween(durationMillis = 300)
    )

    val liquidState = rememberLiquidState()
    var showPlayerMenu by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showCancelDownloadDialog by remember { mutableStateOf(false) }
    var isBlurEnabled by remember { mutableStateOf(true) } // Hoisted state

    // Load lyrics when song changes - first try local, then fetch from API if not found
    LaunchedEffect(song.id) { 
        lyricsViewModel.loadLyricsForSong(song)
    }
    
    // Auto-fetch lyrics from API when local lyrics not found and showLyrics is triggered
    LaunchedEffect(showLyrics, lyrics, song.id) {
        if (showLyrics && lyrics == null && !lyricsState.isFetching && !lyricsViewModel.hasFailedToFindLyrics(song.id)) {
            // No local lyrics found, try fetching from API
            lyricsViewModel.fetchLyricsForSong(song)
        }
    }
    
    LaunchedEffect(lyrics) {
        val lyricsText = lyrics?.joinToString("\n") { it.text }
        playerViewModel.setCurrentLyrics(lyricsText)
    }

    val currentLyricIndex by remember(position, lyrics) {
        derivedStateOf { lyrics?.let { getCurrentLyricLine(it, position) } ?: -1 }
    }

    val context = LocalContext.current
    val userPreferences = remember(context) { UserPreferences(context) }
    var showGuide by remember { mutableStateOf(userPreferences.shouldShowGuide(UserPreferences.GUIDE_PLAYER)) }

    // Show download message in snackbar (but not for downloading progress)
    LaunchedEffect(uiState.downloadMessage) {
        uiState.downloadMessage?.let { message ->
            if (!message.startsWith("Downloading")) {
                SnackbarUtils.showGlobalSnackbar(message = message, duration = SnackbarDuration.Short)
            }
            playerViewModel.dismissDownloadMessage()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        bottomBar = {
            if (uiState.downloadingSongs.contains(song.id)) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = AppColors.downloadProgressBar,
                    trackColor = AppColors.downloadProgressTrack,
                )
            }
        }
    ) { paddingValues ->

        Box(
            modifier = Modifier
                .then(if (isBlurEnabled) Modifier.liquefiable(liquidState) else Modifier)
                .fillMaxSize()
        ) {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TooltipIconButton(onClick = onClose, tooltipText = "Minimize Player") {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Minimize Player",
                            tint = PlayerIconColor,
                            modifier = Modifier.size(32.dp)
                        )
                    }

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

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Lyrics button removed - tap on album art to toggle lyrics
                        
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
                }

                Spacer(modifier = Modifier.weight(0.5f))

                // Album art carousel with gesture controls
                AlbumArtCarousel(
                    queue = carouselQueue,
                    currentSong = song,
                    isPlaying = isPlaying,
                    position = position,
                    duration = duration,
                    volume = volume,
                    expansionProgress = expansionProgress,
                    onSongSelected = onSelectSong,
                    onPlayPauseClick = onPlayPauseClick,
                    onNextClick = onNextClick,
                    onPreviousClick = onPreviousClick,
                    onSeek = onSeek,
                    onSetVolume = onSetVolume,
                    onLyricsClick = { showLyrics = !showLyrics }
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Track info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        horizontalAlignment = Alignment.Start,
                        modifier = Modifier.weight(0.75f)
                    ) {
                        AlwaysScrollMarqueeText(
                            text = song.title,
                            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                            color = PlayerTextPrimary,
                            isPlaying = isPlaying,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        AlwaysScrollMarqueeText(
                            text = song.artist,
                            textStyle = MaterialTheme.typography.titleMedium,
                            color = PlayerTextSecondary,
                            isPlaying = isPlaying, // Or true to always scroll
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (song.id.startsWith("online:")) {
                        Box(
                            modifier = Modifier.weight(0.25f),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            val isDownloaded = uiState.downloadedSongs.contains(song.id)
                            val isDownloading = uiState.downloadingSongs.contains(song.id)

                            AnimatedDownloadButton(
                                isDownloading = isDownloading,
                                isDownloaded = isDownloaded,
                                downloadProgress = animatedProgress,
                                onClick = {
                                    if (isDownloading) {
                                        showCancelDownloadDialog = true
                                    } else {
                                        playerViewModel.startDownloadProcess()
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                WavyPlayerSlider(position = position, duration = duration, onSeek = onSeek)

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

                    val downloadPercentInt = (animatedProgress.times(100f).toInt()).coerceIn(0, 100)
                    if (downloadPercentInt in 1..99 && uiState.downloadingSongs.contains(song.id)) {
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

                // Controls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TooltipIconButton(onClick = onShuffleClick, tooltipText = "Shuffle") {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = if (shuffleEnabled) PlayerAccent else PlayerTextSecondary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    TooltipIconButton(onClick = onPreviousClick, tooltipText = "Previous") {
                        Icon(
                            imageVector = Icons.Rounded.SkipPrevious,
                            contentDescription = "Previous",
                            tint = PlayerTextPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                    }
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
                                contentDescription = null,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    TooltipIconButton(onClick = onNextClick, tooltipText = "Next") {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next",
                            tint = PlayerTextPrimary,
                            modifier = Modifier.size(42.dp)
                        )
                    }
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

                Surface(
                    onClick = { onShowUpNextChange(true) },
                    shape = RoundedCornerShape(12.dp),
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
                            imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                            contentDescription = null,
                            tint = PlayerTextSecondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = if (song.id.startsWith("online:") || song.id.startsWith("youtube:")) "Recommended" else "Up Next",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = PlayerTextSecondary
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if (uiState.isLoadingRecommendations && (song.id.startsWith("online:") || song.id.startsWith(
                                "youtube:"
                            ))
                        ) {
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

            // Lyrics overlay
            if (showLyrics) {
                LyricsOverlay(
                    song = song,
                    lyrics = lyrics,
                    currentLyricIndex = currentLyricIndex,
                    songDominantColor = MaterialTheme.colorScheme.primary,
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

            // Bottom sheet
            if (showUpNext) {
                val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
                val isOnlineSong = song.id.startsWith("online:") || song.id.startsWith("youtube:")

                ModalBottomSheet(
                    onDismissRequest = { onShowUpNextChange(false) },
                    sheetState = sheetState,
                    containerColor = PlayerSurface.copy(alpha = 0.85f),
                    scrimColor = Color.Black.copy(alpha = 0.6f)
                ) {
                    if (isOnlineSong) {
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
                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
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
                        UpNextSheet(
                            upcomingItems = upNext,
                            historyItems = playHistory,
                            onSelect = onSelectSong,
                            onPlaceNext = onPlaceNext,
                            onRemoveFromQueue = onRemoveFromQueue,
                            onReorderQueue = onReorderQueue,
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

        if (showPlayerMenu) {
            SimplePlayerMenu(
                song = song,
                volume = volume,
                onVolumeChange = onSetVolume,
                onDismiss = { showPlayerMenu = false })
        }

        if (showGuide) {
            GuideOverlay(
                steps = GuideContent.playerScreenGuide,
                onDismiss = {
                    userPreferences.setGuideShown(UserPreferences.GUIDE_PLAYER)
                    showGuide = false
                }
            )
        }

        if (uiState.isLoadingFormats) {
            AlertDialog(
                onDismissRequest = { },
                title = { Text("Fetching best quality") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Starting the download in best available quality...")
                    }
                },
                confirmButton = { }
            )
        }

        if (uiState.showFormatDialog) {
            FormatSelectionDialog(
                formats = uiState.availableFormats,
                onFormatSelected = { format -> playerViewModel.downloadWithFormat(format) },
                onDismiss = { playerViewModel.dismissFormatDialog() }
            )
        }

        if (showCancelDownloadDialog) {
            AlertDialog(
                onDismissRequest = { showCancelDownloadDialog = false },
                title = { Text("Cancel Download") },
                text = { Text("Are you sure you want to cancel the download?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            playerViewModel.cancelDownload(song.id)
                            showCancelDownloadDialog = false
                        }
                    ) {
                        Text("Yes", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCancelDownloadDialog = false }) {
                        Text("No")
                    }
                }
            )
        }
    }
}

// Function to get current lyric line based on position
fun getCurrentLyricLine(lyrics: List<LyricLine>, position: Long): Int {
    if (lyrics.isEmpty()) return -1
    for (i in lyrics.indices) {
        if (position < lyrics[i].timestamp) {
            return if (i > 0) i - 1 else 0
        }
    }
    return lyrics.size - 1
}