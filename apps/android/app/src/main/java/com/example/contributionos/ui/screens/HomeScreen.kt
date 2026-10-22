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
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.CommunityMilestone
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
import com.example.contributionos.theme.TextHighlight
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.ui.components.CivicHeader
import com.example.contributionos.ui.components.LayeredPanel

@Composable
fun HomeScreen(
  expedition: CivicExpedition?,
  profile: CivicProfile,
  featuredMilestone: CommunityMilestone?,
  isLive: Boolean,
  environmentMode: DataSourceMode = DataSourceMode.PRODUCTION,
  onToggleMode: () -> Unit = {},
  onExploreMap: () -> Unit,
  onOpenExpeditionDetails: () -> Unit,
  onSelectNode: (ExpeditionNode) -> Unit,
  onGoToMissions: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CivicForestDark)
  ) {
    CivicHeader(
      districtName = expedition?.district ?: "District Civic Registry",
      seed = expedition?.activeRunSeed ?: 42L,
      isLive = isLive,
      environmentMode = environmentMode,
      onToggleMode = onToggleMode
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Contributor Status Strip
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(12.dp))
          .background(CivicSurfaceDark)
          .border(1.dp, CivicBorderSubtle, RoundedCornerShape(12.dp))
          .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(36.dp)
              .clip(CircleShape)
              .background(CivicSurfacePanel)
              .border(1.5.dp, CivicEmerald, CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text("L${profile.level}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = TextHighlight)
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(profile.displayName, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
            Text(profile.rankTitle, fontSize = 11.sp, color = TextMuted)
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(CivicMidnightNavy)
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Text("🔥 ${profile.activeStreakDays}d streak", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = CivicAmber)
        }
      }

      // 2. Active Roguelite Expedition Card
      if (expedition != null) {
        LayeredPanel(
          modifier = Modifier.fillMaxWidth(),
          borderColor = CivicBorderActive
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
                    .background(CivicEmerald)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  if (environmentMode == DataSourceMode.SANDBOX_DEMO) "ACTIVE DEMO EXPEDITION" else "ACTIVE CIVIC EXPEDITION",
                  fontSize = 11.sp,
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = 1.sp,
                  color = CivicEmerald
                )
              }
              Text(
                "${expedition.completedNodes}/${expedition.totalNodes} NODES",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
              )
            }

            Text(
              text = expedition.title,
              fontSize = 20.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )

            Text(
              text = expedition.lore,
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

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = onExploreMap,
                colors = ButtonDefaults.buttonColors(containerColor = CivicEmerald),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Explore World Map", fontWeight = FontWeight.Bold, fontSize = 13.sp)
              }

              Button(
                onClick = onOpenExpeditionDetails,
                colors = ButtonDefaults.buttonColors(containerColor = CivicSurfacePanel),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Route Tree", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
              }
            }
          }
        }

        // 3. Recommended / Available Next Nodes
        val nextAvailableNodes = expedition.nodes.filter { it.status == NodeStatus.AVAILABLE }
        if (nextAvailableNodes.isNotEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
              "CHOOSE ROUTE BRANCH",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              color = TextMuted
            )

            nextAvailableNodes.forEach { node ->
              Card(
                modifier = Modifier
                  .fillMaxWidth()
                  .clickable { onSelectNode(node) },
                colors = CardDefaults.cardColors(containerColor = CivicSurfaceCard),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CivicBorderSubtle)
              ) {
                Row(
                  modifier = Modifier
                    .padding(14.dp)
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
                          .background(NodeAvailable)
                      )
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(
                        node.category.uppercase(),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = CivicCyan
                      )
                      Spacer(modifier = Modifier.width(8.dp))
                      Text("• Tier ${node.difficulty}", fontSize = 10.sp, color = TextMuted)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      node.title,
                      fontSize = 14.sp,
                      fontWeight = FontWeight.Bold,
                      color = TextPrimary
                    )
                  }

                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("+${node.xpReward} XP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CivicAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }
        }
      } else {
        // Honest Production Empty State for Expedition
        LayeredPanel(modifier = Modifier.fillMaxWidth()) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(Icons.Default.Info, contentDescription = null, tint = CivicCyan, modifier = Modifier.size(32.dp))
            Text(
              "NO ACTIVE INITIATIVES IN DISTRICT",
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              "The remote civic registry has no active multi-stage expeditions at this time. You can claim open community tasks directly from the Missions tab.",
              fontSize = 12.sp,
              color = TextSecondary,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center,
              lineHeight = 17.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
              Button(
                onClick = onGoToMissions,
                colors = ButtonDefaults.buttonColors(containerColor = CivicEmerald),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Browse Open Tasks", fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
              Button(
                onClick = onToggleMode,
                colors = ButtonDefaults.buttonColors(containerColor = CivicSurfacePanel),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Test in Sandbox", color = TextPrimary, fontSize = 12.sp)
              }
            }
          }
        }
      }

      // 4. Collective Community Milestone Progress
      if (featuredMilestone != null) {
        LayeredPanel(modifier = Modifier.fillMaxWidth()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "COMMUNITY OUTCOME",
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = CivicCyan
              )
              Text(
                "${featuredMilestone.currentValue} / ${featuredMilestone.targetValue} ${featuredMilestone.unit}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
              )
            }
            Text(
              featuredMilestone.title,
              fontSize = 15.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              featuredMilestone.description,
              fontSize = 12.sp,
              color = TextMuted
            )
            LinearProgressIndicator(
              progress = { featuredMilestone.percent },
              modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(3.dp)),
              color = CivicCyan,
              trackColor = CivicMidnightNavy
            )
          }
        }
      }
    }
  }
}
