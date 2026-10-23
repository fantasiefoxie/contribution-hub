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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
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
  var consentToCivicSubmission by remember { mutableStateOf(existingDraft?.consentToCivicSubmission ?: false) }
  var isLocating by remember { mutableStateOf(false) }
  var isSubmitting by remember { mutableStateOf(false) }

  var tempCameraUri by remember { mutableStateOf<Uri?>(null) }

  val takePictureLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.TakePicture()
  ) { success ->
    if (success && tempCameraUri != null) {
      photoUriString = tempCameraUri.toString()
      Toast.makeText(context, "Specimen photo attached", Toast.LENGTH_SHORT).show()
    }
  }

  val pickImageLauncher = rememberLauncherForActivityResult(
    ActivityResultContracts.GetContent()
  ) { uri ->
    if (uri != null) {
      photoUriString = uri.toString()
      Toast.makeText(context, "Specimen photo selected from gallery", Toast.LENGTH_SHORT).show()
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
        Toast.makeText(context, "Error opening camera: ${e.message}", Toast.LENGTH_SHORT).show()
      }
    } else {
      Toast.makeText(context, "Camera permission denied. You can select an image from your gallery.", Toast.LENGTH_LONG).show()
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
        Toast.makeText(context, "GPS is turned off in settings", Toast.LENGTH_LONG).show()
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
        Toast.makeText(context, "Field coordinates recorded", Toast.LENGTH_SHORT).show()
      } else {
        Toast.makeText(context, "Searching for satellite lock. Geotagging is optional.", Toast.LENGTH_LONG).show()
      }
    } catch (e: SecurityException) {
      Toast.makeText(context, "Location error: ${e.message}", Toast.LENGTH_SHORT).show()
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
      .background(WarmPaper)
  ) {
    // Top Bar
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .background(CardCream)
        .bottomBorder(1.dp, CardBorder)
        .padding(horizontal = 16.dp, vertical = 12.dp)
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
          Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = ForestInk, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            "Cancel",
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            color = ForestInk
          )
        }

        Text(
          "Field Notebook Entry",
          fontSize = 14.sp,
          fontFamily = FontFamily.Serif,
          fontWeight = FontWeight.Bold,
          color = ForestInkDark
        )

        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(LeafPillContainer)
            .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
          Text(
            "+${task.xpReward} XP",
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            color = MossGreenDark
          )
        }
      }
    }

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .imePadding()
        .padding(horizontal = 16.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // 1. QUEST CONTEXT CARD
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CardBorder,
        backgroundColor = CardCream,
        shape = RoundedCornerShape(16.dp),
        padding = 16.dp
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            "Logging for Adventure",
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            color = MutedSecondary
          )
          Text(
            text = task.title,
            fontSize = 17.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            color = ForestInkDark
          )
          Text(
            text = "District: ${task.district.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() }}",
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            color = MutedSecondary
          )
        }
      }

      // 2. SPECIMEN PHOTO BOX (OPTIONAL)
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CardBorder,
        backgroundColor = CardCream,
        shape = RoundedCornerShape(16.dp),
        padding = 16.dp
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("📷", fontSize = 16.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "Specimen Photo",
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ForestInkDark
              )
            }
            Text(
              "Optional",
              fontSize = 11.sp,
              fontFamily = FontFamily.SansSerif,
              color = MutedSecondary
            )
          }

          if (previewBitmap != null) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
            ) {
              Image(
                bitmap = previewBitmap.asImageBitmap(),
                contentDescription = "Specimen Photo",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
              )
              IconButton(
                onClick = { photoUriString = null },
                modifier = Modifier
                  .align(Alignment.TopEnd)
                  .padding(4.dp)
                  .background(CardCream.copy(alpha = 0.85f), CircleShape)
              ) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = ForestInk, modifier = Modifier.size(18.dp))
              }
            }
          } else {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(CardCreamLow)
                .border(1.dp, CardBorderSubtle, RoundedCornerShape(12.dp))
                .padding(16.dp),
              contentAlignment = Alignment.Center
            ) {
              Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text("🌿", fontSize = 24.sp)
                Text(
                  "Snap a close-up of leaf texture, stem, or canopy",
                  fontSize = 12.sp,
                  fontFamily = FontFamily.SansSerif,
                  color = MutedSecondary
                )
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
              },
              colors = ButtonDefaults.buttonColors(containerColor = LeafPillContainer, contentColor = MossGreenDark),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("Take Photo", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }

            Button(
              onClick = {
                pickImageLauncher.launch("image/*")
              },
              colors = ButtonDefaults.buttonColors(containerColor = CardCreamLow, contentColor = ForestInk),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp)
            ) {
              Text("From Gallery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
          }
        }
      }

      // 3. LOCATION CONSENT & GEOTAGGING (OPTIONAL)
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CardBorder,
        backgroundColor = CardCream,
        shape = RoundedCornerShape(16.dp),
        padding = 16.dp
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("📍", fontSize = 16.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "Field Coordinates",
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ForestInkDark
              )
            }
            Text(
              if (currentLatitude != null) "Recorded" else "Optional",
              fontSize = 11.sp,
              fontFamily = FontFamily.SansSerif,
              fontWeight = FontWeight.SemiBold,
              color = if (currentLatitude != null) MossGreenDark else MutedSecondary
            )
          }

          Text(
            "GPS coordinates are stored only on your local device to verify exploration territory. Background tracking is never used.",
            fontSize = 12.sp,
            fontFamily = FontFamily.SansSerif,
            color = MutedSecondary,
            lineHeight = 16.sp
          )

          Button(
            onClick = {
              locationPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
              )
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = if (currentLatitude != null) LeafPillContainer else CardCreamLow,
              contentColor = if (currentLatitude != null) MossGreenDark else ForestInk
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = if (isLocating) "Acquiring satellites..." else if (currentLatitude != null) "Coordinates Attached" else "Acquire GPS Coordinates",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }

      // 4. FIELD OBSERVATIONS NOTE (REQUIRED)
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CardBorder,
        backgroundColor = CardCream,
        shape = RoundedCornerShape(16.dp),
        padding = 16.dp
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("📝", fontSize = 16.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "Field Observations",
                fontSize = 14.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ForestInkDark
              )
            }
            Text(
              "${notes.length} characters (10+ required)",
              fontSize = 11.sp,
              fontFamily = FontFamily.SansSerif,
              color = if (notes.length >= 10) MossGreenDark else MutedSecondary
            )
          }

          OutlinedTextField(
            value = notes,
            onValueChange = { notes = it },
            placeholder = {
              Text(
                "What did you notice today? Describe colors, scents, temperature, moss softness, passing breeze, or tree canopy...",
                fontSize = 13.sp,
                fontFamily = FontFamily.SansSerif,
                color = MutedSecondary
              )
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(130.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = CardCreamLow,
              unfocusedContainerColor = CardCreamLow,
              focusedBorderColor = MossGreen,
              unfocusedBorderColor = CardBorderSubtle,
              focusedTextColor = ForestInkDark,
              unfocusedTextColor = ForestInkDark
            ),
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
          )

          // Offline vault assurance
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text("🔒", fontSize = 12.sp)
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              "Encrypted locally on this device (AES-256-GCM). Works 100% offline.",
              fontSize = 11.sp,
              fontFamily = FontFamily.SansSerif,
              color = MutedSecondary
            )
          }

          // Civic Integrity & Separation Notice
          if (task.isLocalCompletionOnly) {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CardCreamLow)
                .border(1.dp, CardBorderSubtle, RoundedCornerShape(10.dp))
                .padding(10.dp)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🌱", fontSize = 14.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Personal Exploration Proof: Stored in your local Field Journal. Does not create a public civic docket.",
                  fontSize = 11.sp,
                  fontFamily = FontFamily.SansSerif,
                  color = ForestInk,
                  lineHeight = 15.sp
                )
              }
            }
          } else {
            // Civic-eligible task: Explicit, un-checked by default opt-in toggle
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (consentToCivicSubmission) WaterBlueContainer.copy(alpha = 0.25f) else CardCreamLow)
                .border(1.dp, if (consentToCivicSubmission) WaterBlue.copy(alpha = 0.5f) else CardBorderSubtle, RoundedCornerShape(12.dp))
                .clickable { consentToCivicSubmission = !consentToCivicSubmission }
                .padding(12.dp)
            ) {
              Row(verticalAlignment = Alignment.Top) {
                Checkbox(
                  checked = consentToCivicSubmission,
                  onCheckedChange = { consentToCivicSubmission = it },
                  colors = CheckboxDefaults.colors(
                    checkedColor = MossGreenDark,
                    uncheckedColor = MutedSecondary
                  )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏛️", fontSize = 13.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                      text = "Submit for Municipal Review & Civic Credits",
                      fontSize = 12.sp,
                      fontFamily = FontFamily.SansSerif,
                      fontWeight = FontWeight.Bold,
                      color = if (consentToCivicSubmission) ForestInkDark else ForestInk
                    )
                  }
                  Spacer(modifier = Modifier.height(2.dp))
                  Text(
                    text = if (consentToCivicSubmission) {
                      "Explicit consent active: Photos and notes will be submitted to municipal forestry registry for arborist audit and Civic Credit accreditation."
                    } else {
                      "Unchecked (Private): Personal photos and notes stay on this device only. You earn exploration XP without creating a public civic record."
                    },
                    fontSize = 11.sp,
                    fontFamily = FontFamily.SansSerif,
                    color = if (consentToCivicSubmission) ForestInk else MutedSecondary,
                    lineHeight = 15.sp
                  )
                }
              }
            }
          }
        }
      }

      // 5. ACTION BUTTONS
      Column(
        modifier = Modifier.padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        val canComplete = notes.length >= 10

        Button(
          onClick = {
            if (!canComplete) {
              Toast.makeText(context, "Please write at least 10 characters in your field note.", Toast.LENGTH_SHORT).show()
              return@Button
            }
            isSubmitting = true
            val draft = existingDraft?.copy(
              notes = notes,
              photoUri = photoUriString,
              latitude = currentLatitude,
              longitude = currentLongitude,
              consentToCivicSubmission = consentToCivicSubmission
            ) ?: MissionDraft(
              id = UUID.randomUUID().toString(),
              taskId = task.id,
              taskTitle = task.title,
              notes = notes,
              photoUri = photoUriString,
              latitude = currentLatitude,
              longitude = currentLongitude,
              timestamp = System.currentTimeMillis(),
              consentToCivicSubmission = consentToCivicSubmission
            )
            onSubmit(draft)
          },
          enabled = canComplete && !isSubmitting,
          colors = ButtonDefaults.buttonColors(
            containerColor = MossGreenDark,
            contentColor = Color.White,
            disabledContainerColor = CardBorderSubtle,
            disabledContentColor = MutedSecondary
          ),
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
        ) {
          Text(
            if (isSubmitting) "Logging Observation..."
            else if (!task.isLocalCompletionOnly && consentToCivicSubmission) "Submit for Civic Review & Claim XP 🏛️"
            else "Save to Field Journal & Harvest XP 🌿",
            fontSize = 14.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold
          )
        }

        OutlinedButton(
          onClick = {
            val draft = existingDraft?.copy(
              notes = notes,
              photoUri = photoUriString,
              latitude = currentLatitude,
              longitude = currentLongitude,
              consentToCivicSubmission = consentToCivicSubmission
            ) ?: MissionDraft(
              id = UUID.randomUUID().toString(),
              taskId = task.id,
              taskTitle = task.title,
              notes = notes,
              photoUri = photoUriString,
              latitude = currentLatitude,
              longitude = currentLongitude,
              timestamp = System.currentTimeMillis(),
              consentToCivicSubmission = consentToCivicSubmission
            )
            onSaveDraft(draft)
            Toast.makeText(context, "Field draft saved offline", Toast.LENGTH_SHORT).show()
            onBack()
          },
          shape = RoundedCornerShape(12.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
          modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
        ) {
          Text(
            "Save Draft for Later",
            fontSize = 13.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.SemiBold,
            color = ForestInk
          )
        }
      }
    }
  }
}
