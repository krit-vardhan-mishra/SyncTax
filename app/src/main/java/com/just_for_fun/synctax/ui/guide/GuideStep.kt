package com.just_for_fun.synctax.ui.guide

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Represents a single step in the onboarding guide
 */
data class GuideStep(
    val title: String,
    val description: String,
    val targetPosition: Offset? = null,
    val highlightRadius: Dp = 60.dp,
    val arrowDirection: ArrowDirection = ArrowDirection.UP
)

enum class ArrowDirection {
    UP, DOWN, LEFT, RIGHT
}

/**
 * Predefined guides for different screens
 */
object GuideContent {
    
    val homeScreenGuide = listOf(
        GuideStep(
            title = "Welcome to Home",
            description = "This is your personalized music hub. Here you'll find quick picks, your recent songs, and all your music collection."
        ),
        GuideStep(
            title = "Quick Picks Section",
            description = "AI-powered song recommendations based on your listening habits. The more you listen, the better the suggestions!"
        ),
        GuideStep(
            title = "Mini Player",
            description = "Swipe left or right to change songs quickly. Swipe up to open the full-screen player with more controls."
        ),
        GuideStep(
            title = "All Songs",
            description = "Browse your entire music library. Use the sort button to organize by title, artist, or date added."
        )
    )
    
    val playerScreenGuide = listOf(
        GuideStep(
            title = "Full Screen Player",
            description = "Your complete playback control center with album art and lyrics."
        ),
        GuideStep(
            title = "Album Art Controls",
            description = "Double-tap the LEFT side to seek backward 10 seconds. Double-tap the RIGHT side to seek forward 10 seconds. Tap the CENTER to play/pause."
        ),
        GuideStep(
            title = "Queue & Controls",
            description = "Access your play queue, shuffle, and repeat modes. Manage what plays next with the Up Next section."
        ),
        GuideStep(
            title = "Swipe Down",
            description = "Swipe down from anywhere to return to the mini player and browse your library."
        )
    )
    
    val searchScreenGuide = listOf(
        GuideStep(
            title = "Search Your Music",
            description = "Search for songs, artists, or albums in your local library."
        ),
        GuideStep(
            title = "Online Search",
            description = "If a song isn't found locally, we'll automatically search online sources like YouTube to help you find it."
        ),
        GuideStep(
            title = "Quick Results",
            description = "Results appear as you type. Tap any song to start playing immediately."
        )
    )
    
    val libraryScreenGuide = listOf(
        GuideStep(
            title = "Your Library",
            description = "Organized into three tabs for easy navigation: Songs, Artists, and Albums."
        ),
        GuideStep(
            title = "Songs Tab",
            description = "View all your music in one place. Sort and filter to find what you want."
        ),
        GuideStep(
            title = "Artists Tab",
            description = "Browse by artist. Tap any artist to see all their songs in your collection."
        ),
        GuideStep(
            title = "Albums Tab",
            description = "Browse by album. Perfect for listening to full albums in order."
        )
    )
    
    val quickPicksScreenGuide = listOf(
        GuideStep(
            title = "Quick Picks",
            description = "Your personalized recommendations powered by our ML engine."
        ),
        GuideStep(
            title = "How It Works",
            description = "The app analyzes your listening patterns: play count, completion rate, skip rate, and time of day to suggest songs you'll love."
        ),
        GuideStep(
            title = "Score & Confidence",
            description = "Each recommendation shows a score (0-100) and confidence level, indicating how sure the AI is that you'll like it."
        ),
        GuideStep(
            title = "Keep Learning",
            description = "The more you listen, the smarter the recommendations become. Use the shuffle button to explore all picks randomly."
        )
    )
}
