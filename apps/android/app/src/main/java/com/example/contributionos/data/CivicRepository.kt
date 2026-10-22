package com.example.contributionos.data

import android.content.Context
import com.example.contributionos.model.CivicBadge
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.CommunityMilestone
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.model.ExpeditionNode
import com.example.contributionos.model.MissionDraft
import com.example.contributionos.model.NodeStatus
import com.example.contributionos.model.SubmissionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

interface CivicRepository {
  val environmentMode: Flow<DataSourceMode>
  val isLiveConnected: Flow<Boolean>
  val activeExpedition: Flow<CivicExpedition?>
  val availableExpeditions: Flow<List<CivicExpedition>>
  val tasks: Flow<List<CivicTask>>
  val drafts: Flow<List<MissionDraft>>
  val submissions: Flow<List<CivicSubmission>>
  val milestones: Flow<List<CommunityMilestone>>
  val profile: Flow<CivicProfile>
  val syncStatusMessage: Flow<String?>

  suspend fun setEnvironmentMode(mode: DataSourceMode)
  suspend fun refreshRemoteData()
  suspend fun selectExpedition(expeditionId: String)
  suspend fun getTask(taskId: String): CivicTask?
  suspend fun saveDraft(draft: MissionDraft)
  suspend fun deleteDraft(draftId: String)
  suspend fun submitMission(draft: MissionDraft): CivicSubmission
  suspend fun resetRun(expeditionId: String)
}

class DefaultCivicRepository(
  private val context: Context,
  private val localDraftStore: LocalDraftStore = LocalDraftStore(context),
  private val remoteDataSource: RemoteCivicDataSource = RemoteCivicDataSource()
) : CivicRepository {

  private val _environmentMode = MutableStateFlow(DataSourceMode.PRODUCTION)
  override val environmentMode = _environmentMode.asStateFlow()

  private val _isLiveConnected = MutableStateFlow(false)
  override val isLiveConnected = _isLiveConnected.asStateFlow()

  private val _syncStatusMessage = MutableStateFlow<String?>(null)
  override val syncStatusMessage = _syncStatusMessage.asStateFlow()

  private val _expeditions = MutableStateFlow<List<CivicExpedition>>(emptyList())
  override val availableExpeditions = _expeditions.asStateFlow()

  private val _activeExpedition = MutableStateFlow<CivicExpedition?>(null)
  override val activeExpedition = _activeExpedition.asStateFlow()

  private val _tasks = MutableStateFlow<List<CivicTask>>(emptyList())
  override val tasks = _tasks.asStateFlow()

  private val _drafts = MutableStateFlow<List<MissionDraft>>(emptyList())
  override val drafts = _drafts.asStateFlow()

  private val _submissions = MutableStateFlow<List<CivicSubmission>>(emptyList())
  override val submissions = _submissions.asStateFlow()

  private val _milestones = MutableStateFlow<List<CommunityMilestone>>(emptyList())
  override val milestones = _milestones.asStateFlow()

  private val _profile = MutableStateFlow(
    CivicProfile(
      id = "user-anon",
      displayName = "Civic Contributor",
      callsign = "Citizen-01",
      level = 1,
      xp = 0,
      nextLevelXp = 500,
      rankTitle = "Field Candidate",
      verifiedContributionsCount = 0,
      completedExpeditionsCount = 0,
      activeStreakDays = 1,
      specialization = "Community Stewardship",
      badges = listOf(
        CivicBadge("b-first", "First Groundwork", "Complete your initial verified field contribution.", "🌱", false),
        CivicBadge("b-eco", "Ecological Observer", "Record 5 environmental or forestry validations.", "🌳", false)
      ),
      isSandboxDemo = false
    )
  )
  override val profile = _profile.asStateFlow()

  suspend fun initialize() {
    loadLocalStoreData()
    if (_environmentMode.value == DataSourceMode.PRODUCTION) {
      refreshRemoteData()
    } else {
      loadSandboxFixtures()
    }
  }

  private suspend fun loadLocalStoreData() {
    val allDrafts = localDraftStore.getDrafts()
    val allSubmissions = localDraftStore.getSubmissions()
    val isDemo = _environmentMode.value == DataSourceMode.SANDBOX_DEMO

    _drafts.value = allDrafts.filter { it.isSandboxDemo == isDemo }
    _submissions.value = allSubmissions.filter { it.isSandboxDemo == isDemo }
  }

  override suspend fun setEnvironmentMode(mode: DataSourceMode) {
    if (_environmentMode.value == mode) return
    _environmentMode.value = mode
    loadLocalStoreData()

    if (mode == DataSourceMode.SANDBOX_DEMO) {
      loadSandboxFixtures()
    } else {
      _tasks.value = emptyList()
      _expeditions.value = emptyList()
      _activeExpedition.value = null
      _milestones.value = emptyList()
      refreshRemoteData()
    }
  }

  private fun loadSandboxFixtures() {
    val demoExpeditions = SeedMissionCatalog.generateExpeditions()
    _expeditions.value = demoExpeditions
    _activeExpedition.value = demoExpeditions.firstOrNull()
    _tasks.value = SeedMissionCatalog.realTasks
    _milestones.value = SeedMissionCatalog.realMilestones
    _profile.value = SeedMissionCatalog.defaultProfile
    _isLiveConnected.value = false
    _syncStatusMessage.value = "Sandbox Mode Active: Using isolated test fixtures (non-civic simulation)."
  }

  override suspend fun refreshRemoteData() {
    if (_environmentMode.value == DataSourceMode.SANDBOX_DEMO) {
      loadSandboxFixtures()
      return
    }

    _syncStatusMessage.value = "Connecting to Civic Registry at ${remoteDataSource.baseUrl}..."
    when (val taskResult = remoteDataSource.fetchTasks()) {
      is RemoteResult.Success -> {
        _isLiveConnected.value = true
        _tasks.value = taskResult.data
        _syncStatusMessage.value = null
      }
      is RemoteResult.Failure -> {
        _isLiveConnected.value = false
        _syncStatusMessage.value = if (taskResult.isOffline) {
          "Offline: Working in local draft mode. Remote missions unreachable."
        } else {
          taskResult.message
        }
      }
    }

    // Refresh Expeditions from backend initiatives
    when (val expResult = remoteDataSource.fetchInitiatives()) {
      is RemoteResult.Success -> {
        if (expResult.data.isNotEmpty()) {
          _expeditions.value = expResult.data
          if (_activeExpedition.value == null || !_expeditions.value.any { it.id == _activeExpedition.value?.id }) {
            _activeExpedition.value = expResult.data.firstOrNull()
          }
        }
      }
      is RemoteResult.Failure -> {
        // Keep honest empty state or previous cached initiatives
      }
    }

    // Refresh Milestones
    when (val msResult = remoteDataSource.fetchCommunityMilestones()) {
      is RemoteResult.Success -> {
        _milestones.value = msResult.data
      }
      is RemoteResult.Failure -> {
        // Honest empty or offline state
      }
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
    val taggedDraft = draft.copy(isSandboxDemo = _environmentMode.value == DataSourceMode.SANDBOX_DEMO)
    localDraftStore.saveDraft(taggedDraft)
    val current = _drafts.value.filterNot { it.id == taggedDraft.id }
    _drafts.value = listOf(taggedDraft) + current
  }

  override suspend fun deleteDraft(draftId: String) {
    localDraftStore.deleteDraft(draftId)
    _drafts.value = _drafts.value.filterNot { it.id == draftId }
  }

  override suspend fun submitMission(draft: MissionDraft): CivicSubmission {
    deleteDraft(draft.id)
    val isDemo = _environmentMode.value == DataSourceMode.SANDBOX_DEMO

    if (isDemo) {
      val demoSubmission = CivicSubmission(
        id = "sub-demo-" + UUID.randomUUID().toString().take(8),
        taskId = draft.taskId,
        taskTitle = draft.taskTitle,
        submittedAt = System.currentTimeMillis(),
        content = draft.notes,
        status = SubmissionStatus.PENDING_REVIEW,
        feedback = "[SANDBOX] Field evidence logged to demo simulation queue.",
        scoreAwarded = null,
        isOfflineOrigin = true,
        isSandboxDemo = true
      )
      localDraftStore.saveSubmission(demoSubmission)
      _submissions.value = listOf(demoSubmission) + _submissions.value

      // Update node status in active demo expedition
      val currentExp = _activeExpedition.value
      if (currentExp != null) {
        val updatedNodes = currentExp.nodes.map { node ->
          if (node.taskId == draft.taskId && node.status == NodeStatus.AVAILABLE) {
            node.copy(status = NodeStatus.IN_PROGRESS)
          } else {
            node
          }
        }
        _activeExpedition.value = currentExp.copy(nodes = updatedNodes)
      }
      return demoSubmission
    }

    // PRODUCTION MODE: Real Supabase API call
    val attachments = listOfNotNull(draft.photoUri)
    val remoteResult = remoteDataSource.submitEvidence(
      taskId = draft.taskId,
      content = draft.notes,
      attachments = attachments
    )

    val finalSubmission = when (remoteResult) {
      is RemoteResult.Success -> {
        remoteResult.data.copy(
          taskTitle = draft.taskTitle,
          isSandboxDemo = false
        )
      }
      is RemoteResult.Failure -> {
        // Safe offline fallback: store locally with QUEUED_OFFLINE status
        CivicSubmission(
          id = "sub-offline-" + UUID.randomUUID().toString().take(8),
          taskId = draft.taskId,
          taskTitle = draft.taskTitle,
          submittedAt = System.currentTimeMillis(),
          content = draft.notes,
          status = SubmissionStatus.QUEUED_OFFLINE,
          feedback = if (remoteResult.isOffline) {
            "Device is currently offline. Submission is queued safely in local encrypted storage."
          } else {
            "Server validation pending: ${remoteResult.message}"
          },
          scoreAwarded = null,
          isOfflineOrigin = true,
          isSandboxDemo = false,
          syncErrorMessage = remoteResult.message
        )
      }
    }

    localDraftStore.saveSubmission(finalSubmission)
    _submissions.value = listOf(finalSubmission) + _submissions.value

    // Update node status if active expedition has this node
    val currentExp = _activeExpedition.value
    if (currentExp != null) {
      val updatedNodes = currentExp.nodes.map { node ->
        if (node.taskId == draft.taskId && node.status == NodeStatus.AVAILABLE) {
          node.copy(status = NodeStatus.IN_PROGRESS)
        } else {
          node
        }
      }
      _activeExpedition.value = currentExp.copy(nodes = updatedNodes)
    }

    return finalSubmission
  }

  override suspend fun resetRun(expeditionId: String) {
    if (_environmentMode.value == DataSourceMode.SANDBOX_DEMO) {
      val currentExp = _expeditions.value.find { it.id == expeditionId } ?: return
      val newSeed = currentExp.activeRunSeed + 1
      val regenerated = SeedMissionCatalog.generateExpeditions(newSeed).firstOrNull { it.id == expeditionId }
      if (regenerated != null) {
        val allExpeditions = _expeditions.value.map { if (it.id == expeditionId) regenerated else it }
        _expeditions.value = allExpeditions
        if (_activeExpedition.value?.id == expeditionId) {
          _activeExpedition.value = regenerated
        }
      }
    } else {
      // In production mode, re-fetch remote initiatives
      refreshRemoteData()
    }
  }
}
