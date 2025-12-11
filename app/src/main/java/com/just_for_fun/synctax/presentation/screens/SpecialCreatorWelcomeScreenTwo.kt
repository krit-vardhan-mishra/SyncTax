package com.just_for_fun.synctax.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import com.just_for_fun.synctax.R
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.just_for_fun.synctax.presentation.ui.theme.AppColors

// Custom overshoot easing
private val OvershootEasing: Easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1.0f)

// Montserrat font family (place font files in `app/src/main/res/font/`)
// Expected resource names: montserrat_light, montserrat_regular, montserrat_medium,
// montserrat_semibold, montserrat_bold, montserrat_extrabold, montserrat_italic
private val MontserratFamily = FontFamily(
    Font(R.font.montserrat_light, FontWeight.Light),
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold),
    Font(R.font.montserrat_extrabold, FontWeight.ExtraBold),
    Font(R.font.montserrat_italic, style = FontStyle.Italic)
)

@Composable
fun SpecialCreatorWelcomeScreenTwo(
    creatorName: String,
    onContinue: () -> Unit
) {
    var phase by remember { mutableIntStateOf(0) } // 0: confetti → 1: reveal → 2: message → 3: final glow

    // Theme-aware colors from AppColors
    val backgroundColor = AppColors.specialWelcomeBackground
    val textPrimaryColor = AppColors.specialWelcomeTitle
    val textSecondaryColor = AppColors.welcomeSubtitle
    val accentColor = AppColors.specialWelcomeAccent

    val goldGradient = Brush.linearGradient(
        colors = listOf(Color(0xFFFFD700), Color(0xFFFFC107), Color(0xFFFF8C00)),
        start = Offset.Zero,
        end = Offset.Infinite
    )

    LaunchedEffect(Unit) {
        delay(3500)  // Confetti
        phase = 1
        delay(1800)  // Name reveal
        phase = 2
        delay(3000)  // Thank you message
        phase = 3
        delay(2500)  // Final fade
        onContinue()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {

        // Golden Confetti Burst (Phase 0, centered on screen)
        if (phase == 0) {
            val confetti by rememberLottieComposition(LottieCompositionSpec.Asset("golden_confetti_burst.json"))
            val progress by animateLottieCompositionAsState(
                composition = confetti,
                iterations = 1
            )
            LottieAnimation(
                composition = confetti,
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .align(Alignment.Center), // Explicitly center the animation
                contentScale = ContentScale.FillHeight
            )
        }

        // Main Content Column (Text and Icon) - Centered on screen
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
                .padding(vertical = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {

            Spacer(modifier = Modifier.weight(0.3f))

            // Name Reveal (Phase 1+)
            AnimatedVisibility(
                visible = phase >= 1,
                enter = fadeIn(tween(2000)) + scaleIn(tween(2200, easing = OvershootEasing))
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Welcome home,",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = MontserratFamily,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 3.sp
                        ),
                        color = textPrimaryColor,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Golden gradient text
                    Text(
                        text = creatorName,
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = MontserratFamily,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            brush = goldGradient
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "The soul behind SyncTax",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = MontserratFamily,
                            fontStyle = FontStyle.Italic,
                            fontSize = 20.sp
                        ),
                        color = textSecondaryColor,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Personal Message (Phase 2+) - positioned below the Name Reveal block
            AnimatedVisibility(
                visible = phase >= 2,
                enter = fadeIn(tween(1500, delayMillis = 200)) + slideInVertically(
                    tween(
                        1500,
                        delayMillis = 200
                    )
                ) { it / 2 }
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val sparkle by rememberLottieComposition(LottieCompositionSpec.Asset("sparkle_burst.json"))

                    LottieAnimation(
                        sparkle,
                        iterations = 2,
                        modifier = Modifier.size(100.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "This app exists because of your vision.\nEvery beat. Every sync. Every moment.\nIt's all you.",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = MontserratFamily,
                            lineHeight = 30.sp,
                            fontSize = 20.sp
                        ),
                        color = textPrimaryColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "We're not just loading your world…\nWe're coming home.",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = MontserratFamily,
                            fontWeight = FontWeight.Medium,
                            color = accentColor,
                            fontSize = 24.sp,
                            lineHeight = 32.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))
        }

        // Final Fade to App
        AnimatedVisibility(
            visible = phase == 3,
            enter = fadeIn(tween(2000))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor.copy(alpha = 0.8f))
            )
        }
    }
}
