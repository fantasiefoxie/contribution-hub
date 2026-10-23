package com.example.contributionos.model

import kotlinx.serialization.Serializable

enum class NodeStatus {
  AVAILABLE,
  IN_PROGRESS,
  VALIDATED,
  LOCKED
}

enum class SubmissionStatus {
  DRAFT_LOCAL,
  QUEUED_OFFLINE,
  UPLOADING,
  SERVER_ACCEPTED,
  PENDING_REVIEW,
  APPROVED,
  REJECTED,
  RETRYABLE_FAILURE,
  PERMANENT_FAILURE,
  VALIDATED,
  CHANGES_REQUESTED,
  // Self-guided or locally-completing quests record completion here.
  // LOCAL_COMPLETION submissions are NEVER queued for server sync.
  // They award exploration XP only; civic credits are never granted from them.
  LOCAL_COMPLETION
}

enum class DataSourceMode {
  PRODUCTION,
  SANDBOX_DEMO
}

enum class ExpeditionStatus {
  ACTIVE,
  COMPLETED,
  ABANDONED
}

@Serializable
data class CivicTask(
  val id: String,
  val title: String,
  val description: String,
  val difficulty: Int,
  val status: String,
  val visibility: String = "public",
  val tags: List<String> = emptyList(),
  val orgId: String? = null,
  val initiativeId: String? = null,
  val district: String = "Civic Core",
  val locationLabel: String = "Metropolitan Area",
  val evidenceRequirements: String = "Photo of completed action and geotagged field notes",
  val estimatedDurationMinutes: Int = 15,
  val safetyNotice: String = "Stay alert near traffic, respect private property boundaries, and hydrate.",
  val isSandboxDemo: Boolean = false,
  // When true: quest completes locally, awards exploration XP, and is NEVER routed through
  // the civic submission queue or task-engine backend. Appropriate for self-guided exploration
  // activities that are not verifiable by external stewards.
  val isLocalCompletionOnly: Boolean = false,
  // Authoritative exploration XP reward for quest completion
  val xpReward: Int = 50
)

@Serializable
data class ExpeditionNode(
  val id: String,
  val taskId: String,
  val step: Int,
  val branchIndex: Int,
  val title: String,
  val category: String,
  val difficulty: Int,
  val xpReward: Int,
  val status: NodeStatus,
  val isoX: Float,
  val isoY: Float,
  val childNodeIds: List<String> = emptyList(),
  val isSandboxDemo: Boolean = false
)

@Serializable
data class CivicExpedition(
  val id: String,
  val title: String,
  val theme: String,
  val lore: String,
  val district: String,
  val targetMilestone: String,
  val nodes: List<ExpeditionNode>,
  val activeRunSeed: Long,
  val status: ExpeditionStatus = ExpeditionStatus.ACTIVE,
  val isSandboxDemo: Boolean = false
) {
  val completedNodes: Int get() = nodes.count { it.status == NodeStatus.VALIDATED }
  val totalNodes: Int get() = nodes.size
  val progressPercent: Float get() = if (totalNodes == 0) 0f else completedNodes.toFloat() / totalNodes.toFloat()
}

@Serializable
data class MapWaypoint(
  val id: String,
  val title: String,
  val description: String,
  val category: String,
  val latitude: Double,
  val longitude: Double,
  val xpReward: Int = 50,
  val linkedTaskId: String? = null,
  val isSandboxTest: Boolean = true
)

@Serializable
data class MissionDraft(
  val id: String,
  val taskId: String,
  val taskTitle: String,
  val notes: String,
  val photoUri: String? = null,
  val latitude: Double? = null,
  val longitude: Double? = null,
  val timestamp: Long = System.currentTimeMillis(),
  val isOfflinePending: Boolean = false,
  val isSandboxDemo: Boolean = false
)

@Serializable
data class CivicSubmission(
  val id: String,
  val taskId: String,
  val taskTitle: String,
  val submittedAt: Long,
  val content: String,
  val status: SubmissionStatus,
  val feedback: String? = null,
  val scoreAwarded: Int? = null,
  val isOfflineOrigin: Boolean = false,
  val isSandboxDemo: Boolean = false,
  val remoteSubmissionId: String? = null,
  val syncErrorMessage: String? = null,
  val clientSubmissionId: String? = null,
  val localPhotoUri: String? = null,
  val remoteStoragePath: String? = null
) {
  val isVerified: Boolean get() = status == SubmissionStatus.APPROVED || status == SubmissionStatus.VALIDATED
  val isPending: Boolean get() = status == SubmissionStatus.PENDING_REVIEW || status == SubmissionStatus.SERVER_ACCEPTED
  val isOfflineQueue: Boolean get() = status == SubmissionStatus.QUEUED_OFFLINE || status == SubmissionStatus.RETRYABLE_FAILURE || status == SubmissionStatus.DRAFT_LOCAL
  // Local-completion submissions complete on-device; they have no server lifecycle.
  val isLocalOnly: Boolean get() = status == SubmissionStatus.LOCAL_COMPLETION

  fun toShareableSnippet(): String {
    val statusText = when {
      isVerified -> "Authoritatively verified"
      status == SubmissionStatus.LOCAL_COMPLETION -> "Completed locally (Self-guided lore logged)"
      else -> "Side quest logged. Verification pending."
    }
    return buildString {
      appendLine("🌱 TOUCH GRASS — Side Quest Complete!")
      appendLine("Quest: $taskTitle")
      appendLine("Status: $statusText")
      appendLine("\"Side quest complete. Biome lore logged instead of doomscrolling. The grass acknowledges you.\"")
      appendLine("Shared via Touch Grass · Your screen time has side quests.")
    }.trimEnd()
  }
}

@Serializable
data class CommunityMilestone(
  val id: String,
  val title: String,
  val description: String,
  val currentValue: Int,
  val targetValue: Int,
  val unit: String,
  val category: String,
  val isAchieved: Boolean = currentValue >= targetValue,
  val isSandboxDemo: Boolean = false
) {
  val percent: Float get() = (currentValue.toFloat() / targetValue.toFloat()).coerceIn(0f, 1f)
}

@Serializable
data class CivicBadge(
  val id: String,
  val name: String,
  val description: String,
  val iconGlyph: String,
  val isUnlocked: Boolean
)

@Serializable
data class CivicProfile(
  val id: String,
  val displayName: String,
  val callsign: String,
  val level: Int,
  val xp: Int = 0,
  val explorationXp: Int = 0,
  val verifiedCivicCredits: Int = 0,
  val nextLevelXp: Int = 500,
  val rankTitle: String,
  val verifiedContributionsCount: Int = 0,
  val completedExpeditionsCount: Int = 0,
  val activeStreakDays: Int = 1,
  val discoveredWaypointsCount: Int = 0,
  val specialization: String = "Community Stewardship",
  val badges: List<CivicBadge> = emptyList(),
  val isSandboxDemo: Boolean = false,
  // Unique waypoint node IDs and quest reward IDs that have granted exploration XP.
  // Persisted atomically within the encrypted profile to guarantee that XP gains
  // and reward eligibility cannot desynchronize on crashes, power loss, or retries.
  val rewardedWaypointIds: Set<String> = emptySet()
)
