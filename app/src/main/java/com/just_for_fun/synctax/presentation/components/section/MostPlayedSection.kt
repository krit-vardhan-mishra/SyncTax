package com.just_for_fun.synctax.presentation.components.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.components.card.SimpleSongCard
import com.just_for_fun.synctax.presentation.ui.theme.AppColors

/**
 * Most Played Section - Shows songs with highest play counts from listening history
 */
@Composable
fun MostPlayedSection(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (songs.isEmpty()) return

    val haptic = LocalHapticFeedback.current

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = "Most Played",
            subtitle = "Your top tracks",
            onViewAllClick = null,
            titleColor = AppColors.homeSectionTitle,
            subtitleColor = AppColors.homeSectionSubtitle
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 16.dp)
        ) {
            items(
                items = songs.take(10),
                key = { song -> "most_played_${song.id}" }
            ) { song ->
                Box(modifier = Modifier.fillParentMaxWidth(0.85f)) {
                    SimpleSongCard(
                        song = song,
                        onClick = { onSongClick(song) },
                        onLongClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSongLongClick(song)
                        }
                    )
                }
            }
        }
    }
}
