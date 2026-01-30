package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.presentation.components.tabs.LocalHistoryTab
import com.just_for_fun.synctax.presentation.components.tabs.OfflineSavedTab
import com.just_for_fun.synctax.presentation.components.tabs.OnlineHistoryTab
import com.just_for_fun.synctax.presentation.viewmodels.HomeViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlayerViewModel
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
                                homeViewModel.deleteOnlineHistory(history.videoId)
                            }
                        )
                        1 -> LocalHistoryTab(
                            songs = uiState.listenAgain.take(50),
                            onSongClick = { song ->
                                playerViewModel.playSong(song, uiState.allSongs)
                            }
                        )
                        2 -> OfflineSavedTab(
                            songs = uiState.offlineSavedSongs,
                            onSongClick = { song ->
                                playerViewModel.playUrl(
                                    url = "https://music.youtube.com/watch?v=${song.videoId}",
                                    title = song.title,
                                    artist = song.artist,
                                    durationMs = song.duration?.times(1000L) ?: 0L,
                                    thumbnailUrl = song.thumbnailUrl
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}