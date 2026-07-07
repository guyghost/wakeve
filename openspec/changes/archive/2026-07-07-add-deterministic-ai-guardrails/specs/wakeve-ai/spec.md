## ADDED Requirements

### Requirement: Deterministic AI Boundary
Wakeve AI outputs SHALL be treated as edge-generated proposals, drafts, summaries, recommendations, or explanations. AI providers SHALL NOT decide or directly mutate permissions, authentication state, event lifecycle state, invitations, payments, quotas, synchronization, offline conflict resolution, notifications, or persistence.

#### Scenario: AI draft proposes event fields
- **WHEN** Wakeve extracts an event draft from natural language
- **THEN** the result is exposed as a reviewable draft
- **AND** no event is created, updated, invited, confirmed, paid, synced, or notified until an existing deterministic state machine or use case accepts an explicit user action.

#### Scenario: AI provider is unavailable
- **WHEN** every AI model provider is unavailable
- **THEN** Wakeve remains usable through deterministic manual flows and local fallbacks
- **AND** business state transitions continue to run without AI.

### Requirement: AI Interaction Metadata
Every AI output that can influence user-visible event work SHALL expose interaction metadata including use case, provider route, sanitized input summary, sanitized output summary, confidence, reasoning summary, latency, validation result, and provider cost or usage when available.

#### Scenario: Generated message is shown for review
- **WHEN** Wakeve generates an organizer message suggestion
- **THEN** the generated message includes AI interaction metadata
- **AND** the metadata excludes raw prompt text, participant contact details, votes, addresses, prices, and generated personal content from production telemetry.

#### Scenario: AI output fails deterministic validation
- **WHEN** validation rejects generated output because it invents business facts or violates output bounds
- **THEN** the metadata records the rejected validation result and issues
- **AND** Wakeve does not expose an apply action for that output.
