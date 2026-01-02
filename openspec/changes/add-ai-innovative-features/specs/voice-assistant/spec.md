# Specification: Intelligent Voice Assistant

> **Change ID**: `add-ai-innovative-features`
> **Capability**: `voice-assistant` (new)
> **Type**: New Feature
> **Date**: 2026-01-01

## Summary
Créer un assistant vocal intelligent intégré à Wakeve pour permettre une gestion mains libres des événements, depuis la création initiale jusqu'à la consultation des résultats de sondages. L'assistant utilise le traitement du langage naturel (NLP) pour extraire des intentions et des entités à partir de la parole de l'utilisateur.

## ADDED Requirements

### Requirement: Voice-Activated Event Creation
**ID:** `voice-101`

The system SHALL allow users to create events using voice commands.

**Business Rules:**
- Support French and English languages
- Multi-step event creation: title → description → date → participants → confirm
- Natural language parsing for dates (ex: "demain 15h" → "2026-01-02T15:00")
- Confirmation before creation: "Créer l'événement ?"

#### Scenario: Create event via voice command
- **GIVEN** user says "Crée un mariage pour juin"
- **WHEN** VoiceAssistant processes command
- **THEN** user prompted: "Quel titre ?" → "Mariage de Sophie" → "Description ?"
- **THEN** event created with title="Mariage de Sophie", date=June, type=WEDDING
- **THEN** confirmation: "Événement créé avec succès"

### Requirement: Poll Management via Voice
**ID:** `voice-102`

The system SHALL allow poll management through voice commands.

**Business Rules:**
- Add slots: "Ajoute un créneau samedi après-midi"
- View results: "Combien de personnes ont voté ?"
- Confirm date: "Valide la date du 15 juin"

**Scenarios:**
- Given event in POLLING status
- When user says "Ajoute un créneau samedi après-midi"
- Then TimeSlot created with date=upcoming Saturday, time=AFTERNOON
- Given user asks for poll results
- When VoiceAssistant responds
- Then "12 personnes ont voté, 4 OUI, 3 NON, 5 INDÉTERMINÉ"

### Requirement: Contextual Suggestions
**ID:** `voice-103`

The system SHALL provide contextual suggestions based on user preferences.

**Business Rules:**
- Suggestions based on event type (ex: mariage → traiteur, musée)
- Suggestions based on location (ex: Paris → musées, théâtres)
- Suggestions based on participant count (ex: 50+ → salles de réception, valet parking)
- "Smart" suggestions: "Tu sembles aimer les lieux culturels, je vais chercher des musées"

**Scenarios:**
- Given user creating CULTURAL_EVENT
- When VoiceAssistant suggests
- Then "Je peux t'aider avec un musée, un théâtre ou une galerie d'art à Paris"
- Given user selects "musée"
- Then VoiceAssistant adds as potential location with details

### Requirement: Quick Actions via Voice
**ID:** `voice-104`

The system SHALL support quick voice commands for common actions.

**Commands Supported:**
- "Envoie les invitations maintenant"
- "Rappelle-moi de vérifier les votes demain"
- "Ouvre le calendrier"
- "Annule le dernier événement"
- "Combien d'événements ai-je créés ?"

**Scenarios:**
- Given user says "Envoie les invitations maintenant"
- When VoiceAssistant processes command
- Then Invitation emails sent, confirmation displayed
- Given user asks "Combien d'événements ai-je créés ?"
- When VoiceAssistant responds
- Then "Vous avez créé 15 événements depuis janvier"

### Requirement: Accessibility
**ID:** `voice-105`

The system SHALL be accessible to users with disabilities.

**Business Rules:**
- VoiceOver (iOS) and TalkBack (Android) support
- Alternative text input always available
- Visual feedback for voice commands (showing what was understood)
- Error handling with clear messages
- Multi-language support (FR, EN, ES, DE)

**Scenarios:**
- Given user with visual impairment
- When using voice commands
- Then All features accessible via VoiceOver/TalkBack
- Given voice recognition fails
- When user falls back to text input
- Then Same functionality available with no penalty

## Data Models

### VoiceCommand
```kotlin
@Serializable
data class VoiceCommand(
    val intent: VoiceIntent,           // CREATE_EVENT, ADD_SLOT, CONFIRM_DATE, SEND_INVITATIONS, etc.
    val parameters: Map<String, String>,   // Extracted parameters
    val confidenceScore: Double,          // 0.0 - 1.0 (recognition confidence)
    val rawTranscript: String,          // Original speech
    val timestamp: String               // When command was issued
)

enum class VoiceIntent {
    CREATE_EVENT,
    SET_TITLE,
    SET_DESCRIPTION,
    SET_DATE,
    SET_PARTICIPANTS,
    ADD_SLOT,
    CONFIRM_POLL,
    SEND_INVITATIONS,
    OPEN_CALENDAR,
    CANCEL_EVENT,
    GET_STATS
}
```

### VoiceSession
```kotlin
@Serializable
data class VoiceSession(
    val sessionId: String,
    val userId: String,
    val commands: List<VoiceCommand>,
    val context: VoiceContext,
    val status: SessionStatus,      // ACTIVE, COMPLETED, CANCELLED
    val startTime: String,
    val endTime: String?
)

enum class SessionStatus { ACTIVE, COMPLETED, CANCELLED }

@Serializable
data class VoiceContext(
    val eventId: String?,               // Current event being created
    val step: VoiceStep,                 // TITLE, DESCRIPTION, DATE, PARTICIPANTS, CONFIRM
    val language: Language,               // EN, FR, ES, DE
    val suggestionsProvided: Boolean
)
```

### Language
```kotlin
@Serializable
enum class Language { EN, FR, ES, DE, IT }

data class LanguageConfig(
    val code: Language,
    val name: String,               // "English", "Français", etc.
    val locale: String,              // "en-US", "fr-FR", "es-ES"
    val dateFormats: List<String>     // Format preferences per language
)
```

## API Changes

### POST /api/voice/process-command
Process a voice command and return the result.

**Request:**
```json
{
  "command": "create_event",
  "language": "fr",
  "audioData": "base64_encoded_audio"
}
```

**Response:**
```json
{
  "intent": "CREATE_EVENT",
  "parameters": {
      "title": "Mariage de Sophie",
      "description": "..."
  },
  "confidenceScore": 0.95,
  "transcript": "créer un mariage pour juin",
  "nextStep": "description"
}
```

### POST /api/voice/feedback
Submit feedback on voice command accuracy (for ML improvement).

**Request:**
```json
{
  "commandId": "cmd-123",
  "userFeedback": "correct",
  "transcript": "créer un mariage pour juin",
  "actualParameters": {
      "title": "Mariage de Sophie"
  }
}
```

### GET /api/voice/supported-commands
List all supported voice commands and their descriptions.

**Response:**
```json
{
  "language": "fr",
  "commands": [
    {
      "command": "create_event",
      "description": "Créer un nouvel événement",
      "examples": ["Crée un mariage", "Organise un anniversaire", "Planifie une fête"]
    },
    {
      "command": "add_slot",
      "description": "Ajouter un créneau horaire",
      "examples": ["Ajoute un créneau samedi", "Ajoute un créneau demain après-midi"]
    }
  ]
}
```

## Testing Requirements

### Unit Tests (shared)
- VoiceAssistantServiceTest: 8 tests
  - Test intent recognition from transcripts
  - Test parameter extraction (dates, participant counts)
  - Test confidence scoring
  - Test multi-language support
- VoiceSessionRepositoryTest: 5 tests
  - Session lifecycle management
  - Context persistence
- NaturalLanguageParserTest: 6 tests
  - Date parsing (ex: "demain 15h" → "tomorrow 3pm")
  - Number parsing (ex: "vingt participants" → 20)
  - Text normalization

### Integration Tests
- VoiceAssistantIntegrationTest: 5 tests
  - Full workflow: voice command → intent → action → confirmation
  - Fallback to text input if voice fails
  - Context preservation across multi-step sessions
  - Suggestions integration with ML recommendations

### Accessibility Tests
- VoiceOverTest: 3 tests (iOS)
  - Verify screen reader compatibility
  - Verify accessible labels on all UI elements
  - Verify focus order is logical
- TalkBackTest: 3 tests (Android)
  - Same as VoiceOver tests for Android

### Performance Tests
- VoiceCommandLatencyTest: 2 tests
  - Ensure < 300ms for simple commands
  - Ensure < 1s for complex commands

## Implementation Notes

### Speech Recognition Platforms
- **iOS:** Speech Framework (native, offline)
- **Android:** SpeechRecognizer API (native, online)
- **Fallback:** Custom transcription service if needed

### Natural Language Processing
- **Intent Classification:** Use ML model or rule-based matching
- **Entity Extraction:** Regex for dates, numbers, participant counts
- **Context Awareness:** Track current step in multi-step flows

### Integrations
- **SiriKit (iOS):** Custom Intents for voice shortcuts
- **Google Assistant (Android):** Voice Actions for quick commands
- **Shortcuts:** Create home screen shortcuts for common actions

### Accessibility Features
- **Visual Feedback:** Show transcript of what was understood
- **Error Handling:** "Je n'ai pas compris, pouvez-vous répéter ?"
- **Progress Indicators:** "En train de traiter..."
- **Alternative Input:** Always show text input option

### Localization
- **Multi-language:** FR, EN primary, ES, DE secondary
- **Cultural Adaptations:** Date formats, number formats, politeness
- **Audio Prompts:** Native TTS for confirmation messages
