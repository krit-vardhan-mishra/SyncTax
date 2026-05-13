package com.just_for_fun.synctax.presentation.components.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.data.local.entities.Song
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import kotlinx.coroutines.launch

private const val MINI_PLAYER_HEIGHT_DP = 80f

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PartyPlayer(
    song: Song?,
    isPlaying: Boolean,
    position: Long,
    duration: Long,
    isHost: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val miniPlayerHeightPx = with(density) { MINI_PLAYER_HEIGHT_DP.dp.toPx() }

        // Additional padding for mini player to float above bottom nav if needed, 
        // but typically in PartySession it's at the very bottom
        val collapsedOffset = screenHeightPx - miniPlayerHeightPx - with(density) { 16.dp.toPx() } // 16dp for floating margin
        val expandedOffset = 0f

        val anchors = remember(screenHeightPx) {
            DraggableAnchors {
                PlayerState.Collapsed at collapsedOffset
                PlayerState.Expanded at expandedOffset
            }
        }

        val anchoredDraggableState = remember {
            AnchoredDraggableState(
                initialValue = PlayerState.Collapsed,
                positionalThreshold = { distance -> distance * 0.3f },
                velocityThreshold = { with(density) { 200.dp.toPx() } },
                snapAnimationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMedium
                ),
                decayAnimationSpec = exponentialDecay()
            )
        }

        LaunchedEffect(anchors) {
            anchoredDraggableState.updateAnchors(anchors)
        }

        val progress by remember {
            derivedStateOf {
                val offset = if (anchoredDraggableState.offset.isNaN()) collapsedOffset else anchoredDraggableState.offset
                if (collapsedOffset == expandedOffset) 0f
                else 1f - (offset / collapsedOffset).coerceIn(0f, 1f)
            }
        }

        val miniControlsAlpha = (1f - (progress / 0.15f)).coerceIn(0f, 1f)
        val fullControlsAlpha = ((progress - 0.85f) / 0.15f).coerceIn(0f, 1f)
        val scrimAlpha = progress * 0.6f

        val bottomPadding = if (song != null) (MINI_PLAYER_HEIGHT_DP + 16).dp else 0.dp

        // --- CONTENT LAYER ---
        Box(modifier = Modifier.fillMaxSize()) {
            content(PaddingValues(bottom = bottomPadding))
        }

        if (song != null) {
            // --- SCRIM OVERLAY ---
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .alpha(scrimAlpha)
                    .background(Color.Black)
            )

            // --- DRAGGABLE PLAYER ---
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset {
                        val safeOffset = if (anchoredDraggableState.offset.isNaN()) collapsedOffset else anchoredDraggableState.offset
                        IntOffset(0, safeOffset.toInt())
                    }
                    .height(maxHeight)
                    .anchoredDraggable(
                        state = anchoredDraggableState,
                        orientation = Orientation.Vertical
                    ),
                color = Color.Transparent
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Full Background for expanded state (Dark with blur effect)
                    if (progress > 0.01f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(progress)
                                .background(Color(0xFF0A0A0A))
                        ) {
                            // Subtle background glow based on Stitch design
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .alpha(0.4f)
                                    .background(
                                        Brush.radialGradient(
                                            colors = listOf(
                                                AppColors.accentPrimary.copy(alpha = 0.3f),
                                                Color.Transparent
                                            ),
                                            radius = with(density) { 300.dp.toPx() }
                                        )
                                    )
                            )
                        }
                    }

                    // --- MINI PLAYER (Stitch Design) ---
                    if (miniControlsAlpha > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(MINI_PLAYER_HEIGHT_DP.dp)
                                .padding(horizontal = 16.dp)
                                .alpha(miniControlsAlpha)
                                .shadow(8.dp, RoundedCornerShape(24.dp))
                                .clip(RoundedCornerShape(24.dp))
                                .background(Color(0x991A1A1A)) // glass-card
                                .clickable {
                                    coroutineScope.launch {
                                        anchoredDraggableState.animateTo(PlayerState.Expanded)
                                    }
                                }
                        ) {
                            // Progress Bar at bottom of mini player
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .align(Alignment.BottomCenter)
                                    .background(Color.White.copy(alpha = 0.1f))
                            ) {
                                val progressFraction = if (duration > 0) (position.toFloat() / duration).coerceIn(0f, 1f) else 0f
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(progressFraction)
                                        .background(AppColors.accentPrimary)
                                        .shadow(8.dp, spotColor = AppColors.accentPrimary)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Mini Album Art
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color.DarkGray)
                                    ) {
                                        if (song.albumArtUri.isNullOrEmpty()) {
                                            Icon(Icons.Default.MusicNote, null, modifier = Modifier.align(Alignment.Center))
                                        } else {
                                            AsyncImage(
                                                model = song.albumArtUri,
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = if (isHost) "BROADCASTING" else "SYNCED",
                                            color = AppColors.accentPrimary,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            letterSpacing = 1.sp
                                        )
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    // Play/Pause Button
                                    Surface(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .clickable { if (isHost) onPlayPause() },
                                        color = if (isHost) AppColors.accentPrimary else Color.White.copy(alpha = 0.1f)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- FULL SCREEN PLAYER (Stitch Admin Design) ---
                    if (fullControlsAlpha > 0f) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(fullControlsAlpha)
                                .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Top Drag Handle & Title
                            Box(
                                modifier = Modifier
                                    .width(40.dp)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(Color.White.copy(alpha = 0.2f))
                                    .padding(bottom = 24.dp)
                            )
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            // Role Indicator (Stitch styling)
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = AppColors.accentPrimary.copy(alpha = 0.2f),
                                border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.accentPrimary.copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = AppColors.accentPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isHost) "Live Party: Hosting" else "Live Party: Listening",
                                        color = AppColors.accentPrimary,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Album Art
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .shadow(40.dp, RoundedCornerShape(24.dp), spotColor = AppColors.accentPrimary)
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(AppColors.cardBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                if (song.albumArtUri.isNullOrEmpty()) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(80.dp),
                                        tint = Color.White.copy(alpha = 0.5f)
                                    )
                                } else {
                                    AsyncImage(
                                        model = song.albumArtUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(32.dp))

                            // Info and Controls Panel
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = Color(0x992B1C18), // surface-container
                                shape = RoundedCornerShape(24.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Column(modifier = Modifier.padding(24.dp)) {
                                    // Song Info
                                    Text(
                                        text = song.title,
                                        color = Color.White,
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.basicMarquee()
                                    )
                                    Text(
                                        text = song.artist ?: "Unknown Artist",
                                        color = AppColors.textBody,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )

                                    Spacer(modifier = Modifier.height(24.dp))

                                    // Progress Slider
                                    PlayerSlider(
                                        position = position,
                                        duration = duration,
                                        onSeek = { if (isHost) onSeek(it) }
                                    )

                                    Spacer(modifier = Modifier.height(16.dp))

                                    // Controls
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Surface(
                                            modifier = Modifier
                                                .size(64.dp)
                                                .clip(CircleShape)
                                                .clickable { if (isHost) onPlayPause() },
                                            color = if (isHost) AppColors.accentPrimary else Color.White.copy(alpha = 0.1f),
                                            shadowElevation = if (isHost) 8.dp else 0.dp
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Crossfade(targetState = isPlaying, label = "party_play_pause") { playing ->
                                                    Icon(
                                                        imageVector = if (playing) Icons.Default.Pause else Icons.Default.PlayArrow,
                                                        contentDescription = "Play/Pause",
                                                        tint = Color.White,
                                                        modifier = Modifier.size(32.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
