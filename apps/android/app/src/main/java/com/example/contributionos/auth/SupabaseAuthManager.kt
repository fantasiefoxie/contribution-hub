package com.example.contributionos.auth

import android.content.Context
import android.util.Log
import com.example.contributionos.security.SecurityKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

@Serializable
data class AuthSession(
  val userId: String,
  val email: String,
  val accessToken: String,
  val refreshToken: String,
  val expiresAtMillis: Long
) {
  val isExpired: Boolean
    get() = System.currentTimeMillis() >= (expiresAtMillis - 60_000L)
}

sealed class AuthResult<out T> {
  data class Success<T>(val data: T) : AuthResult<T>()
  data class Failure(val message: String, val statusCode: Int? = null, val isNetworkError: Boolean = false) : AuthResult<Nothing>()
}

class SupabaseAuthManager(
  private val filesDir: File,
  val baseUrl: String = "https://dsfbviqvpajvqcyrobsb.supabase.co",
  val anonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRzZmJ2aXF2cGFqdnFjeXJvYnNiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI5NjU1MzksImV4cCI6MjA4ODU0MTUzOX0.me8EIkL4RCPtWIuL5oQwZH1-9XBM6oURLQ9HkGJAIQw",
  private val keyManager: SecurityKeyManager = SecurityKeyManager(filesDir)
) {
  constructor(
    context: Context,
    baseUrl: String = "https://dsfbviqvpajvqcyrobsb.supabase.co",
    anonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRzZmJ2aXF2cGFqdnFjeXJvYnNiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI5NjU1MzksImV4cCI6MjA4ODU0MTUzOX0.me8EIkL4RCPtWIuL5oQwZH1-9XBM6oURLQ9HkGJAIQw",
    keyManager: SecurityKeyManager = SecurityKeyManager(context)
  ) : this(context.filesDir, baseUrl, anonKey, keyManager)

  private val jsonParser = Json { ignoreUnknownKeys = true }
  private val mutex = Mutex()

  private val sessionFile: File
    get() = File(filesDir, "civic_auth_session.enc")

  private val _session = MutableStateFlow<AuthSession?>(null)
  val session: StateFlow<AuthSession?> = _session.asStateFlow()

  suspend fun initialize() = withContext(Dispatchers.IO) {
    mutex.withLock {
      val loaded = readEncryptedSession()
      _session.value = loaded
    }
  }

  suspend fun signIn(email: String, password: String): AuthResult<AuthSession> = withContext(Dispatchers.IO) {
    try {
      val endpoint = "$baseUrl/auth/v1/token?grant_type=password"
      val payload = buildString {
        append("{\"email\":").append(Json.encodeToString(email))
        append(",\"password\":").append(Json.encodeToString(password))
        append("}")
      }
      val (code, body) = executeHttpRequest(endpoint, "POST", payload)
      if (code in 200..299) {
        val root = jsonParser.parseToJsonElement(body).jsonObject
        val accessToken = root["access_token"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing access_token")
        val refreshToken = root["refresh_token"]?.jsonPrimitive?.content ?: ""
        val expiresInSeconds = root["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
        val userObj = root["user"]?.jsonObject
        val userId = userObj?.get("id")?.jsonPrimitive?.content ?: "user-${System.currentTimeMillis()}"
        val userEmail = userObj?.get("email")?.jsonPrimitive?.content ?: email

        val newSession = AuthSession(
          userId = userId,
          email = userEmail,
          accessToken = accessToken,
          refreshToken = refreshToken,
          expiresAtMillis = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        )

        mutex.withLock {
          writeEncryptedSession(newSession)
          _session.value = newSession
        }
        AuthResult.Success(newSession)
      } else {
        AuthResult.Failure("Sign in failed ($code): $body", code)
      }
    } catch (e: Exception) {
      AuthResult.Failure("Network error during sign in: ${e.message}", isNetworkError = true)
    }
  }

  suspend fun signUp(email: String, password: String): AuthResult<AuthSession> = withContext(Dispatchers.IO) {
    try {
      val endpoint = "$baseUrl/auth/v1/signup"
      val payload = buildString {
        append("{\"email\":").append(Json.encodeToString(email))
        append(",\"password\":").append(Json.encodeToString(password))
        append("}")
      }
      val (code, body) = executeHttpRequest(endpoint, "POST", payload)
      if (code in 200..299) {
        val root = jsonParser.parseToJsonElement(body).jsonObject
        val accessToken = root["access_token"]?.jsonPrimitive?.content
        val refreshToken = root["refresh_token"]?.jsonPrimitive?.content ?: ""
        val expiresInSeconds = root["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
        val userObj = root["user"]?.jsonObject
        val userId = userObj?.get("id")?.jsonPrimitive?.content ?: "user-${System.currentTimeMillis()}"
        val userEmail = userObj?.get("email")?.jsonPrimitive?.content ?: email

        if (accessToken != null) {
          val newSession = AuthSession(
            userId = userId,
            email = userEmail,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresAtMillis = System.currentTimeMillis() + (expiresInSeconds * 1000L)
          )
          mutex.withLock {
            writeEncryptedSession(newSession)
            _session.value = newSession
          }
          AuthResult.Success(newSession)
        } else {
          // Email confirmation required
          AuthResult.Failure("Account registered. Please confirm your email before signing in.", code)
        }
      } else {
        AuthResult.Failure("Sign up failed ($code): $body", code)
      }
    } catch (e: Exception) {
      AuthResult.Failure("Network error during sign up: ${e.message}", isNetworkError = true)
    }
  }

  suspend fun refreshSession(): AuthResult<AuthSession> = withContext(Dispatchers.IO) {
    val current = _session.value ?: return@withContext AuthResult.Failure("No active session to refresh")
    if (current.refreshToken.isBlank()) return@withContext AuthResult.Failure("No refresh token available")

    try {
      val endpoint = "$baseUrl/auth/v1/token?grant_type=refresh_token"
      val payload = "{\"refresh_token\":${Json.encodeToString(current.refreshToken)}}"
      val (code, body) = executeHttpRequest(endpoint, "POST", payload)

      if (code in 200..299) {
        val root = jsonParser.parseToJsonElement(body).jsonObject
        val accessToken = root["access_token"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing access_token")
        val refreshToken = root["refresh_token"]?.jsonPrimitive?.content ?: current.refreshToken
        val expiresInSeconds = root["expires_in"]?.jsonPrimitive?.content?.toLongOrNull() ?: 3600L
        val userObj = root["user"]?.jsonObject
        val userId = userObj?.get("id")?.jsonPrimitive?.content ?: current.userId
        val userEmail = userObj?.get("email")?.jsonPrimitive?.content ?: current.email

        val updatedSession = AuthSession(
          userId = userId,
          email = userEmail,
          accessToken = accessToken,
          refreshToken = refreshToken,
          expiresAtMillis = System.currentTimeMillis() + (expiresInSeconds * 1000L)
        )

        mutex.withLock {
          writeEncryptedSession(updatedSession)
          _session.value = updatedSession
        }
        AuthResult.Success(updatedSession)
      } else {
        // Refresh token rejected by server (expired or revoked)
        if (code in 400..403) {
          signOut()
        }
        AuthResult.Failure("Token refresh failed ($code): $body", code)
      }
    } catch (e: Exception) {
      AuthResult.Failure("Network error refreshing token: ${e.message}", isNetworkError = true)
    }
  }

  suspend fun getValidAccessToken(): String? = withContext(Dispatchers.IO) {
    mutex.withLock {
      val current = _session.value ?: return@withContext null
      if (!current.isExpired) {
        return@withContext current.accessToken
      }
    }
    // Expired: attempt refresh
    when (val result = refreshSession()) {
      is AuthResult.Success -> result.data.accessToken
      is AuthResult.Failure -> null
    }
  }

  suspend fun signOut() = withContext(Dispatchers.IO) {
    val current = _session.value
    if (current != null) {
      try {
        val endpoint = "$baseUrl/auth/v1/logout"
        executeHttpRequest(endpoint, "POST", authToken = current.accessToken)
      } catch (_: Exception) {}
    }
    mutex.withLock {
      if (sessionFile.exists()) sessionFile.delete()
      _session.value = null
    }
  }

  fun updateLocalSessionDirectly(session: AuthSession?) {
    _session.value = session
    if (session != null) {
      writeEncryptedSession(session)
    } else if (sessionFile.exists()) {
      sessionFile.delete()
    }
  }

  private fun readEncryptedSession(): AuthSession? {
    if (!sessionFile.exists()) return null
    return try {
      val bytes = sessionFile.readBytes()
      val decrypted = keyManager.decryptString(bytes)
      jsonParser.decodeFromString<AuthSession>(decrypted)
    } catch (e: Exception) {
      Log.e("SupabaseAuthManager", "Failed to decrypt auth session: ${e.message}")
      null
    }
  }

  private fun writeEncryptedSession(session: AuthSession) {
    try {
      val serialized = Json.encodeToString(session)
      val encrypted = keyManager.encryptString(serialized)
      val temp = File(filesDir, "civic_auth_session.enc.tmp")
      temp.writeBytes(encrypted)
      if (sessionFile.exists()) sessionFile.delete()
      temp.renameTo(sessionFile)
    } catch (e: Exception) {
      Log.e("SupabaseAuthManager", "Failed to encrypt auth session: ${e.message}")
    }
  }

  private fun executeHttpRequest(
    urlString: String,
    method: String,
    payload: String? = null,
    authToken: String? = null
  ): Pair<Int, String> {
    val url = URL(urlString)
    val conn = url.openConnection() as HttpURLConnection
    conn.requestMethod = method
    conn.connectTimeout = 8000
    conn.readTimeout = 8000
    conn.setRequestProperty("apikey", anonKey)
    conn.setRequestProperty("Authorization", "Bearer ${authToken ?: anonKey}")
    conn.setRequestProperty("Content-Type", "application/json")
    conn.setRequestProperty("Accept", "application/json")

    if (payload != null && (method == "POST" || method == "PUT" || method == "PATCH")) {
      conn.doOutput = true
      OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { writer ->
        writer.write(payload)
        writer.flush()
      }
    }

    val code = conn.responseCode
    val stream = if (code in 200..299) conn.inputStream else conn.errorStream
    val body = if (stream != null) {
      BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    } else {
      ""
    }
    return Pair(code, body)
  }
}
