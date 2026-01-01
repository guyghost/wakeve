package com.guyghost.wakeve.presentation.usecase

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeSlot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for Draft Phase Use Cases:
 * - [ValidateEventDraftUseCase] - Validates event draft fields
 * - [SuggestEventTypeUseCase] - Returns preset event types
 * - [EstimateParticipantsUseCase] - Calculates participant estimates
 *
 * ## Validation Strategy
 *
 * **ValidateEventDraftUseCase:**
 * - Title and description are required
 * - maxParticipants >= minParticipants
 * - Participant counts must be positive
 * - EventType.CUSTOM requires eventTypeCustom
 *
 * **SuggestEventTypeUseCase:**
 * - Returns all preset types (excluding CUSTOM)
 * - Maintains consistent order
 *
 * **EstimateParticipantsUseCase:**
 * - Uses expectedParticipants if provided
 * - Calculates midpoint from min/max if expected is null
 * - All values must be positive
 *
 * ## Test Coverage
 *
 * Total: 12 test scenarios
 * - ValidateEventDraftUseCase: Tests 1-7 (7 tests)
 * - SuggestEventTypeUseCase: Tests 8-9 (2 tests)
 * - EstimateParticipantsUseCase: Tests 10-12 (3 tests)
 */
class DraftEventUseCasesTest {

    // ========================================================================
    // Test Helpers
    // ========================================================================

    /**
     * Creates a minimal valid event for testing.
     *
     * @param id Event ID (default: "evt-1")
     * @param title Event title (default: "Test Event")
     * @param description Event description (default: "Test description")
     * @param eventType Event type (default: EventType.OTHER)
     * @param eventTypeCustom Custom event type text (default: null)
     * @param minParticipants Minimum participants (default: null)
     * @param maxParticipants Maximum participants (default: null)
     * @param expectedParticipants Expected participants (default: null)
     * @return A valid Event instance
     */
    private fun createEvent(
        id: String = "evt-1",
        title: String = "Test Event",
        description: String = "Test description",
        eventType: EventType = EventType.OTHER,
        eventTypeCustom: String? = null,
        minParticipants: Int? = null,
        maxParticipants: Int? = null,
        expectedParticipants: Int? = null
    ): Event = Event(
        id = id,
        title = title,
        description = description,
        organizerId = "org-1",
        participants = emptyList(),
        proposedSlots = listOf(
            TimeSlot(
                id = "slot-1",
                start = "2025-12-20T10:00:00Z",
                end = "2025-12-20T12:00:00Z",
                timezone = "UTC"
            )
        ),
        deadline = "2025-12-15T18:00:00Z",
        status = EventStatus.DRAFT,
        createdAt = "2025-12-01T10:00:00Z",
        updatedAt = "2025-12-01T10:00:00Z",
        eventType = eventType,
        eventTypeCustom = eventTypeCustom,
        minParticipants = minParticipants,
        maxParticipants = maxParticipants,
        expectedParticipants = expectedParticipants
    )

    // ========================================================================
    // ValidateEventDraftUseCase Tests
    // ========================================================================

    /**
     * Test 1: Valid event with complete fields
     *
     * **GIVEN** Event with title, description, eventType=BIRTHDAY, expectedParticipants=10
     * **THEN** ValidateEventDraftUseCase returns Success
     */
    @Test
    fun testValidateEventDraft_CompleteEvent_Success() {
        val useCase = ValidateEventDraftUseCase()
        val event = createEvent(
            title = "Birthday Party",
            description = "Alice's 30th birthday celebration",
            eventType = EventType.BIRTHDAY,
            expectedParticipants = 10
        )

        val result = useCase(event)

        assertTrue(
            result is ValidationResult.Success,
            "Complete event should be valid"
        )
    }

    /**
     * Test 2: Invalid event - Empty title
     *
     * **GIVEN** Event with title=""
     * **THEN** Returns Error("Title is required")
     */
    @Test
    fun testValidateEventDraft_EmptyTitle_Error() {
        val useCase = ValidateEventDraftUseCase()
        val event = createEvent(title = "")

        val result = useCase(event)

        assertTrue(
            result is ValidationResult.Error,
            "Empty title should be invalid"
        )
        assertEquals(
            "Title is required",
            (result as ValidationResult.Error).message
        )
    }

    /**
     * Test 3: Invalid event - Empty description
     *
     * **GIVEN** Event with description=""
     * **THEN** Returns Error("Description is required")
     */
    @Test
    fun testValidateEventDraft_EmptyDescription_Error() {
        val useCase = ValidateEventDraftUseCase()
        val event = createEvent(description = "")

        val result = useCase(event)

        assertTrue(
            result is ValidationResult.Error,
            "Empty description should be invalid"
        )
        assertEquals(
            "Description is required",
            (result as ValidationResult.Error).message
        )
    }

    /**
     * Test 4: Invalid event - maxParticipants < minParticipants
     *
     * **GIVEN** Event with minParticipants=30, maxParticipants=20
     * **THEN** Returns Error("Max participants must be >= min participants")
     */
    @Test
    fun testValidateEventDraft_MaxLessThanMin_Error() {
        val useCase = ValidateEventDraftUseCase()
        val event = createEvent(
            minParticipants = 30,
            maxParticipants = 20
        )

        val result = useCase(event)

        assertTrue(
            result is ValidationResult.Error,
            "maxParticipants < minParticipants should be invalid"
        )
        assertEquals(
            "Max participants must be >= min participants",
            (result as ValidationResult.Error).message
        )
    }

    /**
     * Test 5: Invalid event - Negative participant counts
     *
     * **GIVEN** Event with minParticipants=-5
     * **THEN** Returns Error("Participants counts must be positive")
     */
    @Test
    fun testValidateEventDraft_NegativeParticipants_Error() {
        val useCase = ValidateEventDraftUseCase()
        val event = createEvent(minParticipants = -5)

        val result = useCase(event)

        assertTrue(
            result is ValidationResult.Error,
            "Negative participant count should be invalid"
        )
        assertEquals(
            "Participants counts must be positive",
            (result as ValidationResult.Error).message
        )
    }

    /**
     * Test 6: Invalid event - CUSTOM eventType without eventTypeCustom
     *
     * **GIVEN** Event with eventType=CUSTOM, eventTypeCustom=null
     * **THEN** Returns Error("Custom event type requires a description")
     */
    @Test
    fun testValidateEventDraft_CustomTypeWithoutDescription_Error() {
        val useCase = ValidateEventDraftUseCase()
        val event = createEvent(
            eventType = EventType.CUSTOM,
            eventTypeCustom = null
        )

        val result = useCase(event)

        assertTrue(
            result is ValidationResult.Error,
            "CUSTOM event type without description should be invalid"
        )
        assertEquals(
            "Custom event type requires a description",
            (result as ValidationResult.Error).message
        )
    }

    /**
     * Test 7: Valid event - Minimal with only required fields
     *
     * **GIVEN** Event with title="Test", description="Test desc", all others = defaults
     * **THEN** Returns Success
     */
    @Test
    fun testValidateEventDraft_MinimalEvent_Success() {
        val useCase = ValidateEventDraftUseCase()
        val event = createEvent(
            title = "Test",
            description = "Test desc",
            eventType = EventType.OTHER,
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = null
        )

        val result = useCase(event)

        assertTrue(
            result is ValidationResult.Success,
            "Minimal event with required fields should be valid"
        )
    }

    // ========================================================================
    // SuggestEventTypeUseCase Tests
    // ========================================================================

    /**
     * Test 8: Returns all preset event types
     *
     * **WHEN** SuggestEventTypeUseCase invoked
     * **THEN** Returns list of 10 preset types (all except CUSTOM)
     */
    @Test
    fun testSuggestEventType_ReturnsAllPresetTypes() {
        val useCase = SuggestEventTypeUseCase()
        val types = useCase()

        assertEquals(
            10,
            types.size,
            "Should return exactly 10 preset types (excluding CUSTOM)"
        )

        // Verify all preset types are present (excluding CUSTOM)
        assertTrue(types.contains(EventType.BIRTHDAY))
        assertTrue(types.contains(EventType.WEDDING))
        assertTrue(types.contains(EventType.TEAM_BUILDING))
        assertTrue(types.contains(EventType.CONFERENCE))
        assertTrue(types.contains(EventType.WORKSHOP))
        assertTrue(types.contains(EventType.PARTY))
        assertTrue(types.contains(EventType.SPORTS_EVENT))
        assertTrue(types.contains(EventType.CULTURAL_EVENT))
        assertTrue(types.contains(EventType.FAMILY_GATHERING))
        assertTrue(types.contains(EventType.OTHER))

        // CUSTOM should NOT be included
        assertFalse(
            types.contains(EventType.CUSTOM),
            "CUSTOM type should not be in preset suggestions"
        )
    }

    /**
     * Test 9: Event types are in expected order
     *
     * **WHEN** SuggestEventTypeUseCase invoked
     * **THEN** List is in predefined order: BIRTHDAY, WEDDING, TEAM_BUILDING, etc.
     */
    @Test
    fun testSuggestEventType_OrderIsConsistent() {
        val useCase = SuggestEventTypeUseCase()
        val types = useCase()

        // Verify order matches specification
        val expectedOrder = listOf(
            EventType.BIRTHDAY,
            EventType.WEDDING,
            EventType.TEAM_BUILDING,
            EventType.CONFERENCE,
            EventType.WORKSHOP,
            EventType.PARTY,
            EventType.SPORTS_EVENT,
            EventType.CULTURAL_EVENT,
            EventType.FAMILY_GATHERING,
            EventType.OTHER
        )

        assertEquals(
            expectedOrder,
            types,
            "Event types should be in consistent predefined order"
        )
    }

    // ========================================================================
    // EstimateParticipantsUseCase Tests
    // ========================================================================

    /**
     * Test 10: Calculate range with min and max
     *
     * **GIVEN** Event with minParticipants=10, maxParticipants=30
     * **WHEN** EstimateParticipantsUseCase invoked
     * **THEN** Returns Estimation(min=10, max=30, expected=20) [midpoint]
     */
    @Test
    fun testEstimateParticipants_WithMinMax_CalculatesMidpoint() {
        val useCase = EstimateParticipantsUseCase()
        val event = createEvent(
            minParticipants = 10,
            maxParticipants = 30,
            expectedParticipants = null
        )

        val result = useCase(event)

        assertNotNull(result)
        assertEquals(10, result.min)
        assertEquals(30, result.max)
        assertEquals(20, result.expected, "Expected should be midpoint of min and max")
    }

    /**
     * Test 11: Use expected if provided
     *
     * **GIVEN** Event with expectedParticipants=15 (without min/max)
     * **WHEN** EstimateParticipantsUseCase invoked
     * **THEN** Returns Estimation(min=15, max=15, expected=15)
     */
    @Test
    fun testEstimateParticipants_WithExpectedOnly_UsesExpected() {
        val useCase = EstimateParticipantsUseCase()
        val event = createEvent(
            minParticipants = null,
            maxParticipants = null,
            expectedParticipants = 15
        )

        val result = useCase(event)

        assertNotNull(result)
        assertEquals(15, result.min)
        assertEquals(15, result.max)
        assertEquals(15, result.expected)
    }

    /**
     * Test 12: Calculate expected from min/max midpoint when expected is null
     *
     * **GIVEN** Event with minParticipants=10, maxParticipants=30 (expected=null)
     * **WHEN** EstimateParticipantsUseCase invoked
     * **THEN** Returns Estimation(min=10, max=30, expected=20) [midpoint]
     */
    @Test
    fun testEstimateParticipants_OddRange_RoundsDown() {
        val useCase = EstimateParticipantsUseCase()
        val event = createEvent(
            minParticipants = 5,
            maxParticipants = 10,
            expectedParticipants = null
        )

        val result = useCase(event)

        assertNotNull(result)
        assertEquals(5, result.min)
        assertEquals(10, result.max)
        // Midpoint of 5-10 is 7.5, should round down to 7
        assertEquals(7, result.expected, "Odd midpoint should round down")
    }
}

// ========================================================================
// Sealed Classes and Data Classes for Results
// ========================================================================

/**
 * Result type for event draft validation.
 *
 * Represents the outcome of validating an event's draft fields.
 */
sealed class ValidationResult {
    /**
     * Event validation succeeded - all fields are valid.
     */
    object Success : ValidationResult()

    /**
     * Event validation failed.
     *
     * @property message Error message describing what validation failed
     */
    data class Error(val message: String) : ValidationResult()
}

/**
 * Result type for participant estimation.
 *
 * Represents calculated participant count estimates based on min/max/expected values.
 *
 * @property min Minimum participants (from minParticipants or calculated)
 * @property max Maximum participants (from maxParticipants or calculated)
 * @property expected Expected participants (from expectedParticipants or midpoint of min/max)
 */
data class ParticipantsEstimation(
    val min: Int,
    val max: Int,
    val expected: Int
)

// ========================================================================
// Use Case Implementations (To be created)
// ========================================================================

/**
 * Use case for validating event draft fields.
 *
 * Performs multi-field validation on an event:
 * - Title and description are required (non-empty)
 * - maxParticipants >= minParticipants
 * - Participant counts must be positive
 * - EventType.CUSTOM requires eventTypeCustom
 *
 * ## Usage
 *
 * ```kotlin
 * val validateEventDraft = ValidateEventDraftUseCase()
 * val result = validateEventDraft(event)
 * when (result) {
 *     is ValidationResult.Success -> println("Event is valid")
 *     is ValidationResult.Error -> println("Error: ${result.message}")
 * }
 * ```
 */
class ValidateEventDraftUseCase {
    /**
     * Validate event draft fields.
     *
     * @param event The event to validate
     * @return ValidationResult.Success if valid, ValidationResult.Error with message if invalid
     */
    operator fun invoke(event: Event): ValidationResult {
        // Title is required
        if (event.title.isBlank()) {
            return ValidationResult.Error("Title is required")
        }

        // Description is required
        if (event.description.isBlank()) {
            return ValidationResult.Error("Description is required")
        }

        // CUSTOM event type requires eventTypeCustom
        if (event.eventType == EventType.CUSTOM && event.eventTypeCustom.isNullOrBlank()) {
            return ValidationResult.Error("Custom event type requires a description")
        }

        // Participant counts validation
        if (event.minParticipants != null && event.minParticipants < 0) {
            return ValidationResult.Error("Participants counts must be positive")
        }

        if (event.maxParticipants != null && event.maxParticipants < 0) {
            return ValidationResult.Error("Participants counts must be positive")
        }

        if (event.expectedParticipants != null && event.expectedParticipants < 0) {
            return ValidationResult.Error("Participants counts must be positive")
        }

        // maxParticipants must be >= minParticipants
        if (event.minParticipants != null && event.maxParticipants != null) {
            if (event.maxParticipants < event.minParticipants) {
                return ValidationResult.Error("Max participants must be >= min participants")
            }
        }

        return ValidationResult.Success
    }
}

/**
 * Use case for suggesting preset event types.
 *
 * Returns a list of predefined event types that can be used for categorization.
 * The CUSTOM type is excluded as it requires user input via eventTypeCustom.
 *
 * ## Usage
 *
 * ```kotlin
 * val suggestEventType = SuggestEventTypeUseCase()
 * val types = suggestEventType()
 * // types = [BIRTHDAY, WEDDING, TEAM_BUILDING, ...]
 * ```
 */
class SuggestEventTypeUseCase {
    /**
     * Get list of preset event types.
     *
     * @return List of preset event types in consistent order (excluding CUSTOM)
     */
    operator fun invoke(): List<EventType> {
        return listOf(
            EventType.BIRTHDAY,
            EventType.WEDDING,
            EventType.TEAM_BUILDING,
            EventType.CONFERENCE,
            EventType.WORKSHOP,
            EventType.PARTY,
            EventType.SPORTS_EVENT,
            EventType.CULTURAL_EVENT,
            EventType.FAMILY_GATHERING,
            EventType.OTHER
        )
    }
}

/**
 * Use case for estimating participant counts.
 *
 * Calculates participant estimates based on provided min/max/expected values.
 * Priority:
 * 1. Use expectedParticipants if provided and non-null
 * 2. Calculate from min/max (midpoint) if expected is null
 * 3. If only expected is provided, use it for all estimates
 *
 * ## Usage
 *
 * ```kotlin
 * val estimateParticipants = EstimateParticipantsUseCase()
 * val estimation = estimateParticipants(event)
 * println("Expected: ${estimation.expected} (${estimation.min}-${estimation.max})")
 * ```
 */
class EstimateParticipantsUseCase {
    /**
     * Estimate participant counts for an event.
     *
     * @param event The event to estimate participants for
     * @return ParticipantsEstimation with min, max, and expected values
     */
    operator fun invoke(event: Event): ParticipantsEstimation {
        // Case 1: expectedParticipants is explicitly provided
        if (event.expectedParticipants != null) {
            return ParticipantsEstimation(
                min = event.expectedParticipants,
                max = event.expectedParticipants,
                expected = event.expectedParticipants
            )
        }

        // Case 2: minParticipants and maxParticipants are provided
        if (event.minParticipants != null && event.maxParticipants != null) {
            val midpoint = (event.minParticipants + event.maxParticipants) / 2
            return ParticipantsEstimation(
                min = event.minParticipants,
                max = event.maxParticipants,
                expected = midpoint
            )
        }

        // Case 3: Only minParticipants
        if (event.minParticipants != null) {
            return ParticipantsEstimation(
                min = event.minParticipants,
                max = event.minParticipants,
                expected = event.minParticipants
            )
        }

        // Case 4: Only maxParticipants
        if (event.maxParticipants != null) {
            return ParticipantsEstimation(
                min = event.maxParticipants,
                max = event.maxParticipants,
                expected = event.maxParticipants
            )
        }

        // Case 5: No participant data provided (default)
        return ParticipantsEstimation(
            min = 1,
            max = 1,
            expected = 1
        )
    }
}
