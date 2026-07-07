package com.guyghost.wakeve.scenario

import com.guyghost.wakeve.createFreshTestDatabase
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.repository.ScenarioRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertIs

class ScenarioRepositoryContractTest {

    private lateinit var database: WakeveDb
    private lateinit var repository: ScenarioRepository

    @BeforeTest
    fun setUp() {
        database = createFreshTestDatabase()
        repository = ScenarioRepository(database)
        seedEvent("event-1")
    }

    @Test
    fun `updateScenario fails when scenario does not exist`() = runTest {
        val result = repository.updateScenario(scenario("missing-scenario"))

        assertNotFound(result, "Scenario not found: missing-scenario")
    }

    @Test
    fun `updateScenarioStatus fails when scenario does not exist`() = runTest {
        val result = repository.updateScenarioStatus("missing-scenario", ScenarioStatus.SELECTED)

        assertNotFound(result, "Scenario not found: missing-scenario")
    }

    @Test
    fun `deleteScenario fails when scenario does not exist`() = runTest {
        val result = repository.deleteScenario("missing-scenario")

        assertNotFound(result, "Scenario not found: missing-scenario")
    }

    @Test
    fun `updateVote fails when vote does not exist`() = runTest {
        val result = repository.updateVote(
            ScenarioVote(
                id = "missing-vote",
                scenarioId = "missing-scenario",
                participantId = "participant-1",
                vote = ScenarioVoteType.PREFER,
                createdAt = "2026-06-20T10:00:00Z"
            )
        )

        assertNotFound(result, "Scenario vote not found: missing-scenario/participant-1")
    }

    @Test
    fun `addVote fails when scenario does not exist`() = runTest {
        val result = repository.addVote(vote(scenarioId = "missing-scenario"))

        assertNotFound(result, "Scenario not found: missing-scenario")
    }

    @Test
    fun `deleteVote fails when vote does not exist`() = runTest {
        val result = repository.deleteVote("missing-scenario", "participant-1")

        assertNotFound(result, "Scenario vote not found: missing-scenario/participant-1")
    }

    private fun assertNotFound(result: Result<*>, expectedMessage: String) {
        assertFalse(result.isSuccess)
        val error = assertIs<IllegalArgumentException>(result.exceptionOrNull())
        assertContains(error.message.orEmpty(), expectedMessage)
    }

    private fun scenario(id: String): Scenario = Scenario(
        id = id,
        eventId = "event-1",
        name = "Paris Weekend",
        dateOrPeriod = "2026-07-10/2026-07-12",
        location = "Paris",
        duration = 2,
        estimatedParticipants = 6,
        estimatedBudgetPerPerson = 320.0,
        description = "Weekend plan",
        status = ScenarioStatus.PROPOSED,
        createdAt = "2026-06-20T09:00:00Z",
        updatedAt = "2026-06-20T09:00:00Z"
    )

    private fun vote(scenarioId: String): ScenarioVote = ScenarioVote(
        id = "vote-1",
        scenarioId = scenarioId,
        participantId = "participant-1",
        vote = ScenarioVoteType.PREFER,
        createdAt = "2026-06-20T10:00:00Z"
    )

    private fun seedEvent(eventId: String) {
        database.eventQueries.insertEvent(
            id = eventId,
            organizerId = "organizer-1",
            title = "Event",
            description = "Description",
            status = "COMPARING",
            deadline = "2026-06-30T00:00:00Z",
            createdAt = "2026-06-20T00:00:00Z",
            updatedAt = "2026-06-20T00:00:00Z",
            version = 1,
            eventType = "OTHER",
            eventTypeCustom = null,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null,
            isSample = 0L
        )
    }
}
