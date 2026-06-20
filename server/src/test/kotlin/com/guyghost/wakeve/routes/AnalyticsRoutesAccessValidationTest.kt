package com.guyghost.wakeve.routes

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.module
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnalyticsRoutesAccessValidationTest {
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "default-secret-key-change-in-production"
    private val jwtIssuer = System.getenv("JWT_ISSUER") ?: "wakev-api"
    private val jwtAudience = System.getenv("JWT_AUDIENCE") ?: "wakev-client"
    private val json = Json { ignoreUnknownKeys = true }

    @BeforeTest
    fun setup() {
        DatabaseProvider.resetDatabase()
    }

    @AfterTest
    fun teardown() {
        DatabaseProvider.resetDatabase()
    }

    @Test
    fun `analytics metrics are admin only`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        application { module(database = database) }

        val userResponse = client.get("/api/analytics/metrics") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("regular-user", role = "USER")}")
        }
        val adminResponse = client.get("/api/analytics/metrics") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("admin-user", role = "ADMIN")}")
        }
        val adminText = adminResponse.bodyAsText()

        assertEquals(HttpStatusCode.Forbidden, userResponse.status, userResponse.bodyAsText())
        assertEquals(HttpStatusCode.OK, adminResponse.status, adminText)
        assertTrue(adminText.contains("mau"), adminText)
        assertTrue(adminText.contains("dau"), adminText)
    }

    @Test
    fun `analytics export is admin only and serializes csv`() = testApplication {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val client = createClient {
            install(ContentNegotiation) { json(json) }
        }

        application { module(database = database) }

        val userResponse = client.get("/api/analytics/export?format=csv") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("regular-user", role = "USER")}")
        }
        val adminResponse = client.get("/api/analytics/export?format=csv") {
            header(HttpHeaders.Authorization, "Bearer ${createTestJwt("admin-user", role = "ADMIN")}")
        }
        val adminText = adminResponse.bodyAsText()

        assertEquals(HttpStatusCode.Forbidden, userResponse.status, userResponse.bodyAsText())
        assertEquals(HttpStatusCode.OK, adminResponse.status, adminText)
        assertTrue(adminText.startsWith("Metric,Value"), adminText)
    }

    private fun createTestJwt(userId: String, role: String): String =
        JWT.create()
            .withIssuer(jwtIssuer)
            .withAudience(jwtAudience)
            .withClaim("userId", userId)
            .withClaim("role", role)
            .withClaim("sessionId", "test-session-$userId")
            .withClaim("permissions", listOf("READ", "WRITE"))
            .withExpiresAt(java.util.Date(System.currentTimeMillis() + 3_600_000))
            .sign(Algorithm.HMAC256(jwtSecret))
}
