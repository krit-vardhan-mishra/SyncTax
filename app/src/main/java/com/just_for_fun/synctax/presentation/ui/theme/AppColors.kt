package com.just_for_fun.synctax.presentation.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

/**
 * AppColors - Theme-aware color system for SyncTax App
 * 
 * This object provides colors that automatically adapt to light/dark theme.
 * Usage: AppColors.backgroundColor (no need to check isSystemInDarkTheme() manually)
 * 
 * All colors are defined as composable properties that return the appropriate
 * color based on the current system theme.
 */
object AppColors {
    
    // =============================================================================
    // === MAIN APP COLORS ===
    // =============================================================================
    
    // Main Background Colors
    val mainBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MainBackground else LightMainBackground
    
    val appBarBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AppBarBackground else LightAppBarBackground
    
    val bottomNavBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) BottomNavBackground else LightBottomNavBackground
    
    val scaffoldBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MainBackground else LightScaffoldBackground
    
    // Icon Colors
    val iconPrimary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) IconPrimary else LightIconPrimary
    
    val iconSecondary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) IconSecondary else LightIconSecondary
    
    val iconTertiary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF79747E) else LightIconTertiary
    
    val iconDisabled: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF5C5C5C) else LightIconDisabled
    
    // Accent Colors
    val accentPrimary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AccentPrimary else LightAccentPrimary
    
    val accentPressed: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AccentPressed else LightAccentPressed
    
    val accentSecondary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFFF6B6B) else LightAccentSecondary
    
    val accentTertiary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFFF9999) else LightAccentTertiary
    
    // Card Colors
    val cardBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CardBackground else LightCardBackground
    
    val cardBorder: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CardBorder else LightCardBorder
    
    val cardShadow: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0x1FFFFFFF) else LightCardShadow
    
    // Text Colors
    val textTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) TextTitle else LightTextTitle
    
    val textBody: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) TextBody else LightTextBody
    
    val textTertiary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) TextTertiary else LightTextTertiary
    
    val textHint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF5C5C5C) else LightTextHint
    
    // Button Colors
    val buttonPrimary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AccentPrimary else LightButtonPrimary
    
    val buttonPrimaryText: Color
        @Composable @ReadOnlyComposable
        get() = Color.White
    
    val buttonSecondary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF2A2A2E) else LightButtonSecondary
    
    val buttonSecondaryText: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightButtonSecondaryText
    
    // Input Field Colors
    val inputBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF1A1A1D) else LightInputBackground
    
    val inputBorder: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF2A2A2E) else LightInputBorder
    
    val inputFocusedBorder: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AccentPrimary else LightInputFocusedBorder
    
    val inputText: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightInputText
    
    val inputHint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF5C5C5C) else LightInputHint
    
    // =============================================================================
    // === PLAYER COLORS (Full Screen & Mini Player) ===
    // =============================================================================
    
    // Player Background
    val playerBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) PlayerBackground else LightPlayerBackground
    
    val playerSurface: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) PlayerSurface else LightPlayerSurface
    
    val playerSurfaceVariant: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) PlayerSurfaceVariant else LightPlayerSurfaceVariant
    
    // Player Text Colors
    val playerTextPrimary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) PlayerTextPrimary else LightPlayerTextPrimary
    
    val playerTextSecondary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) PlayerTextSecondary else LightPlayerTextSecondary
    
    val playerTextTertiary: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF8C8C8C) else LightPlayerTextTertiary
    
    // Player Control Colors
    val playerIconColor: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) PlayerIconColor else LightPlayerIconColor
    
    val playerAccent: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) PlayerAccent else LightPlayerAccent
    
    val playerOnAccent: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) PlayerOnAccent else LightPlayerOnAccent
    
    val playerDisabled: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF5C5C5C) else LightPlayerDisabled
    
    // Player Progress Bar
    val playerTrackUnfilled: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) PlayerTrackUnfilled else LightPlayerTrackUnfilled
    
    val playerTrackFilled: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) PlayerTrackFilled else LightPlayerTrackFilled
    
    val playerThumb: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) PlayerAccent else LightPlayerThumb
    
    // Slider colors (special for light theme - white)
    val sliderThumb: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else Color.White
    
    val sliderActiveTrack: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else Color.White
    
    val sliderInactiveTrack: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.3f)
    
    // Mini Player Colors
    val miniPlayerBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MiniPlayerBackground else LightMiniPlayerBackground
    
    val miniPlayerBorder: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF2A2A2E) else LightMiniPlayerBorder
    
    val miniPlayerProgressFilled: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) ProgressFilled else LightMiniPlayerProgressFilled
    
    val miniPlayerProgressUnfilled: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) ProgressUnfilled else LightMiniPlayerProgressUnfilled
    
    // =============================================================================
    // === SCREEN SPECIFIC COLORS ===
    // =============================================================================
    
    // Home Screen
    val homeBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MainBackground else LightHomeBackground
    
    val homeCardBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CardBackground else LightHomeCardBackground
    
    val homeSectionTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightHomeSectionTitle
    
    val homeSectionSubtitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFB3B3B3) else LightHomeSectionSubtitle
    
    val homeGreetingText: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightHomeGreetingText
    
    // Search Screen
    val searchBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MainBackground else LightSearchBackground
    
    val searchHint: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF79747E) else LightSearchHint
    
    val searchText: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightSearchText
    
    val searchIcon: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF9A9A9A) else LightSearchIcon
    
    // Library Screen
    val libraryTabSelected: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AccentPrimary else LightLibraryTabSelected
    
    val libraryTabUnselected: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF79747E) else LightLibraryTabUnselected
    
    val libraryBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MainBackground else LightLibraryBackground
    
    val libraryTabIndicator: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AccentPrimary else LightLibraryTabIndicator
    
    val libraryTabSelectedText: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else Color.White
    
    // Playlist Screen
    val playlistCardBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CardBackground else LightPlaylistCardBackground
    
    val playlistCardBorder: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CardBorder else LightPlaylistCardBorder
    
    val playlistEmptyIcon: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF5C5C5C) else LightPlaylistEmptyIcon
    
    val playlistEmptyText: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF8C8C8C) else LightPlaylistEmptyText
    
    val playlistImportText: Color
        @Composable @ReadOnlyComposable
        get() = Color.White
    
    // Album Detail Screen
    val albumDetailBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MainBackground else LightAlbumDetailBackground
    
    val albumDetailCardBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF1A1A1D) else LightAlbumDetailCardBackground
    
    val albumDetailTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightAlbumDetailTitle
    
    val albumDetailArtist: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFB3B3B3) else LightAlbumDetailArtist
    
    val albumDetailMeta: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF8C8C8C) else LightAlbumDetailMeta
    
    // Artist Detail Screen
    val artistDetailBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MainBackground else LightArtistDetailBackground
    
    val artistDetailCardBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF1A1A1D) else LightArtistDetailCardBackground
    
    val artistDetailTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightArtistDetailTitle
    
    val artistDetailBio: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFB3B3B3) else LightArtistDetailBio
    
    // Quick Pick Screen
    val quickPickBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MainBackground else LightQuickPickBackground
    
    val quickPickCardBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0x8E252526) else LightQuickPickCardBackground.copy(alpha = 0.8f)
    
    val quickPickTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightQuickPickTitle
    
    // Settings Screen
    val settingsBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MainBackground else LightSettingsBackground
    
    val settingsCardBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CardBackground else LightSettingsCardBackground
    
    val settingsDivider: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF2A2A2E) else LightSettingsDivider
    
    val settingsTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightSettingsTitle
    
    val settingsSubtitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFB3B3B3) else LightSettingsSubtitle
    
    // Welcome Screens
    val welcomeBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MainBackground else LightWelcomeBackground
    
    val welcomeCardBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CardBackground else LightWelcomeCardBackground
    
    val welcomeTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightWelcomeTitle
    
    val welcomeSubtitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFB3B3B3) else LightWelcomeSubtitle
    
    // Special Creator Welcome Screens
    val specialWelcomeBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) MainBackground else LightSpecialWelcomeBackground
    
    val specialWelcomeCardBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CardBackground else LightSpecialWelcomeCardBackground
    
    val specialWelcomeTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightSpecialWelcomeTitle
    
    val specialWelcomeAccent: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AccentPrimary else LightSpecialWelcomeAccent
    
    // =============================================================================
    // === COMPONENT SPECIFIC COLORS ===
    // =============================================================================
    
    // Chip Colors
    val chipUnselected: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) ChipUnselected else LightChipUnselected
    
    val chipSelected: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) ChipSelected else LightChipSelected
    
    val chipSelectedText: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightChipSelectedText
    
    val chipUnselectedText: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightChipUnselectedText
    
    val chipSelectedBorder: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightChipSelected
    
    // Song Card
    val songCardBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CardBackground else LightSongCardBackground
    
    val songCardBorder: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CardBorder else LightSongCardBorder
    
    val songCardTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightSongCardTitle
    
    val songCardArtist: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFB3B3B3) else LightSongCardArtist
    
    // Recommendation Card
    val recommendationCardBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CardBackground else LightRecommendationCardBackground
    
    val recommendationCardBorder: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CardBorder else LightRecommendationCardBorder
    
    val recommendationCardTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightRecommendationCardTitle
    
    val recommendationRankText: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF8C8C8C) else LightPlayerTextSecondary
    
    val recommendationAlbumArtBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF2A2A2E) else LightQuickPickCardBackground
    
    val recommendationReasonColor: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AccentPrimary else LightAccentPrimary
    
    // Online Result Card
    val onlineResultCardBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CardBackground else LightOnlineResultCardBackground
    
    val onlineResultCardBorder: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) CardBorder else LightOnlineResultCardBorder
    
    val onlineResultCardTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightOnlineResultCardTitle
    
    // Navigation Bar
    val navBarSelectedIcon: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightTextTitle
    
    val navBarSelectedText: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightTextTitle
    
    val navBarUnselectedIcon: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) IconSecondary else LightIconSecondary
    
    val navBarUnselectedText: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) IconSecondary else LightIconSecondary
    
    val navBarGlowColor: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AccentPrimary else LightAccentPrimary
    
    // Section Header
    val sectionHeaderTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightHomeSectionTitle
    
    val sectionHeaderIcon: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF9A9A9A) else LightIconSecondary
    
    // Empty State
    val emptyStateIcon: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.LightGray else LightEmptyStateIcon
    
    val emptyStateText: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF8C8C8C) else LightEmptyStateText
    
    // Divider
    val divider: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF2A2A2E) else LightSettingsDivider
    
    // Progress Indicator
    val progressIndicator: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AccentPrimary else LightAccentPrimary
    
    val progressIndicatorTrack: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF393939) else LightPlayerTrackUnfilled
    
    // Download Progress (special - white in both themes for visibility on dynamic backgrounds)
    val downloadProgressBar: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF4CAF50) else Color.White
    
    val downloadProgressTrack: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.2f)
    
    // Snackbar
    val snackbarBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF323232) else Color(0xFF323232)
    
    val snackbarText: Color
        @Composable @ReadOnlyComposable
        get() = Color.White
    
    // Dialog
    val dialogBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF2A2A2E) else Color.White
    
    val dialogTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightTextTitle
    
    val dialogText: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFB3B3B3) else LightTextBody
    
    // Top App Bar
    val topAppBarTitle: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.White else LightTextTitle
    
    val topAppBarIcon: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AccentPrimary else LightAccentPrimary
    
    // FAB (Floating Action Button)
    val fabBackground: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) AccentPrimary else LightAccentPrimary
    
    val fabContent: Color
        @Composable @ReadOnlyComposable
        get() = Color.White
    
    // =============================================================================
    // === STATE COLORS ===
    // =============================================================================
    
    // Error Colors
    val error: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFCF6679) else Color(0xFFBA1A1A)
    
    val errorContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF93000A) else Color(0xFFFFDAD6)
    
    val onError: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.Black else Color.White
    
    // Success Colors
    val success: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF4CAF50) else Color(0xFF4CAF50)
    
    val successContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF1B5E20) else Color(0xFFC8E6C9)
    
    // Warning Colors
    val warning: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFFF9800) else Color(0xFFFF9800)
    
    val warningContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFFE65100) else Color(0xFFFFE0B2)
    
    // Disabled Colors
    val disabled: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF5C5C5C) else Color(0xFFB3B3B3)
    
    val disabledContainer: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color(0xFF2A2A2E) else Color(0xFFE0E0E0)
    
    // Overlay Colors
    val scrim: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.4f)
    
    val overlay: Color
        @Composable @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) Color.Black.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.3f)
}
