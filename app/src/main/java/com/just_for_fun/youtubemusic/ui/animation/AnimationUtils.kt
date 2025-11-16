package com.just_for_fun.youtubemusic.ui.animation

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object Animations {
    val shortTween = tween<Float>(durationMillis = 120, easing = FastOutSlowInEasing)
    val mediumTween = tween<Float>(durationMillis = 240, easing = FastOutSlowInEasing)
    val longTween = tween<Float>(durationMillis = 360, easing = FastOutSlowInEasing)

    fun mediumSpring() = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
}
