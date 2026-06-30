package com.guyghost.wakeve

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Data management screen for account and guest data deletion.
 *
 * Mirrors iOS DataManagementView with Material You styling.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataManagementScreen(
    isGuest: Boolean,
    isDeleting: Boolean,
    errorMessage: String?,
    successMessage: String?,
    onConfirmDeletion: () -> Unit,
    onDismissError: () -> Unit,
    onDismissSuccess: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDeletionConfirmation by remember { mutableStateOf(false) }
    val destructiveTitle = if (isGuest) "Supprimer les données invité" else "Supprimer le compte"
    val scopeDescription = if (isGuest) {
        "Cette action supprime uniquement les données locales de cet appareil en mode invité."
    } else {
        "Cette action supprime votre compte Wakeve, vos données personnelles et révoque votre session sur cet appareil."
    }
    val confirmationMessage = if (isGuest) {
        "Les données locales de cet appareil seront effacées. Cette action est irréversible."
    } else {
        "Votre compte et vos données personnelles seront supprimés. Cette action est irréversible."
    }

    if (showDeletionConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeletionConfirmation = false },
            title = { Text(destructiveTitle) },
            text = { Text(confirmationMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeletionConfirmation = false
                        onConfirmDeletion()
                    }
                ) {
                    Text(destructiveTitle, color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletionConfirmation = false }) {
                    Text("Annuler")
                }
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Gestion des données") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Contrôlez la suppression de vos données Wakeve sur cet appareil.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Portée de la suppression",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Text(
                        text = scopeDescription,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        TextButton(onClick = onDismissError) {
                            Text("Fermer")
                        }
                    }
                }
            }

            successMessage?.let { success ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = success,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        TextButton(onClick = onDismissSuccess) {
                            Text("Fermer")
                        }
                    }
                }
            }

            if (isDeleting) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            } else {
                Button(
                    onClick = { showDeletionConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = if (isGuest) Icons.Default.DeleteForever else Icons.Default.PersonOff,
                        contentDescription = null
                    )
                    Text(
                        text = destructiveTitle,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}

internal fun accountDeletionFailureMessage(): String =
    "Impossible de supprimer le compte. Réessayez."

internal fun guestDataDeletionSuccessMessage(): String =
    "Données invité supprimées"

internal fun accountDeletionSuccessMessage(): String =
    "Compte supprimé"
