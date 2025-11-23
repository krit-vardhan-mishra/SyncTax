package com.just_for_fun.synctax.ui.components.player

import android.media.AudioManager
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.VolumeDown
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.core.data.local.entities.Song
import androidx.compose.material.icons.filled.VolumeOff

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplePlayerMenu(
    song: Song,
    volume: Float,
    onVolumeChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val audioManager = remember {
        context.getSystemService(AudioManager::class.java)
            ?: throw IllegalStateException("AudioManager unavailable")
    }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC) }
    var currentVolume by remember { mutableStateOf(volume.coerceIn(0f..1f)) }

    // Initialize current volume from device
    LaunchedEffect(Unit) {
        val deviceVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        currentVolume = deviceVolume.toFloat() / maxVolume
        onVolumeChange(currentVolume)
    }

    // Observe system volume changes so the slider stays in sync with hardware buttons
    val contentResolver = context.contentResolver
    val handler = remember { Handler(Looper.getMainLooper()) }
    val haptic = LocalHapticFeedback.current
    val volumeObserver = remember {
        object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                val deviceVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                val newVol = deviceVolume.toFloat() / maxVolume
                currentVolume = newVol
                onVolumeChange(newVol)
            }
        }
    }

    DisposableEffect(Unit) {
        contentResolver.registerContentObserver(Settings.System.CONTENT_URI, true, volumeObserver)
        onDispose {
            contentResolver.unregisterContentObserver(volumeObserver)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Volume Control Section
            Text(
                text = "Volume",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                // Left icon: decrease volume / show mute
                IconButton(onClick = {
                    val curInt = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val newInt = (curInt - 1).coerceAtLeast(0)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newInt, 0)
                    val newVol = newInt.toFloat() / maxVolume
                    currentVolume = newVol
                    onVolumeChange(newVol)
                    // Haptic feedback for decrease
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) { }
                }) {
                    val leftIcon = when {
                        currentVolume <= 0f -> Icons.Default.VolumeOff
                        currentVolume < 0.5f -> Icons.Default.VolumeDown
                        else -> Icons.Default.VolumeDown
                    }
                    Icon(
                        imageVector = leftIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Slider(
                    value = currentVolume,
                    onValueChange = { newVol ->
                        currentVolume = newVol
                        val volInt = (newVol * maxVolume).toInt()
                        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volInt, 0)
                        onVolumeChange(newVol)
                    },
                    onValueChangeFinished = {
                        // provide light haptic when user finishes adjusting slider
                        try {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        } catch (_: Exception) { }
                    },
                    valueRange = 0f..1f,
                    modifier = Modifier.weight(1f)
                )

                // Right icon: increase volume
                IconButton(onClick = {
                    val curInt = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
                    val newInt = (curInt + 1).coerceAtMost(maxVolume)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newInt, 0)
                    val newVol = newInt.toFloat() / maxVolume
                    currentVolume = newVol
                    onVolumeChange(newVol)
                    // Haptic feedback for increase
                    try {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    } catch (_: Exception) { }
                }) {
                    val rightIcon = when {
                        currentVolume <= 0f -> Icons.Default.VolumeOff
                        currentVolume < 0.5f -> Icons.Default.VolumeDown
                        else -> Icons.Default.VolumeUp
                    }
                    Icon(
                        imageVector = rightIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Song Details Section
            Text(
                text = "About Song",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Song Title
                DetailRow(
                    label = "Title",
                    value = song.title
                )

                // Artist
                DetailRow(
                    label = "Artist",
                    value = song.artist
                )

                // Album (if available)
                if (!song.album.isNullOrEmpty()) {
                    DetailRow(
                        label = "Album",
                        value = song.album!!
                    )
                }

                // Duration (if available)
                if (song.duration > 0) {
                    DetailRow(
                        label = "Duration",
                        value = formatDuration(song.duration)
                    )
                }

                // Genre (if available)
                if (!song.genre.isNullOrEmpty()) {
                    DetailRow(
                        label = "Genre",
                        value = song.genre!!
                    )
                }

                // Release Year (if available)
                song.releaseYear?.let { year ->
                    DetailRow(
                        label = "Year",
                        value = year.toString()
                    )
                }

                // File Path
                DetailRow(
                    label = "File Path",
                    value = song.filePath,
                    isPath = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    if (milliseconds < 0) return "0:00"
    val seconds = (milliseconds / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}