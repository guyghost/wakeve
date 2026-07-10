package com.guyghost.wakeve.productlanguage

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class AndroidProductLanguageBatch1Test {
    @Test
    fun batch1UsesResourcesForVisibleCopyAndSemantics() {
        val findings = batch1Paths.flatMap { path ->
            val file = projectRoot.resolve(path)
            val source = file.readText()
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
        val source = projectRoot.resolve(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveBottomBar.kt",
        ).readText()

        assertTrue(source.contains("Screen.Inbox"))
        assertTrue(source.contains("R.string.tab_ideas"))
    }

    private fun String.withoutLineComments(): String = lineSequence()
        .joinToString("\n") { line -> line.substringBefore("//").padEnd(line.length) }

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
    }
}
