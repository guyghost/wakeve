# RED evidence — tasks 2.4 and 2.5

Tasks 2.4 and 2.5 remain unchecked pending review. No Swift production source was changed for this RED slice.

## iOS poll-confirmation contracts

Command:

```bash
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme WakeveApp \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -only-testing:WakeveTests/PollConfirmationWorkflowContractTests
```

Result: **expected RED** (`** TEST FAILED **`). The test target built and ran all seven focused tests on an iPhone 17 Pro simulator clone. Each failed because the production iOS workflow projection is not implemented yet:

- no `PollConfirmationViewModel` render contract for `reviewingResults`, `confirmPrompt`, `confirming`, `failed`, `confirmed.pendingSync`, and `confirmed.synced`;
- no cancel-to-reviewing mapping with a provable zero command/navigation/success-feedback branch;
- no one-in-flight duplicate coalescing contract;
- no retry branch that reuses the same `operationId` and selected slot;
- no committed-receipt-only navigation branch;
- no localized local-save/pending-sync copy contract;
- no stable accessibility identifiers and localized labels for confirm, cancel, progress, error, retry, and pending status.

The failures are assertion failures against missing business behavior, not compiler, destination, or test-discovery failures. XCTest result bundle:

```text
~/Library/Developer/Xcode/DerivedData/iosApp-apbkkjufflidnaalnmfuwfwijqop/Logs/Test/Test-WakeveApp-2026.07.09_21-04-36-+0200.xcresult
```

## iOS architecture guard

Command:

```bash
./scripts/test-ios-poll-confirmation-architecture.sh
```

Result: **expected RED** (exit 1). The guard printed the offending file and call for both current view-layer mutations:

```text
iosApp/src/Views/Polls/PollResultsView.swift:91: _ = try await repository.updateEventStatus(
iosApp/src/Views/Events/ParticipantManagementView.swift:1127: _ = try await repository.updateEventStatus(
FAIL: SwiftUI views must not call updateEventStatus; dispatch the typed shared workflow event instead
FAIL: iOS poll-confirmation architecture has 1 violation(s)
```

The second guard for renamed direct confirmation shortcuts currently passes because no `repository.confirmPollDate`, `confirmDate`, `confirmTimeSlot`, `setConfirmedDate`, or `updateConfirmedDate` call exists under `iosApp/src/Views` yet.

The architecture script is wired into `scripts/test-critical-release-gates.sh` immediately after the existing iOS contract scripts.

## Syntax and whitespace checks

```bash
bash -n scripts/test-ios-poll-confirmation-architecture.sh
bash -n scripts/test-critical-release-gates.sh
git diff --check
```

Both shell syntax checks pass. The touched files pass scoped whitespace checks. The global `git diff --check` is RED because an independently modified JVM test contains pre-existing trailing whitespace at `shared/src/jvmTest/kotlin/com/guyghost/wakeve/repository/DatabaseEventRepositoryConfirmDateTest.kt:28`; this iOS test slice does not modify that file.
