package com.just_for_fun.synctax.presentation.components.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp


// Fixed MarqueeText composable with proper overflow detection
@Composable
fun MarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    isPlaying: Boolean
) {
    var textWidth by remember { mutableStateOf(0f) }
    var containerWidth by remember { mutableStateOf(0f) }

    // Check if text should scroll
    val shouldScroll by remember(textWidth, containerWidth) {
        derivedStateOf { textWidth > containerWidth && containerWidth > 0 }
    }

    // Calculate animation duration based on text length
    val animationDuration = remember(textWidth) {
        if (textWidth > 0) {
            ((textWidth / 50) * 1000).toInt().coerceIn(4000, 20000)
        } else {
            6000
        }
    }

    // Animate scroll position
    val scrollOffset by animateFloatAsState(
        targetValue = if (isPlaying && shouldScroll) -(textWidth + 100f) else 0f,
        animationSpec = if (isPlaying && shouldScroll) {
            infiniteRepeatable(
                animation = tween(
                    durationMillis = animationDuration,
                    delayMillis = 2000,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(durationMillis = 300, easing = FastOutSlowInEasing)
        },
        label = "marqueeScroll"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                containerWidth = coordinates.size.width.toFloat()
            }
    ) {
        if (shouldScroll && isPlaying) {
            // Scrolling mode - show two copies for seamless loop
            Row(
                modifier = Modifier.offset(x = scrollOffset.dp)
            ) {
                Text(
                    text = text,
                    style = textStyle,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    softWrap = false,
                    modifier = Modifier.onGloballyPositioned { coordinates ->
                        textWidth = coordinates.size.width.toFloat()
                    }
                )
                Spacer(modifier = Modifier.width(100.dp))
                Text(
                    text = text,
                    style = textStyle,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    softWrap = false
                )
            }
        } else {
            // Static mode - show with ellipsis if too long
            Text(
                text = text,
                style = textStyle,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        textWidth = coordinates.size.width.toFloat()
                    }
            )
        }
    }
}

// Alternative: Simple Auto-Scrolling Marquee (Always scrolls when text is long)
@Composable
fun AlwaysScrollMarqueeText(
    text: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    isPlaying: Boolean
) {
    var textWidth by remember { mutableStateOf(0f) }
    var containerWidth by remember { mutableStateOf(0f) }

    val shouldScroll = textWidth > containerWidth && containerWidth > 0

    val animationDuration = remember(textWidth) {
        if (textWidth > 0) {
            ((textWidth / 40) * 1000).toInt().coerceIn(3000, 15000)
        } else {
            5000
        }
    }

    val scrollOffset by animateFloatAsState(
        targetValue = if (shouldScroll && isPlaying) -(textWidth + 80f) else 0f,
        animationSpec = if (shouldScroll && isPlaying) {
            infiniteRepeatable(
                animation = tween(
                    durationMillis = animationDuration,
                    delayMillis = 1500,
                    easing = LinearEasing
                ),
                repeatMode = RepeatMode.Restart
            )
        } else {
            tween(durationMillis = 200)
        },
        label = "scroll"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .onGloballyPositioned { containerWidth = it.size.width.toFloat() }
    ) {
        if (shouldScroll) {
            Row(modifier = Modifier.offset(x = scrollOffset.dp)) {
                Text(
                    text = text,
                    style = textStyle,
                    color = color,
                    maxLines = 1,
                    overflow = TextOverflow.Visible,
                    softWrap = false,
                    onTextLayout = { textLayoutResult ->
                        textWidth = textLayoutResult.size.width.toFloat()
                    }
                )
                if (isPlaying) {
                    Spacer(modifier = Modifier.width(80.dp))
                    Text(
                        text = text,
                        style = textStyle,
                        color = color,
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        softWrap = false
                    )
                }
            }
        } else {
            Text(
                text = text,
                style = textStyle,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
                onTextLayout = { textLayoutResult ->
                    textWidth = textLayoutResult.size.width.toFloat()
                }
            )
        }
    }
}
