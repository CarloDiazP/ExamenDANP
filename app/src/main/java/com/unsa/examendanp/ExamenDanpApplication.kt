package com.unsa.examendanp

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.unsa.examendanp.services.sync.ContactSyncWorker
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ExamenDanpApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun onCreate() {
        super.onCreate()
        try {
            FirebaseApp.initializeApp(this)
            val firebaseApp = FirebaseApp.getInstance()
            println("Firebase inicializado: ${firebaseApp.name}")
            println("Firebase options: ${firebaseApp.options.applicationId}")
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error al inicializar Firebase: ${e.message}")
        }
        setupPeriodicSync()
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    private fun setupPeriodicSync() {
        val syncWorkRequest = PeriodicWorkRequestBuilder<ContactSyncWorker>(
            2, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "ContactSync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }
}