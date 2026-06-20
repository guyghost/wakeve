# WakeveAI Device Verification

Date: 2026-06-13

Status: fallback verified on simulator; real-device profiling still required.

## Scope

This note records verification evidence for `openspec/changes/add-on-device-wakeve-ai`.

WakeveAI remains iOS-only. There is no server fallback. AI output is transient until the user explicitly applies, saves, or sends it through an existing Wakeve action.

## Local Fallback Verification

The Debug build supports a WakeveAI availability override for simulator verification:

```bash
--wakeve-ai-availability=unsupported
```

The override is compiled only under `#if DEBUG` in `iosApp/src/WakeveAI/WakeveAIAvailabilityService.swift`. Supported values are `available`, `disabled`, `not_ready`, `unsupported`, and `unknown`.

Verification run:

```bash
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme WakeveApp \
  -destination 'platform=iOS Simulator,name=iPhone 17' \
  -only-testing:WakeveTests/WakeveAIContractTests \
  -only-testing:WakeveTests/WakeveAIGeneratorTests \
  -only-testing:WakeveTests/WakeveAIValidationTests
```

Result: `TEST SUCCEEDED`. Latest focused run: `/Users/guy/Library/Developer/Xcode/DerivedData/iosApp-apbkkjufflidnaalnmfuwfwijqop/Logs/Test/Test-WakeveApp-2026.06.13_14-40-23-+0200.xcresult`.

Covered fallback evidence:

- `WakeveAIAvailabilityService.debugAvailabilityOverride` maps simulator override values to app availability states.
- `EventDraftGenerator` emits `WakeveAIError.unavailable(.unsupportedDevice)` when no client is injected and availability is unsupported.
- `CreateEventViewModel` maps unavailable, timeout, cancellation, and generic errors to user-visible manual continuation states.

Simulator launch verification:

```bash
XcodeBuildMCP build_run_sim \
  project=iosApp/iosApp.xcodeproj \
  scheme=WakeveApp \
  simulator='iPhone 17' \
  launchArgs='--wakeve-debug-authenticated --wakeve-ai-availability=unsupported'
```

Result: build, install, and launch succeeded.

Artifacts:

- Build log: `/Users/guy/Library/Developer/XcodeBuildMCP/workspaces/wakeve-cf467b3193b0/logs/build_run_sim_2026-06-13T10-55-13-815Z_pid17199_b2b04021.log`
- Runtime log: `/Users/guy/Library/Developer/XcodeBuildMCP/workspaces/wakeve-cf467b3193b0/logs/com.guyghost.wakeve_2026-06-13T10-55-27-860Z_helperpid66608_ownerpid17199_913747a1.log`
- Screenshot: `/var/folders/1t/456kc0651bl7mgrc62_m43g80000gn/T/screenshot_optimized_8457ca64-187f-497d-b9a4-950a226e9d7c.jpg`

Automation limitation: XcodeBuildMCP runtime UI snapshots failed in this environment because the installed Xcode beta does not include the expected `SimulatorKit.framework` private framework path. The fallback path is therefore verified by deterministic tests plus a successful simulator launch with the Debug-only availability override.

### 2026-06-15 Simulator Regression Refresh

Command:

```bash
XcodeBuildMCP test_sim \
  -only-testing:WakeveTests/WakeveAIContractTests \
  -only-testing:WakeveTests/WakeveAIGeneratorTests \
  -only-testing:WakeveTests/WakeveAIValidationTests
```

Result: `TEST SUCCEEDED` on `iPhone 17` simulator. The focused WakeveAI suite passed `22/22` tests.

Artifacts:

- Build log: `/Users/guy/Library/Developer/XcodeBuildMCP/workspaces/wakeve-cf467b3193b0/logs/test_sim_2026-06-15T17-46-54-503Z_pid86608_81c924d6.log`
- Result bundle: `/Users/guy/Library/Developer/XcodeBuildMCP/workspaces/wakeve-cf467b3193b0/result-bundles/test_sim_2026-06-15T17-46-54-504Z_pid86608_52125868.xcresult`

This refresh confirms the simulator-testable WakeveAI contracts, generator fallbacks, timeout handling, validation bounds, and debug availability override remain green. It does not close OpenSpec task `6.6`, because that task requires profiling on a real supported device with Foundation Models available.

Device-profile preparation report: `docs/performance/wakeve-ai-device-profile-2026-06-15T18-04-07Z.md`. Current status is `PENDING_PHYSICAL_IOS_DEVICE`; Xcode only reported a paired Apple Watch and simulated iPhones.

### 2026-06-15 Physical Device Connection Attempt

An iPhone was connected after the initial simulator-only verification.

Device detection:

- CoreDevice sees `iPhone de GuyGhost`, model `iPhone 15 Pro (iPhone16,1)`, as a physical paired device.
- Xcode destinations include the device as `platform:iOS,id=00008130-001E39811A12001C`.
- Instruments still lists the same device under `Devices Offline`.

Device-profile preparation reports:

- `docs/performance/wakeve-ai-device-profile-2026-06-15T19-32-13Z.md`: physical device detected, but not trace-ready.
- `docs/performance/wakeve-ai-device-profile-2026-06-15T19-37-38Z.md`: physical device detected, not trace-ready in Instruments, and signing readiness is incomplete.

Current status is `PHYSICAL_IOS_DEVICE_NOT_TRACE_READY`.

Build command attempted:

```bash
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme WakeveApp \
  -configuration Debug \
  -destination 'platform=iOS,id=00008130-001E39811A12001C' \
  build
```

First result: build did not start because Xcode timed out waiting for the destination to become available. The destination error was `Developer Mode disabled`.

After Developer Mode was enabled, Xcode listed the iPhone as a compatible destination, but the Debug device build failed at signing:

```text
Signing for "WakeveApp" requires a development team.
```

Signing diagnostics from the profile helper:

- `TEAM_ID` environment value is missing.
- `security find-identity -v -p codesigning` reports `0 valid identities found`.
- No local provisioning profile matches bundle ID `com.guyghost.wakeve`.

Before the WakeveAI real-device trace can be captured, configure a concrete Apple Developer `TEAM_ID`, install a valid Apple Development certificate in the login keychain, and ensure a development provisioning profile exists for `com.guyghost.wakeve`. The iPhone must also move from `Devices Offline` to `Devices` in `xcrun xctrace list devices`.

### 2026-06-20 Device Profile Refresh

Device-profile preparation report: `docs/performance/wakeve-ai-device-profile-2026-06-20T19-52-26Z.md`.

Current status remains `PHYSICAL_IOS_DEVICE_NOT_TRACE_READY`.

The refreshed helper now accepts either `TEAM_ID` or `APPLE_TEAM_ID` and records explicit Apple Intelligence and Foundation Models model-asset closure fields. Current local state:

- CoreDevice sees `iPhone de GuyGhost`, model `iPhone 15 Pro (iPhone16,1)`, as a physical paired device.
- Instruments still lists the iPhone under `Devices Offline`.
- `TEAM_ID` / `APPLE_TEAM_ID` is missing from the shell environment.
- `security find-identity -v -p codesigning` reports `0 valid identities found`.
- No local provisioning profile matches bundle ID `com.guyghost.wakeve`.

This refresh is preparation evidence only. It does not close OpenSpec task `6.6`, because no supported physical device has been profiled with Apple Intelligence enabled, Foundation Models `.available`, latency, cancellation, memory, trace, and production-log privacy evidence.

### 2026-06-20 Simulator Regression Refresh

Command:

```bash
XcodeBuildMCP test_sim \
  -only-testing:WakeveTests/WakeveAIContractTests \
  -only-testing:WakeveTests/WakeveAIGeneratorTests \
  -only-testing:WakeveTests/WakeveAIValidationTests
```

Result: `TEST SUCCEEDED` on the configured `iPhone 17` simulator as part of the combined WeatherKit/WakeveAI focused suite. The WakeveAI subset passed `27/27` selected tests.

Artifacts:

- Build log: `/Users/guy/Library/Developer/XcodeBuildMCP/workspaces/wakeve-cf467b3193b0/logs/test_sim_2026-06-20T19-56-28-754Z_pid9347_bb3368ef.log`
- Result bundle: `/Users/guy/Library/Developer/XcodeBuildMCP/workspaces/wakeve-cf467b3193b0/result-bundles/test_sim_2026-06-20T19-56-28-754Z_pid9347_e5479204.xcresult`

This refresh confirms the simulator-testable WakeveAI contracts, fallback generator behavior, timeout handling, validation bounds, locale-aware UI contracts, and debug availability override remain green after localization contract updates. It does not close OpenSpec task `6.6`, because latency, cancellation, memory, Foundation Models availability, and production-log privacy still require a supported physical device.

## Production Privacy Constraints

- Production builds do not read the Debug availability override.
- `WakeveAILogger.debugPersonalContext` is gated behind `#if DEBUG` and `WAKEVE_AI_LOG_PERSONAL_CONTEXT=1`.
- The production Foundation Models client records prompt id, duration, timeout, cancellation, availability, and validation metadata, but does not log prompt text, participant names, votes, addresses, prices, or generated personal content.

## Remaining Real-Device Profiling

OpenSpec task `6.6` remains open until a real supported device with Foundation Models available is profiled.

Required evidence:

- Device model and OS build.
- Apple Intelligence enabled state.
- Foundation Models availability state is `.available`.
- Foundation Models model assets ready state.
- Smart Event Draft latency for a normal prompt.
- Cancellation latency after tapping cancel during generation.
- Memory delta before, during, and after generation.
- Confirmation that no personal prompt or generated content appears in production logs.
