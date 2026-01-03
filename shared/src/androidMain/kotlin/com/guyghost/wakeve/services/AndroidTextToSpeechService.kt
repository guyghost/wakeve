package com.guyghost.wakeve.services

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.guyghost.wakeve.ml.Language
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Android implementation of TextToSpeechService.
 * Uses the Android Text-to-Speech API for voice feedback.
 *
 * Note: The deprecated TextToSpeech API is still widely used and provides
 * reliable TTS functionality across all Android versions.
 */
@Suppress("DEPRECATION")
class AndroidTextToSpeechService(
    private val context: Context
) : TextToSpeechService {
    
    private var tts: TextToSpeech? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    private var currentLanguage: Language = Language.FR
    private var speechRate: Float = 1.0f
    private var isSpeaking: Boolean = false
    private var isInitialized: Boolean = false
    
    init {
        initTTS()
    }
    
    /**
     * Initializes the Android TextToSpeech engine.
     */
    private fun initTTS() {
        tts = TextToSpeech(context) { status ->
            when (status) {
                TextToSpeech.SUCCESS -> {
                    Log.d(TAG, "TTS initialized successfully")
                    isInitialized = true
                    // Set default language and rate
                    tts?.language = getLocale(Language.FR)
                    tts?.setSpeechRate(speechRate)
                    
                    // Set up utterance progress listener
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            isSpeaking = true
                        }
                        
                        override fun onDone(utteranceId: String?) {
                            isSpeaking = false
                        }
                        
                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            isSpeaking = false
                            Log.e(TAG, "TTS utterance error: $utteranceId")
                        }
                        
                        override fun onError(utteranceId: String?, errorCode: Int) {
                            isSpeaking = false
                            Log.e(TAG, "TTS error code: $errorCode")
                        }
                    })
                }
                TextToSpeech.ERROR -> {
                    Log.e(TAG, "Failed to initialize TTS")
                    isInitialized = false
                }
                else -> {
                    Log.e(TAG, "Unknown TTS initialization status: $status")
                    isInitialized = false
                }
            }
        }
    }
    
    /**
     * Converts a Language enum to a Java Locale.
     *
     * @param language The language to convert
     * @return The corresponding Java Locale
     */
    private fun getLocale(language: Language): Locale {
        return when (language) {
            Language.FR -> Locale.FRENCH
            Language.EN -> Locale.ENGLISH
            Language.ES -> Locale("es")
            Language.DE -> Locale.GERMAN
            Language.IT -> Locale.ITALIAN
        }
    }
    
    override suspend fun speak(
        text: String,
        language: Language,
        queueMode: QueueMode
    ): TTSResult = withContext(Dispatchers.Main) {
        if (!isInitialized || tts == null) {
            return@withContext TTSResult.Error("TTS not initialized")
        }
        
        try {
            // Set the language
            val locale = getLocale(language)
            val result = tts?.setLanguage(locale)
            
            if (result == TextToSpeech.LANG_MISSING_DATA || 
                result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.w(TAG, "Language not supported: $language")
                // Fall back to French
                tts?.setLanguage(Locale.FRENCH)
            }
            
            // Determine the queue mode
            val ttsQueueMode = when (queueMode) {
                QueueMode.ADD -> TextToSpeech.QUEUE_ADD
                QueueMode.FLUSH -> TextToSpeech.QUEUE_FLUSH
                QueueMode.INTERRUPT -> {
                    tts?.stop()
                    TextToSpeech.QUEUE_FLUSH
                }
            }
            
            val startTime = System.currentTimeMillis()
            
            // Speak the text (params are required on newer Android versions)
            val params = HashMap<String, String>().apply {
                put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "utterance_${System.currentTimeMillis()}")
            }
            tts?.speak(text, ttsQueueMode, params)
            
            // Wait for speech to complete (approximate)
            // The actual completion is handled by the UtteranceProgressListener
            delay(calculateEstimatedDuration(text))
            
            val durationMs = System.currentTimeMillis() - startTime
            TTSResult.Success(durationMs)
        } catch (e: Exception) {
            Log.e(TAG, "TTS speak error", e)
            TTSResult.Error(e.message ?: "Unknown TTS error")
        }
    }
    
    /**
     * Calculates an estimated duration for the speech based on text length.
     *
     * @param text The text to be spoken
     * @return Estimated duration in milliseconds
     */
    private fun calculateEstimatedDuration(text: String): Long {
        // Average speaking rate is about 150 words per minute
        // Average word length is about 5 characters
        // So approximately 30 characters per second
        val wordsPerSecond = 2.5
        return (text.length / wordsPerSecond * 1000).toLong().coerceAtLeast(500)
    }
    
    override suspend fun stop() {
        withContext(Dispatchers.Main) {
            tts?.stop()
            isSpeaking = false
        }
    }
    
    override suspend fun pause() {
        // Android TTS doesn't have native pause/resume
        // We stop and let the user resume if needed
        withContext(Dispatchers.Main) {
            tts?.stop()
            isSpeaking = false
        }
    }
    
    override suspend fun resume() {
        // Android TTS doesn't support native pause/resume
        // This is a no-op; the user needs to call speak() again
    }
    
    override fun isSpeaking(): Boolean {
        return isSpeaking || (tts?.isSpeaking == true)
    }
    
    override fun isAvailable(): Boolean {
        return isInitialized && tts != null
    }
    
    override fun setLanguage(language: Language) {
        currentLanguage = language
        tts?.setLanguage(getLocale(language))
    }
    
    override fun getAvailableLanguages(): List<Language> {
        // Android TTS typically supports all our languages
        // In a production app, you could query available voices
        return Language.entries
    }
    
    override fun setSpeechRate(rate: Float) {
        speechRate = rate.coerceIn(MIN_SPEECH_RATE, MAX_SPEECH_RATE)
        tts?.setSpeechRate(speechRate)
    }
    
    override fun getSpeechRate(): Float = speechRate
    
    /**
     * Releases TTS resources. Call this when the service is no longer needed.
     */
    fun dispose() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
    
    companion object {
        private const val TAG = "AndroidTTS"
        private const val MIN_SPEECH_RATE = 0.5f
        private const val MAX_SPEECH_RATE = 2.0f
    }
}
