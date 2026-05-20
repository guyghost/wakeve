# Change: Add a unified iOS design system

## Why
The iOS app currently mixes system forms, bespoke gradients, local color helpers, card styles, and Liquid Glass modifiers across screens. The provided Apple-style references point to a clearer direction: immersive dark surfaces, large rounded panels, bold hierarchy, capsule actions, glass cards, and event-themed gradients.

## What Changes
- Establish a single iOS design system for Wakeve screens, including color roles, typography, spacing, radius, glass surfaces, buttons, list rows, search fields, avatars, and event hero treatments.
- Refactor high-visibility iOS screens to consume shared design tokens and reusable components instead of local hard-coded styles.
- Align onboarding, notification permission/preferences, participant/friend management, profile, poll voting, and event preview screens around the same interaction and visual language.
- Document the system under `docs/` with implementation guidance, component usage, accessibility rules, and screenshot-inspired examples.

## Impact
- Affected specs: `ios-design-system`
- Affected code:
  - `iosApp/src/Theme/DesignSystem.swift`
  - `iosApp/src/Theme/WakeveColors.swift`
  - `iosApp/src/Theme/LiquidGlassModifier.swift`
  - `iosApp/src/Components/`
  - `iosApp/src/Views/OnboardingView.swift`
  - `iosApp/src/Views/ProfileTabView.swift`
  - `iosApp/src/Views/ParticipantManagementView.swift`
  - `iosApp/src/Views/PollVotingView.swift`
  - `iosApp/src/Views/CreateEventSheet.swift`
  - `iosApp/src/Views/Notifications/NotificationPreferencesView.swift`
  - `docs/guides/ios/`
