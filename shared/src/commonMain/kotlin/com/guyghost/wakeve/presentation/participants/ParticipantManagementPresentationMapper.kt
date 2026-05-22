package com.guyghost.wakeve.presentation.participants

import com.guyghost.wakeve.access.DateValidationState
import com.guyghost.wakeve.access.ParticipantAccessState
import com.guyghost.wakeve.access.ParticipantRsvp

data class ParticipantManagementRow(
    val userIdOrEmail: String,
    val roleLabel: String,
    val statusLabel: String,
    val canAccessOrganizationDetails: Boolean
)

object ParticipantManagementPresentationMapper {
    fun map(participants: List<ParticipantAccessState>): List<ParticipantManagementRow> =
        participants.map { participant ->
            ParticipantManagementRow(
                userIdOrEmail = participant.userId,
                roleLabel = participant.role.toRoleLabel(),
                statusLabel = participant.toStatusLabel(),
                canAccessOrganizationDetails = participant.canAccessOrganizationDetails()
            )
        }

    private fun ParticipantAccessState.Role.toRoleLabel(): String =
        when (this) {
            ParticipantAccessState.Role.ORGANIZER -> "Organizer"
            ParticipantAccessState.Role.MEMBER,
            ParticipantAccessState.Role.NON_MEMBER -> "Member"
        }

    private fun ParticipantAccessState.toStatusLabel(): String =
        when (rsvp) {
            ParticipantRsvp.DECLINED -> "Declined"
            ParticipantRsvp.ACCEPTED ->
                if (canAccessOrganizationDetails()) {
                    "Confirmed"
                } else {
                    "Pending"
                }
            ParticipantRsvp.PENDING -> "Pending"
        }

    private fun ParticipantAccessState.canAccessOrganizationDetails(): Boolean =
        role == ParticipantAccessState.Role.ORGANIZER ||
            (rsvp == ParticipantRsvp.ACCEPTED &&
                dateValidation == DateValidationState.VALIDATED_RETAINED_DATE)
}
