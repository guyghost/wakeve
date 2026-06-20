package com.guyghost.wakeve.di

import java.io.File
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertFalse

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

    @Test
    fun createMeetingStateMachineDoesNotInjectMockMeetingLinkProvider() {
        val source = projectFile("shared/src/iosMain/kotlin/com/guyghost/wakeve/di/IosFactory.kt").readText()
        val start = source.indexOf("fun createMeetingStateMachine")
        require(start >= 0) { "Meeting state machine creation was not found in IosFactory." }
        val end = source.indexOf("fun createScenarioStateMachine", start)
        require(end > start) { "Could not isolate createMeetingStateMachine in IosFactory." }
        val invocation = source.substring(start, end)

        assertFalse(
            invocation.contains("MockMeetingPlatformProvider"),
            "iOS production factory must not generate fake meeting links via MockMeetingPlatformProvider."
        )
        assertContains(
            invocation,
            "UnavailableMeetingPlatformProvider",
            message = "iOS production factory should fail honestly until a real meeting provider is configured."
        )
    }

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }
}
