package com.example.contributionos.data

import android.content.Context
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.MissionDraft
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class LocalDraftStore(private val context: Context) {
  private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }
  private val mutex = Mutex()

  private val draftsFile: File
    get() = File(context.filesDir, "civic_mission_drafts.json")

  private val submissionsFile: File
    get() = File(context.filesDir, "civic_local_submissions.json")

  suspend fun getDrafts(): List<MissionDraft> = withContext(Dispatchers.IO) {
    mutex.withLock {
      if (!draftsFile.exists()) return@withContext emptyList()
      try {
        json.decodeFromString<List<MissionDraft>>(draftsFile.readText())
      } catch (e: Exception) {
        emptyList()
      }
    }
  }

  suspend fun saveDraft(draft: MissionDraft) = withContext(Dispatchers.IO) {
    mutex.withLock {
      val existing = getDraftsInternal().toMutableList()
      val index = existing.indexOfFirst { it.id == draft.id || it.taskId == draft.taskId }
      if (index >= 0) {
        existing[index] = draft
      } else {
        existing.add(0, draft)
      }
      draftsFile.writeText(json.encodeToString(existing))
    }
  }

  suspend fun deleteDraft(draftId: String) = withContext(Dispatchers.IO) {
    mutex.withLock {
      val existing = getDraftsInternal().filterNot { it.id == draftId }
      draftsFile.writeText(json.encodeToString(existing))
    }
  }

  suspend fun getSubmissions(): List<CivicSubmission> = withContext(Dispatchers.IO) {
    mutex.withLock {
      if (!submissionsFile.exists()) return@withContext emptyList()
      try {
        json.decodeFromString<List<CivicSubmission>>(submissionsFile.readText())
      } catch (e: Exception) {
        emptyList()
      }
    }
  }

  suspend fun saveSubmission(submission: CivicSubmission) = withContext(Dispatchers.IO) {
    mutex.withLock {
      val existing = getSubmissionsInternal().toMutableList()
      val index = existing.indexOfFirst { it.id == submission.id }
      if (index >= 0) {
        existing[index] = submission
      } else {
        existing.add(0, submission)
      }
      submissionsFile.writeText(json.encodeToString(existing))
    }
  }

  private fun getDraftsInternal(): List<MissionDraft> {
    if (!draftsFile.exists()) return emptyList()
    return try {
      json.decodeFromString<List<MissionDraft>>(draftsFile.readText())
    } catch (e: Exception) {
      emptyList()
    }
  }

  private fun getSubmissionsInternal(): List<CivicSubmission> {
    if (!submissionsFile.exists()) return emptyList()
    return try {
      json.decodeFromString<List<CivicSubmission>>(submissionsFile.readText())
    } catch (e: Exception) {
      emptyList()
    }
  }
}
