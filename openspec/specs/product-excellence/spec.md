# product-excellence Specification

## Purpose
Define Wakeve's cross-cutting product excellence guardrails. This capability keeps Wakeve focused on private event organization, mental-load reduction, state clarity, reviewable AI assistance, mobile-first premium execution, and event-scoped collaboration.
## Requirements
### Requirement: Event Organization Product Boundary
Wakeve MUST only add user-facing capabilities that directly help a private group prepare, decide, coordinate, or complete an event. Wakeve MUST NOT add generic social networking, generic chat, generic task management, generic calendar management, or generic notes/workspace behavior unless the behavior is explicitly scoped to an event organization decision, artifact, or workflow state.

#### Scenario: Generic social feed is rejected
- **GIVEN** a proposal adds a feed of user posts, reactions, or friend activity
- **AND** the feed is not tied to an event decision, invitation, preparation item, logistics section, or post-event artifact explicitly required for event organization
- **WHEN** the proposal is reviewed against product excellence
- **THEN** Wakeve rejects or rescopes the feature before implementation.

#### Scenario: Event-scoped feature is allowed
- **GIVEN** a proposal adds a way for participants to compare lodging options for a confirmed weekend
- **WHEN** the proposal is reviewed against product excellence
- **THEN** Wakeve may accept the feature because it helps the group make an event decision and reduces coordination outside Wakeve.

### Requirement: Event State Confidence
Every primary event screen MUST make the event state clear without explanation by showing what is confirmed, what is pending, who needs to act, and the next useful action. Screens MUST avoid ambiguous states that force organizers or participants to infer whether a decision has been made or whether work remains.

#### Scenario: Event screen exposes decision state
- **GIVEN** a user opens an event screen after participants have voted
- **WHEN** the screen renders
- **THEN** it clearly identifies the recommended or confirmed date state
- **AND** it shows pending participants or missing responses when relevant
- **AND** it exposes the next useful action for the current user.

#### Scenario: Pending sync is not shown as confirmed
- **GIVEN** a participant records a logistics update while offline
- **WHEN** the relevant event section renders before sync completes
- **THEN** Wakeve shows the update as pending local work
- **AND** it does not imply that the server or other participants have confirmed the update.

### Requirement: AI Copilot Reviewability
Wakeve AI-assisted experiences MUST behave as an event-organization copilot that reduces user effort through reviewable suggestions, summaries, drafts, and recommendations. AI output MUST NOT create, send, invite, confirm, modify, delete, or persist user-visible event work without an explicit user action. User-facing AI labels MUST describe the benefit rather than exposing technical labels such as "Assistant IA", "Generate", "Automate", or "Smart".

#### Scenario: AI draft remains reviewable
- **WHEN** Wakeve extracts an event plan from natural language
- **THEN** it presents a reviewable draft with known fields and missing information
- **AND** no event is created or modified until the user explicitly applies the draft.

#### Scenario: AI suggestion does not send automatically
- **GIVEN** Wakeve prepares invitation message suggestions from event context
- **WHEN** suggestions are displayed
- **THEN** the user can review, edit, choose, copy, or send a suggestion through an explicit action
- **AND** Wakeve does not send the invitation automatically.

### Requirement: Mobile-First Premium Interaction
Wakeve MUST optimize primary event organization flows for mobile completion in a few clear steps. Each primary screen MUST have one clear intention, a visible primary action, compact and understandable copy, stable layouts across mobile sizes, accessible controls, and purposeful visual design. Premium feel MUST come from clarity, speed, state confidence, motion restraint, platform-native behavior, and event-specific polish rather than decorative complexity.

#### Scenario: Mobile user understands the next action
- **GIVEN** a user opens a primary Wakeve event flow on a phone
- **WHEN** the screen renders
- **THEN** the primary action is visible without reading instructions
- **AND** secondary actions are progressively disclosed
- **AND** controls and text remain usable with platform accessibility settings.

#### Scenario: Feature review rejects unnecessary steps
- **GIVEN** a proposed flow requires extra taps or screens before the user can complete a primary event action
- **WHEN** the proposal is reviewed against product excellence
- **THEN** the proposal must justify each step as necessary for trust, safety, comprehension, or event organization
- **AND** unnecessary steps are removed before implementation.

### Requirement: Event-Scoped Collaboration
Wakeve collaboration MUST stay scoped to event preparation, decisions, logistics, questions, and artifacts. Wakeve MUST NOT become a general-purpose chat replacement; collaboration surfaces MUST preserve context by linking messages or comments to event sections, participants, decisions, or organization work.

#### Scenario: Generic chat is rejected
- **GIVEN** a proposal adds open-ended direct messaging unrelated to an event
- **WHEN** the proposal is reviewed against product excellence
- **THEN** Wakeve rejects or rescopes the feature because it does not directly organize an event.

#### Scenario: Section collaboration is accepted
- **GIVEN** participants need to discuss a transport plan, budget expense, lodging option, or poll decision
- **WHEN** Wakeve provides comments or messages scoped to that section or item
- **THEN** the collaboration remains tied to event organization context
- **AND** the surface helps reduce external back-and-forth in messaging apps.

### Requirement: Product Excellence Proposal Gate
Future significant OpenSpec proposals for user-facing features MUST state how the change satisfies product excellence. The review MUST cover event relevance, mental-load reduction, speed, state clarity, mobile usability, premium execution, AI reviewability when applicable, and avoidance of generic product drift.

#### Scenario: Future proposal includes product fit
- **GIVEN** a future OpenSpec proposal adds a user-facing event capability
- **WHEN** the proposal is prepared
- **THEN** it includes acceptance criteria or design notes explaining how the feature helps a group prepare an event
- **AND** it identifies the confirmed state, pending state, responsible actor, and next useful action when the feature affects event workflow.

#### Scenario: Proposal fails the gate
- **GIVEN** a future proposal adds a capability that does not reduce event organization effort or improve event decisions
- **WHEN** the proposal is reviewed against product excellence
- **THEN** the proposal is rejected, deferred, or rewritten before implementation begins.

### Requirement: Deterministic AI Product Gate
User-facing AI proposals SHALL explain how AI remains reviewable, event-scoped, metadata-backed, and subordinate to deterministic workflow state. Proposals SHALL identify which deterministic model, state machine, validator, or use case owns any eventual state change.

#### Scenario: Future AI proposal changes event data
- **GIVEN** a future proposal adds AI-assisted event updates
- **WHEN** the proposal is reviewed against product excellence
- **THEN** it identifies the deterministic owner that applies the update after user confirmation
- **AND** it describes metadata, validation, rejected-output behavior, and fallback behavior when AI is unavailable.

#### Scenario: AI would make an irreversible decision
- **GIVEN** a proposed feature allows AI to send invitations, confirm payments, finalize events, modify permissions, or resolve sync conflicts without explicit user confirmation and deterministic guards
- **WHEN** the proposal is reviewed
- **THEN** Wakeve rejects or rescopes the feature before implementation.

