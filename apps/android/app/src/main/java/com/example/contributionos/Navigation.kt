package com.example.contributionos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.contributionos.data.DefaultCivicRepository
import com.example.contributionos.data.SeedMissionCatalog
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.model.NodeStatus
import com.example.contributionos.theme.CivicForestDark
import com.example.contributionos.ui.CivicViewModel
import com.example.contributionos.ui.components.AppDestination
import com.example.contributionos.ui.components.CivicBottomNav
import com.example.contributionos.ui.screens.DiscoveryEncounterScreen
import com.example.contributionos.ui.screens.EvidenceCaptureScreen
import com.example.contributionos.ui.screens.ExpeditionCompleteScreen
import com.example.contributionos.ui.screens.ExpeditionDetailScreen
import com.example.contributionos.ui.screens.FieldJournalScreen
import com.example.contributionos.ui.screens.HomeScreen
import com.example.contributionos.ui.screens.ImpactScreen
import com.example.contributionos.ui.screens.MapScreen
import com.example.contributionos.ui.screens.MissionDetailScreen
import com.example.contributionos.ui.screens.MissionsScreen
import com.example.contributionos.ui.screens.ProfileScreen
import com.example.contributionos.ui.screens.SubmissionStatusScreen

@Composable
fun MainNavigation() {
  val context = LocalContext.current
  val repository = DefaultCivicRepository(context)
  val viewModel: CivicViewModel = viewModel { CivicViewModel(repository) }
  val uiState by viewModel.uiState.collectAsStateWithLifecycle()

  val backStack = rememberNavBackStack(NavHome)
  val currentKey = backStack.lastOrNull() ?: NavHome

  val currentDestination = when (currentKey) {
    is NavHome -> AppDestination.HOME
    is NavMap -> AppDestination.MAP
    is NavMissions -> AppDestination.MISSIONS
    is NavJournal -> AppDestination.JOURNAL
    is NavProfile -> AppDestination.PROFILE
    else -> AppDestination.HOME
  }

  val showBottomNav = (currentKey is NavHome ||
    currentKey is NavMap ||
    currentKey is NavMissions ||
    currentKey is NavJournal ||
    currentKey is NavProfile) &&
    !(currentKey is NavDiscoveryEncounter ||
      currentKey is NavExpeditionComplete ||
      currentKey is NavEvidenceCapture ||
      currentKey is NavMissionDetails)

  val toggleMode = {
    val newMode = if (uiState.environmentMode == DataSourceMode.PRODUCTION) {
      DataSourceMode.SANDBOX_DEMO
    } else {
      DataSourceMode.PRODUCTION
    }
    viewModel.setEnvironmentMode(newMode)
  }

  Scaffold(
    bottomBar = {
      if (showBottomNav) {
        CivicBottomNav(
          currentDestination = currentDestination,
          onDestinationSelected = { dest ->
            val targetKey = when (dest) {
              AppDestination.HOME -> NavHome
              AppDestination.MAP -> NavMap
              AppDestination.MISSIONS -> NavMissions
              AppDestination.JOURNAL -> NavJournal
              AppDestination.PROFILE -> NavProfile
            }
            if (currentKey != targetKey) {
              backStack.clear()
              backStack.add(targetKey)
            }
          },
          draftCount = uiState.drafts.size
        )
      }
    },
    containerColor = CivicForestDark
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(CivicForestDark)
        .padding(paddingValues)
    ) {
      val navigateBackFromCompletion: () -> Unit = {
        while (backStack.size > 1 && (
            backStack.lastOrNull() is NavSubmissionStatus ||
            backStack.lastOrNull() is NavEvidenceCapture ||
            backStack.lastOrNull() is NavMissionDetails
        )) {
          backStack.removeLastOrNull()
        }
        if (backStack.isEmpty()) {
          backStack.add(NavHome)
        }
      }

      NavDisplay(
        backStack = backStack,
        onBack = {
          if (backStack.lastOrNull() is NavSubmissionStatus) {
            navigateBackFromCompletion()
          } else if (backStack.size > 1) {
            backStack.removeLastOrNull()
          }
        },
        entryProvider = entryProvider {
          entry<NavHome> {
            val exp = uiState.activeExpedition
            val prof = uiState.profile
            if (prof != null) {
              HomeScreen(
                expedition = exp,
                profile = prof,
                featuredMilestone = uiState.milestones.firstOrNull(),
                isLive = uiState.isLiveConnected,
                environmentMode = uiState.environmentMode,
                onToggleMode = toggleMode,
                onExploreMap = { backStack.add(NavMap) },
                onOpenExpeditionDetails = { backStack.add(NavExpeditionDetails) },
                onSelectNode = { node ->
                  backStack.add(NavMissionDetails(taskId = node.taskId, nodeId = node.id))
                },
                onGoToMissions = {
                  backStack.clear()
                  backStack.add(NavMissions)
                },
                availableTasks = uiState.tasks,
                onSelectTask = { task ->
                  backStack.add(NavMissionDetails(taskId = task.id))
                }
              )
            }
          }

          entry<NavMap> {
            MapScreen(
              expedition = uiState.activeExpedition,
              rewardedWaypointIds = uiState.profile?.rewardedWaypointIds ?: emptySet(),
              isLive = uiState.isLiveConnected,
              environmentMode = uiState.environmentMode,
              onToggleMode = toggleMode,
              onNodeSelected = { node ->
                backStack.add(NavMissionDetails(taskId = node.taskId, nodeId = node.id))
              },
              onSelectQuest = { taskId ->
                backStack.add(NavMissionDetails(taskId = taskId))
              },
              onRecordDiscovery = { waypointId, xpReward ->
                viewModel.recordWaypointDiscovery(waypointId, xpReward)
              },
              onTriggerEncounter = { expId, nodeId ->
                backStack.add(NavDiscoveryEncounter(expeditionId = expId, nodeId = nodeId))
              },
              onToggleStance = { stance ->
                viewModel.setExpeditionStance(stance)
              }
            )
          }

          entry<NavExpeditionDetails> {
            val exp = uiState.activeExpedition
            if (exp != null) {
              ExpeditionDetailScreen(
                expedition = exp,
                onNodeClick = { node ->
                  backStack.add(NavMissionDetails(taskId = node.taskId, nodeId = node.id))
                },
                onResetRun = {
                  viewModel.resetRun(exp.id)
                },
                onAbandonRun = {
                  viewModel.abandonExpedition(exp.id)
                  backStack.removeLastOrNull()
                },
                onBack = { backStack.removeLastOrNull() }
              )
            }
          }

          entry<NavMissionDetails> { key ->
            val task = uiState.tasks.find { it.id == key.taskId }
              ?: SeedMissionCatalog.findTask(key.taskId)
              ?: uiState.activeExpedition?.nodes?.find { it.taskId == key.taskId }?.let { node ->
                com.example.contributionos.model.CivicTask(
                  id = node.taskId,
                  title = node.title,
                  description = "Complete field survey and audit requirements for this waypoint node.",
                  difficulty = node.difficulty,
                  status = "open",
                  tags = listOf(node.category.lowercase()),
                  district = uiState.activeExpedition?.district ?: "Civic Core",
                  isSandboxDemo = node.isSandboxDemo
                )
              }

            val node = uiState.activeExpedition?.nodes?.find { it.id == key.nodeId || it.taskId == key.taskId }

            if (task != null) {
              MissionDetailScreen(
                task = task,
                node = node,
                onCaptureEvidence = {
                  backStack.add(NavEvidenceCapture(taskId = task.id))
                },
                onBack = { backStack.removeLastOrNull() }
              )
            }
          }

          entry<NavEvidenceCapture> { key ->
            val task = uiState.tasks.find { it.id == key.taskId }
              ?: SeedMissionCatalog.findTask(key.taskId)
              ?: uiState.activeExpedition?.nodes?.find { it.taskId == key.taskId }?.let { node ->
                com.example.contributionos.model.CivicTask(
                  id = node.taskId,
                  title = node.title,
                  description = "Field verification task.",
                  difficulty = node.difficulty,
                  status = "open",
                  isSandboxDemo = node.isSandboxDemo
                )
              }

            val existingDraft = if (key.draftId != null) {
              uiState.drafts.find { it.id == key.draftId }
            } else {
              uiState.drafts.find { it.taskId == key.taskId }
            }

            if (task != null) {
              EvidenceCaptureScreen(
                task = task,
                existingDraft = existingDraft,
                isAuthenticated = uiState.isAuthenticated,
                isProduction = uiState.environmentMode == DataSourceMode.PRODUCTION,
                onSaveDraft = { draft ->
                  viewModel.saveDraft(draft)
                },
                onSubmit = { draft ->
                  viewModel.submitMission(draft) { submission ->
                    val activeExp = uiState.activeExpedition
                    val matchingNode = activeExp?.nodes?.find { it.taskId == task.id }
                    if (activeExp != null && matchingNode != null) {
                      viewModel.recordWaypointDiscovery(matchingNode.id, 50)
                      viewModel.advanceExpeditionNode(activeExp.id, matchingNode.id)
                      val updatedNodes = activeExp.nodes.map {
                        if (it.id == matchingNode.id) it.copy(status = NodeStatus.VALIDATED) else it
                      }
                      if (updatedNodes.all { it.status == NodeStatus.VALIDATED }) {
                        backStack.add(NavExpeditionComplete(activeExp.id))
                        return@submitMission
                      }
                    }
                    backStack.add(NavSubmissionStatus(submission.id))
                  }
                },
                onBack = { backStack.removeLastOrNull() }
              )
            }
          }

          entry<NavDiscoveryEncounter> { key ->
            val exp = uiState.activeExpedition?.takeIf { it.id == key.expeditionId }
            if (exp == null) {
              backStack.removeLastOrNull()
              return@entry
            }
            val node = exp.nodes.find { it.id == key.nodeId }
              ?: exp.nodes.firstOrNull()

            if (node == null || node.status == NodeStatus.LOCKED) {
              backStack.removeLastOrNull()
              return@entry
            }

            val specimen = SeedMissionCatalog.catalogSpecimens.find { sp ->
              sp.linkedWaypointId == node.id ||
              sp.linkedWaypointId == node.taskId.replace("task-", "wp-") ||
              node.taskId.contains(sp.id.removePrefix("sp-")) ||
              node.title.contains(sp.name)
            } ?: SeedMissionCatalog.catalogSpecimens.firstOrNull()

            val discoveredClues = uiState.profile?.activeRun?.discoveredClueIds ?: emptySet()

            DiscoveryEncounterScreen(
              expedition = exp,
              node = node,
              specimen = specimen,
              discoveredClueIds = discoveredClues,
              onCommitChoice = { choiceId ->
                viewModel.resolveEncounterChoice(exp.id, node.id, choiceId) { isRunCompleted ->
                  backStack.removeLastOrNull()
                  if (isRunCompleted) {
                    backStack.add(NavExpeditionComplete(exp.id))
                  }
                }
              },
              onBack = { backStack.removeLastOrNull() }
            )
          }

          entry<NavExpeditionComplete> { key ->
            val exp = uiState.activeExpedition?.takeIf { it.id == key.expeditionId }
            if (exp == null) {
              backStack.clear()
              backStack.add(NavHome)
              return@entry
            }
            ExpeditionCompleteScreen(
              expedition = exp,
              specimens = SeedMissionCatalog.catalogSpecimens,
              totalXpEarned = 250,
              onReturnHome = {
                backStack.clear()
                backStack.add(NavHome)
              },
              onViewJournal = {
                backStack.clear()
                backStack.add(NavJournal)
              }
            )
          }

          entry<NavJournal> {
            FieldJournalScreen(
              specimens = SeedMissionCatalog.catalogSpecimens,
              rewardedWaypointIds = uiState.profile?.rewardedWaypointIds ?: emptySet(),
              chronicledSpecimenIds = uiState.profile?.chronicledSpecimenIds ?: emptySet()
            )
          }

          entry<NavMissions> {
            MissionsScreen(
              drafts = uiState.drafts,
              activeNodes = uiState.activeExpedition?.nodes ?: emptyList(),
              submissions = uiState.submissions,
              tasks = uiState.tasks,
              isLive = uiState.isLiveConnected,
              environmentMode = uiState.environmentMode,
              onToggleMode = toggleMode,
              onResumeDraft = { draft ->
                backStack.add(NavEvidenceCapture(taskId = draft.taskId, draftId = draft.id))
              },
              onDeleteDraft = { draftId ->
                viewModel.deleteDraft(draftId)
              },
              onSelectNode = { node ->
                backStack.add(NavMissionDetails(taskId = node.taskId, nodeId = node.id))
              },
              onSelectTask = { task ->
                backStack.add(NavMissionDetails(taskId = task.id))
              },
              onSelectSubmission = { sub ->
                backStack.add(NavSubmissionStatus(sub.id))
              }
            )
          }

          entry<NavImpact> {
            ImpactScreen(
              milestones = uiState.milestones,
              submissions = uiState.submissions,
              isLive = uiState.isLiveConnected,
              environmentMode = uiState.environmentMode,
              onToggleMode = toggleMode
            )
          }

          entry<NavProfile> {
            val prof = uiState.profile
            if (prof != null) {
              ProfileScreen(
                profile = prof,
                draftCount = uiState.drafts.size,
                submissionCount = uiState.submissions.size,
                isLive = uiState.isLiveConnected,
                environmentMode = uiState.environmentMode,
                onSetEnvironmentMode = { mode -> viewModel.setEnvironmentMode(mode) },
                authSession = uiState.authSession,
                onSignIn = { email, pass, cb -> viewModel.signIn(email, pass, cb) },
                onSignUp = { email, pass, cb -> viewModel.signUp(email, pass, cb) },
                onSignOut = { viewModel.signOut() },
                onRetryPending = { viewModel.retryPendingSubmissions() },
                onViewImpact = { backStack.add(NavImpact) }
              )
            }
          }

          entry<NavSubmissionStatus> { key ->
            val submission = uiState.submissions.find { it.id == key.submissionId }
              ?: uiState.submissions.firstOrNull()
            val task = submission?.let { sub ->
              uiState.tasks.find { it.id == sub.taskId }
                ?: com.example.contributionos.data.SeedMissionCatalog.findTask(sub.taskId)
            }

            BackHandler {
              navigateBackFromCompletion()
            }

            if (submission != null) {
              SubmissionStatusScreen(
                submission = submission,
                task = task,
                onReturnHome = {
                  backStack.clear()
                  backStack.add(NavHome)
                },
                onViewMissions = {
                  backStack.clear()
                  backStack.add(NavMissions)
                },
                onRetry = {
                  viewModel.retryPendingSubmissions()
                }
              )
            }
          }
        }
      )
    }
  }
}
