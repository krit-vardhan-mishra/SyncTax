package com.just_for_fun.synctax.ui.components.chips

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.ui.model.SearchFilterType

/**
 * Filter chips for search results
 * Allows filtering by All, Songs, Albums, Videos
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchFilterChips(
    selectedFilter: SearchFilterType,
    onFilterSelected: (SearchFilterType) -> Unit,
    modifier: Modifier = Modifier,
    showVideos: Boolean = false // Option to show/hide videos filter
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // All filter
        item {
            FilterChipItem(
                label = "All",
                icon = Icons.Default.Apps,
                selected = selectedFilter == SearchFilterType.ALL,
                onClick = { onFilterSelected(SearchFilterType.ALL) }
            )
        }
        
        // Songs filter
        item {
            FilterChipItem(
                label = "Songs",
                icon = Icons.Default.MusicNote,
                selected = selectedFilter == SearchFilterType.SONGS,
                onClick = { onFilterSelected(SearchFilterType.SONGS) }
            )
        }
        
        // Albums filter
        item {
            FilterChipItem(
                label = "Albums",
                icon = Icons.Default.Album,
                selected = selectedFilter == SearchFilterType.ALBUMS,
                onClick = { onFilterSelected(SearchFilterType.ALBUMS) }
            )
        }
        
        // Artists filter
        item {
            FilterChipItem(
                label = "Artists",
                icon = Icons.Default.Person,
                selected = selectedFilter == SearchFilterType.ARTISTS,
                onClick = { onFilterSelected(SearchFilterType.ARTISTS) }
            )
        }
        
        // Videos filter (optional)
        if (showVideos) {
            item {
                FilterChipItem(
                    label = "Videos",
                    icon = Icons.Default.VideoLibrary,
                    selected = selectedFilter == SearchFilterType.VIDEOS,
                    onClick = { onFilterSelected(SearchFilterType.VIDEOS) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterChipItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = modifier
    )
}
