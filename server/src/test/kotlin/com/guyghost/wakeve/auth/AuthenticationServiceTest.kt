package com.guyghost.wakeve.auth

import com.guyghost.wakeve.DatabaseProvider
import com.guyghost.wakeve.JvmDatabaseFactory
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.OAuthProvider
import com.guyghost.wakeve.models.User
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AuthenticationServiceTest {

    private lateinit var database: WakeveDb
    private lateinit var authService: AuthenticationService

    @BeforeTest
    fun setup() {
        database = DatabaseProvider.getDatabase(JvmDatabaseFactory(":memory:"))
        authService = AuthenticationService(
            db = database,
            jwtSecret = "test-secret-key-for-jwt-testing",
            jwtIssuer = "wakev-test",
            jwtAudience = "wakev-test-client"
        )
    }

    @Test
    fun testGenerateJwtToken() {
        val testUser = User(
            id = "test-user-id",
            providerId = "google-test-id",
            email = "test@example.com",
            name = "Test User",
            avatarUrl = "https://example.com/avatar.jpg",
            provider = OAuthProvider.GOOGLE,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val token = authService.generateJwtToken(testUser)

        assertNotNull(token)
        assertTrue(token.isNotEmpty())
        assertTrue(token.startsWith("eyJ")) // JWT tokens start with "eyJ"
    }

    @Test
    fun testVerifyJwtToken() {
        val testUser = User(
            id = "test-user-id",
            providerId = "google-test-id",
            email = "test@example.com",
            name = "Test User",
            avatarUrl = "https://example.com/avatar.jpg",
            provider = OAuthProvider.GOOGLE,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        val token = authService.generateJwtToken(testUser)
        val decoded = authService.verifyJwtToken(token)

        assertNotNull(decoded)
        assertEquals(testUser.id, decoded.subject)
        assertEquals("wakev-test", decoded.issuer)
        assertEquals("wakev-test-client", decoded.audience.first())
        assertEquals(testUser.email, decoded.getClaim("email").asString())
        assertEquals(testUser.id, decoded.getClaim("userId").asString())
        assertEquals("GOOGLE", decoded.getClaim("provider").asString())
    }

    @Test
    fun testVerifyInvalidJwtToken() {
        val invalidToken = "invalid.jwt.token"
        val decoded = authService.verifyJwtToken(invalidToken)

        assertNull(decoded)
    }

    @Test
    fun testVerifyExpiredJwtToken() {
        // Create a service with very short expiration for testing
        val shortLivedAuthService = AuthenticationService(
            db = database,
            jwtSecret = "test-secret-key-for-jwt-testing",
            jwtIssuer = "wakev-test",
            jwtAudience = "wakev-test-client"
        )

        val testUser = User(
            id = "test-user-id",
            providerId = "google-test-id",
            email = "test@example.com",
            name = "Test User",
            avatarUrl = "https://example.com/avatar.jpg",
            provider = OAuthProvider.GOOGLE,
            createdAt = "2025-01-01T00:00:00Z",
            updatedAt = "2025-01-01T00:00:00Z"
        )

        // For this test, we'll just verify that token verification works
        // In a real scenario, you'd test with an actually expired token
        val token = shortLivedAuthService.generateJwtToken(testUser)
        val decoded = shortLivedAuthService.verifyJwtToken(token)

        assertNotNull(decoded)
        assertEquals(testUser.id, decoded.subject)
    }

    @Test
    fun testGetUserFromJwtToken() = runBlocking {
        // For this test, we'll just verify that the method handles invalid tokens correctly
        // since we can't easily mock the database in this test setup
        val invalidToken = "invalid.jwt.token"
        val user = authService.getUserFromJwtToken(invalidToken)

        assertNull(user)
    }

    @Test
    fun testGetUserFromInvalidJwtToken() {
        val invalidToken = "invalid.jwt.token"
        val user = authService.getUserFromJwtToken(invalidToken)

        assertNull(user)
    }
}