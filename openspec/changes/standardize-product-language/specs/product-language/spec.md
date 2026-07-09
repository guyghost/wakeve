## ADDED Requirements

### Requirement: Canonical Product Language Registry
Wakeve MUST maintain one reviewed registry of semantic product concepts, localization keys, supported locales, and user-facing projections for Android, iOS, Siri/App Intents, notifications, accessibility semantics, and server-originated copy. Every supported locale MUST contain every required semantic key, and production UI MUST NOT silently expose another locale because a key is missing.

#### Scenario: Locale catalog is incomplete
- **GIVEN** a required semantic key exists in one supported locale
- **WHEN** localization validation runs
- **THEN** every other supported locale MUST contain the same key
- **AND** the release MUST fail rather than ship unintended fallback copy.

#### Scenario: A user changes locale
- **WHEN** the user opens equivalent Wakeve flows in any supported locale
- **THEN** each surface MUST express the same canonical concept and expectation
- **AND** dates, numbers, plurals, and spoken phrases MUST follow the active locale.

### Requirement: Deterministic State-to-Language Projection
Wakeve MUST derive user-facing state language from deterministic domain state, user role, confirmed facts, pending facts, and allowed actions. Primary event surfaces MUST communicate what is confirmed, what remains pending, who must act, and the next useful action. Copy strings and LLM output MUST NOT be used as state discriminators or transition inputs.

#### Scenario: Event lifecycle is projected
- **GIVEN** an event status is `DRAFT`, `POLLING`, `COMPARING`, `CONFIRMED`, `ORGANIZING`, or `FINALIZED`
- **WHEN** the status is displayed in French
- **THEN** the canonical title MUST respectively be `Brouillon`, `Sondage en cours`, `Options à comparer`, `Date confirmée`, `Détails à préparer`, or `Prêt`
- **AND** equivalent approved concepts MUST be used in every other locale.

#### Scenario: Offline work is pending
- **GIVEN** a user changes event data while offline
- **WHEN** the affected surface renders before synchronization
- **THEN** it MUST identify the change as pending synchronization
- **AND** it MUST NOT imply that other participants or the server confirmed the change
- **AND** the deterministic sync model MUST remain the source of truth.

#### Scenario: Event is terminal
- **GIVEN** an event is `FINALIZED`
- **WHEN** a user opens its primary surface
- **THEN** it MUST present the event as `Prêt` or the approved localized equivalent
- **AND** it MUST NOT expose an invalid editing CTA.

### Requirement: Internal Identifier and UI Language Separation
Wakeve MUST preserve internal domain, API, persistence, route, analytics, and state-machine identifiers independently from user-facing language. `Scenario` MUST project to `Option`, `Inbox` MUST project to `Notifications`, and no internal event status name MUST be translated by renaming its source identifier.

#### Scenario: Scenario appears in UI
- **GIVEN** the domain supplies a `Scenario`
- **WHEN** the object is presented to a user
- **THEN** the UI MUST call it `Option` or the approved localized equivalent
- **AND** the domain model, persistence, API, and analytics identifiers MUST remain unchanged.

#### Scenario: Internal identifier is scanned
- **WHEN** release validation finds `Scenario`, `Inbox`, or raw status identifiers
- **THEN** occurrences in internal contracts MAY be allowlisted
- **AND** occurrences exposed as user-visible copy MUST fail validation.

### Requirement: Ideas Navigation Destination
Wakeve MUST label the event-preparation inspiration and template destination `Idées` in French and `Ideas` in English, with approved equivalents in other locales. The destination MUST remain scoped to preparing events and MUST NOT become a generic group directory or social discovery feed.

#### Scenario: User opens primary navigation
- **WHEN** the primary navigation renders on Android or iOS
- **THEN** the destination MUST be labeled `Idées`, `Ideas`, or the approved active-locale equivalent
- **AND** activating it MUST open event-preparation ideas or templates.

#### Scenario: Existing iOS groups tab is migrated
- **WHEN** the iOS UI-only tab identifier is migrated from `.groups` to `.ideas`
- **THEN** navigation state restoration and deep links MUST continue to open the same intended destination
- **AND** no event lifecycle, group-domain, persistence, API, or analytics state MUST be changed implicitly.

### Requirement: Notifications and Messages Taxonomy
Wakeve MUST display `Notifications` for alerts and updates and MUST display `Messages` only for event-scoped conversations and comments. Notifications MUST offer `À traiter` and `Informations` filters, with approved localized equivalents. `Inbox` MUST remain internal-only.

#### Scenario: User reviews alerts
- **WHEN** the user opens Notifications
- **THEN** alerts and updates MUST be filterable into items requiring attention and informational items
- **AND** conversation threads MUST NOT be relabeled as Notifications.

#### Scenario: Notification opens a conversation
- **GIVEN** a notification refers to an event message
- **WHEN** the user activates it
- **THEN** Wakeve MAY deep-link to the event-scoped Messages surface
- **AND** the source remains named Notifications and the destination remains named Messages.

### Requirement: Benefit-Led Reviewable AI Language
User-facing AI-assisted actions MUST name the immediate event-organization benefit rather than the technology or provider. Relevant actions MUST use concepts such as preparing options, summarizing responses, proposing a message, or completing a list. AI outputs that can influence event work MUST be identified as a proposal to review and MUST NOT imply that content was applied, sent, persisted, or confirmed.

#### Scenario: AI prepares event content
- **WHEN** AI prepares options, a response summary, a message, or list additions
- **THEN** the entry point MUST describe that benefit
- **AND** the output MUST be labeled `Proposition à relire` or the approved localized equivalent
- **AND** an explicit user action plus deterministic validation MUST be required before any mutation.

#### Scenario: AI fails or is unavailable
- **WHEN** an AI provider is unavailable, returns invalid output, is cancelled, or fails deterministic validation
- **THEN** Wakeve MUST preserve a usable manual path
- **AND** the UI MUST NOT advance workflow state or present success language
- **AND** retry copy MUST describe the action that will be retried.

### Requirement: Private Preparation Milestones
Wakeve MUST NOT expose public leaderboards, contributor rankings, competitive scores, or social-archetype badges. Any retained milestone MUST be private, non-comparative, sober, and directly tied to event preparation; it MUST NOT influence permissions or lifecycle state.

#### Scenario: User reaches a milestone
- **WHEN** a user completes a milestone such as a first event, regular voting, or ready organization
- **THEN** the milestone MAY be shown privately using approved localized language
- **AND** no public rank, comparison, or competitive score MUST be displayed
- **AND** no workflow transition or permission MUST depend on it.

### Requirement: Recipient-Locale Notifications and Native Siri Localization
Notification templates MUST be resolved from semantic event types using the recipient's locale and a deterministic documented fallback. Siri/App Intent phrases and responses MUST use native per-locale resources for every application-supported locale.

#### Scenario: Notification is delivered to users with different locales
- **GIVEN** the same event update targets recipients with different stored locales
- **WHEN** notifications are composed or retried
- **THEN** each recipient MUST receive the approved template in their locale
- **AND** retry, offline queueing, and duplicate suppression MUST preserve the semantic event and locale decision.

#### Scenario: Siri invocation uses a supported locale
- **WHEN** a user invokes a Wakeve Siri/App Intent phrase in any supported locale
- **THEN** the system MUST resolve a native resource for that locale
- **AND** it MUST NOT depend on language-suffixed keys inside a single default-language file.

#### Scenario: Notification permission is denied
- **GIVEN** notification permission is denied or restricted
- **WHEN** Wakeve needs to inform the user about delivery state
- **THEN** the app MUST state what will not be delivered
- **AND** offer to open system settings when the permission can be re-enabled there
- **AND** otherwise explain that alerts remain available in Notifications
- **AND** it MUST NOT claim that a push notification was delivered.

### Requirement: Contextual Accessible Language
Every actionable TalkBack and VoiceOver label MUST express the action, target, and relevant state in the active locale. Accessible copy MUST cover loading, disabled, validation error, permission, retry, offline, pending-sync, and terminal states without relying only on visual context.

#### Scenario: User focuses an ambiguous icon action
- **GIVEN** an icon represents opening details, navigating backward, marking payment, or opening more actions
- **WHEN** assistive technology focuses it
- **THEN** the label MUST name the action and target
- **AND** it MUST include relevant state such as paid, selected, pending, or unavailable.

#### Scenario: Accessible text expands
- **WHEN** the user enables large font scaling or a long supported locale
- **THEN** critical meaning and the primary action MUST remain available
- **AND** truncation MUST NOT remove target, state, or consequence.

### Requirement: Error Cancellation Retry Permission and Terminal Copy
Wakeve MUST define canonical localized copy for validation errors, operation failures, cancellations, retries, permissions, offline work, synchronization conflicts, and terminal states. Copy MUST describe the actual deterministic outcome and MUST preserve user input when recovery is possible.

#### Scenario: Operation fails and can be retried
- **WHEN** a save or delivery operation fails without changing business state
- **THEN** Wakeve MUST state that the operation did not complete
- **AND** preserve recoverable input
- **AND** offer a result-specific retry action without implying success.

#### Scenario: User cancels an operation
- **WHEN** a user cancels creation, generation, delivery, or a permission flow
- **THEN** Wakeve MUST not show success or completed-state copy
- **AND** deterministic state MUST remain unchanged unless a separately confirmed prior step was already persisted.

#### Scenario: Sync conflict is resolved
- **WHEN** deterministic synchronization resolves a conflict
- **THEN** the UI MUST identify the affected data and resulting state truthfully
- **AND** free text or AI output MUST NOT choose the conflict outcome.

### Requirement: Product Language Release Gates
Wakeve MUST block release when localization keys are incomplete, unintended fallback is possible, forbidden internal/technical/social terminology is user-visible, unapproved production literals remain, state projections diverge, required notification or Siri templates are absent, or critical accessibility semantics fail. Exceptions MUST use a narrow reviewed allowlist limited to non-user-visible identifiers, diagnostics, tests, or explicit previews.

#### Scenario: User-visible literal bypasses localization
- **WHEN** CI detects an unapproved visible or semantic production literal on Android, iOS, Siri, or notification surfaces
- **THEN** the product-language gate MUST fail
- **AND** implementation MUST move the text to an approved semantic localization key or document a narrow valid exception.

#### Scenario: Cross-platform concepts diverge
- **WHEN** Android and iOS project the same workflow state or action with different canonical concepts
- **THEN** contract tests MUST fail
- **AND** release MUST remain blocked until both consume the approved registry.

#### Scenario: Long text or accessibility loses meaning
- **WHEN** pseudo-localization, font scaling, TalkBack, or VoiceOver verification shows that a critical target, state, consequence, or action is missing
- **THEN** release validation MUST fail until the semantic meaning is restored.
