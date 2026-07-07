package com.guyghost.wakeve

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.auth.SessionRepository
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import io.ktor.client.request.header
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AuthFlowIntegrationTest {

    private lateinit var database: WakeveDb
    private lateinit var sessionRepository: SessionRepository
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "wakev-api"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "wakev-client"

    @Before
    fun setup() {
        DatabaseProvider.resetDatabase()
        database = DatabaseProvider.getDatabase(JvmTestDatabaseFactory())
        sessionRepository = SessionRepository(database)
    }

    @After
    fun teardown() {
        DatabaseProvider.resetDatabase()
    }

    @Test
    fun `authenticated user can list active sessions`() = testApplication {
        application { module(database) }

        val (accessToken, sessionId) = runBlocking {
            createSession(userId = "test-user-123", deviceId = "device-1", deviceName = "iPhone")
        }

        val response = client.get("/api/sessions") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains(sessionId))
        assertTrue(body.contains("iPhone"))
    }

    @Test
    fun `user can revoke another active session`() = testApplication {
        application { module(database) }

        val userId = "test-user-multi"
        val (currentToken, currentSessionId) = runBlocking {
            createSession(userId = userId, deviceId = "device-1", deviceName = "Current Device")
        }

        val (_, otherSessionId) = runBlocking {
            createSession(userId = userId, deviceId = "device-2", deviceName = "Other Device")
        }

        val revokeResponse = client.delete("/api/sessions/$otherSessionId") {
            header(HttpHeaders.Authorization, "Bearer $currentToken")
        }

        assertEquals(HttpStatusCode.OK, revokeResponse.status)

        val activeSessions = runBlocking {
            sessionRepository.getActiveSessionsForUser(userId).getOrThrow()
        }

        assertTrue(activeSessions.any { it.id == currentSessionId })
        assertFalse(activeSessions.any { it.id == otherSessionId })
    }

    @Test
    fun `revoked session token is added to blacklist`() = testApplication {
        application { module(database) }

        val (accessToken, sessionId) = runBlocking {
            createSession(userId = "test-user-blacklist", deviceId = "device-blacklist", deviceName = "Blacklisted Device")
        }

        runBlocking {
            sessionRepository.revokeSession(sessionId).getOrThrow()
            val isBlacklisted = sessionRepository.isTokenBlacklisted(accessToken).getOrThrow()
            assertTrue(isBlacklisted)
        }
    }

    @Test
    fun `revoke all others keeps current session active`() = testApplication {
        application { module(database) }

        val userId = "test-user-revoke-all"
        val (currentToken, currentSessionId) = runBlocking {
            createSession(userId = userId, deviceId = "device-1", deviceName = "Current Device")
        }

        runBlocking {
            createSession(userId = userId, deviceId = "device-2", deviceName = "Laptop")
            createSession(userId = userId, deviceId = "device-3", deviceName = "Tablet")
        }

        val response = client.post("/api/sessions/revoke-all-others") {
            header(HttpHeaders.Authorization, "Bearer $currentToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)

        val activeSessions = runBlocking {
            sessionRepository.getActiveSessionsForUser(userId).getOrThrow()
        }

        assertEquals(1, activeSessions.size)
        assertEquals(currentSessionId, activeSessions.first().id)
    }

    @Test
    fun `current session endpoint does not expose session owned by another user`() = testApplication {
        application { module(database) }

        val (_, otherUserSessionId) = runBlocking {
            createSession(userId = "session-owner-user", deviceId = "owner-device", deviceName = "Owner Device")
        }
        val mismatchedToken = createTestJwt(
            userId = "claim-user",
            sessionId = otherUserSessionId
        )

        val response = client.get("/api/sessions/current") {
            header(HttpHeaders.Authorization, "Bearer $mismatchedToken")
        }
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.NotFound, response.status, body)
        assertFalse(body.contains("Owner Device"), body)
        assertFalse(body.contains(otherUserSessionId), body)
    }

    @Test
    fun `account deletion requires authentication`() = testApplication {
        application { module(database) }

        val response = client.delete("/api/user/delete")

        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun `authenticated account deletion removes account credentials push tokens and active sessions`() = testApplication {
        application { module(database) }

        val userId = "test-user-delete"
        seedUser(userId, "delete@example.com")
        val (accessToken, sessionId) = runBlocking {
            createSession(userId = userId, deviceId = "delete-device", deviceName = "iPhone")
        }
        seedAccountDeletionData(userId, accessToken)

        val response = client.delete("/api/user/delete") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertJsonDeleted(response.bodyAsText(), true)
        assertJsonContains(response.bodyAsText(), "\"localCleanupRequired\":true")
        assertJsonContains(response.bodyAsText(), "\"providerRevocationStatus\":\"not_applicable\"")

        runBlocking {
            assertEquals(null, database.userQueries.selectUserById(userId).executeAsOneOrNull())
            assertEquals(null, database.userQueries.selectTokenByUserId(userId).executeAsOneOrNull())
            assertTrue(database.notificationQueries.getTokensByUser(userId).executeAsList().isEmpty())
            assertTrue(sessionRepository.isTokenBlacklisted(accessToken).getOrThrow())
            val revokedSession = sessionRepository.getSessionById(sessionId).getOrThrow()
            assertEquals("revoked", revokedSession?.status)
            assertTrue(sessionRepository.getActiveSessionsForUser(userId).getOrThrow().isEmpty())
        }
    }

    @Test
    fun `account deletion is idempotent after user row is gone`() = testApplication {
        application { module(database) }

        val userId = "test-user-delete-idempotent"
        seedUser(userId, "delete-idempotent@example.com")
        val accessToken = createTestJwt(userId, "account-delete-idempotent")
        val repeatToken = createTestJwt(userId, "account-delete-idempotent-repeat")

        val firstResponse = client.delete("/api/user/delete") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }
        val secondResponse = client.delete("/api/user/delete") {
            header(HttpHeaders.Authorization, "Bearer $repeatToken")
        }

        assertEquals(HttpStatusCode.OK, firstResponse.status)
        assertEquals(HttpStatusCode.OK, secondResponse.status)
        assertJsonDeleted(firstResponse.bodyAsText(), true)
        assertJsonDeleted(secondResponse.bodyAsText(), false)
        assertJsonContains(secondResponse.bodyAsText(), "\"providerRevocationStatus\":\"already_deleted\"")
    }

    @Test
    fun `apple account deletion records unavailable provider revocation when material is absent`() = testApplication {
        application { module(database) }

        val userId = "test-user-delete-apple"
        seedUser(userId, "delete-apple@example.com", provider = "apple")
        val accessToken = createTestJwt(userId, "account-delete-apple")

        val response = client.delete("/api/user/delete") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertJsonDeleted(response.bodyAsText(), true)
        assertJsonContains(response.bodyAsText(), "\"providerRevocationStatus\":\"unavailable\"")
    }

    @Test
    fun `account deletion anonymizes shared collaborative records and removes personal metadata`() = testApplication {
        application { module(database) }

        val userId = "test-user-delete-collab"
        seedUser(userId, "delete-collab@example.com")
        val accessToken = createTestJwt(userId, "account-delete-collab")
        seedCollaborativeDeletionData(userId)

        val response = client.delete("/api/user/delete") {
            header(HttpHeaders.Authorization, "Bearer $accessToken")
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertJsonDeleted(response.bodyAsText(), true)

        val expectedTombstone = deletedUserTombstone(userId)
        val event = database.eventQueries.selectById("event-delete-collab").executeAsOne()
        val participant = database.participantQueries.selectById("participant-delete-collab").executeAsOne()
        val comment = database.commentQueries.selectCommentById("comment-delete-collab").executeAsOne()
        val message = database.chatMessagesQueries.selectMessageById("message-delete-collab").executeAsOne()

        assertEquals(expectedTombstone, event.organizerId)
        assertEquals(expectedTombstone, participant.userId)
        assertEquals(expectedTombstone, comment.author_id)
        assertEquals("Deleted user", comment.author_name)
        assertEquals(null, comment.mentions)
        assertEquals(expectedTombstone, message.sender_id)
        assertEquals("Deleted user", message.sender_name)
        assertEquals(null, message.sender_avatar_url)
        assertEquals(null, message.reactions_json)
        assertEquals(null, message.read_by_json)
        assertTrue(database.chatMessagesQueries.selectUserReactions(userId).executeAsList().isEmpty())
        assertTrue(database.chatMessagesQueries.selectReadStatusByUser(userId).executeAsList().isEmpty())
        assertTrue(database.userQueries.selectPendingSync().executeAsList().none { it.user_id == userId })
        assertTrue(database.notificationQueries.getNotifications(userId, 20).executeAsList().isEmpty())
    }

    private fun assertJsonDeleted(body: String, expected: Boolean) {
        val compact = body.replace(Regex("\\s+"), "")
        assertTrue(compact.contains("\"deleted\":$expected"), body)
    }

    private fun assertJsonContains(body: String, expected: String) {
        val compact = body.replace(Regex("\\s+"), "")
        assertTrue(compact.contains(expected), body)
    }

    private suspend fun createSession(
        userId: String,
        deviceId: String,
        deviceName: String
    ): Pair<String, String> {
        val bootstrapToken = createTestJwt(userId, "bootstrap-$deviceId")
        val refreshToken = "refresh-$deviceId"
        val expiresAt = (System.currentTimeMillis() + 3_600_000).toString()

        val sessionId = sessionRepository.createSession(
            userId = userId,
            deviceId = deviceId,
            deviceName = deviceName,
            jwtToken = bootstrapToken,
            refreshToken = refreshToken,
            ipAddress = "127.0.0.1",
            userAgent = "Ktor Test Client",
            expiresAt = expiresAt
        ).getOrThrow()

        val accessToken = createTestJwt(userId, sessionId)

        sessionRepository.updateSessionTokens(
            sessionId = sessionId,
            newJwtToken = accessToken,
            newRefreshToken = refreshToken,
            newExpiresAt = expiresAt
        ).getOrThrow()

        return accessToken to sessionId
    }

    private fun seedUser(userId: String, email: String, provider: String = "email") {
        database.userQueries.insertUser(
            id = userId,
            provider_id = "provider-$userId",
            email = email,
            name = "Delete Test",
            avatar_url = null,
            provider = provider,
            role = "USER",
            created_at = "2026-06-07T00:00:00Z",
            updated_at = "2026-06-07T00:00:00Z"
        )
    }

    private fun seedAccountDeletionData(userId: String, accessToken: String) {
        database.userQueries.insertToken(
            id = "token-$userId",
            user_id = userId,
            access_token = accessToken,
            refresh_token = "refresh-$userId",
            token_type = "Bearer",
            expires_at = "2099-01-01T00:00:00Z",
            scope = null,
            created_at = "2026-06-07T00:00:00Z",
            updated_at = "2026-06-07T00:00:00Z"
        )
        database.notificationQueries.upsertToken(
            user_id = userId,
            platform = "ios",
            token = "apns-$userId",
            updated_at = 1L
        )
    }

    private fun seedCollaborativeDeletionData(userId: String) {
        val now = "2026-06-07T00:00:00Z"
        database.eventQueries.insertEvent(
            id = "event-delete-collab",
            organizerId = userId,
            title = "Shared trip",
            description = "Shared event history should remain",
            status = "CONFIRMED",
            deadline = "2026-06-08T00:00:00Z",
            createdAt = now,
            updatedAt = now,
            version = 1,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null,
            isSample = 0
        )
        database.participantQueries.insertParticipant(
            id = "participant-delete-collab",
            eventId = "event-delete-collab",
            userId = userId,
            role = "ORGANIZER",
            hasValidatedDate = 1,
            joinedAt = now,
            updatedAt = now
        )
        database.commentQueries.insertComment(
            id = "comment-delete-collab",
            event_id = "event-delete-collab",
            section = "GENERAL",
            section_item_id = null,
            author_id = userId,
            author_name = "Delete Me",
            content = "Keep this shared comment body",
            parent_comment_id = null,
            mentions = "[\"$userId\"]",
            is_deleted = 0,
            is_pinned = 0,
            created_at = now,
            updated_at = null,
            is_edited = 0,
            reply_count = 0,
            moderation_status = "APPROVED"
        )
        database.chatMessagesQueries.insertMessageMinimal(
            id = "message-delete-collab",
            event_id = "event-delete-collab",
            sender_id = userId,
            sender_name = "Delete Me",
            sender_avatar_url = "https://example.com/avatar.png",
            content = "Keep this shared message body",
            section = "GENERAL",
            section_item_id = null,
            parent_message_id = null,
            timestamp = now,
            status = "SENT",
            is_offline = 0,
            reply_count = 0,
            moderation_status = "APPROVED",
            created_at = now,
            updated_at = now
        )
        database.chatMessagesQueries.insertReaction(
            id = "reaction-delete-collab",
            message_id = "message-delete-collab",
            user_id = userId,
            emoji = "👍",
            timestamp = now
        )
        database.chatMessagesQueries.insertReadStatus(
            id = "read-delete-collab",
            message_id = "message-delete-collab",
            user_id = userId,
            read_at = now
        )
        database.userQueries.insertSyncMetadata(
            id = "sync-delete-collab",
            table_name = "event",
            record_id = "event-delete-collab",
            operation = "UPDATE",
            timestamp = now,
            user_id = userId,
            synced = 0,
            retry_count = 0,
            last_error = null
        )
        database.notificationQueries.insertNotification(
            id = "notification-delete-collab",
            user_id = userId,
            type = "EVENT_UPDATE",
            title = "Event update",
            body = "Personal notification",
            data_ = null,
            created_at = 1L,
            sent_at = null
        )
    }

    private fun deletedUserTombstone(userId: String): String {
        val hash = userId.hashCode().toUInt().toString(16).padStart(8, '0')
        return "deleted_user_$hash"
    }

    private fun createTestJwt(userId: String, sessionId: String): String {
        return JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withClaim("sessionId", sessionId)
            .withClaim("permissions", listOf("READ", "WRITE"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))
    }
}
