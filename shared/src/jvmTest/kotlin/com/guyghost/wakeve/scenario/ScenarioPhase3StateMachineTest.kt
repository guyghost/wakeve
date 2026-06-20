package com.guyghost.wakeve.scenario

import com.guyghost.wakeve.TestDatabaseFactory
import com.guyghost.wakeve.database.DatabaseProvider
import com.guyghost.wakeve.database.WakeveDb
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVote
import com.guyghost.wakeve.models.ScenarioVoteType
import com.guyghost.wakeve.presentation.state.ScenarioManagementContract
import com.guyghost.wakeve.presentation.statemachine.ScenarioManagementStateMachine
import com.guyghost.wakeve.presentation.statemachine.scenarioCreateFailureMessage
import com.guyghost.wakeve.presentation.statemachine.scenarioDeleteFailureMessage
import com.guyghost.wakeve.presentation.statemachine.scenarioEventStatusUpdateFailureMessage
import com.guyghost.wakeve.presentation.statemachine.scenarioLoadFailureMessage
import com.guyghost.wakeve.presentation.statemachine.scenarioMatrixGenerationFailureMessage
import com.guyghost.wakeve.presentation.statemachine.scenarioMatrixPublishFailureMessage
import com.guyghost.wakeve.presentation.statemachine.scenarioReloadFailureMessage
import com.guyghost.wakeve.presentation.statemachine.scenarioSelectionFailureMessage
import com.guyghost.wakeve.presentation.statemachine.scenarioUpdateFailureMessage
import com.guyghost.wakeve.presentation.statemachine.scenarioVoteFailureMessage
import com.guyghost.wakeve.presentation.usecase.CreateScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.DeleteScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.LoadScenariosUseCase
import com.guyghost.wakeve.presentation.usecase.UpdateScenarioUseCase
import com.guyghost.wakeve.presentation.usecase.VoteScenarioUseCase
import com.guyghost.wakeve.repository.DatabaseEventRepository
import com.guyghost.wakeve.repository.ScenarioRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ScenarioPhase3StateMachineTest {

    private lateinit var db: WakeveDb
    private lateinit var eventRepository: DatabaseEventRepository
    private lateinit var scenarioRepository: ScenarioRepository

    @BeforeTest
    fun setUp() {
        DatabaseProvider.resetDatabase()
        db = DatabaseProvider.getDatabase(TestDatabaseFactory())
        eventRepository = DatabaseEventRepository(db)
        scenarioRepository = ScenarioRepository(db)
    }

    @Test
    fun `confirmed event can create first scenario and enter comparing mode`() = runTest {
        val eventId = "event-confirmed-create-scenario"
        eventRepository.createEvent(eventFixture(eventId, EventStatus.CONFIRMED))
        val stateMachine = createStateMachine(this)

        stateMachine.dispatch(
            ScenarioManagementContract.Intent.CreateScenario(
                scenarioFixture(
                    id = "scenario-from-destination",
                    eventId = eventId,
                    name = "Lyon city break",
                    location = "Lyon, France"
                )
            )
        )
        advanceUntilIdle()

        val scenarios = scenarioRepository.getScenariosByEventId(eventId)
        assertEquals(1, scenarios.size)
        assertEquals(
            EventStatus.COMPARING,
            eventRepository.getEvent(eventId)?.status,
            "Creating the first logistics scenario after date confirmation should move the event into COMPARING"
        )
        assertEquals(EventStatus.COMPARING, stateMachine.state.value.eventStatus)
    }

    @Test
    fun `scenario votes are scored and ranked with prefer neutral against weights`() = runTest {
        val eventId = "event-scenario-ranking"
        eventRepository.createEvent(eventFixture(eventId, EventStatus.COMPARING))
        scenarioRepository.createScenario(scenarioFixture("scenario-low", eventId, "Budget hostel", "Lisbon, Portugal"))
        scenarioRepository.createScenario(scenarioFixture("scenario-high", eventId, "Central apartment", "Porto, Portugal"))

        scenarioRepository.addVote(vote("vote-1", "scenario-low", "alice", ScenarioVoteType.PREFER))
        scenarioRepository.addVote(vote("vote-2", "scenario-low", "bob", ScenarioVoteType.AGAINST))
        scenarioRepository.addVote(vote("vote-3", "scenario-high", "alice", ScenarioVoteType.PREFER))
        scenarioRepository.addVote(vote("vote-4", "scenario-high", "bob", ScenarioVoteType.PREFER))
        scenarioRepository.addVote(vote("vote-5", "scenario-high", "chloe", ScenarioVoteType.NEUTRAL))

        val stateMachine = createStateMachine(this)
        stateMachine.dispatch(ScenarioManagementContract.Intent.LoadScenariosForEvent(eventId, "alice"))
        advanceUntilIdle()

        assertEquals(1, stateMachine.state.value.votingResults.getValue("scenario-low").score)
        assertEquals(5, stateMachine.state.value.votingResults.getValue("scenario-high").score)
        assertEquals(
            listOf("scenario-high", "scenario-low"),
            stateMachine.state.value.getScenariosRanked().map { it.scenario.id }
        )
    }

    @Test
    fun `organizer selecting final scenario selects one rejects the rest and navigates to meetings`() = runTest {
        val eventId = "event-select-final-scenario"
        val organizerId = "organizer-1"
        eventRepository.createEvent(eventFixture(eventId, EventStatus.COMPARING, organizerId))
        scenarioRepository.createScenario(scenarioFixture("scenario-selected", eventId, "Final chalet", "Annecy, France"))
        scenarioRepository.createScenario(scenarioFixture("scenario-rejected", eventId, "Fallback hotel", "Grenoble, France"))

        val stateMachine = createStateMachine(this)
        stateMachine.dispatch(ScenarioManagementContract.Intent.LoadScenariosForEvent(eventId, organizerId))
        advanceUntilIdle()
        stateMachine.dispatch(
            ScenarioManagementContract.Intent.SelectScenarioAsFinal(
                eventId = eventId,
                scenarioId = "scenario-selected",
                userId = organizerId
            )
        )
        advanceUntilIdle()
        val effects = collectSideEffects(stateMachine, 2)

        assertEquals(ScenarioStatus.SELECTED, scenarioRepository.getScenarioById("scenario-selected")?.status)
        assertEquals(
            ScenarioStatus.REJECTED,
            scenarioRepository.getScenarioById("scenario-rejected")?.status,
            "Selecting a final scenario should make competing scenarios explicitly non-selected"
        )
        assertTrue(
            effects.any {
                it is ScenarioManagementContract.SideEffect.NavigateTo &&
                    it.route == "meetings/$eventId"
            },
            "Selecting the final destination/lodging scenario should prepare the meetings step"
        )
    }

    @Test
    fun `unconfirmed or non participant cannot vote or open scenario details`() = runTest {
        val eventId = "event-scenario-access"
        eventRepository.createEvent(eventFixture(eventId, EventStatus.COMPARING))
        scenarioRepository.createScenario(scenarioFixture("scenario-access", eventId, "Locked villa", "Nice, France"))
        val stateMachine = createStateMachine(this)

        stateMachine.dispatch(ScenarioManagementContract.Intent.LoadScenariosForEvent(eventId, "pending-user"))
        advanceUntilIdle()
        stateMachine.dispatch(
            ScenarioManagementContract.Intent.VoteScenario(
                scenarioId = "scenario-access",
                vote = ScenarioVoteType.PREFER
            )
        )
        advanceUntilIdle()

        assertTrue(
            scenarioRepository.getVotesByScenarioId("scenario-access").isEmpty(),
            "Scenario votes must be accepted only from confirmed participants"
        )
        assertTrue(
            stateMachine.state.value.error?.contains("confirmed participant", ignoreCase = true) == true,
            "The state machine should explain that scenario details are locked until participation is confirmed"
        )
    }

    @Test
    fun `scenario details fail closed when event repository is unavailable`() = runTest {
        val eventId = "event-scenario-missing-event-repository"
        eventRepository.createEvent(eventFixture(eventId, EventStatus.COMPARING))
        scenarioRepository.createScenario(scenarioFixture("scenario-locked", eventId, "Private chalet", "Chamonix, France"))
        val stateMachine = createStateMachine(scope = this, includeEventRepository = false)

        stateMachine.dispatch(ScenarioManagementContract.Intent.LoadScenariosForEvent(eventId, "pending-user"))
        advanceUntilIdle()
        stateMachine.dispatch(ScenarioManagementContract.Intent.SelectScenario("scenario-locked"))
        advanceUntilIdle()

        assertEquals(null, stateMachine.state.value.selectedScenario)
        assertTrue(
            stateMachine.state.value.error?.contains("confirmed participant", ignoreCase = true) == true,
            "Scenario details must not open when participant access cannot be verified"
        )
    }

    @Test
    fun `failure messages do not expose throwable details`() {
        val messages = listOf(
            scenarioLoadFailureMessage(),
            scenarioCreateFailureMessage(),
            scenarioUpdateFailureMessage(),
            scenarioDeleteFailureMessage(),
            scenarioEventStatusUpdateFailureMessage(),
            scenarioSelectionFailureMessage(),
            scenarioMatrixGenerationFailureMessage(),
            scenarioMatrixPublishFailureMessage(),
            scenarioVoteFailureMessage(),
            scenarioReloadFailureMessage()
        )

        messages.forEach { message ->
            assertFalse(message.contains("SECRET"))
            assertFalse(message.contains("token="))
            assertFalse(message.contains("internal.local"))
            assertFalse(message.contains("SQL"))
        }
    }

    @Test
    fun `state machine does not use throwable messages for UI errors`() {
        val source = projectFile("shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/ScenarioManagementStateMachine.kt").readText()
        val throwableMessage = listOf("error", ".message").joinToString("")
        val nullableMessage = listOf("message", " ?:").joinToString("")

        assertFalse(source.contains(throwableMessage))
        assertFalse(source.contains(nullableMessage))
    }

    private fun createStateMachine(
        scope: kotlinx.coroutines.CoroutineScope,
        includeEventRepository: Boolean = true
    ) =
        ScenarioManagementStateMachine(
            loadScenariosUseCase = LoadScenariosUseCase(scenarioRepository),
            createScenarioUseCase = CreateScenarioUseCase(scenarioRepository),
            voteScenarioUseCase = VoteScenarioUseCase(scenarioRepository),
            updateScenarioUseCase = UpdateScenarioUseCase(scenarioRepository),
            deleteScenarioUseCase = DeleteScenarioUseCase(scenarioRepository),
            eventRepository = eventRepository.takeIf { includeEventRepository },
            scenarioRepository = scenarioRepository,
            scope = scope
        )

    private suspend fun collectSideEffects(
        stateMachine: ScenarioManagementStateMachine,
        count: Int
    ): List<ScenarioManagementContract.SideEffect> =
        withTimeout(1_000) {
            buildList {
                repeat(count) {
                    add(stateMachine.sideEffect.first())
                }
            }
        }

    private fun eventFixture(
        id: String,
        status: EventStatus,
        organizerId: String = "organizer-1"
    ): Event = Event(
        id = id,
        title = "Phase 3 event",
        description = "Scenario organization test",
        organizerId = organizerId,
        participants = listOf(organizerId, "confirmed-user"),
        proposedSlots = emptyList(),
        deadline = "2026-06-01T18:00:00Z",
        status = status,
        finalDate = "2026-06-10T09:00:00Z",
        createdAt = "2026-05-22T10:00:00Z",
        updatedAt = "2026-05-22T10:00:00Z"
    )

    private fun scenarioFixture(
        id: String,
        eventId: String,
        name: String,
        location: String
    ): Scenario = Scenario(
        id = id,
        eventId = eventId,
        name = name,
        dateOrPeriod = "2026-06-10/2026-06-12",
        location = location,
        duration = 2,
        estimatedParticipants = 6,
        estimatedBudgetPerPerson = 240.0,
        description = "Destination and lodging option",
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

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }
}
