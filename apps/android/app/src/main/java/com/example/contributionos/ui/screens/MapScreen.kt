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
import com.example.contributionos.theme.DarkSoilVariant
import com.example.contributionos.theme.ForestFloor
import com.example.contributionos.theme.GrassNeon
import com.example.contributionos.theme.MutedMoss
import com.example.contributionos.theme.NodeAvailable
import com.example.contributionos.theme.NodeInProgress
import com.example.contributionos.theme.NodeLocked
import com.example.contributionos.theme.NodeValidated
import com.example.contributionos.theme.SoilSurfacePanel
import com.example.contributionos.theme.SoilVoid
import com.example.contributionos.theme.SunGold
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.theme.WaypointTeal
import com.example.contributionos.ui.components.CivicHeader
import com.example.contributionos.ui.components.IsometricWorldCanvas
import com.example.contributionos.ui.components.LayeredPanel
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
  waypoints: List<MapWaypoint> = SeedMissionCatalog.testWaypoints,
  isLive: Boolean,
  environmentMode: DataSourceMode = DataSourceMode.PRODUCTION,
  onToggleMode: () -> Unit = {},
  onNodeSelected: (ExpeditionNode) -> Unit,
  onSelectQuest: (String) -> Unit = {},
  onRecordDiscovery: (String, Int) -> Unit = { _, _ -> },
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var activeTab by remember { mutableStateOf(MapViewTab.GEOGRAPHIC_MAP) }
  var selectedWaypoint by remember { mutableStateOf<MapWaypoint?>(waypoints.firstOrNull()) }
  var selectedExpeditionNode by remember {
    mutableStateOf(expedition?.nodes?.firstOrNull { it.status == NodeStatus.AVAILABLE })
  }
  var gpsStatusMessage by remember { mutableStateOf<String?>(null) }
  var mapLoadFailed by remember { mutableStateOf(false) }

  // Foreground location permission launcher: ONLY requested after explicit user tap
  val locationPermissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      checkProximity(context, selectedWaypoint) { msg -> gpsStatusMessage = msg }
    } else {
      gpsStatusMessage = "Location permission declined. Manual discovery mode is fully operational."
    }
  }

  // Animation for waypoint radar beacon in tactical view
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
      .background(CivicForestDark)
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      CivicHeader(
        districtName = "Local Neighborhood Biome",
        seed = expedition?.activeRunSeed ?: 42L,
        isLive = isLive,
        environmentMode = environmentMode,
        onToggleMode = onToggleMode
      )

      // Sub-Navigation Tabs: Real Geographic Map vs Tactical Grid vs Roguelite Run Tree
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(CivicSurfaceDark)
          .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        // Tab 1: Real Map (MapLibre Online Prototype)
        Box(
          modifier = Modifier
            .weight(1.15f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (activeTab == MapViewTab.GEOGRAPHIC_MAP) GrassNeon.copy(alpha = 0.2f) else CivicSurfacePanel)
            .border(1.dp, if (activeTab == MapViewTab.GEOGRAPHIC_MAP) GrassNeon else CivicBorderSubtle, RoundedCornerShape(6.dp))
            .clickable { activeTab = MapViewTab.GEOGRAPHIC_MAP }
            .padding(vertical = 7.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "🌍 Real Map (Online)",
            fontSize = 10.sp,
            fontWeight = if (activeTab == MapViewTab.GEOGRAPHIC_MAP) FontWeight.Bold else FontWeight.Medium,
            color = if (activeTab == MapViewTab.GEOGRAPHIC_MAP) GrassNeon else TextSecondary
          )
        }

        // Tab 2: Tactical Grid (100% Offline Fallback)
        Box(
          modifier = Modifier
            .weight(1.15f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (activeTab == MapViewTab.TACTICAL_GRID) GrassNeon.copy(alpha = 0.2f) else CivicSurfacePanel)
            .border(1.dp, if (activeTab == MapViewTab.TACTICAL_GRID) GrassNeon else CivicBorderSubtle, RoundedCornerShape(6.dp))
            .clickable { activeTab = MapViewTab.TACTICAL_GRID }
            .padding(vertical = 7.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "🗺️ Tactical (Offline)",
            fontSize = 10.sp,
            fontWeight = if (activeTab == MapViewTab.TACTICAL_GRID) FontWeight.Bold else FontWeight.Medium,
            color = if (activeTab == MapViewTab.TACTICAL_GRID) GrassNeon else TextSecondary
          )
        }

        // Tab 3: Roguelite Run Tree
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (activeTab == MapViewTab.EXPEDITION_TREE) GrassNeon.copy(alpha = 0.2f) else CivicSurfacePanel)
            .border(1.dp, if (activeTab == MapViewTab.EXPEDITION_TREE) GrassNeon else CivicBorderSubtle, RoundedCornerShape(6.dp))
            .clickable { activeTab = MapViewTab.EXPEDITION_TREE }
            .padding(vertical = 7.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = "🧭 Run Tree",
            fontSize = 11.sp,
            fontWeight = if (activeTab == MapViewTab.EXPEDITION_TREE) FontWeight.Bold else FontWeight.Medium,
            color = if (activeTab == MapViewTab.EXPEDITION_TREE) GrassNeon else TextSecondary
          )
        }
      }

      // Waypoint Quick Selector Strip
      if (activeTab != MapViewTab.EXPEDITION_TREE) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(CivicMidnightNavy.copy(alpha = 0.95f))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text("CHECKPOINTS:", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextMuted)
          waypoints.forEach { wp ->
            val isRewarded = rewardedWaypointIds.contains(wp.id)
            val isSelected = selectedWaypoint?.id == wp.id
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(
                  if (isSelected) GrassNeon.copy(alpha = 0.25f)
                  else if (isRewarded) CivicForestDark
                  else CivicSurfacePanel
                )
                .border(
                  1.dp,
                  if (isSelected) GrassNeon else if (isRewarded) CivicEmerald else CivicBorderSubtle,
                  RoundedCornerShape(6.dp)
                )
                .clickable {
                  selectedWaypoint = wp
                  gpsStatusMessage = null
                }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(getCategoryIcon(wp.category), fontSize = 11.sp)
                Text(
                  text = wp.title.removePrefix("[TEST] ").take(16),
                  fontSize = 11.sp,
                  fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                  color = if (isSelected) GrassNeon else if (isRewarded) CivicEmerald else TextPrimary
                )
                if (isRewarded) {
                  Icon(Icons.Default.Check, contentDescription = "Discovered", tint = GrassNeon, modifier = Modifier.size(12.dp))
                }
              }
            }
          }
        }
      }

      // Main Content Area
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

            // Attribution & Status Overlay
            Box(
              modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 8.dp, end = 8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CivicMidnightNavy.copy(alpha = 0.85f))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                "Map data © OpenStreetMap contributors | MapLibre",
                fontSize = 8.sp,
                color = TextMuted
              )
            }

            // Fallback Banner if Map Loading has trouble
            if (mapLoadFailed) {
              Box(
                modifier = Modifier
                  .align(Alignment.TopCenter)
                  .padding(8.dp)
                  .clip(RoundedCornerShape(6.dp))
                  .background(CivicAmber.copy(alpha = 0.9f))
                  .clickable { activeTab = MapViewTab.TACTICAL_GRID }
                  .padding(horizontal = 10.dp, vertical = 4.dp)
              ) {
                Text("Map tile network unreachable. Tap for Tactical Grid fallback.", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
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

            // Tactical Grid Overlay HUD
            Box(
              modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(CivicSurfaceDark.copy(alpha = 0.85f))
                .border(1.dp, CivicBorderSubtle, RoundedCornerShape(6.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text("GRID: 12.972° N, 77.594° E", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GrassNeon)
                Text("TACTICAL OFFLINE FALLBACK", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = CivicAmber)
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
              LayeredPanel(modifier = Modifier.fillMaxWidth()) {
                Column(
                  modifier = Modifier.fillMaxWidth().padding(10.dp),
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Icon(Icons.Default.Info, contentDescription = null, tint = CivicCyan, modifier = Modifier.size(36.dp))
                  Text("NO EXPEDITIONS IN PRODUCTION BIOME", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                  Text(
                    "There are no active multi-station initiatives configured on the Supabase backend. Switch to Astroturf (Sandbox) to test the branching run tree.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    lineHeight = 16.sp
                  )
                  Button(onClick = onToggleMode, colors = ButtonDefaults.buttonColors(containerColor = GrassNeon), shape = RoundedCornerShape(8.dp)) {
                    Text("Launch Astroturf Mode", color = Color.Black, fontWeight = FontWeight.Bold)
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

    // Waypoint Inspector Sheet (Visible in both Geographic and Tactical modes)
    AnimatedVisibility(
      visible = activeTab != MapViewTab.EXPEDITION_TREE && selectedWaypoint != null,
      enter = slideInVertically(initialOffsetY = { it }),
      exit = slideOutVertically(targetOffsetY = { it }),
      modifier = Modifier.align(Alignment.BottomCenter)
    ) {
      selectedWaypoint?.let { wp ->
        val isRewarded = rewardedWaypointIds.contains(wp.id)
        LayeredPanel(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          borderColor = if (isRewarded) GrassNeon else SunGold
        ) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            // Header Row
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(SunGold.copy(alpha = 0.15f))
                    .border(1.dp, SunGold, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    "[TEST CHECKPOINT - SANDBOX]",
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.ExtraBold,
                    color = SunGold
                  )
                }

                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(com.example.contributionos.theme.DarkSoilVariant)
                    .border(1.dp, com.example.contributionos.theme.MutedMoss, RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    wp.category,
                    fontSize = 9.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                  )
                }
              }

              IconButton(onClick = { selectedWaypoint = null }, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
              }
            }

            // Waypoint Title & Description
            Text(wp.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(
              wp.description,
              fontSize = 12.sp,
              fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
              color = TextSecondary,
              lineHeight = 16.sp
            )

            // Coordinates & Status Bar
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "Coords: %.4f° N, %.4f° E".format(wp.latitude, wp.longitude),
                fontSize = 10.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                color = TextMuted
              )
              Text(
                if (isRewarded) "✓ DISCOVERED (+${wp.xpReward} XP)" else "+${wp.xpReward} Exploration XP Available",
                fontSize = 11.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isRewarded) GrassNeon else SunGold
              )
            }

            // GPS Feedback Banner
            if (gpsStatusMessage != null) {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(4.dp))
                  .background(com.example.contributionos.theme.DarkSoilVariant)
                  .border(1.dp, com.example.contributionos.theme.WaypointTeal, RoundedCornerShape(4.dp))
                  .padding(8.dp)
              ) {
                Text(
                  gpsStatusMessage!!,
                  fontSize = 11.sp,
                  fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                  color = com.example.contributionos.theme.WaypointTeal,
                  lineHeight = 14.sp
                )
              }
            }

            // Action Buttons
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
              // Linked Quest Button (if applicable)
              if (wp.linkedTaskId != null) {
                val isQuestCompleted = rewardedWaypointIds.contains("quest-reward-${wp.linkedTaskId}")
                Button(
                  onClick = { onSelectQuest(wp.linkedTaskId) },
                  colors = ButtonDefaults.buttonColors(containerColor = com.example.contributionos.theme.DarkSoilVariant),
                  modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, com.example.contributionos.theme.MutedMoss, RoundedCornerShape(6.dp)),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Icon(
                    if (isQuestCompleted) Icons.Default.Check else Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = if (isQuestCompleted) GrassNeon else GrassNeon,
                    modifier = Modifier.size(16.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    if (isQuestCompleted) "View Linked Quest: [COMPLETED ✓]" else "View Linked Quest: [SELF-GUIDED]",
                    fontSize = 12.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isQuestCompleted) GrassNeon else TextPrimary
                  )
                }
              }

              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Foreground GPS Test Button
                Button(
                  onClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                      checkProximity(context, wp) { msg -> gpsStatusMessage = msg }
                    } else {
                      locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = com.example.contributionos.theme.DarkSoilVariant),
                  modifier = Modifier
                    .weight(1f)
                    .border(1.dp, com.example.contributionos.theme.MutedMoss, RoundedCornerShape(6.dp)),
                  shape = RoundedCornerShape(6.dp)
                ) {
                  Icon(Icons.Default.LocationOn, contentDescription = null, tint = com.example.contributionos.theme.WaypointTeal, modifier = Modifier.size(14.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(
                    "Check GPS",
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = TextPrimary
                  )
                }

                // Manual Test Discovery Button
                Button(
                  onClick = {
                    if (!isRewarded) {
                      onRecordDiscovery(wp.id, wp.xpReward)
                    }
                  },
                  colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRewarded) com.example.contributionos.theme.DarkSoilVariant else GrassNeon
                  ),
                  modifier = Modifier
                    .weight(1.3f)
                    .border(
                      1.dp,
                      if (isRewarded) com.example.contributionos.theme.MutedMoss else GrassNeon,
                      RoundedCornerShape(6.dp)
                    ),
                  shape = RoundedCornerShape(6.dp),
                  enabled = !isRewarded
                ) {
                  Text(
                    text = if (isRewarded) "✓ Discovered" else "Log Discovery (+${wp.xpReward} XP)",
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (isRewarded) TextMuted else com.example.contributionos.theme.SoilVoid
                  )
                }
              }

              Text(
                "Manual test discovery operates independently of GPS verification. Awards Sandbox Exploration XP only (0 Civic Credits).",
                fontSize = 9.sp,
                fontFamily = androidx.compose.ui.text.font.FontFamily.Serif,
                color = TextMuted,
                lineHeight = 12.sp
              )
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

  // Safe lazy instantiation of MapView with native library check
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

  // Manage native MapView lifecycle
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

    // Match current lifecycle state upon entering composition
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
          // Use MapLibre public development demo style or CARTO style
          val styleUrl = "https://demotiles.maplibre.org/style.json"
          mapLibreMap.setStyle(styleUrl) { style ->
            // Clear existing markers before re-adding to prevent marker duplication on reload
            mapLibreMap.clear()

            // Initial camera position centered on deterministic Bangalore test sector
            val camera = CameraPosition.Builder()
              .target(LatLng(12.9720, 77.5940))
              .zoom(13.8)
              .build()
            mapLibreMap.moveCamera(CameraUpdateFactory.newCameraPosition(camera))

            // Add deterministic test waypoints as geographic markers
            waypoints.forEach { wp ->
              mapLibreMap.addMarker(
                MarkerOptions()
                  .position(LatLng(wp.latitude, wp.longitude))
                  .title(wp.title)
                  .snippet(wp.category)
              )
            }

            // Marker click handler: maps to existing waypoint inspector
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

  BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    val widthPx = constraints.maxWidth.toFloat()
    val heightPx = constraints.maxHeight.toFloat()

    // 1. Tactical Vector Canvas (Grid, concentric distance rings, axes)
    Canvas(modifier = Modifier.fillMaxSize()) {
      // Grid lines
      val gridSpacing = 60.dp.toPx()
      var x = 0f
      while (x <= size.width) {
        drawLine(
          color = CivicBorderSubtle.copy(alpha = 0.45f),
          start = Offset(x, 0f),
          end = Offset(x, size.height),
          strokeWidth = 1f
        )
        x += gridSpacing
      }
      var y = 0f
      while (y <= size.height) {
        drawLine(
          color = CivicBorderSubtle.copy(alpha = 0.45f),
          start = Offset(0f, y),
          end = Offset(size.width, y),
          strokeWidth = 1f
        )
        y += gridSpacing
      }

      // Center crosshairs
      val centerX = size.width / 2f
      val centerY = size.height / 2f
      drawLine(color = GrassNeon.copy(alpha = 0.6f), start = Offset(centerX - 24f, centerY), end = Offset(centerX + 24f, centerY), strokeWidth = 2f)
      drawLine(color = GrassNeon.copy(alpha = 0.6f), start = Offset(centerX, centerY - 24f), end = Offset(centerX, centerY + 24f), strokeWidth = 2f)

      // Range Rings (150m, 350m, 600m)
      val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
      drawCircle(color = CivicCyan.copy(alpha = 0.25f), radius = 100.dp.toPx(), center = Offset(centerX, centerY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f, pathEffect = dashEffect))
      drawCircle(color = CivicCyan.copy(alpha = 0.20f), radius = 190.dp.toPx(), center = Offset(centerX, centerY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f, pathEffect = dashEffect))
      drawCircle(color = CivicCyan.copy(alpha = 0.15f), radius = 280.dp.toPx(), center = Offset(centerX, centerY), style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5f, pathEffect = dashEffect))
    }

    // 2. Waypoint Beacon Composables
    waypoints.forEach { wp ->
      val isRewarded = rewardedWaypointIds.contains(wp.id)
      val isSelected = selectedWaypoint?.id == wp.id

      val xRatio = ((wp.longitude - centerLng) / lngRange).toFloat()
      val yRatio = ((wp.latitude - centerLat) / latRange).toFloat()
      val pinX = ((0.5f + xRatio) * widthPx).toInt().coerceIn(60, (widthPx - 100).toInt())
      val pinY = ((0.5f - yRatio) * heightPx).toInt().coerceIn(60, (heightPx - 120).toInt())

      Box(
        modifier = Modifier
          .offset { IntOffset(pinX - 40, pinY - 40) }
          .size(80.dp),
        contentAlignment = Alignment.Center
      ) {
        // Radar Pulse Circle for undiscovered waypoints
        if (!isRewarded) {
          Box(
            modifier = Modifier
              .size((44 * pulseRadius).dp)
              .clip(CircleShape)
              .background(GrassNeon.copy(alpha = 0.25f / pulseRadius))
          )
        }

        // Selected halo
        if (isSelected) {
          Box(
            modifier = Modifier
              .size(52.dp)
              .clip(CircleShape)
              .border(2.dp, CivicAmber, CircleShape)
          )
        }

        // Waypoint Pin Button
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(if (isRewarded) CivicForestDark else CivicMidnightNavy)
            .border(2.dp, if (isRewarded) CivicEmerald else if (isSelected) CivicAmber else GrassNeon, CircleShape)
            .clickable { onSelectWaypoint(wp) },
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = if (isRewarded) "✓" else getCategoryIcon(wp.category),
            fontSize = if (isRewarded) 15.sp else 13.sp,
            color = if (isRewarded) GrassNeon else TextPrimary,
            fontWeight = FontWeight.Bold
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

const val CHECKPOINT_PROXIMITY_THRESHOLD_METERS = 50
const val MAX_STALE_FIX_AGE_MS = 15 * 60 * 1000L // 15 minutes
const val MAX_ACCEPTABLE_ACCURACY_METERS = 65f

private fun checkProximity(context: Context, waypoint: MapWaypoint?, onResult: (String) -> Unit) {
  if (waypoint == null) return
  try {
    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (locationManager == null) {
      onResult("LocationManager service unavailable.")
      return
    }

    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
      onResult("Location permission not granted. Manual test discovery is fully available without GPS.")
      return
    }

    val isGpsEnabled = try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }
    val isNetworkEnabled = try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false }
    if (!isGpsEnabled && !isNetworkEnabled) {
      onResult("Location services are disabled on device. Enable GPS in Settings, or use Manual Discovery.")
      return
    }

    val gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

    if (gpsLocation == null) {
      onResult("Awaiting GPS satellite fix. No recent location fix available on device.")
      return
    }

    val fixAgeMs = System.currentTimeMillis() - gpsLocation.time
    val isStale = fixAgeMs > MAX_STALE_FIX_AGE_MS
    val accuracy = if (gpsLocation.hasAccuracy()) gpsLocation.accuracy else Float.MAX_VALUE
    val isInaccurate = accuracy > MAX_ACCEPTABLE_ACCURACY_METERS

    val distResults = FloatArray(1)
    Location.distanceBetween(
      gpsLocation.latitude,
      gpsLocation.longitude,
      waypoint.latitude,
      waypoint.longitude,
      distResults
    )
    val distanceMeters = distResults[0].toInt()

    if (isStale) {
      onResult("GPS fix is stale (${fixAgeMs / 60000}m old, ~%d m away). Proximity unconfirmed until fresh satellite lock. Manual test discovery remains active.".format(distanceMeters))
    } else if (isInaccurate) {
      onResult("GPS accuracy is low (±%d m, ~%d m away). Proximity unconfirmed. Manual test discovery is available.".format(accuracy.toInt(), distanceMeters))
    } else if (distanceMeters <= CHECKPOINT_PROXIMITY_THRESHOLD_METERS) {
      onResult("✓ GPS PROXIMITY CONFIRMED: ~%d m from checkpoint (Threshold: ≤%d m, accuracy ±%d m).".format(distanceMeters, CHECKPOINT_PROXIMITY_THRESHOLD_METERS, accuracy.toInt()))
    } else {
      onResult("GPS Fix acquired: ~%d m away. Outside proximity threshold (≤%d m). Proximity not confirmed. Manual test discovery remains active.".format(distanceMeters, CHECKPOINT_PROXIMITY_THRESHOLD_METERS))
    }
  } catch (e: Exception) {
    onResult("GPS reading exception: ${e.message}")
  }
}
