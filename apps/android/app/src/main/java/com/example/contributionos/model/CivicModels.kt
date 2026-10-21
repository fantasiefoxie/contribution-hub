package com.example.contributionos.model

import kotlinx.serialization.Serializable

enum class NodeStatus {
  AVAILABLE,
  IN_PROGRESS,
  VALIDATED,
  LOCKED
}

enum class SubmissionStatus {
  PENDING_REVIEW,
  VALIDATED,
  CHANGES_REQUESTED,
  REJECTED
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
  val evidenceRequirements: String = "Photo of completed action and geotagged field notes"
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
  val childNodeIds: List<String> = emptyList()
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
  val status: ExpeditionStatus = ExpeditionStatus.ACTIVE
) {
  val completedNodes: Int get() = nodes.count { it.status == NodeStatus.VALIDATED }
  val totalNodes: Int get() = nodes.size
  val progressPercent: Float get() = if (totalNodes == 0) 0f else completedNodes.toFloat() / totalNodes.toFloat()
}

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
  val isOfflinePending: Boolean = false
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
  val isOfflineOrigin: Boolean = false
)

@Serializable
data class CommunityMilestone(
  val id: String,
  val title: String,
  val description: String,
  val currentValue: Int,
  val targetValue: Int,
  val unit: String,
  val category: String,
  val isAchieved: Boolean = currentValue >= targetValue
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
  val xp: Int,
  val nextLevelXp: Int,
  val rankTitle: String,
  val verifiedContributionsCount: Int,
  val completedExpeditionsCount: Int,
  val activeStreakDays: Int,
  val specialization: String,
  val badges: List<CivicBadge>
)
