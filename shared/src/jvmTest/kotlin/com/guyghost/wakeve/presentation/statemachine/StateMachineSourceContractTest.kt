package com.guyghost.wakeve.presentation.statemachine

import java.io.File
import kotlin.test.Test
import kotlin.test.assertFalse

class StateMachineSourceContractTest {
    @Test
    fun eventStateMachineDoesNotUseThrowableMessagesForUiErrors() {
        val source = projectFile("shared/src/commonMain/kotlin/com/guyghost/wakeve/presentation/statemachine/EventManagementStateMachine.kt").readText()
        val throwableMessage = listOf("error", ".message").joinToString("")
        val exceptionMessage = listOf("exceptionOrNull()", "?", ".message").joinToString("")
        val nullableMessage = listOf("message", " ?:").joinToString("")

        assertFalse(source.contains(throwableMessage))
        assertFalse(source.contains(exceptionMessage))
        assertFalse(source.contains(nullableMessage))
    }

    private fun projectFile(relativePath: String): File {
        val root = generateSequence(File(System.getProperty("user.dir")).absoluteFile) { it.parentFile }
            .first { File(it, relativePath).exists() }
        return File(root, relativePath)
    }
}
