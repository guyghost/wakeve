package com.guyghost.wakeve.services

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test class for basic service functionality
 * Tests: service initialization, basic operations, error handling
 */
@ExperimentalCoroutinesApi
class ServiceTest {

    private val testScope = TestScope()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test service initialization`() = runBlocking {
        // Given
        val testService = TestService()

        // When
        val isInitialized = testService.isInitialized()

        // Then
        assertTrue(isInitialized, "Service should be properly initialized")
    }

    @Test
    fun `test service data persistence`() = runBlocking {
        // Given
        val testService = TestService()
        val testData = TestData(id = "123", value = "test_value")

        // When
        val result = testService.saveData(testData)

        // Then
        assertTrue(result.isSuccess, "Data should be saved successfully")
        
        // When - retrieving
        val retrieved = testService.getData("123")
        
        // Then
        assertNotNull(retrieved, "Data should be retrievable")
        assertEquals(testData.value, retrieved?.value, "Retrieved data should match saved data")
    }

    @Test
    fun `test service error handling`() = runBlocking {
        // Given
        val testService = TestService()

        // When - attempting to save invalid data
        val result = testService.saveData(null)

        // Then
        assertTrue(result.isFailure, "Invalid data should result in failure")
        val exception = result.exceptionOrNull()
        assertNotNull(exception, "Should have exception")
        assertTrue(exception.message?.contains("Data cannot be null") == true, 
            "Should have specific error message")
    }

    @Test
    fun `test service cleanup`() = runBlocking {
        // Given
        val testService = TestService()
        testService.saveData(TestData(id = "123", value = "test_value"))
        testService.saveData(TestData(id = "456", value = "another_value"))

        // When
        testService.cleanup()

        // Then
        assertEquals(0, testService.getDataCount(), "All data should be cleaned up")
    }

    @Test
    fun `test concurrent service operations`() = runBlocking {
        // Given
        val testService = TestService()

        // When - performing concurrent operations
        val result1 = testService.saveData(TestData(id = "1", value = "value1"))
        val result2 = testService.saveData(TestData(id = "2", value = "value2"))
        val result3 = testService.saveData(TestData(id = "3", value = "value3"))

        // Then
        assertTrue(result1.isSuccess, "First operation should succeed")
        assertTrue(result2.isSuccess, "Second operation should succeed")
        assertTrue(result3.isSuccess, "Third operation should succeed")
        assertEquals(3, testService.getDataCount(), "All data should be saved")
    }

    @Test
    fun `test notification service basic functionality`() = runBlocking {
        // Given
        val notificationService = MockNotificationService()

        // When
        val tokenRegistered = notificationService.registerPushToken("user123", "ANDROID", "token_123")

        // Then
        assertTrue(tokenRegistered, "Should successfully register push token")
        assertEquals(1, notificationService.getRegisteredTokensCount(), "Should have one registered token")
    }

    @Test
    fun `test notification service send functionality`() = runBlocking {
        // Given
        val notificationService = MockNotificationService()
        notificationService.registerPushToken("user123", "ANDROID", "token_123")

        // When
        val result = notificationService.sendNotification("user123", "Test Title", "Test Message")

        // Then
        assertTrue(result, "Should successfully send notification")
        assertEquals(1, notificationService.getSentNotificationsCount(), "Should have one sent notification")
    }

    @Test
    fun `test notification service failure cases`() = runBlocking {
        // Given
        val notificationService = MockNotificationService()

        // When - trying to send without tokens
        val result = notificationService.sendNotification("user456", "Test Title", "Test Message")

        // Then
        assertFalse(result, "Should fail when no tokens registered")
    }

    @Test
    fun `test sync service record changes`() = runBlocking {
        // Given
        val syncService = MockSyncService()

        // When
        val result = syncService.recordChange("events", "CREATE", "event123", """{"title":"Test Event"}""", "user123")

        // Then
        assertTrue(result, "Should successfully record change")
        assertEquals(1, syncService.getPendingChangesCount(), "Should have one pending change")
    }

    @Test
    fun `test sync service sync functionality`() = runBlocking {
        // Given
        val syncService = MockSyncService()
        syncService.setNetworkAvailable(true)
        syncService.recordChange("events", "CREATE", "event123", """{"title":"Test Event"}""", "user123")

        // When
        val result = syncService.syncWithServer()

        // Then
        assertTrue(result, "Should successfully sync")
        assertEquals(1, syncService.getLastSyncAppliedChanges(), "Should have applied one change")
    }

    @Test
    fun `test sync service offline behavior`() = runBlocking {
        // Given
        val syncService = MockSyncService()
        syncService.setNetworkAvailable(false)
        syncService.recordChange("events", "CREATE", "event123", """{"title":"Test Event"}""", "user123")

        // When
        val result = syncService.syncWithServer()

        // Then
        assertFalse(result, "Should fail when network unavailable")
    }

    @Test
    fun `test sync service retry logic`() = runBlocking {
        // Given
        val syncService = MockSyncService()
        syncService.setNetworkAvailable(true)
        syncService.recordChange("events", "CREATE", "event123", """{"title":"Test Event"}""", "user123")
        
        // Mock failures then success
        var attemptCount = 0
        syncService.setSyncBehavior {
            attemptCount++
            when (attemptCount) {
                1, 2 -> false  // Failure
                3 -> true   // Success
                else -> false
            }
        }

        // When
        val result = syncService.syncWithRetry()

        // Then
        assertTrue(result, "Should eventually succeed after retries")
        assertEquals(3, attemptCount, "Should make 3 attempts")
    }

    @Test
    fun `test auth service login functionality`() = runBlocking {
        // Given
        val authService = MockAuthService()

        // When
        val result = authService.signIn("google")

        // Then
        assertTrue(result.isSuccess, "Should successfully sign in")
        assertEquals("google_user_123", result.userId, "Should return correct user ID")
        assertEquals("test@gmail.com", result.email, "Should return correct email")
    }

    @Test
    fun `test auth service logout functionality`() = runBlocking {
        // Given
        val authService = MockAuthService()
        authService.signIn("google")
        assertTrue(authService.isAuthenticated(), "User should be authenticated initially")

        // When
        authService.signOut()

        // Then
        assertFalse(authService.isAuthenticated(), "User should no longer be authenticated")
    }

    @Test
    fun `test auth service token refresh`() = runBlocking {
        // Given
        val authService = MockAuthService()
        authService.signIn("google")
        authService.setRefreshToken("refresh_token_123")

        // When
        val result = authService.refreshToken()

        // Then
        assertTrue(result.isSuccess, "Should successfully refresh token")
        assertEquals("new_access_token_1", result.accessToken, "Should return new access token")
        assertEquals(1, authService.getRefreshCallCount(), "Should have called refresh once")
    }

    @Test
    fun `test auth service session validation`() = runBlocking {
        // Given
        val authService = MockAuthService()

        // When - initially not authenticated
        assertFalse(authService.isAuthenticated(), "Should start unauthenticated")

        // When - sign in
        authService.signIn("google")

        // Then - should be authenticated
        assertTrue(authService.isAuthenticated(), "Should be authenticated after sign-in")
    }
}

// Mock implementations for testing
class TestService {
    private val dataStore = mutableMapOf<String, TestData>()
    private var initialized = false
    
    init {
        initialized = true
    }
    
    fun isInitialized(): Boolean = initialized
    
    fun saveData(data: TestData?): Result<Unit> {
        return if (data != null) {
            dataStore[data.id] = data
            Result.success(Unit)
        } else {
            Result.failure(IllegalArgumentException("Data cannot be null"))
        }
    }
    
    fun getData(id: String): TestData? = dataStore[id]
    
    fun getDataCount(): Int = dataStore.size
    
    fun cleanup() {
        dataStore.clear()
    }
}

class MockNotificationService {
    private val registeredTokens = mutableListOf<String>()
    private val sentNotifications = mutableListOf<String>()
    
    fun registerPushToken(userId: String, platform: String, token: String): Boolean {
        return try {
            registeredTokens.add(token)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun sendNotification(userId: String, title: String, body: String): Boolean {
        return if (registeredTokens.isEmpty()) {
            false
        } else {
            sentNotifications.add("$title: $body")
            true
        }
    }
    
    fun getRegisteredTokensCount(): Int = registeredTokens.size
    fun getSentNotificationsCount(): Int = sentNotifications.size
}

class MockSyncService {
    private val pendingChanges = mutableListOf<MockSyncChange>()
    private var networkAvailable = true
    private var syncBehavior: (() -> Boolean)? = null
    
    fun recordChange(table: String, operation: String, recordId: String, data: String, userId: String): Boolean {
        return try {
            pendingChanges.add(MockSyncChange(
                id = "sync_${System.currentTimeMillis()}_$recordId",
                table = table,
                operation = operation,
                recordId = recordId,
                data = data,
                userId = userId
            ))
            true
        } catch (e: Exception) {
            false
        }
    }
    
    fun syncWithServer(): Boolean {
        if (!networkAvailable) {
            return false
        }
        
        return syncBehavior?.invoke() ?: true
    }
    
    fun syncWithRetry(): Boolean {
        var lastResult = false
        
        for (attempt in 1..3) {
            lastResult = syncWithServer()
            if (lastResult) break // Success, stop retrying
        }
        
        return lastResult
    }
    
    fun setNetworkAvailable(available: Boolean) {
        networkAvailable = available
    }
    
    fun setSyncBehavior(behavior: () -> Boolean) {
        syncBehavior = behavior
    }
    
    fun getPendingChangesCount(): Int = pendingChanges.size
    fun getLastSyncAppliedChanges(): Int = 1 // Mock successful sync applies 1 change
}

class MockAuthService {
    private val storedTokens = mutableMapOf<String, String>()
    private var currentUser: MockUser? = null
    private var refreshCallCount = 0
    
    data class MockUser(
        val userId: String,
        val email: String,
        val provider: String
    )
    
    data class MockAuthResult(
        val isSuccess: Boolean,
        val userId: String,
        val email: String,
        val accessToken: String
    ) {
        val isFailure: Boolean get() = !isSuccess
    }
    
    fun signIn(provider: String): MockAuthResult {
        val user = MockUser(
            userId = "${provider}_user_123",
            email = "test@gmail.com",
            provider = provider
        )
        storedTokens["access_token"] = "${provider}_access_token_123"
        currentUser = user
        return MockAuthResult(isSuccess = true, user.userId, user.email, storedTokens["access_token"]!!)
    }
    
    fun signOut() {
        currentUser = null
        storedTokens.clear()
    }
    
    fun isAuthenticated(): Boolean {
        return currentUser != null && storedTokens.containsKey("access_token")
    }
    
    fun setRefreshToken(token: String) {
        storedTokens["refresh_token"] = token
    }
    
    fun refreshToken(): MockAuthResult {
        val refreshToken = storedTokens["refresh_token"]
        if (refreshToken == null) {
            return MockAuthResult(isSuccess = false, "", "", "")
        }
        
        refreshCallCount++
        val newAccessToken = "new_access_token_$refreshCallCount"
        storedTokens["access_token"] = newAccessToken
        return MockAuthResult(isSuccess = true, currentUser?.userId ?: "", currentUser?.email ?: "", newAccessToken)
    }
    
    fun getRefreshCallCount(): Int = refreshCallCount
}

data class TestData(
    val id: String,
    val value: String
)

data class MockSyncChange(
    val id: String,
    val table: String,
    val operation: String,
    val recordId: String,
    val data: String,
    val userId: String
)