package com.just_for_fun.synctax.ui.components.chips

import androidx.compose.foundation.isSystemInDarkTheme
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
import com.just_for_fun.synctax.ui.theme.LightChipSelected
import com.just_for_fun.synctax.ui.theme.LightChipSelectedText
import com.just_for_fun.synctax.ui.theme.LightChipUnselected
import com.just_for_fun.synctax.ui.theme.LightChipUnselectedText

@Composable
fun FilterChipsRow(
    selectedChip: String = "All",
    onChipSelected: (String) -> Unit = {}
) {
    val isDarkTheme = isSystemInDarkTheme()

    val chipSelectedColor = if (isDarkTheme) ChipSelected else LightChipSelected
    val chipUnselectedColor = if (isDarkTheme) ChipUnselected else LightChipUnselected
    val chipSelectedTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onPrimary else LightChipSelectedText
    val chipUnselectedTextColor = if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant else LightChipUnselectedText

    val chips = listOf("All", "Quick Picks", "Listen Again", "Speed Dial", "Playlists", "Quick Access")

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
                        selectedContainerColor = chipSelectedColor,
                        selectedLabelColor = chipSelectedTextColor,
                        containerColor = chipUnselectedColor,
                        labelColor = chipUnselectedTextColor
                    ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = chip == selectedChip,
                    selectedBorderColor = if (isDarkTheme) Color.White else LightChipSelected,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                ),
                shape = MaterialTheme.shapes.medium
            )   
        }
    }
}