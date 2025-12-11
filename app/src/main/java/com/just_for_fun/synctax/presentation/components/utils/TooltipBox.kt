package com.just_for_fun.synctax.presentation.components.utils

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TooltipBox(
    tooltip: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    var position by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = modifier
            .onGloballyPositioned { coordinates ->
                position = coordinates.positionInWindow()
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { /* Pass through click handling to child */ },
                onLongClick = { showTooltip = true }
            )
    ) {
        content()
        
        if (showTooltip) {
            Tooltip(
                text = tooltip,
                onDismiss = { showTooltip = false }
            )
        }
    }
}

@Composable
fun Tooltip(
    text: String,
    onDismiss: () -> Unit
) {
    Popup(
        onDismissRequest = onDismiss,
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset {
                // Position above the anchor if possible, otherwise below
                val x = anchorBounds.left + (anchorBounds.width - popupContentSize.width) / 2
                var y = anchorBounds.top - popupContentSize.height - 16
                
                if (y < 0) {
                    y = anchorBounds.bottom + 16
                }
                
                return IntOffset(x, y)
            }
        }
    ) {
        Surface(
            color = MaterialTheme.colorScheme.inverseSurface,
            shape = MaterialTheme.shapes.small,
            shadowElevation = 4.dp
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(8.dp),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
