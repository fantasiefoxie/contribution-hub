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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.contributionos.theme.CivicAmber
import com.example.contributionos.theme.CivicCyan
import com.example.contributionos.theme.DarkSoilVariant
import com.example.contributionos.theme.ForestFloor
import com.example.contributionos.theme.GrassNeon
import com.example.contributionos.theme.MutedLichen
import com.example.contributionos.theme.MutedMoss
import com.example.contributionos.theme.NodeAvailable
import com.example.contributionos.theme.NodeInProgress
import com.example.contributionos.theme.SoilSurfacePanel
import com.example.contributionos.theme.SoilVoid
import com.example.contributionos.theme.SunGold
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.theme.WaypointTeal
import com.example.contributionos.theme.WeatheredPaper
import com.example.contributionos.ui.components.CivicHeader
import com.example.contributionos.ui.components.SegmentedProgressBar
import com.example.contributionos.ui.components.TactileCard
import com.example.contributionos.ui.components.bottomBorder
import com.example.contributionos.ui.components.topBorder

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
            .background(SoilVoid)
    ) {
        CivicHeader(
            districtName = "Side Quest Depot",
            seed = 42L,
            isLive = isLive,
            environmentMode = environmentMode,
            onToggleMode = onToggleMode
        )

        // Telemetry Sub-bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ForestFloor)
                .bottomBorder(1.dp, MutedMoss)
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LOC: URBAN_CANOPY_04",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Text(
                    "${drafts.size} DRAFTS • ${tasks.size} AVAILABLE",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = GrassNeon
                )
            }
        }

        // Tactical Mode Selector Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(ForestFloor)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple(0, "Drafts", drafts.size),
                Triple(1, "Quests", activeNodes.size + tasks.size),
                Triple(2, "Proof Review", submissions.size)
            ).forEach { (index, title, count) ->
                val isSelected = selectedTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) DarkSoilVariant else ForestFloor)
                        .border(
                            1.dp,
                            if (isSelected) GrassNeon else MutedMoss,
                            RoundedCornerShape(6.dp)
                        )
                        .clickable { selectedTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$title ($count)",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) GrassNeon else TextSecondary
                    )
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

    // Filter tasks by selected biome if applicable
    val filteredTasks = when (selectedBiome) {
        "CANOPY" -> tasks.filter { it.tags.any { t -> t.contains("tree") || t.contains("canopy") } || it.title.contains("Canopy", ignoreCase = true) }
        "WATERWAY" -> tasks.filter { it.tags.any { t -> t.contains("water") || t.contains("swale") } || it.title.contains("Water", ignoreCase = true) }
        "ACCESSIBLE" -> tasks.filter { it.tags.any { t -> t.contains("accessible") || t.contains("window") || t.contains("self-guided") } }
        else -> tasks
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Biome Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(scrollState),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Pair("ALL", "[ALL BIOMES]"),
                    Pair("CANOPY", "CANOPY"),
                    Pair("WATERWAY", "WATERWAY"),
                    Pair("ACCESSIBLE", "♿ ACCESSIBLE")
                ).forEach { (key, label) ->
                    val isSelected = selectedBiome == key
                    Box(
                        modifier = Modifier
                          .clip(RoundedCornerShape(4.dp))
                          .background(if (isSelected) GrassNeon else DarkSoilVariant)
                          .border(
                              1.dp,
                              if (isSelected) GrassNeon else MutedMoss,
                              RoundedCornerShape(4.dp)
                          )
                          .clickable { onSelectBiome(key) }
                          .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            label,
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) SoilVoid else TextSecondary
                        )
                    }
                }
            }
        }

        // Active / Featured Quest Slot
        val activeNode = activeNodes.firstOrNull { it.status == NodeStatus.IN_PROGRESS }
            ?: activeNodes.firstOrNull { it.status == NodeStatus.AVAILABLE }
        val featuredTask = tasks.firstOrNull()

        if (activeNode != null || featuredTask != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(GrassNeon)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "[IN-PROGRESS EXPEDITION]",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = GrassNeon
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(SunGold.copy(alpha = 0.15f))
                                .border(1.dp, SunGold, RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                "DEPLOYED",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = SunGold
                            )
                        }
                    }

                    TactileCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = MutedMoss,
                        backgroundColor = ForestFloor
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Specimen Header Visual Well
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SoilVoid)
                                    .border(1.dp, MutedMoss, RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("🌿", fontSize = 22.sp)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                "SPECIMEN // CANOPY SCOUT",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold,
                                                color = SunGold
                                            )
                                            Text(
                                                "Sector 04 Tree Density • Walk or Window",
                                                fontSize = 10.sp,
                                                fontFamily = FontFamily.Serif,
                                                color = TextMuted
                                            )
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(DarkSoilVariant)
                                            .padding(horizontal = 5.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            "SECTOR_04",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextSecondary
                                        )
                                    }
                                }
                            }

                            val taskTitle = activeNode?.title ?: featuredTask?.title ?: "Canopy Scout"
                            Text(
                                taskTitle,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = WeatheredPaper
                            )

                            // Segmented Progress Meter
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "EXPEDITION PROGRESS",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = TextMuted
                                    )
                                    Text(
                                        "6/10 CHECKPOINTS (60%)",
                                        fontSize = 9.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = GrassNeon
                                    )
                                }
                                SegmentedProgressBar(
                                    progressPercent = 0.6f,
                                    totalSegments = 10,
                                    activeColor = GrassNeon,
                                    inactiveColor = DarkSoilVariant
                                )
                            }

                            // 3-Metric Tray
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSoilVariant)
                                    .border(1.dp, MutedMoss, RoundedCornerShape(6.dp))
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("⏱️ 15 MIN", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = WeatheredPaper)
                                    Text("EST. PACE", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                                }
                                Box(modifier = Modifier.width(1.dp).height(20.dp).background(MutedMoss))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("+${featuredTask?.xpReward ?: 50} XP", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = GrassNeon)
                                    Text("BOUNTY", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                                }
                                Box(modifier = Modifier.width(1.dp).height(20.dp).background(MutedMoss))
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("🔒 ZERO-GPS", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = WaypointTeal)
                                    Text("PRIVACY-SAFE", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                                }
                            }

                            // Action Button
                            Button(
                                onClick = {
                                    if (activeNode != null) onSelectNode(activeNode)
                                    else if (featuredTask != null) onSelectTask(featuredTask)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = GrassNeon,
                                    contentColor = SoilVoid
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        "Continue Expedition",
                                        fontSize = 13.sp,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Bold,
                                        color = SoilVoid
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowForward,
                                        contentDescription = null,
                                        tint = SoilVoid,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Local Field Assignments Catalog
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LOCAL FIELD ASSIGNMENTS // AVAILABLE [${filteredTasks.size}]",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = WeatheredPaper
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(DarkSoilVariant)
                        .border(1.dp, MutedMoss, RoundedCornerShape(3.dp))
                        .padding(horizontal = 5.dp, vertical = 2.dp)
                ) {
                    Text(
                        "SYNCED",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = SunGold
                    )
                }
            }
        }

        items(filteredTasks, key = { it.id }) { task ->
            TactileCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTask(task) },
                borderColor = MutedMoss,
                backgroundColor = ForestFloor
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Header Row with ID, Difficulty Stars, and Accessibility Tag
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "[${formatTaskDossierId(task.id)}]",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = GrassNeon
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "★".repeat(task.difficulty ?: 1) + "☆".repeat((5 - (task.difficulty ?: 1)).coerceAtLeast(0)),
                                fontSize = 10.sp,
                                color = SunGold
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(DarkSoilVariant)
                                .border(0.75.dp, MutedMoss, RoundedCornerShape(3.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "[WALK OR WINDOW]",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }
                    }

                    // Body Split Layout
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SoilVoid)
                                .border(1.dp, MutedMoss, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🌱", fontSize = 20.sp)
                                Text(
                                    "BIO:${(task.tags.firstOrNull() ?: task.district).take(4).uppercase()}",
                                    fontSize = 7.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = MutedLichen
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                task.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = WeatheredPaper
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                task.description,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Serif,
                                color = TextMuted,
                                maxLines = 2,
                                lineHeight = 14.sp
                            )
                        }
                    }

                    // Footer Details & Action Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .topBorder(1.dp, MutedMoss)
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "+${task.xpReward} XP",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = GrassNeon
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("•", color = TextMuted, fontSize = 10.sp)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "⏱️ ~${task.estimatedDurationMinutes}m",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextMuted
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSoilVariant)
                                .border(1.dp, MutedMoss, RoundedCornerShape(4.dp))
                                .clickable { onSelectTask(task) }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "Open Briefing",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = WeatheredPaper
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = GrassNeon,
                                    modifier = Modifier.size(12.dp)
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
        EmptyStateBox(message = "No offline drafts saved. Head outside, record field observations, and store securely without cell signal.")
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "LOCAL ENCRYPTED VAULT // DRAFTS [${drafts.size}]",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SunGold
                )
                Text(
                    "AES-256-GCM",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MutedLichen
                )
            }
        }

        items(drafts, key = { it.id }) { draft ->
            TactileCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onResumeDraft(draft) },
                borderColor = SunGold.copy(alpha = 0.5f),
                backgroundColor = ForestFloor
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
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(SunGold)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "LOCAL SPECIMEN LOG",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = SunGold
                            )
                        }
                        IconButton(
                            onClick = { onDeleteDraft(draft.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted)
                        }
                    }

                    Text(draft.taskTitle, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = WeatheredPaper)

                    if (draft.notes.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSoilVariant)
                                .padding(8.dp)
                        ) {
                            Text(
                                "\"${draft.notes}\"",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Serif,
                                fontStyle = FontStyle.Italic,
                                color = TextSecondary,
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
                            if (draft.latitude != null) "📍 Geotagged • 📷 Photo Attached" else "📝 Field Notes (Zero-GPS)",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = WaypointTeal
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = GrassNeon, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Resume", fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = GrassNeon)
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
        EmptyStateBox(message = "No proofs in review. Once proof is logged, local rewards mint immediately and community milestones update.")
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
                "FIELD DISCOVERY ARCHIVE // VALIDATIONS [${submissions.size}]",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = GrassNeon
            )
        }

        items(submissions, key = { it.id }) { sub ->
            val statusColor = when (sub.status) {
                SubmissionStatus.APPROVED, SubmissionStatus.VALIDATED, SubmissionStatus.LOCAL_COMPLETION -> GrassNeon
                SubmissionStatus.PENDING_REVIEW, SubmissionStatus.SERVER_ACCEPTED -> SunGold
                SubmissionStatus.CHANGES_REQUESTED, SubmissionStatus.RETRYABLE_FAILURE -> SunGold
                SubmissionStatus.UPLOADING -> WaypointTeal
                SubmissionStatus.REJECTED, SubmissionStatus.PERMANENT_FAILURE -> Color(0xFFFF6B6B)
                SubmissionStatus.QUEUED_OFFLINE, SubmissionStatus.DRAFT_LOCAL -> WaypointTeal
            }

            val statusLabel = when (sub.status) {
                SubmissionStatus.LOCAL_COMPLETION -> "LOCAL EXPLORATION COMPLETE"
                SubmissionStatus.APPROVED, SubmissionStatus.VALIDATED -> "VERIFIED & LOGGED"
                SubmissionStatus.SERVER_ACCEPTED -> "ACCEPTED BY REGISTRY"
                SubmissionStatus.PENDING_REVIEW -> "IN STEWARD QUEUE"
                SubmissionStatus.CHANGES_REQUESTED -> "CLARIFICATION NEEDED"
                SubmissionStatus.UPLOADING -> "UPLOADING PROOF"
                SubmissionStatus.RETRYABLE_FAILURE -> "RETRY PENDING (OFFLINE)"
                SubmissionStatus.PERMANENT_FAILURE -> "VALIDATION REJECTED"
                SubmissionStatus.REJECTED -> "REJECTED"
                SubmissionStatus.QUEUED_OFFLINE -> "QUEUED OFFLINE"
                SubmissionStatus.DRAFT_LOCAL -> "SAVED LOCAL DRAFT"
            }

            TactileCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectSubmission(sub) },
                borderColor = statusColor.copy(alpha = 0.6f),
                backgroundColor = ForestFloor
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
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                statusLabel,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = statusColor
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(3.dp))
                                .background(DarkSoilVariant)
                                .border(0.75.dp, MutedMoss, RoundedCornerShape(3.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                if (sub.status == SubmissionStatus.LOCAL_COMPLETION) "Local Vault" else "Verified",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = WeatheredPaper
                            )
                        }
                    }

                    Text(
                        sub.taskTitle.ifBlank { "Botanical Field Observation" },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = WeatheredPaper
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "ID: ${sub.id.take(8)}",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextMuted
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "View Log",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = GrassNeon
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, tint = GrassNeon, modifier = Modifier.size(12.dp))
                        }
                    }
                }
            }
        }
    }
}

private fun formatTaskDossierId(id: String): String {
    val digits = id.filter { it.isDigit() }
    return if (digits.isNotEmpty()) "SQ-${digits.takeLast(2).padStart(2, '0')}" else "SQ-01"
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
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}
