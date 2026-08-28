package com.guyghost.wakeve.invitationexperience

import java.io.File
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
    fun `studio commit gate persists its envelope retry budget and correlated resolution fence`() {
        val source = listOf(
            File("src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/InvitationExperiencePublicContracts.kt"),
            File("shared/src/commonMain/kotlin/com/guyghost/wakeve/invitationexperience/InvitationExperiencePublicContracts.kt")
        ).first(File::isFile).readText()
        val gate = source.substringAfter("sealed interface CreationStudioState")
            .substringBefore("enum class StudioResolutionOutcome")

        for (field in listOf(
            "durableOperationRef",
            "requestFingerprint",
            "resolutionRetryBudget",
            "attemptId",
            "fence"
        )) {
            assertTrue(
                gate.contains(field),
                "The shared Studio reducer is missing the reviewed commit-gate field $field."
            )
        }
        assertTrue(
            gate.contains("COMMIT_OUTCOME_UNKNOWN") && gate.contains("RESOLUTION_OUTCOME_UNKNOWN"),
            "Database uncertainty and resolution uncertainty must remain distinct terminal evidence."
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
