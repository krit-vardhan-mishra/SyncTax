package com.just_for_fun.synctax.presentation.screens

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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.components.player.PartyPlayer
import com.just_for_fun.synctax.presentation.components.utils.BottomPaddingSpacer
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import com.just_for_fun.synctax.presentation.viewmodels.PartyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PartySessionScreen(
    partyViewModel: PartyViewModel,
    song: Song?,
    isPlaying: Boolean,
    onNavigateBack: () -> Unit,
    onEndPartyClick: () -> Unit,
    isAdmin: Boolean = true
) {
    val lavenderColor = AppColors.textTitle
    val accentOrange = AppColors.accentPrimary
    val darkBackground = AppColors.mainBackground

    val members by partyViewModel.members.collectAsState()
    val isHosting by partyViewModel.isHosting.collectAsState()
    val clientIssues by partyViewModel.clientIssues.collectAsState()

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

            // Main Player Area
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (song != null) {
                    PartyPlayer(
                        song = song,
                        isPlaying = isPlaying,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No song playing", color = lavenderColor)
                    }
                }
            }

            // Synced Members Bottom Section
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
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
                        if (isAdmin && isHosting) {
                            TextButton(onClick = { /* Transfer Host */ }) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = accentOrange)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Transfer Host", color = accentOrange)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        // Show Me/Host first
                        item {
                            val name = if (isHosting) "Host (Me) \uD83D\uDC51" else "Me"
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
                                    Text(name, color = AppColors.textTitle)
                                }
                                Text("Connected", color = AppColors.textBody, fontSize = 12.sp)
                            }
                        }

                        items(members) { member ->
                            val hasIssue = clientIssues.containsKey(member.endpointId)
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
                                        Text(member.name, color = AppColors.textTitle)
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
}