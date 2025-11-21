import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(
                when {
                    !currentNetworkAvailable -> Color.Red.copy(alpha = 0.1f)
                    currentSyncStatus is SyncStatus.Error -> Color(0xFFFFA500).copy(alpha = 0.1f) // Orange
                    currentSyncStatus == SyncStatus.Syncing -> Color.Blue.copy(alpha = 0.1f)
                    hasPendingChanges -> Color.Yellow.copy(alpha = 0.1f)
                    else -> Color.Green.copy(alpha = 0.1f)
                }
            )
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Network status
            val networkIcon = if (currentNetworkAvailable) "🟢" else "🔴"
            val networkText = if (currentNetworkAvailable) "Online" else "Offline"
            Text("$networkIcon $networkText", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.width(16.dp))

            // Sync status
            val (syncIcon, syncText) = when (currentSyncStatus) {
                SyncStatus.Idle -> "⏸️" to "Idle"
                SyncStatus.Syncing -> "🔄" to "Syncing"
                is SyncStatus.Error -> "❌" to "Sync Error"
            }
            Text("$syncIcon $syncText", style = MaterialTheme.typography.bodySmall)

            if (hasPendingChanges) {
                Spacer(modifier = Modifier.width(16.dp))
                Text("📝 Pending changes", style = MaterialTheme.typography.bodySmall)
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