package com.guyghost.wakeve

import com.guyghost.wakeve.presentation.participants.ParticipantManagementRow
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ParticipantManagementErrorMessageTest {
    @Test
    fun participantFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            participantAddFailureMessage(),
            contactAccessFailureMessage()
        )

        assertEquals(messages.size, messages.distinct().size)
        messages.forEach { message ->
            assertFalse(message.isBlank())
            assertDoesNotExposeSensitiveDetails(message)
        }
    }

    @Test
    fun participantAttendanceSummaryAnswersWhoIsComing() {
        val summary = participantAttendanceSummary(
            listOf(
                participantRow("organizer@example.com", participantConfirmedStatusLabel()),
                participantRow("camille@example.com", participantConfirmedStatusLabel()),
                participantRow("nora@example.com", participantPendingStatusLabel()),
                participantRow("sam@example.com", participantDeclinedStatusLabel())
            )
        )

        assertEquals("Qui vient ?", summary.title)
        assertEquals("2 sur 4 participants confirment.", summary.confirmedLabel)
        assertEquals("1 à relancer · 1 refus", summary.pendingLabel)
        assertEquals("Priorité : relancer nora@example.com.", summary.nextActionLabel)
    }

    @Test
    fun participantAttendanceSummaryHighlightsAllActiveParticipantsConfirmed() {
        val summary = participantAttendanceSummary(
            listOf(
                participantRow("organizer@example.com", participantConfirmedStatusLabel()),
                participantRow("camille@example.com", participantConfirmedStatusLabel())
            )
        )

        assertEquals("2 sur 2 participants confirment.", summary.confirmedLabel)
        assertEquals("Aucune réponse manquante.", summary.pendingLabel)
        assertEquals("Tous les participants actifs sont confirmés.", summary.nextActionLabel)
    }

    @Test
    fun participantAttendanceSummaryCompactsLongPendingList() {
        val summary = participantAttendanceSummary(
            listOf(
                participantRow("a@example.com", participantPendingStatusLabel()),
                participantRow("b@example.com", participantPendingStatusLabel()),
                participantRow("c@example.com", participantPendingStatusLabel()),
                participantRow("d@example.com", participantPendingStatusLabel())
            )
        )

        assertEquals("0 sur 4 participants confirment.", summary.confirmedLabel)
        assertEquals("4 à relancer", summary.pendingLabel)
        assertEquals("Priorité : relancer a@example.com, b@example.com, c@example.com + 1 autre.", summary.nextActionLabel)
    }

    private fun assertDoesNotExposeSensitiveDetails(message: String) {
        listOf(
            "secret@example.com",
            "SECRET",
            "SQL constraint",
            "http://internal.local",
            "token="
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }

    private fun participantRow(userIdOrEmail: String, statusLabel: String): ParticipantManagementRow {
        return ParticipantManagementRow(
            userIdOrEmail = userIdOrEmail,
            roleLabel = "Membre",
            statusLabel = statusLabel,
            canAccessOrganizationDetails = statusLabel == participantConfirmedStatusLabel()
        )
    }
}
