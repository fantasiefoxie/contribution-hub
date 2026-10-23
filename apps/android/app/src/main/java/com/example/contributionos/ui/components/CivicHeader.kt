package com.example.contributionos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.theme.CardBorder
import com.example.contributionos.theme.CardBorderSubtle
import com.example.contributionos.theme.CardCream
import com.example.contributionos.theme.CardCreamLow
import com.example.contributionos.theme.ClayAccent
import com.example.contributionos.theme.ForestInk
import com.example.contributionos.theme.ForestInkDark
import com.example.contributionos.theme.LeafPillContainer
import com.example.contributionos.theme.LeafPillText
import com.example.contributionos.theme.MossGreen
import com.example.contributionos.theme.MossGreenDark
import com.example.contributionos.theme.MutedSecondary
import com.example.contributionos.theme.SoftMeadowSand
import com.example.contributionos.theme.SunGold
import com.example.contributionos.theme.TerracottaContainer
import com.example.contributionos.theme.TerracottaText
import com.example.contributionos.theme.WarmPaper
import com.example.contributionos.theme.WaterBlue

@Composable
fun CivicHeader(
  districtName: String,
  seed: Long,
  isLive: Boolean,
  environmentMode: DataSourceMode = DataSourceMode.PRODUCTION,
  onToggleMode: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .background(CardCream)
      .bottomBorder(1.dp, CardBorder)
  ) {
    // Mode Banner if in Sandbox Demo mode
    if (environmentMode == DataSourceMode.SANDBOX_DEMO) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(TerracottaContainer)
          .bottomBorder(1.dp, CardBorder)
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "🌱 Astroturf Sandbox (Demo Mode)",
          fontSize = 11.sp,
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.SemiBold,
          color = TerracottaText
        )
        if (onToggleMode != null) {
          Text(
            "Switch to Real Outdoor",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MossGreenDark,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(CardCream)
              .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
              .clickable { onToggleMode() }
              .padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(LeafPillContainer)
            .border(1.dp, MossGreen.copy(alpha = 0.3f), CircleShape),
          contentAlignment = Alignment.Center
        ) {
          Text("🌱", fontSize = 16.sp)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column {
          Text(
            text = "Touch Grass",
            fontSize = 17.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = ForestInkDark
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            val dotColor = if (environmentMode == DataSourceMode.SANDBOX_DEMO) SunGold else MossGreen
            val statusLabel = if (environmentMode == DataSourceMode.SANDBOX_DEMO) {
              "Sandbox Explorer"
            } else if (isLive) {
              "Outdoor Companion • Live"
            } else {
              "Offline Safe Explorer"
            }

            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
              text = statusLabel,
              fontSize = 11.sp,
              fontFamily = FontFamily.SansSerif,
              fontWeight = FontWeight.Medium,
              color = MutedSecondary
            )
          }
        }
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        if (onToggleMode != null && environmentMode == DataSourceMode.PRODUCTION) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(CardCreamLow)
              .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
              .clickable { onToggleMode() }
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              "Sandbox",
              fontSize = 11.sp,
              fontFamily = FontFamily.SansSerif,
              fontWeight = FontWeight.Medium,
              color = MutedSecondary
            )
          }
        }

        // Region / District Pill
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LeafPillContainer)
            .border(1.dp, MossGreen.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 9.dp, vertical = 4.dp)
        ) {
          Text(
            text = districtName.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            color = LeafPillText
          )
        }
      }
    }
  }
}

fun Modifier.bottomBorder(strokeWidth: Dp, color: Color): Modifier = this.drawBehind {
  val stroke = strokeWidth.toPx()
  drawLine(
    color = color,
    start = Offset(0f, size.height - stroke / 2),
    end = Offset(size.width, size.height - stroke / 2),
    strokeWidth = stroke
  )
}

fun Modifier.topBorder(strokeWidth: Dp, color: Color): Modifier = this.drawBehind {
  val stroke = strokeWidth.toPx()
  drawLine(
    color = color,
    start = Offset(0f, stroke / 2),
    end = Offset(size.width, stroke / 2),
    strokeWidth = stroke
  )
}

@Composable
fun TactileCard(
  modifier: Modifier = Modifier,
  borderColor: Color = CardBorder,
  backgroundColor: Color = CardCream,
  shape: RoundedCornerShape = RoundedCornerShape(16.dp),
  padding: Dp = 16.dp,
  content: @Composable () -> Unit
) {
  Box(
    modifier = modifier
      .clip(shape)
      .background(backgroundColor)
      .border(1.dp, borderColor, shape)
      .padding(padding)
  ) {
    content()
  }
}

@Composable
fun LayeredPanel(
  modifier: Modifier = Modifier,
  borderColor: Color = CardBorder,
  backgroundColor: Color = CardCream,
  content: @Composable () -> Unit
) {
  TactileCard(
    modifier = modifier,
    borderColor = borderColor,
    backgroundColor = backgroundColor,
    shape = RoundedCornerShape(14.dp),
    padding = 14.dp,
    content = content
  )
}

@Composable
fun BotanicalProgressBar(
  progressPercent: Float,
  modifier: Modifier = Modifier,
  activeColor: Color = MossGreen,
  trackColor: Color = CardBorderSubtle,
  height: Dp = 8.dp
) {
  val clampedProgress = progressPercent.coerceIn(0f, 1f)
  Box(
    modifier = modifier
      .fillMaxWidth()
      .height(height)
      .clip(RoundedCornerShape(999.dp))
      .background(trackColor)
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth(clampedProgress)
        .height(height)
        .clip(RoundedCornerShape(999.dp))
        .background(activeColor)
    )
  }
}

@Composable
fun SegmentedProgressBar(
  progressPercent: Float,
  modifier: Modifier = Modifier,
  totalSegments: Int = 10,
  activeColor: Color = MossGreen,
  inactiveColor: Color = CardBorderSubtle,
  borderColor: Color = CardBorder
) {
  BotanicalProgressBar(
    progressPercent = progressPercent,
    modifier = modifier,
    activeColor = activeColor,
    trackColor = inactiveColor,
    height = 8.dp
  )
}
