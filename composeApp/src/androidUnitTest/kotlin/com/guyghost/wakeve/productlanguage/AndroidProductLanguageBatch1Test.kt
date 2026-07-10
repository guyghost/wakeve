package com.guyghost.wakeve.productlanguage

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidProductLanguageBatch1Test {
    @Test
    fun batch1TranslationsAreNotEnglishCopies() {
        val english = catalog("values-en")
        val findings = listOf("de", "es", "it", "pt").flatMap { locale ->
            val localized = catalog("values-$locale")
            batch1TranslatedKeys.mapNotNull { key ->
                val englishValue = english.getValue(key)
                val localizedValue = localized.getValue(key)
                if (localizedValue == englishValue && key !in reviewedEnglishCognates.getValue(locale)) {
                    "$locale:$key copied English value '$englishValue'"
                } else null
            }
        }

        assertTrue(findings.isEmpty(), findings.joinToString("\n"))
    }

    @Test
    fun batch1UsesResourcesForVisibleCopyAndSemantics() {
        val findings = batch1Paths.flatMap { path ->
            val file = projectRoot.resolve(path)
            val source = file.readText().withoutBatch5EventPhotos()
            patterns.flatMap { (diagnostic, pattern) ->
                pattern.findAll(source.withoutLineComments()).map { match ->
                    val line = source.take(match.range.first).count { it == '\n' } + 1
                    "$path:$line: $diagnostic"
                }
            }
        }

        assertTrue(findings.isEmpty(), findings.joinToString("\n"))
    }

    @Test
    fun navigationKeepsInboxRouteAndUsesCanonicalIdeasTab() {
        val bottomBar = projectRoot.resolve(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveBottomBar.kt",
        ).readText()
        val adaptiveNavigation = projectRoot.resolve(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveAdaptiveNavigationScaffold.kt",
        ).readText()
        val navHost = projectRoot.resolve(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt",
        ).readText()
        val screens = projectRoot.resolve(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/Screen.kt",
        ).readText()

        assertTrue(bottomBar.contains("Screen.Inbox"))
        assertTrue(bottomBar.contains("R.string.tab_ideas"))
        assertTrue(bottomBar.contains("R.string.notifications"))
        assertTrue(adaptiveNavigation.contains("Screen.Inbox"))
        assertTrue(adaptiveNavigation.contains("R.string.tab_ideas"))
        assertTrue(adaptiveNavigation.contains("R.string.notifications"))
        assertFalse(bottomBar.contains("Screen.Messages"))
        assertFalse(adaptiveNavigation.contains("Screen.Messages"))
        assertTrue(screens.contains("data object Comments : Screen(\"event/{eventId}/comments\")"))
        assertTrue(screens.contains("data object Notifications"))
        assertFalse(navHost.contains("Screen.Notifications.createRoute(eventId)"))
    }

    @Test
    fun batch1DoesNotHideVisibleCopyInIntermediateValues() {
        val findings = batch1Paths.flatMap { path ->
            val source = projectRoot.resolve(path).readText().withoutComments().withoutBatch5EventPhotos()
            visibleLiteral.findAll(source).mapNotNull { match ->
                val literal = match.groupValues[1]
                val lineStart = source.lastIndexOf('\n', match.range.first).let { if (it < 0) 0 else it + 1 }
                val lineEnd = source.indexOf('\n', match.range.last).let { if (it < 0) source.length else it }
                val lineText = source.substring(lineStart, lineEnd).trim()
                if (isAllowedTechnicalLiteral(literal, lineText)) null else {
                    val line = source.take(match.range.first).count { it == '\n' } + 1
                    "$path:$line: visible literal '$literal'"
                }
            }.toList()
        }

        assertTrue(findings.isEmpty(), findings.joinToString("\n"))
    }

    private fun String.withoutLineComments(): String = lineSequence()
        .joinToString("\n") { line -> line.substringBefore("//").padEnd(line.length) }

    private fun String.withoutComments(): String =
        replace(Regex("""(?s)/\*.*?\*/""")) { " ".repeat(it.value.length) }
            .withoutLineComments()

    private fun String.withoutBatch5EventPhotos(): String = replace(
        Regex("""(?s)@Composable\s+private fun EventPhotosFollowUpScreen\(.*?\n}\n\n@Composable"""),
        "@Composable",
    )

    private fun isAllowedTechnicalLiteral(literal: String, line: String): Boolean {
        if (!literal.any(Char::isLetter)) return true
        if (literal.startsWith("R.string.")) return true
        if (literal.matches(Regex("""[a-z][A-Za-z0-9_./?={}&:-]*"""))) return true
        if (literal.startsWith("$")) return true
        if (literal.startsWith("http") || literal.startsWith("event-")) return true
        if (line.contains("testTag(") || line.contains("navArgument(") || line.contains("getString(")) return true
        if (line.contains("error(")) return true
        if (line.contains("@Suppress(")) return true
        return literal in allowedTechnicalLiterals
    }

    private fun catalog(directory: String): Map<String, String> {
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
            projectRoot.resolve("composeApp/src/androidMain/res/$directory/strings.xml"),
        )
        return (0 until document.getElementsByTagName("string").length).associate { index ->
            val element = document.getElementsByTagName("string").item(index) as Element
            element.getAttribute("name") to element.textContent.trim()
        }
    }

    private companion object {
        val projectRoot: File by lazy {
            var current = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
            while (!File(current, "settings.gradle.kts").isFile) {
                current = requireNotNull(current.parentFile)
            }
            current
        }

        val batch1Paths = listOf(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/InboxScreenWrapper.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/LoginScreen.android.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/LoginScreen.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/AuthCallbacks.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/ScreenWrappers.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveAdaptiveNavigationScaffold.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveBottomBar.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveNavHost.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ProfileTabScreen.kt",
        )

        val patterns = listOf(
            "hardcoded Text" to Regex("""\bText\s*\(\s*(?:text\s*=\s*)?""" + "\""),
            "hardcoded named UI argument" to Regex(
                """\b(?:label|placeholder|supportingText|headlineContent|overlineContent|text)\s*=\s*""" + "\"",
            ),
            "hardcoded semantics" to Regex("""\bcontentDescription\s*=\s*""" + "\""),
        )

        val visibleLiteral = Regex("""\"([^\"\\]*(?:\\.[^\"\\]*)*)\"""")

        val allowedTechnicalLiterals = setOf(
            "Google",
            "Wakeve",
            "Contact permission denied by user",
        )

        val batch1TranslatedKeys = setOf(
            "a11y_selected", "a11y_unselected", "inbox_filter_all", "inbox_filter_focused",
            "inbox_filter_unread", "inbox_filter_event", "inbox_context_invitation",
            "inbox_context_poll", "inbox_context_vote_reminder", "inbox_context_confirmation",
            "inbox_context_participant", "inbox_context_vote", "inbox_context_comment",
            "inbox_context_reply", "inbox_context_budget", "inbox_context_activity",
            "inbox_context_accommodation", "inbox_context_notification", "relative_time_now",
            "relative_time_minutes", "relative_time_hours", "relative_time_days",
            "relative_time_weeks", "relative_time_months", "relative_time_years",
            "inbox_empty_all_title", "inbox_empty_all_body", "inbox_empty_focused_title",
            "inbox_empty_focused_body", "inbox_empty_unread_title", "inbox_empty_unread_body",
            "inbox_empty_event_title", "inbox_empty_event_body", "profile_version_unknown",
            "profile_version", "profile_guest_name", "profile_default_user_name",
            "profile_limited_features", "profile_dashboard_title", "profile_dashboard_subtitle",
            "profile_notifications_subtitle", "profile_settings_subtitle", "profile_logout_subtitle",
            "profile_create_account_subtitle", "event_not_found", "event_not_found_sentence",
            "event_default_title", "participant_added", "access_denied_meetings",
            "access_denied_payment_pot", "access_denied_tricount", "access_denied_budget",
            "access_denied_expenses", "profile_settings_title", "profile_logout_title",
            "profile_create_account_title",
        )

        val reviewedEnglishCognates = mapOf(
            "de" to setOf("inbox_filter_event", "inbox_context_budget", "event_default_title", "profile_version"),
            "es" to setOf("relative_time_minutes", "relative_time_hours", "relative_time_days", "profile_version"),
            "it" to setOf("inbox_context_budget", "relative_time_minutes", "relative_time_hours", "profile_dashboard_title", "profile_version"),
            "pt" to setOf("relative_time_minutes", "relative_time_hours", "relative_time_days", "profile_version"),
        )
    }
}
