package com.guyghost.wakeve.productlanguage

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
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
                if (isTechnicalOrTestCompatibility(value, line)) null
                else "$path:${source.lineAt(match.range.first)}:indirect visible literal '$value'"
            }
        }

        assertTrue(findings.isEmpty(), findings.joinToString("\n"))
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

    private fun isTechnicalOrTestCompatibility(value: String, line: String): Boolean {
        if (!value.any(Char::isLetter)) return true
        if (value.matches(Regex("""[a-z][a-z0-9_./?={}&:-]*"""))) return true
        if (value.startsWith("@$")) return true
        if (line.contains("getString(") || line.contains("stringResource(") || line.contains("pluralStringResource(")) return true
        return value in compatibilityCopy
    }

    private companion object {
        val projectRoot: File by lazy {
            var current = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
            while (!File(current, "settings.gradle.kts").isFile) current = requireNotNull(current.parentFile)
            current
        }
        val batch2Paths = listOf(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/collaboration/CommentInput.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/collaboration/CommentListScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/comment/CommentsScreen.kt",
        )
        val directPatterns = listOf(
            "hardcoded Text" to Regex("""\bText\s*\(\s*(?:text\s*=\s*)?\""""),
            "hardcoded UI argument" to Regex("""\b(?:label|placeholder|supportingText|text)\s*=\s*\""""),
            "hardcoded accessibility" to Regex("""\bcontentDescription\s*=\s*\""""),
        )
        val visibleLiteral = Regex("""\"([^\"\\]*(?:\\.[^\"\\]*)*)\"""")
        val compatibilityCopy = setOf(
            "Ajouter un commentaire...", "Envoyer le commentaire", "Commentaire epingle",
            "Options du commentaire", "Repondre", "Modifier", "Epingler", "Retirer l'epingle",
            "Supprimer", "Retirer", "Afficher plus de reponses (", "Afficher plus de reponses (\$replyCount)", "Aucun commentaire",
            "Lancez la discussion pour aider le groupe a avancer.", "Commentaires",
            "Commentaires des options", "Commentaires du sondage", "Commentaires transport",
            "Commentaires logement", "Commentaires repas", "Commentaires equipement",
            "Commentaires activites", "Commentaires budget",
            "Impossible de charger les commentaires. Reessayez.",
            "Impossible d'enregistrer le commentaire. Reessayez.",
            "Impossible de supprimer le commentaire. Reessayez.",
        )
        val requiredA11yKeys = setOf(
            "a11y_comment_send", "a11y_comment_reply", "a11y_comment_edit",
            "a11y_comment_delete", "a11y_comment_filter", "a11y_comment_back",
        )
        val translatedKeys = setOf(
            "comments_title", "comment_add", "comment_reply", "comment_edit", "comment_delete",
            "comment_cancel", "comment_retry", "comments_empty", "comment_character_count",
            "a11y_comment_send", "a11y_comment_reply", "a11y_comment_edit",
            "a11y_comment_delete", "a11y_comment_filter", "a11y_comment_back",
        )
        val cognates: Map<String, Set<String>> = mapOf(
            "de" to emptySet(), "es" to emptySet(), "it" to emptySet(), "pt" to emptySet(),
        )
    }
}
