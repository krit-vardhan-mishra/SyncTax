package com.just_for_fun.synctax.ui.components.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.ui.components.section.SortOption

@Composable
fun SectionHeader(
    title: String,
    subtitle: String? = null,
    onViewAllClick: (() -> Unit)? = null,
    showSortButton: Boolean = false,
    currentSortOption: SortOption = SortOption.TITLE_ASC,
    onSortOptionChange: ((SortOption) -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showSortButton && onSortOptionChange != null) {
                var showSortMenu by remember { mutableStateOf(false) }

                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort songs",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Name (A-Z)") },
                        onClick = {
                            onSortOptionChange(SortOption.NAME_ASC)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (currentSortOption == SortOption.NAME_ASC) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Name (Z-A)") },
                        onClick = {
                            onSortOptionChange(SortOption.NAME_DESC)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (currentSortOption == SortOption.NAME_DESC) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Artist") },
                        onClick = {
                            onSortOptionChange(SortOption.ARTIST)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (currentSortOption == SortOption.ARTIST) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Date Added (Oldest)") },
                        onClick = {
                            onSortOptionChange(SortOption.DATE_ADDED_OLDEST)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (currentSortOption == SortOption.DATE_ADDED_OLDEST) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Date Added (Newest)") },
                        onClick = {
                            onSortOptionChange(SortOption.DATE_ADDED_NEWEST)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (currentSortOption == SortOption.DATE_ADDED_NEWEST) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Custom") },
                        onClick = {
                            onSortOptionChange(SortOption.CUSTOM)
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (currentSortOption == SortOption.CUSTOM) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }

            onViewAllClick?.let {
                TextButton(onClick = it) {
                    Text("View all", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}