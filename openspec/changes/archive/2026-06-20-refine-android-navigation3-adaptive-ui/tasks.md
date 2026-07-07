## 1. Audit
- [x] 1.1 Document current adaptive gaps: Navigation 2 runtime, chrome behavior, event grid, filter wrapping, rotation state, and screenshot coverage.
- [x] 1.2 Confirm main Android event screens remain Compose-based and identify any legacy route dependencies.

## 2. Adaptive Foundation
- [x] 2.1 Add `WakeveAdaptiveInfo` and helpers for width, height, pointer precision, navigation kind, grid columns, filter layout, and compact chrome.
- [x] 2.2 Add Navigation 3-ready route key model and migration notes.

## 3. Navigation and Layout
- [x] 3.1 Add adaptive navigation scaffold using bottom navigation on compact width and navigation rail on larger widths.
- [x] 3.2 Refine event list/detail to show "Select an event" placeholder when no event is selected.
- [x] 3.3 Convert event list cards to adaptive grid columns.
- [x] 3.4 Convert filters to horizontal scroll on small containers and wrapping layout from 600dp.
- [x] 3.5 Improve landscape compact-height behavior and preserve selected event/filter/search through rotation.

## 4. Previews and Screenshot/UI Tests
- [x] 4.1 Add previews for phone portrait, phone landscape, foldable, tablet, and desktop-size event workspace.
- [x] 4.2 Add fake-data screenshot/UI tests for the five adaptive breakpoints.
- [x] 4.3 Add unit tests for adaptive helper decisions.

## 5. Verification
- [x] 5.1 Run Android compile, unit tests, instrumented test compile, and focused connected UI tests.
- [x] 5.2 Run `openspec validate refine-android-navigation3-adaptive-ui --strict`.
- [x] 5.3 Update implementation notes with executed commands, screenshot artifacts, and any Navigation 3 limitations.
