package com.example.contributionos.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TouchGrassColorScheme = darkColorScheme(
  primary = GrassNeon,
  onPrimary = SoilVoid,
  primaryContainer = ForestFloor,
  onPrimaryContainer = GrassNeon,
  secondary = SunGold,
  onSecondary = SoilVoid,
  secondaryContainer = SoilSurfacePanel,
  onSecondaryContainer = WeatheredPaper,
  tertiary = WaypointTeal,
  onTertiary = SoilVoid,
  background = SoilVoid,
  onBackground = WeatheredPaper,
  surface = ForestFloor,
  onSurface = WeatheredPaper,
  surfaceVariant = SoilSurfacePanel,
  onSurfaceVariant = FieldFog,
  outline = MutedMoss,
  outlineVariant = GrassNeon
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
