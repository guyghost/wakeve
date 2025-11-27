package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

data class SessionDisplayData(
    val id: String,
    val deviceName: String,
    val deviceId: String,
    val ipAddress: String?,
    val createdAt: String,
    val lastAccessed: String,
    val isCurrent: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: String,
    currentSessionId: String,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var sessions by remember { mutableStateOf<List<SessionDisplayData>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRevokeAllDialog by remember { mutableStateOf(false) }
    var sessionToRevoke by remember { mutableStateOf<SessionDisplayData?>(null) }

    // Load sessions on first composition
    LaunchedEffect(userId) {
        isLoading = true
        // TODO: Load sessions from repository
        // For now, show mock data
        sessions = listOf(
            SessionDisplayData(
                id = currentSessionId,
                deviceName = "Current Device",
                deviceId = "device-1",
                ipAddress = "192.168.1.100",
                createdAt = "2025-11-20T10:00:00Z",
                lastAccessed = "2025-11-27T14:30:00Z",
                isCurrent = true
            ),
            SessionDisplayData(
                id = "session-2",
                deviceName = "iPhone 14 Pro",
                deviceId = "device-2",
                ipAddress = "192.168.1.101",
                createdAt = "2025-11-15T08:00:00Z",
                lastAccessed = "2025-11-26T18:00:00Z",
                isCurrent = false
            ),
            SessionDisplayData(
                id = "session-3",
                deviceName = "MacBook Pro",
                deviceId = "device-3",
                ipAddress = "192.168.1.102",
                createdAt = "2025-11-10T12:00:00Z",
                lastAccessed = "2025-11-25T09:00:00Z",
                isCurrent = false
            )
        )
        isLoading = false
    }

    // Revoke all other sessions dialog
    if (showRevokeAllDialog) {
        AlertDialog(
            onDismissRequest = { showRevokeAllDialog = false },
            title = { Text("Revoke All Other Sessions?") },
            text = {
                Text("This will sign out all other devices. You will remain signed in on this device.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            // TODO: Call API to revoke all other sessions
                            sessions = sessions.filter { it.isCurrent }
                            showRevokeAllDialog = false
                        }
                    }
                ) {
                    Text("Revoke All", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRevokeAllDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Revoke single session dialog
    sessionToRevoke?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToRevoke = null },
            title = { Text("Revoke Session?") },
            text = {
                Text("This will sign out ${session.deviceName}. This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            // TODO: Call API to revoke session
                            sessions = sessions.filter { it.id != session.id }
                            sessionToRevoke = null
                        }
                    }
                ) {
                    Text("Revoke", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToRevoke = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("< Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Account Section
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                Text(
                                    "Account",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                )

                                Text(
                                    "User ID: $userId",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Active Sessions Section
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Active Sessions (${sessions.size})",
                                style = MaterialTheme.typography.titleMedium
                            )

                            if (sessions.size > 1) {
                                TextButton(
                                    onClick = { showRevokeAllDialog = true }
                                ) {
                                    Text(
                                        "Revoke All Others",
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }

                    // Session List
                    items(sessions) { session ->
                        SessionCard(
                            session = session,
                            onRevokeSession = {
                                if (!session.isCurrent) {
                                    sessionToRevoke = session
                                }
                            }
                        )
                    }

                    // Logout Button
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onLogout,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Logout")
                        }
                    }
                }
            }

            // Error message
            errorMessage?.let { error ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { errorMessage = null }) {
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
fun SessionCard(
    session: SessionDisplayData,
    onRevokeSession: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            session.deviceName,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (session.isCurrent)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.onSurface
                        )

                        if (session.isCurrent) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Current", style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.padding(start = 8.dp),
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    session.ipAddress?.let { ip ->
                        Text(
                            "IP: $ip",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Text(
                        "Last active: ${formatTimestamp(session.lastAccessed)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        "Signed in: ${formatTimestamp(session.createdAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (!session.isCurrent) {
                    TextButton(onClick = onRevokeSession) {
                        Text(
                            "Revoke",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(isoString: String): String {
    // Simplified formatting - in production use kotlinx-datetime
    return try {
        val parts = isoString.split("T")
        val date = parts[0]
        val time = parts.getOrNull(1)?.substringBefore("Z")?.substring(0, 5) ?: ""
        "$date $time"
    } catch (e: Exception) {
        isoString
    }
}
