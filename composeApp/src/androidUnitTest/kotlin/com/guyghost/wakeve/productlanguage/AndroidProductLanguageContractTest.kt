package com.guyghost.wakeve.productlanguage

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidProductLanguageContractTest {
    @Test
    fun navigationUsesCanonicalResourcesWithoutRenamingRoutes() {
        val bottomBar = projectFile(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/navigation/WakeveBottomBar.kt",
        ).readText()

        assertTrue(bottomBar.contains("R.string.tab_ideas"))
        assertTrue(bottomBar.contains("Screen.Inbox"))
        assertFalse(bottomBar.contains("label = \"Explorer\""))
    }

    @Test
    fun composeTextDoesNotContainHardcodedProductLanguage() {
        val findings = productionSources().flatMap { ProductLanguageSourceScanner.findings(projectRoot, it) }

        assertTrue(findings.isEmpty(), findings.joinToString("\n"))
    }

    @Test
    fun namedComposeArgumentsDoNotContainHardcodedProductLanguage() {
        val findings = productionSources().flatMap { ProductLanguageSourceScanner.findings(projectRoot, it) }

        assertTrue(findings.isEmpty(), findings.joinToString("\n"))
    }

    @Test
    fun ambiguousActionsHaveTargetAndStateKeys() {
        val findings = productionSources().flatMap { ProductLanguageSourceScanner.findings(projectRoot, it) }

        assertTrue(findings.isEmpty(), findings.joinToString("\n"))
    }

    @Test
    fun exhaustiveIndirectScannerCoversEveryInventoryOwnedAndroidPath() {
        val scanned = productionSources().map { it.relativeTo(projectRoot).invariantSeparatorsPath }.toSet()
        val expected = inventoryAndroidKotlinPaths().filterNot(previewAllowlist::contains).toSet()
        assertTrue(scanned == expected, "scanner inventory drift: missing=${expected - scanned}, extra=${scanned - expected}")
        val findings = productionSources().flatMap { ProductLanguageSourceScanner.findings(projectRoot, it) }
        assertTrue(findings.isEmpty(), findings.joinToString("\n"))
    }

    private fun File.findingsFor(pattern: Regex, diagnostic: String): List<String> {
        val relativePath = relativeTo(projectRoot).invariantSeparatorsPath
        val source = readText()
        val scanSource = source.lineSequence()
            .joinToString("\n") { line -> line.substringBefore("//").padEnd(line.length) }
        return pattern.findAll(scanSource).map { match ->
            val lineNumber = scanSource.take(match.range.first).count { it == '\n' } + 1
            val sourceLine = source.lineSequence().drop(lineNumber - 1).first().trim()
            "$relativePath:$lineNumber: $diagnostic: $sourceLine"
        }
            .toList()
    }

    private fun productionSources(): List<File> =
        inventoryAndroidKotlinPaths()
            .filterNot(previewAllowlist::contains)
            .map(::projectFile)
            .filter(File::isFile)

    private fun inventoryAndroidKotlinPaths(): List<String> {
        val inventory = projectFile("models/product-language.inventory.json").readText()
        return inventoryPath.findAll(inventory)
            .map { it.groupValues[1] }
            .filter { it.startsWith("composeApp/src/androidMain/") && it.endsWith(".kt") }
            .toList()
    }

    private fun projectFile(path: String): File = projectRoot.resolve(path)

    private companion object {
        val projectRoot: File by lazy {
            var current = File(requireNotNull(System.getProperty("user.dir"))).absoluteFile
            while (!File(current, "settings.gradle.kts").isFile) {
                current = requireNotNull(current.parentFile) { "Could not locate Wakeve project root" }
            }
            current
        }

        val previewAllowlist = setOf(
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/preview/factories/InboxItemFactory.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/components/WakeveAsyncImagePreview.kt",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve/ui/inbox/InboxScreenPreview.kt",
        )

        val inventoryPath = Regex("\\\"path\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
        val hardcodedTextCall = Regex("""\bText\s*\(\s*(?:text\s*=\s*)?""" + "\"")
        val hardcodedNamedArgument = Regex(
            """\b(?:label|placeholder|supportingText|headlineContent|overlineContent|text)\s*=\s*""" + "\"",
        )
        val hardcodedSemantics = Regex("""\bcontentDescription\s*=\s*""" + "\"")
    }
}
