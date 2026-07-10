package com.guyghost.wakeve.productlanguage

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.w3c.dom.Element

class AndroidProductLanguageBatch4Test {
    @Test
    fun mealDialogsUseResourcesForEveryVisibleLiteral() {
        assertTrue(findings(dialogsPath).isEmpty(), findings(dialogsPath).joinToString("\n"))
    }

    @Test
    fun mealPlanningScreenUsesResourcesForEveryVisibleLiteral() {
        assertTrue(findings(screenPath).isEmpty(), findings(screenPath).joinToString("\n"))
    }

    @Test
    fun mealDialogsDefineTargetAndStateAwareSemantics() {
        val source = root.resolve(dialogsPath).readText()
        listOf("a11y_meal_type_selection", "a11y_meal_status_selection", "a11y_meal_restriction_selection", "a11y_meal_delete_restriction").forEach {
            assertTrue(source.contains("R.string.$it"), "MealDialogs must consume $it")
        }
    }

    @Test
    fun mealPlanningDefinesActionTargetAndStateAwareSemantics() {
        val source = root.resolve(screenPath).readText()
        listOf(
            "a11y_meal_open_constraints",
            "a11y_meal_filter_type",
            "a11y_meal_filter_status",
            "a11y_meal_open",
            "a11y_meal_delete",
        ).forEach {
            assertTrue(source.contains("R.string.$it"), "MealPlanningScreen must consume $it")
        }
        listOf(
            "meal_count",
            "meal_constraint_count",
            "meal_responsible_count",
            "meal_serving_count",
            "a11y_meal_comments",
        ).forEach {
            assertTrue(source.contains("R.plurals.$it"), "MealPlanningScreen must consume plural $it")
        }
    }

    @Test
    fun everyDerivedMealKeyHasNaturalSixLocaleParity() {
        val expected = keys("values")
        val english = catalog("values-en")
        listOf("values-en", "values-de", "values-es", "values-it", "values-pt").forEach { directory ->
            assertEquals(expected, keys(directory), "$directory Batch 4 keys")
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

    private fun findings(path: String): List<String> {
        val source = root.resolve(path).readText().withoutComments()
        return direct.flatMap { (kind, regex) ->
            regex.findAll(source).map { "$path:${source.lineAt(it.range.first)}:$kind" }.toList()
        } + literal.findAll(source).mapNotNull { match ->
            val value = match.groupValues[1]
            if (!value.any(Char::isLetter) || value in technical) null
            else "$path:${source.lineAt(match.range.first)}:indirect '$value'"
        }
    }

    private fun keys(directory: String) = catalog(directory).keys.filterTo(mutableSetOf()) {
        it.startsWith("meal_") || it.startsWith("a11y_meal_")
    }
    private fun catalog(directory: String): Map<String, String> {
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(root.resolve("composeApp/src/androidMain/res/$directory/strings.xml"))
        return listOf("string", "plurals").flatMap { tag ->
            val nodes = doc.getElementsByTagName(tag)
            (0 until nodes.length).map { i ->
                val element = nodes.item(i) as Element
                element.getAttribute("name") to element.textContent.trim()
            }
        }.toMap()
    }
    private fun placeholders(value: String) = Regex("""%\d+\$(?:\.\d+)?[a-z]""").findAll(value).map { it.value }.toList()
    private fun String.withoutComments() = replace(Regex("""(?s)/\*.*?\*/""")) { " ".repeat(it.value.length) }
        .lineSequence().joinToString("\n") { if (it.trimStart().startsWith("//")) " ".repeat(it.length) else it }
    private fun String.lineAt(index: Int) = take(index).count { it == '\n' } + 1

    private companion object {
        val root: File by lazy { var file = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile; while (!File(file, "settings.gradle.kts").isFile) file = requireNotNull(file.parentFile); file }
        const val dialogsPath = "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/meal/MealDialogs.kt"
        const val screenPath = "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/meal/MealPlanningScreen.kt"
        val direct = listOf("Text" to Regex("""\bText\s*\(\s*(?:text\s*=\s*)?\""""), "argument" to Regex("""\b(?:label|placeholder|supportingText|text)\s*=\s*\""""), "a11y" to Regex("""\bcontentDescription\s*=\s*\""""))
        val literal = Regex("""\"([^\"\\]*(?:\\.[^\"\\]*)*)\"""")
        val technical = setOf("19:00", "", "participant-123")
        val exactFormatCognates = setOf(
            "meal_date_label", "meal_date_placeholder", "meal_time_label", "meal_time_placeholder",
            "meal_people_placeholder", "meal_actual_cost_placeholder", "meal_error_estimated_cost_invalid",
            "meal_start_date_label", "meal_start_date_placeholder", "meal_end_date_label",
            "meal_end_date_placeholder", "meal_participant_id_placeholder"
        )
        val cognates = mapOf(
            "de" to exactFormatCognates,
            "es" to exactFormatCognates,
            "it" to exactFormatCognates,
            "pt" to exactFormatCognates
        )
    }
}
