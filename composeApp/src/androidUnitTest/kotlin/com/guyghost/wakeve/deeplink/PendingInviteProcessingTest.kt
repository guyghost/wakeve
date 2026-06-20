package com.guyghost.wakeve.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PendingInviteProcessingTest {
    @Test
    fun pendingInviteProcessingInputRequiresAuthenticationBeforeAcceptingInvite() {
        val input = pendingInviteProcessingInput(
            pendingInviteCode = "invite-123",
            isAuthenticated = false,
            processingInviteCode = null
        )

        assertEquals(PendingInviteProcessingInput.RequireAuthentication, input)
    }

    @Test
    fun pendingInviteProcessingInputIgnoresInviteAlreadyBeingProcessed() {
        val input = pendingInviteProcessingInput(
            pendingInviteCode = "invite-123",
            isAuthenticated = true,
            processingInviteCode = "invite-123"
        )

        assertEquals(PendingInviteProcessingInput.None, input)
    }

    @Test
    fun pendingInviteProcessingInputAcceptsAuthenticatedPendingInvite() {
        val input = pendingInviteProcessingInput(
            pendingInviteCode = "invite-123",
            isAuthenticated = true,
            processingInviteCode = null
        )

        assertEquals(PendingInviteProcessingInput.Accept("invite-123"), input)
    }

    @Test
    fun pendingInviteProcessingResultRoutesAcceptedInviteAndClearsCode() {
        val result = pendingInviteProcessingResult(
            InvitationDeepLinkAcceptanceResult.Accepted(
                eventId = "event-123",
                message = "Invitation acceptée."
            )
        )

        assertTrue(result.clearPendingInviteCode)
        assertEquals("event-123", result.acceptedEventId)
        assertFalse(result.navigateToAuth)
        assertEquals("Invitation acceptée.", result.message)
    }

    @Test
    fun pendingInviteProcessingResultKeepsCodeWhenAuthIsStillRequired() {
        val result = pendingInviteProcessingResult(
            InvitationDeepLinkAcceptanceResult.AuthenticationRequired(
                message = "Connectez-vous pour accepter cette invitation."
            )
        )

        assertFalse(result.clearPendingInviteCode)
        assertNull(result.acceptedEventId)
        assertTrue(result.navigateToAuth)
        assertEquals("Connectez-vous pour accepter cette invitation.", result.message)
    }

    @Test
    fun pendingInviteProcessingResultClearsCodeForRejectedInvite() {
        val result = pendingInviteProcessingResult(
            InvitationDeepLinkAcceptanceResult.Rejected(
                message = "Invitation invalide ou expirée."
            )
        )

        assertTrue(result.clearPendingInviteCode)
        assertNull(result.acceptedEventId)
        assertFalse(result.navigateToAuth)
        assertEquals("Invitation invalide ou expirée.", result.message)
    }

    @Test
    fun pendingInviteProcessingResultKeepsCodeForRetryableFailure() {
        val result = pendingInviteProcessingResult(
            InvitationDeepLinkAcceptanceResult.RetryableFailure(
                message = "Impossible de rejoindre l'événement pour le moment. Réessayez plus tard."
            )
        )

        assertFalse(result.clearPendingInviteCode)
        assertNull(result.acceptedEventId)
        assertFalse(result.navigateToAuth)
        assertEquals("Impossible de rejoindre l'événement pour le moment. Réessayez plus tard.", result.message)
    }
}
