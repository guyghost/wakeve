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

        val replyActions = sources.flatMap { (path, source) ->
            Regex("""onClick\s*=\s*\{\s*onReply\(""").findAll(source).map { match ->
                val start = (match.range.first - 500).coerceAtLeast(0)
                val end = (match.range.last + 350).coerceAtMost(source.length - 1)
                path to source.substring(start, end)
            }
        }
        assertEquals(3, replyActions.size, "Every rendered reply action must be discovered")
        replyActions.forEach { (path, action) ->
            assertTrue(action.contains("R.string.a11y_comment_reply"), "$path reply action lacks a11y resource")
            assertTrue(action.contains("comment.authorName"), "$path reply action lacks author target")
            assertTrue(action.contains("clearAndSetSemantics"), "$path reply action may duplicate TalkBack copy")
        }
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
        val translatedKeys = batch2Keys("values") - invariantKeys
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
    fun everyDerivedBatch2KeyExistsAcrossCatalogsAndInvariantsAreExact() {
        val expected = batch2Keys("values")
        listOf("values-en", "values-de", "values-es", "values-it", "values-pt").forEach { directory ->
            assertEquals(expected, batch2Keys(directory), "$directory Batch 2 key set")
            assertEquals("@%1\$s", catalog(directory).getValue("mention_user_label"), "$directory mention format")
        }
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

    private fun batch2Keys(directory: String): Set<String> = catalog(directory).keys.filterTo(mutableSetOf()) { key ->
        key.startsWith("comment_") || key.startsWith("comments_") ||
            key.startsWith("a11y_comment_") || key == "mention_prompt" || key == "mention_user_label"
    }

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
        val invariantKeys = setOf("mention_user_label")
        val cognates: Map<String, Set<String>> = mapOf(
            "de" to setOf("comment_section_budget"),
            "es" to setOf("comment_section_general", "comment_error_title"),
            "it" to setOf("comment_section_budget"),
            "pt" to emptySet(),
        )
    }
}
