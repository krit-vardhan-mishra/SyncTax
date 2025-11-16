package com.just_for_fun.youtubemusic.ui.components.player

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.just_for_fun.youtubemusic.core.data.local.entities.Song

@Composable
fun UpNextSheet(
    upcomingItems: List<Song>,
    historyItems: List<Song>,
    onSelect: (Song) -> Unit,
    onPlaceNext: (Song) -> Unit,
    onRemoveFromQueue: (Song) -> Unit,
    onReorderQueue: (Int, Int) -> Unit,
    snackbarHostState: SnackbarHostState
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
    ) {
        Text(
            text = "Queue",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start
        )
        HorizontalDivider()
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (upcomingItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Up next (${upcomingItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(upcomingItems) { song ->
                    UpNextItem(
                        song = song,
                        onSelect = onSelect,
                        onPlaceNext = onPlaceNext,
                        onRemoveFromQueue = onRemoveFromQueue,
                        snackbarHostState = snackbarHostState
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
            if (historyItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "History (${historyItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                items(historyItems.reversed()) { song ->
                    UpNextItem(
                        song = song,
                        onSelect = onSelect,
                        onPlaceNext = onPlaceNext,
                        onRemoveFromQueue = onRemoveFromQueue,
                        snackbarHostState = snackbarHostState,
                        isHistory = true
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }
            if (upcomingItems.isEmpty() && historyItems.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No songs in queue",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
