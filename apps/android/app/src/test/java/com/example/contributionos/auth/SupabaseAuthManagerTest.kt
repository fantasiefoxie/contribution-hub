package com.example.contributionos.auth

import com.example.contributionos.security.SecurityKeyManager
import com.example.contributionos.security.SoftwareAesGcmCipherProvider
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

class SupabaseAuthManagerTest {

  private lateinit var tempDir: File
  private lateinit var keyManager: SecurityKeyManager
  private lateinit var authManager: SupabaseAuthManager

  @Before
  fun setUp() {
    tempDir = Files.createTempDirectory("civic_auth_test").toFile()
    val testKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
    val cipherProvider = SoftwareAesGcmCipherProvider(testKey)
    keyManager = SecurityKeyManager(tempDir, cipherProvider)
    authManager = SupabaseAuthManager(tempDir, keyManager = keyManager)
  }

  @After
  fun tearDown() {
    tempDir.deleteRecursively()
  }

  @Test
  fun testAuthSessionExpirationBuffer() {
    val now = System.currentTimeMillis()

    // Valid session expiring in 1 hour
    val validSession = AuthSession(
      userId = "usr-1",
      email = "citizen@civic.org",
      accessToken = "tok-valid",
      refreshToken = "ref-valid",
      expiresAtMillis = now + 3600_000L
    )
    assertFalse("Session expiring in 1 hour should not be expired", validSession.isExpired)

    // Session expiring in 30 seconds (within 60s buffer)
    val expiringSession = AuthSession(
      userId = "usr-2",
      email = "citizen2@civic.org",
      accessToken = "tok-expiring",
      refreshToken = "ref-expiring",
      expiresAtMillis = now + 30_000L
    )
    assertTrue("Session expiring within 60s buffer must evaluate as expired to trigger early refresh", expiringSession.isExpired)

    // Session already expired in past
    val expiredSession = AuthSession(
      userId = "usr-3",
      email = "citizen3@civic.org",
      accessToken = "tok-past",
      refreshToken = "ref-past",
      expiresAtMillis = now - 5_000L
    )
    assertTrue("Session with past expiration must be expired", expiredSession.isExpired)
  }

  @Test
  fun testEncryptedSessionPersistenceRoundTrip() = runBlocking {
    val session = AuthSession(
      userId = "usr-secure-42",
      email = "field.validator@civic.district",
      accessToken = "jwt.access.token.secret",
      refreshToken = "jwt.refresh.token.secret",
      expiresAtMillis = System.currentTimeMillis() + 1000000L
    )

    authManager.updateLocalSessionDirectly(session)

    // Verify session file is encrypted
    val sessionFile = File(tempDir, "civic_auth_session.enc")
    assertTrue("Session file must exist on disk", sessionFile.exists())
    val rawBytes = sessionFile.readBytes()
    val rawString = String(rawBytes, Charsets.ISO_8859_1)
    assertFalse("Session file must NOT contain plaintext tokens", rawString.contains("jwt.access.token.secret"))

    // Verify another manager instance reading this directory decrypts session accurately
    val secondManager = SupabaseAuthManager(tempDir, keyManager = keyManager)
    secondManager.initialize()

    val loaded = secondManager.session.value
    assertNotNull("Loaded session must not be null", loaded)
    assertEquals(session.userId, loaded?.userId)
    assertEquals(session.email, loaded?.email)
    assertEquals(session.accessToken, loaded?.accessToken)
  }

  @Test
  fun testSignOutClearsEncryptedSession() = runBlocking {
    val session = AuthSession(
      userId = "usr-test",
      email = "test@civic.org",
      accessToken = "tok",
      refreshToken = "ref",
      expiresAtMillis = System.currentTimeMillis() + 100000L
    )
    authManager.updateLocalSessionDirectly(session)
    assertTrue(File(tempDir, "civic_auth_session.enc").exists())

    authManager.signOut()

    assertNull("Session state must be null after sign out", authManager.session.value)
    assertFalse("Session file must be deleted on sign out", File(tempDir, "civic_auth_session.enc").exists())
  }
}
