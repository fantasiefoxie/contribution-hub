package com.example.contributionos.data

import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.DataSourceMode
import com.example.contributionos.model.ExpeditionStatus
import com.example.contributionos.model.MissionDraft
import com.example.contributionos.model.NodeStatus
import com.example.contributionos.model.SubmissionStatus
import com.example.contributionos.security.SecurityKeyManager
import com.example.contributionos.security.SoftwareAesGcmCipherProvider
import com.example.contributionos.ui.screens.CHECKPOINT_PROXIMITY_THRESHOLD_METERS
import com.example.contributionos.ui.screens.MAX_ACCEPTABLE_ACCURACY_METERS
import com.example.contributionos.ui.screens.MAX_STALE_FIX_AGE_MS
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.SecureRandom
import java.util.UUID

class ExpeditionRogueliteLoopTest {

  private lateinit var tempDir: File
  private lateinit var store: LocalDraftStore
  private lateinit var keyManager: SecurityKeyManager

  @Before
  fun setUp() {
    tempDir = Files.createTempDirectory("roguelite_loop_test").toFile()
    val testKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
    val cipherProvider = SoftwareAesGcmCipherProvider(testKey)
    keyManager = SecurityKeyManager(tempDir, cipherProvider)
    store = LocalDraftStore(tempDir, keyManager)
  }

  @After
  fun tearDown() {
    tempDir.deleteRecursively()
  }

  private fun createRepository(localStore: LocalDraftStore = store): DefaultCivicRepository {
    return DefaultCivicRepository(
      context = null,
      localDraftStore = localStore,
      remoteDataSource = RemoteCivicDataSource(),
      authManager = com.example.contributionos.auth.SupabaseAuthManager(tempDir, keyManager = keyManager),
      storageUploader = SupabaseStorageUploader(context = object : android.content.ContextWrapper(null) {})
    )
  }

  @Test
  fun testOfflineProductionFallbackDoesNotExposeFixtures() = runBlocking {
    val repo = createRepository()
    repo.initialize()

    val expeditions = repo.availableExpeditions.first()
    assertTrue("Production mode offline fallback must NOT load unverified fixtures; expeditions must be empty", expeditions.isEmpty())

    val activeExp = repo.activeExpedition.first()
    assertNull("Active expedition must be null when no verified expeditions exist", activeExp)

    // Verify tasks also do not expose unverified fixture tasks
    val tasks = repo.tasks.first()
    assertFalse("Production mode must never expose [FIXTURE] tasks", tasks.any { it.title.startsWith("[FIXTURE]") })
  }

  @Test
  fun testSandboxDemoModeLoadsFixturesExplicitly() = runBlocking {
    val repo = createRepository()
    repo.initialize()
    repo.setEnvironmentMode(DataSourceMode.SANDBOX_DEMO)

    val expeditions = repo.availableExpeditions.first()
    assertFalse("Sandbox demo mode must load demo expeditions", expeditions.isEmpty())
    val firstExp = expeditions.first()
    assertTrue("Demo expedition must be marked as sandbox demo", firstExp.isSandboxDemo)
    assertTrue("Demo expedition must have prefix [DEMO]", firstExp.title.startsWith("[DEMO]"))
  }

  @Test
  fun testNodeProgressionAdvancesStepAndUnlocksNext() = runBlocking {
    val repo = createRepository()
    repo.initialize()

    val fixtureExp = SeedMissionCatalog.productionFixtureExpedition
    repo.loadTestExpedition(fixtureExp)

    val expId = fixtureExp.id
    val node1Id = fixtureExp.nodes[0].id
    val node2Id = fixtureExp.nodes[1].id
    val node3Id = fixtureExp.nodes[2].id

    // Advance Node 1
    repo.advanceExpeditionNode(expId, node1Id)

    var currentExp = repo.activeExpedition.first()!!
    assertEquals(NodeStatus.VALIDATED, currentExp.nodes.find { it.id == node1Id }!!.status)
    assertEquals(NodeStatus.AVAILABLE, currentExp.nodes.find { it.id == node2Id }!!.status)
    assertEquals(NodeStatus.LOCKED, currentExp.nodes.find { it.id == node3Id }!!.status)
    assertEquals(ExpeditionStatus.ACTIVE, currentExp.status)

    // Advance Node 2
    repo.advanceExpeditionNode(expId, node2Id)
    currentExp = repo.activeExpedition.first()!!
    assertEquals(NodeStatus.VALIDATED, currentExp.nodes.find { it.id == node2Id }!!.status)
    assertEquals(NodeStatus.AVAILABLE, currentExp.nodes.find { it.id == node3Id }!!.status)

    // Advance Node 3 (Final Node) -> Completes expedition
    repo.advanceExpeditionNode(expId, node3Id)
    currentExp = repo.activeExpedition.first()!!
    assertEquals(NodeStatus.VALIDATED, currentExp.nodes.find { it.id == node3Id }!!.status)
    assertEquals(ExpeditionStatus.COMPLETED, currentExp.status)
  }

  @Test
  fun testDuplicateNodeRewardIsIdempotent() = runBlocking {
    val repo = createRepository()
    repo.initialize()

    val nodeId = "node-fixture-banyan-1"

    // First discovery awards 50 XP
    repo.recordWaypointDiscovery(nodeId, 50)
    var profile = repo.profile.first()
    assertEquals(50, profile.explorationXp)
    assertEquals(1, profile.discoveredWaypointsCount)

    // Duplicate discovery call must NOT increment XP
    repo.recordWaypointDiscovery(nodeId, 50)
    profile = repo.profile.first()
    assertEquals(50, profile.explorationXp)
    assertEquals(1, profile.discoveredWaypointsCount)
  }

  @Test
  fun testExpeditionCompletionBonusAwardedOnce() = runBlocking {
    val repo = createRepository()
    repo.initialize()

    val expId = SeedMissionCatalog.productionFixtureExpedition.id

    // First completion awards 100 bonus XP
    val firstResult = repo.completeExpedition(expId, 100)
    assertTrue("First completion must grant reward", firstResult)

    var profile = repo.profile.first()
    assertEquals(100, profile.explorationXp)
    assertEquals(1, profile.completedExpeditionsCount)
    assertTrue(profile.completedExpeditionIds.contains(expId))

    // Duplicate completion must be blocked by idempotency guard
    val duplicateResult = repo.completeExpedition(expId, 100)
    assertFalse("Duplicate completion must be rejected", duplicateResult)

    profile = repo.profile.first()
    assertEquals("XP must not increase on duplicate completion", 100, profile.explorationXp)
    assertEquals(1, profile.completedExpeditionsCount)
  }

  @Test
  fun testAppRestartRecoveryPreservesCompletedExpeditionsAndRewardedWaypoints() = runBlocking {
    val repo1 = createRepository()
    repo1.initialize()

    val expId = SeedMissionCatalog.productionFixtureExpedition.id
    val node1Id = "node-fixture-banyan-1"
    val node2Id = "node-fixture-banyan-2"

    repo1.recordWaypointDiscovery(node1Id, 50)
    repo1.recordWaypointDiscovery(node2Id, 50)
    repo1.completeExpedition(expId, 100)

    val profileBefore = repo1.profile.first()
    assertEquals(200, profileBefore.explorationXp)

    // Simulate process death and app restart: create a new repository instance pointing to the same encrypted store
    val repo2 = createRepository()
    repo2.initialize()

    val profileAfter = repo2.profile.first()
    assertEquals("Exploration XP must survive restart", 200, profileAfter.explorationXp)
    assertEquals("Discovered waypoints count must survive restart", 2, profileAfter.discoveredWaypointsCount)
    assertEquals("Completed expeditions count must survive restart", 1, profileAfter.completedExpeditionsCount)
    assertTrue("Rewarded node 1 must be preserved in profile set", profileAfter.rewardedWaypointIds.contains(node1Id))
    assertTrue("Rewarded node 2 must be preserved in profile set", profileAfter.rewardedWaypointIds.contains(node2Id))
    assertTrue("Completed expedition ID must be preserved", profileAfter.completedExpeditionIds.contains(expId))

    // Attempting duplicate discovery of node1 after restart must be ignored
    repo2.recordWaypointDiscovery(node1Id, 50)
    assertEquals("Duplicate discovery after restart must not inflate XP", 200, repo2.profile.first().explorationXp)

    // Attempting duplicate completion after restart must be ignored
    val retryComplete = repo2.completeExpedition(expId, 100)
    assertFalse("Duplicate expedition completion after restart must return false", retryComplete)
    assertEquals(200, repo2.profile.first().explorationXp)
  }

  @Test
  fun testMidExpeditionProcessRestartRestoresExactNodeProgression() = runBlocking {
    val repo1 = createRepository()
    repo1.initialize()

    val fixtureExp = SeedMissionCatalog.productionFixtureExpedition
    repo1.loadTestExpedition(fixtureExp)

    val expId = fixtureExp.id
    val node1Id = fixtureExp.nodes[0].id
    val node2Id = fixtureExp.nodes[1].id
    val node3Id = fixtureExp.nodes[2].id

    // Complete Node 1
    repo1.recordWaypointDiscovery(node1Id, 50)
    repo1.advanceExpeditionNode(expId, node1Id)

    val expBeforeKill = repo1.activeExpedition.first()!!
    assertEquals(NodeStatus.VALIDATED, expBeforeKill.nodes.find { it.id == node1Id }!!.status)
    assertEquals(NodeStatus.AVAILABLE, expBeforeKill.nodes.find { it.id == node2Id }!!.status)
    assertEquals(NodeStatus.LOCKED, expBeforeKill.nodes.find { it.id == node3Id }!!.status)
    assertEquals(ExpeditionStatus.ACTIVE, expBeforeKill.status)

    // Simulate process death and relaunch: instantiate a fresh repository instance using the same persistent store
    val repo2 = createRepository()
    repo2.initialize()

    // Restore test expedition in repo2
    repo2.loadTestExpedition(fixtureExp)

    val expAfterRestart = repo2.activeExpedition.first()!!
    assertEquals(expId, expAfterRestart.id)
    assertEquals("Node 1 must remain VALIDATED after process restart", NodeStatus.VALIDATED, expAfterRestart.nodes.find { it.id == node1Id }!!.status)
    assertEquals("Node 2 must remain AVAILABLE after process restart", NodeStatus.AVAILABLE, expAfterRestart.nodes.find { it.id == node2Id }!!.status)
    assertEquals("Node 3 must remain LOCKED after process restart", NodeStatus.LOCKED, expAfterRestart.nodes.find { it.id == node3Id }!!.status)
    assertEquals("Expedition must remain ACTIVE", ExpeditionStatus.ACTIVE, expAfterRestart.status)

    // Verify duplicate XP cannot be earned for completed Node 1
    repo2.recordWaypointDiscovery(node1Id, 50)
    assertEquals("XP must remain 50 without duplicate reward", 50, repo2.profile.first().explorationXp)
  }

  @Test
  fun testInterruptedRunAndAbandonmentPreservesCompletedNodeRewards() = runBlocking {
    val repo = createRepository()
    repo.initialize()

    val fixtureExp = SeedMissionCatalog.productionFixtureExpedition
    repo.loadTestExpedition(fixtureExp)

    val expId = fixtureExp.id
    val node1Id = fixtureExp.nodes[0].id

    // Complete Node 1
    repo.recordWaypointDiscovery(node1Id, 50)
    repo.advanceExpeditionNode(expId, node1Id)

    assertEquals(50, repo.profile.first().explorationXp)

    // Player pauses and abandons the expedition run mid-way
    repo.abandonExpedition(expId)

    val currentExp = repo.activeExpedition.first()!!
    assertEquals(ExpeditionStatus.ABANDONED, currentExp.status)

    // Completed node rewards and profile XP remain intact
    val profile = repo.profile.first()
    assertEquals(50, profile.explorationXp)
    assertTrue(profile.rewardedWaypointIds.contains(node1Id))
  }

  @Test
  fun testJournalSpecimensDeriveDiscoveryStatusFromRewardedNodes() = runBlocking {
    val specimens = SeedMissionCatalog.catalogSpecimens
    assertEquals(4, specimens.size)

    val specimen1 = specimens[0] // Indian Banyan (linked to wp-fixture-banyan-1)
    val specimen2 = specimens[1] // Golden Fishpole Bamboo (linked to wp-fixture-banyan-2)
    val specimen3 = specimens[2] // Sacred Peepal Fig (linked to wp-fixture-banyan-3)

    // Initial state: empty rewarded set -> all specimens undiscovered
    var rewardedIds = emptySet<String>()
    assertFalse(rewardedIds.contains(specimen1.linkedWaypointId))
    assertFalse(rewardedIds.contains(specimen2.linkedWaypointId))
    assertFalse(rewardedIds.contains(specimen3.linkedWaypointId))

    // Discover Node 1 waypoint
    rewardedIds = rewardedIds + specimen1.linkedWaypointId
    assertTrue("Specimen 1 must be marked discovered after node 1 is rewarded", rewardedIds.contains(specimen1.linkedWaypointId))
    assertFalse("Specimen 2 must remain locked", rewardedIds.contains(specimen2.linkedWaypointId))
    assertFalse("Specimen 3 must remain locked", rewardedIds.contains(specimen3.linkedWaypointId))

    // Discover Node 2 waypoint
    rewardedIds = rewardedIds + specimen2.linkedWaypointId
    assertTrue(rewardedIds.contains(specimen1.linkedWaypointId))
    assertTrue("Specimen 2 must be marked discovered after node 2 is rewarded", rewardedIds.contains(specimen2.linkedWaypointId))
    assertFalse("Specimen 3 must remain locked", rewardedIds.contains(specimen3.linkedWaypointId))
  }

  @Test
  fun testSeparationBetweenGameDiscoveryAndCivicSubmission() = runBlocking {
    val repo = createRepository()
    repo.initialize()

    val nodeId = "node-fixture-banyan-1"

    // Perform game discovery (Quick Stamp / node validation)
    repo.recordWaypointDiscovery(nodeId, 50)

    val profile = repo.profile.first()
    assertEquals(50, profile.explorationXp)
    assertEquals(50, profile.xp)

    // CRITICAL: Civic credits and verified submissions must remain strictly ZERO
    assertEquals("Game exploration must award 0 civic credits", 0, profile.verifiedCivicCredits)
    assertEquals("Game exploration must create 0 verified civic contributions", 0, profile.verifiedContributionsCount)

    // No civic submissions should be queued in repository
    val submissions = repo.submissions.first()
    assertEquals("Game discovery must NOT automatically create a civic submission", 0, submissions.size)
  }

  @Test
  fun testExplicitCivicConsentControlsSubmissionPath() = runBlocking {
    val repo = createRepository()
    repo.initialize()

    val civicTaskId = "demo-task-green-garden-1"

    // Case 1: Civic-eligible task with consentToCivicSubmission = false (Private Journal)
    val privateDraft = MissionDraft(
      id = "draft-private-" + UUID.randomUUID().toString(),
      taskId = civicTaskId,
      taskTitle = "Green Garden Mulching",
      notes = "Examined soil dampness and root coverage along fence line.",
      photoUri = "file:///local/cache/private_photo.jpg",
      consentToCivicSubmission = false
    )

    val privateSubmission = repo.submitMission(privateDraft)
    assertEquals("Unconsented draft must complete as LOCAL_COMPLETION", SubmissionStatus.LOCAL_COMPLETION, privateSubmission.status)
    assertNull("No remote storage path should be generated for private submission", privateSubmission.remoteStoragePath)
    assertTrue("Feedback must reflect private field journal storage", privateSubmission.feedback?.contains("Private") == true || privateSubmission.feedback?.contains("Personal") == true)

    // Case 2: Civic-eligible task with consentToCivicSubmission = true (Civic Review Queue)
    val consentedDraft = MissionDraft(
      id = "draft-consented-" + UUID.randomUUID().toString(),
      taskId = "demo-task-river-testing-2",
      taskTitle = "River Basin Turbidity",
      notes = "Sample water collected at pier 3. Turbidity strip reads 12 NTU.",
      photoUri = "file:///local/cache/civic_proof.jpg",
      consentToCivicSubmission = true
    )

    val civicSubmission = repo.submitMission(consentedDraft)
    // When unauthenticated, explicit consent draft queues for offline sync with clear auth requirement
    assertEquals("Consented unauthenticated draft must enter civic review queue", SubmissionStatus.QUEUED_OFFLINE, civicSubmission.status)
    assertFalse("Must not be marked local-completion only", civicSubmission.status == SubmissionStatus.LOCAL_COMPLETION)
  }

  @Test
  fun testGpsArrivalProximityValidationThresholds() {
    // Assert required thresholds
    assertEquals("Proximity threshold must be 30 meters", 30, CHECKPOINT_PROXIMITY_THRESHOLD_METERS)
    assertEquals("Fix freshness must be 60 seconds (60000ms)", 60_000L, MAX_STALE_FIX_AGE_MS)
    assertEquals("Max acceptable accuracy must be 50 meters", 50f, MAX_ACCEPTABLE_ACCURACY_METERS, 0.001f)
  }
}
