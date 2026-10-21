package com.example.contributionos.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.example.contributionos.model.ExpeditionNode
import com.example.contributionos.model.NodeStatus
import com.example.contributionos.theme.CivicBorderActive
import com.example.contributionos.theme.CivicBorderSubtle
import com.example.contributionos.theme.CivicCyan
import com.example.contributionos.theme.CivicEmerald
import com.example.contributionos.theme.CivicForestDark
import com.example.contributionos.theme.CivicMidnightNavy
import com.example.contributionos.theme.CivicSurfaceDark
import com.example.contributionos.theme.CivicSurfacePanel
import com.example.contributionos.theme.NodeAvailable
import com.example.contributionos.theme.NodeInProgress
import com.example.contributionos.theme.NodeLocked
import com.example.contributionos.theme.NodeValidated
import kotlin.math.sqrt

@Composable
fun IsometricWorldCanvas(
  nodes: List<ExpeditionNode>,
  selectedNodeId: String?,
  onNodeClick: (ExpeditionNode) -> Unit,
  modifier: Modifier = Modifier
) {
  val infiniteTransition = rememberInfiniteTransition(label = "beacon_pulse")
  val pulseRadius by infiniteTransition.animateFloat(
    initialValue = 0.8f,
    targetValue = 1.35f,
    animationSpec = infiniteRepeatable(
      animation = tween(2200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse_radius"
  )

  Box(modifier = modifier) {
    Canvas(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(nodes) {
          detectTapGestures { tapOffset ->
            val w = size.width
            val h = size.height
            // Check tap collision with any node
            val clickedNode = nodes.minByOrNull { node ->
              val nodeScreenPos = Offset(node.isoX * w, node.isoY * h)
              (nodeScreenPos - tapOffset).getDistance()
            }
            if (clickedNode != null) {
              val nodeScreenPos = Offset(clickedNode.isoX * w, clickedNode.isoY * h)
              if ((nodeScreenPos - tapOffset).getDistance() < 55.dp.toPx()) {
                onNodeClick(clickedNode)
              }
            }
          }
        }
    ) {
      val canvasWidth = size.width
      val canvasHeight = size.height

      // 1. Draw Isometric Base Tiles
      drawIsometricTerrain(canvasWidth, canvasHeight)

      // 2. Draw Branching Connecting Routes
      drawBranchingRoutes(nodes, canvasWidth, canvasHeight)

      // 3. Draw Isometric Landmarks (Canopy groves, River, Towers)
      drawIsometricDecorations(canvasWidth, canvasHeight)

      // 4. Draw Interactive Mission Nodes
      drawMissionNodes(nodes, selectedNodeId, pulseRadius, canvasWidth, canvasHeight)
    }
  }
}

private fun DrawScope.drawIsometricTerrain(w: Float, h: Float) {
  // Deep background
  drawRect(color = CivicForestDark)

  // Draw grid lines in isometric angle
  val tileW = w / 4.5f
  val tileH = tileW * 0.55f

  val cols = 6
  val rows = 8

  for (col in -2..cols) {
    for (row in 0..rows) {
      val cx = (col - row) * (tileW * 0.5f) + w * 0.55f
      val cy = (col + row) * (tileH * 0.5f) - h * 0.1f

      val path = Path().apply {
        moveTo(cx, cy - tileH * 0.5f)
        lineTo(cx + tileW * 0.5f, cy)
        lineTo(cx, cy + tileH * 0.5f)
        lineTo(cx - tileW * 0.5f, cy)
        close()
      }

      // Vary ground tint
      val isRiver = (col + row) in 3..4
      val tileColor = when {
        isRiver -> CivicMidnightNavy.copy(alpha = 0.5f)
        (col * 7 + row) % 3 == 0 -> CivicSurfaceDark.copy(alpha = 0.6f)
        else -> CivicSurfacePanel.copy(alpha = 0.35f)
      }

      drawPath(path, color = tileColor)
      drawPath(path, color = CivicBorderSubtle.copy(alpha = 0.25f), style = Stroke(width = 1f))
    }
  }
}

private fun DrawScope.drawBranchingRoutes(
  nodes: List<ExpeditionNode>,
  w: Float,
  h: Float
) {
  val nodeMap = nodes.associateBy { it.id }

  for (node in nodes) {
    val start = Offset(node.isoX * w, node.isoY * h)
    for (childId in node.childNodeIds) {
      val child = nodeMap[childId] ?: continue
      val end = Offset(child.isoX * w, child.isoY * h)

      val isActiveRoute = node.status == NodeStatus.VALIDATED &&
        (child.status == NodeStatus.AVAILABLE || child.status == NodeStatus.IN_PROGRESS || child.status == NodeStatus.VALIDATED)

      // Outer glow line
      if (isActiveRoute) {
        drawLine(
          color = CivicCyan.copy(alpha = 0.25f),
          start = start,
          end = end,
          strokeWidth = 10f,
          cap = StrokeCap.Round
        )
      }

      // Core line
      drawLine(
        color = if (isActiveRoute) CivicCyan else CivicBorderSubtle.copy(alpha = 0.6f),
        start = start,
        end = end,
        strokeWidth = if (isActiveRoute) 4f else 2.5f,
        cap = StrokeCap.Round
      )
    }
  }
}

private fun DrawScope.drawIsometricDecorations(w: Float, h: Float) {
  // Tree grove 1 (Isometric pine pyramid)
  drawIsometricPine(w * 0.15f, h * 0.35f, size = 26f, color = Color(0xFF0F3B28))
  drawIsometricPine(w * 0.18f, h * 0.40f, size = 32f, color = Color(0xFF134832))
  drawIsometricPine(w * 0.12f, h * 0.43f, size = 22f, color = Color(0xFF0B2D1E))

  // River buffer grove
  drawIsometricPine(w * 0.82f, h * 0.68f, size = 28f, color = Color(0xFF11422E))
  drawIsometricPine(w * 0.88f, h * 0.72f, size = 24f, color = Color(0xFF0E3827))

  // Solar Array Pavilion
  val pavilionCenter = Offset(w * 0.38f, h * 0.30f)
  drawCircle(color = CivicMidnightNavy.copy(alpha = 0.7f), radius = 16f, center = pavilionCenter)
  drawCircle(color = CivicCyan.copy(alpha = 0.3f), radius = 16f, center = pavilionCenter, style = Stroke(1.5f))
}

private fun DrawScope.drawIsometricPine(x: Float, y: Float, size: Float, color: Color) {
  val path = Path().apply {
    moveTo(x, y - size)
    lineTo(x + size * 0.6f, y)
    lineTo(x - size * 0.6f, y)
    close()
  }
  drawPath(path, color = color)
  drawCircle(color = color.copy(alpha = 0.4f), radius = size * 0.4f, center = Offset(x, y))
}

private fun DrawScope.drawMissionNodes(
  nodes: List<ExpeditionNode>,
  selectedNodeId: String?,
  pulseRatio: Float,
  w: Float,
  h: Float
) {
  for (node in nodes) {
    val pos = Offset(node.isoX * w, node.isoY * h)
    val isSelected = node.id == selectedNodeId

    val baseColor = when (node.status) {
      NodeStatus.AVAILABLE -> NodeAvailable
      NodeStatus.IN_PROGRESS -> NodeInProgress
      NodeStatus.VALIDATED -> NodeValidated
      NodeStatus.LOCKED -> NodeLocked
    }

    // Outer beacon pulse for available or in-progress nodes
    if (node.status == NodeStatus.AVAILABLE || node.status == NodeStatus.IN_PROGRESS) {
      drawCircle(
        color = baseColor.copy(alpha = 0.25f / pulseRatio),
        radius = 28f * pulseRatio,
        center = pos
      )
    }

    if (isSelected) {
      // Golden selector halo
      drawCircle(
        color = Color(0xFFFDE047).copy(alpha = 0.4f),
        radius = 34f,
        center = pos,
        style = Stroke(width = 3f)
      )
    }

    // Outer node plate
    drawCircle(
      color = CivicSurfaceDark,
      radius = 20f,
      center = pos
    )
    drawCircle(
      color = baseColor,
      radius = 20f,
      center = pos,
      style = Stroke(width = 3f)
    )

    // Inner node core
    drawCircle(
      color = baseColor,
      radius = 11f,
      center = pos
    )

    // Highlight dot for validated nodes
    if (node.status == NodeStatus.VALIDATED) {
      drawCircle(
        color = Color.White,
        radius = 4f,
        center = pos
      )
    }
  }
}
