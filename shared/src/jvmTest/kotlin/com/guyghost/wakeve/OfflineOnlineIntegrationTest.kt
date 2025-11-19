package com.guyghost.wakeve

import com.guyghost.wakeve.models.SyncOperation
import com.guyghost.wakeve.models.SyncResponse
import com.guyghost.wakeve.sync.NetworkStatusDetector
import com.guyghost.wakeve.sync.SyncHttpClient
import com.guyghost.wakeve.sync.SyncManager
import com.guyghost.wakeve.sync.SyncStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import kotlin.test.*

/**
 * Integration test for offline/online scenarios.
 * Tests data persistence, sync recovery, and state transitions.
 */
class OfflineOnlineIntegrationTest {

    private lateinit var database: com.guyghost.wakeve.database.WakevDb
    private lateinit var eventRepository: DatabaseEventRepository
    private lateinit var userRepository: UserRepository
    private lateinit var networkDetector: TestNetworkStatusDetector
    private lateinit var httpClient: TestSyncHttpClient
    private lateinit var syncManager: SyncManager

    @BeforeTest
    fun setup() {
        DatabaseProvider.resetDatabase()
        database = DatabaseProvider.getDatabase(TestDatabaseFactory())
        userRepository = UserRepository(database)
        eventRepository = DatabaseEventRepository(database)
        networkDetector = TestNetworkStatusDetector()
        httpClient = TestSyncHttpClient(
            SyncResponse(
                success = true,
                appliedChanges = 0, // Will be set per test
                conflicts = emptyList(),
                serverTimestamp = "2025-11-19T12:00:00Z",
                message = "Sync successful"
            )
        )

        syncManager = SyncManager(
            database = database,
            eventRepository = eventRepository,
            userRepository = userRepository,
            networkDetector = networkDetector,
            httpClient = httpClient,
            authTokenProvider = { "test-token" }
        )
    }

    @Test
    fun testOfflineDataCreationAndOnlineSync() = runBlocking {
        // Start offline
        networkDetector.setNetworkAvailable(false)

        // Create event offline
        val event = models.Event(
            id = "offline-event-1",
            title = "Offline Created Event",
            description = "Created while offline",
            organizerId = "user-1",
            participants = listOf("user-1"),
            proposedSlots = listOf(
                models.TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
            ),
            deadline = "2025-11-25T18:00:00Z",
            status = models.EventStatus.DRAFT
        )

        val createResult = eventRepository.createEvent(event)
        assertTrue(createResult.isSuccess)

        // Add participant offline
        val addParticipantResult = eventRepository.addParticipant("offline-event-1", "user-2")
        assertTrue(addParticipantResult.isSuccess)

        // Add vote offline
        val addVoteResult = eventRepository.addVote("offline-event-1", "user-2", "slot-1", models.Vote.YES)
        assertTrue(addVoteResult.isSuccess)

        // Verify data is persisted locally
        val retrievedEvent = eventRepository.getEvent("offline-event-1")
        assertNotNull(retrievedEvent)
        assertEquals("Offline Created Event", retrievedEvent.title)
        assertTrue(retrievedEvent.participants.contains("user-2"))

        val poll = eventRepository.getPoll("offline-event-1")
        assertNotNull(poll)
        assertEquals(models.Vote.YES, poll.votes["user-2"]?.get("slot-1"))

        // Verify sync manager has pending changes
        assertTrue(syncManager.hasPendingChanges())

        // Go online
        networkDetector.setNetworkAvailable(true)

        // Update HTTP client to expect the changes
        httpClient.response = httpClient.response.copy(appliedChanges = 3) // create + add participant + vote

        // Trigger sync
        val syncResult = syncManager.triggerSync()
        assertTrue(syncResult.isSuccess)

        val syncResponse = syncResult.getOrThrow()
        assertTrue(syncResponse.success)
        assertEquals(3, syncResponse.appliedChanges)

        // Verify no more pending changes
        assertFalse(syncManager.hasPendingChanges())

        // Verify sync status is idle
        assertEquals(SyncStatus.Idle, syncManager.syncStatus.value)
    }

    @Test
    fun testSyncRecoveryAfterNetworkInterruption() = runBlocking {
        // Start online
        networkDetector.setNetworkAvailable(true)

        // Create event online
        val event = models.Event(
            id = "recovery-event-1",
            title = "Recovery Test Event",
            description = "Test sync recovery",
            organizerId = "user-1",
            participants = listOf("user-1"),
            proposedSlots = listOf(
                models.TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
            ),
            deadline = "2025-11-25T18:00:00Z",
            status = models.EventStatus.DRAFT
        )

        val createResult = eventRepository.createEvent(event)
        assertTrue(createResult.isSuccess)

        // Sync successfully
        httpClient.response = httpClient.response.copy(appliedChanges = 1)
        val firstSyncResult = syncManager.triggerSync()
        assertTrue(firstSyncResult.isSuccess)

        // Go offline
        networkDetector.setNetworkAvailable(false)

        // Make changes offline
        val addParticipantResult = eventRepository.addParticipant("recovery-event-1", "user-2")
        assertTrue(addParticipantResult.isSuccess)

        // Try to sync while offline - should fail
        val offlineSyncResult = syncManager.triggerSync()
        assertTrue(offlineSyncResult.isFailure)
        assertEquals("Network not available", offlineSyncResult.exceptionOrNull()?.message)

        // Verify changes are still pending
        assertTrue(syncManager.hasPendingChanges())

        // Go back online
        networkDetector.setNetworkAvailable(true)

        // Sync the pending changes
        httpClient.response = httpClient.response.copy(appliedChanges = 1)
        val recoverySyncResult = syncManager.triggerSync()
        assertTrue(recoverySyncResult.isSuccess)

        // Verify sync completed
        assertFalse(syncManager.hasPendingChanges())
        assertEquals(SyncStatus.Idle, syncManager.syncStatus.value)
    }

    @Test
    fun testDataPersistenceAcrossAppRestarts() = runBlocking {
        // Simulate app restart by creating new instances
        DatabaseProvider.resetDatabase()
        val freshDatabase = DatabaseProvider.getDatabase(TestDatabaseFactory())
        val freshEventRepository = DatabaseEventRepository(freshDatabase)
        val freshUserRepository = UserRepository(freshDatabase)
        val freshNetworkDetector = TestNetworkStatusDetector().apply { setNetworkAvailable(true) }
        val freshHttpClient = TestSyncHttpClient(
            SyncResponse(
                success = true,
                appliedChanges = 1,
                conflicts = emptyList(),
                serverTimestamp = "2025-11-19T12:00:00Z",
                message = "Sync successful"
            )
        )

        val freshSyncManager = SyncManager(
            database = freshDatabase,
            eventRepository = freshEventRepository,
            userRepository = freshUserRepository,
            networkDetector = freshNetworkDetector,
            httpClient = freshHttpClient,
            authTokenProvider = { "test-token" }
        )

        // Create event with original instance
        val event = models.Event(
            id = "persistence-event-1",
            title = "Persistence Test Event",
            description = "Test data persistence",
            organizerId = "user-1",
            participants = listOf("user-1"),
            proposedSlots = listOf(
                models.TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
            ),
            deadline = "2025-11-25T18:00:00Z",
            status = models.EventStatus.DRAFT
        )

        val createResult = eventRepository.createEvent(event)
        assertTrue(createResult.isSuccess)

        // Sync with original instance
        val syncResult = syncManager.triggerSync()
        assertTrue(syncResult.isSuccess)

        // Verify data is accessible with fresh instance (simulating app restart)
        val retrievedEvent = freshEventRepository.getEvent("persistence-event-1")
        assertNotNull(retrievedEvent)
        assertEquals("Persistence Test Event", retrievedEvent.title)

        // Fresh sync manager should not have pending changes (already synced)
        assertFalse(freshSyncManager.hasPendingChanges())
    }
}