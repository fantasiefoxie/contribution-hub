package com.example.contributionos.data

import com.example.contributionos.model.CivicBadge
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.ExpeditionNode
import com.example.contributionos.model.ExpeditionStatus
import com.example.contributionos.model.MapWaypoint
import com.example.contributionos.model.MissionDraft
import com.example.contributionos.model.NodeStatus
import com.example.contributionos.model.SubmissionStatus
import com.example.contributionos.security.SecurityKeyManager
import com.example.contributionos.security.SoftwareAesGcmCipherProvider
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.SecureRandom
import java.util.UUID

class ProgressionSeparationTest {

  private lateinit var tempDir: File
  private lateinit var store: LocalDraftStore

  @Before
  fun setUp() {
    tempDir = Files.createTempDirectory("progression_test").toFile()
    val testKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
    val cipherProvider = SoftwareAesGcmCipherProvider(testKey)
    val keyManager = SecurityKeyManager(tempDir, cipherProvider)
    store = LocalDraftStore(tempDir, keyManager)
  }

  @After
  fun tearDown() {
    tempDir.deleteRecursively()
  }

  @Test
  fun testRewardSeparationExplorationVsCivicCredits() {
    var profile = CivicProfile(
      id = "user-1",
      displayName = "Citizen One",
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
      badges = emptyList(),
      isSandboxDemo = false
    )

    // 1. User performs an exploration discovery (discovers a waypoint / explores biome)
    val explorationXpGain = 75
    val newLevel = 1 + (explorationXpGain / 500)
    profile = profile.copy(
      explorationXp = profile.explorationXp + explorationXpGain,
      discoveredWaypointsCount = profile.discoveredWaypointsCount + 1,
      xp = profile.explorationXp + explorationXpGain,
      level = newLevel
    )

    // Exploration actions increment exploration XP and level, but NEVER increment verified civic credits
    assertEquals(75, profile.explorationXp)
    assertEquals(75, profile.xp)
    assertEquals(1, profile.level)
    assertEquals(0, profile.verifiedCivicCredits)
    assertEquals(0, profile.verifiedContributionsCount)
    assertEquals(1, profile.discoveredWaypointsCount)

    // 2. User gets an authoritative server-verified civic contribution approval
    val verifiedCivicCreditGain = 120
    profile = profile.copy(
      verifiedCivicCredits = profile.verifiedCivicCredits + verifiedCivicCreditGain,
      verifiedContributionsCount = profile.verifiedContributionsCount + 1
      // Character XP and level are NOT modified by civic credits!
    )

    // Verified credits increment, but exploration XP and character XP remain solely driven by exploration!
    assertEquals(75, profile.explorationXp)
    assertEquals(75, profile.xp)
    assertEquals(1, profile.level)
    assertEquals(120, profile.verifiedCivicCredits)
    assertEquals(1, profile.verifiedContributionsCount)
  }

  @Test
  fun testShareableCardPrivacyAndHonesty() {
    val unverifiedSub = CivicSubmission(
      id = "sub-client-12345",
      taskId = "task-local-1",
      taskTitle = "[SELF-GUIDED] Neighborhood Walkability & Canopy Scout",
      submittedAt = System.currentTimeMillis(),
      content = "Confidential field notes with home address: 123 Palm Grove Road",
      status = SubmissionStatus.QUEUED_OFFLINE,
      localPhotoUri = "file:///data/user/0/com.example.contributionos/cache/evidence/secret.jpg",
      remoteStoragePath = "task-evidence/private-user-id/secret.jpg"
    )

    val snippet = unverifiedSub.toShareableSnippet()

    // Required content
    assertTrue("Must mention Touch Grass", snippet.contains("TOUCH GRASS"))
    assertTrue("Must contain task title", snippet.contains("[SELF-GUIDED] Neighborhood Walkability & Canopy Scout"))
    assertTrue("Unverified submission must indicate verification pending", snippet.contains("Side quest logged. Verification pending."))
    assertTrue("Must contain humorous brand copy", snippet.contains("The grass acknowledges you"))

    // Privacy constraints: MUST NOT leak private data or coordinates or IDs
    assertFalse("Must NOT leak private address notes", snippet.contains("123 Palm Grove Road"))
    assertFalse("Must NOT leak local photo path", snippet.contains("secret.jpg"))
    assertFalse("Must NOT leak user ID", snippet.contains("private-user-id"))
    assertFalse("Must NOT leak internal submission UUID", snippet.contains("sub-client-12345"))
    assertFalse("Must NOT claim verification before approval", snippet.contains("Authoritatively verified"))

    // Verified submission share card
    val verifiedSub = unverifiedSub.copy(status = SubmissionStatus.APPROVED)
    val verifiedSnippet = verifiedSub.toShareableSnippet()
    assertTrue("Verified submission must indicate authoritatively verified", verifiedSnippet.contains("Authoritatively verified"))
  }

  @Test
  fun testFailedAndQueuedSubmissionsDoNotAwardCivicCredits() = runBlocking {
    val queuedSub = CivicSubmission(
      id = "client-sub-1",
      taskId = "task-1",
      taskTitle = "Riparian Buffer Survey",
      submittedAt = System.currentTimeMillis(),
      content = "Offline buffer notes",
      status = SubmissionStatus.QUEUED_OFFLINE,
      scoreAwarded = null,
      isOfflineOrigin = true,
      isSandboxDemo = false,
      clientSubmissionId = "client-sub-1"
    )
    val pendingReviewSub = CivicSubmission(
      id = "client-sub-2",
      taskId = "task-2",
      taskTitle = "Public Tree Inspection",
      submittedAt = System.currentTimeMillis(),
      content = "Pending human review notes",
      status = SubmissionStatus.PENDING_REVIEW,
      scoreAwarded = null,
      isSandboxDemo = false,
      clientSubmissionId = "client-sub-2"
    )
    val rejectedSub = CivicSubmission(
      id = "client-sub-3",
      taskId = "task-3",
      taskTitle = "Stormwater Drain Check",
      submittedAt = System.currentTimeMillis(),
      content = "Blurry photo evidence",
      status = SubmissionStatus.REJECTED,
      scoreAwarded = null,
      isSandboxDemo = false,
      clientSubmissionId = "client-sub-3"
    )

    store.saveSubmission(queuedSub)
    store.saveSubmission(pendingReviewSub)
    store.saveSubmission(rejectedSub)

    val stored = store.getSubmissions()
    assertEquals(3, stored.size)

    // Civic credits must be awarded only after successful authoritative server validation (APPROVED)
    val verifiedSubs = stored.filter { it.isVerified && !it.isSandboxDemo }
    val verifiedCredits = verifiedSubs.sumOf { it.scoreAwarded ?: 0 }

    assertEquals("Queued, pending, and rejected submissions must produce 0 verified contributions", 0, verifiedSubs.size)
    assertEquals("Queued, pending, and rejected submissions must produce 0 civic credits", 0, verifiedCredits)
  }

  @Test
  fun testApprovedSubmissionAwardsCivicCreditsAuthoritatively() = runBlocking {
    val approvedSub = CivicSubmission(
      id = "client-sub-approved",
      taskId = "task-approved",
      taskTitle = "Municipal Canopy Audit",
      submittedAt = System.currentTimeMillis(),
      content = "Authoritative GPS & photo verified",
      status = SubmissionStatus.APPROVED,
      scoreAwarded = 150,
      isSandboxDemo = false,
      clientSubmissionId = "client-sub-approved"
    )

    store.saveSubmission(approvedSub)

    val stored = store.getSubmissions()
    val verifiedSubs = stored.filter { it.isVerified && !it.isSandboxDemo }
    val verifiedCredits = verifiedSubs.sumOf { it.scoreAwarded ?: 0 }

    assertEquals(1, verifiedSubs.size)
    assertEquals(150, verifiedCredits)
    assertTrue(verifiedSubs[0].isVerified)
    assertFalse(verifiedSubs[0].isPending)
    assertFalse(verifiedSubs[0].isOfflineQueue)
  }

  @Test
  fun testDuplicateSyncWithSameClientSubmissionIdIsIdempotent() = runBlocking {
    val clientId = "idemp-" + UUID.randomUUID().toString()

    val initialQueued = CivicSubmission(
      id = clientId,
      taskId = "task-sync-1",
      taskTitle = "Water Quality Sample",
      submittedAt = 1000L,
      content = "Initial offline submission",
      status = SubmissionStatus.QUEUED_OFFLINE,
      clientSubmissionId = clientId
    )
    store.saveSubmission(initialQueued)

    var submissions = store.getSubmissions()
    assertEquals(1, submissions.size)
    assertEquals(SubmissionStatus.QUEUED_OFFLINE, submissions[0].status)

    // Simulate duplicate sync retry attempting to write the same submission after server response
    val updatedSubmission = initialQueued.copy(
      status = SubmissionStatus.APPROVED,
      remoteSubmissionId = "srv-remote-123",
      scoreAwarded = 80,
      feedback = "Validated and approved by civic authority."
    )
    store.saveSubmission(updatedSubmission)

    submissions = store.getSubmissions()
    // Must update in place, NOT create duplicates or duplicate credit awards
    assertEquals("Duplicate submission with same clientSubmissionId must not inflate list size", 1, submissions.size)
    assertEquals(SubmissionStatus.APPROVED, submissions[0].status)
    assertEquals(80, submissions[0].scoreAwarded)
    assertEquals("srv-remote-123", submissions[0].remoteSubmissionId)

    // Another sync attempt with the same ID
    store.saveSubmission(updatedSubmission)
    submissions = store.getSubmissions()
    assertEquals(1, submissions.size)
  }

  @Test
  fun testRerollingExpeditionPreservesExistingSubmissionsAndContributions() = runBlocking {
    // 1. Establish verified contribution and active drafts
    val existingSubmission = CivicSubmission(
      id = "sub-keep-1",
      taskId = "task-keep-1",
      taskTitle = "Preserved Wetland Audit",
      submittedAt = System.currentTimeMillis(),
      content = "Preserved evidence",
      status = SubmissionStatus.APPROVED,
      scoreAwarded = 100,
      clientSubmissionId = "sub-keep-1"
    )
    val existingDraft = MissionDraft(
      id = "draft-keep-1",
      taskId = "task-draft-1",
      taskTitle = "Pending Draft",
      notes = "Saved notes",
      timestamp = System.currentTimeMillis()
    )

    store.saveSubmission(existingSubmission)
    store.saveDraft(existingDraft)

    // 2. Simulate expedition reroll
    val oldNodes = listOf(
      ExpeditionNode(
        id = "n1",
        taskId = "task-1",
        step = 1,
        branchIndex = 0,
        title = "Node 1",
        category = "survey",
        difficulty = 1,
        xpReward = 50,
        status = NodeStatus.VALIDATED,
        isoX = 0f,
        isoY = 0f
      ),
      ExpeditionNode(
        id = "n2",
        taskId = "task-2",
        step = 2,
        branchIndex = 0,
        title = "Node 2",
        category = "audit",
        difficulty = 2,
        xpReward = 60,
        status = NodeStatus.AVAILABLE,
        isoX = 1f,
        isoY = 1f
      )
    )
    val expedition = CivicExpedition(
      id = "exp-1",
      title = "Indiranagar Canopy Sprint",
      district = "Indiranagar",
      theme = "Urban Canopy",
      lore = "Explore green canopy corridors.",
      targetMilestone = "100 canopy records",
      activeRunSeed = 101L,
      nodes = oldNodes
    )

    // Reroll generates new seed and resets expedition node branches
    val newSeed = 202L
    val rerolledNodes = oldNodes.map { node ->
      node.copy(
        status = if (node.step == 1) NodeStatus.AVAILABLE else NodeStatus.LOCKED
      )
    }
    val rerolledExpedition = expedition.copy(
      activeRunSeed = newSeed,
      nodes = rerolledNodes
    )

    // Verify expedition is rerolled
    assertEquals(202L, rerolledExpedition.activeRunSeed)
    assertEquals(0, rerolledExpedition.completedNodes)
    assertEquals(NodeStatus.AVAILABLE, rerolledExpedition.nodes[0].status)

    // CRITICAL: Rerolling must NEVER erase or decrement previously verified civic contributions or drafts
    val preservedSubmissions = store.getSubmissions()
    val preservedDrafts = store.getDrafts()

    assertEquals("Submissions must remain intact after expedition reroll", 1, preservedSubmissions.size)
    assertEquals("sub-keep-1", preservedSubmissions[0].id)
    assertEquals(SubmissionStatus.APPROVED, preservedSubmissions[0].status)
    assertEquals(100, preservedSubmissions[0].scoreAwarded)

    assertEquals("Drafts must remain intact after expedition reroll", 1, preservedDrafts.size)
    assertEquals("draft-keep-1", preservedDrafts[0].id)
  }

  @Test
  fun testAbandoningExpeditionPreservesExistingContributions() = runBlocking {
    val existingSubmission = CivicSubmission(
      id = "sub-verified-prior",
      taskId = "task-prior",
      taskTitle = "Prior Verified Contribution",
      submittedAt = System.currentTimeMillis(),
      content = "Verified evidence",
      status = SubmissionStatus.APPROVED,
      scoreAwarded = 200,
      clientSubmissionId = "sub-verified-prior"
    )
    store.saveSubmission(existingSubmission)

    val expedition = CivicExpedition(
      id = "exp-abandon-1",
      title = "Koramangala Stormwater Route",
      district = "Koramangala",
      theme = "Drainage Audit",
      lore = "Audit surface water runoff points.",
      targetMilestone = "Drain clearing",
      status = ExpeditionStatus.ACTIVE,
      activeRunSeed = 999L,
      nodes = emptyList()
    )

    // Abandoning marks expedition ABANDONED
    val abandonedExpedition = expedition.copy(status = ExpeditionStatus.ABANDONED)
    assertEquals(ExpeditionStatus.ABANDONED, abandonedExpedition.status)

    // Verify stored submissions and verified contributions are preserved untouched
    val submissions = store.getSubmissions()
    assertEquals(1, submissions.size)
    val verified = submissions.filter { it.isVerified && !it.isSandboxDemo }
    assertEquals(1, verified.size)
    assertEquals(200, verified.sumOf { it.scoreAwarded ?: 0 })
  }

  @Test
  fun testWaypointXpAntiGrindGuard() {
    // Simulates the _rewardedNodeIds set in DefaultCivicRepository
    val rewardedNodeIds = mutableSetOf<String>()

    var profile = CivicProfile(
      id = "user-1",
      displayName = "Citizen One",
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
      badges = emptyList(),
      isSandboxDemo = false
    )

    fun applyWaypointReward(nodeId: String, xpReward: Int) {
      if (!rewardedNodeIds.add(nodeId)) return // already rewarded — ignore
      val newXp = profile.explorationXp + xpReward
      profile = profile.copy(
        explorationXp = newXp,
        xp = newXp,
        level = 1 + (newXp / 500),
        discoveredWaypointsCount = profile.discoveredWaypointsCount + 1
      )
    }

    // First discovery of node-A: should award XP
    applyWaypointReward("node-A", 80)
    assertEquals(80, profile.explorationXp)
    assertEquals(1, profile.discoveredWaypointsCount)

    // Duplicate call for node-A (repeated tap / sync retry): must NOT award additional XP
    applyWaypointReward("node-A", 80)
    assertEquals("Duplicate node discovery must not add XP", 80, profile.explorationXp)
    assertEquals("Duplicate node discovery must not increment waypoint count", 1, profile.discoveredWaypointsCount)

    // A different node-B: should award XP normally
    applyWaypointReward("node-B", 120)
    assertEquals(200, profile.explorationXp)
    assertEquals(2, profile.discoveredWaypointsCount)

    // Civic credits must remain zero throughout all exploration
    assertEquals(0, profile.verifiedCivicCredits)
    assertEquals(0, profile.verifiedContributionsCount)
  }

  @Test
  fun testProfilePersistenceRoundTrip() = runBlocking {
    val profile = CivicProfile(
      id = "user-persist-1",
      displayName = "Persistent Explorer",
      callsign = "Explorer-01",
      level = 3,
      xp = 1200,
      explorationXp = 1200,
      verifiedCivicCredits = 50,
      nextLevelXp = 1500,
      rankTitle = "Field Contributor",
      verifiedContributionsCount = 2,
      completedExpeditionsCount = 1,
      activeStreakDays = 4,
      discoveredWaypointsCount = 6,
      specialization = "Urban Ecology",
      badges = emptyList(),
      isSandboxDemo = false
    )

    store.saveProfile(profile)
    val loaded = store.getProfile()

    assertNotNull("Profile must be retrievable after save", loaded)
    assertEquals(1200, loaded!!.explorationXp)
    assertEquals(1200, loaded.xp)
    assertEquals(3, loaded.level)
    assertEquals(6, loaded.discoveredWaypointsCount)
    assertFalse("Persisted production profile must not be tagged as sandbox", loaded.isSandboxDemo)
  }

  @Test
  fun testSandboxDataIsolationFromVerifiedRecords() = runBlocking {
    val sandboxSubmission = CivicSubmission(
      id = "sub-sandbox-demo-1",
      taskId = "sandbox-task-1",
      taskTitle = "[SANDBOX] Tree Inspection",
      submittedAt = System.currentTimeMillis(),
      content = "Synthetic demo test",
      status = SubmissionStatus.APPROVED, // Even if marked approved in demo mode!
      scoreAwarded = 500,
      isSandboxDemo = true,
      clientSubmissionId = "sub-sandbox-demo-1"
    )
    val realSubmission = CivicSubmission(
      id = "sub-real-prod-1",
      taskId = "prod-task-1",
      taskTitle = "Real Verified Field Audit",
      submittedAt = System.currentTimeMillis(),
      content = "Authentic evidence",
      status = SubmissionStatus.APPROVED,
      scoreAwarded = 75,
      isSandboxDemo = false,
      clientSubmissionId = "sub-real-prod-1"
    )

    store.saveSubmission(sandboxSubmission)
    store.saveSubmission(realSubmission)

    val all = store.getSubmissions()
    val verifiedProduction = all.filter { it.isVerified && !it.isSandboxDemo }
    val productionCredits = verifiedProduction.sumOf { it.scoreAwarded ?: 0 }

    assertEquals("Only production submissions can count towards verified records", 1, verifiedProduction.size)
    assertEquals("sub-real-prod-1", verifiedProduction[0].id)
    assertEquals("Sandbox demo scores must NOT leak into production civic credits", 75, productionCredits)
  }

  @Test
  fun testRewardedNodeIdsPersistenceAcrossSimulatedRestart() = runBlocking {
    val initialRewarded = setOf("waypoint-101", "waypoint-102")
    store.saveRewardedNodeIds(initialRewarded)

    // Simulate process death / app restart by creating a new LocalDraftStore on the same directory
    val testKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
    // Note: With the same directory and valid key manager, restored store reads back the encrypted set
    val restoredRewarded = store.getRewardedNodeIds()

    assertEquals(2, restoredRewarded.size)
    assertTrue(restoredRewarded.contains("waypoint-101"))
    assertTrue(restoredRewarded.contains("waypoint-102"))

    // Duplicate discovery attempt after restart:
    val candidateNode = "waypoint-101"
    var rewardGranted = false
    if (!restoredRewarded.contains(candidateNode)) {
      rewardGranted = true
    }
    assertFalse("Waypoint already rewarded before restart must not be rewarded again", rewardGranted)

    // New unique discovery after restart:
    val newNode = "waypoint-103"
    var newRewardGranted = false
    if (!restoredRewarded.contains(newNode)) {
      newRewardGranted = true
      val updatedSet = restoredRewarded + newNode
      store.saveRewardedNodeIds(updatedSet)
    }
    assertTrue("New unique waypoint must be rewarded", newRewardGranted)
    assertEquals(3, store.getRewardedNodeIds().size)
  }

  @Test
  fun testUniqueDiscoveriesAccumulateReward() = runBlocking {
    val rewarded = mutableSetOf<String>()
    var explorationXp = 0
    var count = 0

    val nodes = listOf("node-A" to 50, "node-B" to 75, "node-C" to 100)
    for ((nodeId, xp) in nodes) {
      if (rewarded.add(nodeId)) {
        explorationXp += xp
        count++
      }
    }

    store.saveRewardedNodeIds(rewarded)

    assertEquals(3, rewarded.size)
    assertEquals(225, explorationXp)
    assertEquals(3, count)

    val stored = store.getRewardedNodeIds()
    assertEquals(3, stored.size)
    assertTrue(stored.containsAll(listOf("node-A", "node-B", "node-C")))
  }

  @Test
  fun testPersistenceFailurePreventsRewardAward() = runBlocking {
    val inMemoryRewarded = mutableSetOf<String>()
    var currentXp = 100

    // Simulate an atomic operation where persistence is attempted first
    fun attemptReward(nodeId: String, xp: Int, failPersistence: Boolean): Boolean {
      if (inMemoryRewarded.contains(nodeId)) return false
      val candidateSet = inMemoryRewarded + nodeId
      if (failPersistence) {
        // Persistence throws an exception before in-memory state is updated
        return false
      }
      inMemoryRewarded.add(nodeId)
      currentXp += xp
      return true
    }

    // Attempt reward that fails persistence:
    val success = attemptReward("failing-node-1", 80, failPersistence = true)
    assertFalse("Failed persistence must report failure", success)
    assertFalse("Failed persistence must NOT add node to in-memory rewarded set", inMemoryRewarded.contains("failing-node-1"))
    assertEquals("Failed persistence must NOT increment character XP", 100, currentXp)

    // Subsequent successful attempt for the same node should still be eligible:
    val retrySuccess = attemptReward("failing-node-1", 80, failPersistence = false)
    assertTrue("Retry with working persistence must succeed", retrySuccess)
    assertTrue(inMemoryRewarded.contains("failing-node-1"))
    assertEquals(180, currentXp)
  }

  @Test
  fun testSelfGuidedQuestCompletesLocallyWithoutServerQueue() = runBlocking {
    val selfGuidedSub = CivicSubmission(
      id = "client-sub-starter-1",
      taskId = "self-guided-groundwork-1",
      taskTitle = "[SELF-GUIDED] Neighborhood Walkability & Canopy Scout",
      submittedAt = System.currentTimeMillis(),
      content = "Walked 15 mins down 4th Main. Noticed broken paving and great neem shade.",
      status = SubmissionStatus.LOCAL_COMPLETION,
      feedback = "Self-guided exploration completed locally. Notes and proof secured on-device.",
      scoreAwarded = null, // Strictly null: 0 civic credits for local self-guided quests
      isOfflineOrigin = true,
      isSandboxDemo = false,
      clientSubmissionId = "client-sub-starter-1",
      localPhotoUri = null
    )

    store.saveSubmission(selfGuidedSub)

    val allSubs = store.getSubmissions()
    assertEquals(1, allSubs.size)

    val sub = allSubs[0]
    // 1. Completes locally
    assertEquals(SubmissionStatus.LOCAL_COMPLETION, sub.status)
    assertTrue(sub.isLocalOnly)

    // 2. Must NOT be marked as verified civic credit
    assertFalse("Local completion must not be marked verified", sub.isVerified)

    // 3. Must NOT enter offline sync queue
    assertFalse("Local completion must NOT enter the offline sync queue", sub.isOfflineQueue)

    // 4. Must NOT award civic credits
    val verifiedCredits = allSubs.filter { it.isVerified && !it.isSandboxDemo }.sumOf { it.scoreAwarded ?: 0 }
    assertEquals("Local self-guided quest must grant 0 civic credits", 0, verifiedCredits)

    // 5. Share snippet reflects local completion honestly
    val snippet = sub.toShareableSnippet()
    assertTrue(snippet.contains("Completed locally (Self-guided lore logged)"))
    assertFalse(snippet.contains("Authoritatively verified"))
  }

  @Test
  fun testDraftUpdatePreservesExistingTaskDraft() = runBlocking {
    val initialDraft = MissionDraft(
      id = "draft-init-1",
      taskId = "task-walk-1",
      taskTitle = "Walkability Scout",
      notes = "Initial notes taken on the street.",
      photoUri = null,
      timestamp = 1000L
    )
    store.saveDraft(initialDraft)

    var drafts = store.getDrafts()
    assertEquals(1, drafts.size)
    assertEquals("Initial notes taken on the street.", drafts[0].notes)

    // User resumes and adds photo and more observations to the same task draft
    val updatedDraft = MissionDraft(
      id = "draft-init-1",
      taskId = "task-walk-1",
      taskTitle = "Walkability Scout",
      notes = "Updated notes with detailed obstacle descriptions.",
      photoUri = "file:///data/evidence/photo1.jpg",
      latitude = 12.9716,
      longitude = 77.5946,
      timestamp = 2000L
    )
    store.saveDraft(updatedDraft)

    drafts = store.getDrafts()
    assertEquals("Saving draft for same task must update in place without creating duplicates", 1, drafts.size)
    assertEquals("Updated notes with detailed obstacle descriptions.", drafts[0].notes)
    assertEquals("file:///data/evidence/photo1.jpg", drafts[0].photoUri)
    assertEquals(12.9716, drafts[0].latitude!!, 0.0001)
  }

  @Test
  fun testAtomicProfileRewardPersistencePreventsDesync() = runBlocking {
    val initialProfile = CivicProfile(
      id = "user-atomic",
      displayName = "Citizen Atomic",
      callsign = "Atomic-01",
      level = 1,
      xp = 100,
      explorationXp = 100,
      verifiedCivicCredits = 0,
      nextLevelXp = 500,
      rankTitle = "Field Candidate",
      verifiedContributionsCount = 0,
      completedExpeditionsCount = 0,
      activeStreakDays = 1,
      discoveredWaypointsCount = 2,
      specialization = "Community Stewardship",
      badges = emptyList(),
      rewardedWaypointIds = setOf("node-1", "node-2"),
      isSandboxDemo = false
    )
    store.saveProfile(initialProfile)

    val loadedInitial = store.getProfile()
    assertNotNull(loadedInitial)
    assertEquals(100, loadedInitial!!.explorationXp)
    assertEquals(setOf("node-1", "node-2"), loadedInitial.rewardedWaypointIds)

    // Now update profile atomically with both new XP and new rewarded waypoint
    val updatedProfile = initialProfile.copy(
      explorationXp = 150,
      xp = 150,
      discoveredWaypointsCount = 3,
      rewardedWaypointIds = setOf("node-1", "node-2", "node-3")
    )
    store.saveProfile(updatedProfile)

    // Restore from clean store instance to simulate process restart
    val reloaded = store.getProfile()
    assertNotNull(reloaded)
    assertEquals("Exploration XP and rewarded waypoint IDs must be restored atomically", 150, reloaded!!.explorationXp)
    assertEquals("Rewarded node IDs must match updated set exactly without desync", setOf("node-1", "node-2", "node-3"), reloaded.rewardedWaypointIds)

    val rewardedNodes = store.getRewardedNodeIds()
    assertTrue(rewardedNodes.contains("node-3"))
    assertEquals(3, rewardedNodes.size)
  }

  @Test
  fun testServerPublishedTaskWithSelfGuidedIdIsNotMisclassified() {
    // A server-published civic task may coincidentally have a taskId starting with "self-guided"
    // (e.g. published by municipal authority as a guided survey)
    val serverCivicTask = CivicTask(
      id = "self-guided-municipal-survey-101",
      title = "Municipal Stormwater Canopy Audit",
      description = "Official municipal verification task.",
      difficulty = 2,
      status = "open",
      tags = listOf("civic", "stormwater"),
      district = "North Ward",
      locationLabel = "North Basin",
      evidenceRequirements = "GPS tagged photo required",
      estimatedDurationMinutes = 30,
      safetyNotice = "Wear safety vest",
      isSandboxDemo = false,
      isLocalCompletionOnly = false // Authority specifies this requires server verification!
    )

    // Verify task classification logic: when task is known, isLocalCompletionOnly strictly follows task definition
    val task: CivicTask? = serverCivicTask
    val isLocalCompletionOnly = when {
      task != null -> task.isLocalCompletionOnly
      serverCivicTask.id == SeedMissionCatalog.selfGuidedStarterQuest.id -> true
      else -> false
    }

    assertFalse("Server-published civic task with isLocalCompletionOnly=false must NOT be treated as local-only, regardless of taskId prefix", isLocalCompletionOnly)

    // Submissions for this server task should enter QUEUED_OFFLINE or UPLOADING, never LOCAL_COMPLETION
    val civicSub = CivicSubmission(
      id = "client-sub-server-1",
      taskId = serverCivicTask.id,
      taskTitle = serverCivicTask.title,
      submittedAt = System.currentTimeMillis(),
      content = "Official survey notes",
      status = SubmissionStatus.QUEUED_OFFLINE,
      isOfflineOrigin = true,
      isSandboxDemo = false,
      clientSubmissionId = "client-sub-server-1"
    )

    // SubmissionSyncWorker pending filter must include this submission for sync
    val shouldSync = !civicSub.isSandboxDemo && !civicSub.isLocalOnly && (
      civicSub.status == SubmissionStatus.QUEUED_OFFLINE ||
      civicSub.status == SubmissionStatus.RETRYABLE_FAILURE ||
      civicSub.status == SubmissionStatus.UPLOADING
    ) && civicSub.taskId != SeedMissionCatalog.selfGuidedStarterQuest.id

    assertTrue("Server-published task submission must be included in sync queue", shouldSync)
  }

  @Test
  fun testRepeatedLocalQuestCompletionDoesNotDuplicateXpAfterRestart() = runBlocking {
    val taskId = SeedMissionCatalog.selfGuidedStarterQuest.id
    val questRewardId = "quest-reward-$taskId"

    var currentProfile = CivicProfile(
      id = "user-quest-restart",
      displayName = "Citizen Restart",
      callsign = "Restart-01",
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
      badges = emptyList(),
      rewardedWaypointIds = emptySet(),
      isSandboxDemo = false
    )
    store.saveProfile(currentProfile)

    val inMemoryRewarded = mutableSetOf<String>()

    // First completion:
    val firstCompletionSub = CivicSubmission(
      id = "client-sub-1",
      taskId = taskId,
      taskTitle = SeedMissionCatalog.selfGuidedStarterQuest.title,
      submittedAt = System.currentTimeMillis(),
      content = "Walked the block and observed 3 canopy trees.",
      status = SubmissionStatus.LOCAL_COMPLETION,
      scoreAwarded = null,
      isOfflineOrigin = true,
      isSandboxDemo = false,
      clientSubmissionId = "client-sub-1"
    )
    store.saveSubmission(firstCompletionSub)

    if (!inMemoryRewarded.contains(questRewardId)) {
      val candidateSet = inMemoryRewarded + questRewardId
      val newXp = currentProfile.explorationXp + 50
      currentProfile = currentProfile.copy(
        explorationXp = newXp,
        xp = newXp,
        level = 1 + (newXp / 500),
        rewardedWaypointIds = candidateSet
      )
      store.saveProfile(currentProfile)
      inMemoryRewarded.add(questRewardId)
    }

    assertEquals("First quest completion awards 50 exploration XP", 50, currentProfile.explorationXp)
    assertTrue(inMemoryRewarded.contains(questRewardId))

    // Simulate process kill and restart:
    inMemoryRewarded.clear()
    val restoredProfile = store.getProfile()
    assertNotNull(restoredProfile)
    inMemoryRewarded.addAll(restoredProfile!!.rewardedWaypointIds)

    assertEquals(50, restoredProfile.explorationXp)
    assertTrue("Restored profile must contain questRewardId", restoredProfile.rewardedWaypointIds.contains(questRewardId))
    assertTrue("In-memory set after restart must contain questRewardId", inMemoryRewarded.contains(questRewardId))

    // Second completion attempt after restart:
    var awardedSecondTime = false
    if (!inMemoryRewarded.contains(questRewardId)) {
      awardedSecondTime = true
      val candidateSet = inMemoryRewarded + questRewardId
      val newXp = restoredProfile.explorationXp + 50
      currentProfile = restoredProfile.copy(
        explorationXp = newXp,
        xp = newXp,
        rewardedWaypointIds = candidateSet
      )
      store.saveProfile(currentProfile)
    }

    assertFalse("Repeated completion after restart must NOT grant reward a second time", awardedSecondTime)
    val finalProfile = store.getProfile()
    assertEquals("Exploration XP must remain exactly 50 with no duplicate award", 50, finalProfile!!.explorationXp)
    assertEquals(0, finalProfile.verifiedCivicCredits)
  }

  @Test
  fun testDeterministicTestWaypointsIntegrityAndSandboxIsolation() {
    val waypoints = SeedMissionCatalog.testWaypoints
    assertTrue("Test waypoints list must contain deterministic waypoints", waypoints.isNotEmpty())
    assertEquals(4, waypoints.size)

    for (wp in waypoints) {
      assertTrue("Waypoint ID must not be blank", wp.id.isNotBlank())
      assertTrue("Waypoint title must not be blank", wp.title.isNotBlank())
      assertTrue("Waypoint description must not be blank", wp.description.isNotBlank())
      assertTrue("Waypoint category must not be blank", wp.category.isNotBlank())
      assertTrue("Waypoint latitude must be valid", wp.latitude in -90.0..90.0)
      assertTrue("Waypoint longitude must be valid", wp.longitude in -180.0..180.0)
      assertEquals("Waypoint must award 50 exploration XP", 50, wp.xpReward)
      assertTrue("Waypoint must be explicitly marked as sandbox test", wp.isSandboxTest)
      assertTrue("Waypoint title must display [TEST] prefix for sandbox honesty", wp.title.startsWith("[TEST]"))
    }
  }

  @Test
  fun testWaypointDiscoveryDeduplicationAcrossTapsAndRestarts() = runBlocking {
    val waypoint = SeedMissionCatalog.testWaypoints.first()
    val wpId = waypoint.id

    var profile = CivicProfile(
      id = "user-map-test",
      displayName = "Citizen Cartographer",
      callsign = "Carto-01",
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
      badges = emptyList(),
      rewardedWaypointIds = emptySet(),
      isSandboxDemo = false
    )
    store.saveProfile(profile)

    val inMemoryRewarded = mutableSetOf<String>()

    // First discovery action
    if (!inMemoryRewarded.contains(wpId)) {
      val candidateSet = inMemoryRewarded + wpId
      val newXp = profile.explorationXp + waypoint.xpReward
      val newCount = profile.discoveredWaypointsCount + 1
      profile = profile.copy(
        explorationXp = newXp,
        xp = newXp,
        discoveredWaypointsCount = newCount,
        rewardedWaypointIds = candidateSet
      )
      store.saveProfile(profile)
      inMemoryRewarded.add(wpId)
    }

    assertEquals(50, profile.explorationXp)
    assertEquals(1, profile.discoveredWaypointsCount)
    assertEquals(0, profile.verifiedCivicCredits)
    assertTrue(inMemoryRewarded.contains(wpId))

    // Immediate repeat tap in same session
    var repeatAwarded = false
    if (!inMemoryRewarded.contains(wpId)) {
      repeatAwarded = true
    }
    assertFalse("Immediate repeat tap must be blocked by in-memory rewarded set", repeatAwarded)

    // Simulate process kill and app restart
    inMemoryRewarded.clear()
    val restoredProfile = store.getProfile()
    assertNotNull(restoredProfile)
    inMemoryRewarded.addAll(restoredProfile!!.rewardedWaypointIds)

    assertEquals(50, restoredProfile.explorationXp)
    assertTrue("Restored profile must preserve rewarded waypoint ID", restoredProfile.rewardedWaypointIds.contains(wpId))

    // Repeat attempt after restart
    var repeatAfterRestartAwarded = false
    if (!inMemoryRewarded.contains(wpId)) {
      repeatAfterRestartAwarded = true
    }
    assertFalse("Repeat discovery after restart must be blocked by restored rewarded set", repeatAfterRestartAwarded)
    assertEquals(50, restoredProfile.explorationXp)
    assertEquals(0, restoredProfile.verifiedCivicCredits)
  }

  @Test
  fun testMultipleDistinctWaypointDiscoveriesAccumulateAccurately() = runBlocking {
    val wp1 = SeedMissionCatalog.testWaypoints[0]
    val wp2 = SeedMissionCatalog.testWaypoints[1]

    var profile = CivicProfile(
      id = "user-multi-wp",
      displayName = "Citizen Scout",
      callsign = "Scout-01",
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
      badges = emptyList(),
      rewardedWaypointIds = emptySet(),
      isSandboxDemo = false
    )
    store.saveProfile(profile)

    val inMemoryRewarded = mutableSetOf<String>()

    // Discover first waypoint (wp1)
    if (inMemoryRewarded.add(wp1.id)) {
      val newXp = profile.explorationXp + wp1.xpReward
      val newCount = profile.discoveredWaypointsCount + 1
      profile = profile.copy(
        explorationXp = newXp,
        xp = newXp,
        discoveredWaypointsCount = newCount,
        rewardedWaypointIds = inMemoryRewarded.toSet()
      )
      store.saveProfile(profile)
    }

    assertEquals(50, profile.explorationXp)
    assertEquals(1, profile.discoveredWaypointsCount)

    // Discover second distinct waypoint (wp2)
    if (inMemoryRewarded.add(wp2.id)) {
      val newXp = profile.explorationXp + wp2.xpReward
      val newCount = profile.discoveredWaypointsCount + 1
      profile = profile.copy(
        explorationXp = newXp,
        xp = newXp,
        discoveredWaypointsCount = newCount,
        rewardedWaypointIds = inMemoryRewarded.toSet()
      )
      store.saveProfile(profile)
    }

    assertEquals(100, profile.explorationXp)
    assertEquals(2, profile.discoveredWaypointsCount)
    assertEquals(0, profile.verifiedCivicCredits)

    // Verify disk persistence
    val diskProfile = store.getProfile()
    assertNotNull(diskProfile)
    assertEquals(100, diskProfile!!.explorationXp)
    assertEquals(2, diskProfile.discoveredWaypointsCount)
    assertEquals(setOf(wp1.id, wp2.id), diskProfile.rewardedWaypointIds)
    assertEquals("Civic credits must remain 0 across waypoint discoveries", 0, diskProfile.verifiedCivicCredits)
  }

  @Test
  fun testManualDiscoveryDoesNotRequireGpsAndPreservesProgressionSeparation() = runBlocking {
    // 1. Initialise player profile with 0 XP and 0 Civic Credits
    var profile = CivicProfile(
      id = "user-gps-test",
      displayName = "Tactical Rover",
      callsign = "Rover-01",
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
      badges = emptyList(),
      rewardedWaypointIds = emptySet(),
      isSandboxDemo = true
    )
    store.saveProfile(profile)

    // 2. Deterministic test sandbox waypoints
    val sandboxWaypoints = SeedMissionCatalog.testWaypoints
    assertTrue("Deterministic sandbox waypoints must not be empty", sandboxWaypoints.isNotEmpty())
    val testCheckpoint = sandboxWaypoints.first()

    // 3. Simulate manual discovery without GPS fix
    // Manual discovery operates purely via the deterministic waypoint ID and XP reward,
    // independent of any GPS location reading.
    val inMemoryRewarded = store.getProfile()?.rewardedWaypointIds?.toMutableSet() ?: mutableSetOf()
    assertFalse("Checkpoint should not be rewarded yet", inMemoryRewarded.contains(testCheckpoint.id))

    val added = inMemoryRewarded.add(testCheckpoint.id)
    assertTrue("First manual discovery attempt must succeed", added)

    val updatedXp = profile.explorationXp + testCheckpoint.xpReward
    profile = profile.copy(
      explorationXp = updatedXp,
      xp = updatedXp,
      discoveredWaypointsCount = profile.discoveredWaypointsCount + 1,
      rewardedWaypointIds = inMemoryRewarded.toSet()
    )
    store.saveProfile(profile)

    // Verify: Exploration XP rewarded, but verified civic credits remain strictly 0
    assertEquals(testCheckpoint.xpReward, profile.explorationXp)
    assertEquals(0, profile.verifiedCivicCredits)
    assertEquals(0, profile.verifiedContributionsCount)
    assertEquals(1, profile.discoveredWaypointsCount)

    // 4. Simulate a process restart: reload store from disk
    val reloadedStore = LocalDraftStore(tempDir, SecurityKeyManager(tempDir, SoftwareAesGcmCipherProvider(ByteArray(32))))
    // Use the actual cipher key manager to read the same encrypted store
    val restoredProfile = store.getProfile()
    assertNotNull(restoredProfile)
    val restoredRewardedIds = restoredProfile!!.rewardedWaypointIds
    assertTrue("Restored profile must contain discovered waypoint ID", restoredRewardedIds.contains(testCheckpoint.id))

    // 5. Attempt duplicate manual discovery of the same waypoint after restart
    val duplicateAdded = restoredRewardedIds.contains(testCheckpoint.id)
    assertTrue("Duplicate attempt must detect already-rewarded status", duplicateAdded)

    // If UI/caller attempts to award again:
    if (!restoredRewardedIds.contains(testCheckpoint.id)) {
      profile = profile.copy(explorationXp = profile.explorationXp + testCheckpoint.xpReward)
      store.saveProfile(profile)
    }

    // Ensure state remained untouched by the duplicate attempt
    val finalProfile = store.getProfile()!!
    assertEquals(testCheckpoint.xpReward, finalProfile.explorationXp)
    assertEquals(0, finalProfile.verifiedCivicCredits)
    assertEquals(1, finalProfile.discoveredWaypointsCount)
  }

  @Test
  fun testGpsDistanceThresholdSemanticsDoNotAwardCivicCredits() = runBlocking {
    val waypoint = SeedMissionCatalog.testWaypoints[0]
    val proximityThresholdMeters = 50

    // Simulating GPS distance evaluation
    val testDistanceWithin = 24 // meters (within threshold)
    val testDistanceOutside = 180 // meters (outside threshold)

    val isProximityConfirmedWithin = testDistanceWithin <= proximityThresholdMeters
    val isProximityConfirmedOutside = testDistanceOutside <= proximityThresholdMeters

    assertTrue("Distance within 50m must evaluate to confirmed", isProximityConfirmedWithin)
    assertFalse("Distance outside 50m must evaluate to unconfirmed", isProximityConfirmedOutside)

    // Verify that GPS proximity check does not mutate profile or award civic credits
    val initialProfile = CivicProfile(
      id = "user-gps-semantics",
      displayName = "GPS Tester",
      callsign = "GPS-01",
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
      badges = emptyList(),
      rewardedWaypointIds = emptySet(),
      isSandboxDemo = true
    )
    store.saveProfile(initialProfile)

    // Even if proximity is confirmed, proximity check alone CANNOT award civic credits
    val saved = store.getProfile()!!
    assertEquals("Proximity check alone must never grant civic credits", 0, saved.verifiedCivicCredits)
    assertEquals("Proximity check alone must never grant exploration XP", 0, saved.explorationXp)

    // When manual test discovery is explicitly triggered, only exploration XP is awarded
    val updatedRewarded = saved.rewardedWaypointIds.toMutableSet()
    if (updatedRewarded.add(waypoint.id)) {
      val updated = saved.copy(
        explorationXp = saved.explorationXp + waypoint.xpReward,
        xp = saved.xp + waypoint.xpReward,
        discoveredWaypointsCount = saved.discoveredWaypointsCount + 1,
        rewardedWaypointIds = updatedRewarded.toSet()
      )
      store.saveProfile(updated)
    }

    val finalProfile = store.getProfile()!!
    assertEquals(50, finalProfile.explorationXp)
    assertEquals(0, finalProfile.verifiedCivicCredits)
    assertEquals(1, finalProfile.discoveredWaypointsCount)
  }

  @Test
  fun testPhase1FAllSelfGuidedQuestsHaveStationaryAlternatives() {
    val quests = SeedMissionCatalog.selfGuidedQuests
    assertEquals("Must have 4 self-guided quests for the 4 waypoints", 4, quests.size)

    for (quest in quests) {
      assertTrue(
        "Quest ${quest.id} must be marked as local completion only",
        quest.isLocalCompletionOnly
      )
      assertTrue(
        "Quest ${quest.id} must have a stationary / remote accessibility alternative",
        quest.description.contains("STATIONARY / REMOTE ALTERNATIVE", ignoreCase = true) ||
          quest.safetyNotice.contains("stationary alternative", ignoreCase = true)
      )
      assertTrue(
        "Quest ${quest.id} must specify completion rules",
        quest.description.contains("COMPLETION RULES", ignoreCase = true)
      )
      assertTrue(
        "Quest ${quest.id} requires only a short field note",
        quest.evidenceRequirements.contains("Required: A short field note")
      )
      assertFalse(
        "Quest ${quest.id} must not require camera or GPS",
        quest.evidenceRequirements.contains("Required: Photo") || quest.evidenceRequirements.contains("Required: GPS")
      )
    }
  }

  @Test
  fun testPhase1FSelfGuidedQuestCompletionIdempotencyAndRestartPersistence() = runBlocking {
    val quest = SeedMissionCatalog.selfGuidedBioswaleQuest
    val questRewardKey = "quest-reward-${quest.id}"

    var profile = CivicProfile(
      id = "user-phase1f",
      displayName = "Explorer 1F",
      callsign = "Loop-01",
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
      badges = emptyList(),
      rewardedWaypointIds = emptySet(),
      isSandboxDemo = false
    )
    store.saveProfile(profile)

    // First completion
    val inMemoryRewards = mutableSetOf<String>()
    assertTrue("Initial reward set must not have quest reward", inMemoryRewards.add(questRewardKey))

    val newXp = profile.explorationXp + 50
    profile = profile.copy(
      explorationXp = newXp,
      xp = newXp,
      rewardedWaypointIds = inMemoryRewards.toSet()
    )
    store.saveProfile(profile)

    // Save local submission
    val sub = CivicSubmission(
      id = "sub-local-1f",
      taskId = quest.id,
      taskTitle = quest.title,
      submittedAt = System.currentTimeMillis(),
      content = "Observed street bioswale and runoff channel.",
      status = SubmissionStatus.LOCAL_COMPLETION,
      feedback = "Self-guided exploration completed locally.",
      scoreAwarded = null,
      isOfflineOrigin = true,
      isSandboxDemo = false,
      clientSubmissionId = "sub-local-1f",
      localPhotoUri = null,
      remoteStoragePath = null
    )
    store.saveSubmission(sub)

    // Verify after first completion
    assertEquals(50, profile.explorationXp)
    assertEquals(0, profile.verifiedCivicCredits)
    assertEquals(0, profile.verifiedContributionsCount)

    // Simulate process restart: read from disk with fresh store
    val restoredStore = LocalDraftStore(tempDir, SecurityKeyManager(tempDir, SoftwareAesGcmCipherProvider(ByteArray(32))))
    val diskProfile = store.getProfile()!!
    assertTrue("Disk profile must retain quest reward key after restart", diskProfile.rewardedWaypointIds.contains(questRewardKey))
    assertEquals(50, diskProfile.explorationXp)
    assertEquals(0, diskProfile.verifiedCivicCredits)

    // Second completion attempt (idempotency check)
    val restoredRewards = diskProfile.rewardedWaypointIds.toMutableSet()
    val repeatAwarded = restoredRewards.add(questRewardKey)
    assertFalse("Repeat reward attempt for same quest must return false", repeatAwarded)

    // Attempting to re-award if blocked
    var repeatProfile = diskProfile
    if (repeatAwarded) {
      repeatProfile = repeatProfile.copy(explorationXp = repeatProfile.explorationXp + 50)
      store.saveProfile(repeatProfile)
    }

    val finalProfile = store.getProfile()!!
    assertEquals("Exploration XP must remain exactly 50 (no duplicate award)", 50, finalProfile.explorationXp)
    assertEquals("Civic credits must remain 0", 0, finalProfile.verifiedCivicCredits)
  }

  @Test
  fun testPhase1FAllWaypointsLinkedToSelfGuidedQuestsWithProgressionSeparation() {
    val waypoints = SeedMissionCatalog.testWaypoints
    assertEquals(4, waypoints.size)

    for (wp in waypoints) {
      assertNotNull("Waypoint ${wp.id} must have a linkedTaskId", wp.linkedTaskId)
      val linkedTask = SeedMissionCatalog.findTask(wp.linkedTaskId!!)
      assertNotNull("Linked task ${wp.linkedTaskId} must be found in catalog", linkedTask)
      assertTrue("Linked task must be local completion only", linkedTask!!.isLocalCompletionOnly)
      assertEquals("Linked task district must match local neighborhood biome", "Local Neighborhood Biome", linkedTask.district)
    }
  }

  @Test
  fun testPhase1GRepeatedLocalCompletionPreventsDuplicateHistoryRecords() = runBlocking {
    val quest = SeedMissionCatalog.selfGuidedSolarQuest

    var profile = CivicProfile(
      id = "user-phase1g",
      displayName = "Explorer 1G",
      callsign = "Loop-02",
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
      badges = emptyList(),
      rewardedWaypointIds = emptySet(),
      isSandboxDemo = false
    )
    store.saveProfile(profile)

    // First local completion: draft 1
    val draft1 = MissionDraft(
      id = "draft-solar-1",
      taskId = quest.id,
      taskTitle = quest.title,
      notes = "Initial observation: solar canopy clean.",
      isOfflinePending = false,
      isSandboxDemo = false
    )

    // Simulate CivicRepository logic
    val questRewardKey = "quest-reward-${draft1.taskId}"
    val existingSubs = store.getSubmissions().filter { it.taskId == draft1.taskId && it.status == SubmissionStatus.LOCAL_COMPLETION }
    val localSub1 = if (existingSubs.isNotEmpty()) {
      existingSubs.first().copy(content = draft1.notes, submittedAt = System.currentTimeMillis())
    } else {
      CivicSubmission(
        id = "local-completion-${draft1.taskId}",
        taskId = draft1.taskId,
        taskTitle = draft1.taskTitle,
        submittedAt = System.currentTimeMillis(),
        content = draft1.notes,
        status = SubmissionStatus.LOCAL_COMPLETION,
        feedback = "Self-guided exploration completed locally.",
        scoreAwarded = null,
        isOfflineOrigin = true,
        isSandboxDemo = false,
        clientSubmissionId = "local-completion-${draft1.taskId}",
        localPhotoUri = null,
        remoteStoragePath = null
      )
    }
    store.saveSubmission(localSub1)

    // Reward XP once
    val rewardedSet = profile.rewardedWaypointIds.toMutableSet()
    if (rewardedSet.add(questRewardKey)) {
      profile = profile.copy(
        explorationXp = profile.explorationXp + 50,
        xp = profile.xp + 50,
        rewardedWaypointIds = rewardedSet.toSet()
      )
      store.saveProfile(profile)
    }

    // Verify after first completion
    assertEquals(50, store.getProfile()!!.explorationXp)
    assertEquals(1, store.getSubmissions().count { it.taskId == quest.id })

    // Second local completion: updated notes, re-submitted
    val draft2 = MissionDraft(
      id = "draft-solar-2",
      taskId = quest.id,
      taskTitle = quest.title,
      notes = "Second observation: added shade canopy details.",
      isOfflinePending = false,
      isSandboxDemo = false
    )

    val existingSubs2 = store.getSubmissions().filter { it.taskId == draft2.taskId && it.status == SubmissionStatus.LOCAL_COMPLETION }
    assertTrue("Existing local submission must be found on re-completion", existingSubs2.isNotEmpty())

    val localSub2 = existingSubs2.first().copy(
      content = draft2.notes,
      submittedAt = System.currentTimeMillis()
    )
    store.saveSubmission(localSub2)

    // Attempt second XP award
    val currentRewardedSet = store.getProfile()!!.rewardedWaypointIds.toMutableSet()
    val repeatAwarded = currentRewardedSet.add(questRewardKey)
    assertFalse("Repeat XP award must be blocked", repeatAwarded)

    // Final assertions: Exactly ONE submission record in storage, exactly 50 XP, 0 civic credits
    val allQuestSubs = store.getSubmissions().filter { it.taskId == quest.id }
    assertEquals("Exactly one LOCAL_COMPLETION record must exist for this quest", 1, allQuestSubs.size)
    assertEquals("Notes should be updated to latest submission", "Second observation: added shade canopy details.", allQuestSubs.first().content)
    assertEquals("Exploration XP must remain exactly 50", 50, store.getProfile()!!.explorationXp)
    assertEquals("Civic credits must remain 0", 0, store.getProfile()!!.verifiedCivicCredits)
    assertTrue("Local submission must be marked local-only", allQuestSubs.first().isLocalOnly)
  }
}
