package com.just_for_fun.synctax.presentation.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min

/**
 * Reference design dimensions - based on a standard Android phone.
 * UI elements will scale proportionally relative to these values.
 */
object ReferenceDesign {
    const val WIDTH_DP = 360f  // Standard phone width
    const val HEIGHT_DP = 800f // Standard phone height
    
    // Maximum scale factors to prevent excessive scaling on tablets
    const val MAX_SCALE = 1.3f
    const val MIN_SCALE = 0.85f
}

/**
 * Data class holding scale factors for different dimensions.
 */
data class Scaling(
    /** Scale factor for horizontal dimensions (width-based) */
    val widthScale: Float = 1f,
    
    /** Scale factor for vertical dimensions (height-based) */
    val heightScale: Float = 1f,
    
    /** Average scale factor for general sizing (icons, cards, etc.) */
    val scale: Float = 1f,
    
    /** Scale factor for typography, includes user font preference */
    val fontScale: Float = 1f,
    
    /** The actual screen width in dp */
    val screenWidthDp: Float = ReferenceDesign.WIDTH_DP,
    
    /** The actual screen height in dp */
    val screenHeightDp: Float = ReferenceDesign.HEIGHT_DP
) {
    /** Check if this is a compact (small phone) screen */
    val isCompact: Boolean get() = screenWidthDp < 360f
    
    /** Check if this is an expanded (tablet) screen */
    val isExpanded: Boolean get() = screenWidthDp >= 600f
    
    /** Check if this is a medium (standard phone) screen */
    val isMedium: Boolean get() = !isCompact && !isExpanded
}

/**
 * CompositionLocal for accessing scaling values throughout the app.
 * Default value assumes standard phone dimensions.
 */
val LocalScaling = compositionLocalOf { Scaling() }

/**
 * Provides scaling values based on current device configuration.
 * Wrap your root composable with this to enable scaling throughout the app.
 * 
 * @param content The composable content that will have access to scaling values
 */
@Composable
fun ScalingProvider(content: @Composable () -> Unit) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    
    val scaling = remember(configuration.screenWidthDp, configuration.screenHeightDp, configuration.fontScale) {
        val screenWidth = configuration.screenWidthDp.toFloat()
        val screenHeight = configuration.screenHeightDp.toFloat()
        val userFontScale = configuration.fontScale
        
        // Calculate raw scale factors
        val rawWidthScale = screenWidth / ReferenceDesign.WIDTH_DP
        val rawHeightScale = screenHeight / ReferenceDesign.HEIGHT_DP
        
        // Clamp scale factors to prevent extreme scaling
        val widthScale = rawWidthScale.coerceIn(ReferenceDesign.MIN_SCALE, ReferenceDesign.MAX_SCALE)
        val heightScale = rawHeightScale.coerceIn(ReferenceDesign.MIN_SCALE, ReferenceDesign.MAX_SCALE)
        
        // Use the smaller scale to maintain proportions
        val avgScale = min(widthScale, heightScale)
        
        // Font scale combines our scaling with user preference
        // We use a reduced factor for fonts to prevent them from becoming too large
        val fontScaleFactor = (avgScale * 0.7f + 0.3f) // Blend towards 1.0 for subtler scaling
        val finalFontScale = fontScaleFactor * userFontScale
        
        Scaling(
            widthScale = widthScale,
            heightScale = heightScale,
            scale = avgScale,
            fontScale = finalFontScale.coerceIn(0.8f, 1.4f), // Further constrain font scale
            screenWidthDp = screenWidth,
            screenHeightDp = screenHeight
        )
    }
    
    CompositionLocalProvider(LocalScaling provides scaling) {
        content()
    }
}

// =============================================================================
// Extension functions for easy scaling
// =============================================================================

/**
 * Scales a Dp value based on the current screen size.
 * Use this for dimensions that should grow/shrink with screen size.
 */
@Composable
fun Dp.scaled(): Dp {
    val scaling = LocalScaling.current
    return (this.value * scaling.scale).dp
}

/**
 * Scales a Dp value based on width only.
 * Use this for horizontal-specific dimensions like padding.
 */
@Composable
fun Dp.scaledWidth(): Dp {
    val scaling = LocalScaling.current
    return (this.value * scaling.widthScale).dp
}

/**
 * Scales a Dp value based on height only.
 * Use this for vertical-specific dimensions.
 */
@Composable
fun Dp.scaledHeight(): Dp {
    val scaling = LocalScaling.current
    return (this.value * scaling.heightScale).dp
}

/**
 * Scales a TextUnit (sp) value based on the current screen size and user font preference.
 * Use this for text sizes that should adapt to screen size.
 */
@Composable
fun TextUnit.scaled(): TextUnit {
    val scaling = LocalScaling.current
    return (this.value * scaling.fontScale).sp
}

// =============================================================================
// Scaled dimension accessors
// =============================================================================

/**
 * Provides commonly used scaled dimensions.
 * Access via LocalScaledDimensions.current
 */
data class ScaledDimensions(
    val navigationBarHeight: Dp,
    val miniPlayerHeight: Dp,
    val appBarHeight: Dp,
    val listItemHeight: Dp,
    val listThumbnailSize: Dp,
    val gridThumbnailHeight: Dp,
    val albumThumbnailSize: Dp,
    val thumbnailCornerRadius: Dp,
    val albumCornerRadius: Dp,
    val playerHorizontalPadding: Dp,
    val smallPadding: Dp,
    val mediumPadding: Dp,
    val largePadding: Dp
)

/**
 * Creates scaled dimensions based on current scaling factors.
 */
@Composable
fun rememberScaledDimensions(): ScaledDimensions {
    val scaling = LocalScaling.current
    
    return remember(scaling) {
        ScaledDimensions(
            navigationBarHeight = (80f * scaling.scale).dp,
            miniPlayerHeight = (64f * scaling.scale).dp,
            appBarHeight = (64f * scaling.scale).dp,
            listItemHeight = (64f * scaling.scale).dp,
            listThumbnailSize = (48f * scaling.scale).dp,
            gridThumbnailHeight = (96f * scaling.scale).dp,
            albumThumbnailSize = (144f * scaling.scale).dp,
            thumbnailCornerRadius = (6f * scaling.scale).dp,
            albumCornerRadius = (16f * scaling.scale).dp,
            playerHorizontalPadding = (32f * scaling.widthScale).dp,
            smallPadding = (8f * scaling.scale).dp,
            mediumPadding = (16f * scaling.scale).dp,
            largePadding = (24f * scaling.scale).dp
        )
    }
}

/**
 * CompositionLocal for scaled dimensions.
 */
val LocalScaledDimensions = compositionLocalOf { 
    ScaledDimensions(
        navigationBarHeight = 80.dp,
        miniPlayerHeight = 64.dp,
        appBarHeight = 64.dp,
        listItemHeight = 64.dp,
        listThumbnailSize = 48.dp,
        gridThumbnailHeight = 96.dp,
        albumThumbnailSize = 144.dp,
        thumbnailCornerRadius = 6.dp,
        albumCornerRadius = 16.dp,
        playerHorizontalPadding = 32.dp,
        smallPadding = 8.dp,
        mediumPadding = 16.dp,
        largePadding = 24.dp
    )
}

/**
 * Provider that sets up both scaling and scaled dimensions.
 */
@Composable
fun ResponsiveScalingProvider(content: @Composable () -> Unit) {
    ScalingProvider {
        val scaledDimensions = rememberScaledDimensions()
        CompositionLocalProvider(LocalScaledDimensions provides scaledDimensions) {
            content()
        }
    }
}
