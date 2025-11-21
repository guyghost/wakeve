package com.guyghost.wakeve

import com.guyghost.wakeve.models.SyncChange
import com.guyghost.wakeve.models.SyncRequest
import com.guyghost.wakeve.models.TokenRefreshRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        // Initialize test database
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)

        application {
            module(database, eventRepository)
        }
        val response = client.get("/")
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Wakev API v1.0", response.bodyAsText())
    }

    @Test
    fun testProtectedApiRequiresAuth() = testApplication {
        // Initialize test database
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)

        application {
            module(database, eventRepository)
        }

        // Try to access protected API without authentication
        val response = client.get("/api/events")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testTokenRefreshEndpoint() = testApplication {
        // Initialize test database
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                })
            }
        }

        application {
            module(database, eventRepository)
        }

        // Test token refresh with invalid token (should return 401)
        val refreshRequest = TokenRefreshRequest(refreshToken = "invalid-refresh-token")
        val response = client.post("/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(refreshRequest)
        }

        // Should return 401 for invalid refresh token
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun testSyncEndpointRequiresAuth() = testApplication {
        // Initialize test database
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        val eventRepository = DatabaseEventRepository(database)

        val client = createClient {
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    ignoreUnknownKeys = true
                })
            }
        }

        application {
            module(database, eventRepository)
        }

        // Try to access sync endpoint without authentication
        val syncRequest = SyncRequest(
            changes = listOf(
                SyncChange(
                    id = "test-change-1",
                    table = "events",
                    operation = "CREATE",
                    recordId = "test-event-1",
                    data = """{"id":"test-event-1","title":"Test Event","description":"Test","organizerId":"user-1","deadline":"2025-12-01T12:00:00Z"}""",
                    timestamp = "2025-11-19T10:00:00Z",
                    userId = "user-1"
                )
            )
        )

        val response = client.post("/api/sync") {
            contentType(ContentType.Application.Json)
            setBody(syncRequest)
        }

        // Should return 401 for unauthenticated request
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }
}