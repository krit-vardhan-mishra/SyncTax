package com.just_for_fun.synctax.ui.components.section

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.ui.components.UserProfileDialog
import com.just_for_fun.synctax.ui.components.UserProfileIcon
import com.just_for_fun.synctax.ui.utils.AlbumColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleDynamicMusicTopAppBar(
    title: String,
    albumColors: AlbumColors,
    showShuffleButton: Boolean = false,
    showRefreshButton: Boolean = false,
    showSearchButton: Boolean = false,
    showSortButton: Boolean = false,
    showProfileButton: Boolean = false,
    onShuffleClick: (() -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onSortClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
    onTrainClick: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    userPreferences: UserPreferences? = null,
    userName: String? = null,
    userInitial: String? = null,
    sortOption: SortOption? = null,
    onSortOptionChange: ((SortOption) -> Unit)? = null,
    currentTab: Int? = null
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var showProfileMenu by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }

    val dynamicIconColor = MaterialTheme.colorScheme.onBackground
    val accentIconColor = MaterialTheme.colorScheme.primary

    TopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        actions = {
            // Shuffle button
            if (showShuffleButton && onShuffleClick != null) {
                IconButton(onClick = onShuffleClick) {
                    Icon(
                        imageVector = Icons.Default.Shuffle,
                        contentDescription = "Shuffle",
                        tint = accentIconColor
                    )
                }
            }

            // Refresh button
            if (showRefreshButton && onRefreshClick != null) {
                IconButton(onClick = onRefreshClick) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh",
                        tint = dynamicIconColor
                    )
                }
            }

            // Sort button
            if (showSortButton && onSortClick != null && currentTab == 0) {
                IconButton(onClick = { showSortMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.Sort,
                        contentDescription = "Sort",
                        tint = dynamicIconColor
                    )
                }

                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    SortOption.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option.displayName) },
                            onClick = {
                                onSortOptionChange?.invoke(option)
                                showSortMenu = false
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (option == sortOption) Icons.Default.Check else Icons.Default.Sort,
                                    contentDescription = null,
                                    tint = if (option == sortOption)
                                        accentIconColor
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                    }
                }
            }

            // Search button
            if (showSearchButton && onSearchClick != null) {
                IconButton(onClick = onSearchClick) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = dynamicIconColor
                    )
                }
            }

            // Profile button
            if (showProfileButton && userInitial != null) {
                Box {
                    IconButton(onClick = { showProfileMenu = true }) {
                        UserProfileIcon(userInitial = userInitial)
                    }
                    DropdownMenu(
                        expanded = showProfileMenu,
                        onDismissRequest = { showProfileMenu = false }
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalAlignment = Alignment.Start
                        ) {
                            UserProfileIcon(userInitial = userInitial)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = userName?.ifEmpty { "User" } ?: "User",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("Train model") },
                            onClick = {
                                showProfileMenu = false
                                onTrainClick?.invoke()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Build,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("App settings") },
                            onClick = {
                                showProfileMenu = false
                                onOpenSettings?.invoke()
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null
                                )
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Change name") },
                            onClick = {
                                showProfileMenu = false
                                showProfileDialog = true
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = null
                                )
                            }
                        )
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = albumColors.vibrant.copy(alpha = 0.2f)
        )
    )

    // User Profile Dialog
    if (showProfileDialog && userPreferences != null && userName != null) {
        UserProfileDialog(
            currentUserName = userName,
            onDismiss = { showProfileDialog = false },
            onNameUpdate = { newName ->
                userPreferences.saveUserName(newName)
            }
        )
    }
}