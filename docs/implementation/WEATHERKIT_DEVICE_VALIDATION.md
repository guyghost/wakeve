# WeatherKit Device Validation

Date: 2026-06-20

Status: local source wiring is guarded; Apple Developer capability, provisioning profile, signed app entitlement, and physical-device WeatherKit validation remain required.

## Scope

This note records evidence for `openspec/changes/add-event-weather-forecast`.

The remaining open tasks are:

- `1.2` Confirm WeatherKit entitlement availability for the iOS bundle and Apple Developer team.
- `6.2` Run shared tests, iOS unit tests, and iOS UI validation on simulator where possible and physical device where WeatherKit requires entitlement validation.

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
- required closure fields and non-closure conditions.

The report is preparation evidence only. It must not be used to mark OpenSpec tasks `1.2` or `6.2` complete unless the required real-device WeatherKit fields are filled in.

Every generated report explicitly records `Generated report can close OpenSpec tasks = no - preparation evidence only`. Closing `1.2` or `6.2` requires a reviewed signed-device or TestFlight-equivalent WeatherKit run, not just a fresh helper output.

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

Preparation report: `docs/weatherkit/weatherkit-device-validation-2026-06-20T22-44-14Z.md`.

Current status remains `PHYSICAL_IOS_DEVICE_NOT_TRACE_READY`.

The refreshed helper output proves only the current local readiness state:

- Source entitlement wiring still declares `com.apple.developer.weatherkit = true`.
- CoreDevice sees the physical `iPhone de GuyGhost`, model `iPhone 15 Pro (iPhone16,1)`.
- Instruments still lists the same iPhone under `Devices Offline`, so it is not trace-ready for WeatherKit validation.
- `TEAM_ID` / `APPLE_TEAM_ID` is missing from the shell environment.
- `security find-identity -v -p codesigning` reports `0 valid identities found`.
- No local provisioning profile matches bundle ID `com.guyghost.wakeve`.

This report is preparation evidence only. It does not close tasks `1.2` or `6.2`; final closure still requires the Apple Developer WeatherKit capability, a regenerated provisioning profile containing the WeatherKit entitlement, signed app entitlement inspection, and a real-device or TestFlight-equivalent WeatherKit run.

## Closure Requirements

Before checking off `1.2` and `6.2`, record:

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

OpenSpec tasks `1.2` and `6.2` remain open until signed-device or TestFlight-equivalent evidence proves WeatherKit works with the Apple Developer capability and provisioning profile.
