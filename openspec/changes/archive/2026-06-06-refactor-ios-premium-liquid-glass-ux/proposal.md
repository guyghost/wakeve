# Change: Refactor iOS app into a premium Liquid Glass experience

## Why
Wakeve's iOS experience should feel like a calm, premium, social event planning product rather than a generic feature dashboard. The current design system exists, but the product needs stronger screen intent, predictable navigation, progressive disclosure, and dark-mode-first Liquid Glass execution across core flows.

## What Changes
- Refactor the iOS information architecture around one clear intention per screen.
- Replace feature-heavy navigation with predictable iOS tabs: Home, Groups, Messages, and Profile.
- Move event creation out of the tab bar and into contextual actions such as a floating glass action on Home, toolbar actions, or bottom sheets.
- Update Home, Event Detail, Vote, Transport, Participants, Messages, Create Event, empty states, and loading states to use premium iOS hierarchy.
- Expand the iOS design system with reusable Liquid Glass components, semantic tokens, motion rules, polished empty/loading states, and accessible dark/light mode behavior.
- Preserve shared KMP business logic and existing state machines; this is an iOS UI/UX refactor.

## Impact
- Affected specs: `ios-design-system`
- Affected code:
  - `iosApp/src/Theme/DesignSystem.swift`
  - `iosApp/src/Theme/LiquidGlassModifier.swift`
  - `iosApp/src/Components/`
  - `iosApp/src/Views/App/ContentView.swift`
  - `iosApp/src/Views/Events/HomeView.swift`
  - `iosApp/src/Views/Events/CreateEventSheet.swift`
  - `iosApp/src/Views/Events/ParticipantManagementView.swift`
  - `iosApp/src/Views/Events/TransportPlanningView.swift`
  - `iosApp/src/Views/Polls/PollVotingView.swift`
  - `iosApp/src/Views/Inbox/InboxView.swift`
  - `iosApp/src/Views/Profile/ProfileTabView.swift`
  - `iosApp/src/ViewModels/*` only where mapping or view state is needed for UI presentation
- Testing impact:
  - SwiftUI view model tests for screen state mapping and creation flow steps.
  - XCTest or snapshot-style checks for dark/light mode, tab structure, and major empty/loading states.

## Delivery Notes
- `implementation-audit.md` records the current screen/component gaps found before implementation.
- `execution-plan.md` breaks the refactor into incremental implementation slices.
- `acceptance-checklist.md` defines the evidence required before the change can be considered complete.
- `approval-summary.md` states the exact approval decision needed before implementation starts.
