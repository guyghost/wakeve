package com.guyghost.wakeve.ai

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText
import kotlin.test.Test
import kotlin.test.assertTrue

class DeterministicAiArchitectureGuardTest {
    @Test
    fun `deterministic domain and state machine code does not import AI provider SDKs`() {
        val findings = kotlinFilesUnder(
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/core",
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/budget",
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/meeting",
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/notification",
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/payment",
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/poll",
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/state",
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine",
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/suggestions",
            "shared/src/commonMain/kotlin/com/guyghost/wakeve/sync"
        ).providerFindings()

        assertTrue(
            findings.isEmpty(),
            "AI provider SDK references must stay out of deterministic domain/state-machine code:\n" +
                findings.joinToString("\n")
        )
    }

    @Test
    fun `Compose and SwiftUI views do not call AI providers directly`() {
        val findings = composeViewFiles()
            .plus(swiftFilesUnder("iosApp/src/Views"))
            .providerFindings()

        assertTrue(
            findings.isEmpty(),
            "UI views must dispatch through ViewModels or services instead of AI providers:\n" +
                findings.joinToString("\n")
        )
    }

    private fun List<Path>.providerFindings(): List<String> =
        flatMap { file ->
            val text = file.readText()
            providerPatterns
                .filter { it.containsMatchIn(text) }
                .map { pattern -> "${projectRoot.relativize(file)} contains ${pattern.pattern}" }
        }

    private fun kotlinFilesUnder(vararg roots: String): List<Path> =
        filesUnder(*roots).filter { it.toString().endsWith(".kt") }

    private fun swiftFilesUnder(vararg roots: String): List<Path> =
        filesUnder(*roots).filter { it.toString().endsWith(".swift") }

    private fun composeViewFiles(): List<Path> =
        kotlinFilesUnder(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve",
            "composeApp/src/androidMain/kotlin/com/guyghost/wakeve"
        ).filter { file ->
            val relative = projectRoot.relativize(file).toString()
            val name = file.fileName.toString()
            "/ui/" in relative ||
                name.endsWith("Screen.kt") ||
                name.endsWith("View.kt") ||
                name.endsWith("Content.kt")
        }

    private fun filesUnder(vararg roots: String): List<Path> =
        roots.flatMap { root ->
            val absoluteRoot = projectRoot.resolve(root)
            if (!Files.exists(absoluteRoot)) {
                emptyList()
            } else {
                Files.walk(absoluteRoot).use { stream ->
                    stream.filter { it.isRegularFile() }.toList()
                }
            }
        }

    private companion object {
        val projectRoot: Path = generateSequence(Paths.get("").toAbsolutePath()) { it.parent }
            .first { Files.exists(it.resolve("settings.gradle.kts")) }

        val providerPatterns = listOf(
            Regex("""(?m)^\s*import\s+FoundationModels\b"""),
            Regex("""(?m)^\s*import\s+com\.google\.mlkit"""),
            Regex("""(?m)^\s*import\s+com\.google\.ai"""),
            Regex("""\bLanguageModelSession\s*\("""),
            Regex("""\bSystemLanguageModel\."""),
            Regex("""\bFirebaseAiLogicCloudTextGenerationClient\s*\("""),
            Regex("""\bMlKit(LocalTextGenerationClient|EventPlanningAiAssistant)\s*\("""),
            Regex("""\bGoogleGenerativeAI\b"""),
            Regex("""\bAICore\b""")
        )
    }
}
