# Design: Unified iOS Design System

## Context
The screenshots show five related iOS experiences:
- Notification onboarding: black modal surface, large centered copy, strong blue primary capsule, secondary text action.
- Friend discovery: near-black background, oversized title, rounded search field, circular avatars, compact gray invite capsules.
- Profile/friends hub: immersive warm gradient header, circular close/confirm control, large display name, grouped glass list rows.
- Event invitation preview: purple-to-blue event gradient, floating circular back button, pale capsule next action, RSVP segmented control, stacked rounded detail cards.
- Event detail preview: same event gradient language with weather, route, and map cards.

Current code has useful foundations (`DesignSystem.swift`, `WakeveColors.swift`, `LiquidGlassModifier.swift`) but many screens still define local palettes, one-off card backgrounds, hard-coded gradients, and inconsistent radii. The design system should make the screenshot direction reproducible instead of screen-specific.

## Goals
- Create one source of truth for iOS visual tokens and reusable SwiftUI components.
- Preserve native iOS behavior and Liquid Glass fallbacks for older OS versions.
- Make event screens expressive while keeping account/settings/list screens quiet and readable.
- Keep Dynamic Type, VoiceOver, Reduce Transparency, and high contrast usable.
- Document how to choose between base surfaces, glass surfaces, event gradients, and system forms.

## Non-Goals
- Redesign Android or Web in this change.
- Replace the shared KMP domain layer.
- Introduce remote design tooling or external design dependencies.
- Rebuild navigation flows unrelated to visual consistency.

## Visual Direction

### Foundation
- **App background:** default to near-black in immersive flows and system grouped background in utility flows.
- **Event background:** use theme gradients keyed by event type, with a dark bottom fade so text and cards remain legible.
- **Surface hierarchy:** use three standardized surfaces: base page, elevated glass card, and action capsule.
- **Shape language:** circular icon controls, capsule primary actions, large continuous cards, and grouped list rows.
- **Typography:** SF Pro system fonts with large bold titles for screen intent, semibold row labels, and muted metadata.

### Screenshot-Inspired Tokens
- **Dark base:** near-black `#111114` for profile/list flows.
- **Event night base:** navy `#061B4F` / `#071A3E` for invitation detail cards.
- **Primary action:** iOS blue for permission/utility confirmation.
- **Event action:** pale pink/lilac capsule on purple event backgrounds.
- **Neutral capsule:** translucent gray for secondary invite buttons.
- **Warm profile accent:** orange/brown gradient reserved for profile identity surfaces.
- **Event accent:** purple/magenta/blue gradient reserved for invitation/event creation previews.

## Proposed SwiftUI Structure

### Tokens
Extend `DesignSystem.swift` into explicit namespaces:
- `WakeveTheme.ColorToken`
- `WakeveTheme.Typography`
- `WakeveTheme.Spacing`
- `WakeveTheme.Radius`
- `WakeveTheme.Shadow`
- `WakeveTheme.EventGradient`

The existing `AdaptiveColors`, `Typography`, `Spacing`, and `CornerRadius` APIs can remain as compatibility shims while new screens adopt the namespaced tokens.

### Components
Add reusable components under `iosApp/src/Components/DesignSystem/`:
- `WakeveScreenBackground`: base, utility, profile, and event variants.
- `WakeveGlassCard`: standard card shell with Liquid Glass on iOS 26+ and material fallback.
- `WakeveActionButton`: primary, secondary, neutral, destructive, and eventNext variants.
- `WakeveCircleButton`: close, back, confirm, more, microphone, camera variants.
- `WakeveSearchField`: rounded search field with leading/trailing icons.
- `WakeveListRow`: icon/avatar/title/subtitle/trailing-action row.
- `WakeveSegmentedVoteControl`: Yes/No/Maybe control matching the event invitation references.
- `WakeveAvatar`: image, initials, stacked group, and badge variants.
- `WakeveSectionHeader`: consistent section labels for Friends, Overview, Suggestions.

### Screen Adoption
Refactor in this order:
1. Theme and base components.
2. Profile/friends surfaces (`ProfileTabView`, participant/friend rows).
3. Notification onboarding/preferences.
4. Event invitation/poll preview surfaces (`CreateEventSheet`, `PollVotingView`).
5. Remaining sheets that reuse cards/buttons/search fields.

## Accessibility
- All tappable controls must meet a 44pt minimum hit target.
- Primary and secondary actions must remain distinguishable without color alone.
- Text must support Dynamic Type without clipping; oversized display titles use `minimumScaleFactor` only when layout fallback exists.
- Use `.accessibilityLabel` for icon-only buttons and avatar groups.
- When Reduce Transparency is enabled, glass surfaces fall back to opaque high-contrast fills.

## Documentation
Create `docs/guides/ios/design-system.md` and link it from `docs/guides/ios/README.md` and `docs/README.md`. The guide should include:
- Design principles from the reference screenshots.
- Token tables.
- Component catalog.
- Do/don't examples for gradients, glass, cards, and action buttons.
- Migration checklist for future iOS screens.

## Verification
- Build the iOS target or at minimum compile affected Swift files through Xcode build tooling.
- Add focused snapshot or view tests where current test infrastructure supports it.
- Manually verify representative screens in light/dark mode and Dynamic Type.
- Confirm no screen relies on local duplicated color palettes after migration.
