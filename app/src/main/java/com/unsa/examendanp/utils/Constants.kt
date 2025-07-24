package com.unsa.examendanp.utils

object Constants {
    // Bluetooth
    const val SERVICE_UUID = "00001234-0000-1000-8000-00805F9B34FB"
    const val CHARACTERISTIC_UUID = "00001235-0000-1000-8000-00805F9B34FB"
    const val SCAN_PERIOD: Long = 10000 // 10 seconds
    const val ADVERTISE_PERIOD: Long = 30000 // 30 seconds
    const val RSSI_THRESHOLD = -70 // ~30 meters
    const val MIN_CONTACT_DURATION = 60000L // 1 minute

    // Firebase
    const val CONTACTS_COLLECTION = "contacts"
    const val USERS_COLLECTION = "userscovid"

    // Storage
    const val DATABASE_NAME = "contact_database"
    const val PREFERENCES_NAME = "user_preferences"
    const val KEY_USER_ID = "user_id"
    const val KEY_DEVICE_ID = "device_id"

    // Rotation
    const val ID_ROTATION_INTERVAL = 15 * 60 * 1000L // 15 minutes
    const val DATA_RETENTION_DAYS = 14

    // WorkManager
    const val SYNC_WORK_NAME = "ContactSync"
    const val CLEANUP_WORK_NAME = "DataCleanup"
}