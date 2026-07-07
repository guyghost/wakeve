## 1. Audit and Architecture
- [x] 1.1 Record final UI audit findings in implementation notes, including Compose screens, deprecated screens, and any remaining legacy/non-Material patterns.
- [x] 1.2 Identify current event list, event detail, event creation, poll/date, and RSVP route ownership in `WakeveNavHost`.
- [x] 1.3 Define package placement for design system, adaptive layout, route models, and preview fixtures.

## 2. Design System Foundation
- [x] 2.1 Create centralized `WakeveTheme` foundation with light/dark/dynamic color, typography, shapes, spacing, elevation, and layout tokens.
- [x] 2.2 Add reusable Material 3 wrappers for scaffold, top app bar, cards, list rows, chips, buttons, empty/error/loading states, and offline/sync indicators.
- [x] 2.3 Add expressive-ready wrappers for segmented lists, search bar, button groups, menus, and progress indicators.
- [x] 2.4 Migrate hardcoded colors/sizes from main event screens to theme tokens where practical.

## 3. Immutable UI State and ViewModels
- [x] 3.1 Add immutable UI state models for event list, event detail, event creation wizard route, poll/date selection, and RSVP.
- [x] 3.2 Move event filtering and presentation mapping out of `HomeScreen` into ViewModel or presentation mapper code.
- [x] 3.3 Move poll/date vote submission out of `PollVotingScreen` into ViewModel/use-case handling.
- [x] 3.4 Ensure composables receive state and callbacks only, with no repository writes or domain object construction inside content composables.

## 4. Main Flow Refactor
- [x] 4.1 Refactor event list/home into route and stateless content composables using Wakeve components.
- [x] 4.2 Refactor event detail into route and stateless content composables using Wakeve components.
- [x] 4.3 Route event creation through `DraftEventWizard` and remove deprecated `EventCreationScreen` from the main flow.
- [x] 4.4 Refactor poll/date selection and poll results to Material 3 components and ViewModel-owned state.
- [x] 4.5 Add or refactor RSVP participant response UI to reusable Material 3 components.

## 5. Adaptive Navigation and Layout
- [x] 5.1 Add adaptive window/layout classification for compact, medium, and expanded widths.
- [x] 5.2 Implement single-pane event list/detail navigation on phones.
- [x] 5.3 Implement side-by-side event list/detail layout on tablets, foldables, landscape, and desktop-size windows.
- [x] 5.4 Add navigation state that is compatible with current Compose Navigation and isolated for future Navigation 3 migration.
- [x] 5.5 Ensure main screens work in portrait and landscape without clipping or overlapping content.

## 6. Previews and Tests
- [x] 6.1 Add preview fixtures for representative event, poll, and RSVP states.
- [x] 6.2 Add Compose previews for phone, tablet, and foldable sizes for core screens.
- [x] 6.3 Add Compose UI tests for event list, event detail, adaptive list-detail, event creation wizard entry, and poll/RSVP voting.
- [x] 6.4 Run relevant unit and Android UI tests and document any skipped tests or emulator requirements.

## 7. Cleanup and Documentation
- [x] 7.1 Remove or quarantine deprecated main-flow UI after replacement.
- [x] 7.2 Update docs or implementation notes for the Android UI architecture and design system usage.
- [x] 7.3 Run `openspec validate refactor-android-adaptive-compose --strict`.
