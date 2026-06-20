package com.guyghost.wakeve

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.sync.SyncStatus
import kotlinx.coroutines.flow.StateFlow

/**
 * Status indicator for sync and network status
 */
@Composable
fun SyncStatusIndicator(
    syncStatus: StateFlow<SyncStatus>,
    isNetworkAvailable: StateFlow<Boolean>,
    hasPendingChanges: Boolean,
    modifier: Modifier = Modifier
) {
    val currentSyncStatus by syncStatus.collectAsState()
    val currentNetworkAvailable by isNetworkAvailable.collectAsState()
    val backgroundColor = when {
        !currentNetworkAvailable -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        currentSyncStatus is SyncStatus.Error -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
        currentSyncStatus == SyncStatus.Syncing -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        hasPendingChanges -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val statusColor = when {
        !currentNetworkAvailable -> MaterialTheme.colorScheme.error
        currentSyncStatus is SyncStatus.Error -> MaterialTheme.colorScheme.error
        currentSyncStatus == SyncStatus.Syncing -> MaterialTheme.colorScheme.secondary
        hasPendingChanges -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    val textColor = when {
        !currentNetworkAvailable || currentSyncStatus is SyncStatus.Error -> MaterialTheme.colorScheme.onErrorContainer
        currentSyncStatus == SyncStatus.Syncing -> MaterialTheme.colorScheme.onSecondaryContainer
        hasPendingChanges -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(statusColor)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                networkStatusLabel(currentNetworkAvailable),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                syncStatusLabel(currentSyncStatus),
                style = MaterialTheme.typography.bodySmall,
                color = textColor
            )

            pendingChangesLabel(hasPendingChanges)?.let { pendingLabel ->
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    pendingLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor
                )
            }
        }

        // Auto-hide after 3 seconds if idle and no pending changes
        if (currentSyncStatus == SyncStatus.Idle && !hasPendingChanges && currentNetworkAvailable) {
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(3000)
                // Could add hide logic here, but for now just show
            }
        }
    }
}

@Composable
private fun StatusDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape)
    )
}

internal fun networkStatusLabel(isNetworkAvailable: Boolean): String =
    if (isNetworkAvailable) "En ligne" else "Hors ligne"

internal fun syncStatusLabel(syncStatus: SyncStatus): String =
    when (syncStatus) {
        SyncStatus.Idle -> "A jour"
        SyncStatus.Syncing -> "Synchronisation"
        is SyncStatus.Error -> "Erreur de synchronisation"
    }

internal fun pendingChangesLabel(hasPendingChanges: Boolean): String? =
    if (hasPendingChanges) "Modifications en attente" else null
