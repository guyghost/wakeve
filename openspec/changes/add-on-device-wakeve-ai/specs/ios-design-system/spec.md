## ADDED Requirements

### Requirement: Contextual iOS AI Assistance UX
The iOS application SHALL present WakeveAI features as contextual assistance, not as a generic chatbot.

AI entry points SHALL appear only where they reduce planning effort, SHALL be hidden or subtle when unavailable, and SHALL use Wakeve's native iOS design system. Generated content SHALL be labeled as a suggestion and paired with explicit modify, apply, and ignore actions.

#### Scenario: Smart Event Draft appears in Create Event
- **GIVEN** WakeveAI is available and the user opens Create Event
- **WHEN** the user focuses the free-form event description entry point
- **THEN** the UI shows a calm native generation state with stable layout
- **AND** streamed sections appear progressively without blocking manual editing
- **AND** the final draft exposes “Modifier”, “Appliquer”, and “Ignorer” actions.

### Requirement: iOS AI Availability Fallback UX
The iOS application SHALL provide accessible fallbacks for unavailable WakeveAI features.

Fallbacks SHALL preserve the primary manual task, avoid alarming language, and respect Dynamic Type, Reduce Motion, Reduce Transparency, VoiceOver, and existing Liquid Glass rules.

#### Scenario: Model is not ready
- **GIVEN** Foundation Models are not ready
- **WHEN** the user enters an AI-capable surface
- **THEN** the UI either hides the AI entry point or shows a discreet unavailable state
- **AND** the primary manual workflow remains visually dominant and fully accessible.
