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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.example.contributionos.ui.components.TactileCard
import com.example.contributionos.ui.components.bottomBorder

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
      .background(WarmPaper)
  ) {
    // Top Bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(CardCream)
        .bottomBorder(1.dp, CardBorder)
        .padding(horizontal = 16.dp, vertical = 12.dp)
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
            tint = ForestInk,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Back to Board",
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            color = ForestInk
          )
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LeafPillContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            text = "Adventure Briefing",
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            color = LeafPillText
          )
        }
      }
    }

    // Scrollable Content
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. ADVENTURE TITLE & REWARD STRIP
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
          text = task.title,
          fontSize = 24.sp,
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold,
          color = ForestInkDark
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(LeafPillContainer)
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              "✨ +${task.xpReward} Exploration XP",
              fontSize = 12.sp,
              fontFamily = FontFamily.SansSerif,
              fontWeight = FontWeight.Bold,
              color = MossGreenDark
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(CardCream)
              .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              "⏱ ~${task.estimatedDurationMinutes} min",
              fontSize = 11.sp,
              fontFamily = FontFamily.SansSerif,
              color = MutedSecondary
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(CardCream)
              .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text(
              task.district.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
              fontSize = 11.sp,
              fontFamily = FontFamily.SansSerif,
              color = ForestInk
            )
          }
        }
      }

      // 2. THE ADVENTURE PREMISE CARD
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CardBorder,
        backgroundColor = CardCream,
        shape = RoundedCornerShape(18.dp),
        padding = 18.dp
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "The Adventure Ahead",
            fontSize = 15.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = ForestInkDark
          )

          Text(
            text = task.description,
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            color = ForestInk.copy(alpha = 0.85f),
            lineHeight = 20.sp
          )
        }
      }

      // 3. TWO EQUAL ADVENTURE PATHS
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CardBorder,
        backgroundColor = CardCream,
        shape = RoundedCornerShape(18.dp),
        padding = 18.dp
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Text(
            "Choose Your Exploration Path",
            fontSize = 15.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = ForestInkDark
          )

          // Path A
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(LeafPillContainer)
              .padding(12.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🥾", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  "Path A — The Footpath Explorer",
                  fontSize = 13.sp,
                  fontFamily = FontFamily.SansSerif,
                  fontWeight = FontWeight.Bold,
                  color = MossGreenDark
                )
              }
              Text(
                "Step outside into the district. Locate micro-habitats along brick edges, courtyard trees, or bioswales. Log a field note of living textures.",
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                color = ForestInk,
                lineHeight = 16.sp
              )
            }
          }

          // Path B
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(TerracottaContainer)
              .padding(12.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("♿", fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  "Path B — The Window & Balcony Naturalist",
                  fontSize = 13.sp,
                  fontFamily = FontFamily.SansSerif,
                  fontWeight = FontWeight.Bold,
                  color = TerracottaText
                )
              }
              Text(
                "Observe nature from a window, balcony, or quiet bench. Inspect potted flora, sky bird flight, or weather light. Zero penalty; awards identical rewards.",
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                color = ForestInk,
                lineHeight = 16.sp
              )
            }
          }
        }
      }

      // 4. ADVENTURE REWARDS UNLOCKED ON COMPLETION
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = SunGold.copy(alpha = 0.4f),
        backgroundColor = CardCream,
        shape = RoundedCornerShape(18.dp),
        padding = 18.dp
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "Upon Completion Yields",
            fontSize = 14.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = ForestInkDark
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(SunGoldContainer)
                .padding(10.dp)
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("+${task.xpReward} XP", fontSize = 16.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = SunGoldText)
                Text("Exploration Progress", fontSize = 10.sp, color = MutedSecondary)
              }
            }

            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(LeafPillContainer)
                .padding(10.dp)
            ) {
              Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text("Field Stamp", fontSize = 14.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = LeafPillText)
                Text("Minted to Journal", fontSize = 10.sp, color = MutedSecondary)
              }
            }
          }
        }
      }

      // 5. SAFETY & RESPECT
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CardBorderSubtle,
        backgroundColor = CardCreamLow,
        shape = RoundedCornerShape(14.dp),
        padding = 14.dp
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            "Outdoor Etiquette & Safety",
            fontSize = 13.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = ForestInkDark
          )
          Text(
            task.safetyNotice,
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            color = MutedSecondary,
            lineHeight = 16.sp
          )
          Text(
            "• Leave plants undisturbed — look and appreciate without damaging fragile colonies.",
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
            color = MutedSecondary
          )
        }
      }

      // 6. ACTION CTA BUTTON
      Column(
        modifier = Modifier.padding(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Button(
          onClick = onCaptureEvidence,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = MossGreenDark,
            contentColor = Color.White
          ),
          shape = RoundedCornerShape(14.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
          ) {
            Text(
              "Embark on Adventure 🌿",
              color = Color.White,
              fontFamily = FontFamily.SansSerif,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
              imageVector = Icons.Default.ArrowForward,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
          }
        }
      }
    }
  }
}
