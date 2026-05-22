package com.guyghost.wakeve.di

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains

class IosFactoryScenarioStateMachineContractTest {
    @Test
    fun createScenarioStateMachinePassesRepositoriesNeededForFinalSelection() {
        val source = projectFile("shared/src/iosMain/kotlin/com/guyghost/wakeve/di/IosFactory.kt").readText()
        val start = source.indexOf("val stateMachine = ScenarioManagementStateMachine(")
        require(start >= 0) { "ScenarioManagementStateMachine creation was not found in IosFactory." }
        val end = source.indexOf("\n        )", start)
        require(end > start) { "Could not isolate ScenarioManagementStateMachine arguments in IosFactory." }
        val invocation = source.substring(start, end)

        assertContains(
            invocation,
            "eventRepository = eventRepository",
            message = "iOS factory must pass EventRepositoryInterface so SelectScenarioAsFinal can update event status."
        )
        assertContains(
            invocation,
            "scenarioRepository = scenarioRepository",
            message = "iOS factory must pass ScenarioRepository so SelectScenarioAsFinal can persist the selected scenario."
        )
    }

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }
}
