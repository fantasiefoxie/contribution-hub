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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.CatalogSpecimen
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
fun FieldJournalScreen(
  specimens: List<CatalogSpecimen>,
  rewardedWaypointIds: Set<String> = emptySet(),
  chronicledSpecimenIds: Set<String> = emptySet(),
  onSelectSpecimen: (CatalogSpecimen) -> Unit = {}
) {
  val unlockedSet = chronicledSpecimenIds.ifEmpty { rewardedWaypointIds }
  val discoveredCount = specimens.count {
    unlockedSet.contains(it.id) || unlockedSet.contains(it.linkedWaypointId)
  }
  val totalCount = specimens.size
  val progressFraction = if (totalCount == 0) 0f else discoveredCount.toFloat() / totalCount.toFloat()

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(WarmPaper)
  ) {
    // Header Bar
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(CardCream)
        .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
      Text(
        text = "HERBARIUM COMPENDIUM",
        fontSize = 11.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MutedSecondary
      )
      Text(
        text = "Field Journal",
        fontSize = 24.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        color = ForestInkDark
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Progress Meter
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = "Sector Canopy Flora",
          fontSize = 12.sp,
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Medium,
          color = ForestInk
        )
        Text(
          text = "$discoveredCount / $totalCount Chronicled",
          fontSize = 12.sp,
          fontFamily = FontFamily.SansSerif,
          fontWeight = FontWeight.Bold,
          color = MossGreenDark
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      LinearProgressIndicator(
        progress = { progressFraction },
        modifier = Modifier
          .fillMaxWidth()
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp)),
        color = MossGreenDark,
        trackColor = CardCreamLow
      )
    }

    // Specimen Folio List
    LazyColumn(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(specimens, key = { it.id }) { specimen ->
        val isDiscovered = unlockedSet.contains(specimen.id) || unlockedSet.contains(specimen.linkedWaypointId)

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(if (isDiscovered) CardCream else CardCreamLow)
            .border(
              width = if (isDiscovered) 1.5.dp else 1.dp,
              color = if (isDiscovered) CardBorder else CardBorderSubtle,
              shape = RoundedCornerShape(14.dp)
            )
            .padding(16.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
          ) {
            // Glyph Box
            Box(
              modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(if (isDiscovered) LeafPillContainer else CardCream)
                .border(
                  width = 1.dp,
                  color = if (isDiscovered) CardBorder else CardBorderSubtle,
                  shape = CircleShape
                ),
              contentAlignment = Alignment.Center
            ) {
              if (isDiscovered) {
                Text(text = specimen.illustrationGlyph, fontSize = 26.sp)
              } else {
                Icon(
                  imageVector = Icons.Default.Lock,
                  contentDescription = "Locked",
                  tint = MutedSecondary,
                  modifier = Modifier.size(20.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (isDiscovered) specimen.name else "Undiscovered Flora",
                  fontSize = 16.sp,
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = if (isDiscovered) ForestInkDark else MutedSecondary
                )

                if (isDiscovered) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(9999.dp))
                      .background(LeafPillContainer)
                      .padding(horizontal = 8.dp, vertical = 3.dp)
                  ) {
                    Text(
                      text = "Stamped",
                      fontSize = 10.sp,
                      fontFamily = FontFamily.SansSerif,
                      fontWeight = FontWeight.Bold,
                      color = LeafPillText
                    )
                  }
                } else {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(9999.dp))
                      .background(CardCream)
                      .border(1.dp, CardBorderSubtle, RoundedCornerShape(9999.dp))
                      .padding(horizontal = 8.dp, vertical = 3.dp)
                  ) {
                    Text(
                      text = "Locked",
                      fontSize = 10.sp,
                      fontFamily = FontFamily.SansSerif,
                      fontWeight = FontWeight.Medium,
                      color = MutedSecondary
                    )
                  }
                }
              }

              Text(
                text = if (isDiscovered) specimen.linnaeanName else "Taxonomy locked until field discovery",
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                color = if (isDiscovered) MossGreenDark else MutedSecondary
              )

              Spacer(modifier = Modifier.height(6.dp))

              Text(
                text = if (isDiscovered) specimen.description else specimen.loreHint,
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                lineHeight = 18.sp,
                color = if (isDiscovered) ForestInk else MutedSecondary
              )
            }
          }
        }
      }
    }
  }
}
