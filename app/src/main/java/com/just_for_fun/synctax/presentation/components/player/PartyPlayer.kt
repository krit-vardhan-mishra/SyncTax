package com.just_for_fun.synctax.presentation.components.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.local.entities.Song

@Composable
fun PartyPlayer(
    song: Song,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    // Placeholder for draggable party player based on MiniPlayer and FullScreenPlayer
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF280A0A)),
        contentAlignment = Alignment.Center
    ) {
        Text("Party Player (Draggable Placeholder)", color = Color.White)
    }
}
