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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.MissionDraft
import com.example.contributionos.theme.CivicAmber
import com.example.contributionos.theme.CivicCyan
import com.example.contributionos.theme.DarkSoilVariant
import com.example.contributionos.theme.ForestFloor
import com.example.contributionos.theme.GrassNeon
import com.example.contributionos.theme.MutedLichen
import com.example.contributionos.theme.MutedMoss
import com.example.contributionos.theme.SoilSurfacePanel
import com.example.contributionos.theme.SoilVoid
import com.example.contributionos.theme.SunGold
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.theme.WaypointTeal
import com.example.contributionos.theme.WeatheredPaper
import com.example.contributionos.ui.components.TactileCard
import com.example.contributionos.ui.components.bottomBorder
import java.io.File
import java.util.UUID

@Composable
fun EvidenceCaptureScreen(
  task: CivicTask,
  existingDraft: MissionDraft?,
  onSaveDraft: (MissionDraft) -> Unit,
  onSubmit: (MissionDraft) -> Unit,
  onBack: () -> Unit,
  isAuthenticated: Boolean = false,
  isProduction: Boolean = true,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val focusManager = LocalFocusManager.current
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
        Toast.makeText(context, "Verified GPS waypoint acquired", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, "No recent GPS lock. Head outdoors or wait for satellite fix.", Toast.LENGTH_LONG).show()
      }
    } catch (e: SecurityException) {
      Toast.makeText(context, "GPS permission error: ${e.message}", Toast.LENGTH_SHORT).show()
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
      Toast.makeText(context, "Location permission not granted. Geotagging remains optional.", Toast.LENGTH_LONG).show()
    }
  }

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
      .background(SoilVoid)
  ) {
    // Top Bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(ForestFloor)
        .bottomBorder(1.dp, MutedMoss)
        .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.clickable { onBack() }
        ) {
          Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = WeatheredPaper, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(GrassNeon)
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                "SPECIMEN LOGGING",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = GrassNeon
              )
            }
            Text(
              "// ${formatDossierId(task.id)} RECONNAISSANCE",
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              color = TextMuted
            )
          }
        }

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(3.dp))
            .background(DarkSoilVariant)
            .border(1.dp, MutedMoss, RoundedCornerShape(3.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Text(
            "SEED #4092",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = SunGold
          )
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .imePadding()
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
      // 1. QUEST SPECIMEN HEADER CARD
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MutedMoss,
        backgroundColor = ForestFloor
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(DarkSoilVariant)
                .border(0.75.dp, MutedMoss, RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
              Text(
                "INDEX // [${formatDossierId(task.id)}]",
                fontSize = 9.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = SunGold
              )
            }
            Text(
              "Tier ${task.difficulty} Scout",
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              color = GrassNeon
            )
          }

          Text(
            text = task.title,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = WeatheredPaper
          )

          Text(
            text = "DISTRICT: ${task.district.uppercase()} // ${task.locationLabel.uppercase()}",
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            color = TextSecondary
          )

          // Reward Telemetry Tray
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(6.dp))
              .background(SoilVoid)
              .border(1.dp, MutedMoss, RoundedCornerShape(6.dp))
              .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "+${task.xpReward} Exploration XP",
              fontSize = 10.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = GrassNeon
            )
            Text(
              "0 Civic Credits",
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              color = TextMuted
            )
            Text(
              "STEP 2/3",
              fontSize = 9.sp,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              color = SunGold
            )
          }
        }
      }

      // Existing draft notice if resuming
      if (existingDraft != null) {
        TactileCard(
          modifier = Modifier.fillMaxWidth(),
          borderColor = SunGold.copy(alpha = 0.5f),
          backgroundColor = DarkSoilVariant
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Info, contentDescription = null, tint = SunGold, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
              Text(
                "EDITING EXISTING OFFLINE DRAFT",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = SunGold
              )
              Text(
                "Saving updates this draft in encrypted storage without duplicating records.",
                fontSize = 10.sp,
                fontFamily = FontFamily.Serif,
                color = TextSecondary
              )
            }
          }
        }
      }

      // 2. SPECIMEN PHOTO BOX (Viewfinder & Specimen Capture)
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MutedMoss,
        backgroundColor = ForestFloor
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("📷", fontSize = 14.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "Specimen Photographic Proof",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = WeatheredPaper
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(DarkSoilVariant)
                .border(0.75.dp, MutedMoss, RoundedCornerShape(3.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
              Text(
                "OPTIONAL",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
              )
            }
          }

          if (previewBitmap != null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, GrassNeon, RoundedCornerShape(6.dp))
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
                  .background(SoilVoid.copy(alpha = 0.85f))
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text("Specimen Photo Attached", fontSize = 10.sp, color = GrassNeon, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
              }
              IconButton(
                onClick = { photoUriString = null },
                modifier = Modifier.align(Alignment.TopEnd)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Red)
              }
            }
          } else {
            // Viewfinder Inset Well
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(SoilVoid)
                .border(1.dp, MutedMoss, RoundedCornerShape(6.dp)),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text("🎯", fontSize = 22.sp)
                Text(
                  "Viewfinder Standby • Optional",
                  fontSize = 11.sp,
                  fontFamily = FontFamily.Monospace,
                  color = TextMuted
                )
                Text(
                  "Field notes alone are 100% sufficient to complete this quest.",
                  fontSize = 10.sp,
                  fontFamily = FontFamily.Serif,
                  color = TextSecondary
                )
              }
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
              colors = ButtonDefaults.buttonColors(containerColor = DarkSoilVariant),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text("📷 Take Photo", color = WeatheredPaper, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }

            Button(
              onClick = { pickImageLauncher.launch("image/*") },
              colors = ButtonDefaults.buttonColors(containerColor = DarkSoilVariant),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Text("🖼 Gallery", color = WeatheredPaper, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
          }
        }
      }

      // 3. Geotagging Section (Optional)
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MutedMoss,
        backgroundColor = ForestFloor
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("📍", fontSize = 14.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text("Geotag Waypoint", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = WeatheredPaper)
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(3.dp))
                .background(DarkSoilVariant)
                .border(0.75.dp, MutedMoss, RoundedCornerShape(3.dp))
                .padding(horizontal = 5.dp, vertical = 1.dp)
            ) {
              Text("OPTIONAL", fontSize = 8.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
            }
          }

          if (currentLatitude != null && currentLongitude != null) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(SoilVoid)
                .border(1.dp, GrassNeon, RoundedCornerShape(6.dp))
                .padding(8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                "GPS: %.4f°, %.4f°".format(currentLatitude, currentLongitude),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = GrassNeon,
                fontWeight = FontWeight.Bold
              )
              Text(
                "Clear",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFFF6B6B),
                modifier = Modifier
                  .clickable {
                    currentLatitude = null
                    currentLongitude = null
                  }
                  .padding(4.dp)
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
              colors = ButtonDefaults.buttonColors(containerColor = DarkSoilVariant),
              modifier = Modifier.fillMaxWidth(),
              shape = RoundedCornerShape(6.dp),
              enabled = !isLocating
            ) {
              if (isLocating) {
                CircularProgressIndicator(modifier = Modifier.size(14.dp), color = GrassNeon)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Acquiring GPS...", color = WeatheredPaper, fontSize = 11.sp)
              } else {
                Text("📍 Capture Real Coordinates (Single-Tap)", color = WeatheredPaper, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 11.sp)
              }
            }
          }
        }
      }

      // 4. Field Notes Section
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MutedMoss,
        backgroundColor = ForestFloor
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("FIELD OBSERVATIONS & SPECIMEN LOG", fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = WeatheredPaper)
            Text("REQUIRED", fontSize = 8.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = GrassNeon)
          }

          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            placeholder = { Text("Record specimen color, weather, leaf density, or personal reflections...", fontSize = 12.sp, color = TextMuted) },
            singleLine = false,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            modifier = Modifier
              .fillMaxWidth()
              .height(120.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = GrassNeon,
              unfocusedBorderColor = MutedMoss,
              focusedTextColor = WeatheredPaper,
              unfocusedTextColor = WeatheredPaper,
              focusedContainerColor = SoilVoid,
              unfocusedContainerColor = SoilVoid
            ),
            shape = RoundedCornerShape(6.dp)
          )
        }
      }

      // Vault Assurance Tag
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(6.dp))
          .background(DarkSoilVariant)
          .border(0.75.dp, MutedMoss, RoundedCornerShape(6.dp))
          .padding(horizontal = 10.dp, vertical = 6.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Lock, contentDescription = null, tint = WaypointTeal, modifier = Modifier.size(13.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            "Encrypted Locally (AES-256-GCM) • Zero telemetry leaves device without consent",
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = WaypointTeal
          )
        }
      }

      // Actions: Save Draft vs Complete
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 16.dp),
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
            Toast.makeText(context, "Offline draft saved in encrypted storage", Toast.LENGTH_SHORT).show()
            onBack()
          },
          colors = ButtonDefaults.buttonColors(containerColor = SoilSurfacePanel),
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(8.dp)
        ) {
          Text(
            "Save Draft",
            color = WeatheredPaper,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
          )
        }

        Button(
          onClick = {
            if (notes.isBlank()) {
              Toast.makeText(context, "Please enter notes before completing", Toast.LENGTH_SHORT).show()
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
          colors = ButtonDefaults.buttonColors(
            containerColor = GrassNeon,
            contentColor = SoilVoid
          ),
          modifier = Modifier.weight(1f),
          shape = RoundedCornerShape(8.dp),
          enabled = !isSubmitting
        ) {
          if (isSubmitting) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = SoilVoid)
          } else {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text(
                "Complete Quest",
                color = SoilVoid,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
              Spacer(modifier = Modifier.width(4.dp))
              Icon(Icons.Default.ArrowForward, contentDescription = null, tint = SoilVoid, modifier = Modifier.size(14.dp))
            }
          }
        }
      }
    }
  }
}
