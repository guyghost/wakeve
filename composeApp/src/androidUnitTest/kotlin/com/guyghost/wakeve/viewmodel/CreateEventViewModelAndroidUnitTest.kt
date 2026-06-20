package com.guyghost.wakeve.viewmodel

import com.guyghost.wakeve.access.ParticipantRepositoryRecord
import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.analytics.AnalyticsProvider
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventPlanningMode
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.Poll
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.TimeSlot
import com.guyghost.wakeve.models.Vote
import com.guyghost.wakeve.repository.EventRepositoryInterface
import com.guyghost.wakeve.repository.OrderBy
import com.guyghost.wakeve.workflow.WorkflowOutboxRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class CreateEventViewModelAndroidUnitTest {
    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var repository: FakeCreateEventRepository
    private lateinit var analyticsProvider: RecordingAnalyticsProvider
    private lateinit var viewModel: CreateEventViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeCreateEventRepository()
        analyticsProvider = RecordingAnalyticsProvider()
        viewModel = CreateEventViewModel(repository, analyticsProvider)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createEventUsesGenericMessageWhenRepositoryReturnsFailure() = runTest {
        repository.createEventFailure = IllegalStateException(
            "SQL constraint failed for user secret@example.com token=SECRET"
        )

        fillRequiredFields()
        viewModel.createEvent()
        advanceUntilIdle()

        assertEquals(eventCreationFailureMessage(), viewModel.creationError.value)
        assertLastErrorContextEquals(eventCreationFailureAnalyticsContext())
        assertDoesNotExposeSensitiveDetails(viewModel.creationError.value.orEmpty())
        assertDoesNotExposeSensitiveDetails(lastErrorContext())
    }

    @Test
    fun createEventUsesGenericMessageWhenRepositoryThrows() = runTest {
        repository.throwOnCreate = IllegalStateException(
            "Backend 500 while creating draft for secret@example.com token=SECRET"
        )

        fillRequiredFields()
        viewModel.createEvent()
        advanceUntilIdle()

        assertEquals(eventCreationFailureMessage(), viewModel.creationError.value)
        assertLastErrorContextEquals(eventCreationFailureAnalyticsContext())
        assertDoesNotExposeSensitiveDetails(viewModel.creationError.value.orEmpty())
        assertDoesNotExposeSensitiveDetails(lastErrorContext())
    }

    @Test
    fun eventCreationFailureHelpersUseStableSafeCopy() {
        assertEquals("Impossible de creer l'evenement. Reessayez.", eventCreationFailureMessage())
        assertEquals("event_creation_failed", eventCreationFailureAnalyticsContext())
    }

    @Test
    fun validationErrorsUseLocalizedActionableCopy() = runTest {
        assertValidationError(eventCreationTitleRequiredMessage()) {
            createEvent()
        }

        assertValidationError(eventCreationDescriptionRequiredMessage()) {
            updateTitle("Road trip")
            createEvent()
        }

        assertValidationError(eventCreationTimeSlotRequiredMessage()) {
            updateTitle("Road trip")
            updateDescription("Weekend with friends")
            createEvent()
        }

        assertValidationError(eventCreationScenarioDestinationRequiredMessage()) {
            updateTitle("Road trip")
            updateDescription("Weekend with friends")
            addRequiredSlot()
            updatePlanningMode(EventPlanningMode.SCENARIO_MATRIX)
            createEvent()
        }

        assertValidationError(eventCreationCustomTypeRequiredMessage()) {
            updateTitle("Road trip")
            updateDescription("Weekend with friends")
            addRequiredSlot()
            addPotentialLocation(
                PotentialLocation(
                    id = "loc-1",
                    eventId = "event-1",
                    name = "Biarritz",
                    locationType = LocationType.CITY,
                    createdAt = "2026-06-20T00:00:00Z"
                )
            )
            updateEventType(EventType.CUSTOM)
            createEvent()
        }

        assertValidationError(eventCreationParticipantRangeMessage()) {
            updateTitle("Road trip")
            updateDescription("Weekend with friends")
            addRequiredSlot()
            updateEventType(EventType.BIRTHDAY)
            updateMinParticipants(10)
            updateMaxParticipants(4)
            createEvent()
        }
    }

    @Test
    fun validationHelpersDoNotUseEnglishFormDefaults() {
        listOf(
            eventCreationTitleRequiredMessage(),
            eventCreationDescriptionRequiredMessage(),
            eventCreationTimeSlotRequiredMessage(),
            eventCreationScenarioDestinationRequiredMessage(),
            eventCreationCustomTypeRequiredMessage(),
            eventCreationParticipantRangeMessage()
        ).forEach { message ->
            listOf(
                "Title is required",
                "Description is required",
                "At least one",
                "Custom event type",
                "Maximum participants"
            ).forEach { englishCopy ->
                assertFalse(
                    message.contains(englishCopy, ignoreCase = true),
                    "Message should not contain `$englishCopy`: $message"
                )
            }
        }
    }

    private fun fillRequiredFields() {
        viewModel.updateTitle("Road trip")
        viewModel.updateDescription("Weekend with friends")
        viewModel.addRequiredSlot()
    }

    private fun CreateEventViewModel.addRequiredSlot() {
        addTimeSlot(
            TimeSlot(
                id = "slot-1",
                start = "2026-07-14T09:00:00Z",
                end = "2026-07-14T18:00:00Z",
                timezone = "Europe/Paris"
            )
        )
    }

    private fun assertValidationError(
        expectedMessage: String,
        action: CreateEventViewModel.() -> Unit
    ) {
        viewModel = CreateEventViewModel(repository, analyticsProvider)
        viewModel.action()

        assertEquals(expectedMessage, viewModel.creationError.value)
        assertDoesNotExposeSensitiveDetails(viewModel.creationError.value.orEmpty())
    }

    private fun assertLastErrorContextEquals(expected: String) {
        assertEquals(expected, lastErrorContext())
    }

    private fun lastErrorContext(): String {
        return analyticsProvider.events
            .filterIsInstance<AnalyticsEvent.ErrorOccurred>()
            .last()
            .errorContext
            .orEmpty()
    }

    private fun assertDoesNotExposeSensitiveDetails(message: String) {
        listOf(
            "secret@example.com",
            "SECRET",
            "SQL constraint",
            "Backend 500",
            "token="
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }

    private class FakeCreateEventRepository : EventRepositoryInterface {
        var createEventFailure: Throwable? = null
        var throwOnCreate: Throwable? = null

        override suspend fun createEvent(event: Event): Result<Event> {
            throwOnCreate?.let { throw it }
            createEventFailure?.let { return Result.failure(it) }
            return Result.success(event)
        }

        override fun getEvent(id: String): Event? = null
        override fun getPoll(eventId: String): Poll? = null
        override suspend fun addParticipant(eventId: String, participantId: String): Result<Boolean> = Result.success(true)
        override fun getParticipants(eventId: String): List<String>? = emptyList()
        override fun getParticipantRecords(eventId: String): List<ParticipantRepositoryRecord>? = emptyList()
        override suspend fun addVote(
            eventId: String,
            participantId: String,
            slotId: String,
            vote: Vote
        ): Result<Boolean> = Result.success(true)

        override suspend fun updateEvent(event: Event): Result<Event> = Result.success(event)
        override suspend fun updateEventStatus(id: String, status: EventStatus, finalDate: String?): Result<Boolean> = Result.success(true)
        override suspend fun confirmEventDate(
            eventId: String,
            slotId: String,
            confirmedByOrganizerId: String
        ): Result<Boolean> = Result.success(true)

        override suspend fun queueWorkflowOutbox(record: WorkflowOutboxRecord): Result<Boolean> = Result.success(true)
        override fun getWorkflowOutbox(eventId: String): List<WorkflowOutboxRecord> = emptyList()
        override suspend fun saveEvent(event: Event): Result<Event> = Result.success(event)
        override suspend fun deleteEvent(eventId: String): Result<Unit> = Result.success(Unit)
        override fun isDeadlinePassed(deadline: String): Boolean = false
        override fun isOrganizer(eventId: String, userId: String): Boolean = false
        override fun canModifyEvent(eventId: String, userId: String): Boolean = false
        override fun getAllEvents(): List<Event> = emptyList()
        override fun getEventsPaginated(page: Int, pageSize: Int, orderBy: OrderBy): Flow<List<Event>> = flowOf(emptyList())
    }

    private class RecordingAnalyticsProvider : AnalyticsProvider {
        val events = mutableListOf<AnalyticsEvent>()

        override fun trackEvent(event: AnalyticsEvent, properties: Map<String, Any?>) {
            events += event
        }

        override fun setUserProperty(name: String, value: String) = Unit
        override fun setUserId(userId: String?) = Unit
        override fun setEnabled(enabled: Boolean) = Unit
        override fun clearUserData() = Unit
    }
}
