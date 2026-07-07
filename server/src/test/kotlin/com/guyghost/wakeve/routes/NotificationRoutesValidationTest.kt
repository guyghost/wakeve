package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.module
import com.guyghost.wakeve.notification.APNsSender
import com.guyghost.wakeve.notification.FCMSender
import com.guyghost.wakeve.notification.NotificationPreferencesRepository
import com.guyghost.wakeve.notification.NotificationService
import com.guyghost.wakeve.notification.NotificationType
import com.guyghost.wakeve.notification.Platform
import com.guyghost.wakeve.notification.defaultNotificationPreferences
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NotificationRoutesValidationTest {
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "wakev-api"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "wakev-client"
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun validatePushToken_acceptsTrimmedToken() {
        val result = validatePushToken("  fcm-token  ")

        assertEquals("fcm-token", result.getOrThrow())
    }

    @Test
    fun validatePushToken_rejectsBlankToken() {
        val result = validatePushToken("  ")

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "must not be blank")
    }

    @Test
    fun parseNotificationPlatform_acceptsAndroidCaseInsensitively() {
        val result = parseNotificationPlatform("  AnDrOiD  ")

        assertEquals(Platform.ANDROID, result.getOrThrow())
    }

    @Test
    fun parseNotificationPlatform_acceptsIosCaseInsensitively() {
        val result = parseNotificationPlatform("  IOS  ")

        assertEquals(Platform.IOS, result.getOrThrow())
    }

    @Test
    fun parseNotificationPlatform_rejectsMissingPlatform() {
        val result = parseNotificationPlatform(null)

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "platform query parameter required")
    }

    @Test
    fun parseNotificationPlatform_rejectsUnsupportedPlatform() {
        val result = parseNotificationPlatform("web")

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "Invalid platform: web")
    }

    @Test
    fun bindPreferencesToAuthenticatedUser_allowsMatchingUserId() {
        val preferences = defaultNotificationPreferences("user-123")

        val result = bindPreferencesToAuthenticatedUser(
            preferences = preferences,
            authenticatedUserId = " user-123 "
        )

        assertEquals("user-123", result.getOrThrow().userId)
    }

    @Test
    fun bindPreferencesToAuthenticatedUser_replacesBlankBodyUserIdWithJwtUserId() {
        val preferences = defaultNotificationPreferences("").copy(userId = "  ")

        val result = bindPreferencesToAuthenticatedUser(
            preferences = preferences,
            authenticatedUserId = "user-123"
        )

        assertEquals("user-123", result.getOrThrow().userId)
    }

    @Test
    fun bindPreferencesToAuthenticatedUser_rejectsMismatchedBodyUserId() {
        val preferences = defaultNotificationPreferences("victim-user")

        val result = bindPreferencesToAuthenticatedUser(
            preferences = preferences,
            authenticatedUserId = "attacker-user"
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "another user")
    }

    @Test
    fun bindPreferencesToAuthenticatedUser_rejectsBlankAuthenticatedUserId() {
        val preferences = defaultNotificationPreferences("user-123")

        val result = bindPreferencesToAuthenticatedUser(
            preferences = preferences,
            authenticatedUserId = "  "
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "Missing userId")
    }

    @Test
    fun authorizeNotificationSend_allowsSelfTarget() {
        val result = authorizeNotificationSend(
            senderUserId = " user-123 ",
            targetUserId = "user-123",
            role = null,
            roles = emptyList(),
            permissions = emptyList()
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun authorizeNotificationSend_rejectsBlankTargetUserId() {
        val result = authorizeNotificationSend(
            senderUserId = "user-123",
            targetUserId = "  ",
            role = "ADMIN",
            roles = emptyList(),
            permissions = emptyList()
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "target userId")
    }

    @Test
    fun authorizeNotificationSend_rejectsCrossUserWithoutPrivilege() {
        val result = authorizeNotificationSend(
            senderUserId = "sender-user",
            targetUserId = "target-user",
            role = "USER",
            roles = emptyList(),
            permissions = listOf("READ", "WRITE")
        )

        assertTrue(result.isFailure)
        assertContains(result.exceptionOrNull()?.message.orEmpty(), "another user")
    }

    @Test
    fun authorizeNotificationSend_allowsCrossUserForAdminRole() {
        val result = authorizeNotificationSend(
            senderUserId = "admin-user",
            targetUserId = "target-user",
            role = "admin",
            roles = emptyList(),
            permissions = emptyList()
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun authorizeNotificationSend_allowsCrossUserForNotificationPermission() {
        val result = authorizeNotificationSend(
            senderUserId = "service-user",
            targetUserId = "target-user",
            role = null,
            roles = emptyList(),
            permissions = listOf("notifications_send")
        )

        assertTrue(result.isSuccess)
    }

    @Test
    fun parseNotificationHistoryLimit_defaultsMissingOrInvalidLimit() {
        assertEquals(50, parseNotificationHistoryLimit(null))
        assertEquals(50, parseNotificationHistoryLimit("not-a-number"))
    }

    @Test
    fun parseNotificationHistoryLimit_acceptsTrimmedValidLimit() {
        assertEquals(25, parseNotificationHistoryLimit(" 25 "))
    }

    @Test
    fun parseNotificationHistoryLimit_clampsNonPositiveLimitToOne() {
        assertEquals(1, parseNotificationHistoryLimit("-1"))
        assertEquals(1, parseNotificationHistoryLimit("0"))
    }

    @Test
    fun parseNotificationHistoryLimit_clampsLargeLimitToMaximum() {
        assertEquals(100, parseNotificationHistoryLimit("100000"))
    }

    @Test
    fun resolveEffectiveNotificationPreferences_returnsStoredPreferencesWhenPresent() {
        val stored = defaultNotificationPreferences("user-123").copy(soundEnabled = false)

        val result = resolveEffectiveNotificationPreferences(
            authenticatedUserId = "user-123",
            storedPreferences = stored
        )

        assertEquals(stored, result)
    }

    @Test
    fun resolveEffectiveNotificationPreferences_returnsDefaultPreferencesWhenMissing() {
        val result = resolveEffectiveNotificationPreferences(
            authenticatedUserId = " user-123 ",
            storedPreferences = null
        )

        assertEquals("user-123", result.userId)
        assertTrue(result.soundEnabled)
        assertTrue(result.vibrationEnabled)
        assertTrue(result.enabledTypes.isNotEmpty())
    }

    @Test
    fun registerPushToken_returnsBadRequestWhenServiceRejectsJwtUserId() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        application {
            module(database)
        }

        val response = client.post("/api/notifications/register") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("   ")}")
            contentType(ContentType.Application.Json)
            setBody("""{"token":"fcm-token","platform":"android"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        assertContains(response.bodyAsText(), notificationTokenRegisterFailureMessage())
    }

    @Test
    fun unregisterPushToken_returnsBadRequestWhenServiceRejectsJwtUserId() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        application {
            module(database)
        }

        val response = client.delete("/api/notifications/unregister?platform=android") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("   ")}")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status, response.bodyAsText())
        assertContains(response.bodyAsText(), notificationTokenUnregisterFailureMessage())
    }

    @Test
    fun sendNotification_addsDeepLinkPayloadBeforePersisting() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val fcmSender = RecordingFCMSender()
        val notificationService = NotificationService(
            database = database,
            preferencesRepository = NotificationPreferencesRepository(database),
            fcmSender = fcmSender,
            apnsSender = RecordingAPNsSender()
        )
        seedNotificationUser(database, "sender-user")

        val client = createClient {
            install(ContentNegotiation) {
                json(json)
            }
        }

        application {
            module(database, notificationService = notificationService)
        }

        val response = client.post("/api/notifications/send") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("sender-user")}")
            contentType(ContentType.Application.Json)
            setBody(
                """
                {
                  "userId": "sender-user",
                  "type": "EVENT_UPDATE",
                  "title": "Programme modifie",
                  "body": "Le programme de l'evenement a change",
                  "eventId": "event-123"
                }
                """.trimIndent()
            )
        }

        assertEquals(HttpStatusCode.OK, response.status, response.bodyAsText())
        val notification = database.notificationQueries
            .getNotifications("sender-user", 1)
            .executeAsOneOrNull()
        assertNotNull(notification)
        val payload = json.parseToJsonElement(notification.data_ ?: "{}").jsonObject
        assertEquals("wakeve://event/event-123/details?tab=details", payload["deepLink"]?.jsonPrimitive?.content)
        assertEquals("EVENT_DETAILS", payload["deepLinkRoute"]?.jsonPrimitive?.content)
        assertEquals("event/event-123/details", payload["deepLinkPath"]?.jsonPrimitive?.content)
        assertEquals("details", payload["param_tab"]?.jsonPrimitive?.content)
        assertEquals("wakeve://event/event-123/details?tab=details", fcmSender.sentData.single()["deepLink"])
    }

    private fun createTestJwt(userId: String): String =
        JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withClaim("sessionId", "test-session-$userId")
            .withClaim("permissions", listOf("READ", "WRITE"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))

    private fun seedNotificationUser(database: WakeveDb, userId: String) {
        val now = "2026-01-01T00:00:00Z"
        database.userQueries.insertUser(
            id = userId,
            provider_id = "provider-$userId",
            email = "$userId@example.test",
            name = "User $userId",
            avatar_url = null,
            provider = "google",
            role = "USER",
            created_at = now,
            updated_at = now
        )
        database.userQueries.insertPreferences(
            user_id = userId,
            enabled_types = Json.encodeToString(NotificationType.entries.map { it.name }),
            quiet_hours_start = null,
            quiet_hours_end = null,
            sound_enabled = 1,
            vibration_enabled = 1,
            updated_at = 1L
        )
        database.notificationQueries.upsertToken(
            user_id = userId,
            platform = Platform.ANDROID.name,
            token = "fcm-token-$userId",
            updated_at = 1L
        )
    }

    private class RecordingFCMSender : FCMSender {
        val sentData = mutableListOf<Map<String, String>>()

        override suspend fun sendNotification(
            token: String,
            title: String,
            body: String,
            data: Map<String, String>
        ): Result<Unit> {
            sentData += data
            return Result.success(Unit)
        }
    }

    private class RecordingAPNsSender : APNsSender {
        override suspend fun sendNotification(
            token: String,
            title: String,
            body: String,
            data: Map<String, String>
        ): Result<Unit> = Result.success(Unit)
    }
}
