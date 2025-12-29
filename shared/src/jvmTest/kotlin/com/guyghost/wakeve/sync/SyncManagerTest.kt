package com.guyghost.wakeve.sync

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.UserRepository
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.SyncResponse
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test network detector that can be controlled
 */
class TestNetworkStatusDetector : NetworkStatusDetector {
    private val _isNetworkAvailable = MutableStateFlow(true)
    override val isNetworkAvailable: StateFlow<Boolean> = _isNetworkAvailable

    fun setNetworkAvailable(available: Boolean) {
        _isNetworkAvailable.value = available
    }
}

/**
 * Test HTTP client that returns predefined responses
 */
class TestSyncHttpClient(var response: SyncResponse) : SyncHttpClient {
    override suspend fun sync(requestJson: String, authToken: String): Result<String> {
        // Return the response as JSON
        val jsonResponse = kotlinx.serialization.json.Json.encodeToString(SyncResponse.serializer(), response)
        return Result.success(jsonResponse)
    }
}

/**
 * Test HTTP client that can simulate failures and successes
 */
class FailingSyncHttpClient(
    private val responses: List<Result<SyncResponse>>
) : SyncHttpClient {
    private var callCount = 0

    override suspend fun sync(requestJson: String, authToken: String): Result<String> {
        val response = responses.getOrElse(callCount) { responses.last() }
        callCount++
        return response.map { kotlinx.serialization.json.Json.encodeToString(SyncResponse.serializer(), it) }
    }

    fun getCallCount(): Int = callCount
}

class SyncManagerTest {

    private lateinit var database: com.guyghost.wakeve.database.WakevDb
    private lateinit var eventRepository: DatabaseEventRepository
    private lateinit var userRepository: UserRepository
    private lateinit var networkDetector: TestNetworkStatusDetector
    private lateinit var httpClient: TestSyncHttpClient
    private lateinit var syncManager: SyncManager

    @BeforeTest
    fun setup() {
        // Create a fresh database for each test to ensure isolation
        database = createFreshTestDatabase()
        userRepository = UserRepository(database)
        networkDetector = TestNetworkStatusDetector()
        httpClient = TestSyncHttpClient(
            SyncResponse(
                success = true,
                appliedChanges = 1,
                conflicts = emptyList(),
                serverTimestamp = "2025-11-19T12:00:00Z",
                message = "Test sync successful"
            )
        )

        // Create sync manager without passing it to eventRepository to avoid circular dependency in tests
        syncManager = SyncManager(
            database = database,
            eventRepository = DatabaseEventRepository(database),
            userRepository = userRepository,
            networkDetector = networkDetector,
            httpClient = httpClient,
            authTokenProvider = { "test-token" }
        )
    }

    @Test
    fun testRecordLocalChange() = runBlocking {
        // Disable network to prevent auto-sync when recording change
        networkDetector.setNetworkAvailable(false)

        val result = syncManager.recordLocalChange(
            table = "events",
            operation = SyncOperation.CREATE,
            recordId = "test-event-1",
            data = """{"id":"test-event-1","title":"Test Event"}""",
            userId = "user-1"
        )

        assertTrue(result.isSuccess)
        assertTrue(syncManager.hasPendingChanges())
    }

    @Test
    fun testSyncWithNetworkAvailable() = runBlocking {
        // Disable network first to prevent auto-sync when recording change
        networkDetector.setNetworkAvailable(false)

        // Record a change
        syncManager.recordLocalChange(
            table = "events",
            operation = SyncOperation.CREATE,
            recordId = "test-event-1",
            data = """{"id":"test-event-1","title":"Test Event"}""",
            userId = "user-1"
        )

        // Verify change is pending
        assertTrue(syncManager.hasPendingChanges())

        // Enable network
        networkDetector.setNetworkAvailable(true)

        // Trigger sync manually
        val result = syncManager.triggerSync()

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertTrue(response.success)
        assertEquals(1, response.appliedChanges)
    }

    @Test
    fun testSyncFailsWithoutNetwork() = runBlocking {
        // Record a change
        syncManager.recordLocalChange(
            table = "events",
            operation = SyncOperation.CREATE,
            recordId = "test-event-1",
            data = """{"id":"test-event-1","title":"Test Event"}""",
            userId = "user-1"
        )

        // Disable network
        networkDetector.setNetworkAvailable(false)

        // Trigger sync
        val result = syncManager.triggerSync()

        assertTrue(result.isFailure)
        assertEquals("Network not available", result.exceptionOrNull()?.message)
    }

    @Test
    fun testSyncFailsWithoutAuthToken() = runBlocking {
        // Create sync manager without auth token
        val syncManagerNoAuth = SyncManager(
            database = database,
            eventRepository = DatabaseEventRepository(database),
            userRepository = userRepository,
            networkDetector = networkDetector,
            httpClient = httpClient,
            authTokenProvider = { null }
        )

        // Record a change
        syncManagerNoAuth.recordLocalChange(
            table = "events",
            operation = SyncOperation.CREATE,
            recordId = "test-event-1",
            data = """{"id":"test-event-1","title":"Test Event"}""",
            userId = "user-1"
        )

        // Enable network
        networkDetector.setNetworkAvailable(true)

        // Trigger sync
        val result = syncManagerNoAuth.triggerSync()

        assertTrue(result.isFailure)
        assertEquals("No auth token available", result.exceptionOrNull()?.message)
    }

    @Test
    fun testNoChangesToSync() = runBlocking {
        // Ensure network is available
        networkDetector.setNetworkAvailable(true)

        // Trigger sync without any changes
        val result = syncManager.triggerSync()

        assertTrue(result.isSuccess)
        val response = result.getOrThrow()
        assertTrue(response.success)
        assertEquals(0, response.appliedChanges)
        assertEquals("No changes to sync", response.message)
    }

    @Test
    fun testRetryMechanismSuccessAfterFailure() = runBlocking {
        // Create HTTP client that fails twice then succeeds
        // We provide enough failures to handle potential auto-sync calls
        val failingHttpClient = FailingSyncHttpClient(listOf(
            Result.failure(Exception("Network error")),
            Result.failure(Exception("Server error")),
            Result.failure(Exception("Extra failure 1")),
            Result.failure(Exception("Extra failure 2")),
            Result.failure(Exception("Extra failure 3")),
            Result.success(SyncResponse(
                success = true,
                appliedChanges = 1,
                conflicts = emptyList(),
                serverTimestamp = "2025-11-19T12:00:00Z",
                message = "Sync successful after retries"
            )),
            // Extra success responses for any additional calls
            Result.success(SyncResponse(
                success = true,
                appliedChanges = 0,
                conflicts = emptyList(),
                serverTimestamp = "2025-11-19T12:00:00Z",
                message = "No changes"
            ))
        ))

        // Create a dedicated network detector for this test
        val retryNetworkDetector = TestNetworkStatusDetector().apply {
            setNetworkAvailable(false) // Start with network unavailable
        }

        // Create sync manager with retry configuration
        val retrySyncManager = SyncManager(
            database = database,
            eventRepository = DatabaseEventRepository(database),
            userRepository = userRepository,
            networkDetector = retryNetworkDetector,
            httpClient = failingHttpClient,
            authTokenProvider = { "test-token" },
            maxRetries = 5, // Enough retries to eventually succeed
            baseRetryDelayMs = 10L // Short delay for tests
        )

        // Record a change while network is unavailable
        retrySyncManager.recordLocalChange(
            table = "events",
            operation = SyncOperation.CREATE,
            recordId = "test-event-1",
            data = """{"id":"test-event-1","title":"Test Event"}""",
            userId = "user-1"
        )

        // Enable network and manually trigger sync
        retryNetworkDetector.setNetworkAvailable(true)
        
        // Trigger sync - it should eventually succeed after retries
        val result = retrySyncManager.triggerSync()

        // Verify the sync eventually succeeds
        assertTrue(result.isSuccess, "Sync should succeed after retries")
        val response = result.getOrThrow()
        assertTrue(response.success, "Response should indicate success")
        
        // The HTTP client should have been called multiple times (retries happened)
        assertTrue(failingHttpClient.getCallCount() >= 3, "Should have retried at least a few times")
    }

    @Test
    fun testRetryMechanismFailsAfterMaxRetries() = runBlocking {
        // Create HTTP client that always fails
        val failingHttpClient = FailingSyncHttpClient(listOf(
            Result.failure(Exception("Network error")),
            Result.failure(Exception("Server error")),
            Result.failure(Exception("Timeout")),
            Result.failure(Exception("Connection failed")),
            Result.failure(Exception("Extra failure 1")),
            Result.failure(Exception("Extra failure 2")),
            Result.failure(Exception("Extra failure 3")),
            Result.failure(Exception("Extra failure 4")),
            Result.failure(Exception("Extra failure 5"))
        ))

        // Create a dedicated network detector for this test
        val retryNetworkDetector = TestNetworkStatusDetector().apply {
            setNetworkAvailable(false) // Start with network unavailable
        }

        // Create sync manager with limited retries
        val retrySyncManager = SyncManager(
            database = database,
            eventRepository = DatabaseEventRepository(database),
            userRepository = userRepository,
            networkDetector = retryNetworkDetector,
            httpClient = failingHttpClient,
            authTokenProvider = { "test-token" },
            maxRetries = 2, // Only 2 retries = 3 total attempts per sync call
            baseRetryDelayMs = 10L // Short delay for tests
        )

        // Record a change while network is unavailable
        retrySyncManager.recordLocalChange(
            table = "events",
            operation = SyncOperation.CREATE,
            recordId = "test-event-1",
            data = """{"id":"test-event-1","title":"Test Event"}""",
            userId = "user-1"
        )

        // Enable network and manually trigger sync
        retryNetworkDetector.setNetworkAvailable(true)

        // Trigger sync - should fail after max retries
        val result = retrySyncManager.triggerSync()

        // Verify the sync fails
        assertTrue(result.isFailure, "Sync should fail after max retries")
        
        // The HTTP client should have been called multiple times
        assertTrue(failingHttpClient.getCallCount() >= 3, "Should have made at least 3 attempts (initial + 2 retries)")
        
        // Verify there's an error message
        assertTrue(result.exceptionOrNull() != null, "Should have an exception")
    }

    @Test
    fun testSyncStatusDuringRetries() = runBlocking {
        // Create HTTP client that fails once then succeeds
        val failingHttpClient = FailingSyncHttpClient(listOf(
            Result.failure(Exception("Network error")),
            Result.success(SyncResponse(
                success = true,
                appliedChanges = 1,
                conflicts = emptyList(),
                serverTimestamp = "2025-11-19T12:00:00Z",
                message = "Sync successful"
            ))
        ))

        // Create sync manager with retry configuration
        val retrySyncManager = SyncManager(
            database = database,
            eventRepository = DatabaseEventRepository(database),
            userRepository = userRepository,
            networkDetector = networkDetector,
            httpClient = failingHttpClient,
            authTokenProvider = { "test-token" },
            maxRetries = 3,
            baseRetryDelayMs = 10L // Short delay for tests
        )

        // Record a change
        retrySyncManager.recordLocalChange(
            table = "events",
            operation = SyncOperation.CREATE,
            recordId = "test-event-1",
            data = """{"id":"test-event-1","title":"Test Event"}""",
            userId = "user-1"
        )

        // Ensure network is available
        networkDetector.setNetworkAvailable(true)

        // Trigger sync
        val result = retrySyncManager.triggerSync()

        assertTrue(result.isSuccess)
        // The sync status should eventually be Idle after successful retry
        assertEquals(SyncStatus.Idle, retrySyncManager.syncStatus.value)
    }
}