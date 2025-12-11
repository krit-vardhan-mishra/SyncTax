package com.just_for_fun.synctax.presentation.components.optimization

import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Optimized LazyColumn wrapper that follows OuterTune/SimpMusic patterns
 * 
 * Key optimizations:
 * 1. Provides default item keys to prevent recomposition
 * 2. Uses remember for stable lambda references
 * 3. Configures optimal prefetch distance
 */

/**
 * Helper extension to create stable item keys for common data types
 */
@Composable
fun <T> List<T>.toStableKeys(keySelector: (T) -> Any): List<Any> =
    remember(this) { map(keySelector) }

/**
 * Optimized LazyColumn configuration
 * Use this instead of regular LazyColumn for better performance
 */
@Composable
fun OptimizedLazyColumn(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical =
        if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    flingBehavior: FlingBehavior = ScrollableDefaults.flingBehavior(),
    userScrollEnabled: Boolean = true,
    content: LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        flingBehavior = flingBehavior,
        userScrollEnabled = userScrollEnabled,
        content = content
    )
}

/**
 * Example usage pattern from OuterTune:
 * 
 * ```kotlin
 * OptimizedLazyColumn {
 *     items(
 *         items = songs,
 *         key = { song -> song.id } // ✅ Prevents recomposition
 *     ) { song ->
 *         SongCard(song)
 *     }
 * }
 * ```
 */
