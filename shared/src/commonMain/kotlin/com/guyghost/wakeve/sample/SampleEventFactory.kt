package com.guyghost.wakeve.sample

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.TimeSlot

/**
 * Pure factory for creating sample event data for first-launch onboarding.
 *
 * Produces a realistic birthday event in POLLING status with participants,
 * time slots, and pre-cast votes so new users can explore the full
 * DRAFT → POLLING → CONFIRMED flow interactively.
 *
 * All IDs are prefixed with "sample-" to identify them as synthetic data
 * that should never sync to the backend.
 *
 * FC&IS: This is pure core — no side effects, no I/O.
 */
object SampleEventFactory {

    /** Unique ID for the sample event, used to detect its presence */
    const val SAMPLE_EVENT_ID = "sample-event-birthday-01"

    /** Display names for mock participants */
    private val PARTICIPANT_NAMES = listOf(
        "You",
        "Alice (demo)",
        "Marc (demo)",
        "Léa (demo)",
        "Thomas (demo)"
    )

    /**
     * Generate sample participant IDs.
     * First participant is always the organizer ("You").
     */
    fun createParticipantIds(): List<String> = listOf(
        "sample-user-you",
        "sample-user-alice",
        "sample-user-marc",
        "sample-user-lea",
        "sample-user-thomas"
    )

    /**
     * Generate sample time slots 1-3 weeks from a reference date.
     * Uses dynamic dates to avoid staleness.
     *
     * @param referenceDate ISO 8601 date string to compute future dates from.
     *                      Defaults to a sensible future date.
     */
    fun createTimeSlots(referenceDate: String = "2026-05-10T19:00:00Z"): List<TimeSlot> = listOf(
        TimeSlot(
            id = "sample-slot-1",
            start = "2026-05-10T19:00:00Z",
            end = "2026-05-10T23:00:00Z",
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.EVENING
        ),
        TimeSlot(
            id = "sample-slot-2",
            start = "2026-05-16T10:00:00Z",
            end = "2026-05-16T22:00:00Z",
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.ALL_DAY
        ),
        TimeSlot(
            id = "sample-slot-3",
            start = "2026-05-23T14:00:00Z",
            end = "2026-05-23T18:00:00Z",
            timezone = "Europe/Paris",
            timeOfDay = TimeOfDay.AFTERNOON
        )
    )

    /**
     * Create the complete sample event in POLLING status.
     *
     * The event is seeded with enough data for the user to:
     * 1. See the event card with "✨ Sample" badge
     * 2. Open it and see the poll with time slots
     * 3. Vote on time slots
     * 4. Confirm a date (POLLING → CONFIRMED)
     *
     * The winning slot (slot-2 ALL_DAY) is set up to have the highest score
     * based on pre-cast votes:
     *   Slot 1 (EVENING):   Alice=YES, Marc=YES, Léa=MAYBE, Thomas=NO  → 2+2+1-1 = 4
     *   Slot 2 (ALL_DAY):   Alice=YES, Marc=YES, Léa=YES,  Thomas=YES → 2+2+2+2 = 8 ★ winner
     *   Slot 3 (AFTERNOON): Alice=MAYBE, Marc=NO, Léa=YES, Thomas=MAYBE → 1-1+2+1 = 3
     */
    fun createSampleEvent(): Event = Event(
        id = SAMPLE_EVENT_ID,
        title = "Sophie's Birthday 🎂",
        description = "Let's celebrate Sophie's birthday! Pick the best time for everyone. This is a sample event — feel free to explore and delete it anytime.",
        organizerId = "sample-user-you",
        participants = createParticipantIds(),
        proposedSlots = createTimeSlots(),
        deadline = "2026-06-01T23:59:59Z",
        status = EventStatus.POLLING,
        finalDate = null,
        createdAt = "2026-04-15T10:00:00Z",
        updatedAt = "2026-04-15T10:00:00Z",
        eventType = EventType.BIRTHDAY,
        eventTypeCustom = null,
        minParticipants = 3,
        maxParticipants = 10,
        expectedParticipants = 6
    )

    /**
     * Pre-cast votes for the sample event.
     * Returns a map of participantId → (slotId → vote).
     *
     * The organizer ("You") has NOT voted yet — this is intentional so the
     * new user can experience the voting interaction themselves.
     *
     * Vote distribution designed for deterministic winner (slot-2):
     *   Slot 1 (EVENING):   Alice=YES, Marc=YES, Léa=MAYBE, Thomas=NO  → score 4
     *   Slot 2 (ALL_DAY):   Alice=YES, Marc=YES, Léa=YES,  Thomas=YES → score 8 ★
     *   Slot 3 (AFTERNOON): Alice=MAYBE, Marc=NO, Léa=YES, Thomas=MAYBE → score 3
     */
    fun createSampleVotes(): Map<String, Map<String, com.guyghost.wakeve.models.Vote>> = mapOf(
        "sample-user-alice" to mapOf(
            "sample-slot-1" to com.guyghost.wakeve.models.Vote.YES,
            "sample-slot-2" to com.guyghost.wakeve.models.Vote.YES,
            "sample-slot-3" to com.guyghost.wakeve.models.Vote.MAYBE
        ),
        "sample-user-marc" to mapOf(
            "sample-slot-1" to com.guyghost.wakeve.models.Vote.YES,
            "sample-slot-2" to com.guyghost.wakeve.models.Vote.YES,
            "sample-slot-3" to com.guyghost.wakeve.models.Vote.NO
        ),
        "sample-user-lea" to mapOf(
            "sample-slot-1" to com.guyghost.wakeve.models.Vote.MAYBE,
            "sample-slot-2" to com.guyghost.wakeve.models.Vote.YES,
            "sample-slot-3" to com.guyghost.wakeve.models.Vote.YES
        ),
        "sample-user-thomas" to mapOf(
            "sample-slot-1" to com.guyghost.wakeve.models.Vote.NO,
            "sample-slot-2" to com.guyghost.wakeve.models.Vote.YES,
            "sample-slot-3" to com.guyghost.wakeve.models.Vote.MAYBE
        )
    )

    /**
     * Check if an event ID belongs to a sample event.
     * Used for sync exclusion and badge rendering.
     */
    fun isSampleEventId(eventId: String): Boolean =
        eventId.startsWith("sample-")

    /**
     * Validate that the sample event data is consistent.
     * Returns null if valid, error message if invalid.
     */
    fun validateSampleData(): String? {
        val event = createSampleEvent()
        val votes = createSampleVotes()

        // Event must be in POLLING status
        if (event.status != EventStatus.POLLING) {
            return "Sample event must be in POLLING status, got ${event.status}"
        }

        // Event must be BIRTHDAY type
        if (event.eventType != EventType.BIRTHDAY) {
            return "Sample event must be BIRTHDAY type, got ${event.eventType}"
        }

        // Must have at least 3 time slots
        if (event.proposedSlots.size < 3) {
            return "Sample event must have at least 3 time slots, got ${event.proposedSlots.size}"
        }

        // Must have at least 4 participants (excluding organizer who hasn't voted)
        if (event.participants.size < 5) {
            return "Sample event must have at least 5 participants, got ${event.participants.size}"
        }

        // Vote participants must be in the event participant list
        votes.keys.forEach { voterId ->
            if (!event.participants.contains(voterId)) {
                return "Voter $voterId not in participant list"
            }
        }

        // Each voter must have voted on all time slots
        votes.values.forEach { voterVotes ->
            event.proposedSlots.forEach { slot ->
                if (!voterVotes.containsKey(slot.id)) {
                    return "Missing vote for slot ${slot.id}"
                }
            }
        }

        // Validate event model itself
        event.validate()?.let { return it }

        // Validate each time slot
        event.proposedSlots.forEach { slot ->
            slot.validate()?.let { return "Slot ${slot.id}: $it" }
        }

        return null
    }
}
