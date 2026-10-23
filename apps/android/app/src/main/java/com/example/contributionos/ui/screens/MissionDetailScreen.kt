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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.ExpeditionNode
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
import com.example.contributionos.ui.components.TactileCard
import com.example.contributionos.ui.components.bottomBorder
import com.example.contributionos.ui.components.topBorder

fun formatDossierId(taskId: String): String {
  val digits = taskId.filter { it.isDigit() }
  val sqNumber = if (digits.isBlank()) "01" else digits.takeLast(2).padStart(2, '0')
  return "SQ-$sqNumber"
}

@Composable
fun MissionDetailScreen(
  task: CivicTask,
  node: ExpeditionNode?,
  onCaptureEvidence: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(SoilVoid)
  ) {
    // 1. Top Bar / App Header
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(ForestFloor)
        .bottomBorder(1.dp, MutedMoss)
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { onBack() }
        ) {
          Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = "Back",
            tint = WeatheredPaper,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "CAMP / MAP",
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = WeatheredPaper
          )
        }

        Text(
          text = "[EXPEDITION DOSSIER // ${formatDossierId(task.id)}]",
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          color = GrassNeon
        )

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(SunGold.copy(alpha = 0.15f))
            .border(1.dp, SunGold, RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            "ACTIVE DOSSIER",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = SunGold
          )
        }
      }
    }

    // Sub-header Diagnostics Bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(DarkSoilVariant)
        .bottomBorder(1.dp, MutedMoss.copy(alpha = 0.6f))
        .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          "LOC: ${task.district.uppercase()} // ${task.locationLabel.uppercase()}",
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace,
          color = TextMuted
        )
        Text(
          "SIGNAL: OFF-GRID PERSISTENT",
          fontSize = 9.sp,
          fontFamily = FontFamily.Monospace,
          fontWeight = FontWeight.Bold,
          color = SunGold
        )
      }
    }

    // Main Scrollable Dossier Canvas
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 2. Quest Specimen Header Card
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MutedMoss,
        backgroundColor = ForestFloor
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          // Meta Classification
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("🌿", fontSize = 12.sp)
              Spacer(modifier = Modifier.width(4.dp))
              Text(
                "Botanical Scouting • Self-Guided",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = SunGold
              )
            }
            Text(
              "SEED #8841-B",
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              color = MutedLichen
            )
          }

          // Quest Title
          Text(
            text = task.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.5).sp,
            color = WeatheredPaper
          )

          // Biome Metadata Badges
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(DarkSoilVariant)
                .border(0.75.dp, MutedMoss, RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
              Text(
                "Tier ${task.difficulty} Common",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = GrassNeon
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(DarkSoilVariant)
                .border(0.75.dp, MutedMoss, RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
              Text(
                task.locationLabel,
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
              )
            }
          }

          // Specimen Narrative Study Well
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(SoilVoid)
              .border(1.dp, MutedMoss, RoundedCornerShape(6.dp))
              .padding(10.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSoilVariant)
                    .border(1.dp, MutedMoss, RoundedCornerShape(6.dp)),
                  contentAlignment = Alignment.Center
                ) {
                  Text("🔬", fontSize = 22.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    "\"${task.description}\"",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    color = WeatheredPaper,
                    lineHeight = 16.sp
                  )
                }
              }

              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .topBorder(0.5.dp, MutedMoss.copy(alpha = 0.6f))
                  .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "CLASSIFICATION: Non-Vascular Cryptogam",
                  fontSize = 8.sp,
                  fontFamily = FontFamily.Monospace,
                  color = MutedLichen
                )
                Text(
                  "[EXPEDITION READY]",
                  fontSize = 8.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = SunGold
                )
              }
            }
          }
        }
      }

      // 3. Quest Objectives & Completion Criteria
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MutedMoss,
        backgroundColor = ForestFloor
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "FIELD PROTOCOLS // COMPLETION CRITERIA",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.sp,
            color = WeatheredPaper
          )

          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.Top) {
              Text("01.", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = GrassNeon)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                "Locate the specimen outdoors or view from a window/stoop vantage.",
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
                color = TextSecondary,
                lineHeight = 16.sp
              )
            }
            Row(verticalAlignment = Alignment.Top) {
              Text("02.", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = GrassNeon)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                "Inspect details: surface texture, sunlight, moisture, and surrounding greenery.",
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
                color = TextSecondary,
                lineHeight = 16.sp
              )
            }
            Row(verticalAlignment = Alignment.Top) {
              Text("03.", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = GrassNeon)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                "Record a brief field note (10+ characters). Photos and GPS remain 100% optional.",
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
                color = TextSecondary,
                lineHeight = 16.sp
              )
            }
          }

          // Reward Telemetry Strip
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(DarkSoilVariant)
              .border(1.dp, MutedMoss, RoundedCornerShape(6.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "+${task.xpReward} Exploration XP Available",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = GrassNeon
            )
            Text(
              "0 Civic Credits • Self-Guided",
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              color = TextMuted
            )
          }
        }
      }

      // 4. Dedicated Stationary / Remote Alternative Section
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = SunGold.copy(alpha = 0.5f),
        backgroundColor = ForestFloor
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(SunGold.copy(alpha = 0.15f))
                .border(1.dp, SunGold, RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                "♿ LOW-BARRIER ALTERNATIVE // STATIONARY SCOUT",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = SunGold
              )
            }
          }

          Text(
            "Window or Balcony Botanical Survey: Inspect a houseplant, balcony container, or window garden for leaf moisture, soil condition, or new growth.",
            fontSize = 12.sp,
            fontFamily = FontFamily.Serif,
            color = WeatheredPaper,
            lineHeight = 16.sp
          )

          Text(
            "• 100% equally valid completion for rainy days, recovery, or limited mobility.\n• Full XP awarded identically to outdoor walking scouts without penalty.",
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            color = TextMuted,
            lineHeight = 15.sp
          )
        }
      }

      // 5. Safety & Courtesy First
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MutedMoss.copy(alpha = 0.6f),
        backgroundColor = DarkSoilVariant
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
          Text(
            "🛡️ SAFETY & COURTESY FIRST",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
          )
          Text(
            task.safetyNotice,
            fontSize = 11.sp,
            fontFamily = FontFamily.Serif,
            color = TextMuted,
            lineHeight = 15.sp
          )
          Text(
            "• Stay on public walkways; respect private boundaries.\n• Maintain awareness near traffic.\n• Anti-grind rule: No rush, no timers, hydrate and rest.",
            fontSize = 10.sp,
            fontFamily = FontFamily.Serif,
            color = TextMuted,
            lineHeight = 14.sp
          )
        }
      }

      // 6. Action Button & Privacy Assurance
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 12.dp)
      ) {
        Button(
          onClick = onCaptureEvidence,
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = GrassNeon,
            contentColor = SoilVoid
          ),
          shape = RoundedCornerShape(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text(
              "🌱 Begin Field Logging",
              color = SoilVoid,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
            Spacer(modifier = Modifier.width(6.dp))
            Icon(
              imageVector = Icons.Default.ArrowForward,
              contentDescription = null,
              tint = SoilVoid,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        Text(
          "Zero-GPS tracking required • Off-grid local encrypted vault • Touch grass at your own pace",
          fontSize = 9.sp,
          fontFamily = FontFamily.Serif,
          color = TextMuted,
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}
