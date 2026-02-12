package com.guyghost.wakeve

import com.guyghost.wakeve.models.SyncResponse
import com.guyghost.wakeve.sync.SyncManager
import com.guyghost.wakeve.sync.SyncStatus
import com.guyghost.wakeve.sync.TestNetworkStatusDetector
import com.guyghost.wakeve.sync.TestSyncHttpClient
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration test for offline/online scenarios.
 * Tests data persistence, sync recovery, and state transitions.
 */
class OfflineOnlineIntegrationTest {

    private lateinit var database: com.guyghost.wakeve.database.WakeveDb
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
                appliedChanges = 0, // Will be set per test
                conflicts = emptyList(),
                serverTimestamp = "2025-11-19T12:00:00Z",
                message = "Sync successful"
            )
        )

        // Create SyncManager first with a temporary eventRepository
        val tempEventRepository = DatabaseEventRepository(database)
        syncManager = SyncManager(
            database = database,
            eventRepository = tempEventRepository,
            userRepository = userRepository,
            networkDetector = networkDetector,
            httpClient = httpClient,
            authTokenProvider = { "test-token" }
        )

        // Create eventRepository with syncManager for automatic change tracking
        eventRepository = DatabaseEventRepository(database, syncManager)
    }

    @Test
    fun testOfflineDataCreationAndOnlineSync() = runBlocking {
        // Start offline
        networkDetector.setNetworkAvailable(false)

        // Create event offline
        val now = "2025-11-20T10:00:00Z"
        val event = com.guyghost.wakeve.models.Event(
            id = "offline-event-1",
            title = "Offline Created Event",
            description = "Created while offline",
            organizerId = "user-1",
            participants = listOf("user-1"),
            proposedSlots = listOf(
                com.guyghost.wakeve.models.TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
            ),
            deadline = "2025-11-25T18:00:00Z",
            status = com.guyghost.wakeve.models.EventStatus.DRAFT,
            createdAt = now,
            updatedAt = now
        )

        val createResult = eventRepository.createEvent(event)
        assertTrue(createResult.isSuccess)

        // Add participant offline
        val addParticipantResult = eventRepository.addParticipant("offline-event-1", "user-2")
        assertTrue(addParticipantResult.isSuccess)

        // Change status to POLLING before adding vote (voting requires POLLING status)
        val updateStatusResult = eventRepository.updateEventStatus("offline-event-1", com.guyghost.wakeve.models.EventStatus.POLLING, null)
        assertTrue(updateStatusResult.isSuccess)

        // Add vote offline (now that event is in POLLING status)
        val addVoteResult = eventRepository.addVote("offline-event-1", "user-2", "slot-1", com.guyghost.wakeve.models.Vote.YES)
        assertTrue(addVoteResult.isSuccess)

        // Verify data is persisted locally
        val retrievedEvent = eventRepository.getEvent("offline-event-1")
        assertNotNull(retrievedEvent)
        assertEquals("Offline Created Event", retrievedEvent.title)
        assertTrue(retrievedEvent.participants.contains("user-2"))

        val poll = eventRepository.getPoll("offline-event-1")
        assertNotNull(poll)
        assertEquals(com.guyghost.wakeve.models.Vote.YES, poll.votes["user-2"]?.get("slot-1"))

        // Verify sync manager has pending changes
        assertTrue(syncManager.hasPendingChanges())

        // Go online
        networkDetector.setNetworkAvailable(true)

        // Update HTTP client to expect the changes
        httpClient.response = httpClient.response.copy(appliedChanges = 4) // create + add participant + status change + vote

        // Trigger sync
        val syncResult = syncManager.triggerSync()
        assertTrue(syncResult.isSuccess)

        val syncResponse = syncResult.getOrThrow()
        assertTrue(syncResponse.success)
        assertEquals(4, syncResponse.appliedChanges)

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
        val now = "2025-11-20T10:00:00Z"
        val event = com.guyghost.wakeve.models.Event(
            id = "recovery-event-1",
            title = "Recovery Test Event",
            description = "Test sync recovery",
            organizerId = "user-1",
            participants = listOf("user-1"),
            proposedSlots = listOf(
                com.guyghost.wakeve.models.TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
            ),
            deadline = "2025-11-25T18:00:00Z",
            status = com.guyghost.wakeve.models.EventStatus.DRAFT,
            createdAt = now,
            updatedAt = now
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
        // This test verifies that data persists when we create new repository instances
        // pointing to the same database (simulating app restart without killing the DB)
        
        // Create event with original instance
        val now = "2025-11-20T10:00:00Z"
        val event = com.guyghost.wakeve.models.Event(
            id = "persistence-event-1",
            title = "Persistence Test Event",
            description = "Test data persistence",
            organizerId = "user-1",
            participants = listOf("user-1"),
            proposedSlots = listOf(
                com.guyghost.wakeve.models.TimeSlot("slot-1", "2025-12-01T10:00:00Z", "2025-12-01T12:00:00Z", "UTC")
            ),
            deadline = "2025-11-25T18:00:00Z",
            status = com.guyghost.wakeve.models.EventStatus.DRAFT,
            createdAt = now,
            updatedAt = now
        )

        val createResult = eventRepository.createEvent(event)
        assertTrue(createResult.isSuccess)

        // Ensure network is available for sync
        networkDetector.setNetworkAvailable(true)
        httpClient.response = httpClient.response.copy(appliedChanges = 1)

        // Sync with original instance
        val syncResult = syncManager.triggerSync()
        assertTrue(syncResult.isSuccess)

        // Simulate "app restart" by creating NEW repository instances pointing to the SAME database
        // This tests that data persists in the database and is accessible from new instances
        val freshEventRepository = DatabaseEventRepository(database)
        val freshUserRepository = UserRepository(database)
        val freshNetworkDetector = TestNetworkStatusDetector().apply { setNetworkAvailable(true) }
        val freshHttpClient = TestSyncHttpClient(
            SyncResponse(
                success = true,
                appliedChanges = 0,
                conflicts = emptyList(),
                serverTimestamp = "2025-11-19T12:00:00Z",
                message = "No changes to sync"
            )
        )

        val freshSyncManager = SyncManager(
            database = database, // Same database
            eventRepository = freshEventRepository,
            userRepository = freshUserRepository,
            networkDetector = freshNetworkDetector,
            httpClient = freshHttpClient,
            authTokenProvider = { "test-token" }
        )

        // Verify data is accessible with fresh repository instances
        val retrievedEvent = freshEventRepository.getEvent("persistence-event-1")
        assertNotNull(retrievedEvent)
        assertEquals("Persistence Test Event", retrievedEvent.title)

        // Fresh sync manager should not have pending changes (already synced via original instance)
        assertFalse(freshSyncManager.hasPendingChanges())
    }
}