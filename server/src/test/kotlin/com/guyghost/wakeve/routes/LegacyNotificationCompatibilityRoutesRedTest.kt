package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.module
import com.guyghost.wakeve.notification.APNsEnvironment
import com.guyghost.wakeve.notification.BackendDeviceRegistrationRequest
import com.guyghost.wakeve.notification.DeviceRegistrationScope
import com.guyghost.wakeve.notification.DeviceRegistrationStatus
import com.guyghost.wakeve.notification.DeviceRegistrationStoreConfiguration
import com.guyghost.wakeve.notification.LegacyCompatibilityReconciliationStatus
import com.guyghost.wakeve.notification.LegacyCompatibilityResponseDisposition
import com.guyghost.wakeve.notification.LegacyCompatibilitySagaState
import com.guyghost.wakeve.notification.LegacyNotificationRegistrationCompatibilityWorker
import com.guyghost.wakeve.notification.LegacyNotificationTokenBackfill
import com.guyghost.wakeve.notification.NoConfiguredAPNsSender
import com.guyghost.wakeve.notification.NoConfiguredFCMSender
import com.guyghost.wakeve.notification.NotificationPreferencesRepository
import com.guyghost.wakeve.notification.NotificationService
import com.guyghost.wakeve.notification.Platform
import com.guyghost.wakeve.notification.SqliteBackendDeviceRegistrationStoreFactory
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DriverManager
import kotlin.io.path.deleteIfExists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** HTTP acceptance contracts for the temporary N/N-1 compatibility window. */
class LegacyNotificationCompatibilityRoutesRedTest {
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "wakev-api"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "wakev-client"
    private val json = Json { ignoreUnknownKeys = true }
    private val temporaryRoots = mutableListOf<Path>()

    @AfterTest
    fun tearDown() {
        DatabaseProvider.resetDatabase()
        temporaryRoots.forEach { root ->
            Files.walk(root).use { paths ->
                paths.sorted(Comparator.reverseOrder()).forEach { path -> path.deleteIfExists() }
            }
        }
        temporaryRoots.clear()
    }

    @Test
    fun `legacy success true is returned only after both stores and convergence are durable`() = testApplication {
        val fixture = routeFixture()
        val phone = fixture.registerV2("owner", "phone", "phone-token")
        val pad = fixture.registerV2("owner", "pad", "pad-token")
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(fixture.database, deviceRegistrationStoreFactory = fixture.registrationFactory) }

        val rawToken = "legacy-secret-token"
        val response = client.post("/api/notifications/register") {
            header(HttpHeaders.Authorization, "Bearer ${jwt("owner")}")
            contentType(ContentType.Application.Json)
            setBody("""{"platform":"ios","token":"$rawToken"}""")
        }
        val bodyText = response.bodyAsText()
        val body = json.parseToJsonElement(bodyText).jsonObject

        assertEquals(HttpStatusCode.OK, response.status, bodyText)
        assertEquals(true, body["success"]?.jsonPrimitive?.booleanOrNull)
        assertEquals("converged", body["compatibilityState"]?.jsonPrimitive?.contentOrNull)
        assertFalse(bodyText.contains(rawToken))
        val sagaId = assertNotNull(body["sagaId"]?.jsonPrimitive?.contentOrNull)
        val saga = fixture.registrationFactory.openCompatibilitySagaStore().use {
            assertNotNull(it.findBySagaId(sagaId))
        }
        assertEquals(LegacyCompatibilitySagaState.CONVERGED, saga.state)
        assertEquals(LegacyCompatibilityReconciliationStatus.CONVERGED, saga.reconciliationStatus)
        assertEquals(LegacyCompatibilityResponseDisposition.CONVERGED_SUCCESS, saga.responseDisposition)
        assertEquals(saga.legacyInstallationId, saga.targetInstallationId)
        assertNull(saga.targetRegistrationId, "Legacy register must not persist a v2 registration target.")

        fixture.registrationFactory.open().use { store ->
            assertEquals(DeviceRegistrationStatus.ACTIVE, store.registration(phone.registrationId)?.status)
            assertEquals(DeviceRegistrationStatus.ACTIVE, store.registration(pad.registrationId)?.status)
            assertEquals(3, store.activeRegistrations("owner", fixture.scope).size)
        }
    }

    @Test
    fun `legacy register returns 202 only after durable intent when legacy store is transiently unavailable`() = testApplication {
        val fixture = routeFixture()
        val phone = fixture.registerV2("owner", "phone", "phone-token")
        val other = fixture.registerV2("other", "other-phone", "other-token")
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(fixture.database, deviceRegistrationStoreFactory = fixture.registrationFactory) }

        val rawToken = "legacy-token-under-retry"
        val lock = fixture.acquireLegacyWriteLock()
        val response = try {
            client.post("/api/notifications/register") {
                header(HttpHeaders.Authorization, "Bearer ${jwt("owner")}")
                contentType(ContentType.Application.Json)
                setBody("""{"platform":"ios","token":"$rawToken"}""")
            }
        } finally {
            lock.rollbackAndClose()
        }
        val bodyText = response.bodyAsText()
        val body = json.parseToJsonElement(bodyText).jsonObject

        assertEquals(HttpStatusCode.Accepted, response.status, bodyText)
        assertEquals(true, body["accepted"]?.jsonPrimitive?.booleanOrNull)
        assertTrue(body["success"] == null || body["success"]?.jsonPrimitive?.booleanOrNull == false)
        assertFalse(bodyText.contains(rawToken))
        val sagaId = assertNotNull(body["sagaId"]?.jsonPrimitive?.contentOrNull)
        assertFalse(sagaId.contains(rawToken))
        val saga = fixture.registrationFactory.openCompatibilitySagaStore().use {
            assertNotNull(it.findBySagaId(sagaId))
        }
        assertEquals(LegacyCompatibilityReconciliationStatus.PENDING, saga.reconciliationStatus)
        assertEquals(LegacyCompatibilityResponseDisposition.RECONCILIATION_ACCEPTED, saga.responseDisposition)
        assertEquals(saga.legacyInstallationId, saga.targetInstallationId)
        assertNull(saga.targetRegistrationId, "A pending legacy register must not persist a v2 target.")
        assertFalse(saga.toString().contains(rawToken))

        fixture.registrationFactory.open().use { store ->
            assertEquals(DeviceRegistrationStatus.ACTIVE, store.registration(phone.registrationId)?.status)
            assertEquals(DeviceRegistrationStatus.ACTIVE, store.registration(other.registrationId)?.status)
            assertEquals(1, store.activeRegistrations("owner", fixture.scope).size)
            assertEquals(1, store.activeRegistrations("other", fixture.scope).size)
        }
        assertNull(fixture.database.notificationQueries.getToken("owner", Platform.IOS.name).executeAsOneOrNull())

        val scheduled = fixture.compatibilityWorker().resume(sagaId)
        assertEquals(LegacyCompatibilitySagaState.RETRY_WAIT, scheduled.state)
        val retryDeadline = assertNotNull(scheduled.nextRetryAtEpochSeconds)
        val recovered = fixture.compatibilityWorker().resume(sagaId, nowEpochSeconds = retryDeadline)
        assertEquals(
            LegacyCompatibilitySagaState.CONVERGED,
            recovered.state,
            "A reopened recovery worker must resume the durable 202 intent without another HTTP request."
        )
    }

    @Test
    fun `legacy unregister 202 exposes only a durable accepted reconciliation`() = testApplication {
        val fixture = routeFixture()
        val phone = fixture.registerV2("owner", "phone", "phone-token")
        val pad = fixture.registerV2("owner", "pad", "pad-token")
        val otherLegacy = fixture.backfillLegacy("other", "legacy-ios-other", "other-legacy-token")
        fixture.backfillLegacy("owner", "legacy-ios-owner", "owner-legacy-token")
        fixture.database.notificationQueries.upsertToken(
            user_id = "owner",
            platform = Platform.IOS.name,
            token = "owner-legacy-token",
            updated_at = 1
        )
        fixture.database.notificationQueries.upsertToken(
            user_id = "other",
            platform = Platform.IOS.name,
            token = "other-legacy-token",
            updated_at = 1
        )
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(fixture.database, deviceRegistrationStoreFactory = fixture.registrationFactory) }

        val lock = fixture.acquireLegacyWriteLock()
        val response = try {
            client.delete("/api/notifications/unregister?platform=ios") {
                header(HttpHeaders.Authorization, "Bearer ${jwt("owner")}")
            }
        } finally {
            lock.rollbackAndClose()
        }
        val bodyText = response.bodyAsText()
        val body = json.parseToJsonElement(bodyText).jsonObject

        assertEquals(HttpStatusCode.Accepted, response.status, bodyText)
        assertEquals(true, body["accepted"]?.jsonPrimitive?.booleanOrNull)
        assertTrue(body["success"] == null || body["success"]?.jsonPrimitive?.booleanOrNull == false)
        val sagaId = assertNotNull(body["sagaId"]?.jsonPrimitive?.contentOrNull)
        assertFalse(sagaId.contains("owner-legacy-token"))
        val saga = fixture.registrationFactory.openCompatibilitySagaStore().use {
            assertNotNull(it.findBySagaId(sagaId))
        }
        assertEquals(LegacyCompatibilityReconciliationStatus.PENDING, saga.reconciliationStatus)
        assertEquals(LegacyCompatibilityResponseDisposition.RECONCILIATION_ACCEPTED, saga.responseDisposition)
        assertEquals(saga.legacyInstallationId, saga.targetInstallationId)
        assertNull(saga.targetRegistrationId, "Legacy unregister targeting is by legacy installation only.")
        assertFalse(saga.toString().contains("owner-legacy-token"))

        fixture.registrationFactory.open().use { store ->
            assertEquals(DeviceRegistrationStatus.ACTIVE, store.registration(otherLegacy.registration.registrationId)?.status)
            assertEquals(DeviceRegistrationStatus.ACTIVE, store.registration(phone.registrationId)?.status)
            assertEquals(DeviceRegistrationStatus.ACTIVE, store.registration(pad.registrationId)?.status)
        }
        assertNotNull(
            fixture.database.notificationQueries.getToken("owner", Platform.IOS.name).executeAsOneOrNull(),
            "The unavailable legacy step remains for forward reconciliation."
        )
        assertNotNull(fixture.database.notificationQueries.getToken("other", Platform.IOS.name).executeAsOneOrNull())
    }

    private fun routeFixture(): RouteFixture {
        val root = Files.createTempDirectory("wakeve-legacy-compat-routes-")
        temporaryRoots.add(root)
        val legacyDatabasePath = root.resolve("legacy.sqlite")
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(legacyDatabasePath.toString()))
        val configuration = DeviceRegistrationStoreConfiguration.resolve(
            systemProperties = mapOf(
                "wakeve.notification.device-registration.db.path" to root.resolve("registration.sqlite").toString(),
                "wakeve.notification.device-registration.legacy-identity-hmac-key" to "h".repeat(32),
                "wakeve.notification.device-registration.token-encryption-key" to "e".repeat(32)
            ),
            environment = emptyMap()
        ).getOrThrow()
        return RouteFixture(
            database = database,
            legacyDatabasePath = legacyDatabasePath,
            registrationFactory = SqliteBackendDeviceRegistrationStoreFactory(configuration),
            scope = DeviceRegistrationScope.create(
                APNsEnvironment.PRODUCTION,
                "com.guyghost.wakeve"
            ).getOrThrow()
        )
    }

    private fun jwt(userId: String): String = JWT.create()
        .withIssuer(jwtIssuer)
        .withAudience(jwtAudience)
        .withClaim("userId", userId)
        .withClaim("sessionId", "session-$userId")
        .withClaim("permissions", listOf("READ", "WRITE"))
        .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
        .sign(Algorithm.HMAC256(jwtSecret))

    private class RouteFixture(
        val database: WakeveDb,
        val legacyDatabasePath: Path,
        val registrationFactory: SqliteBackendDeviceRegistrationStoreFactory,
        val scope: DeviceRegistrationScope
    ) {
        fun registerV2(userId: String, installationId: String, token: String) = runBlocking {
            registrationFactory.open().use { store ->
                store.register(
                    BackendDeviceRegistrationRequest.create(
                        installationId = installationId,
                        authenticatedUserId = userId,
                        platform = Platform.IOS,
                        scope = scope,
                        rawToken = token,
                        registeredAtEpochSeconds = 100
                    ).getOrThrow()
                )
            }
        }

        fun backfillLegacy(userId: String, legacyRowKey: String, token: String) = runBlocking {
            registrationFactory.open().use { store ->
                store.backfillLegacy(
                    LegacyNotificationTokenBackfill.create(
                        legacyRowKey = legacyRowKey,
                        userId = userId,
                        platform = Platform.IOS,
                        rawToken = token,
                        scope = scope,
                        updatedAtEpochSeconds = 100
                    ).getOrThrow()
                )
            }
        }

        fun acquireLegacyWriteLock(): Connection = DriverManager
            .getConnection("jdbc:sqlite:$legacyDatabasePath")
            .also { connection ->
                connection.createStatement().use { statement ->
                    statement.execute("PRAGMA busy_timeout = 0")
                    statement.execute("BEGIN IMMEDIATE")
                }
            }

        fun compatibilityWorker(): LegacyNotificationRegistrationCompatibilityWorker =
            LegacyNotificationRegistrationCompatibilityWorker(
                notificationService = NotificationService(
                    database = database,
                    preferencesRepository = NotificationPreferencesRepository(database),
                    fcmSender = NoConfiguredFCMSender,
                    apnsSender = NoConfiguredAPNsSender
                ),
                registrationStoreFactory = registrationFactory
            )
    }
}

private fun Connection.rollbackAndClose() {
    runCatching { createStatement().use { it.execute("ROLLBACK") } }
    close()
}
