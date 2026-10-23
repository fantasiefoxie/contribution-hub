package com.example.contributionos.data

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File
import java.nio.file.Files

class SupabaseStorageUploaderTest {

  private lateinit var tempDir: File

  @Before
  fun setUp() {
    tempDir = Files.createTempDirectory("civic_uploader_test").toFile()
  }

  @After
  fun tearDown() {
    tempDir.deleteRecursively()
  }

  @Test
  fun testRejectsBlankAuthToken() = runBlocking {
    // A standalone uploader helper testing parameter and path validation
    val testFile = File(tempDir, "photo.jpg").apply { writeBytes(byteArrayOf(1, 2, 3)) }

    // Test rejection when auth token is empty
    val uploader = SupabaseStorageUploader(
      context = object : android.content.ContextWrapper(null) {}
    )
    val result = uploader.uploadEvidence(
      localUriString = testFile.toURI().toString(),
      userId = "usr-123",
      taskId = "task-456",
      userAccessToken = "" // Blank token
    )

    assertTrue("Must fail when token is blank", result is StorageUploadResult.Failure)
    val failure = result as StorageUploadResult.Failure
    assertTrue("Must be marked as auth error", failure.isAuthError)
  }

  @Test
  fun testRejectsEmptyImageBytes() = runBlocking {
    val emptyFile = File(tempDir, "empty.jpg").apply { writeBytes(ByteArray(0)) }

    val uploader = SupabaseStorageUploader(
      context = object : android.content.ContextWrapper(null) {}
    )
    val result = uploader.uploadEvidence(
      localUriString = emptyFile.toURI().toString(),
      userId = "usr-123",
      taskId = "task-456",
      userAccessToken = "valid-token"
    )

    assertTrue("Must fail for empty file", result is StorageUploadResult.Failure)
    val failure = result as StorageUploadResult.Failure
    assertTrue(failure.message.contains("empty"))
  }

  @Test
  fun testBucketNameAndPathConventions() {
    assertEquals("task-evidence", SupabaseStorageUploader.BUCKET_NAME)
    assertEquals(10 * 1024 * 1024, SupabaseStorageUploader.MAX_FILE_SIZE_BYTES)
  }
}
