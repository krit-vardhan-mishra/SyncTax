package com.just_for_fun.synctax.presentation.components.chips

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.presentation.model.SearchFilterType
import com.just_for_fun.synctax.presentation.ui.theme.AppColors

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
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        // All filter
        item {
            FilterChipItem(
                label = "All",
                selected = selectedFilter == SearchFilterType.ALL,
                onClick = { onFilterSelected(SearchFilterType.ALL) }
            )
        }
        
        // Songs filter
        item {
            FilterChipItem(
                label = "Songs",
                selected = selectedFilter == SearchFilterType.SONGS,
                onClick = { onFilterSelected(SearchFilterType.SONGS) }
            )
        }
        
        // Albums filter
        item {
            FilterChipItem(
                label = "Albums",
                selected = selectedFilter == SearchFilterType.ALBUMS,
                onClick = { onFilterSelected(SearchFilterType.ALBUMS) }
            )
        }
        
        // Artists filter
        item {
            FilterChipItem(
                label = "Artists",
                selected = selectedFilter == SearchFilterType.ARTISTS,
                onClick = { onFilterSelected(SearchFilterType.ARTISTS) }
            )
        }
        
        // Videos filter (optional)
        if (showVideos) {
            item {
                FilterChipItem(
                    label = "Videos",
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
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = AppColors.chipSelected,
            selectedLabelColor = AppColors.chipSelectedText,
            containerColor = AppColors.chipUnselected,
            labelColor = AppColors.chipUnselectedText
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            selectedBorderColor = AppColors.chipSelectedBorder,
            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
        ),
        shape = MaterialTheme.shapes.medium,
        modifier = modifier
    )
}
