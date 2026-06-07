## ADDED Requirements

### Requirement: WakeveAI Module Isolation
Wakeve SHALL provide an isolated iOS WakeveAI module for on-device planning assistance using Apple Foundation Models.

The module SHALL contain availability, client, prompt, tool, generator, validation, instrumentation, and test boundaries. SwiftUI views SHALL NOT call Foundation Models directly; views SHALL observe ViewModels, and ViewModels SHALL call WakeveAI services.

#### Scenario: SwiftUI uses WakeveAI through a ViewModel
- **GIVEN** a SwiftUI create-event screen exposes Smart Event Draft
- **WHEN** the user enters a free-form event phrase
- **THEN** the screen dispatches the request to its ViewModel
- **AND** the ViewModel calls a WakeveAI generator
- **AND** the view receives typed state updates without owning a Foundation Models session.

### Requirement: Foundation Models Availability Fallback
WakeveAI SHALL map `SystemLanguageModel.default.availability` into app states and keep all affected workflows usable without the model.

If the model is available, contextual AI entry points MAY be shown. If Apple Intelligence is disabled, the model is not ready, the device is incompatible, or availability is unknown, the app SHALL hide AI entry points or show a discreet fallback while preserving manual event creation, polls, checklists, invitations, summaries, and transport planning.

#### Scenario: Apple Intelligence is unavailable
- **GIVEN** the user opens Create Event on a device where Foundation Models are unavailable
- **WHEN** Wakeve checks model availability
- **THEN** Smart Event Draft is hidden or replaced by a discreet fallback message
- **AND** the normal manual create-event flow remains fully usable.

### Requirement: Typed Generable Output
WakeveAI SHALL use typed Swift structures for model outputs rather than free-form JSON.

The initial typed outputs SHALL include `EventDraft`, `DateOption`, `PollSuggestion`, `ChecklistItem`, `TransportHint`, invitation messages, event summaries, and transport coordination suggestions. Generated arrays SHALL be capped to 3 items per category and every output SHALL be validated before it reaches an apply action.

#### Scenario: Generated event draft is bounded and valid
- **GIVEN** the user writes “Week-end à Marrakech avec 8 amis début juillet”
- **WHEN** WakeveAI generates an `EventDraft`
- **THEN** the result contains short typed fields for title, subtitle, description, destination hint, date hints, participant hints, poll suggestions, checklist, transport hints, and rationale
- **AND** no generated category contains more than 3 options
- **AND** vague dates remain hints rather than invented exact dates.

### Requirement: Versioned Specialized Prompts
WakeveAI SHALL use short, specialized, versioned prompts per use case.

The system SHALL provide at least `event_draft_v1`, `poll_suggestions_v1`, `checklist_v1`, `invitation_message_v1`, `event_summary_v1`, and `transport_suggestions_v1`. A universal prompt SHALL NOT be used for all use cases.

#### Scenario: Poll suggestions use their own prompt
- **GIVEN** an event exists with title, type, participants, and proposed date hints
- **WHEN** the user requests poll suggestions
- **THEN** WakeveAI uses the poll-suggestions prompt version
- **AND** the prompt does not include unrelated checklist, invitation, or transport instructions.

### Requirement: Tool-Grounded Business Facts
WakeveAI SHALL use internal read-only tools for Wakeve business facts and SHALL NOT invent participants, votes, availability, addresses, prices, or existing transport data.

The initial tools SHALL include `GetCurrentGroupTool`, `GetEventContextTool`, `GetParticipantStatusesTool`, `GetVoteResultsTool`, `GetTransportContextTool`, and `GetUserPreferencesTool`. Tool responses SHALL be scoped to data the current user is authorized to view.

#### Scenario: Event summary requires vote facts
- **GIVEN** the user asks for an event summary that includes vote status
- **WHEN** WakeveAI needs vote results
- **THEN** it obtains vote facts from `GetVoteResultsTool`
- **AND** any summary mentioning votes only uses facts returned by the tool
- **AND** invented vote results are rejected by validation.

### Requirement: Streaming and Cancellation
WakeveAI SHALL support streaming output, cancellation, timeout, and stable UI state for long-running generations.

Streaming SHALL be used for complete event drafts, weekend planning, checklists, event summaries, and transport or lodging coordination suggestions. The UI SHALL show progressive sections without blocking the whole screen and SHALL allow cancellation.

#### Scenario: Smart Event Draft streams sections
- **GIVEN** the user generates an event from a free-form phrase
- **WHEN** WakeveAI streams an `EventDraft`
- **THEN** the ViewModel exposes sections as they become available
- **AND** the UI can show title, description, date options, checklist, and poll suggestions progressively
- **AND** cancellation stops generation and returns the user to an editable state.

### Requirement: User Confirmation Guardrails
WakeveAI SHALL never apply an AI suggestion automatically.

Every generated result that can affect event data, polls, checklists, invitations, summaries, messages, participants, or transport plans SHALL be labeled as a suggestion and SHALL require explicit user confirmation. The UI SHALL provide explicit modify, apply, and ignore actions where a suggestion can be used.

#### Scenario: Invitation message is generated
- **GIVEN** WakeveAI generates invitation message variants
- **WHEN** the user views the variants
- **THEN** no message is sent automatically
- **AND** the user can edit, apply to a composer, or ignore a variant.

### Requirement: Privacy and Performance Controls
WakeveAI SHALL preserve local privacy and collect only non-personal performance metrics.

The system SHALL NOT send event data to a server for these AI features, SHALL NOT log personal prompt context in production, SHALL NOT store AI output without explicit user action, and SHALL sanitize model context. The model SHALL NOT be loaded at app launch and SHALL be prepared only after strong user intent.

#### Scenario: Production logging is enabled
- **GIVEN** a production build generates an event draft
- **WHEN** WakeveAI records diagnostics
- **THEN** diagnostics include non-personal timing, availability, timeout, cancellation, and validation status
- **AND** diagnostics do not include prompt text, participant names, votes, addresses, prices, or generated personal content.

### Requirement: WakeveAI Test Coverage
WakeveAI SHALL include tests for typed output validation, fallback behavior, prompts, tools, multilingual fixtures, no fact invention, bounded suggestion counts, streaming state, cancellation, and timeout.

Fixtures SHALL include beach party, child birthday, friends weekend, family trip, simple dinner, and road trip examples in French and English where relevant.

#### Scenario: Tool context omits participants
- **GIVEN** a fake model output names a participant absent from tool context and user input
- **WHEN** WakeveAI validation runs
- **THEN** the output is rejected or downgraded to an uncertain hint
- **AND** no apply action can persist that invented participant as a fact.
