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
import com.example.contributionos.theme.StatusAlertRed
import com.example.contributionos.theme.SunGold
import com.example.contributionos.theme.SunGoldContainer
import com.example.contributionos.theme.SunGoldText
import com.example.contributionos.theme.TerracottaContainer
import com.example.contributionos.theme.TerracottaText
import com.example.contributionos.theme.WarmPaper
import com.example.contributionos.theme.WaterBlue
import com.example.contributionos.theme.WaterBlueContainer
import com.example.contributionos.theme.WaterBlueText
import com.example.contributionos.ui.components.BotanicalProgressBar
import com.example.contributionos.ui.components.CivicHeader
import com.example.contributionos.ui.components.TactileCard

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
  onViewImpact: () -> Unit = {},
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
      .background(WarmPaper)
  ) {
    CivicHeader(
      districtName = "Adventurer Profile",
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
      // Explorer Identity & Level Card
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CardBorder,
        backgroundColor = CardCream
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(LeafPillContainer)
                .border(1.5.dp, MossGreen.copy(alpha = 0.4f), CircleShape),
              contentAlignment = Alignment.Center
            ) {
              Text(
                "L${profile.level}",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                fontFamily = FontFamily.Serif,
                color = LeafPillText
              )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
              Text(
                profile.displayName,
                fontSize = 20.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ForestInkDark
              )
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  "🌱 ${profile.callsign}",
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium,
                  color = MossGreenDark
                )
                Text("•", fontSize = 12.sp, color = MutedSecondary)
                Text(
                  profile.rankTitle,
                  fontSize = 12.sp,
                  color = MutedSecondary
                )
              }
            }
          }

          Text(
            "🌱 Your neighbourhood has lore now. Every step outside uncovers local living history.",
            fontSize = 13.sp,
            color = ForestInk,
            lineHeight = 18.sp
          )

          // XP Progress Bar
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                "Exploration Level ${profile.level}",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = MutedSecondary
              )
              Text(
                "${profile.explorationXp} / ${profile.nextLevelXp} XP",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MossGreenDark
              )
            }
            val xpProg = if (profile.nextLevelXp == 0) 0f else profile.explorationXp.toFloat() / profile.nextLevelXp.toFloat()
            BotanicalProgressBar(
              progressPercent = xpProg,
              activeColor = MossGreen,
              trackColor = CardBorderSubtle,
              height = 8.dp
            )
          }

          // Lifetime Stats
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            StatBox(
              modifier = Modifier.weight(1f),
              label = "Verified Proof",
              value = "${profile.verifiedContributionsCount}",
              accentColor = MossGreenDark
            )
            StatBox(
              modifier = Modifier.weight(1f),
              label = "Civic Credits",
              value = "${profile.verifiedCivicCredits}",
              accentColor = WaterBlue
            )
            StatBox(
              modifier = Modifier.weight(1f),
              label = "Expeditions",
              value = "${profile.completedExpeditionsCount}",
              accentColor = ClayAccent
            )
          }

          // Civic Impact Ledger Shortcut
          Button(
            onClick = onViewImpact,
            colors = ButtonDefaults.buttonColors(
              containerColor = CardCreamLow,
              contentColor = ForestInk
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
              .fillMaxWidth()
              .height(40.dp)
          ) {
            Text(
              text = "View Public Civic Ledger & Milestones →",
              fontSize = 12.sp,
              fontFamily = FontFamily.SansSerif,
              fontWeight = FontWeight.SemiBold
            )
          }

          // Progression Guide Card (Warm and honest)
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(SoftMeadowSand)
              .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
              .padding(12.dp)
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text(
                "PROGRESSION & CIVIC TRUST",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = MossGreenDark
              )
              Text(
                "• Exploration XP & Levels: Personal character progression earned by discovering checkpoints and completing self-guided side quests.\n• Verified Civic Credits: Official community acknowledgments issued only after steward review of genuine civic contributions.",
                fontSize = 11.sp,
                color = ForestInk,
                lineHeight = 16.sp
              )
            }
          }
        }
      }

      // FIELD STAMPS CABINET (Collectible Waypoint Seals)
      val stamps = listOf(
        Triple("wp-banyan-canopy", "🌳", "Banyan Canopy"),
        Triple("wp-bioswale-basin", "💧", "Bioswale Basin"),
        Triple("wp-transit-solar", "☀️", "Transit Solar"),
        Triple("wp-pedestrian-ramp", "🚶", "Pedestrian Ramp")
      )
      val collectedStampsCount = stamps.count { profile.rewardedWaypointIds.contains(it.first) }

      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = MossGreen.copy(alpha = 0.35f),
        backgroundColor = CardCream
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Text("🏷️", fontSize = 16.sp)
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                "Field Stamps Cabinet",
                fontSize = 15.sp,
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                color = ForestInkDark
              )
            }
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(LeafPillContainer)
                .padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
              Text(
                "$collectedStampsCount / ${stamps.size} Collected",
                fontSize = 11.sp,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Bold,
                color = LeafPillText
              )
            }
          }

          stamps.chunked(2).forEach { rowStamps ->
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              rowStamps.forEach { (stampId, icon, name) ->
                val isUnlocked = profile.rewardedWaypointIds.contains(stampId)
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isUnlocked) CardCreamLow else SoftMeadowSand)
                    .border(
                      1.dp,
                      if (isUnlocked) MossGreen.copy(alpha = 0.4f) else CardBorderSubtle,
                      RoundedCornerShape(12.dp)
                    )
                    .padding(10.dp)
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                      modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (isUnlocked) LeafPillContainer else CardCream)
                        .border(1.dp, if (isUnlocked) MossGreen else CardBorderSubtle, CircleShape),
                      contentAlignment = Alignment.Center
                    ) {
                      Text(icon, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                      Text(
                        name,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = if (isUnlocked) ForestInkDark else MutedSecondary
                      )
                      Text(
                        if (isUnlocked) "Collected" else "Undiscovered",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.SansSerif,
                        color = if (isUnlocked) MossGreenDark else MutedSecondary
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }

      // Badges & Achievements
      Text(
        "Real-World Achievements & Lore",
        fontSize = 16.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        color = ForestInkDark
      )

      profile.badges.chunked(2).forEach { rowBadges ->
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          rowBadges.forEach { badge ->
            Card(
              modifier = Modifier.weight(1f),
              colors = CardDefaults.cardColors(
                containerColor = if (badge.isUnlocked) CardCream else SoftMeadowSand
              ),
              shape = RoundedCornerShape(14.dp),
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (badge.isUnlocked) MossGreen.copy(alpha = 0.4f) else CardBorderSubtle
              )
            ) {
              Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Text(badge.iconGlyph, fontSize = 24.sp)
                Text(
                  badge.name,
                  fontSize = 13.sp,
                  fontWeight = FontWeight.Bold,
                  color = if (badge.isUnlocked) ForestInkDark else MutedSecondary
                )
                Text(
                  badge.description,
                  fontSize = 11.sp,
                  color = MutedSecondary,
                  lineHeight = 15.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                  modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (badge.isUnlocked) SunGoldContainer else CardCreamLow)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    if (badge.isUnlocked) "Unlocked" else "Locked",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (badge.isUnlocked) SunGoldText else MutedSecondary
                  )
                }
              }
            }
          }
          if (rowBadges.size == 1) {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }

      // SECTION DIVIDER FOR SETTINGS
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        "Expedition Settings & Cloud Vault",
        fontSize = 14.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        color = MutedSecondary
      )

      // Supabase Authentication & Cloud Sync
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (authSession != null) MossGreen.copy(alpha = 0.5f) else CardBorder,
        backgroundColor = CardCream
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "EXPEDITION CLOUD ACCOUNT",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              color = ForestInkDark
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (authSession != null) LeafPillContainer else TerracottaContainer)
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                if (authSession != null) "Signed In" else "Local Guest",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (authSession != null) LeafPillText else TerracottaText
              )
            }
          }

          if (authSession != null) {
            Text(
              "Explorer: ${authSession.email}",
              fontSize = 14.sp,
              fontWeight = FontWeight.SemiBold,
              color = ForestInk
            )
            Text(
              "Local AES-256-GCM token encryption active. Photographic proofs and field observations are securely authenticated.",
              fontSize = 12.sp,
              color = MutedSecondary,
              lineHeight = 16.sp
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
                colors = ButtonDefaults.buttonColors(containerColor = MossGreenDark),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
              ) {
                Text("Sync Offline Quests", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
              }

              Button(
                onClick = onSignOut,
                colors = ButtonDefaults.buttonColors(containerColor = CardCreamLow),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
              ) {
                Text("Sign Out", color = ForestInk, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
              }
            }
          } else {
            Text(
              "Sign in to sync your field journal across devices, log verified real-world quests, and earn community recognition.",
              fontSize = 12.sp,
              color = MutedSecondary,
              lineHeight = 17.sp
            )

            OutlinedTextField(
              value = authEmail,
              onValueChange = { authEmail = it },
              label = { Text("Explorer Email", color = MutedSecondary) },
              modifier = Modifier.fillMaxWidth(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MossGreen,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = ForestInk,
                unfocusedTextColor = ForestInk,
                focusedContainerColor = CardCreamLow,
                unfocusedContainerColor = CardCreamLow
              ),
              shape = RoundedCornerShape(10.dp)
            )

            OutlinedTextField(
              value = authPassword,
              onValueChange = { authPassword = it },
              label = { Text("Password", color = MutedSecondary) },
              modifier = Modifier.fillMaxWidth(),
              visualTransformation = PasswordVisualTransformation(),
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
              singleLine = true,
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MossGreen,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = ForestInk,
                unfocusedTextColor = ForestInk,
                focusedContainerColor = CardCreamLow,
                unfocusedContainerColor = CardCreamLow
              ),
              shape = RoundedCornerShape(10.dp)
            )

            if (authErrorMessage != null) {
              Text(
                authErrorMessage ?: "",
                color = StatusAlertRed,
                fontSize = 12.sp,
                lineHeight = 16.sp
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
                colors = ButtonDefaults.buttonColors(containerColor = MossGreenDark),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp)
              ) {
                if (authLoading) {
                  CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White)
                } else {
                  Text("Sign In", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                colors = ButtonDefaults.buttonColors(containerColor = CardCreamLow),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
              ) {
                Text("Register", color = ForestInk, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
              }
            }
          }
        }
      }

      // ENVIRONMENT MODE TOGGLE (LIVE GRASS vs ASTROTURF)
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = if (environmentMode == DataSourceMode.SANDBOX_DEMO) ClayAccent.copy(alpha = 0.5f) else CardBorder,
        backgroundColor = CardCream
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text(
              "ENVIRONMENT MODE",
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              letterSpacing = 0.5.sp,
              color = ForestInkDark
            )
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (environmentMode == DataSourceMode.PRODUCTION) LeafPillContainer else TerracottaContainer)
                .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
              Text(
                if (environmentMode == DataSourceMode.PRODUCTION) "Live Grass" else "Astroturf (Sandbox)",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (environmentMode == DataSourceMode.PRODUCTION) LeafPillText else TerracottaText
              )
            }
          }

          Text(
            if (environmentMode == DataSourceMode.PRODUCTION) {
              "Live Grass Mode: Connected to real Supabase community tasks and verified contributions."
            } else {
              "Astroturf Simulation: Using offline test fixtures to safely test route discovery and side quests."
            },
            fontSize = 12.sp,
            color = MutedSecondary,
            lineHeight = 17.sp
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Button(
              onClick = { onSetEnvironmentMode(DataSourceMode.PRODUCTION) },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (environmentMode == DataSourceMode.PRODUCTION) MossGreenDark else CardCreamLow
              ),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              border = if (environmentMode == DataSourceMode.PRODUCTION) null else androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
              Text(
                "Live Grass Mode",
                color = if (environmentMode == DataSourceMode.PRODUCTION) Color.White else ForestInk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
              )
            }

            Button(
              onClick = { onSetEnvironmentMode(DataSourceMode.SANDBOX_DEMO) },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (environmentMode == DataSourceMode.SANDBOX_DEMO) ClayAccent else CardCreamLow
              ),
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              border = if (environmentMode == DataSourceMode.SANDBOX_DEMO) null else androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
            ) {
              Text(
                "Astroturf (Sandbox)",
                color = if (environmentMode == DataSourceMode.SANDBOX_DEMO) Color.White else ForestInk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
              )
            }
          }
        }
      }

      // Offline Diagnostics & Integrity
      TactileCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = CardBorder,
        backgroundColor = CardCreamLow
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          Text(
            "LOCAL VAULT & PRIVACY INTEGRITY",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = MutedSecondary
          )
          Text(
            "Encrypted Storage: $draftCount offline draft(s) • $submissionCount tracked quest(s)",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = ForestInk
          )
          Text(
            "Privacy: On-device AES-256-GCM encryption. Zero third-party trackers. Excluded from OS cloud backups.",
            fontSize = 11.sp,
            color = MutedSecondary,
            lineHeight = 15.sp
          )
        }
      }
    }
  }
}

@Composable
private fun StatBox(
  label: String,
  value: String,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(CardCreamLow)
      .border(1.dp, CardBorder, RoundedCornerShape(12.dp))
      .padding(horizontal = 8.dp, vertical = 10.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Text(
        value,
        fontSize = 18.sp,
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Bold,
        color = accentColor
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        label,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium,
        color = MutedSecondary
      )
    }
  }
}
