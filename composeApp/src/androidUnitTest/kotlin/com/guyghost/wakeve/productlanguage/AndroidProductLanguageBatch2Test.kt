package com.guyghost.wakeve.productlanguage

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.w3c.dom.Element

class AndroidProductLanguageBatch2Test {
    @Test
    fun batch2UsesResourcesForDirectAndIndirectVisibleCopy() {
        val findings = batch2Paths.flatMap { path ->
            val source = projectRoot.resolve(path).readText().withoutComments()
            directPatterns.flatMap { (kind, pattern) ->
                pattern.findAll(source).map { match -> "$path:${source.lineAt(match.range.first)}:$kind" }
            } + visibleLiteral.findAll(source).mapNotNull { match ->
                val value = match.groupValues[1]
                val line = source.lineTextAt(match.range.first)
                if (isTechnical(value, line)) null
                else "$path:${source.lineAt(match.range.first)}:indirect visible literal '$value'"
            }
        }

        assertTrue(findings.isEmpty(), findings.joinToString("\n"))
    }

    @Test
    fun batch2HasNoLocalizedCompatibilityApisAndConsumesReplyTargetSemantics() {
        val sources = batch2Paths.associateWith { projectRoot.resolve(it).readText() }
        val combined = sources.values.joinToString("\n")
        legacyHelperNames.forEach { helper ->
            assertFalse(combined.contains("fun $helper("), "Production helper remains: $helper")
        }
        assertFalse(projectRoot.resolve(batch2TestPath).readText().contains("compatibility" + "Copy"))

        val commentItem = sources.getValue(commentItemPath)
        assertEquals(
            2,
            Regex("""stringResource\(R\.string\.a11y_comment_reply,\s*comment\.authorName\)""")
                .findAll(commentItem).count(),
            "Both reply actions must consume author-targeted semantics",
        )
    }

    @Test
    fun batch2DefinesPluralFormatsAndActionTargetStateAccessibility() {
        val defaultSource = catalogSource("values")
        assertTrue(defaultSource.contains("<plurals name=\"comment_reply_count\">"))
        assertTrue(defaultSource.contains("<string name=\"comment_character_count\">%1\$d/%2\$d"))
        requiredA11yKeys.forEach { key ->
            assertTrue(defaultSource.contains("name=\"$key\""), "Missing $key")
        }
        assertTrue(defaultSource.contains("%1\$s"), "Accessibility formats must name their target")
    }

    @Test
    fun batch2TranslationsAreNaturalAndKeepPlaceholderParity() {
        val english = catalog("values-en")
        val findings = listOf("de", "es", "it", "pt").flatMap { locale ->
            val localized = catalog("values-$locale")
            translatedKeys.mapNotNull { key ->
                val en = english[key] ?: return@mapNotNull "en:$key missing"
                val value = localized[key] ?: return@mapNotNull "$locale:$key missing"
                when {
                    placeholders(value) != placeholders(en) -> "$locale:$key placeholder mismatch"
                    value == en && key !in cognates.getValue(locale) -> "$locale:$key copied English value '$en'"
                    else -> null
                }
            }
        }
        assertTrue(findings.isEmpty(), findings.joinToString("\n"))
    }

    @Test
    fun replyPluralKeepsQuantitiesAndPlaceholderSignaturesAcrossCatalogs() {
        val expected = pluralQuantities("values-en", "comment_reply_count")
        assertEquals(setOf("one", "other"), expected.keys)
        listOf("values", "values-de", "values-es", "values-it", "values-pt").forEach { directory ->
            val actual = pluralQuantities(directory, "comment_reply_count")
            assertEquals(expected.keys, actual.keys, "$directory quantities")
            expected.forEach { (quantity, value) ->
                assertEquals(placeholders(value), placeholders(actual.getValue(quantity)), "$directory:$quantity")
            }
        }
    }

    private fun catalog(directory: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            projectRoot.resolve("composeApp/src/androidMain/res/$directory/strings.xml"),
        )
        val strings = (0 until document.getElementsByTagName("string").length).associate { index ->
            val element = document.getElementsByTagName("string").item(index) as Element
            element.getAttribute("name") to element.textContent.trim()
        }
        val plurals = (0 until document.getElementsByTagName("plurals").length).associate { index ->
            val element = document.getElementsByTagName("plurals").item(index) as Element
            element.getAttribute("name") to element.textContent.trim()
        }
        return strings + plurals
    }

    private fun catalogSource(directory: String) =
        projectRoot.resolve("composeApp/src/androidMain/res/$directory/strings.xml").readText()

    private fun placeholders(value: String) = Regex("""%\d+\$[a-z]""").findAll(value).map { it.value }.toList()
    private fun String.withoutComments() = replace(Regex("""(?s)/\*.*?\*/""")) { " ".repeat(it.value.length) }
        .lineSequence().joinToString("\n") { it.substringBefore("//").padEnd(it.length) }
    private fun String.lineAt(index: Int) = take(index).count { it == '\n' } + 1
    private fun String.lineTextAt(index: Int): String {
        val start = lastIndexOf('\n', index).let { if (it < 0) 0 else it + 1 }
        val end = indexOf('\n', index).let { if (it < 0) length else it }
        return substring(start, end)
    }

    private fun pluralQuantities(directory: String, key: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            projectRoot.resolve("composeApp/src/androidMain/res/$directory/strings.xml"),
        )
        val plurals = document.getElementsByTagName("plurals")
        val element = (0 until plurals.length).map { plurals.item(it) as Element }
            .single { it.getAttribute("name") == key }
        return (0 until element.getElementsByTagName("item").length).associate { index ->
            val item = element.getElementsByTagName("item").item(index) as Element
            item.getAttribute("quantity") to item.textContent.trim()
        }
    }

    private fun isTechnical(value: String, line: String): Boolean {
        if (!value.any(Char::isLetter)) return true
        if (value.matches(Regex("""[a-z][a-z0-9_./?={}&:-]*"""))) return true
        if (value.startsWith("@$")) return true
        if (line.contains("getString(") || line.contains("stringResource(") || line.contains("pluralStringResource(")) return true
        return false
    }

    private companion object {
        val projectRoot: File by lazy {
            var current = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
            while (!File(current, "settings.gradle.kts").isFile) current = requireNotNull(current.parentFile)
            current
        }
        val batch2Paths = listOf(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/collaboration/CommentInput.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/collaboration/CommentItem.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/collaboration/CommentListScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/comment/CommentsScreen.kt",
        )
        val directPatterns = listOf(
            "hardcoded Text" to Regex("""\bText\s*\(\s*(?:text\s*=\s*)?\""""),
            "hardcoded UI argument" to Regex("""\b(?:label|placeholder|supportingText|text)\s*=\s*\""""),
            "hardcoded accessibility" to Regex("""\bcontentDescription\s*=\s*\""""),
        )
        val visibleLiteral = Regex("""\"([^\"\\]*(?:\\.[^\"\\]*)*)\"""")
        const val batch2TestPath = "composeApp/src/androidUnitTest/kotlin/com/guyghost/wakeve/productlanguage/AndroidProductLanguageBatch2Test.kt"
        const val commentItemPath = "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/collaboration/CommentItem.kt"
        val legacyHelperNames = setOf(
            "commentInputPlaceholder", "commentSendContentDescription", "getSectionTitle",
            "loadMoreRepliesLabel", "emptyCommentsTitle", "emptyCommentsSubtitle",
            "commentPinnedContentDescription", "commentOptionsContentDescription",
            "commentReplyActionLabel", "commentEditActionLabel", "commentPinActionLabel",
            "commentDeleteActionLabel", "commentLoadFailureMessage", "commentSubmitFailureMessage",
            "commentDeleteFailureMessage",
        )
        val requiredA11yKeys = setOf(
            "a11y_comment_send", "a11y_comment_reply", "a11y_comment_edit",
            "a11y_comment_delete", "a11y_comment_filter", "a11y_comment_back",
        )
        val translatedKeys = setOf(
            "comments_title", "comments_title_for_section", "comments_section_options", "comments_section_poll",
            "comments_section_transport", "comments_section_accommodation", "comments_section_meal",
            "comments_section_equipment", "comments_section_activity", "comments_section_budget",
            "comment_section_general", "comment_section_options", "comment_section_poll", "comment_section_transport",
            "comment_section_accommodation", "comment_section_meal", "comment_section_equipment",
            "comment_section_activity", "comment_section_budget", "comment_input_placeholder", "mention_prompt",
            "comment_add", "comment_reply", "comment_edit", "comment_post", "comment_delete", "comment_cancel",
            "comment_close", "comment_retry", "comment_edited", "comments_empty", "comments_empty_body",
            "comments_empty_event", "comments_empty_section", "comments_empty_call_to_action", "comment_error_title",
            "comment_load_error", "comment_filter_title", "comment_filter_all", "comment_edit_title",
            "comment_reply_to", "comment_field_label", "comment_field_placeholder", "comment_character_count",
            "comment_delete_title", "comment_delete_body", "comment_time_now", "comment_time_minutes",
            "comment_time_hours", "comment_time_days", "comment_time_recent", "a11y_comment_send",
            "a11y_comment_reply", "a11y_comment_edit", "a11y_comment_delete", "a11y_comment_filter",
            "a11y_comment_back", "a11y_comment_add", "comment_submit_error", "comment_delete_error",
            "comment_reply_count", "comment_pin", "comment_unpin", "comment_remove", "comment_time_weeks",
            "a11y_comment_pinned", "a11y_comment_options",
        )
        val cognates: Map<String, Set<String>> = mapOf(
            "de" to setOf("comment_section_budget"),
            "es" to setOf("comment_section_general", "comment_error_title"),
            "it" to setOf("comment_section_budget"),
            "pt" to emptySet(),
        )
    }
}
