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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.theme.DarkSoilVariant
import com.example.contributionos.theme.ForestFloor
import com.example.contributionos.theme.GrassNeon
import com.example.contributionos.theme.HardShadowDark
import com.example.contributionos.theme.MutedMoss
import com.example.contributionos.theme.SoilSurfacePanel
import com.example.contributionos.theme.SoilVoid
import com.example.contributionos.theme.SunGold
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.theme.WaypointTeal

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
      .background(ForestFloor)
      .bottomBorder(1.dp, MutedMoss)
  ) {
    // Mode Banner if in Sandbox Demo mode
    if (environmentMode == DataSourceMode.SANDBOX_DEMO) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(SunGold.copy(alpha = 0.15f))
          .bottomBorder(1.dp, SunGold.copy(alpha = 0.4f))
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "⚠️ ASTROTURF SANDBOX (FIXTURE DATA)",
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.5.sp,
          color = SunGold
        )
        if (onToggleMode != null) {
          Text(
            "Touch Real Grass",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = WaypointTeal,
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(DarkSoilVariant)
              .border(1.dp, MutedMoss, RoundedCornerShape(4.dp))
              .clickable { onToggleMode() }
              .padding(horizontal = 8.dp, vertical = 3.dp)
          )
        }
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
          val dotColor = if (environmentMode == DataSourceMode.SANDBOX_DEMO) {
            SunGold
          } else if (isLive) {
            GrassNeon
          } else {
            WaypointTeal
          }

          val statusLabel = if (environmentMode == DataSourceMode.SANDBOX_DEMO) {
            "ASTROTURF SIMULATION"
          } else if (isLive) {
            "🌱 TOUCHING GRASS (LIVE)"
          } else {
            "🌱 TOUCHING GRASS (OFFLINE SAFE)"
          }

          Box(
            modifier = Modifier
              .size(7.dp)
              .clip(CircleShape)
              .background(dotColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = statusLabel,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = dotColor
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = districtName.uppercase(),
          fontSize = 14.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.5.sp,
          color = TextPrimary
        )
      }

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        if (onToggleMode != null && environmentMode == DataSourceMode.PRODUCTION) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(DarkSoilVariant)
              .border(1.dp, MutedMoss, RoundedCornerShape(4.dp))
              .clickable { onToggleMode() }
              .padding(horizontal = 7.dp, vertical = 3.dp)
          ) {
            Text("Astroturf", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold, color = TextSecondary)
          }
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(DarkSoilVariant)
            .border(1.dp, MutedMoss, RoundedCornerShape(4.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
          Text(
            text = "SEED #$seed",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
            color = SunGold
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
  borderColor: Color = MutedMoss,
  backgroundColor: Color = ForestFloor,
  shape: RoundedCornerShape = RoundedCornerShape(10.dp),
  padding: Dp = 14.dp,
  content: @Composable () -> Unit
) {
  Box(
    modifier = modifier
      .clip(shape)
      .background(backgroundColor)
      .border(1.5.dp, borderColor, shape)
      .padding(padding)
  ) {
    content()
  }
}

@Composable
fun LayeredPanel(
  modifier: Modifier = Modifier,
  borderColor: Color = MutedMoss,
  backgroundColor: Color = ForestFloor,
  content: @Composable () -> Unit
) {
  TactileCard(
    modifier = modifier,
    borderColor = borderColor,
    backgroundColor = backgroundColor,
    shape = RoundedCornerShape(8.dp),
    padding = 14.dp,
    content = content
  )
}

@Composable
fun SegmentedProgressBar(
  progressPercent: Float,
  modifier: Modifier = Modifier,
  totalSegments: Int = 10,
  activeColor: Color = GrassNeon,
  inactiveColor: Color = DarkSoilVariant,
  borderColor: Color = MutedMoss
) {
  val filledSegments = (progressPercent * totalSegments).toInt().coerceIn(0, totalSegments)
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(3.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    for (i in 0 until totalSegments) {
      val isFilled = i < filledSegments
      Box(
        modifier = Modifier
          .weight(1f)
          .height(6.dp)
          .clip(RoundedCornerShape(1.dp))
          .background(if (isFilled) activeColor else inactiveColor)
          .border(0.75.dp, if (isFilled) activeColor.copy(alpha = 0.5f) else borderColor, RoundedCornerShape(1.dp))
      )
    }
  }
}
