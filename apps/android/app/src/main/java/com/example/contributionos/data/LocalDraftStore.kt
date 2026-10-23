package com.example.contributionos.data

import android.content.Context
import android.util.Log
import com.example.contributionos.model.CivicProfile
import com.example.contributionos.model.CivicSubmission
import com.example.contributionos.model.MissionDraft
import com.example.contributionos.security.SecurityKeyManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class LocalDraftStore(
  private val filesDir: File,
  private val keyManager: SecurityKeyManager
) {
  constructor(context: Context, keyManager: SecurityKeyManager = SecurityKeyManager(context)) :
    this(context.filesDir, keyManager)

  private val json = Json { ignoreUnknownKeys = true; prettyPrint = false }
  private val mutex = Mutex()

  private val encryptedDraftsFile: File
    get() = File(filesDir, "civic_secure_drafts.enc")

  private val encryptedSubmissionsFile: File
    get() = File(filesDir, "civic_secure_submissions.enc")

  // Lightweight encrypted profile persistence for exploration state
  private val encryptedProfileFile: File
    get() = File(filesDir, "civic_secure_profile.enc")

  // Backup file to guarantee safe recovery if crash or replacement failure occurs
  private val backupProfileFile: File
    get() = File(filesDir, "civic_secure_profile.enc.bak")

  // Encrypted persistent store for rewarded waypoint and quest node IDs
  private val encryptedRewardedNodesFile: File
    get() = File(filesDir, "civic_secure_rewarded_nodes.enc")

  private val legacyDraftsFile: File
    get() = File(filesDir, "civic_mission_drafts.json")

  private val legacySubmissionsFile: File
    get() = File(filesDir, "civic_local_submissions.json")

  suspend fun getDrafts(): List<MissionDraft> = withContext(Dispatchers.IO) {
    mutex.withLock {
      migrateLegacyDraftsIfNeeded()
      readEncryptedDrafts()
    }
  }

  suspend fun saveDraft(draft: MissionDraft) = withContext(Dispatchers.IO) {
    mutex.withLock {
      migrateLegacyDraftsIfNeeded()
      val existing = readEncryptedDrafts().toMutableList()
      // One live draft per task: if a draft for this task already exists, update it in place.
      // This is deliberate — users cannot maintain two simultaneous drafts for the same task.
      val index = existing.indexOfFirst { it.id == draft.id || it.taskId == draft.taskId }
      if (index >= 0) {
        existing[index] = draft
      } else {
        existing.add(0, draft)
      }
      writeEncryptedDrafts(existing)
    }
  }

  suspend fun deleteDraft(draftId: String) = withContext(Dispatchers.IO) {
    mutex.withLock {
      migrateLegacyDraftsIfNeeded()
      val existing = readEncryptedDrafts().filterNot { it.id == draftId }
      writeEncryptedDrafts(existing)
    }
  }

  suspend fun getSubmissions(): List<CivicSubmission> = withContext(Dispatchers.IO) {
    mutex.withLock {
      migrateLegacySubmissionsIfNeeded()
      readEncryptedSubmissions()
    }
  }

  suspend fun saveSubmission(submission: CivicSubmission) = withContext(Dispatchers.IO) {
    mutex.withLock {
      migrateLegacySubmissionsIfNeeded()
      val existing = readEncryptedSubmissions().toMutableList()
      val index = existing.indexOfFirst { it.id == submission.id || (submission.clientSubmissionId != null && it.clientSubmissionId == submission.clientSubmissionId) }
      if (index >= 0) {
        existing[index] = submission
      } else {
        existing.add(0, submission)
      }
      writeEncryptedSubmissions(existing)
    }
  }

  suspend fun deleteSubmission(submissionId: String) = withContext(Dispatchers.IO) {
    mutex.withLock {
      migrateLegacySubmissionsIfNeeded()
      val existing = readEncryptedSubmissions().filterNot { it.id == submissionId }
      writeEncryptedSubmissions(existing)
    }
  }

  // --- Profile persistence ---
  // Saves only the exploration state fields that survive process death.
  // Verified civic credits are always recomputed authoritatively from persisted submissions.

  suspend fun saveProfile(profile: CivicProfile) = withContext(Dispatchers.IO) {
    mutex.withLock {
      val tempFile = File(filesDir, "civic_secure_profile.enc.tmp")
      try {
        val jsonString = json.encodeToString(profile)
        val encryptedBytes = keyManager.encryptString(jsonString)
        tempFile.writeBytes(encryptedBytes)

        // 1. Preserve the previous valid encrypted profile until the new one is safely committed.
        // A backup copy is maintained so a crash or unexpected failure during replacement never destroys the profile.
        if (encryptedProfileFile.exists()) {
          try {
            encryptedProfileFile.copyTo(backupProfileFile, overwrite = true)
          } catch (e: Exception) {
            Log.w("LocalDraftStore", "Could not create profile backup: ${e.message}")
          }
        }

        // 2. Commit new profile: try atomic move where supported, fallback to standard replace
        try {
          Files.move(
            tempFile.toPath(),
            encryptedProfileFile.toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING
          )
        } catch (_: AtomicMoveNotSupportedException) {
          Files.move(
            tempFile.toPath(),
            encryptedProfileFile.toPath(),
            StandardCopyOption.REPLACE_EXISTING
          )
        } catch (e: Exception) {
          // If Files.move fails, attempt fallback rename
          val renamed = tempFile.renameTo(encryptedProfileFile)
          if (!renamed) {
            if (encryptedProfileFile.exists()) encryptedProfileFile.delete()
            if (!tempFile.renameTo(encryptedProfileFile)) {
              throw e
            }
          }
        }

        // 3. Once safely committed to primary destination, delete backup file
        if (backupProfileFile.exists()) {
          backupProfileFile.delete()
        }
      } catch (e: Exception) {
        if (tempFile.exists()) tempFile.delete()
        Log.e("LocalDraftStore", "Failed to persist profile: ${e.message}")
        throw e
      }
    }
  }

  suspend fun getProfile(): CivicProfile? = withContext(Dispatchers.IO) {
    mutex.withLock {
      // 1. Primary path: attempt to read and decrypt encryptedProfileFile
      if (encryptedProfileFile.exists()) {
        try {
          val encryptedBytes = encryptedProfileFile.readBytes()
          val decryptedJson = keyManager.decryptString(encryptedBytes)
          val profile = json.decodeFromString<CivicProfile>(decryptedJson)
          if (profile.rewardedWaypointIds.isEmpty() && encryptedRewardedNodesFile.exists()) {
            val standalone = readEncryptedRewardedNodes()
            if (standalone.isNotEmpty()) {
              return@withLock profile.copy(rewardedWaypointIds = standalone)
            }
          }
          return@withLock profile
        } catch (e: Exception) {
          Log.w("LocalDraftStore", "Primary profile corrupted or unreadable: ${e.message}. Attempting backup recovery.")
        }
      }

      // 2. Safe Recovery path: if primary file is missing or corrupted, recover from backup
      if (backupProfileFile.exists()) {
        try {
          val backupBytes = backupProfileFile.readBytes()
          val decryptedJson = keyManager.decryptString(backupBytes)
          val profile = json.decodeFromString<CivicProfile>(decryptedJson)
          Log.i("LocalDraftStore", "Successfully recovered profile from backup file.")
          // Restore recovered backup as primary file
          try {
            backupProfileFile.copyTo(encryptedProfileFile, overwrite = true)
          } catch (_: Exception) {}
          return@withLock profile
        } catch (e: Exception) {
          Log.e("LocalDraftStore", "Failed to recover profile from backup: ${e.message}")
        }
      }

      null
    }
  }

  // --- Rewarded node IDs persistence ---

  suspend fun getRewardedNodeIds(): Set<String> = withContext(Dispatchers.IO) {
    mutex.withLock {
      val prof = try {
        if (encryptedProfileFile.exists()) {
          val encryptedBytes = encryptedProfileFile.readBytes()
          val decryptedJson = keyManager.decryptString(encryptedBytes)
          json.decodeFromString<CivicProfile>(decryptedJson)
        } else if (backupProfileFile.exists()) {
          val backupBytes = backupProfileFile.readBytes()
          val decryptedJson = keyManager.decryptString(backupBytes)
          json.decodeFromString<CivicProfile>(decryptedJson)
        } else null
      } catch (_: Exception) {
        null
      }

      val profileSet = prof?.rewardedWaypointIds ?: emptySet()
      if (profileSet.isNotEmpty()) {
        return@withLock profileSet
      }
      readEncryptedRewardedNodes()
    }
  }

  private fun readEncryptedRewardedNodes(): Set<String> {
    if (!encryptedRewardedNodesFile.exists()) return emptySet()
    return try {
      val encryptedBytes = encryptedRewardedNodesFile.readBytes()
      val decryptedJson = keyManager.decryptString(encryptedBytes)
      json.decodeFromString<Set<String>>(decryptedJson)
    } catch (e: Exception) {
      Log.e("LocalDraftStore", "Failed to load rewarded node IDs: ${e.message}")
      emptySet()
    }
  }

  suspend fun saveRewardedNodeIds(nodeIds: Set<String>) = withContext(Dispatchers.IO) {
    mutex.withLock {
      try {
        val jsonString = json.encodeToString(nodeIds)
        val encryptedBytes = keyManager.encryptString(jsonString)
        val tempFile = File(filesDir, "civic_secure_rewarded_nodes.enc.tmp")
        tempFile.writeBytes(encryptedBytes)
        if (encryptedRewardedNodesFile.exists()) encryptedRewardedNodesFile.delete()
        if (!tempFile.renameTo(encryptedRewardedNodesFile)) {
          throw java.io.IOException("Failed to rename temporary rewarded nodes file to destination")
        }
      } catch (e: Exception) {
        Log.e("LocalDraftStore", "Failed to persist rewarded node IDs: ${e.message}")
        throw e
      }
    }
  }

  // --- Legacy migration helpers ---

  private fun migrateLegacyDraftsIfNeeded() {
    try {
      if (legacyDraftsFile.exists() && !encryptedDraftsFile.exists()) {
        val legacyText = legacyDraftsFile.readText()
        if (legacyText.isNotBlank()) {
          val drafts = json.decodeFromString<List<MissionDraft>>(legacyText)
          writeEncryptedDrafts(drafts)
        }
        legacyDraftsFile.delete()
      } else if (legacyDraftsFile.exists() && encryptedDraftsFile.exists()) {
        legacyDraftsFile.delete()
      }
    } catch (e: Exception) {
      Log.w("LocalDraftStore", "Legacy draft migration error: ${e.message}")
    }
  }

  private fun migrateLegacySubmissionsIfNeeded() {
    try {
      if (legacySubmissionsFile.exists() && !encryptedSubmissionsFile.exists()) {
        val legacyText = legacySubmissionsFile.readText()
        if (legacyText.isNotBlank()) {
          val submissions = json.decodeFromString<List<CivicSubmission>>(legacyText)
          writeEncryptedSubmissions(submissions)
        }
        legacySubmissionsFile.delete()
      } else if (legacySubmissionsFile.exists() && encryptedSubmissionsFile.exists()) {
        legacySubmissionsFile.delete()
      }
    } catch (e: Exception) {
      Log.w("LocalDraftStore", "Legacy submission migration error: ${e.message}")
    }
  }

  private fun readEncryptedDrafts(): List<MissionDraft> {
    if (!encryptedDraftsFile.exists()) return emptyList()
    return try {
      val encryptedBytes = encryptedDraftsFile.readBytes()
      val decryptedJson = keyManager.decryptString(encryptedBytes)
      json.decodeFromString<List<MissionDraft>>(decryptedJson)
    } catch (e: Exception) {
      Log.e("LocalDraftStore", "Failed to decrypt local drafts: ${e.message}")
      emptyList()
    }
  }

  private fun writeEncryptedDrafts(drafts: List<MissionDraft>) {
    try {
      val jsonString = json.encodeToString(drafts)
      val encryptedBytes = keyManager.encryptString(jsonString)
      val tempFile = File(filesDir, "civic_secure_drafts.enc.tmp")
      tempFile.writeBytes(encryptedBytes)
      if (encryptedDraftsFile.exists()) encryptedDraftsFile.delete()
      tempFile.renameTo(encryptedDraftsFile)
    } catch (e: Exception) {
      Log.e("LocalDraftStore", "Failed to encrypt and save local drafts: ${e.message}")
    }
  }

  private fun readEncryptedSubmissions(): List<CivicSubmission> {
    if (!encryptedSubmissionsFile.exists()) return emptyList()
    return try {
      val encryptedBytes = encryptedSubmissionsFile.readBytes()
      val decryptedJson = keyManager.decryptString(encryptedBytes)
      json.decodeFromString<List<CivicSubmission>>(decryptedJson)
    } catch (e: Exception) {
      Log.e("LocalDraftStore", "Failed to decrypt local submissions: ${e.message}")
      emptyList()
    }
  }

  private fun writeEncryptedSubmissions(submissions: List<CivicSubmission>) {
    try {
      val jsonString = json.encodeToString(submissions)
      val encryptedBytes = keyManager.encryptString(jsonString)
      val tempFile = File(filesDir, "civic_secure_submissions.enc.tmp")
      tempFile.writeBytes(encryptedBytes)
      if (encryptedSubmissionsFile.exists()) encryptedSubmissionsFile.delete()
      tempFile.renameTo(encryptedSubmissionsFile)
    } catch (e: Exception) {
      Log.e("LocalDraftStore", "Failed to encrypt and save local submissions: ${e.message}")
    }
  }
}
