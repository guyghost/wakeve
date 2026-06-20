package com.guyghost.wakeve.ui.scenario

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ScenarioDetailCopyTest {

    @Test
    fun scenarioDetailCopyUsesFrenchLabels() {
        assertEquals("Details du scenario", scenarioDetailScreenTitle())
        assertEquals("Retour", scenarioDetailBackContentDescription())
        assertEquals("Retenu", scenarioDetailSelectedStatusLabel())
        assertEquals("Date / periode", scenarioDetailDateLabel())
        assertEquals("Destination / logement", scenarioDetailLocationLabel())
        assertEquals("Duree", scenarioDetailDurationLabel())
        assertEquals("1 jour", scenarioDetailDurationValue(1))
        assertEquals("3 jours", scenarioDetailDurationValue(3))
        assertEquals("Participants", scenarioDetailParticipantsLabel())
        assertEquals("8 personnes", scenarioDetailParticipantsValue(8))
        assertEquals("Resultats des votes", scenarioDetailVotingResultsTitle())
        assertEquals("Retenir ce scenario", scenarioDetailSelectFinalLabel())
        assertEquals("Voir les reunions", scenarioDetailViewMeetingsLabel())
        assertEquals("Ouvrir le transport", scenarioDetailOpenTransportLabel())
    }

    @Test
    fun scenarioDetailCopyExplainsAccessBudgetAndVotes() {
        assertEquals("Acces au scenario verrouille", scenarioDetailAccessLockedTitle())
        assertEquals(
            "Confirmez votre presence pour voir la destination, le logement et le budget.",
            scenarioDetailAccessLockedMessage()
        )
        assertEquals("Par personne", scenarioDetailBudgetPerPersonLabel())
        assertEquals("Estimation totale", scenarioDetailBudgetTotalLabel())
        assertEquals("Pour 12 participants", scenarioDetailBudgetParticipantsLabel(12))
        assertEquals("Pour", scenarioDetailPreferVoteLabel())
        assertEquals("Neutre", scenarioDetailNeutralVoteLabel())
        assertEquals("Contre", scenarioDetailAgainstVoteLabel())
        assertEquals("Votes: 7 / 12 participants", scenarioDetailTotalVotesLabel(7, 12))
    }

    @Test
    fun scenarioDetailCopyDoesNotUseOldEnglishDefaults() {
        val copy = listOf(
            scenarioDetailScreenTitle(),
            scenarioDetailBackContentDescription(),
            scenarioDetailSelectedStatusLabel(),
            scenarioDetailDateLabel(),
            scenarioDetailLocationLabel(),
            scenarioDetailDurationLabel(),
            scenarioDetailDurationValue(2),
            scenarioDetailParticipantsValue(8),
            scenarioDetailVotingResultsTitle(),
            scenarioDetailSelectFinalLabel(),
            scenarioDetailViewMeetingsLabel(),
            scenarioDetailOpenTransportLabel(),
            scenarioDetailAccessLockedTitle(),
            scenarioDetailAccessLockedMessage(),
            scenarioDetailBudgetPerPersonLabel(),
            scenarioDetailBudgetTotalLabel(),
            scenarioDetailPreferVoteLabel(),
            scenarioDetailNeutralVoteLabel(),
            scenarioDetailAgainstVoteLabel(),
            scenarioDetailTotalVotesLabel(3, 8)
        )

        copy.forEach { label ->
            listOf(
                "Scenario Details",
                "Back",
                "Selected",
                "Date/Period",
                "Destination / lodging",
                "Duration",
                "days",
                "people",
                "Voting Results",
                "Select as Final Scenario",
                "View Meetings",
                "Open Transport",
                "Scenario access locked",
                "Confirm your attendance",
                "Per Person",
                "Total Estimate",
                "Prefer",
                "Neutral",
                "Against",
                "Total votes"
            ).forEach { oldCopy ->
                assertFalse(
                    label.contains(oldCopy, ignoreCase = true),
                    "Scenario detail copy should not contain `$oldCopy`: $label"
                )
            }
        }
    }
}
