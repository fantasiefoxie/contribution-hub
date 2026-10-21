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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.ExpeditionNode
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
import com.example.contributionos.theme.NodeAvailable
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.ui.components.LayeredPanel

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
      .background(CivicForestDark)
  ) {
    // Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(CivicSurfaceDark)
        .padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
      }
      Text(
        text = "FIELD MISSION BRIEFING",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = TextPrimary
      )
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Mission Title & Category
      LayeredPanel(modifier = Modifier.fillMaxWidth(), borderColor = CivicBorderActive) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                  .background(CivicEmerald)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = (node?.category ?: "Civic Action").uppercase(),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CivicEmerald
              )
            }
            Text(
              "★".repeat(task.difficulty) + " Difficulty",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = CivicAmber
            )
          }

          Text(
            text = task.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(CivicMidnightNavy)
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = CivicCyan, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              "${task.district} • ${task.locationLabel}",
              fontSize = 12.sp,
              color = CivicCyan,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }

      // Objective & Field Instructions
      LayeredPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("FIELD OBJECTIVE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
          Text(
            task.description,
            fontSize = 14.sp,
            color = TextSecondary,
            lineHeight = 20.sp
          )
        }
      }

      // Evidence Requirements
      LayeredPanel(modifier = Modifier.fillMaxWidth(), borderColor = CivicCyan.copy(alpha = 0.5f)) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = CivicCyan, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "REQUIRED VALIDATION EVIDENCE",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = CivicCyan
            )
          }
          Text(
            task.evidenceRequirements,
            fontSize = 13.sp,
            color = TextPrimary,
            lineHeight = 18.sp
          )
          Text(
            "Draft evidence can be saved completely offline and submitted once network connectivity is re-established.",
            fontSize = 11.sp,
            color = TextMuted
          )
        }
      }

      // Tags
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        task.tags.forEach { tag ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(CivicSurfacePanel)
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            Text("#$tag", fontSize = 11.sp, color = TextSecondary)
          }
        }
      }

      Spacer(modifier = Modifier.height(8.dp))

      // Action Button
      Button(
        onClick = onCaptureEvidence,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(containerColor = CivicEmerald),
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Capture Evidence & Save Draft", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
      }
    }
  }
}
