package com.just_for_fun.synctax.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SpecialCreatorWelcomeScreenTwo(
    creatorName: String,
    onContinue: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var showSurprise by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isVisible = true
        kotlinx.coroutines.delay(3000) // Wait for initial animation
        showSurprise = true
        kotlinx.coroutines.delay(5000) // Show surprise for 5 seconds
        onContinue()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Special background with golden accents
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Golden bubbles for special effect
            Box(
                modifier = Modifier
                    .size(450.dp)
                    .offset(x = (-120).dp, y = (-180).dp)
                    .background(
                        Color(0xFFFFD700).copy(alpha = 0.3f),
                        shape = RoundedCornerShape(60)
                    )
                    .blur(120.dp)
            )
            Box(
                modifier = Modifier
                    .size(400.dp)
                    .align(Alignment.BottomEnd)
                    .offset(x = 120.dp, y = 180.dp)
                    .background(
                        Color(0xFFFFA500).copy(alpha = 0.25f),
                        shape = RoundedCornerShape(60)
                    )
                    .blur(120.dp)
            )
            Box(
                modifier = Modifier
                    .size(350.dp)
                    .align(Alignment.CenterStart)
                    .offset(x = (-60).dp, y = 250.dp)
                    .background(
                        Color(0xFFFFD700).copy(alpha = 0.2f),
                        shape = RoundedCornerShape(60)
                    )
                    .blur(120.dp)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(1200)) +
                        slideInVertically(
                            animationSpec = tween(1200, easing = FastOutSlowInEasing),
                            initialOffsetY = { -it / 2 }
                        )
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 32.dp)
                ) {
                    // Crown icon effect with stars
                    Text(
                        text = "👑",
                        style = MaterialTheme.typography.displayLarge,
                        fontSize = 120.sp
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "Welcome Home,",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontSize = 42.sp,
                            fontWeight = FontWeight.ExtraBold
                        ),
                        color = Color(0xFFFFD700),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = creatorName,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Creator of SyncTax",
                        style = MaterialTheme.typography.titleLarge,
                        color = Color(0xFFFFD700).copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    // Surprise animation
                    AnimatedVisibility(
                        visible = showSurprise,
                        enter = fadeIn(animationSpec = tween(800)) +
                                slideInVertically(
                                    animationSpec = tween(800, easing = FastOutSlowInEasing),
                                    initialOffsetY = { it / 3 }
                                )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "🎉 ✨ 🎵 ✨ 🎉",
                                style = MaterialTheme.typography.displaySmall,
                                fontSize = 48.sp,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Thank you for building\nthis amazing music experience!",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = Color.White.copy(alpha = 0.9f),
                                textAlign = TextAlign.Center,
                                lineHeight = 30.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Text(
                                text = "Entering your creation...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color(0xFFFFD700).copy(alpha = 0.7f),
                                textAlign = TextAlign.Center,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}