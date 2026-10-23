package com.example.contributionos.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.auth.AuthResult
import com.example.contributionos.auth.AuthSession
import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.DataSourceMode
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
import com.example.contributionos.theme.SoilVoid
import com.example.contributionos.theme.SunGold
import com.example.contributionos.theme.TextHighlight
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.theme.WaypointTeal
import com.example.contributionos.theme.WeatheredPaper
import com.example.contributionos.ui.components.CivicHeader
import com.example.contributionos.ui.components.LayeredPanel
import com.example.contributionos.ui.components.SegmentedProgressBar

@Composable
fun ProfileScreen(
  profile: CivicProfile,
  draftCount: Int,
  submissionCount: Int,
  isLive: Boolean,
  environmentMode: DataSourceMode = DataSourceMode.PRODUCTION,
  onSetEnvironmentMode: (DataSourceMode) -> Unit = {},
  authSession: AuthSession? = null,
  onSignIn: (String, String, (AuthResult<AuthSession>) -> Unit) -> Unit = { _, _, _ -> },
  onSignUp: (String, String, (AuthResult<AuthSession>) -> Unit) -> Unit = { _, _, _ -> },
  onSignOut: () -> Unit = {},
  onRetryPending: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scrollState = rememberScrollState()

  var authEmail by remember { mutableStateOf("") }
  var authPassword by remember { mutableStateOf("") }
  var authLoading by remember { mutableStateOf(false) }
  var authErrorMessage by remember { mutableStateOf<String?>(null) }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(SoilVoid)
  ) {
    CivicHeader(
      districtName = "Grass Toucher Dossier",
      seed = 42L,
      isLive = isLive,
      environmentMode = environmentMode
    )

    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(scrollState)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Profile Hero Card
      LayeredPanel(modifier = Modifier.fillMaxWidth(), borderColor = MutedMoss) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSoilVariant)
                .border(1.5.dp, GrassNeon, RoundedCornerShape(6.dp)),
              contentAlignment = Alignment.Center
            ) {
              Text(
                "L${profile.level}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                color = GrassNeon
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(profile.displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = WeatheredPaper)
              Text(
                "Callsign: ${profile.callsign}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = WaypointTeal
              )
              Text(
                profile.rankTitle,
                fontSize = 12.sp,
                fontFamily = FontFamily.Serif,
                color = TextMuted
              )
            }
          }

          Text(
            "🌱 Your neighbourhood has lore now.",
            fontSize = 12.sp,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.SemiBold,
            color = GrassNeon
          )

          // XP Segmented Progress Bar
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                "Exploration Level ${profile.level}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextMuted
              )
              Text(
                "${profile.explorationXp} / ${profile.nextLevelXp} Exploration XP",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = SunGold
              )
            }
            val xpProg = if (profile.nextLevelXp == 0) 0f else profile.explorationXp.toFloat() / profile.nextLevelXp.toFloat()
            SegmentedProgressBar(
              progressPercent = xpProg,
              totalSegments = 12,
              activeColor = SunGold,
              inactiveColor = DarkSoilVariant
            )
          }

          // Lifetime Stats
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            StatBox(label = "Verified Proof", value = "${profile.verifiedContributionsCount}")
            StatBox(label = "Civic Credits", value = "${profile.verifiedCivicCredits}")
            StatBox(label = "Expeditions", value = "${profile.completedExpeditionsCount}")
          }

          // Progression Metrics Guide
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(CivicSurfaceDark)
              .border(1.dp, CivicCyan.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
              .padding(10.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("PROGRESSION GUIDE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = CivicCyan)
              Text(
                "• Exploration XP & Levels: Personal character progression earned by discovering checkpoints and completing self-guided side quests.\n• Verified Civic Credits: Official community acknowledgments issued only after steward review of genuine civic contributions.",
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 15.sp
              )
            }
          }
        }
      }

      // AUTHENTICATION & SESSION MANAGEMENT CARD
      LayeredPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (authSession != null) GrassNeon else CivicAmber
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "TOUCH GRASS CLOUD REGISTRY",
              fontSize = 11.sp,
              fontWeight = FontWeight.ExtraBold,
              letterSpacing = 0.5.sp,
              color = if (authSession != null) GrassNeon else CivicAmber
            )
            Text(
              if (authSession != null) "AUTHENTICATED" else "SIGN IN REQUIRED",
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold,
              color = if (authSession != null) GrassNeon else CivicAmber
            )
          }

          if (authSession != null) {
            Text(
              "Signed in as: ${authSession.email}",
              fontSize = 13.sp,
              fontWeight = FontWeight.SemiBold,
              color = TextPrimary
            )
            Text(
              "Encrypted Storage: Local AES-256-GCM token storage active. Proof uploads signed and authenticated.",
              fontSize = 11.sp,
              color = TextSecondary
            )

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = {
                  onRetryPending()
                  Toast.makeText(context, "Offline submission sync initiated...", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CivicCyan),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Sync Offline Quests", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }

              Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = CivicSurfacePanel),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Sign Out", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }
          } else {
            Text(
              "Sign in to log verified real-world quests, upload photo proof, and earn permanent community recognition.",
              fontSize = 12.sp,
              color = TextSecondary
            )

            OutlinedTextField(
              value = authEmail,
              onValueChange = { authEmail = it },
              label = { Text("Explorer Email", color = TextMuted) },
              modifier = Modifier.fillMaxWidth(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrassNeon,
                unfocusedBorderColor = CivicBorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = CivicSurfaceDark,
                unfocusedContainerColor = CivicSurfaceDark
              ),
              shape = RoundedCornerShape(8.dp)
            )

            OutlinedTextField(
              value = authPassword,
              onValueChange = { authPassword = it },
              label = { Text("Password", color = TextMuted) },
              modifier = Modifier.fillMaxWidth(),
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = GrassNeon,
                unfocusedBorderColor = CivicBorderSubtle,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedContainerColor = CivicSurfaceDark,
                unfocusedContainerColor = CivicSurfaceDark
              ),
              shape = RoundedCornerShape(8.dp)
            )

            if (authErrorMessage != null) {
              Text(
                authErrorMessage ?: "",
                color = CivicAmber,
                fontSize = 11.sp,
                lineHeight = 15.sp
              )
            }

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              Button(
                onClick = {
                  if (authEmail.isBlank() || authPassword.isBlank()) {
                    authErrorMessage = "Please enter both email and password."
                    return@Button
                  }
                  authLoading = true
                  authErrorMessage = null
                  onSignIn(authEmail, authPassword) { result ->
                    authLoading = false
                    when (result) {
                      is AuthResult.Success -> {
                        Toast.makeText(context, "Welcome back, ${result.data.email}!", Toast.LENGTH_SHORT).show()
                      }
                      is AuthResult.Failure -> {
                        authErrorMessage = result.message
                      }
                    }
                  }
                },
                enabled = !authLoading,
                colors = ButtonDefaults.buttonColors(containerColor = GrassNeon),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
              ) {
                if (authLoading) {
                  CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                } else {
                  Text("Sign In", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
              }

              Button(
                onClick = {
                  if (authEmail.isBlank() || authPassword.isBlank()) {
                    authErrorMessage = "Please enter both email and password."
                    return@Button
                  }
                  authLoading = true
                  authErrorMessage = null
                  onSignUp(authEmail, authPassword) { result ->
                    authLoading = false
                    when (result) {
                      is AuthResult.Success -> {
                        Toast.makeText(context, "Account created: ${result.data.email}", Toast.LENGTH_SHORT).show()
                      }
                      is AuthResult.Failure -> {
                        authErrorMessage = result.message
                      }
                    }
                  }
                },
                enabled = !authLoading,
                colors = ButtonDefaults.buttonColors(containerColor = CivicSurfacePanel),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(8.dp)
              ) {
                Text("Register", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }
            }
          }
        }
      }

      // ENVIRONMENT MODE TOGGLE (LIVE GRASS vs ASTROTURF)
      LayeredPanel(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (environmentMode == DataSourceMode.SANDBOX_DEMO) CivicAmber else GrassNeon.copy(alpha = 0.5f)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            "DATA ENVIRONMENT (REAL GRASS vs ASTROTURF)",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            color = if (environmentMode == DataSourceMode.SANDBOX_DEMO) CivicAmber else GrassNeon
          )

          Text(
            if (environmentMode == DataSourceMode.PRODUCTION) {
              "CURRENT: LIVE GRASS (PRODUCTION)\nConnected to live Supabase backend. Displays authentic community tasks and verified on-chain contributions only."
            } else {
              "CURRENT: ASTROTURF (SANDBOX SIMULATION)\nUsing illustrative mock fixtures to test the roguelite route tree. Quests completed here do not touch real grass."
            },
            fontSize = 12.sp,
            color = TextSecondary,
            lineHeight = 17.sp
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { onSetEnvironmentMode(DataSourceMode.PRODUCTION) },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (environmentMode == DataSourceMode.PRODUCTION) GrassNeon else CivicSurfacePanel
              ),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                "Live Grass Mode",
                color = if (environmentMode == DataSourceMode.PRODUCTION) Color.Black else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }

            Button(
              onClick = { onSetEnvironmentMode(DataSourceMode.SANDBOX_DEMO) },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (environmentMode == DataSourceMode.SANDBOX_DEMO) CivicAmber else CivicSurfacePanel
              ),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(8.dp)
            ) {
              Text(
                "Astroturf (Sandbox)",
                color = if (environmentMode == DataSourceMode.SANDBOX_DEMO) Color.Black else TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
              )
            }
          }
        }
      }

      // Badges & Achievements
      Text("REAL-WORLD ACHIEVEMENTS & LORE", fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = TextMuted)

      profile.badges.chunked(2).forEach { rowBadges ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          rowBadges.forEach { badge ->
            Card(
              modifier = Modifier.weight(1f),
              colors = CardDefaults.cardColors(containerColor = ForestFloor),
              shape = RoundedCornerShape(8.dp),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (badge.isUnlocked) SunGold.copy(alpha = 0.6f) else MutedMoss
              )
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(badge.iconGlyph, fontSize = 24.sp)
                Text(badge.name, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (badge.isUnlocked) WeatheredPaper else TextMuted)
                Text(badge.description, fontSize = 11.sp, fontFamily = FontFamily.Serif, color = TextMuted)
                Text(
                  if (badge.isUnlocked) "UNLOCKED" else "LOCKED",
                  fontSize = 9.sp,
                  fontFamily = FontFamily.Monospace,
                  fontWeight = FontWeight.Bold,
                  color = if (badge.isUnlocked) SunGold else TextMuted
                )
              }
            }
          }
          if (rowBadges.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }

      // Offline Diagnostics & Integrity
      LayeredPanel(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text("LOCAL VAULT & PRIVACY LEDGER", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
          Text(
            "Encrypted Storage: $draftCount offline draft(s) • $submissionCount tracked quest(s)",
            fontSize = 12.sp,
            color = TextSecondary
          )
          Text(
            "Security: Android Keystore AES-256-GCM local encryption. No ad trackers, no idle location hoarding. Excluded from OS cloud backup via backup_rules.xml.",
            fontSize = 11.sp,
            color = CivicCyan
          )
        }
      }
    }
  }
}

@Composable
private fun StatBox(label: String, value: String) {
  Box(
    modifier = Modifier
      .clip(RoundedCornerShape(6.dp))
      .background(DarkSoilVariant)
      .border(1.dp, MutedMoss, RoundedCornerShape(6.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(value, fontSize = 15.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, color = GrassNeon)
      Text(label, fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = TextMuted)
    }
  }
}
