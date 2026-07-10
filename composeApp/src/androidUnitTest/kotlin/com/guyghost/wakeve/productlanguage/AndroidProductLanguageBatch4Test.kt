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
        assertEquals(reviewedMealKeys, expected, "Batch 4 must keep the complete reviewed key set")
        val english = catalog("values-en")
        listOf("values-en", "values-de", "values-es", "values-it", "values-pt").forEach { directory ->
            assertEquals(expected, keys(directory), "$directory Batch 4 keys")
            catalog(directory).filterKeys { it in expected }.forEach { (key, value) ->
                assertEquals(placeholders(english.getValue(key)), placeholders(value), "$directory:$key placeholders")
            }
        }
        listOf("de", "es", "it", "pt").forEach { locale ->
            val localized = catalog("values-$locale")
            expected.filterNot { it in narrowCognates }.forEach { key ->
                assertTrue(localized.getValue(key) != english.getValue(key), "$locale:$key copied English")
            }
            localized.filterKeys { it in expected }.forEach { (key, value) ->
                forbiddenEnglishLexicon.getValue(locale).forEach { word ->
                    assertTrue(!Regex("(?i)(?<![\\p{L}])${Regex.escape(word)}(?![\\p{L}])").containsMatchIn(value), "$locale:$key contains English token '$word': $value")
                }
                forbiddenMixedLanguageFragments.forEach { fragment ->
                    assertTrue(!value.contains(fragment, ignoreCase = true), "$locale:$key contains mixed-language fragment '$fragment': $value")
                }
            }
        }
    }

    @Test
    fun selectedStateIsLocalizedAndComposedWithoutBooleanFormattingInAllSixLocales() {
        localeDirectories.forEach { directory ->
            val strings = catalog(directory)
            assertTrue(strings.getValue("meal_selected_state").isNotBlank(), "$directory selected state")
            assertTrue(strings.getValue("meal_unselected_state").isNotBlank(), "$directory unselected state")
            selectionSemanticKeys.forEach { key ->
                val value = strings.getValue(key)
                assertEquals(listOf("%1\$s", "%2\$s"), placeholders(value), "$directory:$key composes target and localized state")
                assertTrue("%b" !in value, "$directory:$key must not render a raw boolean")
            }
        }
        val sources = listOf(dialogsPath, screenPath).associateWith { root.resolve(it).readText() }
        sources.values.forEach { source ->
            assertTrue(source.contains("R.string.meal_selected_state"), "meal semantics must resolve selected state")
            assertTrue(source.contains("R.string.meal_unselected_state"), "meal semantics must resolve unselected state")
        }
    }

    @Test
    fun customMealSemanticsSuppressDescendantTalkBackDuplication() {
        val dialogs = root.resolve(dialogsPath).readText()
        val screen = root.resolve(screenPath).readText()
        assertTrue(dialogs.contains("clearAndSetSemantics"), "dialog chips and controls must replace descendant semantics")
        assertTrue(!Regex("Modifier\\.semantics\\s*\\{\\s*contentDescription\\s*=\\s*(?:type|status|restriction)Description").containsMatchIn(dialogs), "dialog selection controls must not merge duplicate label semantics")
        listOf("constraintsDescription", "typeDescription", "statusDescription", "openDescription").forEach { description ->
            assertTrue(Regex("clearAndSetSemantics\\s*\\{[^}]*contentDescription\\s*=\\s*$description", RegexOption.DOT_MATCHES_ALL).containsMatchIn(screen), "$description must replace descendant semantics")
        }
        assertTrue(
            Regex("Modifier\\.semantics\\s*\\{[^}]*contentDescription\\s*=\\s*deleteDescription", RegexOption.DOT_MATCHES_ALL).containsMatchIn(screen),
            "deleteDescription belongs to an icon-only sibling and must retain IconButton click semantics"
        )
    }

    @Test
    fun technicalExamplesAreAllowedOnlyInTheirReviewedContext() {
        val kotlinOccurrences = listOf(dialogsPath, screenPath).flatMap { path ->
            root.resolve(path).readLines().mapIndexedNotNull { index, line -> if ("19:00" in line || "participant-123" in line) "$path:${index + 1}:$line" else null }
        }
        kotlinOccurrences.forEach { occurrence ->
            assertTrue(occurrence.endsWith("var time by remember { mutableStateOf(meal?.time ?: \"19:00\") }"), "context-free technical literal: $occurrence")
        }
        localeDirectories.forEach { directory ->
            val strings = catalog(directory)
            assertEquals("19:00", strings.getValue("meal_time_placeholder"), "$directory time example context")
            assertEquals("participant-123", strings.getValue("meal_participant_id_placeholder"), "$directory participant example context")
            strings.filterKeys { it != "meal_time_placeholder" }.forEach { (key, value) -> assertTrue("19:00" !in value, "$directory:$key context-free time") }
            strings.filterKeys { it != "meal_participant_id_placeholder" }.forEach { (key, value) -> assertTrue("participant-123" !in value, "$directory:$key context-free participant id") }
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
        val technical = setOf("")
        val localeDirectories = listOf("values", "values-en", "values-de", "values-es", "values-it", "values-pt")
        val selectionSemanticKeys = setOf("a11y_meal_type_selection", "a11y_meal_status_selection", "a11y_meal_restriction_selection", "a11y_meal_filter_type", "a11y_meal_filter_status")
        val narrowCognates = setOf(
            "meal_date_placeholder", "meal_time_placeholder", "meal_people_placeholder",
            "meal_actual_cost_placeholder", "meal_start_date_placeholder", "meal_end_date_placeholder",
            "meal_participant_id_placeholder", "meal_restriction_vegan", "meal_restriction_kosher",
            "meal_restriction_halal"
        )
        val commonForbiddenEnglishLexicon = setOf(
            "add", "actual", "additional", "barbecue", "choose", "close", "cost", "count", "date", "days",
            "delete", "details", "different", "dismiss", "edit", "estimated", "finish", "location", "meal",
            "menu", "name", "notes", "optional", "people", "person", "plan", "prepare", "preview", "remove",
            "required", "restriction", "save", "selected", "start", "status", "time", "total", "type", "without"
        )
        val forbiddenEnglishLexicon = mapOf(
            "de" to commonForbiddenEnglishLexicon - setOf("name", "menu", "optional", "person", "plan", "status"),
            "es" to commonForbiddenEnglishLexicon - setOf("menu", "plan", "total"),
            "it" to commonForbiddenEnglishLexicon - setOf("menu", "total"),
            "pt" to commonForbiddenEnglishLexicon - setOf("menu", "prepare", "total")
        )
        val forbiddenMixedLanguageFragments = setOf(
            "comida bearbeiten", "pasto bearbeiten", "refeição bearbeiten",
            "meal plan", "meal type", "meal name", "participant count"
        )
        val reviewedMealKeys = """
            meal_planning meal_add_title meal_edit_title meal_type_label meal_name_label meal_name_placeholder
            meal_date_label meal_date_placeholder meal_time_label meal_time_placeholder meal_location_label meal_location_placeholder
            meal_people_label meal_people_placeholder meal_estimated_cost_label meal_estimated_cost_placeholder meal_actual_cost_label meal_actual_cost_placeholder
            meal_currency_amount meal_status_label meal_notes_label meal_notes_placeholder meal_action_add meal_action_save meal_action_cancel meal_action_generate meal_action_close meal_action_delete
            meal_error_name_required meal_error_date_required meal_error_time_required meal_error_people_invalid meal_error_estimated_cost_invalid
            meal_generate_title meal_start_date_label meal_start_date_placeholder meal_end_date_label meal_end_date_placeholder meal_participant_count_label
            meal_cost_per_person_label meal_cost_per_person_placeholder meal_cost_per_person_value meal_types_to_generate meal_preview_title meal_preview_summary
            meal_error_start_date_required meal_error_end_date_required meal_error_participants_invalid meal_error_cost_invalid meal_error_type_required
            meal_restrictions_title meal_restrictions_empty meal_participant_value meal_add_restriction_action meal_delete_restriction_title meal_delete_restriction_message
            meal_add_restriction_title meal_participant_id_label meal_participant_id_placeholder meal_restriction_type_label meal_restriction_notes_placeholder meal_error_participant_required
            meal_restriction_vegetarian meal_restriction_vegan meal_restriction_gluten_free meal_restriction_lactose_intolerant meal_restriction_nut_allergy
            meal_restriction_shellfish_allergy meal_restriction_kosher meal_restriction_halal meal_restriction_diabetic meal_restriction_other
            meal_selected_state meal_unselected_state a11y_meal_type_selection a11y_meal_status_selection a11y_meal_restriction_selection a11y_meal_delete_restriction
            meal_screen_title meal_empty_title meal_empty_description meal_delete_title meal_delete_message meal_summary_title meal_total_label meal_estimated_total_label
            meal_completed_label meal_completed_ratio meal_type_breakfast meal_type_lunch meal_type_dinner meal_type_snack meal_type_aperitif
            meal_status_planned meal_status_assigned meal_status_in_progress meal_status_completed meal_status_cancelled a11y_meal_back a11y_meal_open_constraints
            a11y_meal_prepare_plan a11y_meal_add a11y_meal_filter_type a11y_meal_filter_status a11y_meal_open a11y_meal_delete
            meal_count meal_constraint_count meal_responsible_count meal_serving_count a11y_meal_comments
        """.trimIndent().split(Regex("\\s+")).toSet()
    }
}
