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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.CatalogSpecimen
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.EncounterChoice
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
import com.example.contributionos.theme.SunGold
import com.example.contributionos.theme.SunGoldContainer
import com.example.contributionos.theme.SunGoldText
import com.example.contributionos.theme.WarmPaper

@Composable
fun DiscoveryEncounterScreen(
  expedition: CivicExpedition,
  node: ExpeditionNode,
  specimen: CatalogSpecimen?,
  discoveredClueIds: Set<String> = emptySet(),
  onCommitChoice: (choiceId: String) -> Unit,
  onBack: () -> Unit
) {
  val activeChoices = remember(node.choices) {
    if (node.choices.isNotEmpty()) {
      node.choices
    } else {
      listOf(
        EncounterChoice(
          id = "c-${node.id}-inspect",
          title = "Examine bark texture & foliage canopy",
          description = "Inspect leaf margins, nodal structure, and bark striations under the canopy.",
          consequenceSnippet = "Chronicled botanical micro-habitat data into Field Journal.",
          unlocksSpecimenId = specimen?.id
        ),
        EncounterChoice(
          id = "c-${node.id}-acoustic",
          title = "Record ambient wind & bioacoustic notes",
          description = "Listen to wind murmurs, bird rustles, and branch resonance across the grove.",
          consequenceSnippet = "Minted Bioacoustic Field Stamp into Trail Collection.",
          awardsStampId = "stamp-${node.id}-acoustic"
        )
      )
    }
  }

  var selectedChoiceId by remember {
    mutableStateOf(node.selectedChoiceId ?: activeChoices.firstOrNull { it.requiresClueId == null || discoveredClueIds.contains(it.requiresClueId) }?.id)
  }

  val isStationary = expedition.activeStance == "STATIONARY"

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(WarmPaper)
  ) {
    // Header Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(CardCream)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(CardCreamLow)
          .border(1.dp, CardBorderSubtle, CircleShape)
          .clickable { onBack() },
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.ArrowBack,
          contentDescription = "Back",
          tint = ForestInk,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
      Column {
        Text(
          text = if (isStationary) "STATIONARY FIELD ENCOUNTER" else "DISCOVERY ENCOUNTER",
          fontSize = 10.sp,
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          color = MutedSecondary
        )
        Text(
          text = "Tier ${node.step + 1} • ${node.title}",
          fontSize = 14.sp,
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold,
          color = ForestInkDark
        )
      }
    }

    // Scrollable Content
    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Illustrated Botanical Card
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(CardCream)
          .border(1.5.dp, CardBorder, RoundedCornerShape(16.dp))
          .padding(20.dp)
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.fillMaxWidth()
        ) {
          // Specimen Glyph / Emblem
          Box(
            modifier = Modifier
              .size(72.dp)
              .clip(CircleShape)
              .background(LeafPillContainer)
              .border(2.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = specimen?.illustrationGlyph ?: "🌿",
              fontSize = 36.sp
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Linnaean Taxonomy & Vernacular Name
          Text(
            text = specimen?.name ?: node.title,
            fontSize = 22.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = ForestInkDark
          )

          if (specimen != null) {
            Text(
              text = specimen.linnaeanName,
              fontSize = 14.sp,
              fontFamily = FontFamily.Serif,
              fontStyle = FontStyle.Italic,
              color = MossGreenDark
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(9999.dp))
                .background(SunGoldContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                text = "+${node.xpReward} Exploration XP",
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = SunGoldText
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(9999.dp))
                .background(LeafPillContainer)
                .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
              Text(
                text = "Consequential Choice",
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = LeafPillText
              )
            }
          }
        }
      }

      // Naturalist Lore & Field Narrative
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(CardCream)
          .border(1.dp, CardBorderSubtle, RoundedCornerShape(14.dp))
          .padding(16.dp)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            text = "FIELD NATURALIST LORE",
            fontSize = 10.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = MutedSecondary
          )
          Text(
            text = if (node.encounterLore.isNotEmpty()) node.encounterLore else (specimen?.description ?: "Examine the unique flora micro-habitat along this trail tier."),
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            lineHeight = 20.sp,
            color = ForestInk
          )
        }
      }

      // Discovered Clues Section (if any active in run)
      if (discoveredClueIds.isNotEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SunGoldContainer.copy(alpha = 0.5f))
            .border(1.dp, SunGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(12.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("🔍", fontSize = 16.sp)
            Column {
              Text(
                text = "ACTIVE TRAIL CLUES",
                fontSize = 10.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = SunGoldText
              )
              Text(
                text = discoveredClueIds.joinToString(" • ") { it.replace("clue-", "").replace("-", " ") },
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                color = ForestInkDark
              )
            }
          }
        }
      }

      // Consequential Action Choices Section
      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
          text = "WHAT DO YOU CHOOSE TO INVESTIGATE?",
          fontSize = 11.sp,
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Bold,
          letterSpacing = 1.sp,
          color = ForestInkDark
        )

        activeChoices.forEach { choice ->
          val isSelected = selectedChoiceId == choice.id
          val isClueLocked = choice.requiresClueId != null && !discoveredClueIds.contains(choice.requiresClueId)

          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(14.dp))
              .background(
                when {
                  isClueLocked -> CardCreamLow.copy(alpha = 0.6f)
                  isSelected -> CardCream
                  else -> CardCreamLow
                }
              )
              .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = when {
                  isClueLocked -> CardBorderSubtle.copy(alpha = 0.5f)
                  isSelected -> ClayAccent
                  else -> CardBorderSubtle
                },
                shape = RoundedCornerShape(14.dp)
              )
              .clickable(enabled = !isClueLocked) {
                selectedChoiceId = choice.id
              }
              .padding(16.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  Box(
                    modifier = Modifier
                      .size(20.dp)
                      .clip(CircleShape)
                      .background(
                        when {
                          isClueLocked -> Color.Transparent
                          isSelected -> ClayAccent
                          else -> Color.Transparent
                        }
                      )
                      .border(
                        1.5.dp,
                        if (isClueLocked) CardBorderSubtle else if (isSelected) ClayAccent else CardBorder,
                        CircleShape
                      ),
                    contentAlignment = Alignment.Center
                  ) {
                    if (isSelected) {
                      Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = CardCream,
                        modifier = Modifier.size(12.dp)
                      )
                    } else if (isClueLocked) {
                      Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = MutedSecondary,
                        modifier = Modifier.size(10.dp)
                      )
                    }
                  }

                  Text(
                    text = choice.title,
                    fontSize = 14.sp,
                    fontFamily = FontFamily.Serif,
                    fontWeight = FontWeight.Bold,
                    color = if (isClueLocked) MutedSecondary else ForestInkDark
                  )
                }
              }

              Text(
                text = choice.description,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                color = if (isClueLocked) MutedSecondary else ForestInk
              )

              // Consequence Snippet Tag
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(if (isClueLocked) CardCreamLow else LeafPillContainer)
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = if (isClueLocked) "🔒 Requires clue: ${choice.requiresClueId?.replace("clue-", "")?.replace("-", " ")}" else "➜ ${choice.consequenceSnippet}",
                  fontSize = 11.sp,
                  fontFamily = FontFamily.SansSerif,
                  fontWeight = FontWeight.SemiBold,
                  color = if (isClueLocked) MutedSecondary else MossGreenDark
                )
              }
            }
          }
        }
      }
    }

    // Action Buttons Footer
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(CardCream)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = {
          selectedChoiceId?.let { choiceId ->
            onCommitChoice(choiceId)
          }
        },
        enabled = selectedChoiceId != null,
        modifier = Modifier
          .fillMaxWidth()
          .height(50.dp),
        shape = RoundedCornerShape(9999.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = ClayAccent,
          contentColor = CardCream
        )
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Commit Exploration Choice",
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}
