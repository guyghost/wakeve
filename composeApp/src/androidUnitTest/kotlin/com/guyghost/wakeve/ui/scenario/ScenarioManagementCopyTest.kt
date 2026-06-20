package com.guyghost.wakeve.ui.scenario

import com.guyghost.wakeve.models.EventStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ScenarioManagementCopyTest {

    @Test
    fun scenarioManagementCopyUsesFrenchActionLabels() {
        assertEquals("Scenarios proposes", scenarioScreenTitle())
        assertEquals("Comparer (2 selectionnes)", scenarioComparisonTitle(2))
        assertEquals("Selectionnez au moins 2 scenarios a comparer.", scenarioCompareMinimumMessage())
        assertEquals("Creer un scenario", scenarioCreateContentDescription())
        assertEquals("Vote enregistre.", scenarioVoteSubmittedMessage())
        assertEquals("Retenu", scenarioSelectedStatusLabel())
        assertEquals("Brouillon", scenarioDraftStatusLabel())
        assertEquals("Pour", scenarioPreferVoteLabel())
        assertEquals("Neutre", scenarioNeutralVoteLabel())
        assertEquals("Contre", scenarioAgainstVoteLabel())
        assertEquals("Statut: CONFIRMED", scenarioWorkflowStatusLabel(EventStatus.CONFIRMED))
        assertEquals("Statut: INCONNU", scenarioWorkflowStatusLabel(null))
    }

    @Test
    fun scenarioManagementCopyExplainsLockedAccessAndDeletion() {
        assertEquals(
            "Confirmez votre presence pour voir les details et voter.",
            scenarioAccessLockedDetailsMessage()
        )
        assertEquals(
            "Confirmez votre presence pour acceder aux details de destination, logement et vote.",
            scenarioAccessLockedEmptyMessage()
        )
        assertEquals("Supprimer le scenario ?", scenarioDeleteDialogTitle())
        assertEquals(
            "Voulez-vous vraiment supprimer \"Option mer\" ? Cette action est definitive.",
            scenarioDeleteDialogMessage("Option mer")
        )
    }

    @Test
    fun scenarioManagementCopyDoesNotUseOldEnglishDefaults() {
        val copy = listOf(
            scenarioCompareMinimumMessage(),
            scenarioCreateContentDescription(),
            scenarioAccessLockedDetailsMessage(),
            scenarioVoteSubmittedMessage(),
            scenarioCreatedMessage(),
            scenarioUpdatedMessage(),
            scenarioDeletedMessage(),
            scenarioSelectedStatusLabel(),
            scenarioDraftStatusLabel(),
            scenarioVotingResultsTitle(),
            scenarioNoVotesMessage(),
            scenarioEmptyTitle(),
            scenarioEmptyMessage(),
            scenarioCreateDialogTitle(),
            scenarioEditDialogTitle(),
            scenarioNameFieldLabel(),
            scenarioNamePlaceholder(),
            scenarioDateFieldLabel(),
            scenarioLocationFieldLabel(),
            scenarioDeleteDialogTitle(),
            scenarioDeleteDialogMessage("Option mer"),
            scenarioDismissContentDescription()
        )

        copy.forEach { label ->
            listOf(
                "Select at least",
                "Create scenario",
                "Confirm your attendance",
                "Vote submitted",
                "Scenario created",
                "Scenario updated",
                "Scenario deleted",
                "Selected",
                "Draft",
                "Voting Results",
                "No votes yet",
                "No scenarios yet",
                "Scenario Name",
                "Date or Period",
                "Location",
                "Delete Scenario",
                "Are you sure",
                "Dismiss"
            ).forEach { oldCopy ->
                assertFalse(
                    label.contains(oldCopy, ignoreCase = true),
                    "Scenario copy should not contain `$oldCopy`: $label"
                )
            }
        }
    }
}
