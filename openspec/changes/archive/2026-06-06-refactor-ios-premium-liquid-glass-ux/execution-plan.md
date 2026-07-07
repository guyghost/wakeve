# Execution Plan: Premium iOS Liquid Glass UX

This plan is the implementation sequence after OpenSpec approval. It keeps the refactor incremental while preserving the full target experience from the brief.

## Slice 1: Design System Foundations

Goal: make the target UI possible without one-off styling.

Code targets:
- `iosApp/src/Theme/DesignSystem.swift`
- `iosApp/src/Theme/LiquidGlassModifier.swift`
- `iosApp/src/Components/DesignSystem/WakeveDesignSystemComponents.swift`
- `iosApp/src/Components/LiquidGlassAnimations.swift`

Work:
- Add semantic tokens for background, secondary background, label, secondary label, separator, accent, destructive, progress, confirmation, muted event highlight, glass tint, and skeleton surfaces.
- Add motion tokens for quick, standard, sheet, confirmation, and tab transitions.
- Reduce purple dominance in event and app gradients.
- Add or consolidate reusable components:
  - `LiquidGlassCard`
  - `LiquidGlassButton`
  - `LiquidGlassToolbar`
  - `LiquidGlassTabBar`
  - `BottomSheet`
  - `EmptyState`
  - `LoadingSkeleton`
- Ensure glass components use native iOS 26 APIs where available and readable material/opaque fallbacks otherwise.

Proof:
- Components compile.
- Existing screens can still import and use the design system.
- Native Liquid Glass calls remain gated with fallbacks.

## Slice 2: Destination Navigation

Goal: make navigation predictable and align tabs with destinations only.

Code targets:
- `iosApp/src/Models/WakeveTab.swift`
- `iosApp/src/Views/App/ContentView.swift`
- `iosApp/src/Views/Profile/ProfileTabView.swift`
- New or adapted `Groups` and `Messages` destination files.

Work:
- Change tab enum to `home`, `groups`, `messages`, `profile`.
- Replace `Inbox` tab with `Messages`.
- Replace `Explore` tab with `Groups` unless a dedicated groups view is introduced.
- Promote Profile from sheet to tab destination.
- Keep Create Event as contextual state, not a tab.
- Preserve event-specific navigation through `AppView` or a narrowly scoped route model.

Proof:
- Swift tests assert the tab cases and labels.
- `ContentView` no longer exposes Create Event as a tab.
- `ProfileTabView` is reachable as a tab destination.

## Slice 3: Home / À Venir

Goal: make the first screen calm, premium, and action-oriented.

Code targets:
- `iosApp/src/Views/Events/HomeView.swift`
- Shared event card/list components.

Work:
- Replace carousel-first layout with:
  - greeting
  - featured next event
  - upcoming events grouped by relevance or date
  - subtle empty state
  - floating glass create action
- Move filters into progressive disclosure or a compact menu.
- Add stable loading skeletons.

Proof:
- Home renders useful states for loading, empty, one event, and multiple events.
- Primary create action remains visible without being a tab.

## Slice 4: Event Detail Workspace

Goal: make each event feel alive and organized without exposing every feature at once.

Code targets:
- Extract `EventDetailView` from `iosApp/src/Views/App/ContentView.swift` into a dedicated event view file.
- Shared components: `EventHeroCard`, `ParticipantAvatarStack`, section rows/cards.

Work:
- Add immersive event header.
- Show event title, date/place metadata, participants preview, and the most urgent next action.
- Organize progressive sections:
  - Overview
  - Participants
  - Vote
  - Transport
  - Messages
- Move edit/delete/share/advanced actions into menus or bottom sheets.

Proof:
- Event Detail remains wired to existing callbacks.
- Access-control and phase gating behavior remain unchanged.

## Slice 5: Create Event Wizard

Goal: make creation lightweight rather than form-heavy.

Code targets:
- `iosApp/src/Views/Events/CreateEventSheet.swift`
- `iosApp/src/ViewModels/CreateEventViewModel.swift`

Work:
- Introduce five explicit steps:
  1. Name
  2. Date
  3. Place
  4. Invite people
  5. Confirm
- Add one main task per step, strong title, large input/selector, progress indicator, and contextual next action.
- Preserve repository save and event construction logic.

Proof:
- Swift tests cover step progression and required-field validation.
- Created events still save through the existing repository path.

## Slice 6: Flow Screens

Goal: reduce stress in secondary flows and make each screen single-purpose.

Code targets:
- `iosApp/src/Views/Polls/PollVotingView.swift`
- `iosApp/src/Views/Events/TransportPlanningView.swift`
- `iosApp/src/Views/Events/ParticipantManagementView.swift`
- `iosApp/src/Views/Inbox/InboxView.swift`

Work:
- Vote: one question, visible progress, options, selected state, confirmation feedback, glass capsule action.
- Transport: route/meeting point first, departure/arrival info, participants, one primary action; move optimization into disclosure.
- Participants: group accepted, pending, declined; invite through toolbar or bottom sheet.
- Messages: event-based conversations, search, unread states, compact previews, event context.

Proof:
- Existing transport and organization contract tests still pass.
- Existing inbox model tests continue to pass or are updated to Messages naming while preserving behavior.

## Slice 7: Verification and Polish

Goal: prove the refactor satisfies the spec and does not regress shared behavior.

Work:
- Replace major loading spinners with shared skeletons.
- Normalize empty states.
- Verify dark mode and light mode.
- Verify dynamic type, increased contrast, Reduce Motion, and Reduce Transparency.
- Run iOS tests or document exact local tooling blocker.

Proof:
- `openspec validate refactor-ios-premium-liquid-glass-ux --strict`
- Relevant Swift/Xcode test command output.
- Screenshot evidence or documented manual verification for key screens.
