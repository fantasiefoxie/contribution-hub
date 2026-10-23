package com.example.contributionos.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Field Log Roguelite Typography
val Typography =
  Typography(
    displayLarge = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Bold,
      fontSize = 28.sp,
      lineHeight = 34.sp,
      letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.Bold,
      fontSize = 20.sp,
      lineHeight = 26.sp,
      letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
      fontFamily = FontFamily.SansSerif,
      fontWeight = FontWeight.SemiBold,
      fontSize = 16.sp,
      lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
      fontFamily = FontFamily.Serif,
      fontWeight = FontWeight.Normal,
      fontSize = 15.sp,
      lineHeight = 22.sp
    ),
    bodyMedium = TextStyle(
      fontFamily = FontFamily.Serif,
      fontWeight = FontWeight.Normal,
      fontSize = 13.sp,
      lineHeight = 19.sp
    ),
    labelLarge = TextStyle(
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Bold,
      fontSize = 12.sp,
      lineHeight = 16.sp,
      letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
      fontFamily = FontFamily.Monospace,
      fontWeight = FontWeight.Medium,
      fontSize = 10.sp,
      lineHeight = 14.sp,
      letterSpacing = 0.8.sp
    )
  )
