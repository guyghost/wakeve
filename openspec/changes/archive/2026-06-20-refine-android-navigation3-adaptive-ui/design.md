## Context
The current Android app uses Compose Navigation 2 with route strings. The official Navigation 3 direction is Compose-first, state-driven navigation using app-owned back stack keys and `NavDisplay`. A complete app-wide migration would affect every route, deep link, and test, so this change keeps the production runtime stable while creating Navigation 3-compatible route keys and notes for a controlled migration.

## Goals
- Adapt based on capabilities: width, height, pointer precision, and actual container constraints.
- Preserve one set of screen composables rather than duplicating phone/tablet screens.
- Keep business logic in ViewModels and mappers.
- Prove adaptive behavior with previews plus device/emulator screenshot or UI tests.

## Non-Goals
- Do not rewrite every Wakeve route to Navigation 3 in this pass.
- Do not introduce form-factor branches such as explicit tablet mode.
- Do not change event domain behavior.

## Decisions
- Keep `NavHostController` and current route strings for production while introducing a small `WakeveNavKey`/adaptive destination model that mirrors Navigation 3's state-owned back stack approach.
- Add a `WakeveAdaptiveInfo` helper computed from constraints and optional platform hints. The helper exposes width class, height class, navigation kind, list-detail eligibility, filter layout kind, grid column count, and compact-chrome eligibility.
- Use `NavigationBar` for compact width and `NavigationRail` for medium/expanded width.
- Use `BoxWithConstraints` for local container decisions:
  - filters scroll horizontally below 600dp and wrap at or above 600dp,
  - cards use adaptive columns based on available width,
  - list-detail is enabled only with enough width and non-compact height.
- Use `rememberSaveable` for route-level selected event, selected filter, and search query so rotation keeps UI state stable.

## Navigation 3 Migration Notes
- Current production runtime remains Navigation 2 because the app has many existing routes, Koin-injected wrappers, deep-link-like route helpers, and broad instrumented tests around route strings.
- The next migration step should replace route strings with serializable Navigation 3 keys, create an app-owned mutable back stack, and use `NavDisplay` to render entries.
- The current adaptive route model keeps content composables independent of Navigation 2 so the `NavDisplay` resolver can reuse the same screens later.

## Verification
- Compile Android debug and Android instrumented tests.
- Run focused unit tests for adaptive helper behavior.
- Run focused Compose UI tests for compact list, expanded list-detail, adaptive grid, filter wrapping, navigation chrome, landscape compact chrome, and rotation state preservation.
- Generate or validate screenshot artifacts for five adaptive breakpoints.
