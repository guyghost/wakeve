# WeatherKit Device Validation Preparation

Generated: 2026-09-03T21:00:25Z

Status: `PHYSICAL_IOS_DEVICE_NOT_TRACE_READY`

This report supports the event weather forecast backlog items.
It is not completion evidence until a signed build on a real iOS device proves WeatherKit entitlement availability for bundle `com.guyghost.wakeve`.

## Source Entitlement Check

| Field | Value |
| --- | --- |
| Entitlements file | `iosApp/src/Wakeve.entitlements` |
| Source WeatherKit entitlement | `true` |
| Bundle ID | `com.guyghost.wakeve` |
| TEAM_ID / APPLE_TEAM_ID environment value | `missing` |
| Valid code signing identities | `1` |
| Matching provisioning profiles | `missing` |
| Matching profiles with WeatherKit entitlement | `missing` |
| Generated report can close backlog tasks | `no - preparation evidence only` |

## Toolchain Readiness

| Field | Value |
| --- | --- |
| Xcode version | `Xcode 27.0` |
| iPhoneOS SDK version | `27.0` |

### Associated Domains Source Entitlement

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<array>
	<string>applinks:wakeve.app</string>
</array>
</plist>
```

## Detected Devices

### CoreDevice

```text
Name                      Hostname                              Identifier                             State                Model                        Reality
-----------------------   -----------------------------------   ------------------------------------   ------------------   --------------------------   ---------
Guy’s Apple Watch         Guys-AppleWatch.coredevice.local      223D1560-A19A-5D32-AED6-813175A21D43   available (paired)   Watch7,2
Wakeve-QA-iPhone-16-Pro                                         D0C6C94E-801C-4256-A3A5-95A99EAE2F27   shutdown             iPhone 16 Pro (iPhone17,1)   simulated
iPhone de GuyGhost        iPhone-de-GuyGhost.coredevice.local   197C4A1B-1D18-55CC-9F14-F191512757C8   available (paired)   iPhone 15 Pro (iPhone16,1)   physical
```

### Xcode Instruments

```text
== Devices ==
Guy’s MacBook Pro (305FB8F6-801F-5FD2-BE49-BE799A13D773)

== Devices Offline ==
Guy’s Apple Watch (00008310-001233622188A01E)
iPhone de GuyGhost (27.0) (00008130-001E39811A12001C)

== Simulators ==
Wakeve-QA-iPhone-16-Pro Simulator (26.5) (D0C6C94E-801C-4256-A3A5-95A99EAE2F27)
```

If a physical iPhone appears in CoreDevice but under `Devices Offline` for
Instruments, unlock the device, trust this Mac, and enable Developer Mode in
`Settings -> Privacy & Security -> Developer Mode`.

## Signing Readiness

### Code Signing Identities

```text
  1) EDCAAA7C543CAA9288686D3741030AEE6BEB238B "Apple Development: guyghost@gmail.com (6J32GUMZLS)"
     1 valid identities found
```

### Matching Provisioning Profiles

```text
No provisioning profile matched com.guyghost.wakeve.
```

The profile used for final validation must include both:

- `Entitlements.application-identifier = <TEAM_ID>.com.guyghost.wakeve`
- `Entitlements.com.apple.developer.weatherkit = true`

## Required Closure Evidence

Generated reports from this helper are never sufficient on their own. They only
become closure evidence after the table below is filled from a signed physical
device or TestFlight-equivalent WeatherKit run and reviewed.

Record all fields below before closing the backlog items:

| Field | Value |
| --- | --- |
| Apple Developer Team ID | TODO |
| App ID / Identifier screenshot or API evidence | TODO |
| WeatherKit capability enabled on App ID | TODO |
| Provisioning profile name and UUID | TODO |
| Provisioning profile contains WeatherKit entitlement | TODO |
| Device model | TODO |
| Device OS build | TODO |
| Wakeve build configuration | TODO |
| Wakeve commit / build number | TODO |
| Signed app entitlement inspection path | TODO |
| WeatherKit request fixture | TODO: non-personal location/date |
| WeatherKit request result | TODO: success or mapped entitlement/provider state |
| iOS event weather UI validation result | TODO |
| Reviewer/date | TODO |

## Recommended Device Flow

1. Enable WeatherKit for the explicit App ID `com.guyghost.wakeve` in the Apple Developer portal.
2. Regenerate and install a development or distribution provisioning profile that includes WeatherKit.
3. Set `TEAM_ID` or `APPLE_TEAM_ID` to the real 10-character Apple Developer Team ID.
4. Build and install Wakeve on a physical iPhone using the regenerated profile.
5. Inspect the signed app entitlements and confirm `com.apple.developer.weatherkit = true`.
6. Open an event or scenario with a non-personal location/date fixture inside WeatherKit's supported forecast window.
7. Verify the UI reaches the available weather state or records the exact mapped provider/entitlement error.
8. Save the device log, signed entitlement inspection, and screenshot/result bundle path.
9. Update `docs/implementation/WEATHERKIT_DEVICE_VALIDATION.md` with final values.

## Non-Closure Conditions

Do not mark `1.2` or `6.2` complete if any of these are true:

- Only source entitlement or simulator evidence is available.
- The Apple Developer App ID capability has not been confirmed.
- The provisioning profile does not include `com.apple.developer.weatherkit = true`.
- The signed app entitlements were not inspected.
- The physical device is unavailable, offline in Instruments, or cannot install the signed build.
- WeatherKit was not exercised on a real device or TestFlight-equivalent signed build.
- The report uses personal location, participant, vote, address, price, or chat data that cannot be safely committed.
