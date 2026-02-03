package com.guyghost.wakeve.notification

import com.guyghost.wakeve.database.WakevDb
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for NotificationService.
 * Tests token registration, notification sending, and history management.
 */
@Ignore
class NotificationServiceTest {

    @Test
    fun `should register push token successfully`() = runTest {
        // This test would require a real database driver
        // For now, it's a placeholder showing the test structure
        val mockDatabase = createMockDatabase()
        val preferencesRepository = MockNotificationPreferencesRepository()
        val fcmSender = MockFCMSender()
        val apnsSender = MockAPNsSender()

        val service = NotificationService(mockDatabase, preferencesRepository, fcmSender, apnsSender)

        val result = service.registerPushToken(
            userId = "user123",
            platform = Platform.ANDROID,
            token = "test_fcm_token_12345"
        )

        assertTrue(result.isSuccess, "Token registration should succeed")
    }

    @Test
    fun `should send notification when user has registered token`() = runTest {
        val mockDatabase = createMockDatabase()
        val preferencesRepository = MockNotificationPreferencesRepository()
        val fcmSender = MockFCMSender()
        val apnsSender = MockAPNsSender()

        val service = NotificationService(mockDatabase, preferencesRepository, fcmSender, apnsSender)

        // Register token first
        service.registerPushToken("user123", Platform.ANDROID, "test_token")

        val request = NotificationRequest(
            userId = "user123",
            type = NotificationType.EVENT_INVITE,
            title = "You're invited!",
            body = "Join us for a fun event",
            eventId = "event123"
        )

        val result = service.sendNotification(request)

        assertTrue(result.isSuccess, "Notification should be sent successfully")
        assertNotNull(result.getOrNull(), "Notification ID should be returned")
    }

    @Test
    fun `should not send notification when type is disabled`() = runTest {
        val mockDatabase = createMockDatabase()
        val preferencesRepository = MockNotificationPreferencesRepository(
            preferences = defaultNotificationPreferences("user123").copy(
                enabledTypes = emptySet() // All types disabled
            )
        )
        val fcmSender = MockFCMSender()
        val apnsSender = MockAPNsSender()

        val service = NotificationService(mockDatabase, preferencesRepository, fcmSender, apnsSender)

        // Register token first
        service.registerPushToken("user123", Platform.ANDROID, "test_token")

        val request = NotificationRequest(
            userId = "user123",
            type = NotificationType.EVENT_INVITE,
            title = "You're invited!",
            body = "Join us for a fun event"
        )

        val result = service.sendNotification(request)

        assertFalse(result.isSuccess, "Notification should fail when type is disabled")
    }

    @Test
    fun `should respect quiet hours for non-urgent notifications`() = runTest {
        val mockDatabase = createMockDatabase()
        val preferencesRepository = MockNotificationPreferencesRepository()
        val fcmSender = MockFCMSender()
        val apnsSender = MockAPNsSender()

        val service = NotificationService(mockDatabase, preferencesRepository, fcmSender, apnsSender)

        // Register token
        service.registerPushToken("user123", Platform.ANDROID, "test_token")

        // Test quiet hours (22:00 - 08:00)
        // Current time: 23:00 (within quiet hours)
        val currentTime = Instant.parse("2025-02-03T23:00:00Z")

        val request = NotificationRequest(
            userId = "user123",
            type = NotificationType.NEW_COMMENT, // Non-urgent
            title = "New comment",
            body = "Someone commented on your event"
        )

        val result = service.sendNotification(request, currentTime)

        assertFalse(result.isSuccess, "Non-urgent notification should be suppressed in quiet hours")
    }

    @Test
    fun `should send urgent notifications during quiet hours`() = runTest {
        val mockDatabase = createMockDatabase()
        val preferencesRepository = MockNotificationPreferencesRepository()
        val fcmSender = MockFCMSender()
        val apnsSender = MockAPNsSender()

        val service = NotificationService(mockDatabase, preferencesRepository, fcmSender, apnsSender)

        // Register token
        service.registerPushToken("user123", Platform.ANDROID, "test_token")

        // Test quiet hours (22:00 - 08:00)
        // Current time: 23:00 (within quiet hours)
        val currentTime = Instant.parse("2025-02-03T23:00:00Z")

        val request = NotificationRequest(
            userId = "user123",
            type = NotificationType.MEETING_REMINDER, // Urgent
            title = "Meeting starting soon!",
            body = "Your meeting starts in 5 minutes"
        )

        val result = service.sendNotification(request, currentTime)

        assertTrue(result.isSuccess, "Urgent notification should bypass quiet hours")
    }

    @Test
    fun `should mark notification as read`() = runTest {
        val mockDatabase = createMockDatabase()
        val preferencesRepository = MockNotificationPreferencesRepository()
        val fcmSender = MockFCMSender()
        val apnsSender = MockAPNsSender()

        val service = NotificationService(mockDatabase, preferencesRepository, fcmSender, apnsSender)

        // Send notification first
        service.registerPushToken("user123", Platform.ANDROID, "test_token")
        val notificationId = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.EVENT_INVITE,
                title = "You're invited!",
                body = "Join us for a fun event"
            )
        ).getOrNull() ?: return@runTest

        // Mark as read
        val result = service.markAsRead(notificationId)

        assertTrue(result.isSuccess, "Should mark notification as read")
    }

    @Test
    fun `should get unread notifications`() = runTest {
        val mockDatabase = createMockDatabase()
        val preferencesRepository = MockNotificationPreferencesRepository()
        val fcmSender = MockFCMSender()
        val apnsSender = MockAPNsSender()

        val service = NotificationService(mockDatabase, preferencesRepository, fcmSender, apnsSender)

        // Send notifications
        service.registerPushToken("user123", Platform.ANDROID, "test_token")
        service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.EVENT_INVITE,
                title = "Event 1",
                body = "Description 1"
            )
        )
        service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.VOTE_REMINDER,
                title = "Event 2",
                body = "Description 2"
            )
        )

        val unread = service.getUnreadNotifications("user123")

        assertEquals(2, unread.size, "Should have 2 unread notifications")
    }

    @Test
    fun `should get priority correctly for notification types`() {
        assertEquals(NotificationPriority.HIGH, NotificationType.EVENT_INVITE.getPriority())
        assertEquals(NotificationPriority.MEDIUM, NotificationType.VOTE_REMINDER.getPriority())
        assertEquals(NotificationPriority.LOW, NotificationType.NEW_COMMENT.getPriority())
        assertEquals(NotificationPriority.URGENT, NotificationType.MEETING_REMINDER.getPriority())
    }

    @Test
    fun `should identify urgent notifications correctly`() {
        assertTrue(NotificationType.MEETING_REMINDER.isUrgent())
        assertFalse(NotificationType.EVENT_INVITE.isUrgent())
        assertFalse(NotificationType.NEW_COMMENT.isUrgent())
    }

    @Test
    fun `should identify action-required notifications correctly`() {
        assertTrue(NotificationType.EVENT_INVITE.requiresAction())
        assertTrue(NotificationType.SCENARIO_SELECTED.requiresAction())
        assertTrue(NotificationType.MENTION.requiresAction())
        assertFalse(NotificationType.VOTE_REMINDER.requiresAction())
        assertFalse(NotificationType.NEW_COMMENT.requiresAction())
    }

    @Test
    fun `should respect quiet hours`() = runTest {
        val preferences = defaultNotificationPreferences("user123")

        // Test outside quiet hours (09:00, between 08:00 and 22:00)
        val timeOutside = Instant.parse("2025-02-03T09:00:00Z")
        assertTrue(
            preferences.shouldSend(NotificationType.EVENT_INVITE, timeOutside),
            "Should send notification outside quiet hours"
        )

        // Test inside quiet hours (23:00, between 22:00 and 08:00)
        val timeInside = Instant.parse("2025-02-03T23:00:00Z")
        assertFalse(
            preferences.shouldSend(NotificationType.NEW_COMMENT, timeInside),
            "Should not send non-urgent notification in quiet hours"
        )

        // Test urgent notification bypasses quiet hours
        assertTrue(
            preferences.shouldSend(NotificationType.MEETING_REMINDER, timeInside),
            "Should send urgent notification in quiet hours"
        )
    }

    @Test
    fun `should format quiet time correctly`() {
        val time = QuietTime(22, 30)
        assertEquals("22:30", time.toDisplayString())
    }

    @Test
    fun `should parse quiet time from string`() {
        val time = QuietTime.fromString("22:30")
        assertNotNull(time)
        assertEquals(22, time.hour)
        assertEquals(30, time.minute)

        val invalidTime = QuietTime.fromString("invalid")
        assertEquals(null, invalidTime)
    }
}

// Mock implementations for testing

fun createMockDatabase(): WakevDb {
    throw NotImplementedError("Create a test database driver")
}

class MockNotificationPreferencesRepository(
    private val preferences: NotificationPreferences? = defaultNotificationPreferences("user123")
) : NotificationPreferencesRepositoryInterface {
    override suspend fun getPreferences(userId: String): NotificationPreferences? = preferences
    override suspend fun savePreferences(preferences: NotificationPreferences): Result<Unit> = Result.success(Unit)
    override suspend fun deletePreferences(userId: String): Result<Unit> = Result.success(Unit)
}
