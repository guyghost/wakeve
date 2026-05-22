package com.guyghost.wakeve.workflow

import com.guyghost.wakeve.accommodation.AccommodationRepository
import com.guyghost.wakeve.budget.BudgetRepository
import com.guyghost.wakeve.budget.ExpenseRepository
import com.guyghost.wakeve.calendar.CalendarService
import com.guyghost.wakeve.calendar.PlatformCalendarService
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.meeting.MeetingService
import com.guyghost.wakeve.models.Accommodation
import com.guyghost.wakeve.models.AccommodationType
import com.guyghost.wakeve.models.BookingStatus
import com.guyghost.wakeve.models.BudgetCategory
import com.guyghost.wakeve.models.EnhancedCalendarEvent
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.MeetingPlatform
import com.guyghost.wakeve.models.NotificationMessage
import com.guyghost.wakeve.models.PushToken
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.notification.NotificationServiceInterface
import com.guyghost.wakeve.organization.EventOrganizationReadinessRepository
import com.guyghost.wakeve.payment.PaymentPotRepository
import com.guyghost.wakeve.payment.TricountHandoffRepository
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.ScenarioRepository
import com.guyghost.wakeve.transport.TransportRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

class EventOrganizationPhase6EndToEndSyncTest {

    private lateinit var database: WakeveDb
    private lateinit var eventRepository: DatabaseEventRepository
    private lateinit var scenarioRepository: ScenarioRepository
    private lateinit var accommodationRepository: AccommodationRepository
    private lateinit var transportRepository: TransportRepository
    private lateinit var meetingService: MeetingService
    private lateinit var budgetRepository: BudgetRepository
    private lateinit var expenseRepository: ExpenseRepository
    private lateinit var paymentPotRepository: PaymentPotRepository
    private lateinit var tricountHandoffRepository: TricountHandoffRepository
    private lateinit var readinessRepository: EventOrganizationReadinessRepository

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
        eventRepository = DatabaseEventRepository(database)
        scenarioRepository = ScenarioRepository(database)
        accommodationRepository = AccommodationRepository(database)
        transportRepository = TransportRepository(database)
        meetingService = MeetingService(
            database = database,
            calendarService = CalendarService(database, Phase6NoopPlatformCalendarService()),
            notificationService = Phase6NoopNotificationService()
        )
        budgetRepository = BudgetRepository(database)
        expenseRepository = ExpenseRepository(database)
        paymentPotRepository = PaymentPotRepository(database)
        tricountHandoffRepository = TricountHandoffRepository(database)
        readinessRepository = EventOrganizationReadinessRepository(database)
    }

    @Test
    fun `Phase6 complete shared workflow blocks finalization until offline sync converges`() = runTest {
        val eventId = "phase6-e2e"
        val organizerId = "organizer-1"
        val confirmedParticipants = listOf(organizerId, "participant-a", "participant-b")

        eventRepository.createEvent(
            eventFixture(
                eventId = eventId,
                organizerId = organizerId,
                participants = emptyList()
            )
        ).getOrThrow()
        confirmedParticipants.drop(1).forEach { participantId ->
            eventRepository.addParticipant(eventId, participantId).getOrThrow()
        }
        normalizeParticipantPrimaryKeys(eventId)

        eventRepository.updateEventStatus(eventId, EventStatus.POLLING, null).getOrThrow()
        confirmedParticipants.drop(1).forEach { participantId ->
            eventRepository.addVote(eventId, participantId, "slot-1", Vote.YES).getOrThrow()
        }
        eventRepository.confirmEventDate(eventId, "slot-1", organizerId).getOrThrow()
        confirmedParticipants.forEach { markParticipantDateValidated(eventId, it) }

        val scenario = Scenario(
            id = "scenario-phase6",
            eventId = eventId,
            name = "Bordeaux weekend",
            dateOrPeriod = "2026-07-18/2026-07-20",
            location = "Bordeaux",
            duration = 2,
            estimatedParticipants = confirmedParticipants.size,
            estimatedBudgetPerPerson = 220.0,
            description = "Central lodging and train-friendly transport",
            status = ScenarioStatus.PROPOSED,
            createdAt = now,
            updatedAt = now
        )
        scenarioRepository.createScenario(scenario).getOrThrow()
        scenarioRepository.addVote(
            ScenarioVote(
                id = "scenario-vote-a",
                scenarioId = scenario.id,
                participantId = "participant-a",
                vote = ScenarioVoteType.PREFER,
                createdAt = now
            )
        ).getOrThrow()
        scenarioRepository.selectFinalScenario(eventId, scenario.id).getOrThrow()

        accommodationRepository.createAccommodation(
            Accommodation(
                id = "lodging-phase6",
                eventId = eventId,
                name = "Hotel des Quais",
                type = AccommodationType.HOTEL,
                address = "1 Quai Wakeve, Bordeaux",
                capacity = 3,
                pricePerNight = 18_000,
                totalNights = 2,
                totalCost = 36_000,
                bookingStatus = BookingStatus.RESERVED,
                bookingUrl = "https://booking.example/wakeve-safe",
                checkInDate = "2026-07-18",
                checkOutDate = "2026-07-20",
                notes = "Refundable reservation",
                createdAt = now,
                updatedAt = now
            )
        )
        accommodationRepository.updateBookingStatus("lodging-phase6", BookingStatus.CONFIRMED)

        eventRepository.updateEventStatus(eventId, EventStatus.ORGANIZING, "2026-07-18T08:00:00Z").getOrThrow()
        transportRepository.markTransportNotNeeded(eventId, organizerId).getOrThrow()
        readinessRepository.markMeetingsNotNeeded(eventId, organizerId)
        meetingService.createMeeting(
            eventId = eventId,
            organizerId = organizerId,
            platform = MeetingPlatform.GOOGLE_MEET,
            title = "Final logistics checkpoint",
            description = "Confirm access, payments, and attendance",
            scheduledFor = Instant.parse("2026-07-17T18:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris"
        ).getOrThrow()

        val budget = budgetRepository.createBudget(eventId)
        budgetRepository.createBudgetItem(
            budgetId = budget.id,
            category = BudgetCategory.ACCOMMODATION,
            name = "Hotel deposit",
            description = "Two nights",
            estimatedCost = 360.0,
            sharedBy = confirmedParticipants
        )
        expenseRepository.createExpense(
            eventId = eventId,
            amount = 120.0,
            category = BudgetCategory.MEALS,
            payerId = organizerId,
            splitParticipantIds = confirmedParticipants,
            receiptMetadata = mapOf("sha256" to "phase6-receipt"),
            syncState = "PENDING"
        )
        paymentPotRepository.createPot(
            eventId = eventId,
            organizerId = organizerId,
            goalAmount = 480.0,
            title = "Weekend shared pot",
            tricountGroupId = "tricount-phase6",
            tricountGroupUrl = "https://tricount.com/group/phase6"
        )
        tricountHandoffRepository.linkHandoff(
            eventId = eventId,
            provider = "TRICOUNT",
            providerId = "tricount-phase6",
            providerUrl = "https://tricount.com/group/phase6",
            syncStatus = "LINKED"
        )

        val pendingBeforeFinalization = database.syncMetadataQueries.selectPending().executeAsList()
        assertTrue(
            criticalEntityTypes.all { expected ->
                pendingBeforeFinalization.any { it.entityType == expected || it.entityType.startsWith(expected) }
            },
            "E2E setup must queue offline-first writes for every critical section. " +
                "Queued=${pendingBeforeFinalization.map { it.entityType }}"
        )

        val blocked = eventRepository.updateEventStatus(
            id = eventId,
            status = EventStatus.FINALIZED,
            finalDate = "2026-07-18T08:00:00Z"
        )

        assertTrue(
            blocked.isFailure,
            "Finalization must be blocked until all critical offline-first writes are synced."
        )
        assertEquals(EventStatus.ORGANIZING, eventRepository.getEvent(eventId)?.status)

        markAllPendingSyncAsSynced()
        assertTrue(
            eventRepository.updateEventStatus(
                id = eventId,
                status = EventStatus.FINALIZED,
                finalDate = "2026-07-18T08:00:00Z"
            ).isSuccess,
            "After critical sync convergence, organizer finalization should complete locally."
        )
        assertEquals(EventStatus.FINALIZED, eventRepository.getEvent(eventId)?.status)
    }

    @Test
    fun `Phase6 failed critical sync retries and pending conflicts block finalization until resolved`() = runTest {
        val eventId = "phase6-conflict-retry"
        seedReadyOrganizingEvent(eventId)
        seedFailedCriticalSync(eventId, entityType = "transport_plan_selection")
        seedPendingCriticalConflict(eventId, fieldName = "payment_tricount")

        val blocked = eventRepository.updateEventStatus(
            id = eventId,
            status = EventStatus.FINALIZED,
            finalDate = "2026-07-18T08:00:00Z"
        )

        assertTrue(
            blocked.isFailure,
            "Pending CRITICAL conflicts and failed retryable sync operations must block FINALIZED."
        )
        assertEquals(EventStatus.ORGANIZING, eventRepository.getEvent(eventId)?.status)
    }

    @Test
    fun `Phase6 finalized events reject organization mutations as read only`() = runTest {
        val eventId = "phase6-finalized-readonly"
        seedReadyOrganizingEvent(eventId)
        eventRepository.updateEventStatus(eventId, EventStatus.FINALIZED, "2026-07-18T08:00:00Z").getOrThrow()

        val selectedScenarioMutation = scenarioRepository.selectFinalScenario(eventId, "scenario-$eventId")
        val transportMutation = transportRepository.markTransportNotNeeded(eventId, "organizer-1")
        val meetingMutation = meetingService.createMeeting(
            eventId = eventId,
            organizerId = "organizer-1",
            platform = MeetingPlatform.ZOOM,
            title = "Should be read only",
            description = null,
            scheduledFor = Instant.parse("2026-07-17T18:00:00Z"),
            duration = 1.hours,
            timezone = "Europe/Paris"
        )

        assertTrue(
            selectedScenarioMutation.isFailure,
            "After FINALIZED, scenario/destination/lodging decisions must be read-only."
        )
        assertTrue(
            transportMutation.isFailure,
            "After FINALIZED, transport decisions must be read-only."
        )
        assertTrue(
            meetingMutation.isFailure,
            "After FINALIZED, meeting creation must be read-only."
        )
    }

    private suspend fun seedReadyOrganizingEvent(eventId: String) {
        eventRepository.createEvent(
            eventFixture(
                eventId = eventId,
                organizerId = "organizer-1",
                participants = emptyList()
            )
        ).getOrThrow()
        eventRepository.addParticipant(eventId, "participant-a").getOrThrow()
        eventRepository.updateEventStatus(eventId, EventStatus.CONFIRMED, "2026-07-18T08:00:00Z").getOrThrow()
        markParticipantDateValidated(eventId, "organizer-1")
        markParticipantDateValidated(eventId, "participant-a")
        val scenario = Scenario(
            id = "scenario-$eventId",
            eventId = eventId,
            name = "Ready destination",
            dateOrPeriod = "2026-07-18",
            location = "Bordeaux",
            duration = 2,
            estimatedParticipants = 2,
            estimatedBudgetPerPerson = 200.0,
            description = "Selected destination and lodging context",
            status = ScenarioStatus.PROPOSED,
            createdAt = now,
            updatedAt = now
        )
        scenarioRepository.createScenario(scenario).getOrThrow()
        scenarioRepository.selectFinalScenario(eventId, scenario.id).getOrThrow()
        accommodationRepository.createAccommodation(
            Accommodation(
                id = "lodging-$eventId",
                eventId = eventId,
                name = "Ready lodging",
                type = AccommodationType.HOTEL,
                address = "1 Ready Street, Bordeaux",
                capacity = 2,
                pricePerNight = 12_000,
                totalNights = 2,
                totalCost = 24_000,
                bookingStatus = BookingStatus.CONFIRMED,
                bookingUrl = "https://booking.example/$eventId",
                checkInDate = "2026-07-18",
                checkOutDate = "2026-07-20",
                notes = "Confirmed readiness fixture",
                createdAt = now,
                updatedAt = now
            )
        )
        eventRepository.updateEventStatus(eventId, EventStatus.ORGANIZING, "2026-07-18T08:00:00Z").getOrThrow()
        transportRepository.markTransportNotNeeded(eventId, "organizer-1").getOrThrow()
        readinessRepository.markMeetingsNotNeeded(eventId, "organizer-1")
        val budget = budgetRepository.createBudget(eventId)
        budgetRepository.createBudgetItem(
            budgetId = budget.id,
            category = BudgetCategory.ACCOMMODATION,
            name = "Ready lodging baseline",
            description = "Finalization readiness fixture",
            estimatedCost = 240.0,
            sharedBy = listOf("organizer-1", "participant-a")
        )
        database.organizationReadinessDecisionQueries.upsertDecision(
            id = "budget-not-needed-$eventId",
            eventId = eventId,
            section = "BUDGET_BASELINE",
            notNeeded = 1L,
            decidedBy = "organizer-1",
            decidedAt = now
        )
        tricountHandoffRepository.markNotNeeded(eventId, "organizer-1")
        markAllPendingSyncAsSynced()
    }

    private fun eventFixture(
        eventId: String,
        organizerId: String,
        participants: List<String>
    ): Event = Event(
        id = eventId,
        title = "Phase 6 Complete Flow",
        description = "Complete event organization from creation to finalization",
        organizerId = organizerId,
        participants = participants,
        proposedSlots = listOf(
            TimeSlot(
                id = "slot-1",
                start = "2026-07-18T08:00:00Z",
                end = "2026-07-18T10:00:00Z",
                timezone = "Europe/Paris",
                timeOfDay = TimeOfDay.SPECIFIC
            )
        ),
        deadline = "2026-06-01T18:00:00Z",
        status = EventStatus.DRAFT,
        createdAt = now,
        updatedAt = now,
        eventType = EventType.OTHER
    )

    private fun markParticipantDateValidated(eventId: String, userId: String) {
        val participant = database.participantQueries
            .selectByEventIdAndUserId(eventId, userId)
            .executeAsOne()
        database.participantQueries.updateValidation(
            hasValidatedDate = 1L,
            updatedAt = now,
            id = participant.id
        )
    }

    private fun normalizeParticipantPrimaryKeys(eventId: String) {
        val participants = database.participantQueries.selectByEventId(eventId).executeAsList()
        participants.forEach { participant ->
            if (participant.id != participant.userId) {
                database.participantQueries.deleteParticipant(participant.id)
                database.participantQueries.insertParticipant(
                    id = participant.userId,
                    eventId = eventId,
                    userId = participant.userId,
                    role = participant.role,
                    hasValidatedDate = participant.hasValidatedDate,
                    joinedAt = participant.joinedAt,
                    updatedAt = participant.updatedAt
                )
            }
        }
    }

    private fun seedFailedCriticalSync(eventId: String, entityType: String) {
        database.syncMetadataQueries.insertSyncMetadataWithPayload(
            id = "failed-$eventId-$entityType",
            entityType = entityType,
            entityId = eventId,
            operation = "UPDATE",
            payload = """{"eventId":"$eventId","critical":true,"retryRequired":true}""",
            timestamp = "2026-05-22T11:00:00Z_$entityType",
            retryState = "FAILED",
            retryCount = 3L,
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
            local_updated_at = "2026-05-22T11:00:00Z",
            remote_updated_at = "2026-05-22T11:01:00Z",
            severity = "CRITICAL",
            resolution_strategy = "PENDING",
            resolved_by = null,
            resolved_at = null,
            created_at = "2026-05-22T11:02:00Z"
        )
    }

    private fun markAllPendingSyncAsSynced() {
        database.syncMetadataQueries.selectPending().executeAsList().forEach { pending ->
            database.syncMetadataQueries.markSynced(pending.id)
        }
    }

    private companion object {
        const val now = "2026-05-22T10:00:00Z"
        val criticalEntityTypes = setOf(
            "scenario",
            "scenario_vote",
            "scenario_selection",
            "lodging_selection",
            "transport_event_status",
            "meeting",
            "expense",
            "payment_pot",
            "tricount_handoff"
        )
    }
}

private class Phase6NoopPlatformCalendarService : PlatformCalendarService {
    override fun addEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun updateEvent(event: EnhancedCalendarEvent): Result<Unit> = Result.success(Unit)
    override fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
}

private class Phase6NoopNotificationService : NotificationServiceInterface {
    override suspend fun sendNotification(message: NotificationMessage): Result<Unit> = Result.success(Unit)
    override suspend fun registerPushToken(token: PushToken): Result<Unit> = Result.success(Unit)
    override suspend fun unregisterPushToken(userId: String, deviceId: String): Result<Unit> = Result.success(Unit)
    override suspend fun getUnreadNotifications(userId: String): List<NotificationMessage> = emptyList()
    override suspend fun markAsRead(notificationId: String): Result<Unit> = Result.success(Unit)
}
