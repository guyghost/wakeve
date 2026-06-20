# Change: Standardize Wakeve naming and microcopy

## Why
Wakeve uses several overlapping labels for the same user concepts across Android, iOS, server notifications, and assistant-driven surfaces. This creates avoidable ambiguity around events, options, scenarios, participants, votes, and AI suggestions.

## What Changes
- Define a shared FR/EN naming system for primary navigation, business objects, states, actions, and AI-assisted flows.
- Audit visible labels and prioritize ambiguous, inconsistent, or risky copy.
- Align localization and hardcoded strings with the approved naming guidelines in a follow-up implementation pass.
- Prefer user-benefit wording over technical AI wording.

## Impact
- Affected specs: `cross-platform-organization-ux`
- Affected docs: `docs/design/wakeve-naming-guidelines.md`, `docs/design/wakeve-naming-audit.md`, `docs/design/wakeve-voice-and-tone.md`
- Affected code in the implementation pass: Android string resources and Compose hardcoded labels, iOS `Localizable.strings` and SwiftUI hardcoded labels, server notification copy, Siri/App Actions vocabulary.
