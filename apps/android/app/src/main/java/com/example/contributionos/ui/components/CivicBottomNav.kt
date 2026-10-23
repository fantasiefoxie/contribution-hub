package com.example.contributionos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.theme.DarkSoilVariant
import com.example.contributionos.theme.ForestFloor
import com.example.contributionos.theme.GrassNeon
import com.example.contributionos.theme.MutedLichen
import com.example.contributionos.theme.MutedMoss
import com.example.contributionos.theme.SoilSurfacePanel
import com.example.contributionos.theme.SunGold
import com.example.contributionos.theme.WeatheredPaper

enum class AppDestination(val title: String, val icon: ImageVector) {
  HOME("Camp", Icons.Default.Home),
  MAP("Map", Icons.Default.LocationOn),
  MISSIONS("Quests", Icons.Default.Menu),
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
      .background(ForestFloor)
      .border(1.dp, MutedMoss),
    containerColor = ForestFloor,
    contentColor = WeatheredPaper,
    tonalElevation = 0.dp
  ) {
    AppDestination.values().forEach { destination ->
      val selected = destination == currentDestination
      NavigationBarItem(
        selected = selected,
        onClick = { onDestinationSelected(destination) },
        icon = {
          if (destination == AppDestination.MISSIONS && draftCount > 0) {
            BadgedBox(badge = {
              Badge(containerColor = SunGold) {
                Text(
                  draftCount.toString(),
                  fontSize = 10.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = ForestFloor
                )
              }
            }) {
              Icon(
                imageVector = destination.icon,
                contentDescription = destination.title,
                modifier = Modifier.size(22.dp)
              )
            }
          } else {
            Icon(
              imageVector = destination.icon,
              contentDescription = destination.title,
              modifier = Modifier.size(22.dp)
            )
          }
        },
        label = {
          Text(
            text = destination.title,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = GrassNeon,
          selectedTextColor = GrassNeon,
          indicatorColor = DarkSoilVariant,
          unselectedIconColor = MutedLichen,
          unselectedTextColor = MutedLichen
        )
      )
    }
  }
}
