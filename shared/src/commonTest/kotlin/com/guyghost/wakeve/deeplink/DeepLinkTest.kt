package com.guyghost.wakeve.deeplink

import com.guyghost.wakeve.notification.DeepLinkInfo
import com.guyghost.wakeve.notification.createNotificationDataWithDeepLink
import com.guyghost.wakeve.notification.deepLinkRoute
import com.guyghost.wakeve.notification.deepLinkUri
import com.guyghost.wakeve.notification.hasDeepLink
import com.guyghost.wakeve.notification.parseDeepLinkFromNotificationData
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

/**
 * Tests for DeepLink parsing and functionality.
 */
class DeepLinkTest {

    // ==================== DeepLink Parsing Tests ====================

    @Test
    fun parse_validEventDetailsUri_parsesCorrectly() {
        val uri = "wakeve://event/123/details?tab=votes"
        val result = DeepLink.parse(uri)

        assertTrue(result.isSuccess)
        val deepLink = result.getOrThrow()
        assertEquals("event/123/details", deepLink.route)
        assertEquals("votes", deepLink.getParameter("tab"))
        assertEquals(uri, deepLink.fullUri)
    }

    @Test
    fun parse_validUriWithoutParams_parsesCorrectly() {
        val uri = "wakeve://profile"
        val result = DeepLink.parse(uri)

        assertTrue(result.isSuccess)
        val deepLink = result.getOrThrow()
        assertEquals("profile", deepLink.route)
        assertTrue(deepLink.parameters.isEmpty())
    }

    @Test
    fun parse_validUriWithMultipleParams_parsesCorrectly() {
        val uri = "wakeve://event/456/poll?slotId=789&highlight=true"
        val result = DeepLink.parse(uri)

        assertTrue(result.isSuccess)
        val deepLink = result.getOrThrow()
        assertEquals("event/456/poll", deepLink.route)
        assertEquals("789", deepLink.getParameter("slotId"))
        assertEquals("true", deepLink.getParameter("highlight"))
    }

    @Test
    fun parse_invalidScheme_fails() {
        val uri = "https://event/123/details"
        val result = DeepLink.parse(uri)

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("wakeve://") == true)
    }

    @Test
    fun parse_blankUri_fails() {
        val result = DeepLink.parse("")

        assertTrue(result.isFailure)
    }

    @Test
    fun parse_uriWithEncodedParams_decodesCorrectly() {
        val uri = "wakeve://event/123/details?title=Hello%20World"
        val result = DeepLink.parse(uri)

        assertTrue(result.isSuccess)
        val deepLink = result.getOrThrow()
        assertEquals("Hello World", deepLink.getParameter("title"))
    }

    @Test
    fun parse_uriWithSpecialCharactersInPath_parsesCorrectly() {
        val uri = "wakeve://event/test-123_456/details"
        val result = DeepLink.parse(uri)

        assertTrue(result.isSuccess)
        val deepLink = result.getOrThrow()
        assertEquals("event/test-123_456/details", deepLink.route)
    }

    // ==================== DeepLink Creation Tests ====================

    @Test
    fun create_withRouteAndParams_createsCorrectUri() {
        val deepLink = DeepLink.create(
            route = "event/123/details",
            parameters = mapOf("tab" to "votes")
        )

        assertEquals("event/123/details", deepLink.route)
        assertEquals("wakeve://event/123/details?tab=votes", deepLink.fullUri)
    }

    @Test
    fun create_withoutParams_createsCorrectUri() {
        val deepLink = DeepLink.create(
            route = "profile",
            parameters = emptyMap()
        )

        assertEquals("profile", deepLink.route)
        assertEquals("wakeve://profile", deepLink.fullUri)
    }

    @Test
    fun create_withMultipleParams_createsCorrectUri() {
        val deepLink = DeepLink.create(
            route = "event/456/meetings",
            parameters = mapOf("meetingId" to "789", "autoJoin" to "true")
        )

        assertTrue(deepLink.fullUri.contains("meetingId=789"))
        assertTrue(deepLink.fullUri.contains("autoJoin=true"))
        assertTrue(deepLink.fullUri.startsWith("wakeve://event/456/meetings?"))
    }

    // ==================== Pattern Matching Tests ====================

    @Test
    fun matchesPattern_matchingPattern_returnsTrue() {
        val deepLink = DeepLink.create("event/123/details", emptyMap())

        assertTrue(deepLink.matchesPattern("event/{id}/details"))
    }

    @Test
    fun matchesPattern_nonMatchingPattern_returnsFalse() {
        val deepLink = DeepLink.create("event/123/details", emptyMap())

        assertFalse(deepLink.matchesPattern("event/{id}/poll"))
    }

    @Test
    fun matchesPattern_differentLength_returnsFalse() {
        val deepLink = DeepLink.create("event/123/details", emptyMap())

        assertFalse(deepLink.matchesPattern("event/{id}"))
    }

    @Test
    fun matchesPattern_exactMatch_returnsTrue() {
        val deepLink = DeepLink.create("profile", emptyMap())

        assertTrue(deepLink.matchesPattern("profile"))
    }

    @Test
    fun extractPathParameters_validPattern_extractsCorrectly() {
        val deepLink = DeepLink.create("event/123/details", emptyMap())
        val params = deepLink.extractPathParameters("event/{eventId}/details")

        assertEquals("123", params["eventId"])
    }

    @Test
    fun extractPathParameters_multipleParams_extractsAll() {
        val deepLink = DeepLink.create("event/123/poll/456", emptyMap())
        val params = deepLink.extractPathParameters("event/{eventId}/poll/{slotId}")

        assertEquals("123", params["eventId"])
        assertEquals("456", params["slotId"])
    }

    // ==================== Parameter Access Tests ====================

    @Test
    fun getParameter_existingParam_returnsValue() {
        val deepLink = DeepLink.create(
            route = "event/123/details",
            parameters = mapOf("tab" to "votes")
        )

        assertEquals("votes", deepLink.getParameter("tab"))
    }

    @Test
    fun getParameter_missingParam_returnsNull() {
        val deepLink = DeepLink.create("event/123/details", emptyMap())

        assertNull(deepLink.getParameter("nonexistent"))
    }

    @Test
    fun getRequiredParameter_existingParam_returnsValue() {
        val deepLink = DeepLink.create(
            route = "event/123/details",
            parameters = mapOf("tab" to "votes")
        )

        assertEquals("votes", deepLink.getRequiredParameter("tab"))
    }

    @Test
    fun getRequiredParameter_missingParam_throwsException() {
        val deepLink = DeepLink.create("event/123/details", emptyMap())

        assertFailsWith<IllegalArgumentException> {
            deepLink.getRequiredParameter("missing")
        }
    }

    // ==================== DeepLinkRoute Tests ====================

    @Test
    fun toDeepLinkRoute_matchingRoute_returnsRoute() {
        val deepLink = DeepLink.create("event/123/details", emptyMap())
        val route = deepLink.toDeepLinkRoute()

        assertEquals(DeepLinkRoute.EVENT_DETAILS, route)
    }

    @Test
    fun toDeepLinkRoute_unknownRoute_returnsNull() {
        val deepLink = DeepLink.create("unknown/path/here", emptyMap())
        val route = deepLink.toDeepLinkRoute()

        assertNull(route)
    }

    @Test
    fun deepLinkRoute_matches_correctlyIdentifiesMatch() {
        val deepLink = DeepLink.create("event/456/poll", emptyMap())

        assertTrue(DeepLinkRoute.EVENT_POLL.matches(deepLink))
        assertFalse(DeepLinkRoute.EVENT_DETAILS.matches(deepLink))
    }

    @Test
    fun deepLinkRoute_extractParameters_extractsCorrectly() {
        val deepLink = DeepLink.create("event/789/scenarios", emptyMap())
        val params = DeepLinkRoute.EVENT_SCENARIOS.extractParameters(deepLink)

        assertEquals("789", params["eventId"])
    }

    @Test
    fun deepLinkRoute_createUri_generatesCorrectUri() {
        val uri = DeepLinkRoute.EVENT_DETAILS.createUri(
            pathParams = mapOf("eventId" to "123"),
            queryParams = mapOf("tab" to "votes")
        )

        assertEquals("wakeve://event/123/details?tab=votes", uri)
    }

    @Test
    fun deepLinkRoute_createUri_withoutQueryParams_generatesCorrectUri() {
        val uri = DeepLinkRoute.PROFILE.createUri()

        assertEquals("wakeve://profile", uri)
    }

    @Test
    fun deepLinkRoute_fromPattern_findsCorrectRoute() {
        val route = DeepLinkRoute.fromPattern("event/{eventId}/details")

        assertEquals(DeepLinkRoute.EVENT_DETAILS, route)
    }

    @Test
    fun deepLinkRoute_fromPattern_unknownPattern_returnsNull() {
        val route = DeepLinkRoute.fromPattern("unknown/{pattern}")

        assertNull(route)
    }

    @Test
    fun deepLinkRoute_fromUri_parsesAndFindsRoute() {
        val route = DeepLinkRoute.fromUri("wakeve://event/123/meetings")

        assertEquals(DeepLinkRoute.EVENT_MEETINGS, route)
    }
}

/**
 * Tests for DeepLinkFactory.
 */
class DeepLinkFactoryTest {

    @Test
    fun createEventDetailsLink_withEventId_createsCorrectLink() {
        val deepLink = DeepLinkFactory.createEventDetailsLink("event-123")

        assertEquals("event/event-123/details", deepLink.route)
        assertEquals("wakeve://event/event-123/details", deepLink.fullUri)
    }

    @Test
    fun createEventDetailsLink_withTab_createsCorrectLink() {
        val deepLink = DeepLinkFactory.createEventDetailsLink("event-123", "participants")

        assertEquals("participants", deepLink.getParameter("tab"))
        assertTrue(deepLink.fullUri.contains("tab=participants"))
    }

    @Test
    fun createPollVoteLink_withEventId_createsCorrectLink() {
        val deepLink = DeepLinkFactory.createPollVoteLink("event-456")

        assertEquals("event/event-456/poll", deepLink.route)
    }

    @Test
    fun createPollVoteLink_withSlotId_includesSlotId() {
        val deepLink = DeepLinkFactory.createPollVoteLink("event-456", "slot-789")

        assertEquals("slot-789", deepLink.getParameter("slotId"))
    }

    @Test
    fun createScenarioComparisonLink_withEventId_createsCorrectLink() {
        val deepLink = DeepLinkFactory.createScenarioComparisonLink("event-789")

        assertEquals("event/event-789/scenarios", deepLink.route)
    }

    @Test
    fun createScenarioComparisonLink_withScenarioId_includesScenarioId() {
        val deepLink = DeepLinkFactory.createScenarioComparisonLink("event-789", "scenario-abc")

        assertEquals("scenario-abc", deepLink.getParameter("scenarioId"))
    }

    @Test
    fun createMeetingJoinLink_withIds_createsCorrectLink() {
        val deepLink = DeepLinkFactory.createMeetingJoinLink("event-123", "meeting-456")

        assertEquals("event/event-123/meetings", deepLink.route)
        assertEquals("meeting-456", deepLink.getParameter("meetingId"))
        assertNull(deepLink.getParameter("autoJoin"))
    }

    @Test
    fun createMeetingJoinLink_withAutoJoin_includesAutoJoin() {
        val deepLink = DeepLinkFactory.createMeetingJoinLink("event-123", "meeting-456", autoJoin = true)

        assertEquals("true", deepLink.getParameter("autoJoin"))
    }

    @Test
    fun createNotificationPreferencesLink_createsCorrectLink() {
        val deepLink = DeepLinkFactory.createNotificationPreferencesLink()

        assertEquals("settings", deepLink.route)
        assertEquals("notifications", deepLink.getParameter("category"))
    }

    @Test
    fun createNotificationPreferencesLink_withSection_includesSection() {
        val deepLink = DeepLinkFactory.createNotificationPreferencesLink("push")

        assertEquals("push", deepLink.getParameter("section"))
    }

    @Test
    fun createProfileLink_withoutUserId_createsCorrectLink() {
        val deepLink = DeepLinkFactory.createProfileLink()

        assertEquals("profile", deepLink.route)
        assertNull(deepLink.getParameter("userId"))
    }

    @Test
    fun createProfileLink_withUserId_includesUserId() {
        val deepLink = DeepLinkFactory.createProfileLink("user-123")

        assertEquals("user-123", deepLink.getParameter("userId"))
    }

    @Test
    fun createNotificationsListLink_createsCorrectLink() {
        val deepLink = DeepLinkFactory.createNotificationsListLink()

        assertEquals("notifications", deepLink.route)
    }

    @Test
    fun createNotificationsListLink_withFilter_includesFilter() {
        val deepLink = DeepLinkFactory.createNotificationsListLink("unread")

        assertEquals("unread", deepLink.getParameter("filter"))
    }

    @Test
    fun createHomeLink_createsCorrectLink() {
        val deepLink = DeepLinkFactory.createHomeLink()

        assertEquals("home", deepLink.route)
    }

    @Test
    fun createHomeLink_withTab_includesTab() {
        val deepLink = DeepLinkFactory.createHomeLink("upcoming")

        assertEquals("upcoming", deepLink.getParameter("tab"))
    }
}

/**
 * Tests for Notification Deep Link Integration.
 */
class NotificationDeepLinkIntegrationTest {

    @Test
    fun createNotificationDataWithDeepLink_eventDetails_createsCorrectData() {
        val data = createNotificationDataWithDeepLink(
            eventId = "event-123",
            deepLinkRoute = DeepLinkRoute.EVENT_DETAILS,
            additionalParams = mapOf("tab" to "votes")
        )

        assertTrue(data.containsKey("deepLink"))
        assertTrue(data.containsKey("deepLinkRoute"))
        assertEquals("EVENT_DETAILS", data["deepLinkRoute"])
        assertTrue(data["deepLink"]!!.contains("event/event-123/details"))
        assertEquals("votes", data["param_tab"])
    }

    @Test
    fun createNotificationDataWithDeepLink_pollVote_createsCorrectData() {
        val data = createNotificationDataWithDeepLink(
            eventId = "event-456",
            deepLinkRoute = DeepLinkRoute.EVENT_POLL,
            additionalParams = mapOf("slotId" to "slot-789")
        )

        assertTrue(data["deepLink"]!!.contains("event/event-456/poll"))
        assertEquals("slot-789", data["param_slotId"])
    }

    @Test
    fun createNotificationDataWithDeepLink_meetingJoin_createsCorrectData() {
        val data = createNotificationDataWithDeepLink(
            eventId = "event-123",
            deepLinkRoute = DeepLinkRoute.EVENT_MEETINGS,
            additionalParams = mapOf("meetingId" to "meet-456", "autoJoin" to "true")
        )

        assertTrue(data["deepLink"]!!.contains("event/event-123/meetings"))
        assertEquals("meet-456", data["param_meetingId"])
        assertEquals("true", data["param_autoJoin"])
    }

    @Test
    fun parseDeepLinkFromNotificationData_validData_parsesCorrectly() {
        val notificationData = mapOf(
            "deepLink" to "wakeve://event/123/details?tab=votes",
            "deepLinkRoute" to "EVENT_DETAILS",
            "deepLinkPath" to "event/123/details",
            "param_tab" to "votes",
            "otherData" to "value"
        )

        val deepLinkInfo = parseDeepLinkFromNotificationData(notificationData)

        assertNotNull(deepLinkInfo)
        assertEquals("wakeve://event/123/details?tab=votes", deepLinkInfo.uri)
        assertEquals(DeepLinkRoute.EVENT_DETAILS, deepLinkInfo.route)
        assertEquals("votes", deepLinkInfo.getParameter("tab"))
        assertEquals("value", deepLinkInfo.rawData["otherData"])
    }

    @Test
    fun parseDeepLinkFromNotificationData_missingDeepLink_returnsNull() {
        val notificationData = mapOf(
            "someOtherKey" to "someValue"
        )

        val deepLinkInfo = parseDeepLinkFromNotificationData(notificationData)

        assertNull(deepLinkInfo)
    }

    @Test
    fun parseDeepLinkFromNotificationData_invalidDeepLinkRoute_returnsNullRoute() {
        val notificationData = mapOf(
            "deepLink" to "wakeve://event/123/details",
            "deepLinkRoute" to "INVALID_ROUTE"
        )

        val deepLinkInfo = parseDeepLinkFromNotificationData(notificationData)

        assertNotNull(deepLinkInfo)
        assertNull(deepLinkInfo.route)
    }

    @Test
    fun hasDeepLink_withDeepLink_returnsTrue() {
        val data = mapOf("deepLink" to "wakeve://profile")

        assertTrue(data.hasDeepLink)
    }

    @Test
    fun hasDeepLink_withoutDeepLink_returnsFalse() {
        val data = mapOf("otherKey" to "value")

        assertFalse(data.hasDeepLink)
    }

    @Test
    fun deepLinkUri_withDeepLink_returnsUri() {
        val data = mapOf("deepLink" to "wakeve://profile")

        assertEquals("wakeve://profile", data.deepLinkUri)
    }

    @Test
    fun deepLinkUri_withoutDeepLink_returnsNull() {
        val data = mapOf("otherKey" to "value")

        assertNull(data.deepLinkUri)
    }

    @Test
    fun deepLinkRoute_withValidRoute_returnsRoute() {
        val data = mapOf("deepLinkRoute" to "PROFILE")

        assertEquals(DeepLinkRoute.PROFILE, data.deepLinkRoute)
    }

    @Test
    fun deepLinkRoute_withInvalidRoute_returnsNull() {
        val data = mapOf("deepLinkRoute" to "INVALID")

        assertNull(data.deepLinkRoute)
    }

    @Test
    fun deepLinkInfo_isRoute_correctlyIdentifiesRoute() {
        val info = DeepLinkInfo(
            uri = "wakeve://profile",
            route = DeepLinkRoute.PROFILE,
            path = "profile",
            parameters = emptyMap(),
            rawData = emptyMap()
        )

        assertTrue(info.isRoute(DeepLinkRoute.PROFILE))
        assertFalse(info.isRoute(DeepLinkRoute.SETTINGS))
    }
}

/**
 * Tests for DeepLinkFactory.createFromNotification.
 */
class DeepLinkFactoryNotificationTest {

    @Test
    fun createFromNotification_eventInvite_createsEventDetailsLink() {
        val deepLink = DeepLinkFactory.createFromNotification(
            notificationType = "EVENT_INVITE",
            eventId = "event-123"
        )

        assertEquals("event/event-123/details", deepLink.route)
    }

    @Test
    fun createFromNotification_voteReminder_createsPollVoteLink() {
        val deepLink = DeepLinkFactory.createFromNotification(
            notificationType = "VOTE_REMINDER",
            eventId = "event-456"
        )

        assertEquals("event/event-456/poll", deepLink.route)
    }

    @Test
    fun createFromNotification_newScenario_createsScenarioComparisonLink() {
        val deepLink = DeepLinkFactory.createFromNotification(
            notificationType = "NEW_SCENARIO",
            eventId = "event-789"
        )

        assertEquals("event/event-789/scenarios", deepLink.route)
    }

    @Test
    fun createFromNotification_meetingReminder_createsMeetingJoinLink() {
        val deepLink = DeepLinkFactory.createFromNotification(
            notificationType = "MEETING_REMINDER",
            eventId = "event-123",
            additionalParams = mapOf("meetingId" to "meet-456")
        )

        assertEquals("event/event-123/meetings", deepLink.route)
        assertEquals("meet-456", deepLink.getParameter("meetingId"))
    }

    @Test
    fun createFromNotification_mention_createsEventDetailsWithCommentsTab() {
        val deepLink = DeepLinkFactory.createFromNotification(
            notificationType = "MENTION",
            eventId = "event-123",
            additionalParams = mapOf("section" to "general")
        )

        assertEquals("event/event-123/details", deepLink.route)
        assertEquals("general", deepLink.getParameter("tab"))
    }

    @Test
    fun createFromNotification_paymentDue_createsEventDetailsWithBudgetTab() {
        val deepLink = DeepLinkFactory.createFromNotification(
            notificationType = "PAYMENT_DUE",
            eventId = "event-123"
        )

        assertEquals("event/event-123/details", deepLink.route)
        assertEquals("budget", deepLink.getParameter("tab"))
    }

    @Test
    fun createFromNotification_unknownType_createsHomeLink() {
        val deepLink = DeepLinkFactory.createFromNotification(
            notificationType = "UNKNOWN_TYPE",
            eventId = "event-123"
        )

        assertEquals("home", deepLink.route)
    }

    @Test
    fun createFromNotification_nullEventId_usesEmptyString() {
        val deepLink = DeepLinkFactory.createFromNotification(
            notificationType = "EVENT_INVITE",
            eventId = null
        )

        assertEquals("event//details", deepLink.route)
    }
}

/**
 * Tests for DeepLinkHandler (Mock implementation).
 */
class DeepLinkHandlerTest {

    @Test
    fun mockHandler_canHandle_validDeepLink_returnsTrue() {
        val handler = MockDeepLinkHandler()

        assertTrue(handler.canHandle("wakeve://event/123/details"))
    }

    @Test
    fun mockHandler_handleDeepLink_tracksHandledLink() {
        val handler = MockDeepLinkHandler()
        val deepLink = DeepLink.create("event/123/details", emptyMap())

        val result = handler.handleDeepLink(deepLink)

        assertTrue(result)
        assertTrue(handler.wasHandled("wakeve://event/123/details"))
        assertEquals(1, handler.getHandledDeepLinks().size)
    }

    @Test
    fun mockHandler_registerHandler_andInvoke() {
        val handler = MockDeepLinkHandler()
        var invoked = false
        var receivedDeepLink: DeepLink? = null

        handler.registerHandler(DeepLinkRoute.EVENT_DETAILS) { deepLink ->
            invoked = true
            receivedDeepLink = deepLink
        }

        val deepLink = DeepLink.create("event/123/details", emptyMap())
        handler.handleDeepLink(deepLink)

        assertTrue(invoked)
        assertEquals(deepLink, receivedDeepLink)
    }

    @Test
    fun mockHandler_unregisterHandler_handlerNotInvoked() {
        val handler = MockDeepLinkHandler()
        var invoked = false

        handler.registerHandler(DeepLinkRoute.EVENT_DETAILS) { _ ->
            invoked = true
        }
        handler.unregisterHandler(DeepLinkRoute.EVENT_DETAILS)

        val deepLink = DeepLink.create("event/123/details", emptyMap())
        handler.handleDeepLink(deepLink)

        assertFalse(invoked)
    }

    @Test
    fun mockHandler_clearHandlers_removesAll() {
        val handler = MockDeepLinkHandler()
        handler.registerHandler(DeepLinkRoute.EVENT_DETAILS) { _ -> }
        handler.registerHandler(DeepLinkRoute.PROFILE) { _ -> }

        handler.clearHandlers()

        assertTrue(handler.getRegisteredHandlers().isEmpty())
    }

    @Test
    fun mockHandler_getRegisteredHandlers_returnsCorrectHandlers() {
        val handler = MockDeepLinkHandler()
        val eventHandler: (DeepLink) -> Unit = { _ -> }
        val profileHandler: (DeepLink) -> Unit = { _ -> }

        handler.registerHandler(DeepLinkRoute.EVENT_DETAILS, eventHandler)
        handler.registerHandler(DeepLinkRoute.PROFILE, profileHandler)

        val registered = handler.getRegisteredHandlers()

        assertEquals(2, registered.size)
        assertTrue(registered.containsKey(DeepLinkRoute.EVENT_DETAILS))
        assertTrue(registered.containsKey(DeepLinkRoute.PROFILE))
    }
}

/**
 * Tests for DeepLinkUriBuilder.
 */
class DeepLinkUriBuilderTest {

    @Test
    fun builder_withPathParams_buildsCorrectUri() {
        val uri = DeepLinkUriBuilder.route("event/{eventId}/details")
            .pathParam("eventId", "123")
            .build()

        assertEquals("wakeve://event/123/details", uri)
    }

    @Test
    fun builder_withQueryParams_buildsCorrectUri() {
        val uri = DeepLinkUriBuilder.route("profile")
            .queryParam("tab", "settings")
            .build()

        assertEquals("wakeve://profile?tab=settings", uri)
    }

    @Test
    fun builder_withBothParams_buildsCorrectUri() {
        val uri = DeepLinkUriBuilder.route("event/{eventId}/poll")
            .pathParam("eventId", "456")
            .queryParam("slotId", "789")
            .build()

        assertTrue(uri.startsWith("wakeve://event/456/poll?"))
        assertTrue(uri.contains("slotId=789"))
    }

    @Test
    fun builder_buildDeepLink_parsesSuccessfully() {
        val result = DeepLinkUriBuilder.route("event/{eventId}/details")
            .pathParam("eventId", "123")
            .queryParam("tab", "votes")
            .buildDeepLink()

        assertTrue(result.isSuccess)
        val deepLink = result.getOrThrow()
        assertEquals("event/123/details", deepLink.route)
        assertEquals("votes", deepLink.getParameter("tab"))
    }

    @Test
    fun builder_fromDeepLinkRoute_usesCorrectPattern() {
        val uri = DeepLinkUriBuilder.route(DeepLinkRoute.EVENT_SCENARIOS)
            .pathParam("eventId", "789")
            .queryParam("scenarioId", "abc")
            .build()

        assertEquals("wakeve://event/789/scenarios?scenarioId=abc", uri)
    }

    @Test
    fun builder_multipleQueryParams_buildsCorrectly() {
        val uri = DeepLinkUriBuilder.route("settings")
            .queryParam("category", "notifications")
            .queryParam("section", "push")
            .build()

        assertTrue(uri.contains("category=notifications"))
        assertTrue(uri.contains("section=push"))
    }

    @Test
    fun builder_queryParamsMap_addsAllParams() {
        val uri = DeepLinkUriBuilder.route("event/123/details")
            .queryParams(mapOf("tab" to "votes", "highlight" to "true"))
            .build()

        assertTrue(uri.contains("tab=votes"))
        assertTrue(uri.contains("highlight=true"))
    }
}
