package com.guyghost.wakeve.ui.event

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.guyghost.wakeve.access.ParticipantRsvp
import com.guyghost.wakeve.ui.designsystem.WakeveButtonGroup
import com.guyghost.wakeve.ui.designsystem.WakeveCard
import com.guyghost.wakeve.ui.designsystem.WakeveSize
import com.guyghost.wakeve.ui.designsystem.WakeveSpacing

@Composable
fun EventRsvpResponseCard(
    state: EventRsvpUiState,
    onResponseSelected: (ParticipantRsvp) -> Unit,
    modifier: Modifier = Modifier
) {
    WakeveCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("event_rsvp_card")
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.md)) {
            Column(verticalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
                Text(
                    text = "Votre réponse",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = if (state.isOrganizer) {
                        "Vous organisez cet événement. Votre participation est confirmée."
                    } else {
                        state.statusLabel
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            WakeveButtonGroup {
                RsvpOptionButton(
                    label = "Oui",
                    response = ParticipantRsvp.ACCEPTED,
                    selected = state.selectedResponse == ParticipantRsvp.ACCEPTED,
                    enabled = state.isEnabled,
                    onSelected = onResponseSelected,
                    modifier = Modifier.weight(1f)
                )
                RsvpOptionButton(
                    label = "Non",
                    response = ParticipantRsvp.DECLINED,
                    selected = state.selectedResponse == ParticipantRsvp.DECLINED,
                    enabled = state.isEnabled,
                    onSelected = onResponseSelected,
                    modifier = Modifier.weight(1f)
                )
                RsvpOptionButton(
                    label = "Peut-être",
                    response = ParticipantRsvp.PENDING,
                    selected = state.selectedResponse == ParticipantRsvp.PENDING,
                    enabled = state.isEnabled,
                    onSelected = onResponseSelected,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun RsvpOptionButton(
    label: String,
    response: ParticipantRsvp,
    selected: Boolean,
    enabled: Boolean,
    onSelected: (ParticipantRsvp) -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = when (response) {
        ParticipantRsvp.ACCEPTED -> Icons.Default.Check
        ParticipantRsvp.DECLINED -> Icons.Default.Close
        ParticipantRsvp.PENDING -> Icons.Default.QuestionMark
    }
    val buttonModifier = modifier
        .heightIn(min = WakeveSize.minTouchTarget)
        .semantics { role = Role.Button }
        .testTag("event_rsvp_${response.name.lowercase()}")

    if (selected) {
        Button(
            onClick = { onSelected(response) },
            enabled = enabled,
            modifier = buttonModifier
        ) {
            RsvpButtonContent(label = label, icon = icon)
        }
    } else if (response == ParticipantRsvp.PENDING) {
        FilledTonalButton(
            onClick = { onSelected(response) },
            enabled = enabled,
            modifier = buttonModifier
        ) {
            RsvpButtonContent(label = label, icon = icon)
        }
    } else {
        OutlinedButton(
            onClick = { onSelected(response) },
            enabled = enabled,
            modifier = buttonModifier
        ) {
            RsvpButtonContent(label = label, icon = icon)
        }
    }
}

@Composable
private fun RsvpButtonContent(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Row(horizontalArrangement = Arrangement.spacedBy(WakeveSpacing.xs)) {
        Icon(imageVector = icon, contentDescription = null)
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
