## ADDED Requirements
### Requirement: Deterministic Invitation-Canvas Event Detail
The iOS Event Detail screen MUST present an immersive invitation canvas as its primary viewport. The canvas MUST show an event image or event-mood fallback, event title, date and lifecycle state, organizer context when available, confirmed and pending participant context when available, the responsible actor, and one next useful action derived from structured Wakeve state.

The invitation canvas MUST NOT parse display text or use LLM output to select a lifecycle state, permission, responsible actor, or action destination. It MUST consume the existing Event lifecycle, repository permissions, participant state, sync state, and deterministic next-action model. Native navigation controls MUST remain platform-familiar while the event canvas carries Wakeve brand expression.

#### Scenario: Confirmed event has pending responses
- **GIVEN** an event date is server-confirmed
- **AND** structured participant data reports confirmed and pending responses
- **WHEN** an authorized user opens Event Detail
- **THEN** the invitation canvas shows the confirmed date state
- **AND** it distinguishes confirmed participants from pending responses
- **AND** it identifies the responsible actor and one next useful organization action.

#### Scenario: Displayed update is pending synchronization
- **GIVEN** a displayed Event Detail update is recorded locally but not yet synchronized
- **WHEN** the invitation canvas renders the affected status or action context
- **THEN** it identifies the update as awaiting synchronization
- **AND** it does not present that update as server-confirmed.

#### Scenario: Hero image is unavailable
- **GIVEN** an event has no hero image, the image is loading, or the image cannot load offline
- **WHEN** Event Detail renders
- **THEN** it uses the event mood palette as a non-blocking visual fallback
- **AND** the same state, participant context, and primary action remain readable and usable.

#### Scenario: User cannot access organization details
- **GIVEN** the current user is neither the organizer nor a confirmed attendee for restricted organization content
- **WHEN** Event Detail renders
- **THEN** the invitation canvas does not expose restricted planning details
- **AND** its next action is limited to an action permitted by the existing access model or a clear non-actionable access state.

#### Scenario: Organizer shares an invitation from the canvas
- **GIVEN** the current user is authorized to issue or share an invitation
- **AND** the secure invitation flow has a server-issued canonical URL ready to share
- **WHEN** the organizer activates the invitation-canvas share control
- **THEN** the canvas delegates to the typed secure invitation callback
- **AND** it does not construct, infer, log, or persist an invitation token or redeemable URL.

#### Scenario: Secure invitation sharing is unavailable
- **GIVEN** the current user lacks invitation permission or no server-issued invitation is ready
- **WHEN** Event Detail renders
- **THEN** the canvas hides or disables the invitation share control with an honest structured unavailable state
- **AND** it does not fall back to a locally generated or predictable invitation URL.

### Requirement: Invitation-Canvas Progressive Disclosure and Accessibility
The iOS Event Detail screen MUST preserve event-scoped weather, anticipation, reviewable AI suggestions, readiness, participants, voting, scenarios, transport, accommodation, meals, equipment, activities, comments, photos, invitations, meetings, budget, payments, Tricount, and message destinations when existing workflow and access rules allow them. These secondary modules MUST be progressively disclosed after the invitation canvas and MUST NOT compete with the primary action at the same visual priority.

Interactive glass elements MUST use native Liquid Glass when available and MUST provide accessible material or opaque fallbacks when native glass is unavailable, Reduce Transparency is enabled, or increased contrast requires it. The screen MUST support Dynamic Type, Reduce Motion, readable contrast, VoiceOver ordering, and minimum 44-point interactive targets.

Exactly one primary action MUST remain visible without requiring the user to discover it by scrolling. The layout MUST reflow or move that same action to a persistent safe-area placement on compact screens and at accessibility Dynamic Type sizes; it MUST NOT duplicate the action.

#### Scenario: Authorized organizer reveals details
- **GIVEN** an organizer opens Event Detail for an organizing event
- **WHEN** the organizer continues beyond the invitation canvas
- **THEN** every existing organization destination allowed by the workflow remains reachable
- **AND** the invitation canvas still exposes only one primary next action.

#### Scenario: Accessibility settings reduce visual effects
- **GIVEN** Reduce Transparency, Reduce Motion, increased contrast, or an iOS version without native Liquid Glass
- **WHEN** Event Detail renders
- **THEN** controls and the next-action surface use stable accessible fallbacks
- **AND** state, actor, pending responses, and action text retain readable contrast and predictable layout.

#### Scenario: Accessibility text would push the action below the viewport
- **GIVEN** an accessibility Dynamic Type size or compact-height device
- **WHEN** the invitation canvas cannot fit its semantic content and primary action in the first viewport
- **THEN** the same primary action moves to an accessible persistent safe-area placement
- **AND** no duplicate primary action is rendered
- **AND** the event status, responsible actor, and pending context remain available in VoiceOver order.
