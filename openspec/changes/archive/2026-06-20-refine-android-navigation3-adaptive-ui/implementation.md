# Android Adaptive UI Refinement Implementation Notes

## Audit Findings
- The main Android event flow is Compose-based and enters `EventWorkspaceRoute` from `WakeveNavHost`.
- Production navigation still uses Compose Navigation 2 route strings. This remains intentionally stable for this pass because the app has many route wrappers, auth/deep-link flows, and existing tests tied to `NavHostController`.
- The previous event workspace had a list-detail shell but still used a single `LazyColumn`, always-scrollable filters, bottom-only top-level navigation, and `remember` route state.
- Edge-to-edge support already exists in `MainActivity` through `enableEdgeToEdge()`.

## Implemented
- Added `WakeveAdaptiveInfo` in `ui/designsystem` with width class, height class, pointer precision, navigation kind, filter layout, event grid columns, list-detail eligibility, and compact chrome.
- Added `WakeveAdaptiveNavigationScaffold` for top-level Android navigation:
  - compact width uses `NavigationBar`,
  - medium/expanded width uses `NavigationRail`,
  - compact-height bottom chrome is collapsed.
- Added `WakeveNavKey` as a Navigation 3-ready destination key model. Runtime remains Navigation 2; migration notes are in `design.md`.
- Updated event workspace:
  - `LazyVerticalGrid` for event cards,
  - 1/2/3+ adaptive grid columns,
  - filters scroll below 600dp and wrap above 600dp,
  - list-detail only when width and height support it,
  - exact `"Select an event"` placeholder when nothing is selected,
  - reduced top chrome for compact-height landscape.
- Updated route state to `rememberSaveable` for selected filter, search query, and selected event ID.
- Added previews for phone portrait, phone landscape, foldable, tablet, and desktop.
- Added fake-data screenshot test coverage for phone portrait, phone landscape, foldable, tablet, and desktop-size breakpoints.

## Screenshot Artifacts
Generated screenshots are available under:
- `screenshots/android/adaptive/WakeveAdaptiveScreenshots/phone-portrait.png`
- `screenshots/android/adaptive/WakeveAdaptiveScreenshots/phone-landscape.png`
- `screenshots/android/adaptive/WakeveAdaptiveScreenshots/foldable.png`
- `screenshots/android/adaptive/WakeveAdaptiveScreenshots/tablet.png`
- `screenshots/android/adaptive/WakeveAdaptiveScreenshots/desktop.png`

## Verification
- `./gradlew :composeApp:compileDebugKotlinAndroid --no-configuration-cache` passes.
- `./gradlew :composeApp:testDebugUnitTest --tests com.guyghost.wakeve.ui.designsystem.WakeveAdaptiveInfoTest --tests com.guyghost.wakeve.ui.event.EventWorkspaceModelsTest --no-configuration-cache` passes.
- `./gradlew :composeApp:compileDebugAndroidTestKotlinAndroid --no-configuration-cache` passes.
- `./gradlew :composeApp:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.guyghost.wakeve.ui.event.EventWorkspaceScreenTest,com.guyghost.wakeve.navigation.WakeveAdaptiveNavigationScaffoldTest,com.guyghost.wakeve.ui.event.EventWorkspaceAdaptiveScreenshotTest --no-configuration-cache` passes with 13 tests, 0 failures, 0 errors, 0 skipped on API 32.
- `openspec validate refine-android-navigation3-adaptive-ui --strict` passes.

## Navigation 3 Limitation
Navigation 3 was not fully adopted as the production runtime in this pass. The current code is prepared through `WakeveNavKey` and screen/content boundaries, but replacing the whole app back stack with Navigation 3 `NavDisplay` should be done as a separate migration because it touches auth, deep links, route wrappers, and many existing destinations.
