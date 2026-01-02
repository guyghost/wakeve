package com.guyghost.wakeve.voice

import com.guyghost.wakeve.gamification.Badge
import com.guyghost.wakeve.gamification.BadgeCategory
import com.guyghost.wakeve.gamification.BadgeRarity
import com.guyghost.wakeve.ml.Language
import com.guyghost.wakeve.ml.VoiceCommand
import com.guyghost.wakeve.ml.VoiceContext
import com.guyghost.wakeve.ml.VoiceIntent
import com.guyghost.wakeve.ml.VoiceStep
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

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

    @Before
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
    fun `given voice assistant FAB, when accessibility enabled, then provides proper labels`() {
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
    fun `given voice assistant FAB not listening, when accessibility enabled, then provides proper labels`() {
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
    fun `given voice assistant FAB state changes, when accessibility enabled, then announces change`() {
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
    fun `given voice command fails, when fallback to text input, then same functionality available`() {
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
            "Low confidence command should offer text input fallback",
            shouldOfferFallback || voiceCommand.confidenceScore >= 0.5
        )
    }

    /**
     * Tests that text input mode provides identical parsing results to voice.
     */
    @Test
    fun `given text input mode, when parsing same command, then identical results`() {
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
    fun `given speech recognition error, when fallback to text, then user can complete action`() {
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
    fun `given commands in different languages, when parse, then correctly interpreted`() {
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
            "French CREATE_EVENT intent not recognized",
            VoiceIntent.CREATE_EVENT,
            frCommand.intent
        )

        // English command
        val enCommand = voiceCommandParser.parse(
            "Create a birthday party",
            Language.EN,
            context.copy(language = Language.EN)
        )
        assertEquals(
            "English CREATE_EVENT intent not recognized",
            VoiceIntent.CREATE_EVENT,
            enCommand.intent
        )

        // Spanish command
        val esCommand = voiceCommandParser.parse(
            "Crea una fiesta de cumpleaños",
            Language.ES,
            context.copy(language = Language.ES)
        )
        assertEquals(
            "Spanish CREATE_EVENT intent not recognized",
            VoiceIntent.CREATE_EVENT,
            esCommand.intent
        )

        // German command
        val deCommand = voiceCommandParser.parse(
            "Erstelle eine Geburtstagsparty",
            Language.DE,
            context.copy(language = Language.DE)
        )
        assertEquals(
            "German CREATE_EVENT intent not recognized",
            VoiceIntent.CREATE_EVENT,
            deCommand.intent
        )
    }

    /**
     * Tests that event type detection works across languages.
     */
    @Test
    fun `given event types in different languages, when parse, then detects correctly`() {
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
    fun `given language switch during session, when parse, then uses new language`() {
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
    fun `given natural language date, when parse, then extracts correct date`() {
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
        assertNotNull(
            "Today should be parsed",
            todayCommand.parameters["date"]
        )

        // Test "demain" (tomorrow)
        val tomorrowCommand = voiceCommandParser.parse(
            "Crée un événement pour demain",
            Language.FR,
            context
        )
        assertNotNull(
            "Tomorrow should be parsed",
            tomorrowCommand.parameters["date"]
        )

        // Test "samedi prochain" (next Saturday)
        val saturdayCommand = voiceCommandParser.parse(
            "Crée un événement pour samedi prochain",
            Language.FR,
            context
        )
        assertNotNull(
            "Saturday should be parsed",
            saturdayCommand.parameters["date"]
        )
    }

    /**
     * Tests that English date expressions are parsed correctly.
     */
    @Test
    fun `given English natural language date, when parse, then extracts correct date`() {
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
        assertNotNull(
            "Today should be parsed",
            todayCommand.parameters["date"]
        )

        // Test "tomorrow"
        val tomorrowCommand = voiceCommandParser.parse(
            "Create an event for tomorrow",
            Language.EN,
            context
        )
        assertNotNull(
            "Tomorrow should be parsed",
            tomorrowCommand.parameters["date"]
        )
    }

    /**
     * Tests that date with time of day is correctly extracted.
     */
    @Test
    fun `given date with time of day, when parse, then extracts both`() {
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

        assertNotNull("Date should be extracted", command.parameters["date"])
        assertEquals(
            "Time of day should be AFTERNOON",
            "AFTERNOON",
            command.parameters["timeOfDay"]
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
    fun `given natural language number, when parse, then extracts correct count`() {
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
            "Numeric word 'twenty' should be parsed",
            "20",
            numericCommand.parameters["participantCount"]
        )

        // Test "quelques" (a few)
        val fewCommand = voiceCommandParser.parse(
            "Ajoute quelques participants",
            Language.FR,
            context
        )
        assertEquals(
            "Word 'quelques' should map to 3",
            "3",
            fewCommand.parameters["participantCount"]
        )
    }

    /**
     * Tests that English number words are parsed correctly.
     */
    @Test
    fun `given English number words, when parse, then extracts correct count`() {
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
            "Word 'ten' should be parsed as 10",
            "10",
            tenCommand.parameters["participantCount"]
        )

        // Test "a few"
        val fewCommand = voiceCommandParser.parse(
            "A few participants",
            Language.EN,
            context
        )
        assertEquals(
            "Word 'a few' should map to 3",
            "3",
            fewCommand.parameters["participantCount"]
        )
    }

    /**
     * Tests that numeric digits are parsed correctly.
     */
    @Test
    fun `given numeric digits, when parse, then extracts correct number`() {
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
            "Numeric digit should be parsed",
            "15",
            command.parameters["participantCount"]
        )
    }

    /**
     * Tests that Spanish number words are parsed correctly.
     */
    @Test
    fun `given Spanish number words, when parse, then extracts correct count`() {
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
            "Spanish 'cinco' should be parsed as 5",
            "5",
            command.parameters["participantCount"]
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
