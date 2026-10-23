package com.example.contributionos

import com.example.contributionos.data.SeedMissionCatalog
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.model.ExpeditionStatus
import com.example.contributionos.model.MissionDraft
import com.example.contributionos.model.NodeStatus
import com.example.contributionos.model.SubmissionStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CivicRogueliteTest {

  @Test
  fun testDeterministicExpeditionGeneration() {
    val expA = SeedMissionCatalog.generateExpeditions(seed = 42L)
    val expB = SeedMissionCatalog.generateExpeditions(seed = 42L)

    assertEquals("Expected identical expedition count", expA.size, expB.size)
    assertEquals("Expected matching expedition title", expA[0].title, expB[0].title)
    assertEquals("Expected matching nodes count", expA[0].nodes.size, expB[0].nodes.size)
    assertEquals("Expected matching seed", expA[0].activeRunSeed, expB[0].activeRunSeed)

    for (i in expA[0].nodes.indices) {
      val nodeA = expA[0].nodes[i]
      val nodeB = expB[0].nodes[i]
      assertEquals("Node ID at $i should match", nodeA.id, nodeB.id)
      assertEquals("Node status at $i should match", nodeA.status, nodeB.status)
      assertEquals("Node isoX at $i should match", nodeA.isoX, nodeB.isoX, 0.001f)
      assertEquals("Node isoY at $i should match", nodeA.isoY, nodeB.isoY, 0.001f)
    }
  }

  @Test
  fun testDifferentSeedsProduceDifferentSeeds() {
    val expA = SeedMissionCatalog.generateExpeditions(seed = 42L)
    val expB = SeedMissionCatalog.generateExpeditions(seed = 99L)

    assertEquals(expA[0].activeRunSeed, 42L)
    assertEquals(expB[0].activeRunSeed, 99L)
  }

  @Test
  fun testSandboxDataSeparationAndLabels() {
    val tasks = SeedMissionCatalog.realTasks
    assertTrue("Demo tasks catalog should have items", tasks.isNotEmpty())

    tasks.forEach { task ->
      assertTrue("Task ${task.id} must be marked as sandbox demo", task.isSandboxDemo)
      assertTrue("Task title must be explicitly prefixed with [DEMO]", task.title.startsWith("[DEMO]"))
      assertTrue("Task difficulty must be between 1 and 5", task.difficulty in 1..5)
      assertTrue("Task description must have substance", task.description.length > 10)
    }

    val milestones = SeedMissionCatalog.realMilestones
    milestones.forEach { ms ->
      assertTrue("Milestone ${ms.id} must be marked as sandbox demo", ms.isSandboxDemo)
      assertTrue("Milestone title must be prefixed with [DEMO]", ms.title.startsWith("[DEMO]"))
    }

    val profile = SeedMissionCatalog.defaultProfile
    assertTrue("Demo profile must be flagged as sandbox demo", profile.isSandboxDemo)
    assertTrue("Demo profile title must include Sandbox", profile.rankTitle.contains("Sandbox"))

    val expeditions = SeedMissionCatalog.generateExpeditions()
    expeditions.forEach { exp ->
      assertTrue("Expedition must be flagged as sandbox demo", exp.isSandboxDemo)
      assertTrue("Expedition title must be prefixed with [DEMO]", exp.title.startsWith("[DEMO]"))
      exp.nodes.forEach { node ->
        assertTrue("Node must be flagged as sandbox demo", node.isSandboxDemo)
      }
    }
  }

  @Test
  fun testSubmissionStatusLifecycleIntegrity() {
    // Verify all submission lifecycle states are present and distinct
    val statuses = SubmissionStatus.values()
    assertTrue(statuses.contains(SubmissionStatus.PENDING_REVIEW))
    assertTrue(statuses.contains(SubmissionStatus.VALIDATED))
    assertTrue(statuses.contains(SubmissionStatus.CHANGES_REQUESTED))
    assertTrue(statuses.contains(SubmissionStatus.REJECTED))
    assertTrue(statuses.contains(SubmissionStatus.QUEUED_OFFLINE))

    val offlineSubmission = CivicSubmission(
      id = "sub-test-offline",
      taskId = "task-1",
      taskTitle = "Local Street Tree Health Assessment",
      submittedAt = System.currentTimeMillis(),
      content = "Recorded trunk circumferences.",
      status = SubmissionStatus.QUEUED_OFFLINE,
      feedback = "Queued safely in local storage",
      scoreAwarded = null,
      isOfflineOrigin = true,
      isSandboxDemo = false,
      remoteSubmissionId = null,
      syncErrorMessage = "Server unreachable"
    )

    assertTrue(offlineSubmission.isOfflineOrigin)
    assertFalse(offlineSubmission.isSandboxDemo)
    assertEquals(SubmissionStatus.QUEUED_OFFLINE, offlineSubmission.status)
    assertNull(offlineSubmission.scoreAwarded)
  }

  @Test
  fun testRealEvidenceCoordinatesPreservation() {
    // Ensure no hardcoding of coordinates in model
    val realLat = 37.7749
    val realLon = -122.4194

    val draft = MissionDraft(
      id = "draft-real-gps",
      taskId = "task-water-1",
      taskTitle = "Creek Hydrology Testing",
      notes = "Sample pH measured at 7.2.",
      photoUri = "file:///data/user/0/com.example.contributionos/cache/evidence/photo_123.jpg",
      latitude = realLat,
      longitude = realLon,
      isOfflinePending = true,
      isSandboxDemo = false
    )

    assertEquals(realLat, draft.latitude!!, 0.0001)
    assertEquals(realLon, draft.longitude!!, 0.0001)
    assertFalse(draft.isSandboxDemo)
    assertTrue(draft.isOfflinePending)
  }

  @Test
  fun testEnvironmentModeValues() {
    assertEquals(2, DataSourceMode.values().size)
    assertTrue(DataSourceMode.values().contains(DataSourceMode.PRODUCTION))
    assertTrue(DataSourceMode.values().contains(DataSourceMode.SANDBOX_DEMO))
  }

  @Test
  fun testTouchGrassBrandShellIntegrity() {
    val profile = SeedMissionCatalog.defaultProfile
    assertTrue("Demo profile rank must reflect meme-native identity", profile.rankTitle.contains("Actually Useful Human"))
    assertTrue("Demo profile rank must retain Sandbox distinction", profile.rankTitle.contains("Sandbox"))
    assertEquals("Touch Grass Explorer (Demo)", profile.displayName)
    assertEquals("Grasshopper-01", profile.callsign)

    val firstBadge = profile.badges.first()
    assertEquals("Touched real grass for the first time.", firstBadge.description)
    assertEquals("🌱", firstBadge.iconGlyph)
  }

  @Test
  fun testAppDestinationNavigationLabels() {
    val destinations = com.example.contributionos.ui.components.AppDestination.values()
    assertEquals(5, destinations.size)
    assertEquals("Camp", com.example.contributionos.ui.components.AppDestination.HOME.title)
    assertEquals("Map", com.example.contributionos.ui.components.AppDestination.MAP.title)
    assertEquals("Quests", com.example.contributionos.ui.components.AppDestination.MISSIONS.title)
    assertEquals("Impact", com.example.contributionos.ui.components.AppDestination.IMPACT.title)
    assertEquals("Profile", com.example.contributionos.ui.components.AppDestination.PROFILE.title)
  }

  @Test
  fun testExpeditionDossierIdentifierFormatting() {
    assertEquals("SQ-01", com.example.contributionos.ui.screens.formatDossierId("task-1"))
    assertEquals("SQ-12", com.example.contributionos.ui.screens.formatDossierId("task-12"))
    assertEquals("SQ-01", com.example.contributionos.ui.screens.formatDossierId("no-digits"))
    assertEquals("SQ-01", com.example.contributionos.ui.screens.formatDossierId(""))
    assertEquals("SQ-01", com.example.contributionos.ui.screens.formatDossierId("self-guided-groundwork-1"))
    assertEquals("SQ-02", com.example.contributionos.ui.screens.formatDossierId("self-guided-bioswale-2"))
    assertEquals("SQ-99", com.example.contributionos.ui.screens.formatDossierId("task-999"))
  }

  @Test
  fun testTaskModelAuthoritativeXpReward() {
    val defaultTask = CivicTask(
      id = "test-task-default",
      title = "Test Default XP Task",
      description = "Verifying default 50 XP reward",
      difficulty = 1,
      status = "open"
    )
    assertEquals("Default task xpReward must be 50", 50, defaultTask.xpReward)

    val customTask = defaultTask.copy(xpReward = 100)
    assertEquals("Custom task xpReward must be preserved", 100, customTask.xpReward)

    // Check all self-guided starter quests in catalog have authoritative xpReward
    SeedMissionCatalog.selfGuidedQuests.forEach { quest ->
      assertEquals("Self-guided quest ${quest.id} must offer 50 XP", 50, quest.xpReward)
    }
  }
}
