package com.just_for_fun.synctax.presentation.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Colors for SyncTax App
// This file defines all colors used in light theme mode for every component, screen, and state
// Organized by sections: Main App, Screens, Components, States, Dynamic, and Special Features

// =============================================================================
// === MAIN APP COLORS ===
// =============================================================================

// Main Background Colors
val LightMainBackground = Color(0xFFF8F9FA)
val LightAppBarBackground = Color(0xFFFFFFFF)
val LightBottomNavBackground = Color(0xFFFFFFFF)
val LightScaffoldBackground = Color(0xFFF8F9FA)

// Icon Colors
val LightIconPrimary = Color(0xFF1C1B1F)
val LightIconSecondary = Color(0xFF49454F)
val LightIconTertiary = Color(0xFF79747E)
val LightIconDisabled = Color(0xFFB3B3B3)

// Accent Colors
val LightAccentPrimary = Color(0xFFFF0033)
val LightAccentPressed = Color(0xFFE0002A)
val LightAccentSecondary = Color(0xFFFF6B6B)
val LightAccentTertiary = Color(0xFFFF9999)

// Card Colors
val LightCardBackground = Color(0xFFFFFFFF)
val LightCardBorder = Color(0xFFE0E0E0)
val LightCardShadow = Color(0x1F000000)

// Text Colors
val LightTextTitle = Color(0xFF1C1B1F)
val LightTextBody = Color(0xFF49454F)
val LightTextTertiary = Color(0xFF79747E)
val LightTextHint = Color(0xFFFFFFFF)

// Button Colors
val LightButtonPrimary = LightAccentPrimary
val LightButtonPrimaryText = Color.White
val LightButtonSecondary = Color(0xFFE0E0E0)
val LightButtonSecondaryText = LightTextTitle
val LightButtonOutlined = Color.Transparent
val LightButtonOutlinedBorder = LightAccentPrimary
val LightButtonOutlinedText = LightAccentPrimary

// Input Field Colors
val LightInputBackground = Color(0xFFF5F5F5)
val LightInputBorder = Color(0xFFE0E0E0)
val LightInputFocusedBorder = LightAccentPrimary
val LightInputErrorBorder = Color(0xFFBA1A1A)
val LightInputText = LightTextTitle
val LightInputHint = LightTextHint

// =============================================================================
// === PLAYER COLORS (Full Screen & Mini Player) ===
// =============================================================================

// Player Background
val LightPlayerBackground = Color(0xFFF5F5F5)
val LightPlayerSurface = Color(0xFFFFFFFF)
val LightPlayerSurfaceVariant = Color(0xFFF8F8F8)

// Player Text Colors
val LightPlayerTextPrimary = Color(0xFF1C1B1F)
val LightPlayerTextSecondary = Color(0xFF49454F)
val LightPlayerTextTertiary = Color(0xFF79747E)

// Player Control Colors
val LightPlayerIconColor = Color(0xFF49454F)
val LightPlayerAccent = LightAccentPrimary
val LightPlayerOnAccent = Color.White
val LightPlayerDisabled = Color(0xFFB3B3B3)

// Player Progress Bar
val LightPlayerTrackUnfilled = Color(0xFFE0E0E0)
val LightPlayerTrackFilled = LightPlayerAccent
val LightPlayerThumb = LightPlayerAccent

// Mini Player Colors
val LightMiniPlayerBackground = Color(0xFFFFFFFF)
val LightMiniPlayerBorder = Color(0xFFE0E0E0)
val LightMiniPlayerProgressFilled = LightAccentPrimary
val LightMiniPlayerProgressUnfilled = Color(0xFFE0E0E0)

// =============================================================================
// === SCREEN SPECIFIC COLORS ===
// =============================================================================

// Home Screen
val LightHomeBackground = Color(0xFFF8F9FA)
val LightHomeCardBackground = Color(0xFFFFFFFF)
val LightHomeSectionTitle = Color(0xFF1C1B1F)
val LightHomeSectionSubtitle = Color(0xFF49454F)
val LightHomeGreetingText = Color(0xFF1C1B1F)

// Search Screen
val LightSearchBackground = Color(0xFFF8F9FA)
val LightSearchHint = Color(0xFF79747E)
val LightSearchText = Color(0xFF1C1B1F)
val LightSearchIcon = Color(0xFF49454F)
val LightSearchClearIcon = Color(0xFF79747E)

// Library Screen
val LightLibraryTabSelected = LightAccentPrimary
val LightLibraryTabUnselected = Color(0xFF79747E)
val LightLibraryBackground = Color(0xFFF8F9FA)
val LightLibraryTabIndicator = LightAccentPrimary

// Playlist Screen
val LightPlaylistCardBackground = Color(0xFFFFFFFF)
val LightPlaylistCardBorder = Color(0xFFE0E0E0)
val LightPlaylistEmptyIcon = Color(0xFFB3B3B3)
val LightPlaylistEmptyText = Color(0xFF79747E)

// Album Detail Screen
val LightAlbumDetailBackground = Color(0xFFF8F9FA)
val LightAlbumDetailCardBackground = Color(0xFFFFFFFF)
val LightAlbumDetailTitle = Color(0xFF1C1B1F)
val LightAlbumDetailArtist = Color(0xFF49454F)
val LightAlbumDetailMeta = Color(0xFF79747E)

// Artist Detail Screen
val LightArtistDetailBackground = Color(0xFFF8F9FA)
val LightArtistDetailCardBackground = Color(0xFFFFFFFF)
val LightArtistDetailTitle = Color(0xFF1C1B1F)
val LightArtistDetailBio = Color(0xFF49454F)

// Quick Pick Screen
val LightQuickPickBackground = Color(0xFFF8F9FA)
val LightQuickPickCardBackground = Color(0xFFFFFFFF)
val LightQuickPickTitle = Color(0xFF1C1B1F)

// Settings Screen
val LightSettingsBackground = Color(0xFFF8F9FA)
val LightSettingsCardBackground = Color(0xFFFFFFFF)
val LightSettingsDivider = Color(0xFFE0E0E0)
val LightSettingsTitle = Color(0xFF1C1B1F)
val LightSettingsSubtitle = Color(0xFF49454F)

// Import Playlist Screen
val LightImportBackground = Color(0xFFF8F9FA)
val LightImportCardBackground = Color(0xFFFFFFFF)
val LightImportTitle = Color(0xFF1C1B1F)

// Training Screen
val LightTrainingBackground = Color(0xFFF8F9FA)
val LightTrainingCardBackground = Color(0xFFFFFFFF)
val LightTrainingProgress = LightAccentPrimary
val LightTrainingText = Color(0xFF1C1B1F)

// Welcome Screens
val LightWelcomeBackground = Color(0xFFF8F9FA)
val LightWelcomeCardBackground = Color(0xFFFFFFFF)
val LightWelcomeTitle = Color(0xFF1C1B1F)
val LightWelcomeSubtitle = Color(0xFF49454F)

// Special Creator Welcome Screens
val LightSpecialWelcomeBackground = Color(0xFFF8F9FA)
val LightSpecialWelcomeCardBackground = Color(0xFFFFFFFF)
val LightSpecialWelcomeTitle = Color(0xFF1C1B1F)
val LightSpecialWelcomeAccent = LightAccentPrimary

// =============================================================================
// === COMPONENT SPECIFIC COLORS ===
// =============================================================================

// Chip Colors
val LightChipUnselected = Color(0xFFE8E8E8)
val LightChipSelected = LightAccentPrimary
val LightChipSelectedText = Color.White
val LightChipUnselectedText = Color(0xFF49454F)

// Card Components
val LightSongCardBackground = Color(0xFFFFFFFF)
val LightSongCardBorder = Color(0xFFE0E0E0)
val LightSongCardTitle = Color(0xFF1C1B1F)
val LightSongCardArtist = Color(0xFF49454F)

val LightRecommendationCardBackground = Color(0xFFFFFFFF)
val LightRecommendationCardBorder = Color(0xFFE0E0E0)
val LightRecommendationCardTitle = Color(0xFF1C1B1F)

val LightSavedPlaylistCardBackground = Color(0xFFFFFFFF)
val LightSavedPlaylistCardBorder = Color(0xFFE0E0E0)
val LightSavedPlaylistCardTitle = Color(0xFF1C1B1F)

val LightOnlineResultCardBackground = Color(0xFFFFFFFF)
val LightOnlineResultCardBorder = Color(0xFFE0E0E0)
val LightOnlineResultCardTitle = Color(0xFF1C1B1F)

val LightOnlineHistoryCardBackground = Color(0xFFFFFFFF)
val LightOnlineHistoryCardBorder = Color(0xFFE0E0E0)
val LightOnlineHistoryCardTitle = Color(0xFF1C1B1F)

val LightFormatCardBackground = Color(0xFFFFFFFF)
val LightFormatCardBorder = Color(0xFFE0E0E0)
val LightFormatCardTitle = Color(0xFF1C1B1F)

// Player Components
val LightPlayerSliderTrack = Color(0xFFE0E0E0)
val LightPlayerSliderThumb = LightAccentPrimary
val LightPlayerSliderActive = LightAccentPrimary

val LightAnimatedWaveformColor = LightAccentPrimary
val LightAnimatedWaveformBackground = Color(0xFFE0E0E0)

val LightLyricsOverlayBackground = Color(0xB3000000)
val LightLyricsOverlayText = Color.White
val LightLyricsOverlayAccent = LightAccentPrimary

val LightUpNextItemBackground = Color(0xFFFFFFFF)
val LightUpNextItemBorder = Color(0xFFE0E0E0)
val LightUpNextItemTitle = Color(0xFF1C1B1F)

val LightPlayerMenuBackground = Color(0xFFFFFFFF)
val LightPlayerMenuBorder = Color(0xFFE0E0E0)
val LightPlayerMenuText = Color(0xFF1C1B1F)

// Section Components
val LightSectionHeaderTitle = Color(0xFF1C1B1F)
val LightSectionHeaderSubtitle = Color(0xFF49454F)

val LightEmptyStateIcon = Color(0xFFB3B3B3)
val LightEmptyStateText = Color(0xFF79747E)

val LightSpeedDialBackground = Color(0xFFFFFFFF)
val LightSpeedDialIcon = LightAccentPrimary

// App Components
val LightNavigationBarBackground = Color(0xFFFFFFFF)
val LightNavigationBarSelected = LightAccentPrimary
val LightNavigationBarUnselected = Color(0xFF79747E)

val LightBottomSheetBackground = Color(0xFFFFFFFF)
val LightBottomSheetHandle = Color(0xFFE0E0E0)

val LightUserProfileDialogBackground = Color(0xFFFFFFFF)
val LightUserProfileDialogBorder = Color(0xFFE0E0E0)
val LightUserProfileDialogTitle = Color(0xFF1C1B1F)

// Onboarding Components
val LightDirectoryDialogBackground = Color(0xFFFFFFFF)
val LightDirectoryDialogBorder = Color(0xFFE0E0E0)
val LightDirectoryDialogTitle = Color(0xFF1C1B1F)

// =============================================================================
// === STATE COLORS ===
// =============================================================================

// Loading States
val LightLoadingBackground = Color(0xFFFFFFFF)
val LightLoadingSpinner = LightAccentPrimary
val LightLoadingText = Color(0xFF49454F)

// Error States
val LightErrorBackground = Color(0xFFFFDAD6)
val LightErrorText = Color(0xFFBA1A1A)
val LightErrorIcon = Color(0xFFBA1A1A)
val LightErrorBorder = Color(0xFFBA1A1A)

// Success States
val LightSuccessBackground = Color(0xFFC8E6C9)
val LightSuccessText = Color(0xFF2E7D32)
val LightSuccessIcon = Color(0xFF2E7D32)
val LightSuccessBorder = Color(0xFF2E7D32)

// Warning States
val LightWarningBackground = Color(0xFFFFF3E0)
val LightWarningText = Color(0xFFE65100)
val LightWarningIcon = Color(0xFFE65100)
val LightWarningBorder = Color(0xFFE65100)

// Info States
val LightInfoBackground = Color(0xFFE3F2FD)
val LightInfoText = Color(0xFF1976D2)
val LightInfoIcon = Color(0xFF1976D2)
val LightInfoBorder = Color(0xFF1976D2)

// Disabled States
val LightDisabledBackground = Color(0xFFE0E0E0)
val LightDisabledText = Color(0xFF79747E)
val LightDisabledIcon = Color(0xFF79747E)
val LightDisabledBorder = Color(0xFFB3B3B3)

// Focused States
val LightFocusedBorder = LightAccentPrimary
val LightFocusedBackground = Color(0xFFF5F5F5)

// Pressed States
val LightPressedBackground = Color(0xFFE0E0E0)
val LightPressedText = LightTextTitle

// Selected States
val LightSelectedBackground = LightAccentPrimary
val LightSelectedText = Color.White
val LightSelectedIcon = Color.White

// Hovered States (for web/desktop, but defined for consistency)
val LightHoveredBackground = Color(0xFFF5F5F5)
val LightHoveredText = LightTextTitle

// =============================================================================
// === DYNAMIC COLORS ===
// =============================================================================

// Album Background Gradients (Light Theme)
val LightAlbumGradientStart = Color(0xFFE3F2FD)
val LightAlbumGradientEnd = Color(0xFFF8F9FA)

// Glow Effects
val LightGlowColor = LightAccentPrimary.copy(alpha = 0.3f)

// Ripple Effects
val LightRippleColor = LightAccentPrimary.copy(alpha = 0.1f)

// Elevation Shadows
val LightElevationLow = Color(0x1F000000)
val LightElevationMedium = Color(0x29000000)
val LightElevationHigh = Color(0x33000000)

// =============================================================================
// === CONTRIBUTOR FEATURES ===
// =============================================================================

// Contributor Badge
val LightContributorBadgeBackground = Color(0xFFFFF3E0)
val LightContributorBadgeText = Color(0xFFE65100)
val LightContributorBadgeBorder = Color(0xFFFFCC02)

// Thank You Card
val LightThankYouCardBackground = Color(0xFFFFF8E1)
val LightThankYouCardText = Color(0xFFE65100)
val LightThankYouCardBorder = Color(0xFFFFCC02)

// =============================================================================
// === ADDITIONAL COLORS FOR ENHANCED THEMING ===
// =============================================================================

// Dialog Colors
val LightDialogBackground = Color(0xFFFFFFFF)
val LightDialogBorder = Color(0xFFE0E0E0)
val LightDialogTitle = Color(0xFF1C1B1F)
val LightDialogContent = Color(0xFF49454F)

// Snackbar Colors
val LightSnackbarBackground = Color(0xFF333333)
val LightSnackbarText = Color.White
val LightSnackbarAction = LightAccentPrimary

// Tab Colors
val LightTabSelected = LightAccentPrimary
val LightTabUnselected = Color(0xFF79747E)
val LightTabIndicator = LightAccentPrimary

// Divider Colors
val LightDivider = Color(0xFFE0E0E0)
val LightDividerThick = Color(0xFFB3B3B3)

// Overlay Colors
val LightOverlayScrim = Color(0x80000000)
val LightOverlayLight = Color(0x40FFFFFF)

// Progress Indicator Colors
val LightProgressIndicator = LightAccentPrimary
val LightProgressTrack = Color(0xFFE0E0E0)

// Switch Colors
val LightSwitchThumb = Color.White
val LightSwitchTrack = Color(0xFFE0E0E0)
val LightSwitchTrackChecked = LightAccentPrimary.copy(alpha = 0.5f)

// Checkbox Colors
val LightCheckboxUnchecked = Color(0xFFE0E0E0)
val LightCheckboxChecked = LightAccentPrimary
val LightCheckboxBorder = Color(0xFFB3B3B3)

// Radio Button Colors
val LightRadioButtonUnchecked = Color(0xFFE0E0E0)
val LightRadioButtonChecked = LightAccentPrimary
val LightRadioButtonBorder = Color(0xFFB3B3B3)

// Slider Colors
val LightSliderTrack = Color(0xFFE0E0E0)
val LightSliderThumb = LightAccentPrimary
val LightSliderActive = LightAccentPrimary

// Floating Action Button Colors
val LightFabBackground = LightAccentPrimary
val LightFabIcon = Color.White
val LightFabPressed = LightAccentPressed

// App Bar Colors
val LightAppBarTitle = Color(0xFF1C1B1F)
val LightAppBarIcon = Color(0xFF49454F)
val LightAppBarAction = Color(0xFF49454F)

// Bottom Navigation Colors
val LightBottomNavSelected = LightAccentPrimary
val LightBottomNavUnselected = Color(0xFF79747E)
val LightBottomNavIndicator = LightAccentPrimary

// Drawer Colors
val LightDrawerBackground = Color(0xFFFFFFFF)
val LightDrawerBorder = Color(0xFFE0E0E0)
val LightDrawerHeader = Color(0xFFF8F9FA)
val LightDrawerItem = Color(0xFF1C1B1F)
val LightDrawerItemSelected = LightAccentPrimary

// Menu Colors
val LightMenuBackground = Color(0xFFFFFFFF)
val LightMenuBorder = Color(0xFFE0E0E0)
val LightMenuItem = Color(0xFF1C1B1F)
val LightMenuItemSelected = LightAccentPrimary

// Tooltip Colors
val LightTooltipBackground = Color(0xFF333333)
val LightTooltipText = Color.White
val LightTooltipBorder = Color(0xFFE0E0E0)

// Badge Colors
val LightBadgeBackground = LightAccentPrimary
val LightBadgeText = Color.White

// Notification Colors
val LightNotificationBackground = Color(0xFFFFFFFF)
val LightNotificationBorder = Color(0xFFE0E0E0)
val LightNotificationTitle = Color(0xFF1C1B1F)
val LightNotificationContent = Color(0xFF49454F)
