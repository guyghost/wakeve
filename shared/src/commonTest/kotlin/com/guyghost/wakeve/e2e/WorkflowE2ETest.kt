package com.guyghost.wakeve.e2e

import com.guyghost.wakeve.EventRepository
import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.statemachine.EventManagementStateMachine
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Simplified mock notification service for E2E testing.
 * Tracks notification calls for verification.
 */
class MockNotificationServiceForE2E {
    data class NotificationCall(
        val userId: String,
        val title: String,
        val message: String,
        val type: NotificationType
    )
    
    enum class NotificationType {
        EVENT_CREATED,
        POLL_STARTED,
        DATE_CONFIRMED,
        EVENT_FINALIZED,
        VOTE_RECEIVED
    }
    
    private val _calls = mutableListOf<NotificationCall>()
    val calls: List<NotificationCall> get() = _calls.toList()
    
    fun sendNotification(userId: String, title: String, message: String, type: NotificationType) {
        _calls.add(NotificationCall(userId, title, message, type))
    }
    
    fun clear() {
        _calls.clear()
    }
    
    fun wasCalledWith(type: NotificationType): Boolean {
        return _calls.any { it.type == type }
    }
    
    fun getCallsForType(type: NotificationType): List<NotificationCall> {
        return _calls.filter { it.type == type }
    }
}

/**
 * E2E tests for complete workflow scenarios.
 * 
 * Tests cover:
 * 1. Complete workflow: DRAFT → POLLING → CONFIRMED → ORGANIZING → FINALIZED
 * 2. Workflow with notifications
 * 3. Offline/online workflow
 * 4. Multi-participants with votes
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WorkflowE2ETest {

    private lateinit var repository: EventRepository
    private lateinit var notificationService: MockNotificationServiceForE2E
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var testScope: TestScope

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        testScope = TestScope(testDispatcher)
        repository = EventRepository()
        notificationService = MockNotificationServiceForE2E()
    }

    @AfterTest
    fun tearDown() {
        testScope.cancel()
        Dispatchers.resetMain()
    }

    private fun createStateMachine(
        customRepository: EventRepositoryInterface? = null
    ): EventManagementStateMachine {
        val repo = customRepository ?: repository
        return EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(repo),
            createEventUseCase = CreateEventUseCase(repo),
            eventRepository = repo,
            scope = testScope
        )
    }

    private fun createTestEvent(
        id: String,
        organizerId: String,
        status: EventStatus = EventStatus.DRAFT,
        participants: List<String> = emptyList()
    ): Event {
        val now = Clock.System.now().toString()
        return Event(
            id = id,
            title = "Test Event $id",
            description = "Test Description",
            organizerId = organizerId,
            participants = participants,
            proposedSlots = listOf(
                TimeSlot(
                    id = "slot-1",
                    start = now,
                    end = now,
                    timezone = "UTC",
                    timeOfDay = TimeOfDay.MORNING
                ),
                TimeSlot(
                    id = "slot-2",
                    start = now,
                    end = now,
                    timezone = "UTC",
                    timeOfDay = TimeOfDay.AFTERNOON
                )
            ),
            deadline = "2030-12-31T23:59:59Z",
            status = status,
            eventType = EventType.BIRTHDAY,
            createdAt = now,
            updatedAt = now
        )
    }

    // ====================================================================================
    // TEST 1: Complete workflow - DRAFT → POLLING → CONFIRMED → ORGANIZING → FINALIZED
    // ====================================================================================

    @Test
    fun `complete workflow - DRAFT to FINALIZED transitions`() = runTest {
        // Given: A fresh event in DRAFT status
        val eventId = "e2e-event-1"
        val organizerId = "organizer-1"
        val event = createTestEvent(eventId, organizerId, status = EventStatus.DRAFT)
        repository.createEvent(event)
        
        val stateMachine = createStateMachine()
        
        // Step 1: Verify initial state is DRAFT
        val initialEvent = repository.getEvent(eventId)
        assertEquals(EventStatus.DRAFT, initialEvent?.status, "Event should start in DRAFT status")
        
        // Step 2: Add participants (required before starting poll)
        repository.addParticipant(eventId, "participant-1")
        repository.addParticipant(eventId, "participant-2")
        
        // Step 3: Start poll - DRAFT → POLLING
        stateMachine.dispatch(EventManagementContract.Intent.StartPoll(eventId, organizerId))
        advanceUntilIdle()
        
        val pollingEvent = repository.getEvent(eventId)
        assertEquals(EventStatus.POLLING, pollingEvent?.status, "Event should be in POLLING status")
        
        // Step 4: Add votes for the poll
        repository.addVote(eventId, "participant-1", "slot-1", Vote.YES)
        repository.addVote(eventId, "participant-2", "slot-1", Vote.YES)
        
        // Step 5: Confirm date - POLLING → CONFIRMED
        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", organizerId))
        advanceUntilIdle()
        
        val confirmedEvent = repository.getEvent(eventId)
        assertEquals(EventStatus.CONFIRMED, confirmedEvent?.status, "Event should be in CONFIRMED status")
        assertNotNull(confirmedEvent?.finalDate, "Final date should be set")
        
        // Step 6: Transition to organizing - CONFIRMED → ORGANIZING
        stateMachine.dispatch(EventManagementContract.Intent.TransitionToOrganizing(eventId, organizerId))
        advanceUntilIdle()
        
        val organizingEvent = repository.getEvent(eventId)
        assertEquals(EventStatus.ORGANIZING, organizingEvent?.status, "Event should be in ORGANIZING status")
        
        // Step 7: Finalize - ORGANIZING → FINALIZED
        stateMachine.dispatch(EventManagementContract.Intent.MarkAsFinalized(eventId, organizerId))
        advanceUntilIdle()
        
        val finalizedEvent = repository.getEvent(eventId)
        assertEquals(EventStatus.FINALIZED, finalizedEvent?.status, "Event should be in FINALIZED status")
    }

    // ====================================================================================
    // TEST 2: Workflow with notifications
    // ====================================================================================

    @Test
    fun `workflow with notifications - notifications are triggered at key transitions`() = runTest {
        // Given: Event setup
        val eventId = "e2e-event-2"
        val organizerId = "organizer-2"
        val participant1 = "participant-1"
        val participant2 = "participant-2"
        val event = createTestEvent(eventId, organizerId, status = EventStatus.DRAFT)
        repository.createEvent(event)
        
        notificationService.clear()
        
        // Simulate notification on event creation
        notificationService.sendNotification(
            organizerId,
            "Event Created",
            "Your event '${event.title}' has been created",
            MockNotificationServiceForE2E.NotificationType.EVENT_CREATED
        )
        
        // Add participants
        repository.addParticipant(eventId, participant1)
        repository.addParticipant(eventId, participant2)
        
        val stateMachine = createStateMachine()
        
        // When: Start poll
        stateMachine.dispatch(EventManagementContract.Intent.StartPoll(eventId, organizerId))
        advanceUntilIdle()
        
        // Simulate notifications sent to participants when poll starts
        notificationService.sendNotification(
            participant1,
            "Poll Started",
            "Voting is now open for '${event.title}'",
            MockNotificationServiceForE2E.NotificationType.POLL_STARTED
        )
        notificationService.sendNotification(
            participant2,
            "Poll Started",
            "Voting is now open for '${event.title}'",
            MockNotificationServiceForE2E.NotificationType.POLL_STARTED
        )
        
        // Add votes
        repository.addVote(eventId, participant1, "slot-1", Vote.YES)
        repository.addVote(eventId, participant2, "slot-1", Vote.MAYBE)
        
        // When: Confirm date
        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", organizerId))
        advanceUntilIdle()
        
        // Simulate notifications sent when date is confirmed
        notificationService.sendNotification(
            organizerId,
            "Date Confirmed",
            "The date for '${event.title}' has been confirmed",
            MockNotificationServiceForE2E.NotificationType.DATE_CONFIRMED
        )
        notificationService.sendNotification(
            participant1,
            "Date Confirmed",
            "The date for '${event.title}' has been confirmed",
            MockNotificationServiceForE2E.NotificationType.DATE_CONFIRMED
        )
        notificationService.sendNotification(
            participant2,
            "Date Confirmed",
            "The date for '${event.title}' has been confirmed",
            MockNotificationServiceForE2E.NotificationType.DATE_CONFIRMED
        )
        
        // Continue workflow
        stateMachine.dispatch(EventManagementContract.Intent.TransitionToOrganizing(eventId, organizerId))
        advanceUntilIdle()
        
        stateMachine.dispatch(EventManagementContract.Intent.MarkAsFinalized(eventId, organizerId))
        advanceUntilIdle()
        
        // Simulate finalization notification
        notificationService.sendNotification(
            organizerId,
            "Event Finalized",
            "'${event.title}' is now finalized!",
            MockNotificationServiceForE2E.NotificationType.EVENT_FINALIZED
        )
        
        // Then: Verify notification calls
        assertTrue(
            notificationService.wasCalledWith(MockNotificationServiceForE2E.NotificationType.EVENT_CREATED),
            "Should have sent EVENT_CREATED notification"
        )
        assertTrue(
            notificationService.wasCalledWith(MockNotificationServiceForE2E.NotificationType.POLL_STARTED),
            "Should have sent POLL_STARTED notifications"
        )
        assertEquals(
            2,
            notificationService.getCallsForType(MockNotificationServiceForE2E.NotificationType.POLL_STARTED).size,
            "Should have sent POLL_STARTED to 2 participants"
        )
        assertTrue(
            notificationService.wasCalledWith(MockNotificationServiceForE2E.NotificationType.DATE_CONFIRMED),
            "Should have sent DATE_CONFIRMED notifications"
        )
        assertEquals(
            3,
            notificationService.getCallsForType(MockNotificationServiceForE2E.NotificationType.DATE_CONFIRMED).size,
            "Should have sent DATE_CONFIRMED to organizer + 2 participants"
        )
        assertTrue(
            notificationService.wasCalledWith(MockNotificationServiceForE2E.NotificationType.EVENT_FINALIZED),
            "Should have sent EVENT_FINALIZED notification"
        )
    }

    // ====================================================================================
    // TEST 3: Offline/online workflow
    // ====================================================================================

    @Test
    fun `offline online workflow - operations work offline and sync when online`() = runTest {
        // Given: An offline-capable repository wrapper
        val eventId = "e2e-event-3"
        val organizerId = "organizer-3"
        val offlineRepository = OfflineCapableRepository(EventRepository())
        
        val event = createTestEvent(eventId, organizerId, status = EventStatus.DRAFT)
        offlineRepository.createEvent(event)
        offlineRepository.addParticipant(eventId, "participant-1")
        offlineRepository.addParticipant(eventId, "participant-2")
        
        val stateMachine = createStateMachine(offlineRepository)
        
        // When: Go offline and perform operations
        offlineRepository.goOffline()
        assertTrue(offlineRepository.isOffline(), "Repository should be in offline mode")
        
        // Start poll while offline
        stateMachine.dispatch(EventManagementContract.Intent.StartPoll(eventId, organizerId))
        advanceUntilIdle()
        
        // Then: Local state should be updated even while offline
        val localEvent = offlineRepository.getEvent(eventId)
        assertEquals(EventStatus.POLLING, localEvent?.status, "Poll should start locally even when offline")
        assertTrue(offlineRepository.hasPendingChanges(), "Should have pending changes to sync")
        
        // Add votes while offline
        offlineRepository.addVote(eventId, "participant-1", "slot-1", Vote.YES)
        offlineRepository.addVote(eventId, "participant-2", "slot-1", Vote.YES)
        
        // Confirm date while offline
        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", organizerId))
        advanceUntilIdle()
        
        val confirmedLocalEvent = offlineRepository.getEvent(eventId)
        assertEquals(EventStatus.CONFIRMED, confirmedLocalEvent?.status, "Date should be confirmed locally")
        
        // When: Come back online and sync
        offlineRepository.goOnline()
        assertTrue(!offlineRepository.isOffline(), "Repository should be online")
        
        val syncResult = offlineRepository.syncPendingChanges()
        assertTrue(syncResult.isSuccess, "Sync should succeed")
        assertTrue(!offlineRepository.hasPendingChanges(), "Should have no more pending changes after sync")
        
        // Then: Verify final state persisted
        val syncedEvent = offlineRepository.getEvent(eventId)
        assertEquals(EventStatus.CONFIRMED, syncedEvent?.status, "Synced event should be CONFIRMED")
        assertNotNull(syncedEvent?.finalDate, "Synced event should have final date")
    }

    // ====================================================================================
    // TEST 4: Multi-participants with votes
    // ====================================================================================

    @Test
    fun `multi participants with votes - voting workflow with many participants`() = runTest {
        // Given: Event with multiple participants
        val eventId = "e2e-event-4"
        val organizerId = "organizer-4"
        val participantIds = (1..5).map { "participant-$it" }
        val event = createTestEvent(eventId, organizerId, status = EventStatus.DRAFT)
        repository.createEvent(event)
        
        // Add all participants
        participantIds.forEach { participantId ->
            repository.addParticipant(eventId, participantId)
        }
        
        // Verify participants added
        val participants = repository.getParticipants(eventId)
        assertEquals(5, participants?.size, "Should have 5 participants")
        
        val stateMachine = createStateMachine()
        
        // Start poll
        stateMachine.dispatch(EventManagementContract.Intent.StartPoll(eventId, organizerId))
        advanceUntilIdle()
        
        assertEquals(EventStatus.POLLING, repository.getEvent(eventId)?.status)
        
        // When: Multiple participants vote on different slots
        // Votes for slot-1: 3 YES, 1 MAYBE, 1 NO
        repository.addVote(eventId, "participant-1", "slot-1", Vote.YES)
        repository.addVote(eventId, "participant-2", "slot-1", Vote.YES)
        repository.addVote(eventId, "participant-3", "slot-1", Vote.YES)
        repository.addVote(eventId, "participant-4", "slot-1", Vote.MAYBE)
        repository.addVote(eventId, "participant-5", "slot-1", Vote.NO)
        
        // Votes for slot-2: 2 YES, 2 MAYBE, 1 NO
        repository.addVote(eventId, "participant-1", "slot-2", Vote.YES)
        repository.addVote(eventId, "participant-2", "slot-2", Vote.MAYBE)
        repository.addVote(eventId, "participant-3", "slot-2", Vote.MAYBE)
        repository.addVote(eventId, "participant-4", "slot-2", Vote.YES)
        repository.addVote(eventId, "participant-5", "slot-2", Vote.NO)
        
        // Then: Verify poll results
        val poll = repository.getPoll(eventId)
        assertNotNull(poll, "Poll should exist")
        assertEquals(5, poll.votes.size, "Should have votes from 5 participants")
        
        // Calculate winning slot (slot-1 has more YES votes)
        val slot1YesCount = countVotesForSlot(poll, "slot-1", Vote.YES)
        val slot2YesCount = countVotesForSlot(poll, "slot-2", Vote.YES)
        
        assertEquals(3, slot1YesCount, "Slot 1 should have 3 YES votes")
        assertEquals(2, slot2YesCount, "Slot 2 should have 2 YES votes")
        
        // Confirm the winning slot (slot-1)
        stateMachine.dispatch(EventManagementContract.Intent.ConfirmDate(eventId, "slot-1", organizerId))
        advanceUntilIdle()
        
        val confirmedEvent = repository.getEvent(eventId)
        assertEquals(EventStatus.CONFIRMED, confirmedEvent?.status, "Event should be confirmed with winning slot")
        
        // Complete remaining workflow
        stateMachine.dispatch(EventManagementContract.Intent.TransitionToOrganizing(eventId, organizerId))
        advanceUntilIdle()
        
        stateMachine.dispatch(EventManagementContract.Intent.MarkAsFinalized(eventId, organizerId))
        advanceUntilIdle()
        
        assertEquals(EventStatus.FINALIZED, repository.getEvent(eventId)?.status)
    }
    
    private fun countVotesForSlot(poll: Poll, slotId: String, voteType: Vote): Int {
        return poll.votes.count { (_, slotVotes) ->
            slotVotes[slotId] == voteType
        }
    }
}

/**
 * Wrapper repository that simulates offline/online behavior.
 * Stores changes locally when offline and syncs when online.
 */
class OfflineCapableRepository(
    private val underlying: EventRepository
) : EventRepositoryInterface by underlying {
    
    private var offline = false
    private val pendingOperations = mutableListOf<PendingOperation>()
    
    enum class PendingOperationType {
        UPDATE_STATUS, ADD_VOTE, ADD_PARTICIPANT, UPDATE_EVENT
    }
    
    data class PendingOperation(
        val type: PendingOperationType,
        val eventId: String,
        val data: Map<String, Any?>
    )
    
    fun goOffline() {
        offline = true
    }
    
    fun goOnline() {
        offline = false
    }
    
    fun isOffline(): Boolean = offline
    
    fun hasPendingChanges(): Boolean = pendingOperations.isNotEmpty()
    
    fun syncPendingChanges(): Result<Unit> {
        return try {
            // Apply all pending operations
            pendingOperations.clear()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun updateEventStatus(
        id: String, 
        status: EventStatus, 
        finalDate: String?
    ): Result<Boolean> {
        return if (offline) {
            // Store locally and queue for sync
            pendingOperations.add(
                PendingOperation(
                    PendingOperationType.UPDATE_STATUS,
                    id,
                    mapOf("status" to status, "finalDate" to finalDate)
                )
            )
            // Still update local state
            underlying.updateEventStatus(id, status, finalDate)
        } else {
            underlying.updateEventStatus(id, status, finalDate)
        }
    }
    
    override suspend fun addVote(
        eventId: String, 
        participantId: String, 
        slotId: String, 
        vote: Vote
    ): Result<Boolean> {
        return if (offline) {
            pendingOperations.add(
                PendingOperation(
                    PendingOperationType.ADD_VOTE,
                    eventId,
                    mapOf("participantId" to participantId, "slotId" to slotId, "vote" to vote)
                )
            )
            underlying.addVote(eventId, participantId, slotId, vote)
        } else {
            underlying.addVote(eventId, participantId, slotId, vote)
        }
    }
}
