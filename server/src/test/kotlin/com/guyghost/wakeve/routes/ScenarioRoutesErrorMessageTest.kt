package com.guyghost.wakeve.routes

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class ScenarioRoutesErrorMessageTest {
    @Test
    fun scenarioFailureMessagesUseStableSafeCopy() {
        val messages = listOf(
            scenarioDetailFailureMessage(),
            invalidScenarioStatusMessage(),
            invalidScenarioGenerationTypeMessage(),
            scenarioUpdateFailureMessage(),
            scenarioDeleteFailureMessage(),
            scenarioVoteCreateFailureMessage(),
            scenarioVotesFailureMessage(),
            scenarioListFailureMessage(),
            scenarioMatrixGenerateFailureMessage(),
            scenarioMatrixPublishFailureMessage(),
            scenarioFinalSelectionFailureMessage(),
            scenarioCreateFailureMessage(),
            scenariosWithVotesFailureMessage()
        )

        assertEquals(messages.size, messages.distinct().size)
        messages.forEach { message ->
            assertFalse(message.isBlank())
            assertDoesNotExposeSensitiveDetails(message)
        }
    }

    private fun assertDoesNotExposeSensitiveDetails(message: String) {
        listOf(
            "secret@example.com",
            "SECRET",
            "SQL constraint",
            "http://internal.local",
            "token=",
            "scenario_",
            "event_",
            "participant-"
        ).forEach { sensitiveValue ->
            assertFalse(
                message.contains(sensitiveValue, ignoreCase = true),
                "Message should not expose `$sensitiveValue`: $message"
            )
        }
    }
}
