## Context
Wakeve already declares `AppIntents.framework` in the iOS app target and has Siri metadata in `iosApp/src/Siri/`, but the current implementation is legacy:
- `WakeveSiriManager.swift` donates `NSUserActivity` values and uses `Intents`.
- `WakeveIntents.intentdefinition` lists legacy custom intents.
- No Swift types currently conform to `AppIntent`, `AppEntity`, `EntityQuery`, or `AppShortcutsProvider`.

Apple's current App Intents guidance positions intents, entities, queries, and shortcuts as the integration layer used by Siri, Shortcuts, Spotlight, widgets, and related system experiences. Apple's App Intents Testing documentation describes `AppIntentsTesting` as an out-of-process way to run and test intents, entities, enums, and query logic the same way system surfaces invoke them.

The local Xcode beta toolchain exposes `AppIntentsTesting.framework` under the iPhoneOS and iPhoneSimulator platform developer frameworks. Its Swift interface is available for iOS 27 and includes:
- `IntentDefinitions(bundleIdentifier:)`
- `AppIntentDefinition.makeIntent`
- `AnyAppIntent.run()`
- `AppEntityDefinition.entities(identifiers:)`
- `AppEntityDefinition.entities(matching:)`
- `AppEntityDefinition.suggestedEntities()`
- `AppEntityDefinition.spotlightQuery(_:)`
- `AppEntityDefinition.viewAnnotations()`

## Goals / Non-Goals
- Goals:
  - Provide a small, high-value App Intents surface for Wakeve's core event coordination workflows.
  - Keep App Intent types thin and route domain work through a shared service boundary.
  - Make entity lookup deterministic, display-friendly, and resilient to accent/case/partial matching.
  - Validate App Intent behavior out-of-process with `WakeveAppIntentsTests`.
  - Preserve existing SwiftUI navigation and legacy Siri behavior until replacement is verified.
- Non-Goals:
  - Replace all existing `.intentdefinition` entries in the first implementation pass.
  - Expose every Wakeve screen or full persistence model as App Intents.
  - Depend on SwiftUI UI tests for App Intent correctness.
  - Give the XCUI App Intents target direct access to private app implementation APIs.

## Decisions
- Decision: Introduce a dedicated `iosApp/src/AppIntents/` area.
  - Rationale: The system-facing layer should be discoverable and separated from SwiftUI screens.
- Decision: Use narrow Swift entity structs rather than mirroring KMP domain models.
  - Rationale: App entities need stable identifiers, display metadata, and query behavior, not full persistence graphs.
- Decision: Add a Wakeve intent service boundary.
  - Rationale: Intents should be thin; business logic remains in app services/repositories. The boundary also gives tests an explicit state-preparation and verification path without importing internals.
- Decision: Use a dedicated XCUI target named `WakeveAppIntentsTests`.
  - Rationale: `AppIntentsTesting` is meant to exercise the app's App Intents out-of-process, closer to Siri and Shortcuts than in-process unit tests.
- Decision: Keep the production app deployment target independent from the App Intents test runtime.
  - Rationale: The current app target can continue supporting its configured iOS deployment target, while `WakeveAppIntentsTests` may require an iOS 27 simulator because `AppIntentsTesting` is only available there in the local SDK.
- Decision: Implement `OpenEventIntent` as an open-app handoff.
  - Rationale: Opening an event is navigation, not a background mutation. The app should receive one clear handoff and route to the event detail view.
- Decision: Implement create/update/invite/poll/vote/transport/summarize intents as inline actions where possible.
  - Rationale: These workflows can return structured results or dialog/snippet feedback without requiring full UI, and tests can assert returned entities and state changes.

## Risks / Trade-offs
- Risk: App Intents Testing APIs may depend on the active Xcode beta SDK.
  - Mitigation: Verify against the local SDK module and use Apple documentation as the primary source during implementation.
- Risk: Out-of-process tests need deterministic app state setup without importing internals.
  - Mitigation: Add minimal test support hooks that prepare/reset seeded data through launch arguments, an app-group test store, or other explicit external boundary.
- Risk: Legacy `.intentdefinition` and modern `AppIntent` metadata may overlap.
  - Mitigation: Keep the first pass additive, document duplicate system phrases, and remove legacy definitions only in a later migration if validation proves parity.
- Risk: KMP repository access from App Intents may be expensive or unavailable during background execution.
  - Mitigation: Keep intents thin, fail with clear dialog when required data is unavailable, and prefer local-first cached reads for entity queries.

## Test Strategy
- Add `WakeveAppIntentsTests` as an XCUI test target that uses `AppIntentsTesting`.
- Use `WakeveIntentTestSupport.swift` to:
  - reset app state,
  - seed deterministic events/groups/participants/polls/transport proposals,
  - load App Intent definitions for the Wakeve app bundle identifier,
  - invoke intents and queries out-of-process.
- Cover:
  - successful intent execution and returned entities,
  - state mutation visible through the app/test boundary,
  - entity lookup by stable identifiers,
  - text search with accent-insensitive, case-insensitive, and partial matching,
  - suggested entities,
  - Spotlight entity search through `spotlightQuery(_:)`,
  - view annotations through `viewAnnotations()`,
  - no-result behavior,
  - open-app handoff for `OpenEventIntent`.

## Open Questions
- Which external state boundary should `WakeveAppIntentsTests` use for seeding: launch arguments, app group storage, or a dedicated debug-only URL command?
- Should `SummarizeEventIntent` return a structured `EventSummaryEntity`, dialog text, or both?
- Should `InviteParticipantsIntent` return the updated `EventEntity` or a dedicated `InviteResult` with counts and failures?
