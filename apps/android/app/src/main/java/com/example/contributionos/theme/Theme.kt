package com.example.contributionos.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CivicRogueliteColorScheme = darkColorScheme(
  primary = CivicEmerald,
  onPrimary = CivicVoidDark,
  primaryContainer = CivicSurfacePanel,
  onPrimaryContainer = TextHighlight,
  secondary = CivicCyan,
  onSecondary = CivicVoidDark,
  secondaryContainer = CivicMidnightNavy,
  onSecondaryContainer = Color(0xFFA5F3FC),
  tertiary = CivicAmber,
  onTertiary = CivicVoidDark,
  background = CivicForestDark,
  onBackground = TextPrimary,
  surface = CivicSurfaceDark,
  onSurface = TextPrimary,
  surfaceVariant = CivicSurfaceCard,
  onSurfaceVariant = TextSecondary,
  outline = CivicBorderSubtle,
  outlineVariant = CivicBorderActive
)

@Composable
fun ContributionOSTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = CivicRogueliteColorScheme,
    typography = Typography,
    content = content
  )
}
