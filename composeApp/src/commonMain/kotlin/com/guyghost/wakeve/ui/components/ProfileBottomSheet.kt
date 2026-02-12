package com.guyghost.wakeve.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Profile & Settings Bottom Sheet style iOS.
 * 
 * Displays user profile card and settings options in a modal bottom sheet
 * that slides up from the bottom of the screen.
 * 
 * @param isVisible Whether the bottom sheet is visible
 * @param onDismiss Called when the bottom sheet is dismissed
 * @param userName User's display name
 * @param userEmail User's email address
 * @param userPhotoUrl User's photo URL (optional)
 * @param isGuest Whether the user is in guest mode
 * @param isAuthenticated Whether the user is authenticated
 * @param notificationsEnabled Current notifications state
 * @param calendarSyncEnabled Current calendar sync state
 * @param emailNotificationsEnabled Current email notifications state
 * @param onNotificationsClick Called when notifications row is clicked
 * @param onCalendarSyncClick Called when calendar sync row is clicked
 * @param onEmailNotificationsClick Called when email notifications row is clicked
 * @param onPrivacyClick Called when privacy section is clicked
 * @param onHelpClick Called when help link is clicked
 * @param onTermsClick Called when terms link is clicked
 * @param onSignOutClick Called when user wants to sign out
 * @param onCreateAccountClick Called when guest wants to create account
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileBottomSheet(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    userName: String? = null,
    userEmail: String? = null,
    userPhotoUrl: String? = null,
    isGuest: Boolean = false,
    isAuthenticated: Boolean = false,
    notificationsEnabled: Boolean = false,
    calendarSyncEnabled: Boolean = false,
    emailNotificationsEnabled: Boolean = false,
    onNotificationsClick: () -> Unit = {},
    onCalendarSyncClick: () -> Unit = {},
    onEmailNotificationsClick: () -> Unit = {},
    onPrivacyClick: () -> Unit = {},
    onHelpClick: () -> Unit = {},
    onTermsClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onCreateAccountClick: () -> Unit = {}
) {
    if (!isVisible) return
    
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = false
    )
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = { BottomSheetDragHandle() },
        modifier = Modifier.fillMaxHeight(0.9f)
    ) {
        ProfileSheetContent(
            userName = userName,
            userEmail = userEmail,
            userPhotoUrl = userPhotoUrl,
            isGuest = isGuest,
            isAuthenticated = isAuthenticated,
            notificationsEnabled = notificationsEnabled,
            calendarSyncEnabled = calendarSyncEnabled,
            emailNotificationsEnabled = emailNotificationsEnabled,
            onNotificationsClick = onNotificationsClick,
            onCalendarSyncClick = onCalendarSyncClick,
            onEmailNotificationsClick = onEmailNotificationsClick,
            onPrivacyClick = onPrivacyClick,
            onHelpClick = onHelpClick,
            onTermsClick = onTermsClick,
            onSignOutClick = onSignOutClick,
            onCreateAccountClick = onCreateAccountClick,
            onClose = onDismiss
        )
    }
}

@Composable
private fun BottomSheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
    }
}

@Composable
private fun ProfileSheetContent(
    userName: String?,
    userEmail: String?,
    userPhotoUrl: String?,
    isGuest: Boolean,
    isAuthenticated: Boolean,
    notificationsEnabled: Boolean,
    calendarSyncEnabled: Boolean,
    emailNotificationsEnabled: Boolean,
    onNotificationsClick: () -> Unit,
    onCalendarSyncClick: () -> Unit,
    onEmailNotificationsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onHelpClick: () -> Unit,
    onTermsClick: () -> Unit,
    onSignOutClick: () -> Unit,
    onCreateAccountClick: () -> Unit,
    onClose: () -> Unit
) {
    val displayName = userName ?: if (isGuest) "Invité" else "Utilisateur"
    val displayEmail = userEmail ?: if (isGuest) "Mode invité limité" else "Non connecté"
    
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Header with title and close button
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Spacer(modifier = Modifier.width(40.dp))
                
                Text(
                    text = "Réglages",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onBackground
                )
                
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.size(40.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Fermer",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
        
        // Profile Card
        item {
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
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
                            Color(0xFFFFA500) // Orange like in the screenshot
                        }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (userPhotoUrl != null) {
                                // TODO: Load actual image with Coil
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = displayName,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Text(
                            text = displayEmail,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        // Guest mode info text
        if (isGuest) {
            item {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Votre appareil est en mode invité. Vous pourrez créer un compte pour sauvegarder vos données et accéder à toutes les fonctionnalités.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
        
        // Settings Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            
            SettingsGroup {
                // Notifications
                SettingsRow(
                    icon = Icons.Default.Notifications,
                    title = "Notifications",
                    value = if (notificationsEnabled) "Activées" else "Désactivées",
                    onClick = onNotificationsClick
                )
                
                SettingsDivider()
                
                // Calendar Sync
                SettingsRow(
                    icon = Icons.Default.CalendarToday,
                    title = "Synchronisation du calendrier",
                    value = if (calendarSyncEnabled) "Oui" else "Non",
                    onClick = onCalendarSyncClick
                )
                
                SettingsDivider()
                
                // Email Notifications
                SettingsRow(
                    icon = Icons.Default.Email,
                    title = "Notifications par e-mail",
                    value = if (emailNotificationsEnabled) "Activées" else "Désactivées",
                    onClick = onEmailNotificationsClick
                )
            }
        }
        
        // Privacy Section
        item {
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Confidentialité et informations",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
            )
            
            SettingsGroup {
                // Privacy policy link
                SettingsLinkRow(
                    title = "Découvrez comment sont gérées vos données...",
                    onClick = onPrivacyClick
                )
                
                SettingsDivider()
                
                // Help link
                SettingsLinkRow(
                    title = "Aide",
                    onClick = onHelpClick
                )
                
                SettingsDivider()
                
                // Terms link
                SettingsLinkRow(
                    title = "Conditions générales",
                    onClick = onTermsClick
                )
            }
        }
        
        // Create Account button for guests
        if (isGuest) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onCreateAccountClick),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Créer un compte",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
        
        // Sign Out button for authenticated users
        if (isAuthenticated && !isGuest) {
            item {
                Spacer(modifier = Modifier.height(24.dp))
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSignOutClick),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Se déconnecter",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            }
        }
        
        // Bottom spacing
        item {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SettingsGroup(
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SettingsLinkRow(
    title: String,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SettingsDivider() {
    Divider(
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}
