package com.guyghost.wakeve.deeplink

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class AndroidNavigationDeepLinkParserTest {
    @Test
    fun parseDeepLink_notificationEventRoute_returnsEventDetail() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event-123")
        )

        val event = assertIs<AndroidNavigationDeepLink.EventDetail>(result)
        assertEquals("event-123", event.eventId)
    }

    @Test
    fun parseDeepLink_notificationPollRoute_returnsPollVoting() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "poll",
            pathSegments = listOf("event-123")
        )

        val poll = assertIs<AndroidNavigationDeepLink.PollVoting>(result)
        assertEquals("event-123", poll.eventId)
    }

    @Test
    fun parseDeepLink_notificationMeetingRoute_returnsMeetingDetail() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "meeting",
            pathSegments = listOf("meeting-456")
        )

        val meeting = assertIs<AndroidNavigationDeepLink.MeetingDetail>(result)
        assertEquals("meeting-456", meeting.meetingId)
    }

    @Test
    fun parseDeepLink_universalInviteWithSingleCode_returnsInvite() {
        val result = parseDeepLinkParts(
            scheme = "https",
            host = "wakeve.app",
            pathSegments = listOf("invite", "invite-code-123")
        )

        val invite = assertIs<AndroidNavigationDeepLink.Invite>(result)
        assertEquals("invite-code-123", invite.token)
    }

    @Test
    fun parseDeepLink_customInviteWithSingleCode_returnsInvite() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "invite",
            pathSegments = listOf("invite-code-123")
        )

        val invite = assertIs<AndroidNavigationDeepLink.Invite>(result)
        assertEquals("invite-code-123", invite.token)
    }

    @Test
    fun parseDeepLink_universalInviteWithExtraPathSegments_returnsNull() {
        val result = parseDeepLinkParts(
            scheme = "https",
            host = "wakeve.app",
            pathSegments = listOf("invite", "invite-code-123", "extra")
        )

        assertNull(result)
    }

    @Test
    fun parseDeepLink_customInviteWithExtraPathSegments_returnsNull() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "invite",
            pathSegments = listOf("invite-code-123", "extra")
        )

        assertNull(result)
    }

    @Test
    fun parseDeepLink_inviteWithoutCode_returnsNull() {
        val universal = parseDeepLinkParts(
            scheme = "https",
            host = "wakeve.app",
            pathSegments = listOf("invite")
        )
        val custom = parseDeepLinkParts(
            scheme = "wakeve",
            host = "invite",
            pathSegments = emptyList()
        )

        assertNull(universal)
        assertNull(custom)
    }

    @Test
    fun parseDeepLink_customEventWithExtraPathSegments_returnsNull() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event-123", "unexpected")
        )

        assertNull(result)
    }
}
