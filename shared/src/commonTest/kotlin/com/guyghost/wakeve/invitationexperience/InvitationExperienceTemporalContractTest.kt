package com.guyghost.wakeve.invitationexperience

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.TimeSlot
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class InvitationExperienceTemporalContractTest {
    private val now = Instant.parse("2030-06-15T12:00:00Z")

    @Test
    fun `temporal classifier uses structured end bounds and fails closed`() {
        val cases = listOf(
            Case(
                name = "undated draft",
                event = event(status = EventStatus.DRAFT),
                expected = TemporalClass.UNDATED_DRAFT
            ),
            Case(
                name = "future non-draft",
                event = event(
                    status = EventStatus.POLLING,
                    slots = listOf(slot("2030-06-20T10:00:00Z", "2030-06-20T12:00:00Z"))
                ),
                expected = TemporalClass.UPCOMING
            ),
            Case(
                name = "past non-draft",
                event = event(
                    status = EventStatus.ORGANIZING,
                    slots = listOf(slot("2030-06-10T10:00:00Z", "2030-06-10T12:00:00Z"))
                ),
                expected = TemporalClass.PAST
            ),
            Case(
                name = "in progress confirmed slot uses its end",
                event = event(
                    status = EventStatus.CONFIRMED,
                    finalDate = "2030-06-15T10:00:00Z",
                    slots = listOf(slot("2030-06-15T10:00:00Z", "2030-06-15T14:00:00Z"))
                ),
                expected = TemporalClass.UPCOMING
            ),
            Case(
                name = "missing structured date outside draft",
                event = event(status = EventStatus.CONFIRMED),
                expected = TemporalClass.PAST
            ),
            Case(
                name = "malformed structured date outside draft",
                event = event(
                    status = EventStatus.POLLING,
                    slots = listOf(slot("not-an-instant", "still-not-an-instant"))
                ),
                expected = TemporalClass.PAST
            )
        )

        cases.forEach { case ->
            assertEquals(
                case.expected,
                EventTemporalClassifier.classify(case.event, now),
                case.name
            )
        }
    }

    @Test
    fun `past finalized and archive context are always read only`() {
        val cases = listOf(
            Triple(TemporalClass.PAST, EventStatus.POLLING, false) to InteractionPolicy.READ_ONLY,
            Triple(TemporalClass.UPCOMING, EventStatus.FINALIZED, false) to InteractionPolicy.READ_ONLY,
            Triple(TemporalClass.UPCOMING, EventStatus.CONFIRMED, true) to InteractionPolicy.READ_ONLY,
            Triple(TemporalClass.UPCOMING, EventStatus.CONFIRMED, false) to InteractionPolicy.INTERACTIVE,
            Triple(TemporalClass.UNDATED_DRAFT, EventStatus.DRAFT, false) to InteractionPolicy.INTERACTIVE
        )

        cases.forEach { (input, expected) ->
            assertEquals(
                expected,
                InvitationExperienceInteractionPolicy.derive(
                    temporalClass = input.first,
                    eventStatus = input.second,
                    archiveActive = input.third
                ),
                input.toString()
            )
        }
    }

    private fun event(
        status: EventStatus,
        finalDate: String? = null,
        slots: List<TimeSlot> = emptyList()
    ) = Event(
        id = "event-${status.name.lowercase()}",
        title = "Event",
        description = "Description",
        organizerId = "organizer",
        proposedSlots = slots,
        deadline = "2030-06-01T00:00:00Z",
        status = status,
        finalDate = finalDate,
        createdAt = "2030-05-01T00:00:00Z",
        updatedAt = "2030-05-01T00:00:00Z"
    )

    private fun slot(start: String?, end: String?) = TimeSlot(
        id = "slot-${start.hashCode()}",
        start = start,
        end = end,
        timezone = "UTC"
    )

    private data class Case(
        val name: String,
        val event: Event,
        val expected: TemporalClass
    )
}
