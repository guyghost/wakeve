package com.guyghost.wakeve.ui.budget

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BudgetOverviewCopyTest {
    private val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
        .first { File(it, "settings.gradle.kts").isFile }
    private val source = root.resolve("composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetOverviewScreen.kt").readText()

    @Test
    fun overviewProjectsAmountsRatiosStatusesAndSettlementCopyFromResources() {
        listOf(
            "R.string.currency_amount",
            "R.string.budget_paid_ratio",
            "R.string.budget_usage",
            "R.plurals.budget_per_person_title",
            "R.plurals.budget_settlement_count",
            "R.plurals.budget_settlement_body",
            "R.string.budget_settlement_line",
            "R.string.budget_pending_offline_sync"
        ).forEach { assertTrue(source.contains(it), "Missing resource projection $it") }
    }

    @Test
    fun categoryLabelsDoNotExposeEnumNames() {
        assertTrue(source.contains("budgetCategoryLabel(category.category)"))
        assertFalse(source.contains("category.category.name.lowercase()"))
    }
}
