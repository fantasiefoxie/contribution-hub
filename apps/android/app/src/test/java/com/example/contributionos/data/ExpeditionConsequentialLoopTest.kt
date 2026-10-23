package com.example.contributionos.data

import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.ExpeditionStatus
import com.example.contributionos.model.NodeStatus
import com.example.contributionos.security.SecurityKeyManager
import com.example.contributionos.security.SoftwareAesGcmCipherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.security.SecureRandom

class ExpeditionConsequentialLoopTest {

  private lateinit var tempDir: File
  private lateinit var store: LocalDraftStore
  private lateinit var keyManager: SecurityKeyManager

  @Before
  fun setUp() {
    tempDir = Files.createTempDirectory("consequential_loop_test").toFile()
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
  fun testBranchLockingEnforcesMutualExclusion(): Unit = runBlocking {
    val repo = createRepository()
    repo.initialize()

    val exp = SeedMissionCatalog.branchingCanopyExpedition
    repo.startExpeditionRun(exp.id, stance = "TREK")

    var currentExp = repo.activeExpedition.first()!!
    assertEquals(NodeStatus.AVAILABLE, currentExp.nodes.find { it.id == "node-canopy-1" }!!.status)
    assertEquals(NodeStatus.LOCKED, currentExp.nodes.find { it.id == "node-canopy-2A" }!!.status)
    assertEquals(NodeStatus.LOCKED, currentExp.nodes.find { it.id == "node-canopy-2B" }!!.status)

    // 1. Resolve Tier 1 -> Unlocks both 2A and 2B
    repo.resolveEncounterChoice(exp.id, "node-canopy-1", "c-leaf-litter")
    currentExp = repo.activeExpedition.first()!!
    assertEquals(NodeStatus.VALIDATED, currentExp.nodes.find { it.id == "node-canopy-1" }!!.status)
    assertEquals(NodeStatus.AVAILABLE, currentExp.nodes.find { it.id == "node-canopy-2A" }!!.status)
    assertEquals(NodeStatus.AVAILABLE, currentExp.nodes.find { it.id == "node-canopy-2B" }!!.status)

    // 2. Commit to Branch 2A -> Locks Branch 2B
    repo.commitToBranch(exp.id, "node-canopy-2A")
    currentExp = repo.activeExpedition.first()!!
    assertEquals(NodeStatus.IN_PROGRESS, currentExp.nodes.find { it.id == "node-canopy-2A" }!!.status)
    assertEquals(NodeStatus.LOCKED, currentExp.nodes.find { it.id == "node-canopy-2B" }!!.status)

    // 3. Attempting to resolve choice on locked 2B must fail with IllegalStateException
    try {
      repo.resolveEncounterChoice(exp.id, "node-canopy-2B", "c-climb-roots")
      fail("Resolving choice on a locked branch must throw IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message!!.contains("locked"))
    }
  }

  @Test
  fun testDistinctChoiceOutcomesProduceDistinctProgression(): Unit = runBlocking {
    val exp = SeedMissionCatalog.branchingCanopyExpedition

    // Run A: Choose Choice 1 (Measure nodal rings) -> Unlocks Specimen sp-phyllostachys-aurea
    val repoA = createRepository()
    repoA.initialize()
    repoA.startExpeditionRun(exp.id)
    repoA.resolveEncounterChoice(exp.id, "node-canopy-1", "c-leaf-litter")
    repoA.resolveEncounterChoice(exp.id, "node-canopy-2A", "c-measure-rings")

    val profileA = repoA.profile.first()
    assertTrue("Choice 1 must unlock Golden Bamboo specimen", profileA.chronicledSpecimenIds.contains("sp-phyllostachys-aurea"))
    assertFalse("Choice 1 must NOT award audio wind stamp", profileA.mintedStampIds.contains("stamp-bioacoustic-wind"))

    // Run B (fresh repository): Choose Choice 2 (Record wind acoustic) -> Awards Stamp stamp-bioacoustic-wind
    val tempDirB = Files.createTempDirectory("consequential_loop_test_b").toFile()
    val testKeyB = ByteArray(32).apply { SecureRandom().nextBytes(this) }
    val cipherProviderB = SoftwareAesGcmCipherProvider(testKeyB)
    val keyManagerB = SecurityKeyManager(tempDirB, cipherProviderB)
    val storeB = LocalDraftStore(tempDirB, keyManagerB)
    val repoB = createRepository(storeB)
    repoB.initialize()

    repoB.startExpeditionRun(exp.id)
    repoB.resolveEncounterChoice(exp.id, "node-canopy-1", "c-leaf-litter")
    repoB.resolveEncounterChoice(exp.id, "node-canopy-2A", "c-audio-wind")

    val profileB = repoB.profile.first()
    assertTrue("Choice 2 must award audio wind stamp", profileB.mintedStampIds.contains("stamp-bioacoustic-wind"))
    assertFalse("Choice 2 must NOT unlock Golden Bamboo specimen", profileB.chronicledSpecimenIds.contains("sp-phyllostachys-aurea"))

    tempDirB.deleteRecursively()
  }

  @Test
  fun testConsequentialClueUnlocksSpecialChoiceAtTier2(): Unit = runBlocking {
    val exp = SeedMissionCatalog.branchingCanopyExpedition

    // Case 1: Player picks c-leaf-litter at Tier 1 -> Grants clue-bamboo-resonance
    val repo1 = createRepository()
    repo1.initialize()
    repo1.startExpeditionRun(exp.id)
    repo1.resolveEncounterChoice(exp.id, "node-canopy-1", "c-leaf-litter")

    val activeRun1 = repo1.profile.first().activeRun!!
    assertTrue("Active run must hold discovered clue", activeRun1.discoveredClueIds.contains("clue-bamboo-resonance"))

    // Special clue-gated choice c-bamboo-flute succeeds!
    repo1.resolveEncounterChoice(exp.id, "node-canopy-2A", "c-bamboo-flute")
    val profile1 = repo1.profile.first()
    assertTrue("Special choice must unlock specimen", profile1.chronicledSpecimenIds.contains("sp-phyllostachys-aurea"))
    assertTrue("Special choice must award resonance master stamp", profile1.mintedStampIds.contains("stamp-resonance-master"))

    // Case 2: Player picks c-archway-moss at Tier 1 -> Grants clue-solar-cavity (NOT clue-bamboo-resonance)
    val tempDir2 = Files.createTempDirectory("consequential_loop_test_2").toFile()
    val testKey2 = ByteArray(32).apply { SecureRandom().nextBytes(this) }
    val cipherProvider2 = SoftwareAesGcmCipherProvider(testKey2)
    val keyManager2 = SecurityKeyManager(tempDir2, cipherProvider2)
    val store2 = LocalDraftStore(tempDir2, keyManager2)
    val repo2 = createRepository(store2)
    repo2.initialize()

    repo2.startExpeditionRun(exp.id)
    repo2.resolveEncounterChoice(exp.id, "node-canopy-1", "c-archway-moss")

    // Attempting c-bamboo-flute without the required clue fails
    try {
      repo2.resolveEncounterChoice(exp.id, "node-canopy-2A", "c-bamboo-flute")
      fail("Attempting clue-gated choice without prerequisite clue must throw IllegalStateException")
    } catch (e: IllegalStateException) {
      assertTrue(e.message!!.contains("requires missing clue"))
    }

    tempDir2.deleteRecursively()
  }

  @Test
  fun testProgressionConceptsAreStrictlySeparated(): Unit = runBlocking {
    val repo = createRepository()
    repo.initialize()

    val exp = SeedMissionCatalog.branchingCanopyExpedition
    repo.startExpeditionRun(exp.id)

    // Complete Tier 1 (50 XP), Tier 2A (50 XP), Tier 3 (50 XP + 100 run bonus = 250 XP total)
    repo.resolveEncounterChoice(exp.id, "node-canopy-1", "c-leaf-litter")
    repo.resolveEncounterChoice(exp.id, "node-canopy-2A", "c-measure-rings")
    val isCompleted = repo.resolveEncounterChoice(exp.id, "node-canopy-3", "c-matriarch-chronicle")

    assertTrue("Expedition must be completed", isCompleted)
    val profile = repo.profile.first()

    // 1. Exploration XP
    assertEquals(250, profile.explorationXp)

    // 2. Civic Credits must remain exactly 0 (strict boundary)
    assertEquals(0, profile.verifiedCivicCredits)

    // 3. Completed node IDs (3 unique nodes)
    assertEquals(3, profile.completedNodeIds.size)
    assertTrue(profile.completedNodeIds.contains("node-canopy-1"))
    assertTrue(profile.completedNodeIds.contains("node-canopy-2A"))
    assertTrue(profile.completedNodeIds.contains("node-canopy-3"))

    // 4. Chronicled specimens (2 specimens unlocked)
    assertEquals(2, profile.chronicledSpecimenIds.size)
    assertTrue(profile.chronicledSpecimenIds.contains("sp-phyllostachys-aurea"))
    assertTrue(profile.chronicledSpecimenIds.contains("sp-ficus-benghalensis"))

    // 5. Minted stamps (1 stamp unlocked)
    assertEquals(1, profile.mintedStampIds.size)
    assertTrue(profile.mintedStampIds.contains("stamp-matriarch-seal"))

    // 6. Completed run instance ID recorded
    assertEquals(1, profile.completedRunIds.size)
    assertTrue(profile.completedRunIds.first().startsWith("run-demo-expedition-emerald-canopy"))
  }

  @Test
  fun testAppRestartPreservesLockedBranchesAndSelectedChoices(): Unit = runBlocking {
    val repo1 = createRepository()
    repo1.initialize()

    val exp = SeedMissionCatalog.branchingCanopyExpedition
    repo1.startExpeditionRun(exp.id)
    repo1.resolveEncounterChoice(exp.id, "node-canopy-1", "c-leaf-litter")
    repo1.resolveEncounterChoice(exp.id, "node-canopy-2A", "c-measure-rings")

    val profileBefore = repo1.profile.first()
    assertEquals(100, profileBefore.explorationXp)

    // Simulate process death and app restart
    val repo2 = createRepository()
    repo2.initialize()

    val profileAfter = repo2.profile.first()
    assertEquals(100, profileAfter.explorationXp)
    assertTrue(profileAfter.chronicledSpecimenIds.contains("sp-phyllostachys-aurea"))

    val activeExpAfter = repo2.activeExpedition.first()!!
    val node1 = activeExpAfter.nodes.find { it.id == "node-canopy-1" }!!
    val node2A = activeExpAfter.nodes.find { it.id == "node-canopy-2A" }!!
    val node2B = activeExpAfter.nodes.find { it.id == "node-canopy-2B" }!!

    assertEquals(NodeStatus.VALIDATED, node1.status)
    assertEquals("c-leaf-litter", node1.selectedChoiceId)

    assertEquals(NodeStatus.VALIDATED, node2A.status)
    assertEquals("c-measure-rings", node2A.selectedChoiceId)

    assertEquals("Competing branch 2B must remain LOCKED after restart", NodeStatus.LOCKED, node2B.status)

    // Attempting to resolve locked branch 2B after restart must still be rejected
    try {
      repo2.resolveEncounterChoice(exp.id, "node-canopy-2B", "c-climb-roots")
      fail("Locked branch must stay locked after restart")
    } catch (e: IllegalStateException) {
      assertTrue(e.message!!.contains("locked"))
    }
  }

  @Test
  fun testReplayPolicyAllowsCollectingRemainingSpecimenWithoutDuplicatingNodeXP(): Unit = runBlocking {
    val repo = createRepository()
    repo.initialize()

    val exp = SeedMissionCatalog.branchingCanopyExpedition

    // --- RUN 1: Choose Branch 2A ---
    repo.startExpeditionRun(exp.id)
    repo.resolveEncounterChoice(exp.id, "node-canopy-1", "c-leaf-litter")   // +50 XP
    repo.resolveEncounterChoice(exp.id, "node-canopy-2A", "c-measure-rings")// +50 XP
    repo.resolveEncounterChoice(exp.id, "node-canopy-3", "c-matriarch-chronicle") // +50 XP + 100 run bonus = 250 XP

    val prof1 = repo.profile.first()
    assertEquals(250, prof1.explorationXp)
    assertTrue("Run 1 chronicles Golden Bamboo", prof1.chronicledSpecimenIds.contains("sp-phyllostachys-aurea"))
    assertFalse("Run 1 does not chronicle Strangler Fig", prof1.chronicledSpecimenIds.contains("sp-ficus-microcarpa"))

    // --- RUN 2: Replay same expedition, Choose Branch 2B ---
    repo.startExpeditionRun(exp.id) // Fresh run instance
    val run2Exp = repo.activeExpedition.first()!!
    assertEquals(NodeStatus.AVAILABLE, run2Exp.nodes.find { it.id == "node-canopy-1" }!!.status)

    // Node 1: already completed previously -> +0 XP
    repo.resolveEncounterChoice(exp.id, "node-canopy-1", "c-archway-moss")
    assertEquals("Repeated node 1 must NOT grant duplicate XP", 250, repo.profile.first().explorationXp)

    // Node 2B: FIRST time completing Node 2B -> +50 XP, unlocks Strangler Fig!
    repo.resolveEncounterChoice(exp.id, "node-canopy-2B", "c-climb-roots")
    val prof2Mid = repo.profile.first()
    assertEquals("First time completing 2B must award 50 XP", 300, prof2Mid.explorationXp)
    assertTrue("Run 2 chronicles Strangler Fig", prof2Mid.chronicledSpecimenIds.contains("sp-ficus-microcarpa"))
    assertTrue("Golden Bamboo from Run 1 remains chronicled", prof2Mid.chronicledSpecimenIds.contains("sp-phyllostachys-aurea"))

    // Node 3: already completed previously -> +0 node XP, but Run 2 completion bonus (+100 XP) is awarded!
    val isCompletedRun2 = repo.resolveEncounterChoice(exp.id, "node-canopy-3", "c-matriarch-chronicle")
    assertTrue(isCompletedRun2)

    val prof2Final = repo.profile.first()
    assertEquals("Run 2 completion bonus (100) added to 300 = 400 XP", 400, prof2Final.explorationXp)
    assertEquals("Total completed runs count must be 2", 2, prof2Final.completedRunIds.size)
  }

  @Test
  fun testStationaryParitySharesIdenticalGraphAndRewards(): Unit = runBlocking {
    val repo = createRepository()
    repo.initialize()

    val exp = SeedMissionCatalog.branchingCanopyExpedition

    // Start run with STATIONARY stance
    val stationaryExp = repo.startExpeditionRun(exp.id, stance = "STATIONARY")
    assertEquals("STATIONARY", stationaryExp.activeStance)

    repo.resolveEncounterChoice(exp.id, "node-canopy-1", "c-leaf-litter")
    repo.resolveEncounterChoice(exp.id, "node-canopy-2A", "c-measure-rings")
    val isCompleted = repo.resolveEncounterChoice(exp.id, "node-canopy-3", "c-matriarch-chronicle")

    assertTrue(isCompleted)
    val profile = repo.profile.first()

    // Identical rewards and unlocks
    assertEquals("Stationary mode must award full 250 XP", 250, profile.explorationXp)
    assertTrue("Stationary mode unlocks Golden Bamboo", profile.chronicledSpecimenIds.contains("sp-phyllostachys-aurea"))
    assertTrue("Stationary mode unlocks Indian Banyan", profile.chronicledSpecimenIds.contains("sp-ficus-benghalensis"))
    assertTrue("Stationary mode mints seal", profile.mintedStampIds.contains("stamp-matriarch-seal"))
  }
}
