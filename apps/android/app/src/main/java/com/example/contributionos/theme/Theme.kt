package com.example.contributionos.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TouchGrassColorScheme = lightColorScheme(
  primary = MossGreenDark,
  onPrimary = Color.White,
  primaryContainer = LeafPillContainer,
  onPrimaryContainer = LeafPillText,
  secondary = ClayAccent,
  onSecondary = Color.White,
  secondaryContainer = TerracottaContainer,
  onSecondaryContainer = TerracottaText,
  tertiary = WaterBlue,
  onTertiary = Color.White,
  tertiaryContainer = WaterBlueContainer,
  onTertiaryContainer = WaterBlueText,
  background = WarmPaper,
  onBackground = ForestInk,
  surface = CardCream,
  onSurface = ForestInk,
  surfaceVariant = CardCreamLow,
  onSurfaceVariant = MutedSecondary,
  outline = BoundaryLine,
  outlineVariant = CardBorder
)

@Composable
fun ContributionOSTheme(
  content: @Composable () -> Unit,
) {
  MaterialTheme(
    colorScheme = TouchGrassColorScheme,
    typography = Typography,
    content = content
  )
}

@Composable
fun TouchGrassTheme(
  content: @Composable () -> Unit,
) {
  ContributionOSTheme(content = content)
}
