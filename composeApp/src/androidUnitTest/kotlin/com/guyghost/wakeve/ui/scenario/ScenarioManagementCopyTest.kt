package com.guyghost.wakeve.ui.scenario

import com.guyghost.wakeve.models.EventStatus
import com.guyghost.wakeve.models.Scenario
import com.guyghost.wakeve.models.ScenarioStatus
import com.guyghost.wakeve.models.ScenarioVotingResult
import com.guyghost.wakeve.models.ScenarioWithVotes
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
        assertEquals("Ou allons-nous ?", scenarioDestinationQuestionLabel())
        assertEquals("Destination retenue", scenarioDestinationSelectedTitle())
        assertEquals("Option en tete", scenarioDestinationLeaderTitle())
        assertEquals("Destination a choisir", scenarioDestinationPendingTitle())
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

    @Test
    fun scenarioDestinationSummarySurfacesSelectedDestination() {
        val summary = scenarioDestinationSummary(
            listOf(
                scenarioWithVotes(
                    id = "draft",
                    location = "Lyon",
                    score = 8,
                    totalVotes = 4
                ),
                scenarioWithVotes(
                    id = "selected",
                    name = "Maison a Marseille",
                    location = "Marseille",
                    dateOrPeriod = "12-14 juillet",
                    duration = 3,
                    status = ScenarioStatus.SELECTED,
                    score = 3,
                    totalVotes = 2
                )
            )
        )

        assertEquals("Destination retenue", summary.title)
        assertEquals("Marseille", summary.headline)
        assertEquals("Maison a Marseille - 12-14 juillet - 3 jours", summary.details)
        assertEquals("Scenario retenu pour le groupe", summary.voteSignal)
    }

    @Test
    fun scenarioDestinationSummarySurfacesLeadingDestination() {
        val summary = scenarioDestinationSummary(
            listOf(
                scenarioWithVotes(
                    id = "lower",
                    location = "Annecy",
                    score = 2,
                    totalVotes = 3
                ),
                scenarioWithVotes(
                    id = "leader",
                    name = "Villa ocean",
                    location = "Biarritz",
                    dateOrPeriod = "aout",
                    duration = 5,
                    score = 7,
                    totalVotes = 4
                )
            )
        )

        assertEquals("Option en tete", summary.title)
        assertEquals("Biarritz", summary.headline)
        assertEquals("Villa ocean - aout - 5 jours", summary.details)
        assertEquals("Score 7 avec 4 votes", summary.voteSignal)
    }

    @Test
    fun scenarioDestinationSummaryKeepsNoVoteStateActionable() {
        val summary = scenarioDestinationSummary(
            listOf(
                scenarioWithVotes(id = "a", location = "Nantes"),
                scenarioWithVotes(id = "b", location = "Rennes")
            )
        )

        assertEquals("Destination a choisir", summary.title)
        assertEquals("2 options a departager", summary.headline)
        assertEquals("Premiere option: Nantes - week-end", summary.details)
        assertEquals("Aucun vote pour le moment", summary.voteSignal)
    }

    private fun scenarioWithVotes(
        id: String,
        name: String = "Option $id",
        location: String = "Paris",
        dateOrPeriod: String = "week-end",
        duration: Int = 2,
        status: ScenarioStatus = ScenarioStatus.PROPOSED,
        score: Int = 0,
        totalVotes: Int = 0
    ): ScenarioWithVotes {
        val preferCount = if (totalVotes > 0) totalVotes else 0

        return ScenarioWithVotes(
            scenario = Scenario(
                id = id,
                eventId = "event-1",
                name = name,
                dateOrPeriod = dateOrPeriod,
                location = location,
                duration = duration,
                estimatedParticipants = 6,
                estimatedBudgetPerPerson = 120.0,
                description = "Option de test",
                status = status,
                createdAt = "2026-06-20T10:00:00Z",
                updatedAt = "2026-06-20T10:00:00Z"
            ),
            votes = emptyList(),
            votingResult = ScenarioVotingResult(
                scenarioId = id,
                preferCount = preferCount,
                neutralCount = 0,
                againstCount = 0,
                totalVotes = totalVotes,
                score = score
            )
        )
    }
}
