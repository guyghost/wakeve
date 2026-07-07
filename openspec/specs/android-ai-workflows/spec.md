# android-ai-workflows Specification

## Purpose
Define Android AI workflow capabilities for local event summaries, organizer message generation, planning-agent session boundaries, privacy-preserving routing, and test coverage.
## Requirements
### Requirement: Android On-Device Event Summary
Wakeve Android MUST provide an event summary assistant that generates an `EventAiSummary` from `EventPlanningPromptContext` through `EventSummaryAiAssistant` without using a network call on the happy path.

The assistant MUST use a short prompt optimized for local Gemini Nano generation through ML Kit GenAI Prompt API when available. The summary output MUST include a short event summary, preparation advice, packing checklist, and missing information.

#### Scenario: Event summary generated locally
- **GIVEN** an event context with title, destination, dates, participants, activities, constraints, and budget
- **WHEN** the organizer requests an event summary on a device where Gemini Nano is available
- **THEN** Wakeve generates an `EventAiSummary` through `EventSummaryAiAssistant`
- **AND** the route is local on-device inference
- **AND** no event context is sent to a cloud provider.

#### Scenario: On-device summary model unavailable
- **GIVEN** an event context with user-sensitive details
- **WHEN** Gemini Nano is unavailable, downloading, blocked, or fails
- **THEN** Wakeve does not send the event context to a cloud provider for summary generation
- **AND** the assistant returns an unavailable or local fallback state that the UI can render without breaking the event screen.

### Requirement: Hybrid Organizer Message Generation
Wakeve Android MUST provide an organizer message assistant that generates `GeneratedOrganizerMessage` values for invitation messages, reminder messages, RSVP follow-ups, budget reminders, and logistics updates.

The assistant MUST use `OrganizerMessageAiAssistant` and MUST prefer on-device Gemini Nano inference when available. If the local model is unavailable and the selected provider permits cloud fallback, the assistant MAY route to Gemini Flash Lite through Firebase AI Logic and MUST expose the routing decision internally for logging, debugging, and UI transparency.

#### Scenario: Message generated on device
- **GIVEN** the organizer requests an invitation message
- **AND** Gemini Nano is available
- **WHEN** Wakeve generates the message
- **THEN** `HybridOrganizerMessageAiAssistant` uses the on-device provider
- **AND** the returned `GeneratedOrganizerMessage` records local routing
- **AND** no cloud provider receives the event context.

#### Scenario: Message falls back to cloud
- **GIVEN** the organizer requests a budget reminder
- **AND** Gemini Nano is unavailable
- **AND** the hybrid provider is configured to allow cloud fallback
- **WHEN** Wakeve generates the message
- **THEN** `HybridOrganizerMessageAiAssistant` routes to Gemini Flash Lite through Firebase AI Logic
- **AND** the returned `GeneratedOrganizerMessage` records cloud routing and provider name
- **AND** the ViewModel exposes that route so the UI can explain that cloud generation was used.

### Requirement: Planning Agent Client Boundary
Wakeve Android MUST model complex planning assistance through `PlanningAgentClient` instead of executing autonomous planning logic directly in the app.

The client boundary MUST model `PlanningAgentSession` state and `PlanningAgentEvent` streams for plan suggestions, participant task splits, budget categories, missing logistics, progress updates, errors, completion, and user confirmation requests. The initial implementation MUST be a fake local client and MUST NOT implement an ADK backend or A2UI runtime.

#### Scenario: Fake planning agent emits progress
- **GIVEN** an organizer starts a planning-agent demo session
- **WHEN** `FakePlanningAgentClient` runs
- **THEN** Wakeve receives a `PlanningAgentSession` and a stream of `PlanningAgentEvent` values
- **AND** the stream includes progress, suggested plan, task split, budget categories, missing logistics, and completion events.

#### Scenario: Agent requests user confirmation
- **GIVEN** the fake planning agent identifies an action that requires organizer approval
- **WHEN** the event stream emits a confirmation request
- **THEN** the Compose screen renders the requested action with explicit confirmation and dismissal controls
- **AND** no task, budget, message, or event data is applied automatically.

### Requirement: AI Routing and Privacy Isolation
Wakeve Android MUST keep AI routing outside composables and MUST avoid logging or sending sensitive event data except through the explicitly selected provider route.

Composable functions MUST render ViewModel state and dispatch user actions only. AI provider availability, prompt creation, routing, fallback, privacy metadata, and provider diagnostics MUST live behind domain ports and implementation classes.

#### Scenario: UI requests AI through ViewModel
- **GIVEN** the AI workflow demo screen is displayed
- **WHEN** the user generates a summary, message, or planning-agent session
- **THEN** the screen dispatches the action to a ViewModel
- **AND** the ViewModel calls domain ports
- **AND** the composable does not call ML Kit, Firebase AI Logic, Gemini, or backend agent SDK APIs directly.

### Requirement: Android AI Workflow Test Coverage
Wakeve MUST include tests proving Android AI workflows work across local success, hybrid fallback, unavailable local model, and planning-agent event rendering.

Tests MUST use fake providers and MUST verify that summary generation does not call cloud fallback, organizer message routing remains isolated from UI, and fake planning-agent confirmation requests are renderable by ViewModel or UI projection state.

#### Scenario: Focused AI tests run without real AI SDKs
- **GIVEN** fake local, cloud, and unavailable providers
- **WHEN** shared and Compose AI workflow tests run
- **THEN** they validate success, fallback, unavailable model, and agent event rendering without requiring ML Kit GenAI, Firebase AI Logic, Gemini Nano, or a backend agent.

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

