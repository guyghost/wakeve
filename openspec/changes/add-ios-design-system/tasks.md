## 1. Foundation
- [x] 1.1 Audit existing iOS style definitions and list local color/radius/typography duplicates.
- [x] 1.2 Extend `iosApp/src/Theme/DesignSystem.swift` with namespaced Wakeve tokens.
- [x] 1.3 Update Liquid Glass modifiers to include Reduce Transparency and high-contrast fallbacks.

## 2. Components
- [x] 2.1 Add shared screen background, glass card, action button, and circular icon button components.
- [x] 2.2 Add shared search field, avatar, stacked avatar, section header, list row, and vote segmented control components.
- [x] 2.3 Add SwiftUI previews covering dark, light, event gradient, and accessibility variants.

## 3. Screen Migration
- [x] 3.1 Refactor notification onboarding/preferences to use the design system.
- [x] 3.2 Refactor profile/friends and participant management screens to use the design system.
- [x] 3.3 Refactor event preview/poll voting/create event surfaces to use event gradients, vote controls, cards, and action tokens.
- [x] 3.4 Remove or deprecate local style helpers made redundant by shared components.

## 4. Documentation
- [x] 4.1 Create `docs/guides/ios/design-system.md` with principles, tokens, components, and migration rules.
- [x] 4.2 Link the guide from `docs/guides/ios/README.md` and `docs/README.md`.

## 5. Validation
- [x] 5.1 Build the iOS app or run the closest available Swift/Xcode validation command.
- [ ] 5.2 Run relevant iOS unit/UI tests where available. Blocked locally: Xcode test run failed before tests executed because the disk is full / codesign failed while embedding the shared framework.
- [ ] 5.3 Capture/inspect representative screens for layout, contrast, Dynamic Type, and visual consistency.
