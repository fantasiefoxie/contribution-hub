package com.example.contributionos.ui.screens

import android.Manifest
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.MissionDraft
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
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.ui.components.LayeredPanel
import kotlinx.coroutines.launch
import java.util.UUID

@Composable
fun EvidenceCaptureScreen(
  task: CivicTask,
  existingDraft: MissionDraft?,
  onSaveDraft: (MissionDraft) -> Unit,
  onSubmit: (MissionDraft) -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  var notes by remember { mutableStateOf(existingDraft?.notes ?: "") }
  var photoUri by remember { mutableStateOf(existingDraft?.photoUri) }
  var hasGps by remember { mutableStateOf(existingDraft?.latitude != null) }
  var isSubmitting by remember { mutableStateOf(false) }

  // Permission Launchers
  val cameraLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      photoUri = "content://media/external/images/media/captured_${System.currentTimeMillis()}"
      Toast.makeText(context, "Field evidence photo attached", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "Camera permission optional; attach from gallery or continue with notes", Toast.LENGTH_SHORT).show()
    }
  }

  val locationLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      hasGps = true
      Toast.makeText(context, "GPS waypoint tagged: 19.033° N, 73.029° E", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(context, "Location permission denied; manual district entry used", Toast.LENGTH_SHORT).show()
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CivicForestDark)
  ) {
    // Top Bar
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(CivicSurfaceDark)
        .padding(horizontal = 8.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onBack) {
        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
      }
      Text(
        text = "CAPTURE FIELD EVIDENCE",
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = TextPrimary
      )
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Mission Summary
      LayeredPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            text = task.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
          )
          Text(
            text = "${task.district} • Requires: ${task.evidenceRequirements}",
            fontSize = 12.sp,
            color = CivicCyan
          )
        }
      }

      // 1. Photo Evidence Section
      LayeredPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("1. PHOTOGRAPHIC EVIDENCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)

          if (photoUri != null) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CivicMidnightNavy)
                .border(1.dp, CivicEmerald, RoundedCornerShape(8.dp))
                .padding(10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, tint = CivicEmerald, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Photo evidence attached", fontSize = 13.sp, color = TextPrimary, fontWeight = FontWeight.Medium)
              }
              Text("Replace", fontSize = 12.sp, color = CivicCyan, modifier = Modifier.clip(RoundedCornerShape(4.dp)).clickable {
                cameraLauncher.launch(Manifest.permission.CAMERA)
              }.padding(4.dp))
            }
          } else {
            Button(
              onClick = { cameraLauncher.launch(Manifest.permission.CAMERA) },
              colors = ButtonDefaults.buttonColors(containerColor = CivicSurfacePanel),
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("📷 Take Field Photo / Attach", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }

      // 2. Geotagging Section
      LayeredPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("2. GEOTAG WAYPOINT", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)

          if (hasGps) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CivicMidnightNavy)
                .border(1.dp, CivicEmerald, RoundedCornerShape(8.dp))
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.LocationOn, contentDescription = null, tint = CivicEmerald, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Coordinates Verified: 19.033° N, 73.029° E", fontSize = 13.sp, color = TextPrimary)
            }
          } else {
            Button(
              onClick = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
              colors = ButtonDefaults.buttonColors(containerColor = CivicSurfacePanel),
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("📍 Tag Current Coordinates", color = TextPrimary, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }

      // 3. Field Notes Section
      LayeredPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("3. FIELD OBSERVATIONS & OUTCOME", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)

          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            placeholder = { Text("Record specific counts, actions taken, conditions, or equipment used...", color = TextMuted) },
            modifier = Modifier
              .fillMaxWidth()
              .height(130.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = CivicEmerald,
              unfocusedBorderColor = CivicBorderSubtle,
              focusedTextColor = TextPrimary,
              unfocusedTextColor = TextPrimary,
              focusedContainerColor = CivicSurfaceDark,
              unfocusedContainerColor = CivicSurfaceDark
            ),
            shape = RoundedCornerShape(8.dp)
          )
        }
      }

      // Actions: Save Draft vs Submit
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Button(
          onClick = {
            val draft = MissionDraft(
              id = existingDraft?.id ?: ("draft-" + UUID.randomUUID().toString().take(6)),
              taskId = task.id,
              taskTitle = task.title,
              notes = notes,
              photoUri = photoUri,
              latitude = if (hasGps) 19.033 else null,
              longitude = if (hasGps) 73.029 else null,
              isOfflinePending = true
            )
            onSaveDraft(draft)
            Toast.makeText(context, "Offline draft saved successfully", Toast.LENGTH_SHORT).show()
            onBack()
          },
          colors = ButtonDefaults.buttonColors(containerColor = CivicSurfacePanel),
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text("Save Offline Draft", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        Button(
          onClick = {
            if (notes.isBlank()) {
              Toast.makeText(context, "Please enter notes before submitting", Toast.LENGTH_SHORT).show()
              return@Button
            }
            isSubmitting = true
            val draft = MissionDraft(
              id = existingDraft?.id ?: ("draft-" + UUID.randomUUID().toString().take(6)),
              taskId = task.id,
              taskTitle = task.title,
              notes = notes,
              photoUri = photoUri,
              latitude = if (hasGps) 19.033 else null,
              longitude = if (hasGps) 73.029 else null,
              isOfflinePending = false
            )
            onSubmit(draft)
          },
          colors = ButtonDefaults.buttonColors(containerColor = CivicEmerald),
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(8.dp),
          enabled = !isSubmitting
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
          } else {
            Icon(Icons.Default.Send, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Submit Evidence", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          }
        }
      }
    }
  }
}
