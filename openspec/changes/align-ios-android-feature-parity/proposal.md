# Change: Align iOS feature parity with Android

> **Status (2026-07-07): DEFERRED to post-App-Store-release.**
> This change is validated but not started (0/37 tasks). It is out of scope
> for the first iOS App Store submission. The priority order defined in
> ROADMAP.md is: (1) close App Store blockers AS-01 to AS-22, (2) finish the
> two in-flight iOS changes (weather-forecast, on-device-wakeve-ai), (3) then
> resume deferred work like this parity alignment. Do not start implementation
> until the App Store P0 gate is closed.

## Why
Wakeve's Android app currently exposes a broader routed event-organization surface than iOS. iOS has many of the underlying SwiftUI components and shared KMP services, but several Android routes still resolve to placeholders, partial substitutes, or missing deep-link destinations on iOS.

This creates product drift: the same event can be organized through Android-only paths for logistics, comments, dashboard utilities, deep links, and platform helpers while iOS users see incomplete or less direct flows.

## What Changes
- Replace iOS placeholders for Android-equivalent organization routes with real SwiftUI flows: accommodation, meal planning, equipment, activity planning, scenario detail, and event creation fallback routing.
- Add iOS route coverage for Android secondary destinations where they are product-scoped and already supported by shared data: comments, post-event photos/follow-up, organizer dashboard, leaderboard, notifications, settings, invitation share, budget/payment/tricount detail paths, and meeting paths.
- Expand iOS deep-link parsing and navigation to match Android's supported event, poll, scenario, budget, meeting, comments, notifications, settings, profile, home, and invite destinations where access rules allow.
- Replace iOS platform placeholders with native iOS equivalents where Android already has platform behavior and the feature is user-visible: meeting URL launching, text-to-speech availability, badge notification permission/status behavior, and safe app/settings openings.
- Add a parity verification suite that compares Android route inventory, iOS `AppView`/deep-link inventory, localization keys, access-control states, offline/pending-sync states, and UI test coverage.
- Preserve platform-native UX: Android remains Material/Compose; iOS uses SwiftUI and the existing Liquid Glass-compatible design system.

## Product Excellence Fit
This change directly helps private groups complete event organization from iOS with the same confidence Android users have: lodging, meals, equipment, activities, budgets, meetings, comments, and final readiness become reachable and stateful. It reduces out-of-Wakeve coordination by removing dead-end placeholders and incomplete links. Each added surface must show what is confirmed, pending, blocked, optional, or read-only, and must expose the next useful event action. The work remains mobile-first by reusing native SwiftUI navigation, compact event-scoped screens, existing access controls, and offline indicators. It avoids generic social/chat/task/calendar drift by limiting parity to event-scoped planning, logistics, collaboration, notifications, and account utility flows already present in Wakeve.

## Impact
- Affected specs: `cross-platform-organization-ux`, `ios-design-system`, `meeting-service`, `notification-management`, `collaboration-management`
- Affected code:
  - `iosApp/src/Views/App/ContentView.swift`
  - `iosApp/src/Services/DeepLinkService.swift`
  - `iosApp/src/Views/Events/**`
  - `iosApp/src/Views/Collaboration/**`
  - `iosApp/src/Views/Explore/LeaderboardView.swift`
  - `iosApp/src/Views/Profile/ProfileTabView.swift`
  - `iosApp/src/Views/Notifications/**`
  - `iosApp/src/Components/InvitationShareSheet.swift`
  - `iosApp/src/ViewModels/**`
  - `shared/src/iosMain/kotlin/com/guyghost/wakeve/**`
  - iOS unit/UI/contract tests under `iosApp/WakeveTests`, `iosApp/iosAppTests`, and `iosApp/iosAppUITests`
  - Android route/deep-link tests only as parity fixtures, not behavior changes

## Scope Notes
- This change does not redefine the active `add-event-weather-forecast` or `add-on-device-wakeve-ai` proposals. It should consume their completed iOS surfaces when present and avoid duplicating their remaining device-validation work.
- This change does not require Android UI redesign. Android is the parity source for feature reachability and workflow states; iOS keeps native layout and interaction patterns.
- External-provider creation for meetings remains bounded by existing provider availability. Parity means iOS can launch supported meeting URLs and present provider-unavailable states as clearly as Android, not that it invents provider credentials.
- The current route and platform gap inventory is recorded in `parity-audit.md`; implementation should keep that audit current until automated drift tests replace it as the primary evidence.
- Completion evidence is defined in `verification-matrix.md`. The change is not complete until each Android user-visible route is classified there by automated tests, each iOS exception has a platform/provider/product reason, and no Android-equivalent event organization route renders a generic placeholder.
