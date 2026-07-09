package com.guyghost.wakeve.notification

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** RED contracts for OpenSpec harden-apns-production-delivery task 2.2. */
class APNsProductionDeliveryRedTest {
    private lateinit var database: WakeveDb
    private lateinit var apnsSender: FailingAPNsSender
    private lateinit var service: NotificationService

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
        database.userQueries.insertUser(
            id = "recipient-user",
            provider_id = "provider-recipient-user",
            email = "recipient@example.test",
            name = "Recipient",
            avatar_url = null,
            provider = "google",
            role = "USER",
            created_at = "2026-01-01T00:00:00Z",
            updated_at = "2026-01-01T00:00:00Z"
        )
        apnsSender = FailingAPNsSender()
        service = NotificationService(
            database = database,
            preferencesRepository = AlwaysEnabledPreferencesRepository,
            fcmSender = SuccessfulFCMSender,
            apnsSender = apnsSender
        )
    }

    @Test
    fun providerFailureIsNotAbsorbedAsSuccessfulAcceptance() = runTest {
        service.registerPushToken("recipient-user", Platform.IOS, "raw-device-token").getOrThrow()

        val result = service.sendNotification(invitation())

        assertTrue(result.isFailure, "A provider error must remain observable; history persistence is not provider success")
        assertEquals(1, apnsSender.calls)
    }

    @Test
    fun sentAtRemainsNullUntilProviderReturnsHttp200Acceptance() = runTest {
        service.registerPushToken("recipient-user", Platform.IOS, "raw-device-token").getOrThrow()

        val result = service.sendNotification(invitation())
        val persisted = database.notificationQueries.getNotifications("recipient-user", 10).executeAsList().single()

        assertTrue(result.isFailure)
        assertNull(persisted.sent_at, "sent_at must not be populated before a proven provider acceptance")
    }

    @Test
    fun registeringTwoIosInstallationsDoesNotOverwriteTheFirstDevice() = runTest {
        service.registerPushToken("recipient-user", Platform.IOS, "iphone-token").getOrThrow()
        service.registerPushToken("recipient-user", Platform.IOS, "ipad-token").getOrThrow()

        val tokens = database.notificationQueries.getTokensByUser("recipient-user").executeAsList()

        assertEquals(2, tokens.size, "two installations owned by one user must remain independently addressable")
        assertEquals(setOf("iphone-token", "ipad-token"), tokens.map { it.token }.toSet())
    }

    private fun invitation() = NotificationRequest(
        userId = "recipient-user",
        type = NotificationType.EVENT_INVITE,
        title = "Invitation",
        body = "Join the event"
    )

    @Test
    fun localClientDatabaseDoesNotOwnBackendRecipientOrProviderDeliveryTables() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WakeveDb.Schema.create(driver)
        val backendTables = driver.executeQuery(
            identifier = null,
            sql = "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('notification_recipient','notification_delivery')",
            mapper = { cursor ->
                val names = mutableListOf<String>()
                while (cursor.next().value) names += cursor.getString(0).orEmpty()
                app.cash.sqldelight.db.QueryResult.Value(names)
            },
            parameters = 0
        ).value

        assertTrue(backendTables.isEmpty(), "provider recipients and deliveries belong exclusively to the backend datastore")
    }
}

private class FailingAPNsSender : APNsSender {
    var calls = 0

    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> {
        calls += 1
        return Result.failure(IllegalStateException("provider unavailable"))
    }
}

private object SuccessfulFCMSender : FCMSender {
    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> = Result.success(Unit)
}

private object AlwaysEnabledPreferencesRepository : NotificationPreferencesRepositoryInterface {
    override suspend fun getPreferences(userId: String): NotificationPreferences =
        defaultNotificationPreferences(userId).copy(quietHoursStart = null, quietHoursEnd = null)

    override suspend fun savePreferences(preferences: NotificationPreferences): Result<Unit> = Result.success(Unit)

    override suspend fun deletePreferences(userId: String): Result<Unit> = Result.success(Unit)
}
