# Wakeve Brand on iOS Audit

## Summary
Wakeve should feel native in the UI layer and distinctive in the content layer. The current app already uses destination-only tabs and premium SwiftUI surfaces, but brand expression is still spread across legacy color constants, mixed event themes, and some hard-coded system-action styling.

## Layer Audit

### UI Layer: Keep Native
- Tabs: `À venir`, `Groupes`, `Messages`, `Profil` are destinations. Create Event remains contextual.
- Navigation bars, toolbars, menus, search, forms, sheets, alerts, and standard action rows should keep SwiftUI/iOS behavior.
- Toolbar and tab surfaces should use system backgrounds, materials, or semantic selected tint only.
- The Wakeve logo should not be repeated in routine toolbars.

### Content Layer: Express Wakeve
- Event cards, event heroes, invitation previews, vote summaries, group cards, message previews, transport cards, empty states, loading states, and widget previews are the right places for brand.
- These surfaces should show event mood, social context, participant presence, progress, and decisions.
- Content can use immersive imagery, soft gradients, calm microcopy, and warmer motion as long as contrast and Dynamic Type remain intact.

## Current Risks
- `WakeveColors.swift` still exposes older blue and purple accent tokens. New work should prefer `BrandColor`, `SemanticColor`, and `EventMoodPalette`.
- Some event themes still rely on emoji-heavy treatments. Future component work should replace those with mood palettes, imagery, avatars, and decision states.
- Some utility surfaces use hard-coded blue or purple gradients. These should move to semantic or mood tokens when touched.
- Contract tests now protect tab destination labels and brand token deliverables, but visual screenshots still need to be captured after component migration.

## Priority Components
1. Home featured event and event list rows.
2. Event detail header and `EventHeroCard`.
3. Empty states and loading states.
4. Vote option cards and vote summaries.
5. Invitation previews, group cards, message previews, and transport cards.
6. Widget previews for next event, active vote, guest list, notification preview, and possible Live Activity.

## Acceptance Evidence
- Swift tokens exist for brand, semantic color, event mood, and typography.
- Tabs remain destination-only and use familiar SF Symbols.
- Create Event is not a tab.
- Brand docs define what stays native and what is custom.
- Light mode and dark mode Home captures are stored in `docs/design/evidence/wakeve-ios-home-light.jpg` and `docs/design/evidence/wakeve-ios-home-dark.jpg`.
- Dynamic Type, Reduce Motion, and Reduce Transparency were audited in the migrated token/component layer.
