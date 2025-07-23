package com.unsa.examendanp.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.unsa.examendanp.utils.Constants
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = Constants.PREFERENCES_NAME)

@Singleton
class UserPreferences @Inject constructor(
    private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        val USER_ID_KEY = stringPreferencesKey(Constants.KEY_USER_ID)
        val DEVICE_ID_KEY = stringPreferencesKey(Constants.KEY_DEVICE_ID)
        val LAST_ID_ROTATION_KEY = longPreferencesKey("last_id_rotation")
        val FCM_TOKEN_KEY = stringPreferencesKey("fcm_token")
        val IS_INFECTED_KEY = booleanPreferencesKey("is_infected")
    }

    val userId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[USER_ID_KEY]
    }

    val deviceId: Flow<String?> = dataStore.data.map { preferences ->
        preferences[DEVICE_ID_KEY]
    }

    val lastIdRotation: Flow<Long> = dataStore.data.map { preferences ->
        preferences[LAST_ID_ROTATION_KEY] ?: 0L
    }

    val fcmToken: Flow<String?> = dataStore.data.map { preferences ->
        preferences[FCM_TOKEN_KEY]
    }

    val isInfected: Flow<Boolean> = dataStore.data.map { preferences ->
        preferences[IS_INFECTED_KEY] ?: false
    }

    suspend fun saveUserId(userId: String) {
        dataStore.edit { preferences ->
            preferences[USER_ID_KEY] = userId
        }
    }

    suspend fun saveDeviceId(deviceId: String) {
        dataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = deviceId
        }
    }

    suspend fun updateLastIdRotation(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_ID_ROTATION_KEY] = timestamp
        }
    }

    suspend fun saveFcmToken(token: String) {
        dataStore.edit { preferences ->
            preferences[FCM_TOKEN_KEY] = token
        }
    }

    suspend fun setInfectionStatus(isInfected: Boolean) {
        dataStore.edit { preferences ->
            preferences[IS_INFECTED_KEY] = isInfected
        }
    }
}