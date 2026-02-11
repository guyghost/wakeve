package com.guyghost.wakeve.notification

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for RichNotificationService.
 * Covers rich notification creation, sending, validation, and retrieval.
 */
class RichNotificationServiceTest {

    @Test
    fun `RichNotification validate returns null for valid notification`() {
        val notification = createTestRichNotification(
            id = "notif-1",
            userId = "user-1",
            title = "Test Title",
            body = "Test Body"
        )

        val error = notification.validate()

        assertNull(error)
    }

    @Test
    fun `RichNotification validate returns error for blank title`() {
        val notification = createTestRichNotification(
            id = "notif-1",
            userId = "user-1",
            title = "",
            body = "Test Body"
        )

        val error = notification.validate()

        assertNotNull(error)
        assertTrue(error.contains("Title"))
    }

    @Test
    fun `RichNotification validate returns error for blank body`() {
        val notification = createTestRichNotification(
            id = "notif-1",
            userId = "user-1",
            title = "Test Title",
            body = ""
        )

        val error = notification.validate()

        assertNotNull(error)
        assertTrue(error.contains("Body"))
    }

    @Test
    fun `RichNotification validate returns error for blank userId`() {
        val notification = createTestRichNotification(
            id = "notif-1",
            userId = "",
            title = "Test Title",
            body = "Test Body"
        )

        val error = notification.validate()

        assertNotNull(error)
        assertTrue(error.contains("UserId"))
    }

    @Test
    fun `RichNotification validate returns error for blank id`() {
        val notification = createTestRichNotification(
            id = "",
            userId = "user-1",
            title = "Test Title",
            body = "Test Body"
        )

        val error = notification.validate()

        assertNotNull(error)
        assertTrue(error.contains("Id"))
    }

    @Test
    fun `RichNotification hasRichContent returns true when imageUrl is set`() {
        val notification = createTestRichNotification(
            imageUrl = "https://example.com/image.jpg"
        )

        assertTrue(notification.hasRichContent())
    }

    @Test
    fun `RichNotification hasRichContent returns true when largeIcon is set`() {
        val notification = createTestRichNotification(
            largeIcon = "https://example.com/icon.png"
        )

        assertTrue(notification.hasRichContent())
    }

    @Test
    fun `RichNotification hasRichContent returns true when customSound is set`() {
        val notification = createTestRichNotification(
            customSound = "alert_sound"
        )

        assertTrue(notification.hasRichContent())
    }

    @Test
    fun `RichNotification hasRichContent returns true when vibrationPattern is set`() {
        val notification = createTestRichNotification(
            vibrationPattern = listOf(300, 200, 300)
        )

        assertTrue(notification.hasRichContent())
    }

    @Test
    fun `RichNotification hasRichContent returns true when ledColor is set`() {
        val notification = createTestRichNotification(
            ledColor = 0xFF0000FF.toInt()
        )

        assertTrue(notification.hasRichContent())
    }

    @Test
    fun `RichNotification hasRichContent returns false when no rich content is set`() {
        val notification = createTestRichNotification()

        assertFalse(notification.hasRichContent())
    }

    @Test
    fun `RichNotification hasActions returns true when actions are present`() {
        val notification = createTestRichNotification(
            actions = listOf(
                NotificationAction("accept", "Accept", ActionType.JOIN_EVENT),
                NotificationAction("decline", "Decline", ActionType.VOTE_NO)
            )
        )

        assertTrue(notification.hasActions())
    }

    @Test
    fun `RichNotification hasActions returns false when actions are empty`() {
        val notification = createTestRichNotification()

        assertFalse(notification.hasActions())
    }

    @Test
    fun `RichNotification hasDeepLink returns true when deep link is set`() {
        val notification = createTestRichNotification(
            deepLink = "wakeve://events/123"
        )

        assertTrue(notification.hasDeepLink())
    }

    @Test
    fun `RichNotification hasDeepLink returns false when deep link is null`() {
        val notification = createTestRichNotification()

        assertFalse(notification.hasDeepLink())
    }

    @Test
    fun `RichNotification hasDeepLink returns false when deep link is blank`() {
        val notification = createTestRichNotification(
            deepLink = ""
        )

        assertFalse(notification.hasDeepLink())
    }

    @Test
    fun `RichNotificationPriority toSystemPriority returns correct values`() {
        assertEquals(2, RichNotificationPriority.HIGH.toSystemPriority())
        assertEquals(0, RichNotificationPriority.DEFAULT.toSystemPriority())
        assertEquals(-1, RichNotificationPriority.LOW.toSystemPriority())
    }

    @Test
    fun `RichNotificationPriority isInterruptive returns true only for HIGH`() {
        assertTrue(RichNotificationPriority.HIGH.isInterruptive())
        assertFalse(RichNotificationPriority.DEFAULT.isInterruptive())
        assertFalse(RichNotificationPriority.LOW.isInterruptive())
    }

    @Test
    fun `RichNotificationPriority toLegacyPriority maps correctly`() {
        assertEquals(NotificationPriority.HIGH, RichNotificationPriority.HIGH.toLegacyPriority())
        assertEquals(NotificationPriority.MEDIUM, RichNotificationPriority.DEFAULT.toLegacyPriority())
        assertEquals(NotificationPriority.LOW, RichNotificationPriority.LOW.toLegacyPriority())
    }

    @Test
    fun `NotificationPriority toRichPriority maps correctly`() {
        assertEquals(RichNotificationPriority.HIGH, NotificationPriority.HIGH.toRichPriority())
        assertEquals(RichNotificationPriority.HIGH, NotificationPriority.URGENT.toRichPriority())
        assertEquals(RichNotificationPriority.DEFAULT, NotificationPriority.MEDIUM.toRichPriority())
        assertEquals(RichNotificationPriority.LOW, NotificationPriority.LOW.toRichPriority())
    }

    @Test
    fun `RichNotificationBuilder creates notification with all properties`() {
        val notification = richNotification {
            id("test-id")
            userId("user-1")
            title("Test Title")
            body("Test Body")
            imageUrl("https://example.com/image.jpg")
            largeIcon("https://example.com/icon.png")
            priority(RichNotificationPriority.HIGH)
            category(NotificationCategory.EVENT_INVITE)
            deepLink("wakeve://events/123")
            customSound("custom_sound")
            vibrationPattern(listOf(100, 200, 100))
            ledColor(0xFF0000FF.toInt())
        }

        assertEquals("test-id", notification.id)
        assertEquals("user-1", notification.userId)
        assertEquals("Test Title", notification.title)
        assertEquals("Test Body", notification.body)
        assertEquals("https://example.com/image.jpg", notification.imageUrl)
        assertEquals("https://example.com/icon.png", notification.largeIcon)
        assertEquals(RichNotificationPriority.HIGH, notification.priority)
        assertEquals(NotificationCategory.EVENT_INVITE, notification.category)
        assertEquals("wakeve://events/123", notification.deepLink)
        assertEquals("custom_sound", notification.customSound)
        assertEquals(listOf(100, 200, 100), notification.vibrationPattern)
        assertEquals(0xFF0000FF.toInt(), notification.ledColor)
    }

    @Test
    fun `RichNotificationBuilder withDefaultActions applies event invite actions`() {
        val notification = richNotification {
            id("test-id")
            userId("user-1")
            title("Event Invite")
            body("You're invited!")
            category(NotificationCategory.EVENT_INVITE)
            withDefaultActions()
        }

        assertTrue(notification.hasActions())
        assertEquals(3, notification.actions.size)
        assertTrue(notification.actions.any { it.identifier == "accept" })
        assertTrue(notification.actions.any { it.identifier == "maybe" })
        assertTrue(notification.actions.any { it.identifier == "decline" })
    }

    @Test
    fun `RichNotificationBuilder withDefaultActions applies poll reminder actions`() {
        val notification = richNotification {
            id("test-id")
            userId("user-1")
            title("Poll Reminder")
            body("Vote now!")
            category(NotificationCategory.POLL_REMINDER)
            withDefaultActions()
        }

        assertTrue(notification.hasActions())
        assertEquals(1, notification.actions.size)
        assertEquals("vote", notification.actions[0].identifier)
    }

    @Test
    fun `RichNotificationBuilder withDefaultActions applies meeting starting actions`() {
        val notification = richNotification {
            id("test-id")
            userId("user-1")
            title("Meeting Starting")
            body("Join now!")
            category(NotificationCategory.MEETING_STARTING)
            withDefaultActions()
        }

        assertTrue(notification.hasActions())
        assertEquals(1, notification.actions.size)
        assertEquals("join", notification.actions[0].identifier)
    }

    @Test
    fun `RichNotificationBuilder withDefaultActions applies scenario vote actions`() {
        val notification = richNotification {
            id("test-id")
            userId("user-1")
            title("Scenario Vote")
            body("Vote on this scenario!")
            category(NotificationCategory.SCENARIO_VOTE)
            withDefaultActions()
        }

        assertTrue(notification.hasActions())
        assertEquals(2, notification.actions.size)
        assertTrue(notification.actions.any { it.identifier == "yes" })
        assertTrue(notification.actions.any { it.identifier == "no" })
    }

    @Test
    fun `RichNotificationBuilder withDefaultActions applies no actions for general category`() {
        val notification = richNotification {
            id("test-id")
            userId("user-1")
            title("General Notification")
            body("Just FYI")
            category(NotificationCategory.GENERAL)
            withDefaultActions()
        }

        assertFalse(notification.hasActions())
    }

    @Test
    fun `RichNotification companion constants are correct`() {
        assertEquals(listOf(300, 200, 300), RichNotification.DEFAULT_VIBRATION_PATTERN)
        assertEquals(listOf(100), RichNotification.SHORT_VIBRATION_PATTERN)
        assertEquals(listOf(500, 200, 500, 200, 500), RichNotification.URGENT_VIBRATION_PATTERN)
        assertEquals(0xFF4285F4.toInt(), RichNotification.DEFAULT_LED_COLOR)
        assertEquals(0xFFFF0000.toInt(), RichNotification.URGENT_LED_COLOR)
        assertEquals(0xFF00FF00.toInt(), RichNotification.SUCCESS_LED_COLOR)
    }

    @Test
    fun `NotificationAction companion factory methods create correct actions`() {
        val eventInviteActions = NotificationAction.eventInviteActions()
        assertEquals(3, eventInviteActions.size)
        assertTrue(eventInviteActions.any { it.type == ActionType.JOIN_EVENT })
        assertTrue(eventInviteActions.any { it.type == ActionType.VOTE_MAYBE })
        assertTrue(eventInviteActions.any { it.type == ActionType.VOTE_NO })

        val pollReminderActions = NotificationAction.pollReminderActions()
        assertEquals(1, pollReminderActions.size)
        assertEquals(ActionType.VOTE_YES, pollReminderActions[0].type)

        val meetingStartingActions = NotificationAction.meetingStartingActions()
        assertEquals(1, meetingStartingActions.size)
        assertEquals(ActionType.JOIN_MEETING, meetingStartingActions[0].type)

        val scenarioVoteActions = NotificationAction.scenarioVoteActions()
        assertEquals(2, scenarioVoteActions.size)
        assertTrue(scenarioVoteActions.any { it.type == ActionType.VOTE_YES })
        assertTrue(scenarioVoteActions.any { it.type == ActionType.VOTE_NO })
    }

    @Test
    fun `NotificationCategory fromString returns correct category`() {
        assertEquals(NotificationCategory.EVENT_INVITE, NotificationCategory.fromString("event_invite"))
        assertEquals(NotificationCategory.POLL_REMINDER, NotificationCategory.fromString("poll_reminder"))
        assertEquals(NotificationCategory.MEETING_STARTING, NotificationCategory.fromString("meeting_starting"))
        assertEquals(NotificationCategory.SCENARIO_VOTE, NotificationCategory.fromString("scenario_vote"))
        assertEquals(NotificationCategory.GENERAL, NotificationCategory.fromString("general"))
        assertNull(NotificationCategory.fromString("unknown"))
    }

    @Test
    fun `NotificationCategory getDefaultActions returns correct actions`() {
        val eventInviteActions = NotificationCategory.EVENT_INVITE.getDefaultActions()
        assertEquals(3, eventInviteActions.size)

        val pollReminderActions = NotificationCategory.POLL_REMINDER.getDefaultActions()
        assertEquals(1, pollReminderActions.size)

        val meetingStartingActions = NotificationCategory.MEETING_STARTING.getDefaultActions()
        assertEquals(1, meetingStartingActions.size)

        val scenarioVoteActions = NotificationCategory.SCENARIO_VOTE.getDefaultActions()
        assertEquals(2, scenarioVoteActions.size)

        val generalActions = NotificationCategory.GENERAL.getDefaultActions()
        assertEquals(0, generalActions.size)
    }

    @Test
    fun `RichNotification with custom sound vibration and led is valid`() {
        val notification = createTestRichNotification(
            customSound = "meeting_alert",
            vibrationPattern = listOf(500, 200, 500),
            ledColor = RichNotification.URGENT_LED_COLOR
        )

        assertNull(notification.validate())
        assertTrue(notification.hasRichContent())
        assertEquals("meeting_alert", notification.customSound)
        assertEquals(listOf(500, 200, 500), notification.vibrationPattern)
        assertEquals(RichNotification.URGENT_LED_COLOR, notification.ledColor)
    }

    // Helper function

    private fun createTestRichNotification(
        id: String = "test-id",
        userId: String = "test-user",
        title: String = "Test Title",
        body: String = "Test Body",
        imageUrl: String? = null,
        largeIcon: String? = null,
        actions: List<NotificationAction> = emptyList(),
        priority: RichNotificationPriority = RichNotificationPriority.DEFAULT,
        category: NotificationCategory = NotificationCategory.GENERAL,
        deepLink: String? = null,
        customSound: String? = null,
        vibrationPattern: List<Int>? = null,
        ledColor: Int? = null
    ): RichNotification {
        return RichNotification(
            id = id,
            userId = userId,
            title = title,
            body = body,
            imageUrl = imageUrl,
            largeIcon = largeIcon,
            actions = actions,
            priority = priority,
            category = category,
            deepLink = deepLink,
            customSound = customSound,
            vibrationPattern = vibrationPattern,
            ledColor = ledColor
        )
    }
}
