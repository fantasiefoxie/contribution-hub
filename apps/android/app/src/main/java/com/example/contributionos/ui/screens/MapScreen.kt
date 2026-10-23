package com.example.contributionos.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.contributionos.data.SeedMissionCatalog
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.model.ExpeditionNode
import com.example.contributionos.model.MapWaypoint
import com.example.contributionos.model.NodeStatus
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
import com.example.contributionos.theme.NewLeafGreen
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
import com.example.contributionos.ui.components.CivicHeader
import com.example.contributionos.ui.components.IsometricWorldCanvas
import com.example.contributionos.ui.components.TactileCard
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView

enum class MapViewTab {
  GEOGRAPHIC_MAP,
  TACTICAL_GRID,
  EXPEDITION_TREE
}

@Composable
fun MapScreen(
  expedition: CivicExpedition?,
  rewardedWaypointIds: Set<String> = emptySet(),
  customWaypoints: List<MapWaypoint> = emptyList(),
  isLive: Boolean,
  environmentMode: DataSourceMode = DataSourceMode.PRODUCTION,
  onToggleMode: () -> Unit = {},
  onNodeSelected: (ExpeditionNode) -> Unit,
  onSelectQuest: (String) -> Unit = {},
  onRecordDiscovery: (String, Int) -> Unit = { _, _ -> },
  onTriggerEncounter: (String, String) -> Unit = { _, _ -> },
  onToggleStance: ((String) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val waypoints = if (customWaypoints.isNotEmpty()) {
    customWaypoints
  } else if (environmentMode == DataSourceMode.SANDBOX_DEMO || expedition?.isSandboxDemo == true) {
    SeedMissionCatalog.testWaypoints
  } else {
    emptyList()
  }
  val isStationary = expedition?.activeStance == "STATIONARY"
  var activeTab by remember { mutableStateOf(MapViewTab.GEOGRAPHIC_MAP) }
  var selectedWaypoint by remember { mutableStateOf<MapWaypoint?>(waypoints.firstOrNull()) }
  var selectedExpeditionNode by remember {
    mutableStateOf(expedition?.nodes?.firstOrNull { it.status == NodeStatus.AVAILABLE })
  }
  var gpsStatusMessage by remember { mutableStateOf<String?>(null) }
  var mapLoadFailed by remember { mutableStateOf(false) }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      checkProximity(context, selectedWaypoint) { msg -> gpsStatusMessage = msg }
    } else {
      gpsStatusMessage = "Location permission declined. Stationary mode remains available."
    }
  }

  val transition = rememberInfiniteTransition(label = "waypoint_pulse")
  val pulseRadius by transition.animateFloat(
    initialValue = 0.9f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(1800, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_radius"
  )

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(WarmPaper)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      CivicHeader(
        districtName = "Nature Biome Map",
        seed = expedition?.activeRunSeed ?: 42L,
        isLive = isLive,
        environmentMode = environmentMode,
        onToggleMode = onToggleMode
      )

      // Active Expedition In-Field HUD (Stitch 08)
      val activeNode = expedition?.nodes?.firstOrNull { it.status == NodeStatus.AVAILABLE }
      if (expedition != null && activeNode != null && expedition.status == com.example.contributionos.model.ExpeditionStatus.ACTIVE) {
        val targetWp = waypoints.find { it.linkedTaskId == activeNode.taskId || it.id == activeNode.id || it.title.contains(activeNode.title) } ?: selectedWaypoint
        val arrivalCheck = evaluateGpsArrival(context, targetWp)
        val canTriggerEncounter = isStationary || arrivalCheck.isEligible

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .background(CardCream)
            .border(1.dp, CardBorderSubtle)
            .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Mode toggle row (Trekking vs Stationary)
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (!isStationary) MossGreenDark else CardCreamLow)
                    .border(1.dp, if (!isStationary) MossGreenDark else CardBorderSubtle, RoundedCornerShape(8.dp))
                    .clickable { onToggleStance?.invoke("TREK") }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(
                    "🚶 Trek (≤30m GPS)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (!isStationary) FontWeight.Bold else FontWeight.Medium,
                    color = if (!isStationary) Color.White else ForestInk
                  )
                }
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isStationary) MossGreenDark else CardCreamLow)
                    .border(1.dp, if (isStationary) MossGreenDark else CardBorderSubtle, RoundedCornerShape(8.dp))
                    .clickable { onToggleStance?.invoke("STATIONARY") }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                  Text(
                    "🪑 Stationary (Observe)",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = if (isStationary) FontWeight.Bold else FontWeight.Medium,
                    color = if (isStationary) Color.White else ForestInk
                  )
                }
              }

              if (!isStationary) {
                Text(
                  text = arrivalCheck.message,
                  fontSize = 10.sp,
                  fontFamily = FontFamily.SansSerif,
                  color = if (arrivalCheck.isEligible) MossGreenDark else ClayAccent,
                  fontWeight = FontWeight.Medium
                )
              }
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = "${expedition.title.removePrefix("[FIXTURE] ")} • Step ${activeNode.step + 1} of ${expedition.totalNodes}",
                  fontSize = 11.sp,
                  fontFamily = FontFamily.SansSerif,
                  fontWeight = FontWeight.Bold,
                  color = MossGreenDark
                )
                Text(
                  text = activeNode.title.removePrefix("[FIXTURE] "),
                  fontSize = 13.sp,
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = ForestInkDark
                )
              }

              Button(
                onClick = { onTriggerEncounter(expedition.id, activeNode.id) },
                enabled = canTriggerEncounter,
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (canTriggerEncounter) ClayAccent else CardBorderSubtle,
                  contentColor = if (canTriggerEncounter) CardCream else MutedSecondary,
                  disabledContainerColor = CardBorderSubtle,
                  disabledContentColor = MutedSecondary
                ),
                shape = RoundedCornerShape(9999.dp),
                modifier = Modifier.height(34.dp)
              ) {
                Text(
                  text = if (isStationary) {
                    "Observe • Encounter 🪑"
                  } else if (arrivalCheck.isEligible) {
                    "Arrived • Encounter 🧭"
                  } else if (arrivalCheck.distanceMeters != null) {
                    "Walk Closer (${arrivalCheck.distanceMeters}m)"
                  } else {
                    "GPS Lock Needed (≤30m)"
                  },
                  fontSize = 11.sp,
                  fontFamily = FontFamily.SansSerif,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }

      // Sub-Navigation Tabs (Clean pill style)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(CardCream)
          .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        listOf(
          Pair(MapViewTab.GEOGRAPHIC_MAP, "🌍 Live Map"),
          Pair(MapViewTab.TACTICAL_GRID, "🗺️ Offline Topo"),
          Pair(MapViewTab.EXPEDITION_TREE, "🧭 Trail Tree")
        ).forEach { (tab, label) ->
          val isSelected = activeTab == tab
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
              .clickable { activeTab = tab }
              .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = label,
              fontSize = 11.sp,
              fontFamily = FontFamily.SansSerif,
              fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
              color = if (isSelected) Color.White else ForestInk
            )
          }
        }
      }

      // Checkpoints Quick Selector Strip
      if (activeTab != MapViewTab.EXPEDITION_TREE) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(CardCreamLow)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp, vertical = 8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            "Checkpoints:",
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            color = MutedSecondary
          )
          waypoints.forEach { wp ->
            val isRewarded = rewardedWaypointIds.contains(wp.id)
            val isSelected = selectedWaypoint?.id == wp.id
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                  if (isSelected) LeafPillContainer
                  else if (isRewarded) SoftMeadowSand
                  else CardCream
                )
                .border(
                  1.dp,
                  if (isSelected) MossGreen else CardBorder,
                  RoundedCornerShape(999.dp)
                )
                .clickable {
                  selectedWaypoint = wp
                  gpsStatusMessage = null
                }
                .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(getCategoryIcon(wp.category), fontSize = 12.sp)
                Text(
                  text = wp.title.removePrefix("[TEST] ").take(16),
                  fontSize = 11.sp,
                  fontFamily = FontFamily.SansSerif,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) LeafPillText else ForestInk
                )
                if (isRewarded) {
                  Icon(Icons.Default.Check, contentDescription = "Discovered", tint = MossGreenDark, modifier = Modifier.size(13.dp))
                }
              }
            }
          }
        }
      }

      // Main Viewport
      when (activeTab) {
        MapViewTab.GEOGRAPHIC_MAP -> {
          Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            MapLibreGeographicView(
              waypoints = waypoints,
              selectedWaypoint = selectedWaypoint,
              onSelectWaypoint = { wp ->
                selectedWaypoint = wp
                gpsStatusMessage = null
              },
              onError = { mapLoadFailed = true }
            )

            // Attribution Overlay
            Box(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp, end = 8.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(CardCream.copy(alpha = 0.9f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
              Text(
                "Map data © OpenStreetMap | MapLibre",
                fontSize = 9.sp,
                fontFamily = FontFamily.SansSerif,
                color = MutedSecondary
              )
            }

            if (mapLoadFailed) {
              Box(
                modifier = Modifier
                  .align(Alignment.TopCenter)
                  .padding(10.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(SunGoldContainer)
                  .border(1.dp, SunGold.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                  .clickable { activeTab = MapViewTab.TACTICAL_GRID }
                  .padding(horizontal = 12.dp, vertical = 6.dp)
              ) {
                Text(
                  "Tiles unreachable offline. Tap for Offline Topo fallback.",
                  fontSize = 11.sp,
                  color = SunGoldText,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }

        MapViewTab.TACTICAL_GRID -> {
          Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            TacticalCanvasView(
              waypoints = waypoints,
              rewardedWaypointIds = rewardedWaypointIds,
              selectedWaypoint = selectedWaypoint,
              pulseRadius = pulseRadius,
              onSelectWaypoint = { wp ->
                selectedWaypoint = wp
                gpsStatusMessage = null
              }
            )

            Box(
              modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(CardCream.copy(alpha = 0.92f))
                .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                  "Neighborhood Topo Grid",
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Serif,
                  fontWeight = FontWeight.Bold,
                  color = ForestInkDark
                )
                Text(
                  "100% Offline Topographic Mode",
                  fontSize = 10.sp,
                  fontFamily = FontFamily.SansSerif,
                  color = MossGreenDark
                )
              }
            }
          }
        }

        MapViewTab.EXPEDITION_TREE -> {
          if (expedition == null || expedition.nodes.isEmpty()) {
            Box(
              modifier = Modifier.fillMaxSize().padding(24.dp),
              contentAlignment = Alignment.Center
            ) {
              TactileCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(12.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Text("🧭", fontSize = 32.sp)
                  Text("No Multi-Station Expeditions", fontSize = 16.sp, fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold, color = ForestInkDark)
                  Text(
                    "You are currently in Single Side Quest mode. Switch to Sandbox Demo mode to test the branching multi-checkpoint expedition trail.",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = MutedSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 17.sp
                  )
                  Button(onClick = onToggleMode, colors = ButtonDefaults.buttonColors(containerColor = MossGreenDark), shape = RoundedCornerShape(10.dp)) {
                    Text("Switch to Sandbox Mode", color = Color.White, fontWeight = FontWeight.Bold)
                  }
                }
              }
            }
          } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
              IsometricWorldCanvas(
                nodes = expedition.nodes,
                selectedNodeId = selectedExpeditionNode?.id,
                onNodeClick = { clickedNode -> selectedExpeditionNode = clickedNode },
                modifier = Modifier.fillMaxSize()
              )
            }
          }
        }
      }
    }

    // Waypoint Inspector Sheet (Smooth organic card)
    AnimatedVisibility(
      visible = activeTab != MapViewTab.EXPEDITION_TREE && selectedWaypoint != null,
      enter = slideInVertically(initialOffsetY = { it }),
      exit = slideOutVertically(targetOffsetY = { it }),
      modifier = Modifier.align(Alignment.BottomCenter)
    ) {
      selectedWaypoint?.let { wp ->
        val isRewarded = rewardedWaypointIds.contains(wp.id)
        TactileCard(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          borderColor = CardBorder,
          backgroundColor = CardCream,
          shape = RoundedCornerShape(20.dp),
          padding = 18.dp
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(LeafPillContainer)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                  Text(
                    wp.category,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.SemiBold,
                    color = LeafPillText
                  )
                }

                if (isRewarded) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(8.dp))
                      .background(SoftMeadowSand)
                      .padding(horizontal = 8.dp, vertical = 3.dp)
                  ) {
                    Text(
                      "✓ Discovered",
                      fontSize = 11.sp,
                      fontFamily = FontFamily.SansSerif,
                      fontWeight = FontWeight.SemiBold,
                      color = MossGreenDark
                    )
                  }
                }
              }

              IconButton(onClick = { selectedWaypoint = null }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = MutedSecondary)
              }
            }

            // Title & Description
            Text(
              wp.title.removePrefix("[TEST] "),
              fontSize = 17.sp,
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = ForestInkDark
            )
            Text(
              wp.description,
              fontSize = 13.sp,
              fontFamily = FontFamily.SansSerif,
              color = ForestInk.copy(alpha = 0.85f),
              lineHeight = 18.sp
            )

            // Reward & Coordinates
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "Coords: %.4f° N, %.4f° E".format(wp.latitude, wp.longitude),
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                color = MutedSecondary
              )
              Text(
                "+${wp.xpReward} Exploration XP",
                fontSize = 12.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = MossGreenDark
              )
            }

            // GPS Feedback Banner
            if (gpsStatusMessage != null) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(CardCreamLow)
                  .border(1.dp, CardBorderSubtle, RoundedCornerShape(8.dp))
                  .padding(10.dp)
              ) {
                Text(
                  gpsStatusMessage!!,
                  fontSize = 11.sp,
                  fontFamily = FontFamily.SansSerif,
                  color = ForestInk,
                  lineHeight = 15.sp
                )
              }
            }

            // Actions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
              if (wp.linkedTaskId != null) {
                val isQuestCompleted = rewardedWaypointIds.contains("quest-reward-${wp.linkedTaskId}")
                Button(
                  onClick = { onSelectQuest(wp.linkedTaskId) },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (isQuestCompleted) SoftMeadowSand else LeafPillContainer,
                    contentColor = MossGreenDark
                  ),
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.fillMaxWidth().height(42.dp)
                ) {
                  Icon(
                    if (isQuestCompleted) Icons.Default.Check else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = MossGreenDark,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    if (isQuestCompleted) "View Completed Quest (SQ-01)" else "Explore Linked Quest (SQ-01) →",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold
                  )
                }
              }

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                  onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                      checkProximity(context, wp) { msg -> gpsStatusMessage = msg }
                    } else {
                      locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = CardCreamLow, contentColor = ForestInk),
                  modifier = Modifier.weight(1f).height(40.dp),
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Icon(Icons.Default.LocationOn, contentDescription = null, tint = MossGreenDark, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Check GPS", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                  onClick = {
                    if (!isRewarded) {
                      onRecordDiscovery(wp.id, wp.xpReward)
                    }
                  },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRewarded) SoftMeadowSand else MossGreenDark,
                    contentColor = if (isRewarded) MutedSecondary else Color.White
                  ),
                  modifier = Modifier.weight(1.3f).height(40.dp),
                  shape = RoundedCornerShape(10.dp),
                  enabled = !isRewarded
                ) {
                  Text(
                    text = if (isRewarded) "✓ Discovered" else "Log Discovery (+${wp.xpReward} XP)",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
private fun MapLibreGeographicView(
  waypoints: List<MapWaypoint>,
  selectedWaypoint: MapWaypoint?,
  onSelectWaypoint: (MapWaypoint) -> Unit,
  onError: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val lifecycleOwner = LocalLifecycleOwner.current

  val mapView = remember {
    try {
      MapLibre.getInstance(context)
      MapView(context).apply {
        onCreate(null)
      }
    } catch (_: Exception) {
      null
    }
  }

  if (mapView == null) {
    onError()
    return
  }

  DisposableEffect(lifecycleOwner, mapView) {
    var isDestroyed = false

    val failListener = MapView.OnDidFailLoadingMapListener {
      onError()
    }
    mapView.addOnDidFailLoadingMapListener(failListener)

    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_START -> mapView.onStart()
        Lifecycle.Event.ON_RESUME -> mapView.onResume()
        Lifecycle.Event.ON_PAUSE -> mapView.onPause()
        Lifecycle.Event.ON_STOP -> mapView.onStop()
        Lifecycle.Event.ON_DESTROY -> {
          if (!isDestroyed) {
            isDestroyed = true
            mapView.onDestroy()
          }
        }
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)

    when (lifecycleOwner.lifecycle.currentState) {
      Lifecycle.State.STARTED -> mapView.onStart()
      Lifecycle.State.RESUMED -> {
        mapView.onStart()
        mapView.onResume()
      }
      else -> Unit
    }

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      try {
        mapView.removeOnDidFailLoadingMapListener(failListener)
      } catch (_: Exception) {}
      if (!isDestroyed) {
        isDestroyed = true
        try {
          mapView.onPause()
          mapView.onStop()
          mapView.onDestroy()
        } catch (_: Exception) {}
      }
    }
  }

  AndroidView(
    factory = {
      mapView.apply {
        getMapAsync { mapLibreMap ->
          val styleUrl = "https://demotiles.maplibre.org/style.json"
          mapLibreMap.setStyle(styleUrl) {
            mapLibreMap.clear()
            val camera = CameraPosition.Builder()
              .target(LatLng(12.9720, 77.5940))
              .zoom(13.8)
              .build()
            mapLibreMap.moveCamera(CameraUpdateFactory.newCameraPosition(camera))

            waypoints.forEach { wp ->
              mapLibreMap.addMarker(
                MarkerOptions()
                  .position(LatLng(wp.latitude, wp.longitude))
                  .title(wp.title)
                  .snippet(wp.category)
              )
            }

            mapLibreMap.setOnMarkerClickListener { marker ->
              val matched = waypoints.find { it.title == marker.title }
              if (matched != null) {
                onSelectWaypoint(matched)
              }
              true
            }
          }
        }
      }
    },
    modifier = modifier.fillMaxSize()
  )
}

@Composable
private fun TacticalCanvasView(
  waypoints: List<MapWaypoint>,
  rewardedWaypointIds: Set<String>,
  selectedWaypoint: MapWaypoint?,
  pulseRadius: Float,
  onSelectWaypoint: (MapWaypoint) -> Unit
) {
  val centerLat = 12.9720
  val centerLng = 77.5940
  val latRange = 0.016
  val lngRange = 0.016

  BoxWithConstraints(modifier = Modifier.fillMaxSize().background(WarmPaper)) {
    val widthPx = constraints.maxWidth.toFloat()
    val heightPx = constraints.maxHeight.toFloat()

    Canvas(modifier = Modifier.fillMaxSize()) {
      val gridSpacing = 60.dp.toPx()
      var x = 0f
      while (x <= size.width) {
        drawLine(
          color = CardBorder.copy(alpha = 0.5f),
          start = Offset(x, 0f),
          end = Offset(x, size.height),
          strokeWidth = 1f
        )
        x += gridSpacing
      }
      var y = 0f
      while (y <= size.height) {
        drawLine(
          color = CardBorder.copy(alpha = 0.5f),
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1f
        )
        y += gridSpacing
      }

      val centerX = size.width / 2f
      val centerY = size.height / 2f
      drawLine(color = MossGreen.copy(alpha = 0.4f), start = Offset(centerX - 20f, centerY), end = Offset(centerX + 20f, centerY), strokeWidth = 2f)
      drawLine(color = MossGreen.copy(alpha = 0.4f), start = Offset(centerX, centerY - 20f), end = Offset(centerX, centerY + 20f), strokeWidth = 2f)

      val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
      drawCircle(color = MossGreen.copy(alpha = 0.20f), radius = 100.dp.toPx(), center = Offset(centerX, centerY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f, pathEffect = dashEffect))
      drawCircle(color = MossGreen.copy(alpha = 0.15f), radius = 190.dp.toPx(), center = Offset(centerX, centerY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f, pathEffect = dashEffect))
      drawCircle(color = MossGreen.copy(alpha = 0.10f), radius = 280.dp.toPx(), center = Offset(centerX, centerY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f, pathEffect = dashEffect))
    }

    if (waypoints.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        TactileCard(
          backgroundColor = CardCream,
          borderColor = CardBorder,
          shape = RoundedCornerShape(16.dp),
          padding = 16.dp
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("🧭", fontSize = 28.sp)
            Text(
              "No Active Sector Checkpoints",
              fontSize = 14.sp,
              fontFamily = FontFamily.Serif,
              fontWeight = FontWeight.Bold,
              color = ForestInkDark
            )
            Text(
              "Verified field initiatives will display here when published. Toggle Sandbox Demo mode in the header to simulate checkpoints.",
              fontSize = 11.sp,
              fontFamily = FontFamily.SansSerif,
              color = MutedSecondary,
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
          }
        }
      }
    }

    waypoints.forEach { wp ->
      val isRewarded = rewardedWaypointIds.contains(wp.id)
      val isSelected = selectedWaypoint?.id == wp.id

      val xRatio = ((wp.longitude - centerLng) / lngRange).toFloat()
      val yRatio = ((wp.latitude - centerLat) / latRange).toFloat()
      val pinX = ((0.5f + xRatio) * widthPx).toInt().coerceIn(60, (widthPx - 100).toInt())
      val pinY = ((0.5f - yRatio) * heightPx).toInt().coerceIn(60, (heightPx - 120).toInt())

      Box(
        modifier = Modifier
          .offset { IntOffset(pinX - 35, pinY - 35) }
          .size(70.dp),
        contentAlignment = Alignment.Center
      ) {
        if (!isRewarded) {
          Box(
            modifier = Modifier
              .size((36 * pulseRadius).dp)
              .clip(CircleShape)
              .background(NewLeafGreen.copy(alpha = 0.35f / pulseRadius))
          )
        }

        if (isSelected) {
          Box(
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .border(2.dp, ClayAccent, CircleShape)
          )
        }

        Box(
          modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (isRewarded) LeafPillContainer else CardCream)
            .border(1.5.dp, if (isRewarded) MossGreen else if (isSelected) ClayAccent else CardBorder, CircleShape)
            .clickable { onSelectWaypoint(wp) },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (isRewarded) "✓" else getCategoryIcon(wp.category),
            fontSize = 13.sp
          )
        }
      }
    }
  }
}

private fun getCategoryIcon(category: String): String {
  return when {
    category.contains("Canopy", ignoreCase = true) || category.contains("Tree", ignoreCase = true) -> "🌱"
    category.contains("Water", ignoreCase = true) -> "💧"
    category.contains("Energy", ignoreCase = true) || category.contains("Solar", ignoreCase = true) -> "⚡"
    category.contains("Access", ignoreCase = true) || category.contains("Walk", ignoreCase = true) -> "♿"
    else -> "📍"
  }
}

const val CHECKPOINT_PROXIMITY_THRESHOLD_METERS = 30
const val MAX_STALE_FIX_AGE_MS = 60 * 1000L // 1 minute fresh fix required
const val MAX_ACCEPTABLE_ACCURACY_METERS = 50f // Accuracy <= 50m

data class GpsArrivalCheck(
  val isEligible: Boolean,
  val distanceMeters: Int? = null,
  val accuracyMeters: Float? = null,
  val isStale: Boolean = false,
  val message: String = ""
)

fun evaluateGpsArrival(context: Context, waypoint: MapWaypoint?): GpsArrivalCheck {
  if (waypoint == null) return GpsArrivalCheck(false, message = "No target waypoint coordinates.")
  try {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      ?: return GpsArrivalCheck(false, message = "Location service unavailable.")

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      return GpsArrivalCheck(false, message = "Location permission needed.")
    }

    val isGpsEnabled = try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }
    val isNetworkEnabled = try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false }
    if (!isGpsEnabled && !isNetworkEnabled) {
      return GpsArrivalCheck(false, message = "GPS disabled on device.")
    }

    val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
      ?: return GpsArrivalCheck(false, message = "Awaiting satellite fix...")

    val fixAgeMs = System.currentTimeMillis() - gpsLocation.time
    if (fixAgeMs > MAX_STALE_FIX_AGE_MS) {
      return GpsArrivalCheck(false, isStale = true, message = "GPS fix stale (>1m old). Walk into open sky.")
    }

    val accuracy = if (gpsLocation.hasAccuracy()) gpsLocation.accuracy else Float.MAX_VALUE
    if (accuracy > MAX_ACCEPTABLE_ACCURACY_METERS) {
      return GpsArrivalCheck(false, accuracyMeters = accuracy, message = "Low accuracy (±${accuracy.toInt()}m).")
    }

    val distResults = FloatArray(1)
    Location.distanceBetween(
      gpsLocation.latitude,
      gpsLocation.longitude,
      waypoint.latitude,
      waypoint.longitude,
      distResults
    )
    val distanceMeters = distResults[0].toInt()
    val isWithinProximity = distanceMeters <= CHECKPOINT_PROXIMITY_THRESHOLD_METERS

    return GpsArrivalCheck(
      isEligible = isWithinProximity,
      distanceMeters = distanceMeters,
      accuracyMeters = accuracy,
      message = if (isWithinProximity) "✓ Arrived within ≤30m (~${distanceMeters}m)." else "~${distanceMeters}m away (≤30m needed)."
    )
  } catch (e: Exception) {
    return GpsArrivalCheck(false, message = "GPS check error: ${e.message}")
  }
}

private fun checkProximity(context: Context, waypoint: MapWaypoint?, onResult: (String) -> Unit) {
  val check = evaluateGpsArrival(context, waypoint)
  onResult(check.message)
}
