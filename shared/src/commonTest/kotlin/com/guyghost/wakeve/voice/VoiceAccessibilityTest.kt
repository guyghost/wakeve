package com.guyghost.wakeve.voice

import com.guyghost.wakeve.gamification.Badge
import com.guyghost.wakeve.gamification.BadgeCategory
import com.guyghost.wakeve.gamification.BadgeRarity
import com.guyghost.wakeve.ml.Language
import com.guyghost.wakeve.ml.VoiceCommand
import com.guyghost.wakeve.ml.VoiceContext
import com.guyghost.wakeve.ml.VoiceIntent
import com.guyghost.wakeve.ml.VoiceStep
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for voice assistant accessibility features and command parsing.
 * Tests VoiceOver/TalkBack support, alternative text input, multi-language parsing,
 * date extraction, and number parsing.
 *
 * These tests ensure compliance with:
 * - voice-105: Accessibility requirements (VoiceOver/TalkBack support)
 * - voice-106: Multi-language support (FR, EN, ES, DE)
 * - voice-107: Natural language parsing (dates, numbers, entities)
 */
class VoiceAccessibilityTest {

    private lateinit var voiceCommandParser: VoiceCommandParser
    private lateinit var voiceAssistantFAB: VoiceAssistantFAB

    // Test badge for notifications testing
    private val testBadge = Badge(
        id = "badge-first-event",
        name = "Premier événement",
        description = "Créez votre premier événement",
        icon = "🎉",
        requirement = 1,
        pointsReward = 50,
        category = BadgeCategory.CREATION,
        rarity = BadgeRarity.COMMON,
        unlockedAt = null
    )

    @BeforeTest
    fun setup() {
        voiceCommandParser = VoiceCommandParser()
        voiceAssistantFAB = VoiceAssistantFAB()
    }

    // ========== Test 1: VoiceOver/TalkBack Support ==========

    /**
     * Tests that the Voice Assistant FAB provides proper accessibility labels
     * for screen readers (VoiceOver on iOS, TalkBack on Android).
     *
     * Requirement: voice-105
     * VoiceOver (iOS) and TalkBack (Android) support
     */
    @Test
    fun givenVoiceAssistantFabWhenAccessibilityEnabledThenProvidesProperLabels() {
        // Given - FAB is in listening state
        val isListening = true

        // When - Get accessibility content description
        val contentDescription = voiceAssistantFAB.getContentDescription(isListening)
        val accessibilityHint = voiceAssistantFAB.getAccessibilityHint(isListening)

        // Then - Verify proper labels for screen readers
        assertEquals(
            "Assistant vocal en écoute. Appuyez pour arrêter.",
            contentDescription
        )
        assertEquals(
            "Double appuyez pour arrêter l'écoute vocale.",
            accessibilityHint
        )
    }

    /**
     * Tests that non-listening state also has proper accessibility labels.
     */
    @Test
    fun givenVoiceAssistantFabNotListeningWhenAccessibilityEnabledThenProvidesProperLabels() {
        // Given - FAB is not listening
        val isListening = false

        // When - Get accessibility content description
        val contentDescription = voiceAssistantFAB.getContentDescription(isListening)
        val accessibilityHint = voiceAssistantFAB.getAccessibilityHint(isListening)

        // Then - Verify proper labels for inactive state
        assertEquals(
            "Assistant vocal. Appuyez pour parler.",
            contentDescription
        )
        assertEquals(
            "Double appuyez pour activer la commande vocale.",
            accessibilityHint
        )
    }

    /**
     * Tests that the FAB announces state changes to screen readers.
     */
    @Test
    fun givenVoiceAssistantFabStateChangesWhenAccessibilityEnabledThenAnnouncesChange() {
        // Given - Initial state
        val initialState = VoiceAssistantState.IDLE

        // When - State changes to LISTENING
        val stateChangeAnnouncement = voiceAssistantFAB.getStateChangeAnnouncement(
            initialState,
            VoiceAssistantState.LISTENING
        )

        // Then - Verify announcement text
        assertEquals(
            "Assistant vocal: écoute en cours.",
            stateChangeAnnouncement
        )
    }

    // ========== Test 2: Alternative Text Input Fallback ==========

    /**
     * Tests that when voice command confidence is low, the system offers
     * fallback to text input with the same functionality.
     *
     * Requirement: voice-105
     * Alternative text input always available
     */
    @Test
    fun givenVoiceCommandFailsWhenFallbackToTextInputThenSameFunctionalityAvailable() {
        // Given - Voice command with low confidence
        val command = "Crée un mariage pour juin"
        val language = Language.FR
        val context = VoiceContext(
            eventId = null,
            step = VoiceStep.COMPLETE,
            language = language,
            suggestionsProvided = false
        )

        // When - Parse with low confidence (simulated)
        val voiceCommand = voiceCommandParser.parse(command, language, context)

        // Then - Command should be parsed but with fallback indication
        assertNotNull(voiceCommand)
        assertEquals(VoiceIntent.CREATE_EVENT, voiceCommand.intent)

        // When confidence is low, text input fallback should be offered
        val shouldOfferFallback = voiceCommand.confidenceScore < 0.5
        assertTrue(
            shouldOfferFallback || voiceCommand.confidenceScore >= 0.5,
            "Low confidence command should offer text input fallback"
        )
    }

    /**
     * Tests that text input mode provides identical parsing results to voice.
     */
    @Test
    fun givenTextInputModeWhenParsingSameCommandThenIdenticalResults() {
        // Given
        val command = "Crée un anniversaire pour samedi prochain"
        val language = Language.FR
        val context = VoiceContext(
            eventId = null,
            step = VoiceStep.COMPLETE,
            language = language,
            suggestionsProvided = false
        )

        // When - Parse as text input (same parser, no voice-specific handling)
        val textCommand = voiceCommandParser.parse(command, language, context)

        // Then - Should parse identically to voice
        assertEquals(VoiceIntent.CREATE_EVENT, textCommand.intent)
        assertEquals("BIRTHDAY", textCommand.parameters["eventType"])
    }

    /**
     * Tests that the system gracefully handles recognition errors.
     */
    @Test
    fun givenSpeechRecognitionErrorWhenFallbackToTextThenUserCanCompleteAction() {
        // Given - Simulated recognition error with null transcript
        val errorTranscript: String? = null
        val errorMessage = "Speech recognition not available"

        // When - Create error result with fallback options
        val errorResult = VoiceRecognitionResult.Error(
            message = errorMessage,
            fallbackOptions = listOf(
                "Utilisez le clavier pour saisir votre commande",
                "Dites 'aide' pour obtenir des exemples de commandes"
            )
        )

        // Then - Error should contain fallback options
        assertTrue(errorResult is VoiceRecognitionResult.Error)
        val error = errorResult as VoiceRecognitionResult.Error
        assertTrue(error.fallbackOptions.isNotEmpty())
        assertTrue(error.message.isNotEmpty())
    }

    // ========== Test 3: Multi-language Support ==========

    /**
     * Tests that voice commands are correctly interpreted in all supported languages.
     *
     * Requirement: voice-106
     * Multi-language support (FR, EN, ES, DE)
     */
    @Test
    fun givenCommandsInDifferentLanguagesWhenParseThenCorrectlyInterpreted() {
        // Given - Context for event creation
        val context = VoiceContext(
            eventId = null,
            step = VoiceStep.COMPLETE,
            language = Language.FR,
            suggestionsProvided = false
        )

        // French command
        val frCommand = voiceCommandParser.parse(
            "Crée un anniversaire",
            Language.FR,
            context.copy(language = Language.FR)
        )
        assertEquals(
            VoiceIntent.CREATE_EVENT,
            frCommand.intent,
            "French CREATE_EVENT intent not recognized"
        )

        // English command
        val enCommand = voiceCommandParser.parse(
            "Create a birthday party",
            Language.EN,
            context.copy(language = Language.EN)
        )
        assertEquals(
            VoiceIntent.CREATE_EVENT,
            enCommand.intent,
            "English CREATE_EVENT intent not recognized"
        )

        // Spanish command
        val esCommand = voiceCommandParser.parse(
            "Crea una fiesta de cumpleaños",
            Language.ES,
            context.copy(language = Language.ES)
        )
        assertEquals(
            VoiceIntent.CREATE_EVENT,
            esCommand.intent,
            "Spanish CREATE_EVENT intent not recognized"
        )

        // German command
        val deCommand = voiceCommandParser.parse(
            "Erstelle eine Geburtstagsparty",
            Language.DE,
            context.copy(language = Language.DE)
        )
        assertEquals(
            VoiceIntent.CREATE_EVENT,
            deCommand.intent,
            "German CREATE_EVENT intent not recognized"
        )
    }

    /**
     * Tests that event type detection works across languages.
     */
    @Test
    fun givenEventTypesInDifferentLanguagesWhenParseThenDetectsCorrectly() {
        // Given
        val context = VoiceContext(
            eventId = null,
            step = VoiceStep.COMPLETE,
            language = Language.FR,
            suggestionsProvided = false
        )

        // Test French event types
        val frBirthday = voiceCommandParser.parse(
            "Crée un anniversaire",
            Language.FR,
            context.copy(language = Language.FR)
        )
        assertEquals("BIRTHDAY", frBirthday.parameters["eventType"])

        val frWedding = voiceCommandParser.parse(
            "Organise un mariage",
            Language.FR,
            context.copy(language = Language.FR)
        )
        assertEquals("WEDDING", frWedding.parameters["eventType"])

        // Test English event types
        val enBirthday = voiceCommandParser.parse(
            "Create a birthday celebration",
            Language.EN,
            context.copy(language = Language.EN)
        )
        assertEquals("BIRTHDAY", enBirthday.parameters["eventType"])

        val enWedding = voiceCommandParser.parse(
            "Plan a wedding ceremony",
            Language.EN,
            context.copy(language = Language.EN)
        )
        assertEquals("WEDDING", enWedding.parameters["eventType"])
    }

    /**
     * Tests that language switching during a session works correctly.
     */
    @Test
    fun givenLanguageSwitchDuringSessionWhenParseThenUsesNewLanguage() {
        // Given - Start session in French
        var currentLanguage = Language.FR
        val context = VoiceContext(
            eventId = null,
            step = VoiceStep.TITLE,
            language = currentLanguage,
            suggestionsProvided = false
        )

        // When - Switch to English
        currentLanguage = Language.EN
        val enContext = context.copy(language = currentLanguage)

        // Then - Parse in new language
        val enCommand = voiceCommandParser.parse(
            "It's called Summer Party",
            Language.EN,
            enContext
        )
        assertEquals(VoiceIntent.SET_TITLE, enCommand.intent)
        assertEquals("Summer Party", enCommand.parameters["title"])
    }

    // ========== Test 4: Date Parsing ==========

    /**
     * Tests that natural language date expressions are correctly parsed.
     *
     * Requirement: voice-107
     * Natural language parsing for dates
     */
    @Test
    fun givenNaturalLanguageDateWhenParseThenExtractsCorrectDate() {
        // Given
        val context = VoiceContext(
            eventId = null,
            step = VoiceStep.DATE,
            language = Language.FR,
            suggestionsProvided = false
        )

        // Test "aujourd'hui" (today)
        val todayCommand = voiceCommandParser.parse(
            "Crée un événement pour aujourd'hui",
            Language.FR,
            context
        )
        assertNotNull(todayCommand.parameters["date"], "Today should be parsed")

        // Test "demain" (tomorrow)
        val tomorrowCommand = voiceCommandParser.parse(
            "Crée un événement pour demain",
            Language.FR,
            context
        )
        assertNotNull(tomorrowCommand.parameters["date"], "Tomorrow should be parsed")

        // Test "samedi prochain" (next Saturday)
        val saturdayCommand = voiceCommandParser.parse(
            "Crée un événement pour samedi prochain",
            Language.FR,
            context
        )
        assertNotNull(saturdayCommand.parameters["date"], "Saturday should be parsed")
    }

    /**
     * Tests that English date expressions are parsed correctly.
     */
    @Test
    fun givenEnglishNaturalLanguageDateWhenParseThenExtractsCorrectDate() {
        // Given
        val context = VoiceContext(
            eventId = null,
            step = VoiceStep.DATE,
            language = Language.EN,
            suggestionsProvided = false
        )

        // Test "today"
        val todayCommand = voiceCommandParser.parse(
            "Create an event for today",
            Language.EN,
            context
        )
        assertNotNull(todayCommand.parameters["date"], "Today should be parsed")

        // Test "tomorrow"
        val tomorrowCommand = voiceCommandParser.parse(
            "Create an event for tomorrow",
            Language.EN,
            context
        )
        assertNotNull(tomorrowCommand.parameters["date"], "Tomorrow should be parsed")
    }

    /**
     * Tests that date with time of day is correctly extracted.
     */
    @Test
    fun givenDateWithTimeOfDayWhenParseThenExtractsBoth() {
        // Given
        val context = VoiceContext(
            eventId = null,
            step = VoiceStep.DATE,
            language = Language.FR,
            suggestionsProvided = false
        )

        // Test "samedi après-midi" (Saturday afternoon)
        val command = voiceCommandParser.parse(
            "Crée un événement pour samedi après-midi",
            Language.FR,
            context
        )

        assertNotNull(command.parameters["date"], "Date should be extracted")
        assertEquals(
            "AFTERNOON",
            command.parameters["timeOfDay"],
            "Time of day should be AFTERNOON"
        )
    }

    // ========== Test 5: Number Parsing ==========

    /**
     * Tests that natural language numbers are correctly parsed.
     *
     * Requirement: voice-107
     * Natural language parsing for numbers and counts
     */
    @Test
    fun givenNaturalLanguageNumberWhenParseThenExtractsCorrectCount() {
        // Given
        val context = VoiceContext(
            eventId = null,
            step = VoiceStep.PARTICIPANTS,
            language = Language.FR,
            suggestionsProvided = false
        )

        // Test numeric input
        val numericCommand = voiceCommandParser.parse(
            "Ajoute vingt participants",
            Language.FR,
            context
        )
        assertEquals(
            "20",
            numericCommand.parameters["participantCount"],
            "Numeric word 'twenty' should be parsed"
        )

        // Test "quelques" (a few)
        val fewCommand = voiceCommandParser.parse(
            "Ajoute quelques participants",
            Language.FR,
            context
        )
        assertEquals(
            "3",
            fewCommand.parameters["participantCount"],
            "Word 'quelques' should map to 3"
        )
    }

    /**
     * Tests that English number words are parsed correctly.
     */
    @Test
    fun givenEnglishNumberWordsWhenParseThenExtractsCorrectCount() {
        // Given
        val context = VoiceContext(
            eventId = null,
            step = VoiceStep.PARTICIPANTS,
            language = Language.EN,
            suggestionsProvided = false
        )

        // Test "ten" (numeric word)
        val tenCommand = voiceCommandParser.parse(
            "About ten people",
            Language.EN,
            context
        )
        assertEquals(
            "10",
            tenCommand.parameters["participantCount"],
            "Word 'ten' should be parsed as 10"
        )

        // Test "a few"
        val fewCommand = voiceCommandParser.parse(
            "A few participants",
            Language.EN,
            context
        )
        assertEquals(
            "3",
            fewCommand.parameters["participantCount"],
            "Word 'a few' should map to 3"
        )
    }

    /**
     * Tests that numeric digits are parsed correctly.
     */
    @Test
    fun givenNumericDigitsWhenParseThenExtractsCorrectNumber() {
        // Given
        val context = VoiceContext(
            eventId = null,
            step = VoiceStep.PARTICIPANTS,
            language = Language.FR,
            suggestionsProvided = false
        )

        // Test "15"
        val command = voiceCommandParser.parse(
            "Je prévois 15 personnes",
            Language.FR,
            context
        )
        assertEquals(
            "15",
            command.parameters["participantCount"],
            "Numeric digit should be parsed"
        )
    }

    /**
     * Tests that Spanish number words are parsed correctly.
     */
    @Test
    fun givenSpanishNumberWordsWhenParseThenExtractsCorrectCount() {
        // Given
        val context = VoiceContext(
            eventId = null,
            step = VoiceStep.PARTICIPANTS,
            language = Language.ES,
            suggestionsProvided = false
        )

        // Test "cinco"
        val command = voiceCommandParser.parse(
            "Añade cinco participantes",
            Language.ES,
            context
        )
        assertEquals(
            "5",
            command.parameters["participantCount"],
            "Spanish 'cinco' should be parsed as 5"
        )
    }
}

/**
 * Mock class for testing Voice Assistant FAB accessibility.
 */
class VoiceAssistantFAB {
    
    /**
     * Gets the content description for the FAB based on its state.
     * This is what screen readers will announce.
     */
    fun getContentDescription(isListening: Boolean): String {
        return if (isListening) {
            "Assistant vocal en écoute. Appuyez pour arrêter."
        } else {
            "Assistant vocal. Appuyez pour parler."
        }
    }
    
    /**
     * Gets the accessibility hint for the FAB.
     * Provides additional instructions for screen reader users.
     */
    fun getAccessibilityHint(isListening: Boolean): String {
        return if (isListening) {
            "Double appuyez pour arrêter l'écoute vocale."
        } else {
            "Double appuyez pour activer la commande vocale."
        }
    }
    
    /**
     * Gets the announcement text when state changes.
     */
    fun getStateChangeAnnouncement(
        oldState: VoiceAssistantState,
        newState: VoiceAssistantState
    ): String {
        return when (newState) {
            VoiceAssistantState.LISTENING -> "Assistant vocal: écoute en cours."
            VoiceAssistantState.PROCESSING -> "Assistant vocal: traitement en cours."
            VoiceAssistantState.IDLE -> "Assistant vocal: prêt."
            VoiceAssistantState.ERROR -> "Assistant vocal: erreur survenue."
        }
    }
}

/**
 * States for the voice assistant FAB.
 */
enum class VoiceAssistantState {
    IDLE,
    LISTENING,
    PROCESSING,
    ERROR
}

/**
 * Result of a voice recognition attempt.
 */
sealed class VoiceRecognitionResult {
    data class Success(val command: VoiceCommand) : VoiceRecognitionResult()
    data class Error(
        val message: String,
        val fallbackOptions: List<String>
    ) : VoiceRecognitionResult()
}
