package com.just_for_fun.synctax.presentation.components.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory
import com.just_for_fun.synctax.presentation.components.card.OnlineHistoryCard
import com.just_for_fun.synctax.presentation.components.utils.EmptyHistoryState

@Composable
fun OnlineHistoryTab(
    history: List<OnlineListeningHistory>,
    onHistoryClick: (OnlineListeningHistory) -> Unit,
    onRemove: (OnlineListeningHistory) -> Unit
) {
    if (history.isEmpty()) {
        EmptyHistoryState(message = "No online listening history yet.\nPlay some songs online to see them here.")
    } else {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.width(400.dp), // Adjust width to fit 2 cards comfortably
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = history,
                    key = { it.id }
                ) { item ->
                    OnlineHistoryCard(
                        history = item,
                        onClick = { onHistoryClick(item) },
                        onRemoveFromHistory = { onRemove(item) }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(225.dp))
                }
            }
        }
    }
}

