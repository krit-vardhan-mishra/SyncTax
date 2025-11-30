package com.just_for_fun.synctax.ui.components.chips

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.ui.theme.ChipSelected
import com.just_for_fun.synctax.ui.theme.ChipUnselected

@Composable
fun FilterChipsRow(
    selectedChip: String = "All",
    onChipSelected: (String) -> Unit = {}
) {
    val chips = listOf("All", "Quick Picks", "Listen Again", "Speed Dial", "Quick Access")

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(chips) { chip ->
            FilterChip(
                selected = chip == selectedChip,
                onClick = { onChipSelected(chip) },
                label = { Text(chip) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ChipSelected,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                        containerColor = ChipUnselected,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = chip == selectedChip,
                    selectedBorderColor = Color.White,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                ),
                shape = MaterialTheme.shapes.medium
            )   
        }
    }
}