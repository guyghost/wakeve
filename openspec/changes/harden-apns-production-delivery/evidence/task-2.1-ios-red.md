# Task 2.1 — iOS registration contract RED evidence

Date: 2026-07-09  
Destination: `platform=iOS Simulator,name=iPhone 17 Pro`  
Scope: `APNsProductionRegistrationContractTests` only

## Command

```bash
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme WakeveApp \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -only-testing:WakeveTests/APNsProductionRegistrationContractTests \
  CODE_SIGNING_ALLOWED=NO
```

## Executable reference behavior result

The test-only reference adapter consumes the reviewed state/event vocabulary through injectable permission, UIApplication/APNs, backend registration, credential, and clock ports. Its spies execute and assert transitions, effects, retry timing, and effect order rather than inspecting source text.

```bash
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme WakeveApp \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -only-testing:WakeveTests/APNsRegistrationReferenceBehaviorTests \
  CODE_SIGNING_ALLOWED=NO
```

- Build and XCTest compilation succeeded.
- All 10 executable reference behavior tests passed.
- Result bundle: `/Users/guy/Library/Developer/Xcode/DerivedData/iosApp-apbkkjufflidnaalnmfuwfwijqop/Logs/Test/Test-WakeveApp-2026.07.09_21-15-13-+0200.xcresult`

## Production-binding RED result

- Build and XCTest compilation succeeded, including `APNsProductionRegistrationContractTests.swift`.
- Combined rerun: 10/10 executable reference behaviors passed, then 13/13 production-binding contracts failed on assertions because the current Swift adapters do not yet expose the reviewed registration machine contract or conform to the planned injectable ports.
- `xcodebuild` exited with status `65` and `** TEST FAILED **`, which is the expected RED state before task 4 implementation.
- Combined result bundle: `/Users/guy/Library/Developer/Xcode/DerivedData/iosApp-apbkkjufflidnaalnmfuwfwijqop/Logs/Test/Test-WakeveApp-2026.07.09_21-16-21-+0200.xcresult`

The executable behaviors and remaining production-binding failures cover:

- launch/resume status-only permission resolution;
- explicit enable from `notDetermined`;
- denied → Settings → active status refresh;
- correlated AppDelegate callbacks and stale callback rejection;
- token rotation and recoverable backend registration;
- authentication-deferred registration;
- unregister completion before JWT clearing;
- logout retry/offline and idempotent already-absent handling;
- per-installation logout preserving a second device;
- parity with reviewed states/events and correlation invariant;
- stable accessible controls for enable, Settings, retry, registered, and misconfigured states.

Accessibility remains deliberately RED at the production binding: the current view has no injectable registration render state and lacks stable accessible enable/retry/misconfigured controls. The supplemental contract therefore requires actual identifiers and localized labels on the real SwiftUI controls; it must be replaced or augmented with hosting-level assertions once task 4.2 introduces the injectable render projection.

Task 2.1 intentionally remains unchecked until review accepts this RED contract evidence.
