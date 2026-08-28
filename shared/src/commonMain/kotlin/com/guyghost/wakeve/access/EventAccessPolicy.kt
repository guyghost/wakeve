package com.guyghost.wakeve.access

import com.guyghost.wakeve.models.Event
import kotlinx.serialization.Serializable

@Serializable
enum class ParticipantRsvp {
    PENDING,
    ACCEPTED,
    DECLINED,
    NOT_APPLICABLE,
    UNAVAILABLE
}

@Serializable
enum class DateValidationState {
    NOT_VALIDATED,
    VALIDATED_RETAINED_DATE,
    NOT_APPLICABLE,
    UNAVAILABLE
}

@Serializable
enum class InvitationPreviewVisibility {
    EVENT_METADATA,
    PUBLIC_METADATA_ONLY
}

@Serializable
enum class OrganizationSection {
    TRANSPORT,
    LODGING,
    BUDGET,
    PAYMENT,
    MEETING
}

@Serializable
enum class AccessDeniedReason {
    NOT_EVENT_MEMBER,
    ATTENDANCE_NOT_CONFIRMED,
    PARTICIPATION_DECLINED,
    RETAINED_DATE_NOT_VALIDATED
}

@Serializable
data class ParticipantAccessState(
    val userId: String,
    val role: Role,
    val rsvp: ParticipantRsvp,
    val dateValidation: DateValidationState
) {
    @Serializable
    enum class Role {
        ORGANIZER,
        MEMBER,
        NON_MEMBER
    }

    companion object {
        fun organizer(userId: String): ParticipantAccessState =
            ParticipantAccessState(
                userId = userId,
                role = Role.ORGANIZER,
                rsvp = ParticipantRsvp.ACCEPTED,
                dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
            )

        fun member(
            userId: String,
            rsvp: ParticipantRsvp,
            dateValidation: DateValidationState
        ): ParticipantAccessState =
            ParticipantAccessState(
                userId = userId,
                role = Role.MEMBER,
                rsvp = rsvp,
                dateValidation = dateValidation
            )

        fun invitedPending(userId: String): ParticipantAccessState =
            member(
                userId = userId,
                rsvp = ParticipantRsvp.PENDING,
                dateValidation = DateValidationState.NOT_VALIDATED
            )

        fun declined(userId: String): ParticipantAccessState =
            member(
                userId = userId,
                rsvp = ParticipantRsvp.DECLINED,
                dateValidation = DateValidationState.NOT_VALIDATED
            )

        fun nonMember(userId: String): ParticipantAccessState =
            ParticipantAccessState(
                userId = userId,
                role = Role.NON_MEMBER,
                rsvp = ParticipantRsvp.NOT_APPLICABLE,
                dateValidation = DateValidationState.NOT_APPLICABLE
            )
    }
}

data class ParticipantRepositoryRecord(
    val id: String,
    val eventId: String,
    val userId: String,
    val role: String,
    val rsvp: String,
    val hasValidatedDate: Long,
    /**
     * Total persisted retained-date axis. `null` is reserved for legacy/fake
     * repositories that predate the aggregate schema; database adapters always
     * provide an explicit value.
     */
    val dateValidation: String? = null
)

object ParticipantAccessMapper {
    fun fromRepositoryRecord(record: ParticipantRepositoryRecord): ParticipantAccessState {
        return when (record.role.uppercase()) {
            "ORGANIZER" -> ParticipantAccessState.organizer(record.userId)
            "MEMBER", "PARTICIPANT" -> ParticipantAccessState.member(
                userId = record.userId,
                rsvp = when (record.rsvp) {
                    "ACCEPTED" -> ParticipantRsvp.ACCEPTED
                    "DECLINED" -> ParticipantRsvp.DECLINED
                    "PENDING" -> ParticipantRsvp.PENDING
                    else -> ParticipantRsvp.UNAVAILABLE
                },
                dateValidation = record.dateValidation?.let(::parsePersistedDateValidation)
                    ?: when (record.hasValidatedDate) {
                        1L -> DateValidationState.VALIDATED_RETAINED_DATE
                        0L -> DateValidationState.NOT_VALIDATED
                        else -> DateValidationState.UNAVAILABLE
                    }
            )
            else -> ParticipantAccessState.nonMember(record.userId)
        }
    }

    private fun parsePersistedDateValidation(value: String): DateValidationState = when (value) {
        "NOT_VALIDATED" -> DateValidationState.NOT_VALIDATED
        "VALIDATED_RETAINED_DATE" -> DateValidationState.VALIDATED_RETAINED_DATE
        "NOT_APPLICABLE" -> DateValidationState.NOT_APPLICABLE
        "UNAVAILABLE" -> DateValidationState.UNAVAILABLE
        else -> DateValidationState.UNAVAILABLE
    }
}

@Serializable
data class InvitationPreview(
    val visibility: InvitationPreviewVisibility,
    val eventId: String,
    val title: String? = null,
    val canJoin: Boolean,
    val exposesOrganizationDetails: Boolean = false
)

@Serializable
data class AccessDecision(
    val isAllowed: Boolean,
    val reason: AccessDeniedReason? = null
)

object EventAccessPolicy {
    /** The exact organizer or an accepted active member may submit a poll ballot. */
    fun canSubmitPollBallot(
        event: Event,
        viewer: ParticipantAccessState
    ): AccessDecision {
        if (viewer.userId == event.organizerId && viewer.role == ParticipantAccessState.Role.ORGANIZER) {
            return AccessDecision(isAllowed = true)
        }
        if (viewer.role != ParticipantAccessState.Role.MEMBER || !event.participants.contains(viewer.userId)) {
            return AccessDecision(isAllowed = false, reason = AccessDeniedReason.NOT_EVENT_MEMBER)
        }
        return when (viewer.rsvp) {
            ParticipantRsvp.ACCEPTED -> AccessDecision(isAllowed = true)
            ParticipantRsvp.DECLINED ->
                AccessDecision(isAllowed = false, reason = AccessDeniedReason.PARTICIPATION_DECLINED)
            ParticipantRsvp.PENDING,
            ParticipantRsvp.NOT_APPLICABLE,
            ParticipantRsvp.UNAVAILABLE ->
                AccessDecision(isAllowed = false, reason = AccessDeniedReason.ATTENDANCE_NOT_CONFIRMED)
        }
    }

    fun invitationPreviewFor(
        event: Event,
        viewer: ParticipantAccessState
    ): InvitationPreview {
        val isKnownViewer = viewer.role == ParticipantAccessState.Role.ORGANIZER ||
            event.participants.contains(viewer.userId)

        return if (isKnownViewer) {
            InvitationPreview(
                visibility = InvitationPreviewVisibility.EVENT_METADATA,
                eventId = event.id,
                title = event.title,
                canJoin = viewer.role != ParticipantAccessState.Role.ORGANIZER,
                exposesOrganizationDetails = false
            )
        } else {
            InvitationPreview(
                visibility = InvitationPreviewVisibility.PUBLIC_METADATA_ONLY,
                eventId = event.id,
                canJoin = false,
                exposesOrganizationDetails = false
            )
        }
    }

    fun canAccessOrganizationSection(
        event: Event,
        viewer: ParticipantAccessState,
        section: OrganizationSection
    ): AccessDecision {
        if (viewer.role == ParticipantAccessState.Role.ORGANIZER && viewer.userId == event.organizerId) {
            return AccessDecision(isAllowed = true)
        }

        if (viewer.role == ParticipantAccessState.Role.NON_MEMBER || !event.participants.contains(viewer.userId)) {
            return AccessDecision(isAllowed = false, reason = AccessDeniedReason.NOT_EVENT_MEMBER)
        }

        return when (viewer.rsvp) {
            ParticipantRsvp.DECLINED ->
                AccessDecision(isAllowed = false, reason = AccessDeniedReason.PARTICIPATION_DECLINED)
            ParticipantRsvp.PENDING ->
                AccessDecision(isAllowed = false, reason = AccessDeniedReason.ATTENDANCE_NOT_CONFIRMED)
            ParticipantRsvp.NOT_APPLICABLE,
            ParticipantRsvp.UNAVAILABLE ->
                AccessDecision(isAllowed = false, reason = AccessDeniedReason.ATTENDANCE_NOT_CONFIRMED)
            ParticipantRsvp.ACCEPTED ->
                if (viewer.dateValidation == DateValidationState.VALIDATED_RETAINED_DATE) {
                    AccessDecision(isAllowed = true)
                } else {
                    AccessDecision(
                        isAllowed = false,
                        reason = AccessDeniedReason.RETAINED_DATE_NOT_VALIDATED
                    )
                }
        }
    }
}
