package com.example.contributionos.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ============================================================================
// TOUCH GRASS — ILLUSTRATED NATURE ADVENTURE TYPOGRAPHY
// Warm editorial serif display typography paired with clean, readable sans-serif
// body and label text for effortless reading in outdoor sunlight.
// ============================================================================
val Typography =
  Typography(
    displayLarge = TextStyle(
      fontFamily = FontFamily.Serif,
      fontWeight = FontWeight.Bold,
      fontSize = 28.sp,
      lineHeight = 36.sp,
      letterSpacing = (-0.3).sp
    ),
    headlineMedium = TextStyle(
      fontFamily = FontFamily.Serif,
      fontWeight = FontWeight.Bold,
      fontSize = 22.sp,
      lineHeight = 28.sp,
      letterSpacing = (-0.2).sp
    ),
    headlineSmall = TextStyle(
      fontFamily = FontFamily.Serif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 18.sp,
      lineHeight = 24.sp
    ),
    titleMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 16.sp,
      lineHeight = 22.sp
    ),
    titleSmall = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 14.sp,
      lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 15.sp,
      lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 13.sp,
      lineHeight = 19.sp
    ),
    bodySmall = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Normal,
      fontSize = 12.sp,
      lineHeight = 17.sp
    ),
    labelLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 13.sp,
      lineHeight = 18.sp
    ),
    labelMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 11.sp,
      lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Medium,
      fontSize = 10.sp,
      lineHeight = 14.sp
    )
  )
