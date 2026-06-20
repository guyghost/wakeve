package com.guyghost.wakeve.services

import com.guyghost.wakeve.ml.Language
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

class NoConfiguredTextToSpeechServiceTest {

    @Test
    fun `speak returns explicit error when text to speech is not configured`() = runTest {
        val result = NoConfiguredTextToSpeechService.speak(
            text = "Bonjour",
            language = Language.FR,
            queueMode = QueueMode.ADD
        )

        val error = assertIs<TTSResult.Error>(result)
        assertEquals("Text-to-speech service is not configured", error.message)
    }

    @Test
    fun `availability probes are false when text to speech is not configured`() {
        assertFalse(NoConfiguredTextToSpeechService.isAvailable())
        assertFalse(NoConfiguredTextToSpeechService.isSpeaking())
    }

    @Test
    fun `control actions fail when text to speech is not configured`() = runTest {
        val stopError = assertFailsWith<IllegalStateException> {
            NoConfiguredTextToSpeechService.stop()
        }
        val pauseError = assertFailsWith<IllegalStateException> {
            NoConfiguredTextToSpeechService.pause()
        }
        val resumeError = assertFailsWith<IllegalStateException> {
            NoConfiguredTextToSpeechService.resume()
        }

        assertEquals("Text-to-speech service is not configured", stopError.message)
        assertEquals("Text-to-speech service is not configured", pauseError.message)
        assertEquals("Text-to-speech service is not configured", resumeError.message)
    }

    @Test
    fun `configuration methods fail when text to speech is not configured`() {
        val languageError = assertFailsWith<IllegalStateException> {
            NoConfiguredTextToSpeechService.setLanguage(Language.EN)
        }
        val languagesError = assertFailsWith<IllegalStateException> {
            NoConfiguredTextToSpeechService.getAvailableLanguages()
        }
        val rateError = assertFailsWith<IllegalStateException> {
            NoConfiguredTextToSpeechService.setSpeechRate(1.2f)
        }
        val getRateError = assertFailsWith<IllegalStateException> {
            NoConfiguredTextToSpeechService.getSpeechRate()
        }

        assertEquals("Text-to-speech service is not configured", languageError.message)
        assertEquals("Text-to-speech service is not configured", languagesError.message)
        assertEquals("Text-to-speech service is not configured", rateError.message)
        assertEquals("Text-to-speech service is not configured", getRateError.message)
    }
}
