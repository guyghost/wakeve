package com.guyghost.wakeve.invitationexperience

import com.guyghost.wakeve.models.Event
import com.guyghost.wakeve.models.EventStatus
import kotlinx.datetime.Instant

/** Repository-derived temporal position of an event. */
enum class TemporalClass {
    UNDATED_DRAFT,
    UPCOMING,
    PAST
}

/** Global interaction override evaluated before surface-specific capabilities. */
enum class InteractionPolicy {
    INTERACTIVE,
    READ_ONLY
}

/**
 * Classifies an event from structured time bounds and a caller-supplied trusted clock.
 *
 * A confirmed slot uses its end when available so an in-progress event is not archived
 * at its start time. Missing or malformed bounds outside DRAFT fail closed to PAST.
 */
object EventTemporalClassifier {
    fun classify(event: Event, now: Instant): TemporalClass {
        val bound = structuredEndBound(event)
            ?: return if (event.status == EventStatus.DRAFT) {
                TemporalClass.UNDATED_DRAFT
            } else {
                TemporalClass.PAST
            }

        return if (bound < now) TemporalClass.PAST else TemporalClass.UPCOMING
    }

    fun structuredEndBound(event: Event): Instant? {
        val finalStart = event.finalDate.toInstantOrNull()
        if (finalStart != null) {
            val confirmedSlot = event.proposedSlots.firstOrNull { slot ->
                slot.start.toInstantOrNull() == finalStart
            }
            return confirmedSlot?.end.toInstantOrNull() ?: finalStart
        }

        return event.proposedSlots
            .mapNotNull { slot -> slot.end.toInstantOrNull() ?: slot.start.toInstantOrNull() }
            .maxOrNull()
    }

    private fun String?.toInstantOrNull(): Instant? =
        this?.let { value -> runCatching { Instant.parse(value) }.getOrNull() }
}

object InvitationExperienceInteractionPolicy {
    fun derive(
        temporalClass: TemporalClass,
        eventStatus: EventStatus,
        archiveActive: Boolean = false
    ): InteractionPolicy =
        if (
            archiveActive ||
            temporalClass == TemporalClass.PAST ||
            eventStatus == EventStatus.FINALIZED
        ) {
            InteractionPolicy.READ_ONLY
        } else {
            InteractionPolicy.INTERACTIVE
        }
}
