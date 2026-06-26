# iOS App Intents Testing

Wakeve exposes a focused App Intents surface for system integrations such as Siri, Shortcuts, Spotlight, widgets, and entity-aware view annotations.

## Intents

Production intents:

- `CreateEventIntent`: creates a Wakeve event and returns `EventEntity`.
- `UpdateEventIntent`: updates title, date, location, or notes and returns `EventEntity`.
- `InviteParticipantsIntent`: invites selected participants and returns the updated `EventEntity`.
- `CreatePollIntent`: creates a poll for an event and returns `PollEntity`.
- `VoteIntent`: records a vote on an active poll and returns `PollEntity`.
- `ProposeTransportIntent`: creates a transport proposal and returns `TransportEntity`.
- `OpenEventIntent`: opens Wakeve and hands off to `wakeve://event/{id}`.
- `SummarizeEventIntent`: returns a concise text summary with event, poll, and transport state.
- `ViewUpcomingEventsIntent`: returns a concise list of upcoming Wakeve events.

Debug-only test support intents:

- `SeedWakeveTestDataIntent`: resets deterministic App Intents fixtures.
- `ClearWakeveTestDataIntent`: clears deterministic App Intents fixture data.
- `OpenWakeveScreenForTestIntent`: opens an entity-specific DEBUG annotation surface for `EventEntity`, `PollEntity`, `GroupEntity`, or `TransportEntity`.
- `DeleteEventForTestIntent`: deletes an event fixture to verify Spotlight removal and deleted-event errors.
- `FinalizeEventForTestIntent`: marks an event fixture finalized to verify transport proposal errors.
- `ClosePollForTestIntent`: closes a poll fixture to verify closed-vote errors.

The test-only intents are guarded with `#if DEBUG`, set `isDiscoverable = false`, and are used only through `AppIntentsTesting`.

## Entities

- `EventEntity`: `id`, `title`, `date`, `location`, `status`, `participantsCount`.
- `GroupEntity`: `id`, `name`, `membersCount`.
- `ParticipantEntity`: `id`, `displayName`, `status`.
- `PollEntity`: `id`, `eventId`, `question`, `options`, `status`.
- `TransportEntity`: `id`, `eventId`, `driver`, `departure`, `seats`.

Each entity has a stable identifier, display representation, type display representation, and an entity query. Event entities also conform to `IndexedEntity` for Spotlight coverage.

## Tests

The App Intents test target is `WakeveAppIntentsTests`, an out-of-process XCUI test bundle. It does not import Wakeve implementation modules. Tests use:

```swift
let definitions = IntentDefinitions(bundleIdentifier: "com.guyghost.wakeve")
```

Coverage currently includes:

- intent and entity discoverability,
- construction coverage for Siri/Shortcuts-facing intent metadata and required/optional parameters,
- create, update, invite, poll, vote, transport, open, and summarize flows,
- chained Shortcuts-style workflows that pass returned entities into later intents,
- entity query lookup by identifier, text, suggestions, no-result cases, accents, partial terms, and case-insensitive terms,
- group, participant, poll, and transport queries,
- Spotlight query checks for created, renamed, deleted, matching, and non-matching events,
- view annotation coverage through `viewAnnotations()` for event, poll, group, and transport entities,
- negative input coverage for blank event titles, closed polls, duplicate invites, finalized-event transport proposals, missing/deleted events, empty poll options, invalid poll options, and invalid transport seats.

The CI job also checks the generated `Metadata.appintents/extract.actionsdata` artifact after `build-for-testing` to ensure Wakeve-specific intent identifiers and App Shortcut phrases are exported by Xcode. This complements AppIntentsTesting because the current public testing API exposes intent construction and execution, but not shortcut phrase collections as first-class properties.

## Running Locally

Build the app and App Intents tests:

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

Run without rebuilding:

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

On the local Xcode 27 beta runtime, `AppIntentsTesting` currently returns `AppIntentsServicesSecurityErrorDomain` code `803` with "Unable to run internal tests on a Customer build". The test target converts only that specific runtime refusal into `XCTSkip`; all other App Intents errors still fail the suite. On an Apple-authorized runtime, the same tests execute their assertions.

CI compiles the test bundle with:

```bash
SWIFT_ACTIVE_COMPILATION_CONDITIONS='$(inherited) WAKEVE_APP_INTENTS_REQUIRE_RUNTIME_EXECUTION'
```

With that compilation condition set, the customer-build refusal is not converted to `XCTSkip`; it fails the job. This prevents a CI image that has `AppIntentsTesting.framework` and an iOS 27 runtime from silently reporting success without executing the AppIntentsTesting assertions.

After `test-without-building`, CI also parses the xcodebuild summary and fails unless it finds an App Intents run with at least one executed test, zero failures, and zero skipped tests. A CI pass therefore means the AppIntentsTesting assertions actually ran; a local customer-build skip remains diagnostic only.

The CI availability check is intentionally mandatory. If the selected macOS/Xcode image does not include `AppIntentsTesting.framework` or an iOS 27 simulator runtime, the job fails with a configuration error instead of skipping the suite. That keeps the acceptance criterion honest: WakeveAppIntentsTests must execute in CI, not merely be present in the workflow.

## Adding An Intent

1. Add the `AppIntent` under `iosApp/src/AppIntents/`.
2. Keep the intent thin and move shared state behavior into `WakeveIntentStore` or an app service boundary.
3. Add user-facing title, description, parameter labels, dialog, and return value.
4. Add an `AppShortcut` when the action should be discoverable from Shortcuts or Siri.
5. Add an AppIntentsTesting test that constructs the intent from `IntentDefinitions`.

## Adding An Entity Query

1. Add a narrow `AppEntity` with stable `id`, `displayRepresentation`, and `typeDisplayRepresentation`.
2. Add an `EntityStringQuery` and `EnumerableEntityQuery` when users need search, suggestions, or widget configuration.
3. Cover identifier lookup, text lookup, suggestions, no-result behavior, case-insensitive terms, partial terms, and accent-insensitive terms.
4. If the entity should appear in Spotlight, conform it to `IndexedEntity` and add a Spotlight test.

## Spotlight And View Annotations

Spotlight tests use `AppEntityDefinition.spotlightQuery(_:)` through AppIntentsTesting. The suite creates `Week-end Lisbonne`, verifies a Spotlight match for `Lisbonne`, renames it to `Week-end Porto`, verifies the old query no longer matches, deletes it, and verifies the deleted event disappears from Spotlight results.

View annotation coverage uses `AppEntityDefinition.viewAnnotations()` after opening Wakeve through `OpenWakeveScreenForTestIntent`. It asserts that the visible surface annotates the expected `EventEntity`, `PollEntity`, `GroupEntity`, and `TransportEntity`.

`EventDetailView`, poll voting/results, and transport planning apply App Entity annotations on iOS 18.4 and newer. The modifiers are availability-checked so Wakeve can keep the current iOS 18.2 deployment target. Group annotations are verified through the DEBUG AppIntentsTesting surface because the current shared `Event` model does not expose a stable group id to the participant management view.
