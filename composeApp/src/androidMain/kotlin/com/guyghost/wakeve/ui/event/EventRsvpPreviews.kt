package com.guyghost.wakeve.ui.event

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.guyghost.wakeve.access.ParticipantRsvp
import com.guyghost.wakeve.preview.PreviewTheme

@Preview(name = "RSVP - phone pending", widthDp = 393, heightDp = 220, showBackground = true)
@Composable
private fun EventRsvpPendingPhonePreview() {
    PreviewTheme {
        EventRsvpResponseCard(
            state = EventRsvpUiState(
                participantId = "camille",
                selectedResponse = ParticipantRsvp.PENDING,
                isOrganizer = false,
                isEnabled = true,
                statusLabel = "Réponse en attente"
            ),
            onResponseSelected = {}
        )
    }
}

@Preview(name = "RSVP - tablet accepted", widthDp = 720, heightDp = 220, showBackground = true)
@Composable
private fun EventRsvpAcceptedTabletPreview() {
    PreviewTheme {
        EventRsvpResponseCard(
            state = EventRsvpUiState(
                participantId = "camille",
                selectedResponse = ParticipantRsvp.ACCEPTED,
                isOrganizer = false,
                isEnabled = true,
                statusLabel = "Participation confirmée"
            ),
            onResponseSelected = {}
        )
    }
}

@Preview(name = "RSVP - organizer locked", widthDp = 673, heightDp = 220, showBackground = true)
@Composable
private fun EventRsvpOrganizerFoldablePreview() {
    PreviewTheme {
        EventRsvpResponseCard(
            state = EventRsvpUiState(
                participantId = "organizer",
                selectedResponse = ParticipantRsvp.ACCEPTED,
                isOrganizer = true,
                isEnabled = false,
                statusLabel = "Participation confirmée"
            ),
            onResponseSelected = {}
        )
    }
}
