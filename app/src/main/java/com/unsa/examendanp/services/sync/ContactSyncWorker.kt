package com.unsa.examendanp.services.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.unsa.examendanp.data.local.preferences.UserPreferences
import com.unsa.examendanp.data.repository.ContactRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

@HiltWorker
class ContactSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val repository: ContactRepository,
    private val userPreferences: UserPreferences
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Get user ID
            val userId = userPreferences.userId.first()

            if (userId.isNullOrEmpty()) {
                return Result.failure()
            }

            // Sync contacts
            val syncResult = repository.syncContacts(userId)

            if (syncResult.isSuccess) {
                // Clean up old data
                repository.cleanupOldData()

                // Check infection status
                val infectionResult = repository.checkInfectionStatus(userId)
                if (infectionResult.isSuccess) {
                    userPreferences.setInfectionStatus(infectionResult.getOrDefault(false))
                }

                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.retry()
        }
    }
}