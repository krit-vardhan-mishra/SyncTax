package com.just_for_fun.synctax.presentation.components.state

/**
 * Represents the state of the player sheet.
 * Used for managing the animated transition between miniplayer and fullscreen modes.
 */
enum class PlayerSheetState {
    /**
     * Player is in collapsed/miniplayer mode at the bottom of the screen
     */
    COLLAPSED,
    
    /**
     * Player is in expanded/fullscreen mode taking up the entire screen
     */
    EXPANDED
}
