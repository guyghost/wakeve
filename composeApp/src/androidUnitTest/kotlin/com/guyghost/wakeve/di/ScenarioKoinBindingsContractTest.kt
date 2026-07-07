package com.guyghost.wakeve.di

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class ScenarioKoinBindingsContractTest {
    @Test
    fun appModuleDeclaresScenarioManagementGraphBindings() {
        val source = projectFile(
            "composeApp/src/commonMain/kotlin/com/guyghost/wakeve/di/AppModule.kt"
        ).readText()

        assertTrue(
            source.contains("val appModule: Module = module"),
            "AppModule must expose the appModule Koin module."
        )
        assertTrue(
            source.contains("ScenarioManagementStateMachine("),
            "appModule must bind ScenarioManagementStateMachine."
        )
        assertTrue(
            source.contains("ScenarioManagementViewModel(stateMachine = stateMachine)"),
            "appModule must bind ScenarioManagementViewModel to the scenario state machine."
        )
        assertTrue(
            source.contains("getOrNull<EventRepositoryInterface>()"),
            "Scenario state machine binding must use the optional event repository."
        )
        assertTrue(
            source.contains("getOrNull<ScenarioRepository>()"),
            "Scenario graph must prefer the concrete ScenarioRepository when available."
        )
        assertTrue(
            source.contains("get<IScenarioRepositoryWrite>()"),
            "Scenario use cases must keep the test/offline repository fallback."
        )
    }

    private fun projectFile(relativePath: String): File {
        var root = File(System.getProperty("user.dir") ?: error("Missing user.dir")).absoluteFile
        while (!File(root, relativePath).exists()) {
            root = root.parentFile ?: error("Could not find project root for $relativePath")
        }
        return File(root, relativePath)
    }
}
