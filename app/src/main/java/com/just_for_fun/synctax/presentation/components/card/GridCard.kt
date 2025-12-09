package com.just_for_fun.synctax.presentation.components.card

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun GridCard(
    imageUri: String? = null,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Main Container Box
    Box(
        modifier = modifier
            .fillMaxWidth()
            // Set a fixed height to make it look like a card.
            // You can adjust this (e.g., 200.dp) or use aspect ratio modifiers if preferred.
            .height(180.dp)
            .padding(4.dp) // Outer padding for spacing between grid items
            .clip(RoundedCornerShape(20.dp)) // Heavily rounded corners like the screenshot
            .clickable(onClick = onClick)
    ) {
        // 1. Background Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUri)
                .crossfade(true)
                .build(),
            contentDescription = "Image for $title",
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )

        // 2. Gradient Overlay (Shadow)
        // This ensures text is readable even on light images
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.8f)
                        ),
                        startY = 100f // Start gradient partially down the image
                    )
                )
        )

        // 3. Text Content Layered on Top (Bottom Left)
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp) // Inner padding for text
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                // Using a slightly transparent white for the subtitle
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}