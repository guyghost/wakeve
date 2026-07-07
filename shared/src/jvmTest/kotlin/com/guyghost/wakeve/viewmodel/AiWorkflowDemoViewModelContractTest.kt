package com.guyghost.wakeve.viewmodel

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

class AiWorkflowDemoViewModelContractTest {
    @Test
    fun defaultsDoNotUseFakeTextGenerationClients() {
        val source = projectFile("composeApp/src/commonMain/kotlin/com/guyghost/wakeve/viewmodel/AiWorkflowDemoViewModel.kt").readText()
        val start = source.indexOf("class AiWorkflowDemoViewModel(")
        require(start >= 0) { "AiWorkflowDemoViewModel constructor was not found." }
        val end = source.indexOf(") : ViewModel()", start)
        require(end > start) { "Could not isolate AiWorkflowDemoViewModel constructor defaults." }
        val constructor = source.substring(start, end)

        assertFalse(
            constructor.contains("FakeAiTextGenerationClient"),
            "AI workflow defaults must not fabricate generated text when real clients are not configured."
        )
        assertContains(
            constructor,
            "UnavailableAiTextGenerationClient",
            message = "AI workflow defaults should fail or fall back honestly when text generation is not configured."
        )
        assertFalse(
            constructor.contains("Cloud fallback message"),
            "AI workflow defaults must not expose a hardcoded cloud-generated message."
        )
    }

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }
}
