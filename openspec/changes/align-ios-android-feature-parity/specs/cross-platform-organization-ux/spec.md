## ADDED Requirements
### Requirement: iOS Android Feature Reachability Parity
Wakeve MUST keep iOS feature reachability aligned with Android for user-visible event organization capabilities.

Every Android route that exposes a private event planning capability MUST have one of the following on iOS: an equivalent native SwiftUI route, an explicitly documented platform/provider exception, or a product-approved deferred state that is not a generic placeholder. Equivalent iOS routes MUST preserve the same event access rules, workflow status rules, pending-sync semantics, and finalized read-only behavior while using native iOS navigation and design patterns.

#### Scenario: Android route has an iOS equivalent
- **GIVEN** Android exposes an event-scoped route for participants, polls, scenarios, transport, accommodation, meals, equipment, activities, comments, meetings, budget, payment, Tricount, notifications, or invitations
- **WHEN** the parity inventory test runs
- **THEN** iOS has a classified equivalent route or documented exception
- **AND** the equivalent route is reachable from normal navigation and deep links where Android supports deep links
- **AND** the route does not render a generic placeholder.

#### Scenario: Platform exception is documented
- **GIVEN** Android exposes behavior that depends on Android-only APIs or configured providers
- **WHEN** iOS cannot provide equivalent behavior in the current release
- **THEN** the exception is documented with the platform/provider reason
- **AND** iOS shows a clear event-scoped unavailable state if the user can encounter the feature
- **AND** iOS does not present the feature as complete or silently ignore the user action.

### Requirement: iOS Organization Logistics Parity
Wakeve MUST provide iOS organization logistics screens equivalent to Android for accommodation, meal planning, equipment checklist, and activity planning.

These iOS screens MUST use shared Wakeve repositories or shared presentation data where available. They MUST support empty states, editable organizer states, participant-visible states, access-denied states, pending-sync states, and finalized read-only states.

#### Scenario: Organizer opens logistics from iOS event detail
- **GIVEN** an event is in `ORGANIZING`
- **AND** the current iOS user is the organizer
- **WHEN** the organizer opens accommodation, meals, equipment, or activities
- **THEN** iOS shows the corresponding event-scoped planning screen
- **AND** the organizer can perform the same core add/edit/complete/not-needed actions available from Android when backed by shared data
- **AND** changes use the same local-first persistence and pending-sync state model.

#### Scenario: Finalized event logistics are read-only
- **GIVEN** an event is in `FINALIZED`
- **WHEN** an iOS user opens any logistics screen
- **THEN** the screen shows the confirmed logistics information in read-only mode
- **AND** mutation controls are hidden or disabled
- **AND** the screen explains the next useful action only when one exists.

### Requirement: Cross-Platform Deep Link Parity
Wakeve MUST keep iOS and Android deep-link support aligned for product-scoped event and app utility destinations.

Deep links MUST normalize unsafe path segments, require authentication for private destinations, preserve invite handling for unauthenticated users, and route through access-control gates before showing restricted event details.

#### Scenario: User opens an event subroute deep link on iOS
- **GIVEN** a supported Wakeve link points to an event subroute such as participants, poll, scenarios, budget, meetings, or comments
- **WHEN** the authenticated iOS user opens the link
- **THEN** Wakeve parses the same destination class Android supports
- **AND** navigates to the matching native iOS route if the user has access
- **AND** falls back to the event detail or an access-denied state when workflow or permission rules block the subroute.

#### Scenario: Unsafe link is rejected
- **GIVEN** a Wakeve deep link contains encoded path traversal, fragments, user info, unsupported ports, or unsafe encoded separators
- **WHEN** iOS parses the link
- **THEN** parsing fails safely
- **AND** no restricted route is opened.

### Requirement: Parity Drift Detection
Wakeve MUST include automated drift detection for Android/iOS feature parity.

The drift detection MUST compare Android route inventory, iOS route inventory, iOS deep-link parsing, user-facing labels, and classified exceptions. A new Android user-visible route MUST fail parity checks until iOS support or a documented exception is added.

#### Scenario: Android adds a new event route
- **GIVEN** a new Android `Screen` destination is added for a user-visible event organization capability
- **WHEN** the parity test suite runs
- **THEN** the suite fails until the iOS route inventory classifies the destination
- **AND** the classification identifies an equivalent native iOS route, a documented exception, or a deferred product-approved state.
