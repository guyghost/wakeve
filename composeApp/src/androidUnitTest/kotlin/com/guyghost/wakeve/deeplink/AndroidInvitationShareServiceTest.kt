package com.guyghost.wakeve.deeplink

import com.guyghost.wakeve.models.InvitationResponse
import io.ktor.http.HttpStatusCode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AndroidInvitationShareServiceTest {
    @Test
    fun normalizeInvitationShareEventIdTrimsValidEventIds() {
        assertEquals("event-123", normalizeInvitationShareEventId("  event-123  "))
    }

    @Test
    fun normalizeInvitationShareEventIdRejectsBlankEventIds() {
        assertNull(normalizeInvitationShareEventId(""))
        assertNull(normalizeInvitationShareEventId("   "))
        assertNull(normalizeInvitationShareEventId(null))
    }

    @Test
    fun normalizeInvitationShareEventIdRejectsPathAndQueryInjection() {
        assertNull(normalizeInvitationShareEventId("event/../other"))
        assertNull(normalizeInvitationShareEventId("event?invite=true"))
        assertNull(normalizeInvitationShareEventId("event#fragment"))
    }

    @Test
    fun invitationShareFailureMessageDoesNotExposeBackendBody() {
        assertEquals("Événement introuvable.", invitationShareFailureMessage(HttpStatusCode.NotFound))
        assertEquals(
            "Vous n'avez pas l'autorisation de créer une invitation pour cet événement.",
            invitationShareFailureMessage(HttpStatusCode.Forbidden)
        )
        assertEquals("Impossible de créer le lien d'invitation.", invitationShareFailureMessage(HttpStatusCode.InternalServerError))
        assertEquals("Impossible de créer le lien d'invitation.", invitationShareFailureMessage())
    }

    @Test
    fun normalizeCreatedInvitationResponseReturnsCanonicalLinks() {
        val result = normalizeCreatedInvitationResponse(
            invitation = invitationResponse(
                code = " INVITE123 ",
                eventId = " event-123 ",
                inviteUrl = " https://wakeve.app/invite/INVITE123 ",
                deepLinkUrl = " wakeve://invite/INVITE123 "
            ),
            expectedEventId = "event-123"
        )

        requireNotNull(result)
        assertEquals("INVITE123", result.code)
        assertEquals("event-123", result.eventId)
        assertEquals("https://wakeve.app/invite/INVITE123", result.inviteUrl)
        assertEquals("wakeve://invite/INVITE123", result.deepLinkUrl)
    }

    @Test
    fun normalizeCreatedInvitationResponseRejectsMismatchedEventOrCode() {
        val mismatchedEvent = normalizeCreatedInvitationResponse(
            invitation = invitationResponse(eventId = "other-event"),
            expectedEventId = "event-123"
        )
        val mismatchedInviteUrlCode = normalizeCreatedInvitationResponse(
            invitation = invitationResponse(inviteUrl = "https://wakeve.app/invite/OTHER123"),
            expectedEventId = "event-123"
        )
        val mismatchedDeepLinkCode = normalizeCreatedInvitationResponse(
            invitation = invitationResponse(deepLinkUrl = "wakeve://invite/OTHER123"),
            expectedEventId = "event-123"
        )

        assertNull(mismatchedEvent)
        assertNull(mismatchedInviteUrlCode)
        assertNull(mismatchedDeepLinkCode)
    }

    @Test
    fun normalizeCreatedInvitationResponseRejectsAmbiguousOrExternalLinks() {
        val externalHost = normalizeCreatedInvitationResponse(
            invitation = invitationResponse(inviteUrl = "https://evil.example/invite/INVITE123"),
            expectedEventId = "event-123"
        )
        val userInfo = normalizeCreatedInvitationResponse(
            invitation = invitationResponse(inviteUrl = "https://user@wakeve.app/invite/INVITE123"),
            expectedEventId = "event-123"
        )
        val port = normalizeCreatedInvitationResponse(
            invitation = invitationResponse(inviteUrl = "https://wakeve.app:443/invite/INVITE123"),
            expectedEventId = "event-123"
        )
        val fragment = normalizeCreatedInvitationResponse(
            invitation = invitationResponse(deepLinkUrl = "wakeve://invite/INVITE123#join"),
            expectedEventId = "event-123"
        )

        assertNull(externalHost)
        assertNull(userInfo)
        assertNull(port)
        assertNull(fragment)
    }

    private fun invitationResponse(
        id: String = "invitation-1",
        code: String = "INVITE123",
        eventId: String = "event-123",
        createdBy: String = "user-123",
        createdAt: String = "2026-06-20T10:00:00Z",
        inviteUrl: String = "https://wakeve.app/invite/INVITE123",
        deepLinkUrl: String = "wakeve://invite/INVITE123"
    ): InvitationResponse {
        return InvitationResponse(
            id = id,
            code = code,
            eventId = eventId,
            createdBy = createdBy,
            createdAt = createdAt,
            inviteUrl = inviteUrl,
            deepLinkUrl = deepLinkUrl
        )
    }
}
