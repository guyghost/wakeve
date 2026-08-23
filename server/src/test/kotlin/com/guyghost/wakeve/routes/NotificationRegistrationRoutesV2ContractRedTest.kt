package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.module
import com.guyghost.wakeve.notification.APNsEnvironment
import com.guyghost.wakeve.notification.BackendDeviceRegistrationRequest
import com.guyghost.wakeve.notification.DEFAULT_COMPATIBILITY_UNIQUE_MIGRATION_ID
import com.guyghost.wakeve.notification.DEFAULT_COMPATIBILITY_UNIQUE_MIGRATION_SCHEMA_VERSION
import com.guyghost.wakeve.notification.DeviceRegistrationScope
import com.guyghost.wakeve.notification.DeviceRegistrationStatus
import com.guyghost.wakeve.notification.DeviceRegistrationStoreConfiguration
import com.guyghost.wakeve.notification.LegacyCompatibilityUniqueMigrationCheckpointPhase
import com.guyghost.wakeve.notification.LegacyCompatibilityUniqueMigrationEffectCheckpoint
import com.guyghost.wakeve.notification.LegacyCompatibilityUniqueMigrationFailure
import com.guyghost.wakeve.notification.LegacyCompatibilityUniqueMigrationFaultInjector
import com.guyghost.wakeve.notification.LegacyCompatibilityUniqueMigrationState
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
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.io.path.deleteIfExists
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import java.nio.file.Files
import java.nio.file.Path
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.sql.DriverManager
import java.sql.ResultSet
import java.util.Base64

/**
 * RED contracts for OpenSpec 6.1.  They exercise the HTTP surface and reopen the
 * production registration store on the exact configured database file; a legacy
 * SQLDelight token row must never be mistaken for an authenticated v2 association.
 */
class NotificationRegistrationRoutesV2ContractRedTest {
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
    fun `v2 register uses JWT subject and persists idempotent rotation plus two installations`() = testApplication {
        val fixture = registrationFixture()
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val client = createClient { install(ContentNegotiation) { json(json) } }

        application { module(database, deviceRegistrationStoreFactory = fixture.factory) }

        val first = register(
            client = client,
            jwtUserId = "owner",
            bodyUserId = "attacker",
            installationId = "installation-phone",
            token = "token-phone-v1"
        )
        assertEquals(HttpStatusCode.OK, first.status, first.bodyAsText())
        val firstBody = first.bodyAsText()
        assertTokenRedacted(firstBody, "token-phone-v1")
        val firstRegistrationId = firstBody.registrationId()

        val replay = register(
            client = client,
            jwtUserId = "owner",
            bodyUserId = "another-attacker",
            installationId = "installation-phone",
            token = "token-phone-v1"
        )
        val replayBody = replay.bodyAsText()
        assertTokenRedacted(replayBody, "token-phone-v1")
        assertEquals(firstRegistrationId, replayBody.registrationId(), "The same v2 registration must be idempotent.")

        val rotation = register(
            client = client,
            jwtUserId = "owner",
            bodyUserId = "attacker",
            installationId = "installation-phone",
            token = "token-phone-v2"
        )
        val rotationBody = rotation.bodyAsText()
        assertTokenRedacted(rotationBody, "token-phone-v2")
        assertEquals(firstRegistrationId, rotationBody.registrationId(), "Token rotation in the same account/scope retains registrationId.")

        val secondDevice = register(
            client = client,
            jwtUserId = "owner",
            bodyUserId = "attacker",
            installationId = "installation-pad",
            token = "token-pad-v1"
        )
        val secondDeviceBody = secondDevice.bodyAsText()
        assertTokenRedacted(secondDeviceBody, "token-pad-v1")
        val secondRegistrationId = secondDeviceBody.registrationId()
        assertNotEquals(firstRegistrationId, secondRegistrationId)

        fixture.store.use { store ->
            val phone = assertNotNull(store.activeRegistration("installation-phone"))
            val pad = assertNotNull(store.activeRegistration("installation-pad"))
            assertEquals("owner", phone.userId, "The JWT subject, never body userId, owns the association.")
            assertEquals("owner", pad.userId)
            assertEquals(firstRegistrationId, phone.registrationId)
            assertEquals(secondRegistrationId, pad.registrationId)
            assertEquals(2, store.activeRegistrations("owner", scope()).size)
        }
    }

    @Test
    fun `v2 route cannot bypass a blocked compatibility migration through the public store`() = testApplication {
        val fixture = registrationFixture()
        seedExistingV2RegistrationSentinel(fixture.databasePath)
        val blocked = fixture.factory.openCompatibilityUniqueMigration(
            migrationId = DEFAULT_COMPATIBILITY_UNIQUE_MIGRATION_ID,
            schemaVersion = DEFAULT_COMPATIBILITY_UNIQUE_MIGRATION_SCHEMA_VERSION,
            initialLogicalNowEpochSeconds = 100,
            faultInjector = LegacyCompatibilityUniqueMigrationFaultInjector { checkpoint, phase ->
                LegacyCompatibilityUniqueMigrationFailure.DDL_UNAVAILABLE.takeIf {
                    checkpoint == LegacyCompatibilityUniqueMigrationEffectCheckpoint
                        .INSTALL_UNIQUE_REQUEST_KEY_INDEX &&
                        phase == LegacyCompatibilityUniqueMigrationCheckpointPhase.BEFORE_EFFECT
                }
            }
        ).use { migration -> migration.startOrResume() }
        assertEquals(LegacyCompatibilityUniqueMigrationState.BLOCKED_MIGRATION_FAILURE, blocked.state)
        val schemaBeforeRequest = sqliteSchemaDump(fixture.databasePath)
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(fixture.legacyDatabasePath.toString()))
        database.notificationQueries.upsertToken(
            user_id = "sentinel-owner",
            platform = Platform.IOS.name,
            token = "sentinel-existing-legacy-token",
            updated_at = 1
        )
        val persistenceBeforeRequest = blockedRoutePersistenceDump(fixture)
        assertEquals(1L, persistenceBeforeRequest.registration.counts["device_installation"])
        assertEquals(1L, persistenceBeforeRequest.registration.counts["device_registration"])
        assertEquals(0L, persistenceBeforeRequest.registration.counts["legacy_notification_compatibility_saga"])
        assertEquals(1L, persistenceBeforeRequest.legacy.counts["notification_token"])
        val notificationService = NotificationService(
            database = database,
            preferencesRepository = NotificationPreferencesRepository(database),
            fcmSender = NoConfiguredFCMSender,
            apnsSender = NoConfiguredAPNsSender
        )
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application {
            install(io.ktor.server.plugins.contentnegotiation.ContentNegotiation) { json(json) }
            install(Authentication) {
                jwt("auth-jwt") {
                    verifier(
                        JWT.require(Algorithm.HMAC256(jwtSecret))
                            .withIssuer(jwtIssuer)
                            .withAudience(jwtAudience)
                            .build()
                    )
                    validate { credential -> JWTPrincipal(credential.payload) }
                }
            }
            routing {
                authenticate("auth-jwt") {
                    route("/api") {
                        notificationRoutes(
                            notificationService = notificationService,
                            deviceRegistrationStoreFactory = fixture.factory
                        )
                    }
                }
            }
        }

        val secretToken = "blocked-route-token-must-not-persist"
        val response = register(
            client = client,
            jwtUserId = "owner",
            bodyUserId = "attacker",
            installationId = "blocked-installation",
            token = secretToken
        )

        val responseBody = response.bodyAsText()
        val persistenceAfterRequest = blockedRoutePersistenceDump(fixture)
        assertEquals(
            persistenceBeforeRequest,
            persistenceAfterRequest,
            "A blocked route must preserve every existing v2 association, legacy token and compatibility saga row."
        )
        assertEquals(1L, persistenceAfterRequest.registration.counts["device_installation"])
        assertEquals(1L, persistenceAfterRequest.registration.counts["device_registration"])
        assertEquals(0L, persistenceAfterRequest.registration.counts["legacy_notification_compatibility_saga"])
        assertEquals(1L, persistenceAfterRequest.legacy.counts["notification_token"])
        assertTokenRedacted(responseBody, secretToken)
        assertTrue(
            response.status.value in 500..599,
            "A blocked migration must keep the v2 route fail-closed (status=${response.status.value})."
        )
        assertEquals(
            schemaBeforeRequest,
            sqliteSchemaDump(fixture.databasePath),
            "The v2 route must not create registration schema or data while migration is blocked."
        )
    }

    @Test
    fun `application migration composition turns its clock source into a durable correlated command`() =
        testApplication {
            val fixture = registrationFixture()
            val initial = fixture.factory.openCompatibilityUniqueMigration(
                migrationId = DEFAULT_COMPATIBILITY_UNIQUE_MIGRATION_ID,
                schemaVersion = DEFAULT_COMPATIBILITY_UNIQUE_MIGRATION_SCHEMA_VERSION,
                initialLogicalNowEpochSeconds = 100,
                faultInjector = LegacyCompatibilityUniqueMigrationFaultInjector { _, _ -> null }
            ).use { migration -> migration.startOrResume() }
            assertEquals(100, initial.logicalNowEpochSeconds)
            assertEquals(0, initial.clockRevision)
            val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))

            application {
                module(
                    database = database,
                    deviceRegistrationStoreFactory = fixture.factory,
                    compatibilityMigrationClock = { 150L }
                )
            }
            startApplication()

            val persisted = fixture.factory.openCompatibilityUniqueMigration(
                migrationId = DEFAULT_COMPATIBILITY_UNIQUE_MIGRATION_ID,
                schemaVersion = DEFAULT_COMPATIBILITY_UNIQUE_MIGRATION_SCHEMA_VERSION,
                initialLogicalNowEpochSeconds = 1,
                faultInjector = LegacyCompatibilityUniqueMigrationFaultInjector { _, _ -> null }
            ).use { migration -> migration.currentSnapshot() }
            assertEquals(150, persisted.logicalNowEpochSeconds)
            assertEquals(1, persisted.clockRevision)
        }

    @Test
    fun `v2 register rejects missing installation invalid platform scope or token before durable mutation`() = testApplication {
        val fixture = registrationFixture()
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(database, deviceRegistrationStoreFactory = fixture.factory) }

        val invalidBodies = listOf(
            """{"platform":"ios","environment":"production","topic":"com.guyghost.wakeve","token":"token"}""",
            """{"installationId":"installation-a","platform":"android","environment":"production","topic":"com.guyghost.wakeve","token":"token"}""",
            """{"installationId":"installation-a","platform":"ios","environment":"staging","topic":"com.guyghost.wakeve","token":"token"}""",
            """{"installationId":"installation-a","platform":"ios","environment":"production","topic":" ","token":"token"}""",
            """{"installationId":"installation-a","platform":"ios","environment":"production","topic":"com.guyghost.wakeve","token":" "}"""
        )

        invalidBodies.forEach { body ->
            val response = client.post("/api/notifications/register") {
                header(HttpHeaders.Authorization, "Bearer ${jwt("owner")}")
                contentType(ContentType.Application.Json)
                setBody(body)
            }
            assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        }

        fixture.store.use { store ->
            assertNull(store.activeRegistration("installation-a"))
        }
    }

    @Test
    fun `v2 delete limits removal to JWT owned registration or installation and preserves other devices`() = testApplication {
        val fixture = registrationFixture()
        val phone = fixture.register(userId = "owner", installationId = "installation-phone", token = "token-phone")
        val pad = fixture.register(userId = "owner", installationId = "installation-pad", token = "token-pad")
        val other = fixture.register(userId = "other", installationId = "installation-other", token = "token-other")
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(database, deviceRegistrationStoreFactory = fixture.factory) }

        val crossUser = client.delete("/api/notifications/registrations/${other.registrationId}") {
            header(HttpHeaders.Authorization, "Bearer ${jwt("owner")}")
        }
        assertTrue(crossUser.status == HttpStatusCode.Forbidden || crossUser.status == HttpStatusCode.NotFound, crossUser.bodyAsText())

        val removed = client.delete("/api/notifications/registrations/${phone.registrationId}") {
            header(HttpHeaders.Authorization, "Bearer ${jwt("owner")}")
        }
        assertEquals(HttpStatusCode.NoContent, removed.status, removed.bodyAsText())

        val alreadyAbsent = client.delete("/api/notifications/registrations/${phone.registrationId}") {
            header(HttpHeaders.Authorization, "Bearer ${jwt("owner")}")
        }
        assertEquals(HttpStatusCode.NoContent, alreadyAbsent.status, "Already absent is an idempotent terminal success.")

        fixture.store.use { store ->
            assertEquals(DeviceRegistrationStatus.UNREGISTERED, store.registration(phone.registrationId)?.status)
            assertEquals(DeviceRegistrationStatus.ACTIVE, store.registration(pad.registrationId)?.status)
            assertEquals(DeviceRegistrationStatus.ACTIVE, store.registration(other.registrationId)?.status)
        }
    }

    @Test
    fun `legacy register and platform delete stay bounded and cannot overwrite other v2 installations`() = testApplication {
        val fixture = registrationFixture()
        val phone = fixture.register(userId = "owner", installationId = "installation-phone", token = "token-phone")
        val pad = fixture.register(userId = "owner", installationId = "installation-pad", token = "token-pad")
        val legacy = fixture.backfillLegacy(userId = "owner", legacyRowKey = "legacy-ios-owner", token = "legacy-token")
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(database, deviceRegistrationStoreFactory = fixture.factory) }

        val legacyRegister = client.post("/api/notifications/register") {
            header(HttpHeaders.Authorization, "Bearer ${jwt("owner")}")
            contentType(ContentType.Application.Json)
            setBody("""{"platform":"ios","token":"legacy-token"}""")
        }
        assertEquals(HttpStatusCode.OK, legacyRegister.status, legacyRegister.bodyAsText())
        assertEquals(true, json.parseToJsonElement(legacyRegister.bodyAsText()).jsonObject["success"]?.jsonPrimitive?.boolean)
        assertTokenRedacted(legacyRegister.bodyAsText(), "legacy-token")

        val legacyDelete = client.delete("/api/notifications/unregister?platform=ios") {
            header(HttpHeaders.Authorization, "Bearer ${jwt("owner")}")
        }
        assertEquals(HttpStatusCode.OK, legacyDelete.status, legacyDelete.bodyAsText())

        fixture.store.use { store ->
            assertEquals(DeviceRegistrationStatus.ACTIVE, store.registration(phone.registrationId)?.status)
            assertEquals(DeviceRegistrationStatus.ACTIVE, store.registration(pad.registrationId)?.status)
            assertEquals(
                DeviceRegistrationStatus.UNREGISTERED,
                store.registration(legacy.registration.registrationId)?.status,
                "The legacy platform alias may remove only its deterministic compatibility association."
            )
            assertEquals(2, store.activeRegistrations("owner", scope()).size, "Legacy compatibility must not collapse or delete v2 devices.")
        }
    }

    @Test
    fun `v2 registration rejects missing or invalid JWT and never reflects tokens in responses`() = testApplication {
        val fixture = registrationFixture()
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val client = createClient { install(ContentNegotiation) { json(json) } }
        application { module(database, deviceRegistrationStoreFactory = fixture.factory) }
        val secretToken = "apns-token-must-never-appear-in-http-body"
        val body = """{"installationId":"installation-a","platform":"ios","environment":"production","topic":"com.guyghost.wakeve","token":"$secretToken"}"""

        val missingJwt = client.post("/api/notifications/register") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val invalidJwt = client.post("/api/notifications/register") {
            header(HttpHeaders.Authorization, "Bearer invalid.jwt.value")
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        val invalidRequest = client.post("/api/notifications/register") {
            header(HttpHeaders.Authorization, "Bearer ${jwt("owner")}")
            contentType(ContentType.Application.Json)
            setBody(body.replace("\"topic\":\"com.guyghost.wakeve\"", "\"topic\":\" \""))
        }

        listOf(missingJwt, invalidJwt).forEach { response ->
            assertEquals(HttpStatusCode.Unauthorized, response.status, response.bodyAsText())
            assertTrue(!response.bodyAsText().contains(secretToken))
        }
        assertEquals(HttpStatusCode.BadRequest, invalidRequest.status, invalidRequest.bodyAsText())
        assertTrue(!invalidRequest.bodyAsText().contains(secretToken))
        fixture.store.use { store -> assertNull(store.activeRegistration("installation-a")) }
    }

    private suspend fun register(
        client: io.ktor.client.HttpClient,
        jwtUserId: String,
        bodyUserId: String,
        installationId: String,
        token: String
    ): io.ktor.client.statement.HttpResponse = client.post("/api/notifications/register") {
        header(HttpHeaders.Authorization, "Bearer ${jwt(jwtUserId)}")
        contentType(ContentType.Application.Json)
        setBody(
            """{"userId":"$bodyUserId","installationId":"$installationId","platform":"ios","environment":"production","topic":"com.guyghost.wakeve","token":"$token"}"""
        )
    }

    private fun String.registrationId(): String = assertNotNull(
            json.parseToJsonElement(this).jsonObject["registrationId"]?.jsonPrimitive?.contentOrNull,
            "v2 registration response must return the association registrationId."
        )

    private fun assertTokenRedacted(body: String, token: String) {
        assertTrue(!body.contains(token), "HTTP response must never echo a raw APNs token.")
    }

    private fun registrationFixture(): RegistrationFixture {
        val root = Files.createTempDirectory("wakeve-registration-routes-")
        temporaryRoots.add(root)
        val configuration = DeviceRegistrationStoreConfiguration.resolve(
            systemProperties = mapOf(
                "wakeve.notification.device-registration.db.path" to root.resolve("registration.sqlite").toString(),
                "wakeve.notification.device-registration.legacy-identity-hmac-key" to "h".repeat(32),
                "wakeve.notification.device-registration.token-encryption-key" to "e".repeat(32)
            ),
            environment = emptyMap()
        ).getOrThrow()
        return RegistrationFixture(
            factory = SqliteBackendDeviceRegistrationStoreFactory(configuration),
            databasePath = configuration.databasePath,
            legacyDatabasePath = root.resolve("legacy.sqlite")
        )
    }

    private fun seedExistingV2RegistrationSentinel(databasePath: Path) {
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("PRAGMA foreign_keys = ON")
                statement.execute(
                    """
                    CREATE TABLE device_installation (
                        installation_id TEXT PRIMARY KEY NOT NULL,
                        platform TEXT NOT NULL,
                        created_at_epoch_seconds INTEGER NOT NULL,
                        updated_at_epoch_seconds INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE TABLE device_registration (
                        registration_id TEXT PRIMARY KEY NOT NULL,
                        installation_id TEXT NOT NULL,
                        user_id TEXT NOT NULL,
                        environment TEXT NOT NULL,
                        topic TEXT NOT NULL,
                        token_ciphertext BLOB NOT NULL,
                        token_hash TEXT NOT NULL,
                        status TEXT NOT NULL CHECK(status IN ('ACTIVE', 'INVALID', 'UNREGISTERED')),
                        created_at_epoch_seconds INTEGER NOT NULL,
                        updated_at_epoch_seconds INTEGER NOT NULL,
                        last_registered_at_epoch_seconds INTEGER NOT NULL,
                        invalidated_at_epoch_seconds INTEGER,
                        invalidation_reason TEXT,
                        unregistered_at_epoch_seconds INTEGER,
                        unregistered_reason TEXT,
                        CHECK (
                            (
                                status = 'ACTIVE'
                                AND invalidated_at_epoch_seconds IS NULL
                                AND invalidation_reason IS NULL
                                AND unregistered_at_epoch_seconds IS NULL
                                AND unregistered_reason IS NULL
                            )
                            OR (
                                status = 'INVALID'
                                AND invalidated_at_epoch_seconds IS NOT NULL
                                AND invalidation_reason IS NOT NULL
                                AND invalidation_reason IN (
                                    'BAD_DEVICE_TOKEN',
                                    'DEVICE_TOKEN_NOT_FOR_TOPIC',
                                    'EXPIRED_TOKEN',
                                    'UNREGISTERED'
                                )
                                AND unregistered_at_epoch_seconds IS NULL
                                AND unregistered_reason IS NULL
                            )
                            OR (
                                status = 'UNREGISTERED'
                                AND invalidated_at_epoch_seconds IS NULL
                                AND invalidation_reason IS NULL
                                AND unregistered_at_epoch_seconds IS NOT NULL
                                AND unregistered_reason IS NOT NULL
                                AND unregistered_reason IN (
                                    'ACCOUNT_CHANGED',
                                    'SCOPE_CHANGED',
                                    'LOGOUT',
                                    'USER_REQUESTED',
                                    'ADMIN_REVOKED'
                                )
                            )
                        ),
                        FOREIGN KEY (installation_id)
                            REFERENCES device_installation(installation_id)
                            ON UPDATE RESTRICT
                            ON DELETE RESTRICT
                    )
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE UNIQUE INDEX one_active_registration_per_installation
                    ON device_registration(installation_id)
                    WHERE status = 'ACTIVE'
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE UNIQUE INDEX one_active_registration_per_token_scope
                    ON device_registration(environment, topic, token_hash)
                    WHERE status = 'ACTIVE'
                    """.trimIndent()
                )
                statement.execute(
                    """
                    CREATE INDEX active_registration_recipient_lookup
                    ON device_registration(user_id, environment, topic, status)
                    """.trimIndent()
                )
            }
            connection.prepareStatement(
                """
                INSERT INTO device_installation(
                    installation_id, platform, created_at_epoch_seconds, updated_at_epoch_seconds
                ) VALUES (?, ?, ?, ?)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, "sentinel-installation")
                statement.setString(2, Platform.IOS.name)
                statement.setLong(3, 1)
                statement.setLong(4, 1)
                statement.executeUpdate()
            }
            connection.prepareStatement(
                """
                INSERT INTO device_registration(
                    registration_id, installation_id, user_id, environment, topic,
                    token_ciphertext, token_hash, status, created_at_epoch_seconds,
                    updated_at_epoch_seconds, last_registered_at_epoch_seconds,
                    invalidated_at_epoch_seconds, invalidation_reason,
                    unregistered_at_epoch_seconds, unregistered_reason
                ) VALUES (?, ?, ?, ?, ?, ?, ?, 'ACTIVE', ?, ?, ?, NULL, NULL, NULL, NULL)
                """.trimIndent()
            ).use { statement ->
                statement.setString(1, "sentinel-registration")
                statement.setString(2, "sentinel-installation")
                statement.setString(3, "sentinel-owner")
                statement.setString(4, APNsEnvironment.PRODUCTION.name)
                statement.setString(5, "com.guyghost.wakeve")
                statement.setBytes(6, byteArrayOf(1, 2, 3, 4))
                statement.setString(7, "sentinel-token-digest")
                statement.setLong(8, 1)
                statement.setLong(9, 1)
                statement.setLong(10, 1)
                statement.executeUpdate()
            }
        }
    }

    private fun blockedRoutePersistenceDump(fixture: RegistrationFixture): BlockedRoutePersistenceDump =
        BlockedRoutePersistenceDump(
            registration = sanitizedJdbcDump(
                fixture.databasePath,
                listOf(
                    "device_installation",
                    "device_registration",
                    "legacy_notification_compatibility_saga",
                    "legacy_notification_compatibility_effect_history",
                    "legacy_notification_compatibility_generation"
                )
            ),
            legacy = sanitizedJdbcDump(fixture.legacyDatabasePath, listOf("notification_token"))
        )

    private fun sanitizedJdbcDump(databasePath: Path, trackedTables: List<String>): SanitizedJdbcDump =
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { it.execute("PRAGMA query_only = ON") }
            val counts = linkedMapOf<String, Long>()
            val rowDigests = linkedMapOf<String, List<String>>()
            trackedTables.forEach { table ->
                require(table.matches(Regex("[a-z_]+")))
                if (connection.prepareStatement(
                        "SELECT 1 FROM sqlite_schema WHERE type = 'table' AND name = ?"
                    ).use { statement ->
                        statement.setString(1, table)
                        statement.executeQuery().use { it.next() }
                    }
                ) {
                    counts[table] = connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT COUNT(*) FROM \"$table\"").use { rows ->
                            rows.next()
                            rows.getLong(1)
                        }
                    }
                    rowDigests[table] = connection.createStatement().use { statement ->
                        statement.executeQuery("SELECT * FROM \"$table\"").use { rows ->
                            buildList {
                                while (rows.next()) add(rows.sanitizedDigest())
                            }.sorted()
                        }
                    }
                }
            }
            SanitizedJdbcDump(counts = counts, rowDigests = rowDigests)
        }

    private fun ResultSet.sanitizedDigest(): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val metadata = metaData
        for (column in 1..metadata.columnCount) {
            val value = getObject(column)
            val encoded = when (value) {
                null -> "null".toByteArray(StandardCharsets.UTF_8)
                is ByteArray -> Base64.getEncoder().encode(value)
                else -> "${value.javaClass.name}:$value".toByteArray(StandardCharsets.UTF_8)
            }
            digest.update("${metadata.getColumnName(column)}:${encoded.size}:".toByteArray(StandardCharsets.UTF_8))
            digest.update(encoded)
        }
        return digest.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }

    private fun sqliteSchemaDump(databasePath: Path): List<String> =
        DriverManager.getConnection("jdbc:sqlite:$databasePath").use { connection ->
            connection.createStatement().use { it.execute("PRAGMA query_only = ON") }
            connection.createStatement().use { statement ->
                statement.executeQuery(
                    """
                    SELECT type, name, tbl_name, COALESCE(sql, '')
                    FROM sqlite_schema
                    WHERE name NOT LIKE 'sqlite_%'
                    ORDER BY type, name
                    """.trimIndent()
                ).use { rows ->
                    buildList {
                        while (rows.next()) {
                            add((1..4).joinToString("|") { column -> rows.getString(column) })
                        }
                    }
                }
            }
        }

    private fun scope(): DeviceRegistrationScope =
        DeviceRegistrationScope.create(APNsEnvironment.PRODUCTION, "com.guyghost.wakeve").getOrThrow()

    private fun jwt(userId: String): String = JWT.create()
        .withIssuer(jwtIssuer)
        .withAudience(jwtAudience)
        .withClaim("userId", userId)
        .withClaim("sessionId", "session-$userId")
        .withClaim("permissions", listOf("READ", "WRITE"))
        .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
        .sign(Algorithm.HMAC256(jwtSecret))

    private inner class RegistrationFixture(
        val factory: SqliteBackendDeviceRegistrationStoreFactory,
        val databasePath: Path,
        val legacyDatabasePath: Path
    ) {
        val store get() = factory.open()

        fun register(userId: String, installationId: String, token: String) = runBlocking {
            factory.open().use { store ->
                store.register(
                    BackendDeviceRegistrationRequest.create(
                        installationId = installationId,
                        authenticatedUserId = userId,
                        platform = Platform.IOS,
                        scope = scope(),
                        rawToken = token,
                        registeredAtEpochSeconds = 100
                    ).getOrThrow()
                )
            }
        }

        fun backfillLegacy(userId: String, legacyRowKey: String, token: String) = runBlocking {
            factory.open().use { store ->
                store.backfillLegacy(
                    LegacyNotificationTokenBackfill.create(
                        legacyRowKey = legacyRowKey,
                        userId = userId,
                        platform = Platform.IOS,
                        rawToken = token,
                        scope = scope(),
                        updatedAtEpochSeconds = 100
                    ).getOrThrow()
                )
            }
        }
    }

    private data class BlockedRoutePersistenceDump(
        val registration: SanitizedJdbcDump,
        val legacy: SanitizedJdbcDump
    )

    private data class SanitizedJdbcDump(
        val counts: Map<String, Long>,
        val rowDigests: Map<String, List<String>>
    )
}
