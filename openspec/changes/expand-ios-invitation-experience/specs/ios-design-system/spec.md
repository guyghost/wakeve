## ADDED Requirements

### Requirement: iOS SHALL Provide Six Connected Invitation Experience Surfaces
The iOS application MUST provide six functional, repository-backed invitation experience surfaces: Event Library, Creation Studio, Audience, Event Detail with real owner routes, Event Information, and Archive. The completed invitation canvas MUST remain the Event Detail hero and MUST be consumed as a building block rather than replaced, duplicated, or expanded into a business-state owner.

Each surface MUST expose its current repository state, relevant pending/freshness/access state, responsible actor when applicable, and at most one visually primary next action. Loading, empty, stale, offline, conflict, failure, cancellation, restricted, read-only, and terminal states MUST use typed projections rather than fixture data or parsed display text.

#### Scenario: User moves from Library through an active event
- **GIVEN** a repository-backed upcoming event and installed route capabilities
- **WHEN** the user opens its Library card and follows the typed next action
- **THEN** Wakeve opens the existing invitation-canvas Event Detail or its owning Poll/Organization flow as modeled
- **AND** back navigation returns to the same Library projection
- **AND** no placeholder or duplicate Event Detail screen is shown.

#### Scenario: Auxiliary repository data is unavailable
- **GIVEN** one surface has a structured event but participant, artwork, provider, or readiness data is unavailable
- **WHEN** the surface renders
- **THEN** it omits claims it cannot prove and uses an honest unavailable or fallback state
- **AND** it does not infer state, permission, actor, or action from copy, color, imagery, or AI output.

#### Scenario: Past event opens from any surface
- **GIVEN** the global route preflight classifies an event as `PAST`
- **WHEN** the user opens it from Library, Audience, Detail, Information, a menu, or a deep link
- **THEN** iOS opens Archive or a local read-only summary
- **AND** no mutating owner callback is reached.

### Requirement: iOS Library, Studio, and Archive SHALL Use Artwork-Led Information Hierarchy
Event Library MUST provide native filter/projection controls and glanceable layered event cards ordered from structured repository data. Creation Studio MUST provide `NEW` and guarded `EDIT_EXISTING` experiences with release-1 `NONE`, curated preset, and already-authorized server-asset artwork choices, validation, current-revision preview, commit, pending sync, retry, and cancellation. If no existing server-asset owner capability is installed, Studio MUST expose `NONE` and curated presets only. Release 1 MUST NOT expose a device photo picker or upload flow. Archive MUST present permitted artwork and settled event summaries as a read-only Wakeve event view.

Artwork rendering MUST consume the total persisted artwork union. `NONE`, loading, offline failure, unavailable/invalid legacy artwork, and render failure MUST use the deterministic event-mood fallback without changing semantic state or action availability. A server asset MUST be selectable only when an installed owner has already authorized its validated reference.

#### Scenario: Organizer relaunches an edited draft
- **GIVEN** an authorized DRAFT edit committed event fields and structured artwork locally
- **WHEN** the app is relaunched before server synchronization
- **THEN** Studio and Library read the committed revision and artwork from the repository
- **AND** show the matching pending-sync subject
- **AND** do not create a second event or downgrade artwork to a local file URL.

#### Scenario: Studio has no authorized server-asset owner
- **GIVEN** release 1 runs without an installed authorized server-asset owner
- **WHEN** the organizer opens artwork choices
- **THEN** Studio shows `NONE` and curated presets only
- **AND** no device photo permission, picker, upload progress, or local file selection is displayed.

#### Scenario: Artwork cannot load offline
- **GIVEN** an event references valid remote artwork and the device is offline
- **WHEN** Library, Event Detail, Information, or Archive renders
- **THEN** the surface uses the same event-mood fallback policy
- **AND** event state, access, metadata, and primary action remain readable and usable.

#### Scenario: Accessibility text reflows Studio
- **GIVEN** an accessibility Dynamic Type size and a compact-height device
- **WHEN** Studio renders validation, preview, or pending-sync state
- **THEN** content remains scrollable and ordered
- **AND** exactly one primary action remains reachable
- **AND** no artwork layer obscures the action or error text.

### Requirement: iOS Audience, Detail, and Information SHALL Route Through Typed Owners
Audience MUST render independent invitation delivery, approval, membership, RSVP, and retained-date validation axes from one coherent snapshot. Its direct-invite and public-share controls MUST reflect distinct injected capabilities. Event Detail MUST route `EDIT_DRAFT`, `SUBMIT_VOTE`, `VIEW_POLL_RESULTS`, `COMPARE_OPTIONS`, `CONTINUE_ORGANIZATION`, `SHOW_ACCESS_STATE`, `SHOW_DETAILS`, and `VIEW_FINAL_DETAILS` only to their modeled owners or safe local fallbacks. Event Information MUST render typed metadata, provider destinations, notification axes, and guarded existing operations.

Missing route, provider, access, repository, or secure-share capability MUST fail closed. Provider destinations MUST be available only from structured validated inputs and MUST NOT claim Wakeve workflow completion. Event Information MUST edit only event-scoped notification preference and MUST use confirmation for destructive actions.

#### Scenario: Canvas action opens a real Poll route
- **GIVEN** the completed canvas projects `SUBMIT_VOTE`
- **AND** typed polling access and the Poll route are installed
- **WHEN** the user activates the action
- **THEN** iOS opens the real Poll owner flow
- **AND** the canvas does not submit a vote or mutate event state itself.

#### Scenario: Required Detail route is unavailable
- **GIVEN** a canvas or Information action has no installed permitted destination
- **WHEN** the user activates or resolves that action
- **THEN** iOS shows permitted local details or a specific unavailable state
- **AND** it does not dispatch an adjacent mutation or render a generic construction placeholder.

#### Scenario: Organizer opens Audience without secure public sharing
- **GIVEN** a DRAFT direct-invite capability is ready
- **AND** `harden-event-invitation-links` has not supplied a matching ready secure-share capability
- **WHEN** Audience renders
- **THEN** the organizer may use the DRAFT direct-invite owner
- **AND** public share remains hidden or honestly unavailable
- **AND** no local token, URL, or QR fallback is displayed.

#### Scenario: Information displays notification controls
- **GIVEN** Event Information has event, account, and system notification inputs
- **WHEN** the user opens notification information
- **THEN** iOS displays all three axes and their effective result separately
- **AND** editing changes only the event preference
- **AND** system permission and account settings link to their dedicated native/owner flows rather than being mutated in the sheet.

### Requirement: Apple-Inspired Invitation Presentation SHALL Preserve Wakeve Semantics and Accessibility
The iOS invitation experience MAY adapt Apple Invites workflow composition and Apple Wallet glanceable information architecture from the canonical Mobbin references. Apple Invites MUST be treated as workflow inspiration; Apple Wallet MUST be treated as information architecture inspiration only. The implementation MUST NOT add PassKit, passes, payment/admission semantics, scannable local QR codes, or locally constructed invitation credentials.

Native navigation bars, tab bars, toolbars, menus, forms, sheets, alerts, search, and standard actions MUST remain familiar iOS chrome. Event artwork, mood, participant context, state clarity, and progress MAY express Wakeve identity. Liquid Glass MUST be limited to meaningful controls or elevated surfaces and MUST provide accessible material/opaque fallbacks.

All six surfaces MUST support light/dark mode, VoiceOver order and actions, Dynamic Type including accessibility sizes, minimum 44-point targets, increased contrast, Reduce Motion, Reduce Transparency, compact height, landscape, long localized copy, and reduced imagery conditions. Supported localization catalogs MUST include English, French, Spanish, Italian, and Portuguese with canonical terminology coordinated with `standardize-product-language`.

#### Scenario: Designer applies Wallet-inspired card stacking
- **GIVEN** Library or Archive uses layered cards for glanceable hierarchy
- **WHEN** the surface is implemented
- **THEN** the cards remain ordinary Wakeve event views with native navigation
- **AND** they contain no pass identifier, barcode, payment balance, admission claim, or PassKit behavior.

#### Scenario: Transparency and motion are reduced
- **GIVEN** Reduce Transparency and Reduce Motion are enabled
- **WHEN** any invitation experience surface renders or transitions
- **THEN** controls use stable opaque/material fallbacks
- **AND** essential hierarchy, focus order, action identity, and state feedback remain intact
- **AND** no decorative motion is required to understand progress.

#### Scenario: Visual fidelity is reviewed
- **GIVEN** all six surfaces run against real repository-backed simulator data
- **WHEN** design QA compares them with the canonical Mobbin references
- **THEN** the review evaluates hierarchy, crop, information density, native chrome, typography, color, accessibility, and action priority
- **AND** all P0, P1, and P2 findings are corrected before handoff
- **AND** reference fixture names or data are not introduced as production defaults.
