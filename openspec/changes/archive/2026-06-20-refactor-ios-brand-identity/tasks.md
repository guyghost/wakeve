## 1. Audit
- [x] 1.1 Audit iOS navigation destinations, contextual create actions, toolbar usage, and any custom system-action controls.
- [x] 1.2 Audit local color usage, especially purple-heavy surfaces and toolbar/background tinting.
- [x] 1.3 Audit high-visibility content components: Home event cards, Event Detail header, EmptyState, invitation previews, voting, transport, groups, messages, loading states, and widget/preview surfaces.
- [x] 1.4 Audit iconography for standard action SF Symbol usage versus Wakeve-specific concepts.
- [x] 1.5 Audit light mode, dark mode, Dynamic Type, Reduce Motion, Reduce Transparency, and increased contrast risks.

## 2. Contract Tests
- [x] 2.1 Add iOS contract tests for brand token files: `BrandColor`, `SemanticColor`, `EventMoodPalette`, and `TypographyTokens`.
- [x] 2.2 Add iOS contract tests that tab destinations exclude create-event actions and use clear labels/icons.
- [x] 2.3 Add iOS contract tests that standard actions use SF Symbols and logo usage is limited to brand moments.
- [x] 2.4 Add documentation contract tests for the brand audit, brand guidelines, voice/tone, and motion guidelines.

## 3. Tokens and Guidelines
- [x] 3.1 Create/refactor `BrandColor.swift` with midnight blue, graphite, soft ivory, warm peach, muted lavender, subtle amber, and blue grey tokens.
- [x] 3.2 Create/refactor `SemanticColor.swift` for backgrounds, surfaces, text, borders, separators, selected states, badges, CTA, progress, feedback, and status.
- [x] 3.3 Create/refactor `EventMoodPalette.swift` for evening, travel, birthday, family, dinner, beach, and weekend moods with light/dark variants.
- [x] 3.4 Create/refactor `TypographyTokens.swift` around SF Pro, Dynamic Type, and native hierarchy.
- [x] 3.5 Create or document `IconographyGuidelines` for SF Symbols and Wakeve-specific icons.

## 4. Content Components
- [x] 4.1 Refactor Home and event card surfaces so brand expression lives in content, not persistent navigation.
- [x] 4.2 Refactor `EventHeroCard` and event detail headers with mood imagery/gradients, social details, progress, and decisions.
- [x] 4.3 Refactor EmptyState and LoadingState with calm Wakeve voice, one clear action, stable layout, and accessible motion.
- [x] 4.4 Refactor voting, invitation, group, message, and transport cards to use mood/semantic tokens and human microcopy.
- [x] 4.5 Prepare widget or widget-preview visual components for upcoming event, active vote, guest list, notification preview, and possible Live Activity direction.

## 5. Documentation and Verification
- [x] 5.1 Create `docs/design/wakeve-brand-on-ios-audit.md`.
- [x] 5.2 Create `docs/design/wakeve-brand-guidelines.md`.
- [x] 5.3 Create `docs/design/wakeve-voice-and-tone.md`.
- [x] 5.4 Create `docs/design/wakeve-motion-guidelines.md`.
- [x] 5.5 Run iOS unit/contract tests and relevant KMP checks. Targeted iOS brand/navigation/home/vote/transport/messages/invitation/participants contracts pass.
- [x] 5.6 Capture or verify representative light/dark mode screens after implementation.
- [x] 5.7 Run `openspec validate refactor-ios-brand-identity --strict`.
