package com.just_for_fun.synctax.presentation.components.card

import android.content.Intent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.just_for_fun.synctax.data.local.entities.OnlineListeningHistory
import com.just_for_fun.synctax.presentation.components.player.BottomOptionsDialog
import com.just_for_fun.synctax.presentation.components.player.DialogOption

@Composable
fun createOnlineHistoryOptions(
    history: OnlineListeningHistory,
    onPlay: () -> Unit,
    onAddToQueue: (() -> Unit)? = null,
    onShare: () -> Unit,
    onRemove: () -> Unit
): List<DialogOption> {
    return mutableListOf<DialogOption>().apply {
        // Play option
        add(
            DialogOption(
                id = "play",
                title = "Play Now",
                subtitle = "Stream this song",
                icon = {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = onPlay
            )
        )
        
        // Add to Queue option
        onAddToQueue?.let {
            add(
                DialogOption(
                    id = "add_to_queue",
                    title = "Add to Queue",
                    subtitle = "Add to end of current queue",
                    icon = {
                        Icon(
                            Icons.Default.QueueMusic,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = it
                )
            )
        }
        
        // Share option
        add(
            DialogOption(
                id = "share",
                title = "Share",
                subtitle = "Share YouTube Music link",
                icon = {
                    Icon(
                        Icons.Default.Share,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                onClick = onShare
            )
        )
        
        // Remove from History option
        add(
            DialogOption(
                id = "remove",
                title = "Remove from History",
                subtitle = "Remove this song from online history",
                icon = {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                },
                destructive = true,
                onClick = onRemove
            )
        )
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun OnlineHistoryCarousel(
    history: List<OnlineListeningHistory>,
    onHistoryClick: (OnlineListeningHistory) -> Unit,
    currentVideoId: String? = null,
    onRemoveFromHistory: ((OnlineListeningHistory) -> Unit)? = null,
    onAddToQueue: ((OnlineListeningHistory) -> Unit)? = null
) {
    if (history.isEmpty()) return

    val context = LocalContext.current
    val selectedHistory = remember { mutableStateOf<OnlineListeningHistory?>(null) }
    val showDialog = remember { mutableStateOf(false) }

    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { history.size },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 16.dp, bottom = 16.dp),
        preferredItemWidth = 180.dp,
        itemSpacing = 12.dp,
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) { i ->
        val item = history[i]
        val haptic = LocalHapticFeedback.current

        Column(
            modifier = Modifier
                .wrapContentWidth()
                .wrapContentHeight()
                .combinedClickable(
                    onClick = { onHistoryClick(item) },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        selectedHistory.value = item
                        showDialog.value = true
                    }
                )
        ) {
            Box(
                modifier = Modifier
//                    .size(140.dp)
                    .size(180.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (item.videoId == currentVideoId) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayCircle,
                            contentDescription = "Playing",
                            tint = Color.White,
                            modifier = Modifier.size(48.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = item.artist,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (selectedHistory.value != null) {
        BottomOptionsDialog(
            song = null,
            isVisible = showDialog.value,
            onDismiss = { showDialog.value = false },
            options = createOnlineHistoryOptions(
                history = selectedHistory.value!!,
                onPlay = {
                    onHistoryClick(selectedHistory.value!!)
                    showDialog.value = false
                },
                onAddToQueue = onAddToQueue?.let { callback ->
                    {
                        callback(selectedHistory.value!!)
                        showDialog.value = false
                    }
                },
                onShare = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "https://music.youtube.com/watch?v=${selectedHistory.value!!.videoId}")
                        putExtra(Intent.EXTRA_SUBJECT, "${selectedHistory.value!!.title} - ${selectedHistory.value!!.artist}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share song"))
                    showDialog.value = false
                },
                onRemove = {
                    onRemoveFromHistory?.invoke(selectedHistory.value!!)
                    showDialog.value = false
                }
            ),
            title = "History Options",
            description = "Manage this song in your listening history",
            songTitle = selectedHistory.value!!.title,
            songArtist = selectedHistory.value!!.artist,
            songThumbnail = selectedHistory.value!!.thumbnailUrl
        )
    }
}