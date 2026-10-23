package com.example.contributionos.data

import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.MissionDraft
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

class SecureDraftStoreTest {

  private lateinit var tempDir: File
  private lateinit var keyManager: SecurityKeyManager
  private lateinit var store: LocalDraftStore

  @Before
  fun setUp() {
    tempDir = Files.createTempDirectory("civic_store_test").toFile()
    val testKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
    val cipherProvider = SoftwareAesGcmCipherProvider(testKey)
    keyManager = SecurityKeyManager(tempDir, cipherProvider)
    store = LocalDraftStore(tempDir, keyManager)
  }

  @After
  fun tearDown() {
    tempDir.deleteRecursively()
  }

  @Test
  fun testSaveAndRetrieveDraftEncrypted() = runBlocking {
    val draft = MissionDraft(
      id = "draft-sec-1",
      taskId = "task-urban-1",
      taskTitle = "Urban Canopy Heat Mapping",
      notes = "Surface temperature recorded at 38.4C on asphalt, 27.2C under canopy.",
      photoUri = "file:///storage/emulated/0/civic/tree.jpg",
      latitude = 37.7749,
      longitude = -122.4194,
      isOfflinePending = true,
      isSandboxDemo = false
    )

    store.saveDraft(draft)

    // Verify file is encrypted on disk and does NOT contain plaintext
    val encFile = File(tempDir, "civic_secure_drafts.enc")
    assertTrue("Encrypted drafts file must exist on disk", encFile.exists())
    val rawBytes = encFile.readBytes()
    val rawString = String(rawBytes, Charsets.ISO_8859_1)
    assertFalse("Encrypted file must NOT contain plaintext notes", rawString.contains("Surface temperature recorded"))

    // Verify store reads and decrypts properly
    val loadedDrafts = store.getDrafts()
    assertEquals(1, loadedDrafts.size)
    assertEquals(draft.id, loadedDrafts[0].id)
    assertEquals(draft.notes, loadedDrafts[0].notes)
    assertEquals(draft.latitude, loadedDrafts[0].latitude)
  }

  @Test
  fun testLegacyDraftMigration() = runBlocking {
    // Write legacy plain JSON file
    val legacyFile = File(tempDir, "civic_mission_drafts.json")
    val legacyJson = """
      [
        {
          "id": "legacy-draft-42",
          "taskId": "task-legacy-1",
          "taskTitle": "Community Solar Survey",
          "notes": "Verified rooftop orientation facing South-West.",
          "photoUri": null,
          "latitude": 34.0522,
          "longitude": -118.2437,
          "timestamp": 1700000000000,
          "isOfflinePending": true,
          "isSandboxDemo": false
        }
      ]
    """.trimIndent()
    legacyFile.writeText(legacyJson)

    assertTrue("Legacy JSON exists before store access", legacyFile.exists())

    // Initializing / accessing store should migrate to encrypted file and delete plain file
    val drafts = store.getDrafts()
    assertEquals(1, drafts.size)
    assertEquals("legacy-draft-42", drafts[0].id)
    assertEquals("Community Solar Survey", drafts[0].taskTitle)

    assertFalse("Legacy unencrypted JSON file must be deleted after migration", legacyFile.exists())
    val encFile = File(tempDir, "civic_secure_drafts.enc")
    assertTrue("Encrypted file must exist after migration", encFile.exists())
  }

  @Test
  fun testSubmissionDeduplicationAndIdempotency() = runBlocking {
    val subId = "client-sub-100"
    val sub1 = CivicSubmission(
      id = subId,
      taskId = "task-recycling-1",
      taskTitle = "Battery Drop-off Verification",
      submittedAt = 1000L,
      content = "Deposited 4 lithium-ion cells at depot.",
      status = SubmissionStatus.QUEUED_OFFLINE,
      clientSubmissionId = "client-sub-100"
    )

    store.saveSubmission(sub1)
    assertEquals(1, store.getSubmissions().size)

    // Update the same submission to UPLOADING and SERVER_ACCEPTED
    val sub2 = sub1.copy(
      status = SubmissionStatus.SERVER_ACCEPTED,
      remoteSubmissionId = "srv-999",
      feedback = "Accepted by server"
    )
    store.saveSubmission(sub2)

    val all = store.getSubmissions()
    assertEquals("Updating submission with matching clientSubmissionId must not duplicate entry", 1, all.size)
    assertEquals(SubmissionStatus.SERVER_ACCEPTED, all[0].status)
    assertEquals("srv-999", all[0].remoteSubmissionId)
  }

  @Test
  fun testCorruptedFileResilienceDoesNotCrash() = runBlocking {
    val encFile = File(tempDir, "civic_secure_drafts.enc")
    encFile.writeBytes(byteArrayOf(0x00, 0x11, 0x22, 0x33, 0x44)) // Invalid payload

    val drafts = store.getDrafts()
    assertTrue("Corrupted encrypted file must return empty list safely without crashing", drafts.isEmpty())
  }

  @Test
  fun testProfileReplacementFailureRecoversFromBackup() = runBlocking {
    val initialProfile = CivicProfile(
      id = "user-recovery",
      displayName = "Citizen Resilient",
      callsign = "Resilient-01",
      level = 1,
      xp = 120,
      explorationXp = 120,
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

    // Save initial profile
    store.saveProfile(initialProfile)
    val loaded = store.getProfile()
    assertNotNull(loaded)
    assertEquals(120, loaded!!.explorationXp)

    val primaryFile = File(tempDir, "civic_secure_profile.enc")
    val backupFile = File(tempDir, "civic_secure_profile.enc.bak")
    assertTrue("Primary profile file must exist after save", primaryFile.exists())

    // Simulate scenario: a replacement was in progress, backup was taken, but the primary file was corrupted or wiped mid-crash
    primaryFile.copyTo(backupFile, overwrite = true)
    primaryFile.writeBytes(byteArrayOf(0x00, 0x01, 0x02)) // Corrupted / partial write in primary

    // Store should detect corruption, recover from backup, and restore valid profile
    val recovered = store.getProfile()
    assertNotNull("Must recover profile from backup file", recovered)
    assertEquals("Recovered profile must match previous valid XP", 120, recovered!!.explorationXp)
    assertEquals(setOf("node-1", "node-2"), recovered.rewardedWaypointIds)

    // Also verify getRewardedNodeIds recovers through backup path
    val rewarded = store.getRewardedNodeIds()
    assertEquals(setOf("node-1", "node-2"), rewarded)
  }

  @Test
  fun testProfileSafeCommitPreservesExistingWhenPrimaryFileMissing() = runBlocking {
    val initialProfile = CivicProfile(
      id = "user-missing-primary",
      displayName = "Citizen Safe",
      callsign = "Safe-01",
      level = 2,
      xp = 250,
      explorationXp = 250,
      verifiedCivicCredits = 0,
      nextLevelXp = 500,
      rankTitle = "Field Candidate",
      verifiedContributionsCount = 0,
      completedExpeditionsCount = 0,
      activeStreakDays = 2,
      discoveredWaypointsCount = 4,
      specialization = "Community Stewardship",
      badges = emptyList(),
      rewardedWaypointIds = setOf("node-A", "node-B"),
      isSandboxDemo = false
    )

    store.saveProfile(initialProfile)

    val primaryFile = File(tempDir, "civic_secure_profile.enc")
    val backupFile = File(tempDir, "civic_secure_profile.enc.bak")

    // Simulate crash after primary file was unlinked but before new file was renamed in place
    primaryFile.copyTo(backupFile, overwrite = true)
    primaryFile.delete()

    assertFalse("Primary file missing", primaryFile.exists())
    assertTrue("Backup file exists", backupFile.exists())

    // Loading profile should recover from backup and re-establish primary file
    val recovered = store.getProfile()
    assertNotNull("Must recover profile from backup when primary file is missing", recovered)
    assertEquals(250, recovered!!.explorationXp)
    assertTrue("Recovered profile should restore primary file on disk", primaryFile.exists())
  }
}
