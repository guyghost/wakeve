package com.guyghost.wakeve.sample

import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.EventType
import com.guyghost.wakeve.models.TimeOfDay
import com.guyghost.wakeve.models.Vote
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for SampleEventFactory — pure core, no I/O.
 *
 * Validates that the factory produces consistent, well-formed sample data
 * suitable for the first-launch onboarding experience.
 */
class SampleEventFactoryTest {

    // MARK: - Factory Output Tests

    @Test
    fun `createSampleEvent returns valid POLLING event`() {
        val event = SampleEventFactory.createSampleEvent()

        assertEquals("sample-event-birthday-01", event.id)
        assertEquals("Sophie's Birthday 🎂", event.title)
        assertEquals(EventStatus.POLLING, event.status)
        assertEquals(EventType.BIRTHDAY, event.eventType)
        assertEquals("sample-user-you", event.organizerId)
        assertNull(event.finalDate)
        assertNotNull(event.description)
        assertTrue(event.description.contains("sample event"))
    }

    @Test
    fun `createSampleEvent has required participant and slot counts`() {
        val event = SampleEventFactory.createSampleEvent()

        // At least 5 participants (including organizer)
        assertTrue(event.participants.size >= 5,
            "Expected >= 5 participants, got ${event.participants.size}")

        // At least 3 time slots
        assertTrue(event.proposedSlots.size >= 3,
            "Expected >= 3 time slots, got ${event.proposedSlots.size}")

        // Enhanced draft fields
        assertNotNull(event.minParticipants)
        assertNotNull(event.maxParticipants)
        assertNotNull(event.expectedParticipants)
        assertTrue(event.maxParticipants!! >= event.minParticipants!!)
    }

    @Test
    fun `createTimeSlots uses distinct TimeOfDay values`() {
        val slots = SampleEventFactory.createTimeSlots()
        val timeOfDayValues = slots.map { it.timeOfDay }.distinct()

        assertTrue(timeOfDayValues.size >= 3,
            "Expected >= 3 distinct TimeOfDay values, got ${timeOfDayValues.size}")

        // Verify specific values
        assertTrue(slots.any { it.timeOfDay == TimeOfDay.EVENING })
        assertTrue(slots.any { it.timeOfDay == TimeOfDay.ALL_DAY })
        assertTrue(slots.any { it.timeOfDay == TimeOfDay.AFTERNOON })
    }

    @Test
    fun `createSampleVotes excludes organizer from voting`() {
        val event = SampleEventFactory.createSampleEvent()
        val votes = SampleEventFactory.createSampleVotes()

        // Organizer should NOT have pre-cast votes
        assertFalse(votes.containsKey("sample-user-you"),
            "Organizer should not have pre-cast votes — user should vote themselves")
    }

    @Test
    fun `createSampleVotes produces deterministic winner for slot-2`() {
        val votes = SampleEventFactory.createSampleVotes()
        val slots = SampleEventFactory.createTimeSlots()

        // Calculate scores per slot (YES=2, MAYBE=1, NO=-1)
        val scores = slots.associateWith { slot ->
            votes.values.sumOf { voterVotes ->
                when (voterVotes[slot.id]) {
                    Vote.YES -> 2
                    Vote.MAYBE -> 1
                    Vote.NO -> -1
                    null -> 0
                }
            }
        }

        // Slot 2 (ALL_DAY) should be the clear winner
        val winningSlot = scores.maxByOrNull { it.value }
        assertNotNull(winningSlot)
        assertEquals("sample-slot-2", winningSlot.key.id,
            "Slot 2 should be the deterministic winner, got ${winningSlot.key.id}")

        // Winning slot should have strictly higher score than all others
        scores.forEach { (slot, score) ->
            if (slot.id != "sample-slot-2") {
                assertTrue(winningSlot.value > score,
                    "Winning slot score (${winningSlot.value}) should be > $slot.id score ($score)")
            }
        }
    }

    @Test
    fun `createSampleVotes covers all time slots for each voter`() {
        val votes = SampleEventFactory.createSampleVotes()
        val slots = SampleEventFactory.createTimeSlots()
        val slotIds = slots.map { it.id }.toSet()

        votes.forEach { (voterId, voterVotes) ->
            assertEquals(slotIds, voterVotes.keys,
                "Voter $voterId should have voted on all slots")
        }
    }

    // MARK: - Validation Tests

    @Test
    fun `validateSampleData returns null for valid data`() {
        val error = SampleEventFactory.validateSampleData()
        assertNull(error, "Expected valid sample data, got error: $error")
    }

    // MARK: - ID Detection Tests

    @Test
    fun `isSampleEventId correctly identifies sample IDs`() {
        assertTrue(SampleEventFactory.isSampleEventId("sample-event-birthday-01"))
        assertTrue(SampleEventFactory.isSampleEventId("sample-anything"))
        assertFalse(SampleEventFactory.isSampleEventId("real-event-123"))
        assertFalse(SampleEventFactory.isSampleEventId("event-with-sample-in-name"))
    }

    @Test
    fun `participant IDs are consistent between factory methods`() {
        val event = SampleEventFactory.createSampleEvent()
        val participantIds = SampleEventFactory.createParticipantIds()

        assertEquals(event.participants, participantIds,
            "Event participants should match createParticipantIds()")
    }

    @Test
    fun `vote participants are a subset of event participants`() {
        val event = SampleEventFactory.createSampleEvent()
        val votes = SampleEventFactory.createSampleVotes()

        votes.keys.forEach { voterId ->
            assertTrue(event.participants.contains(voterId),
                "Voter $voterId should be in participant list")
        }
    }
}
