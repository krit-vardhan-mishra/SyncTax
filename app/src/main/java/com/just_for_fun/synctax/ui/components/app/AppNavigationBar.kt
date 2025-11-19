package com.just_for_fun.synctax.ui.components.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.just_for_fun.synctax.ui.utils.AlbumColors

@Composable
fun AppNavigationBar(
    navController: NavController,
    albumColors: AlbumColors = AlbumColors.default()
) {
    val backgroundColor = albumColors.blackColor.copy(alpha = 0.98f)
    val glowColor = albumColors.vibrant
    val backgroundLuminance = backgroundColor.luminance()
    val unselectedIconColor = if (backgroundLuminance > 0.5f) {
        Color.Black.copy(alpha = 0.65f)
    } else {
        Color.White.copy(alpha = 0.65f)
    }

    val unselectedTextColor = if (backgroundLuminance > 0.5f) {
        Color.Black.copy(alpha = 0.75f)
    } else {
        Color.White.copy(alpha = 0.75f)
    }

    NavigationBar(
        containerColor = backgroundColor,
        contentColor = if (backgroundLuminance > 0.5f) Color.Black else Color.White
    ) {
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
                "Library",
                { Icon(Icons.Default.LibraryMusic, contentDescription = "Library") })
        )

        items.forEach { item ->
            val isSelected = currentRoute == item.route

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
                                                glowColor.copy(alpha = 0.6f), // Center (Lighter)
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
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.Transparent,
                    selectedTextColor = Color.White,
                    selectedIconColor = Color.White,
                    unselectedIconColor = unselectedIconColor,
                    unselectedTextColor = unselectedTextColor,
                )
            )
        }
    }
}