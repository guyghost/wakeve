# WeatherKit Device Validation Preparation

Generated: 2026-06-20T19:50:24Z

Status: `PHYSICAL_IOS_DEVICE_NOT_TRACE_READY`

This report supports OpenSpec tasks `add-event-weather-forecast` / `1.2` and `6.2`.
It is not completion evidence until a signed build on a real iOS device proves WeatherKit entitlement availability for bundle `com.guyghost.wakeve`.

## Source Entitlement Check

| Field | Value |
| --- | --- |
| Entitlements file | `iosApp/src/Wakeve.entitlements` |
| Source WeatherKit entitlement | `true` |
| Bundle ID | `com.guyghost.wakeve` |
| TEAM_ID / APPLE_TEAM_ID environment value | `missing` |
| Valid code signing identities | `0` |
| Matching provisioning profiles | `missing` |
| Matching profiles with WeatherKit entitlement | `missing` |

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
Name                 Hostname                              Identifier                             State                Model                            Reality  
------------------   -----------------------------------   ------------------------------------   ------------------   ------------------------------   ---------
Guy’s Apple Watch    Guys-AppleWatch.coredevice.local      223D1560-A19A-5D32-AED6-813175A21D43   available (paired)   Watch7,2                                  
iPhone 17                                                  57ACA8ED-0497-4661-B429-F3B34E7FA508   shutdown             iPhone 17 (iPhone18,3)           simulated
iPhone 17 Pro                                              45D578D6-0267-467B-BA25-3CF08BCFE468   shutdown             iPhone 17 Pro (iPhone18,1)       simulated
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
`Settings -> Privacy & Security -> Developer Mode`.

## Signing Readiness

### Code Signing Identities

```text
     0 valid identities found
```

### Matching Provisioning Profiles

```text
No provisioning profile matched com.guyghost.wakeve.
```

The profile used for final validation must include both:

- `Entitlements.application-identifier = <TEAM_ID>.com.guyghost.wakeve`
- `Entitlements.com.apple.developer.weatherkit = true`

## Required Closure Evidence

Record all fields below before checking off OpenSpec tasks `1.2` and `6.2`:

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
