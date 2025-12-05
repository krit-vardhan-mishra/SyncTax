package com.just_for_fun.synctax.presentation.components.section

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.preferences.UserPreferences
import com.just_for_fun.synctax.presentation.components.app.UserProfileDialog
import com.just_for_fun.synctax.presentation.components.app.UserProfileIcon
import com.just_for_fun.synctax.presentation.components.utils.SortOption
import com.just_for_fun.synctax.presentation.components.utils.TooltipBox
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import com.just_for_fun.synctax.presentation.utils.AlbumColors

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

    // Theme-aware colors from AppColors
    val appBarBackgroundColor = AppColors.appBarBackground
    val titleTextColor = AppColors.topAppBarTitle
    val iconColor = AppColors.topAppBarTitle
    val accentIconColor = AppColors.topAppBarIcon

    val dynamicIconColor = iconColor

    TopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = titleTextColor
            )
        },
        actions = {
            // Shuffle button
            if (showShuffleButton && onShuffleClick != null) {
                TooltipBox(tooltip = "Shuffle songs") {
                    IconButton(onClick = onShuffleClick) {
                        Icon(
                            imageVector = Icons.Default.Shuffle,
                            contentDescription = "Shuffle",
                            tint = accentIconColor
                        )
                    }
                }
            }

            // Refresh button
            if (showRefreshButton && onRefreshClick != null) {
                TooltipBox(tooltip = "Refresh library") {
                    IconButton(onClick = onRefreshClick) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = dynamicIconColor
                        )
                    }
                }
            }

            // Sort button
            if (showSortButton && onSortClick != null && currentTab == 0) {
                TooltipBox(tooltip = "Sort songs") {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = dynamicIconColor
                        )
                    }
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
                                        iconColor
                                )
                            }
                        )
                    }
                }
            }

            // Search button
            if (showSearchButton && onSearchClick != null) {
                TooltipBox(tooltip = "Search music") {
                    IconButton(onClick = onSearchClick) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = dynamicIconColor
                        )
                    }
                }
            }

            // Profile button
            if (showProfileButton && userInitial != null) {
                Box {
                    TooltipBox(tooltip = "Profile & Settings") {
                        IconButton(onClick = { showProfileMenu = true }) {
                            UserProfileIcon(userInitial = userInitial)
                        }
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
                                color = titleTextColor
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
            containerColor = appBarBackgroundColor
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
