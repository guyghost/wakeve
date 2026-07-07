## Context
Wakeve is a KMP mobile-first event planning app. Shared business logic and state machines live in Kotlin, while iOS should use native SwiftUI UI patterns. The desired iOS direction is dark-mode-first, Liquid Glass-compatible, emotionally calm, and structured around social event planning.

Existing iOS files already include `DesignSystem.swift`, `LiquidGlassModifier.swift`, reusable components, and views for Home, event creation, participants, transport, polls, inbox, and profile. This change should refine and consolidate those surfaces rather than rewrite domain logic.

## Goals
- Make every major iOS screen answer: where am I, what can I do, and where can I go next.
- Enforce one primary intention per screen.
- Use predictable tabs only for destinations: Home, Groups, Messages, Profile.
- Make event creation contextual, not a permanent tab destination.
- Improve Home, Event Detail, Vote, Transport, Participants, Messages, Create Event, empty states, and loading states.
- Centralize tokens for semantic colors, typography, spacing, radius, blur, opacity, elevation, and motion.
- Support excellent dark mode, coherent light mode, increased contrast, dynamic type, and reduced transparency.

## Non-Goals
- Do not change event lifecycle, polling logic, transport business rules, auth, sync, or repository behavior.
- Do not introduce new backend endpoints.
- Do not redesign Android in this change.
- Do not expose unfinished agents or hidden product features.

## Decisions
- Decision: Treat `ios-design-system` as the owning spec for this refactor.
  Rationale: The request is an iOS presentation and interaction change, and the existing spec already owns iOS tokens, components, Liquid Glass, and design documentation.

- Decision: Keep KMP shared state machines unchanged unless a view needs a small presentation mapper.
  Rationale: The brief asks to preserve business logic and improve native UI. Shared state must remain the source of truth.

- Decision: Use destination-only tabs: Home, Groups, Messages, Profile.
  Rationale: The tab bar should orient the user. Create Event is an action and belongs in a floating action, toolbar, or bottom sheet.

- Decision: Prefer native iOS Liquid Glass APIs where available with fallbacks.
  Rationale: iOS 26 Liquid Glass should use platform-native APIs when possible. Older OS versions and reduced-transparency settings need readable fallbacks.

- Decision: Use progressive disclosure for secondary actions.
  Rationale: Event actions such as edit, delete, share, advanced settings, and transport details should not compete with the primary task.

## Screen Intent Model
- Home: make the next shared moment feel under control.
- Event Detail: make the event feel alive, warm, and shared.
- Vote: make group decision-making feel effortless and fair.
- Transport: reduce logistical stress.
- Participants: clarify who is accepted, pending, or declined.
- Messages: keep event groups connected without becoming a generic chat product.
- Create Event: make creation feel lightweight, with one decision per step.

## Component Model
Reusable iOS components should include:
- `LiquidGlassCard`
- `LiquidGlassButton`
- `LiquidGlassToolbar`
- `LiquidGlassTabBar`
- `EventHeroCard`
- `EventListRow`
- `ParticipantAvatarStack`
- `VoteOptionCard`
- `BottomSheet`
- `EmptyState`
- `LoadingSkeleton`

These components should use centralized tokens and avoid one-off hardcoded styling.

## Risks / Trade-offs
- Risk: Custom glass visuals can reduce readability.
  Mitigation: Use semantic contrast tokens, reduced-transparency fallbacks, and increased-contrast checks.

- Risk: A broad UI refactor can accidentally change behavior.
  Mitigation: Keep changes scoped to SwiftUI views/components and preserve KMP state machine contracts.

- Risk: Large motion effects can hurt performance or accessibility.
  Mitigation: Use subtle, purposeful animations and respect Reduce Motion.

## Migration Plan
1. Audit the current iOS screens and reusable components.
2. Update tokens and base Liquid Glass components first.
3. Refactor navigation and tab structure.
4. Refactor Home and Event Detail as the primary visual anchors.
5. Refactor Vote, Transport, Participants, Messages, and Create Event flow.
6. Add empty/loading states.
7. Add tests and documentation notes.
8. Validate on dark mode, light mode, dynamic type, increased contrast, and reduced transparency.

## Current-State Findings
- The current tab model is `Home`, `Inbox`, and `Explore`; `Profile` is opened as a sheet, and `Groups` is missing as a stable destination.
- Home currently emphasizes filters and a centered visual carousel. The target experience needs a calmer `À venir` hierarchy with a greeting, a featured next event, and upcoming events grouped by relevance or date.
- `EventDetailView` is defined inside `ContentView.swift`, which makes a first-class refactor harder and should be extracted during implementation.
- The existing design system has useful foundations, but motion tokens, shared skeletons, shared empty states, tab/toolbar components, and several named reusable event components are still missing or incomplete.
- Some current colors and event gradients remain purple-heavy; the refactor should rebalance toward graphite, midnight blue, soft ivory, pale blue, muted lavender, and sparse warm accents.

See `implementation-audit.md` for screen-by-screen mapping and migration order.

## Delivery Controls
- `execution-plan.md` defines the implementation slices and proof expected after each slice.
- `acceptance-checklist.md` defines the final product, navigation, visual system, component, business-logic preservation, and verification checks.
- Implementation remains gated until this OpenSpec proposal is approved.
