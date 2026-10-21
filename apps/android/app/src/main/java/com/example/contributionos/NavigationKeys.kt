package com.example.contributionos

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object NavHome : NavKey
@Serializable data object NavMap : NavKey
@Serializable data object NavMissions : NavKey
@Serializable data object NavImpact : NavKey
@Serializable data object NavProfile : NavKey
@Serializable data object NavExpeditionDetails : NavKey
@Serializable data class NavMissionDetails(val taskId: String, val nodeId: String? = null) : NavKey
@Serializable data class NavEvidenceCapture(val taskId: String, val draftId: String? = null) : NavKey
@Serializable data class NavSubmissionStatus(val submissionId: String) : NavKey
