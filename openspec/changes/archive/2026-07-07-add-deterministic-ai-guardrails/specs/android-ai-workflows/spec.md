## ADDED Requirements

### Requirement: Shared AI Metadata Contract
Android AI workflows SHALL attach shared `AiInteractionMetadata` to event drafts, event summaries, organizer messages, and planning-agent sessions while preserving existing `AiRoutingMetadata` compatibility.

#### Scenario: Android summary uses on-device model
- **GIVEN** the on-device model returns an event summary
- **WHEN** Wakeve exposes the summary to the ViewModel
- **THEN** the summary includes routing metadata and AI interaction metadata
- **AND** the metadata records local inference, latency, accepted or needs-review validation, and sanitized summaries.

#### Scenario: Organizer message uses cloud fallback
- **GIVEN** cloud fallback is explicitly allowed for organizer messages
- **WHEN** Wakeve routes generation to Firebase AI Logic
- **THEN** the generated message metadata records cloud routing and provider name
- **AND** the UI can disclose the route without logging raw event context.

### Requirement: Review-Only Planning Agent Events
Planning-agent events SHALL remain review-only proposals unless an explicit user confirmation is routed through deterministic application code.

#### Scenario: Planning agent suggests budget categories
- **WHEN** a planning-agent event suggests budget categories
- **THEN** Wakeve renders the suggestion as pending review
- **AND** budget state is not persisted until a user action is processed by existing deterministic budget or workflow code.
