# Wakeve Brand Guidelines for iOS

## Principle
Wakeve is familiar where iOS conventions matter and custom where shared moments need identity.

## What Stays Native
- Tab bar destinations.
- Navigation titles and back behavior.
- Toolbars and standard actions.
- Search fields, forms, menus, alerts, context menus, and sheets.
- Share, delete, edit, search, back, close, add, and confirm actions.

These areas should use SwiftUI/iOS conventions, SF Symbols, accessible labels, and semantic tint only.

## What Becomes Wakeve
- Event hero cards.
- Event detail headers.
- Invitation previews.
- Vote summaries and vote choices.
- Group cards and participant presence.
- Transport coordination cards.
- Message previews.
- Empty states and first-run moments.
- Widget and notification previews.

These areas can use `BrandColor`, `SemanticColor`, `EventMoodPalette`, social metadata, progress, decisions, and warm microcopy.

## Color System
- `BrandColor`: core identity palette, including midnight blue, graphite, soft ivory, warm peach, muted lavender, subtle amber, blue grey, and pale blue.
- `SemanticColor`: function-based colors for content surfaces, native chrome, text, borders, selection, CTA, progress, confirmation, warning, and destructive states.
- `EventMoodPalette`: content mood palettes for evening, travel, birthday, family, dinner, beach, and weekend events.

Avoid decorative saturated purple. Use color to communicate hierarchy, grouping, status, selection, progress, feedback, or event mood.

## Typography
Wakeve uses SF Pro through SwiftUI dynamic text styles. Functional UI should stay readable and native. Use custom or emotional typography only in brand moments such as onboarding, large event moments, empty states, or marketing surfaces.

## Iconography
Use SF Symbols for standard actions:
- share: `square.and.arrow.up`
- delete: `trash`
- edit: `pencil`
- search: `magnifyingglass`
- back: `chevron.backward`
- close: `xmark`
- add: `plus`

Wakeve-specific concepts can use a restrained custom direction or expressive SF Symbol choices: shared moment, group, event mood, social vote, coordination, and shared travel.

## Logo Use
Show the Wakeve logo only in brand moments:
- onboarding
- splash or launch loading
- first home experience
- empty states
- App Store and marketing
- widget previews when useful

Do not place the logo in every toolbar or repeated navigation surface.

## Accessibility
- Preserve contrast in light and dark mode.
- Support Dynamic Type without truncating core labels.
- Respect Reduce Motion and Reduce Transparency.
- Keep tappable controls at least 44 pt high.
- Prefer native controls for predictable VoiceOver behavior.
