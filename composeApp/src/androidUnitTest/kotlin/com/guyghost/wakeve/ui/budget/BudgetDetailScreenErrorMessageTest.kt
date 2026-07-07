package com.guyghost.wakeve.ui.budget

import com.guyghost.wakeve.models.BudgetCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BudgetDetailScreenErrorMessageTest {

    @Test
    fun budgetItemSaveFailureMessage_doesNotExposeRepositoryFailureDetails() {
        val sensitiveRepositoryFailure = "SQL constraint failed for budget-1 user secret@example.com token=SECRET"

        val result = budgetItemSaveFailureMessage()

        assertEquals("Impossible d'enregistrer cette dépense. Réessayez.", result)
        assertFalse(result.contains(sensitiveRepositoryFailure))
        assertFalse(result.contains("secret@example.com", ignoreCase = true))
        assertFalse(result.contains("SECRET", ignoreCase = true))
        assertFalse(result.contains("SQL constraint", ignoreCase = true))
        assertFalse(result.contains("token=", ignoreCase = true))
    }

    @Test
    fun budgetDetailCopyUsesConcreteFrenchExpenseLabels() {
        assertEquals("Détails du budget", budgetDetailTitle())
        assertEquals("Retour", budgetDetailBackContentDescription())
        assertEquals("Aucun commentaire budget", budgetCommentContentDescription(0))
        assertEquals("1 commentaire budget", budgetCommentContentDescription(1))
        assertEquals("3 commentaires budget", budgetCommentContentDescription(3))
        assertEquals("Ajouter une dépense", budgetAddItemContentDescription())
        assertEquals("Aucune dépense enregistrée", budgetDetailEmptyMessage(noItems = true))
        assertEquals("Aucune dépense ne correspond aux filtres", budgetDetailEmptyMessage(noItems = false))
        assertEquals("Supprimer la dépense ?", budgetDeleteItemTitle())
        assertEquals("Dépassement de budget", budgetDetailOverBudgetLabel())
        assertEquals("Payées", budgetPaidFilterLabel())
        assertEquals("À payer", budgetUnpaidFilterLabel())
        assertEquals("Marquer payée", budgetMarkPaidActionLabel())
        assertEquals("Dépense payée", budgetItemPaidContentDescription())
    }

    @Test
    fun budgetCategoryCopyHidesEnumNames() {
        assertEquals("Transport", budgetCategoryLabel(BudgetCategory.TRANSPORT))
        assertEquals("Logement", budgetCategoryLabel(BudgetCategory.ACCOMMODATION))
        assertEquals("Repas", budgetCategoryLabel(BudgetCategory.MEALS))
        assertEquals("Activités", budgetCategoryLabel(BudgetCategory.ACTIVITIES))
        assertEquals("Équipement", budgetCategoryLabel(BudgetCategory.EQUIPMENT))
        assertEquals("Autre", budgetCategoryLabel(BudgetCategory.OTHER))

        BudgetCategory.values().forEach { category ->
            assertFalse(
                budgetCategoryLabel(category).contains(category.name),
                "Visible budget category should not leak enum name ${category.name}"
            )
        }
    }

    @Test
    fun budgetPaymentCopyDoesNotExposeParticipantIdentifiers() {
        assertEquals("Partagée par 1 participant", budgetSharedByLabel(1))
        assertEquals("Partagée par 4 participants", budgetSharedByLabel(4))
        assertEquals("Payée par vous", budgetPaidByLabel("user-1", "user-1"))

        val otherParticipantLabel = budgetPaidByLabel("secret@example.com", "user-1")

        assertEquals("Payée par un participant", otherParticipantLabel)
        assertFalse(otherParticipantLabel.contains("secret@example.com", ignoreCase = true))
        assertFalse(otherParticipantLabel.contains("user-1", ignoreCase = true))
    }

    @Test
    fun budgetDialogCopyExplainsValidation() {
        assertEquals("Ajouter une dépense", budgetItemDialogTitle(isNewItem = true))
        assertEquals("Modifier la dépense", budgetItemDialogTitle(isNewItem = false))
        assertEquals("Le nom de la dépense est requis.", budgetItemMissingNameMessage())
        assertEquals("Le coût doit être un nombre positif.", budgetItemInvalidCostMessage())
        assertEquals("Ajouter", budgetItemDialogConfirmLabel(isNewItem = true))
        assertEquals("Modifier", budgetItemDialogConfirmLabel(isNewItem = false))
        assertTrue(budgetDeleteItemMessage("Train").contains("Train"))
    }
}
