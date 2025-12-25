package com.guyghost.wakeve

import com.guyghost.wakeve.database.WakevDb
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Unit tests for SessionRepository.
 *
 * Tests cover:
 * - Session creation and retrieval
 * - Session revocation (single and bulk)
 * - Token blacklist management
 * - Device fingerprint tracking
 * - Session cleanup
 */
class SessionRepositoryTest {

    private lateinit var database: WakevDb
    private lateinit var repository: SessionRepository

    @BeforeTest
    fun setup() {
        // Initialize in-memory database
        database = DatabaseProvider.getDatabase(TestDatabaseFactory())
        repository = SessionRepository(database)
    }

    @AfterTest
    fun teardown() {
        database.close()
    }

    // ============================================
    // Session Management Tests
    // ============================================

    @Test
    fun `createSession should store session in database`() = runTest {
        // When
        val result = repository.createSession(
            userId = "user-123",
            deviceId = "device-1",
            deviceName = "iPhone 14",
            jwtToken = "jwt-token-abc",
            refreshToken = "refresh-token-xyz",
            ipAddress = "192.168.1.100",
            userAgent = "iOS/16.0",
            expiresAt = (System.currentTimeMillis() + 3600000).toString()
        )

        // Then
        assertTrue(result.isSuccess)
        val sessionId = result.getOrNull()
        assertNotNull(sessionId)
        assertTrue(sessionId.startsWith("session-"))
    }

    @Test
    fun `getActiveSessionsForUser should return only active sessions`() = runTest {
        // Given
        val userId = "user-123"
        val expiresAt = (System.currentTimeMillis() + 3600000).toString()

        repository.createSession(
            userId = userId,
            deviceId = "device-1",
            deviceName = "Device 1",
            jwtToken = "token-1",
            refreshToken = "refresh-1",
            expiresAt = expiresAt
        )

        repository.createSession(
            userId = userId,
            deviceId = "device-2",
            deviceName = "Device 2",
            jwtToken = "token-2",
            refreshToken = "refresh-2",
            expiresAt = expiresAt
        )

        // When
        val result = repository.getActiveSessionsForUser(userId)

        // Then
        assertTrue(result.isSuccess)
        val sessions = result.getOrNull()
        assertNotNull(sessions)
        assertEquals(2, sessions.size)
        assertTrue(sessions.all { it.userId == userId })
        assertTrue(sessions.all { it.status == "active" })
    }

    @Test
    fun `getSessionById should return correct session`() = runTest {
        // Given
        val createResult = repository.createSession(
            userId = "user-123",
            deviceId = "device-1",
            deviceName = "Test Device",
            jwtToken = "token",
            refreshToken = "refresh",
            expiresAt = (System.currentTimeMillis() + 3600000).toString()
        )
        val sessionId = createResult.getOrNull()!!

        // When
        val result = repository.getSessionById(sessionId)

        // Then
        assertTrue(result.isSuccess)
        val session = result.getOrNull()
        assertNotNull(session)
        assertEquals(sessionId, session.id)
        assertEquals("user-123", session.userId)
        assertEquals("Test Device", session.deviceName)
    }

    @Test
    fun `revokeSession should mark session as revoked`() = runTest {
        // Given
        val createResult = repository.createSession(
            userId = "user-123",
            deviceId = "device-1",
            deviceName = "Device",
            jwtToken = "token",
            refreshToken = "refresh",
            expiresAt = (System.currentTimeMillis() + 3600000).toString()
        )
        val sessionId = createResult.getOrNull()!!

        // When
        val revokeResult = repository.revokeSession(sessionId)

        // Then
        assertTrue(revokeResult.isSuccess)

        // Verify session is revoked
        val activeResult = repository.getActiveSessionsForUser("user-123")
        val activeSessions = activeResult.getOrNull()!!
        assertEquals(0, activeSessions.size)
    }

    @Test
    fun `revokeAllUserSessions should revoke all sessions for user`() = runTest {
        // Given
        val userId = "user-123"
        val expiresAt = (System.currentTimeMillis() + 3600000).toString()

        repository.createSession(
            userId = userId,
            deviceId = "device-1",
            deviceName = "Device 1",
            jwtToken = "token-1",
            refreshToken = "refresh-1",
            expiresAt = expiresAt
        )

        repository.createSession(
            userId = userId,
            deviceId = "device-2",
            deviceName = "Device 2",
            jwtToken = "token-2",
            refreshToken = "refresh-2",
            expiresAt = expiresAt
        )

        // When
        val result = repository.revokeAllUserSessions(userId)

        // Then
        assertTrue(result.isSuccess)

        // Verify all sessions revoked
        val activeResult = repository.getActiveSessionsForUser(userId)
        val activeSessions = activeResult.getOrNull()!!
        assertEquals(0, activeSessions.size)
    }

    @Test
    fun `revokeAllOtherSessions should keep current session active`() = runTest {
        // Given
        val userId = "user-123"
        val expiresAt = (System.currentTimeMillis() + 3600000).toString()

        val session1Result = repository.createSession(
            userId = userId,
            deviceId = "device-1",
            deviceName = "Current Device",
            jwtToken = "token-1",
            refreshToken = "refresh-1",
            expiresAt = expiresAt
        )
        val currentSessionId = session1Result.getOrNull()!!

        repository.createSession(
            userId = userId,
            deviceId = "device-2",
            deviceName = "Other Device",
            jwtToken = "token-2",
            refreshToken = "refresh-2",
            expiresAt = expiresAt
        )

        // When
        val result = repository.revokeAllOtherSessions(userId, currentSessionId)

        // Then
        assertTrue(result.isSuccess)

        // Verify only current session is active
        val activeResult = repository.getActiveSessionsForUser(userId)
        val activeSessions = activeResult.getOrNull()!!
        assertEquals(1, activeSessions.size)
        assertEquals(currentSessionId, activeSessions[0].id)
    }

    @Test
    fun `countActiveSessions should return correct count`() = runTest {
        // Given
        val userId = "user-123"
        val expiresAt = (System.currentTimeMillis() + 3600000).toString()

        repository.createSession(
            userId = userId,
            deviceId = "device-1",
            deviceName = "Device 1",
            jwtToken = "token-1",
            refreshToken = "refresh-1",
            expiresAt = expiresAt
        )

        repository.createSession(
            userId = userId,
            deviceId = "device-2",
            deviceName = "Device 2",
            jwtToken = "token-2",
            refreshToken = "refresh-2",
            expiresAt = expiresAt
        )

        // When
        val result = repository.countActiveSessions(userId)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(2L, result.getOrNull())
    }

    // ============================================
    // Token Blacklist Tests
    // ============================================

    @Test
    fun `isTokenBlacklisted should return false for non-blacklisted token`() = runTest {
        // When
        val result = repository.isTokenBlacklisted("non-blacklisted-token")

        // Then
        assertTrue(result.isSuccess)
        assertFalse(result.getOrNull()!!)
    }

    @Test
    fun `isTokenBlacklisted should return true for blacklisted token`() = runTest {
        // Given
        val expiresAt = (System.currentTimeMillis() + 3600000).toString()
        val createResult = repository.createSession(
            userId = "user-123",
            deviceId = "device-1",
            deviceName = "Device",
            jwtToken = "blacklisted-token",
            refreshToken = "refresh",
            expiresAt = expiresAt
        )
        val sessionId = createResult.getOrNull()!!

        // Revoke session to blacklist token
        repository.revokeSession(sessionId)

        // When
        val result = repository.isTokenBlacklisted("blacklisted-token")

        // Then
        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull()!!)
    }

    // ============================================
    // Device Fingerprint Tests
    // ============================================

    @Test
    fun `registerDevice should create new device`() = runTest {
        // When
        val result = repository.registerDevice(
            userId = "user-123",
            deviceId = "device-1",
            deviceName = "iPhone 14",
            deviceType = "mobile",
            fingerprintHash = "fingerprint-hash-abc",
            trusted = false
        )

        // Then
        assertTrue(result.isSuccess)
        val deviceDbId = result.getOrNull()
        assertNotNull(deviceDbId)
    }

    @Test
    fun `registerDevice should update existing device last seen`() = runTest {
        // Given
        val fingerprintHash = "fingerprint-hash-abc"
        val firstResult = repository.registerDevice(
            userId = "user-123",
            deviceId = "device-1",
            deviceName = "iPhone 14",
            deviceType = "mobile",
            fingerprintHash = fingerprintHash,
            trusted = false
        )
        val firstDeviceId = firstResult.getOrNull()!!

        // When - register same fingerprint again
        val secondResult = repository.registerDevice(
            userId = "user-123",
            deviceId = "device-1",
            deviceName = "iPhone 14",
            deviceType = "mobile",
            fingerprintHash = fingerprintHash,
            trusted = false
        )

        // Then - should return same device ID
        assertTrue(secondResult.isSuccess)
        assertEquals(firstDeviceId, secondResult.getOrNull())
    }

    @Test
    fun `getDevicesForUser should return all user devices`() = runTest {
        // Given
        val userId = "user-123"

        repository.registerDevice(
            userId = userId,
            deviceId = "device-1",
            deviceName = "iPhone 14",
            deviceType = "mobile",
            fingerprintHash = "hash-1",
            trusted = false
        )

        repository.registerDevice(
            userId = userId,
            deviceId = "device-2",
            deviceName = "MacBook Pro",
            deviceType = "desktop",
            fingerprintHash = "hash-2",
            trusted = true
        )

        // When
        val result = repository.getDevicesForUser(userId)

        // Then
        assertTrue(result.isSuccess)
        val devices = result.getOrNull()!!
        assertEquals(2, devices.size)
        assertTrue(devices.all { it.userId == userId })
    }

    @Test
    fun `updateDeviceTrust should change trust status`() = runTest {
        // Given
        val registerResult = repository.registerDevice(
            userId = "user-123",
            deviceId = "device-1",
            deviceName = "iPhone",
            deviceType = "mobile",
            fingerprintHash = "hash-1",
            trusted = false
        )
        val deviceId = registerResult.getOrNull()!!

        // When
        val updateResult = repository.updateDeviceTrust(deviceId, trusted = true)

        // Then
        assertTrue(updateResult.isSuccess)

        // Verify trust status changed
        val devicesResult = repository.getDevicesForUser("user-123")
        val devices = devicesResult.getOrNull()!!
        val device = devices.find { it.id == deviceId }
        assertNotNull(device)
        assertTrue(device.trusted)
    }
}

/**
 * Test database factory for in-memory SQLDelight database.
 */
expect class TestDatabaseFactory() {
    fun createDriver(): app.cash.sqldelight.db.SqlDriver
}
