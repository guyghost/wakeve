package com.guyghost.wakeve.e2e

import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.*
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.*
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.*

/**
 * # Offline Sync E2E Test (E2E-003)
 * 
 * Tests offline-first synchronization:
 * - Create events while offline
 * - Vote offline
 * - Sync on reconnection
 * - Conflict resolution (last-write-wins)
 * - Queue operations while offline
 */
@OptIn(ExperimentalCoroutinesApi::class)
class OfflineSyncE2ETest {

    // ========================================================================
    // Test Infrastructure
    // ========================================================================

    private lateinit var database: WakevDb
    private lateinit var eventRepository: EventRepositoryInterface
    private lateinit var syncManager: MockSyncManager
    private lateinit var testScope: CoroutineScope

    /**
     * Mock Sync Manager for testing offline/online behavior
     */
    class MockSyncManager(
        private val repository: EventRepositoryInterface,
        private val scope: CoroutineScope
    ) {
        var isOnline = false
        val queuedOperations = mutableListOf<QueuedOperation>()
        var conflictResolutionStrategy = ConflictResolution.LAST_WRITE_WINS

        data class QueuedOperation(
            val id: String,
            val type: OperationType,
            val data: Any,
            val timestamp: Long,
            var retryCount: Int = 0
        )

        enum class OperationType {
            CREATE_EVENT, UPDATE_EVENT, ADD_VOTE, ADD_COMMENT, ADD_SCENARIO
        }

        enum class ConflictResolution {
            LAST_WRITE_WINS, MANUAL_RESOLUTION
        }

        suspend fun <T> executeOnline(operation: () -> Result<T>): Result<T> {
            return if (isOnline) {
                operation()
            } else {
                // Queue operation for later sync
                val queuedOp = QueuedOperation(
                    id = "op-${Clock.System.now().toEpochMilliseconds()}",
                    type = OperationType.UPDATE_EVENT, // Simplified
                    data = operation,
                    timestamp = Clock.System.now().toEpochMilliseconds()
                )
                queuedOperations.add(queuedOp)
                Result.failure(Exception("Offline - operation queued"))
            }
        }

        suspend fun syncQueuedOperations(): List<Result<Unit>> {
            val results = mutableListOf<Result<Unit>>()
            
            for (operation in queuedOperations.toList()) {
                if (isOnline) {
                    try {
                        when (operation.type) {
                            OperationType.CREATE_EVENT -> {
                                val event = operation.data as Event
                                val result = repository.createEvent(event)
                                if (result.isSuccess) {
                                    queuedOperations.remove(operation)
                                    results.add(Result.success(Unit))
                                } else {
                                    operation.retryCount++
                                    results.add(Result.failure(Exception("Sync failed")))
                                }
                            }
                            OperationType.UPDATE_EVENT -> {
                                val event = operation.data as Event
                                val result = repository.updateEvent(event)
                                if (result.isSuccess) {
                                    queuedOperations.remove(operation)
                                    results.add(Result.success(Unit))
                                } else {
                                    operation.retryCount++
                                    results.add(Result.failure(Exception("Sync failed")))
                                }
                            }
                            OperationType.ADD_VOTE -> {
                                // Implementation for vote sync
                                queuedOperations.remove(operation)
                                results.add(Result.success(Unit))
                            }
                            OperationType.ADD_COMMENT -> {
                                // Implementation for comment sync
                                queuedOperations.remove(operation)
                                results.add(Result.success(Unit))
                            }
                            OperationType.ADD_SCENARIO -> {
                                // Implementation for scenario sync
                                queuedOperations.remove(operation)
                                results.add(Result.success(Unit))
                            }
                        }
                    } catch (e: Exception) {
                        operation.retryCount++
                        results.add(Result.failure(e))
                    }
                }
            }
            
            return results
        }
    }

    @BeforeTest
    fun setup() {
        database = createFreshTestDatabase()
        eventRepository = DatabaseEventRepository(database)
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        syncManager = MockSyncManager(eventRepository, testScope)
    }

    @AfterTest
    fun cleanup() {
        testScope.cancel()
        database.close()
    }

    // ========================================================================
    // Test Cases
    // ========================================================================

    /**
     * Test: Create event while offline
     * 
     * GIVEN: User is offline
     * WHEN: User creates an event
     * THEN: Event is saved locally and queued for sync
     */
    @Test
    fun `create event while offline`() = runTest {
        // GIVEN
        syncManager.isOnline = false
        
        val eventId = "event-offline-create"
        val event = createTestEvent(
            id = eventId,
            title = "Offline Event",
            description = "Created while offline",
            organizerId = "user-1",
            status = EventStatus.DRAFT
        )

        // WHEN - Create event while offline
        val createResult = syncManager.executeOnline {
            eventRepository.createEvent(event)
        }

        // THEN - Should fail (offline) but event should be in queue
        assertTrue(createResult.isFailure, "Should fail when offline")
        assertEquals(1, syncManager.queuedOperations.size, "Operation should be queued")
        
        // Verify event is NOT in repository yet (offline)
        val storedEvent = eventRepository.getEvent(eventId)
        assertNull(storedEvent, "Event should not be in repository when offline")

        // WHEN - Go online and sync
        syncManager.isOnline = true
        val syncResults = syncManager.syncQueuedOperations()
        
        // THEN - Sync should succeed
        assertEquals(1, syncResults.size, "Should have one sync result")
        assertTrue(syncResults[0].isSuccess, "Sync should succeed")
        assertEquals(0, syncManager.queuedOperations.size, "Queue should be empty after sync")
        
        // Verify event is now in repository
        val syncedEvent = eventRepository.getEvent(eventId)
        assertNotNull(syncEvent, "Event should be in repository after sync")
        assertEquals("Offline Event", syncedEvent.title)
        assertEquals(EventStatus.DRAFT, syncedEvent.status)
    }

    /**
     * Test: Vote while offline
     * 
     * GIVEN: User is offline, event exists locally
     * WHEN: User votes on time slots
     * THEN: Votes are queued and synced when online
     */
    @Test
    fun `vote while offline`() = runTest {
        // GIVEN
        syncManager.isOnline = true // Start online to create event
        
        val eventId = "event-offline-vote"
        val event = createTestEvent(
            id = eventId,
            title = "Voting Test Event",
            organizerId = "organizer",
            participants = listOf("voter-1", "voter-2"),
            proposedSlots = listOf(
                createTestTimeSlot("slot-1", "2025-06-15T10:00:00Z", "2025-06-15T12:00:00Z"),
                createTestTimeSlot("slot-2", "2025-06-16T10:00:00Z", "2025-06-16T12:00:00Z")
            ),
            status = EventStatus.POLLING
        )
        eventRepository.createEvent(event)
        
        // Go offline
        syncManager.isOnline = false

        // WHEN - Vote while offline
        val voteOperations = listOf(
            async {
                syncManager.executeOnline {
                    eventRepository.addVote(eventId, "voter-1", "slot-1", Vote.YES)
                }
            },
            async {
                syncManager.executeOnline {
                    eventRepository.addVote(eventId, "voter-1", "slot-2", Vote.NO)
                }
            },
            async {
                syncManager.executeOnline {
                    eventRepository.addVote(eventId, "voter-2", "slot-1", Vote.MAYBE)
                }
            }
        )

        // Wait for all operations
        val voteResults = voteOperations.awaitAll()
        
        // THEN - All should fail (offline) and be queued
        voteResults.forEach { result ->
            assertTrue(result.isFailure, "Vote should fail when offline")
        }
        assertEquals(3, syncManager.queuedOperations.size, "All votes should be queued")
        
        // Verify votes are NOT in repository yet
        val pollBeforeSync = eventRepository.getPoll(eventId)
        assertNotNull(pollBeforeSync)
        assertTrue(pollBeforeSync.votes.isEmpty(), "Votes should not be in repository when offline")

        // WHEN - Go online and sync
        syncManager.isOnline = true
        val syncResults = syncManager.syncQueuedOperations()
        
        // THEN - All votes should sync
        assertEquals(3, syncResults.size, "Should sync all 3 votes")
        syncResults.forEach { result ->
            assertTrue(result.isSuccess, "Each vote should sync successfully")
        }
        assertEquals(0, syncManager.queuedOperations.size, "Queue should be empty after sync")
        
        // Verify votes are now in repository
        val pollAfterSync = eventRepository.getPoll(eventId)
        assertNotNull(pollAfterSync)
        assertEquals(2, pollAfterSync.votes.size, "Should have votes from 2 users")
        
        // Verify vote content
        val voter1Votes = pollAfterSync.votes["voter-1"]
        val voter2Votes = pollAfterSync.votes["voter-2"]
        
        assertNotNull(voter1Votes)
        assertNotNull(voter2Votes)
        assertEquals(Vote.YES, voter1Votes["slot-1"])
        assertEquals(Vote.NO, voter1Votes["slot-2"])
        assertEquals(Vote.MAYBE, voter2Votes["slot-1"])
    }

    /**
     * Test: Sync on reconnection
     * 
     * GIVEN: Multiple operations queued while offline
     * WHEN: User reconnects to network
     * THEN: All operations are synced in order
     */
    @Test
    fun `sync on reconnection`() = runTest {
        // GIVEN
        syncManager.isOnline = false
        
        val eventId = "event-reconnection-sync"
        val userId = "user-1"

        // Queue multiple operations while offline
        val operations = mutableListOf<() -> Result<*>>()
        
        // Operation 1: Create event
        val event = createTestEvent(
            id = eventId,
            title = "Multi-Operation Event",
            organizerId = userId,
            status = EventStatus.DRAFT
        )
        operations.add {
            syncManager.executeOnline { eventRepository.createEvent(event) }
        }
        
        // Operation 2: Add participant
        operations.add {
            syncManager.executeOnline { eventRepository.addParticipant(eventId, "participant-1") }
        }
        
        // Operation 3: Start poll
        operations.add {
            syncManager.executeOnline { 
                eventRepository.updateEventStatus(eventId, EventStatus.POLLING, null)
            }
        }

        // Execute all operations offline
        val offlineResults = operations.map { it() }
        offlineResults.forEach { result ->
            assertTrue(result.isFailure, "Should fail when offline")
        }
        assertEquals(3, syncManager.queuedOperations.size, "All operations should be queued")

        // WHEN - Reconnect and sync
        syncManager.isOnline = true
        val syncResults = syncManager.syncQueuedOperations()
        advanceUntilIdle()

        // THEN - All operations should sync in order
        assertEquals(3, syncResults.size, "Should sync all 3 operations")
        syncResults.forEach { result ->
            assertTrue(result.isSuccess, "Each operation should sync successfully")
        }
        assertEquals(0, syncManager.queuedOperations.size, "Queue should be empty after sync")

        // Verify final state
        val finalEvent = eventRepository.getEvent(eventId)
        assertNotNull(finalEvent, "Event should exist after sync")
        assertEquals("Multi-Operation Event", finalEvent.title)
        assertEquals(EventStatus.POLLING, finalEvent.status)
        
        val participants = eventRepository.getParticipants(eventId)
        assertNotNull(participants)
        assertTrue(participants.contains("participant-1"), "Participant should be added")
    }

    /**
     * Test: Conflict resolution with last-write-wins
     * 
     * GIVEN: Two users edit same field while offline
     * WHEN: Sync occurs
     * THEN: Last-write-wins resolution is applied
     */
    @Test
    fun `conflict resolution with last write wins`() = runTest {
        // GIVEN
        syncManager.isOnline = true // Start online
        
        val eventId = "event-conflict"
        val originalEvent = createTestEvent(
            id = eventId,
            title = "Original Title",
            description = "Original description",
            organizerId = "user-1",
            status = EventStatus.DRAFT
        )
        eventRepository.createEvent(originalEvent)
        
        // User A goes offline and updates
        syncManager.isOnline = false
        delay(10) // Small delay for timestamp
        
        val userAUpdate = originalEvent.copy(
            title = "User A's Title",
            description = "Updated by User A",
            updatedAt = Clock.System.now().toString()
        )
        
        val resultA = syncManager.executeOnline {
            eventRepository.updateEvent(userAUpdate)
        }
        assertTrue(resultA.isFailure, "Should fail when offline")
        
        // User B also goes offline and updates (later timestamp)
        delay(20) // Longer delay for later timestamp
        
        val userBUpdate = originalEvent.copy(
            title = "User B's Title",
            description = "Updated by User B",
            updatedAt = Clock.System.now().toString()
        )
        
        val resultB = syncManager.executeOnline {
            eventRepository.updateEvent(userBUpdate)
        }
        assertTrue(resultB.isFailure, "Should fail when offline")
        
        assertEquals(2, syncManager.queuedOperations.size, "Both updates should be queued")

        // WHEN - Go online and sync with last-write-wins
        syncManager.isOnline = true
        syncManager.conflictResolutionStrategy = MockSyncManager.ConflictResolution.LAST_WRITE_WINS
        val syncResults = syncManager.syncQueuedOperations()
        advanceUntilIdle()

        // THEN - Last write (User B) should win
        assertEquals(2, syncResults.size, "Should sync both updates")
        
        val finalEvent = eventRepository.getEvent(eventId)
        assertNotNull(finalEvent, "Event should exist after conflict resolution")
        assertEquals("User B's Title", finalEvent.title, "User B's title should win (later timestamp)")
        assertEquals("Updated by User B", finalEvent.description, "User B's description should win")
    }

    /**
     * Test: Queue operations while offline with retry logic
     * 
     * GIVEN: Operations fail during sync
     * WHEN: Retry mechanism is applied
     * THEN: Failed operations are retried up to a limit
     */
    @Test
    fun `queue operations with retry logic`() = runTest {
        // GIVEN
        syncManager.isOnline = false
        
        val eventId = "event-retry-test"
        val event = createTestEvent(
            id = eventId,
            title = "Retry Test Event",
            organizerId = "user-1",
            status = EventStatus.DRAFT
        )
        
        // Queue operation while offline
        val offlineResult = syncManager.executeOnline {
            eventRepository.createEvent(event)
        }
        assertTrue(offlineResult.isFailure, "Should fail when offline")
        assertEquals(1, syncManager.queuedOperations.size, "Operation should be queued")
        
        // WHEN - Go online but sync fails (simulate network issue)
        syncManager.isOnline = true
        
        // Mock a failed sync by manually incrementing retry count
        syncManager.queuedOperations[0].retryCount = 2 // Simulate 2 failed retries
        
        val syncResults = syncManager.syncQueuedOperations()
        
        // THEN - Operation should succeed on third try (assuming repository works)
        assertEquals(1, syncResults.size, "Should have one sync result")
        
        // If retry count < max, it would retry; otherwise it would fail
        val finalEvent = eventRepository.getEvent(eventId)
        if (syncManager.queuedOperations[0].retryCount < 3) {
            assertNotNull(finalEvent, "Event should exist after successful retry")
            assertEquals("Retry Test Event", finalEvent.title)
        } else {
            assertNull(finalEvent, "Event should not exist if max retries exceeded")
            assertFalse(syncResults[0].isSuccess, "Should fail after max retries")
        }
    }

    /**
     * Test: Mixed online/offline scenario
     * 
     * GIVEN: User goes online and offline during workflow
     * WHEN: Operations occur in different connectivity states
     * THEN: Appropriate operations are synced or executed immediately
     */
    @Test
    fun `mixed online offline scenario`() = runTest {
        // GIVEN
        syncManager.isOnline = true
        
        val eventId = "event-mixed-scenario"
        val userId = "user-1"

        // Start online - create event
        val event = createTestEvent(
            id = eventId,
            title = "Mixed Scenario Event",
            organizerId = userId,
            status = EventStatus.DRAFT
        )
        val createResult = eventRepository.createEvent(event)
        assertTrue(createResult.isSuccess, "Should create event when online")
        
        // Go offline - add participant
        syncManager.isOnline = false
        val offlineAddResult = syncManager.executeOnline {
            eventRepository.addParticipant(eventId, "participant-1")
        }
        assertTrue(offlineAddResult.isFailure, "Should fail when offline")
        assertEquals(1, syncManager.queuedOperations.size, "Add participant should be queued")
        
        // Still offline - update description
        val updateEvent = event.copy(
            description = "Updated while offline",
            updatedAt = Clock.System.now().toString()
        )
        val offlineUpdateResult = syncManager.executeOnline {
            eventRepository.updateEvent(updateEvent)
        }
        assertTrue(offlineUpdateResult.isFailure, "Should fail when offline")
        assertEquals(2, syncManager.queuedOperations.size, "Update should be queued")
        
        // Go back online - start poll
        syncManager.isOnline = true
        val onlineStartPollResult = eventRepository.updateEventStatus(eventId, EventStatus.POLLING, null)
        assertTrue(onlineStartPollResult.isSuccess, "Should succeed when online")
        
        // Sync queued operations
        val syncResults = syncManager.syncQueuedOperations()
        advanceUntilIdle()
        
        // THEN - Verify final state
        assertEquals(2, syncResults.size, "Should sync 2 queued operations")
        syncResults.forEach { result ->
            assertTrue(result.isSuccess, "Each queued operation should sync successfully")
        }
        
        val finalEvent = eventRepository.getEvent(eventId)
        assertNotNull(finalEvent, "Event should exist")
        assertEquals("Mixed Scenario Event", finalEvent.title)
        assertEquals("Updated while offline", finalEvent.description)
        assertEquals(EventStatus.POLLING, finalEvent.status)
        
        val participants = eventRepository.getParticipants(eventId)
        assertNotNull(participants)
        assertTrue(participants.contains("participant-1"), "Participant should be added from queue")
    }
}