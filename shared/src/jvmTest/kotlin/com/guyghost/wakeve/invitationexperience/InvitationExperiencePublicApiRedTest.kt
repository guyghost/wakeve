package com.guyghost.wakeve.invitationexperience

import kotlin.test.Test
import kotlin.test.assertTrue

class InvitationExperiencePublicApiRedTest {

    @Test
    fun `library exposes typed projection action and exact cancellation contracts`() {
        assertPublicTypes(
            "EventLibraryProjector",
            "LibraryProjection",
            "LibraryNextAction",
            "PreviousStableState",
            "LibraryLoadState"
        )
    }

    @Test
    fun `studio exposes total artwork state machine and atomic owner transaction`() {
        assertPublicTypes(
            "CreationStudioStateMachine",
            "Artwork",
            "ArtworkSelectionCapability",
            "UpdateDraftAggregateUseCase"
        )
    }

    @Test
    fun `audience exposes coherent projection and direct invite owner`() {
        assertPublicTypes(
            "AudienceProjector",
            "DirectInviteBatchUseCase"
        )
    }

    @Test
    fun `event information exposes notification policy and event scoped repository`() {
        assertPublicTypes(
            "EventNotificationPolicy",
            "EventNotificationPreferenceRepository"
        )
    }

    @Test
    fun `canvas and deep links share one global invitation experience router`() {
        assertPublicTypes("InvitationExperienceRouter")
    }

    private fun assertPublicTypes(vararg simpleNames: String) {
        val missing = simpleNames.filter { simpleName ->
            runCatching {
                Class.forName("com.guyghost.wakeve.invitationexperience.$simpleName")
            }.isFailure
        }

        assertTrue(
            missing.isEmpty(),
            "Missing approved invitation-experience contracts: $missing"
        )
    }
}
