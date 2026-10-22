package com.example.contributionos.ui.screens

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.SubmissionStatus
import com.example.contributionos.theme.CivicAmber
import com.example.contributionos.theme.CivicBorderActive
import com.example.contributionos.theme.CivicBorderSubtle
import com.example.contributionos.theme.CivicCyan
import com.example.contributionos.theme.CivicEmerald
import com.example.contributionos.theme.CivicForestDark
import com.example.contributionos.theme.CivicMidnightNavy
import com.example.contributionos.theme.CivicSurfaceDark
import com.example.contributionos.theme.CivicSurfacePanel
import com.example.contributionos.theme.TextMuted
import com.example.contributionos.theme.TextPrimary
import com.example.contributionos.theme.TextSecondary
import com.example.contributionos.ui.components.LayeredPanel

@Composable
fun SubmissionStatusScreen(
  submission: CivicSubmission,
  onReturnHome: () -> Unit,
  onViewMissions: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isOffline = submission.status == SubmissionStatus.QUEUED_OFFLINE
  val isDemo = submission.isSandboxDemo

  val headerTitle = when {
    isDemo -> "SANDBOX EVIDENCE LOGGED"
    isOffline -> "EVIDENCE QUEUED OFFLINE"
    submission.status == SubmissionStatus.VALIDATED -> "EVIDENCE VALIDATED & MINTED"
    else -> "EVIDENCE SUBMITTED TO SERVER"
  }

  val accentColor = when {
    isDemo -> CivicAmber
    isOffline -> CivicCyan
    submission.status == SubmissionStatus.VALIDATED -> CivicEmerald
    else -> CivicEmerald
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(CivicForestDark)
      .padding(20.dp),
    verticalArrangement = Arrangement.Center,
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Box(
      modifier = Modifier
        .size(68.dp)
        .clip(CircleShape)
        .background(CivicSurfaceDark)
        .border(2.dp, accentColor, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Icon(
        if (isOffline) Icons.Default.Info else Icons.Default.Check,
        contentDescription = null,
        tint = accentColor,
        modifier = Modifier.size(36.dp)
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    Text(
      headerTitle,
      fontSize = 18.sp,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 1.sp,
      color = TextPrimary
    )

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      if (isOffline) {
        "Submission #${submission.id} is stored in secure local storage and will sync when network connectivity is available."
      } else if (isDemo) {
        "Submission #${submission.id} recorded in isolated sandbox demo mode (non-civic simulation)."
      } else {
        "Submission #${submission.id} has been logged to the civic validation queue."
      },
      fontSize = 13.sp,
      color = TextSecondary,
      textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Lifecycle Timeline
    LayeredPanel(modifier = Modifier.fillMaxWidth(), borderColor = CivicBorderActive) {
      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        TimelineItem(
          stepNumber = "1",
          title = "Field Evidence Encrypted & Logged",
          subtitle = "Notes and coordinates stored in client-signed ledger",
          isDone = true,
          accentColor = accentColor
        )
        TimelineItem(
          stepNumber = "2",
          title = if (isOffline) "Server Transmission" else "Peer Validator Review",
          subtitle = if (isOffline) "Awaiting internet connection to upload to Supabase" else "Assigned to accredited district reviewer for audit",
          isDone = submission.status != SubmissionStatus.QUEUED_OFFLINE && submission.status != SubmissionStatus.PENDING_REVIEW,
          accentColor = accentColor
        )
        TimelineItem(
          stepNumber = "3",
          title = "Server-Authoritative Progression",
          subtitle = "XP and badge progression awarded only upon server approval",
          isDone = submission.status == SubmissionStatus.VALIDATED,
          accentColor = accentColor
        )
      }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Button(
        onClick = onReturnHome,
        colors = ButtonDefaults.buttonColors(containerColor = CivicSurfacePanel),
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("Home Base", color = TextPrimary, fontWeight = FontWeight.Bold)
      }

      Button(
        onClick = onViewMissions,
        colors = ButtonDefaults.buttonColors(containerColor = accentColor),
        modifier = Modifier.weight(1f),
        shape = RoundedCornerShape(8.dp)
      ) {
        Text("View in Missions", color = Color.Black, fontWeight = FontWeight.Bold)
      }
    }
  }
}

@Composable
private fun TimelineItem(
  stepNumber: String,
  title: String,
  subtitle: String,
  isDone: Boolean,
  accentColor: Color = CivicEmerald
) {
  Row(verticalAlignment = Alignment.Top) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .clip(CircleShape)
        .background(if (isDone) accentColor else CivicSurfaceDark)
        .border(1.dp, if (isDone) accentColor else CivicBorderSubtle, CircleShape),
      contentAlignment = Alignment.Center
    ) {
      Text(
        stepNumber,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = if (isDone) Color.Black else TextMuted
      )
    }
    Spacer(modifier = Modifier.width(10.dp))
    Column {
      Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = if (isDone) accentColor else TextPrimary)
      Text(subtitle, fontSize = 11.sp, color = TextMuted)
    }
  }
}
