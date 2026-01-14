package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
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

/**
 * Settings screen displaying account info, active sessions, and app settings.
 * 
 * Shows:
 * - Account section with user info or guest mode badge
 * - Active sessions list with revoke capability
 * - Logout button (for authenticated users)
 * - Create account button (for guests)
 * 
 * Material You design following Android guidelines.
 * 
 * @param userId The current user ID
 * @param currentSessionId The current session ID for highlighting
 * @param isGuest Whether the user is in guest mode
 * @param isAuthenticated Whether the user is authenticated
 * @param userEmail The user's email (if authenticated)
 * @param userName The user's display name (if authenticated)
 * @param onLogout Callback when user taps logout
 * @param onCreateAccount Callback when guest taps "Create Account"
 * @param onBack Callback when user taps back
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    userId: String,
    currentSessionId: String,
    isGuest: Boolean = false,
    isAuthenticated: Boolean = false,
    userEmail: String? = null,
    userName: String? = null,
    onLogout: () -> Unit,
    onCreateAccount: () -> Unit = {},
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
                title = { Text("Paramètres") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Retour"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
                        AccountSection(
                            isGuest = isGuest,
                            isAuthenticated = isAuthenticated,
                            userEmail = userEmail,
                            userName = userName,
                            userId = userId,
                            onCreateAccount = onCreateAccount
                        )
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

                    // Logout Button (only for authenticated users)
                    if (isAuthenticated && !isGuest) {
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = onLogout,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Se déconnecter")
                            }
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

/**
 * Account section composable showing user info or guest mode status.
 */
@Composable
private fun AccountSection(
    isGuest: Boolean,
    isAuthenticated: Boolean,
    userEmail: String?,
    userName: String?,
    userId: String,
    onCreateAccount: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGuest) {
                MaterialTheme.colorScheme.tertiaryContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar
                Surface(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    color = if (isGuest) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isGuest) Icons.Default.Person else Icons.Default.AccountCircle,
                            contentDescription = "Avatar",
                            modifier = Modifier.size(32.dp),
                            tint = if (isGuest) {
                                MaterialTheme.colorScheme.onTertiary
                            } else {
                                MaterialTheme.colorScheme.onPrimary
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.padding(horizontal = 8.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Compte",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = if (isGuest) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            }
                        )
                        
                        if (isGuest) {
                            Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                            AssistChip(
                                onClick = {},
                                label = { 
                                    Text(
                                        "Mode invité",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary,
                                    labelColor = MaterialTheme.colorScheme.onTertiary
                                )
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    if (isGuest) {
                        Text(
                            text = "Fonctionnalités limitées",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "Créez un compte pour sauvegarder vos données",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f)
                        )
                    } else if (isAuthenticated) {
                        userName?.let { name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        userEmail?.let { email ->
                            Text(
                                text = email,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                        if (userName == null && userEmail == null) {
                            Text(
                                text = "ID: $userId",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    } else {
                        Text(
                            text = "Non connecté",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Create Account button for guest users
            if (isGuest) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onCreateAccount,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.PersonAdd,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.padding(horizontal = 4.dp))
                    Text("Créer un compte")
                }
            }
        }
    }
}
