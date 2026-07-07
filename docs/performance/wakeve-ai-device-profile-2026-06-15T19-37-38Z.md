# WakeveAI Device Profile Preparation

Generated: 2026-06-15T19:37:41Z

Status: `PHYSICAL_IOS_DEVICE_NOT_TRACE_READY`

This report supports OpenSpec task `add-on-device-wakeve-ai` / `6.6`.
It is not completion evidence until a real supported iOS device is profiled with Foundation Models available.

## Detected Devices

### CoreDevice

```text
Name                 Hostname                              Identifier                             State                Model                            Reality  
------------------   -----------------------------------   ------------------------------------   ------------------   ------------------------------   ---------
Guy’s Apple Watch    Guys-AppleWatch.coredevice.local      223D1560-A19A-5D32-AED6-813175A21D43   available (paired)   Watch7,2                                  
iPhone 17                                                  57ACA8ED-0497-4661-B429-F3B34E7FA508   shutdown             iPhone 17 (iPhone18,3)           simulated
iPhone 17 Pro Max                                          41400CAC-B0CC-4B19-BAC6-C0DFF259CEDD   shutdown             iPhone 17 Pro Max (iPhone18,2)   simulated
iPhone de GuyGhost   iPhone-de-GuyGhost.coredevice.local   197C4A1B-1D18-55CC-9F14-F191512757C8   available (paired)   iPhone 15 Pro (iPhone16,1)       physical 
```

### Xcode Instruments

```text
== Devices ==
Guy’s MacBook Pro (305FB8F6-801F-5FD2-BE49-BE799A13D773)

== Devices Offline ==
Guy’s Apple Watch (00008310-001233622188A01E)
iPhone de GuyGhost (27.0) (00008130-001E39811A12001C)

== Simulators ==
iPad (A16) Simulator (27.0) (8E788B42-658C-47F3-A314-68CA670B7CFC)
iPad Air 11-inch (M4) Simulator (27.0) (DB098A6A-5031-40BA-B40E-9B53DFBDE3E0)
iPad Air 13-inch (M4) Simulator (27.0) (1D63835E-A24E-440C-8AFA-A03A4761617B)
iPad Pro 11-inch (M5) Simulator (27.0) (DD2D341F-0F71-4872-AE07-D6367DCE111A)
iPad Pro 13-inch (M5) Simulator (27.0) (FDDF6A63-4861-4E8A-9EBD-A740DC2BCE8C)
iPad mini (A17 Pro) Simulator (27.0) (97457E7F-51B5-4A4E-A51E-6BD967E53254)
iPhone 17 Simulator (27.0) (57ACA8ED-0497-4661-B429-F3B34E7FA508)
iPhone 17 Pro Simulator (27.0) (45D578D6-0267-467B-BA25-3CF08BCFE468)
iPhone 17 Pro Max Simulator (27.0) (41400CAC-B0CC-4B19-BAC6-C0DFF259CEDD)
iPhone 17e Simulator (27.0) (E3E10EE4-294C-4922-84E9-6BCB988288C5)
iPhone Air Simulator (27.0) (9E3680A6-08E0-44D2-A3CA-8E9123E44F86)
```

If a physical iPhone appears in CoreDevice but under `Devices Offline` for
Instruments, unlock the device, trust this Mac, and enable Developer Mode in
`Settings -> Privacy & Security -> Developer Mode` before profiling.

## Signing Readiness

| Field | Value |
| --- | --- |
| Bundle ID | `com.guyghost.wakeve` |
| TEAM_ID environment value | `missing` |
| Valid code signing identities | `0` |
| Matching provisioning profiles | `missing` |

### Code Signing Identities

```text
     0 valid identities found
```

### Matching Provisioning Profiles

```text
No provisioning profile matched com.guyghost.wakeve.
```

If signing is not ready, configure a concrete `TEAM_ID`, install an Apple
Development signing certificate in the login keychain, and ensure the Apple
Developer account has a provisioning profile for `com.guyghost.wakeve`.

## Required Closure Evidence

Record all fields below before checking off OpenSpec task `6.6`:

| Field | Value |
| --- | --- |
| Device model | TODO |
| Device OS build | TODO |
| Wakeve build configuration | TODO |
| Wakeve build number / commit | TODO |
| Foundation Models availability | TODO: must be `.available` |
| Smart Event Draft prompt | TODO: describe fixture without personal data |
| Generation latency | TODO |
| Cancellation latency | TODO |
| Memory before generation | TODO |
| Peak memory during generation | TODO |
| Memory after idle | TODO |
| Production log privacy checked | TODO |
| Instruments trace path | TODO |
| WakeveAI metrics snapshot path or screenshot | TODO |
| Reviewer/date | TODO |

## Recommended Device Flow

1. Install a signed Debug, Release, or TestFlight build on a supported physical iPhone with Apple Intelligence and Foundation Models available.
2. Open Wakeve and navigate to event creation.
3. Confirm the model availability state is `.available` through the WakeveAI availability path or debug instrumentation.
4. Start an Instruments run using Allocations or Leaks plus Time Profiler.
5. Generate a Smart Event Draft from a non-personal fixture, for example: `Week-end à Lisbonne avec 8 amis en septembre`.
6. Record generation duration from `WakeveAIMetricsRecorder` or the debug metrics UI/log snapshot.
7. Start a second generation and tap cancel while generation is in progress; record cancellation latency.
8. Record memory before generation, peak during generation, and memory after 30 seconds idle.
9. Inspect production-style logs and confirm prompt text, generated text, participant names, votes, addresses, and prices are absent.
10. Save the Instruments trace and update `docs/implementation/WAKEVE_AI_DEVICE_VERIFICATION.md` with the final values.

## Non-Closure Conditions

Do not mark `6.6` complete if any of these are true:

- Only simulator evidence is available.
- The physical device is connected but not trace-ready in Instruments.
- Xcode signing is missing a development team, valid Apple Development identity, or provisioning profile for the Wakeve bundle ID.
- The physical device reports Foundation Models as disabled, not ready, unsupported, or unknown.
- The run uses personal prompt content that cannot be committed or summarized safely.
- Latency is measured without cancellation and memory evidence.
- Logs contain personal prompt text, generated content, participant names, votes, addresses, or prices.
