package com.example.contributionos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.theme.CivicBorderSubtle
import com.example.contributionos.theme.CivicEmerald
import com.example.contributionos.theme.CivicSurfaceDark
import com.example.contributionos.theme.CivicSurfacePanel
import com.example.contributionos.theme.NodeInProgress
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary

enum class AppDestination(val title: String, val icon: ImageVector) {
  HOME("Home", Icons.Default.Home),
  MAP("Map", Icons.Default.LocationOn),
  MISSIONS("Missions", Icons.Default.Menu),
  IMPACT("Impact", Icons.Default.Star),
  PROFILE("Profile", Icons.Default.Person)
}

@Composable
fun CivicBottomNav(
  currentDestination: AppDestination,
  onDestinationSelected: (AppDestination) -> Unit,
  draftCount: Int = 0,
  modifier: Modifier = Modifier
) {
  NavigationBar(
    modifier = modifier
      .background(CivicSurfaceDark)
      .border(0.75.dp, CivicBorderSubtle),
    containerColor = CivicSurfaceDark,
    contentColor = TextPrimary,
    tonalElevation = 8.dp
  ) {
    AppDestination.values().forEach { destination ->
      val selected = destination == currentDestination
      NavigationBarItem(
        selected = selected,
        onClick = { onDestinationSelected(destination) },
        icon = {
          if (destination == AppDestination.MISSIONS && draftCount > 0) {
            BadgedBox(badge = {
              Badge(containerColor = NodeInProgress) {
                Text(draftCount.toString(), fontSize = 10.sp, fontWeight = FontWeight.Bold)
              }
            }) {
              Icon(
                imageVector = destination.icon,
                contentDescription = destination.title,
                modifier = Modifier.size(24.dp)
              )
            }
          } else {
            Icon(
              imageVector = destination.icon,
              contentDescription = destination.title,
              modifier = Modifier.size(24.dp)
            )
          }
        },
        label = {
          Text(
            text = destination.title,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = CivicEmerald,
          selectedTextColor = CivicEmerald,
          indicatorColor = CivicSurfacePanel,
          unselectedIconColor = TextMuted,
          unselectedTextColor = TextMuted
        )
      )
    }
  }
}
