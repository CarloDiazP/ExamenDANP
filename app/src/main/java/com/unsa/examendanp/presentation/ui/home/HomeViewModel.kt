package com.unsa.examendanp.presentation.ui.home

import android.app.Application
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.messaging.FirebaseMessaging
import com.unsa.examendanp.data.local.preferences.UserPreferences
import com.unsa.examendanp.data.repository.ContactRepository
import com.unsa.examendanp.domain.model.Contact
import com.unsa.examendanp.services.bluetooth.BluetoothLeService
import com.unsa.examendanp.utils.CryptoUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    application: Application,
    private val repository: ContactRepository,
    private val userPreferences: UserPreferences,
    private val cryptoUtils: CryptoUtils
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    val recentContacts: Flow<List<Contact>> = repository.getAllContacts()
    val contactCount: Flow<Int> = repository.getRecentContactCount()
    val isInfected: Flow<Boolean> = userPreferences.isInfected

    init {
        initializeUser()
        observeServiceStatus()
    }

    private fun initializeUser() {
        viewModelScope.launch {
            val existingUserId = userPreferences.userId.first()
            if (existingUserId == null) {
                val newUserId = cryptoUtils.generateUserId()
                val newDeviceId = cryptoUtils.generateUserId()

                userPreferences.saveUserId(newUserId)
                userPreferences.saveDeviceId(newDeviceId)

                try {
                    // Verificar si Firebase está disponible
                    val fcmToken = try {
                        val token = FirebaseMessaging.getInstance().token.await()
                        println("FCM Token obtenido: ${token.take(20)}...")
                        token
                    } catch (e: Exception) {
                        println("Error obteniendo FCM token: ${e.message}")
                        "offline_token_${System.currentTimeMillis()}"
                    }

                    userPreferences.saveFcmToken(fcmToken)

                    val registerResult = repository.registerUser(newUserId, fcmToken)
                    if (registerResult.isFailure) {
                        println("Error registrando usuario: ${registerResult.exceptionOrNull()?.message}")
                    }
                } catch (e: Exception) {
                    // Continuar en modo offline
                    println("Excepción general: ${e.message}")
                    e.printStackTrace()
                    _uiState.update { it.copy(error = "Modo offline: ${e.message}") }
                }
            }
            Log.d("USERID",userPreferences.userId.first().toString() )

            _uiState.update { it.copy(userId = userPreferences.userId.first() ?: "") }
        }
    }

    private fun observeServiceStatus() {
        viewModelScope.launch {
            userPreferences.userId.collect { userId ->
                _uiState.update { it.copy(userId = userId ?: "") }
            }
        }
    }

    fun startTracing() {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, BluetoothLeService::class.java)
        context.startService(serviceIntent)
        _uiState.update { it.copy(isTracingActive = true) }
    }

    fun stopTracing() {
        val context = getApplication<Application>()
        val serviceIntent = Intent(context, BluetoothLeService::class.java)
        context.stopService(serviceIntent)
        _uiState.update { it.copy(isTracingActive = false) }
    }

    fun syncContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, error = null) }

            try {
                val userId = userPreferences.userId.first()
                if (userId.isNullOrEmpty()) {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            error = "Usuario no registrado"
                        )
                    }
                    return@launch
                }

                val result = repository.syncContacts(userId)

                if (result.isSuccess) {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            lastSyncTime = System.currentTimeMillis()
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            error = result.exceptionOrNull()?.message ?: "Error al sincronizar"
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSyncing = false,
                        error = "Error: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    // Solo para pruebas
    fun toggleInfectionStatus() {
        viewModelScope.launch {
            val currentStatus = userPreferences.isInfected.first()
            userPreferences.setInfectionStatus(!currentStatus)
        }
    }
}

data class HomeUiState(
    val userId: String = "",
    val isTracingActive: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = 0L,
    val error: String? = null
)