package com.guyghost.wakeve.repository

import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.test.createTestEvent
import com.guyghost.wakeve.test.createTestTimeSlot
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * QA iOS 2026-09-27 (IOS-1): selecting a final scenario after a confirmed date
 * failed with "Failed to update event status" because the COMPARING -> CONFIRMED
 * write re-inserted the already existing confirmedDate row. The failure happened
 * after the status UPDATE, outside any transaction, which left a dangling
 * aggregate write authorization that bricked every later lifecycle write.
 */
class ScenarioFinalSelectionStatusRedTest {

    private lateinit var db: WakeveDb
    private lateinit var eventRepository: DatabaseEventRepository
    private lateinit var scenarioRepository: ScenarioRepository

    @BeforeTest
    fun setup() {
        db = createFreshTestDatabase()
        eventRepository = DatabaseEventRepository(
            db,
            ConfirmationClock { Instant.parse("2026-09-27T21:00:00Z") }
        )
        scenarioRepository = ScenarioRepository(db)
    }

    private suspend fun confirmedEventWithSelectedScenario(): String {
        val eventId = "event-lisbon"
        val created = eventRepository.createEvent(
            createTestEvent(
                id = eventId,
                title = "Road trip Lisbonne",
                description = "Une semaine au Portugal",
                organizerId = "org-alice",
                proposedSlots = listOf(
                    createTestTimeSlot(id = "slot-1", start = "2026-10-17T08:00:00Z", end = "2026-10-17T20:00:00Z"),
                    createTestTimeSlot(id = "slot-2", start = "2026-10-24T08:00:00Z", end = "2026-10-24T20:00:00Z")
                ),
                deadline = "2026-10-04T18:00:00Z",
                status = EventStatus.DRAFT,
                createdAt = "2026-09-27T20:00:00Z",
                updatedAt = "2026-09-27T20:00:00Z"
            )
        )
        assertTrue(created.isSuccess, "setup: create ${created.exceptionOrNull()}")
        assertTrue(eventRepository.addParticipant(eventId, "bruno").isSuccess, "setup: participant")
        val polling = eventRepository.updateEventStatus(eventId, EventStatus.POLLING, null)
        assertTrue(polling.isSuccess, "setup: polling ${polling.exceptionOrNull()}")
        val vote = eventRepository.addVote(eventId, "bruno", "slot-2", Vote.YES)
        assertTrue(vote.isSuccess, "setup: vote ${vote.exceptionOrNull()}")

        val confirmed = eventRepository.confirmEventDate(eventId, "slot-2", "org-alice")
        assertTrue(confirmed.isSuccess, "setup: confirm ${confirmed.exceptionOrNull()}")

        val scenario = scenarioRepository.createScenario(
            Scenario(
                id = "scenario-alfama",
                eventId = eventId,
                name = "Airbnb Alfama",
                dateOrPeriod = "2026-10-24",
                location = "Lisbonne, Portugal",
                duration = 1,
                estimatedParticipants = 6,
                estimatedBudgetPerPerson = 450.0,
                description = "Option a comparer",
                status = ScenarioStatus.PROPOSED,
                createdAt = "2026-09-27T21:30:00Z",
                updatedAt = "2026-09-27T21:30:00Z"
            )
        )
        assertTrue(scenario.isSuccess, "setup: scenario ${scenario.exceptionOrNull()}")
        assertEquals(EventStatus.COMPARING, eventRepository.getEvent(eventId)?.status)

        val selected = scenarioRepository.selectFinalScenario(eventId, "scenario-alfama")
        assertTrue(selected.isSuccess, "setup: select ${selected.exceptionOrNull()}")
        return eventId
    }

    @Test
    fun comparingBackToConfirmedSucceedsWhenDateWasAlreadyConfirmed() = runBlocking {
        val eventId = confirmedEventWithSelectedScenario()
        val finalDate = eventRepository.getEvent(eventId)?.finalDate

        val result = eventRepository.updateEventStatus(eventId, EventStatus.CONFIRMED, finalDate)

        assertTrue(result.isSuccess, "COMPARING -> CONFIRMED must succeed: ${result.exceptionOrNull()}")
        assertEquals(EventStatus.CONFIRMED, eventRepository.getEvent(eventId)?.status)
        assertNull(
            db.invitationExperienceQueries.selectAggregateWriteAuthorization(eventId).executeAsOneOrNull(),
            "No aggregate write authorization may outlive the status write"
        )
    }

    @Test
    fun reconfirmationKeepsTheSlotChosenByThePoll() = runBlocking {
        val eventId = confirmedEventWithSelectedScenario()
        val before = db.confirmedDateQueries.selectByEventId(eventId).executeAsOne()

        eventRepository.updateEventStatus(eventId, EventStatus.CONFIRMED, eventRepository.getEvent(eventId)?.finalDate)

        val after = db.confirmedDateQueries.selectByEventId(eventId).executeAsOne()
        assertEquals(before.timeslotId, after.timeslotId, "The poll decision (slot-2) must not be replaced by the first slot")
    }

    @Test
    fun lifecycleContinuesToOrganizingAfterScenarioSelection() = runBlocking {
        val eventId = confirmedEventWithSelectedScenario()
        eventRepository.updateEventStatus(eventId, EventStatus.CONFIRMED, eventRepository.getEvent(eventId)?.finalDate)

        val organizing = eventRepository.updateEventStatus(eventId, EventStatus.ORGANIZING, null)

        assertTrue(organizing.isSuccess, "CONFIRMED -> ORGANIZING must succeed: ${organizing.exceptionOrNull()}")
        assertEquals(EventStatus.ORGANIZING, eventRepository.getEvent(eventId)?.status)
    }

    @Test
    fun eventBrickedByALegacyDanglingStatusAuthorizationRecovers() = runBlocking {
        val eventId = confirmedEventWithSelectedScenario()
        val aggregate = db.eventQueries.selectById(eventId).executeAsOne()
        // State left on devices by the pre-fix writer: status already written,
        // authorization of that same status writer never cleared.
        db.invitationExperienceQueries.authorizeAggregateWrite(
            writer_schema_version = 1,
            operation_id = "event-status:$eventId:${aggregate.aggregateRevision}:CONFIRMED",
            created_at = "2026-09-27T21:34:52Z",
            id = eventId,
            aggregateRevision = aggregate.aggregateRevision,
            aggregateSchemaVersion = 1
        )
        db.eventQueries.advanceAggregateRevisionIfCurrent(
            updatedAt = "2026-09-27T21:34:53Z",
            id = eventId,
            aggregateRevision = aggregate.aggregateRevision
        )

        val result = eventRepository.updateEventStatus(eventId, EventStatus.CONFIRMED, null)

        assertTrue(result.isSuccess, "A stale status authorization must not fence the event forever: ${result.exceptionOrNull()}")
        assertNull(db.invitationExperienceQueries.selectAggregateWriteAuthorization(eventId).executeAsOneOrNull())
    }

    @Test
    fun failedStatusWriteLeavesNoPartialStateBehind() = runBlocking {
        val eventId = confirmedEventWithSelectedScenario()
        val revisionBefore = db.eventQueries.selectById(eventId).executeAsOne().aggregateRevision

        // FINALIZED from COMPARING is refused by the lifecycle rules.
        val refused = eventRepository.updateEventStatus(eventId, EventStatus.FINALIZED, null)

        assertTrue(refused.isFailure)
        val after = db.eventQueries.selectById(eventId).executeAsOne()
        assertEquals(EventStatus.COMPARING.name, after.status)
        assertEquals(revisionBefore, after.aggregateRevision)
        assertNull(db.invitationExperienceQueries.selectAggregateWriteAuthorization(eventId).executeAsOneOrNull())
    }
}
