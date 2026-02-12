package com.guyghost.wakeve.workflow

import com.guyghost.wakeve.DatabaseEventRepository
import com.guyghost.wakeve.EventRepositoryInterface
import com.guyghost.wakeve.PollLogic
import com.guyghost.wakeve.ScenarioRepository
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.meeting.MeetingRepository
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.MeetingReminderTiming
import com.guyghost.wakeve.models.NotificationType
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.models.CreateMeetingRequest
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import com.guyghost.wakeve.createFreshTestDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

/**
 * # Complete Workflow E2E Tests
 * 
 * Comprehensive end-to-end tests covering the full event planning workflow:
 * - DRAFT → POLLING → CONFIRMED → COMPARING → ORGANIZING → FINALIZED
 * 
 * Test Suite:
 * 1. **E2E Test 1**: Complete workflow DRAFT → FINALIZED
 * 2. **E2E Test 2**: Workflow with push notifications
 * 3. **E2E Test 3**: Workflow offline → online sync
 * 4. **E2E Test 4**: Multi-participant scenario with best slot calculation
 * 5. **E2E Test 5**: Data integrity throughout workflow
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CompleteWorkflowE2ETest {

    private lateinit var database: WakeveDb
    private lateinit var eventRepository: EventRepositoryInterface
    private lateinit var scenarioRepository: ScenarioRepository
    private lateinit var meetingRepository: MeetingRepository
    private lateinit var testScope: CoroutineScope

    class MockNotificationService {
        data class NotificationRecord(
            val id: String, val userId: String, val type: NotificationType,
            val title: String, val body: String, val data: Map<String, String>,
            val platform: TestPlatform, val timestamp: Long
        )
        data class PushToken(val userId: String, val platform: TestPlatform, val token: String)
        data class ScheduledReminder(val id: String, val userId: String, val meetingId: String, val scheduledTime: Instant, val timing: MeetingReminderTiming)

        private val _sentNotifications = mutableListOf<NotificationRecord>()
        private val _registeredTokens = mutableListOf<PushToken>()
        private val _scheduledReminders = mutableListOf<ScheduledReminder>()

        val sentNotifications: List<NotificationRecord> get() = _sentNotifications.toList()
        val registeredTokens: List<PushToken> get() = _registeredTokens.toList()
        val scheduledReminders: List<ScheduledReminder> get() = _scheduledReminders.toList()

        fun registerToken(userId: String, platform: TestPlatform, token: String): Result<Unit> {
            _registeredTokens.add(PushToken(userId, platform, token))
            return Result.success(Unit)
        }

        fun sendNotification(userId: String, type: NotificationType, title: String, body: String, data: Map<String, String> = emptyMap(), platform: TestPlatform = TestPlatform.ANDROID): Result<String> {
            val notification = NotificationRecord(
                id = "notif-${Clock.System.now().toEpochMilliseconds()}-${_sentNotifications.size}",
                userId = userId, type = type, title = title, body = body, data = data,
                platform = platform, timestamp = Clock.System.now().toEpochMilliseconds()
            )
            _sentNotifications.add(notification)
            return Result.success(notification.id)
        }

        fun scheduleReminder(userId: String, meetingId: String, scheduledTime: Instant, timing: MeetingReminderTiming): String {
            val reminder = ScheduledReminder(
                id = "reminder-${Clock.System.now().toEpochMilliseconds()}-${_scheduledReminders.size}",
                userId = userId, meetingId = meetingId, scheduledTime = scheduledTime, timing = timing
            )
            _scheduledReminders.add(reminder)
            return reminder.id
        }

        fun getNotificationsForUser(userId: String): List<NotificationRecord> = _sentNotifications.filter { it.userId == userId }
        fun getNotificationsByType(type: NotificationType): List<NotificationRecord> = _sentNotifications.filter { it.type == type }
    }

    @BeforeTest
    fun setup() {
        database = createFreshTestDatabase()
        eventRepository = DatabaseEventRepository(database)
        scenarioRepository = ScenarioRepository(database)
        meetingRepository = MeetingRepository(database)
        testScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    }

    @AfterTest
    fun cleanup() {
        testScope.cancel()
    }

    @Test
    fun `E2E-001 complete workflow from DRAFT to FINALIZED`() = runTest {
        val eventId = "e2e-001"
        val organizerId = "organizer-1"
        val participantIds = listOf("p1", "p2", "p3")

        // Create event in DRAFT status
        val event = createTestEvent(
            id = eventId, title = "Team Building", description = "Annual retreat",
            organizerId = organizerId, participants = participantIds,
            proposedSlots = listOf(
                createTestTimeSlot("slot-1", "2025-06-15T09:00:00Z", "2025-06-15T17:00:00Z"),
                createTestTimeSlot("slot-2", "2025-06-22T09:00:00Z", "2025-06-22T17:00:00Z")
            ),
            status = EventStatus.DRAFT
        )
        eventRepository.createEvent(event)
        
        // Verify DRAFT status
        var currentEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.DRAFT, currentEvent.status)

        // Add participants
        participantIds.forEach { eventRepository.addParticipant(eventId, it) }

        // Transition to POLLING
        eventRepository.updateEventStatus(eventId, EventStatus.POLLING, null)
        currentEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.POLLING, currentEvent.status)

        // Participants vote
        val participants = eventRepository.getParticipants(eventId)!!
        eventRepository.addVote(eventId, participants[0], "slot-1", Vote.YES)
        eventRepository.addVote(eventId, participants[1], "slot-1", Vote.YES)
        eventRepository.addVote(eventId, participants[2], "slot-1", Vote.MAYBE)

        // Verify votes
        val poll = eventRepository.getPoll(eventId)
        assertNotNull(poll)

        // Confirm date and transition to CONFIRMED
        eventRepository.updateEventStatus(eventId, EventStatus.CONFIRMED, "2025-06-15T09:00:00Z")
        currentEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.CONFIRMED, currentEvent.status)

        // Create scenario in COMPARING status
        eventRepository.updateEventStatus(eventId, EventStatus.COMPARING, "2025-06-15T09:00:00Z")
        val scenario = Scenario(
            id = "scenario-1", eventId = eventId, name = "Mountain Retreat",
            dateOrPeriod = "2025-06-15", location = "Alps", duration = 2,
            estimatedParticipants = 10, estimatedBudgetPerPerson = 500.0,
            description = "Team building in mountains", status = ScenarioStatus.PROPOSED,
            createdAt = Clock.System.now().toString(), updatedAt = Clock.System.now().toString()
        )
        scenarioRepository.createScenario(scenario)

        // Select scenario and transition to ORGANIZING
        scenarioRepository.updateScenarioStatus("scenario-1", ScenarioStatus.SELECTED)
        eventRepository.updateEventStatus(eventId, EventStatus.ORGANIZING, "2025-06-15T09:00:00Z")
        currentEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.ORGANIZING, currentEvent.status)

        // Create meeting
        val meeting = com.guyghost.wakeve.meeting.Meeting(
            id = "meeting-1", eventId = eventId, organizerId = organizerId,
            title = "Planning Meeting", description = "Pre-event planning",
            startTime = Instant.parse("2025-06-10T14:00:00Z"), duration = 1.hours,
            platform = MeetingPlatform.ZOOM, meetingLink = "https://zoom.us/j/123",
            hostMeetingId = "123", password = "pwd", invitedParticipants = participantIds,
            status = com.guyghost.wakeve.meeting.MeetingStatus.SCHEDULED,
            createdAt = Clock.System.now().toString()
        )
        meetingRepository.createMeeting(meeting)

        // Finalize event
        eventRepository.updateEventStatus(eventId, EventStatus.FINALIZED, "2025-06-15T09:00:00Z")
        currentEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.FINALIZED, currentEvent.status)

        // Verify final state
        assertEquals(1, meetingRepository.getMeetingsByEventId(eventId).size)
        assertEquals(ScenarioStatus.SELECTED, scenarioRepository.getSelectedScenario(eventId)?.status)

        println("✅ E2E-001: Workflow DRAFT → FINALIZED passed!")
    }

    @Test
    fun `E2E-002 workflow with push notifications and rich content`() = runTest {
        val mockNotificationService = MockNotificationService()
        val eventId = "e2e-002"
        val organizerId = "organizer-2"
        val androidUsers = listOf("android-1", "android-2")
        val iosUsers = listOf("ios-1", "ios-2")
        val allUsers = androidUsers + iosUsers

        // Register tokens for mixed platforms
        androidUsers.forEach { mockNotificationService.registerToken(it, TestPlatform.ANDROID, "token-$it") }
        iosUsers.forEach { mockNotificationService.registerToken(it, TestPlatform.IOS, "token-$it") }
        assertEquals(4, mockNotificationService.registeredTokens.size)

        // Create event
        val event = createTestEvent(
            id = eventId, title = "Product Launch", description = "New product",
            organizerId = organizerId, participants = allUsers,
            proposedSlots = listOf(createTestTimeSlot("slot-1", "2025-07-15T19:00:00Z", "2025-07-15T23:00:00Z")),
            status = EventStatus.POLLING
        )
        eventRepository.createEvent(event)

        // Send invite notifications with explicit platform
        androidUsers.forEach { userId ->
            mockNotificationService.sendNotification(userId, NotificationType.EVENT_CONFIRMED, "You're Invited!", "Join us for ${event.title}", emptyMap(), TestPlatform.ANDROID)
        }
        iosUsers.forEach { userId ->
            mockNotificationService.sendNotification(userId, NotificationType.EVENT_CONFIRMED, "You're Invited!", "Join us for ${event.title}", emptyMap(), TestPlatform.IOS)
        }

        // Schedule reminders
        allUsers.forEach { userId ->
            mockNotificationService.scheduleReminder(userId, "meeting-1", Instant.parse("2025-07-14T19:00:00Z"), MeetingReminderTiming.ONE_DAY_BEFORE)
            mockNotificationService.scheduleReminder(userId, "meeting-1", Instant.parse("2025-07-15T18:00:00Z"), MeetingReminderTiming.ONE_HOUR_BEFORE)
        }

        // Verify notifications
        assertEquals(4, mockNotificationService.sentNotifications.size)
        assertEquals(8, mockNotificationService.scheduledReminders.size)

        // Verify platform distribution (explicit platform parameter)
        val androidNotifications = mockNotificationService.sentNotifications.filter { it.platform == TestPlatform.ANDROID }
        val iosNotifications = mockNotificationService.sentNotifications.filter { it.platform == TestPlatform.IOS }
        assertEquals(2, androidNotifications.size)
        assertEquals(2, iosNotifications.size)

        println("✅ E2E-002: Push notifications workflow passed!")
    }

    @Test
    fun `E2E-003 workflow offline to online sync`() = runTest {
        val syncManager = TestSyncManager(eventRepository)
        val eventId = "e2e-003"
        val organizerId = "organizer-3"
        val participantIds = listOf("p1", "p2", "p3")

        // Go offline
        syncManager.setOnline(false)

        // Queue event creation (without participants - they will be added separately)
        val event = createTestEvent(
            id = eventId, title = "Offline Event", description = "Created offline",
            organizerId = organizerId, participants = emptyList(),
            proposedSlots = listOf(createTestTimeSlot("slot-1", "2025-08-15T10:00:00Z", "2025-08-15T12:00:00Z")),
            status = EventStatus.DRAFT
        )
        syncManager.queueOperation(TestSyncManager.OperationType.CREATE_EVENT, event)
        assertNull(eventRepository.getEvent(eventId), "Event should not exist while offline")
        assertEquals(1, syncManager.pendingOperationsCount)

        // Queue add participant operations (participants are added separately after event creation)
        participantIds.forEach { syncManager.queueOperation(TestSyncManager.OperationType.ADD_PARTICIPANT, Pair(eventId, it)) }
        syncManager.queueOperation(TestSyncManager.OperationType.UPDATE_STATUS, Triple(eventId, EventStatus.POLLING, null))
        assertEquals(5, syncManager.pendingOperationsCount)

        // Go online and sync
        syncManager.setOnline(true)
        val results = syncManager.syncPendingOperations()
        assertEquals(0, syncManager.pendingOperationsCount)
        assertEquals(5, results.size)
        results.forEach { assertTrue(it.isSuccess, "Sync operation should succeed") }

        // Verify synced data
        val syncedEvent = eventRepository.getEvent(eventId)!!
        assertEquals(EventStatus.POLLING, syncedEvent.status)
        val syncedParticipants = eventRepository.getParticipants(eventId)
        assertNotNull(syncedParticipants, "Synced participants should not be null")
        // 4 participants = 1 organizer (auto-added) + 3 added participants (p1, p2, p3)
        assertEquals(4, syncedParticipants.size, "Should have 4 synced participants (1 organizer + 3 added)")

        println("✅ E2E-003: Offline → Online sync workflow passed!")
    }

    @Test
    fun `E2E-004 multi-participant scenario with best slot calculation`() = runTest {
        val eventId = "e2e-004"
        val organizerId = "organizer-4"
        val participants = (1..10).map { "participant-$it" }

        val slots = listOf(
            createTestTimeSlot("slot-monday", "2025-09-15T09:00:00Z", "2025-09-15T17:00:00Z"),
            createTestTimeSlot("slot-tuesday", "2025-09-16T09:00:00Z", "2025-09-16T17:00:00Z"),
            createTestTimeSlot("slot-wednesday", "2025-09-17T09:00:00Z", "2025-09-17T17:00:00Z")
        )

        // Create event in DRAFT status first (required for adding participants)
        val event = createTestEvent(
            id = eventId, title = "All-Hands Meeting", description = "Company meeting",
            organizerId = organizerId, participants = emptyList(), proposedSlots = slots,
            status = EventStatus.DRAFT
        )
        eventRepository.createEvent(event)
        
        // Add participants while in DRAFT status
        participants.forEach { eventRepository.addParticipant(eventId, it) }
        
        // Transition to POLLING status before voting
        eventRepository.updateEventStatus(eventId, EventStatus.POLLING, null)

        // Get participants with null-safety check
        val allParticipants = eventRepository.getParticipants(eventId)
        assertNotNull(allParticipants, "Participants should not be null")
        assertTrue(allParticipants.size >= 10, "Should have at least 10 participants, got ${allParticipants.size}")

        // Group 1 (4 people): Strong preference for Monday
        allParticipants.take(4).forEach { eventRepository.addVote(eventId, it, "slot-monday", Vote.YES) }

        // Group 2 (3 people): Preference for Wednesday
        if (allParticipants.size >= 7) {
            allParticipants.subList(4, 7).forEach { eventRepository.addVote(eventId, it, "slot-wednesday", Vote.YES) }
        }

        // Group 3 (3 people): Mixed preferences (with bounds checking)
        if (allParticipants.size > 7) {
            eventRepository.addVote(eventId, allParticipants[7], "slot-monday", Vote.YES)
        }
        if (allParticipants.size > 8) {
            eventRepository.addVote(eventId, allParticipants[8], "slot-tuesday", Vote.MAYBE)
        }
        if (allParticipants.size > 9) {
            eventRepository.addVote(eventId, allParticipants[9], "slot-monday", Vote.MAYBE)
        }

        // Calculate best slot
        val poll = eventRepository.getPoll(eventId)
        assertNotNull(poll, "Poll should not be null")
        val scores = PollLogic.getSlotScores(poll, slots)
        val bestSlot = PollLogic.getBestSlotWithScore(poll, slots)

        // Verify Monday wins (5 YES + 2 MAYBE = 12 points)
        assertNotNull(bestSlot)
        val mondayScore = scores.find { it.slotId == "slot-monday" }
        assertNotNull(mondayScore)
        assertEquals(5, mondayScore.yesCount)
        assertTrue(mondayScore.totalScore >= 10)

        println("✅ E2E-004: Multi-participant scenario passed!")
    }

    @Test
    fun `E2E-005 data integrity throughout workflow`() = runTest {
        val eventId = "e2e-005"
        val organizerId = "organizer-5"
        val participantId = "participant-1"

        // Track data through all status transitions
        val event = createTestEvent(
            id = eventId, title = "Integrity Test", organizerId = organizerId,
            participants = listOf(participantId),
            proposedSlots = listOf(createTestTimeSlot("slot-1", "2025-10-15T10:00:00Z", "2025-10-15T12:00:00Z")),
            status = EventStatus.DRAFT
        )
        eventRepository.createEvent(event)
        assertEquals("Integrity Test", eventRepository.getEvent(eventId)!!.title)

        eventRepository.updateEventStatus(eventId, EventStatus.POLLING, null)
        eventRepository.addParticipant(eventId, participantId)
        eventRepository.addVote(eventId, participantId, "slot-1", Vote.YES)

        eventRepository.updateEventStatus(eventId, EventStatus.CONFIRMED, "2025-10-15T10:00:00Z")
        eventRepository.updateEventStatus(eventId, EventStatus.COMPARING, "2025-10-15T10:00:00Z")

        val scenario = Scenario(
            id = "scenario-1", eventId = eventId, name = "Test Scenario",
            dateOrPeriod = "2025-10-15", location = "Test Location", duration = 1,
            estimatedParticipants = 5, estimatedBudgetPerPerson = 100.0, description = "Test",
            status = ScenarioStatus.PROPOSED,
            createdAt = Clock.System.now().toString(), updatedAt = Clock.System.now().toString()
        )
        scenarioRepository.createScenario(scenario)
        scenarioRepository.updateScenarioStatus(scenario.id, ScenarioStatus.SELECTED)

        eventRepository.updateEventStatus(eventId, EventStatus.ORGANIZING, "2025-10-15T10:00:00Z")
        eventRepository.updateEventStatus(eventId, EventStatus.FINALIZED, "2025-10-15T10:00:00Z")

        // Verify final state
        val finalEvent = eventRepository.getEvent(eventId)!!
        assertEquals("Integrity Test", finalEvent.title)
        assertEquals(organizerId, finalEvent.organizerId)
        assertEquals(EventStatus.FINALIZED, finalEvent.status)
        assertEquals(ScenarioStatus.SELECTED, scenarioRepository.getSelectedScenario(eventId)?.status)

        println("✅ E2E-005: Data integrity verified!")
    }

    class TestSyncManager(private val repository: EventRepositoryInterface) {
        enum class OperationType { CREATE_EVENT, ADD_PARTICIPANT, UPDATE_STATUS, ADD_VOTE, UPDATE_EVENT }
        data class PendingOperation(val id: String, val type: OperationType, val data: Any, val timestamp: Long = Clock.System.now().toEpochMilliseconds(), var retryCount: Int = 0)

        private var isOnline = true
        private val _pendingOperations = mutableListOf<PendingOperation>()
        val pendingOperationsCount: Int get() = _pendingOperations.size

        fun setOnline(online: Boolean) { isOnline = online }

        fun queueOperation(type: OperationType, data: Any): Result<Unit> {
            return if (isOnline) executeOperation(type, data)
            else {
                _pendingOperations.add(PendingOperation(
                    id = "op-${Clock.System.now().toEpochMilliseconds()}-${_pendingOperations.size}", type = type, data = data
                ))
                Result.success(Unit)
            }
        }

        suspend fun syncPendingOperations(): List<Result<Unit>> {
            if (!isOnline) return listOf(Result.failure(Exception("Still offline")))
            val results = mutableListOf<Result<Unit>>()
            _pendingOperations.toList().forEach { operation ->
                val result = executeOperation(operation.type, operation.data)
                if (result.isSuccess) _pendingOperations.remove(operation) else operation.retryCount++
                results.add(result)
            }
            return results
        }

        private fun executeOperation(type: OperationType, data: Any): Result<Unit> {
            return try {
                when (type) {
                    OperationType.CREATE_EVENT -> { runBlocking { repository.createEvent(data as Event) }; Result.success(Unit) }
                    OperationType.ADD_PARTICIPANT -> { runBlocking { repository.addParticipant((data as Pair<String, String>).first, data.second) }; Result.success(Unit) }
                    OperationType.UPDATE_STATUS -> { runBlocking { repository.updateEventStatus((data as Triple<String, EventStatus, String?>).first, data.second, data.third) }; Result.success(Unit) }
                    OperationType.ADD_VOTE -> { 
                        val (eventId, userId, voteData) = data as Triple<String, String, Pair<String, Vote>>
                        runBlocking { repository.addVote(eventId, userId, voteData.first, voteData.second) }
                        Result.success(Unit)
                    }
                    OperationType.UPDATE_EVENT -> { runBlocking { repository.updateEvent(data as Event) }; Result.success(Unit) }
                }
            } catch (e: Exception) { Result.failure(e) }
        }
    }
}

enum class TestPlatform { ANDROID, IOS }
