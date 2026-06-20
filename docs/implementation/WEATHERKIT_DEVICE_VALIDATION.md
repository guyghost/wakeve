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
