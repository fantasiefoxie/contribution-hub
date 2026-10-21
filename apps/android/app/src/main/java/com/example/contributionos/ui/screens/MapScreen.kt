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
  expedition: CivicExpedition,
  isLive: Boolean,
  onNodeSelected: (ExpeditionNode) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedNode by remember {
    mutableStateOf(expedition.nodes.firstOrNull { it.status == NodeStatus.AVAILABLE })
  }

  val districts = listOf("Greenway District", "Harbor Basin", "Civic Core")
  var activeDistrict by remember { mutableStateOf(expedition.district) }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(CivicForestDark)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      CivicHeader(
        districtName = activeDistrict,
        seed = expedition.activeRunSeed,
        isLive = isLive
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
              .padding(horizontal = 10.dp, vertical = 5.dp)
          ) {
            Text(
              dist,
              fontSize = 11.sp,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) CivicEmerald else TextMuted
            )
          }
        }
      }

      // Interactive Illustrated Isometric Map Canvas
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      ) {
        IsometricWorldCanvas(
          nodes = expedition.nodes,
          selectedNodeId = selectedNode?.id,
          onNodeClick = { node ->
            selectedNode = node
          },
          modifier = Modifier.fillMaxSize()
        )

        // Overlay Legend
        Row(
          modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(12.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(CivicSurfaceDark.copy(alpha = 0.85f))
            .border(1.dp, CivicBorderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(NodeAvailable))
            Spacer(Modifier.width(4.dp))
            Text("Open", fontSize = 10.sp, color = TextSecondary)
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(NodeValidated))
            Spacer(Modifier.width(4.dp))
            Text("Verified", fontSize = 10.sp, color = TextSecondary)
          }
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(NodeLocked))
            Spacer(Modifier.width(4.dp))
            Text("Locked", fontSize = 10.sp, color = TextMuted)
          }
        }
      }
    }

    // Slide-up Node Inspector Drawer
    AnimatedVisibility(
      visible = selectedNode != null,
      enter = slideInVertically(initialOffsetY = { it }),
      exit = slideOutVertically(targetOffsetY = { it }),
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .padding(16.dp)
    ) {
      selectedNode?.let { node ->
        LayeredPanel(
          modifier = Modifier.fillMaxWidth(),
          borderColor = when (node.status) {
            NodeStatus.AVAILABLE -> CivicEmerald
            NodeStatus.IN_PROGRESS -> CivicAmber
            NodeStatus.VALIDATED -> CivicCyan
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
                    .size(10.dp)
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
                  text = node.status.name.replace("_", " "),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold,
                  color = when (node.status) {
                    NodeStatus.AVAILABLE -> NodeAvailable
                    NodeStatus.IN_PROGRESS -> NodeInProgress
                    NodeStatus.VALIDATED -> NodeValidated
                    NodeStatus.LOCKED -> NodeLocked
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
