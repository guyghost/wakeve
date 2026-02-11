package com.guyghost.wakeve.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.analytics.AnalyticsProvider
import com.guyghost.wakeve.consent.ConsentRepository
import kotlinx.coroutines.launch

/**
 * RGPD Consent Dialog.
 *
 * Displays on first app launch to request user consent for analytics.
 * Shows information about data collection and user rights.
 *
 * @param consentRepository Repository for managing consent state
 * @param analyticsProvider Provider for analytics tracking
 * @param userId The current user ID for consent management
 * @param onDismiss Callback when dialog is dismissed
 */
@Composable
fun ConsentDialog(
    consentRepository: ConsentRepository,
    analyticsProvider: AnalyticsProvider,
    userId: String,
    onDismiss: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showDetails by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { /* Prevent dismiss without choice */ },
        title = {
            Text(
                "Contrôle de vos données",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column {
                Text(
                    "Nous utilisons des cookies et technologies similaires pour améliorer votre expérience et analyser l'utilisation de l'application.",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    "Données collectées :",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "• Actions dans l'app (création d'événements, votes)\n• Préférences et paramètres\n• Informations de diagnostic",
                    style = MaterialTheme.typography.bodySmall
                )

                if (showDetails) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "Vos droits RGPD : accès, rectification, effacement, portabilité, opposition. " +
                                "Vous pouvez retirer votre consentement à tout moment dans les paramètres.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                TextButton(
                    onClick = { showDetails = !showDetails },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(if (showDetails) "Masquer détails" else "En savoir plus")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        consentRepository.grantConsent(userId)
                        analyticsProvider.setEnabled(true)
                        analyticsProvider.trackEvent(AnalyticsEvent.AnalyticsConsentGranted)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Accepter")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    scope.launch {
                        consentRepository.revokeConsent(userId)
                        analyticsProvider.setEnabled(false)
                        analyticsProvider.clearUserData()
                        analyticsProvider.trackEvent(AnalyticsEvent.AnalyticsConsentRevoked)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Refuser")
            }
        }
    )
}

/**
 * Consent Settings Item.
 *
 * Displays a toggle switch in settings for managing analytics consent.
 *
 * @param consentRepository Repository for managing consent state
 * @param analyticsProvider Provider for analytics tracking
 * @param userId The current user ID for consent management
 */
@Composable
fun ConsentSettingsItem(
    consentRepository: ConsentRepository,
    analyticsProvider: AnalyticsProvider,
    userId: String
) {
    val scope = rememberCoroutineScope()
    val hasConsent by consentRepository.observeConsent().collectAsState()

    androidx.compose.material3.ListItem(
        headlineContent = {
            Text("Partage de données d'utilisation")
        },
        supportingContent = {
            Text(
                if (hasConsent) "Activé - Vos données aident à améliorer l'application"
                else "Désactivé - Aucune donnée n'est collectée",
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = {
            Switch(
                checked = hasConsent,
                onCheckedChange = { checked ->
                    scope.launch {
                        if (checked) {
                            consentRepository.grantConsent(userId)
                            analyticsProvider.setEnabled(true)
                            analyticsProvider.trackEvent(AnalyticsEvent.AnalyticsConsentGranted)
                        } else {
                            consentRepository.revokeConsent(userId)
                            analyticsProvider.setEnabled(false)
                            analyticsProvider.clearUserData()
                            analyticsProvider.trackEvent(AnalyticsEvent.AnalyticsConsentRevoked)
                        }
                    }
                }
            )
        }
    )
}
