package com.just_for_fun.synctax.presentation.components.player

/**
 * Constants for player sheet animations.
 * These values control the behavior of the animated player sheet transitions.
 */
object PlayerSheetConstants {
    /**
     * Height of the miniplayer in collapsed state (dp)
     */
    const val MINI_PLAYER_HEIGHT_DP = 80

    /**
     * Additional height added when expanding to fullscreen (dp)
     * Total expanded height = MINI_PLAYER_HEIGHT_DP + MAX_EXPANSION_HEIGHT_DP
     */
    const val MAX_EXPANSION_HEIGHT_DP = 920

    /**
     * Minimum blur radius for background in collapsed state (dp)
     */
    const val MIN_BLUR_RADIUS_DP = 20

    /**
     * Additional blur added when expanding (dp)
     * Total expanded blur = MIN_BLUR_RADIUS_DP + BLUR_RADIUS_EXPANSION_DP
     */
    const val BLUR_RADIUS_EXPANSION_DP = 30

    /**
     * Threshold in pixels for drag gestures to trigger state change
     */
    const val DRAG_THRESHOLD_PX = 200f

    /**
     * Duration of fade animations in milliseconds
     */
    const val FADE_DURATION_MS = 200

    /**
     * Delay before expanded content starts fading in (milliseconds)
     */
    const val FADE_IN_DELAY_MS = 100

    /**
     * Alpha threshold below which content is not rendered
     * Used to optimize performance by skipping composition of invisible content
     */
    const val ALPHA_RENDER_THRESHOLD = 0.01f

    /**
     * Minimum background opacity in collapsed state
     */
    const val MIN_BACKGROUND_OPACITY = 0.5f

    /**
     * Additional opacity added when expanding
     */
    const val BACKGROUND_OPACITY_EXPANSION = 0.2f

    /**
     * Minimum background scale in collapsed state
     */
    const val MIN_BACKGROUND_SCALE = 1.0f

    /**
     * Additional scale added when expanding for depth effect
     */
    const val BACKGROUND_SCALE_EXPANSION = 0.05f

    /**
     * Minimum gradient alpha (top) in collapsed state
     */
    const val MIN_GRADIENT_TOP_ALPHA = 0.2f

    /**
     * Additional gradient alpha (top) when expanding
     */
    const val GRADIENT_TOP_ALPHA_EXPANSION = 0.1f

    /**
     * Minimum gradient alpha (bottom) in collapsed state
     */
    const val MIN_GRADIENT_BOTTOM_ALPHA = 0.6f

    /**
     * Additional gradient alpha (bottom) when expanding
     */
    const val GRADIENT_BOTTOM_ALPHA_EXPANSION = 0.2f

    /**
     * Album art scale in collapsed state
     */
    const val ALBUM_ART_MINI_SCALE = 0.85f

    /**
     * Album art scale in expanded state
     */
    const val ALBUM_ART_EXPANDED_SCALE = 1.0f
}
