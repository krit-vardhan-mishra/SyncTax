package com.just_for_fun.synctax.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.components.SongCard
import com.just_for_fun.synctax.ui.components.QuickPickCard
import com.just_for_fun.synctax.ui.components.UserProfileDialog
import com.just_for_fun.synctax.ui.components.UserProfileIcon
import com.just_for_fun.synctax.ui.viewmodels.HomeViewModel
import com.just_for_fun.synctax.ui.viewmodels.PlayerViewModel
import com.just_for_fun.synctax.ui.screens.SortOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    homeViewModel: HomeViewModel = viewModel(),
    playerViewModel: PlayerViewModel = viewModel(),
    userPreferences: UserPreferences,
    onSearchClick: () -> Unit = {},
    onTrainClick: () -> Unit = {},
    onOpenSettings: () -> Unit = {}
) {
    val uiState by homeViewModel.uiState.collectAsState()
    val playerState by playerViewModel.uiState.collectAsState()
    val userName by userPreferences.userName.collectAsState()
    val userInitial = userPreferences.getUserInitial()
    
    // State for profile dialog and profile menu
    var showProfileDialog by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }

    // Sorting state for All Songs section
    var currentSortOption by remember { mutableStateOf(SortOption.TITLE_ASC) }

    // Sort songs based on current sort option
    val sortedSongs = remember(uiState.allSongs, currentSortOption) {
        when (currentSortOption) {
            SortOption.TITLE_ASC -> uiState.allSongs.sortedBy { it.title.lowercase() }
            SortOption.TITLE_DESC -> uiState.allSongs.sortedByDescending { it.title.lowercase() }
            SortOption.ARTIST_ASC -> uiState.allSongs.sortedBy { it.artist.lowercase() }
            SortOption.ARTIST_DESC -> uiState.allSongs.sortedByDescending { it.artist.lowercase() }
            SortOption.RELEASE_YEAR_DESC -> uiState.allSongs.sortedByDescending { it.releaseYear ?: 0 }
            SortOption.RELEASE_YEAR_ASC -> uiState.allSongs.sortedBy { it.releaseYear ?: 0 }
            SortOption.ADDED_TIMESTAMP_DESC -> uiState.allSongs.sortedByDescending { it.addedTimestamp }
            SortOption.ADDED_TIMESTAMP_ASC -> uiState.allSongs.sortedBy { it.addedTimestamp }
            SortOption.DURATION_DESC -> uiState.allSongs.sortedByDescending { it.duration }
            SortOption.DURATION_ASC -> uiState.allSongs.sortedBy { it.duration }
            SortOption.NAME_ASC -> uiState.allSongs.sortedBy { it.title.lowercase() }
            SortOption.NAME_DESC -> uiState.allSongs.sortedByDescending { it.title.lowercase() }
            SortOption.ARTIST -> uiState.allSongs.sortedBy { it.artist.lowercase() }
            SortOption.DATE_ADDED_OLDEST -> uiState.allSongs.sortedBy { it.addedTimestamp }
            SortOption.DATE_ADDED_NEWEST -> uiState.allSongs.sortedByDescending { it.addedTimestamp }
            SortOption.CUSTOM -> uiState.allSongs // No sorting for custom
        }
    }

    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Music",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                },
                actions = {
                    // Shuffle All Songs
                    IconButton(onClick = { 
                        if (uiState.allSongs.isNotEmpty()) {
                            playerViewModel.shufflePlay(uiState.allSongs)
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle All",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    // Refresh library (re-scan device)
                    IconButton(onClick = { homeViewModel.scanMusic() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Library",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Search Icon
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Profile Icon with dropdown menu (like YouTube Music)
                    Box {
                        IconButton(onClick = { showProfileMenu = true }) {
                            UserProfileIcon(userInitial = userInitial)
                        }
                        DropdownMenu(
                            expanded = showProfileMenu,
                            onDismissRequest = { showProfileMenu = false }
                        ) {
                            // Header with profile icon and full name
                            Column(
                                modifier = Modifier
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                horizontalAlignment = Alignment.Start
                            ) {
                                UserProfileIcon(userInitial = userInitial)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = userName.ifEmpty { "User" },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Divider()
                            // Train model option
                            DropdownMenuItem(
                                text = { Text("Train model") },
                                onClick = {
                                    showProfileMenu = false
                                    onTrainClick()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Build,
                                        contentDescription = null
                                    )
                                }
                            )
                            // App settings option
                            DropdownMenuItem(
                                text = { Text("App settings") },
                                onClick = {
                                    showProfileMenu = false
                                    onOpenSettings()
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = null
                                    )
                                }
                            )
                            // Change user name option
                            DropdownMenuItem(
                                text = { Text("Change name") },
                                onClick = {
                                    showProfileMenu = false
                                    showProfileDialog = true
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null
                                    )
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                uiState.allSongs.isEmpty() -> {
                    EmptyMusicState(
                        onScanClick = { homeViewModel.scanMusic() },
                        isScanning = uiState.isScanning,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                else -> {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn(animationSpec = tween(600)) +
                                slideInVertically(animationSpec = tween(600)) { it / 4 }
                    ) {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(0.dp)
                        ) {

                            // Greeting Section
                            if (userName.isNotEmpty()) {
                                item {
                                    GreetingSection(userName = userName)
                                }
                            }
                           
                            // Filter Chips
                            item {
                                FilterChipsRow()
                            }


                            // Quick Picks Section (always visible; if no picks yet, shows learning message)
                            item {
                                QuickPicksSection(
                                    songs = uiState.quickPicks,
                                    onSongClick = { song ->
                                        playerViewModel.playSong(song, uiState.quickPicks)
                                    },
                                    onRefreshClick = { homeViewModel.generateQuickPicks() },
                                    onViewAllClick = { /* Navigate to Quick Picks */ },
                                    isGenerating = uiState.isGeneratingRecommendations,
                                    onPlayAll = {
                                        uiState.quickPicks.firstOrNull()?.let { firstSong ->
                                            playerViewModel.playSong(
                                                firstSong,
                                                uiState.quickPicks
                                            )
                                        }
                                    }
                                )
                            }

                            // All Songs Section
                            @OptIn(ExperimentalFoundationApi::class)
                            item {
                                SectionHeader(
                                    title = "Listen again",
                                    subtitle = null,
                                    onViewAllClick = null
                                )

                                val songsPerPage = 4
                                // Shuffle the songs to show random ones each time
                                val shuffledSongs = remember(uiState.allSongs) {
                                    uiState.allSongs.shuffled()
                                }
                                val pages = shuffledSongs.chunked(songsPerPage)

                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 16.dp)
                                ) {
                                    items(pages.size) { pageIndex ->
                                        Column(
                                            modifier = Modifier
                                                .fillParentMaxWidth()
                                                .padding(vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            pages[pageIndex].forEach { song ->
                                                SongCard(
                                                    song = song,
                                                    onClick = {
                                                        playerViewModel.playSong(
                                                            song,
                                                            uiState.allSongs
                                                        )
                                                    },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .animateItem()
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Speed Dial Section
                            item {
                                SpeedDialSection(
                                    songs = uiState.allSongs,
                                    onSongClick = { song ->
                                        playerViewModel.playSong(song, uiState.allSongs)
                                    },
                                    userInitial = userInitial
                                )
                            }

                             // All Songs Section
                            item {
                                SectionHeader(
                                    title = "All Songs",
                                    subtitle = null,
                                    onViewAllClick = null,
                                    showSortButton = true,
                                    currentSortOption = currentSortOption,
                                    onSortOptionChange = { currentSortOption = it }
                                )
                            }

                            items(sortedSongs, key = { it.id }) { song ->
                                AnimatedVisibility(
                                    visible = true,
                                    enter = fadeIn(animationSpec = tween(400)) +
                                            expandVertically(animationSpec = tween(400))
                                ) {
                                    SongCard(
                                        song = song,
                                        onClick = {
                                            playerViewModel.playSong(song, uiState.allSongs)
                                        }
                                    )
                                }
                            }

                            // Bottom padding for mini player
                            item {
                                Spacer(modifier = Modifier.height(96.dp))
                            }
                        }
                    }
                }
            }

            // Snackbar for errors
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { homeViewModel.dismissError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            // Training indicator
            AnimatedVisibility(
                visible = uiState.isTraining,
                enter = fadeIn() + slideInVertically { it },
                exit = fadeOut() + slideOutVertically { it },
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Card(
                    modifier = Modifier.padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Training ML models...",
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        
        // User Profile Dialog
        if (showProfileDialog) {
            UserProfileDialog(
                currentUserName = userName,
                onDismiss = { showProfileDialog = false },
                onNameUpdate = { newName ->
                    userPreferences.saveUserName(newName)
                }
            )
        }
    }
}

@Composable
fun GreetingSection(userName: String) {

    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)

        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..22 -> "Good evening"
            else -> "Hey there, burning the midnight oil?"
        }
    }

    val subGreeting = remember {
        when (greeting) {
            "Good morning" -> "Hope your day starts great!"
            "Good afternoon" -> "Keep going strong!"
            "Good evening" -> "Hope you had a good day so far!"
            else -> "Don't forget to rest when you can."
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        val annotatedString = buildAnnotatedString {
            withStyle(
            style = SpanStyle(
                fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            ) {
            append("$greeting, ")
            }
            withStyle(
            style = SpanStyle(
                fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            ) {
            append(userName)
            }
        }

        Text(
            text = annotatedString,
            style = MaterialTheme.typography.headlineMedium // Base style, but overridden by spans
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = subGreeting,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}


@Composable
fun FilterChipsRow() {
    val chips = listOf("All", "Podcasts", "Romance", "Feel good", "Relax", "Energy")
    var selectedChip by remember { mutableStateOf(chips[0]) }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(chips) { chip ->
            FilterChip(
                selected = chip == selectedChip,
                onClick = { selectedChip = chip },
                label = { Text(chip) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    }
}

@Composable
fun QuickPicksSection(
    songs: List<com.just_for_fun.synctax.core.data.local.entities.Song>,
    onSongClick: (com.just_for_fun.synctax.core.data.local.entities.Song) -> Unit,
    onRefreshClick: () -> Unit,
    onViewAllClick: () -> Unit,
    isGenerating: Boolean,
    onPlayAll: () -> Unit = {}
) {
    Column(
        modifier = Modifier.padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Quick Picks",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {

                // Play All Button
                TextButton(onClick = onPlayAll) {
                    Text(
                        "Play all",
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        if (songs.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isGenerating) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                }
                Text(
                    text = "The app is analyzing your listening habits to find the best songs for you. Listen to songs and we'll find your match.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Text(
                    text = "Listen to your favorite songs and the app will learn your taste",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                items(songs) { song ->
                    QuickPickCard(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedDialSection(
    songs: List<com.just_for_fun.synctax.core.data.local.entities.Song>,
    onSongClick: (com.just_for_fun.synctax.core.data.local.entities.Song) -> Unit,
    userInitial: String = "M"
) {
    Column(
        modifier = Modifier.padding(vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            UserProfileIcon(
                userInitial = userInitial,
                useGradient = true
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "Speed dial",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(songs.take(9)) { song ->
                QuickPickCard(
                    song = song,
                    onClick = { onSongClick(song) }
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onViewAllClick: (() -> Unit)? = null,
    showSortButton: Boolean = false,
    currentSortOption: SortOption = SortOption.TITLE_ASC,
    onSortOptionChange: ((SortOption) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSortButton && onSortOptionChange != null) {
                var showSortMenu by remember { mutableStateOf(false) }

                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort songs",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Name (A-Z)") },
                        onClick = {
                            onSortOptionChange(SortOption.NAME_ASC)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (currentSortOption == SortOption.NAME_ASC) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Name (Z-A)") },
                        onClick = {
                            onSortOptionChange(SortOption.NAME_DESC)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (currentSortOption == SortOption.NAME_DESC) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Artist") },
                        onClick = {
                            onSortOptionChange(SortOption.ARTIST)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (currentSortOption == SortOption.ARTIST) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Date Added (Oldest)") },
                        onClick = {
                            onSortOptionChange(SortOption.DATE_ADDED_OLDEST)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (currentSortOption == SortOption.DATE_ADDED_OLDEST) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Date Added (Newest)") },
                        onClick = {
                            onSortOptionChange(SortOption.DATE_ADDED_NEWEST)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (currentSortOption == SortOption.DATE_ADDED_NEWEST) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Custom") },
                        onClick = {
                            onSortOptionChange(SortOption.CUSTOM)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (currentSortOption == SortOption.CUSTOM) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }

            onViewAllClick?.let {
                TextButton(onClick = it) {
                    Text("View all", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
fun EmptyMusicState(
    onScanClick: () -> Unit,
    isScanning: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            imageVector = Icons.Default.MusicNote,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Text(
            text = "No Music Found",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Scan your device to find music files",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Button(
            onClick = onScanClick,
            enabled = !isScanning
        ) {
            if (isScanning) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text(if (isScanning) "Scanning..." else "Scan Device")
        }
    }
}