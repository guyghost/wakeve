package com.guyghost.wakeve.ai

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UnavailableAiTextGenerationClientTest {
    @Test
    fun unavailableClientReportsUnavailable() = runTest {
        val client = UnavailableAiTextGenerationClient()

        assertEquals(AiModelAvailability.UNAVAILABLE, client.availability())
    }

    @Test
    fun unavailableClientDoesNotGenerateText() = runTest {
        val client = UnavailableAiTextGenerationClient("No configured model")

        assertFailsWith<IllegalStateException> {
            client.generateText("Write something")
        }
    }

    @Test
    fun legacyFakeTextGenerationClientDoesNotGenerateText() = runTest {
        @Suppress("DEPRECATION")
        val client = FakeAiTextGenerationClient(response = "This should not be returned")

        assertEquals(AiModelAvailability.UNAVAILABLE, client.availability())
        assertFailsWith<IllegalStateException> {
            client.generateText("Write something")
        }
    }
}
