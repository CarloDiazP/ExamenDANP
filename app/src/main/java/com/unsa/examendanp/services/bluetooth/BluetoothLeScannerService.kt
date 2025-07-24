package com.unsa.examendanp.services.bluetooth

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import com.unsa.examendanp.data.local.database.entities.ContactEntity
import com.unsa.examendanp.data.repository.ContactRepository
import com.unsa.examendanp.utils.Constants
import com.unsa.examendanp.utils.CryptoUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@SuppressLint("MissingPermission")
@Singleton
class BluetoothLeScannerService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ContactRepository,
    private val cryptoUtils: CryptoUtils
) {
    private val bluetoothAdapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private var bluetoothLeScanner: BluetoothLeScanner? = bluetoothAdapter?.bluetoothLeScanner
    private var bluetoothLeAdvertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser

    private val scanScope = CoroutineScope(Dispatchers.IO)
    private val detectedDevices = mutableMapOf<String, Long>()
    private var currentUserId: String? = null
    private var currentDeviceId: String? = null

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            handleScanResult(result)
        }

        override fun onBatchScanResults(results: List<ScanResult>) {
            results.forEach { handleScanResult(it) }
        }

        override fun onScanFailed(errorCode: Int) {
            // Log scan failure
        }
    }

    fun startScanning(userId: String, deviceId: String) {
        currentUserId = userId
        currentDeviceId = deviceId

        val scanFilter = ScanFilter.Builder()
            .setServiceUuid(ParcelUuid(UUID.fromString(Constants.SERVICE_UUID)))
            .build()

        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
            .build()

        bluetoothLeScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
    }

    fun stopScanning() {
        bluetoothLeScanner?.stopScan(scanCallback)
    }

    fun startAdvertising(userId: String, deviceId: String) {
        currentUserId = userId
        currentDeviceId = deviceId

        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_LOW)
            .setConnectable(false)
            .setTimeout(0)
            .build()

        val temporaryId = cryptoUtils.generateTemporaryId(userId, System.currentTimeMillis())
        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .addServiceUuid(ParcelUuid(UUID.fromString(Constants.SERVICE_UUID)))
            .addServiceData(
                ParcelUuid(UUID.fromString(Constants.SERVICE_UUID)),
                temporaryId.toByteArray()
            )
            .build()

        bluetoothLeAdvertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    fun stopAdvertising() {
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback)
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            // Advertising started successfully
        }

        override fun onStartFailure(errorCode: Int) {
            // Handle advertising failure
        }
    }

    private fun handleScanResult(result: ScanResult) {
        val scanRecord = result.scanRecord ?: return
        val serviceData = scanRecord.serviceData[ParcelUuid(UUID.fromString(Constants.SERVICE_UUID))] ?: return
        val encounteredId = String(serviceData)

        if (!cryptoUtils.isWithinRange(result.rssi)) return

        val now = System.currentTimeMillis()
        val lastSeen = detectedDevices[encounteredId]

        if (lastSeen == null) {
            // New contact
            detectedDevices[encounteredId] = now
        } else if (now - lastSeen > Constants.MIN_CONTACT_DURATION) {
            // Contact duration met, save to database
            saveContact(encounteredId, result.rssi, now - lastSeen)
            detectedDevices[encounteredId] = now
        }
    }

    private fun saveContact(encounteredId: String, rssi: Int, duration: Long) {
        scanScope.launch {
            val contact = ContactEntity(
                id = UUID.randomUUID().toString(),
                userId = currentUserId ?: return@launch,
                encounteredUserId = encounteredId,
                timestamp = Date(),
                duration = duration,
                rssi = rssi,
                distance = cryptoUtils.calculateDistance(rssi),
                isSynced = false
            )
            repository.saveContact(contact)
        }
    }
}