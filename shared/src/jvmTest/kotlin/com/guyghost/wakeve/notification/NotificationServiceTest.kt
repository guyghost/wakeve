package com.guyghost.wakeve.notification

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.NotificationType as ModelNotificationType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

class NotificationServiceTest {

    private lateinit var database: WakeveDb
    private lateinit var preferencesRepository: InMemoryNotificationPreferencesRepository
    private lateinit var fcmSender: RecordingFCMSender
    private lateinit var apnsSender: RecordingAPNsSender
    private lateinit var service: NotificationService

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
        preferencesRepository = InMemoryNotificationPreferencesRepository()
        fcmSender = RecordingFCMSender()
        apnsSender = RecordingAPNsSender()
        service = NotificationService(database, preferencesRepository, fcmSender, apnsSender)

        seedUser("user123")
    }

    // ==================== 1. registerPushToken Tests ====================

    @Test
    fun registerPushToken_success_storesToken() = runTest {
        val result = service.registerPushToken(
            userId = "user123",
            platform = Platform.ANDROID,
            token = "fcm-token-123"
        )

        assertTrue(result.isSuccess)

        val token = database.notificationQueries
            .getToken(user_id = "user123", platform = Platform.ANDROID.name)
            .executeAsOneOrNull()

        assertNotNull(token)
        assertEquals("fcm-token-123", token.token)
        assertEquals("user123", token.user_id)
        assertEquals(Platform.ANDROID.name, token.platform)
    }

    @Test
    fun registerPushToken_success_overwritesExistingToken() = runTest {
        // First registration
        service.registerPushToken("user123", Platform.ANDROID, "old-token").getOrThrow()

        // Second registration (should overwrite)
        val result = service.registerPushToken(
            userId = "user123",
            platform = Platform.ANDROID,
            token = "new-token-456"
        )

        assertTrue(result.isSuccess)

        val token = database.notificationQueries
            .getToken(user_id = "user123", platform = Platform.ANDROID.name)
            .executeAsOneOrNull()

        assertNotNull(token)
        assertEquals("new-token-456", token.token)
    }

    @Test
    fun registerPushToken_success_allowsMultiplePlatformsForSameUser() = runTest {
        // Register Android token
        service.registerPushToken("user123", Platform.ANDROID, "android-token").getOrThrow()

        // Register iOS token
        val result = service.registerPushToken("user123", Platform.IOS, "ios-token")

        assertTrue(result.isSuccess)

        val tokens = database.notificationQueries.getTokensByUser("user123").executeAsList()
        assertEquals(2, tokens.size)
        assertTrue(tokens.any { it.platform == Platform.ANDROID.name && it.token == "android-token" })
        assertTrue(tokens.any { it.platform == Platform.IOS.name && it.token == "ios-token" })
    }

    // ==================== 2. unregisterPushToken Tests ====================

    @Test
    fun unregisterPushToken_success_removesToken() = runTest {
        // Setup: Register a token
        service.registerPushToken("user123", Platform.ANDROID, "token-to-remove").getOrThrow()

        // Verify token exists
        val before = database.notificationQueries
            .getToken(user_id = "user123", platform = Platform.ANDROID.name)
            .executeAsOneOrNull()
        assertNotNull(before)

        // Unregister
        val result = service.unregisterPushToken("user123", Platform.ANDROID)

        assertTrue(result.isSuccess)

        // Verify token removed
        val after = database.notificationQueries
            .getToken(user_id = "user123", platform = Platform.ANDROID.name)
            .executeAsOneOrNull()
        assertNull(after)
    }

    @Test
    fun unregisterPushToken_success_whenTokenDoesNotExist() = runTest {
        // Try to unregister non-existent token
        val result = service.unregisterPushToken("user123", Platform.IOS)

        assertTrue(result.isSuccess)
    }

    // ==================== 3. sendNotification Tests ====================

    @Test
    fun sendNotification_success_sendsViaFCM() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token-123").getOrThrow()

        val result = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.EVENT_INVITE,
                title = "Invitation",
                body = "Vous etes invite",
                data = mapOf("eventId" to "evt-123")
            )
        )

        assertTrue(result.isSuccess)
        val notificationId = result.getOrThrow()
        assertTrue(notificationId.isNotBlank())
        assertTrue(fcmSender.sentTokens.contains("fcm-token-123"))
        assertEquals(1, fcmSender.callCount)
    }

    @Test
    fun sendNotification_success_sendsViaAPNsForIOSToken() = runTest {
        service.registerPushToken("user123", Platform.IOS, "apns-token-456").getOrThrow()

        val result = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.DATE_CONFIRMED,
                title = "Date Confirmed",
                body = "The event date has been confirmed"
            )
        )

        assertTrue(result.isSuccess)
        assertTrue(apnsSender.sentTokens.contains("apns-token-456"))
        assertEquals(0, fcmSender.callCount)
        assertEquals(1, apnsSender.callCount)
    }

    @Test
    fun sendNotification_success_sendsToMultipleDevices() = runTest {
        // Register both Android and iOS tokens
        service.registerPushToken("user123", Platform.ANDROID, "android-token").getOrThrow()
        service.registerPushToken("user123", Platform.IOS, "ios-token").getOrThrow()

        val result = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.EVENT_INVITE,
                title = "Test",
                body = "Test body"
            )
        )

        assertTrue(result.isSuccess)
        assertEquals(1, fcmSender.callCount)
        assertEquals(1, apnsSender.callCount)
    }

    @Test
    fun sendNotification_fails_whenTypeIsDisabled() = runTest {
        // Setup preferences with EVENT_INVITE disabled
        preferencesRepository.setPreferences(
            defaultNotificationPreferences("user123").copy(
                enabledTypes = setOf(NotificationType.MENTION) // Only MENTION enabled
            )
        )

        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        val result = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.EVENT_INVITE,
                title = "Invitation",
                body = "Body"
            )
        )

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("disabled") == true)
        assertEquals(0, fcmSender.callCount)
    }

    @Test
    fun sendNotification_fails_duringQuietHours() = runTest {
        // Setup quiet hours from 22:00 to 08:00
        val testTime = Instant.parse("2026-01-15T23:00:00Z") // 23:00 UTC - in quiet hours

        preferencesRepository.setPreferences(
            defaultNotificationPreferences("user123").copy(
                quietHoursStart = QuietTime(22, 0),
                quietHoursEnd = QuietTime(8, 0),
                enabledTypes = NotificationType.entries.toSet() // All enabled
            )
        )

        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        val result = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.VOTE_REMINDER, // Non-urgent type
                title = "Reminder",
                body = "Vote now"
            ),
            currentTime = testTime
        )

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("quiet hours") == true)
    }

    @Test
    fun sendNotification_success_duringQuietHoursForUrgentNotification() = runTest {
        // Urgent notifications (like MEETING_REMINDER) should bypass quiet hours
        val testTime = Instant.parse("2026-01-15T23:00:00Z") // 23:00 UTC - in quiet hours

        preferencesRepository.setPreferences(
            defaultNotificationPreferences("user123").copy(
                quietHoursStart = QuietTime(22, 0),
                quietHoursEnd = QuietTime(8, 0),
                enabledTypes = NotificationType.entries.toSet()
            )
        )

        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        val result = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.MEETING_REMINDER, // Urgent type
                title = "Meeting Soon",
                body = "Your meeting starts in 5 minutes"
            ),
            currentTime = testTime
        )

        assertTrue(result.isSuccess)
        assertEquals(1, fcmSender.callCount)
    }

    @Test
    fun sendNotification_fails_whenNoTokensRegistered() = runTest {
        // No token registered for user

        val result = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.EVENT_INVITE,
                title = "Invitation",
                body = "Body"
            )
        )

        assertFalse(result.isSuccess)
        assertTrue(result.exceptionOrNull()?.message?.contains("No push tokens") == true)
    }

    @Test
    fun sendNotification_persistsNotificationInDatabase() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        val result = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.EVENT_INVITE,
                title = "Test Title",
                body = "Test Body",
                data = mapOf("key1" to "value1", "key2" to "value2")
            )
        )

        assertTrue(result.isSuccess)
        val notificationId = result.getOrThrow()

        // Verify notification stored in database
        val dbNotification = database.notificationQueries
            .getNotificationById(notificationId)
            .executeAsOneOrNull()

        assertNotNull(dbNotification)
        assertEquals("user123", dbNotification.user_id)
        assertEquals(NotificationType.EVENT_INVITE.name, dbNotification.type)
        assertEquals("Test Title", dbNotification.title)
        assertEquals("Test Body", dbNotification.body)
        assertEquals(0, dbNotification.is_read)
        assertNotNull(dbNotification.sent_at)
    }

    // ==================== 4. getUnreadNotifications Tests ====================

    @Test
    fun getUnreadNotifications_returnsOnlyUnreadNotifications() = runTest {
        // Setup: Send 3 notifications
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        val id1 = service.sendNotification(createRequest("Notification 1")).getOrThrow()
        val id2 = service.sendNotification(createRequest("Notification 2")).getOrThrow()
        val id3 = service.sendNotification(createRequest("Notification 3")).getOrThrow()

        // Mark one as read
        service.markAsRead(id2).getOrThrow()

        // Get unread
        val unread = service.getUnreadNotifications("user123")

        assertEquals(2, unread.size)
        val unreadIds = unread.map { it.id }
        assertTrue(unreadIds.contains(id1))
        assertTrue(unreadIds.contains(id3))
        assertFalse(unreadIds.contains(id2))
    }

    @Test
    fun getUnreadNotifications_returnsEmptyWhenAllRead() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        val id1 = service.sendNotification(createRequest("Notification 1")).getOrThrow()
        service.markAsRead(id1).getOrThrow()

        val unread = service.getUnreadNotifications("user123")

        assertTrue(unread.isEmpty())
    }

    @Test
    fun getUnreadNotifications_returnsEmptyForUserWithNoNotifications() = runTest {
        val unread = service.getUnreadNotifications("user123")
        assertTrue(unread.isEmpty())
    }

    // ==================== 5. getNotifications Tests ====================

    @Test
    fun getNotifications_withDefaultLimit() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        // Send 60 notifications
        repeat(60) { i ->
            service.sendNotification(createRequest("Notification $i")).getOrThrow()
        }

        // Default limit is 50
        val notifications = service.getNotifications("user123")

        assertEquals(50, notifications.size)
    }

    @Test
    fun getNotifications_withCustomLimit() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        // Send 20 notifications
        repeat(20) { i ->
            service.sendNotification(createRequest("Notification $i")).getOrThrow()
        }

        // Get with custom limit of 10
        val notifications = service.getNotifications("user123", limit = 10)

        assertEquals(10, notifications.size)
    }

    @Test
    fun getNotifications_returnsReadAndUnread() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        val id1 = service.sendNotification(createRequest("Unread")).getOrThrow()
        val id2 = service.sendNotification(createRequest("Read")).getOrThrow()
        service.markAsRead(id2).getOrThrow()

        val notifications = service.getNotifications("user123")

        assertEquals(2, notifications.size)
    }

    // ==================== 6. markAsRead Tests ====================

    @Test
    fun markAsRead_success_marksNotificationAsRead() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        val notificationId = service.sendNotification(createRequest("Test")).getOrThrow()

        // Verify initially unread
        val before = service.getUnreadNotifications("user123")
        assertEquals(1, before.size)

        // Mark as read
        val result = service.markAsRead(notificationId)

        assertTrue(result.isSuccess)

        // Verify now read
        val after = service.getUnreadNotifications("user123")
        assertTrue(after.isEmpty())

        // Verify read_at is set in database
        val dbNotification = database.notificationQueries
            .getNotificationById(notificationId)
            .executeAsOneOrNull()
        assertNotNull(dbNotification)
        assertEquals(1, dbNotification.is_read)
        assertNotNull(dbNotification.read_at)
    }

    // ==================== 7. markAllAsRead Tests ====================

    @Test
    fun markAllAsRead_success_marksAllAsRead() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        // Send 5 notifications
        repeat(5) { i ->
            service.sendNotification(createRequest("Notification $i")).getOrThrow()
        }

        // Verify all unread
        assertEquals(5, service.getUnreadNotifications("user123").size)

        // Mark all as read
        val result = service.markAllAsRead("user123")

        assertTrue(result.isSuccess)
        assertTrue(service.getUnreadNotifications("user123").isEmpty())
        assertEquals(5, service.getNotifications("user123").size) // Still exist, just read
    }

    @Test
    fun markAllAsRead_success_whenNoUnread() = runTest {
        val result = service.markAllAsRead("user123")
        assertTrue(result.isSuccess)
    }

    // ==================== 8. deleteNotification Tests ====================

    @Test
    fun deleteNotification_success_removesNotification() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        val notificationId = service.sendNotification(createRequest("To Delete")).getOrThrow()

        // Verify exists
        assertEquals(1, service.getNotifications("user123").size)

        // Delete
        val result = service.deleteNotification(notificationId)

        assertTrue(result.isSuccess)
        assertTrue(service.getNotifications("user123").isEmpty())

        // Verify not in database
        val dbNotification = database.notificationQueries
            .getNotificationById(notificationId)
            .executeAsOneOrNull()
        assertNull(dbNotification)
    }

    @Test
    fun deleteNotification_success_whenNotificationDoesNotExist() = runTest {
        val result = service.deleteNotification("non-existent-id")
        assertTrue(result.isSuccess)
    }

    // ==================== 9. getPreferences Tests ====================

    @Test
    fun getPreferences_returnsStoredPreferences() = runTest {
        val customPrefs = NotificationPreferences(
            userId = "user123",
            enabledTypes = setOf(NotificationType.MENTION, NotificationType.EVENT_INVITE),
            quietHoursStart = QuietTime(23, 30),
            quietHoursEnd = QuietTime(7, 0),
            soundEnabled = false,
            vibrationEnabled = false,
            updatedAt = Clock.System.now()
        )

        preferencesRepository.setPreferences(customPrefs)

        val result = service.getPreferences("user123")

        assertNotNull(result)
        assertEquals("user123", result.userId)
        assertEquals(setOf(NotificationType.MENTION, NotificationType.EVENT_INVITE), result.enabledTypes)
        assertEquals(QuietTime(23, 30), result.quietHoursStart)
        assertEquals(QuietTime(7, 0), result.quietHoursEnd)
        assertFalse(result.soundEnabled)
        assertFalse(result.vibrationEnabled)
    }

    @Test
    fun getPreferences_returnsDefaultWhenNotSet() = runTest {
        val result = service.getPreferences("user-with-no-prefs")
        // When no preferences are set, the repository returns default preferences
        assertNotNull(result)
        assertEquals("user-with-no-prefs", result?.userId)
        assertTrue(result?.enabledTypes?.contains(NotificationType.EVENT_INVITE) == true)
    }

    // ==================== 10. updatePreferences Tests ====================

    @Test
    fun updatePreferences_success_savesPreferences() = runTest {
        val prefs = NotificationPreferences(
            userId = "user123",
            enabledTypes = setOf(NotificationType.DATE_CONFIRMED),
            quietHoursStart = null,
            quietHoursEnd = null,
            soundEnabled = true,
            vibrationEnabled = true,
            updatedAt = Clock.System.now()
        )

        val result = service.updatePreferences(prefs)

        assertTrue(result.isSuccess)

        // Verify saved
        val saved = preferencesRepository.getPreferences("user123")
        assertNotNull(saved)
        assertEquals(setOf(NotificationType.DATE_CONFIRMED), saved.enabledTypes)
    }

    // ==================== 11. Filtering by Type Tests ====================

    @Test
    fun sendNotification_filtersByType_votingRelated() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        // Enable only voting-related types
        preferencesRepository.setPreferences(
            defaultNotificationPreferences("user123").copy(
                enabledTypes = setOf(
                    NotificationType.VOTE_REMINDER,
                    NotificationType.VOTE_CLOSE_REMINDER,
                    NotificationType.DATE_CONFIRMED
                )
            )
        )

        // Should succeed for enabled type
        val voteResult = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.VOTE_REMINDER,
                title = "Vote",
                body = "Please vote"
            )
        )
        assertTrue(voteResult.isSuccess)

        // Should fail for disabled type
        val mentionResult = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.MENTION,
                title = "Mention",
                body = "You were mentioned"
            )
        )
        assertFalse(mentionResult.isSuccess)
    }

    @Test
    fun sendNotification_allTypesCanBeDisabled() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        // Disable all types
        preferencesRepository.setPreferences(
            defaultNotificationPreferences("user123").copy(
                enabledTypes = emptySet()
            )
        )

        // Try sending each type (should all fail)
        NotificationType.entries.forEach { type ->
            // Skip urgent types that bypass quiet hours but still respect enabledTypes
            val result = service.sendNotification(
                NotificationRequest(
                    userId = "user123",
                    type = type,
                    title = "Test",
                    body = "Test"
                )
            )
            assertFalse(result.isSuccess, "Type $type should be disabled")
        }
    }

    // ==================== 12. Notification → NotificationMessage Conversion Tests ====================

    @Test
    fun getNotifications_convertsToNotificationMessageCorrectly() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        val result = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.EVENT_INVITE,
                title = "Event Invite",
                body = "You've been invited",
                data = mapOf("eventId" to "evt-456", "inviter" to "John")
            )
        )
        val notificationId = result.getOrThrow()

        val notifications = service.getNotifications("user123")

        assertEquals(1, notifications.size)

        val message = notifications.first()
        assertEquals(notificationId, message.id)
        assertEquals("user123", message.userId)
        assertEquals(ModelNotificationType.EVENT_UPDATE, message.type) // Mapped type
        assertEquals("Event Invite", message.title)
        assertEquals("You've been invited", message.body)
        assertEquals("evt-456", message.data["eventId"])
        assertEquals("John", message.data["inviter"])
        assertNotNull(message.sentAt)
        assertNull(message.readAt) // Not read yet
    }

    @Test
    fun getUnreadNotifications_correctlyMapsAllTypes() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        // Enable all notification types for this test
        preferencesRepository.setPreferences(
            defaultNotificationPreferences("user123").copy(
                enabledTypes = NotificationType.entries.toSet()
            )
        )

        // Send notifications of various types
        val typeMappings = listOf(
            NotificationType.EVENT_INVITE to ModelNotificationType.EVENT_UPDATE,
            NotificationType.DATE_CONFIRMED to ModelNotificationType.EVENT_CONFIRMED,
            NotificationType.MENTION to ModelNotificationType.MENTION,
            NotificationType.NEW_COMMENT to ModelNotificationType.COMMENT_POSTED,
            NotificationType.COMMENT_REPLY to ModelNotificationType.COMMENT_REPLY,
            NotificationType.VOTE_CLOSE_REMINDER to ModelNotificationType.VOTE_CLOSE_REMINDER,
            NotificationType.DEADLINE_REMINDER to ModelNotificationType.DEADLINE_REMINDER
        )

        typeMappings.forEach { (internalType, expectedModelType) ->
            // Clear previous notifications
            service.getNotifications("user123").forEach {
                service.deleteNotification(it.id)
            }

            service.sendNotification(
                NotificationRequest(
                    userId = "user123",
                    type = internalType,
                    title = "Test",
                    body = "Test"
                )
            ).getOrThrow()

            val notifications = service.getUnreadNotifications("user123")
            assertEquals(1, notifications.size)
            assertEquals(expectedModelType, notifications.first().type)
        }
    }

    @Test
    fun notificationMessage_hasCorrectReadStatus() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token").getOrThrow()

        val id = service.sendNotification(createRequest("Test")).getOrThrow()

        // Initially unread
        val unread = service.getUnreadNotifications("user123")
        assertEquals(1, unread.size)
        assertNull(unread.first().readAt)

        // Mark as read
        service.markAsRead(id).getOrThrow()

        // Now should have readAt
        val read = service.getNotifications("user123")
        assertEquals(1, read.size)
        assertNotNull(read.first().readAt)
    }

    // ==================== Helper Methods ====================

    private fun createRequest(body: String): NotificationRequest {
        return NotificationRequest(
            userId = "user123",
            type = NotificationType.EVENT_INVITE,
            title = "Test Title",
            body = body
        )
    }

    private fun seedUser(userId: String) {
        val now = "2026-01-01T00:00:00Z"
        database.userQueries.insertUser(
            id = userId,
            provider_id = "provider-$userId",
            email = "$userId@example.com",
            name = "User $userId",
            avatar_url = null,
            provider = "google",
            role = "USER",
            created_at = now,
            updated_at = now
        )
    }
}

// ==================== Mock Implementations ====================

private class InMemoryNotificationPreferencesRepository : NotificationPreferencesRepositoryInterface {
    private val preferences = mutableMapOf<String, NotificationPreferences>()
    private var returnNullForUnknownUser = false

    override suspend fun getPreferences(userId: String): NotificationPreferences? {
        return if (returnNullForUnknownUser) {
            preferences[userId]
        } else {
            preferences[userId] ?: defaultNotificationPreferences(userId)
        }
    }

    fun setReturnNullForUnknownUser(value: Boolean) {
        returnNullForUnknownUser = value
    }

    override suspend fun savePreferences(prefs: NotificationPreferences): Result<Unit> {
        preferences[prefs.userId] = prefs
        return Result.success(Unit)
    }

    override suspend fun deletePreferences(userId: String): Result<Unit> {
        preferences.remove(userId)
        return Result.success(Unit)
    }

    fun setPreferences(prefs: NotificationPreferences) {
        preferences[prefs.userId] = prefs
    }
}

private class RecordingFCMSender : FCMSender {
    val sentTokens = mutableListOf<String>()
    var callCount = 0
    var shouldFail = false
    var failureMessage = "FCM Error"

    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> {
        callCount++
        return if (shouldFail) {
            Result.failure(RuntimeException(failureMessage))
        } else {
            sentTokens += token
            Result.success(Unit)
        }
    }
}

private class RecordingAPNsSender : APNsSender {
    val sentTokens = mutableListOf<String>()
    var callCount = 0
    var shouldFail = false
    var failureMessage = "APNs Error"

    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> {
        callCount++
        return if (shouldFail) {
            Result.failure(RuntimeException(failureMessage))
        } else {
            sentTokens += token
            Result.success(Unit)
        }
    }
}
