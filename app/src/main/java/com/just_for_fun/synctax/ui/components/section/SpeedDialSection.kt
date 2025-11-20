package com.just_for_fun.synctax.ui.components.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.ui.components.app.UserProfileIcon
import com.just_for_fun.synctax.ui.components.card.QuickPickCard


@Composable
fun SpeedDialSection(
    songs: List<com.just_for_fun.synctax.core.data.local.entities.Song>,
    onSongClick: (com.just_for_fun.synctax.core.data.local.entities.Song) -> Unit,
    userInitial: String = "M",
    currentSong: com.just_for_fun.synctax.core.data.local.entities.Song? = null
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
                    onClick = { onSongClick(song) },
                    isPlaying = song.id == currentSong?.id
                )
            }
        }
    }
}