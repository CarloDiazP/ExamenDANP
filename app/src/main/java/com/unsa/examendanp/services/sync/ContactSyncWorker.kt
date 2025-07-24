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

                // Check infection status - manejar errores aquí
                try {
                    val infectionResult = repository.checkInfectionStatus(userId)
                    if (infectionResult.isSuccess) {
                        val isInfected = infectionResult.getOrDefault(false)
                        // AQUÍ SE ACTUALIZA EL ESTADO DE INFECCIÓN
                        userPreferences.setInfectionStatus(isInfected)

                        if (isInfected) {
                            println("Usuario marcado como POSITIVO durante sincronización")
                        }
                    }
                } catch (e: Exception) {
                    // Log pero no fallar si no se puede verificar el estado
                    e.printStackTrace()
                }

                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}