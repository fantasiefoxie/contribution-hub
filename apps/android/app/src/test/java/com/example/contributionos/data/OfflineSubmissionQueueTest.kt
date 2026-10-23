package com.example.contributionos.data

import com.example.contributionos.model.CivicSubmission
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

class OfflineSubmissionQueueTest {

  private lateinit var tempDir: File
  private lateinit var store: LocalDraftStore

  @Before
  fun setUp() {
    tempDir = Files.createTempDirectory("civic_queue_test").toFile()
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
  fun testRequiredLifecycleStatesAreDistinct() {
    val statuses = SubmissionStatus.values()
    assertTrue(statuses.contains(SubmissionStatus.DRAFT_LOCAL))
    assertTrue(statuses.contains(SubmissionStatus.QUEUED_OFFLINE))
    assertTrue(statuses.contains(SubmissionStatus.UPLOADING))
    assertTrue(statuses.contains(SubmissionStatus.SERVER_ACCEPTED))
    assertTrue(statuses.contains(SubmissionStatus.PENDING_REVIEW))
    assertTrue(statuses.contains(SubmissionStatus.APPROVED))
    assertTrue(statuses.contains(SubmissionStatus.REJECTED))
    assertTrue(statuses.contains(SubmissionStatus.RETRYABLE_FAILURE))
    assertTrue(statuses.contains(SubmissionStatus.PERMANENT_FAILURE))
  }

  @Test
  fun testOfflineQueueStateTransitions() = runBlocking {
    val clientSubmissionId = "client-sub-" + UUID.randomUUID().toString()
    val taskId = "task-urban-water-5"
    val localEvidencePath = "file:///data/user/0/com.example.contributionos/cache/evidence/photo_456.jpg"

    // 1. Initial creation: queued offline
    val queued = CivicSubmission(
      id = clientSubmissionId,
      taskId = taskId,
      taskTitle = "Creek Riparian Buffer Inspection",
      submittedAt = System.currentTimeMillis(),
      content = "Planted 15 native willow cuttings along western bank.",
      status = SubmissionStatus.QUEUED_OFFLINE,
      feedback = "Queued safely in local encrypted storage.",
      isOfflineOrigin = true,
      isSandboxDemo = false,
      clientSubmissionId = clientSubmissionId,
      localPhotoUri = localEvidencePath
    )
    store.saveSubmission(queued)

    var currentList = store.getSubmissions()
    assertEquals(1, currentList.size)
    assertEquals(SubmissionStatus.QUEUED_OFFLINE, currentList[0].status)
    assertEquals(clientSubmissionId, currentList[0].clientSubmissionId)
    assertEquals(localEvidencePath, currentList[0].localPhotoUri)

    // 2. Sync Worker starts: transitions to UPLOADING
    val uploading = currentList[0].copy(
      status = SubmissionStatus.UPLOADING,
      feedback = "Uploading photographic evidence to Supabase Storage..."
    )
    store.saveSubmission(uploading)
    currentList = store.getSubmissions()
    assertEquals(1, currentList.size)
    assertEquals(SubmissionStatus.UPLOADING, currentList[0].status)

    // 3. Temporary network failure: transitions to RETRYABLE_FAILURE (preserving notes and image)
    val retryable = currentList[0].copy(
      status = SubmissionStatus.RETRYABLE_FAILURE,
      syncErrorMessage = "Connection reset by peer (offline)"
    )
    store.saveSubmission(retryable)
    currentList = store.getSubmissions()
    assertEquals(1, currentList.size)
    assertEquals(SubmissionStatus.RETRYABLE_FAILURE, currentList[0].status)
    assertEquals(localEvidencePath, currentList[0].localPhotoUri) // Evidence preserved!

    // 4. Successful sync: transitions to SERVER_ACCEPTED with remote ID
    val serverAccepted = currentList[0].copy(
      status = SubmissionStatus.SERVER_ACCEPTED,
      remoteSubmissionId = "srv-remote-uuid-789",
      remoteStoragePath = "task-evidence/usr-1/$taskId/photo.jpg",
      syncErrorMessage = null,
      feedback = "Acknowledged by Civic Registry. Awaiting accredited validator."
    )
    store.saveSubmission(serverAccepted)
    currentList = store.getSubmissions()
    assertEquals(1, currentList.size)
    assertEquals(SubmissionStatus.SERVER_ACCEPTED, currentList[0].status)
    assertEquals("srv-remote-uuid-789", currentList[0].remoteSubmissionId)
  }

  @Test
  fun testSandboxIsolationFromProduction() = runBlocking {
    val sandboxSub = CivicSubmission(
      id = "sub-demo-1",
      taskId = "demo-task-1",
      taskTitle = "[DEMO] Tree Audit",
      submittedAt = System.currentTimeMillis(),
      content = "Illustrative notes",
      status = SubmissionStatus.PENDING_REVIEW,
      isSandboxDemo = true
    )
    val realSub = CivicSubmission(
      id = "sub-prod-1",
      taskId = "prod-task-1",
      taskTitle = "Real Verified Action",
      submittedAt = System.currentTimeMillis(),
      content = "Authentic field evidence",
      status = SubmissionStatus.QUEUED_OFFLINE,
      isSandboxDemo = false
    )

    store.saveSubmission(sandboxSub)
    store.saveSubmission(realSub)

    val all = store.getSubmissions()
    val demoSubmissions = all.filter { it.isSandboxDemo }
    val prodSubmissions = all.filter { !it.isSandboxDemo }

    assertEquals(1, demoSubmissions.size)
    assertEquals(1, prodSubmissions.size)
    assertTrue("Demo submission must have isSandboxDemo flag", demoSubmissions[0].isSandboxDemo)
    assertFalse("Prod submission must NOT have isSandboxDemo flag", prodSubmissions[0].isSandboxDemo)
  }
}
