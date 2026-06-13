# Release Performance Profiling Runbook

Date: 2026-06-13

Status: local harness ready, device evidence pending.

## Scope

This runbook covers the remaining roadmap performance evidence for:

- iOS and Android cold start.
- Home/list scrolling.
- Event creation.
- Scenario matrix rendering and voting.
- WakeveAI generation latency, cancellation latency, and memory use.

It does not close the roadmap items by itself. Closure requires real captures from representative release hardware, plus the generated report committed or attached to the App Store evidence packet.

## Local Capture Command

Use the profiling helper to collect repeatable local evidence:

```bash
./scripts/profile-release-performance.sh --runs 5
```

For stronger release evidence on machines with the right toolchain and connected targets:

```bash
./scripts/profile-release-performance.sh --build-ios --build-android --runs 5
```

Useful scoped runs:

```bash
./scripts/profile-release-performance.sh --ios-only --build-ios --runs 5
./scripts/profile-release-performance.sh --android-only --build-android --runs 5
```

Reports are written to `docs/performance/release-performance-<timestamp>.md`.
The helper records min, median, p95, max, and average for successful cold-start
samples. When several iOS simulators are booted, `IOS_SIMULATOR` selects the
preferred target before falling back to the first booted simulator.

Builds are bounded so the helper can be used in release gates without hanging on
slow or broken local toolchains:

```bash
IOS_BUILD_TIMEOUT_SECONDS=900 ANDROID_BUILD_TIMEOUT_SECONDS=900 \
  ./scripts/profile-release-performance.sh --build-ios --build-android --runs 5
```

If a build fails or exceeds its timeout, the report records the platform section
as `SKIPPED` and points to the raw build log for that run. The default timeout is
600 seconds per platform build.

Each generated report also includes a runtime profiling matrix for the flows
that still require device traces: cold start, home/list, create event, scenario
matrix, and WakeveAI generation/cancellation/memory. Rows remain
`PENDING_DEVICE_TRACE` until a representative signed build is profiled.

## Device Closure Requirements

Before checking off the roadmap performance items, attach or reference:

- iOS physical device model, OS version, build number, and run count.
- Android physical device or emulator model/API, build variant, and run count.
- Cold-start samples from the script report.
- iOS Instruments SwiftUI + Time Profiler traces for home/list, event creation, scenario matrix, and WakeveAI.
- WakeveAI generation latency, cancellation latency, and memory peak from a supported device where Foundation Models is available.
- Notes for every skipped flow, including whether it is blocked by device support, Apple Intelligence availability, signing, or missing test data.

## Interpretation

Treat script cold-start numbers as process-launch samples. They are useful for regression tracking, but they do not prove first meaningful render or SwiftUI frame stability.

Treat simulator captures as local preflight only. Device traces are required for App Store/release closure because Simulator timing can hide rendering, memory, and Apple Intelligence availability behavior.
