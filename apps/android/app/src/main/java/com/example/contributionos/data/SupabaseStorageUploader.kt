package com.example.contributionos.data

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

sealed class StorageUploadResult {
  data class Success(val storagePath: String, val fullStorageKey: String) : StorageUploadResult()
  data class Failure(
    val message: String,
    val statusCode: Int? = null,
    val isNetworkError: Boolean = false,
    val isAuthError: Boolean = false
  ) : StorageUploadResult()
}

class SupabaseStorageUploader(
  private val context: Context,
  val baseUrl: String = "https://dsfbviqvpajvqcyrobsb.supabase.co",
  val anonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRzZmJ2aXF2cGFqdnFjeXJvYnNiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI5NjU1MzksImV4cCI6MjA4ODU0MTUzOX0.me8EIkL4RCPtWIuL5oQwZH1-9XBM6oURLQ9HkGJAIQw"
) {
  companion object {
    const val BUCKET_NAME = "task-evidence"
    const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 // 10 MB
  }

  suspend fun uploadEvidence(
    localUriString: String,
    userId: String,
    taskId: String,
    userAccessToken: String
  ): StorageUploadResult = withContext(Dispatchers.IO) {
    if (userAccessToken.isBlank()) {
      return@withContext StorageUploadResult.Failure("Authentication required for evidence upload", isAuthError = true)
    }

    val imageBytes: ByteArray = try {
      readBytesFromUri(localUriString)
    } catch (e: Exception) {
      return@withContext StorageUploadResult.Failure("Could not read evidence file: ${e.message}")
    }

    if (imageBytes.isEmpty()) {
      return@withContext StorageUploadResult.Failure("Evidence file is empty")
    }

    if (imageBytes.size > MAX_FILE_SIZE_BYTES) {
      return@withContext StorageUploadResult.Failure("Evidence photo exceeds maximum allowed size (10 MB)")
    }

    val sanitizedUserId = userId.replace("[^a-zA-Z0-9_-]".toRegex(), "")
    val sanitizedTaskId = taskId.replace("[^a-zA-Z0-9_-]".toRegex(), "")
    val objectFileName = "${UUID.randomUUID()}.jpg"
    val objectPath = "$sanitizedUserId/$sanitizedTaskId/$objectFileName"
    val fullKey = "$BUCKET_NAME/$objectPath"

    val endpoint = "$baseUrl/storage/v1/object/$BUCKET_NAME/$objectPath"

    try {
      val url = URL(endpoint)
      val conn = url.openConnection() as HttpURLConnection
      conn.requestMethod = "POST"
      conn.connectTimeout = 15000
      conn.readTimeout = 15000
      conn.setRequestProperty("apikey", anonKey)
      conn.setRequestProperty("Authorization", "Bearer $userAccessToken")
      conn.setRequestProperty("Content-Type", "image/jpeg")
      conn.setRequestProperty("x-upsert", "true")
      conn.doOutput = true

      conn.outputStream.use { out ->
        out.write(imageBytes)
        out.flush()
      }

      val code = conn.responseCode
      val stream = if (code in 200..299) conn.inputStream else conn.errorStream
      val body = if (stream != null) {
        BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
      } else {
        ""
      }

      if (code in 200..299) {
        StorageUploadResult.Success(storagePath = objectPath, fullStorageKey = fullKey)
      } else if (code == 401 || code == 403) {
        StorageUploadResult.Failure("Storage access denied ($code): $body", statusCode = code, isAuthError = true)
      } else {
        StorageUploadResult.Failure("Storage upload failed ($code): $body", statusCode = code)
      }
    } catch (e: Exception) {
      StorageUploadResult.Failure("Storage connection error: ${e.message}", isNetworkError = true)
    }
  }

  private fun readBytesFromUri(uriString: String): ByteArray {
    val cleanPath = when {
      uriString.startsWith("file:///") -> uriString.removePrefix("file://")
      uriString.startsWith("file://") -> uriString.removePrefix("file://")
      uriString.startsWith("file:/") -> uriString.removePrefix("file:")
      else -> null
    }

    if (cleanPath != null) {
      val file = File(cleanPath)
      if (file.exists()) {
        return file.readBytes()
      }
    }

    val directFile = File(uriString)
    if (directFile.exists()) {
      return directFile.readBytes()
    }

    // Content resolver fallback (for content:// or FileProvider URIs)
    return try {
      val uri = Uri.parse(uriString)
      val inputStream: InputStream? = context.contentResolver?.openInputStream(uri)
      inputStream?.use { it.readBytes() } ?: directFile.readBytes()
    } catch (_: Exception) {
      directFile.readBytes()
    }
  }
}
