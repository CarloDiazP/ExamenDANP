package com.unsa.examendanp.utils

import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CryptoUtils @Inject constructor() {

    fun generateUserId(): String {
        return UUID.randomUUID().toString()
    }

    fun generateTemporaryId(userId: String, timestamp: Long): String {
        val input = "$userId-$timestamp"
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }.take(16)
    }

    fun calculateDistance(rssi: Int): Float {
        // Simplified path loss formula for distance estimation
        // Distance = 10^((Measured Power - RSSI) / (10 * n))
        // Where n is the path loss exponent (typically 2 for free space)
        val measuredPower = -59 // Typical BLE measured power at 1 meter
        val n = 2.0 // Path loss exponent
        return Math.pow(10.0, (measuredPower - rssi) / (10.0 * n)).toFloat()
    }

    fun isWithinRange(rssi: Int): Boolean {
        return rssi >= Constants.RSSI_THRESHOLD
    }
}