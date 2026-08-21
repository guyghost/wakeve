package com.guyghost.wakeve.invitationexperience

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class AudienceProjectorContractTest {
    private val projector = AudienceProjector()

    @Test
    fun `counts reconcile to the same identities without propagating between axes`() {
        val identities = listOf(
            AudienceIdentityAxes(
                identityKey = "recipient-hmac-1",
                delivery = InviteDeliveryState.Delivered("invitation-1"),
                approval = ApprovalState.Requested("request-1"),
                membership = MembershipState.NonMember,
                rsvp = RsvpState.NOT_APPLICABLE,
                dateValidation = DateValidationState.NOT_APPLICABLE
            ),
            AudienceIdentityAxes(
                identityKey = "member-1",
                delivery = InviteDeliveryState.ServerAccepted("invitation-2"),
                approval = ApprovalState.Approved("request-2"),
                membership = MembershipState.ActiveMember("member-1"),
                rsvp = RsvpState.ACCEPTED,
                dateValidation = DateValidationState.VALIDATED_RETAINED_DATE
            ),
            AudienceIdentityAxes(
                identityKey = "member-2",
                delivery = InviteDeliveryState.None,
                approval = ApprovalState.NotApplicable,
                membership = MembershipState.ActiveMember("member-2"),
                rsvp = RsvpState.DECLINED,
                dateValidation = DateValidationState.NOT_VALIDATED
            )
        )

        val result = projector.project(identities, Freshness.Current)

        assertEquals(identities, result.identities)
        assertEquals(Freshness.Current, result.freshness)
        assertEquals(
            AudienceCounts(
                totalIdentities = 3,
                deliveredInvitations = 1,
                pendingApprovals = 1,
                activeMembers = 2,
                acceptedRsvps = 1,
                validatedDates = 1
            ),
            result.counts
        )
        assertEquals(
            InviteDeliveryState.ServerAccepted("invitation-2"),
            result.identities[1].delivery,
            "Server acceptance must not be inferred as delivery."
        )
        assertEquals(
            MembershipState.NonMember,
            result.identities[0].membership,
            "Requested approval must not be inferred as membership."
        )
    }

    @Test
    fun `unavailable member axes remain unavailable rather than guessed`() {
        val unavailable = AudienceIdentityAxes(
            identityKey = "member-incomplete",
            delivery = InviteDeliveryState.None,
            approval = ApprovalState.NotApplicable,
            membership = MembershipState.ActiveMember("member-incomplete"),
            rsvp = RsvpState.UNAVAILABLE,
            dateValidation = DateValidationState.UNAVAILABLE
        )

        val projection = projector.project(listOf(unavailable), Freshness.Unavailable)

        assertEquals(RsvpState.UNAVAILABLE, projection.identities.single().rsvp)
        assertEquals(DateValidationState.UNAVAILABLE, projection.identities.single().dateValidation)
        assertEquals(1, projection.counts.activeMembers)
        assertEquals(0, projection.counts.acceptedRsvps)
        assertEquals(0, projection.counts.validatedDates)
    }

    @Test
    fun `recipient key rejects raw recipient identifiers at construction boundary`() {
        listOf(
            "alice@example.com",
            "+33612345678",
            "Alice Dupont",
            ""
        ).forEach { rawIdentifier ->
            assertFails("Raw recipient identifier must not become a persisted key: $rawIdentifier") {
                RecipientKey(rawIdentifier)
            }
        }
    }
}
