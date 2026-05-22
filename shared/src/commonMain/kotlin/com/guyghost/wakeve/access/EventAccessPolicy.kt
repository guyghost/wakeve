package com.guyghost.wakeve.access

import com.guyghost.wakeve.models.Event
import kotlinx.serialization.Serializable

@Serializable
enum class ParticipantRsvp {
    PENDING,
    ACCEPTED,
    DECLINED
}

@Serializable
enum class DateValidationState {
    NOT_VALIDATED,
    VALIDATED_RETAINED_DATE
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
                rsvp = ParticipantRsvp.PENDING,
                dateValidation = DateValidationState.NOT_VALIDATED
            )
    }
}

data class ParticipantRepositoryRecord(
    val id: String,
    val eventId: String,
    val userId: String,
    val role: String,
    val rsvp: String,
    val hasValidatedDate: Long
)

object ParticipantAccessMapper {
    fun fromRepositoryRecord(record: ParticipantRepositoryRecord): ParticipantAccessState {
        if (record.role == "ORGANIZER") {
            return ParticipantAccessState.organizer(record.userId)
        }

        return ParticipantAccessState.member(
            userId = record.userId,
            rsvp = when (record.rsvp) {
                "ACCEPTED" -> ParticipantRsvp.ACCEPTED
                "DECLINED" -> ParticipantRsvp.DECLINED
                else -> ParticipantRsvp.PENDING
            },
            dateValidation = if (record.hasValidatedDate == 1L) {
                DateValidationState.VALIDATED_RETAINED_DATE
            } else {
                DateValidationState.NOT_VALIDATED
            }
        )
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
