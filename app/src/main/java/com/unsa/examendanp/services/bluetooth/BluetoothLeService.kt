package com.unsa.examendanp.services.bluetooth

import android.app.*
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.unsa.examendanp.MainActivity
import com.unsa.examendanp.R
import com.unsa.examendanp.data.local.preferences.UserPreferences
import com.unsa.examendanp.utils.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@AndroidEntryPoint
class BluetoothLeService : Service() {

    @Inject
    lateinit var bluetoothScanner: BluetoothLeScannerService

    @Inject
    lateinit var userPreferences: UserPreferences

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var scanJob: Job? = null
    private var rotationJob: Job? = null

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "BluetoothServiceChannel"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startBluetoothOperations()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        bluetoothScanner.stopScanning()
        bluetoothScanner.stopAdvertising()
        super.onDestroy()
    }

    private fun startBluetoothOperations() {
        serviceScope.launch {
            val userId = userPreferences.userId.first() ?: return@launch
            val deviceId = userPreferences.deviceId.first() ?: return@launch

            // Start advertising
            bluetoothScanner.startAdvertising(userId, deviceId)

            // Start scanning with periodic stops for battery efficiency
            scanJob = launch {
                while (isActive) {
                    bluetoothScanner.startScanning(userId, deviceId)
                    delay(Constants.SCAN_PERIOD)
                    bluetoothScanner.stopScanning()
                    delay(Constants.SCAN_PERIOD)
                }
            }

            // Rotate IDs periodically
            rotationJob = launch {
                while (isActive) {
                    delay(Constants.ID_ROTATION_INTERVAL)
                    rotateIds()
                }
            }
        }
    }

    private suspend fun rotateIds() {
        val userId = userPreferences.userId.first() ?: return
        val deviceId = userPreferences.deviceId.first() ?: return

        bluetoothScanner.stopAdvertising()
        delay(1000)
        bluetoothScanner.startAdvertising(userId, deviceId)

        userPreferences.updateLastIdRotation(System.currentTimeMillis())
    }

    private fun createNotification(): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Rastreo de Contactos Activo")
            .setContentText("Detectando dispositivos cercanos para su seguridad")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Rastreo de Contactos",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notificaciones del servicio de rastreo de contactos COVID-19"
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}