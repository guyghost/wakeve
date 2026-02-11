package com.guyghost.wakeve.viewmodel

import androidx.lifecycle.viewModelScope
import com.guyghost.wakeve.EventRepository
import com.guyghost.wakeve.analytics.AnalyticsEvent
import com.guyghost.wakeve.analytics.AnalyticsProvider
import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.PotentialLocation
import com.guyghost.wakeve.models.TimeSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the event creation screen with analytics tracking.
 *
 * This ViewModel manages the state of event creation, validates input,
 * communicates with the repository, and tracks user interactions for
 * analytics purposes.
 *
 * ## Usage in Compose
 *
 * ```kotlin
 * @Composable
 * fun CreateEventScreen(
 *     viewModel: CreateEventViewModel = koinViewModel()
 * ) {
 *     val title by viewModel.title.collectAsState()
 *     val description by viewModel.description.collectAsState()
 *     val eventType by viewModel.eventType.collectAsState()
 *     val isCreating by viewModel.isCreating.collectAsState()
 *     val creationError by viewModel.creationError.collectAsState()
 *
 *     CreateEventContent(
 *         title = title,
 *         onTitleChange = { viewModel.updateTitle(it) },
 *         description = description,
 *         onDescriptionChange = { viewModel.updateDescription(it) },
 *         eventType = eventType,
 *         onEventTypeChange = { viewModel.updateEventType(it) },
 *         timeSlots = viewModel.timeSlots.toList(),
 *         onAddTimeSlot = { viewModel.addTimeSlot(it) },
 *         onCreateEvent = { viewModel.createEvent() },
 *         isCreating = isCreating,
 *         error = creationError
 *     )
 * }
 * ```
 *
 * ## Analytics Tracked
 *
 * - Screen views on initialization
 * - Event creation attempts
 * - Event creation successes with metadata (event type, location, slots count)
 * - Event creation failures with error details
 * - Field validation errors
 *
 * @property eventRepository Repository for creating and storing events
 * @property analyticsProvider Analytics provider for tracking user actions
 */
class CreateEventViewModel(
    private val eventRepository: EventRepository,
    analyticsProvider: AnalyticsProvider
) : AnalyticsViewModel(analyticsProvider) {

    // Form fields
    private val _title = MutableStateFlow("")
    val title: StateFlow<String> = _title.asStateFlow()

    private val _description = MutableStateFlow("")
    val description: StateFlow<String> = _description.asStateFlow()

    private val _eventType = MutableStateFlow(EventType.OTHER)
    val eventType: StateFlow<EventType> = _eventType.asStateFlow()

    private val _eventTypeCustom = MutableStateFlow<String?>(null)
    val eventTypeCustom: StateFlow<String?> = _eventTypeCustom.asStateFlow()

    // Optional fields
    private val _minParticipants = MutableStateFlow<Int?>(null)
    val minParticipants: StateFlow<Int?> = _minParticipants.asStateFlow()

    private val _maxParticipants = MutableStateFlow<Int?>(null)
    val maxParticipants: StateFlow<Int?> = _maxParticipants.asStateFlow()

    private val _expectedParticipants = MutableStateFlow<Int?>(null)
    val expectedParticipants: StateFlow<Int?> = _expectedParticipants.asStateFlow()

    private val _potentialLocations = MutableStateFlow<List<PotentialLocation>>(emptyList())
    val potentialLocations: StateFlow<List<PotentialLocation>> = _potentialLocations.asStateFlow()

    // Required fields
    private val _timeSlots = MutableStateFlow<List<TimeSlot>>(emptyList())
    val timeSlots: StateFlow<List<TimeSlot>> = _timeSlots.asStateFlow()

    // UI state
    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _creationError = MutableStateFlow<String?>(null)
    val creationError: StateFlow<String?> = _creationError.asStateFlow()

    init {
        trackScreenView("create_event", "CreateEventViewModel")
    }

    // ========== Form Field Updates ==========

    fun updateTitle(newTitle: String) {
        _title.value = newTitle
        _creationError.value = null
    }

    fun updateDescription(newDescription: String) {
        _description.value = newDescription
        _creationError.value = null
    }

    fun updateEventType(newType: EventType) {
        _eventType.value = newType
        _creationError.value = null
    }

    fun updateEventTypeCustom(newCustom: String?) {
        _eventTypeCustom.value = newCustom?.takeIf { it.isNotBlank() }
        _creationError.value = null
    }

    fun updateMinParticipants(count: Int?) {
        _minParticipants.value = count?.takeIf { it > 0 }
        _creationError.value = null
    }

    fun updateMaxParticipants(count: Int?) {
        _maxParticipants.value = count?.takeIf { it > 0 }
        _creationError.value = null
    }

    fun updateExpectedParticipants(count: Int?) {
        _expectedParticipants.value = count?.takeIf { it > 0 }
        _creationError.value = null
    }

    fun addPotentialLocation(location: PotentialLocation) {
        _potentialLocations.value = _potentialLocations.value + location
    }

    fun removePotentialLocation(locationId: String) {
        _potentialLocations.value = _potentialLocations.value.filter { it.id != locationId }
    }

    fun addTimeSlot(slot: TimeSlot) {
        _timeSlots.value = _timeSlots.value + slot
    }

    fun removeTimeSlot(slotId: String) {
        _timeSlots.value = _timeSlots.value.filter { it.id != slotId }
    }

    // ========== Validation ==========

    /**
     * Validate the current form state.
     *
     * @return Null if valid, error message if invalid
     */
    private fun validateForm(): String? {
        // Required fields
        if (_title.value.isBlank()) {
            return "Title is required"
        }
        if (_description.value.isBlank()) {
            return "Description is required"
        }
        if (_timeSlots.value.isEmpty()) {
            return "At least one time slot is required"
        }

        // Conditional validation for custom event type
        if (_eventType.value == EventType.CUSTOM && (_eventTypeCustom.value.isNullOrBlank())) {
            return "Custom event type requires a description"
        }

        // Participant count validation
        val min = _minParticipants.value
        val max = _maxParticipants.value
        if (min != null && max != null && max < min) {
            return "Maximum participants must be greater than or equal to minimum"
        }

        return null
    }

    // ========== Event Creation ==========

    /**
     * Create a new event with the current form data.
     *
     * This method:
     * 1. Validates the form data
     * 2. Creates an Event object
     * 3. Saves it to the repository
     * 4. Tracks the creation with analytics metadata
     *
     * Analytics tracked:
     * - Event type (e.g., BIRTHDAY, WEDDING, etc.)
     * - Whether the event has locations
     * - Number of time slots
     * - Creation errors for debugging
     */
    fun createEvent() {
        val validationError = validateForm()
        if (validationError != null) {
            _creationError.value = validationError
            trackError("validation_failed", validationError, isFatal = false)
            return
        }

        viewModelScope.launch {
            _isCreating.value = true
            _creationError.value = null

            try {
                val event = Event(
                    id = generateEventId(),
                    title = _title.value.trim(),
                    description = _description.value.trim(),
                    organizerId = "current_user_id", // Would come from auth state
                    participants = emptyList(), // Organizer added later
                    proposedSlots = _timeSlots.value,
                    deadline = generateDefaultDeadline(), // Would come from form
                    status = EventStatus.DRAFT,
                    createdAt = getCurrentIsoTimestamp(),
                    updatedAt = getCurrentIsoTimestamp(),
                    eventType = _eventType.value,
                    eventTypeCustom = _eventTypeCustom.value,
                    minParticipants = _minParticipants.value,
                    maxParticipants = _maxParticipants.value,
                    expectedParticipants = _expectedParticipants.value,
                    heroImageUrl = null
                )

                val result = eventRepository.createEvent(event)

                if (result.isSuccess) {
                    // Track successful creation with metadata
                    trackEvent(
                        AnalyticsEvent.EventCreated(
                            eventType = _eventType.value.name,
                            hasLocation = _potentialLocations.value.isNotEmpty(),
                            timeSlotsCount = _timeSlots.value.size
                        )
                    )

                    // Reset form
                    resetForm()
                } else {
                    val error = result.exceptionOrNull()?.message ?: "Unknown error"
                    _creationError.value = error
                    trackError("create_event_failed", error, isFatal = false)
                }
            } catch (e: Exception) {
                val error = e.message ?: "Unknown error"
                _creationError.value = error
                trackError("create_event_exception", error, isFatal = false)
            } finally {
                _isCreating.value = false
            }
        }
    }

    /**
     * Clear the creation error.
     *
     * Call this when the user dismisses an error message.
     */
    fun clearError() {
        _creationError.value = null
    }

    // ========== Helper Methods ==========

    private fun resetForm() {
        _title.value = ""
        _description.value = ""
        _eventType.value = EventType.OTHER
        _eventTypeCustom.value = null
        _minParticipants.value = null
        _maxParticipants.value = null
        _expectedParticipants.value = null
        _potentialLocations.value = emptyList()
        _timeSlots.value = emptyList()
    }

    private fun generateEventId(): String {
        return "event_${System.currentTimeMillis()}"
    }

    private fun generateDefaultDeadline(): String {
        // Default to 7 days from now
        val futureTime = System.currentTimeMillis() + (7 * 24 * 60 * 60 * 1000)
        return java.time.Instant.ofEpochMilli(futureTime).toString()
    }

    private fun getCurrentIsoTimestamp(): String {
        return java.time.Instant.now().toString()
    }
}
