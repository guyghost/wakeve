package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class BudgetRoutesErrorMessageTest {
    @Test
    fun budgetFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            budgetReadFailureMessage(),
            budgetBaselineSaveFailureMessage(),
            budgetItemListFailureMessage(),
            budgetItemCreateFailureMessage(),
            budgetExpenseInvalidFailureMessage(),
            budgetExpenseCreateFailureMessage(),
            budgetItemDetailFailureMessage(),
            budgetItemUpdateFailureMessage(),
            budgetItemDeleteFailureMessage(),
            budgetSummaryFailureMessage(),
            budgetSettlementsFailureMessage(),
            participantBudgetInfoFailureMessage(),
            budgetStatisticsFailureMessage()
        )

        assertEquals(messages.size, messages.distinct().size)
        messages.forEach { message ->
            assertFalse(message.isBlank())
            assertDoesNotExposeSensitiveDetails(message)
        }
    }

    private fun assertDoesNotExposeSensitiveDetails(message: String) {
        listOf(
            "secret@example.com",
            "SECRET",
            "SQL constraint",
            "http://internal.local",
            "token=",
            "budget-",
            "expense-",
            "participant-"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
