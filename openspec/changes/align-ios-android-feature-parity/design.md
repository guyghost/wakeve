## Context
Android's navigation graph includes a broad set of event-organization routes: event creation, participant management, poll voting/results, scenario list/detail/comparison/management, transport, accommodation, meals, equipment, activities, comments, event photos/follow-up, meetings, budget, payment, Tricount, notifications, notification preferences, leaderboard, organizer dashboard, settings, invite sharing, auth, onboarding, home, explore, and profile.

iOS has many corresponding SwiftUI components and shared repositories, but `ContentView.swift` still contains direct placeholders for several Android-equivalent route states:
- `eventCreation`
- `mealPlanning`
- `equipmentChecklist`
- `activityPlanning`
- `scenarioDetail`

It also maps accommodation to the broader scenario organization view rather than an accommodation-specific flow, and it lacks top-level navigation handling for several Android secondary routes. The iOS deep-link service currently handles event detail, poll voting, meeting detail, and invite links, while Android supports more event subroutes and app utility routes.

Shared iOS platform services also contain explicit placeholders for behavior Android partially implements, including `IosTextToSpeechService`, `IosBadgeNotificationService`, and `IosMeetingProvider`.

The detailed route and platform gap inventory lives in `parity-audit.md`. That file is part of the proposal evidence and should be converted into automated parity fixtures during implementation.

## Goals
- Make every Android user-visible event-organization feature either reachable on iOS or explicitly documented as not applicable for platform/provider reasons.
- Remove route placeholders from iOS primary and secondary event workflows.
- Keep iOS screens native SwiftUI and compatible with the existing Wakeve iOS design system.
- Preserve shared KMP business logic, repositories, access-control rules, pending-sync semantics, and workflow states.
- Add automated parity checks so future Android routes cannot silently drift away from iOS.

## Non-Goals
- Do not copy Android Material UI into iOS.
- Do not add generic chat, generic tasks, or generic social features outside event-scoped flows.
- Do not complete unrelated WeatherKit entitlement/device validation or WakeveAI device profiling tasks from active proposals.
- Do not add new external provider credentials or server APIs unless an existing Android path already depends on them and the iOS gap cannot be closed without them.
- Do not change Android production behavior except for tests or route inventory fixtures needed for parity verification.

## Decisions
- Decision: Treat Android route reachability as the parity baseline, not Android visual design.
  - Why: Product capability should match, while platform UX should remain native.
  - Alternative considered: Reusing Compose Multiplatform screens directly on iOS. Rejected because the project convention is shared logic, native UI.

- Decision: Build an explicit iOS route model for secondary destinations.
  - Why: The current `AppView` switch is manageable for core flows but hides route gaps and makes deep-link parity hard to test.
  - Alternative considered: Add ad hoc cases until placeholders disappear. Rejected because this would not prevent future drift.

- Decision: Use shared repositories and existing SwiftUI views before creating new services.
  - Why: Many iOS components already exist for budget, transport, meetings, comments, notifications, invitation sharing, and weather/AI surfaces.
  - Alternative considered: Create a new iOS feature layer for every Android route. Rejected as unnecessary duplication.

- Decision: Platform placeholder replacement should be feature-visible, minimal, and honest.
  - Why: Some providers cannot create meetings without credentials. iOS should still launch meeting URLs, expose availability, request notification permission/status, and report unsupported provider states clearly.

## Implementation Strategy
1. Create parity inventory tests that enumerate Android `Screen` routes and iOS route/deep-link cases.
2. Replace SwiftUI placeholders with real screens or focused native equivalents.
3. Add missing navigation adapters from event detail and organization surfaces into those routes.
4. Expand deep-link parsing and route handling on iOS to match Android's supported route set.
5. Replace user-visible iOS platform placeholders where native APIs are sufficient.
6. Add localization and contract tests for equivalent labels, access-denied states, pending-sync states, and read-only finalized behavior.
7. Run shared KMP tests, Android route/deep-link tests, and iOS unit/UI tests.

## Definition of Done
- The automated parity inventory covers every Android `Screen` route listed in `verification-matrix.md`.
- Every iOS route classification is backed by either a native route, an automated test for a product-approved non-placeholder unavailable/deferred state, or a documented platform/provider exception.
- iOS deep-link parsing covers the Android-equivalent route set and rejects unsafe URL forms before navigation.
- Newly connected iOS routes preserve shared repository state, access-control decisions, pending-sync presentation, and finalized read-only behavior.
- Platform helper replacements are limited to user-visible parity gaps and do not add unrelated provider setup or server contracts.
- Active weather and WakeveAI proposals remain separate dependencies; this change only integrates their completed surfaces when present.

## Risks / Trade-offs
- `ContentView.swift` is already large and may become harder to maintain.
  - Mitigation: Extract route handling, parity helpers, and newly connected feature views into focused files as part of the implementation.
- Some iOS feature files exist only as sheets or partial components.
  - Mitigation: Wrap partial components in complete screen flows with clear empty/access states before expanding feature depth.
- Meeting provider parity can be misunderstood as full provider creation.
  - Mitigation: Preserve existing provider-unavailable states and implement launch/deep-link behavior separately from creation.
- Deep-link expansion can reveal restricted event data if access checks are bypassed.
  - Mitigation: Route all event-scoped deep links through existing repository lookup and access-control gates before showing detail surfaces.

## Migration Plan
1. Add tests and route inventory without changing runtime behavior.
2. Introduce iOS route wrappers and adapters behind existing `AuthenticatedView`.
3. Replace placeholders one route group at a time: logistics, scenario detail, collaboration/photos, utility routes, deep links, then platform helpers.
4. Keep old local state transitions working until new route wrappers cover every current entry point.
5. Remove obsolete placeholder cases only after tests prove no navigation entry still points to them.

## Resolved Scope Decisions
- Organizer dashboard: iOS should expose a dedicated secondary route so Android's `organizer_dashboard` route has an explicit parity target. Home or Profile may link to it, but the route itself must remain deep-linkable or route-addressable for parity tests.
- Event photos/follow-up: iOS should ship the same event-scoped follow-up capability Android exposes, using a native minimal surface first. A richer photo workflow can be proposed separately; this change should not leave a generic placeholder.
- Text-to-speech: iOS should implement AVFoundation-backed shared service behavior only if a current iOS user-visible route calls the shared KMP TTS service. If no visible route calls it, this change should classify it as non-user-visible and document why it is outside the first parity implementation.
