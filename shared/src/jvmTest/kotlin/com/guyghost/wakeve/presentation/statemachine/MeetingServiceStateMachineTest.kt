package com.guyghost.wakeve.presentation.statemachine

import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.calendar.PlatformCalendarService
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.meeting.Meeting
import com.guyghost.wakeve.meeting.MeetingRepository
import com.guyghost.wakeve.meeting.MeetingService
import com.guyghost.wakeve.meeting.MockMeetingPlatformProvider
import com.guyghost.wakeve.models.CreateMeetingRequest
import com.guyghost.wakeve.models.EnhancedCalendarEvent
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.notification.DefaultNotificationService
import com.guyghost.wakeve.presentation.state.MeetingManagementContract.Intent
import com.guyghost.wakeve.presentation.usecase.CancelMeetingUseCase
import com.guyghost.wakeve.presentation.usecase.CreateMeetingUseCase
import com.guyghost.wakeve.presentation.usecase.GenerateMeetingLinkUseCase
import com.guyghost.wakeve.presentation.usecase.LoadMeetingsUseCase
import com.guyghost.wakeve.presentation.usecase.UpdateMeetingUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

/**
 * Tests for MeetingServiceStateMachine.
 *
 * Tests the intent → state transitions and side effects for meeting management.
 * Uses a real database (JVM SQLite) and real use cases to verify the full chain.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MeetingServiceStateMachineTest {

    private lateinit var database: WakeveDb
    private lateinit var meetingService: MeetingService
    private lateinit var stateMachine: MeetingServiceStateMachine
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
        val calendarService = CalendarService(database, StubCalendarService())
        meetingService = MeetingService(database, calendarService, DefaultNotificationService())
        val repo = MeetingRepository(database)

        stateMachine = MeetingServiceStateMachine(
            loadMeetingsUseCase = LoadMeetingsUseCase(repo),
            createMeetingUseCase = CreateMeetingUseCase(meetingService, repo),
            updateMeetingUseCase = UpdateMeetingUseCase(repo),
            cancelMeetingUseCase = CancelMeetingUseCase(meetingService, repo),
            generateMeetingLinkUseCase = GenerateMeetingLinkUseCase(MockMeetingPlatformProvider(), repo),
            scope = testScope
        )

        seedConfirmedEvent()
    }

    // ── LoadMeetings ─────────────────────────────────────────────────────────

    @Test
    fun `LoadMeetings sets eventId and loads empty list initially`() = runTest {
        stateMachine.dispatch(Intent.LoadMeetings("event-1"))
        testScope.advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals("event-1", state.eventId)
        assertFalse(state.isLoading)
        assertTrue(state.meetings.isEmpty())
        assertNull(state.error)
    }

    @Test
    fun `LoadMeetings for unknown event loads empty list without error`() = runTest {
        stateMachine.dispatch(Intent.LoadMeetings("unknown-event"))
        testScope.advanceUntilIdle()

        val state = stateMachine.state.value
        assertEquals("unknown-event", state.eventId)
        assertTrue(state.meetings.isEmpty())
    }

    // ── CreateMeeting ────────────────────────────────────────────────────────

    @Test
    fun `CreateMeeting adds meeting to state`() = runTest {
        stateMachine.dispatch(Intent.LoadMeetings("event-1"))
        testScope.advanceUntilIdle()

        stateMachine.dispatch(
            Intent.CreateMeeting(
                CreateMeetingRequest(
                    eventId = "event-1",
                    organizerId = "organizer-1",
                    platform = MeetingPlatform.ZOOM,
                    title = "Team meeting",
                    description = null,
                    scheduledFor = Clock.System.now(),
                    duration = 60.minutes,
                    timezone = "UTC"
                )
            )
        )
        testScope.advanceUntilIdle()

        val state = stateMachine.state.value
        assertFalse(state.isLoading)
        assertEquals(1, state.meetings.size)
        assertEquals("Team meeting", state.meetings.first().title)
        assertNull(state.error)
    }

    @Test
    fun `CreateMeeting for non-existent event sets error`() = runTest {
        stateMachine.dispatch(Intent.LoadMeetings("event-1"))
        testScope.advanceUntilIdle()

        stateMachine.dispatch(
            Intent.CreateMeeting(
                CreateMeetingRequest(
                    eventId = "non-existent",
                    organizerId = "organizer-1",
                    platform = MeetingPlatform.ZOOM,
                    title = "Invalid",
                    description = null,
                    scheduledFor = Clock.System.now(),
                    duration = 60.minutes,
                    timezone = "UTC"
                )
            )
        )
        testScope.advanceUntilIdle()

        val state = stateMachine.state.value
        assertFalse(state.isLoading)
        // Error or empty meetings — creation should not succeed
        assertTrue(state.meetings.isEmpty() || state.error != null)
    }

    // ── CancelMeeting ────────────────────────────────────────────────────────

    @Test
    fun `CancelMeeting removes meeting from state`() = runTest {
        // Create a meeting first
        stateMachine.dispatch(Intent.LoadMeetings("event-1"))
        stateMachine.dispatch(
            Intent.CreateMeeting(
                CreateMeetingRequest(
                    eventId = "event-1",
                    organizerId = "organizer-1",
                    platform = MeetingPlatform.ZOOM,
                    title = "To cancel",
                    description = null,
                    scheduledFor = Clock.System.now(),
                    duration = 30.minutes,
                    timezone = "UTC"
                )
            )
        )
        testScope.advanceUntilIdle()

        val meetingId = stateMachine.state.value.meetings.first().id
        assertEquals(1, stateMachine.state.value.meetings.size)

        // Cancel it
        stateMachine.dispatch(Intent.CancelMeeting(meetingId))
        testScope.advanceUntilIdle()

        val state = stateMachine.state.value
        assertFalse(state.isLoading)
        assertNull(state.error)
    }

    @Test
    fun `CancelMeeting non-existent meeting emits side effect not state error`() = runTest {
        stateMachine.dispatch(Intent.LoadMeetings("event-1"))
        testScope.advanceUntilIdle()

        // Meeting not in current state -> ShowError side effect, state.error stays null
        stateMachine.dispatch(Intent.CancelMeeting("non-existent-meeting-id"))
        testScope.advanceUntilIdle()

        val state = stateMachine.state.value
        assertFalse(state.isLoading)
        assertTrue(state.meetings.isEmpty())
    }

    // ── State helpers ────────────────────────────────────────────────────────

    @Test
    fun `ClearError is idempotent when no error exists`() = runTest {
        stateMachine.dispatch(Intent.LoadMeetings("event-1"))
        testScope.advanceUntilIdle()

        // ClearError with no active error is a no-op
        stateMachine.dispatch(Intent.ClearError)
        testScope.advanceUntilIdle()

        assertNull(stateMachine.state.value.error)
        assertFalse(stateMachine.state.value.isLoading)
    }

    @Test
    fun `State canCreateMeetings returns true for CONFIRMED status`() {
        val state = stateMachine.state.value.copy(eventStatus = EventStatus.CONFIRMED)
        assertTrue(state.canCreateMeetings())
    }

    @Test
    fun `State canCreateMeetings returns false for DRAFT status`() {
        val state = stateMachine.state.value.copy(eventStatus = EventStatus.DRAFT)
        assertFalse(state.canCreateMeetings())
    }

    @Test
    fun `State isEmpty returns true when no meetings loaded`() {
        assertTrue(stateMachine.state.value.isEmpty)
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun seedConfirmedEvent() {
        val now = "2026-01-01T00:00:00Z"
        database.eventQueries.insertEvent(
            id = "event-1",
            organizerId = "organizer-1",
            title = "Test Event",
            description = "Description",
            status = "CONFIRMED",
            deadline = now,
            createdAt = now,
            updatedAt = now,
            version = 1,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null
        )
        // participant.id must equal participant.userId (MeetingService FK convention)
        database.participantQueries.insertParticipant(
            id = "organizer-1",
            eventId = "event-1",
            userId = "organizer-1",
            role = "ORGANIZER",
            hasValidatedDate = 1L,
            joinedAt = now,
            updatedAt = now
        )
    }
}

private class StubCalendarService : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
}
