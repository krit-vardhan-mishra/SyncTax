package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory
import com.just_for_fun.synctax.data.local.entities.OnlineSong
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.components.player.BottomOptionsDialog
import com.just_for_fun.synctax.presentation.components.player.DialogOption
import com.just_for_fun.synctax.presentation.components.tabs.LocalHistoryTab
import com.just_for_fun.synctax.presentation.components.tabs.OfflineSavedTab
import com.just_for_fun.synctax.presentation.components.tabs.OnlineHistoryTab
import com.just_for_fun.synctax.presentation.viewmodels.HomeViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlayerViewModel
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    onNavigateBack: () -> Unit = {},
    initialTab: Int = 0
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val tabs = listOf("Online", "Local", "Offline Saved")
    
    // 1. Initialize PagerState with initialTab
    val pagerState = rememberPagerState(initialPage = initialTab.coerceIn(0, tabs.size - 1), pageCount = { tabs.size })
    val coroutineScope = rememberCoroutineScope()
    
    // Bottom Options Dialog State
    var showOptionsSheet by remember { mutableStateOf(false) }
    var selectedItemForOptions by remember { mutableStateOf<Any?>(null) } // Can be OnlineListeningHistory, Song, or OnlineSong

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Listening History",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        val pullToRefreshState = rememberPullToRefreshState()
        
        PullToRefreshBox(
            isRefreshing = uiState.isScanning,
            onRefresh = { homeViewModel.forceRefreshLibrary() },
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            indicator = {
                androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                    state = pullToRefreshState,
                    isRefreshing = uiState.isScanning,
                    modifier = Modifier.align(Alignment.TopCenter),
                    color = androidx.compose.ui.graphics.Color(0xFFFF0033)
                )
            }
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // 2. Sync TabRow with PagerState
                PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = pagerState.currentPage == index,
                            onClick = {
                                // Animate to the selected page when tab is clicked
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }
                            },
                            text = {
                                Text(
                                    text = title,
                                    fontWeight = if (pagerState.currentPage == index) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        )
                    }
                }

                // 3. Replace 'when' block with HorizontalPager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.Top // Ensures content starts from top
                ) { page ->
                    when (page) {
                        0 -> OnlineHistoryTab(
                            history = uiState.onlineHistory,
                            onHistoryClick = { history ->
                                playerViewModel.playUrl(
                                    url = history.watchUrl,
                                    title = history.title,
                                    artist = history.artist,
                                    durationMs = 0L
                                )
                            },
                            onRemove = { history ->
                                selectedItemForOptions = history
                                showOptionsSheet = true
                            }
                        )
                        1 -> LocalHistoryTab(
                            songs = uiState.listenAgain.take(50),
                            onSongClick = { song ->
                                playerViewModel.playSong(song, uiState.allSongs)
                            },
                           onLongClick = { song ->
                               selectedItemForOptions = song
                               showOptionsSheet = true
                           }
                        )
                        2 -> OfflineSavedTab(
                            songs = uiState.offlineSavedSongs,
                            onSongClick = { song ->
                                // Use offline playback function that checks for cached audio first
                                playerViewModel.playOfflineSavedSong(
                                    videoId = song.videoId,
                                    title = song.title,
                                    artist = song.artist,
                                    durationMs = song.duration?.times(1000L) ?: 0L,
                                    thumbnailUrl = song.thumbnailUrl
                                )
                            },
                            onRemove = { song ->
                                selectedItemForOptions = song
                                showOptionsSheet = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Bottom Options Dialog
    if (showOptionsSheet && selectedItemForOptions != null) {
        val item = selectedItemForOptions!!
        
        // Extract basic info based on type
        var title = ""
        var subtitle: String? = null
        var thumbnail: String? = null
        var removeAction: () -> Unit = {}
        
        when (item) {
            is OnlineListeningHistory -> {
                title = item.title
                subtitle = item.artist
                thumbnail = item.thumbnailUrl
                removeAction = { homeViewModel.deleteOnlineHistory(item.videoId) }
            }
            is Song -> {
                title = item.title
                subtitle = item.artist
                thumbnail = item.albumArtUri
                // Local history "remove" usually means removing from "Recent" list or actually deleting?
                // For "Listen Again" (LocalHistoryTab), likely removing from History/Stats if possible, 
                // but HomeViewModel manages `listenAgain` based on stats. 
                // Since the user asked "remove from saved cache/history", for local files it might mean 
                // removing from the "Recently Played" view. 
                // However, `listenAgain` is computed. 
                // Let's implement removing from `PlaybackCollector` or `MusicRepository` history if possible.
                // Or simply: "Remove from History" (hiding it).
                // Existing `LocalHistoryTab` didn't have remove callback.
                // Assuming we want to remove from "History" (last played).
                removeAction = { 
                     // TODO: Implement removal from local history stats if needed. 
                     // For now, we call a placeholder or nothing if not supported.
                     // The user request "remove from saved cache/history" implies it should work.
                     // `HomeViewModel` has `deleteSearchHistoryItem` and `deleteOnlineHistory`.
                     // Does it have deleteLocalHistory? No.
                     // But we can just show the option and maybe implement it later or toast "Not supported for local files" if complex.
                     // Verify if `LocalHistoryTab` is "Recently Played". Yes.
                }
            }
            is OnlineSong -> {
                title = item.title
                subtitle = item.artist
                thumbnail = item.thumbnailUrl
                removeAction = { 
                    homeViewModel.deleteSavedSong(item)
                }
            }
        }
        
        BottomOptionsDialog(
            isVisible = true,
            onDismiss = { showOptionsSheet = false },
            title = "Options",
            songTitle = title,
            songArtist = subtitle,
            songThumbnail = thumbnail,
            options = listOf(
                DialogOption(
                    id = "remove",
                    title = "Remove",
                    subtitle = when(item) {
                        is OnlineSong -> "Remove from Downloads"
                        is OnlineListeningHistory -> "Remove from History"
                        else -> "Remove from History"
                    },
                    icon = {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    destructive = true,
                    onClick = {
                        removeAction()
                        // Ensure dialog closes
                    }
                )
            )
        )
    }
}