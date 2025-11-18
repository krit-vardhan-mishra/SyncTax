package com.just_for_fun.synctax.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun AppNavigationBar(navController: NavController) {
    val selectedBackgroundColor = Color(0xFFE80432)

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface
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
            NavigationBarItem(
                icon = item.icon,
                label = { Text(item.label) },
                selected = currentRoute == item.route,
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
                    selectedIconColor = Color.White,
                    selectedTextColor = Color.White,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = selectedBackgroundColor
                )
            )
        }
    }
}