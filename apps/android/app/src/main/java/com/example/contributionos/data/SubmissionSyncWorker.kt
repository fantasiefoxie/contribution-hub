package com.example.contributionos.data

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.contributionos.auth.SupabaseAuthManager
import com.example.contributionos.model.SubmissionStatus
import java.io.File
import java.util.concurrent.TimeUnit

class SubmissionSyncWorker(
  appContext: Context,
  workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

  override suspend fun doWork(): Result {
    val context = applicationContext
    val draftStore = LocalDraftStore(context)
    val authManager = SupabaseAuthManager(context).apply { initialize() }
    val storageUploader = SupabaseStorageUploader(context)
    val remoteDataSource = RemoteCivicDataSource()

    val pendingSubmissions = draftStore.getSubmissions().filter { sub ->
      !sub.isSandboxDemo && !sub.isLocalOnly && (
        sub.status == SubmissionStatus.QUEUED_OFFLINE ||
        sub.status == SubmissionStatus.RETRYABLE_FAILURE ||
        sub.status == SubmissionStatus.UPLOADING
      ) && sub.taskId != SeedMissionCatalog.selfGuidedStarterQuest.id
    }

    if (pendingSubmissions.isEmpty()) {
      return Result.success()
    }

    val userToken = authManager.getValidAccessToken()
    val session = authManager.session.value
    if (userToken == null || session == null) {
      Log.w("SubmissionSyncWorker", "No authenticated session. Offline queue deferred.")
      pendingSubmissions.forEach { sub ->
        draftStore.saveSubmission(
          sub.copy(
            status = SubmissionStatus.RETRYABLE_FAILURE,
            syncErrorMessage = "Authentication required: Please sign in to sync pending evidence."
          )
        )
      }
      return Result.retry()
    }

    var hasRetryableFailure = false

    for (sub in pendingSubmissions) {
      try {
        var currentSub = sub.copy(
          status = SubmissionStatus.UPLOADING,
          syncErrorMessage = null
        )
        draftStore.saveSubmission(currentSub)

        var storagePath = currentSub.remoteStoragePath

        // Step 1: Upload photo evidence if local photo exists and not yet uploaded
        if (storagePath == null && !currentSub.localPhotoUri.isNullOrBlank()) {
          val uploadResult = storageUploader.uploadEvidence(
            localUriString = currentSub.localPhotoUri,
            userId = session.userId,
            taskId = currentSub.taskId,
            userAccessToken = userToken
          )

          when (uploadResult) {
            is StorageUploadResult.Success -> {
              storagePath = uploadResult.fullStorageKey
              currentSub = currentSub.copy(remoteStoragePath = storagePath)
              draftStore.saveSubmission(currentSub)
            }
            is StorageUploadResult.Failure -> {
              if (uploadResult.isNetworkError) {
                hasRetryableFailure = true
                draftStore.saveSubmission(
                  currentSub.copy(
                    status = SubmissionStatus.RETRYABLE_FAILURE,
                    syncErrorMessage = "Evidence upload network error: ${uploadResult.message}"
                  )
                )
                continue
              } else if (uploadResult.isAuthError) {
                draftStore.saveSubmission(
                  currentSub.copy(
                    status = SubmissionStatus.RETRYABLE_FAILURE,
                    syncErrorMessage = "Storage access unauthorized: ${uploadResult.message}"
                  )
                )
                return Result.retry()
              } else {
                // Permanent failure (e.g. oversize, corrupt format)
                draftStore.saveSubmission(
                  currentSub.copy(
                    status = SubmissionStatus.PERMANENT_FAILURE,
                    syncErrorMessage = uploadResult.message
                  )
                )
                continue
              }
            }
          }
        }

        // Step 2: Submit task to backend with client_submission_id
        val attachments = listOfNotNull(storagePath)
        val submitResult = remoteDataSource.submitEvidence(
          taskId = currentSub.taskId,
          content = currentSub.content,
          attachments = attachments,
          userAuthToken = userToken,
          clientSubmissionId = currentSub.clientSubmissionId ?: currentSub.id
        )

        when (submitResult) {
          is RemoteResult.Success -> {
            val remote = submitResult.data
            val acceptedSub = currentSub.copy(
              id = remote.id,
              status = SubmissionStatus.SERVER_ACCEPTED,
              feedback = remote.feedback ?: "Evidence accepted by Civic Registry.",
              remoteSubmissionId = remote.id,
              syncErrorMessage = null
            )
            draftStore.saveSubmission(acceptedSub)

            // Safe cleanup of temporary camera file after confirmed server acceptance
            if (currentSub.localPhotoUri?.contains(context.cacheDir.path) == true) {
              try {
                val cacheFile = File(currentSub.localPhotoUri.removePrefix("file://"))
                if (cacheFile.exists()) cacheFile.delete()
              } catch (_: Exception) {}
            }
          }
          is RemoteResult.Failure -> {
            if (submitResult.isOffline || (submitResult.statusCode ?: 0) >= 500) {
              hasRetryableFailure = true
              draftStore.saveSubmission(
                currentSub.copy(
                  status = SubmissionStatus.RETRYABLE_FAILURE,
                  syncErrorMessage = submitResult.message
                )
              )
            } else {
              draftStore.saveSubmission(
                currentSub.copy(
                  status = SubmissionStatus.PERMANENT_FAILURE,
                  syncErrorMessage = submitResult.message
                )
              )
            }
          }
        }
      } catch (e: Exception) {
        Log.e("SubmissionSyncWorker", "Error processing submission ${sub.id}", e)
        hasRetryableFailure = true
        draftStore.saveSubmission(
          sub.copy(
            status = SubmissionStatus.RETRYABLE_FAILURE,
            syncErrorMessage = "Sync unexpected error: ${e.message}"
          )
        )
      }
    }

    return if (hasRetryableFailure) Result.retry() else Result.success()
  }

  companion object {
    const val UNIQUE_WORK_NAME = "civic_submission_sync"

    fun enqueue(context: Context) {
      val constraints = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

      val workRequest = OneTimeWorkRequestBuilder<SubmissionSyncWorker>()
        .setConstraints(constraints)
        .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
        .build()

      // KEEP: do not cancel a sync already in progress — an in-flight upload must not be interrupted.
      WorkManager.getInstance(context).enqueueUniqueWork(
        UNIQUE_WORK_NAME,
        ExistingWorkPolicy.KEEP,
        workRequest
      )
    }
  }
}
