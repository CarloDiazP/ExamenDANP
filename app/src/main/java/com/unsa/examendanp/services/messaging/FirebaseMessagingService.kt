package com.unsa.examendanp.services.messaging

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.unsa.examendanp.R
import com.unsa.examendanp.data.local.preferences.UserPreferences
import com.unsa.examendanp.data.repository.ContactRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var userPreferences: UserPreferences

    @Inject
    lateinit var repository: ContactRepository

    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        const val EXPOSURE_CHANNEL_ID = "ExposureNotifications"
        const val EXPOSURE_NOTIFICATION_ID = 100
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        scope.launch {
            userPreferences.saveFcmToken(token)

            val userId = userPreferences.userId.first()
            if (!userId.isNullOrEmpty()) {
                repository.updateFcmToken(userId, token)
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        when (remoteMessage.data["type"]) {
            "exposure_alert" -> showExposureNotification(remoteMessage)
            "infection_update" -> handleInfectionUpdate(remoteMessage)
        }
    }

    private fun showExposureNotification(remoteMessage: RemoteMessage) {
        createExposureNotificationChannel()

        val notification = NotificationCompat.Builder(this, EXPOSURE_CHANNEL_ID)
            .setContentTitle("Posible Exposición al COVID-19")
            .setContentText("Ha estado cerca de alguien que dio positivo. Tome precauciones.")
            .setSmallIcon(R.drawable.ic_warning)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(EXPOSURE_NOTIFICATION_ID, notification)
    }

    private fun handleInfectionUpdate(remoteMessage: RemoteMessage) {
        val isInfected = remoteMessage.data["isInfected"]?.toBoolean() ?: false

        scope.launch {
            userPreferences.setInfectionStatus(isInfected)
        }
    }

    private fun createExposureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                EXPOSURE_CHANNEL_ID,
                "Notificaciones de Exposición",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas sobre posible exposición al COVID-19"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}