package com.guyghost.wakeve

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.auth.GUEST_REFRESH_TOKEN_VALIDITY_SECONDS
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import io.ktor.client.request.delete
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.jsonObject
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for proposal #39: guest sessions receive a refresh token
 * (30-day validity) so multi-day planning is not cut off after one hour, the
 * generic /auth/refresh flow accepts it, and account deletion invalidates it.
 */
class GuestRefreshTokenTest {

    private lateinit var database: WakeveDb
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "wakev-api"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "wakev-client"

    @Before
    fun setup() {
        DatabaseProvider.resetDatabase()
        database = DatabaseProvider.getDatabase(JvmTestDatabaseFactory())
    }

    @After
    fun teardown() {
        DatabaseProvider.resetDatabase()
    }

    private fun createTestJwt(userId: String): String =
        JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))

    @Test
    fun `guest login issues a 30-day refresh token`() = testApplication {
        application { module(database) }

        val response = client.post("/auth/guest") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"test-device-guest-refresh"}""")
        }
        val body = response.bodyAsText()

        assertEquals(HttpStatusCode.OK, response.status, body)
        val json = kotlinx.serialization.json.Json.parseToJsonElement(body).jsonObject
        val refreshToken = json["refreshToken"]?.toString()?.trim('"')
        assertTrue(!refreshToken.isNullOrEmpty(), "guest sessions must receive a refresh token: $body")
        // The stored token expiry must be ~30 days, not 1 hour.
        val stored = database.dbVerifyRefreshTokenExpiry(refreshToken)
        assertTrue(stored, "stored refresh expiry must be ~30 days")
        assertEquals(
            30L * 24 * 3600,
            GUEST_REFRESH_TOKEN_VALIDITY_SECONDS
        )
    }

    @Test
    fun `guest refresh token yields a new access token`() = testApplication {
        application { module(database) }

        val login = client.post("/auth/guest") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"test-device-guest-refresh-2"}""")
        }
        val loginJson = kotlinx.serialization.json.Json.parseToJsonElement(login.bodyAsText()).jsonObject
        val refreshToken = loginJson["refreshToken"]!!.toString().trim('"')

        val refreshed = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$refreshToken"}""")
        }
        assertEquals(HttpStatusCode.OK, refreshed.status, refreshed.bodyAsText())
        val refreshedJson = kotlinx.serialization.json.Json.parseToJsonElement(refreshed.bodyAsText()).jsonObject
        val newAccess = refreshedJson["accessToken"]?.toString()?.trim('"')
        assertTrue(!newAccess.isNullOrEmpty(), "refresh must issue an access token")
        assertEquals("guest", refreshedJson["user"]?.jsonObject?.get("provider")?.toString()?.trim('"'))
        assertEquals(
            refreshToken,
            refreshedJson["refreshToken"]?.toString()?.trim('"'),
            "the refresh token is kept across refreshes (existing contract)"
        )
    }

    @Test
    fun `invalid refresh token is rejected`() = testApplication {
        application { module(database) }

        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"not-a-real-token"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status, response.bodyAsText())
    }

    @Test
    fun `re-login with the same device resumes the guest profile`() = testApplication {
        application { module(database) }

        val first = client.post("/auth/guest") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"same-device"}""")
        }
        val second = client.post("/auth/guest") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"same-device"}""")
        }
        assertEquals(HttpStatusCode.OK, first.status, first.bodyAsText())
        assertEquals(HttpStatusCode.OK, second.status, "same-device re-login must resume, not crash: ${second.bodyAsText()}")

        val firstJson = kotlinx.serialization.json.Json.parseToJsonElement(first.bodyAsText()).jsonObject
        val secondJson = kotlinx.serialization.json.Json.parseToJsonElement(second.bodyAsText()).jsonObject
        assertEquals(
            firstJson["user"]!!.jsonObject["id"],
            secondJson["user"]!!.jsonObject["id"],
            "same device must resume the same guest profile"
        )
    }

    @Test
    fun `account deletion invalidates the guest refresh token`() = testApplication {
        application { module(database) }

        val login = client.post("/auth/guest") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"test-device-guest-refresh-3"}""")
        }
        val loginJson = kotlinx.serialization.json.Json.parseToJsonElement(login.bodyAsText()).jsonObject
        val refreshToken = loginJson["refreshToken"]!!.toString().trim('"')
        val userId = loginJson["user"]!!.jsonObject["id"]!!.toString().trim('"')

        val delete = client.delete("/api/user/delete") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt(userId)}")
        }
        assertEquals(HttpStatusCode.OK, delete.status, delete.bodyAsText())

        val refreshed = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody("""{"refreshToken":"$refreshToken"}""")
        }
        assertEquals(
            HttpStatusCode.Unauthorized,
            refreshed.status,
            "deleted guest account must not be refreshable: ${refreshed.bodyAsText()}"
        )
    }
}

private fun WakeveDb.dbVerifyRefreshTokenExpiry(refreshToken: String): Boolean {
    val repository = com.guyghost.wakeve.repository.UserRepository(this)
    val token = runBlocking { repository.getUserTokenByRefreshToken(refreshToken) } ?: return false
    val expiresAt = java.time.Instant.parse(token.expiresAt)
    val expectedMin = java.time.Instant.now().plusSeconds(GUEST_REFRESH_TOKEN_VALIDITY_SECONDS - 3600)
    return expiresAt.isAfter(expectedMin)
}

