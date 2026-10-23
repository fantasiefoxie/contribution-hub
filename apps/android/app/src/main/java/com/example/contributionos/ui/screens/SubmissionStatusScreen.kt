package com.example.contributionos.ui.screens

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

  var isVisible by remember { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    isVisible = true
  }

  val resolvedTask = task ?: com.example.contributionos.data.SeedMissionCatalog.findTask(submission.taskId)
  val xpReward = resolvedTask?.xpReward ?: 50

  AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(animationSpec = tween(350)) + slideInVertically(animationSpec = tween(350), initialOffsetY = { 40 })
  ) {
    Column(
      modifier = modifier
        .fillMaxSize()
        .background(WarmPaper)
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. JOYFUL CELEBRATION HEADER
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(20.dp))
          .background(CardCream)
          .border(1.dp, CardBorder, RoundedCornerShape(20.dp))
          .padding(20.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Box(
            modifier = Modifier
              .size(56.dp)
              .clip(CircleShape)
              .background(LeafPillContainer)
              .border(1.5.dp, MossGreen.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text("🌿", fontSize = 28.sp)
          }

          Text(
            text = "Quest Completed!",
            fontSize = 26.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = ForestInkDark
          )

          Text(
            text = "You successfully explored ${submission.taskTitle}",
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            color = MutedSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
          )
        }
      }

      // 2. REWARD YIELD CARD
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MossGreen.copy(alpha = 0.35f),
        backgroundColor = CardCream,
        shape = RoundedCornerShape(18.dp),
        padding = 18.dp
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "Harvested Yield",
              fontSize = 14.sp,
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = ForestInkDark
            )

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(SunGoldContainer)
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                "🔥 Streak Maintained",
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = SunGoldText
              )
            }
          }

          // Prominent XP Pill
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(LeafPillContainer)
              .padding(14.dp),
            contentAlignment = Alignment.Center
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("✨", fontSize = 20.sp)
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                "+$xpReward Exploration XP",
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = MossGreenDark
              )
            }
          }

          // Smooth Progress Bar
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "Explorer Level Progression",
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Medium,
                color = ForestInk
              )
              Text(
                "Rank Advancing",
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = MossGreenDark
              )
            }
            BotanicalProgressBar(
              progressPercent = 1.0f,
              activeColor = MossGreen,
              trackColor = CardBorderSubtle,
              height = 8.dp
            )
          }
        }
      }

      // 3. COLLECTIBLE FIELD STAMP MINTED
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = SunGold.copy(alpha = 0.5f),
        backgroundColor = CardCream,
        shape = RoundedCornerShape(18.dp),
        padding = 18.dp
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("🏷️", fontSize = 16.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "Field Stamp Minted",
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ForestInkDark
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(SunGoldContainer)
                .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
              Text("ADDED TO JOURNAL", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = SunGoldText)
            }
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(CardCreamLow)
              .border(1.dp, CardBorderSubtle, RoundedCornerShape(12.dp))
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(SoftMeadowSand)
                .border(1.5.dp, MossGreen.copy(alpha = 0.4f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text("🌳", fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                "${submission.taskTitle} Seal",
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ForestInkDark
              )
              Text(
                "Permanent Collectible • Stored in Field Journal",
                fontSize = 11.sp,
                color = MutedSecondary
              )
            }
          }
        }
      }

      // 4. FIELD NOTE DISCOVERY CARD
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CardBorder,
        backgroundColor = CardCream,
        shape = RoundedCornerShape(18.dp),
        padding = 18.dp
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "Your Field Note Excerpt",
              fontSize = 14.sp,
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = ForestInkDark
            )

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(CardCreamLow)
                .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
              Text(
                "Local Journal",
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                color = MutedSecondary
              )
            }
          }

          val fieldQuote = if (submission.content.isNotBlank()) {
            "“${submission.content}”"
          } else {
            "“Side quest complete. Biome observations logged from the field.”"
          }

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(CardCreamLow)
              .border(1.dp, CardBorderSubtle, RoundedCornerShape(12.dp))
              .padding(14.dp)
          ) {
            Text(
              text = fieldQuote,
              fontSize = 13.sp,
              fontFamily = FontFamily.Serif,
              fontStyle = FontStyle.Italic,
              color = ForestInk,
              lineHeight = 19.sp
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
            colors = ButtonDefaults.buttonColors(containerColor = LeafPillContainer, contentColor = MossGreenDark),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(Icons.Default.Send, contentDescription = null, tint = MossGreenDark, modifier = Modifier.size(15.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "Share Field Discovery",
              color = MossGreenDark,
              fontFamily = FontFamily.SansSerif,
              fontWeight = FontWeight.Bold,
              fontSize = 13.sp
            )
          }
        }
      }

      // 5. NEXT ADVENTURE RECOMMENDATION
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MossGreen.copy(alpha = 0.4f),
        backgroundColor = LeafPillContainer,
        shape = RoundedCornerShape(18.dp),
        padding = 16.dp
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "Next Adventure Recommendation",
              fontSize = 12.sp,
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = MossGreenDark
            )
            Text(
              "+50 XP",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MossGreenDark
            )
          }
          Text(
            "💧 Waterway Bioswale Walk",
            fontSize = 15.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = ForestInkDark
          )
          Text(
            "Inspect native drainage vegetation catching stormwater runoff along permeable pathways.",
            fontSize = 12.sp,
            color = ForestInk.copy(alpha = 0.85f),
            lineHeight = 16.sp
          )
          Button(
            onClick = onViewMissions,
            colors = ButtonDefaults.buttonColors(containerColor = MossGreenDark, contentColor = Color.White),
            modifier = Modifier.fillMaxWidth().height(42.dp),
            shape = RoundedCornerShape(10.dp)
          ) {
            Text("Embark on Next Adventure →", fontSize = 12.sp, fontWeight = FontWeight.Bold)
          }
        }
      }

      // 6. HONEST CIVIC IMPACT DISTINCTION CARD
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CardBorderSubtle,
        backgroundColor = CardCreamLow,
        shape = RoundedCornerShape(14.dp),
        padding = 14.dp
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = MossGreen, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              "Honest Nature Progression",
              fontSize = 12.sp,
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = ForestInkDark
            )
            Text(
              "Exploration XP progresses your personal outdoor level. Verified civic impact credits require community validation queue review.",
              fontSize = 11.sp,
              fontFamily = FontFamily.SansSerif,
              color = MutedSecondary,
              lineHeight = 15.sp
            )
          }
        }
      }

      // 7. RETURN TO WORLD
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = onReturnHome,
          colors = ButtonDefaults.buttonColors(
            containerColor = MossGreenDark,
            contentColor = Color.White
          ),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
          shape = RoundedCornerShape(12.dp)
        ) {
          Icon(Icons.Default.Home, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            "Return to Base Camp",
            color = Color.White,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
        }
      }
    }
  }
}
