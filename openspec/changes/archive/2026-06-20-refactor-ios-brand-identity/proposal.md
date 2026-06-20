# Change: Refactor iOS Brand Identity

## Why
Wakeve needs a stronger, premium, recognizable iOS identity without making the app feel non-native. The current iOS design system already defines premium navigation and surface patterns, but the brand expression needs clearer boundaries so system UI remains familiar while event content carries Wakeve's personality.

## What Changes
- Define a two-layer iOS model: native UI layer for navigation/actions and branded content layer for events, groups, invitations, voting, transport, messages, empty states, and widgets.
- Mature the Wakeve palette by reducing decorative purple usage and formalizing brand, semantic, and event mood color tokens.
- Add requirements for event mood palettes, immersive event visuals, human microcopy, SF Symbol usage, logo restraint, motion behavior, and widget/extension brand previews.
- Add design documentation deliverables for brand audit, brand guidelines, voice/tone, and motion guidelines.
- Refactor high-visibility iOS content components after approval while preserving native tab, toolbar, sheet, search, form, and context menu behavior.

## Impact
- Affected specs: `ios-design-system`
- Affected code:
  - `iosApp/src/Theme/`
  - `iosApp/src/Components/DesignSystem/`
  - `iosApp/src/Components/`
  - `iosApp/src/Views/App/ContentView.swift`
  - `iosApp/src/Views/Events/`
  - `iosApp/src/Views/Polls/`
  - `iosApp/src/Views/Inbox/`
  - `iosApp/src/Views/Profile/`
  - iOS widgets/extensions if present or introduced
- Affected docs:
  - `docs/design/wakeve-brand-on-ios-audit.md`
  - `docs/design/wakeve-brand-guidelines.md`
  - `docs/design/wakeve-voice-and-tone.md`
  - `docs/design/wakeve-motion-guidelines.md`
