package com.guyghost.wakeve.deeplink

import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class AndroidInvitationDeepLinkServiceTest {
    @Test
    fun normalizeInvitationAcceptanceCodeTrimsValidCodes() {
        assertEquals("invite-123", normalizeInvitationAcceptanceCode("  invite-123  "))
    }

    @Test
    fun normalizeInvitationAcceptanceCodeRejectsBlankCodes() {
        assertNull(normalizeInvitationAcceptanceCode(""))
        assertNull(normalizeInvitationAcceptanceCode("   "))
        assertNull(normalizeInvitationAcceptanceCode(null))
    }

    @Test
    fun normalizeInvitationAcceptanceCodeRejectsPathAndQueryInjection() {
        assertNull(normalizeInvitationAcceptanceCode("invite/../other"))
        assertNull(normalizeInvitationAcceptanceCode("invite?admin=true"))
        assertNull(normalizeInvitationAcceptanceCode("invite#fragment"))
    }

    @Test
    fun normalizeInvitationAcceptanceEventIdTrimsValidEventIds() {
        assertEquals("event-123", normalizeInvitationAcceptanceEventId("  event-123  "))
    }

    @Test
    fun normalizeInvitationAcceptanceEventIdRejectsBlankEventIds() {
        assertNull(normalizeInvitationAcceptanceEventId(""))
        assertNull(normalizeInvitationAcceptanceEventId("   "))
        assertNull(normalizeInvitationAcceptanceEventId(null))
    }

    @Test
    fun normalizeInvitationAcceptanceEventIdRejectsPathAndQueryInjection() {
        assertNull(normalizeInvitationAcceptanceEventId("event/../other"))
        assertNull(normalizeInvitationAcceptanceEventId("event?tab=admin"))
        assertNull(normalizeInvitationAcceptanceEventId("event#fragment"))
    }

    @Test
    fun invitationAcceptanceRejectedMessageDoesNotExposeBackendBody() {
        assertEquals("Lien d'invitation invalide.", invitationAcceptanceRejectedMessage(HttpStatusCode.BadRequest))
        assertEquals("Invitation invalide ou expirée.", invitationAcceptanceRejectedMessage(HttpStatusCode.NotFound))
        assertEquals("Invitation invalide ou expirée.", invitationAcceptanceRejectedMessage(HttpStatusCode.Gone))
    }

    @Test
    fun invitationAcceptanceRetryableFailureMessageDoesNotExposeExceptionDetails() {
        assertEquals(
            "Impossible de rejoindre l'événement pour le moment. Réessayez plus tard.",
            invitationAcceptanceRetryableFailureMessage()
        )
    }

    @Test
    fun invitationAcceptanceResultMessagesDoNotExposeBackendBody() {
        val backendBody = "SQL error for event=SECRET token=SECRET"
        val success = invitationAcceptanceSuccessMessage()
        val rejected = invitationAcceptanceRejectedByServerMessage()

        assertEquals("Invitation acceptée.", success)
        assertEquals("Invitation invalide ou expirée.", rejected)
        assertFalse(success.contains(backendBody))
        assertFalse(rejected.contains(backendBody))
        assertFalse(success.contains("SECRET"))
        assertFalse(rejected.contains("SECRET"))
        assertFalse(success.contains("token=", ignoreCase = true))
        assertFalse(rejected.contains("token=", ignoreCase = true))
    }
}
