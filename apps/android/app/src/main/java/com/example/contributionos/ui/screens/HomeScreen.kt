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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.CommunityMilestone
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.model.ExpeditionNode
import com.example.contributionos.model.NodeStatus
import com.example.contributionos.theme.CivicAmber
import com.example.contributionos.theme.DarkSoilVariant
import com.example.contributionos.theme.ForestFloor
import com.example.contributionos.theme.GrassNeon
import com.example.contributionos.theme.MutedLichen
import com.example.contributionos.theme.MutedMoss
import com.example.contributionos.theme.NodeAvailable
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
  availableTasks: List<CivicTask> = emptyList(),
  onSelectTask: (CivicTask) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val scrollState = rememberScrollState()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(SoilVoid)
  ) {
    CivicHeader(
      districtName = expedition?.district ?: "Local Neighborhood Biome",
      seed = expedition?.activeRunSeed ?: 42L,
      isLive = isLive,
      environmentMode = environmentMode,
      onToggleMode = onToggleMode
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. Sub-Header Telemetry & Field Journal Motto
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(ForestFloor)
              .border(1.dp, MutedMoss, RoundedCornerShape(4.dp))
              .padding(horizontal = 7.dp, vertical = 3.dp)
          ) {
            Box(
              modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(GrassNeon)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "🌱 Touching Grass — Offline Safe",
              fontSize = 11.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = GrassNeon
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(4.dp))
              .background(SoilVoid)
              .border(1.dp, MutedMoss, RoundedCornerShape(4.dp))
              .padding(horizontal = 7.dp, vertical = 3.dp)
          ) {
            Text(
              "SEED #${expedition?.activeRunSeed ?: 42L}",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = SunGold
            )
          }
        }

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .border(
              width = 1.dp,
              color = MutedMoss.copy(alpha = 0.5f),
              shape = RoundedCornerShape(6.dp)
            )
            .background(DarkSoilVariant.copy(alpha = 0.6f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              modifier = Modifier
                .width(2.dp)
                .height(18.dp)
                .background(GrassNeon)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              "Field Journal // \"Your screen time has side quests.\"",
              fontSize = 12.sp,
              fontFamily = FontFamily.Serif,
              fontStyle = FontStyle.Italic,
              color = TextSecondary
            )
          }
        }
      }

      // 2. PLAYER DOSSIER CARD ('CITIZEN DOSSIER // EXPEDITION READY')
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MutedMoss,
        backgroundColor = ForestFloor
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          // Dossier Header Strip
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                "🪪 CITIZEN DOSSIER // EXPEDITION READY",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = TextSecondary
              )
            }
            Text(
              "MK-III",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = MutedLichen
            )
          }

          // Scout Silhouette & Identification
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Botanical Scout Avatar Frame
            Box(
              modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SoilVoid)
                .border(1.5.dp, MutedMoss, RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = GrassNeon,
                modifier = Modifier.size(30.dp)
              )
              Box(
                modifier = Modifier
                  .align(Alignment.BottomEnd)
                  .clip(RoundedCornerShape(topStart = 4.dp))
                  .background(SunGold)
                  .padding(horizontal = 4.dp, vertical = 1.dp)
              ) {
                Text(
                  "01",
                  fontSize = 8.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = SoilVoid
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  profile.displayName.ifBlank { "Citizen-01" },
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold,
                  color = WeatheredPaper
                )
                // Roguelite Stamp: Angled Discovery Badge
                Box(
                  modifier = Modifier
                    .rotate(-2f)
                    .clip(RoundedCornerShape(3.dp))
                    .background(SunGold.copy(alpha = 0.15f))
                    .border(1.dp, SunGold, RoundedCornerShape(3.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    "L${profile.level} ${profile.rankTitle}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SunGold
                  )
                }
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                "Class: Botanical Forager • Off-Grid Ready",
                fontSize = 11.sp,
                fontFamily = FontFamily.Serif,
                color = TextMuted
              )
            }
          }

          // XP Gauge: Segmented Botanical Progress Bar
          val xpInLevel = profile.explorationXp % 100
          val xpPercent = (xpInLevel / 100f).coerceIn(0f, 1f)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(SoilVoid)
              .border(1.dp, MutedMoss, RoundedCornerShape(6.dp))
              .padding(8.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  "EXPEDITION XP",
                  fontSize = 10.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  letterSpacing = 0.8.sp,
                  color = TextSecondary
                )
                Text(
                  "${profile.explorationXp} XP (Next Lvl: ${100 - xpInLevel} XP)",
                  fontSize = 10.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = GrassNeon
                )
              }
              SegmentedProgressBar(
                progressPercent = xpPercent,
                totalSegments = 10,
                activeColor = GrassNeon,
                inactiveColor = DarkSoilVariant
              )
            }
          }

          // Mini Stats Dual Tray
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .border(
                width = 1.dp,
                color = MutedMoss.copy(alpha = 0.6f),
                shape = RoundedCornerShape(6.dp)
              )
              .background(DarkSoilVariant.copy(alpha = 0.4f))
              .padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("🔥", fontSize = 16.sp)
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  "EXPEDITION STREAK",
                  fontSize = 9.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = TextMuted
                )
                Text(
                  "3 Days",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = WeatheredPaper
                )
              }
            }

            Box(
              modifier = Modifier
                .width(1.dp)
                .height(26.dp)
                .background(MutedMoss)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("🌿", fontSize = 16.sp)
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(
                  "BIOMES CATALOGED",
                  fontSize = 9.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = TextMuted
                )
                Text(
                  "4 Verified",
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = WeatheredPaper
                )
              }
            }
          }
        }
      }

      // 3. QUICK START 3-STEP GUIDE ('FIELD PROTOCOL // QUICK START')
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              "⌨️ FIELD PROTOCOL // QUICK START",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 1.sp,
              color = SunGold
            )
          }
          Text(
            "v1.4a",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = MutedLichen
          )
        }

        // 3 Connected Step Cards in Grid
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          // Step 1
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(ForestFloor)
              .border(1.dp, MutedMoss, RoundedCornerShape(8.dp))
              .padding(8.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(3.dp))
                  .background(DarkSoilVariant)
                  .padding(horizontal = 4.dp, vertical = 1.dp)
              ) {
                Text(
                  "01",
                  fontSize = 9.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = GrassNeon
                )
              }
              Text(
                "Map Points",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WeatheredPaper
              )
              Text(
                "Scout local green spaces & trails.",
                fontSize = 10.sp,
                fontFamily = FontFamily.Serif,
                color = TextSecondary,
                lineHeight = 13.sp
              )
            }
          }

          // Step 2
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(ForestFloor)
              .border(1.dp, MutedMoss, RoundedCornerShape(8.dp))
              .padding(8.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(3.dp))
                  .background(DarkSoilVariant)
                  .padding(horizontal = 4.dp, vertical = 1.dp)
              ) {
                Text(
                  "02",
                  fontSize = 9.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = GrassNeon
                )
              }
              Text(
                "Walk/Window",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = WeatheredPaper
              )
              Text(
                "Brisk stride or seated scout.",
                fontSize = 10.sp,
                fontFamily = FontFamily.Serif,
                color = TextSecondary,
                lineHeight = 13.sp
              )
            }
          }

          // Step 3
          Box(
            modifier = Modifier
              .weight(1f)
              .clip(RoundedCornerShape(8.dp))
              .background(ForestFloor)
              .border(1.dp, GrassNeon.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
              .padding(8.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(3.dp))
                  .background(GrassNeon.copy(alpha = 0.2f))
                  .padding(horizontal = 4.dp, vertical = 1.dp)
              ) {
                Text(
                  "03",
                  fontSize = 9.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = GrassNeon
                )
              }
              Text(
                "Harvest XP",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = GrassNeon
              )
              Text(
                "Log notes & touch fresh leaves.",
                fontSize = 10.sp,
                fontFamily = FontFamily.Serif,
                color = TextSecondary,
                lineHeight = 13.sp
              )
            }
          }
        }
      }

      // 4. FEATURED SIDE QUEST CARD
      val featuredTask = availableTasks.firstOrNull()
      if (featuredTask != null) {
        TactileCard(
          modifier = Modifier.fillMaxWidth(),
          borderColor = GrassNeon.copy(alpha = 0.7f),
          backgroundColor = ForestFloor
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Top Quest Stamp & Ticker
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(3.dp))
                  .background(SunGold.copy(alpha = 0.15f))
                  .border(1.dp, SunGold, RoundedCornerShape(3.dp))
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = null,
                    tint = SunGold,
                    modifier = Modifier.size(11.dp)
                  )
                  Spacer(modifier = Modifier.width(3.dp))
                  Text(
                    "★ TIER 1 QUEST",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SunGold
                  )
                }
              }

              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                Text(
                  "⏱️ ~${featuredTask.estimatedDurationMinutes}m",
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  color = TextMuted
                )
                Text(
                  "+${featuredTask.xpReward} XP",
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = GrassNeon
                )
              }
            }

            // Quest Title
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Text(
                "[SELF-GUIDED EXPEDITION]",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = MutedLichen
              )
              Text(
                featuredTask.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = WeatheredPaper
              )
            }

            // Specimen Thumbnail & Field Notes Tray
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SoilVoid)
                .border(1.dp, MutedMoss, RoundedCornerShape(8.dp))
                .padding(8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSoilVariant)
                    .border(1.dp, MutedMoss, RoundedCornerShape(6.dp)),
                  contentAlignment = Alignment.Center
                ) {
                  Text("🌿", fontSize = 24.sp)
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    featuredTask.description,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Serif,
                    color = TextSecondary,
                    maxLines = 2,
                    lineHeight = 15.sp
                  )
                  Spacer(modifier = Modifier.height(3.dp))
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                      "♿ Outdoor walk OR relaxed window scout",
                      fontSize = 10.sp,
                      fontFamily = FontFamily.Monospace,
                      color = WaypointTeal
                    )
                  }
                }
              }
            }

            // Primary Tactile Button
            Button(
              onClick = { onSelectTask(featuredTask) },
              colors = ButtonDefaults.buttonColors(
                containerColor = GrassNeon,
                contentColor = SoilVoid
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
              ) {
                Text(
                  "🌱 Start Side Quest",
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

      // 5. PRIMARY EXPLORATION ACTION BANNER (EXPLORE BIOME MAP)
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(10.dp))
          .background(ForestFloor)
          .border(1.5.dp, MutedMoss, RoundedCornerShape(10.dp))
          .clickable { onExploreMap() }
          .padding(14.dp)
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
          ) {
            Box(
              modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(SoilVoid)
                .border(1.dp, MutedMoss, RoundedCornerShape(8.dp)),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                tint = GrassNeon,
                modifier = Modifier.size(24.dp)
              )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                  "Explore Biome Map",
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  color = WeatheredPaper
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(DarkSoilVariant)
                    .border(0.75.dp, SunGold.copy(alpha = 0.5f), RoundedCornerShape(3.dp))
                    .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                  Text(
                    "GRID ON",
                    fontSize = 8.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = SunGold
                  )
                }
              }
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                "Locate nearby moss, pocket parks & tree corridors",
                fontSize = 11.sp,
                fontFamily = FontFamily.Serif,
                color = TextMuted
              )
            }
          }
          Icon(
            imageVector = Icons.Default.ArrowForward,
            contentDescription = null,
            tint = GrassNeon,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // 6. ACTIVE ROGUELITE EXPEDITION (if expedition != null)
      if (expedition != null) {
        TactileCard(
          modifier = Modifier.fillMaxWidth(),
          borderColor = GrassNeon.copy(alpha = 0.5f)
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
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(GrassNeon)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                  if (environmentMode == DataSourceMode.SANDBOX_DEMO) "ACTIVE ASTROTURF RUN" else "🌱 ACTIVE EXPEDITION",
                  fontSize = 10.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.ExtraBold,
                  letterSpacing = 1.sp,
                  color = GrassNeon
                )
              }
              Text(
                "${expedition.completedNodes}/${expedition.totalNodes} CHECKPOINTS",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
              )
            }

            Text(
              text = expedition.title,
              fontSize = 16.sp,
              fontWeight = FontWeight.Bold,
              color = WeatheredPaper
            )

            Text(
              text = expedition.lore,
              fontSize = 12.sp,
              fontFamily = FontFamily.Serif,
              color = TextSecondary,
              lineHeight = 16.sp
            )

            SegmentedProgressBar(
              progressPercent = expedition.progressPercent,
              totalSegments = 10,
              activeColor = GrassNeon,
              inactiveColor = DarkSoilVariant
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = onExploreMap,
                colors = ButtonDefaults.buttonColors(containerColor = GrassNeon),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = SoilVoid, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Explore Map", color = SoilVoid, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }

              Button(
                onClick = onOpenExpeditionDetails,
                colors = ButtonDefaults.buttonColors(containerColor = SoilSurfacePanel),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(6.dp)
              ) {
                Text("Route Tree", color = WeatheredPaper, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
              }
            }
          }
        }
      }

      // 7. ADDITIONAL AVAILABLE SIDE QUESTS (If multiple tasks exist)
      if (availableTasks.size > 1) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "AVAILABLE SIDE QUESTS [${availableTasks.size}]",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              letterSpacing = 1.sp,
              color = GrassNeon
            )
            Text(
              "Browse All →",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = SunGold,
              modifier = Modifier.clickable { onGoToMissions() }
            )
          }

          availableTasks.drop(1).forEach { task ->
            Card(
              modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelectTask(task) },
              colors = CardDefaults.cardColors(containerColor = ForestFloor),
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, MutedMoss)
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
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(GrassNeon)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                      task.district.uppercase(),
                      fontSize = 9.sp,
                      fontFamily = FontFamily.Monospace,
                      fontWeight = FontWeight.Bold,
                      color = WaypointTeal
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("• ~${task.estimatedDurationMinutes}m", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    task.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                  )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text("+${task.xpReward} XP", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = SunGold)
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(Icons.Default.ArrowForward, contentDescription = null, tint = TextMuted, modifier = Modifier.size(14.dp))
                }
              }
            }
          }
        }
      }

      // 8. Collective Community Milestone Progress
      if (featuredMilestone != null) {
        TactileCard(modifier = Modifier.fillMaxWidth()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "COMMUNITY LORE UNLOCKED",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp,
                color = WaypointTeal
              )
              Text(
                "${featuredMilestone.currentValue} / ${featuredMilestone.targetValue} ${featuredMilestone.unit}",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
              )
            }
            Text(
              featuredMilestone.title,
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              color = TextPrimary
            )
            Text(
              featuredMilestone.description,
              fontSize = 11.sp,
              fontFamily = FontFamily.Serif,
              color = TextMuted
            )
            SegmentedProgressBar(
              progressPercent = featuredMilestone.percent,
              totalSegments = 12,
              activeColor = WaypointTeal,
              inactiveColor = DarkSoilVariant
            )
            Text(
              "Stepped away from the feed. Biome lore logged.",
              fontSize = 10.sp,
              fontFamily = FontFamily.Serif,
              fontStyle = FontStyle.Italic,
              color = GrassNeon
            )
          }
        }
      }
    }
  }
}
