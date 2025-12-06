package com.just_for_fun.synctax.presentation.components.loading

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Skeleton loading components following OuterTune/SimpMusic patterns
 * Uses shimmer effect for better UX
 */

@Composable
fun ShimmerBrush(
    shimmerColors: List<Color> = listOf(
        Color.LightGray.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.2f),
        Color.LightGray.copy(alpha = 0.6f)
    )
): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )

    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 200f, translateAnim - 200f),
        end = Offset(translateAnim, translateAnim)
    )
}

/**
 * Skeleton loader for song cards
 * Matches SimpleSongCard dimensions
 */
@Composable
fun SkeletonSongCard(
    modifier: Modifier = Modifier
) {
    val shimmerBrush = ShimmerBrush()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(72.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Album art skeleton
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Text content skeleton
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Title skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                
                // Artist skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
            
            // Duration skeleton
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(shimmerBrush)
            )
        }
    }
}

/**
 * Skeleton loader for album cards
 * Matches album card dimensions
 */
@Composable
fun SkeletonAlbumCard(
    modifier: Modifier = Modifier
) {
    val shimmerBrush = ShimmerBrush()
    
    Column(
        modifier = modifier.width(140.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Album art skeleton
        Box(
            modifier = Modifier
                .size(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(shimmerBrush)
        )
        
        // Title skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        
        // Artist skeleton
        Box(
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
    }
}

/**
 * Skeleton loader for playlist cards
 */
@Composable
fun SkeletonPlaylistCard(
    modifier: Modifier = Modifier
) {
    val shimmerBrush = ShimmerBrush()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Playlist thumbnail skeleton
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(shimmerBrush)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Playlist name skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(16.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                
                // Song count skeleton
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
        }
    }
}

/**
 * Skeleton for section headers
 */
@Composable
fun SkeletonSectionHeader(
    modifier: Modifier = Modifier
) {
    val shimmerBrush = ShimmerBrush()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        
        Box(
            modifier = Modifier
                .width(60.dp)
                .height(16.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
    }
}

/**
 * Skeleton for quick picks section (horizontal scroll)
 */
@Composable
fun SkeletonQuickPicksSection(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        SkeletonSectionHeader()
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            repeat(3) {
                SkeletonAlbumCard()
            }
        }
    }
}

/**
 * Skeleton for artist grid item
 */
@Composable
fun SkeletonArtistCard(
    modifier: Modifier = Modifier
) {
    val shimmerBrush = ShimmerBrush()
    
    Column(
        modifier = modifier.width(120.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Artist image (circular)
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(shimmerBrush)
        )
        
        // Artist name
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
    }
}

/**
 * Full screen skeleton for initial load
 */
@Composable
fun SkeletonHomeScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Quick Picks section
        SkeletonQuickPicksSection()
        
        // Song list
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SkeletonSectionHeader()
            repeat(5) {
                SkeletonSongCard()
            }
        }
    }
}

/**
 * Skeleton for library grid
 */
@Composable
fun SkeletonLibraryGrid(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(3) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                repeat(2) {
                    SkeletonAlbumCard(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
