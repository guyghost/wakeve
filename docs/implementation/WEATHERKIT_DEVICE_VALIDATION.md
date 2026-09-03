# WeatherKit Device Validation

Date: 2026-09-03

Status: local source wiring is guarded; Apple Developer capability, provisioning profile, signed app entitlement, and physical-device WeatherKit validation remain required.

## Scope

This note records evidence for Swarm DAO proposal #9 (WeatherKit sur device physique), migrated from the former `add-event-weather-forecast` change.

The remaining open items are:

- Entitlement capability: confirm WeatherKit entitlement availability for the iOS bundle and Apple Developer team (former task `1.2`).
- Device validation: run shared tests, iOS unit tests, and iOS UI validation on simulator where possible and physical device where WeatherKit requires entitlement validation (former task `6.2`).

## Local Evidence Already Covered

- `iosApp/src/Wakeve.entitlements` declares `com.apple.developer.weatherkit = true`.
- `EventWeatherMapCardContractTests.testWeatherKitEntitlementIsWiredToAppTarget` guards source entitlement wiring.
- `scripts/lint-store-metadata.sh` fails when WeatherKit is imported without the matching source entitlement.
- `EventWeatherProviderTests` covers WeatherKit success, entitlement failure, provider outage, and unsupported-date mapping through fakes.
- `EventWeatherMapCardContractTests` covers loading, available, stale, pending, and unavailable UI states.
- `docs/reviews/event-weather-privacy-review.md` records the privacy and access-control review.

This local evidence is necessary but not enough to close the WeatherKit tasks, because source files and simulator tests cannot prove Apple Developer portal capability state or the entitlements embedded in a signed installed app.

## Device Validation Helper

Use:

```bash
./scripts/prepare-weatherkit-device-validation.sh
```

The helper writes a report under `docs/weatherkit/` and records:

- CoreDevice and Instruments device visibility.
- Source WeatherKit entitlement state.
- `TEAM_ID` / `APPLE_TEAM_ID` environment value.
- local code signing identities.
- matching provisioning profiles for `com.guyghost.wakeve`.
- whether matching profiles expose `Entitlements.com.apple.developer.weatherkit = true`.
- Xcode and iPhoneOS SDK versions used for the local readiness capture.
- required closure fields and non-closure conditions.

The report is preparation evidence only. It must not be used to close the entitlement-capability or device-validation items unless the required real-device WeatherKit fields are filled in.

Every generated report explicitly records `Generated report can close backlog tasks = no - preparation evidence only`. Closing the remaining items requires a reviewed signed-device or TestFlight-equivalent WeatherKit run, not just a fresh helper output.

## 2026-06-20 Simulator Regression Refresh

Command:

```bash
XcodeBuildMCP test_sim \
  -only-testing:WakeveTests/EventWeatherProviderTests \
  -only-testing:WakeveTests/EventWeatherMapCardContractTests \
  -only-testing:WakeveTests/PremiumEventDetailContractTests/testEventDetailWeatherCardUsesWeatherKitAndMapKit
```

Result: `TEST SUCCEEDED` on the configured `iPhone 17` simulator as part of the combined WeatherKit/WakeveAI focused suite. The weather subset passed `15/15` selected tests.

Artifacts:

- Build log: `/Users/guy/Library/Developer/XcodeBuildMCP/workspaces/wakeve-cf467b3193b0/logs/test_sim_2026-06-20T19-56-28-754Z_pid9347_bb3368ef.log`
- Result bundle: `/Users/guy/Library/Developer/XcodeBuildMCP/workspaces/wakeve-cf467b3193b0/result-bundles/test_sim_2026-06-20T19-56-28-754Z_pid9347_e5479204.xcresult`

This refresh proves the simulator-testable provider mapping, entitlement source wiring, UI state contracts, and premium access-control integration remain green. It does not close tasks `1.2` or `6.2`, because simulator XCTest cannot prove Apple Developer portal WeatherKit capability, signed app entitlements, or live WeatherKit behavior on a physical device.

## 2026-06-21 Device Readiness Refresh

Preparation report: `docs/weatherkit/weatherkit-device-validation-2026-06-20T23-08-57Z.md`.

Current status remains `PHYSICAL_IOS_DEVICE_NOT_TRACE_READY`.

The refreshed helper output proves only the current local readiness state:

- Source entitlement wiring still declares `com.apple.developer.weatherkit = true`.
- CoreDevice sees the physical `iPhone de GuyGhost`, model `iPhone 15 Pro (iPhone16,1)`.
- Instruments still lists the same iPhone under `Devices Offline`, so it is not trace-ready for WeatherKit validation.
- `TEAM_ID` / `APPLE_TEAM_ID` is missing from the shell environment.
- `security find-identity -v -p codesigning` reports `0 valid identities found`.
- No local provisioning profile matches bundle ID `com.guyghost.wakeve`.
- The local capture is on Xcode 27.0 with iPhoneOS SDK 27.0.

This report is preparation evidence only. It does not close tasks `1.2` or `6.2`; final closure still requires the Apple Developer WeatherKit capability, a regenerated provisioning profile containing the WeatherKit entitlement, signed app entitlement inspection, and a real-device or TestFlight-equivalent WeatherKit run.

## 2026-09-03 Refresh (Swarm DAO proposal #9)

Simulator regression re-run on `Wakeve-QA-iPhone-16-Pro`:

```bash
xcodebuild test -project iosApp/iosApp.xcodeproj -scheme WakeveApp \
  -destination 'platform=iOS Simulator,id=D0C6C94E-801C-4256-A3A5-95A99EAE2F27' \
  -only-testing:WakeveTests/EventWeatherProviderTests \
  -only-testing:WakeveTests/EventWeatherMapCardContractTests \
  -only-testing:WakeveTests/PremiumEventDetailContractTests/testEventDetailWeatherCardUsesWeatherKitAndMapKit
```

Result: `TEST SUCCEEDED`; 15/15 selected weather tests passed, 0 failed. Shared JVM weather tests (`com.guyghost.wakeve.weather.*`) also pass via `./gradlew shared:jvmTest`.

Fresh device readiness capture: `docs/weatherkit/weatherkit-device-validation-2026-09-03T21-00-19Z.md`.

Current status remains `PHYSICAL_IOS_DEVICE_NOT_TRACE_READY`, with one improvement over the 2026-06-20 capture:

- `security find-identity -v -p codesigning` now reports `1 valid identity` (was `0` in June) — progress toward the signed build.
- Source entitlement wiring still declares `com.apple.developer.weatherkit = true`.
- `TEAM_ID` / `APPLE_TEAM_ID` remains missing from the shell environment.
- No local provisioning profile matches bundle ID `com.guyghost.wakeve` yet.

Remaining external closure path (owner): enable the WeatherKit capability for `com.guyghost.wakeve` in the Apple Developer portal, regenerate the provisioning profile with the entitlement, set `TEAM_ID`, then run the signed-device or TestFlight-equivalent WeatherKit validation and fill the closure fields below.

## Closure Requirements

Before closing the entitlement-capability and device-validation items, record:

- Apple Developer Team ID.
- App ID or Identifier evidence showing WeatherKit is enabled for `com.guyghost.wakeve`.
- provisioning profile name/UUID and proof it contains `com.apple.developer.weatherkit = true`.
- signed app entitlement inspection output.
- physical iPhone model and OS build.
- Wakeve build configuration and commit/build number.
- non-personal WeatherKit request fixture.
- WeatherKit result: successful forecast or exact mapped provider/entitlement state.
- iOS event weather UI validation result.
- reviewer/date and artifact paths.

## Current Non-Closure State

The entitlement-capability and device-validation items remain open until signed-device or TestFlight-equivalent evidence proves WeatherKit works with the Apple Developer capability and provisioning profile.
