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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.theme.CardBorder
import com.example.contributionos.theme.CardBorderSubtle
import com.example.contributionos.theme.CardCream
import com.example.contributionos.theme.CardCreamLow
import com.example.contributionos.theme.ClayAccent
import com.example.contributionos.theme.ForestInk
import com.example.contributionos.theme.ForestInkDark
import com.example.contributionos.theme.LeafPillContainer
import com.example.contributionos.theme.LeafPillText
import com.example.contributionos.theme.MossGreenDark
import com.example.contributionos.theme.MutedSecondary
import com.example.contributionos.theme.SunGold
import com.example.contributionos.theme.SunGoldContainer
import com.example.contributionos.theme.SunGoldText
import com.example.contributionos.theme.WarmPaper

@Composable
fun ExpeditionCompleteScreen(
  expedition: CivicExpedition,
  specimens: List<CatalogSpecimen>,
  totalXpEarned: Int = 250,
  onReturnHome: () -> Unit,
  onViewJournal: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(WarmPaper)
  ) {
    // Top banner
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(CardCream)
        .padding(top = 24.dp, bottom = 16.dp, start = 20.dp, end = 20.dp),
      horizontalAlignment = Alignment.CenterHorizontally
    ) {
      Box(
        modifier = Modifier
          .size(56.dp)
          .clip(CircleShape)
          .background(LeafPillContainer)
          .border(2.dp, CardBorder, CircleShape),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Check,
          contentDescription = null,
          tint = MossGreenDark,
          modifier = Modifier.size(28.dp)
        )
      }

      Spacer(modifier = Modifier.height(12.dp))

      Text(
        text = "EXPEDITION RUN COMPLETE",
        fontSize = 11.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MutedSecondary
      )

      Text(
        text = expedition.title,
        fontSize = 20.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        color = ForestInkDark
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "\"The soil remembers footsteps taken with an observing eye.\"",
        fontSize = 12.sp,
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic,
        color = ForestInk
      )
    }

    // Scrollable Specimen & Reward List
    Column(
      modifier = Modifier
        .weight(1f)
        .verticalScroll(rememberScrollState())
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // XP Earned Callout
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(14.dp))
          .background(SunGoldContainer)
          .border(1.5.dp, CardBorder, RoundedCornerShape(14.dp))
          .padding(16.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "TOTAL EXPLORATION REWARD",
              fontSize = 10.sp,
              fontFamily = FontFamily.SansSerif,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              color = SunGoldText
            )
            Text(
              text = "+$totalXpEarned XP Earned",
              fontSize = 20.sp,
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = ForestInkDark
            )
            Text(
              text = "3 nodes (+150 XP) + Run Completion (+100 XP)",
              fontSize = 11.sp,
              fontFamily = FontFamily.SansSerif,
              color = ForestInk
            )
          }

          Box(
            modifier = Modifier
              .size(48.dp)
              .clip(CircleShape)
              .background(CardCream)
              .border(1.5.dp, CardBorder, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(text = "🎖️", fontSize = 24.sp)
          }
        }
      }

      // Cataloged Specimens Section
      Text(
        text = "SPECIMENS CHRONICLED THIS RUN (${specimens.size})",
        fontSize = 11.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = MutedSecondary,
        modifier = Modifier.padding(top = 4.dp)
      )

      specimens.forEach { specimen ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardCream)
            .border(1.dp, CardBorderSubtle, RoundedCornerShape(12.dp))
            .padding(14.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(LeafPillContainer)
                .border(1.dp, CardBorder, CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(text = specimen.illustrationGlyph, fontSize = 22.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = specimen.name,
                fontSize = 15.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ForestInkDark
              )
              Text(
                text = specimen.linnaeanName,
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
                fontStyle = FontStyle.Italic,
                color = MossGreenDark
              )
            }
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
          }
        }
      }
    }

    // Bottom Action Buttons
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(CardCream)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
      Button(
        onClick = onViewJournal,
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
          Text(
            text = "View Field Journal",
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold
          )
          Spacer(modifier = Modifier.width(8.dp))
          Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
        }
      }

      Button(
        onClick = onReturnHome,
        modifier = Modifier
          .fillMaxWidth()
          .height(46.dp),
        shape = RoundedCornerShape(9999.dp),
        colors = ButtonDefaults.buttonColors(
          containerColor = CardCreamLow,
          contentColor = ForestInk
        )
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center
        ) {
          Icon(
            imageVector = Icons.Default.Home,
            contentDescription = null,
            modifier = Modifier.size(16.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Return to Camp Basecamp",
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium
          )
        }
      }
    }
  }
}
