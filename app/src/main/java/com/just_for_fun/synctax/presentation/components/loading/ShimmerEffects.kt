package com.just_for_fun.synctax.presentation.components.loading

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

/**
 * Shimmer effect modifier for loading placeholders
 */
@Composable
fun Modifier.shimmerEffect(): Modifier = composed {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    )

    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    background(
        brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim, translateAnim),
            end = Offset(translateAnim + 200f, translateAnim + 200f)
        )
    )
}

/**
 * Shimmer placeholder for song cards in lists
 */
@Composable
fun SongCardShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Album art placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmerEffect()
        )
        
        Spacer(modifier = Modifier.width(12.dp))
        
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Title placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.7f)
                    .height(16.dp)
                    .shimmerEffect()
            )
            // Artist placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .shimmerEffect()
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Duration placeholder
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(12.dp)
                .shimmerEffect()
        )
    }
}

/**
 * Shimmer placeholder for grid items (Quick Access, Speed Dial)
 */
@Composable
fun GridItemShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(120.dp)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Album art placeholder
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(RoundedCornerShape(8.dp))
                .shimmerEffect()
        )
        
        // Title placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp)
                .shimmerEffect()
        )
        
        // Artist placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp)
                .shimmerEffect()
        )
    }
}

/**
 * Shimmer placeholder for section headers
 */
@Composable
fun SectionHeaderShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Title placeholder
        Box(
            modifier = Modifier
                .width(150.dp)
                .height(20.dp)
                .shimmerEffect()
        )
        
        // View all button placeholder
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(16.dp)
                .shimmerEffect()
        )
    }
}
