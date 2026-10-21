package com.example.contributionos.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.contributionos.data.CivicRepository
import com.example.contributionos.data.DefaultCivicRepository
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.CommunityMilestone
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
  val activeExpedition: CivicExpedition? = null,
  val tasks: List<CivicTask> = emptyList(),
  val drafts: List<MissionDraft> = emptyList(),
  val submissions: List<CivicSubmission> = emptyList(),
  val milestones: List<CommunityMilestone> = emptyList(),
  val profile: CivicProfile? = null,
  val errorMessage: String? = null
)

internal data class Quadruple<A, B, C, D>(
  val first: A,
  val second: B,
  val third: C,
  val fourth: D
)

class CivicViewModel(private val repository: CivicRepository) : ViewModel() {

  val uiState: StateFlow<CivicUiState> = combine(
    repository.isLiveConnected,
    repository.activeExpedition,
    repository.tasks,
    combine(
      repository.drafts,
      repository.submissions,
      repository.milestones,
      repository.profile
    ) { drafts, submissions, milestones, profile ->
      Quadruple(drafts, submissions, milestones, profile)
    }
  ) { isLive, expedition, tasks, quad ->
    CivicUiState(
      isLoading = false,
      isLiveConnected = isLive,
      activeExpedition = expedition,
      tasks = tasks,
      drafts = quad.first,
      submissions = quad.second,
      milestones = quad.third,
      profile = quad.fourth,
      errorMessage = null
    )
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
