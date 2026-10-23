package com.example.contributionos.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.theme.CardBorder
import com.example.contributionos.theme.CardCream
import com.example.contributionos.theme.ClayAccent
import com.example.contributionos.theme.ForestInk
import com.example.contributionos.theme.LeafPillContainer
import com.example.contributionos.theme.MossGreenDark
import com.example.contributionos.theme.MutedSecondary
import com.example.contributionos.theme.WarmPaper

enum class AppDestination(val title: String, val icon: ImageVector) {
  HOME("Camp", Icons.Default.Home),
  MAP("World", Icons.Default.LocationOn),
  MISSIONS("Expeditions", Icons.Default.Menu),
  JOURNAL("Journal", Icons.Default.Star),
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
      .background(CardCream)
      .topBorder(1.dp, CardBorder),
    containerColor = CardCream,
    contentColor = ForestInk,
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
              Badge(containerColor = ClayAccent) {
                Text(
                  draftCount.toString(),
                  fontSize = 10.sp,
                  fontFamily = FontFamily.SansSerif,
                  fontWeight = FontWeight.Bold,
                  color = CardCream
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
            fontFamily = FontFamily.SansSerif,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
          )
        },
        colors = NavigationBarItemDefaults.colors(
          selectedIconColor = MossGreenDark,
          selectedTextColor = MossGreenDark,
          indicatorColor = LeafPillContainer,
          unselectedIconColor = MutedSecondary,
          unselectedTextColor = MutedSecondary
        )
      )
    }
  }
}
