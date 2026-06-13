# Wakeve Release Performance Capture

Generated: 2026-06-13T12:08:21Z

Status: LOCAL EVIDENCE

This report is a reproducible local capture. It does not close the App Store or roadmap device-performance items until the same flows are profiled on representative release hardware.

## Configuration

| Field | Value |
| --- | --- |
| Project | `/Users/guy/Developer/dev/wakeve` |
| Runs per platform | 1 |
| iOS enabled | true |
| Android enabled | true |
| iOS build requested | false |
| Android build requested | false |
| iOS simulator | `iPhone 17 Pro Max` |
| iOS bundle ID | `com.guyghost.wakeve` |
| Android package | `com.guyghost.wakeve` |
| Android activity | `com.guyghost.wakeve.MainActivity` |

## Toolchain

```text
Xcode 27.0
Build version 27A5194q
== Devices ==
-- iOS 18.4 --
-- iOS 26.5 --
-- iOS 27.0 --
    iPhone 17 (57ACA8ED-0497-4661-B429-F3B34E7FA508) (Booted) 
List of devices attached

```

## iOS Cold Start

Measurement: elapsed host time for `xcrun simctl launch --terminate-running-process com.guyghost.wakeve`.

| Run | Elapsed ms | Result |
| --- | ---: | --- |
| 1 | 308.9 | ok |

## Android Cold Start

Status: SKIPPED

No connected Android device or emulator was found.

## Runtime Profiling Checklist

Collect these traces before closing the roadmap performance items:

- iOS Release on a supported physical device: Instruments SwiftUI + Time Profiler for home/list scrolling.
- iOS Release on a supported physical device: create-event sheet from open to saved draft.
- iOS Release on a supported physical device: scenario matrix render, vote, and final selection.
- iOS Release on a supported physical device: WakeveAI generation latency, cancellation latency, and memory peak.
- Android Release on a representative device: cold start with `am start -W`, home/list scrolling, create event, and scenario matrix.

Use focused captures: one trace per flow, record device model, OS version, build number, run count, and any caveats.

Raw command logs for this run were written to:

- `/var/folders/1t/456kc0651bl7mgrc62_m43g80000gn/T//wakeve-performance-profile-98035/logs`

