# wakeve-ai Specification

## Purpose
Define Wakeve's AI-assisted planning capabilities, including on-device event draft extraction, provider boundaries, fallback behavior, and future input modality support.
## Requirements
### Requirement: Android On-Device Event Plan Extraction
Wakeve Android MUST provide an event planning AI assistant that extracts a structured `EventPlanDraft` from a natural language text prompt without requiring a network call on the happy path.

#### Scenario: Structured event plan extracted locally
- **WHEN** the user submits `On part à Biarritz du 12 au 15 juillet, 8 personnes, budget 300€ par personne.`
- **THEN** the assistant returns an `EventPlanDraft` with destination `Biarritz`, start date, end date, participant count `8`, budget per person `300`, inferred event type, constraints, and missing information.

#### Scenario: Missing information is surfaced
- **WHEN** the prompt omits fields required for event creation
- **THEN** the assistant returns a draft with known fields populated and missing fields listed in `missingInformation`.

### Requirement: Replaceable AI Provider Port
Wakeve MUST keep event planning AI behind a domain port so UI code and ViewModels do not call ML Kit, Gemini Nano, AICore, or any other provider API directly.

#### Scenario: UI extracts through ViewModel and port
- **WHEN** the Compose AI event planning screen requests extraction
- **THEN** the request flows through a ViewModel and `EventPlanningAiAssistant` implementation rather than calling platform AI APIs from the UI.

### Requirement: Local Fallback for Unsupported Android Devices
Wakeve Android MUST remain usable when Gemini Nano through AICore is unavailable, downloadable, downloading, busy, blocked, or fails to return parseable structured output.

#### Scenario: Unsupported device uses fallback
- **WHEN** the Android ML Kit GenAI provider reports that Gemini Nano is unavailable
- **THEN** Wakeve uses `RuleBasedEventPlanningAiAssistant` to return a best-effort local `EventPlanDraft` and marks the provider as fallback.

### Requirement: Speech-Ready Event Planning Input
Wakeve MUST model AI event planning requests so future speech transcription can pass text into the same extraction port without changing provider interfaces.

#### Scenario: Text input uses modality metadata
- **WHEN** a text prompt is submitted
- **THEN** the request records text modality and the assistant extracts the draft from the normalized prompt.

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

