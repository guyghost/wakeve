## Context
Wakeve is a Kotlin Multiplatform app with Android UI in `composeApp`. Android already uses Compose and Material 3 dependencies, but main event flows are inconsistent: some screens are rooted in `commonMain`, some Android-specific screens wrap newer flows, and theme/component tokens are incomplete. The modernization should not rewrite domain logic; it should make the UI layer more coherent, adaptive, and ready for future Material 3 Expressive APIs.

## Goals
- Establish a stable Android design system API that uses Material 3 today and can wrap future Material 3 Expressive components later.
- Refactor core event flows around immutable screen state, stateless content composables, and ViewModel event dispatch.
- Support phone, foldable, tablet, landscape, and desktop-size layouts without duplicating business logic.
- Preserve current Compose Navigation behavior while isolating route state enough to migrate toward Navigation 3 concepts later.
- Add verification with previews and Compose UI tests.

## Non-Goals
- Do not change shared event, poll, participant, or repository domain behavior except where presentation boundaries require a ViewModel adapter.
- Do not replace the whole app navigation stack with an unreleased or incompatible Navigation 3 API.
- Do not introduce legacy Android Views for new UI.
- Do not redesign iOS in this change.

## Decisions
- Create a `ui/designsystem` style package for Android-facing shared Compose code. It will own `WakeveTheme`, `WakeveSpacing`, `WakeveShapes`, adaptive tokens, and reusable component wrappers.
- Keep component wrappers thin and Material 3-backed: `WakeveScaffold`, `WakeveTopAppBar`, `WakeveEventCard`, `WakeveSearchBar`, `WakeveSegmentedControl`, `WakeveButtonGroup`, `WakeveMenu`, and `WakeveProgressIndicator`.
- Use local presentation models for screen rendering. ViewModels map domain models and use-case results into immutable UI state classes.
- Split screens into stateful route composables and stateless content composables:
  - Route composables collect state, dispatch events, and handle side effects.
  - Content composables receive immutable state and lambdas only.
- Add an adaptive event route that chooses:
  - single-pane list or detail on compact width,
  - list-detail side-by-side on medium/expanded width,
  - navigation rail or drawer-compatible structure for large windows.
- Keep current Compose Navigation as the runtime, but introduce typed destination definitions and top-level navigation state so migration to Navigation 3 can happen behind the same route model.

## Risks / Trade-offs
- Existing screens contain local UI behavior that may be hard to move all at once. Mitigation: introduce design system and adaptive shell first, then migrate one core flow at a time.
- Compose Multiplatform/commonMain placement may limit Android-only APIs such as dynamic color and window size helpers. Mitigation: keep Android-only wrappers or expect/actual shims where needed, while common content stays platform-neutral.
- Existing tests may rely on screen text or routes. Mitigation: preserve user-facing routes and add stable test tags for modernized screens.

## Migration Plan
1. Add the design system package and migrate `WakeveTheme` to centralized tokens while preserving current public theme entrypoint.
2. Introduce immutable UI state and content/route split for event list and event detail.
3. Add adaptive event list-detail shell and connect it to existing navigation.
4. Replace deprecated main-flow event creation entry with `DraftEventWizard` route state and ViewModel dispatch.
5. Move poll/date voting and RSVP repository interactions into ViewModels/use cases.
6. Add previews and UI tests for compact and expanded layouts.
7. Remove or quarantine deprecated main-flow screens after replacement.

## Open Questions
- Whether the final package should be named `ui.designsystem` or align with existing `theme`/`ui.components` packages during implementation.
- Whether RSVP should be represented as a dedicated route or as part of the poll/date selection route, depending on the current participant access flow.
