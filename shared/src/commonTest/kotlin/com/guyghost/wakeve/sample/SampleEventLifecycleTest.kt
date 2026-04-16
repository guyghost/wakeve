package com.guyghost.wakeve.sample

import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.Vote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Edge case tests for the sample event lifecycle.
 *
 * Tests cover:
 * - Sample event data consistency
 * - Boundary conditions
 * - Non-regression of existing behavior
 */
class SampleEventLifecycleTest {

    // MARK: - Vote Scoring Edge Cases

    @Test
    fun `sample event scoring produces no ties`() {
        val votes = SampleEventFactory.createSampleVotes()
        val slots = SampleEventFactory.createTimeSlots()

        // Calculate scores per slot
        val scores = slots.map { slot ->
            votes.values.sumOf { voterVotes ->
                when (voterVotes[slot.id]) {
                    Vote.YES -> 2
                    Vote.MAYBE -> 1
                    Vote.NO -> -1
                    null -> 0
                }
            }
        }

        // Verify no ties exist
        val distinctScores = scores.distinct()
        assertEquals(scores.size, distinctScores.size,
            "All slot scores should be distinct to avoid ties: $scores")
    }

    @Test
    fun `sample event winning slot is clearly ahead`() {
        val votes = SampleEventFactory.createSampleVotes()
        val slots = SampleEventFactory.createTimeSlots()

        val scores = slots.map { slot ->
            votes.values.sumOf { voterVotes ->
                when (voterVotes[slot.id]) {
                    Vote.YES -> 2
                    Vote.MAYBE -> 1
                    Vote.NO -> -1
                    null -> 0
                }
            }
        }

        val maxScore = scores.maxOrNull()!!
        val secondHighest = scores.sortedDescending()[1]

        // Winner should be at least 2 points ahead
        assertTrue(maxScore - secondHighest >= 2,
            "Winning slot should be clearly ahead: max=$maxScore, second=$secondHighest")
    }

    // MARK: - Data Integrity Edge Cases

    @Test
    fun `sample event has no null required fields`() {
        val event = SampleEventFactory.createSampleEvent()

        assertNotNull(event.id, "id must not be null")
        assertNotNull(event.title, "title must not be null")
        assertNotNull(event.description, "description must not be null")
        assertNotNull(event.organizerId, "organizerId must not be null")
        assertNotNull(event.deadline, "deadline must not be null")
        assertNotNull(event.createdAt, "createdAt must not be null")
        assertNotNull(event.updatedAt, "updatedAt must not be null")
    }

    @Test
    fun `sample event time slots have valid timeOfDay values`() {
        val slots = SampleEventFactory.createTimeSlots()

        slots.forEach { slot ->
            assertNotNull(slot.id, "Slot ID must not be null")
            assertNotNull(slot.timezone, "Slot timezone must not be null")
            assertTrue(
                slot.timeOfDay in listOf(
                    TimeOfDay.ALL_DAY,
                    TimeOfDay.MORNING,
                    TimeOfDay.AFTERNOON,
                    TimeOfDay.EVENING,
                    TimeOfDay.SPECIFIC
                ),
                "Slot timeOfDay must be a valid enum value, got ${slot.timeOfDay}"
            )
        }
    }

    @Test
    fun `sample event model validation passes`() {
        val event = SampleEventFactory.createSampleEvent()
        val validationError = event.validate()
        assertEquals(null, validationError,
            "Sample event should pass model validation: $validationError")
    }

    @Test
    fun `sample event slots pass validation`() {
        val slots = SampleEventFactory.createTimeSlots()
        slots.forEach { slot ->
            val error = slot.validate()
            assertEquals(null, error,
                "Slot ${slot.id} should pass validation: $error")
        }
    }

    // MARK: - ID Detection Edge Cases

    @Test
    fun `isSampleEventId handles empty and blank strings`() {
        assertFalse(SampleEventFactory.isSampleEventId(""))
        assertFalse(SampleEventFactory.isSampleEventId("   "))
        assertFalse(SampleEventFactory.isSampleEventId("real-event"))
    }

    @Test
    fun `sample event ID is globally unique and stable`() {
        val event1 = SampleEventFactory.createSampleEvent()
        val event2 = SampleEventFactory.createSampleEvent()

        assertEquals(event1.id, event2.id,
            "Sample event ID should be stable across factory calls")
        assertEquals(SampleEventFactory.SAMPLE_EVENT_ID, event1.id,
            "Sample event ID should match the constant")
    }

    // MARK: - Non-Regression: Verify expected participant structure

    @Test
    fun `organizer is first participant`() {
        val event = SampleEventFactory.createSampleEvent()
        val participantIds = SampleEventFactory.createParticipantIds()

        assertEquals(event.organizerId, participantIds.first(),
            "Organizer should be the first participant")
    }

    @Test
    fun `sample event is BIRTHDAY type for consistency`() {
        val event = SampleEventFactory.createSampleEvent()
        assertEquals(EventType.BIRTHDAY, event.eventType,
            "Sample event must be BIRTHDAY for consistent testing")
    }

    @Test
    fun `sample event is in POLLING status for immediate interaction`() {
        val event = SampleEventFactory.createSampleEvent()
        assertEquals(EventStatus.POLLING, event.status,
            "Sample event must be in POLLING status for immediate voting interaction")
    }
}
