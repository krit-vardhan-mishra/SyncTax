package com.just_for_fun.synctax.presentation.utils

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * CompositionLocal to provide bottom content padding for screens.
 * This accounts for the mini player (80.dp) and bottom navigation bar (~80.dp).
 * 
 * Usage: 
 * - Wrap your NavHost with `CompositionLocalProvider(LocalBottomPadding provides paddingValue)`
 * - In any screen composable, use `Spacer(modifier = Modifier.height(LocalBottomPadding.current))`
 *   at the end of scrollable content.
 */
val LocalBottomPadding = compositionLocalOf { 0.dp }

/**
 * Standard heights for calculating bottom padding
 */
object BottomPaddingDefaults {
    val MiniPlayerHeight = 80.dp
    val NavigationBarHeight = 80.dp
    
    /**
     * Total padding when both mini player and nav bar are visible
     */
    val TotalPadding = MiniPlayerHeight + NavigationBarHeight
    
    /**
     * Padding when only navigation bar is visible (no song playing)
     */
    val NavBarOnlyPadding = NavigationBarHeight
}
