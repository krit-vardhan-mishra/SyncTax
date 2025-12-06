package com.just_for_fun.synctax.presentation.components.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

data class FabAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

/**
 * A Jetpack Compose implementation of the Material 3 FAB Menu (Speed Dial).
 *
 * @param actions The list of actions to display in the menu.
 * @param modifier The modifier to be applied to the outer Box container.
 */
@Composable
fun FABMenu(
    actions: List<FabAction>,
    modifier: Modifier = Modifier
) {
    // State to track whether the menu items are visible
    var isMenuOpen by remember { mutableStateOf(false) }

    // Reverse the actions so the first item is highest on the screen
    val displayActions = remember(actions) { actions.reversed() }

    // Use a Box to layer the menu items (Column) and the main FAB
    Box(modifier = modifier) {
        // --- Menu Items (Speed Dial Actions) ---
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 16.dp), // Position actions above the main FAB
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp) // Spacing between items
        ) {
            // Display actions in reverse order so the first item is highest on the screen
            displayActions.forEach { action ->
                // Animate the visibility and sliding of each action item
                AnimatedVisibility(
                    visible = isMenuOpen,
                    enter = fadeIn(animationSpec = tween(300)) + slideInVertically(initialOffsetY = { it }),
                    exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(targetOffsetY = { it })
                ) {
                    // M3 Speed Dial actions are often implemented using Extended FABs for clear labeling
                    ExtendedFloatingActionButton(
                        onClick = {
                            action.onClick()
                            isMenuOpen = false // Close menu after action
                        },
                        text = { Text(text = action.label) },
                        icon = { Icon(imageVector = action.icon, contentDescription = action.label) },
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh, // High contrast background
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- Main FAB to toggle the menu ---
        FloatingActionButton(
            onClick = { isMenuOpen = !isMenuOpen },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary, // Primary color for the main button
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = if (isMenuOpen) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = if (isMenuOpen) "Close menu" else "Open menu"
            )
        }
    }
}