package com.hasanege.materialtv.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@HiltWorker
class CatalogSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val syncManager: CatalogSyncManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val profileId = inputData.getString("PROFILE_ID") ?: return@withContext Result.failure()
        val username = inputData.getString("USERNAME") ?: return@withContext Result.failure()
        val password = inputData.getString("PASSWORD") ?: return@withContext Result.failure()

        try {
            syncManager.syncIfNeeded(profileId, username, password)
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            if (runAttemptCount < 3) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }
}
