package com.guyghost.wakeve.scenario

import com.guyghost.wakeve.TestDatabaseFactory
import com.guyghost.wakeve.accommodation.AccommodationRepository
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Accommodation
import com.guyghost.wakeve.models.AccommodationType
import com.guyghost.wakeve.models.BookingStatus
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.ScenarioRepository
import kotlinx.coroutines.runBlocking
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScenarioOfflineRepositoryPhase3Test {

    private lateinit var db: WakeveDb
    private lateinit var eventRepository: DatabaseEventRepository
    private lateinit var scenarioRepository: ScenarioRepository
    private lateinit var accommodationRepository: AccommodationRepository

    @BeforeTest
    fun setUp() {
        DatabaseProvider.resetDatabase()
        db = DatabaseProvider.getDatabase(TestDatabaseFactory())
        eventRepository = DatabaseEventRepository(db)
        scenarioRepository = ScenarioRepository(db)
        accommodationRepository = AccommodationRepository(db)
    }

    @Test
    fun `scenario vote is local first idempotent per participant and queued for sync`() = runBlocking {
        val eventId = "event-offline-scenario-vote"
        eventRepository.createEvent(eventFixture(eventId, EventStatus.COMPARING))
        scenarioRepository.createScenario(scenarioFixture("scenario-vote-sync", eventId))

        scenarioRepository.addVote(vote("vote-first", "scenario-vote-sync", "confirmed-user", ScenarioVoteType.PREFER))
        scenarioRepository.addVote(vote("vote-replacement", "scenario-vote-sync", "confirmed-user", ScenarioVoteType.AGAINST))

        val votes = scenarioRepository.getVotesByScenarioId("scenario-vote-sync")
        assertEquals(1, votes.size, "A second scenario vote from the same participant should replace the first one")
        assertEquals(ScenarioVoteType.AGAINST, votes.single().vote)

        val pending = db.syncMetadataQueries.selectPending().executeAsList()
        assertTrue(
            pending.any {
                it.entityType == "scenario_vote" &&
                    it.entityId == "scenario-vote-sync:confirmed-user" &&
                    it.operation == "UPSERT" &&
                    it.synced == 0L
            },
            "Scenario votes must create a pending local sync operation with an UPSERT contract"
        )
    }

    @Test
    fun `selected lodging conflict is resolved deterministically and synced locally`() = runBlocking {
        val eventId = "event-lodging-conflict"
        eventRepository.createEvent(eventFixture(eventId, EventStatus.COMPARING))
        accommodationRepository.createAccommodation(accommodationFixture("lodging-old", eventId, "Old hotel"))
        accommodationRepository.createAccommodation(accommodationFixture("lodging-new", eventId, "New apartment"))

        accommodationRepository.updateBookingStatus("lodging-old", BookingStatus.CONFIRMED)
        accommodationRepository.updateBookingStatus("lodging-new", BookingStatus.CONFIRMED)

        val confirmed = accommodationRepository.getConfirmedAccommodations(eventId)
        assertEquals(
            listOf("lodging-new"),
            confirmed.map { it.id },
            "Concurrent lodging selections should resolve deterministically with one selected lodging per event"
        )

        val pending = db.syncMetadataQueries.selectPending().executeAsList()
        assertTrue(
            pending.any {
                it.entityType == "lodging_selection" &&
                    it.entityId == eventId &&
                    it.operation == "CONFLICT_RESOLVED" &&
                    it.synced == 0L
            },
            "Resolving a lodging selection conflict should be visible as pending local sync metadata"
        )
    }

    private fun eventFixture(id: String, status: EventStatus): Event = Event(
        id = id,
        title = "Offline scenario event",
        description = "Local-first scenario repository test",
        organizerId = "organizer-1",
        participants = listOf("organizer-1", "confirmed-user"),
        proposedSlots = emptyList(),
        deadline = "2026-06-01T18:00:00Z",
        status = status,
        finalDate = "2026-06-10T09:00:00Z",
        createdAt = "2026-05-22T10:00:00Z",
        updatedAt = "2026-05-22T10:00:00Z"
    )

    private fun scenarioFixture(id: String, eventId: String): Scenario = Scenario(
        id = id,
        eventId = eventId,
        name = "Train and hotel plan",
        dateOrPeriod = "2026-06-10/2026-06-12",
        location = "Bordeaux, France",
        duration = 2,
        estimatedParticipants = 6,
        estimatedBudgetPerPerson = 260.0,
        description = "Scenario with destination and lodging",
        status = ScenarioStatus.PROPOSED,
        createdAt = "2026-05-22T10:00:00Z",
        updatedAt = "2026-05-22T10:00:00Z"
    )

    private fun vote(
        id: String,
        scenarioId: String,
        participantId: String,
        voteType: ScenarioVoteType
    ): ScenarioVote = ScenarioVote(
        id = id,
        scenarioId = scenarioId,
        participantId = participantId,
        vote = voteType,
        createdAt = "2026-05-22T11:00:00Z"
    )

    private fun accommodationFixture(
        id: String,
        eventId: String,
        name: String
    ): Accommodation = Accommodation(
        id = id,
        eventId = eventId,
        name = name,
        type = AccommodationType.HOTEL,
        address = "1 rue de la Paix, Bordeaux",
        capacity = 8,
        pricePerNight = 12_000,
        totalNights = 2,
        totalCost = 24_000,
        bookingStatus = BookingStatus.SEARCHING,
        bookingUrl = null,
        checkInDate = "2026-06-10",
        checkOutDate = "2026-06-12",
        notes = null,
        createdAt = "2026-05-22T10:00:00Z",
        updatedAt = "2026-05-22T10:00:00Z"
    )
}
