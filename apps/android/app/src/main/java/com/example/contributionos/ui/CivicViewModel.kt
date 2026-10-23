package com.example.contributionos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.contributionos.auth.AuthResult
import com.example.contributionos.auth.AuthSession
import com.example.contributionos.data.CivicRepository
import com.example.contributionos.data.DefaultCivicRepository
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.CommunityMilestone
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.model.MissionDraft
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CivicUiState(
  val isLoading: Boolean = false,
  val isLiveConnected: Boolean = false,
  val environmentMode: DataSourceMode = DataSourceMode.PRODUCTION,
  val authSession: AuthSession? = null,
  val syncStatusMessage: String? = null,
  val activeExpedition: CivicExpedition? = null,
  val tasks: List<CivicTask> = emptyList(),
  val drafts: List<MissionDraft> = emptyList(),
  val submissions: List<CivicSubmission> = emptyList(),
  val milestones: List<CommunityMilestone> = emptyList(),
  val profile: CivicProfile? = null,
  val errorMessage: String? = null
) {
  val isAuthenticated: Boolean get() = authSession != null
}

class CivicViewModel(private val repository: CivicRepository) : ViewModel() {

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing = _isRefreshing.asStateFlow()

  val uiState: StateFlow<CivicUiState> = combine(
    repository.environmentMode,
    repository.isLiveConnected,
    repository.authSession,
    repository.syncStatusMessage,
    repository.activeExpedition
  ) { envMode, isLive, authSession, syncMsg, expedition ->
    Tuple5(envMode, isLive, authSession, syncMsg, expedition)
  }.let { part1Flow ->
    combine(
      part1Flow,
      combine(
        repository.tasks,
        repository.drafts,
        repository.submissions,
        combine(repository.milestones, repository.profile) { ms, prof -> Pair(ms, prof) }
      ) { tasks, drafts, submissions, pair ->
        Tuple4(tasks, drafts, submissions, pair)
      }
    ) { part1, part2 ->
      CivicUiState(
        isLoading = false,
        environmentMode = part1.first,
        isLiveConnected = part1.second,
        authSession = part1.third,
        syncStatusMessage = part1.fourth,
        activeExpedition = part1.fifth,
        tasks = part2.first,
        drafts = part2.second,
        submissions = part2.third,
        milestones = part2.fourth.first,
        profile = part2.fourth.second,
        errorMessage = null
      )
    }
  }.stateIn(
    scope = viewModelScope,
    started = SharingStarted.WhileSubscribed(5000),
    initialValue = CivicUiState(isLoading = true)
  )

  init {
    if (repository is DefaultCivicRepository) {
      viewModelScope.launch {
        repository.initialize()
      }
    }
  }

  fun setEnvironmentMode(mode: DataSourceMode) {
    viewModelScope.launch {
      repository.setEnvironmentMode(mode)
    }
  }

  fun refresh() {
    viewModelScope.launch {
      _isRefreshing.value = true
      try {
        repository.refreshRemoteData()
      } finally {
        _isRefreshing.value = false
      }
    }
  }

  fun signIn(email: String, pass: String, onResult: (AuthResult<AuthSession>) -> Unit) {
    viewModelScope.launch {
      val result = repository.signIn(email, pass)
      onResult(result)
    }
  }

  fun signUp(email: String, pass: String, onResult: (AuthResult<AuthSession>) -> Unit) {
    viewModelScope.launch {
      val result = repository.signUp(email, pass)
      onResult(result)
    }
  }

  fun signOut() {
    viewModelScope.launch {
      repository.signOut()
    }
  }

  fun retryPendingSubmissions() {
    viewModelScope.launch {
      repository.retryPendingSubmissions()
    }
  }

  fun saveDraft(draft: MissionDraft) {
    viewModelScope.launch {
      repository.saveDraft(draft)
    }
  }

  fun deleteDraft(draftId: String) {
    viewModelScope.launch {
      repository.deleteDraft(draftId)
    }
  }

  fun submitMission(draft: MissionDraft, onComplete: (CivicSubmission) -> Unit) {
    viewModelScope.launch {
      val submission = repository.submitMission(draft)
      onComplete(submission)
    }
  }

  fun resetRun(expeditionId: String) {
    viewModelScope.launch {
      repository.resetRun(expeditionId)
    }
  }

  fun abandonExpedition(expeditionId: String) {
    viewModelScope.launch {
      repository.abandonExpedition(expeditionId)
    }
  }

  fun recordWaypointDiscovery(nodeId: String, xpReward: Int) {
    viewModelScope.launch {
      repository.recordWaypointDiscovery(nodeId, xpReward)
    }
  }

  fun selectExpedition(expeditionId: String) {
    viewModelScope.launch {
      repository.selectExpedition(expeditionId)
    }
  }

  fun advanceExpeditionNode(expeditionId: String, completedNodeId: String) {
    viewModelScope.launch {
      repository.advanceExpeditionNode(expeditionId, completedNodeId)
    }
  }

  fun completeExpedition(expeditionId: String, bonusXp: Int = 100) {
    viewModelScope.launch {
      repository.completeExpedition(expeditionId, bonusXp)
    }
  }

  fun setExpeditionStance(stance: String) {
    viewModelScope.launch {
      repository.setExpeditionStance(stance)
    }
  }

  fun startExpeditionRun(expeditionId: String, stance: String = "TREK") {
    viewModelScope.launch {
      repository.startExpeditionRun(expeditionId, stance)
    }
  }

  fun commitToBranch(expeditionId: String, targetNodeId: String) {
    viewModelScope.launch {
      repository.commitToBranch(expeditionId, targetNodeId)
    }
  }

  fun resolveEncounterChoice(
    expeditionId: String,
    nodeId: String,
    choiceId: String,
    onComplete: (Boolean) -> Unit = {}
  ) {
    viewModelScope.launch {
      val isRunCompleted = repository.resolveEncounterChoice(expeditionId, nodeId, choiceId)
      onComplete(isRunCompleted)
    }
  }
}

internal data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
internal data class Tuple5<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
