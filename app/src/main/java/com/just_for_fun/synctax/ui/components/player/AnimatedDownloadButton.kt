package com.just_for_fun.synctax.ui.components.player

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedDownloadButton(
    isDownloading: Boolean,
    isDownloaded: Boolean,
    downloadProgress: Float = 0f,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Colors
    val idleColor = Color(0xFFE53935) // Red
    val completeColor = Color(0xFF4CAF50) // Green

    // 1. Animate the progress value
    val animatedProgress by animateFloatAsState(
        targetValue = if (isDownloading) downloadProgress else if (isDownloaded) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "download_progress"
    )

    // 2. Get the vector painter for the Download icon
    val iconPainter = rememberVectorPainter(image = Icons.Default.Download)

    Box(
        modifier = modifier
            .size(48.dp) // Standard touch target size
            .clip(CircleShape) // Ripple stays circular
            .clickable(
                enabled = !isDownloaded, // Allow clicking when downloading to cancel, but not when already downloaded
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        // We use a Canvas of size 24.dp (standard icon size) to draw the icon manually
        Canvas(modifier = Modifier.size(24.dp)) {

            // Step A: Draw the "Idle" Red Icon (The Background Layer)
            // We use `with(iconPainter)` to draw the vector into the Canvas
            with(iconPainter) {
                draw(
                    size = size,
                    colorFilter = ColorFilter.tint(idleColor)
                )
            }

            // Step B: Draw the "Complete" Green Icon (The Foreground Layer)
            // We only draw this if there is progress > 0
            if (animatedProgress > 0f) {
                val fillHeight = size.height * animatedProgress

                // Clip the drawing area.
                // We define a rectangle from the bottom moving upwards.
                clipRect(
                    left = 0f,
                    top = size.height - fillHeight,
                    right = size.width,
                    bottom = size.height
                ) {
                    with(iconPainter) {
                        draw(
                            size = size,
                            colorFilter = ColorFilter.tint(completeColor)
                        )
                    }
                }
            }
        }
    }
}