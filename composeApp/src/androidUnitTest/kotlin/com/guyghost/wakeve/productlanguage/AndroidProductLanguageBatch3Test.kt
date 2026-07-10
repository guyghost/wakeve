package com.guyghost.wakeve.productlanguage

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.w3c.dom.Element

class AndroidProductLanguageBatch3Test {
    @Test
    fun batch3UsesResourcesForDirectAndIndirectVisibleCopy() {
        val findings = paths.flatMap { path ->
            val source = root.resolve(path).readText().withoutComments()
            direct.flatMap { (kind, regex) -> regex.findAll(source).map { "$path:${source.lineAt(it.range.first)}:$kind" } } +
                literal.findAll(source).mapNotNull { match ->
                    val value = match.groupValues[1]
                    val line = source.lineTextAt(match.range.first)
                    if (technical(value, line)) null else "$path:${source.lineAt(match.range.first)}:indirect '$value'"
                }
        }
        assertTrue(findings.isEmpty(), findings.joinToString("\n"))
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
        prefixes.any(key::startsWith) || key == "currency_amount"
    }
    private fun catalog(directory: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(root.resolve("composeApp/src/androidMain/res/$directory/strings.xml"))
        return (0 until doc.getElementsByTagName("string").length).associate { i ->
            val e = doc.getElementsByTagName("string").item(i) as Element
            e.getAttribute("name") to e.textContent.trim()
        }
    }
    private fun placeholders(value: String) = Regex("""%\d+\$[a-z]""").findAll(value).map { it.value }.toList()
    private fun String.withoutComments() = replace(Regex("""(?s)/\*.*?\*/""")) { " ".repeat(it.value.length) }.lineSequence().joinToString("\n") { it.substringBefore("//").padEnd(it.length) }
    private fun String.lineAt(index: Int) = take(index).count { it == '\n' } + 1
    private fun String.lineTextAt(index: Int): String { val s = lastIndexOf('\n', index).let { if (it < 0) 0 else it + 1 }; val e = indexOf('\n', index).let { if (it < 0) length else it }; return substring(s, e) }
    private fun technical(value: String, line: String): Boolean = !value.any(Char::isLetter) || value.matches(Regex("""[a-z][a-z0-9_./?={}&:-]*""")) || line.contains("stringResource(") || line.contains("getString(") || line.contains("Regex(")

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
        val cognates = mapOf("de" to setOf("budget_overview_title"), "es" to setOf("budget_overview_title"), "it" to setOf("budget_overview_title"), "pt" to setOf("budget_overview_title"))
    }
}
