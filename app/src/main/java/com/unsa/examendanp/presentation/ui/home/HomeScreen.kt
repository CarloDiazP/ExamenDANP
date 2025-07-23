package com.unsa.examendanp.presentation.ui.home

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.accompanist.permissions.*
import com.unsa.examendanp.domain.model.Contact
import com.unsa.examendanp.utils.PermissionUtils
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val contacts by viewModel.recentContacts.collectAsStateWithLifecycle(initialValue = emptyList())
    val contactCount by viewModel.contactCount.collectAsStateWithLifecycle(initialValue = 0)
    val isInfected by viewModel.isInfected.collectAsStateWithLifecycle(initialValue = false)

    val permissionsState = rememberMultiplePermissionsState(
        permissions = PermissionUtils.requiredPermissions
    )

    LaunchedEffect(permissionsState.allPermissionsGranted) {
        if (permissionsState.allPermissionsGranted && !uiState.isTracingActive) {
            viewModel.startTracing()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rastreo de Contactos COVID-19") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (permissionsState.allPermissionsGranted) {
                FloatingActionButton(
                    onClick = { viewModel.syncContacts() },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    if (uiState.isSyncing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = "Sincronizar")
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Permission Request
            if (!permissionsState.allPermissionsGranted) {
                PermissionRequestCard(
                    onRequestPermissions = { permissionsState.launchMultiplePermissionRequest() }
                )
            } else {
                // Status Cards
                StatusSection(
                    isTracingActive = uiState.isTracingActive,
                    contactCount = contactCount,
                    isInfected = isInfected,
                    lastSyncTime = uiState.lastSyncTime,
                    onToggleTracing = {
                        if (uiState.isTracingActive) {
                            viewModel.stopTracing()
                        } else {
                            viewModel.startTracing()
                        }
                    }
                )

                // Recent Contacts
                if (contacts.isNotEmpty()) {
                    RecentContactsSection(contacts = contacts)
                }
            }

            // Error Handling
            uiState.error?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }
        }
    }
}

@Composable
fun PermissionRequestCard(
    onRequestPermissions: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Permisos Requeridos",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                "Esta aplicación necesita permisos de Bluetooth y ubicación para detectar dispositivos cercanos.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRequestPermissions,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Otorgar Permisos")
            }
        }
    }
}

@Composable
fun StatusSection(
    isTracingActive: Boolean,
    contactCount: Int,
    isInfected: Boolean,
    lastSyncTime: Long,
    onToggleTracing: () -> Unit
) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        // Tracing Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (isTracingActive)
                    MaterialTheme.colorScheme.primaryContainer
                else
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Estado del Rastreo",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        if (isTracingActive) "Activo" else "Inactivo",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (isTracingActive)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = isTracingActive,
                    onCheckedChange = { onToggleTracing() }
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Contact Count Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "$contactCount",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Contactos (24h)",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Infection Status Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(
                    containerColor = if (isInfected)
                        MaterialTheme.colorScheme.errorContainer
                    else
                        MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        if (isInfected) Icons.Default.Check else Icons.Default.Clear,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (isInfected) "Positivo" else "Negativo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Estado COVID",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Last Sync Info
        if (lastSyncTime > 0) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Última sincronización: ${formatTime(lastSyncTime)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun RecentContactsSection(contacts: List<Contact>) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp)
    ) {
        Text(
            "Contactos Recientes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(contacts.take(10)) { contact ->
                ContactItem(contact = contact)
            }
        }
    }
}

@Composable
fun ContactItem(contact: Contact) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    "ID: ${contact.encounteredUserId.take(8)}...",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "Distancia: ${String.format("%.1f", contact.distance)}m",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    formatTime(contact.timestamp.time),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    "Duración: ${contact.duration / 60000}min",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

private fun formatTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}