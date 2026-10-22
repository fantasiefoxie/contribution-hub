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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.theme.CivicAmber
import com.example.contributionos.theme.CivicBorderSubtle
import com.example.contributionos.theme.CivicCyan
import com.example.contributionos.theme.CivicEmerald
import com.example.contributionos.theme.CivicMidnightNavy
import com.example.contributionos.theme.CivicSurfaceCard
import com.example.contributionos.theme.CivicSurfaceDark
import com.example.contributionos.theme.CivicSurfacePanel
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary

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
      .background(CivicSurfaceDark)
  ) {
    // Mode Banner if in Sandbox Demo mode
    if (environmentMode == DataSourceMode.SANDBOX_DEMO) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(CivicAmber.copy(alpha = 0.2f))
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "⚠️ SANDBOX / DEMO SIMULATION (FIXTURE DATA)",
          fontSize = 11.sp,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.5.sp,
          color = CivicAmber
        )
        if (onToggleMode != null) {
          Text(
            "Switch to Live",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = CivicCyan,
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .clickable { onToggleMode() }
              .padding(horizontal = 6.dp, vertical = 2.dp)
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
            CivicAmber
          } else if (isLive) {
            CivicEmerald
          } else {
            TextMuted
          }

          val statusLabel = if (environmentMode == DataSourceMode.SANDBOX_DEMO) {
            "DEV SANDBOX"
          } else if (isLive) {
            "CIVIC REGISTRY (CONNECTED)"
          } else {
            "CIVIC REGISTRY (OFFLINE DRAFT)"
          }

          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(dotColor)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = statusLabel,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = dotColor
          )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = districtName.uppercase(),
          fontSize = 16.sp,
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
              .clip(RoundedCornerShape(6.dp))
              .background(CivicSurfacePanel)
              .border(1.dp, CivicBorderSubtle, RoundedCornerShape(6.dp))
              .clickable { onToggleMode() }
              .padding(horizontal = 8.dp, vertical = 5.dp)
          ) {
            Text("Sandbox", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
          }
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CivicSurfacePanel)
            .border(1.dp, CivicBorderSubtle, RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 5.dp)
        ) {
          Text(
            text = "SEED #$seed",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
          )
        }
      }
    }
  }
}

@Composable
fun LayeredPanel(
  modifier: Modifier = Modifier,
  borderColor: Color = CivicBorderSubtle,
  content: @Composable () -> Unit
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(CivicSurfaceCard)
      .border(1.dp, borderColor, RoundedCornerShape(12.dp))
      .padding(14.dp)
  ) {
    content()
  }
}
