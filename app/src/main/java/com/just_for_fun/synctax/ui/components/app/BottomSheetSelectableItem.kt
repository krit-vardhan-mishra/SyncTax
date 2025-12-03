package com.just_for_fun.synctax.ui.components.app

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun BottomSheetSelectableItem(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    // Animate background when selected
    val backgroundColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
        else Color.Transparent,
        animationSpec = tween(250)
    )

    // Checkmark animation
    val checkScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = tween(200)
    )

    ListItem(
        headlineContent = {
            Text(text)
        },

        leadingContent = {
            // Animated checkmark like M3 ListItem
            if (checkScale > 0f) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.graphicsLayer {
                        scaleX = checkScale
                        scaleY = checkScale
                        alpha = checkScale
                    }
                )
            }
        },

        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(
                    bounded = true,
                    radius = 32.dp
                )
            ) {
                onClick()
            }
            .padding(horizontal = 4.dp)
    )
}
