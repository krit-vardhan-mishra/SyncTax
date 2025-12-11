package com.just_for_fun.synctax.presentation.components.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.delay

/**
 * Composable wrapper that adds long-press tooltip functionality to any content
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    onClick: () -> Unit,
    tooltipText: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val tooltipState = rememberTooltipState(isPersistent = true)

    // Auto-hide tooltip after 2 seconds
    LaunchedEffect(showTooltip) {
        if (showTooltip) {
            // Manually show the tooltip when showTooltip state changes
            tooltipState.show()
            delay(2000)
            tooltipState.dismiss()
            showTooltip = false
        }
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(tooltipText)
            }
        },
        state = tooltipState,
        enableUserInput = false // Prevent default long-press from TooltipBox
    ) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            // Use combinedClickable to intercept the long-press gesture
            modifier = modifier.combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showTooltip = true
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
        ) {
            content()
        }
    }
}

/**
 * Extension for regular buttons with long-press tooltip
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TooltipButton(
    onClick: () -> Unit,
    tooltipText: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    var showTooltip by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val tooltipState = rememberTooltipState(isPersistent = true)

    LaunchedEffect(showTooltip) {
        if (showTooltip) {
            tooltipState.show()
            delay(2000)
            tooltipState.dismiss()
            showTooltip = false
        }
    }

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(tooltipText)
            }
        },
        state = tooltipState,
        enableUserInput = false // Prevent default long-press from TooltipBox
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = modifier.combinedClickable(
                onClick = onClick,
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    showTooltip = true
                },
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            )
        ) {
            content()
        }
    }
}
