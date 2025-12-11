package com.just_for_fun.synctax.presentation.components.utils

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ShapeDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

// Custom colors derived from the user's image (Bright Red: ~#F44336)
private val FabRed = Color(0xFFF44336)

// A light tint for the main FAB when the menu is open and for menu item backgrounds
private val FabRedTint = Color(0xFFD56168) // A light, low-tint red/pink

data class FabAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit
)

/**
 * A Jetpack Compose implementation of the Material 3 FAB Menu (Speed Dial)
 * with custom colors and transition animations based on user request and M3 Expressive principles.
 *
 * @param actions The list of actions to display in the menu.
 * @param modifier The modifier to be applied to the outer Box container.
 */
@Composable
fun FABMenu(
    actions: List<FabAction>,
    modifier: Modifier = Modifier
) {
    var isMenuOpen by remember { mutableStateOf(false) }
    val displayActions = remember(actions) { actions.reversed() }

    val rotation by animateFloatAsState(
        targetValue = if (isMenuOpen) 135f else 0f,
        animationSpec = tween(300)
    )

    val fabContainerColor by animateColorAsState(
        targetValue = if (isMenuOpen) FabRed else FabRed,
        animationSpec = tween(300)
    )

    val fabContentColor by animateColorAsState(
        targetValue = if (isMenuOpen) Color.White else Color.White,
        animationSpec = tween(300)
    )

    val fabShape: Shape = if (isMenuOpen) CircleShape else MaterialTheme.shapes.extraLarge

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 156.dp, end = 16.dp),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            displayActions.forEachIndexed { index, action ->
                AnimatedVisibility(
                    visible = isMenuOpen,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 300, delayMillis = index * 50)
                    ) + slideInVertically(
                        animationSpec = tween(durationMillis = 300, delayMillis = index * 50),
                        initialOffsetY = { it / 2 }
                    ),
                    exit = fadeOut(
                        animationSpec = tween(durationMillis = 150)
                    ) + slideOutVertically(
                        animationSpec = tween(durationMillis = 150),
                        targetOffsetY = { it / 2 }
                    )
                ) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            action.onClick()
                            isMenuOpen = false
                        },
                        text = { Text(text = action.label) },
                        icon = {
                            Icon(
                                imageVector = action.icon,
                                contentDescription = action.label
                            )
                        },
                        containerColor = FabRed,
                        contentColor = Color.White,
                        shape = ShapeDefaults.ExtraLarge
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { isMenuOpen = !isMenuOpen },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 75.dp, end = 16.dp),
            containerColor = fabContainerColor,
            contentColor = fabContentColor,
            shape = fabShape
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = if (isMenuOpen) "Close menu" else "Open menu",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}