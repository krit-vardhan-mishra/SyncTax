package com.just_for_fun.synctax.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.just_for_fun.synctax.presentation.ui.theme.AppColors
import kotlinx.coroutines.delay

@Composable
fun SpecialCreatorWelcomeScreenTwo(
    creatorName: String,
    onContinue: () -> Unit
) {
    // Theme-aware colors from AppColors
    val backgroundColor = AppColors.specialWelcomeBackground

    LaunchedEffect(Unit) {
        delay(3500)
        onContinue()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        val confetti by rememberLottieComposition(LottieCompositionSpec.Asset("golden_confetti_burst.json"))
        val progress by animateLottieCompositionAsState(
            composition = confetti,
            iterations = 1
        )
        LottieAnimation(
            composition = confetti,
            progress = { progress },
            modifier = Modifier
                .fillMaxSize()
                .align(Alignment.Center),
            contentScale = ContentScale.FillHeight
        )
    }
}
