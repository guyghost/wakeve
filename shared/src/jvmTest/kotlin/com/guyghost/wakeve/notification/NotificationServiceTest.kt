package com.guyghost.wakeve.notification

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakevDb
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NotificationServiceTest {

    private lateinit var database: WakevDb
    private lateinit var preferencesRepository: NotificationPreferencesRepositoryInterface
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

    @Test
    fun registerPushTokenStoresToken() = runTest {
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
    }

    @Test
    fun sendNotificationPersistsMessageWhenAllowed() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token-123").getOrThrow()

        val result = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.EVENT_INVITE,
                title = "Invitation",
                body = "Vous etes invite"
            )
        )

        assertTrue(result.isSuccess)
        val notificationId = result.getOrThrow()
        assertTrue(fcmSender.sentTokens.contains("fcm-token-123"))

        val unread = service.getUnreadNotifications("user123")
        assertEquals(1, unread.size)
        assertEquals(notificationId, unread.first().id)
    }

    @Test
    fun sendNotificationFailsWhenTypeIsDisabled() = runTest {
        preferencesRepository = InMemoryNotificationPreferencesRepository(
            preferences = defaultNotificationPreferences("user123").copy(enabledTypes = emptySet())
        )
        service = NotificationService(database, preferencesRepository, fcmSender, apnsSender)

        service.registerPushToken("user123", Platform.ANDROID, "fcm-token-123").getOrThrow()

        val result = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.EVENT_INVITE,
                title = "Invitation",
                body = "Body"
            )
        )

        assertFalse(result.isSuccess)
    }

    @Test
    fun markAsReadRemovesNotificationFromUnreadList() = runTest {
        service.registerPushToken("user123", Platform.ANDROID, "fcm-token-123").getOrThrow()

        val notificationId = service.sendNotification(
            NotificationRequest(
                userId = "user123",
                type = NotificationType.EVENT_INVITE,
                title = "Invitation",
                body = "Body"
            )
        ).getOrThrow()

        val markResult = service.markAsRead(notificationId)

        assertTrue(markResult.isSuccess)
        assertTrue(service.getUnreadNotifications("user123").isEmpty())
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

private class InMemoryNotificationPreferencesRepository(
    private val preferences: NotificationPreferences? = null
) : NotificationPreferencesRepositoryInterface {
    override suspend fun getPreferences(userId: String): NotificationPreferences? {
        return preferences ?: defaultNotificationPreferences(userId)
    }

    override suspend fun savePreferences(preferences: NotificationPreferences): Result<Unit> = Result.success(Unit)

    override suspend fun deletePreferences(userId: String): Result<Unit> = Result.success(Unit)
}

private class RecordingFCMSender : FCMSender {
    val sentTokens = mutableListOf<String>()

    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> {
        sentTokens += token
        return Result.success(Unit)
    }
}

private class RecordingAPNsSender : APNsSender {
    val sentTokens = mutableListOf<String>()

    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> {
        sentTokens += token
        return Result.success(Unit)
    }
}
