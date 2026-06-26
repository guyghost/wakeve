## 1. Audit and Setup
- [x] 1.1 Audit existing `iosApp/src/Siri/` legacy intent definitions, `Info.plist` intent keys, and current app routing/deep-link behavior.
- [x] 1.2 Confirm local Xcode SDK availability for `AppIntents` and `AppIntentsTesting`, including the iOS 27 simulator availability required by `AppIntentsTesting`.
- [x] 1.3 Define the black-box state preparation and verification boundary for out-of-process tests.
- [x] 1.4 Use `implementation-notes.md` to preserve verified Xcode/AppIntentsTesting assumptions during implementation.

## 2. App Intents Surface
- [x] 2.1 Add a dedicated `iosApp/src/AppIntents/` source area.
- [x] 2.2 Implement narrow App Intent domain service APIs used by the intents.
- [x] 2.3 Implement `EventEntity`, `GroupEntity`, `ParticipantEntity`, `PollEntity`, and `TransportEntity`.
- [x] 2.4 Implement entity queries with stable ID lookup, suggestions, accent-insensitive search, case-insensitive search, partial search, and no-result behavior.
- [x] 2.5 Implement `CreateEventIntent`, `UpdateEventIntent`, `InviteParticipantsIntent`, `CreatePollIntent`, `VoteIntent`, `ProposeTransportIntent`, `OpenEventIntent`, and `SummarizeEventIntent`.
- [x] 2.6 Implement `AppShortcutsProvider` entries and user-facing metadata for Siri, Shortcuts, Spotlight, widgets, and view annotations.
- [x] 2.7 Wire `OpenEventIntent` into one app routing handoff path.
- [x] 2.8 Add view annotation support for selected or visible event, poll, group, and transport entities where Wakeve views expose entities to the system.

## 3. AppIntentsTesting Target
- [x] 3.1 Add an XCUI test target named `WakeveAppIntentsTests` with the same development team as `WakeveApp`.
- [x] 3.2 Configure the target for out-of-process App Intents testing of bundle identifier `com.guyghost.wakeve`.
- [x] 3.3 Add `Tests/AppIntents/WakeveIntentTestSupport.swift`.
- [x] 3.4 Add `Tests/AppIntents/WakeveAppIntentsTests.swift`.
- [x] 3.5 Add the new test target to `WakeveTest.xctestplan` or a dedicated App Intents test plan.
- [x] 3.6 Link the test target against `AppIntentsTesting.framework` from the platform developer frameworks.

## 4. Intent Tests
- [x] 4.1 Test `CreateEventIntent` creates an event and returns an `EventEntity`.
- [x] 4.2 Test `UpdateEventIntent` changes optional event fields.
- [x] 4.3 Test `InviteParticipantsIntent` adds the requested participants.
- [x] 4.4 Test `CreatePollIntent` creates a poll with expected options.
- [x] 4.5 Test `VoteIntent` records a vote on the selected option.
- [x] 4.6 Test `ProposeTransportIntent` creates a transport proposal.
- [x] 4.7 Test `OpenEventIntent` opens/routes to the selected event detail.
- [x] 4.8 Test `SummarizeEventIntent` returns a useful structured summary or dialog.

## 5. Entity Query Tests
- [x] 5.1 Test `EventEntityQuery` lookup by identifier, text, suggestions, absence, accents, partial terms, and case-insensitive terms.
- [x] 5.2 Test `GroupEntityQuery` lookup by name and recent group suggestions.
- [x] 5.3 Test `ParticipantEntityQuery` lookup by display name and lookup scoped to a group.
- [x] 5.4 Test `PollEntityQuery` lookup by event and active poll suggestions.
- [x] 5.5 Test `TransportEntityQuery` lookup by event and active transport suggestions.
- [x] 5.6 Test Spotlight entity search with `AppEntityDefinition.spotlightQuery(_:)` for matching, non-matching, renamed, and deleted event queries.
- [x] 5.7 Test view annotations with `AppEntityDefinition.viewAnnotations()` for the selected or visible event, poll, group, and transport entity state.
- [x] 5.8 Test App Intent definitions are discoverable through `IntentDefinitions(bundleIdentifier:)` for every supported Wakeve intent and entity type.

## 6. System Surface Metadata Tests
- [x] 6.1 Verify Siri/Shortcuts-facing titles, descriptions, parameter labels, and shortcut phrases are present for supported App Intents.
- [x] 6.2 Verify widget and view annotation entity parameters reuse the same entity query behavior as shortcuts.
- [x] 6.3 Verify App Shortcut metadata stays concrete and Wakeve-specific.

## 7. Verification and Documentation
- [x] 7.1 Build the iOS app and App Intents test target.
- [ ] 7.2 Run the App Intents test suite on an iOS 27 simulator without the local `Customer build` skip.
- [x] 7.3 Update iOS Siri/App Intents documentation with the supported intents, entities, and test command.
- [x] 7.4 Run `openspec validate add-ios-app-intents-testing --strict`.
