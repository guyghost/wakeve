package com.guyghost.wakeve

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.database.WakevDb
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
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

    private lateinit var database: WakevDb
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
