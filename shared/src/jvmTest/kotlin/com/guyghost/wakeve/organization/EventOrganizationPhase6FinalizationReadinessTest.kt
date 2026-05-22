package com.guyghost.wakeve.organization

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
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
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.EventRepositoryInterface
import com.guyghost.wakeve.repository.OrderBy
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class EventOrganizationPhase6FinalizationReadinessTest {

    private lateinit var database: WakeveDb

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
    }

    @Test
    fun `Phase6 readiness summary exposes every critical finalization section`() {
        val eventId = "phase6-readiness-blockers"
        seedEvent(eventId, EventStatus.ORGANIZING)
        seedParticipant(eventId, "organizer-1", role = "ORGANIZER", confirmed = true)
        seedParticipant(eventId, "confirmed-a", confirmed = true)
        seedPendingCriticalSync(
            id = "sync-phase6-lodging-pending",
            entityType = "lodging_selection",
            entityId = eventId,
            operation = "UPSERT"
        )
        seedPendingCriticalConflict(
            eventId = eventId,
            fieldName = "lodging_selection"
        )

        val readiness = EventOrganizationReadinessRepository(database).getReadiness(eventId)
        val exposedSections = readiness.javaClass.declaredFields.map { it.name }.toSet()

        assertFalse(readiness.complete)
        assertTrue(
            requiredFinalizationSections.all { it in exposedSections },
            "Phase 6 readiness must expose all critical finalization sections. " +
                "Missing: ${requiredFinalizationSections - exposedSections}; exposed=$exposedSections"
        )
    }

    @Test
    fun `Phase6 repository finalization is blocked while critical readiness or sync blockers remain`() = runTest {
        val eventId = "phase6-repository-blocked"
        seedEvent(eventId, EventStatus.ORGANIZING)
        seedParticipant(eventId, "organizer-1", role = "ORGANIZER", confirmed = true)
        seedParticipant(eventId, "confirmed-a", confirmed = true)
        seedPendingCriticalSync(
            id = "sync-phase6-meeting-pending",
            entityType = "meeting",
            entityId = "meeting-phase6",
            operation = "CREATE"
        )

        val result = DatabaseEventRepository(database).updateEventStatus(
            id = eventId,
            status = EventStatus.FINALIZED,
            finalDate = "2026-07-18T08:00:00Z"
        )

        assertTrue(
            result.isFailure,
            "Finalization must fail while any critical organization readiness or sync blocker remains."
        )
        assertEquals(
            EventStatus.ORGANIZING.name,
            database.eventQueries.selectById(eventId).executeAsOne().status,
            "A blocked finalization attempt must leave the event in ORGANIZING."
        )
    }

    @Test
    fun `Phase4 11a legacy transport conflict resolution audit row does not block finalization readiness`() = runTest {
        val eventId = "phase4-11a-legacy-transport-audit"
        seedOtherwiseReadyFinalizationEvent(eventId)
        seedPendingCriticalSync(
            id = "sync-phase4-11a-legacy-transport-selection-conflict-resolved",
            entityType = "transport_plan_selection",
            entityId = eventId,
            operation = "CONFLICT_RESOLVED"
        )

        val readiness = EventOrganizationReadinessRepository(database).getReadiness(eventId)

        assertFalse(
            readiness.sync.blockers.contains("CRITICAL_SYNC_PENDING"),
            "Legacy transport_plan_selection CONFLICT_RESOLVED rows are audit-only and non-replayable; " +
                "they must not create a sync readiness blocker. blockers=${readiness.blockers}"
        )
        assertFalse(
            readiness.blockers.contains("CRITICAL_SYNC_PENDING"),
            "Otherwise-ready events must not expose CRITICAL_SYNC_PENDING for legacy transport audit rows. " +
                "blockers=${readiness.blockers}"
        )

        val result = DatabaseEventRepository(database).updateEventStatus(
            id = eventId,
            status = EventStatus.FINALIZED,
            finalDate = "2026-07-18T08:00:00Z"
        )

        assertTrue(
            result.isSuccess,
            "Finalization must not be blocked by a legacy non-replayable transport CONFLICT_RESOLVED audit row. " +
                "failure=${result.exceptionOrNull()?.message}"
        )
        assertEquals(
            EventStatus.FINALIZED.name,
            database.eventQueries.selectById(eventId).executeAsOne().status,
            "Otherwise-ready events should finalize when the only pending row is transport conflict audit metadata."
        )
    }

    @Test
    fun `Phase6 state machine finalizes organizing event when readiness is complete`() = runTest {
        val eventId = "phase6-state-machine-success"
        val organizerId = "organizer-1"
        val repository = Phase6StateMachineRepository(
            eventFixture(
                id = eventId,
                organizerId = organizerId,
                status = EventStatus.ORGANIZING
            )
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(repository),
            createEventUseCase = CreateEventUseCase(repository),
            eventRepository = repository,
            scope = scope
        )

        stateMachine.dispatch(EventManagementContract.Intent.MarkAsFinalized(eventId, organizerId))
        scope.advanceUntilIdle()
        val effect = withTimeout(1_000) { stateMachine.sideEffect.first() }

        assertEquals(EventStatus.FINALIZED, repository.events.getValue(eventId).status)
        assertTrue(
            effect is EventManagementContract.SideEffect.ShowToast &&
                effect.message.contains("finalized", ignoreCase = true),
            "Successful finalization should emit a user-visible completion side effect."
        )
    }

    @Test
    fun `Phase6 state machine does not finalize when repository reports readiness blockers`() = runTest {
        val eventId = "phase6-state-machine-blocked"
        val organizerId = "organizer-1"
        val repository = Phase6StateMachineRepository(
            eventFixture(
                id = eventId,
                organizerId = organizerId,
                status = EventStatus.ORGANIZING
            ),
            finalizationBlockers = listOf(
                "TRANSPORT_REQUIRED",
                "PAYMENT_TRICOUNT_REQUIRED",
                "CRITICAL_SYNC_PENDING"
            )
        )
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = TestScope(dispatcher)
        val stateMachine = EventManagementStateMachine(
            loadEventsUseCase = LoadEventsUseCase(repository),
            createEventUseCase = CreateEventUseCase(repository),
            eventRepository = repository,
            scope = scope
        )

        stateMachine.dispatch(EventManagementContract.Intent.MarkAsFinalized(eventId, organizerId))
        scope.advanceUntilIdle()

        assertEquals(
            EventStatus.ORGANIZING,
            repository.events.getValue(eventId).status,
            "State machine finalization must preserve ORGANIZING while readiness blockers remain."
        )
        assertTrue(
            stateMachine.state.value.error.orEmpty().contains("TRANSPORT_REQUIRED") ||
                stateMachine.state.value.error.orEmpty().contains("CRITICAL_SYNC_PENDING"),
            "The state machine error should surface the readiness blockers that prevent finalization."
        )
    }

    private fun seedEvent(eventId: String, status: EventStatus) {
        val now = "2026-05-22T10:00:00Z"
        database.eventQueries.insertEvent(
            id = eventId,
            organizerId = "organizer-1",
            title = "Phase 6 Event",
            description = "Finalization readiness event",
            status = status.name,
            deadline = "2026-07-18T08:00:00Z",
            createdAt = now,
            updatedAt = now,
            version = 1,
            eventType = EventType.OTHER.name,
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null,
            isSample = 0L
        )
    }

    private fun seedParticipant(
        eventId: String,
        userId: String,
        role: String = "PARTICIPANT",
        confirmed: Boolean
    ) {
        database.participantQueries.insertParticipant(
            id = "$eventId-$userId",
            eventId = eventId,
            userId = userId,
            role = role,
            hasValidatedDate = if (confirmed) 1L else 0L,
            joinedAt = "2026-05-22T10:05:00Z",
            updatedAt = "2026-05-22T10:05:00Z"
        )
    }

    private fun seedOtherwiseReadyFinalizationEvent(eventId: String) {
        val now = "2026-05-22T10:00:00Z"
        val slotId = "slot-$eventId"
        val planId = "plan-$eventId"

        seedEvent(eventId, EventStatus.ORGANIZING)
        seedParticipant(eventId, "organizer-1", role = "ORGANIZER", confirmed = true)
        seedParticipant(eventId, "confirmed-a", confirmed = true)
        database.timeSlotQueries.insertTimeSlot(
            id = slotId,
            eventId = eventId,
            startTime = "2026-07-18T08:00:00Z",
            endTime = "2026-07-18T10:00:00Z",
            timezone = "Europe/Paris",
            proposedByParticipantId = null,
            createdAt = now,
            updatedAt = now,
            timeOfDay = TimeOfDay.SPECIFIC.name
        )
        database.confirmedDateQueries.insertConfirmedDate(
            id = "confirmed-$eventId",
            eventId = eventId,
            timeslotId = slotId,
            confirmedByOrganizerId = "organizer-1",
            confirmedAt = now,
            updatedAt = now
        )
        database.scenarioQueries.insertScenario(
            id = "scenario-$eventId",
            eventId = eventId,
            name = "Selected scenario",
            dateOrPeriod = "2026-07-18",
            location = "Paris",
            duration = 1,
            estimatedParticipants = 2,
            estimatedBudgetPerPerson = 120.0,
            description = "Selected finalization fixture scenario",
            status = "SELECTED",
            createdAt = now,
            updatedAt = now
        )
        database.transportQueries.insertPlan(
            id = planId,
            event_id = eventId,
            destination_json = """{"name":"Paris","address":"Paris, France"}""",
            optimization_type = "BALANCED",
            total_group_cost = 0.0,
            group_arrivals_json = "[]",
            created_at = now
        )
        database.transportQueries.upsertSelectedPlan(
            event_id = eventId,
            plan_id = planId,
            selected_at = now,
            selected_by_user_id = "organizer-1"
        )
        seedReadinessDecision(eventId, "MEETINGS", now)
        seedReadinessDecision(eventId, "BUDGET_BASELINE", now)
        database.tricountHandoffQueries.upsertHandoff(
            eventId = eventId,
            provider = "TRICOUNT",
            providerId = null,
            providerUrl = null,
            syncStatus = "NOT_NEEDED",
            trusted = 1L,
            explicitNotNeeded = 1L,
            lastSyncAt = now,
            createdAt = now,
            updatedAt = now
        )
    }

    private fun seedReadinessDecision(eventId: String, section: String, decidedAt: String) {
        database.organizationReadinessDecisionQueries.upsertDecision(
            id = "readiness-$section-$eventId",
            eventId = eventId,
            section = section,
            notNeeded = 1L,
            decidedBy = "organizer-1",
            decidedAt = decidedAt
        )
    }

    private fun seedPendingCriticalSync(
        id: String,
        entityType: String,
        entityId: String,
        operation: String,
        retryState: String = "READY"
    ) {
        database.syncMetadataQueries.insertSyncMetadataWithPayload(
            id = id,
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payload = """{"eventId":"$entityId","critical":true}""",
            timestamp = "2026-05-22T10:10:00Z_$id",
            retryState = retryState,
            retryCount = if (retryState == "FAILED") 3L else 0L,
            synced = 0L
        )
    }

    private fun seedPendingCriticalConflict(eventId: String, fieldName: String) {
        database.conflictLogQueries.insertConflict(
            id = "conflict-$eventId-$fieldName",
            event_id = eventId,
            field_name = fieldName,
            local_value = "local",
            remote_value = "remote",
            local_updated_at = "2026-05-22T10:10:00Z",
            remote_updated_at = "2026-05-22T10:11:00Z",
            severity = "CRITICAL",
            resolution_strategy = "PENDING",
            resolved_by = null,
            resolved_at = null,
            created_at = "2026-05-22T10:12:00Z"
        )
    }

    private fun eventFixture(
        id: String,
        organizerId: String,
        status: EventStatus
    ): Event = Event(
        id = id,
        title = "Phase 6 State Machine",
        description = "Finalization state machine fixture",
        organizerId = organizerId,
        participants = listOf(organizerId, "confirmed-a"),
        proposedSlots = listOf(
            TimeSlot(
                id = "slot-1",
                start = "2026-07-18T08:00:00Z",
                end = "2026-07-18T10:00:00Z",
                timezone = "Europe/Paris",
                timeOfDay = TimeOfDay.SPECIFIC
            )
        ),
        deadline = "2026-07-18T08:00:00Z",
        status = status,
        finalDate = "2026-07-18T08:00:00Z",
        createdAt = "2026-05-22T10:00:00Z",
        updatedAt = "2026-05-22T10:00:00Z",
        eventType = EventType.OTHER
    )

    private companion object {
        val requiredFinalizationSections = setOf(
            "participants",
            "scenario",
            "destination",
            "lodging",
            "transport",
            "meetings",
            "calendar",
            "notifications",
            "budget",
            "payment",
            "tricount",
            "sync",
            "unsafeLinks",
            "accessControl"
        )
    }
}

private class Phase6StateMachineRepository(
    initialEvent: Event,
    private val finalizationBlockers: List<String> = emptyList()
) : EventRepositoryInterface {
    val events = mutableMapOf(initialEvent.id to initialEvent)
    private val polls = mutableMapOf<String, Poll>()

    override suspend fun createEvent(event: Event): Result<Event> {
        events[event.id] = event
        polls[event.id] = Poll(event.id, event.id, emptyMap())
        return Result.success(event)
    }

    override fun getEvent(id: String): Event? = events[id]

    override fun getPoll(eventId: String): Poll? = polls[eventId]

    override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> {
        val event = events[eventId] ?: return Result.failure(IllegalArgumentException("Event not found"))
        events[eventId] = event.copy(participants = event.participants + participantId)
        return Result.success(true)
    }

    override fun getParticipants(eventId: String): List<String>? = events[eventId]?.participants

    override suspend fun addVote(
        eventId: String,
        participantId: String,
        slotId: String,
        vote: Vote
    ): Result<Boolean> = Result.success(true)

    override suspend fun updateEvent(event: Event): Result<Event> {
        events[event.id] = event
        return Result.success(event)
    }

    override suspend fun updateEventStatus(
        id: String,
        status: EventStatus,
        finalDate: String?
    ): Result<Boolean> {
        if (status == EventStatus.FINALIZED && finalizationBlockers.isNotEmpty()) {
            return Result.failure(
                IllegalStateException("Finalization blocked by ${finalizationBlockers.joinToString(",")}")
            )
        }
        val event = events[id] ?: return Result.failure(IllegalArgumentException("Event not found"))
        events[id] = event.copy(status = status, finalDate = finalDate)
        return Result.success(true)
    }

    override suspend fun saveEvent(event: Event): Result<Event> {
        events[event.id] = event
        return Result.success(event)
    }

    override suspend fun deleteEvent(eventId: String): Result<Unit> {
        events.remove(eventId)
        polls.remove(eventId)
        return Result.success(Unit)
    }

    override fun isDeadlinePassed(deadline: String): Boolean = false

    override fun isOrganizer(eventId: String, userId: String): Boolean =
        events[eventId]?.organizerId == userId

    override fun canModifyEvent(eventId: String, userId: String): Boolean = isOrganizer(eventId, userId)

    override fun getAllEvents(): List<Event> = events.values.toList()

    override fun getEventsPaginated(
        page: Int,
        pageSize: Int,
        orderBy: OrderBy
    ): Flow<List<Event>> = flowOf(events.values.toList().drop(page * pageSize).take(pageSize))
}
