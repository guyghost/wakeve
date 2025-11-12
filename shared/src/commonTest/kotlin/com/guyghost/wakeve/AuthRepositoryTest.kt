package com.guyghost.wakeve

import com.guyghost.wakeve.models.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.assertEquals

/**
 * Tests pour AuthRepository
 */
class AuthRepositoryTest {
    
    @Test
    fun testUserSessionIsValidWhenNotExpired() {
        val futureTime = getCurrentTimeMillis() + (3600 * 1000) // 1 heure dans le futur
        val session = UserSession(
            user = createTestUser(),
            accessToken = "token",
            refreshToken = "refresh",
            expiresAt = futureTime,
            createdAt = getCurrentTimeMillis()
        )
        
        assertTrue(session.isValid())
        assertTrue(!session.isAccessTokenExpired())
    }
    
    @Test
    fun testUserSessionIsExpiredWhenTimeExceeded() {
        val pastTime = getCurrentTimeMillis() - (3600 * 1000) // 1 heure dans le passé
        val session = UserSession(
            user = createTestUser(),
            accessToken = "token",
            refreshToken = "refresh",
            expiresAt = pastTime,
            createdAt = getCurrentTimeMillis() - (7200 * 1000)
        )
        
        assertTrue(session.isAccessTokenExpired())
        assertTrue(!session.isValid())
    }
    
    @Test
    fun testUserSessionExpirationBufferOf5Minutes() {
        // Token expire dans 3 minutes (moins que le buffer de 5 minutes)
        val almostExpiredTime = getCurrentTimeMillis() + (3 * 60 * 1000)
        val session = UserSession(
            user = createTestUser(),
            accessToken = "token",
            refreshToken = "refresh",
            expiresAt = almostExpiredTime,
            createdAt = getCurrentTimeMillis()
        )
        
        // Devrait être considéré comme expiré à cause du buffer
        assertTrue(session.isAccessTokenExpired())
    }
    
    @Test
    fun testAuthErrorTypes() {
        val invalidCredentialsError = AuthError.InvalidCredentials("Bad email/password")
        assertEquals("Bad email/password", invalidCredentialsError.message)
        
        val userNotFoundError = AuthError.UserNotFound("User does not exist")
        assertEquals("User does not exist", userNotFoundError.message)
        
        val emailExistsError = AuthError.EmailAlreadyExists("Email already exists")
        assertEquals("Email already exists", emailExistsError.message)
        
        val tokenError = AuthError.InvalidToken("Invalid token")
        assertEquals("Invalid token", tokenError.message)
        
        val expiredError = AuthError.TokenExpired("Token expired")
        assertEquals("Token expired", expiredError.message)
    }
    
    // MARK: - Test Helpers
    
    private fun createTestUser(): User {
        return User(
            id = "user-1",
            email = "test@example.com",
            name = "Test User",
            avatar = null,
            provider = AuthProvider.LOCAL,
            createdAt = System.currentTimeMillis().toString()
        )
    }
}
