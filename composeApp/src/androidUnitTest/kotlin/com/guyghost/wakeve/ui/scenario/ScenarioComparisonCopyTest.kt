package com.guyghost.wakeve.ui.scenario

import com.guyghost.wakeve.models.EventStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ScenarioComparisonCopyTest {

    @Test
    fun scenarioComparisonCopyUsesFrenchDecisionLabels() {
        assertEquals("Comparer les scenarios", scenarioComparisonScreenTitle())
        assertEquals("Retour", scenarioComparisonBackContentDescription())
        assertEquals("Aucun scenario a comparer", scenarioComparisonEmptyTitle())
        assertEquals("Voir les reunions", scenarioComparisonViewMeetingsLabel())
        assertEquals("Ouvrir le transport", scenarioComparisonOpenTransportLabel())
        assertEquals("Option en tete", scenarioComparisonCurrentLeaderLabel())
        assertEquals("Retenir ce scenario", scenarioComparisonSelectFinalLabel())
        assertEquals("Retenu", scenarioComparisonSelectedStatusLabel())
        assertEquals("En tete", scenarioComparisonLeaderBadgeLabel())
        assertEquals("Pour", scenarioComparisonPreferVoteLabel())
        assertEquals("Neutre", scenarioComparisonNeutralVoteLabel())
        assertEquals("Contre", scenarioComparisonAgainstVoteLabel())
        assertEquals("Voter pour cette option", scenarioComparisonVoteForThisLabel())
    }

    @Test
    fun scenarioComparisonCopyExplainsAccessAndWorkflowState() {
        assertEquals(
            "Confirmez votre presence pour comparer les details des scenarios.",
            scenarioComparisonLockedAccessMessage()
        )
        assertEquals(
            "Confirmez votre presence pour voter et ouvrir les details.",
            scenarioComparisonVoteLockedMessage()
        )
        assertEquals(
            "Les details des scenarios sont disponibles apres confirmation de presence.",
            scenarioComparisonWorkflowLockedMessage()
        )
        assertEquals(
            "Comparez destination, logement, periode, budget, duree et adequation au groupe.",
            scenarioComparisonWorkflowAvailableMessage()
        )
        assertEquals("Statut: COMPARING", scenarioComparisonWorkflowStatusLabel(EventStatus.COMPARING))
        assertEquals("Statut: INCONNU", scenarioComparisonWorkflowStatusLabel(null))
    }

    @Test
    fun scenarioComparisonCopyDoesNotUseOldEnglishDefaults() {
        val copy = listOf(
            scenarioComparisonScreenTitle(),
            scenarioComparisonEmptyTitle(),
            scenarioComparisonEmptyMessage(),
            scenarioComparisonViewMeetingsLabel(),
            scenarioComparisonOpenTransportLabel(),
            scenarioComparisonLockedAccessMessage(),
            scenarioComparisonCurrentLeaderLabel(),
            scenarioComparisonSelectFinalLabel(),
            scenarioComparisonSelectedStatusLabel(),
            scenarioComparisonLeaderBadgeLabel(),
            scenarioComparisonLocationLabel("Marseille"),
            scenarioComparisonBudgetPerPersonLabel("100 EUR"),
            scenarioComparisonPeopleLabel(8),
            scenarioComparisonPreferVoteLabel(),
            scenarioComparisonNeutralVoteLabel(),
            scenarioComparisonAgainstVoteLabel(),
            scenarioComparisonVoteForThisLabel(),
            scenarioComparisonVoteLockedMessage(),
            scenarioComparisonWorkflowLockedMessage(),
            scenarioComparisonWorkflowAvailableMessage()
        )

        copy.forEach { label ->
            listOf(
                "Compare Scenarios",
                "No scenarios to compare",
                "Create scenarios",
                "View Meetings",
                "Open Transport",
                "Confirm attendance",
                "Current Leader",
                "Select This Scenario",
                "Selected",
                "Leader",
                "Destination / lodging",
                "/person",
                "people",
                "Prefer",
                "Neutral",
                "Against",
                "Vote for this",
                "Scenario details are available",
                "participant fit"
            ).forEach { oldCopy ->
                assertFalse(
                    label.contains(oldCopy, ignoreCase = true),
                    "Scenario comparison copy should not contain `$oldCopy`: $label"
                )
            }
        }
    }
}
