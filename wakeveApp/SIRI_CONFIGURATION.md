# SiriKit Configuration for Wakeve

## Overview

This document describes the SiriKit configuration implemented for Wakeve's intelligent voice assistant, enabling users to create and manage events using voice commands through Siri on iOS 16+.

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      User Interaction                         │
│         (Voice: "Crée un mariage pour juin")                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                     SiriKit Framework                         │
│         (Speech Recognition + Intent Parsing)                 │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                Wakeve Intents Extension                       │
│                   (IntentHandler.swift)                       │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   WakeveVoiceService                          │
│            (Business Logic Bridge to Shared)                  │
└─────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────┐
│                   Shared Kotlin Code                          │
│              (EventRepository, PollService)                   │
└─────────────────────────────────────────────────────────────┘
```

## Supported Intents

### 1. WakeveCreateEventIntent
Create new events via voice commands.

**Parameters:**
- `eventTitle` (required): Title of the event
- `eventDescription` (optional): Event description
- `eventDate` (required): Date and time of the event
- `participantCount` (optional): Estimated number of participants
- `eventType` (optional): Type of event (WEDDING, BIRTHDAY, etc.)

**Example phrases:**
- "Crée un mariage pour juin"
- "Organise un anniversaire le 15 juillet"
- "Planifie une fête samedi prochain"

### 2. WakeveAddPollSlotIntent
Add time slots to event polls.

**Parameters:**
- `slotDate` (required): Date for the slot
- `timeOfDay` (optional): MORNING, AFTERNOON, EVENING, ALL_DAY
- `event` (required): Event identifier

**Example phrases:**
- "Ajoute un créneau samedi après-midi"
- "Propose dimanche matin"

### 3. WakeveConfirmPollDateIntent
Confirm the final date of a poll.

**Parameters:**
- `slotToConfirm` (required): Slot to confirm
- `event` (required): Event identifier

**Example phrases:**
- "Valide la date du 15 juin"
- "Confirme le créneau de samedi"

### 4. WakeveSendInvitationsIntent
Send invitations for an event.

**Parameters:**
- `event` (required): Event identifier

**Example phrases:**
- "Envoie les invitations maintenant"
- "Envoie les invitations pour le mariage"

### 5. WakeveOpenCalendarIntent
Open the Wakeve calendar.

**Example phrases:**
- "Ouvre le calendrier Wakeve"
- "Affiche mes événements"

### 6. WakeveCancelEventIntent
Cancel an event.

**Parameters:**
- `event` (optional): Event identifier (uses last event if not provided)

**Example phrases:**
- "Annule le dernier événement"
- "Annule le mariage de samedi"

### 7. WakeveGetStatsIntent
Get statistics about events.

**Parameters:**
- `statsType` (optional): TOTAL, UPCOMING, PAST

**Example phrases:**
- "Combien d'événements ai-je créés ?"
- "Mes événements à venir"

### 8. WakeveAddReminderIntent
Add a reminder for an event.

**Parameters:**
- `reminderDate` (required): When to remind
- `event` (optional): Event identifier

**Example phrases:**
- "Rappelle-moi de vérifier les votes demain"
- "Rappelle-moi de confirmer la date samedi"

## File Structure

```
iosApp/iosApp/Siri/
├── WakeveIntents.intentdefinition     # Intent definitions
├── WakeveIntentHandler.swift          # Main intent handler
├── WakeveVoiceService.swift           # Voice service bridge
├── WakeveSiriManager.swift            # Siri shortcuts manager
├── WakeveVocabulary.plist             # Custom vocabulary
├── Localizable.strings                # Localized strings
└── WakeveIntentsExtension/
    ├── IntentHandler.swift            # Extension handler
    └── Info.plist                     # Extension configuration
└── WakeveIntentExtensionUI/
    ├── IntentViewController.swift     # Custom UI controller
    ├── MainInterface.storyboard       # UI storyboard
    └── Info.plist                     # UI extension configuration
```

## Configuration Requirements

### Info.plist Keys

```xml
<key>INIntentsSupported</key>
<array>
    <string>WakeveCreateEventIntent</string>
    <string>WakeveAddPollSlotIntent</string>
    <string>WakeveConfirmPollDateIntent</string>
    <string>WakeveSendInvitationsIntent</string>
    <string>WakeveOpenCalendarIntent</string>
    <string>WakeveCancelEventIntent</string>
    <string>WakeveGetStatsIntent</string>
    <string>WakeveAddReminderIntent</string>
</array>

<key>NSSpeechRecognitionUsageDescription</key>
<string>Wakeve utilise la reconnaissance vocale pour vous permettre 
de créer et gérer des événements en utilisant votre voix.</string>
```

### App Group Configuration

For data sharing between the main app and intent extensions:

```xml
<key>com.apple.security.application-groups</key>
<array>
    <string>group.com.guyghost.wakeve</string>
</array>
```

## Language Support

| Language | Code | Status |
|----------|------|--------|
| French | fr | ✅ Complete |
| English | en | ✅ Complete |
| Spanish | es | ✅ Complete |
| German | de | ✅ Complete |

## Custom Vocabulary

The vocabulary plist includes:

### Event Types
- Mariage/Wedding/Boda
- Anniversaire/Birthday/Cumpleaños
- Fête/Party/Fiesta
- Conférence/Conference
- Team building

### Time of Day
- Matin/Morning/Mañana/Vormittag
- Après-midi/Afternoon/Tarde/Nachmittag
- Soir/Evening/Noche/Abend
- Toute la journée/All day

### Date References
- Aujourd'hui/Today/Hoy/Heute
- Demain/Tomorrow/Mañana/Morgen
- Ce week-end/This weekend/Este fin de semana/Dieses Wochenende

## Implementation Notes

### Voice Command Processing Flow

1. **Speech Recognition**: Siri converts speech to text
2. **Intent Classification**: Match text to Wakeve intents
3. **Parameter Extraction**: Extract entities (dates, counts, etc.)
4. **Context Awareness**: Maintain conversation context
5. **Service Invocation**: Call appropriate Wakeve service
6. **Response Generation**: Generate confirmation message
7. **UI Update**: Show results in Siri UI

### Error Handling

- **Missing required parameters**: Siri prompts for missing info
- **Authentication required**: Prompt user to authorize
- **Event not found**: Inform user and suggest alternatives
- **Network unavailable**: Queue action for later sync

### Offline Support

- Voice recognition works offline on iOS
- Intent processing cached locally
- Actions queued when offline, synced when online

## Testing

### Test Commands

```bash
# Create event
"Hey Siri, crée un mariage avec Wakeve"
"Hey Siri, organize a birthday with Wakeve"

# Add poll slot
"Hey Siri, ajoute un créneau samedi après-midi sur Wakeve"
"Hey Siri, add a time slot with Wakeve"

# Send invitations
"Hey Siri, envoie les invitations avec Wakeve"
```

### Siri Simulator Testing

1. Enable Siri in Settings > Siri & Search
2. Enable "Hey Siri" if on supported device
3. Test with voice commands
4. Check Shortcuts app for donated shortcuts

## Performance Requirements

- Intent handling: < 500ms
- Voice recognition: < 300ms for short commands
- Memory usage: < 50MB for extension

## Security Considerations

- Siri data processed on-device when possible
- No audio data sent to external servers
- User consent required for speech recognition
- App Group for secure data sharing

## Future Enhancements

- [ ] Add custom synonyms for event types
- [ ] Implement multi-turn conversation
- [ ] Add voice biometric authentication
- [ ] Support for complex date expressions
- [ ] Integration with Shortcuts app
- [ ] Donate shortcuts for personalization
