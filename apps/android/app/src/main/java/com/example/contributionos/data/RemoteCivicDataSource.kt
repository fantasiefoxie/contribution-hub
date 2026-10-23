package com.example.contributionos.data

import com.example.contributionos.model.CivicBadge
import com.example.contributionos.model.CivicExpedition
import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.CivicTask
import com.example.contributionos.model.CommunityMilestone
import com.example.contributionos.model.ExpeditionNode
import com.example.contributionos.model.ExpeditionStatus
import com.example.contributionos.model.NodeStatus
import com.example.contributionos.model.SubmissionStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

sealed class RemoteResult<out T> {
  data class Success<T>(val data: T) : RemoteResult<T>()
  data class Failure(val message: String, val statusCode: Int? = null, val isOffline: Boolean = false) : RemoteResult<Nothing>()
}

class RemoteCivicDataSource(
  val baseUrl: String = "https://dsfbviqvpajvqcyrobsb.supabase.co",
  val anonKey: String = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImRzZmJ2aXF2cGFqdnFjeXJvYnNiIiwicm9sZSI6ImFub24iLCJpYXQiOjE3NzI5NjU1MzksImV4cCI6MjA4ODU0MTUzOX0.me8EIkL4RCPtWIuL5oQwZH1-9XBM6oURLQ9HkGJAIQw"
) {

  private val jsonParser = Json { ignoreUnknownKeys = true }

  suspend fun fetchTasks(): RemoteResult<List<CivicTask>> = withContext(Dispatchers.IO) {
    try {
      val endpoint = "$baseUrl/functions/v1/task-engine"
      val (responseCode, responseBody) = executeHttpRequest(endpoint, "GET")
      if (responseCode in 200..299) {
        val root = jsonParser.parseToJsonElement(responseBody).jsonObject
        val tasksArray = root["tasks"]?.jsonArray ?: JsonArray(emptyList())
        val tasks = tasksArray.mapNotNull { parseCivicTask(it.jsonObject) }
        RemoteResult.Success(tasks)
      } else {
        // Fallback to direct REST table query if Edge Function is in cold start or restricted
        val restEndpoint = "$baseUrl/rest/v1/tasks?select=*&visibility=eq.public&order=created_at.desc"
        val (restCode, restBody) = executeHttpRequest(restEndpoint, "GET")
        if (restCode in 200..299) {
          val array = jsonParser.parseToJsonElement(restBody).jsonArray
          val tasks = array.mapNotNull { parseCivicTask(it.jsonObject) }
          RemoteResult.Success(tasks)
        } else {
          RemoteResult.Failure("Failed to retrieve public missions ($responseCode): $responseBody", responseCode)
        }
      }
    } catch (e: Exception) {
      RemoteResult.Failure("Network error communicating with Civic Registry: ${e.localizedMessage ?: e.message}", isOffline = true)
    }
  }

  suspend fun fetchInitiatives(): RemoteResult<List<CivicExpedition>> = withContext(Dispatchers.IO) {
    try {
      val endpoint = "$baseUrl/functions/v1/initiative-engine"
      val (responseCode, responseBody) = executeHttpRequest(endpoint, "GET")
      if (responseCode in 200..299) {
        val root = jsonParser.parseToJsonElement(responseBody).jsonObject
        val initArray = root["initiatives"]?.jsonArray ?: JsonArray(emptyList())
        val expeditions = initArray.mapIndexed { idx, item ->
          parseCivicExpedition(item.jsonObject, idx)
        }
        RemoteResult.Success(expeditions)
      } else {
        val restEndpoint = "$baseUrl/rest/v1/initiatives?select=*&status=eq.active&order=created_at.desc"
        val (restCode, restBody) = executeHttpRequest(restEndpoint, "GET")
        if (restCode in 200..299) {
          val array = jsonParser.parseToJsonElement(restBody).jsonArray
          val expeditions = array.mapIndexed { idx, item ->
            parseCivicExpedition(item.jsonObject, idx)
          }
          RemoteResult.Success(expeditions)
        } else {
          RemoteResult.Failure("Failed to retrieve expeditions ($responseCode): $responseBody", responseCode)
        }
      }
    } catch (e: Exception) {
      RemoteResult.Failure("Offline: could not fetch remote expeditions", isOffline = true)
    }
  }

  suspend fun fetchCommunityMilestones(): RemoteResult<List<CommunityMilestone>> = withContext(Dispatchers.IO) {
    try {
      val restEndpoint = "$baseUrl/rest/v1/initiatives?select=id,title,description,target_metrics&status=eq.active"
      val (responseCode, responseBody) = executeHttpRequest(restEndpoint, "GET")
      if (responseCode in 200..299) {
        val array = jsonParser.parseToJsonElement(responseBody).jsonArray
        val milestones = array.mapIndexed { idx, elem ->
          val obj = elem.jsonObject
          val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "ms-$idx"
          val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "Community Initiative Target"
          val desc = obj["description"]?.jsonPrimitive?.contentOrNull ?: "Verified public civic milestone"
          CommunityMilestone(
            id = id,
            title = title,
            description = desc,
            currentValue = 0,
            targetValue = 100,
            unit = "verified actions",
            category = "Civic Progress",
            isAchieved = false,
            isSandboxDemo = false
          )
        }
        RemoteResult.Success(milestones)
      } else {
        RemoteResult.Failure("Failed to load community milestones: $responseCode", responseCode)
      }
    } catch (e: Exception) {
      RemoteResult.Failure("Offline: could not load milestones", isOffline = true)
    }
  }

  suspend fun submitEvidence(
    taskId: String,
    content: String,
    attachments: List<String>,
    userAuthToken: String?,
    clientSubmissionId: String? = null
  ): RemoteResult<CivicSubmission> = withContext(Dispatchers.IO) {
    if (userAuthToken.isNullOrBlank()) {
      return@withContext RemoteResult.Failure(
        message = "Authentication required: Please sign in to submit verified contributions.",
        statusCode = 401
      )
    }

    try {
      val endpoint = "$baseUrl/functions/v1/task-engine/$taskId/submit"
      val requestBody = buildString {
        append("{")
        append("\"content\":").append(kotlinx.serialization.json.JsonPrimitive(content).toString()).append(",")
        append("\"attachments\":[")
        append(attachments.joinToString(",") { kotlinx.serialization.json.JsonPrimitive(it).toString() })
        append("]")
        if (clientSubmissionId != null) {
          append(",\"client_submission_id\":").append(kotlinx.serialization.json.JsonPrimitive(clientSubmissionId).toString())
        }
        append("}")
      }

      val (code, body) = executeHttpRequest(endpoint, "POST", requestBody, userAuthToken)
      if (code in 200..299) {
        val root = jsonParser.parseToJsonElement(body).jsonObject
        val submissionObj = root["submission"]?.jsonObject
        val id = submissionObj?.get("id")?.jsonPrimitive?.contentOrNull ?: "sub-rem-${System.currentTimeMillis()}"
        val submission = CivicSubmission(
          id = id,
          taskId = taskId,
          taskTitle = "Remote Verified Task",
          submittedAt = System.currentTimeMillis(),
          content = content,
          status = SubmissionStatus.SERVER_ACCEPTED,
          feedback = "Acknowledged by Civic Registry. Awaiting accredited validator review.",
          scoreAwarded = null,
          isOfflineOrigin = false,
          isSandboxDemo = false,
          remoteSubmissionId = id,
          clientSubmissionId = clientSubmissionId,
          remoteStoragePath = attachments.firstOrNull()
        )
        RemoteResult.Success(submission)
      } else {
        RemoteResult.Failure("Submission rejected by server ($code): $body", code)
      }
    } catch (e: Exception) {
      RemoteResult.Failure("Network transmission failure: ${e.localizedMessage ?: e.message}", isOffline = true)
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

    val responseCode = conn.responseCode
    val stream = if (responseCode in 200..299) conn.inputStream else conn.errorStream
    val body = if (stream != null) {
      BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
    } else {
      ""
    }
    return Pair(responseCode, body)
  }

  private fun parseCivicTask(obj: JsonObject): CivicTask? {
    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: return null
    val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "Untitled Task"
    val description = obj["description"]?.jsonPrimitive?.contentOrNull ?: ""
    val difficulty = obj["difficulty"]?.jsonPrimitive?.intOrNull ?: 1
    val status = obj["status"]?.jsonPrimitive?.contentOrNull ?: "open"
    val visibility = obj["visibility"]?.jsonPrimitive?.contentOrNull ?: "public"
    val tags = obj["tags"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList()
    val orgId = obj["org_id"]?.jsonPrimitive?.contentOrNull
    val initiativeId = obj["initiative_id"]?.jsonPrimitive?.contentOrNull

    return CivicTask(
      id = id,
      title = title,
      description = description,
      difficulty = difficulty,
      status = status,
      visibility = visibility,
      tags = tags,
      orgId = orgId,
      initiativeId = initiativeId,
      district = "District Registry",
      locationLabel = "Verified Coordinate",
      evidenceRequirements = "Submit authentic photographic evidence and verification notes.",
      isSandboxDemo = false
    )
  }

  private fun parseCivicExpedition(obj: JsonObject, index: Int): CivicExpedition {
    val id = obj["id"]?.jsonPrimitive?.contentOrNull ?: "exp-$index"
    val title = obj["title"]?.jsonPrimitive?.contentOrNull ?: "Civic Initiative Expedition"
    val lore = obj["description"]?.jsonPrimitive?.contentOrNull ?: "Community-verified collective objective."

    return CivicExpedition(
      id = id,
      title = title,
      theme = "Verified Civic Initiative",
      lore = lore,
      district = "Civic Sector $index",
      targetMilestone = title,
      nodes = emptyList(),
      activeRunSeed = index.toLong() + 101L,
      status = ExpeditionStatus.ACTIVE,
      isSandboxDemo = false
    )
  }
}
