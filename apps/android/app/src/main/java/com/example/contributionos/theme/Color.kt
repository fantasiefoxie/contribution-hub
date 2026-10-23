package com.example.contributionos.theme

import androidx.compose.ui.graphics.Color

// Field Log Roguelite Palette (Direction 1 - Stitch Redesign)
val GrassNeon = Color(0xFF8DEB65)        // Primary bioluminescent action green
val GrassSprout = Color(0xFF4CCB72)      // Secondary botanical foliage green
val GrassLime = Color(0xFFA3E635)        // Sunlight blade highlight
val GrassDarkGreen = Color(0xFF1B4D2B)   // Deep pine woodland

// Warm humus & soil surfaces
val SoilVoid = Color(0xFF0B100C)         // Deepest organic loam / ground canvas
val SoilVoidDark = SoilVoid
val ForestFloor = Color(0xFF18251B)      // Elevation 1: Primary card slate & bottom dock
val DarkSoilVariant = Color(0xFF121C14)  // Elevation 0: Inset wells & progress slots
val SoilSurfacePanel = Color(0xFF203C2D) // Elevation 2: Elevated chips & tactical selectors
val SoilMidnightNavy = Color(0xFF101914) // Damp humus night tone
val SoilForestDark = SoilVoid

// Structural line weights & tactile borders
val MutedMoss = Color(0xFF283F2F)        // 1.5px tactile boundary stroke
val SoilBorderSubtle = MutedMoss
val SoilBorderActive = GrassNeon
val HardShadowDark = Color(0xFF080D09)   // Flat mechanical 3px drop shadow

// Stamps, Waypoints & Progressions
val SunGold = Color(0xFFF2B84B)          // Wax field stamp / streak multiplier / XP
val WaypointTeal = Color(0xFF66C9C2)     // Cryptographic verified civic badge
val NodeAvailable = GrassNeon
val NodeInProgress = SunGold
val NodeValidated = WaypointTeal
val NodeLocked = Color(0xFF3E4F42)       // Overgrown / locked trail stone
val NodeCritical = Color(0xFFFF6B6B)     // Alert red

// Brand Accents & Compatibility aliases
val CivicEmerald = GrassNeon
val CivicCyan = WaypointTeal
val CivicAmber = SunGold
val CivicRuneViolet = Color(0xFFB57EDC)

val CivicVoidDark = SoilVoid
val CivicMidnightNavy = SoilMidnightNavy
val CivicForestDark = SoilVoid
val CivicSurfaceDark = SoilVoid
val CivicSurfaceCard = ForestFloor
val CivicSurfacePanel = SoilSurfacePanel
val CivicBorderSubtle = MutedMoss
val CivicBorderActive = GrassNeon

// Outdoor Legibility & High Contrast Parchment
val WeatheredPaper = Color(0xFFF3F0DF)   // Dry botanical press bone-white
val FieldFog = Color(0xFFB8C4B6)         // Secondary metadata & narrative text
val MutedLichen = Color(0xFF9AA69B)      // Monospaced telemetry & coordinates
val TextPrimary = WeatheredPaper
val TextSecondary = FieldFog
val TextMuted = MutedLichen
val TextHighlight = GrassNeon
