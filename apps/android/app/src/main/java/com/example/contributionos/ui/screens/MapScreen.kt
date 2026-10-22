package com.example.contributionos.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.DataSourceMode
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
import com.example.contributionos.ui.components.CivicHeader
import com.example.contributionos.ui.components.IsometricWorldCanvas
import com.example.contributionos.ui.components.LayeredPanel

@Composable
fun MapScreen(
  expedition: CivicExpedition?,
  isLive: Boolean,
  environmentMode: DataSourceMode = DataSourceMode.PRODUCTION,
  onToggleMode: () -> Unit = {},
  onNodeSelected: (ExpeditionNode) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedNode by remember {
    mutableStateOf(expedition?.nodes?.firstOrNull { it.status == NodeStatus.AVAILABLE })
  }

  val districts = listOf("Greenway District", "Harbor Basin", "Civic Core")
  var activeDistrict by remember { mutableStateOf(expedition?.district ?: "Civic Core") }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(CivicForestDark)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      CivicHeader(
        districtName = activeDistrict,
        seed = expedition?.activeRunSeed ?: 42L,
        isLive = isLive,
        environmentMode = environmentMode,
        onToggleMode = onToggleMode
      )

      // District Filter Chips
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(CivicSurfaceDark)
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        districts.forEach { dist ->
          val isSelected = dist == activeDistrict
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelected) CivicEmerald.copy(alpha = 0.2f) else CivicSurfacePanel)
              .border(
                1.dp,
                if (isSelected) CivicEmerald else CivicBorderSubtle,
                RoundedCornerShape(6.dp)
              )
              .clickable { activeDistrict = dist }
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = dist,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) CivicEmerald else TextSecondary
            )
          }
        }
      }

      if (expedition == null || expedition.nodes.isEmpty()) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
          contentAlignment = Alignment.Center
        ) {
          LayeredPanel(modifier = Modifier.fillMaxWidth()) {
            Column(
              modifier = Modifier.fillMaxWidth().padding(10.dp),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Icon(Icons.Default.Info, contentDescription = null, tint = CivicCyan, modifier = Modifier.size(36.dp))
              Text(
                "NO EXPEDITIONS IN ACTIVE REGISTRY",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
              )
              Text(
                "There are no active multi-station initiatives configured on the Supabase backend for $activeDistrict. You can switch to Sandbox Mode to test the roguelite route tree.",
                fontSize = 12.sp,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 17.sp
              )
              Button(
                onClick = onToggleMode,
                colors = ButtonDefaults.buttonColors(containerColor = CivicEmerald),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Launch Sandbox Map", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      } else {
        // Interactive Isometric World Canvas
        Box(
          modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
        ) {
          IsometricWorldCanvas(
            nodes = expedition.nodes,
            selectedNodeId = selectedNode?.id,
            onNodeClick = { clickedNode ->
              selectedNode = clickedNode
            },
            modifier = Modifier.fillMaxSize()
          )

          // Legend Overlay
          Box(
            modifier = Modifier
              .align(Alignment.TopStart)
              .padding(12.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(CivicSurfaceDark.copy(alpha = 0.85f))
              .border(1.dp, CivicBorderSubtle, RoundedCornerShape(8.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(NodeAvailable))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Available", fontSize = 10.sp, color = TextMuted)
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(NodeInProgress))
                Spacer(modifier = Modifier.width(4.dp))
                Text("In Progress", fontSize = 10.sp, color = TextMuted)
              }
              Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(NodeValidated))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Validated", fontSize = 10.sp, color = TextMuted)
              }
            }
          }
        }
      }
    }

    // Node Inspect Drawer
    AnimatedVisibility(
      visible = selectedNode != null,
      enter = slideInVertically(initialOffsetY = { it }),
      exit = slideOutVertically(targetOffsetY = { it }),
      modifier = Modifier.align(Alignment.BottomCenter)
    ) {
      selectedNode?.let { node ->
        LayeredPanel(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          borderColor = when (node.status) {
            NodeStatus.AVAILABLE -> NodeAvailable
            NodeStatus.IN_PROGRESS -> NodeInProgress
            NodeStatus.VALIDATED -> NodeValidated
            NodeStatus.LOCKED -> CivicBorderSubtle
          }
        ) {
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
                  text = when (node.status) {
                    NodeStatus.AVAILABLE -> "MISSION STATION READY"
                    NodeStatus.IN_PROGRESS -> "WORK IN PROGRESS"
                    NodeStatus.VALIDATED -> "COMPLETED & VERIFIED"
                    NodeStatus.LOCKED -> "PREREQUISITES REQUIRED"
                  },
                  fontSize = 11.sp,
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = 0.5.sp,
                  color = when (node.status) {
                    NodeStatus.AVAILABLE -> NodeAvailable
                    NodeStatus.IN_PROGRESS -> NodeInProgress
                    NodeStatus.VALIDATED -> NodeValidated
                    NodeStatus.LOCKED -> TextMuted
                  }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("• ${node.category}", fontSize = 11.sp, color = TextMuted)
              }

              IconButton(
                onClick = { selectedNode = null },
                modifier = Modifier.size(24.dp)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
              }
            }

            Text(
              text = node.title,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Difficulty: ", fontSize = 12.sp, color = TextMuted)
                Text("★".repeat(node.difficulty), fontSize = 13.sp, color = CivicAmber)
              }
              Text("+${node.xpReward} XP Reward", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CivicAmber)
            }

            Button(
              onClick = { onNodeSelected(node) },
              modifier = Modifier.fillMaxWidth(),
              colors = ButtonDefaults.buttonColors(
                containerColor = if (node.status == NodeStatus.LOCKED) CivicSurfacePanel else CivicEmerald
              ),
              shape = RoundedCornerShape(8.dp),
              enabled = node.status != NodeStatus.LOCKED
            ) {
              Text(
                text = if (node.status == NodeStatus.LOCKED) "Prerequisites Locked" else "Inspect Mission Requirements",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (node.status == NodeStatus.LOCKED) TextMuted else Color.Black
              )
            }
          }
        }
      }
    }
  }
}
