package com.unsa.examendanp.presentation.ui.home

import android.app.Application
import android.content.Intent
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
                    val fcmToken = FirebaseMessaging.getInstance().token.await()
                    userPreferences.saveFcmToken(fcmToken)
                    repository.registerUser(newUserId, fcmToken)
                } catch (e: Exception) {
                    _uiState.update { it.copy(error = "Error al registrar usuario") }
                }
            }

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
            _uiState.update { it.copy(isSyncing = true) }

            val userId = userPreferences.userId.first() ?: return@launch
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
                        error = "Error al sincronizar contactos"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

data class HomeUiState(
    val userId: String = "",
    val isTracingActive: Boolean = false,
    val isSyncing: Boolean = false,
    val lastSyncTime: Long = 0L,
    val error: String? = null
)