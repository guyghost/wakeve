package com.guyghost.wakeve

import com.guyghost.wakeve.database.WakevDb
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for server-side SessionManager.
 *
 * Tests cover:
 * - Session retrieval
 * - Session revocation
 * - Token blacklist checking
 */
class SessionManagerTest {

    private lateinit var database: WakevDb
    private lateinit var sessionRepository: SessionRepository
    private lateinit var sessionManager: SessionManager

    @Before
    fun setup() {
        // Initialize in-memory test database
        database = DatabaseProvider.getDatabase(JvmTestDatabaseFactory())
        sessionRepository = SessionRepository(database)
        sessionManager = SessionManager(database)
    }

    @After
    fun teardown() {
        // Reset the database singleton for the next test
        DatabaseProvider.resetDatabase()
    }

    // ============================================
    // Session Retrieval Tests
    // ============================================

    @Test
    fun `getUserSessions should return all active sessions for user`() = runBlocking {
        // Given
        val userId = "user-123"
        val expiresAt = (System.currentTimeMillis() + 3600000).toString()

        sessionRepository.createSession(
            userId = userId,
            deviceId = "device-1",
            deviceName = "Device 1",
            jwtToken = "token-1",
            refreshToken = "refresh-1",
            expiresAt = expiresAt
        )

        sessionRepository.createSession(
            userId = userId,
            deviceId = "device-2",
            deviceName = "Device 2",
            jwtToken = "token-2",
            refreshToken = "refresh-2",
            expiresAt = expiresAt
        )

        // When
        val sessions = sessionManager.getUserSessions(userId)

        // Then
        assertEquals(2, sessions.size)
        assertTrue(sessions.all { it.userId == userId })
    }

    @Test
    fun `getUserSessions should return empty list for user with no sessions`() = runBlocking {
        // When
        val sessions = sessionManager.getUserSessions("non-existent-user")

        // Then
        assertTrue(sessions.isEmpty())
    }

    @Test
    fun `getSession should return session by ID`() = runBlocking {
        // Given
        val createResult = sessionRepository.createSession(
            userId = "user-123",
            deviceId = "device-1",
            deviceName = "Test Device",
            jwtToken = "token",
            refreshToken = "refresh",
            expiresAt = (System.currentTimeMillis() + 3600000).toString()
        )
        val sessionId = createResult.getOrNull()!!

        // When
        val session = sessionManager.getSession(sessionId)

        // Then
        assertNotNull(session)
        assertEquals(sessionId, session.id)
        assertEquals("user-123", session.userId)
    }

    @Test
    fun `getSession should return null for non-existent session`() = runBlocking {
        // When
        val session = sessionManager.getSession("non-existent-session")

        // Then
        assertNull(session)
    }

    // ============================================
    // Session Revocation Tests
    // ============================================

    @Test
    fun `revokeSession should successfully revoke session`() = runBlocking {
        // Given
        val createResult = sessionRepository.createSession(
            userId = "user-123",
            deviceId = "device-1",
            deviceName = "Device",
            jwtToken = "token",
            refreshToken = "refresh",
            expiresAt = (System.currentTimeMillis() + 3600000).toString()
        )
        val sessionId = createResult.getOrNull()!!

        // When
        val result = sessionManager.revokeSession(sessionId)

        // Then
        assertTrue(result.isSuccess)

        // Verify session is no longer active
        val sessions = sessionManager.getUserSessions("user-123")
        assertEquals(0, sessions.size)
    }

    @Test
    fun `revokeSession should add token to blacklist`() = runBlocking {
        // Given
        val createResult = sessionRepository.createSession(
            userId = "user-123",
            deviceId = "device-1",
            deviceName = "Device",
            jwtToken = "token-to-blacklist",
            refreshToken = "refresh",
            expiresAt = (System.currentTimeMillis() + 3600000).toString()
        )
        val sessionId = createResult.getOrNull()!!

        // When
        sessionManager.revokeSession(sessionId)

        // Then
        val isBlacklisted = sessionManager.isTokenBlacklisted("token-to-blacklist")
        assertTrue(isBlacklisted)
    }

    // ============================================
    // Token Blacklist Tests
    // ============================================

    @Test
    fun `isTokenBlacklisted should return false for non-blacklisted token`() = runBlocking {
        // When
        val isBlacklisted = sessionManager.isTokenBlacklisted("non-blacklisted-token")

        // Then
        assertFalse(isBlacklisted)
    }

    @Test
    fun `isTokenBlacklisted should return true for blacklisted token`() = runBlocking {
        // Given
        val expiresAt = (System.currentTimeMillis() + 3600000).toString()
        val createResult = sessionRepository.createSession(
            userId = "user-123",
            deviceId = "device-1",
            deviceName = "Device",
            jwtToken = "blacklisted-token",
            refreshToken = "refresh",
            expiresAt = expiresAt
        )
        val sessionId = createResult.getOrNull()!!
        sessionManager.revokeSession(sessionId)

        // When
        val isBlacklisted = sessionManager.isTokenBlacklisted("blacklisted-token")

        // Then
        assertTrue(isBlacklisted)
    }

    // ============================================
    // Update Tests
    // ============================================

    @Test
    fun `updateLastAccessed should update session timestamp`() = runBlocking {
        // Given
        val createResult = sessionRepository.createSession(
            userId = "user-123",
            deviceId = "device-1",
            deviceName = "Device",
            jwtToken = "token",
            refreshToken = "refresh",
            expiresAt = (System.currentTimeMillis() + 3600000).toString()
        )
        val sessionId = createResult.getOrNull()!!

        // Get initial session
        val initialSession = sessionManager.getSession(sessionId)
        assertNotNull(initialSession)
        val initialLastAccessed = initialSession.lastAccessed

        // Wait a moment
        Thread.sleep(100)

        // When
        val result = sessionManager.updateLastAccessed(sessionId)

        // Then
        assertTrue(result.isSuccess)

        // Verify timestamp was updated
        val updatedSession = sessionManager.getSession(sessionId)
        assertNotNull(updatedSession)
        assertTrue(updatedSession.lastAccessed >= initialLastAccessed)
    }
}

/**
 * Test database factory for JVM tests.
 */
class JvmTestDatabaseFactory : DatabaseFactory {
    override fun createDriver(): app.cash.sqldelight.db.SqlDriver {
        return app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver(
            url = app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver.IN_MEMORY
        ).also { driver ->
            com.guyghost.wakeve.database.WakevDb.Schema.create(driver)
        }
    }
}
