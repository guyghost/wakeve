package com.guyghost.wakeve.deeplink

import java.io.File
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

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
    fun parseDeepLink_plaintextUniversalInvite_returnsNull() {
        val result = parseDeepLinkParts(
            scheme = "http",
            host = "wakeve.app",
            pathSegments = listOf("invite", "invite-code-123")
        )

        assertNull(result)
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
    fun parseDeepLink_eventCreateRoute_returnsEventCreate() {
        assertIs<AndroidNavigationDeepLink.EventCreate>(
            parseDeepLinkParts(
                scheme = "wakeve",
                host = "event",
                pathSegments = listOf("create")
            )
        )
    }

    @Test
    fun androidShortcutsUseSupportedDeepLinksInsteadOfDeadExtras() {
        val shortcuts = projectFile("composeApp/src/androidMain/res/xml/shortcuts.xml").readText()
        val manifest = projectFile("composeApp/src/androidMain/AndroidManifest.xml").readText()
        val shortcutUris = Regex("""android:data="([^"]+)"""")
            .findAll(shortcuts)
            .map { it.groupValues[1] }
            .toList()

        assertEquals(
            listOf(
                "wakeve://event/create",
                "wakeve://calendar",
                "wakeve://notifications?filter=unread"
            ),
            shortcutUris
        )
        assertFalse(shortcuts.contains("shortcut_action"))
        assertTrue(manifest.contains("""android:host="home""""))
        assertTrue(manifest.contains("""android:host="notifications""""))
        shortcutUris.forEach { uri ->
            assertTrue(parseShortcutUri(uri) != null, "$uri must resolve to a supported Android deep link")
        }
    }

    @Test
    fun parseDeepLink_reservedAmbiguousEventActions_returnNull() {
        assertNull(
            parseDeepLinkParts(
                scheme = "wakeve",
                host = "event",
                pathSegments = listOf("share")
            )
        )
        assertNull(
            parseDeepLinkParts(
                scheme = "wakeve",
                host = "event",
                pathSegments = listOf("cancel")
            )
        )
    }

    @Test
    fun parseDeepLink_manifestCalendarAndReminderRoutes_returnDestinations() {
        assertIs<AndroidNavigationDeepLink.Calendar>(
            parseDeepLinkParts(
                scheme = "wakeve",
                host = "calendar",
                pathSegments = emptyList()
            )
        )
        assertIs<AndroidNavigationDeepLink.VoteReminder>(
            parseDeepLinkParts(
                scheme = "wakeve",
                host = "reminder",
                pathSegments = listOf("check-votes")
            )
        )
    }

    @Test
    fun parseDeepLink_calendarAndReminderWithExtraSegments_returnNull() {
        assertNull(
            parseDeepLinkParts(
                scheme = "wakeve",
                host = "calendar",
                pathSegments = listOf("extra")
            )
        )
        assertNull(
            parseDeepLinkParts(
                scheme = "wakeve",
                host = "reminder",
                pathSegments = listOf("check-votes", "extra")
            )
        )
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

    @Test
    fun parseDeepLink_sharedEventDetailsRoute_returnsEventDetail() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event-123", "details"),
            queryParameters = mapOf("tab" to "details")
        )

        val event = assertIs<AndroidNavigationDeepLink.EventDetail>(result)
        assertEquals("event-123", event.eventId)
    }

    @Test
    fun parseDeepLink_sharedEventDetailsCommentsTab_returnsComments() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event-123", "details"),
            queryParameters = mapOf("tab" to "comments")
        )

        val comments = assertIs<AndroidNavigationDeepLink.Comments>(result)
        assertEquals("event-123", comments.eventId)
    }

    @Test
    fun parseDeepLink_sharedEventDetailsBudgetTab_returnsBudgetOverview() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event-123", "details"),
            queryParameters = mapOf("tab" to "budget")
        )

        val budget = assertIs<AndroidNavigationDeepLink.BudgetOverview>(result)
        assertEquals("event-123", budget.eventId)
    }

    @Test
    fun parseDeepLink_sharedEventDetailsParticipantsTab_returnsParticipantManagement() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event-123", "details"),
            queryParameters = mapOf("tab" to "participants")
        )

        val participants = assertIs<AndroidNavigationDeepLink.ParticipantManagement>(result)
        assertEquals("event-123", participants.eventId)
    }

    @Test
    fun parseDeepLink_sharedPollRoute_returnsPollVoting() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event-123", "poll"),
            queryParameters = mapOf("slotId" to "slot-456")
        )

        val poll = assertIs<AndroidNavigationDeepLink.PollVoting>(result)
        assertEquals("event-123", poll.eventId)
    }

    @Test
    fun parseDeepLink_sharedScenariosRoute_returnsScenarioList() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event-123", "scenarios")
        )

        val scenarios = assertIs<AndroidNavigationDeepLink.ScenarioList>(result)
        assertEquals("event-123", scenarios.eventId)
    }

    @Test
    fun parseDeepLink_sharedMeetingsRouteUsesMeetingIdWhenPresent() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event-123", "meetings"),
            queryParameters = mapOf("meetingId" to "meeting-456")
        )

        val meeting = assertIs<AndroidNavigationDeepLink.MeetingDetail>(result)
        assertEquals("meeting-456", meeting.meetingId)
    }

    @Test
    fun parseDeepLink_sharedMeetingsRouteWithoutMeetingIdReturnsMeetingList() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event-123", "meetings")
        )

        val meetings = assertIs<AndroidNavigationDeepLink.MeetingList>(result)
        assertEquals("event-123", meetings.eventId)
    }

    @Test
    fun parseDeepLink_sharedMeetingsRouteWithInvalidMeetingIdReturnsNull() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event-123", "meetings"),
            queryParameters = mapOf("meetingId" to "meeting/456")
        )

        assertNull(result)
    }

    @Test
    fun parseDeepLink_sharedTopLevelRoutesReturnAndroidDestinations() {
        assertIs<AndroidNavigationDeepLink.Home>(
            parseDeepLinkParts("wakeve", "home", emptyList())
        )
        assertIs<AndroidNavigationDeepLink.Profile>(
            parseDeepLinkParts("wakeve", "profile", emptyList())
        )
        assertIs<AndroidNavigationDeepLink.Settings>(
            parseDeepLinkParts("wakeve", "settings", emptyList())
        )
        assertIs<AndroidNavigationDeepLink.NotificationPreferences>(
            parseDeepLinkParts(
                scheme = "wakeve",
                host = "settings",
                pathSegments = emptyList(),
                queryParameters = mapOf("category" to "notifications")
            )
        )
        val notifications = assertIs<AndroidNavigationDeepLink.Notifications>(
            parseDeepLinkParts("wakeve", "notifications", emptyList())
        )
        assertEquals(null, notifications.filter)
    }

    @Test
    fun parseDeepLink_notificationsUnreadFilterReturnsFilteredDestination() {
        val notifications = assertIs<AndroidNavigationDeepLink.Notifications>(
            parseDeepLinkParts(
                scheme = "wakeve",
                host = "notifications",
                pathSegments = emptyList(),
                queryParameters = mapOf("filter" to " unread ")
            )
        )

        assertEquals("unread", notifications.filter)
    }

    @Test
    fun parseDeepLink_notificationsRejectsUnknownFilterToDefaultInbox() {
        val notifications = assertIs<AndroidNavigationDeepLink.Notifications>(
            parseDeepLinkParts(
                scheme = "wakeve",
                host = "notifications",
                pathSegments = emptyList(),
                queryParameters = mapOf("filter" to "all")
            )
        )

        assertEquals(null, notifications.filter)
    }

    @Test
    fun parseDeepLink_trimsValidPathSegments() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf(" event-123 ")
        )

        val event = assertIs<AndroidNavigationDeepLink.EventDetail>(result)
        assertEquals("event-123", event.eventId)
    }

    @Test
    fun parseDeepLink_rejectsDecodedRouteDelimitersInPathSegment() {
        val event = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event/123")
        )
        val poll = parseDeepLinkParts(
            scheme = "wakeve",
            host = "poll",
            pathSegments = listOf("event?tab=budget")
        )
        val meeting = parseDeepLinkParts(
            scheme = "wakeve",
            host = "meeting",
            pathSegments = listOf("meeting#fragment")
        )
        val invite = parseDeepLinkParts(
            scheme = "https",
            host = "wakeve.app",
            pathSegments = listOf("invite", "INV/ITE")
        )

        assertNull(event)
        assertNull(poll)
        assertNull(meeting)
        assertNull(invite)
    }

    @Test
    fun parseDeepLink_rejectsEncodedRouteDelimitersInPathSegment() {
        val event = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event%2F123")
        )
        val poll = parseDeepLinkParts(
            scheme = "wakeve",
            host = "poll",
            pathSegments = listOf("event%3Ftab=budget")
        )
        val meeting = parseDeepLinkParts(
            scheme = "wakeve",
            host = "meeting",
            pathSegments = listOf("meeting%23fragment")
        )
        val invite = parseDeepLinkParts(
            scheme = "https",
            host = "wakeve.app",
            pathSegments = listOf("invite", "INV%2FITE")
        )

        assertNull(event)
        assertNull(poll)
        assertNull(meeting)
        assertNull(invite)
    }

    @Test
    fun parseDeepLink_rejectsEncodedDelimitersInMeetingQueryParam() {
        val result = parseDeepLinkParts(
            scheme = "wakeve",
            host = "event",
            pathSegments = listOf("event-123", "meetings"),
            queryParameters = mapOf("meetingId" to "meeting%2F456")
        )

        assertNull(result)
    }

    @Test
    fun normalizeDeepLinkPathSegmentRejectsEncodedDelimiters() {
        assertNull(normalizeDeepLinkPathSegment("event%2F123"))
        assertNull(normalizeDeepLinkPathSegment("event%3Ftab=budget"))
        assertNull(normalizeDeepLinkPathSegment("event%23fragment"))
    }

    @Test
    fun hasUnsupportedDeepLinkUriComponents_rejectsAmbiguousUriComponents() {
        assertTrue(
            hasUnsupportedDeepLinkUriComponents(
                encodedFragment = "comments",
                encodedUserInfo = null,
                port = -1
            )
        )
        assertTrue(
            hasUnsupportedDeepLinkUriComponents(
                encodedFragment = null,
                encodedUserInfo = "user",
                port = -1
            )
        )
        assertTrue(
            hasUnsupportedDeepLinkUriComponents(
                encodedFragment = null,
                encodedUserInfo = null,
                port = 443
            )
        )
        assertFalse(
            hasUnsupportedDeepLinkUriComponents(
                encodedFragment = null,
                encodedUserInfo = null,
                port = -1
            )
        )
    }

    @Test
    fun requiresAuthenticatedDeepLinkSession_marksPrivateEventDestinations() {
        val privateDestinations = listOf(
            AndroidNavigationDeepLink.EventCreate,
            AndroidNavigationDeepLink.EventDetail("event-123"),
            AndroidNavigationDeepLink.PollVoting("event-123"),
            AndroidNavigationDeepLink.ParticipantManagement("event-123"),
            AndroidNavigationDeepLink.ScenarioList("event-123"),
            AndroidNavigationDeepLink.BudgetOverview("event-123"),
            AndroidNavigationDeepLink.MeetingList("event-123"),
            AndroidNavigationDeepLink.MeetingDetail("meeting-123"),
            AndroidNavigationDeepLink.Comments("event-123"),
            AndroidNavigationDeepLink.Calendar,
            AndroidNavigationDeepLink.VoteReminder,
            AndroidNavigationDeepLink.Profile,
            AndroidNavigationDeepLink.Settings,
            AndroidNavigationDeepLink.NotificationPreferences,
            AndroidNavigationDeepLink.Notifications()
        )

        privateDestinations.forEach { destination ->
            assertEquals(true, requiresAuthenticatedDeepLinkSession(destination))
        }
    }

    @Test
    fun requiresAuthenticatedDeepLinkSession_keepsInviteAndHomePublic() {
        assertEquals(false, requiresAuthenticatedDeepLinkSession(AndroidNavigationDeepLink.Invite("INVITE123")))
        assertEquals(false, requiresAuthenticatedDeepLinkSession(AndroidNavigationDeepLink.Home))
    }

    private fun parseShortcutUri(value: String): AndroidNavigationDeepLink? {
        val uri = URI(value)
        val pathSegments = uri.rawPath
            ?.trim('/')
            ?.takeIf(String::isNotBlank)
            ?.split('/')
            ?: emptyList()
        val queryParameters = uri.rawQuery
            ?.split('&')
            ?.mapNotNull { parameter ->
                val parts = parameter.split('=', limit = 2)
                parts.firstOrNull()?.takeIf(String::isNotBlank)?.let { name ->
                    name to parts.getOrElse(1) { "" }
                }
            }
            ?.toMap()
            ?: emptyMap()

        return parseDeepLinkParts(
            scheme = uri.scheme,
            host = uri.host,
            pathSegments = pathSegments,
            queryParameters = queryParameters
        )
    }

    private fun projectFile(relativePath: String): File {
        val userDir = requireNotNull(System.getProperty("user.dir")) { "user.dir is not set" }
        var current: File? = File(userDir).absoluteFile
        while (current != null) {
            val candidate = File(current, relativePath)
            if (candidate.exists()) return candidate
            current = current.parentFile
        }
        error("Could not find project file: $relativePath")
    }
}
