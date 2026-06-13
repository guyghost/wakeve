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

## Production Privacy Constraints

- Production builds do not read the Debug availability override.
- `WakeveAILogger.debugPersonalContext` is gated behind `#if DEBUG` and `WAKEVE_AI_LOG_PERSONAL_CONTEXT=1`.
- The production Foundation Models client records prompt id, duration, timeout, cancellation, availability, and validation metadata, but does not log prompt text, participant names, votes, addresses, prices, or generated personal content.

## Remaining Real-Device Profiling

OpenSpec task `6.6` remains open until a real supported device with Foundation Models available is profiled.

Required evidence:

- Device model and OS build.
- Foundation Models availability state is `.available`.
- Smart Event Draft latency for a normal prompt.
- Cancellation latency after tapping cancel during generation.
- Memory delta before, during, and after generation.
- Confirmation that no personal prompt or generated content appears in production logs.
