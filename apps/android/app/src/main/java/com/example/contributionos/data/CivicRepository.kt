package com.example.contributionos.data

import android.content.Context
import android.util.Log
import com.example.contributionos.auth.AuthResult
import com.example.contributionos.auth.AuthSession
import com.example.contributionos.auth.SupabaseAuthManager
import com.example.contributionos.model.CivicBadge
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.CommunityMilestone
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.model.ExpeditionNode
import com.example.contributionos.model.ExpeditionStatus
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
  val authSession: Flow<AuthSession?>
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
  suspend fun signIn(email: String, password: String): AuthResult<AuthSession>
  suspend fun signUp(email: String, password: String): AuthResult<AuthSession>
  suspend fun signOut()
  suspend fun selectExpedition(expeditionId: String)
  suspend fun getTask(taskId: String): CivicTask?
  suspend fun saveDraft(draft: MissionDraft)
  suspend fun deleteDraft(draftId: String)
  suspend fun submitMission(draft: MissionDraft): CivicSubmission
  suspend fun retryPendingSubmissions()
  suspend fun resetRun(expeditionId: String)
  suspend fun abandonExpedition(expeditionId: String)
  suspend fun recordWaypointDiscovery(nodeId: String, xpReward: Int)
}

class DefaultCivicRepository(
  private val context: Context,
  private val localDraftStore: LocalDraftStore = LocalDraftStore(context),
  private val remoteDataSource: RemoteCivicDataSource = RemoteCivicDataSource(),
  private val authManager: SupabaseAuthManager = SupabaseAuthManager(context),
  private val storageUploader: SupabaseStorageUploader = SupabaseStorageUploader(context)
) : CivicRepository {

  private val _environmentMode = MutableStateFlow(DataSourceMode.PRODUCTION)
  override val environmentMode = _environmentMode.asStateFlow()

  private val _isLiveConnected = MutableStateFlow(false)
  override val isLiveConnected = _isLiveConnected.asStateFlow()

  override val authSession: Flow<AuthSession?> = authManager.session

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
      explorationXp = 0,
      verifiedCivicCredits = 0,
      nextLevelXp = 500,
      rankTitle = "Field Candidate",
      verifiedContributionsCount = 0,
      completedExpeditionsCount = 0,
      activeStreakDays = 1,
      discoveredWaypointsCount = 0,
      specialization = "Community Stewardship",
      badges = listOf(
        CivicBadge("b-first", "First Groundwork", "Touched real grass for the first time.", "🌱", false),
        CivicBadge("b-eco", "Ecological Observer", "Record 5 environmental or forestry validations.", "🌳", false)
      ),
      isSandboxDemo = false
    )
  )
  override val profile = _profile.asStateFlow()

  // Anti-grind guard: track which waypoint nodeIds have already been rewarded.
  // Restored from encrypted storage on startup and updated atomically with persistence.
  private val _rewardedNodeIds = mutableSetOf<String>()

  suspend fun initialize() {
    authManager.initialize()
    updateProfileForSession(authManager.session.value)

    // Restore persisted exploration profile and rewarded waypoint/quest IDs
    val persistedProfile = localDraftStore.getProfile()
    if (persistedProfile != null && !persistedProfile.isSandboxDemo) {
      _profile.value = _profile.value.copy(
        explorationXp = persistedProfile.explorationXp,
        xp = persistedProfile.explorationXp,
        level = persistedProfile.level,
        discoveredWaypointsCount = persistedProfile.discoveredWaypointsCount,
        rewardedWaypointIds = persistedProfile.rewardedWaypointIds
      )
      _rewardedNodeIds.clear()
      _rewardedNodeIds.addAll(persistedProfile.rewardedWaypointIds)
    }

    val persistedRewarded = localDraftStore.getRewardedNodeIds()
    _rewardedNodeIds.addAll(persistedRewarded)

    loadLocalStoreData()
    if (_environmentMode.value == DataSourceMode.PRODUCTION) {
      refreshRemoteData()
    } else {
      loadSandboxFixtures()
    }
  }

  private fun updateProfileForSession(session: AuthSession?) {
    if (session != null) {
      val callsign = session.email.substringBefore('@')
      _profile.value = _profile.value.copy(
        id = session.userId,
        displayName = session.email,
        callsign = callsign,
        rankTitle = "Accredited Field Contributor"
      )
    } else {
      _profile.value = _profile.value.copy(
        id = "user-anon",
        displayName = "Civic Contributor",
        callsign = "Citizen-01",
        rankTitle = "Field Candidate"
      )
    }
  }

  override suspend fun signIn(email: String, password: String): AuthResult<AuthSession> {
    val result = authManager.signIn(email, password)
    if (result is AuthResult.Success) {
      updateProfileForSession(result.data)
      refreshRemoteData()
      retryPendingSubmissions()
    }
    return result
  }

  override suspend fun signUp(email: String, password: String): AuthResult<AuthSession> {
    val result = authManager.signUp(email, password)
    if (result is AuthResult.Success) {
      updateProfileForSession(result.data)
      refreshRemoteData()
    }
    return result
  }

  override suspend fun signOut() {
    authManager.signOut()
    updateProfileForSession(null)
  }

  private suspend fun loadLocalStoreData() {
    val allDrafts = localDraftStore.getDrafts()
    val allSubmissions = localDraftStore.getSubmissions()
    val isDemo = _environmentMode.value == DataSourceMode.SANDBOX_DEMO

    _drafts.value = allDrafts.filter { it.isSandboxDemo == isDemo }
    _submissions.value = allSubmissions.filter { it.isSandboxDemo == isDemo }

    if (!isDemo) {
      // Authoritatively calibrate verified contributions and credits from server-approved records only
      val verifiedSubs = _submissions.value.filter { it.isVerified && !it.isSandboxDemo }
      val verifiedCount = verifiedSubs.size
      val civicCredits = verifiedSubs.sumOf { it.scoreAwarded ?: 0 }
      val currentProf = _profile.value
      _profile.value = currentProf.copy(
        verifiedContributionsCount = verifiedCount,
        verifiedCivicCredits = civicCredits,
        xp = currentProf.explorationXp
      )
    }
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
      // Restore persisted production exploration profile and rewarded node IDs
      val persistedProfile = localDraftStore.getProfile()
      if (persistedProfile != null && !persistedProfile.isSandboxDemo) {
        _profile.value = _profile.value.copy(
          explorationXp = persistedProfile.explorationXp,
          xp = persistedProfile.explorationXp,
          level = persistedProfile.level,
          discoveredWaypointsCount = persistedProfile.discoveredWaypointsCount,
          rewardedWaypointIds = persistedProfile.rewardedWaypointIds
        )
      }
      val persistedRewarded = localDraftStore.getRewardedNodeIds()
      _rewardedNodeIds.clear()
      if (persistedProfile?.rewardedWaypointIds?.isNotEmpty() == true) {
        _rewardedNodeIds.addAll(persistedProfile.rewardedWaypointIds)
      }
      _rewardedNodeIds.addAll(persistedRewarded)
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
        if (_tasks.value.isEmpty()) {
          // Provide clearly labelled self-guided exploration quests so offline users have a playable loop
          _tasks.value = SeedMissionCatalog.selfGuidedQuests
        }
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
        if (msResult.data.isNotEmpty()) {
          _milestones.value = msResult.data
        } else {
          deriveLocalMilestones()
        }
      }
      is RemoteResult.Failure -> {
        deriveLocalMilestones()
      }
    }

    loadLocalStoreData()
  }

  private fun deriveLocalMilestones() {
    val isDemo = _environmentMode.value == DataSourceMode.SANDBOX_DEMO
    if (isDemo) {
      _milestones.value = SeedMissionCatalog.realMilestones
    } else {
      val verifiedCount = _submissions.value.count { it.isVerified && !it.isSandboxDemo }
      _milestones.value = listOf(
        CommunityMilestone(
          id = "ms-verified-actions",
          title = "Verified Community Improvements",
          description = "Authoritative citizen actions validated by accredited stewards.",
          currentValue = verifiedCount,
          targetValue = 100,
          unit = "verified acts",
          category = "Civic Progress",
          isSandboxDemo = false
        )
      )
    }
  }

  override suspend fun selectExpedition(expeditionId: String) {
    val exp = _expeditions.value.find { it.id == expeditionId } ?: return
    _activeExpedition.value = exp
  }

  override suspend fun getTask(taskId: String): CivicTask? {
    return _tasks.value.find { it.id == taskId }
      ?: SeedMissionCatalog.findTask(taskId)
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
    val clientSubmissionId = "client-sub-" + UUID.randomUUID().toString()

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
        isSandboxDemo = true,
        clientSubmissionId = clientSubmissionId,
        localPhotoUri = draft.photoUri
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

    // PRODUCTION MODE
    // Check if task is a self-guided exploration quest or marked as local-completion only.
    // Server-published tasks (task != null) strictly obey task.isLocalCompletionOnly.
    val task = getTask(draft.taskId)
    val isLocalCompletionOnly = when {
      task != null -> task.isLocalCompletionOnly
      SeedMissionCatalog.selfGuidedQuests.any { it.id == draft.taskId } -> true
      else -> false
    }

    if (isLocalCompletionOnly) {
      val existingLocalSub = _submissions.value.find { it.taskId == draft.taskId && it.status == SubmissionStatus.LOCAL_COMPLETION }
        ?: localDraftStore.getSubmissions().find { it.taskId == draft.taskId && it.status == SubmissionStatus.LOCAL_COMPLETION }

      val localSub = if (existingLocalSub != null) {
        existingLocalSub.copy(
          content = draft.notes,
          localPhotoUri = draft.photoUri ?: existingLocalSub.localPhotoUri,
          submittedAt = System.currentTimeMillis()
        )
      } else {
        CivicSubmission(
          id = "local-completion-${draft.taskId}",
          taskId = draft.taskId,
          taskTitle = draft.taskTitle,
          submittedAt = System.currentTimeMillis(),
          content = draft.notes,
          status = SubmissionStatus.LOCAL_COMPLETION,
          feedback = "Self-guided exploration completed locally. Notes and proof secured on-device.",
          scoreAwarded = null, // Civic credits are strictly server-authoritative; zero civic credits for local exploration
          isOfflineOrigin = true,
          isSandboxDemo = false,
          clientSubmissionId = "local-completion-${draft.taskId}",
          localPhotoUri = draft.photoUri,
          remoteStoragePath = null
        )
      }
      localDraftStore.saveSubmission(localSub)
      _submissions.value = listOf(localSub) + _submissions.value.filterNot { it.id == localSub.id }

      // Award authoritative exploration XP once per task ID using persistent rewarded set
      val questRewardId = "quest-reward-${draft.taskId}"
      if (!_rewardedNodeIds.contains(questRewardId)) {
        val matchingTask = getTask(draft.taskId)
        val questXpReward = matchingTask?.xpReward ?: 50
        if (questXpReward > 0) {
          val candidateRewardedSet = _rewardedNodeIds + questRewardId
          val currentProf = _profile.value
          val newExplorationXp = currentProf.explorationXp + questXpReward
          val newLevel = 1 + (newExplorationXp / 500)
          val updatedProfile = currentProf.copy(
            explorationXp = newExplorationXp,
            xp = newExplorationXp,
            level = newLevel,
            rewardedWaypointIds = candidateRewardedSet
          )
          try {
            // Atomic single write: both XP and rewarded set saved in CivicProfile
            localDraftStore.saveProfile(updatedProfile)
            try { localDraftStore.saveRewardedNodeIds(candidateRewardedSet) } catch (_: Exception) {}
            _rewardedNodeIds.add(questRewardId)
            _profile.value = updatedProfile
          } catch (e: Exception) {
            Log.e("CivicRepo", "Failed to persist quest reward: ${e.message}", e)
          }
        }
      }

      return localSub
    }

    val userToken = authManager.getValidAccessToken()
    val session = authManager.session.value

    // If unauthenticated: queue safely offline with clear sign-in required state
    if (userToken == null || session == null) {
      val offlineSub = CivicSubmission(
        id = clientSubmissionId,
        taskId = draft.taskId,
        taskTitle = draft.taskTitle,
        submittedAt = System.currentTimeMillis(),
        content = draft.notes,
        status = SubmissionStatus.QUEUED_OFFLINE,
        feedback = "Submission queued offline. Sign in with your Civic Registry account to sync.",
        scoreAwarded = null,
        isOfflineOrigin = true,
        isSandboxDemo = false,
        clientSubmissionId = clientSubmissionId,
        localPhotoUri = draft.photoUri,
        syncErrorMessage = "Sign in required"
      )
      localDraftStore.saveSubmission(offlineSub)
      _submissions.value = listOf(offlineSub) + _submissions.value.filterNot { it.id == offlineSub.id }
      return offlineSub
    }

    // Authenticated: attempt upload evidence first if photo attached
    var remoteStoragePath: String? = null
    if (!draft.photoUri.isNullOrBlank()) {
      val uploadResult = storageUploader.uploadEvidence(
        localUriString = draft.photoUri,
        userId = session.userId,
        taskId = draft.taskId,
        userAccessToken = userToken
      )
      when (uploadResult) {
        is StorageUploadResult.Success -> {
          remoteStoragePath = uploadResult.fullStorageKey
        }
        is StorageUploadResult.Failure -> {
          // If offline or network issue, queue offline and schedule sync
          val queuedSub = CivicSubmission(
            id = clientSubmissionId,
            taskId = draft.taskId,
            taskTitle = draft.taskTitle,
            submittedAt = System.currentTimeMillis(),
            content = draft.notes,
            status = if (uploadResult.isNetworkError) SubmissionStatus.QUEUED_OFFLINE else SubmissionStatus.PERMANENT_FAILURE,
            feedback = if (uploadResult.isNetworkError) {
              "Offline: Photo preserved in encrypted local storage. Will sync automatically."
            } else {
              "Evidence upload failed: ${uploadResult.message}"
            },
            scoreAwarded = null,
            isOfflineOrigin = true,
            isSandboxDemo = false,
            clientSubmissionId = clientSubmissionId,
            localPhotoUri = draft.photoUri,
            syncErrorMessage = uploadResult.message
          )
          localDraftStore.saveSubmission(queuedSub)
          _submissions.value = listOf(queuedSub) + _submissions.value.filterNot { it.id == queuedSub.id }
          if (uploadResult.isNetworkError) {
            SubmissionSyncWorker.enqueue(context)
          }
          return queuedSub
        }
      }
    }

    // Evidence uploaded or notes only: submit to backend
    val attachments = listOfNotNull(remoteStoragePath)
    val remoteResult = remoteDataSource.submitEvidence(
      taskId = draft.taskId,
      content = draft.notes,
      attachments = attachments,
      userAuthToken = userToken,
      clientSubmissionId = clientSubmissionId
    )

    val finalSubmission = when (remoteResult) {
      is RemoteResult.Success -> {
        remoteResult.data.copy(
          taskTitle = draft.taskTitle,
          isSandboxDemo = false,
          clientSubmissionId = clientSubmissionId,
          localPhotoUri = draft.photoUri,
          remoteStoragePath = remoteStoragePath
        )
      }
      is RemoteResult.Failure -> {
        val isRetryable = remoteResult.isOffline || (remoteResult.statusCode ?: 0) >= 500
        CivicSubmission(
          id = clientSubmissionId,
          taskId = draft.taskId,
          taskTitle = draft.taskTitle,
          submittedAt = System.currentTimeMillis(),
          content = draft.notes,
          status = if (isRetryable) SubmissionStatus.QUEUED_OFFLINE else SubmissionStatus.PERMANENT_FAILURE,
          feedback = if (isRetryable) {
            "Network unavailable. Stored in encrypted queue with idempotency token."
          } else {
            "Server validation rejected: ${remoteResult.message}"
          },
          scoreAwarded = null,
          isOfflineOrigin = true,
          isSandboxDemo = false,
          clientSubmissionId = clientSubmissionId,
          localPhotoUri = draft.photoUri,
          remoteStoragePath = remoteStoragePath,
          syncErrorMessage = remoteResult.message
        )
      }
    }

    localDraftStore.saveSubmission(finalSubmission)
    _submissions.value = listOf(finalSubmission) + _submissions.value.filterNot { it.id == finalSubmission.id }

    // Recalibrate verified contributions and credits
    val verifiedSubs = _submissions.value.filter { it.isVerified && !it.isSandboxDemo }
    val currentProf = _profile.value
    val newCivicCredits = verifiedSubs.sumOf { it.scoreAwarded ?: 0 }
    _profile.value = currentProf.copy(
      verifiedContributionsCount = verifiedSubs.size,
      verifiedCivicCredits = newCivicCredits,
      xp = currentProf.explorationXp
    )

    if (finalSubmission.status == SubmissionStatus.QUEUED_OFFLINE) {
      SubmissionSyncWorker.enqueue(context)
    }

    // Update active expedition node state
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

  override suspend fun retryPendingSubmissions() {
    SubmissionSyncWorker.enqueue(context)
    loadLocalStoreData()
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
      refreshRemoteData()
    }
    // Submissions, drafts, and verified civic profile contributions are explicitly untouched!
  }

  override suspend fun abandonExpedition(expeditionId: String) {
    val currentExp = _activeExpedition.value ?: return
    if (currentExp.id == expeditionId) {
      val abandonedExp = currentExp.copy(status = ExpeditionStatus.ABANDONED)
      _activeExpedition.value = abandonedExp
      _expeditions.value = _expeditions.value.map { if (it.id == expeditionId) abandonedExp else it }
    }
    // Submissions, drafts, and verified civic profile contributions are explicitly untouched!
  }

  override suspend fun recordWaypointDiscovery(nodeId: String, xpReward: Int) {
    // Anti-grind guard: each nodeId can only award XP once.
    // Sandbox demo nodes are also blocked — they must never increment production XP.
    if (_environmentMode.value == DataSourceMode.SANDBOX_DEMO) return
    if (_rewardedNodeIds.contains(nodeId)) {
      // nodeId was already rewarded — ignore duplicate call (repeated tap, sync retry, after restart)
      return
    }

    val candidateRewardedSet = _rewardedNodeIds + nodeId
    val currentProf = _profile.value
    val newExplorationXp = currentProf.explorationXp + xpReward
    val newDiscoveredCount = currentProf.discoveredWaypointsCount + 1
    val newLevel = 1 + (newExplorationXp / 500)
    val updatedProfile = currentProf.copy(
      explorationXp = newExplorationXp,
      discoveredWaypointsCount = newDiscoveredCount,
      xp = newExplorationXp,
      level = newLevel,
      rewardedWaypointIds = candidateRewardedSet
    )

    // Persist FIRST before mutating in-memory state.
    // Atomic single write: both XP and rewarded set saved in CivicProfile.
    // If saving fails (throws exception), in-memory state is unaffected and no XP is awarded.
    localDraftStore.saveProfile(updatedProfile)
    try { localDraftStore.saveRewardedNodeIds(candidateRewardedSet) } catch (_: Exception) {}

    _rewardedNodeIds.add(nodeId)
    _profile.value = updatedProfile
  }
}
