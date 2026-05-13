package com.just_for_fun.synctax.presentation.screens

import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.components.card.SimpleSongCard
import com.just_for_fun.synctax.presentation.components.player.PartyPlayer
import com.just_for_fun.synctax.presentation.components.utils.BottomPaddingSpacer
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import com.just_for_fun.synctax.presentation.viewmodels.HomeViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PlayerViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PartyViewModel
import com.just_for_fun.synctax.presentation.viewmodels.PartyUiEvent
import android.os.SystemClock

private const val TAG = "PartySessionScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartySessionScreen(
    partyViewModel: PartyViewModel,
    homeViewModel: HomeViewModel,
    playerViewModel: PlayerViewModel,
    song: Song?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    onNavigateBack: () -> Unit,
    onEndPartyClick: () -> Unit,
    onOpenSearch: () -> Unit,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    isAdmin: Boolean = true
) {
    val lavenderColor = AppColors.textTitle
    val accentOrange = AppColors.accentPrimary
    val darkBackground = AppColors.mainBackground
    val context = LocalContext.current
    val userPreferences = remember { UserPreferences(context) }

    val members by partyViewModel.members.collectAsState()
    val isHosting by partyViewModel.isHosting.collectAsState()
    val clientIssues by partyViewModel.clientIssues.collectAsState()
    val homeState by homeViewModel.uiState.collectAsState()

    var showLibrarySheet by remember { mutableStateOf(false) }
    var songQuery by remember { mutableStateOf("") }
    val librarySheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val offlineSongs = homeState.allSongs
    val filteredSongs = remember(songQuery, offlineSongs) {
        if (songQuery.isBlank()) {
            offlineSongs
        } else {
            val query = songQuery.trim().lowercase()
            offlineSongs.filter { song ->
                song.title.lowercase().contains(query) ||
                    (song.artist ?: "").lowercase().contains(query) ||
                    (song.album ?: "").lowercase().contains(query)
            }
        }
    }

    // Host transfer request dialog state
    var showTransferDialog by remember { mutableStateOf(false) }
    var transferRequestEndpoint by remember { mutableStateOf("") }
    var transferRequestUserName by remember { mutableStateOf("") }

    // Collect host transfer request events
    LaunchedEffect(Unit) {
        partyViewModel.uiEvents.collect { event ->
            if (event is PartyUiEvent.HostTransferRequested) {
                Log.d(TAG, "👑 Showing host transfer dialog for '${event.userName}'")
                transferRequestEndpoint = event.endpointId
                transferRequestUserName = event.userName
                showTransferDialog = true
            }
        }
    }

    // Host transfer request dialog (shown to host)
    if (showTransferDialog && isHosting) {
        AlertDialog(
            onDismissRequest = { showTransferDialog = false },
            containerColor = AppColors.cardBackground,
            title = {
                Text("Host Transfer Request", color = lavenderColor, fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    "$transferRequestUserName wants to become the host. Do you want to transfer host controls?",
                    color = AppColors.textBody
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        partyViewModel.respondToHostTransfer(transferRequestEndpoint, true)
                        showTransferDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentOrange)
                ) {
                    Text("Accept", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    partyViewModel.respondToHostTransfer(transferRequestEndpoint, false)
                    showTransferDialog = false
                }) {
                    Text("Deny", color = AppColors.textBody)
                }
            }
        )
    }

    PartyPlayer(
        song = song,
        isPlaying = isPlaying,
        position = position,
        duration = duration,
        isHost = isHosting,
        onPlayPause = onPlayPause,
        onSeek = onSeek
    ) { playerPadding ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(Color.Green)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Live Party: ${members.size + (if (isHosting) 1 else 0)} Members",
                            color = lavenderColor,
                            fontSize = 16.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = lavenderColor)
                    }
                },
                actions = {
                    // Songs and Search only for host
                    if (isHosting) {
                        TextButton(onClick = {
                            showLibrarySheet = true
                        }) {
                            Text("Songs", color = accentOrange)
                        }
                        TextButton(onClick = onOpenSearch) {
                            Text("Search", color = accentOrange)
                        }
                    }
                    IconButton(onClick = {
                        if (isHosting) {
                            partyViewModel.stopHosting()
                        } else {
                            partyViewModel.leaveParty()
                        }
                        onEndPartyClick()
                    }) {
                        Icon(Icons.Default.Close, contentDescription = "End Party", tint = Color.Red)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = darkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(bottom = playerPadding.calculateBottomPadding())
        ) {
            // Host banner: show client issues if any
            if (isHosting && clientIssues.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF442222)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Member Issues",
                                color = Color(0xFFFF9800),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            clientIssues.values.forEach { issue ->
                                Text(
                                    text = issue,
                                    color = AppColors.textBody,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            // Main Content Area
            if (song == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(0.3f),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No song playing", color = lavenderColor)
                }
            }

            // Synced Members Bottom Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = AppColors.cardBackground
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Synced Members",
                            color = lavenderColor,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (isHosting && isAdmin) {
                            // Host can see Transfer Host button (placeholder for now)
                            TextButton(onClick = { /* Transfer Host via selection */ }) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = accentOrange)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Transfer Host", color = accentOrange)
                            }
                        } else if (!isHosting) {
                            // Client can request to be host
                            val myName = userPreferences.getUserName().ifBlank { "Guest" } + "_SyncTax"
                            TextButton(onClick = {
                                Log.d(TAG, "👑 Requesting host transfer as '$myName'")
                                partyViewModel.requestHostTransfer(myName)
                            }) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = accentOrange)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Request Host", color = accentOrange, fontSize = 13.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Show "Me" entry first
                        item {
                            val myName = if (isHosting) {
                                val name = userPreferences.getUserName().ifBlank { "Host" }
                                "${name}_SyncTax (Me) \uD83D\uDC51"
                            } else {
                                val name = userPreferences.getUserName().ifBlank { "Guest" }
                                "${name}_SyncTax (Me)"
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(Color.Green)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(myName, color = AppColors.textTitle)
                                }
                                Text("Connected", color = AppColors.textBody, fontSize = 12.sp)
                            }
                        }

                        items(members) { member ->
                            val hasIssue = clientIssues.containsKey(member.endpointId)
                            val displayName = if (member.isHost) {
                                "${member.name} \uD83D\uDC51"
                            } else {
                                member.name
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (hasIssue) Color(0xFFFF9800)
                                                else Color.Yellow
                                            )
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(displayName, color = AppColors.textTitle)
                                        if (hasIssue) {
                                            Text(
                                                text = clientIssues[member.endpointId] ?: "",
                                                color = Color(0xFFFF9800),
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                                Text(
                                    if (hasIssue) "Issue" else "Synced",
                                    color = if (hasIssue) Color(0xFFFF9800) else AppColors.textBody,
                                    fontSize = 12.sp
                                )
                            }
                        }

                        item {
                            BottomPaddingSpacer()
                        }
                    }
                }
            }
        }
    }

    if (showLibrarySheet) {
        ModalBottomSheet(
            onDismissRequest = { showLibrarySheet = false },
            sheetState = librarySheetState,
            containerColor = AppColors.cardBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Party Songs",
                    color = lavenderColor,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = songQuery,
                    onValueChange = { songQuery = it },
                    label = { Text("Search offline songs", color = AppColors.textBody) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentOrange,
                        unfocusedBorderColor = AppColors.textBody,
                        focusedTextColor = AppColors.textTitle,
                        unfocusedTextColor = AppColors.textTitle,
                        cursorColor = accentOrange
                    ),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredSongs.isEmpty()) {
                    Text(
                        text = "No songs match your search.",
                        color = AppColors.textBody,
                        fontSize = 13.sp
                    )
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items = filteredSongs.take(50)) { itemSong: Song ->
                            SimpleSongCard(
                                song = itemSong,
                                onClick = {
                                    Log.d(TAG, "🎵 Host selected song: '${itemSong.title}' by '${itemSong.artist}' (id=${itemSong.id})")
                                    // 1. Send NowPlaying so client knows WHICH song
                                    partyViewModel.sendNowPlaying(
                                        songId = itemSong.id,
                                        title = itemSong.title,
                                        artist = itemSong.artist,
                                        album = itemSong.album,
                                        thumbnailUrl = itemSong.albumArtUri,
                                        isOffline = true
                                    )
                                    // 2. Send PlayCommand for timing sync
                                    val now = SystemClock.elapsedRealtime() + 1500L
                                    partyViewModel.sendPlayCommand(
                                        songId = itemSong.id,
                                        startTimestamp = now,
                                        title = itemSong.title,
                                        artist = itemSong.artist,
                                        album = itemSong.album,
                                        thumbnailUrl = itemSong.albumArtUri,
                                        isOffline = true
                                    )
                                    // 3. Play locally on host
                                    playerViewModel.playSong(itemSong)
                                    showLibrarySheet = false
                                },
                                onLongClick = {
                                    playerViewModel.placeNext(itemSong)
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Long press adds the song next. Use Search for offline and online search.",
                    color = AppColors.textBody,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
        }
    }
}