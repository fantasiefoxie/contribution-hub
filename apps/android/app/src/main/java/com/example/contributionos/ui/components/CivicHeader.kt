package com.example.contributionos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.theme.CivicBorderSubtle
import com.example.contributionos.theme.CivicEmerald
import com.example.contributionos.theme.CivicMidnightNavy
import com.example.contributionos.theme.CivicSurfaceCard
import com.example.contributionos.theme.CivicSurfaceDark
import com.example.contributionos.theme.CivicSurfacePanel
import com.example.contributionos.theme.NodeInProgress
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary

@Composable
fun CivicHeader(
  districtName: String,
  seed: Long,
  isLive: Boolean,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier
      .fillMaxWidth()
      .background(CivicSurfaceDark)
      .padding(horizontal = 16.dp, vertical = 10.dp),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically
  ) {
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(if (isLive) CivicEmerald else NodeInProgress)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = if (isLive) "LIVE SUPABASE" else "DEV SANDBOX",
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          color = if (isLive) CivicEmerald else NodeInProgress
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
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .background(CivicSurfacePanel)
        .border(1.dp, CivicBorderSubtle, RoundedCornerShape(8.dp))
        .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
      Text(
        text = "SEED #$seed",
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = TextSecondary
      )
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
