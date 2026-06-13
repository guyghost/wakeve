# Wakeve Release Performance Capture

Generated: 2026-06-13T13:29:50Z

Status: LOCAL EVIDENCE

This report is a reproducible local capture. It does not close the App Store or roadmap device-performance items until the same flows are profiled on representative release hardware.

## Configuration

| Field | Value |
| --- | --- |
| Project | `/Users/guy/Developer/dev/wakeve` |
| Runs per platform | 1 |
| iOS enabled | true |
| Android enabled | false |
| iOS build requested | false |
| Android build requested | false |
| iOS simulator | `iPhone 17` |
| iOS bundle ID | `com.guyghost.wakeve` |
| Android package | `com.guyghost.wakeve` |
| Android activity | `com.guyghost.wakeve.MainActivity` |
| iOS build timeout seconds | 600 |
| Android build timeout seconds | 600 |

## Toolchain

```text
Xcode 27.0
Build version 27A5194q
== Devices ==
-- iOS 18.4 --
-- iOS 26.5 --
-- iOS 27.0 --
    iPhone 17 Pro Max (41400CAC-B0CC-4B19-BAC6-C0DFF259CEDD) (Booted) 
    iPhone 17 (57ACA8ED-0497-4661-B429-F3B34E7FA508) (Booted) 
List of devices attached

```

## iOS Cold Start

Measurement: elapsed host time for `xcrun simctl launch --terminate-running-process com.guyghost.wakeve`.

| Run | Elapsed ms | Result |
| --- | ---: | --- |
| 1 | 292.2 | ok |

### iOS Cold Start Summary

| Metric | Samples | Min ms | Median ms | P95 ms | Max ms | Average ms |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| simctl launch elapsed | 1 | 292.2 | 292.2 | 292.2 | 292.2 | 292.2 |

## Runtime Profiling Matrix

These rows define the release-device traces required before the roadmap performance items can be checked off. Local simulator or emulator samples may support regression tracking, but the status remains `PENDING_DEVICE_TRACE` until a representative signed build is profiled.

| Flow | Platform | Required capture | Required measurements | Status |
| --- | --- | --- | --- | --- |
| Cold start | iOS | Physical device, signed Release/TestFlight build, 5+ launches | Process launch, first meaningful screen, memory after idle | PENDING_DEVICE_TRACE |
| Cold start | Android | Representative device or emulator, release APK, 5+ launches | `am start -W` TotalTime/WaitTime, first meaningful screen, memory after idle | PENDING_DEVICE_TRACE |
| Home/list scrolling | iOS | Instruments SwiftUI + Time Profiler trace | Frame hitches, CPU hot spots, memory growth | PENDING_DEVICE_TRACE |
| Home/list scrolling | Android | Android Studio Profiler or Perfetto trace | Jank, CPU hot spots, memory growth | PENDING_DEVICE_TRACE |
| Create event | iOS | Instruments trace from opening sheet through saved draft | Sheet open latency, validation/apply latency, frame hitches | PENDING_DEVICE_TRACE |
| Create event | Android | Profiler/Perfetto trace for matching flow | Screen open latency, validation/apply latency, jank | PENDING_DEVICE_TRACE |
| Scenario matrix | iOS | Instruments trace for render, vote, final selection | Render latency, vote latency, CPU/memory under loaded matrix | PENDING_DEVICE_TRACE |
| Scenario matrix | Android | Profiler/Perfetto trace for render, vote, final selection | Render latency, vote latency, CPU/memory under loaded matrix | PENDING_DEVICE_TRACE |
| WakeveAI generation | iOS supported device | Instruments + WakeveAI metrics snapshot | Availability, generation latency, cancellation latency, timeout behavior, memory peak | PENDING_DEVICE_TRACE |

## Runtime Profiling Checklist

For every captured flow, record:

- Device model, OS version, app version/build, distribution channel, run count, and dataset size.
- Raw trace artifact path or App Store/TestFlight attachment reference.
- Measured latency, CPU, memory, and any skipped subflow with the reason.
- Whether the capture used a signed App Store/TestFlight build or a local Release build.

Raw command logs for this run were written to:

- `/var/folders/1t/456kc0651bl7mgrc62_m43g80000gn/T//wakeve-performance-profile-31004/logs`

