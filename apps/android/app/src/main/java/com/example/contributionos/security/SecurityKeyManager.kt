package com.example.contributionos.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.File
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

interface CipherProvider {
  fun encrypt(plaintext: ByteArray): ByteArray
  fun decrypt(ciphertext: ByteArray): ByteArray
  val isHardwareBacked: Boolean
}

class SoftwareAesGcmCipherProvider(private val secretKey: SecretKey) : CipherProvider {
  private val secureRandom = SecureRandom()

  override val isHardwareBacked: Boolean = false

  constructor(keyBytes: ByteArray) : this(
    SecretKeySpec(
      if (keyBytes.size == 32) keyBytes else keyBytes.copyOf(32),
      "AES"
    )
  )

  override fun encrypt(plaintext: ByteArray): ByteArray {
    val iv = ByteArray(12)
    secureRandom.nextBytes(iv)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
    val encrypted = cipher.doFinal(plaintext)
    return byteArrayOf(iv.size.toByte()) + iv + encrypted
  }

  override fun decrypt(ciphertext: ByteArray): ByteArray {
    if (ciphertext.isEmpty()) return ByteArray(0)
    val ivLength = ciphertext[0].toInt()
    if (ivLength != 12 || ciphertext.size < 1 + ivLength + 16) {
      throw IllegalArgumentException("Malformed AES-GCM ciphertext payload")
    }
    val iv = ciphertext.copyOfRange(1, 1 + ivLength)
    val encryptedData = ciphertext.copyOfRange(1 + ivLength, ciphertext.size)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
    return cipher.doFinal(encryptedData)
  }
}

class AndroidKeyStoreCipherProvider(
  private val alias: String = "civic_secure_master_key"
) : CipherProvider {
  private val keyStore: KeyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }

  override val isHardwareBacked: Boolean
    get() = try {
      val key = getSecretKey()
      val factory = SecretKeyFactory.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
      val keyInfo = factory.getKeySpec(key, KeyInfo::class.java) as KeyInfo
      val insideHardware = keyInfo.isInsideSecureHardware
      Log.i("SecurityKeyManager", "Key '$alias' isInsideSecureHardware: $insideHardware")
      insideHardware
    } catch (e: Throwable) {
      Log.w("SecurityKeyManager", "isInsideSecureHardware query failed: ${e.message}")
      false
    }

  init {
    ensureKeyExists()
    try {
      Log.i("SecurityKeyManager", "AndroidKeyStore cipher initialized (hardwareBacked=$isHardwareBacked)")
    } catch (_: Throwable) {}
  }

  @Synchronized
  private fun ensureKeyExists() {
    if (!keyStore.containsAlias(alias)) {
      generateKey()
    }
  }

  @Synchronized
  private fun generateKey() {
    val keyGenerator = KeyGenerator.getInstance(
      KeyProperties.KEY_ALGORITHM_AES,
      "AndroidKeyStore"
    )
    val spec = KeyGenParameterSpec.Builder(
      alias,
      KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
    )
      .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
      .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
      .setKeySize(256)
      .build()
    keyGenerator.init(spec)
    keyGenerator.generateKey()
  }

  private fun getSecretKey(): SecretKey {
    ensureKeyExists()
    val entry = keyStore.getEntry(alias, null) as? KeyStore.SecretKeyEntry
    return entry?.secretKey ?: throw IllegalStateException("Could not obtain secret key from Keystore")
  }

  override fun encrypt(plaintext: ByteArray): ByteArray {
    return try {
      val cipher = Cipher.getInstance("AES/GCM/NoPadding")
      cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
      val iv = cipher.iv ?: throw IllegalStateException("Cipher failed to generate IV")
      val encrypted = cipher.doFinal(plaintext)
      byteArrayOf(iv.size.toByte()) + iv + encrypted
    } catch (e: Exception) {
      // If key is permanently invalidated or corrupted, regenerate and retry once
      try {
        keyStore.deleteEntry(alias)
        generateKey()
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey())
        val iv = cipher.iv ?: throw IllegalStateException("Cipher failed to generate IV after re-keying")
        val encrypted = cipher.doFinal(plaintext)
        byteArrayOf(iv.size.toByte()) + iv + encrypted
      } catch (retryEx: Exception) {
        throw SecurityException("Local Keystore encryption failure: ${e.message}", retryEx)
      }
    }
  }

  override fun decrypt(ciphertext: ByteArray): ByteArray {
    if (ciphertext.isEmpty()) return ByteArray(0)
    val ivLength = ciphertext[0].toInt()
    if (ivLength != 12 || ciphertext.size < 1 + ivLength + 16) {
      throw IllegalArgumentException("Malformed Keystore AES-GCM payload")
    }
    val iv = ciphertext.copyOfRange(1, 1 + ivLength)
    val encryptedData = ciphertext.copyOfRange(1 + ivLength, ciphertext.size)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), GCMParameterSpec(128, iv))
    return cipher.doFinal(encryptedData)
  }
}

class SecurityKeyManager(
  private val filesDir: File,
  customProvider: CipherProvider? = null
) {
  constructor(context: Context, customProvider: CipherProvider? = null) : this(context.filesDir, customProvider)

  val provider: CipherProvider = customProvider ?: resolveProvider()

  val isHardwareBacked: Boolean get() = provider.isHardwareBacked

  private fun resolveProvider(): CipherProvider {
    return try {
      AndroidKeyStoreCipherProvider()
    } catch (e: Throwable) {
      // Fallback for JVM host unit tests or unsupported Android environments
      val keyFile = File(filesDir, "civic_sec_seed.dat")
      val keyBytes = if (keyFile.exists() && keyFile.length() == 32L) {
        keyFile.readBytes()
      } else {
        val newKey = ByteArray(32)
        SecureRandom().nextBytes(newKey)
        try {
          keyFile.writeBytes(newKey)
        } catch (_: Exception) {}
        newKey
      }
      SoftwareAesGcmCipherProvider(keyBytes)
    }
  }

  fun encryptString(plainText: String): ByteArray {
    return provider.encrypt(plainText.toByteArray(Charsets.UTF_8))
  }

  fun decryptString(cipherBytes: ByteArray): String {
    return String(provider.decrypt(cipherBytes), Charsets.UTF_8)
  }
}
