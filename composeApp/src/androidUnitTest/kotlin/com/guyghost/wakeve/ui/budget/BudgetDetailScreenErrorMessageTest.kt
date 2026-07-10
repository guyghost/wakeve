package com.guyghost.wakeve.ui.budget

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BudgetDetailScreenErrorMessageTest {
    private val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile }
    private val source = root.resolve("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetDetailScreen.kt").readText()

    @Test
    fun budgetDetailUsesResourcesForErrorsCategoriesAndPaymentCopy() {
        listOf(
            "R.string.budget_item_save_error",
            "R.string.budget_category_transport",
            "R.string.budget_category_accommodation",
            "R.string.budget_category_meals",
            "R.string.budget_category_activities",
            "R.string.budget_category_equipment",
            "R.string.budget_category_other",
            "R.plurals.budget_shared_by"
        ).forEach { assertTrue(source.contains(it), "Missing resource projection $it") }
        assertFalse(source.contains("SQL constraint"))
        assertFalse(source.contains("secret@example.com"))
    }

    @Test
    fun paidActionNamesTargetAndState() {
        assertTrue(source.contains("R.string.a11y_budget_mark_paid"))
        assertTrue(source.contains("R.string.a11y_budget_unpaid_state"))
        assertTrue(source.contains("Modifier.semantics"))
    }
}
