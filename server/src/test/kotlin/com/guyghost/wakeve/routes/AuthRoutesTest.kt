package com.guyghost.wakeve.routes

import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.auth.AuthenticationService
import com.guyghost.wakeve.auth.EmailOtpSender
import com.guyghost.wakeve.auth.OtpManager
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.models.EmailOTPRequest
import com.guyghost.wakeve.models.EmailOTPVerifyRequest
import com.guyghost.wakeve.models.GoogleAuthRequest
import com.guyghost.wakeve.models.GuestSessionRequest
import com.guyghost.wakeve.models.TokenRefreshRequest
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
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        val authService = createAuthService()
        val client = createJsonClient()

        application {
            install(ServerContentNegotiation) {
                json(json)
            }
            routing {
                route("/api") {
                    authRoutes(
                        authService = authService,
                        otpManager = otpManager,
                        emailOtpSender = sender
                    )
                }
            }
        }

        val response = client.post("/api/auth/email/request") {
            contentType(ContentType.Application.Json)
            setBody(EmailOTPRequest(" User@Example.com "))
        }

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("user@example.com", sender.lastEmail)
        assertNotNull(sender.lastOtp)
        assertTrue(otpManager.verifyOtp("user@example.com", sender.lastOtp ?: ""))
    }

    @Test
    fun `email OTP verification accepts trimmed canonical email and OTP`() = testApplication {
        val otpManager = OtpManager()
        val sender = CapturingEmailOtpSender()
        val authService = createAuthService()
        val client = createJsonClient()

        application {
            install(ServerContentNegotiation) {
                json(json)
            }
            routing {
                route("/api") {
                    authRoutes(
                        authService = authService,
                        otpManager = otpManager,
                        emailOtpSender = sender
                    )
                }
            }
        }

        val requestResponse = client.post("/api/auth/email/request") {
            contentType(ContentType.Application.Json)
            setBody(EmailOTPRequest(" USER@Example.COM "))
        }
        assertEquals(HttpStatusCode.OK, requestResponse.status, requestResponse.bodyAsText())
        val deliveredOtp = sender.lastOtp ?: error("Expected OTP delivery")

        val verifyResponse = client.post("/api/auth/email/verify") {
            contentType(ContentType.Application.Json)
            setBody(EmailOTPVerifyRequest(" user@example.com ", " $deliveredOtp "))
        }
        val verifyBody = verifyResponse.bodyAsText()
        val verifiedEmail = json.parseToJsonElement(verifyBody)
            .jsonObject
            .getValue("user")
            .jsonObject
            .getValue("email")
            .jsonPrimitive
            .content

        assertEquals(HttpStatusCode.OK, verifyResponse.status, verifyBody)
        assertEquals("user@example.com", verifiedEmail)
    }

    @Test
    fun `public auth routes reject blank or oversized inputs before service calls`() = testApplication {
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

        val blankGuest = client.post("/api/auth/guest") {
            contentType(ContentType.Application.Json)
            setBody(GuestSessionRequest("   "))
        }
        val longGuest = client.post("/api/auth/guest") {
            contentType(ContentType.Application.Json)
            setBody(GuestSessionRequest("d".repeat(201)))
        }
        val blankRefresh = client.post("/api/auth/refresh") {
            contentType(ContentType.Application.Json)
            setBody(TokenRefreshRequest("   "))
        }
        val oversizedGoogleToken = client.post("/api/auth/google") {
            contentType(ContentType.Application.Json)
            setBody(GoogleAuthRequest("x".repeat(8_193), "user@example.com"))
        }

        assertEquals(HttpStatusCode.BadRequest, blankGuest.status, blankGuest.bodyAsText())
        assertEquals(HttpStatusCode.BadRequest, longGuest.status, longGuest.bodyAsText())
        assertEquals(HttpStatusCode.BadRequest, blankRefresh.status, blankRefresh.bodyAsText())
        assertEquals(HttpStatusCode.BadRequest, oversizedGoogleToken.status, oversizedGoogleToken.bodyAsText())
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
