# Change: Add iOS App Intents and AppIntentsTesting coverage

## Why
Wakeve currently exposes Siri-related behavior through legacy `Intents`/`NSUserActivity` wiring and an `.intentdefinition` file, but it does not have modern Swift `AppIntent`, `AppEntity`, `EntityQuery`, or `AppShortcutsProvider` types that can be exercised with `AppIntentsTesting`.

System surfaces such as Siri, Shortcuts, Spotlight, widgets, and view annotations can regress independently from the main SwiftUI app. Wakeve needs out-of-process App Intents tests so broken intent definitions, entity resolution, or shortcut metadata fail early without relying on fragile UI tests.

## What Changes
- Add a modern iOS App Intents surface for Wakeve event planning actions:
  - `CreateEventIntent`
  - `UpdateEventIntent`
  - `InviteParticipantsIntent`
  - `CreatePollIntent`
  - `VoteIntent`
  - `ProposeTransportIntent`
  - `OpenEventIntent`
  - `SummarizeEventIntent`
- Add focused `AppEntity` models and `EntityQuery` implementations for events, groups, participants, polls, and transport proposals.
- Add discoverability through `AppShortcutsProvider` and metadata suitable for Siri, Shortcuts, Spotlight, widgets, and view annotations.
- Add App Intents tests that explicitly exercise intent definitions, entity queries, Spotlight query results, and view annotations through the local `AppIntentsTesting` API surface.
- Add a dedicated out-of-process XCUI test target named `WakeveAppIntentsTests` using `AppIntentsTesting`.
- Add `Tests/AppIntents/WakeveAppIntentsTests.swift` and `Tests/AppIntents/WakeveIntentTestSupport.swift`.
- Keep tests black-box from app internals: no direct import of Wakeve implementation modules; assertions interact through App Intents Testing and explicit test support hooks only.

## Impact
- Affected specs: `ios-system-intents`
- Affected code:
  - `iosApp/src/AppIntents/**`
  - `iosApp/src/Siri/**`
  - `iosApp/src/Services/**`
  - `iosApp/src/Views/App/ContentView.swift`
  - `iosApp/iosApp.xcodeproj/project.pbxproj`
  - `iosApp/WakeveTest.xctestplan`
  - `iosApp/Tests/AppIntents/**`
- Dependencies/platform:
  - Xcode 27 beta or newer toolchain with `AppIntents` and `AppIntentsTesting`
  - iOS 27 simulator test execution for `AppIntentsTesting`
  - Same development team and Wakeve bundle identifier for the host app/test configuration
