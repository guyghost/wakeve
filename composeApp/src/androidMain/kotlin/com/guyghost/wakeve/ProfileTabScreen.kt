package com.guyghost.wakeve

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Profile Tab Screen - User info, settings, and inbox link.
 * 
 * Displays:
 * - User profile information (avatar, name, email)
 * - Guest mode badge if applicable
 * - Quick links (Inbox, Settings)
 * - Account actions (Sign out or Create Account)
 * - App information
 * 
 * Matches iOS ProfileTabView with Material You design.
 * 
 * @param userId The current user ID (may be "guest" for guest users)
 * @param isGuest Whether the user is in guest mode
 * @param isAuthenticated Whether the user is authenticated
 * @param userEmail The user's email (if authenticated)
 * @param userName The user's display name (if authenticated)
 * @param onNavigateToSettings Callback when user taps Settings
 * @param onNavigateToInbox Callback when user taps Inbox
 * @param onSignOut Callback when user signs out
 * @param onCreateAccount Callback when guest taps "Create Account"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileTabScreen(
    userId: String,
    isGuest: Boolean = false,
    isAuthenticated: Boolean = false,
    userEmail: String? = null,
    userName: String? = null,
    onNavigateToSettings: () -> Unit,
    onNavigateToInbox: () -> Unit,
    onSignOut: () -> Unit,
    onCreateAccount: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Profil",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // User Profile Card
            item {
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Avatar
                        Surface(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape),
                            color = if (isGuest) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center
                            ) {
                                // Show initials if authenticated with name, otherwise default icon
                                if (isAuthenticated && !isGuest && userName != null && userName.isNotBlank()) {
                                    Text(
                                        text = getInitials(userName),
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Avatar",
                                        modifier = Modifier.size(36.dp),
                                        tint = if (isGuest) {
                                            MaterialTheme.colorScheme.onTertiary
                                        } else {
                                            MaterialTheme.colorScheme.onPrimary
                                        }
                                    )
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        // User info
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = when {
                                        isGuest -> "Invité"
                                        userName != null -> userName
                                        else -> "Utilisateur"
                                    },
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = if (isGuest) {
                                        MaterialTheme.colorScheme.onTertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                )
                                
                                if (isGuest) {
                                    Spacer(modifier = Modifier.width(8.dp))
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
                            
                            Text(
                                text = when {
                                    isGuest -> "Fonctionnalités limitées"
                                    userEmail != null -> userEmail
                                    else -> userId
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isGuest) {
                                    MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                }
                            )
                        }
                    }
                }
            }
            
            // Quick Actions Section
            item {
                Text(
                    text = "Actions rapides",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                ProfileActionItem(
                    icon = Icons.Default.Notifications,
                    title = "Boîte de réception",
                    subtitle = "Notifications et invitations",
                    onClick = onNavigateToInbox
                )
            }
            
            item {
                ProfileActionItem(
                    icon = Icons.Default.Settings,
                    title = "Paramètres",
                    subtitle = "Préférences de l'application",
                    onClick = onNavigateToSettings
                )
            }
            
            // Account Section
            item {
                Text(
                    text = "Compte",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            // Sign Out (only for authenticated users)
            if (isAuthenticated && !isGuest) {
                item {
                    ProfileActionItem(
                        icon = Icons.Default.ExitToApp,
                        title = "Se déconnecter",
                        subtitle = "Quitter votre compte",
                        onClick = onSignOut,
                        destructive = true
                    )
                }
            }
            
            // Create Account (only for guest users)
            if (isGuest) {
                item {
                    ProfileActionItem(
                        icon = Icons.Default.PersonAdd,
                        title = "Créer un compte",
                        subtitle = "Sauvegarder vos données",
                        onClick = onCreateAccount,
                        highlight = true
                    )
                }
            }
            
            // App Info Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "À propos",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Wakeve",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Version 1.0.0 (Phase 2)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Planification d'événements collaborative avec sondages, gestion de budget, et synchronisation hors ligne.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Profile action item composable.
 */
@Composable
private fun ProfileActionItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    destructive: Boolean = false,
    highlight: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                destructive -> MaterialTheme.colorScheme.errorContainer
                highlight -> MaterialTheme.colorScheme.primaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = when {
                    destructive -> MaterialTheme.colorScheme.onErrorContainer
                    highlight -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = when {
                        destructive -> MaterialTheme.colorScheme.onErrorContainer
                        highlight -> MaterialTheme.colorScheme.onPrimaryContainer
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = when {
                        destructive -> MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.7f)
                        highlight -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    }
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowForward,
                contentDescription = "Accéder",
                tint = when {
                    destructive -> MaterialTheme.colorScheme.onErrorContainer
                    highlight -> MaterialTheme.colorScheme.onPrimaryContainer
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}

/**
 * Get initials from a user name.
 */
private fun getInitials(name: String): String {
    return name
        .split(" ")
        .filter { it.isNotBlank() }
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercaseChar() }
        .joinToString("")
        .ifEmpty { "?" }
}
