package com.example.contributionos.data

import android.content.Context
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.CommunityMilestone
import com.example.contributionos.model.ExpeditionNode
import com.example.contributionos.model.MissionDraft
import com.example.contributionos.model.NodeStatus
import com.example.contributionos.model.SubmissionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

interface CivicRepository {
  val isLiveConnected: Flow<Boolean>
  val activeExpedition: Flow<CivicExpedition>
  val availableExpeditions: Flow<List<CivicExpedition>>
  val tasks: Flow<List<CivicTask>>
  val drafts: Flow<List<MissionDraft>>
  val submissions: Flow<List<CivicSubmission>>
  val milestones: Flow<List<CommunityMilestone>>
  val profile: Flow<CivicProfile>

  suspend fun selectExpedition(expeditionId: String)
  suspend fun getTask(taskId: String): CivicTask?
  suspend fun saveDraft(draft: MissionDraft)
  suspend fun deleteDraft(draftId: String)
  suspend fun submitMission(draft: MissionDraft): CivicSubmission
  suspend fun resetRun(expeditionId: String)
}

class DefaultCivicRepository(
  private val context: Context,
  private val localDraftStore: LocalDraftStore = LocalDraftStore(context)
) : CivicRepository {

  private val _isLiveConnected = MutableStateFlow(false)
  override val isLiveConnected = _isLiveConnected.asStateFlow()

  private val _expeditions = MutableStateFlow(SeedMissionCatalog.generateExpeditions())
  override val availableExpeditions = _expeditions.asStateFlow()

  private val _activeExpedition = MutableStateFlow(_expeditions.value.first())
  override val activeExpedition = _activeExpedition.asStateFlow()

  private val _tasks = MutableStateFlow(SeedMissionCatalog.realTasks)
  override val tasks = _tasks.asStateFlow()

  private val _drafts = MutableStateFlow<List<MissionDraft>>(emptyList())
  override val drafts = _drafts.asStateFlow()

  private val _submissions = MutableStateFlow<List<CivicSubmission>>(emptyList())
  override val submissions = _submissions.asStateFlow()

  private val _milestones = MutableStateFlow(SeedMissionCatalog.realMilestones)
  override val milestones = _milestones.asStateFlow()

  private val _profile = MutableStateFlow(SeedMissionCatalog.defaultProfile)
  override val profile = _profile.asStateFlow()

  suspend fun initialize() {
    val savedDrafts = localDraftStore.getDrafts()
    val savedSubmissions = localDraftStore.getSubmissions()

    if (savedDrafts.isNotEmpty()) {
      _drafts.value = savedDrafts
    } else {
      // Provide an initial realistic draft for demonstration of the vertical slice workflow
      val initialDraft = MissionDraft(
        id = "draft-1",
        taskId = "task-solar-audit-7",
        taskTitle = "Municipal Solar Canopy Shade Survey",
        notes = "Checked azimuth shade angles on Panels 1-4. Overhanging elm limbs cast 15% shade at 14:00.",
        latitude = 19.033,
        longitude = 73.029,
        isOfflinePending = true
      )
      _drafts.value = listOf(initialDraft)
    }

    if (savedSubmissions.isNotEmpty()) {
      _submissions.value = savedSubmissions
    } else {
      val initialSubmission = CivicSubmission(
        id = "sub-init-1",
        taskId = "task-green-garden-1",
        taskTitle = "Community Garden Canopy Restoration",
        submittedAt = System.currentTimeMillis() - 86400000L,
        content = "Spread organic pine mulch across 12 sapling basins and cleared bindweed roots.",
        status = SubmissionStatus.VALIDATED,
        feedback = "Verified by District Ecologist. Soil moisture retention confirmed.",
        scoreAwarded = 80,
        isOfflineOrigin = false
      )
      _submissions.value = listOf(initialSubmission)
    }
  }

  override suspend fun selectExpedition(expeditionId: String) {
    val exp = _expeditions.value.find { it.id == expeditionId } ?: return
    _activeExpedition.value = exp
  }

  override suspend fun getTask(taskId: String): CivicTask? {
    return _tasks.value.find { it.id == taskId }
  }

  override suspend fun saveDraft(draft: MissionDraft) {
    localDraftStore.saveDraft(draft)
    val current = _drafts.value.filterNot { it.id == draft.id }
    _drafts.value = listOf(draft) + current
  }

  override suspend fun deleteDraft(draftId: String) {
    localDraftStore.deleteDraft(draftId)
    _drafts.value = _drafts.value.filterNot { it.id == draftId }
  }

  override suspend fun submitMission(draft: MissionDraft): CivicSubmission {
    // Delete draft once submitted
    deleteDraft(draft.id)

    val submission = CivicSubmission(
      id = "sub-" + UUID.randomUUID().toString().take(8),
      taskId = draft.taskId,
      taskTitle = draft.taskTitle,
      submittedAt = System.currentTimeMillis(),
      content = draft.notes,
      status = SubmissionStatus.PENDING_REVIEW,
      feedback = "Submission queued for server-authoritative validator review.",
      scoreAwarded = null,
      isOfflineOrigin = !_isLiveConnected.value
    )

    localDraftStore.saveSubmission(submission)
    _submissions.value = listOf(submission) + _submissions.value

    // Update node status in active expedition to IN_PROGRESS
    val currentExp = _activeExpedition.value
    val updatedNodes = currentExp.nodes.map { node ->
      if (node.taskId == draft.taskId && node.status == NodeStatus.AVAILABLE) {
        node.copy(status = NodeStatus.IN_PROGRESS)
      } else {
        node
      }
    }
    _activeExpedition.value = currentExp.copy(nodes = updatedNodes)

    return submission
  }

  override suspend fun resetRun(expeditionId: String) {
    // Resets current expedition route while preserving permanent profile stats and verified submissions
    val currentExp = _expeditions.value.find { it.id == expeditionId } ?: return
    val newSeed = currentExp.activeRunSeed + 1
    val regenerated = SeedMissionCatalog.generateExpeditions(newSeed).first { it.id == expeditionId }
    
    val allExpeditions = _expeditions.value.map { if (it.id == expeditionId) regenerated else it }
    _expeditions.value = allExpeditions
    if (_activeExpedition.value.id == expeditionId) {
      _activeExpedition.value = regenerated
    }
  }
}
