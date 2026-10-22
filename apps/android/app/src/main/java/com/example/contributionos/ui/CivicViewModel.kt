package com.example.contributionos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
  val syncStatusMessage: String? = null,
  val activeExpedition: CivicExpedition? = null,
  val tasks: List<CivicTask> = emptyList(),
  val drafts: List<MissionDraft> = emptyList(),
  val submissions: List<CivicSubmission> = emptyList(),
  val milestones: List<CommunityMilestone> = emptyList(),
  val profile: CivicProfile? = null,
  val errorMessage: String? = null
)

internal data class RepositoryBundle(
  val envMode: DataSourceMode,
  val isLive: Boolean,
  val syncMsg: String?,
  val expedition: CivicExpedition?,
  val tasks: List<CivicTask>,
  val drafts: List<MissionDraft>,
  val submissions: List<CivicSubmission>,
  val milestones: List<CommunityMilestone>,
  val profile: CivicProfile
)

class CivicViewModel(private val repository: CivicRepository) : ViewModel() {

  private val _isRefreshing = MutableStateFlow(false)
  val isRefreshing = _isRefreshing.asStateFlow()

  val uiState: StateFlow<CivicUiState> = combine(
    repository.environmentMode,
    repository.isLiveConnected,
    repository.syncStatusMessage,
    repository.activeExpedition,
    repository.tasks
  ) { envMode, isLive, syncMsg, expedition, tasks ->
    Tuple5(envMode, isLive, syncMsg, expedition, tasks)
  }.let { part1Flow ->
    combine(
      part1Flow,
      combine(
        repository.drafts,
        repository.submissions,
        repository.milestones,
        repository.profile
      ) { drafts, submissions, milestones, profile ->
        Tuple4(drafts, submissions, milestones, profile)
      }
    ) { part1, part2 ->
      CivicUiState(
        isLoading = false,
        isLiveConnected = part1.second,
        environmentMode = part1.first,
        syncStatusMessage = part1.third,
        activeExpedition = part1.fourth,
        tasks = part1.fifth,
        drafts = part2.first,
        submissions = part2.second,
        milestones = part2.third,
        profile = part2.fourth,
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
}

internal data class Tuple4<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
internal data class Tuple5<A, B, C, D, E>(val first: A, val second: B, val third: C, val fourth: D, val fifth: E)
