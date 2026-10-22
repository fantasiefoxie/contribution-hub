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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.model.ExpeditionNode
import com.example.contributionos.model.MissionDraft
import com.example.contributionos.model.NodeStatus
import com.example.contributionos.model.SubmissionStatus
import com.example.contributionos.theme.CivicAmber
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
import com.example.contributionos.theme.NodeValidated
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.ui.components.CivicHeader

@Composable
fun MissionsScreen(
  drafts: List<MissionDraft>,
  activeNodes: List<ExpeditionNode>,
  submissions: List<CivicSubmission>,
  isLive: Boolean,
  environmentMode: DataSourceMode = DataSourceMode.PRODUCTION,
  onToggleMode: () -> Unit = {},
  onResumeDraft: (MissionDraft) -> Unit,
  onDeleteDraft: (String) -> Unit,
  onSelectNode: (ExpeditionNode) -> Unit,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableIntStateOf(0) }
  val tabs = listOf("Drafts (${drafts.size})", "Active Work", "Review Status (${submissions.size})")

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CivicForestDark)
  ) {
    CivicHeader(
      districtName = "Field Mission Depot",
      seed = 42L,
      isLive = isLive,
      environmentMode = environmentMode,
      onToggleMode = onToggleMode
    )

    TabRow(
      selectedTabIndex = selectedTab,
      containerColor = CivicSurfaceDark,
      contentColor = CivicEmerald,
      indicator = { tabPositions ->
        TabRowDefaults.SecondaryIndicator(
          modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
          color = CivicEmerald
        )
      }
    ) {
      tabs.forEachIndexed { index, title ->
        Tab(
          selected = selectedTab == index,
          onClick = { selectedTab = index },
          text = {
            Text(
              text = title,
              fontSize = 12.sp,
              fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium
            )
          }
        )
      }
    }

    when (selectedTab) {
      0 -> DraftsTab(drafts = drafts, onResumeDraft = onResumeDraft, onDeleteDraft = onDeleteDraft)
      1 -> ActiveWorkTab(activeNodes = activeNodes, onSelectNode = onSelectNode)
      2 -> SubmissionsTab(submissions = submissions)
    }
  }
}

@Composable
private fun DraftsTab(
  drafts: List<MissionDraft>,
  onResumeDraft: (MissionDraft) -> Unit,
  onDeleteDraft: (String) -> Unit
) {
  if (drafts.isEmpty()) {
    EmptyStateBox(message = "No offline drafts saved. You can record evidence in the field and save drafts without network connectivity.")
    return
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(drafts, key = { it.id }) { draft ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onResumeDraft(draft) },
        colors = CardDefaults.cardColors(containerColor = CivicSurfaceCard),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, CivicAmber.copy(alpha = 0.5f))
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                  .background(CivicAmber)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text("OFFLINE DRAFT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CivicAmber)
            }
            IconButton(
              onClick = { onDeleteDraft(draft.id) },
              modifier = Modifier.size(24.dp)
            ) {
              Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted)
            }
          }

          Text(draft.taskTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

          if (draft.notes.isNotBlank()) {
            Text(draft.notes, fontSize = 12.sp, color = TextSecondary, maxLines = 2)
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              if (draft.latitude != null) "📍 Geotagged • 📷 Photo Attached" else "📝 Notes Only",
              fontSize = 11.sp,
              color = CivicCyan
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CivicEmerald, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Resume", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CivicEmerald)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ActiveWorkTab(
  activeNodes: List<ExpeditionNode>,
  onSelectNode: (ExpeditionNode) -> Unit
) {
  val acceptedNodes = activeNodes.filter { it.status == NodeStatus.AVAILABLE || it.status == NodeStatus.IN_PROGRESS }

  if (acceptedNodes.isEmpty()) {
    EmptyStateBox(message = "No active field work in progress on this run. Choose an open route node on the World Map to begin.")
    return
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(acceptedNodes, key = { it.id }) { node ->
      Card(
        modifier = Modifier
          .fillMaxWidth()
          .clickable { onSelectNode(node) },
        colors = CardDefaults.cardColors(containerColor = CivicSurfaceCard),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(
          1.dp,
          if (node.status == NodeStatus.IN_PROGRESS) NodeInProgress else CivicBorderSubtle
        )
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                  .background(if (node.status == NodeStatus.IN_PROGRESS) NodeInProgress else NodeAvailable)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                if (node.status == NodeStatus.IN_PROGRESS) "IN PROGRESS" else "READY TO CLAIM",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (node.status == NodeStatus.IN_PROGRESS) NodeInProgress else NodeAvailable
              )
            }
            Text("+${node.xpReward} XP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CivicAmber)
          }

          Text(node.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("Tier ${node.difficulty} • ${node.category}", fontSize = 12.sp, color = TextMuted)
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.PlayArrow, contentDescription = null, tint = CivicEmerald, modifier = Modifier.size(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Log Evidence", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = CivicEmerald)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SubmissionsTab(submissions: List<CivicSubmission>) {
  if (submissions.isEmpty()) {
    EmptyStateBox(message = "No submissions in review. Once evidence is submitted, accredited civic stewards inspect and validate the field outcome.")
    return
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    items(submissions, key = { it.id }) { sub ->
      val statusColor = when (sub.status) {
        SubmissionStatus.VALIDATED -> CivicEmerald
        SubmissionStatus.PENDING_REVIEW -> CivicAmber
        SubmissionStatus.CHANGES_REQUESTED -> CivicAmber
        SubmissionStatus.REJECTED -> Color(0xFFEF4444)
        SubmissionStatus.QUEUED_OFFLINE -> CivicCyan
      }

      val statusLabel = when (sub.status) {
        SubmissionStatus.VALIDATED -> "VERIFIED & APPROVED"
        SubmissionStatus.PENDING_REVIEW -> "IN VALIDATOR QUEUE"
        SubmissionStatus.CHANGES_REQUESTED -> "CLARIFICATION NEEDED"
        SubmissionStatus.REJECTED -> "REJECTED"
        SubmissionStatus.QUEUED_OFFLINE -> "QUEUED OFFLINE (AWAITING SYNC)"
      }

      Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CivicSurfaceCard),
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, statusColor)
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                  .background(statusColor)
              )
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                statusLabel,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = statusColor
              )
            }

            if (sub.isOfflineOrigin) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .background(CivicMidnightNavy)
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(
                  if (sub.status == SubmissionStatus.QUEUED_OFFLINE) "Local Cache" else "Offline Synced",
                  fontSize = 10.sp,
                  color = CivicCyan
                )
              }
            }
          }

          Text(sub.taskTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
          Text(sub.content, fontSize = 12.sp, color = TextSecondary)

          if (sub.feedback != null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(CivicSurfaceDark)
                .border(1.dp, CivicBorderSubtle, RoundedCornerShape(6.dp))
                .padding(8.dp)
            ) {
              Text(
                "Steward Note: ${sub.feedback}",
                fontSize = 11.sp,
                color = if (sub.status == SubmissionStatus.VALIDATED) CivicEmerald else TextMuted
              )
            }
          }

          if (sub.scoreAwarded != null) {
            Text(
              "+${sub.scoreAwarded} XP permanently minted to profile ledger",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = CivicAmber
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EmptyStateBox(message: String) {
  Box(
    modifier = Modifier
      .fillMaxSize()
      .padding(32.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      text = message,
      fontSize = 13.sp,
      color = TextMuted,
      lineHeight = 20.sp,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
  }
}
