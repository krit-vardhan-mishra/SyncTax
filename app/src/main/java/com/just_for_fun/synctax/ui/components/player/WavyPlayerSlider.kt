package com.just_for_fun.synctax.ui.components.player

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ir.mahozad.multiplatform.wavyslider.material3.WavySlider

@Composable
fun WavyPlayerSlider(
    position: Long,
    duration: Long,
    onSeek: (Long) -> Unit
) {
    val safeDuration = maxOf(1L, duration)
    val safePosition = position.coerceIn(0, safeDuration)

    var sliderPosition by remember(safePosition, safeDuration) {
        mutableFloatStateOf(safePosition.toFloat())
    }
    var isSeeking by remember { mutableStateOf(false) }

    LaunchedEffect(position, duration) {
        if (!isSeeking) {
            sliderPosition = position.coerceIn(0, safeDuration).toFloat()
        }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        WavySlider(
            value = sliderPosition,
            onValueChange = { newValue ->
                isSeeking = true
                sliderPosition = newValue
            },
            onValueChangeFinished = {
                onSeek(sliderPosition.toLong().coerceIn(0L, safeDuration))
                isSeeking = false
            },
            valueRange = 0f..safeDuration.toFloat(),
            enabled = duration > 0,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.onBackground,
                activeTrackColor = MaterialTheme.colorScheme.onBackground,
                inactiveTrackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

//        Row(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 4.dp),
//            horizontalArrangement = Arrangement.SpaceBetween
//        ) {
//            Text(
//                text = formatDuration(sliderPosition.toLong().coerceIn(0L, safeDuration)),
//                style = MaterialTheme.typography.labelSmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//            Text(
//                text = formatDuration(duration),
//                style = MaterialTheme.typography.labelSmall,
//                color = MaterialTheme.colorScheme.onSurfaceVariant
//            )
//        }
    }
}

private fun formatDuration(milliseconds: Long): String {
    if (milliseconds < 0) return "0:00"
    val seconds = (milliseconds / 1000).toInt()
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format("%d:%02d", minutes, remainingSeconds)
}
