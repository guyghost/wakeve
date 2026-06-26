## ADDED Requirements

### Requirement: Wakeve SHALL expose core event workflows through modern iOS App Intents
Wakeve SHALL provide Swift `AppIntent` types for high-value event planning actions that can be invoked from Siri, Shortcuts, Spotlight, widgets, and related system surfaces.

The App Intents surface SHALL include:
- `CreateEventIntent`
- `UpdateEventIntent`
- `InviteParticipantsIntent`
- `CreatePollIntent`
- `VoteIntent`
- `ProposeTransportIntent`
- `OpenEventIntent`
- `SummarizeEventIntent`

Each intent SHALL define user-facing metadata, typed parameters, predictable result values, and failure behavior that is understandable outside the main app UI.

#### Scenario: User creates an event from a system surface
- **GIVEN** the user invokes `CreateEventIntent` with a title, date, optional location, optional group, and optional notes
- **WHEN** the intent runs successfully
- **THEN** Wakeve SHALL create an event
- **AND** the intent SHALL return an `EventEntity` representing the created event.

#### Scenario: User opens a specific event from a system surface
- **GIVEN** the user selects an `EventEntity`
- **WHEN** `OpenEventIntent` runs
- **THEN** Wakeve SHALL open the app
- **AND** Wakeve SHALL route to the selected event detail view.

#### Scenario: User asks for an event summary
- **GIVEN** the user selects an existing `EventEntity`
- **WHEN** `SummarizeEventIntent` runs
- **THEN** Wakeve SHALL return a concise summary of the event's title, date, location, status, participant count, active poll state, and transport readiness when available.

### Requirement: Wakeve App Intent entities SHALL be stable, displayable, and queryable
Wakeve SHALL expose focused `AppEntity` types for objects the system needs to identify or disambiguate.

The entity surface SHALL include:
- `EventEntity` with id, title, date, location, status, and participants count
- `GroupEntity` with id, name, and members count
- `ParticipantEntity` with id, display name, and status
- `PollEntity` with id, question, options, and status
- `TransportEntity` with id, event id, driver, departure, and seats

Each entity SHALL have a stable identifier, `displayRepresentation`, `typeDisplayRepresentation`, and an appropriate `EntityQuery`. Suggested entities SHALL be provided where they improve picker, widget, or shortcut configuration UX.

#### Scenario: System resolves an event by stable identifier
- **GIVEN** an event exists with a stable identifier
- **WHEN** `EventEntityQuery` resolves that identifier
- **THEN** it SHALL return the matching `EventEntity`
- **AND** the display representation SHALL include a clear event title.

#### Scenario: System searches entities with user-friendly text matching
- **GIVEN** Wakeve contains events named "Anniversaire Emma" and "Week-end Famille"
- **WHEN** a system surface searches event entities with partial, case-insensitive, or accent-insensitive text
- **THEN** matching event entities SHALL be returned
- **AND** unrelated entities SHALL NOT be returned.

#### Scenario: System requests suggested entities
- **GIVEN** Wakeve has recent or active events, groups, polls, or transport proposals
- **WHEN** a matching entity query requests suggestions
- **THEN** Wakeve SHALL return relevant, displayable suggestions ordered for user selection.

### Requirement: Wakeve SHALL make App Intents discoverable through system metadata
Wakeve SHALL provide App Shortcut definitions and metadata so supported App Intents are discoverable in Siri, Shortcuts, Spotlight, widgets, and view annotations.

Shortcut phrases SHALL be direct, task-oriented, and Wakeve-specific. Metadata SHALL use clear localized titles, descriptions, symbols, and parameter labels.

#### Scenario: User browses Wakeve actions in Shortcuts
- **WHEN** the system displays Wakeve actions in Shortcuts
- **THEN** the supported App Intents SHALL appear with clear titles, descriptions, symbols, and parameter prompts
- **AND** the phrases SHALL describe concrete Wakeve tasks rather than generic navigation.

#### Scenario: Widget or view annotation needs an event picker
- **WHEN** a widget configuration or view annotation needs an event parameter
- **THEN** it SHALL use the same `EventEntity` and entity query behavior as the App Intents surface.

#### Scenario: Spotlight searches Wakeve events
- **GIVEN** Wakeve contains active event entities
- **WHEN** Spotlight or an App Intents test performs an event entity spotlight query
- **THEN** Wakeve SHALL return relevant event entities for matching queries
- **AND** Wakeve SHALL return no unrelated event entities for non-matching queries.

#### Scenario: System requests view annotations
- **GIVEN** Wakeve has selected or visible event entities in a supported view
- **WHEN** the system or an App Intents test requests view annotations
- **THEN** Wakeve SHALL expose annotations that identify the related `EventEntity`
- **AND** selected state SHALL be represented when applicable.

### Requirement: Wakeve SHALL validate App Intents with out-of-process AppIntentsTesting tests
Wakeve SHALL provide a dedicated XCUI test target named `WakeveAppIntentsTests` that uses `AppIntentsTesting` to exercise App Intents, entities, queries, and shortcut-facing behavior out-of-process for the Wakeve app bundle.

The target SHALL:
- use the same development team as `WakeveApp`
- target the Wakeve app bundle identifier `com.guyghost.wakeve`
- run on an iOS simulator that supports `AppIntentsTesting`
- avoid direct imports of private Wakeve app implementation modules
- prepare deterministic state through explicit test support boundaries
- execute intents and queries via `AppIntentsTesting`
- verify both returned values and real app state changes where applicable

#### Scenario: App Intents test suite runs create/update/invite/poll/vote/transport flows
- **GIVEN** `WakeveAppIntentsTests` has prepared deterministic app state
- **WHEN** the suite invokes Wakeve App Intents through `AppIntentsTesting`
- **THEN** each intent SHALL return the expected entity or result
- **AND** the underlying Wakeve state SHALL reflect the mutation.

#### Scenario: App Intents test suite validates entity queries
- **GIVEN** deterministic events, groups, participants, polls, and transport proposals exist
- **WHEN** `WakeveAppIntentsTests` executes entity queries through `AppIntentsTesting`
- **THEN** lookup by identifier, text search, suggestions, spotlight queries, view annotations, and no-result cases SHALL match expected behavior.

#### Scenario: App Intents test suite validates definitions are discoverable
- **GIVEN** the Wakeve app bundle is installed for testing
- **WHEN** `WakeveAppIntentsTests` loads `IntentDefinitions` for `com.guyghost.wakeve`
- **THEN** every supported Wakeve intent and entity type SHALL be discoverable by its expected identifier
- **AND** the test suite SHALL be able to construct supported intents with test parameters.

#### Scenario: App Intents test suite catches system-surface regressions
- **GIVEN** an App Intent, entity, query, or shortcut metadata change breaks expected behavior
- **WHEN** `WakeveAppIntentsTests` runs on an iOS simulator
- **THEN** the suite SHALL fail before release so Siri, Shortcuts, Spotlight, widget, or view annotation regressions are detected early.
