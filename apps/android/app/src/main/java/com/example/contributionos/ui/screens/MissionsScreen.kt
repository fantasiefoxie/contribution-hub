package com.example.contributionos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.model.ExpeditionNode
import com.example.contributionos.model.MissionDraft
import com.example.contributionos.model.NodeStatus
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
import com.example.contributionos.ui.components.CivicHeader
import com.example.contributionos.ui.components.TactileCard

@Composable
fun MissionsScreen(
    drafts: List<MissionDraft>,
    activeNodes: List<ExpeditionNode>,
    submissions: List<CivicSubmission>,
    tasks: List<CivicTask> = emptyList(),
    isLive: Boolean,
    environmentMode: DataSourceMode = DataSourceMode.PRODUCTION,
    onToggleMode: () -> Unit = {},
    onResumeDraft: (MissionDraft) -> Unit,
    onDeleteDraft: (String) -> Unit,
    onSelectNode: (ExpeditionNode) -> Unit,
    onSelectTask: (CivicTask) -> Unit = {},
    onSelectSubmission: (CivicSubmission) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(1) } // Default to Quests tab
    var selectedBiomeFilter by remember { mutableStateOf("ALL") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(WarmPaper)
    ) {
        CivicHeader(
            districtName = "Side Quests",
            seed = 42L,
            isLive = isLive,
            environmentMode = environmentMode,
            onToggleMode = onToggleMode
        )

        // Screen Subtitle & Mode Navigation
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardCream)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Side Quests Catalogue",
                        fontSize = 18.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ForestInkDark
                    )
                    Text(
                        "Micro-adventures just beyond your doorstep",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = MutedSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(LeafPillContainer)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        "${tasks.size} Available",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.SemiBold,
                        color = LeafPillText
                    )
                }
            }

            // Mode Selector Tabs (Pill style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple(0, "Drafts", drafts.size),
                    Triple(1, "Available", tasks.size),
                    Triple(2, "Past Logs", submissions.size)
                ).forEach { (index, title, count) ->
                    val isSelected = selectedTab == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) MossGreenDark else CardCreamLow)
                            .border(
                                1.dp,
                                if (isSelected) MossGreenDark else CardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedTab = index }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$title ($count)",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else ForestInk
                        )
                    }
                }
            }
        }

        when (selectedTab) {
            0 -> DraftsTab(
                drafts = drafts,
                onResumeDraft = onResumeDraft,
                onDeleteDraft = onDeleteDraft
            )
            1 -> QuestsCatalogTab(
                activeNodes = activeNodes,
                tasks = tasks,
                selectedBiome = selectedBiomeFilter,
                onSelectBiome = { selectedBiomeFilter = it },
                onSelectNode = onSelectNode,
                onSelectTask = onSelectTask
            )
            2 -> SubmissionsTab(
                submissions = submissions,
                onSelectSubmission = onSelectSubmission
            )
        }
    }
}

@Composable
private fun QuestsCatalogTab(
    activeNodes: List<ExpeditionNode>,
    tasks: List<CivicTask>,
    selectedBiome: String,
    onSelectBiome: (String) -> Unit,
    onSelectNode: (ExpeditionNode) -> Unit,
    onSelectTask: (CivicTask) -> Unit
) {
    val scrollState = rememberScrollState()

    val filteredTasks = when (selectedBiome) {
        "CANOPY" -> tasks.filter { it.tags.any { t -> t.contains("tree") || t.contains("canopy") } || it.title.contains("Canopy", ignoreCase = true) }
        "WATERWAY" -> tasks.filter { it.tags.any { t -> t.contains("water") || t.contains("swale") } || it.title.contains("Water", ignoreCase = true) }
        "ACCESSIBLE" -> tasks.filter { it.tags.any { t -> t.contains("accessible") || t.contains("window") || t.contains("self-guided") } }
        else -> tasks
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Biome Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Pair("ALL", "All Trails"),
                    Pair("CANOPY", "🌿 Urban Canopy"),
                    Pair("WATERWAY", "💧 Waterway Walk"),
                    Pair("ACCESSIBLE", "♿ Paved & Window")
                ).forEach { (key, label) ->
                    val isSelected = selectedBiome == key
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) LeafPillContainer else CardCream)
                            .border(
                                1.dp,
                                if (isSelected) MossGreen else CardBorder,
                                RoundedCornerShape(999.dp)
                            )
                            .clickable { onSelectBiome(key) }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            label,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) LeafPillText else ForestInk
                        )
                    }
                }
            }
        }

        // Active Expedition Banner if active
        val activeNode = activeNodes.firstOrNull { it.status == NodeStatus.IN_PROGRESS }
            ?: activeNodes.firstOrNull { it.status == NodeStatus.AVAILABLE }
        val featuredTask = tasks.firstOrNull()

        if (activeNode != null || featuredTask != null) {
            item {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = MossGreen.copy(alpha = 0.35f),
                    backgroundColor = CardCream,
                    shape = RoundedCornerShape(16.dp),
                    padding = 16.dp
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
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MossGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Active Field Expedition",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = MossGreenDark
                                )
                            }
                            Text(
                                "In Progress",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Medium,
                                color = MutedSecondary
                            )
                        }

                        val taskTitle = activeNode?.title ?: featuredTask?.title ?: "Canopy Scout"
                        Text(
                            taskTitle,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = ForestInkDark
                        )

                        BotanicalProgressBar(
                            progressPercent = 0.6f,
                            activeColor = MossGreen,
                            trackColor = CardBorderSubtle,
                            height = 6.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "⏱ ~15 min • +${featuredTask?.xpReward ?: 50} XP",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = MutedSecondary
                            )

                            Button(
                                onClick = {
                                    if (activeNode != null) onSelectNode(activeNode)
                                    else if (featuredTask != null) onSelectTask(featuredTask)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MossGreenDark,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Text("Continue Log →", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Section Title: Available Quests
        item {
            Text(
                "Available Nature Walks (${filteredTasks.size})",
                fontSize = 15.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ForestInkDark,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        if (filteredTasks.isEmpty()) {
            item {
                TactileCard(
                    modifier = Modifier.fillMaxWidth(),
                    borderColor = CardBorder,
                    backgroundColor = CardCream,
                    shape = RoundedCornerShape(16.dp),
                    padding = 20.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("🧭", fontSize = 28.sp)
                        Text(
                            "No Expeditions or Quests in this Sector",
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = ForestInkDark
                        )
                        Text(
                            "Verified field initiatives will display here when online. Toggle Sandbox Demo mode in the header to test simulated expeditions.",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MutedSecondary,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }

        // Quest Cards List
        items(filteredTasks, key = { it.id }) { task ->
            TactileCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTask(task) },
                borderColor = CardBorder,
                backgroundColor = CardCream,
                shape = RoundedCornerShape(16.dp),
                padding = 16.dp
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Header tags
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(LeafPillContainer)
                                    .padding(horizontal = 7.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    task.district.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.SemiBold,
                                    color = LeafPillText
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "🌱".repeat(task.difficulty ?: 1) + " Easy",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = MossGreenDark,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(CardCreamLow)
                                .padding(horizontal = 7.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "♿ Window Friendly",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.SansSerif,
                                color = MutedSecondary
                            )
                        }
                    }

                    // Title & Description
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            task.title,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            color = ForestInkDark
                        )
                        Text(
                            task.description,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = ForestInk.copy(alpha = 0.8f),
                            lineHeight = 18.sp
                        )
                    }

                    // Footer with XP and Open Briefing button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Box(
                            modifier = Modifier
                              .clip(RoundedCornerShape(6.dp))
                              .background(SunGoldContainer)
                              .padding(horizontal = 7.dp, vertical = 3.dp)
                          ) {
                            Text(
                              "+${task.xpReward} XP",
                              fontSize = 11.sp,
                              fontFamily = FontFamily.SansSerif,
                              fontWeight = FontWeight.Bold,
                              color = SunGoldText
                            )
                          }
                          Spacer(modifier = Modifier.width(8.dp))
                          Text(
                            "⏱ ~${task.estimatedDurationMinutes} min",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MutedSecondary
                          )
                        }

                        Button(
                            onClick = { onSelectTask(task) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = LeafPillContainer,
                                contentColor = MossGreenDark
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "View Briefing",
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.SansSerif,
                                    fontWeight = FontWeight.Bold,
                                    color = MossGreenDark
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = MossGreenDark,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
            }
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
        EmptyStateBox(message = "No offline drafts saved. Head outside, record field observations, and they will be safely kept in your local field journal.")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Offline Field Journal Drafts (${drafts.size})",
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ForestInkDark
            )
        }

        items(drafts, key = { it.id }) { draft ->
            TactileCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onResumeDraft(draft) },
                borderColor = CardBorder,
                backgroundColor = CardCream
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Field Log Draft",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            fontWeight = FontWeight.Bold,
                            color = MossGreenDark
                        )
                        IconButton(
                            onClick = { onDeleteDraft(draft.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MutedSecondary)
                        }
                    }

                    Text(draft.taskTitle, fontSize = 15.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = ForestInkDark)

                    if (draft.notes.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(CardCreamLow)
                                .padding(8.dp)
                        ) {
                            Text(
                                "\"${draft.notes}\"",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                color = ForestInk.copy(alpha = 0.85f),
                                maxLines = 2
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (draft.latitude != null) "📍 Geotagged • 📷 Photo Added" else "📝 Note Saved (Zero-GPS)",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MutedSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = MossGreenDark, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", fontSize = 12.sp, fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.Bold, color = MossGreenDark)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubmissionsTab(
    submissions: List<CivicSubmission>,
    onSelectSubmission: (CivicSubmission) -> Unit
) {
    if (submissions.isEmpty()) {
        EmptyStateBox(message = "No completed quest logs yet. Complete a side quest to record your first field specimen log!")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Completed Field Journal Logs (${submissions.size})",
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ForestInkDark
            )
        }

        items(submissions, key = { it.id }) { sub ->
            val statusColor = when (sub.status) {
                SubmissionStatus.APPROVED, SubmissionStatus.VALIDATED, SubmissionStatus.LOCAL_COMPLETION -> MossGreenDark
                SubmissionStatus.PENDING_REVIEW, SubmissionStatus.SERVER_ACCEPTED -> SunGold
                SubmissionStatus.CHANGES_REQUESTED, SubmissionStatus.RETRYABLE_FAILURE -> SunGold
                SubmissionStatus.UPLOADING -> WaterBlue
                SubmissionStatus.REJECTED, SubmissionStatus.PERMANENT_FAILURE -> Color(0xFFBA1A1A)
                SubmissionStatus.QUEUED_OFFLINE, SubmissionStatus.DRAFT_LOCAL -> WaterBlue
            }

            val statusLabel = when (sub.status) {
                SubmissionStatus.LOCAL_COMPLETION -> "Completed Locally"
                SubmissionStatus.APPROVED, SubmissionStatus.VALIDATED -> "Verified & Logged"
                SubmissionStatus.SERVER_ACCEPTED -> "Synced Online"
                SubmissionStatus.PENDING_REVIEW -> "Pending Review"
                SubmissionStatus.CHANGES_REQUESTED -> "Clarification Needed"
                SubmissionStatus.UPLOADING -> "Syncing Proof"
                SubmissionStatus.RETRYABLE_FAILURE -> "Sync Pending (Offline)"
                SubmissionStatus.PERMANENT_FAILURE -> "Validation Rejected"
                SubmissionStatus.REJECTED -> "Rejected"
                SubmissionStatus.QUEUED_OFFLINE -> "Queued Offline"
                SubmissionStatus.DRAFT_LOCAL -> "Local Draft"
            }

            TactileCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSubmission(sub) },
                borderColor = CardBorder,
                backgroundColor = CardCream
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                statusLabel,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.SemiBold,
                                color = statusColor
                            )
                        }

                        Text(
                            if (sub.status == SubmissionStatus.LOCAL_COMPLETION) "Field Log" else "Verified",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MutedSecondary
                        )
                    }

                    Text(
                        sub.taskTitle.ifBlank { "Botanical Field Observation" },
                        fontSize = 15.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = ForestInkDark
                    )

                    val formattedDate = remember(sub.submittedAt) {
                        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(sub.submittedAt))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            formattedDate,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.SansSerif,
                            color = MutedSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "View Entry",
                                fontSize = 12.sp,
                                fontFamily = FontFamily.SansSerif,
                                fontWeight = FontWeight.Bold,
                                color = MossGreenDark
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = MossGreenDark, modifier = Modifier.size(13.dp))
                        }
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
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("🌱", fontSize = 32.sp)
            Text(
                message,
                fontSize = 13.sp,
                fontFamily = FontFamily.Serif,
                color = MutedSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
