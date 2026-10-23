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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
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
import com.example.contributionos.theme.SunGoldContainer
import com.example.contributionos.theme.SunGoldText
import com.example.contributionos.theme.TerracottaContainer
import com.example.contributionos.theme.TerracottaText
import com.example.contributionos.theme.WarmPaper
import com.example.contributionos.theme.WaterBlue
import com.example.contributionos.theme.WaterBlueContainer
import com.example.contributionos.theme.WaterBlueText
import com.example.contributionos.ui.components.BotanicalProgressBar
import com.example.contributionos.ui.components.CivicHeader
import com.example.contributionos.ui.components.TactileCard

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
      .background(WarmPaper)
  ) {
    CivicHeader(
      districtName = "Community & Civic Impact",
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
      // Dual-Ledger Separation Principle Card
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CardBorder,
        backgroundColor = CardCream
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(TerracottaContainer)
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                "DUAL-LEDGER SEPARATION",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = TerracottaText
              )
            }
          }

          Text(
            "Exploration XP ≠ Civic Credits",
            fontSize = 18.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = ForestInkDark
          )

          Text(
            "Exploration XP measures your personal outdoor nature adventures and local route mastery. Civic Credits represent cryptographically verified public contributions (such as municipal tree audits, trail maintenance, and water catchment surveys) reviewed by community stewards.",
            fontSize = 13.sp,
            color = ForestInk,
            lineHeight = 18.sp
          )

          // Side-by-Side Comparison Counters
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(CardCreamLow)
                .border(1.dp, MossGreen.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  "PERSONAL PROGRESSION",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp,
                  color = MossGreenDark
                )
                Text(
                  "Exploration XP",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = ForestInkDark
                )
                Text(
                  "Local on-device leveling and route mastery.",
                  fontSize = 11.sp,
                  color = MutedSecondary,
                  lineHeight = 14.sp
                )
              }
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(CardCreamLow)
                .border(1.dp, WaterBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                .padding(12.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                  "PUBLIC RECOGNITION",
                  fontSize = 9.sp,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.5.sp,
                  color = WaterBlue
                )
                Text(
                  "Civic Credits",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = ForestInkDark
                )
                Text(
                  "Accredited community and municipal audit.",
                  fontSize = 11.sp,
                  color = MutedSecondary,
                  lineHeight = 14.sp
                )
              }
            }
          }
        }
      }

      // Community Milestone Goals
      Text(
        "Collective Community Milestones",
        fontSize = 16.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        color = ForestInkDark
      )

      if (milestones.isEmpty()) {
        TactileCard(
          modifier = Modifier.fillMaxWidth(),
          borderColor = CardBorder,
          backgroundColor = CardCream
        ) {
          Text(
            "No collective civic milestones currently loaded from the registry. Active initiative goals will appear here as community actions are aggregated.",
            fontSize = 13.sp,
            color = MutedSecondary,
            lineHeight = 18.sp
          )
        }
      } else {
        milestones.forEach { milestone ->
          TactileCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = CardBorder,
            backgroundColor = CardCream
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(LeafPillContainer)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
                ) {
                  Text(
                    milestone.category.replaceFirstChar { it.uppercase() },
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = LeafPillText
                  )
                }
                Text(
                  "${milestone.currentValue} / ${milestone.targetValue} ${milestone.unit}",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = MutedSecondary
                )
              }

              Text(
                milestone.title,
                fontSize = 16.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ForestInkDark
              )
              Text(
                milestone.description,
                fontSize = 12.sp,
                color = MutedSecondary,
                lineHeight = 16.sp
              )

              BotanicalProgressBar(
                progressPercent = milestone.percent,
                activeColor = if (milestone.isAchieved) MossGreen else WaterBlue,
                trackColor = CardBorderSubtle,
                height = 8.dp
              )

              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "${(milestone.percent * 100).toInt()}% Achieved",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.SemiBold,
                  color = if (milestone.isAchieved) MossGreenDark else WaterBlue
                )
                if (milestone.isAchieved) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                      Icons.Default.CheckCircle,
                      contentDescription = null,
                      tint = MossGreen,
                      modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      "Milestone Achieved",
                      fontSize = 11.sp,
                      fontWeight = FontWeight.SemiBold,
                      color = MossGreenDark
                    )
                  }
                }
              }
            }
          }
        }
      }

      // Permanent Contributor History
      Text(
        "Permanent Proof Ledger",
        fontSize = 16.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        color = ForestInkDark
      )

      if (verifiedSubmissions.isEmpty()) {
        TactileCard(
          modifier = Modifier.fillMaxWidth(),
          borderColor = CardBorder,
          backgroundColor = CardCream
        ) {
          Text(
            "No verified contributions recorded yet. Submit field evidence for an open side quest to receive community approval.",
            fontSize = 13.sp,
            color = MutedSecondary,
            lineHeight = 18.sp
          )
        }
      } else {
        verifiedSubmissions.forEach { sub ->
          TactileCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = CardBorder,
            backgroundColor = CardCream
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MossGreen,
                    modifier = Modifier.size(15.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    "Verified Community Lore",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MossGreenDark
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  sub.taskTitle,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = ForestInkDark
                )
                if (sub.feedback != null) {
                  Text(
                    sub.feedback,
                    fontSize = 12.sp,
                    color = MutedSecondary,
                    lineHeight = 16.sp
                  )
                }
              }

              if (sub.scoreAwarded != null) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(SunGoldContainer)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(
                    "+${sub.scoreAwarded} XP",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = SunGoldText
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}
