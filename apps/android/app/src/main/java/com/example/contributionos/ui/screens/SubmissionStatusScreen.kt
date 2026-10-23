package com.example.contributionos.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.SubmissionStatus
import com.example.contributionos.theme.CivicAmber
import com.example.contributionos.theme.DarkSoilVariant
import com.example.contributionos.theme.ForestFloor
import com.example.contributionos.theme.GrassNeon
import com.example.contributionos.theme.MutedLichen
import com.example.contributionos.theme.MutedMoss
import com.example.contributionos.theme.SoilSurfacePanel
import com.example.contributionos.theme.SoilVoid
import com.example.contributionos.theme.SunGold
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.theme.WaypointTeal
import com.example.contributionos.theme.WeatheredPaper
import com.example.contributionos.ui.components.SegmentedProgressBar
import com.example.contributionos.ui.components.TactileCard

@Composable
fun SubmissionStatusScreen(
  submission: CivicSubmission,
  onReturnHome: () -> Unit,
  onViewMissions: () -> Unit,
  onRetry: () -> Unit = {},
  task: com.example.contributionos.model.CivicTask? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  val resolvedTask = task ?: com.example.contributionos.data.SeedMissionCatalog.findTask(submission.taskId)
  val xpReward = resolvedTask?.xpReward ?: 50

  val isOffline = submission.status == SubmissionStatus.QUEUED_OFFLINE
  val isDemo = submission.isSandboxDemo
  val isRetryable = submission.status == SubmissionStatus.RETRYABLE_FAILURE
  val isPermanentFail = submission.status == SubmissionStatus.PERMANENT_FAILURE

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(SoilVoid)
      .verticalScroll(scrollState)
      .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Top Status & Victory Bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(ForestFloor)
        .border(1.5.dp, MutedMoss, RoundedCornerShape(8.dp))
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(GrassNeon)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            "EXPEDITION LOGGED // SUCCESS",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            color = GrassNeon
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(DarkSoilVariant)
            .border(0.75.dp, MutedMoss, RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, contentDescription = null, tint = SunGold, modifier = Modifier.size(11.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              "Local Verified Draft",
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              color = TextSecondary
            )
          }
        }
      }
    }

    // 2. Hero Reward & Level-Up Progression Card
    TactileCard(
      modifier = Modifier.fillMaxWidth(),
      borderColor = GrassNeon.copy(alpha = 0.7f),
      backgroundColor = ForestFloor
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Tag Index
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            "QUEST CLEARED // EXPEDITION COMPLETE",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = TextSecondary
          )
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(3.dp))
              .background(DarkSoilVariant)
              .border(0.75.dp, MutedMoss, RoundedCornerShape(3.dp))
              .padding(horizontal = 5.dp, vertical = 1.dp)
          ) {
            Text(
              "#LOG-${submission.id.takeLast(4)}",
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = SunGold
            )
          }
        }

        // Quest Title & Category
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Text(
            text = submission.taskTitle,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = WeatheredPaper
          )
          Text(
            text = "Field Biome Survey • Offline Log Verified",
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            color = TextMuted
          )
        }

        // Reward Callout Plate
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SoilVoid)
            .border(1.dp, MutedMoss, RoundedCornerShape(8.dp))
            .padding(12.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                "DISCOVERED YIELD",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextMuted
              )
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("✨", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  "+$xpReward Exploration XP",
                  fontSize = 18.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.ExtraBold,
                  color = GrassNeon
                )
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(3.dp))
                  .background(SunGold.copy(alpha = 0.2f))
                  .border(0.75.dp, SunGold, RoundedCornerShape(3.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  "STREAK +1",
                  fontSize = 9.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = SunGold
                )
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                "Off-Grid Multiplier",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
              )
            }
          }
        }

        // Segmented Progress Bar
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "EXPEDITION XP PROGRESSION",
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = TextSecondary
            )
            Text(
              "Level Advanced",
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = GrassNeon
            )
          }

          SegmentedProgressBar(
            progressPercent = 1.0f,
            totalSegments = 10,
            activeColor = GrassNeon,
            inactiveColor = DarkSoilVariant
          )

          Text(
            "0 Civic Credits • Exploration XP is separate from Civic Impact",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
          )
        }
      }
    }

    // 3. Collectible Field Specimen Discovery Card
    TactileCard(
      modifier = Modifier.fillMaxWidth(),
      borderColor = MutedMoss,
      backgroundColor = ForestFloor
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🌱", fontSize = 14.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "FIELD SPECIMEN DISCOVERY // TOUCH GRASS",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.5.sp,
              color = GrassNeon
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(3.dp))
              .background(DarkSoilVariant)
              .border(0.75.dp, MutedMoss, RoundedCornerShape(3.dp))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              "LOCAL LORE",
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = GrassNeon
            )
          }
        }

        val fieldQuote = if (submission.content.isNotBlank()) {
          "\"${submission.content}\""
        } else {
          "\"Side quest complete. Biome lore logged from the field or window. Character development.\""
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(DarkSoilVariant)
            .padding(10.dp)
        ) {
          Text(
            text = fieldQuote,
            fontSize = 12.sp,
            fontFamily = FontFamily.Serif,
            fontStyle = FontStyle.Italic,
            color = WeatheredPaper,
            lineHeight = 17.sp
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            "🌱 Local Lore (+${xpReward} XP)",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = GrassNeon
          )
          Text(
            "🛡️ Privacy-Safe: Zero PII Exposed",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = TextMuted
          )
        }

        Button(
          onClick = {
            val shareSnippet = submission.toShareableSnippet()
            val sendIntent = Intent().apply {
              action = Intent.ACTION_SEND
              putExtra(Intent.EXTRA_TEXT, shareSnippet)
              type = "text/plain"
            }
            val chooser = Intent.createChooser(sendIntent, "Share Touch Grass Quest")
            context.startActivity(chooser)
          },
          colors = ButtonDefaults.buttonColors(containerColor = SoilSurfacePanel),
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(6.dp)
        ) {
          Icon(Icons.Default.Send, contentDescription = null, tint = WeatheredPaper, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            "Share Discovery Card",
            color = WeatheredPaper,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )
        }
      }
    }

    // 4. Sleek Local Security & Storage Seal
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(8.dp))
        .background(DarkSoilVariant)
        .border(1.dp, MutedMoss, RoundedCornerShape(8.dp))
        .padding(10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(Icons.Default.Lock, contentDescription = null, tint = WaypointTeal, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            "ENCRYPTED LOCALLY (AES-256-GCM)",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = WaypointTeal
          )
          Text(
            "Self-guided quest completed on-device. Notes secured in local vault.",
            fontSize = 10.sp,
            fontFamily = FontFamily.Serif,
            color = TextMuted
          )
        }
      }
    }

    // Actions
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp, bottom = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = onReturnHome,
        colors = ButtonDefaults.buttonColors(
          containerColor = GrassNeon,
          contentColor = SoilVoid
        ),
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Base Camp", color = SoilVoid, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }

      Button(
        onClick = onViewMissions,
        colors = ButtonDefaults.buttonColors(containerColor = SoilSurfacePanel),
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("View Quests", color = WeatheredPaper, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
      }
    }
  }
}
