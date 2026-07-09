package com.guyghost.wakeve.presentation.statemachine

import com.guyghost.wakeve.access.ParticipantAccessMapper
import com.guyghost.wakeve.confirmation.ConfirmationClock
import com.guyghost.wakeve.confirmation.SystemConfirmationClock
import com.guyghost.wakeve.models.Coordinates
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.LocationType
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.presentation.state.EventManagementContract
import com.guyghost.wakeve.presentation.usecase.CreateEventUseCase
import com.guyghost.wakeve.presentation.usecase.LoadEventsUseCase
import com.guyghost.wakeve.sample.SampleEventFactory
import com.guyghost.wakeve.workflow.WorkflowOutboxRecord
import com.guyghost.wakeve.workflow.WorkflowOutboxType
import kotlinx.coroutines.CoroutineScope
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Functional interface for seeding sample event data.
 * Implemented by DatabaseEventRepository to insert sample data into SQLDelight.
 */
fun interface SampleEventSeeder {
    suspend fun seedSampleEvent(): Result<com.guyghost.wakeve.models.Event>
}

/**
 * State Machine for event management workflows.
 *
 * This state machine handles all event-related intents:
 * - Loading events list
 * - Creating, updating, deleting events
 * - Selecting events and loading their details
 * - Managing participants and poll results
 * - Seeding sample events for first-launch onboarding
 *
 * ## Architecture
 *
 * ```
 * Intent (user action)
 *   ↓
 * handleIntent()
 *   ↓
 * updateState() ← Updates the UI state
 * emitSideEffect() ← Triggers navigation/toasts
 * ```
 *
 * ## Usage Example (Android)
 *
 * ```kotlin
 * @Composable
 * fun EventListScreen(
 *     viewModel: EventManagementViewModel = koinViewModel()
 * ) {
 *     val state by viewModel.state.collectAsState()
 *
 *     LaunchedEffect(Unit) {
 *         viewModel.dispatch(EventManagementContract.Intent.LoadEvents)
 *     }
 *
 *     LaunchedEffect(Unit) {
 *         viewModel.sideEffect.collect { effect ->
 *             when (effect) {
 *                 is EventManagementContract.SideEffect.NavigateTo -> navigate(effect.route)
 *                 is EventManagementContract.SideEffect.ShowToast -> showToast(effect.message)
 *                 is EventManagementContract.SideEffect.NavigateBack -> navController.popBackStack()
 *             }
 *         }
 *     }
 *
 *     EventListContent(
 *         state = state) { intent ->
 *             viewModel.dispatch(intent)
 *         }
 *     }
 * ```
 *
 * ## Usage Example (iOS)
 *
 * ```swift
 * class EventListViewModel: ObservableObject {
 *     @Published var state: EventManagementContract.State
 *     private let stateMachine: ObservableStateMachine<...>
 *
 *     init() {
 *         stateMachine = IosFactory().createEventStateMachine()
 *         state = stateMachine.currentState
 *         stateMachine.onStateChange = { [weak self] in self?.state = $0 }
 *     }
 *
 *     func dispatch(_ intent: EventManagementContract.Intent) {
 *         stateMachine.dispatch(intent: intent)
 *     }
 * }
 * ```
 *
 * @property loadEventsUseCase Use case for loading events
 * @property createEventUseCase Use case for creating events
 * @property eventRepository Direct access to repository for additional operations (nullable)
 * @property sampleEventSeeder Seeder for sample/onboarding event data (nullable)
 * @property scope CoroutineScope for launching async work
 */
class EventManagementStateMachine(
    private val loadEventsUseCase: LoadEventsUseCase,
    private val createEventUseCase: CreateEventUseCase,
    private val eventRepository: com.guyghost.wakeve.repository.EventRepositoryInterface?,
    private val sampleEventSeeder: SampleEventSeeder? = null,
    private val confirmationClock: ConfirmationClock = SystemConfirmationClock,
    scope: CoroutineScope
) : StateMachine<EventManagementContract.State, EventManagementContract.Intent, EventManagementContract.SideEffect>(
    initialState = EventManagementContract.State(),
    scope = scope
) {

    override suspend fun handleIntent(intent: EventManagementContract.Intent) {
        when (intent) {
            is EventManagementContract.Intent.LoadEvents -> loadEvents()
            is EventManagementContract.Intent.SelectEvent -> selectEvent(intent.eventId)
            is EventManagementContract.Intent.CreateEvent -> createEvent(intent.event)
            is EventManagementContract.Intent.UpdateEvent -> updateEvent(intent.event)
            is EventManagementContract.Intent.DeleteEvent -> deleteEvent(intent.eventId, intent.userId)
            is EventManagementContract.Intent.LoadParticipants -> loadParticipants(intent.eventId)
            is EventManagementContract.Intent.AddParticipant -> addParticipant(intent.eventId, intent.participantId)
            is EventManagementContract.Intent.LoadPollResults -> loadPollResults(intent.eventId)
            is EventManagementContract.Intent.StartPoll -> startPoll(intent.eventId, intent.userId)
            is EventManagementContract.Intent.ConfirmDate -> confirmDate(intent.eventId, intent.slotId, intent.userId)
            is EventManagementContract.Intent.OpenConfirmPrompt -> openConfirmPrompt(
                intent.eventId,
                intent.slotId,
                intent.actorId
            )
            is EventManagementContract.Intent.CancelConfirmation -> cancelConfirmation()
            is EventManagementContract.Intent.SubmitConfirmation -> submitConfirmation(intent.operationId)
            is EventManagementContract.Intent.RetryConfirmation -> retryConfirmation()
            is EventManagementContract.Intent.DismissConfirmationFailure -> dismissConfirmationFailure()
            is EventManagementContract.Intent.SyncCompleted -> markConfirmationSynced(intent.receiptId)
            is EventManagementContract.Intent.SyncFailed -> retainPendingConfirmation(intent.receiptId)
            is EventManagementContract.Intent.RehydrateConfirmation -> rehydrateConfirmation(intent.projection)
            is EventManagementContract.Intent.TransitionToOrganizing -> transitionToOrganizing(intent.eventId, intent.userId)
            is EventManagementContract.Intent.MarkAsFinalized -> markAsFinalized(intent.eventId, intent.userId)
            is EventManagementContract.Intent.ClearError -> clearError()
            is EventManagementContract.Intent.UpdateDraftEvent -> updateDraftEvent(
                intent.eventId,
                intent.eventType,
                intent.eventTypeCustom,
                intent.expectedParticipants,
                intent.minParticipants,
                intent.maxParticipants
            )
            is EventManagementContract.Intent.AddPotentialLocation -> addPotentialLocation(
                intent.eventId,
                intent.locationId,
                intent.locationName,
                intent.locationType,
                intent.address,
                intent.coordinates
            )
            is EventManagementContract.Intent.RemovePotentialLocation -> removePotentialLocation(
                intent.eventId,
                intent.locationId
            )
            is EventManagementContract.Intent.SeedSampleEvent -> seedSampleEvent()
        }
    }

    // ========================================================================
    // Intent Handlers
    // ========================================================================

    /**
     * Load all events from repository and update state.
     *
     * Flow:
     * 1. Set isLoading = true
     * 2. Call loadEventsUseCase()
     * 3. On success: update events list, set isLoading = false
     * 4. On failure: set error message, emit ShowToast, set isLoading = false
     */
    private suspend fun loadEvents() {
        updateState { it.copy(isLoading = true, error = null) }

        val result = loadEventsUseCase()

        result.fold(
            onSuccess = { events ->
                updateState { it.copy(isLoading = false, events = events) }
            },
            onFailure = { _ ->
                val errorMessage = eventLoadFailureMessage()
                updateState { it.copy(isLoading = false, error = errorMessage) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMessage))
            }
        )
    }

    /**
     * Select an event and prepare for detail view.
     *
     * Flow:
     * 1. Find the event in current events list
     * 2. Update selectedEvent
     * 3. Load its participants and poll results
     * 4. Emit NavigateTo side effect
     *
     * @param eventId The ID of the event to select
     */
    private suspend fun selectEvent(eventId: String) {
        val event = currentState.events.find { it.id == eventId }

        if (event == null) {
            updateState { it.copy(error = "Event not found") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event not found"))
            return
        }

        updateState { it.copy(selectedEvent = event) }

        // Load additional data
        loadParticipants(eventId)
        loadPollResults(eventId)

        // Emit navigation side effect
        emitSideEffect(EventManagementContract.SideEffect.NavigateTo("event/$eventId"))
    }

    /**
     * Create a new event.
     *
     * Flow:
     * 1. Set isLoading = true
     * 2. Call createEventUseCase(event)
     * 3. On success: refresh events list, emit ShowToast, emit NavigateBack
     * 4. On failure: set error, emit ShowToast
     *
     * @param event The event to create
     */
    private suspend fun createEvent(event: com.guyghost.wakeve.models.Event) {
        updateState { it.copy(isLoading = true, error = null) }

        val result = createEventUseCase(event)

        result.fold(
            onSuccess = {
                // Refresh events list
                loadEvents()
                emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event created successfully"))
            },
            onFailure = { _ ->
                val errorMessage = eventCreateFailureMessage()
                updateState { it.copy(isLoading = false, error = errorMessage) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMessage))
            }
        )
    }

    /**
     * Update an existing event.
     *
     * Flow:
     * 1. Check if repository is available
     * 2. Set isLoading = true
     * 3. Call repository.updateEvent(event)
     * 4. On success: update in state, emit ShowToast
     * 5. On failure: set error, emit ShowToast
     *
     * @param event The updated event
     */
    private suspend fun updateEvent(event: com.guyghost.wakeve.models.Event) {
        if (eventRepository == null) {
            updateState { it.copy(error = "Repository not available") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Repository not available"))
            return
        }

        updateState { it.copy(isLoading = true, error = null) }

        val result = eventRepository.saveEvent(event)

        result.fold(
            onSuccess = {
                // Update in state
                val updatedEvents = currentState.events.filterNot { it.id == event.id } + event
                updateState { it.copy(isLoading = false, events = updatedEvents, selectedEvent = event) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event updated successfully"))
            },
            onFailure = { _ ->
                val errorMessage = eventUpdateFailureMessage()
                updateState { it.copy(isLoading = false, error = errorMessage) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMessage))
            }
        )
    }

    /**
     * Delete an event.
     *
     * Flow:
     * 1. Validate repository is available
     * 2. Load the event
     * 3. Validate user is organizer
     * 4. Validate event status is not FINALIZED
     * 5. Call repository.deleteEvent()
     * 6. Remove from state
     * 7. Emit ShowToast and NavigateBack
     *
     * @param eventId The ID of the event to delete
     * @param userId The ID of the user attempting to delete (must be organizer)
     */
    private suspend fun deleteEvent(eventId: String, userId: String) {
        if (eventRepository == null) {
            updateState { it.copy(error = "Repository not available") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Repository not available"))
            return
        }

        updateState { it.copy(isLoading = true, error = null) }

        val event = eventRepository.getEvent(eventId)

        // Guard: Event must exist
        if (event == null) {
            updateState { it.copy(isLoading = false, error = "Event not found") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event not found"))
            return
        }

        // Guard: Only organizer can delete
        if (!validateOrganizerPermission(event, userId)) {
            emitUnauthorizedError("Only event organizer can delete this event")
            return
        }

        // Guard: FINALIZED events cannot be deleted
        if (event.status == EventStatus.FINALIZED) {
            val errorMsg = "Cannot delete a finalized event"
            updateState { it.copy(isLoading = false, error = errorMsg) }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMsg))
            return
        }

        // Perform deletion
        val result = eventRepository.deleteEvent(eventId)

        result.fold(
            onSuccess = {
                // Remove from state
                val updatedEvents = currentState.events.filter { it.id != eventId }
                updateState { 
                    it.copy(
                        isLoading = false, 
                        events = updatedEvents,
                        selectedEvent = if (it.selectedEvent?.id == eventId) null else it.selectedEvent
                    ) 
                }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event deleted successfully"))
                emitSideEffect(EventManagementContract.SideEffect.NavigateBack)
            },
            onFailure = { _ ->
                val errorMessage = eventDeleteFailureMessage()
                updateState { it.copy(isLoading = false, error = errorMessage) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMessage))
            }
        )
    }

    /**
     * Load participants for an event.
     *
     * Flow:
     * 1. Check if repository is available
     * 2. Call repository.getParticipantRecords(eventId)
     * 3. Update state with participant IDs and access states
     *
     * @param eventId The ID of the event
     */
    private suspend fun loadParticipants(eventId: String) {
        if (eventRepository == null) return

        val participantRecords = eventRepository.getParticipantRecords(eventId) ?: return
        val participantIds = participantRecords.map { it.userId }
        val participantAccessStates = participantRecords.map(ParticipantAccessMapper::fromRepositoryRecord)

        updateState {
            it.copy(
                participantIds = participantIds,
                participantAccessStates = participantAccessStates
            )
        }
    }

    /**
     * Add a participant to an event.
     *
     * Flow:
     * 1. Check if repository is available
     * 2. Call repository.addParticipant(eventId, participantId)
     * 3. On success: reload participants, emit ShowToast
     * 4. On failure: emit ShowToast with error
     *
     * @param eventId The ID of the event
     * @param participantId The ID of the participant to add
     */
    private suspend fun addParticipant(eventId: String, participantId: String) {
        if (eventRepository == null) {
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Repository not available"))
            return
        }

        val result = eventRepository.addParticipant(eventId, participantId)

        result.fold(
            onSuccess = {
                loadParticipants(eventId)
                emitSideEffect(EventManagementContract.SideEffect.ShowToast("Participant added successfully"))
            },
            onFailure = { _ ->
                val errorMessage = eventAddParticipantFailureMessage()
                emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMessage))
            }
        )
    }

    /**
     * Load poll results for an event.
     *
     * Flow:
     * 1. Check if repository is available
     * 2. Call repository.getPoll(eventId)
     * 3. Update state with poll votes
     *
     * @param eventId The ID of the event
     */
    private suspend fun loadPollResults(eventId: String) {
        if (eventRepository == null) return

        val poll = eventRepository.getPoll(eventId) ?: return

        updateState { it.copy(pollVotes = poll.votes) }
    }

    /**
     * Clear error state.
     *
     * Used to dismiss error messages in the UI.
     */
    private suspend fun clearError() {
        updateState { it.copy(error = null) }
    }

    // ========================================================================
    // Draft Phase Update Handlers
    // ========================================================================

    /**
     * Update a draft event incrementally.
     *
     * Only works if event is in DRAFT status.
     * Allows partial updates (one or more fields).
     * Validates: maxParticipants >= minParticipants, participants >= 0, custom type requires description.
     *
     * @param eventId The ID of the event to update
     * @param eventType Optional: new event type
     * @param eventTypeCustom Optional: custom type description (required if eventType=CUSTOM)
     * @param expectedParticipants Optional: expected number of participants
     * @param minParticipants Optional: minimum participants
     * @param maxParticipants Optional: maximum participants
     */
    private suspend fun updateDraftEvent(
        eventId: String,
        eventType: EventType?,
        eventTypeCustom: String?,
        expectedParticipants: Int?,
        minParticipants: Int?,
        maxParticipants: Int?
    ) {
        if (eventRepository == null) {
            updateState { it.copy(isLoading = false, error = "Repository not available") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Repository not available"))
            return
        }

        updateState { it.copy(isLoading = true, error = null) }

        val event = eventRepository.getEvent(eventId)
        if (event == null) {
            updateState { it.copy(isLoading = false, error = "Event not found") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event not found"))
            return
        }

        // Validate status - only DRAFT events can be updated
        if (event.status != EventStatus.DRAFT) {
            val errorMsg = "Cannot update draft: Event is not in DRAFT status"
            updateState { it.copy(isLoading = false, error = errorMsg) }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMsg))
            return
        }

        // Validate participant counts
        if (minParticipants != null && minParticipants < 0) {
            val errorMsg = "Participants counts must be non-negative"
            updateState { it.copy(isLoading = false, error = errorMsg) }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMsg))
            return
        }

        if (maxParticipants != null && maxParticipants < 0) {
            val errorMsg = "Participants counts must be non-negative"
            updateState { it.copy(isLoading = false, error = errorMsg) }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMsg))
            return
        }

        if (minParticipants != null && maxParticipants != null && maxParticipants < minParticipants) {
            val errorMsg = "Max participants must be >= min participants"
            updateState { it.copy(isLoading = false, error = errorMsg) }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMsg))
            return
        }

        // Validate custom event type
        if (eventType == EventType.CUSTOM && eventTypeCustom.isNullOrBlank()) {
            val errorMsg = "Custom event type requires a description"
            updateState { it.copy(isLoading = false, error = errorMsg) }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMsg))
            return
        }

        // Build updated event with new fields
        val updatedEvent = event.copy(
            eventType = eventType ?: event.eventType,
            eventTypeCustom = eventTypeCustom ?: event.eventTypeCustom,
            expectedParticipants = expectedParticipants ?: event.expectedParticipants,
            minParticipants = minParticipants ?: event.minParticipants,
            maxParticipants = maxParticipants ?: event.maxParticipants
        )

        // Update via repository
        val result = eventRepository.updateEvent(updatedEvent)

        result.fold(
            onSuccess = {
                // Update in state
                val updatedEvents = currentState.events.map { if (it.id == updatedEvent.id) updatedEvent else it }
                updateState { it.copy(isLoading = false, events = updatedEvents, selectedEvent = updatedEvent) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event updated successfully"))
            },
            onFailure = { _ ->
                val errorMessage = eventUpdateFailureMessage()
                updateState { it.copy(isLoading = false, error = errorMessage) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMessage))
            }
        )
    }

    /**
     * Add a potential location to a draft event.
     *
     * Only works if event is in DRAFT status.
     *
     * @param eventId The ID of the event
     * @param locationId The ID of the location to add
     * @param name Name of the location (required)
     * @param locationType Type of location (CITY, REGION, SPECIFIC_VENUE, ONLINE)
     * @param address Optional: text address
     * @param coordinates Optional: geographic coordinates
     */
    private suspend fun addPotentialLocation(
        eventId: String,
        locationId: String,
        name: String,
        locationType: LocationType,
        address: String?,
        coordinates: Coordinates?
    ) {
        if (eventRepository == null) {
            updateState { it.copy(isLoading = false, error = "Repository not available") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Repository not available"))
            return
        }

        updateState { it.copy(isLoading = true, error = null) }

        val event = eventRepository.getEvent(eventId)
        if (event == null) {
            updateState { it.copy(isLoading = false, error = "Event not found") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event not found"))
            return
        }

        // Validate status - only DRAFT events can have locations added
        if (event.status != EventStatus.DRAFT) {
            val errorMsg = "Cannot add location: Event is not in DRAFT status"
            updateState { it.copy(isLoading = false, error = errorMsg) }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMsg))
            return
        }

        // Create PotentialLocation
        val now = Clock.System.now().toString()
        val location = PotentialLocation(
            id = locationId,
            eventId = eventId,
            name = name,
            locationType = locationType,
            address = address,
            coordinates = coordinates,
            createdAt = now
        )

        // Add to state
        val updatedLocations = currentState.potentialLocations + location
        updateState { it.copy(isLoading = false, potentialLocations = updatedLocations) }

        emitSideEffect(EventManagementContract.SideEffect.ShowToast("Location added successfully"))
    }

    /**
     * Remove a potential location from a draft event.
     *
     * Only works if event is in DRAFT status.
     *
     * @param eventId The ID of the event
     * @param locationId The ID of the location to remove
     */
    private suspend fun removePotentialLocation(
        eventId: String,
        locationId: String
    ) {
        if (eventRepository == null) {
            updateState { it.copy(isLoading = false, error = "Repository not available") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Repository not available"))
            return
        }

        updateState { it.copy(isLoading = true, error = null) }

        val event = eventRepository.getEvent(eventId)
        if (event == null) {
            updateState { it.copy(isLoading = false, error = "Event not found") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event not found"))
            return
        }

        // Validate status - only DRAFT events can have locations removed
        if (event.status != EventStatus.DRAFT) {
            val errorMsg = "Cannot remove location: Event is not in DRAFT status"
            updateState { it.copy(isLoading = false, error = errorMsg) }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMsg))
            return
        }

        // Remove from state
        val updatedLocations = currentState.potentialLocations.filter { it.id != locationId }
        updateState { it.copy(isLoading = false, potentialLocations = updatedLocations) }

        emitSideEffect(EventManagementContract.SideEffect.ShowToast("Location removed successfully"))
    }

    // ========================================================================
    // Permission Validation Helpers
    // ========================================================================

    /**
     * Validate that a user is the organizer of an event.
     *
     * This is a pure function that checks organizer permissions.
     * Used by privileged operations like confirming dates, transitioning phases,
     * and finalizing events.
     *
     * @param event The event to check (null if not found)
     * @param userId The ID of the user to validate
     * @return true if user is the organizer, false otherwise
     */
    private fun validateOrganizerPermission(event: com.guyghost.wakeve.models.Event?, userId: String): Boolean {
        if (event == null) return false
        return userId == event.organizerId
    }

    /**
     * Emit an error side effect for unauthorized access.
     *
     * @param message The error message to show
     */
    private suspend fun emitUnauthorizedError(message: String) {
        updateState { it.copy(isLoading = false, error = message) }
        emitSideEffect(EventManagementContract.SideEffect.ShowToast(message))
    }

    // ========================================================================
    // Workflow Transition Handlers
    // ========================================================================

    /**
     * Start polling on time slots.
     *
     * Transitions event from DRAFT to POLLING.
     * Only the organizer can start polling.
     *
     * Flow:
     * 1. Validate repository is available
     * 2. Load the event
     * 3. Validate event status is DRAFT
     * 4. Validate user is organizer
     * 5. Update event status to POLLING
     * 6. Update state
     * 7. Emit ShowToast
     *
     * @param eventId The ID of the event to start polling for
     * @param userId The ID of the user attempting to start poll (must be organizer)
     */
    private suspend fun startPoll(eventId: String, userId: String) {
        if (eventRepository == null) {
            updateState { it.copy(error = "Repository not available") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Repository not available"))
            return
        }

        updateState { it.copy(isLoading = true, error = null) }

        val event = eventRepository.getEvent(eventId)
        if (event == null) {
            updateState { it.copy(isLoading = false, error = "Event not found") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event not found"))
            return
        }

        // Guard: Only organizer can start poll
        if (!validateOrganizerPermission(event, userId)) {
            emitUnauthorizedError("Only event organizer can start poll")
            return
        }

        // Validate status
        if (event.status != com.guyghost.wakeve.models.EventStatus.DRAFT) {
            val errorMsg = "Cannot start poll: Event is not in DRAFT status"
            updateState { it.copy(isLoading = false, error = errorMsg) }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMsg))
            return
        }

        if (event.planningMode == com.guyghost.wakeve.models.EventPlanningMode.SCENARIO_MATRIX) {
            val errorMsg = "Cannot start poll: Matrix events must publish scenarios instead"
            updateState { it.copy(isLoading = false, error = errorMsg) }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMsg))
            return
        }

        // Update event status to POLLING
        val result = eventRepository.updateEventStatus(
            id = eventId,
            status = com.guyghost.wakeve.models.EventStatus.POLLING,
            finalDate = null
        )

        result.fold(
            onSuccess = {
                // Reload events to get updated status
                loadEvents()
                emitSideEffect(EventManagementContract.SideEffect.ShowToast("Poll started successfully"))
            },
            onFailure = { _ ->
                val errorMessage = eventStartPollFailureMessage()
                updateState { it.copy(isLoading = false, error = errorMessage) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMessage))
            }
        )
    }

    /**
     * Confirm the final date for an event.
     *
     * Transitions event from POLLING to CONFIRMED.
     * Only the organizer can confirm the date.
     * At least one participant must have voted.
     *
     * Flow:
     * 1. Validate repository is available
     * 2. Load the event
     * 3. Validate event status is POLLING
     * 4. Validate user is organizer
     * 5. Validate at least one vote exists
     * 6. Find the selected time slot
     * 7. Update event status to CONFIRMED with finalDate
     * 8. Update state with scenariosUnlocked = true
     * 9. Emit NavigateTo scenarios screen
     *
     * @param eventId The ID of the event
     * @param slotId The ID of the selected time slot
     * @param userId The ID of the user attempting to confirm (must be organizer)
     */
    private suspend fun confirmDate(eventId: String, slotId: String, userId: String) {
        openConfirmPrompt(eventId, slotId, userId)
        if (currentState.confirmationPhase == EventManagementContract.ConfirmationPhase.CONFIRM_PROMPT) {
            submitConfirmation("legacy-$eventId-$slotId")
        }
    }

    private fun openConfirmPrompt(eventId: String, slotId: String, actorId: String) {
        if (currentState.confirmationPhase != EventManagementContract.ConfirmationPhase.REVIEWING_RESULTS) return
        updateState {
            it.copy(
                confirmationPhase = EventManagementContract.ConfirmationPhase.CONFIRM_PROMPT,
                confirmationEventId = eventId,
                confirmationActorId = actorId,
                confirmationSlotId = slotId,
                confirmationOperationId = null,
                confirmationReceiptId = null,
                confirmationDecisionSyncStatus = null,
                confirmationEffectDispatchStatus = null,
                confirmationDiagnosticReason = null,
                confirmationFailure = null,
                error = null
            )
        }
    }

    private fun cancelConfirmation() {
        if (currentState.confirmationPhase != EventManagementContract.ConfirmationPhase.CONFIRM_PROMPT) return
        clearConfirmationAttempt()
    }

    private suspend fun submitConfirmation(operationId: String) {
        if (currentState.confirmationPhase != EventManagementContract.ConfirmationPhase.CONFIRM_PROMPT) return
        val eventId = currentState.confirmationEventId ?: return
        val slotId = currentState.confirmationSlotId ?: return
        val actorId = currentState.confirmationActorId ?: return
        val command = EventManagementContract.ConfirmPollDateCommand(
            operationId = operationId,
            eventId = eventId,
            slotId = slotId,
            actorId = actorId,
            requestedAt = confirmationClock.now()
        )
        updateState {
            it.copy(
                confirmationPhase = EventManagementContract.ConfirmationPhase.CONFIRMING,
                confirmationOperationId = operationId,
                confirmationDiagnosticReason = null,
                confirmationFailure = null,
                error = null
            )
        }
        executeConfirmation(command)
    }

    private suspend fun retryConfirmation() {
        val state = currentState
        if (state.confirmationPhase != EventManagementContract.ConfirmationPhase.FAILED ||
            state.confirmationFailure?.retryable != true
        ) return
        val operationId = state.confirmationOperationId ?: return
        val eventId = state.confirmationEventId ?: return
        val slotId = state.confirmationSlotId ?: return
        val actorId = state.confirmationActorId ?: return
        updateState {
            it.copy(
                confirmationPhase = EventManagementContract.ConfirmationPhase.CONFIRMING,
                confirmationDiagnosticReason = null,
                confirmationFailure = null,
                error = null
            )
        }
        executeConfirmation(
            EventManagementContract.ConfirmPollDateCommand(
                operationId = operationId,
                eventId = eventId,
                slotId = slotId,
                actorId = actorId,
                requestedAt = confirmationClock.now()
            )
        )
    }

    private fun dismissConfirmationFailure() {
        if (currentState.confirmationPhase == EventManagementContract.ConfirmationPhase.FAILED) {
            clearConfirmationAttempt()
        }
    }

    private fun clearConfirmationAttempt() {
        updateState {
            it.copy(
                confirmationPhase = EventManagementContract.ConfirmationPhase.REVIEWING_RESULTS,
                confirmationEventId = null,
                confirmationActorId = null,
                confirmationSlotId = null,
                confirmationOperationId = null,
                confirmationReceiptId = null,
                confirmationDecisionSyncStatus = null,
                confirmationEffectDispatchStatus = null,
                confirmationDiagnosticReason = null,
                confirmationFailure = null,
                error = null,
                isLoading = false
            )
        }
    }

    private suspend fun executeConfirmation(command: EventManagementContract.ConfirmPollDateCommand) {
        val eventId = command.eventId
        val slotId = command.slotId
        val userId = command.actorId
        if (eventRepository == null) {
            failConfirmation(EventManagementContract.ConfirmationFailureCode.REPOSITORY_UNAVAILABLE, true)
            return
        }

        updateState { it.copy(isLoading = true, error = null) }

        val event = eventRepository.getEvent(eventId)

        // Guard: Event must exist
        if (event == null) {
            failConfirmation(EventManagementContract.ConfirmationFailureCode.EVENT_NOT_FOUND, false)
            return
        }

        // Guard: Only organizer can confirm dates
        if (!validateOrganizerPermission(event, userId)) {
            failConfirmation(EventManagementContract.ConfirmationFailureCode.NOT_ORGANIZER, false)
            return
        }

        // Find the selected time slot
        val selectedSlot = event.proposedSlots.find { it.id == slotId }
        if (selectedSlot == null) {
            failConfirmation(EventManagementContract.ConfirmationFailureCode.SLOT_NOT_FOUND, false)
            return
        }

        val finalDate = selectedSlot.start
        if (finalDate == null) {
            failConfirmation(EventManagementContract.ConfirmationFailureCode.SLOT_NOT_CONFIRMABLE, false)
            return
        }

        val replayingSameConfirmedSlot = event.status == com.guyghost.wakeve.models.EventStatus.CONFIRMED &&
            event.finalDate == finalDate
        if (event.status != com.guyghost.wakeve.models.EventStatus.POLLING && !replayingSameConfirmedSlot) {
            failConfirmation(EventManagementContract.ConfirmationFailureCode.INVALID_EVENT_STATUS, false)
            return
        }

        // A receipt replay is already locally committed; only new POLLING confirmations require votes.
        if (!replayingSameConfirmedSlot) {
            val poll = eventRepository.getPoll(eventId)
            if (poll == null || poll.votes.isEmpty()) {
                failConfirmation(EventManagementContract.ConfirmationFailureCode.NO_VOTES, false)
                return
            }
        }

        when (val result = eventRepository.confirmPollDate(command)) {
            is EventManagementContract.ConfirmationResult.Committed -> applyCommittedConfirmation(result.receipt)
            is EventManagementContract.ConfirmationResult.AlreadyCommitted -> applyCommittedConfirmation(result.receipt)
            is EventManagementContract.ConfirmationResult.ReadOnly ->
                applyReadOnlyConfirmation(result.projection)
            is EventManagementContract.ConfirmationResult.Conflict ->
                failConfirmation(result.failure.code, result.failure.retryable)
            is EventManagementContract.ConfirmationResult.Failed ->
                failConfirmation(result.failure.code, result.failure.retryable)
        }
    }

    private suspend fun applyCommittedConfirmation(receipt: EventManagementContract.ConfirmationReceipt) {
        // The durable receipt is the sole eligibility source for success feedback and navigation.
        loadEvents()
        updateState {
            it.copy(
                scenariosUnlocked = true,
                confirmationPhase = when (receipt.decisionSyncStatus) {
                    EventManagementContract.DecisionSyncStatus.LOCAL_PENDING ->
                        EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC
                    EventManagementContract.DecisionSyncStatus.SERVER_ACKNOWLEDGED ->
                        EventManagementContract.ConfirmationPhase.CONFIRMED_SYNCED
                },
                confirmationReceiptId = receipt.receiptId,
                confirmationDecisionSyncStatus = receipt.decisionSyncStatus,
                confirmationEffectDispatchStatus = receipt.effectDispatchStatus,
                confirmationDiagnosticReason = null,
                confirmationFailure = null,
                isLoading = false
            )
        }
        emitSideEffect(EventManagementContract.SideEffect.ShowToast("Date confirmed successfully"))
        emitSideEffect(EventManagementContract.SideEffect.NavigateTo(receipt.nextNavigationTarget))
    }

    private fun applyReadOnlyConfirmation(
        projection: EventManagementContract.ConfirmationProjection.ReadOnly
    ) {
        // A recovered historical classification only changes render state. It has no completion effect.
        rehydrateConfirmation(projection)
    }

    private suspend fun markConfirmationSynced(receiptId: String) {
        val state = currentState
        if (state.confirmationPhase != EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC ||
            state.confirmationReceiptId != receiptId
        ) return
        val projection = eventRepository?.markConfirmationSynced(receiptId)
            as? EventManagementContract.ConfirmationProjection.Confirmed
            ?: return
        if (projection.receiptId != receiptId ||
            projection.decisionSyncStatus != EventManagementContract.DecisionSyncStatus.SERVER_ACKNOWLEDGED
        ) return
        updateState {
            it.copy(
                confirmationPhase = EventManagementContract.ConfirmationPhase.CONFIRMED_SYNCED,
                confirmationDecisionSyncStatus = projection.decisionSyncStatus,
                confirmationEffectDispatchStatus = projection.effectDispatchStatus
            )
        }
    }

    private fun retainPendingConfirmation(receiptId: String) {
        if (currentState.confirmationPhase != EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC ||
            currentState.confirmationReceiptId != receiptId
        ) return
        // A delivery/sync failure never rolls back the local date decision.
    }

    private fun rehydrateConfirmation(projection: EventManagementContract.ConfirmationProjection) {
        when (projection) {
            is EventManagementContract.ConfirmationProjection.Reviewing -> updateState {
                it.copy(
                    isLoading = false,
                    confirmationPhase = EventManagementContract.ConfirmationPhase.REVIEWING_RESULTS,
                    confirmationEventId = projection.eventId,
                    confirmationSlotId = null,
                    confirmationOperationId = null,
                    confirmationReceiptId = null,
                    confirmationDecisionSyncStatus = null,
                    confirmationEffectDispatchStatus = null,
                    confirmationDiagnosticReason = null,
                    confirmationFailure = null,
                    error = null
                )
            }
            is EventManagementContract.ConfirmationProjection.Confirmed -> updateState {
                it.copy(
                    isLoading = false,
                    scenariosUnlocked = true,
                    confirmationPhase = when (projection.decisionSyncStatus) {
                        EventManagementContract.DecisionSyncStatus.LOCAL_PENDING ->
                            EventManagementContract.ConfirmationPhase.CONFIRMED_PENDING_SYNC
                        EventManagementContract.DecisionSyncStatus.SERVER_ACKNOWLEDGED ->
                            EventManagementContract.ConfirmationPhase.CONFIRMED_SYNCED
                    },
                    confirmationEventId = projection.eventId,
                    confirmationSlotId = projection.slotId,
                    confirmationOperationId = null,
                    confirmationReceiptId = projection.receiptId,
                    confirmationDecisionSyncStatus = projection.decisionSyncStatus,
                    confirmationEffectDispatchStatus = projection.effectDispatchStatus,
                    confirmationDiagnosticReason = null,
                    confirmationFailure = null,
                    error = null
                )
            }
            is EventManagementContract.ConfirmationProjection.LegacyApplied -> updateState {
                it.copy(
                    isLoading = false,
                    scenariosUnlocked = true,
                    confirmationPhase = EventManagementContract.ConfirmationPhase.LEGACY_APPLIED,
                    confirmationEventId = projection.eventId,
                    confirmationSlotId = projection.slotId,
                    confirmationOperationId = null,
                    confirmationReceiptId = projection.receiptId,
                    confirmationDecisionSyncStatus = null,
                    confirmationEffectDispatchStatus = null,
                    confirmationDiagnosticReason = null,
                    confirmationFailure = null,
                    error = null
                )
            }
            is EventManagementContract.ConfirmationProjection.Quarantined -> updateState {
                it.copy(
                    isLoading = false,
                    scenariosUnlocked = false,
                    confirmationPhase = EventManagementContract.ConfirmationPhase.QUARANTINED,
                    confirmationEventId = projection.eventId,
                    confirmationSlotId = null,
                    confirmationOperationId = null,
                    confirmationReceiptId = null,
                    confirmationDecisionSyncStatus = null,
                    confirmationEffectDispatchStatus = null,
                    confirmationDiagnosticReason = projection.reason,
                    confirmationFailure = null,
                    error = null
                )
            }
        }
    }

    private suspend fun failConfirmation(
        code: EventManagementContract.ConfirmationFailureCode,
        retryable: Boolean
    ) {
        val message = when (code) {
            EventManagementContract.ConfirmationFailureCode.EVENT_NOT_FOUND -> "Event not found"
            EventManagementContract.ConfirmationFailureCode.NOT_ORGANIZER ->
                "Only event organizer can confirm dates"
            EventManagementContract.ConfirmationFailureCode.INVALID_EVENT_STATUS ->
                "Cannot confirm date: Event is not in POLLING status"
            EventManagementContract.ConfirmationFailureCode.NO_VOTES ->
                "Cannot confirm date: No votes have been submitted"
            EventManagementContract.ConfirmationFailureCode.SLOT_NOT_FOUND -> "Selected time slot not found"
            EventManagementContract.ConfirmationFailureCode.SLOT_NOT_CONFIRMABLE ->
                "Selected time slot has no confirmed start date"
            EventManagementContract.ConfirmationFailureCode.ALREADY_CONFIRMED_DIFFERENT_SLOT ->
                "Event is already confirmed with a different time slot"
            EventManagementContract.ConfirmationFailureCode.LOCAL_PERSISTENCE_FAILED ->
                eventDateConfirmationFailureMessage()
            EventManagementContract.ConfirmationFailureCode.REPOSITORY_UNAVAILABLE -> "Repository not available"
        }
        updateState {
            it.copy(
                isLoading = false,
                confirmationPhase = EventManagementContract.ConfirmationPhase.FAILED,
                confirmationDiagnosticReason = null,
                confirmationFailure = EventManagementContract.ConfirmationFailure(code, retryable),
                error = message
            )
        }
        emitSideEffect(EventManagementContract.SideEffect.ShowToast(message))
    }

    /**
     * Transition event to organizing phase.
     *
     * Transitions event from CONFIRMED to ORGANIZING.
     * Only the organizer can trigger this transition.
     * A scenario must have been selected.
     *
     * Flow:
     * 1. Validate repository is available
     * 2. Load the event
     * 3. Validate event status is CONFIRMED
     * 4. Validate user is organizer
     * 5. Update event status to ORGANIZING
     * 6. Update state with meetingsUnlocked = true
     * 7. Emit NavigateTo meetings screen
     *
     * @param eventId The ID of the event
     * @param userId The ID of the user attempting to transition (must be organizer)
     */
    private suspend fun transitionToOrganizing(eventId: String, userId: String) {
        if (eventRepository == null) {
            updateState { it.copy(error = "Repository not available") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Repository not available"))
            return
        }

        updateState { it.copy(isLoading = true, error = null) }

        val event = eventRepository.getEvent(eventId)

        // Guard: Event must exist
        if (event == null) {
            updateState { it.copy(isLoading = false, error = "Event not found") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event not found"))
            return
        }

        // Guard: Only organizer can transition
        if (!validateOrganizerPermission(event, userId)) {
            emitUnauthorizedError("Only event organizer can transition to organizing")
            return
        }

        // Validate status
        if (event.status != com.guyghost.wakeve.models.EventStatus.CONFIRMED) {
            val errorMsg = "Cannot transition to organizing: Event is not in CONFIRMED status"
            updateState { it.copy(isLoading = false, error = errorMsg) }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMsg))
            return
        }

        // Update event status to ORGANIZING
        val result = eventRepository.updateEventStatus(
            id = eventId,
            status = com.guyghost.wakeve.models.EventStatus.ORGANIZING,
            finalDate = event.finalDate
        )

        result.fold(
            onSuccess = {
                // Reload events to get updated status
                loadEvents()
                updateState { it.copy(meetingsUnlocked = true) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast("Transitioned to organizing phase"))
                emitSideEffect(EventManagementContract.SideEffect.NavigateTo("event/$eventId/meetings"))
            },
            onFailure = { _ ->
                val errorMessage = eventTransitionToOrganizingFailureMessage()
                updateState { it.copy(isLoading = false, error = errorMessage) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMessage))
            }
        )
    }

    /**
     * Mark event as finalized.
     *
     * Transitions event from ORGANIZING to FINALIZED.
     * Only the organizer can finalize.
     * All critical details must be confirmed.
     *
     * Flow:
     * 1. Validate repository is available
     * 2. Load the event
     * 3. Validate event status is ORGANIZING
     * 4. Validate user is organizer
     * 5. Update event status to FINALIZED
     * 6. Update state
     * 7. Emit ShowToast
     *
     * @param eventId The ID of the event
     * @param userId The ID of the user attempting to finalize (must be organizer)
     */
    private suspend fun markAsFinalized(eventId: String, userId: String) {
        if (eventRepository == null) {
            updateState { it.copy(error = "Repository not available") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Repository not available"))
            return
        }

        updateState { it.copy(isLoading = true, error = null) }

        val event = eventRepository.getEvent(eventId)

        // Guard: Event must exist
        if (event == null) {
            updateState { it.copy(isLoading = false, error = "Event not found") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event not found"))
            return
        }

        // Guard: Only organizer can finalize
        if (!validateOrganizerPermission(event, userId)) {
            emitUnauthorizedError("Only event organizer can finalize the event")
            return
        }

        // Validate status
        if (event.status != com.guyghost.wakeve.models.EventStatus.ORGANIZING) {
            val errorMsg = "Cannot finalize: Event is not in ORGANIZING status"
            updateState { it.copy(isLoading = false, error = errorMsg) }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMsg))
            return
        }

        // Update event status to FINALIZED
        val result = eventRepository.updateEventStatus(
            id = eventId,
            status = com.guyghost.wakeve.models.EventStatus.FINALIZED,
            finalDate = event.finalDate
        )

        result.fold(
            onSuccess = { _: Boolean ->
                // Reload events to get updated status
                loadEvents()
                emitSideEffect(EventManagementContract.SideEffect.ShowToast("Event finalized successfully!"))
            },
            onFailure = { error ->
                val errorMessage = eventFinalizeFailureMessage(error)
                updateState { it.copy(isLoading = false, error = errorMessage) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMessage))
            }
        )
    }

    // ========================================================================
    // Sample Event Handler
    // ========================================================================

    /**
     * Seed the sample event for first-launch onboarding.
     *
     * Creates a pre-populated birthday event in POLLING status
     * with participants, time slots, and pre-cast votes.
     * Idempotent — does nothing if sample already exists.
     *
     * Flow:
     * 1. Check if sampleEventSeeder is available
     * 2. Set isLoading = true
     * 3. Call seeder.seedSampleEvent()
     * 4. On success: reload events, navigate to event detail
     * 5. On failure: set error, emit ShowToast
     */
    private suspend fun seedSampleEvent() {
        if (sampleEventSeeder == null) {
            updateState { it.copy(error = "Sample event seeder not available") }
            emitSideEffect(EventManagementContract.SideEffect.ShowToast("Sample event not available"))
            return
        }

        updateState { it.copy(isLoading = true, error = null) }

        val result = sampleEventSeeder.seedSampleEvent()

        result.fold(
            onSuccess = { event ->
                // Reload events to include the new sample
                loadEvents()
                emitSideEffect(
                    EventManagementContract.SideEffect.ShowToast(
                        "✨ Sample event created! Explore the full flow."
                    )
                )
                // Navigate to the sample event detail
                emitSideEffect(
                    EventManagementContract.SideEffect.NavigateTo(
                        "event/${event.id}"
                    )
                )
            },
            onFailure = { _ ->
                val errorMessage = eventSeedSampleFailureMessage()
                updateState { it.copy(isLoading = false, error = errorMessage) }
                emitSideEffect(EventManagementContract.SideEffect.ShowToast(errorMessage))
            }
        )
    }
}

internal fun eventLoadFailureMessage(): String =
    "Failed to load events"

internal fun eventCreateFailureMessage(): String =
    "Failed to create event"

internal fun eventUpdateFailureMessage(): String =
    "Failed to update event"

internal fun eventDeleteFailureMessage(): String =
    "Failed to delete event"

internal fun eventAddParticipantFailureMessage(): String =
    "Failed to add participant"

internal fun eventStartPollFailureMessage(): String =
    "Failed to start poll"

internal fun eventDateConfirmationFailureMessage(): String =
    "Failed to confirm date"

internal fun eventConfirmationWorkflowQueueFailureMessage(): String =
    "Failed to queue confirmation workflow"

internal fun eventTransitionToOrganizingFailureMessage(): String =
    "Failed to transition to organizing"

internal fun eventFinalizeFailureMessage(error: Throwable? = null): String {
    val messageText = error?.safeMessage().orEmpty()
    val prefix = "Finalization blocked by "
    if (messageText.startsWith(prefix)) {
        val blockerText = messageText.removePrefix(prefix)
        val blockersAreStructured = blockerText.isNotBlank() &&
            blockerText.split(",").all { blocker ->
                blocker.isNotBlank() &&
                    blocker.all { it.isUpperCase() || it.isDigit() || it == '_' }
            }
        if (blockersAreStructured) {
            return "$prefix$blockerText"
        }
    }

    return "Failed to finalize event"
}

internal fun eventSeedSampleFailureMessage(): String =
    "Failed to seed sample event"

private fun Throwable.safeMessage(): String =
    message.orEmpty()
