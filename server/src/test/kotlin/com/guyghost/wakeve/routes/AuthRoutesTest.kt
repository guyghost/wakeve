package com.guyghost.wakeve.routes

import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.auth.AuthenticationService
import com.guyghost.wakeve.auth.EmailOtpSender
import com.guyghost.wakeve.auth.OtpManager
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.models.EmailOTPRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.application.install
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation as ServerContentNegotiation
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AuthRoutesTest {
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @BeforeTest
    fun setup() {
        DatabaseProvider.resetDatabase()
    }

    @AfterTest
    fun teardown() {
        DatabaseProvider.resetDatabase()
    }

    @Test
    fun `email OTP request returns unavailable when sender is not configured`() = testApplication {
        val otpManager = OtpManager()
        val client = createJsonClient()

        application {
            install(ServerContentNegotiation) {
                json(json)
            }
            routing {
                route("/api") {
                    authRoutes(createAuthService(), otpManager)
                }
            }
        }

        val response = client.post("/api/auth/email/request") {
            contentType(ContentType.Application.Json)
            setBody(EmailOTPRequest("user@example.com"))
        }

        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
        assertTrue(response.bodyAsText().contains("EMAIL_OTP_UNAVAILABLE"))
        assertFalse(otpManager.verifyOtp("user@example.com", "000000"))
        assertFalse(otpManager.isRateLimited("user@example.com"))
    }

    @Test
    fun `email OTP request stores the same OTP that was delivered`() = testApplication {
        val otpManager = OtpManager()
        val sender = CapturingEmailOtpSender()
        val client = createJsonClient()

        application {
            install(ServerContentNegotiation) {
                json(json)
            }
            routing {
                route("/api") {
                    authRoutes(
                        authService = createAuthService(),
                        otpManager = otpManager,
                        emailOtpSender = sender
                    )
                }
            }
        }

        val response = client.post("/api/auth/email/request") {
            contentType(ContentType.Application.Json)
            setBody(EmailOTPRequest("User@Example.com"))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("User@Example.com", sender.lastEmail)
        assertNotNull(sender.lastOtp)
        assertTrue(otpManager.verifyOtp("user@example.com", sender.lastOtp ?: ""))
    }

    private fun ApplicationTestBuilder.createJsonClient() = createClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun createAuthService(): AuthenticationService {
        val database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        return AuthenticationService(
            db = database,
            jwtSecret = "test-secret",
            jwtIssuer = "wakev-test",
            jwtAudience = "wakev-test"
        )
    }

    private class CapturingEmailOtpSender : EmailOtpSender {
        var lastEmail: String? = null
            private set
        var lastOtp: String? = null
            private set

        override suspend fun sendOtp(
            email: String,
            otp: String,
            expiresInSeconds: Long
        ): Result<Unit> {
            lastEmail = email
            lastOtp = otp
            return Result.success(Unit)
        }
    }
}
