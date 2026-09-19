package com.guyghost.wakeve

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression tests for proposal #38: guests set a server-side display name
 * (PUT /user/display-name) and comment attribution resolves from that profile
 * instead of showing guest_<uuid>. The forged request-body authorName remains
 * ignored (anti-impersonation contract).
 */
class GuestDisplayNameTest {

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

    private fun seedGuest(name: String = "Invité") {
        database.userQueries.insertUser(
            id = "guest-user-1",
            provider_id = "provider-guest-user-1",
            email = "guest-user-1@guest.wakeve.local",
            name = name,
            avatar_url = null,
            provider = "guest",
            role = "USER",
            created_at = "2026-09-19T07:00:00Z",
            updated_at = "2026-09-19T07:00:00Z"
        )
        // Comments need an event + participant record.
        val repository = com.guyghost.wakeve.repository.DatabaseEventRepository(database)
        runBlocking {
            repository.createEvent(
                com.guyghost.wakeve.models.Event(
                    id = "event-names",
                    title = "Rio",
                    description = "QA",
                    organizerId = "guest-user-1",
                    participants = listOf("guest-user-1"),
                    proposedSlots = listOf(
                        com.guyghost.wakeve.models.TimeSlot(
                            id = "slot-1",
                            start = "2026-12-05T12:00:00Z",
                            end = "2026-12-12T22:00:00Z",
                            timezone = "America/Sao_Paulo"
                        )
                    ),
                    deadline = "2099-01-01T00:00:00Z",
                    status = com.guyghost.wakeve.models.EventStatus.ORGANIZING,
                    createdAt = "2026-09-19T07:00:00Z",
                    updatedAt = "2026-09-19T07:00:00Z"
                )
            ).getOrThrow()
        }
    }

    @Test
    fun `guest sets display name and thread shows it`() = testApplication {
        application { module(database) }
        seedGuest()
        val auth = "Bearer ${createTestJwt("guest-user-1")}"

        val setName = client.put("/api/user/display-name") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"Carla"}""")
        }
        assertEquals(HttpStatusCode.OK, setName.status, setName.bodyAsText())
        assertTrue(setName.bodyAsText().contains("Carla"), setName.bodyAsText())

        val created = client.post("/api/events/event-names/comments") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"authorId":"guest-user-1","authorName":"forged","content":"On part quel jour ?","section":"GENERAL"}""")
        }
        assertEquals(HttpStatusCode.Created, created.status, created.bodyAsText())

        val thread = client.get("/api/events/event-names/comments") {
            header(HttpHeaders.Authorization, auth)
        }
        val body = thread.bodyAsText()
        assertTrue(body.contains("Carla"), "thread must show the profile name: $body")
        assertTrue(!body.contains("forged"), "forged authorName must stay ignored: $body")
        assertTrue(!body.contains("guest-user-1@guest"), "guest email prefix must be replaced: $body")
    }

    @Test
    fun `blank or oversized display name is rejected`() = testApplication {
        application { module(database) }
        seedGuest()
        val auth = "Bearer ${createTestJwt("guest-user-1")}"

        val blank = client.put("/api/user/display-name") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"   "}""")
        }
        assertEquals(HttpStatusCode.BadRequest, blank.status, blank.bodyAsText())

        val oversized = client.put("/api/user/display-name") {
            header(HttpHeaders.Authorization, auth)
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"${"x".repeat(31)}"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, oversized.status, oversized.bodyAsText())
    }

    @Test
    fun `display name requires authentication`() = testApplication {
        application { module(database) }

        val response = client.put("/api/user/display-name") {
            contentType(ContentType.Application.Json)
            setBody("""{"displayName":"Anonyme"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status, response.bodyAsText())
    }
}
