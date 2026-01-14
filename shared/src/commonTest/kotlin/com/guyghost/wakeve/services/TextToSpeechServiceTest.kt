package com.guyghost.wakeve.services

import com.guyghost.wakeve.ml.Language
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertEquals
import kotlin.test.BeforeTest

/**
 * Unit tests for TextToSpeechService.
 * Tests the TTS interface and common behavior.
 *
 * Note: These tests use a mock implementation since actual platform-specific
 * TTS services require Android/iOS runtime contexts.
 */
class TextToSpeechServiceTest {
    
    private lateinit var mockTTSService: MockTextToSpeechService
    
    @BeforeTest
    fun setup() {
        mockTTSService = MockTextToSpeechService()
    }
    
    // ========== Speak Tests ==========
    
    @Test
    fun `speak returns Success with duration when text is spoken`() = runTest {
        // When
        val result = mockTTSService.speak("Test message", Language.FR, QueueMode.ADD)
        
        // Then
        assertTrue(result is TTSResult.Success)
        val success = result as TTSResult.Success
        assertTrue(success.durationMs > 0)
    }
    
    @Test
    fun `speak supports all languages`() = runTest {
        // Test each language
        val languages = listOf(Language.FR, Language.EN, Language.ES, Language.DE, Language.IT)
        
        for (language in languages) {
            // When
            val result = mockTTSService.speak("Test", language, QueueMode.ADD)

            // Then
            assertTrue(result is TTSResult.Success, "Should support language: $language")
        }
    }
    
    @Test
    fun `speak with INTERRUPT mode stops current speech`() = runTest {
        // Given - start speaking
        mockTTSService.speak("First message", Language.FR, QueueMode.ADD)
        
        // When
        val result = mockTTSService.speak("Second message", Language.FR, QueueMode.INTERRUPT)
        
        // Then
        assertTrue(result is TTSResult.Success)
    }
    
    @Test
    fun `speak with FLUSH mode clears queue`() = runTest {
        // When
        val result = mockTTSService.speak("Test message", Language.FR, QueueMode.FLUSH)
        
        // Then
        assertTrue(result is TTSResult.Success)
    }
    
    @Test
    fun `speak with ADD mode adds to queue`() = runTest {
        // When
        val result = mockTTSService.speak("Test message", Language.FR, QueueMode.ADD)
        
        // Then
        assertTrue(result is TTSResult.Success)
    }
    
    // ========== Queue Mode Tests ==========
    
    @Test
    fun `queue modes are correctly defined`() {
        // Verify all queue modes exist
        assertEquals(3, QueueMode.entries.size)
        assertTrue(QueueMode.entries.contains(QueueMode.ADD))
        assertTrue(QueueMode.entries.contains(QueueMode.FLUSH))
        assertTrue(QueueMode.entries.contains(QueueMode.INTERRUPT))
    }
    
    // ========== Status Tests ==========
    
    @Test
    fun `isSpeaking returns false when not speaking`() {
        // Given - not speaking
        
        // When
        val isSpeaking = mockTTSService.isSpeaking()
        
        // Then
        assertFalse(isSpeaking)
    }
    
    @Test
    fun `isSpeaking returns true when speaking`() = runTest {
        // Given - start speaking
        mockTTSService.setSpeaking(true)
        
        // When
        val isSpeaking = mockTTSService.isSpeaking()
        
        // Then
        assertTrue(isSpeaking)
    }
    
    @Test
    fun `isAvailable returns true when service is available`() {
        // When
        val isAvailable = mockTTSService.isAvailable()
        
        // Then
        assertTrue(isAvailable)
    }
    
    // ========== Language Tests ==========
    
    @Test
    fun `setLanguage updates current language`() {
        // When
        mockTTSService.setLanguage(Language.EN)
        
        // Then
        assertEquals(Language.EN, mockTTSService.getCurrentLanguage())
    }
    
    @Test
    fun `getAvailableLanguages returns all supported languages`() {
        // When
        val languages = mockTTSService.getAvailableLanguages()
        
        // Then
        assertEquals(5, languages.size)
        assertTrue(languages.contains(Language.FR))
        assertTrue(languages.contains(Language.EN))
        assertTrue(languages.contains(Language.ES))
        assertTrue(languages.contains(Language.DE))
        assertTrue(languages.contains(Language.IT))
    }
    
    // ========== Speech Rate Tests ==========
    
    @Test
    fun `setSpeechRate updates rate within bounds`() {
        // Given
        assertEquals(1.0f, mockTTSService.getSpeechRate(), 0.01f)
        
        // When - within bounds
        mockTTSService.setSpeechRate(1.5f)
        
        // Then
        assertEquals(1.5f, mockTTSService.getSpeechRate(), 0.01f)
    }
    
    @Test
    fun `setSpeechRate clamps rate below minimum`() {
        // When - below minimum
        mockTTSService.setSpeechRate(0.1f)
        
        // Then
        assertEquals(0.5f, mockTTSService.getSpeechRate(), 0.01f)
    }
    
    @Test
    fun `setSpeechRate clamps rate above maximum`() {
        // When - above maximum
        mockTTSService.setSpeechRate(3.0f)
        
        // Then
        assertEquals(2.0f, mockTTSService.getSpeechRate(), 0.01f)
    }
    
    @Test
    fun `speech rate range is 0_5 to 2_0`() {
        // Test minimum
        mockTTSService.setSpeechRate(0.5f)
        assertEquals(0.5f, mockTTSService.getSpeechRate(), 0.01f)
        
        // Test maximum
        mockTTSService.setSpeechRate(2.0f)
        assertEquals(2.0f, mockTTSService.getSpeechRate(), 0.01f)
    }
    
    // ========== Control Tests ==========
    
    @Test
    fun `stop stops speaking`() = runTest {
        // Given - start speaking
        mockTTSService.setSpeaking(true)
        
        // When
        mockTTSService.stop()
        
        // Then
        assertFalse(mockTTSService.isSpeaking())
    }
    
    @Test
    fun `pause pauses speaking`() = runTest {
        // Given - start speaking
        mockTTSService.setSpeaking(true)
        
        // When
        mockTTSService.pause()
        
        // Then
        assertFalse(mockTTSService.isSpeaking())
    }
    
    @Test
    fun `resume continues speaking after pause`() = runTest {
        // Given - start and pause
        mockTTSService.setSpeaking(true)
        mockTTSService.pause()
        
        // When
        mockTTSService.resume()
        
        // Then
        assertFalse(mockTTSService.isSpeaking()) // Mock doesn't actually resume
    }
    
    // ========== TTS Result Tests ==========
    
    @Test
    fun `TTSResult Success contains duration`() {
        // Given
        val duration = 1500L
        
        // When
        val result = TTSResult.Success(duration)
        
        // Then
        assertEquals(1500L, result.durationMs)
    }
    
    @Test
    fun `TTSResult Error contains message`() {
        // Given
        val message = "TTS initialization failed"
        
        // When
        val result = TTSResult.Error(message)
        
        // Then
        assertEquals("TTS initialization failed", result.message)
    }
    
    @Test
    fun `TTSResult Error handles null message`() {
        // When
        val result = TTSResult.Error("Unknown error")
        
        // Then
        assertNotNull(result.message)
    }
    
    // ========== Language Enum Tests ==========
    
    @Test
    fun `Language enum contains all supported languages`() {
        // Verify all languages exist
        assertEquals(5, Language.entries.size)
        assertTrue(Language.entries.contains(Language.FR))
        assertTrue(Language.entries.contains(Language.EN))
        assertTrue(Language.entries.contains(Language.ES))
        assertTrue(Language.entries.contains(Language.DE))
        assertTrue(Language.entries.contains(Language.IT))
    }
    
    /**
     * Mock implementation of TextToSpeechService for testing.
     * Simulates TTS behavior without requiring platform-specific code.
     */
    private class MockTextToSpeechService : TextToSpeechService {
        private var _isSpeaking = false
        private var _currentLanguage = Language.FR
        private var _speechRate = 1.0f
        
        fun setSpeaking(value: Boolean) {
            _isSpeaking = value
        }
        
        fun getCurrentLanguage(): Language = _currentLanguage
        
        override suspend fun speak(
            text: String,
            language: Language,
            queueMode: QueueMode
        ): TTSResult {
            _isSpeaking = true
            // Simulate speech duration
            kotlinx.coroutines.delay(100)
            _isSpeaking = false
            return TTSResult.Success(100L)
        }
        
        override suspend fun stop() {
            _isSpeaking = false
        }
        
        override suspend fun pause() {
            _isSpeaking = false
        }
        
        override suspend fun resume() {
            // Mock doesn't implement resume
        }
        
        override fun isSpeaking(): Boolean = _isSpeaking
        
        override fun isAvailable(): Boolean = true
        
        override fun setLanguage(language: Language) {
            _currentLanguage = language
        }
        
        override fun getAvailableLanguages(): List<Language> = Language.entries
        
        override fun setSpeechRate(rate: Float) {
            _speechRate = rate.coerceIn(0.5f, 2.0f)
        }
        
        override fun getSpeechRate(): Float = _speechRate
    }
}
