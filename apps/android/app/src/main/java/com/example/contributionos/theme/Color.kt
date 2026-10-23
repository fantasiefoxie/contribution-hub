package com.example.contributionos.theme

import androidx.compose.ui.graphics.Color

// ============================================================================
// TOUCH GRASS — ILLUSTRATED NATURE ADVENTURE PALETTE
// A contemporary illustrated nature-adventure game with the warmth of an indie
// exploration game and the clarity of a well-designed outdoor companion app.
// ============================================================================

// Core Canvas & Paper Surfaces
val WarmPaper = Color(0xFFF5F1E6)          // Warm sunlit paper background (#F5F1E6)
val CardCream = Color(0xFFFFFDF7)          // Parchment card stock (#FFFDF7)
val CardCreamLow = Color(0xFFF8F5EC)       // Recessed surface / input wells
val SoftMeadowSand = Color(0xFFF2EDE0)     // Surface container / subtle fills
val SandHigh = Color(0xFFEAE4D5)           // Elevated chips / headers

// Primary Botanical Ink & Greens
val ForestInk = Color(0xFF293B2D)          // Deep readable forest ink (#293B2D)
val ForestInkDark = Color(0xFF1B291E)      // Deepest forest ink for display titles
val MossGreen = Color(0xFF718653)          // Rich moss botanical accent (#718653)
val MossGreenDark = Color(0xFF4F6233)      // Primary action CTA button fill
val NewLeafGreen = Color(0xFFB8CC8A)       // Tender new-leaf sprout highlight (#B8CC8A)
val LeafPillContainer = Color(0xFFEBF2DC)  // Soft leaf badge background
val LeafPillText = Color(0xFF324823)       // Contrast text inside leaf badge

// Warm Terracotta & Clay Accents
val ClayAccent = Color(0xFFD98656)         // Warm clay / adventure milestones (#D98656)
val ClayAccentDark = Color(0xFF914C21)     // Deep terracotta for high contrast
val TerracottaContainer = Color(0xFFFCECE2)// Soft clay / accessible badge container
val TerracottaText = Color(0xFF5A2711)     // Contrast text inside clay container

// Waterway & Atmospheric Accents
val WaterBlue = Color(0xFF4F726C)          // Creek & bioswale waterway teal
val WaterBlueContainer = Color(0xFFDCEDE9) // Waterway badge container
val WaterBlueText = Color(0xFF173530)

// Warm Sun & Streak Accents
val SunGold = Color(0xFFE5A04D)            // Warm morning sun / streak multiplier
val SunGoldContainer = Color(0xFFFDF4E5)   // Sun gold badge container
val SunGoldText = Color(0xFF5A3911)

// Muted Typography & Boundaries
val MutedSecondary = Color(0xFF70796D)     // Muted secondary narrative text (#70796D)
val CardBorder = Color(0xFFE2DDD1)         // Delicate organic card border
val CardBorderSubtle = Color(0xFFEFEBE1)   // Lightest hairline divider
val BoundaryLine = Color(0xFFC8C2B3)       // Tactile border outline

// Semantic & Status Colors
val StatusAlertRed = Color(0xFFBA1A1A)
val StatusAlertContainer = Color(0xFFFFDAD6)
val NodeAvailable = MossGreen
val NodeInProgress = SunGold
val NodeValidated = WaterBlue
val NodeLocked = Color(0xFFA5ABA0)
val NodeCritical = StatusAlertRed

// Primary High-Level Roles
val TextPrimary = ForestInk
val TextSecondary = MutedSecondary
val TextMuted = Color(0xFF8A9387)
val TextHighlight = MossGreen

// ============================================================================
// BACKWARD-COMPATIBILITY ALIASES (Ensures existing references resolve smoothly)
// ============================================================================
val SoilVoid = WarmPaper
val SoilVoidDark = WarmPaper
val ForestFloor = CardCream
val DarkSoilVariant = CardCreamLow
val SoilSurfacePanel = SoftMeadowSand
val SoilMidnightNavy = CardCream
val SoilForestDark = WarmPaper
val MutedMoss = CardBorder
val SoilBorderSubtle = CardBorderSubtle
val SoilBorderActive = MossGreen
val HardShadowDark = Color(0x15293B2D)     // Gentle organic paper shadow
val GrassNeon = MossGreenDark              // Replaced neon green with rich forest moss
val GrassSprout = MossGreen
val GrassLime = NewLeafGreen
val GrassDarkGreen = ForestInkDark
val WaypointTeal = WaterBlue
val WeatheredPaper = ForestInk             // Inverted: text is now dark ink
val FieldFog = MutedSecondary
val MutedLichen = MutedSecondary

val CivicEmerald = MossGreen
val CivicCyan = WaterBlue
val CivicAmber = SunGold
val CivicRuneViolet = ClayAccent
val CivicVoidDark = WarmPaper
val CivicMidnightNavy = WarmPaper
val CivicForestDark = WarmPaper
val CivicSurfaceDark = WarmPaper
val CivicSurfaceCard = CardCream
val CivicSurfacePanel = SoftMeadowSand
val CivicBorderSubtle = CardBorder
val CivicBorderActive = MossGreen
