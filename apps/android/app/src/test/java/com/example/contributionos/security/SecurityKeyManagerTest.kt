package com.example.contributionos.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.security.SecureRandom
import javax.crypto.AEADBadTagException

class SecurityKeyManagerTest {

  private val testKey = ByteArray(32).apply { SecureRandom().nextBytes(this) }
  private val provider = SoftwareAesGcmCipherProvider(testKey)

  @Test
  fun testEncryptDecryptRoundTrip() {
    val originalText = "TopSecret-Civic-Field-Observation-12345"
    val originalBytes = originalText.toByteArray(Charsets.UTF_8)

    val encrypted = provider.encrypt(originalBytes)
    assertTrue("Ciphertext should be larger than plaintext due to IV and GCM tag", encrypted.size > originalBytes.size)

    val decrypted = provider.decrypt(encrypted)
    val decryptedText = String(decrypted, Charsets.UTF_8)

    assertEquals("Decrypted text must match original", originalText, decryptedText)
  }

  @Test
  fun testRandomIvProducesDifferentCiphertextForSamePlaintext() {
    val text = "Consistent-Plaintext-Payload"
    val bytes = text.toByteArray(Charsets.UTF_8)

    val cipherA = provider.encrypt(bytes)
    val cipherB = provider.encrypt(bytes)

    assertFalse("Two encryptions of same text must not produce identical ciphertext", cipherA.contentEquals(cipherB))

    assertEquals(text, String(provider.decrypt(cipherA), Charsets.UTF_8))
    assertEquals(text, String(provider.decrypt(cipherB), Charsets.UTF_8))
  }

  @Test
  fun testGcmTamperDetectionFailsDecryption() {
    val text = "Unmodified-Payload"
    val cipher = provider.encrypt(text.toByteArray(Charsets.UTF_8))

    // Flip a bit in the encrypted data region
    val tampered = cipher.copyOf()
    tampered[tampered.size - 1] = (tampered[tampered.size - 1].toInt() xor 0x01).toByte()

    try {
      provider.decrypt(tampered)
      fail("Decryption of tampered ciphertext must throw AEADBadTagException")
    } catch (e: Exception) {
      assertTrue("Exception should indicate auth tag failure or corrupted payload", e is AEADBadTagException || e is IllegalArgumentException)
    }
  }

  @Test
  fun testMalformedPayloadThrowsIllegalArgument() {
    try {
      provider.decrypt(byteArrayOf(0x05, 0x01, 0x02)) // Invalid IV length
      fail("Should reject malformed payload")
    } catch (e: IllegalArgumentException) {
      assertTrue(e.message?.contains("Malformed") == true)
    }
  }

  @Test
  fun testSoftwareCipherReportsNonHardwareBacked() {
    assertFalse("Software AES-GCM provider must report isHardwareBacked=false", provider.isHardwareBacked)
  }
}
