package com.just_for_fun.synctax.presentation.components.section

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.just_for_fun.synctax.presentation.components.player.BottomOptionsDialog
import com.just_for_fun.synctax.presentation.components.player.DialogOption
import com.just_for_fun.synctax.presentation.components.utils.SortOption
import com.just_for_fun.synctax.presentation.components.utils.TooltipBox
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import com.just_for_fun.synctax.presentation.ui.theme.PlayerTextPrimary
import com.just_for_fun.synctax.presentation.utils.AlbumColors
import androidx.compose.material.icons.filled.Person


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
    showPersonalizeButton: Boolean = false,
    onShuffleClick: (() -> Unit)? = null,
    onRefreshClick: (() -> Unit)? = null,
    onSearchClick: (() -> Unit)? = null,
    onSortClick: (() -> Unit)? = null,
    onProfileClick: (() -> Unit)? = null,
    onPersonalizeClick: (() -> Unit)? = null,
    onNavigateBack: (() -> Unit)? = null,
    onTrainClick: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    onNavigateToHistory: (() -> Unit)? = null,
    onNavigateToStats: (() -> Unit)? = null,
    userPreferences: UserPreferences? = null,
    userName: String? = null,
    userInitial: String? = null,
    sortOption: SortOption? = null,
    onSortOptionChange: ((SortOption) -> Unit)? = null,
    currentTab: Int? = null,
    showSortDialog: Boolean = false,
    onShowSortDialogChange: ((Boolean) -> Unit)? = null
) {
    var showProfileDialog by remember { mutableStateOf(false) }
    var showNameChangeDialog by remember { mutableStateOf(false) }

    // Theme-aware colors from AppColors
    val appBarBackgroundColor = AppColors.appBarBackground
    val titleTextColor = AppColors.topAppBarTitle
    val iconColor = AppColors.topAppBarTitle
    val accentIconColor = AppColors.topAppBarIcon

    TopAppBar(
        title = {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = titleTextColor
            )
        },
        navigationIcon = {
            if (onNavigateBack != null) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = iconColor
                    )
                }
            }
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
                            tint = iconColor
                        )
                    }
                }
            }

            // Sort button
            if (showSortButton && onSortOptionChange != null && currentTab == 0) {
                TooltipBox(tooltip = "Sort songs") {
                    IconButton(onClick = { onShowSortDialogChange?.invoke(true) }) {
                        Icon(
                            imageVector = Icons.Default.Sort,
                            contentDescription = "Sort",
                            tint = iconColor
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
                            tint = iconColor
                        )
                    }
                }
            }

            // Personalize button
            if (showPersonalizeButton && onPersonalizeClick != null) {
                TooltipBox(tooltip = "Personalize recommendations") {
                    IconButton(onClick = onPersonalizeClick) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Personalize",
                            tint = accentIconColor
                        )
                    }
                }
            }

            // Profile button
            if (showProfileButton && userInitial != null) {
                Box {
                    TooltipBox(tooltip = "Profile & Settings") {
                        IconButton(onClick = { showProfileDialog = true }) {
                            UserProfileIcon(userInitial = userInitial)
                        }
                    }
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = appBarBackgroundColor
        )
    )

    // User Profile Dialog
    if (showProfileDialog) {
        val options = listOf(
            DialogOption(
                id = "train",
                title = "Train model",
                icon = {
                    Icon(
                        Icons.Default.Build,
                        contentDescription = null,
                        tint = PlayerTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    showProfileDialog = false
                    onTrainClick?.invoke()
                }
            ),
            DialogOption(
                id = "settings",
                title = "App settings",
                icon = {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = null,
                        tint = PlayerTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    showProfileDialog = false
                    onOpenSettings?.invoke()
                }
            ),
            DialogOption(
                id = "edit",
                title = "Change name",
                icon = {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        tint = PlayerTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    showProfileDialog = false
                    showNameChangeDialog = true
                }
            ),
            DialogOption(
                id = "history",
                title = "Listening History",
                icon = {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = PlayerTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    showProfileDialog = false
                    onNavigateToHistory?.invoke()
                }
            ),
            DialogOption(
                id = "stats",
                title = "Listening Stats",
                icon = {
                    Icon(
                        Icons.Default.BarChart,
                        contentDescription = null,
                        tint = PlayerTextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = {
                    showProfileDialog = false
                    onNavigateToStats?.invoke()
                }
            )
        )

        BottomOptionsDialog(
            isVisible = true,
            onDismiss = { showProfileDialog = false },
            options = options,
            customHeader = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    UserProfileIcon(userInitial = userInitial ?: "")
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = userName?.ifEmpty { "User" } ?: "User",
                        style = MaterialTheme.typography.titleMedium,
                        color = PlayerTextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        )
    }

    // User Profile Name Change Dialog
    if (showNameChangeDialog && userPreferences != null && userName != null) {
        UserProfileDialog(
            currentUserName = userName,
            onDismiss = { showNameChangeDialog = false },
            onNameUpdate = { newName ->
                userPreferences.saveUserName(newName)
            }
        )
    }
}
