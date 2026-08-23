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
    fun historyIsDurableWithNullSentAtBeforeTheProviderIsInvoked() = runTest {
        service.registerPushToken("recipient-user", Platform.IOS, "raw-device-token").getOrThrow()
        var historyObservedAtInvocation = false
        var sentAtObservedAtInvocation: Long? = null
        apnsSender.onInvoked = {
            val history = database.notificationQueries
                .getNotifications("recipient-user", 10)
                .executeAsList()
            historyObservedAtInvocation = history.size == 1
            sentAtObservedAtInvocation = history.singleOrNull()?.sent_at
        }

        val result = service.sendNotification(invitation())

        assertTrue(result.isFailure)
        assertTrue(historyObservedAtInvocation, "durable history must exist before provider I/O")
        assertNull(sentAtObservedAtInvocation, "provider I/O must begin with an unaccepted history row")
    }

    @Test
    fun publicFailureRedactsTokenAndSecretAcrossCauseAndSuppressedChains() = runTest {
        val rawToken = "raw-device-token"
        val secret = "provider-signing-secret"
        service.registerPushToken("recipient-user", Platform.IOS, rawToken).getOrThrow()
        apnsSender.failureFactory = { token ->
            IllegalStateException(
                "provider failed token=$token secret=$secret",
                IllegalArgumentException("cause token=$token secret=$secret")
            ).apply {
                addSuppressed(IllegalArgumentException("suppressed token=$token secret=$secret"))
            }
        }

        val failure = assertNotNull(service.sendNotification(invitation()).exceptionOrNull())

        assertTrue(
            failure.allMessages().none { rawToken in it || secret in it },
            "public delivery failures must not retain provider secrets in any throwable link"
        )
    }

    private fun invitation() = NotificationRequest(
        userId = "recipient-user",
        type = NotificationType.EVENT_INVITE,
        title = "Invitation",
        body = "Join the event"
    )

    @Test
    fun localClientDatabaseDoesNotOwnBackendRegistrationRecipientOrProviderDeliveryTables() {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        WakeveDb.Schema.create(driver)
        val backendTables = driver.executeQuery(
            identifier = null,
            sql = "SELECT name FROM sqlite_master WHERE type='table' AND name IN ('device_installation','device_registration','notification_recipient','notification_delivery')",
            mapper = { cursor ->
                val names = mutableListOf<String>()
                while (cursor.next().value) names += cursor.getString(0).orEmpty()
                app.cash.sqldelight.db.QueryResult.Value(names)
            },
            parameters = 0
        ).value

        assertTrue(
            backendTables.isEmpty(),
            "device installations, registrations, provider recipients, and deliveries belong exclusively to the backend datastore"
        )
    }
}

private class FailingAPNsSender : APNsSender {
    var calls = 0
    var onInvoked: () -> Unit = {}
    var failureFactory: (String) -> Throwable = { IllegalStateException("provider unavailable") }

    override suspend fun sendNotification(
        token: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Result<Unit> {
        calls += 1
        onInvoked()
        return Result.failure(failureFactory(token))
    }
}

private fun Throwable.allMessages(): List<String> {
    val visited = mutableSetOf<Throwable>()
    fun collect(failure: Throwable?): List<String> = when {
        failure == null || !visited.add(failure) -> emptyList()
        else -> listOfNotNull(failure.message) +
            collect(failure.cause) +
            failure.suppressed.flatMap(::collect)
    }
    return collect(this)
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
