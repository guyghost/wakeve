package com.guyghost.wakeve.ui.transport

import com.guyghost.wakeve.models.OptimizationType
import com.guyghost.wakeve.models.TransportLocation
import com.guyghost.wakeve.models.TransportPlan
import com.guyghost.wakeve.models.TransportReadiness
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class TransportPlanningCopyTest {

    @Test
    fun transportPlanningCopyExplainsBlockedGenerationStates() {
        assertEquals(
            "Aucun fournisseur de transport réel n'est configuré. Wakeve peut collecter les départs, mais ne génère pas encore de prix, horaires ou réservations.",
            transportProviderMissingMessage()
        )
        assertEquals(
            "Seul l'organisateur peut générer le plan de transport partagé.",
            transportOrganizerOnlyGenerationMessage()
        )
        assertEquals(
            "Sélectionnez une destination de scénario avant de générer un plan de transport.",
            transportDestinationRequiredForGenerationMessage()
        )
        assertEquals(
            "Ajoutez tous les points de départ manquants avant de générer les options de transport.",
            transportMissingDeparturesForGenerationMessage()
        )
    }

    @Test
    fun transportPlanningCopyUsesClearAnchorAndReadinessLabels() {
        assertEquals("Événement event-123", transportEventLabel("event-123"))
        assertEquals("Date confirmée : 2026-07-01", transportConfirmedDateLabel("2026-07-01"))
        assertEquals("Date confirmée : date bientôt confirmée", transportConfirmedDateLabel(null))
        assertEquals("Destination : Marseille", transportDestinationLabel("Marseille"))
        assertEquals("Destination : aucune destination sélectionnée", transportDestinationLabel(null))
        assertEquals("Préparation complète", transportReadinessTitle(true))
        assertEquals("Préparation incomplète", transportReadinessTitle(false))
        assertEquals("- Nora", transportMissingDepartureParticipantLabel("Nora"))
    }

    @Test
    fun transportPlanningCopyClarifiesPlanState() {
        assertEquals("Aucun plan généré", transportEmptyPlansTitle())
        assertEquals(
            "Choisissez un mode d'optimisation et générez un plan lorsque tous les départs sont prêts.",
            transportEmptyPlansConfiguredMessage()
        )
        assertEquals("Plan sélectionné", transportSelectedPlanLabel())
        assertEquals("Plan généré : plan-1", transportGeneratedPlanLabel("plan-1"))
        assertEquals("Coût total du groupe : 125.5 EUR", transportTotalGroupCostLabel(125.5))
        assertEquals("Trajets : 4", transportRoutesCountLabel(4))
        assertEquals("Sélectionner le plan final", transportSelectFinalPlanLabel())
        assertEquals("Plan final sélectionné", transportFinalSelectionUnavailableLabel(true))
        assertEquals("Sélection indisponible", transportFinalSelectionUnavailableLabel(false))
    }

    @Test
    fun transportMeetingPointSummaryUsesSelectedDestinationAndPlannedArrival() {
        val summary = transportMeetingPointSummary(
            selectedDestination = TransportLocation(name = "Lyon Part-Dieu"),
            highlightedPlan = transportPlan(groupArrivals = listOf("2026-07-01T18:30:00Z")),
            readiness = transportReadiness(isComplete = true)
        )

        assertEquals("Rendez-vous à Lyon Part-Dieu", summary.title)
        assertEquals(
            "Arrivée groupée prévue à Lyon Part-Dieu : 2026-07-01 à 18:30.",
            summary.body
        )
        assertEquals(null, summary.detail)
    }

    @Test
    fun transportMeetingPointSummaryKeepsRendezvousExplicitWhenDeparturesAreMissing() {
        val summary = transportMeetingPointSummary(
            selectedDestination = TransportLocation(name = "Marseille"),
            highlightedPlan = null,
            readiness = transportReadiness(
                isComplete = false,
                missingNames = listOf("Nora", "Ilyes", "Lina", "Sam")
            )
        )

        assertEquals("Rendez-vous à Marseille", summary.title)
        assertEquals(
            "Marseille est le point de rendez-vous provisoire tant que tous les départs ne sont pas renseignés.",
            summary.body
        )
        assertEquals("Départs encore manquants : Nora, Ilyes, Lina + 1 autre.", summary.detail)
    }

    @Test
    fun transportMeetingPointSummaryExplainsMissingDestinationAndTransportNotNeeded() {
        assertEquals(
            TransportMeetingPointSummary(
                title = "Point de rendez-vous à définir",
                body = "Choisissez d'abord la destination finale pour afficher clairement où le groupe se retrouve."
            ),
            transportMeetingPointSummary(
                selectedDestination = null,
                highlightedPlan = null,
                readiness = null
            )
        )

        assertEquals(
            TransportMeetingPointSummary(
                title = "Rendez-vous à Nantes",
                body = "Transport non requis : le groupe se retrouve directement à Nantes."
            ),
            transportMeetingPointSummary(
                selectedDestination = TransportLocation(name = "Nantes"),
                highlightedPlan = null,
                readiness = transportReadiness(isComplete = true, transportNotNeeded = true)
            )
        )
    }

    @Test
    fun transportPlanningCopyDoesNotExposeRawEnglishEventLabel() {
        val copy = listOf(
            transportEventLabel("event-123"),
            transportConfirmedDateLabel(null),
            transportDestinationLabel(null),
            transportAccessDeniedTitle(),
            transportAccessDeniedMessage(),
            transportMissingDestinationTitle(),
            transportMissingDestinationMessage(),
            transportPendingSyncMessage(),
            transportEmptyPlansProviderMissingMessage(),
            transportTotalGroupCostLabel(99.0)
        )

        copy.forEach { label ->
            listOf(
                "Event event-123",
                "Total group cost"
            ).forEach { oldCopy ->
                assertFalse(
                    label.contains(oldCopy, ignoreCase = true),
                    "Transport copy should not contain `$oldCopy`: $label"
                )
            }
        }
    }

    private fun transportPlan(groupArrivals: List<String>): TransportPlan {
        return TransportPlan(
            eventId = "event-123",
            participantRoutes = emptyMap(),
            groupArrivals = groupArrivals,
            totalGroupCost = 0.0,
            optimizationType = OptimizationType.BALANCED,
            createdAt = "2026-06-20T10:00:00Z",
            id = "plan-123"
        )
    }

    private fun transportReadiness(
        isComplete: Boolean,
        transportNotNeeded: Boolean = false,
        missingNames: List<String> = emptyList()
    ): TransportReadiness {
        return TransportReadiness(
            eventId = "event-123",
            destination = TransportLocation(name = "Destination"),
            isComplete = isComplete,
            canGeneratePlan = isComplete && !transportNotNeeded,
            transportNotNeeded = transportNotNeeded,
            canFinalizeWithoutPlan = transportNotNeeded,
            missingDepartureParticipantIds = emptyList(),
            missingDepartureParticipantNames = missingNames
        )
    }
}
