# WakeveAI Device Profile Preparation

Generated: 2026-06-15T18:04:08Z

Status: `PENDING_PHYSICAL_IOS_DEVICE`

This report supports OpenSpec task `add-on-device-wakeve-ai` / `6.6`.
It is not completion evidence until a real supported iOS device is profiled with Foundation Models available.

## Detected Devices

```text
Name                Hostname                           Identifier                             State                Model                            Reality  
-----------------   --------------------------------   ------------------------------------   ------------------   ------------------------------   ---------
Guy’s Apple Watch   Guys-AppleWatch.coredevice.local   223D1560-A19A-5D32-AED6-813175A21D43   available (paired)   Watch7,2                                  
iPhone 17                                              57ACA8ED-0497-4661-B429-F3B34E7FA508   shutdown             iPhone 17 (iPhone18,3)           simulated
iPhone 17 Pro Max                                      41400CAC-B0CC-4B19-BAC6-C0DFF259CEDD   shutdown             iPhone 17 Pro Max (iPhone18,2)   simulated
```

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
- The physical device reports Foundation Models as disabled, not ready, unsupported, or unknown.
- The run uses personal prompt content that cannot be committed or summarized safely.
- Latency is measured without cancellation and memory evidence.
- Logs contain personal prompt text, generated content, participant names, votes, addresses, or prices.
