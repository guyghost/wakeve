package com.guyghost.wakeve.ui.budget

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BudgetDetailScreenErrorMessageTest {

    @Test
    fun budgetItemSaveFailureMessage_doesNotExposeRepositoryFailureDetails() {
        val sensitiveRepositoryFailure = "SQL constraint failed for budget-1 user secret@example.com token=SECRET"

        val result = budgetItemSaveFailureMessage()

        assertEquals("Impossible d'enregistrer cet item de budget. Reessayez.", result)
        assertFalse(result.contains(sensitiveRepositoryFailure))
        assertFalse(result.contains("secret@example.com", ignoreCase = true))
        assertFalse(result.contains("SECRET", ignoreCase = true))
        assertFalse(result.contains("SQL constraint", ignoreCase = true))
        assertFalse(result.contains("token=", ignoreCase = true))
    }
}
