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
