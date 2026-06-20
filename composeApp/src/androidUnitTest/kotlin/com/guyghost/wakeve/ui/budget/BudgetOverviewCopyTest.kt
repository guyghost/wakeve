package com.guyghost.wakeve.ui.budget

import com.guyghost.wakeve.models.BudgetCategory
import com.guyghost.wakeve.models.BudgetItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BudgetOverviewCopyTest {

    @Test
    fun budgetOverviewCopyUsesClearFrenchLabels() {
        assertEquals("Budget", budgetOverviewTitle())
        assertEquals("Retour", budgetBackContentDescription())
        assertEquals("Voir les détails", budgetDetailsContentDescription())
        assertEquals("Aucun budget pour cet événement", budgetEmptyTitle())
        assertEquals("Créer un budget", budgetCreateActionLabel())
        assertEquals("Budget total", budgetTotalTitle())
        assertEquals("Estimé", budgetEstimatedLabel())
        assertEquals("Dépensé", budgetSpentLabel())
        assertEquals("45,5% utilisé", budgetUsageLabel(45.5))
        assertEquals("Par personne (8 participants)", budgetPerPersonTitle(8))
    }

    @Test
    fun paymentPotCopyDoesNotExposeRawEventIds() {
        assertEquals("Cagnotte de l'événement", paymentPotDefaultTitle())
        assertEquals("Cagnotte", paymentPotScreenTitle())
        assertEquals(
            "Cagnotte active - 120.0 EUR. Les changements sont enregistrés sur cet appareil.",
            paymentPotActiveMessage(120.0, "EUR")
        )
        assertEquals("Aucune cagnotte active pour cet événement.", paymentPotInactiveMessage())
        assertEquals("Créer une cagnotte", paymentPotCreateActionLabel())
        assertEquals("Activer la cagnotte", paymentPotActivateActionLabel())
        assertEquals("Ouvrir la cagnotte", paymentPotOpenActionLabel())
        assertEquals("Clôturer la cagnotte", paymentPotCloseActionLabel())

        listOf(paymentPotDefaultTitle(), paymentPotInactiveMessage()).forEach { label ->
            assertFalse(
                label.contains("event-123", ignoreCase = true) ||
                    label.contains("Cagnotte event", ignoreCase = true),
                "Payment pot copy should not expose raw event ids: $label"
            )
        }
    }

    @Test
    fun tricountCopyExplainsTrustState() {
        assertEquals("Tricount", tricountScreenTitle())
        assertEquals("Ouvrir Tricount", tricountOpenActionLabel())
        assertEquals("Lien Tricount", tricountLinkTitle())
        assertEquals(
            "Aucun lien Tricount n'est encore associé. Ajoutez un lien vérifié avant de rediriger les participants.",
            tricountMissingLinkMessage()
        )
        assertEquals("Lien vérifié pour TRICOUNT.", tricountLinkStatusMessage("vérifié", "TRICOUNT"))
        assertEquals("Lien non vérifié pour Tricount.", tricountLinkStatusMessage(null, null))
        assertEquals("Associer Tricount", tricountLinkActionLabel())
        assertEquals("Dissocier Tricount", tricountUnlinkActionLabel())
        assertEquals("Tricount non requis", tricountNotNeededActionLabel())
    }

    @Test
    fun budgetStatusCopyAvoidsDecorativeEmojiOnlySignals() {
        assertEquals(
            "Dépassement de budget",
            budgetStatusText(isOverBudget = true, remaining = -10.0, totalEstimated = 100.0, totalActual = 110.0)
        )
        assertEquals(
            "Budget presque épuisé",
            budgetStatusText(isOverBudget = false, remaining = 5.0, totalEstimated = 100.0, totalActual = 95.0)
        )
        assertEquals(
            "Aucune dépense enregistrée",
            budgetStatusText(isOverBudget = false, remaining = 100.0, totalEstimated = 100.0, totalActual = 0.0)
        )
        assertEquals(
            "Dans le budget",
            budgetStatusText(isOverBudget = false, remaining = 40.0, totalEstimated = 100.0, totalActual = 60.0)
        )

        listOf(
            budgetStatusText(true, -10.0, 100.0, 110.0),
            budgetStatusText(false, 5.0, 100.0, 95.0),
            budgetStatusText(false, 100.0, 100.0, 0.0),
            budgetStatusText(false, 40.0, 100.0, 60.0)
        ).forEach { status ->
            listOf("⚠️", "⚡", "💡", "✓").forEach { symbol ->
                assertFalse(
                    status.contains(symbol),
                    "Budget status should not depend on decorative symbol `$symbol`: $status"
                )
            }
        }
    }

    @Test
    fun offlineBudgetCopyIsExplicit() {
        assertEquals(
            "Synchronisation en attente. Modifications locales en attente d'envoi.",
            budgetPendingOfflineSyncMessage()
        )
        assertEquals("Modifications locales en attente d'envoi.", budgetPendingSyncMessage())
        assertEquals("Données locales disponibles hors ligne.", budgetOfflineAvailableMessage())
        assertEquals("Reste disponible : 42,50 €", budgetRemainingLabel(42.5))
        assertEquals("Dépassement : 12,30 €", budgetOverspendLabel(12.3))
    }

    @Test
    fun settlementSummaryExplainsWhoMustReimburseWhom() {
        val summary = budgetSettlementSummary(
            items = listOf(
                budgetItem(
                    id = "dinner",
                    actualCost = 90.0,
                    paidBy = "alice@example.com",
                    sharedBy = listOf("alice@example.com", "user-2", "user-3")
                )
            )
        )

        assertEquals("2 remboursements à régler", summary.title)
        assertEquals(
            "2 remboursements à solder pour que chacun paie sa part réelle.",
            summary.body
        )
        assertEquals(
            listOf(
                "Participant 2 doit 30,00 € à Alice",
                "Participant 3 doit 30,00 € à Alice"
            ),
            summary.lines.map { it.sentence }
        )
        summary.lines.forEach { line ->
            assertFalse(line.sentence.contains("user-"))
            assertFalse(line.sentence.contains("@example.com"))
        }
    }

    @Test
    fun settlementSummaryHandlesNoPaidItemsAndBalancedExpenses() {
        assertEquals(
            BudgetSettlementSummary(
                title = "Remboursements",
                body = "Aucune dépense payée à répartir pour l'instant.",
                lines = emptyList()
            ),
            budgetSettlementSummary(items = emptyList())
        )

        assertEquals(
            BudgetSettlementSummary(
                title = "Remboursements à jour",
                body = "Les dépenses payées sont équilibrées entre les participants.",
                lines = emptyList()
            ),
            budgetSettlementSummary(
                items = listOf(
                    budgetItem(
                        id = "solo",
                        actualCost = 25.0,
                        paidBy = "user-2",
                        sharedBy = listOf("user-2")
                    )
                )
            )
        )
    }

    private fun budgetItem(
        id: String,
        actualCost: Double,
        paidBy: String,
        sharedBy: List<String>
    ): BudgetItem =
        BudgetItem(
            id = id,
            budgetId = "budget-1",
            category = BudgetCategory.MEALS,
            name = id,
            description = "",
            estimatedCost = actualCost,
            actualCost = actualCost,
            isPaid = true,
            paidBy = paidBy,
            sharedBy = sharedBy,
            notes = "",
            createdAt = "2026-06-01T08:00:00Z",
            updatedAt = "2026-06-01T08:00:00Z"
        )
}
