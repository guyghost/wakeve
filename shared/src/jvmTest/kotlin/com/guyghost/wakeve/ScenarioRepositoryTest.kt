package com.guyghost.wakeve

import com.guyghost.wakeve.database.WakevDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVoteType
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScenarioRepositoryTest {

    private lateinit var db: WakevDb
    private lateinit var repository: ScenarioRepository
    private lateinit var eventRepository: DatabaseEventRepository

    private fun createTestDatabase(): WakevDb {
        return DatabaseProvider.getDatabase(TestDatabaseFactory())
    }

    private suspend fun createTestEvent(eventId: String): Event {
        val event = Event(
            id = eventId,
            title = "Test Event",
            description = "Test event for scenarios",
            organizerId = "org-1",
            participants = emptyList(),
            proposedSlots = emptyList(),
            deadline = "2025-12-01T00:00:00Z",
            status = EventStatus.COMPARING,
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )
        eventRepository.createEvent(event)
        return event
    }

    fun setup() = runBlocking {
        DatabaseProvider.resetDatabase()  // Clear cached database
        db = createTestDatabase()
        repository = ScenarioRepository(db)
        eventRepository = DatabaseEventRepository(db)
        
        // Create common test events
        createTestEvent("event-1")
        createTestEvent("event-create-1")
        createTestEvent("event-update-1")
        createTestEvent("event-vote-1")
        createTestEvent("event-delete-1")
    }

    @Test
    fun testDatabaseConnection() {
        val db = DatabaseProvider.getDatabase(TestDatabaseFactory())
        assertNotNull(db, "Database should be created")
        assertNotNull(db.scenarioQueries, "Scenario queries should exist")
        assertNotNull(db.scenarioVoteQueries, "ScenarioVote queries should exist")
    }

    @Test
    fun testCreateAndRetrieveScenario() = runBlocking {
        setup()

        val scenario = Scenario(
            id = "scenario-create-1",
            eventId = "event-create-1",
            name = "Paris Weekend",
            dateOrPeriod = "2025-12-15/2025-12-17",
            location = "Paris, France",
            duration = 2,
            estimatedParticipants = 5,
            estimatedBudgetPerPerson = 450.0,
            description = "Weekend trip to Paris",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )

        val result = repository.createScenario(scenario)
        if (result.isFailure) {
            val exception = result.exceptionOrNull()
            throw AssertionError("Scenario creation failed: ${exception?.message}", exception)
        }
        assertTrue(result.isSuccess, "Scenario creation should succeed")
        assertEquals(scenario, result.getOrNull(), "Created scenario should match input")

        val retrieved = repository.getScenarioById("scenario-create-1")
        assertNotNull(retrieved, "Scenario should be retrievable")
        assertEquals("Paris Weekend", retrieved?.name, "Name should match")
        assertEquals(ScenarioStatus.PROPOSED, retrieved?.status, "Status should match")
    }

    @Test
    fun testGetScenariosByEventId() = runBlocking {
        setup()

        val scenario1 = Scenario(
            id = "scenario-1",
            eventId = "event-1",
            name = "Paris Weekend",
            dateOrPeriod = "2025-12-15/2025-12-17",
            location = "Paris, France",
            duration = 2,
            estimatedParticipants = 5,
            estimatedBudgetPerPerson = 450.0,
            description = "Weekend trip to Paris",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )

        val scenario2 = Scenario(
            id = "scenario-2",
            eventId = "event-1",
            name = "Barcelona Week",
            dateOrPeriod = "2025-12-20/2025-12-27",
            location = "Barcelona, Spain",
            duration = 7,
            estimatedParticipants = 6,
            estimatedBudgetPerPerson = 850.0,
            description = "Week-long trip to Barcelona",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-11-20T11:00:00Z",
            updatedAt = "2025-11-20T11:00:00Z"
        )

        repository.createScenario(scenario1)
        repository.createScenario(scenario2)

        val scenarios = repository.getScenariosByEventId("event-1")
        assertEquals(2, scenarios.size, "Should have 2 scenarios")
        assertTrue(scenarios.any { it.name == "Paris Weekend" }, "Should contain Paris scenario")
        assertTrue(scenarios.any { it.name == "Barcelona Week" }, "Should contain Barcelona scenario")
    }

    @Test
    fun testUpdateScenario() = runBlocking {
        setup()

        val scenario = Scenario(
            id = "scenario-1",
            eventId = "event-1",
            name = "Paris Weekend",
            dateOrPeriod = "2025-12-15/2025-12-17",
            location = "Paris, France",
            duration = 2,
            estimatedParticipants = 5,
            estimatedBudgetPerPerson = 450.0,
            description = "Weekend trip to Paris",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )

        repository.createScenario(scenario)

        val updated = scenario.copy(
            estimatedBudgetPerPerson = 500.0,
            description = "Updated description"
        )

        val result = repository.updateScenario(updated)
        assertTrue(result.isSuccess, "Update should succeed")

        val retrieved = repository.getScenarioById("scenario-1")
        assertEquals(500.0, retrieved?.estimatedBudgetPerPerson, "Budget should be updated")
        assertEquals("Updated description", retrieved?.description, "Description should be updated")
    }

    @Test
    fun testUpdateScenarioStatus() = runBlocking {
        setup()

        val scenario = Scenario(
            id = "scenario-1",
            eventId = "event-1",
            name = "Paris Weekend",
            dateOrPeriod = "2025-12-15/2025-12-17",
            location = "Paris, France",
            duration = 2,
            estimatedParticipants = 5,
            estimatedBudgetPerPerson = 450.0,
            description = "Weekend trip to Paris",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )

        repository.createScenario(scenario)

        val result = repository.updateScenarioStatus("scenario-1", ScenarioStatus.SELECTED)
        assertTrue(result.isSuccess, "Status update should succeed")

        val retrieved = repository.getScenarioById("scenario-1")
        assertEquals(ScenarioStatus.SELECTED, retrieved?.status, "Status should be updated")
    }

    @Test
    fun testAddScenarioVote() = runBlocking {
        setup()

        val scenario = Scenario(
            id = "scenario-1",
            eventId = "event-1",
            name = "Paris Weekend",
            dateOrPeriod = "2025-12-15/2025-12-17",
            location = "Paris, France",
            duration = 2,
            estimatedParticipants = 5,
            estimatedBudgetPerPerson = 450.0,
            description = "Weekend trip to Paris",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )

        repository.createScenario(scenario)

        val vote = ScenarioVote(
            id = "vote-1",
            scenarioId = "scenario-1",
            participantId = "participant-1",
            vote = ScenarioVoteType.PREFER,
            createdAt = "2025-11-21T10:00:00Z"
        )

        val result = repository.addVote(vote)
        assertTrue(result.isSuccess, "Adding vote should succeed")

        val votes = repository.getVotesByScenarioId("scenario-1")
        assertEquals(1, votes.size, "Should have 1 vote")
        assertEquals(ScenarioVoteType.PREFER, votes[0].vote, "Vote type should match")
    }

    @Test
    fun testUpdateExistingVote() = runBlocking {
        setup()

        val scenario = Scenario(
            id = "scenario-1",
            eventId = "event-1",
            name = "Paris Weekend",
            dateOrPeriod = "2025-12-15/2025-12-17",
            location = "Paris, France",
            duration = 2,
            estimatedParticipants = 5,
            estimatedBudgetPerPerson = 450.0,
            description = "Weekend trip to Paris",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )

        repository.createScenario(scenario)

        val vote1 = ScenarioVote(
            id = "vote-1",
            scenarioId = "scenario-1",
            participantId = "participant-1",
            vote = ScenarioVoteType.PREFER,
            createdAt = "2025-11-21T10:00:00Z"
        )

        repository.addVote(vote1)

        // Same participant changes their vote
        val vote2 = ScenarioVote(
            id = "vote-2",
            scenarioId = "scenario-1",
            participantId = "participant-1",
            vote = ScenarioVoteType.AGAINST,
            createdAt = "2025-11-21T11:00:00Z"
        )

        val result = repository.updateVote(vote2)
        assertTrue(result.isSuccess, "Updating vote should succeed")

        val votes = repository.getVotesByScenarioId("scenario-1")
        assertEquals(1, votes.size, "Should still have 1 vote")
        assertEquals(ScenarioVoteType.AGAINST, votes[0].vote, "Vote should be updated")
    }

    @Test
    fun testGetVotingResultForScenario() = runBlocking {
        setup()

        val scenario = Scenario(
            id = "scenario-1",
            eventId = "event-1",
            name = "Paris Weekend",
            dateOrPeriod = "2025-12-15/2025-12-17",
            location = "Paris, France",
            duration = 2,
            estimatedParticipants = 5,
            estimatedBudgetPerPerson = 450.0,
            description = "Weekend trip to Paris",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )

        repository.createScenario(scenario)

        // Add votes
        repository.addVote(ScenarioVote("vote-1", "scenario-1", "p1", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"))
        repository.addVote(ScenarioVote("vote-2", "scenario-1", "p2", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"))
        repository.addVote(ScenarioVote("vote-3", "scenario-1", "p3", ScenarioVoteType.NEUTRAL, "2025-11-21T10:00:00Z"))
        repository.addVote(ScenarioVote("vote-4", "scenario-1", "p4", ScenarioVoteType.AGAINST, "2025-11-21T10:00:00Z"))

        val result = repository.getVotingResult("scenario-1")
        assertNotNull(result, "Voting result should exist")
        assertEquals(2, result.preferCount, "Should have 2 PREFER votes")
        assertEquals(1, result.neutralCount, "Should have 1 NEUTRAL vote")
        assertEquals(1, result.againstCount, "Should have 1 AGAINST vote")
        assertEquals(4, result.totalVotes, "Should have 4 total votes")
        assertEquals(4, result.score, "Score should be 2*2 + 1*1 - 1*1 = 4")
    }

    @Test
    fun testDeleteScenario() = runBlocking {
        setup()

        val scenario = Scenario(
            id = "scenario-1",
            eventId = "event-1",
            name = "Paris Weekend",
            dateOrPeriod = "2025-12-15/2025-12-17",
            location = "Paris, France",
            duration = 2,
            estimatedParticipants = 5,
            estimatedBudgetPerPerson = 450.0,
            description = "Weekend trip to Paris",
            status = ScenarioStatus.PROPOSED,
            createdAt = "2025-11-20T10:00:00Z",
            updatedAt = "2025-11-20T10:00:00Z"
        )

        repository.createScenario(scenario)

        // Add a vote
        repository.addVote(ScenarioVote("vote-1", "scenario-1", "p1", ScenarioVoteType.PREFER, "2025-11-21T10:00:00Z"))

        val result = repository.deleteScenario("scenario-1")
        assertTrue(result.isSuccess, "Delete should succeed")

        val retrieved = repository.getScenarioById("scenario-1")
        assertNull(retrieved, "Scenario should be deleted")

        val votes = repository.getVotesByScenarioId("scenario-1")
        assertEquals(0, votes.size, "Votes should be cascade deleted")
    }
}
