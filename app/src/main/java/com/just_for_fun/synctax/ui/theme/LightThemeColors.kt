package com.just_for_fun.synctax.ui.theme

import androidx.compose.ui.graphics.Color

// Light Theme Colors for SyncTax App
// This file defines all colors used in light theme mode for every component, screen, and state

// === MAIN APP COLORS ===

// Main Background Colors
val LightMainBackground = Color(0xFFF8F9FA)
val LightAppBarBackground = Color(0xFFFFFFFF)
val LightBottomNavBackground = Color(0xFFFFFFFF)

// Icon Colors
val LightIconPrimary = Color(0xFF1C1B1F)
val LightIconSecondary = Color(0xFF49454F)

// Accent Colors
val LightAccentPrimary = Color(0xFFFF0033)
val LightAccentPressed = Color(0xFFE0002A)

// Card Colors
val LightCardBackground = Color(0xFFFFFFFF)
val LightCardBorder = Color(0xFFE0E0E0)

// Text Colors
val LightTextTitle = Color(0xFF1C1B1F)
val LightTextBody = Color(0xFF49454F)
val LightTextTertiary = Color(0xFF79747E)

// Chip Colors
val LightChipUnselected = Color(0xFFE8E8E8)
val LightChipSelected = LightAccentPrimary

// Mini Player Colors
val LightMiniPlayerBackground = Color(0xFFFFFFFF)
val LightProgressFilled = LightAccentPrimary
val LightProgressUnfilled = Color(0xFFE0E0E0)

// === PLAYER COLORS (Full Screen) ===

// Player Background
val LightPlayerBackground = Color(0xFFF5F5F5)

// Player Surface Colors
val LightPlayerSurface = Color(0xFFFFFFFF)
val LightPlayerSurfaceVariant = Color(0xFFF8F8F8)

// Player Text Colors
val LightPlayerTextPrimary = Color(0xFF1C1B1F)
val LightPlayerTextSecondary = Color(0xFF49454F)

// Player Control Colors
val LightPlayerIconColor = Color(0xFF49454F)
val LightPlayerAccent = LightAccentPrimary
val LightPlayerOnAccent = Color.White

// Player Progress Bar
val LightPlayerTrackUnfilled = Color(0xFFE0E0E0)
val LightPlayerTrackFilled = LightPlayerAccent

// === COMPONENT SPECIFIC COLORS ===

// Search Screen
val LightSearchBackground = Color(0xFFF8F9FA)
val LightSearchHint = Color(0xFF79747E)
val LightSearchText = Color(0xFF1C1B1F)

// Library Screen
val LightLibraryTabSelected = LightAccentPrimary
val LightLibraryTabUnselected = Color(0xFF79747E)
val LightLibraryBackground = Color(0xFFF8F9FA)

// Playlist Screen
val LightPlaylistCardBackground = Color(0xFFFFFFFF)
val LightPlaylistCardBorder = Color(0xFFE0E0E0)
val LightPlaylistEmptyIcon = Color(0xFFB3B3B3)

// Home Screen
val LightHomeBackground = Color(0xFFF8F9FA)
val LightHomeCardBackground = Color(0xFFFFFFFF)
val LightHomeSectionTitle = Color(0xFF1C1B1F)

// Quick Picks Screen
val LightQuickPickBackground = Color(0xFFF8F9FA)
val LightQuickPickCardBackground = Color(0xFFFFFFFF)

// Settings Screen
val LightSettingsBackground = Color(0xFFF8F9FA)
val LightSettingsCardBackground = Color(0xFFFFFFFF)
val LightSettingsDivider = Color(0xFFE0E0E0)

// Album Detail Screen
val LightAlbumDetailBackground = Color(0xFFF8F9FA)
val LightAlbumDetailCardBackground = Color(0xFFFFFFFF)

// Artist Detail Screen
val LightArtistDetailBackground = Color(0xFFF8F9FA)
val LightArtistDetailCardBackground = Color(0xFFFFFFFF)

// Import Playlist Screen
val LightImportBackground = Color(0xFFF8F9FA)
val LightImportCardBackground = Color(0xFFFFFFFF)

// Training Screen
val LightTrainingBackground = Color(0xFFF8F9FA)
val LightTrainingCardBackground = Color(0xFFFFFFFF)

// Welcome Screens
val LightWelcomeBackground = Color(0xFFF8F9FA)
val LightWelcomeCardBackground = Color(0xFFFFFFFF)

// === STATE COLORS ===

// Loading States
val LightLoadingBackground = Color(0xFFFFFFFF)
val LightLoadingSpinner = LightAccentPrimary

// Error States
val LightErrorBackground = Color(0xFFFFDAD6)
val LightErrorText = Color(0xFFBA1A1A)
val LightErrorIcon = Color(0xFFBA1A1A)

// Success States
val LightSuccessBackground = Color(0xFFC8E6C9)
val LightSuccessText = Color(0xFF2E7D32)
val LightSuccessIcon = Color(0xFF2E7D32)

// Disabled States
val LightDisabledBackground = Color(0xFFE0E0E0)
val LightDisabledText = Color(0xFF79747E)
val LightDisabledIcon = Color(0xFF79747E)

// === DYNAMIC COLORS ===

// Album Background Gradients (Light Theme)
val LightAlbumGradientStart = Color(0xFFE3F2FD)
val LightAlbumGradientEnd = Color(0xFFF8F9FA)

// Glow Effects
val LightGlowColor = LightAccentPrimary.copy(alpha = 0.3f)

// === CONTRIBUTOR FEATURES ===

// Contributor Badge
val LightContributorBadgeBackground = Color(0xFFFFF3E0)
val LightContributorBadgeText = Color(0xFFE65100)
val LightContributorBadgeBorder = Color(0xFFFFCC02)

// Thank You Card
val LightThankYouCardBackground = Color(0xFFFFF8E1)
val LightThankYouCardText = Color(0xFFE65100)
val LightThankYouCardBorder = Color(0xFFFFCC02)