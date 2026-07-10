package com.guyghost.wakeve.productlanguage

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.w3c.dom.Element

class AndroidProductLanguageBatch3Test {
    @Test
    fun batch3UsesResourcesForDirectAndIndirectVisibleCopy() {
        val findings = paths.flatMap { path ->
            sourceFindings(path, root.resolve(path).readText())
        }
        assertTrue(findings.isEmpty(), findings.joinToString("\n"))
    }

    @Test
    fun indirectScannerRejectsVisibleOneWordHelpersEnumsAndDefaults() {
        val fixtures = mapOf(
            "helper return" to "fun editLabel(): String { return \"Edit\" }",
            "helper argument" to "ActionChip(label = \"Edit\")",
            "enum label" to "enum class Action(val label: String) { EDIT(\"Edit\") }",
            "visible default" to "fun ActionButton(label: String = \"Edit\") = Unit"
        )

        fixtures.forEach { (name, source) ->
            assertTrue(sourceFindings("fixture/$name.kt", source).isNotEmpty(), "$name must be reported")
        }
    }

    @Test
    fun indirectScannerAllowsOnlyExactReviewedTechnicalIdentifiersAndUrls() {
        val reviewed = """
            val provider = "TRICOUNT"
            val status = "ACTIVE"
            val url = "https://tricount.com/group/${'$'}eventId"
        """.trimIndent()
        assertTrue(sourceFindings("fixture/reviewed.kt", reviewed).isEmpty())

        val unreviewed = """
            val label = "Edit"
            val provider = "SOME_NEW_PROVIDER"
            val url = "https://example.com/unreviewed"
        """.trimIndent()
        val findings = sourceFindings("fixture/unreviewed.kt", unreviewed)
        assertTrue(findings.any { it.contains("'Edit'") }, findings.joinToString("\n"))
        assertTrue(findings.any { it.contains("'SOME_NEW_PROVIDER'") }, findings.joinToString("\n"))
        assertTrue(findings.any { it.contains("'https://example.com/unreviewed'") }, findings.joinToString("\n"))
    }

    @Test
    fun batch3DefinesFormattedAmountsRatiosAndPaidStateSemantics() {
        val values = catalog("values")
        setOf("currency_amount", "budget_amount_ratio", "activity_registration_ratio", "equipment_progress_ratio").forEach {
            assertTrue(it in values, "Missing $it")
            assertTrue(placeholders(values.getValue(it)).isNotEmpty(), "$it must be formatted")
        }
        setOf("a11y_budget_mark_paid", "a11y_budget_paid_state", "a11y_budget_unpaid_state").forEach {
            assertTrue(it in values, "Missing $it")
            assertTrue(values.getValue(it).contains("%1\$s"), "$it must name its target")
        }
    }

    @Test
    fun budgetDetailConsumesTargetAwarePaidUnpaidEditAndDeleteSemanticsOnce() {
        val source = root.resolve(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetDetailScreen.kt"
        ).readText().withoutComments()

        listOf(
            Regex("""stringResource\(R\.string\.a11y_budget_paid_state,\s*item\.name\)""") to 1,
            Regex("""stringResource\(R\.string\.a11y_budget_unpaid_state,\s*item\.name\)""") to 1,
            Regex("""stringResource\(R\.string\.a11y_budget_edit,\s*item\.name\)""") to 1,
            Regex("""stringResource\(R\.string\.a11y_budget_delete,\s*item\.name\)""") to 1,
            Regex("""Modifier\.clearAndSetSemantics\s*\{\s*contentDescription\s*=\s*markPaidDescription\s*onClick""") to 1,
            Regex("""Modifier\.clearAndSetSemantics\s*\{\s*contentDescription\s*=\s*editDescription\s*onClick""") to 1,
            Regex("""Modifier\.clearAndSetSemantics\s*\{\s*contentDescription\s*=\s*deleteDescription\s*onClick""") to 1,
        ).forEach { (expected, count) ->
            assertEquals(count, expected.findAll(source).count(), "Unexpected BudgetDetail consumption count: $expected")
        }

        assertFalse(
            Regex("""semantics\s*\{\s*contentDescription\s*=\s*(?:markPaidDescription|editDescription|deleteDescription)\s*}""").containsMatchIn(source),
            "Budget actions must not append a second description to their visible label"
        )
    }

    @Test
    fun everyDerivedBatch3KeyHasNaturalSixLocaleParity() {
        val expected = keys("values")
        val english = catalog("values-en")
        listOf("values-en", "values-de", "values-es", "values-it", "values-pt").forEach { directory ->
            assertEquals(expected, keys(directory), "$directory Batch 3 keys")
            catalog(directory).filterKeys { it in expected }.forEach { (key, value) ->
                assertEquals(placeholders(english.getValue(key)), placeholders(value), "$directory:$key placeholders")
            }
        }
        listOf("de", "es", "it", "pt").forEach { locale ->
            val localized = catalog("values-$locale")
            expected.filterNot { it in cognates.getValue(locale) }.forEach { key ->
                assertTrue(localized.getValue(key) != english.getValue(key), "$locale:$key copied English")
            }
        }
    }

    private fun keys(directory: String) = catalog(directory).keys.filterTo(mutableSetOf()) { key ->
        (prefixes.any(key::startsWith) && key !in legacyKeys) || key == "currency_amount"
    }
    private fun catalog(directory: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(root.resolve("composeApp/src/androidMain/res/$directory/strings.xml"))
        return listOf("string", "plurals").flatMap { tag ->
            val nodes = doc.getElementsByTagName(tag)
            (0 until nodes.length).map { i ->
                val e = nodes.item(i) as Element
                e.getAttribute("name") to e.textContent.trim()
            }
        }.toMap()
    }
    private fun placeholders(value: String) = Regex("""%\d+\$(?:\.\d+)?[a-z]""").findAll(value).map { it.value }.toList()
    private fun String.withoutComments() = replace(Regex("""(?s)/\*.*?\*/""")) { " ".repeat(it.value.length) }
        .lineSequence()
        .joinToString("\n") { line -> if (line.trimStart().startsWith("//")) " ".repeat(line.length) else line }
    private fun String.lineAt(index: Int) = take(index).count { it == '\n' } + 1
    private fun sourceFindings(path: String, rawSource: String): List<String> {
        val source = rawSource.withoutComments()
        return direct.flatMap { (kind, regex) ->
            regex.findAll(source).map { "$path:${source.lineAt(it.range.first)}:$kind" }.toList()
        } + literal.findAll(source).mapNotNull { match ->
            val value = match.groupValues[1]
            if (technical(value)) null else "$path:${source.lineAt(match.range.first)}:indirect '$value'"
        }
    }
    private fun technical(value: String): Boolean =
        !value.any(Char::isLetter) || value in reviewedTechnicalLiterals

    private companion object {
        val root: File by lazy { var f = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile; while (!File(f, "settings.gradle.kts").isFile) f = requireNotNull(f.parentFile); f }
        val paths = listOf(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/activity/ActivityDialogs.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/activity/ActivityPlanningScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetDetailScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/budget/BudgetOverviewScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/equipment/EquipmentChecklistScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/equipment/EquipmentDialogs.kt",
        )
        val direct = listOf("Text" to Regex("""\bText\s*\(\s*(?:text\s*=\s*)?\""""), "argument" to Regex("""\b(?:label|placeholder|supportingText|text)\s*=\s*\""""), "a11y" to Regex("""\bcontentDescription\s*=\s*\""""))
        val literal = Regex("""\"([^\"\\]*(?:\\.[^\"\\]*)*)\"""")
        val prefixes = listOf("budget_", "activity_", "equipment_", "a11y_budget_", "a11y_activity_", "a11y_equipment_")
        val legacyKeys = setOf("budget_per_person", "budget_hint", "budget_overview", "budget_label")
        val reviewedTechnicalLiterals = setOf(
            "TRICOUNT", "ACTIVE", "LINKED", "verified",
            "camping", "beach", "ski", "hiking", "picnic", "indoor",
            "%.2f", "99:99", "-", " ",
            "tricount-\$eventId", "https://tricount.com/group/\$eventId",
            "\\\\d{4}-\\\\d{2}-\\\\d{2}", "\\\\d{2}:\\\\d{2}",
            "^user[-_ ]?(.+)\$", "[-_]+"
        )
        val cognates = mapOf(
            "de" to setOf("budget_overview_title", "budget_status_filter", "budget_item_name", "budget_tricount_title", "activity_time_placeholder", "equipment_quantity_value", "equipment_status_label"),
            "es" to setOf("budget_overview_title", "budget_total_label", "budget_tricount_title", "activity_time_placeholder", "activity_time_duration", "activity_date_display", "equipment_quantity_value"),
            "it" to setOf("budget_overview_title", "budget_tricount_title", "activity_time_placeholder", "activity_time_duration", "activity_date_display", "equipment_quantity_value"),
            "pt" to setOf("budget_overview_title", "budget_total_label", "budget_tricount_title", "activity_time_placeholder", "activity_time_duration", "activity_date_display", "equipment_quantity_value")
        )
    }
}
