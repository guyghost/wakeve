## Context
Wakeve is a mobile-first iOS event coordination app. The product should feel calm, premium, social, and warm, while keeping iOS conventions intact for navigation and system actions.

The existing `ios-design-system` spec already requires native tab destinations, premium screen hierarchy, Liquid Glass fallbacks, and centralized tokens. This change narrows the brand rule: Wakeve should be familiar where users rely on platform conventions and custom where event content can communicate mood, group energy, and shared decisions.

## Goals / Non-Goals
- Goals: separate UI layer from content layer, reduce decorative purple usage, introduce mature brand and event mood tokens, make content components more recognizable, document voice and motion, and preserve Dynamic Type, dark mode, Reduce Motion, and Reduce Transparency.
- Non-Goals: replacing native tab/navigation/search/sheet behavior, introducing a custom typeface across functional UI, redesigning Android, changing shared domain models unless required for existing event mood data, or shipping a full widget extension if the app does not already include one.

## Decisions
- Decision: keep navigation, tabs, toolbars, menus, search, sheets, forms, and standard actions native-first.
- Rationale: these areas carry platform expectations and should not be overloaded with branding.

- Decision: concentrate Wakeve brand expression in event and social content surfaces.
- Rationale: event cards, heroes, invitation previews, votes, transport coordination, group cards, and empty states are where custom mood, imagery, social metadata, and microcopy improve recognition without hurting usability.

- Decision: introduce explicit Swift tokens named `BrandColor`, `SemanticColor`, `EventMoodPalette`, and `TypographyTokens`.
- Rationale: implementation should move hard-coded local colors and one-off text hierarchy into reusable, testable tokens while allowing SwiftUI to keep SF Pro and Dynamic Type behavior.

- Decision: use SF Symbols for standard actions and reserve custom iconography for Wakeve-specific concepts.
- Rationale: familiar actions such as share, delete, edit, search, back, close, and add need predictable symbols.

- Decision: document widget and extension direction before committing to a widget target.
- Rationale: visual direction can be validated as previews or static components first; a new extension target should be a follow-up only if needed.

## Risks / Trade-offs
- Broad visual refactors can churn many screens. Mitigation: start with tokens and contract tests, then migrate high-visibility components before lower-priority surfaces.
- Too much brand personality can reduce clarity. Mitigation: keep action labels explicit and reserve more emotional copy for onboarding, empty states, invitations, and event moments.
- Event mood palettes can create contrast regressions. Mitigation: validate light/dark/increased contrast and avoid saturated overlays in dark mode.
- Liquid Glass can be misapplied to content. Mitigation: follow the iOS skill rule: glass belongs to navigation/control layers; content cards should use solid, material-backed, or image-based surfaces unless they are interactive controls.

## Migration Plan
1. Audit the current iOS navigation, tokens, high-visibility content components, local colors, icon usage, logo placement, and motion.
2. Add contract tests for token presence, tab destination rules, content-layer brand expression, SF Symbol usage for standard actions, and documentation deliverables.
3. Create/refactor brand tokens and event mood palettes.
4. Refactor content components in priority order: Home event hero/card, Event Detail header, EmptyState, VoteSummary/VoteCard, GroupCard, MessagePreview, TransportCard, invitation preview.
5. Update docs and preview evidence for light mode, dark mode, Dynamic Type, Reduce Motion, and Reduce Transparency.

## Open Questions
- Should tab labels remain French (`Accueil`, `Groupes`, `Messages`, `Profil`) or move toward the requested `A venir`, `Groupes`, `Messages`, `Profil` wording during this refactor?
- Is there an existing app icon/logo source of truth that should constrain the brand palette and logo usage rules?
