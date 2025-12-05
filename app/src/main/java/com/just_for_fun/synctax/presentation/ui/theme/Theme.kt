package com.just_for_fun.synctax.presentation.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// YouTube Music Dark Theme Colors
private val YTMusicDarkColorScheme = darkColorScheme(
    primary = AccentPrimary, // Accent primary
    onPrimary = Color.White,
    primaryContainer = AccentPressed, // Pressed / hover tone
    onPrimaryContainer = Color(0xFFFFDAD6),

    secondary = Color(0xFFE1BEE7),
    onSecondary = Color(0xFF4A148C),
    secondaryContainer = Color(0xFF6A1B9A),
    onSecondaryContainer = Color(0xFFF3E5F5),

    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF1B5E20),
    tertiaryContainer = Color(0xFF2E7D32),
    onTertiaryContainer = Color(0xFFC8E6C9),

    error = Color(0xFFCF6679),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    background = MainBackground, // Main screen background
    onBackground = Color(0xFFFFFFFF),

    surface = AppBarBackground, // App bar & bottom nav
    onSurface = Color(0xFFFFFFFF),
    surfaceVariant = CardBackground, // Cards / sections
    onSurfaceVariant = TextBody, // Body text

    surfaceContainer = CardBackground,
    surfaceContainerHigh = CardBackground,
    surfaceContainerHighest = CardBackground,

    outline = CardBorder, // Optional border
    outlineVariant = ProgressUnfilled // Progress track / tertiary usage
)

// YouTube Music Light Theme Colors
private val YTMusicLightColorScheme = lightColorScheme(
    primary = LightAccentPrimary,
    onPrimary = LightButtonPrimaryText,
    primaryContainer = LightAccentPressed,
    onPrimaryContainer = Color(0xFF7C1E1F),

    secondary = LightAccentSecondary,
    onSecondary = Color.White,
    secondaryContainer = LightAccentTertiary,
    onSecondaryContainer = Color(0xFF1A237E),

    tertiary = LightAccentTertiary,
    onTertiary = Color.White,
    tertiaryContainer = LightSuccessBackground,
    onTertiaryContainer = Color(0xFF1B5E20),

    error = LightErrorText,
    onError = Color.White,
    errorContainer = LightErrorBackground,
    onErrorContainer = Color(0xFF410002),

    background = LightMainBackground,
    onBackground = LightTextTitle,

    surface = LightAppBarBackground,
    onSurface = LightTextTitle,
    surfaceVariant = LightCardBackground,
    onSurfaceVariant = LightTextBody,

    surfaceContainer = LightCardBackground,
    surfaceContainerHigh = LightCardBackground,
    surfaceContainerHighest = LightCardBackground,

    outline = LightCardBorder,
    outlineVariant = LightInputBorder
)

@Composable
fun SynctaxTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default for YT Music theme
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> YTMusicDarkColorScheme
        else -> YTMusicLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
