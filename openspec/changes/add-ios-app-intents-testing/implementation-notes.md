# Implementation Notes: iOS App Intents Testing

These notes capture verified local toolchain and project details for the implementation pass after OpenSpec approval.

## Local Toolchain Evidence

- Xcode: `Xcode 27.0`, build `27A5194q`.
- iPhone simulator SDK: `/Applications/Xcode-beta.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/SDKs/iPhoneSimulator27.0.sdk`.
- `AppIntentsTesting.framework` exists at:
  - `/Applications/Xcode-beta.app/Contents/Developer/Platforms/iPhoneSimulator.platform/Developer/Library/Frameworks/AppIntentsTesting.framework`
  - `/Applications/Xcode-beta.app/Contents/Developer/Platforms/iPhoneOS.platform/Developer/Library/Frameworks/AppIntentsTesting.framework`
- Local `AppIntentsTesting.framework` metadata reports:
  - `DTPlatformVersion = 27.0`
  - `MinimumOSVersion = 26.4`
  - Swift interface availability annotations use `iOS 27.0`.
- Available iOS 27 simulator devices include:
  - `iPhone 17 Pro`
  - `iPhone 17e`
  - `iPhone Air`
  - `iPad Pro 13-inch (M5)`
  - `iPad Pro 11-inch (M5)`

## Existing Wakeve iOS State

- App target: `WakeveApp`
- App bundle identifier: `com.guyghost.wakeve`
- Existing unit test target: `WakeveTests`
- Existing test plan: `iosApp/WakeveTest.xctestplan`
- Current Xcode project list includes:
  - Targets: `WakeveApp`, `WakeveTests`
  - Scheme: `WakeveApp`
- `AppIntents.framework` is already linked by `WakeveApp`.
- Current Siri integration is legacy:
  - `iosApp/src/Siri/WakeveSiriManager.swift` uses `Intents` and `NSUserActivity`.
  - `iosApp/src/Siri/WakeveIntents.intentdefinition` defines legacy custom intents.
  - `iosApp/src/Info.plist` declares legacy `INIntentsSupported`.
- No current Swift source under `iosApp/src` defines `AppIntent`, `AppEntity`, `EntityQuery`, or `AppShortcutsProvider`.

## Xcode Target Shape

Create a new target named `WakeveAppIntentsTests`.

Expected target characteristics:
- Product type: `com.apple.product-type.bundle.ui-testing`
- Product: `WakeveAppIntentsTests.xctest`
- Target dependency: `WakeveApp`
- Test target: `WakeveApp`
- Development team: same as `WakeveApp`
- App under test bundle identifier: `com.guyghost.wakeve`
- Deployment/runtime: an iOS simulator that supports `AppIntentsTesting`
- Frameworks:
  - `XCTest.framework`
  - `AppIntentsTesting.framework`
- Framework search path for `AppIntentsTesting.framework` may need:
  - `$(PLATFORM_DIR)/Developer/Library/Frameworks`

Do not configure `WakeveAppIntentsTests` as an in-process unit test bundle. It must remain an out-of-process XCUI test target and must not import private Wakeve app modules.

## Test File Locations

The objective requires:
- `Tests/AppIntents/WakeveAppIntentsTests.swift`
- `Tests/AppIntents/WakeveIntentTestSupport.swift`

Because the current Xcode project uses file system synchronized groups for source folders, the implementation pass should either:
- add `iosApp/Tests/AppIntents/` as a synchronized root group for the new target, or
- add an explicit target group under the project if synchronized groups are not reliable for UI test membership.

Keep the path named `Tests/AppIntents` in the project tree so the required filenames are directly discoverable.

## AppIntentsTesting API Shape

The local Swift interface exposes these relevant APIs:

```swift
import AppIntentsTesting

let definitions = IntentDefinitions(bundleIdentifier: "com.guyghost.wakeve")

let createEvent = definitions.intents["CreateEventIntent"]
let intent = createEvent.makeIntent(
    title: "Anniversaire Emma",
    date: someDate,
    location: "Paris",
    notes: "Prepare cake"
)
let result = try await intent.run()

let events = definitions.entities["EventEntity"]
let byIdentifier = try await events.entities(identifiers: ["event-emma"])
let byText = try await events.entities(matching: "anniversaire")
let suggestions = try await events.suggestedEntities()
let spotlight = try await events.spotlightQuery("week-end")
let annotations = try await events.viewAnnotations()
```

The exact intent and entity identifiers should be verified once the Swift `AppIntent` and `AppEntity` types compile, because App Intents type identifiers can include module-qualified names depending on how the app exports them.

## Required Test Support Boundary

`WakeveAppIntentsTests` must not import Wakeve implementation modules. State setup and assertions need an explicit black-box boundary.

Preferred implementation order:
1. Add a test-only state reset/seed boundary that is available to the app when launched for UI/App Intents tests.
2. Keep seed data deterministic:
   - Event: `Anniversaire Emma`
   - Event: `Week-end Famille`
   - Group: `Famille`
   - Participants with stable display names and statuses
   - Active poll with stable option identifiers
   - Active transport proposal with stable departure and seats
3. Expose verification through the same external boundary or through App Intents query results.

Acceptable boundary candidates:
- launch arguments and environment read by the app on startup,
- app group test storage,
- debug-only URL commands guarded by test-only launch configuration.

Avoid:
- importing `WakeveApp` or KMP internals in the UI test target,
- depending on SwiftUI element hierarchy for App Intent assertions,
- relying on localized UI text to prove App Intent state changes.

## Intent Coverage Matrix

| Intent | Parameters | Expected result | State evidence |
|--------|------------|-----------------|----------------|
| `CreateEventIntent` | title, date, location, group, notes | `EventEntity` | Event appears through `EventEntityQuery` by id/text |
| `UpdateEventIntent` | event, title?, date?, location?, notes? | `EventEntity` | Returned and queried event reflect updated fields |
| `InviteParticipantsIntent` | event, participants | `EventEntity` or `InviteResult` | Participant count/status changes |
| `CreatePollIntent` | event, question, options | `PollEntity` | Poll query returns question/options/status |
| `VoteIntent` | poll, option | `PollEntity` or `VoteResult` | Poll result reflects recorded vote |
| `ProposeTransportIntent` | event, departure, seats, time | `TransportEntity` | Transport query returns proposal for event |
| `OpenEventIntent` | event | app navigation | App opens/routes to event detail through handoff state |
| `SummarizeEventIntent` | event | `EventSummaryEntity` or structured dialog | Summary includes title, date, location, status, participant count, poll, transport |

## Entity Query Coverage Matrix

| Entity | Required query coverage |
|--------|-------------------------|
| `EventEntity` | id, text, suggestions, no result, accents, partial, case-insensitive, Spotlight |
| `GroupEntity` | group name, recent suggestions |
| `ParticipantEntity` | display name, scoped-to-group lookup |
| `PollEntity` | by event, active suggestions |
| `TransportEntity` | by event, active suggestions |

## Verification Commands

Run after implementation:

```bash
openspec validate add-ios-app-intents-testing --strict
```

Expected iOS build/test shape:

```bash
xcrun xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme WakeveApp \
  -testPlan WakeveTest \
  -destination 'platform=iOS Simulator,OS=27.0,name=iPhone 17 Pro' \
  test
```

If the App Intents suite is placed in a dedicated test plan, replace `WakeveTest` with that plan and document the command in the iOS App Intents docs.

## Verification Evidence

Verified on 2026-06-25 with Xcode 27 beta build `27A5194q`:

```bash
xcrun swiftc -typecheck \
  -target arm64-apple-ios18.2-simulator \
  -sdk "$(xcrun --sdk iphonesimulator --show-sdk-path)" \
  iosApp/src/AppIntents/WakeveIntentStore.swift \
  iosApp/src/AppIntents/WakeveIntentEntities.swift \
  iosApp/src/AppIntents/WakeveAppIntents.swift
```

Result: passed.

```bash
xcrun xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme WakeveApp \
  -testPlan WakeveTest \
  -destination 'platform=iOS Simulator,OS=27.0,name=iPhone 17 Pro' \
  -only-testing:WakeveAppIntentsTests \
  build-for-testing \
  CODE_SIGNING_ALLOWED=NO
```

Result: `TEST BUILD SUCCEEDED`. The app target emitted `Metadata.appintents` and localized `nlu.appintents` bundles, and `WakeveAppIntentsTests` linked against `AppIntentsTesting.framework`.

The generated `Metadata.appintents/extract.actionsdata` artifact includes the supported Wakeve intent identifiers, entity query metadata, parameter labels, shortcut titles, and shortcut phrases for:

- `CreateEventIntent`
- `UpdateEventIntent`
- `InviteParticipantsIntent`
- `CreatePollIntent`
- `VoteIntent`
- `ProposeTransportIntent`
- `OpenEventIntent`
- `SummarizeEventIntent`
- `ViewUpcomingEventsIntent`

The same artifact also includes concrete App Shortcut phrases such as `Create event with ${applicationName}`, `Plan with ${applicationName}`, `Invite participants with ${applicationName}`, `Propose transport with ${applicationName}`, and `Show upcoming events in ${applicationName}`.

```bash
xcrun xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme WakeveApp \
  -testPlan WakeveTest \
  -destination 'platform=iOS Simulator,OS=27.0,name=iPhone 17 Pro' \
  -only-testing:WakeveAppIntentsTests \
  test-without-building \
  CODE_SIGNING_ALLOWED=NO
```

Result: `TEST EXECUTE SUCCEEDED`; 9 tests were skipped by the explicit guard for `AppIntentsServicesSecurityErrorDomain` code `803` because the local iOS 27 runtime reports "Unable to run internal tests on a Customer build". This skip is limited to that Apple runtime refusal. Other App Intents failures still fail the test suite.

The 9 discovered tests now cover:

- intent/entity discovery, including DEBUG-only test support intents,
- Siri/Shortcuts metadata construction and entity suggestion reuse,
- create/update/invite/poll/vote/transport/open/summarize flows,
- chained intent workflows where returned entities feed later intents,
- event/group/participant/poll/transport entity queries,
- Spotlight created, renamed, deleted, matching, and non-matching event queries,
- view annotations for `EventEntity`, `PollEntity`, `GroupEntity`, and `TransportEntity`,
- negative cases for blank titles, closed polls, duplicate invites, finalized-event transport proposals, deleted-event update/open, empty poll options, invalid poll options, and invalid transport seats.

`EventDetailView`, poll voting/results, and transport planning apply App Entity annotations on iOS 18.4 or newer. A DEBUG-only annotation surface is used by `OpenWakeveScreenForTestIntent` to provide deterministic AppIntentsTesting coverage for event, poll, group, and transport annotations without relying on fragile UI navigation or app database setup.

Additional local metadata verification:

```bash
APP_BUNDLE="$(find ~/Library/Developer/Xcode/DerivedData -path '*/Build/Products/Debug-iphonesimulator/Wakeve.app' -type d | head -n 1)"
METADATA="$APP_BUNDLE/Metadata.appintents/extract.actionsdata"
strings "$METADATA" | grep -F "ViewUpcomingEventsIntent"
```

Result: passed. The CI job `ios-app-intents-tests` runs the same metadata presence checks after `build-for-testing` when an Xcode image with `AppIntentsTesting.framework` and an iOS 27 simulator runtime is available.

CI compiles the test bundle with `SWIFT_ACTIVE_COMPILATION_CONDITIONS='$(inherited) WAKEVE_APP_INTENTS_REQUIRE_RUNTIME_EXECUTION'`. With that compilation condition enabled, `AppIntentsServicesSecurityErrorDomain` code `803` is treated as a test failure instead of a local `XCTSkip`, so a compatible CI image cannot silently pass without executing the AppIntentsTesting assertions.

This strict behavior was verified locally by rebuilding `WakeveAppIntentsTests` with the compilation condition and re-running `test-without-building` on the local customer-build runtime. The strict run failed with 9 failures on `AppIntentsServicesSecurityErrorDomain` code `803`, proving the CI mode does not mask runtime refusal.

The CI job also parses the `xcodebuild` test summary and fails unless it finds a non-zero executed test count, zero failures, and zero skipped tests. This is the required evidence for marking the AppIntentsTesting CI gate as truly executed rather than merely discovered or skipped.

The CI availability check is mandatory: missing `AppIntentsTesting.framework` or missing iOS 27 simulator runtime fails the job instead of skipping it. This intentionally forces the workflow onto a compatible Apple runtime before the OpenSpec task `7.2` can be checked.
