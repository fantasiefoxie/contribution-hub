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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.ExpeditionNode
import com.example.contributionos.model.NodeStatus
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
import com.example.contributionos.theme.NodeInProgress
import com.example.contributionos.theme.NodeLocked
import com.example.contributionos.theme.NodeValidated
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.ui.components.LayeredPanel

@Composable
fun ExpeditionDetailScreen(
  expedition: CivicExpedition,
  onNodeClick: (ExpeditionNode) -> Unit,
  onResetRun: () -> Unit,
  onAbandonRun: () -> Unit = {},
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  var showResetDialog by remember { mutableStateOf(false) }
  var showAbandonDialog by remember { mutableStateOf(false) }
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
        text = "EXPEDITION ROUTE TREE",
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
      // Header Lore Panel
      LayeredPanel(modifier = Modifier.fillMaxWidth(), borderColor = CivicBorderActive) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            expedition.title,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            "District: ${expedition.district} • Target: ${expedition.targetMilestone}",
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = CivicCyan
          )
          Text(
            expedition.lore,
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp
          )
          LinearProgressIndicator(
            progress = { expedition.progressPercent },
            modifier = Modifier
              .fillMaxWidth()
              .height(6.dp)
              .clip(RoundedCornerShape(3.dp)),
            color = CivicEmerald,
            trackColor = CivicSurfaceDark
          )
          Text(
            "${expedition.completedNodes} of ${expedition.totalNodes} Waypoints Validated",
            fontSize = 11.sp,
            color = TextMuted
          )
        }
      }

      // Route Steps (Branching representation)
      Text(
        "BRANCHING TACTICAL WAYPOINTS",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        color = TextMuted
      )

      val stepGroups = expedition.nodes.groupBy { it.step }
      stepGroups.keys.sorted().forEach { step ->
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text(
            "Tier $step Waypoints",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = CivicAmber
          )

          stepGroups[step]?.forEach { node ->
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onNodeClick(node) },
              colors = CardDefaults.cardColors(containerColor = CivicSurfaceCard),
              shape = RoundedCornerShape(10.dp),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                when (node.status) {
                  NodeStatus.AVAILABLE -> CivicEmerald
                  NodeStatus.IN_PROGRESS -> CivicAmber
                  NodeStatus.VALIDATED -> CivicCyan
                  NodeStatus.LOCKED -> CivicBorderSubtle
                }
              )
            ) {
              Row(
                modifier = Modifier
                  .padding(12.dp)
                  .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                          when (node.status) {
                            NodeStatus.AVAILABLE -> NodeAvailable
                            NodeStatus.IN_PROGRESS -> NodeInProgress
                            NodeStatus.VALIDATED -> NodeValidated
                            NodeStatus.LOCKED -> NodeLocked
                          }
                        )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      node.status.name.replace("_", " "),
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      color = when (node.status) {
                        NodeStatus.AVAILABLE -> NodeAvailable
                        NodeStatus.IN_PROGRESS -> NodeInProgress
                        NodeStatus.VALIDATED -> NodeValidated
                        NodeStatus.LOCKED -> NodeLocked
                      }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("• ${node.category}", fontSize = 10.sp, color = TextMuted)
                  }
                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    node.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                  )
                }

                Text("+${node.xpReward} XP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CivicAmber)
              }
            }
          }
        }
      }

      // Roguelike Run Control
      LayeredPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("ROGUELITE PERSISTENCE POLICY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          Text(
            "You may reset or re-roll your route at any time. Resetting a run regenerates waypoints with a new seed, but all verified contributions, XP, badges, and permanent ledger entries are NEVER removed.",
            fontSize = 12.sp,
            color = TextMuted,
            lineHeight = 16.sp
          )
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = { showResetDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = CivicSurfacePanel),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Refresh, contentDescription = null, tint = CivicAmber, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Re-Roll", color = CivicAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Button(
              onClick = { showAbandonDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = CivicSurfacePanel),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Abandon", color = TextMuted, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
          }
        }
      }
    }
  }

  // Reset Run Confirmation Dialog
  if (showResetDialog) {
    AlertDialog(
      onDismissRequest = { showResetDialog = false },
      containerColor = CivicSurfaceDark,
      title = {
        Text("Re-Roll Route Branches?", fontWeight = FontWeight.Bold, color = TextPrimary)
      },
      text = {
        Text(
          "This will reset active route branches and generate a fresh waypoint seed. All previously validated civic achievements remain permanently preserved in the community ledger.",
          fontSize = 13.sp,
          color = TextSecondary
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showResetDialog = false
            onResetRun()
          },
          colors = ButtonDefaults.buttonColors(containerColor = CivicAmber)
        ) {
          Text("Confirm Reset", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showResetDialog = false }) {
          Text("Cancel", color = TextMuted)
        }
      }
    )
  }

  // Abandon Expedition Confirmation Dialog
  if (showAbandonDialog) {
    AlertDialog(
      onDismissRequest = { showAbandonDialog = false },
      containerColor = CivicSurfaceDark,
      title = {
        Text("Abandon Active Expedition?", fontWeight = FontWeight.Bold, color = TextPrimary)
      },
      text = {
        Text(
          "Abandoning this expedition frees your focus. No shame, no penalties, and every single verified contribution and proof you've already completed is safely kept forever.",
          fontSize = 13.sp,
          color = TextSecondary
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showAbandonDialog = false
            onAbandonRun()
          },
          colors = ButtonDefaults.buttonColors(containerColor = CivicAmber)
        ) {
          Text("Abandon Run", color = Color.Black, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAbandonDialog = false }) {
          Text("Keep Going", color = TextMuted)
        }
      }
    )
  }
}
