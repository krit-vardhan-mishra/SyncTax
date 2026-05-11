package com.just_for_fun.synctax.presentation.screens

import android.util.Log
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.just_for_fun.synctax.presentation.components.utils.BottomPaddingSpacer
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import com.just_for_fun.synctax.presentation.viewmodels.PartyViewModel

private const val TAG = "CreatePartyScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePartyScreen(
    partyViewModel: PartyViewModel,
    onNavigateBack: () -> Unit,
    onStartPartyClick: () -> Unit
) {
    val lavenderColor = AppColors.textTitle
    val accentOrange = AppColors.accentPrimary
    val darkBackground = AppColors.mainBackground

    val isHosting by partyViewModel.isHosting.collectAsState()
    val members by partyViewModel.members.collectAsState()

    var partyName by remember { mutableStateOf("My Party") }

    // Radar pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 150f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "radar_anim"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = androidx.compose.animation.core.RepeatMode.Restart
        ),
        label = "radar_alpha"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isHosting) "Hosting: $partyName" else "Create a Party",
                        color = lavenderColor
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = lavenderColor)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBackground)
            )
        },
        bottomBar = {
            Button(
                onClick = {
                    if (!isHosting) {
                        Log.d(TAG, "🎉 Starting party: $partyName")
                        partyViewModel.startHosting(partyName)
                    }
                    onStartPartyClick()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accentOrange),
                shape = RoundedCornerShape(24.dp)
            ) {
                Text(
                    if (isHosting) "Go to Session" else "Start Party",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        containerColor = darkBackground
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Party Name Input (only visible before hosting starts)
            if (!isHosting) {
                OutlinedTextField(
                    value = partyName,
                    onValueChange = { partyName = it },
                    label = { Text("Party Name", color = AppColors.textBody) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentOrange,
                        unfocusedBorderColor = AppColors.textBody,
                        focusedTextColor = AppColors.textTitle,
                        unfocusedTextColor = AppColors.textTitle,
                        cursorColor = accentOrange
                    ),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "This name will be visible to nearby devices looking to join.",
                    color = AppColors.textBody,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )
            }

            // Radar Animation Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (isHosting) 200.dp else 180.dp),
                contentAlignment = Alignment.Center
            ) {
                // Pulsing rings
                Box(
                    modifier = Modifier
                        .size((pulseRadius * 2).dp)
                        .clip(CircleShape)
                        .background(accentOrange.copy(alpha = pulseAlpha * 0.3f))
                )

                // Central Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(accentOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Group, contentDescription = null, tint = Color.White)
                }
            }

            if (isHosting) {
                Text(
                    text = "Waiting for devices to join...",
                    color = AppColors.textBody,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Connected Members Section (visible when hosting)
            if (isHosting && members.isNotEmpty()) {
                Text(
                    text = "Connected Members (${members.size})",
                    color = lavenderColor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(members) { member ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(AppColors.cardBackground)
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.Green)
                                )
                                Spacer(modifier = Modifier.size(8.dp))
                                Text(
                                    text = member.name,
                                    color = AppColors.textTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "Connected",
                                color = Color.Green.copy(alpha = 0.7f),
                                fontSize = 13.sp
                            )
                        }
                    }

                    item {
                        BottomPaddingSpacer()
                    }
                }
            } else if (isHosting) {
                // Hosting but no one connected yet
                Text(
                    text = "No devices connected yet",
                    color = AppColors.textBody,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(16.dp),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}