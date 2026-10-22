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
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.ui.components.CivicHeader
import com.example.contributionos.ui.components.LayeredPanel

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
  val verifiedSubmissions = submissions.filter { it.status == SubmissionStatus.VALIDATED }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CivicForestDark)
  ) {
    CivicHeader(
      districtName = "Collective Impact Ledger",
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
      // Impact Integrity Notice
      LayeredPanel(modifier = Modifier.fillMaxWidth(), borderColor = CivicCyan.copy(alpha = 0.5f)) {
        Row(verticalAlignment = Alignment.Top) {
          Icon(Icons.Default.Info, contentDescription = null, tint = CivicCyan, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              "SERVER-AUTHORITATIVE VERIFICATION",
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.5.sp,
              color = CivicCyan
            )
            Text(
              "Civic milestones represent real-world verified actions approved by accredited validators. Game runs may be rerolled, but verified impact records persist permanently in the civic registry.",
              fontSize = 12.sp,
              color = TextSecondary,
              lineHeight = 17.sp
            )
          }
        }
      }

      // Community Milestone Goals
      Text(
        "COLLECTIVE CIVIC MILESTONES",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = TextMuted
      )

      if (milestones.isEmpty()) {
        LayeredPanel(modifier = Modifier.fillMaxWidth()) {
          Text(
            "No collective civic milestones currently loaded from the registry. Active initiative goals will appear here as community actions are aggregated.",
            fontSize = 13.sp,
            color = TextMuted,
            lineHeight = 18.sp
          )
        }
      } else {
        milestones.forEach { milestone ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CivicSurfaceCard),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CivicBorderSubtle)
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
                  fontWeight = FontWeight.Bold,
                  color = CivicEmerald
                )
                Text(
                  "${milestone.currentValue} / ${milestone.targetValue} ${milestone.unit}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Bold,
                  color = TextSecondary
                )
              }

              Text(milestone.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
              Text(milestone.description, fontSize = 12.sp, color = TextMuted)

              LinearProgressIndicator(
                progress = { milestone.percent },
                modifier = Modifier
                  .fillMaxWidth()
                  .height(6.dp)
                  .clip(RoundedCornerShape(3.dp)),
                color = if (milestone.isAchieved) CivicEmerald else CivicCyan,
                trackColor = CivicSurfaceDark
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text(
                  "${(milestone.percent * 100).toInt()}% Met",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = CivicCyan
                )
                if (milestone.isAchieved) {
                  Text("✓ Goal Achieved", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CivicEmerald)
                }
              }
            }
          }
        }
      }

      // Permanent Contributor History
      Text(
        "MY PERMANENT VERIFIED CONTRIBUTIONS",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = TextMuted
      )

      if (verifiedSubmissions.isEmpty()) {
        LayeredPanel(modifier = Modifier.fillMaxWidth()) {
          Text(
            "No verified contributions recorded on-chain yet. Submit field evidence for an open task to receive accredited validator approval.",
            fontSize = 13.sp,
            color = TextMuted,
            lineHeight = 18.sp
          )
        }
      } else {
        verifiedSubmissions.forEach { sub ->
          Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = CivicSurfaceCard),
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, CivicEmerald.copy(alpha = 0.5f))
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
                  Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CivicEmerald, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text("APPROVED CITIZEN RECORD", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CivicEmerald)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(sub.taskTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (sub.feedback != null) {
                  Text(sub.feedback, fontSize = 11.sp, color = TextMuted)
                }
              }

              if (sub.scoreAwarded != null) {
                Text("+${sub.scoreAwarded} XP", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CivicAmber)
              }
            }
          }
        }
      }
    }
  }
}
