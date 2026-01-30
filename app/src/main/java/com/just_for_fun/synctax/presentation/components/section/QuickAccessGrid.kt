package com.just_for_fun.synctax.presentation.components.section

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.R
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.components.card.PlayingIndicator

@Composable
fun QuickAccessGrid(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    currentSong: Song?,
    modifier: Modifier = Modifier
) {
    val gridColumns = 4 // Responsive: 3 on smaller screens
    
    // We use a fixed height grid or calculate height based on rows to avoid scrolling within scrolling
    // Since this is inside a LazyColumn in HomeScreen, we generally shouldn't use LazyVerticalGrid 
    // without a fixed height or we should use FlowRow.
    // However, the spec asks for LazyVerticalGrid with fixed height.
    
    LazyVerticalGrid(
        columns = GridCells.Fixed(gridColumns),
        modifier = modifier
            .fillMaxWidth()
            .height(240.dp), // Increased slightly to accommodate 2 rows comfortably if needed
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false // Disable scrolling since it's nested
    ) {
        items(songs.take(8)) { song -> // Limit to 8 items (2 rows of 4)
            Box(
                modifier = Modifier
                    .aspectRatio(1f) // Square aspect ratio
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .clickable { onSongClick(song) }
            ) {
                // Album art
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = song.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback or placeholder could go here
                     Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)))
                }
                
                // Current playing indicator
                if (currentSong?.id == song.id) {
                     Box(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(24.dp)
                            .background(MaterialTheme.colorScheme.background.copy(alpha=0.5f), CircleShape)
                     ) {
                        PlayingIndicator(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(4.dp)
                        )
                     }
                }
            }
        }
    }
}
