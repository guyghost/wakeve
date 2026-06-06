# Implementation Audit: Premium iOS Liquid Glass UX

## Current-State Summary

The iOS app already has a partial Apple-inspired visual system, but the experience does not yet match the requested product model.

- Navigation currently uses three tabs: `Accueil`, `Inbox`, and `Explorer`.
- `ProfileTabView` exists but is opened as a Home header sheet, not as a stable tab destination.
- There is no `Groups` tab; event/group organization is currently nested behind Home and event detail routes.
- Create Event is already contextual through `showEventCreationSheet`, but Home also uses a header plus button rather than a more intentional floating glass action.
- `EventDetailView` is implemented inside `iosApp/src/Views/App/ContentView.swift`, making it harder to evolve as a first-class premium screen.
- Home is currently a centered visual carousel with filters, not the requested hierarchy of greeting, featured next event, upcoming list, and subtle empty state.
- Messages are represented by `InboxView`, which is notification/invitation oriented instead of event-conversation oriented.
- Transport, Participants, and Vote screens exist and use shared data, but their hierarchy is still closer to grouped cards than one-primary-intention flows.

## Screen Mapping

| Target screen | Current file(s) | Current fit | Required direction |
| --- | --- | --- | --- |
| Home / À venir | `iosApp/src/Views/Events/HomeView.swift` | Partial | Replace carousel-first layout with greeting, featured next event, upcoming list, floating create action, polished empty/loading states. |
| Event Detail | `iosApp/src/Views/App/ContentView.swift` (`EventDetailView`) | Partial | Extract to dedicated file, add immersive header, event metadata, participants preview, urgent next action, progressive sections, message preview. |
| Create Event | `iosApp/src/Views/Events/CreateEventSheet.swift` | Partial | Convert from large form/cards to five-step lightweight wizard: name, date, place, invite people, confirm. |
| Vote Flow | `iosApp/src/Views/Polls/PollVotingView.swift` | Partial | Reduce instructional clutter; emphasize one question, progress, options, selected state, confirmation feedback, glass capsule action. |
| Transport | `iosApp/src/Views/Events/TransportPlanningView.swift` | Partial | Prioritize route/meeting point, departure/arrival, participants involved, and one primary action; move optimization details into disclosure. |
| Participants | `iosApp/src/Views/Events/ParticipantManagementView.swift` | Partial | Group accepted, pending, declined; move invite action to toolbar or bottom sheet. |
| Messages | `iosApp/src/Views/Inbox/InboxView.swift` | Needs product shift | Keep notification logic if needed, but introduce event conversation hierarchy with search, unread states, compact previews, event context. |
| Profile | `iosApp/src/Views/Profile/ProfileTabView.swift` | Partial | Promote to stable tab destination; reduce dashboard/gamification prominence if it competes with account/settings intent. |
| Groups | No direct destination | Missing | Add a destination-oriented Groups view or repurpose an existing event/group list as a stable tab. |

## Navigation Migration

Current model:
- `WakeveTab`: `.home`, `.inbox`, `.explore`
- `AuthenticatedView` tabs: Accueil, Inbox, Explorer
- Profile is `showProfileSheet`
- Current route state is local `AppView`

Target model:
- `WakeveTab`: `.home`, `.groups`, `.messages`, `.profile`
- Tab labels: `Accueil`, `Groupes`, `Messages`, `Profil`
- Tab items are destinations only.
- Create Event remains `showEventCreationSheet` but is triggered by Home floating glass action, relevant toolbars, or bottom sheets.
- Event-specific actions remain inside event detail or contextual sheets.

Initial code touchpoints:
- `iosApp/src/Models/WakeveTab.swift`
- `iosApp/src/Views/App/ContentView.swift`
- `iosApp/src/Views/Events/HomeView.swift`
- `iosApp/src/Views/Profile/ProfileTabView.swift`
- New or adapted groups/messages destination views.

## Design System Migration

Existing assets:
- `WakeveTheme` has color, typography, spacing, radius, shadows, gradients, and navigation constants.
- `LiquidGlassModifier` supports native `.glassEffect` on iOS 26 with material fallback.
- `WakeveDesignSystemComponents.swift` already provides `WakeveScreenBackground`, `WakeveGlassCard`, `WakeveGlassControl`, `WakeveActionButton`, and `WakeveCircleButton`.

Gaps against the spec:
- `WakeveTheme.Typography` uses fixed sizes rather than semantic iOS text styles.
- `EventGradient.invitation` is still strongly purple-dominant.
- Motion tokens and easing/duration tokens are not centralized.
- No dedicated reusable `LiquidGlassToolbar`, `LiquidGlassTabBar`, `EventHeroCard`, `EventListRow`, `ParticipantAvatarStack`, `VoteOptionCard`, `BottomSheet`, `EmptyState`, or `LoadingSkeleton` components under those concepts.
- Loading still uses `ProgressView` in important places instead of stable skeleton states.
- Empty states are inconsistent: some use `ContentUnavailableView`, some are custom, and Home has a bespoke empty state.

Initial code touchpoints:
- `iosApp/src/Theme/DesignSystem.swift`
- `iosApp/src/Theme/LiquidGlassModifier.swift`
- `iosApp/src/Components/DesignSystem/WakeveDesignSystemComponents.swift`
- `iosApp/src/Components/SharedComponents.swift`
- `iosApp/src/Components/LiquidGlassAnimations.swift`

## Implementation Order

1. Consolidate design tokens and components first.
2. Refactor navigation to Home, Groups, Messages, Profile.
3. Refactor Home because it sets the product feel and create-event entry point.
4. Extract and refactor Event Detail as the primary event workspace.
5. Refactor Create Event into the five-step flow.
6. Refactor Vote, Transport, Participants, and Messages.
7. Replace major loading/empty states with shared components.
8. Add tests and run iOS verification.

## Verification Targets

- `openspec validate refactor-ios-premium-liquid-glass-ux --strict`
- Swift tests for `WakeveTab` target cases and labels.
- Swift tests for create-event step progression and validation.
- Existing contract tests covering transport, organization access, inbox, budget, and meetings.
- Xcode build/test of the iOS app when the local simulator/tooling is available.
- Manual or automated screenshots in dark mode and light mode for Home, Event Detail, Create Event, Vote, Transport, Messages, and Profile.

## Open Decisions Before Implementation

- Whether `Groups` should be a dedicated social groups list, an event groups aggregation, or a renamed/restructured replacement for `Explore`.
- Whether `InboxView` should be renamed/reworked into Messages while preserving notification subfilters, or whether Messages should be a new view with Inbox retained for notification settings.
- Whether to extract all large views from `ContentView.swift` during this change or only `EventDetailView` and navigation-related pieces.
