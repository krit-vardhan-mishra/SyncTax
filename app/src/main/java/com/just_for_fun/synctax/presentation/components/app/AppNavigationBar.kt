package com.just_for_fun.synctax.presentation.components.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import com.just_for_fun.synctax.presentation.utils.AlbumColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigationBar(
    navController: NavController,
    albumColors: AlbumColors = AlbumColors.default()
) {
    var lastLibraryClickTime by remember { mutableLongStateOf(0L) }
    var showLibraryBottomSheet by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    data class NavItem(val route: String, val label: String, val icon: @Composable () -> Unit)

    val items = listOf(
        NavItem(
            "home",
            "Home",
            { Icon(Icons.Default.Home, contentDescription = "Home") }),
        NavItem(
            "search",
            "Search",
            { Icon(Icons.Default.Search, contentDescription = "Search") }),
        NavItem(
            "quick_picks",
            "Picks",
            { Icon(Icons.Default.PlayCircle, contentDescription = "Quick Picks") }),
        NavItem(
            "library",
            if (currentRoute == "playlists") "Playlists" else "Library",
            { Icon(if (currentRoute == "playlists") Icons.Default.QueueMusic else Icons.Default.LibraryMusic, contentDescription = if (currentRoute == "playlists") "Playlists" else "Library") })
    )

    NavigationBar(
        containerColor = AppColors.bottomNavBackground,
        contentColor = Color.White
    ) {

        items.forEach { item ->
            val isSelected = when (item.route) {
                "library" -> currentRoute == "library" || currentRoute == "playlists"
                else -> currentRoute == item.route
            }

            NavigationBarItem(
                // 1. Custom Icon Composable with Glow Effect
                icon = {
                    Box(contentAlignment = Alignment.Center) {
                        if (isSelected) {
                            // The Glow Effect behind the icon (lighter)
                            Box(
                                modifier = Modifier
                                    .size(50.dp) // Size of the glow
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                AppColors.navBarGlowColor.copy(alpha = 0.6f), // Center (Lighter)
                                                Color.Transparent // Edges (Fades out)
                                            ),
                                            radius = 70f // Adjust radius for spread
                                        )
                                    )
                            )
                        }
                        // Render the actual icon on top
                        item.icon()
                    }
                },
                label = { Text(item.label) },
                selected = isSelected,
                onClick = {
                    if (item.route == "library") {
                        val currentTime = System.currentTimeMillis()
                        if (currentTime - lastLibraryClickTime < 300) {
                            showLibraryBottomSheet = true
                        } else {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                        lastLibraryClickTime = currentTime
                    } else {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedTextColor = AppColors.navBarSelectedText,
                    selectedIconColor = AppColors.navBarSelectedIcon,
                    unselectedIconColor = AppColors.navBarUnselectedIcon,
                    unselectedTextColor = AppColors.navBarUnselectedText,
                )
            )
        }
    }

    if (showLibraryBottomSheet) {
        ModalBottomSheet(onDismissRequest = { showLibraryBottomSheet = false }) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Text(
                    text = "View my",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                )

                BottomSheetSelectableItem(
                    text = "Library",
                    selected = currentRoute == "library",
                    onClick = {
                        showLibraryBottomSheet = false
                        if (currentRoute != "library") {
                            navController.navigate("library") {
                                popUpTo(navController.graph.findStartDestination().id)
                                launchSingleTop = true
                            }
                        }
                    }
                )

                BottomSheetSelectableItem(
                    text = "Playlists",
                    selected = currentRoute == "playlists",
                    onClick = {
                        showLibraryBottomSheet = false
                        navController.navigate("playlists") {
                            popUpTo(navController.graph.findStartDestination().id)
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
