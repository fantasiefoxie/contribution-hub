package com.example.contributionos.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
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
import java.io.File
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
  var photoUriString by remember { mutableStateOf(existingDraft?.photoUri) }
  var currentLatitude by remember { mutableStateOf(existingDraft?.latitude) }
  var currentLongitude by remember { mutableStateOf(existingDraft?.longitude) }
  var isLocating by remember { mutableStateOf(false) }
  var isSubmitting by remember { mutableStateOf(false) }

  // Pending camera target file URI
  var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

  val takePictureLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.TakePicture()
  ) { success ->
    if (success && tempCameraUri != null) {
      photoUriString = tempCameraUri.toString()
      Toast.makeText(context, "Field evidence photo captured and attached", Toast.LENGTH_SHORT).show()
    }
  }

  val pickImageLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      photoUriString = uri.toString()
      Toast.makeText(context, "Field evidence image attached from gallery", Toast.LENGTH_SHORT).show()
    }
  }

  val cameraPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    if (isGranted) {
      try {
        val evidenceDir = File(context.cacheDir, "evidence")
        if (!evidenceDir.exists()) evidenceDir.mkdirs()
        val photoFile = File(evidenceDir, "evidence_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
        tempCameraUri = uri
        takePictureLauncher.launch(uri)
      } catch (e: Exception) {
        Toast.makeText(context, "Error opening camera storage: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    } else {
      Toast.makeText(context, "Camera permission denied. You can select an image from gallery.", Toast.LENGTH_LONG).show()
    }
  }

  fun requestRealLocation() {
    val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    if (!fineGranted && !coarseGranted) return

    isLocating = true
    try {
      val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
      if (locationManager == null) {
        Toast.makeText(context, "Location service unavailable on this device", Toast.LENGTH_SHORT).show()
        isLocating = false
        return
      }

      val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
      val isNetEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

      if (!isGpsEnabled && !isNetEnabled) {
        Toast.makeText(context, "GPS/Location is turned off in device settings", Toast.LENGTH_LONG).show()
        isLocating = false
        return
      }

      var bestLocation: Location? = null
      if (isGpsEnabled) {
        bestLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
      }
      if (bestLocation == null && isNetEnabled) {
        bestLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
      }

      if (bestLocation != null) {
        currentLatitude = bestLocation.latitude
        currentLongitude = bestLocation.longitude
        Toast.makeText(context, "Waypoint acquired: %.4f° N, %.4f° E".format(bestLocation.latitude, bestLocation.longitude), Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, "Acquiring GPS fix... Keep device near open sky or use manual notes.", Toast.LENGTH_LONG).show()
      }
    } catch (e: Exception) {
      Toast.makeText(context, "Location reading error: ${e.message}", Toast.LENGTH_SHORT).show()
    } finally {
      isLocating = false
    }
  }

  val locationPermissionLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.RequestMultiplePermissions()
  ) { permissions ->
    val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
    val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
    if (fineGranted || coarseGranted) {
      requestRealLocation()
    } else {
      Toast.makeText(context, "Location permission declined. Geotag will be omitted.", Toast.LENGTH_SHORT).show()
    }
  }

  // Load preview bitmap safely
  val previewBitmap = remember(photoUriString) {
    photoUriString?.let { uriStr ->
      try {
        val uri = Uri.parse(uriStr)
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream)
        }
      } catch (e: Exception) {
        null
      }
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
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("1. PHOTOGRAPHIC EVIDENCE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            if (photoUriString != null) {
              Text(
                "Remove",
                fontSize = 11.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .clickable { photoUriString = null }
                  .padding(4.dp)
              )
            }
          }

          if (previewBitmap != null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, CivicEmerald, RoundedCornerShape(8.dp))
            ) {
              Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Attached Field Evidence",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )
              Box(
                modifier = Modifier
                  .align(Alignment.BottomStart)
                  .background(CivicSurfaceDark.copy(alpha = 0.85f))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text("Field Evidence Attached", fontSize = 11.sp, color = CivicEmerald, fontWeight = FontWeight.Bold)
              }
            }
          } else if (photoUriString != null) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(CivicMidnightNavy)
                .border(1.dp, CivicEmerald, RoundedCornerShape(8.dp))
                .padding(10.dp),
              verticalAlignment = Alignment.CenterVertically
            ) {
              Icon(Icons.Default.Check, contentDescription = null, tint = CivicEmerald, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text("Evidence file attached", fontSize = 13.sp, color = TextPrimary)
            }
          }

          // Camera & Gallery action buttons
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                val hasCam = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                if (hasCam) {
                  val evidenceDir = File(context.cacheDir, "evidence")
                  if (!evidenceDir.exists()) evidenceDir.mkdirs()
                  val photoFile = File(evidenceDir, "evidence_${System.currentTimeMillis()}.jpg")
                  val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", photoFile)
                  tempCameraUri = uri
                  takePictureLauncher.launch(uri)
                } else {
                  cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = CivicSurfacePanel),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("📷 Take Photo", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }

            Button(
              onClick = { pickImageLauncher.launch("image/*") },
              colors = ButtonDefaults.buttonColors(containerColor = CivicSurfacePanel),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text("🖼 From Gallery", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
            }
          }
        }
      }

      // 2. Geotagging Section
      LayeredPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("2. GEOTAG WAYPOINT (OPTIONAL)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            if (currentLatitude != null && currentLongitude != null) {
              Text(
                "Clear",
                fontSize = 11.sp,
                color = Color(0xFFEF4444),
                modifier = Modifier
                  .clip(RoundedCornerShape(4.dp))
                  .clickable {
                    currentLatitude = null
                    currentLongitude = null
                  }
                  .padding(4.dp)
              )
            }
          }

          if (currentLatitude != null && currentLongitude != null) {
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
              Text(
                "Verified GPS: %.4f°, %.4f°".format(currentLatitude, currentLongitude),
                fontSize = 13.sp,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
              )
            }
          } else {
            Button(
              onClick = {
                val fineGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                val coarseGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

                if (fineGranted || coarseGranted) {
                  requestRealLocation()
                } else {
                  locationPermissionLauncher.launch(
                    arrayOf(
                      Manifest.permission.ACCESS_FINE_LOCATION,
                      Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                  )
                }
              },
              colors = ButtonDefaults.buttonColors(containerColor = CivicSurfacePanel),
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(8.dp),
              enabled = !isLocating
            ) {
              if (isLocating) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = CivicCyan)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Acquiring GPS...", color = TextPrimary)
              } else {
                Text("📍 Capture Real Coordinates", color = TextPrimary, fontWeight = FontWeight.SemiBold)
              }
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
              photoUri = photoUriString,
              latitude = currentLatitude,
              longitude = currentLongitude,
              isOfflinePending = true,
              isSandboxDemo = task.isSandboxDemo
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
              photoUri = photoUriString,
              latitude = currentLatitude,
              longitude = currentLongitude,
              isOfflinePending = false,
              isSandboxDemo = task.isSandboxDemo
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
