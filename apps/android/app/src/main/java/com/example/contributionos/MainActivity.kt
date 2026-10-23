package com.example.contributionos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.contributionos.theme.ContributionOSTheme

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    try {
      org.maplibre.android.MapLibre.getInstance(this)
    } catch (e: Exception) {
      android.util.Log.w("MainActivity", "MapLibre initialization note: ${e.message}")
    }

    enableEdgeToEdge()
    setContent {
      ContributionOSTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
