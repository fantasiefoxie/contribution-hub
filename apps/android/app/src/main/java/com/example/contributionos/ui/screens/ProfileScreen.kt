package com.example.contributionos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.theme.CivicAmber
import com.example.contributionos.theme.CivicBorderActive
import com.example.contributionos.theme.CivicBorderSubtle
import com.example.contributionos.theme.CivicCyan
import com.example.contributionos.theme.CivicEmerald
import com.example.contributionos.theme.CivicForestDark
import com.example.contributionos.theme.CivicMidnightNavy
import com.example.contributionos.theme.CivicSurfaceCard
import com.example.contributionos.theme.CivicSurfaceDark
import com.example.contributionos.theme.CivicSurfacePanel
import com.example.contributionos.theme.TextHighlight
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.ui.components.CivicHeader
import com.example.contributionos.ui.components.LayeredPanel

@Composable
fun ProfileScreen(
  profile: CivicProfile,
  draftCount: Int,
  submissionCount: Int,
  isLive: Boolean,
  environmentMode: DataSourceMode = DataSourceMode.PRODUCTION,
  onSetEnvironmentMode: (DataSourceMode) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CivicForestDark)
  ) {
    CivicHeader(
      districtName = "Contributor Dossier",
      seed = 42L,
      isLive = isLive,
      environmentMode = environmentMode
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Profile Hero Card
      LayeredPanel(modifier = Modifier.fillMaxWidth(), borderColor = CivicBorderActive) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(CivicSurfaceDark)
                .border(2.dp, CivicEmerald, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text("L${profile.level}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = TextHighlight)
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(profile.displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
              Text("Callsign: ${profile.callsign}", fontSize = 12.sp, color = CivicCyan)
              Text(profile.rankTitle, fontSize = 12.sp, color = TextMuted)
            }
          }

          // XP Progress Bar
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text("Progress to Level ${profile.level + 1}", fontSize = 11.sp, color = TextMuted)
              Text("${profile.xp} / ${profile.nextLevelXp} XP", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CivicAmber)
            }
            LinearProgressIndicator(
              progress = { if (profile.nextLevelXp == 0) 0f else profile.xp.toFloat() / profile.nextLevelXp.toFloat() },
              modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = CivicAmber,
              trackColor = CivicSurfaceDark
            )
          }

          // Lifetime Stats
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            StatBox(label = "Verified Actions", value = "${profile.verifiedContributionsCount}")
            StatBox(label = "Expeditions", value = "${profile.completedExpeditionsCount}")
            StatBox(label = "Active Streak", value = "${profile.activeStreakDays}d")
          }
        }
      }

      // ENVIRONMENT MODE TOGGLE
      LayeredPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (environmentMode == DataSourceMode.SANDBOX_DEMO) CivicAmber else CivicEmerald.copy(alpha = 0.5f)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "DATA ENVIRONMENT CONFIGURATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            color = if (environmentMode == DataSourceMode.SANDBOX_DEMO) CivicAmber else CivicEmerald
          )

          Text(
            if (environmentMode == DataSourceMode.PRODUCTION) {
              "CURRENT: PRODUCTION REGISTRY\nConnected to live Supabase backend. Displays authentic community tasks and verified on-chain contributions only."
            } else {
              "CURRENT: ISOLATED SANDBOX / DEMO SIMULATION\nUsing illustrative mock fixtures to demonstrate the roguelite route tree. Submissions in sandbox mode are isolated from production."
            },
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 17.sp
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { onSetEnvironmentMode(DataSourceMode.PRODUCTION) },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (environmentMode == DataSourceMode.PRODUCTION) CivicEmerald else CivicSurfacePanel
              ),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                "Production Mode",
                color = if (environmentMode == DataSourceMode.PRODUCTION) Color.Black else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }

            Button(
              onClick = { onSetEnvironmentMode(DataSourceMode.SANDBOX_DEMO) },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (environmentMode == DataSourceMode.SANDBOX_DEMO) CivicAmber else CivicSurfacePanel
              ),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                "Sandbox Demo",
                color = if (environmentMode == DataSourceMode.SANDBOX_DEMO) Color.Black else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
          }
        }
      }

      // Badges & Achievements
      Text("CIVIC ACHIEVEMENTS & MEDALS", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = TextMuted)

      profile.badges.chunked(2).forEach { rowBadges ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          rowBadges.forEach { badge ->
            Card(
              modifier = Modifier.weight(1f),
              colors = CardDefaults.cardColors(containerColor = CivicSurfaceCard),
              shape = RoundedCornerShape(10.dp),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (badge.isUnlocked) CivicEmerald.copy(alpha = 0.5f) else CivicBorderSubtle
              )
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(badge.iconGlyph, fontSize = 24.sp)
                Text(badge.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (badge.isUnlocked) TextPrimary else TextMuted)
                Text(badge.description, fontSize = 11.sp, color = TextMuted)
                Text(
                  if (badge.isUnlocked) "UNLOCKED" else "LOCKED",
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (badge.isUnlocked) CivicEmerald else TextMuted
                )
              }
            }
          }
          if (rowBadges.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }

      // Offline Diagnostics & Integrity
      LayeredPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("DEVICE & OFFLINE ENGINE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
          Text(
            "Local Storage: $draftCount offline draft(s) stored • $submissionCount tracked submission(s)",
            fontSize = 12.sp,
            color = TextSecondary
          )
          Text(
            "Engine Status: Room/JSON resilient persistent draft store active. Survives device restarts.",
            fontSize = 11.sp,
            color = CivicCyan
          )
        }
      }
    }
  }
}

@Composable
private fun StatBox(label: String, value: String) {
  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(CivicSurfacePanel)
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    Text(label, fontSize = 10.sp, color = TextMuted)
  }
}
