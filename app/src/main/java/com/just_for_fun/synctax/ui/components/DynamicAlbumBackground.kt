package com.just_for_fun.synctax.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.just_for_fun.synctax.ui.utils.AlbumColors

@Composable
fun DynamicAlbumBackground(
    albumColors: AlbumColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        albumColors.lightVibrant,
                        albumColors.muted,
                        albumColors.darkVibrant.copy(alpha = 0.05f)
                    ),
                    startY = 0f,
                    endY = 1800f
                )
            )
    ) {
        content()
    }
}


@Composable
fun DynamicRadialBackground(
    albumColors: AlbumColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        albumColors.vibrant,
                        albumColors.muted.copy(alpha = 0.08f),
                        albumColors.darkVibrant.copy(alpha = 0.03f)
                    ),
                    radius = 1500f
                )
            )
    ) {
        content()
    }
}


@Composable
fun DynamicHorizontalBackground(
    albumColors: AlbumColors,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .background(
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        albumColors.vibrant.copy(alpha = 0.12f),
                        albumColors.dominant.copy(alpha = 0.08f),
                        albumColors.muted.copy(alpha = 0.05f)
                    )
                )
            )
    ) {
        content()
    }
}


@Composable
fun DynamicGreetingSection(
    userName: String,
    albumColors: AlbumColors
) {
    val greeting = remember {
        val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
        when (hour) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..22 -> "Good evening"
            else -> "Hey there, burning the midnight oil?"
        }
    }

    val subGreeting = remember {
        when (greeting) {
            "Good morning" -> "Hope your day starts great!"
            "Good afternoon" -> "Keep going strong!"
            "Good evening" -> "Hope you had a good day so far!"
            else -> "Don't forget to rest when you can."
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = albumColors.vibrant.copy(alpha = 0.25f) // Dynamic color from album
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            val annotatedString = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                ) {
                    append("$greeting, ")
                }
                withStyle(
                    style = SpanStyle(
                        fontSize = MaterialTheme.typography.headlineLarge.fontSize,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                ) {
                    append(userName)
                }
            }

            Text(
                text = annotatedString,
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subGreeting,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun DynamicSectionBackground(
    albumColors: AlbumColors,
    modifier: Modifier = Modifier,
    useAccent: Boolean = false,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier,
        color = if (useAccent) {
            albumColors.dominant.copy(alpha = 0.15f)
        } else {
            albumColors.muted.copy(alpha = 0.08f)
        },
        shape = MaterialTheme.shapes.medium
    ) {
        content()
    }
}
