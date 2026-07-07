## ADDED Requirements

### Requirement: Smart Event Draft Creation
Wakeve SHALL allow an organizer to create an event draft from a free-form phrase when on-device WakeveAI is available.

The generated draft SHALL prefill reviewable fields such as title, subtitle, description, destination hint, date hints, participant hints, suggested polls, checklist items, and transport hints. The organizer SHALL review and explicitly apply the draft before any event is created or modified.

#### Scenario: Organizer creates an event with a phrase
- **GIVEN** on-device WakeveAI is available
- **WHEN** the organizer enters “Anniversaire d’Emma samedi soir”
- **THEN** Wakeve displays a progressively generated draft with short structured suggestions
- **AND** the organizer can modify, apply, or ignore the draft
- **AND** no event is persisted until the organizer explicitly confirms the existing create-event flow.

#### Scenario: Smart draft is unavailable
- **GIVEN** WakeveAI is unavailable
- **WHEN** the organizer opens event creation
- **THEN** the manual create-event flow remains available
- **AND** the organizer can create the same event without AI assistance.

### Requirement: AI-Assisted Poll Suggestions
Wakeve SHALL offer up to 3 AI-assisted poll suggestions for an event when enough event context exists.

Poll suggestions SHALL be reviewable and editable. Wakeve SHALL NOT create a poll automatically from a generated suggestion.

#### Scenario: Organizer applies a poll suggestion
- **GIVEN** an event draft has a destination hint and participant estimate
- **WHEN** WakeveAI suggests “Quelle date convient le mieux ?”
- **THEN** the suggestion is labeled as a suggestion
- **AND** the organizer can edit and apply it to the poll creation UI
- **AND** the poll is not created until the organizer confirms it.

### Requirement: AI-Assisted Event Checklist
Wakeve SHALL offer short AI-assisted checklist suggestions based on event type, location hints, participant context, and current organization state.

Checklist suggestions SHALL be grouped by practical categories and SHALL require explicit user action before persistence.

#### Scenario: Organizer reviews checklist suggestions
- **GIVEN** an event is a beach party with a destination hint
- **WHEN** WakeveAI generates checklist suggestions
- **THEN** Wakeve shows concise items for categories such as food, transport, venue, guests, equipment, and budget
- **AND** the organizer can modify, apply, or ignore each suggested item.

### Requirement: AI-Assisted Invitation Messages
Wakeve SHALL generate reviewable invitation message variants from existing event context.

Wakeve SHALL provide simple, warm, and short WhatsApp-style variants. Wakeve SHALL NOT send invitations or messages automatically.

#### Scenario: Organizer uses a generated invitation
- **GIVEN** an event has a title and date hint
- **WHEN** WakeveAI generates invitation messages
- **THEN** Wakeve shows three editable variants
- **AND** choosing one only places it into a composer or share flow
- **AND** the organizer must explicitly send it.

### Requirement: AI-Assisted Event Summary
Wakeve SHALL generate an event summary from real event facts when useful organization state exists.

The summary SHALL clearly separate what is decided, what is missing, and the recommended next action. Any vote, participant, message, transport, or task facts SHALL come from Wakeve data or tools.

#### Scenario: Organizer views missing work
- **GIVEN** an event has participants, votes, and incomplete transport data
- **WHEN** WakeveAI generates an event summary
- **THEN** the summary identifies decided items, missing items, and one recommended next action
- **AND** the summary does not invent participant availability or vote results.
