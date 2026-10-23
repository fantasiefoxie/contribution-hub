package com.example.contributionos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.CommunityMilestone
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.model.SubmissionStatus
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
import com.example.contributionos.theme.DarkSoilVariant
import com.example.contributionos.theme.ForestFloor
import com.example.contributionos.theme.GrassNeon
import com.example.contributionos.theme.MutedMoss
import com.example.contributionos.theme.SoilVoid
import com.example.contributionos.theme.SunGold
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.theme.WaypointTeal
import com.example.contributionos.theme.WeatheredPaper
import com.example.contributionos.ui.components.CivicHeader
import com.example.contributionos.ui.components.LayeredPanel
import com.example.contributionos.ui.components.SegmentedProgressBar

@Composable
fun ImpactScreen(
  milestones: List<CommunityMilestone>,
  submissions: List<CivicSubmission>,
  isLive: Boolean,
  environmentMode: DataSourceMode = DataSourceMode.PRODUCTION,
  onToggleMode: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()
  val verifiedSubmissions = submissions.filter { it.status == SubmissionStatus.APPROVED || it.status == SubmissionStatus.VALIDATED }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(SoilVoid)
  ) {
    CivicHeader(
      districtName = "CIVIC IMPACT // PUBLIC LEDGER",
      seed = 42L,
      isLive = isLive,
      environmentMode = environmentMode,
      onToggleMode = onToggleMode
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Critical Dual-Ledger Separation Plate
      LayeredPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = SunGold.copy(alpha = 0.5f)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(SunGold.copy(alpha = 0.2f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                "DUAL-LEDGER SEPARATION",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = SunGold
              )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "Exploration XP ≠ Civic Credits",
              fontSize = 13.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = WeatheredPaper
            )
          }

          Text(
            "Exploration XP measures your personal outdoor nature adventures. Civic Credits represent cryptographically verified public contributions (e.g. municipal tree canopy audits, trail maintenance, water catchment reporting).",
            fontSize = 12.sp,
            fontFamily = FontFamily.Serif,
            color = TextSecondary,
            lineHeight = 17.sp
          )

          // Side-by-Side Comparison Counters
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSoilVariant)
                .border(1.dp, MutedMoss, RoundedCornerShape(6.dp))
                .padding(10.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("PERSONAL ADVENTURE", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                Text("Exploration XP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = GrassNeon)
                Text("Local on-device leveling", fontSize = 10.sp, fontFamily = FontFamily.Serif, color = TextSecondary)
              }
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSoilVariant)
                .border(1.dp, WaypointTeal.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                .padding(10.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("PUBLIC CIVIC PROOF", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = WaypointTeal)
                Text("0 Civic Credits", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WaypointTeal)
                Text("Accredited community audit", fontSize = 10.sp, fontFamily = FontFamily.Serif, color = TextSecondary)
              }
            }
          }
        }
      }

      // Community Milestone Goals
      Text(
        "COLLECTIVE BIOME MILESTONES",
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = TextMuted
      )

      if (milestones.isEmpty()) {
        LayeredPanel(modifier = Modifier.fillMaxWidth()) {
          Text(
            "No collective civic milestones currently loaded from the registry. Active initiative goals will appear here as community actions are aggregated.",
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif,
            color = TextMuted,
            lineHeight = 18.sp
          )
        }
      } else {
        milestones.forEach { milestone ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ForestFloor),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MutedMoss)
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  milestone.category.uppercase(),
                  fontSize = 10.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = GrassNeon
                )
                Text(
                  "${milestone.currentValue} / ${milestone.targetValue} ${milestone.unit}",
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = TextSecondary
                )
              }

              Text(milestone.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = WeatheredPaper)
              Text(milestone.description, fontSize = 12.sp, fontFamily = FontFamily.Serif, color = TextMuted)

              SegmentedProgressBar(
                progressPercent = milestone.percent,
                totalSegments = 10,
                activeColor = if (milestone.isAchieved) GrassNeon else WaypointTeal,
                inactiveColor = DarkSoilVariant
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  "${(milestone.percent * 100).toInt()}% Met",
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = WaypointTeal
                )
                if (milestone.isAchieved) {
                  Text("✓ Lore Milestone Achieved", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = GrassNeon)
                }
              }
            }
          }
        }
      }

      // Permanent Contributor History
      Text(
        "MY PERMANENT PROOF LEDGER",
        fontSize = 11.sp,
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = TextMuted
      )

      if (verifiedSubmissions.isEmpty()) {
        LayeredPanel(modifier = Modifier.fillMaxWidth()) {
          Text(
            "No verified contributions recorded yet. Submit field evidence for an open side quest to receive community approval.",
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif,
            color = TextMuted,
            lineHeight = 18.sp
          )
        }
      } else {
        verifiedSubmissions.forEach { sub ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = ForestFloor),
            shape = RoundedCornerShape(8.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MutedMoss)
          ) {
            Row(
              modifier = Modifier
                .padding(14.dp)
                .fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = GrassNeon, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("🌱 VERIFIED CITIZEN LORE", fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = GrassNeon)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(sub.taskTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = WeatheredPaper)
                if (sub.feedback != null) {
                  Text(sub.feedback, fontSize = 11.sp, fontFamily = FontFamily.Serif, color = TextMuted)
                }
              }

              if (sub.scoreAwarded != null) {
                Text(
                  "+${sub.scoreAwarded} XP",
                  fontSize = 13.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = SunGold
                )
              }
            }
          }
        }
      }
    }
  }
}
