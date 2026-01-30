package com.just_for_fun.synctax.presentation.components.player

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.data.local.entities.Song
import kotlinx.coroutines.Job

@Composable
fun UpNextSheet(
    upcomingItems: List<Song>,
    historyItems: List<Song>,
    onSelect: (Song) -> Unit,  // This should now call playFromQueue
    onPlaceNext: (Song) -> Unit,
    onRemoveFromQueue: (Song) -> Unit,
    onReorderQueue: (Int, Int) -> Unit,
    modifier: Modifier,
    shape: CornerBasedShape,
    color: Color
) {
    // --- DRAG AND DROP STATE ---
    val scope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    var draggingItemIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dragJob by remember { mutableStateOf<Job?>(null) }

    val itemHeights = remember { mutableStateMapOf<Int, Int>() }

    fun handleDragEnd() {
        draggingItemIndex?.let { startIndex ->
            val currentLayoutInfo = lazyListState.layoutInfo
            val visibleItems = currentLayoutInfo.visibleItemsInfo
            
            // Find the item user is hovering over
            val targetItem = visibleItems.find {
                val itemCenterY = it.offset + it.size / 2
                dragOffset in (it.offset.toFloat().. (it.offset + it.size).toFloat())
            }

            targetItem?.index?.let { endIndex ->
                if (startIndex != endIndex) {
                    onReorderQueue(startIndex, endIndex)
                }
            }
        }
        dragJob?.cancel()
        draggingItemIndex = null
        dragOffset = 0f
    }
    // --- END DRAG AND DROP STATE ---

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .pointerInput(Unit) {
                // This outer pointerInput is to catch when the drag is released
                // anywhere on the screen, not just on the item.
                detectDragGesturesAfterLongPress(
                    onDragStart = { /* This is handled by item's modifier */ },
                    onDrag = { change, dragAmount -> 
                        draggingItemIndex?.let {
                            dragOffset += dragAmount.y
                        }
                    },
                    onDragEnd = { handleDragEnd() },
                    onDragCancel = { handleDragEnd() }
                )
            }
    ) {
        Text(
            text = "Queue",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Start
        )
        HorizontalDivider()

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            if (upcomingItems.isNotEmpty()) {
                item {
                    Text(
                        text = "Up next (${upcomingItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                
                // --- DRAG AND DROP LAZYCOLUMN ---
                itemsIndexed(upcomingItems, key = { _, song -> song.id }) { index, song ->
                    val isDragging = draggingItemIndex == index
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                // Apply shadow and vertical offset if dragging
                                shadowElevation = if (isDragging) 8f else 0f
                                translationY = if (isDragging) dragOffset - (itemHeights[index] ?: 0) / 2 else 0f
                            }
                    ) {
                        UpNextItem(
                            song = song,
                            onSelect = onSelect,
                            onPlaceNext = onPlaceNext,
                            onRemoveFromQueue = onRemoveFromQueue,
                            dragHandleModifier = Modifier.pointerInput(Unit) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = { offset ->
                                        // Store item height
                                        itemHeights[index] = size.height
                                        // Set dragging state
                                        draggingItemIndex = index
                                        // Center the drag offset
                                        dragOffset = offset.y + (itemHeights[index] ?: 0) / 2
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                    },
                                    onDragEnd = { handleDragEnd() },
                                    onDragCancel = { handleDragEnd() }
                                )
                            }
                        )
                    }
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                }
                // --- END DRAG AND DROP ---
            }

            if (historyItems.isNotEmpty()) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "History (${historyItems.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                itemsIndexed(historyItems.reversed(), key = { _, song -> song.id }) { _, song ->
                    UpNextItem(
                        song = song,
                        onSelect = onSelect,
                        onPlaceNext = onPlaceNext,
                        onRemoveFromQueue = onRemoveFromQueue,
                        isHistory = true
                        // No drag handle modifier for history
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

            if (upcomingItems.isEmpty() && historyItems.isEmpty()) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No songs in queue",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
