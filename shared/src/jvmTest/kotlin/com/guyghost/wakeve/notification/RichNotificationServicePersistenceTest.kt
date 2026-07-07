package com.guyghost.wakeve.notification

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.NotificationType as ModelNotificationType
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class RichNotificationServicePersistenceTest {

    private lateinit var database: WakeveDb
    private lateinit var preferencesRepository: NullRichNotificationPreferencesRepository
    private lateinit var fcmSender: RecordingRichFCMSender
    private lateinit var apnsSender: RecordingRichAPNsSender
    private lateinit var service: RichNotificationService

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
        preferencesRepository = NullRichNotificationPreferencesRepository()
        fcmSender = RecordingRichFCMSender()
        apnsSender = RecordingRichAPNsSender()
        service = RichNotificationService(database, preferencesRepository, fcmSender, apnsSender)

        seedUser("user123")
    }

    @Test
    fun sendRichNotification_persistsHistoryWhenNoTokensRegistered() = runTest {
        val result = service.sendRichNotification(createNotification(userId = "user123"))

        assertTrue(result.isSuccess)
        val notificationId = result.getOrThrow()
        assertEquals(0, fcmSender.callCount)
        assertEquals(0, apnsSender.callCount)

        val persisted = database.notificationQueries
            .getNotificationById(notificationId)
            .executeAsOneOrNull()

        assertNotNull(persisted)
        assertEquals("user123", persisted.user_id)
        assertEquals("Rich title", persisted.title)
    }

    @Test
    fun sendRichNotification_persistsHistoryWhenAllPushTransportsFail() = runTest {
        database.notificationQueries.upsertToken("user123", Platform.ANDROID.name, "fcm-token", 1L)
        database.notificationQueries.upsertToken("user123", Platform.IOS.name, "apns-token", 1L)
        fcmSender.shouldFail = true
        apnsSender.shouldFail = true

        val result = service.sendRichNotification(createNotification(userId = "user123"))

        assertTrue(result.isSuccess)
        val notificationId = result.getOrThrow()
        assertEquals(1, fcmSender.callCount)
        assertEquals(1, apnsSender.callCount)
        assertTrue(fcmSender.sentTokens.isEmpty())
        assertTrue(apnsSender.sentTokens.isEmpty())
        assertNotNull(database.notificationQueries.getNotificationById(notificationId).executeAsOneOrNull())
    }

    @Test
    fun sendRichNotification_attemptsRemainingTransportsWhenOnePushTransportFails() = runTest {
        database.notificationQueries.upsertToken("user123", Platform.ANDROID.name, "fcm-token", 1L)
        database.notificationQueries.upsertToken("user123", Platform.IOS.name, "apns-token", 1L)
        fcmSender.shouldFail = true

        val result = service.sendRichNotification(createNotification(userId = "user123"))

        assertTrue(result.isSuccess)
        val notificationId = result.getOrThrow()
        assertEquals(1, fcmSender.callCount)
        assertEquals(1, apnsSender.callCount)
        assertTrue(fcmSender.sentTokens.isEmpty())
        assertEquals(listOf("apns-token"), apnsSender.sentTokens)
        assertNotNull(database.notificationQueries.getNotificationById(notificationId).executeAsOneOrNull())
    }

    @Test
    fun sendRichNotification_generatesIdWhenNotificationIdIsBlank() = runTest {
        val result = service.sendRichNotification(
            createNotification(id = "  ", userId = "user123")
        )

        assertTrue(result.isSuccess)
        val notificationId = result.getOrThrow()
        assertTrue(notificationId.isNotBlank())
        assertNotNull(database.notificationQueries.getNotificationById(notificationId).executeAsOneOrNull())
    }

    @Test
    fun sendRichNotification_normalizesIdUserTitleAndBodyBeforePersistingAndSending() = runTest {
        database.notificationQueries.upsertToken("user123", Platform.ANDROID.name, "fcm-token", 1L)

        val result = service.sendRichNotification(
            createNotification(
                id = " rich-id ",
                userId = " user123 ",
                title = "  Rich title  ",
                body = "  Rich body  "
            )
        )

        assertTrue(result.isSuccess)
        assertEquals("rich-id", result.getOrThrow())
        assertEquals(1, fcmSender.callCount)
        assertEquals(listOf("fcm-token"), fcmSender.sentTokens)
        assertEquals("rich-id", fcmSender.sentNotifications.single().id)
        assertEquals("user123", fcmSender.sentNotifications.single().userId)
        assertEquals("Rich title", fcmSender.sentNotifications.single().title)
        assertEquals("Rich body", fcmSender.sentNotifications.single().body)

        val persisted = database.notificationQueries.getNotificationById("rich-id").executeAsOneOrNull()
        assertNotNull(persisted)
        assertEquals("user123", persisted.user_id)
        assertEquals("Rich title", persisted.title)
        assertEquals("Rich body", persisted.body)
    }

    @Test
    fun normalizeRichNotificationForSend_rejectsBlankFallbackId() {
        val result = normalizeRichNotificationForSend(
            notification = createNotification(id = " ", userId = "user123"),
            fallbackId = " "
        )

        assertTrue(result.isFailure)
    }

    @Test
    fun sendRichNotification_rejectsDisabledCategoryFromPreferences() = runTest {
        preferencesRepository.setPreferences(
            defaultNotificationPreferences("user123").copy(
                enabledTypes = setOf(NotificationType.MENTION),
                quietHoursStart = null,
                quietHoursEnd = null
            )
        )

        val result = service.sendRichNotification(
            createNotification(userId = "user123", category = NotificationCategory.EVENT_INVITE)
        )

        assertTrue(result.isFailure)
        assertEquals(0, fcmSender.callCount)
        assertEquals(0, apnsSender.callCount)
        assertTrue(database.notificationQueries.getNotifications("user123", 10).executeAsList().isEmpty())
    }

    @Test
    fun sendRichNotification_respectsQuietHoursForNonUrgentHighPriorityCategories() = runTest {
        preferencesRepository.setPreferences(
            defaultNotificationPreferences("user123").copy(
                enabledTypes = setOf(NotificationType.EVENT_INVITE),
                quietHoursStart = QuietTime(22, 0),
                quietHoursEnd = QuietTime(8, 0)
            )
        )

        val result = service.sendRichNotification(
            createNotification(
                userId = "user123",
                category = NotificationCategory.EVENT_INVITE,
                priority = RichNotificationPriority.HIGH
            ),
            currentTime = Instant.parse("2026-01-01T23:00:00Z")
        )

        assertTrue(result.isFailure)
        assertTrue(database.notificationQueries.getNotifications("user123", 10).executeAsList().isEmpty())
    }

    @Test
    fun sendRichNotification_allowsUrgentMeetingReminderDuringQuietHours() = runTest {
        preferencesRepository.setPreferences(
            defaultNotificationPreferences("user123").copy(
                enabledTypes = setOf(NotificationType.MEETING_REMINDER),
                quietHoursStart = QuietTime(22, 0),
                quietHoursEnd = QuietTime(8, 0)
            )
        )

        val result = service.sendRichNotification(
            createNotification(
                userId = "user123",
                category = NotificationCategory.MEETING_STARTING,
                priority = RichNotificationPriority.HIGH
            ),
            currentTime = Instant.parse("2026-01-01T23:00:00Z")
        )

        assertTrue(result.isSuccess)
        assertNotNull(database.notificationQueries.getNotificationById(result.getOrThrow()).executeAsOneOrNull())
    }

    @Test
    fun notificationCategoryToNotificationType_mapsRichCategoriesToPreferenceTypes() {
        assertEquals(NotificationType.EVENT_INVITE, NotificationCategory.EVENT_INVITE.toNotificationType())
        assertEquals(NotificationType.VOTE_REMINDER, NotificationCategory.POLL_REMINDER.toNotificationType())
        assertEquals(NotificationType.MEETING_REMINDER, NotificationCategory.MEETING_STARTING.toNotificationType())
        assertEquals(NotificationType.NEW_SCENARIO, NotificationCategory.SCENARIO_VOTE.toNotificationType())
        assertEquals(NotificationType.EVENT_UPDATE, NotificationCategory.GENERAL.toNotificationType())
    }

    @Test
    fun sendRichNotification_persistsCanonicalTypeForStandardInboxReaders() = runTest {
        val notificationId = service.sendRichNotification(
            createNotification(
                id = "poll-rich",
                userId = "user123",
                category = NotificationCategory.POLL_REMINDER
            )
        ).getOrThrow()

        val persisted = database.notificationQueries.getNotificationById(notificationId).executeAsOneOrNull()
        assertNotNull(persisted)
        assertEquals(NotificationType.VOTE_REMINDER.name, persisted.type)

        val richNotification = service.getRichNotifications("user123").single()
        assertEquals(NotificationCategory.POLL_REMINDER, richNotification.category)

        val standardService = NotificationService(
            database = database,
            preferencesRepository = preferencesRepository,
            fcmSender = NoConfiguredFCMSender,
            apnsSender = NoConfiguredAPNsSender
        )
        val standardNotification = standardService.getNotifications("user123").single()
        assertEquals(ModelNotificationType.VOTE_CLOSE_REMINDER, standardNotification.type)
    }

    @Test
    fun markAsReadForUser_rejectsNotificationOwnedByAnotherUser() = runTest {
        seedUser("user456")
        val notificationId = service.sendRichNotification(
            createNotification(userId = "user456", id = "rich-user456")
        ).getOrThrow()

        val result = service.markAsReadForUser(
            notificationId = notificationId,
            userId = "user123"
        )

        assertTrue(result.isFailure)
        val persisted = database.notificationQueries.getNotificationById(notificationId).executeAsOneOrNull()
        assertNotNull(persisted)
        assertEquals(0, persisted.is_read)
    }

    @Test
    fun markAsReadForUser_normalizesUserIdBeforeOwnershipCheck() = runTest {
        val notificationId = service.sendRichNotification(createNotification(userId = "user123")).getOrThrow()

        val result = service.markAsReadForUser(
            notificationId = " $notificationId ",
            userId = " user123 "
        )

        assertTrue(result.isSuccess)
        val persisted = database.notificationQueries.getNotificationById(notificationId).executeAsOneOrNull()
        assertNotNull(persisted)
        assertEquals(1, persisted.is_read)
        assertNotNull(persisted.read_at)
    }

    @Test
    fun markAsReadForUser_rejectsBlankNotificationId() = runTest {
        val notificationId = service.sendRichNotification(createNotification(userId = "user123")).getOrThrow()

        val result = service.markAsReadForUser(
            notificationId = "  ",
            userId = "user123"
        )

        assertTrue(result.isFailure)
        val persisted = database.notificationQueries.getNotificationById(notificationId).executeAsOneOrNull()
        assertNotNull(persisted)
        assertEquals(0, persisted.is_read)
    }

    @Test
    fun deleteNotificationForUser_rejectsNotificationOwnedByAnotherUser() = runTest {
        seedUser("user456")
        val notificationId = service.sendRichNotification(
            createNotification(userId = "user456", id = "rich-user456")
        ).getOrThrow()

        val result = service.deleteNotificationForUser(
            notificationId = notificationId,
            userId = "user123"
        )

        assertTrue(result.isFailure)
        assertNotNull(database.notificationQueries.getNotificationById(notificationId).executeAsOneOrNull())
    }

    @Test
    fun deleteNotificationForUser_normalizesUserIdBeforeOwnershipCheck() = runTest {
        val notificationId = service.sendRichNotification(createNotification(userId = "user123")).getOrThrow()

        val result = service.deleteNotificationForUser(
            notificationId = " $notificationId ",
            userId = " user123 "
        )

        assertTrue(result.isSuccess)
        assertEquals(null, database.notificationQueries.getNotificationById(notificationId).executeAsOneOrNull())
    }

    @Test
    fun deleteNotificationForUser_rejectsBlankNotificationId() = runTest {
        val notificationId = service.sendRichNotification(createNotification(userId = "user123")).getOrThrow()

        val result = service.deleteNotificationForUser(
            notificationId = "  ",
            userId = "user123"
        )

        assertTrue(result.isFailure)
        assertNotNull(database.notificationQueries.getNotificationById(notificationId).executeAsOneOrNull())
    }

    @Test
    fun getRichNotifications_normalizesUserIdAndClampsLimit() = runTest {
        repeat(3) { index ->
            service.sendRichNotification(createNotification(id = "rich-$index", userId = "user123")).getOrThrow()
        }

        val result = service.getRichNotifications(" user123 ", limit = 0)

        assertEquals(1, result.size)
        assertEquals("user123", result.single().userId)
    }

    @Test
    fun getRichNotifications_keepsNotificationWhenStoredDataIsCorrupt() = runTest {
        database.notificationQueries.insertNotification(
            id = "corrupt-rich",
            user_id = "user123",
            type = NotificationCategory.EVENT_INVITE.name,
            title = "Corrupt extras",
            body = "Still show this",
            data_ = "{not-json",
            created_at = 1_000L,
            sent_at = 1_000L
        )

        val result = service.getRichNotifications("user123")

        assertEquals(1, result.size)
        val notification = result.single()
        assertEquals("corrupt-rich", notification.id)
        assertEquals("Corrupt extras", notification.title)
        assertEquals(NotificationCategory.EVENT_INVITE, notification.category)
        assertEquals(RichNotificationPriority.DEFAULT, notification.priority)
        assertTrue(notification.actions.isEmpty())
    }

    @Test
    fun getRichNotifications_keepsNotificationWhenStoredDataUsesLegacyPayloadShape() = runTest {
        database.notificationQueries.insertNotification(
            id = "legacy-rich",
            user_id = "user123",
            type = "poll_reminder",
            title = "Legacy payload",
            body = "Payload contains non-rich keys",
            data_ = """{"eventId":"event-123","notificationId":"legacy-rich","deepLink":"wakeve://event/event-123/poll"}""",
            created_at = 1_000L,
            sent_at = 1_000L
        )

        val result = service.getRichNotifications("user123")

        assertEquals(1, result.size)
        val notification = result.single()
        assertEquals("legacy-rich", notification.id)
        assertEquals(NotificationCategory.POLL_REMINDER, notification.category)
        assertEquals("wakeve://event/event-123/poll", notification.deepLink)
    }

    private fun createNotification(
        id: String = "rich-notification",
        userId: String,
        title: String = "Rich title",
        body: String = "Rich body",
        category: NotificationCategory = NotificationCategory.GENERAL,
        priority: RichNotificationPriority = RichNotificationPriority.DEFAULT
    ): RichNotification {
        return RichNotification(
            id = id,
            userId = userId,
            title = title,
            body = body,
            category = category,
            priority = priority
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

private class NullRichNotificationPreferencesRepository : NotificationPreferencesRepositoryInterface {
    private val preferences = mutableMapOf<String, NotificationPreferences>()

    override suspend fun getPreferences(userId: String): NotificationPreferences? {
        return preferences[userId] ?: defaultNotificationPreferences(userId).copy(
            quietHoursStart = null,
            quietHoursEnd = null
        )
    }

    override suspend fun savePreferences(preferences: NotificationPreferences): Result<Unit> =
        runCatching {
            this.preferences[preferences.userId] = preferences
        }

    override suspend fun deletePreferences(userId: String): Result<Unit> =
        runCatching {
            preferences.remove(userId)
        }

    fun setPreferences(preferences: NotificationPreferences) {
        this.preferences[preferences.userId] = preferences
    }
}

private class RecordingRichFCMSender : RichFCMSender {
    val sentTokens = mutableListOf<String>()
    val sentNotifications = mutableListOf<RichNotification>()
    var callCount = 0
    var shouldFail = false

    override suspend fun sendRichNotification(
        token: String,
        notification: RichNotification
    ): Result<Unit> {
        callCount++
        return if (shouldFail) {
            Result.failure(IllegalStateException("FCM failure"))
        } else {
            sentTokens += token
            sentNotifications += notification
            Result.success(Unit)
        }
    }
}

private class RecordingRichAPNsSender : RichAPNsSender {
    val sentTokens = mutableListOf<String>()
    val sentNotifications = mutableListOf<RichNotification>()
    var callCount = 0
    var shouldFail = false

    override suspend fun sendRichNotification(
        token: String,
        notification: RichNotification
    ): Result<Unit> {
        callCount++
        return if (shouldFail) {
            Result.failure(IllegalStateException("APNs failure"))
        } else {
            sentTokens += token
            sentNotifications += notification
            Result.success(Unit)
        }
    }
}
