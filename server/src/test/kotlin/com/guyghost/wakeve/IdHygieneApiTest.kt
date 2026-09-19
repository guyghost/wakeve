package com.guyghost.wakeve

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proposal #42: generated ids are opaque and collision-free — event ids use a
 * UUID suffix instead of Math.random(), invitation row ids instead of a
 * 0-9999 random int, user ids instead of double hashCode().
 */
class IdHygieneApiTest {

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
    fun `rapidly created events and invitations get distinct clean ids`() = testApplication {
        application { module(database) }
        val auth = "Bearer ${createTestJwt("user-hygiene")}"

        val eventIds = mutableSetOf<String>()
        val invitationIds = mutableSetOf<String>()
        repeat(5) { index ->
            val created = client.post("/api/events") {
                header(HttpHeaders.Authorization, auth)
                contentType(ContentType.Application.Json)
                setBody(
                    """{"title":"Hygiène $index","description":"QA","organizerId":"user-hygiene",
                       "deadline":"2099-01-01T00:00:00Z",
                       "proposedSlots":[{"id":"s$index","start":"2026-12-05T12:00:00Z","end":"2026-12-12T22:00:00Z","timezone":"UTC","timeOfDay":"ALL_DAY"}]}"""
                )
            }
            assertEquals(HttpStatusCode.Created, created.status, created.bodyAsText())
            val body = created.bodyAsText()
            val id = Regex(""""id":\s*"([^"]+)"""").find(body)!!.groupValues.let { g ->
                // first occurrence of an event id (double-escaped JSON tolerated)
                Regex("""event_[0-9]+_[0-9a-f]{8}""").find(body)?.value
            }
            assertTrue(!id.isNullOrEmpty(), "event id must use the clean format: $body")
            assertTrue(eventIds.add(id), "event ids must be unique: $eventIds")

            val invite = client.post("/api/events/$id/invite") {
                header(HttpHeaders.Authorization, auth)
                contentType(ContentType.Application.Json)
                setBody("{}")
            }
            assertEquals(HttpStatusCode.Created, invite.status, invite.bodyAsText())
            val invitationId = Regex(""""id":\s*"([^"]+)"""").find(invite.bodyAsText())!!.groupValues[1]
            assertTrue(invitationIds.add(invitationId), "invitation ids must be unique: $invitationIds")
        }
    }

    @Test
    fun `guest re-login with same device resumes profile instead of crashing`() = testApplication {
        application { module(database) }

        val first = client.post("/auth/guest") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"hygiene-device"}""")
        }
        val second = client.post("/auth/guest") {
            contentType(ContentType.Application.Json)
            setBody("""{"deviceId":"hygiene-device"}""")
        }
        assertEquals(HttpStatusCode.OK, first.status, first.bodyAsText())
        assertEquals(HttpStatusCode.OK, second.status, second.bodyAsText())
    }
}
