# Event Weather Integration

Wakeve displays event weather only when an event has a confirmed or organizing date and a resolvable location. The first iOS implementation uses MapKit for place resolution and WeatherKit for daily forecasts. Android is wired with a provider abstraction and an optional Google Maps Platform Weather API implementation.

## iOS

- File: `iosApp/src/Components/EventWeatherMapCard.swift`
- Location resolution:
  - Reads stored `PotentialLocation` rows from SQLDelight.
  - Uses stored coordinates when available.
  - Falls back to `MKLocalSearch` for location name/address text.
  - Scenario comparison rows resolve non-empty scenario location text through `MKLocalSearch` when `dateOrPeriod` contains an ISO-like date.
- Weather source:
  - Uses `WeatherService.shared.weather(for:)`.
  - Shows daily low/high temperature, precipitation chance, wind speed, condition, and a compact MapKit marker.
- Entitlement:
  - `iosApp/src/Wakeve.entitlements` includes `com.apple.developer.weatherkit`.
  - The Apple Developer account and App ID must also have WeatherKit enabled before device/TestFlight validation.

## Android

- File: `shared/src/androidMain/kotlin/com/guyghost/wakeve/weather/GoogleMapsWeatherProvider.android.kt`
- Configure `google.maps.api.key` in `local.properties` to enable the Google Maps Platform Weather API provider.
- Without a key, Android returns an explicit provider-unavailable state and the Compose detail screen explains that weather is not configured.
- The Google Weather API daily endpoint supports a limited forecast window. The shared service keeps events outside that window in a pending state instead of calling the provider.

## Shared Cache Policy

- SQLDelight tables:
  - `eventWeatherSnapshot`
  - `resolvedEventLocation`
- Snapshots are keyed by event, location, date range, and provider.
- Offline mode serves the latest cached snapshot with `STALE` metadata when the provider cannot be reached.
- Missing locations and future dates outside the provider forecast window do not enqueue destructive sync operations.

## Current Limitations

- The iOS card currently fetches directly through WeatherKit and does not yet route through the shared SQLDelight weather cache.
- Scenario/date comparison weather context is intentionally compact and silent on provider failures because it is advisory.
- Physical-device WeatherKit validation still depends on a configured Apple Developer capability, the `TEAM_ID` release environment, and a regenerated provisioning profile for `com.guyghost.wakeve`.

## Device Validation Evidence

Run the preparation helper before closing OpenSpec tasks `add-event-weather-forecast` / `1.2` or `6.2`:

```bash
./scripts/prepare-weatherkit-device-validation.sh
```

The helper writes a report under `docs/weatherkit/` with source entitlement state, connected device visibility, signing identities, matching provisioning profiles, and required real-device closure fields. The generated report is preparation evidence only until a signed physical-device or TestFlight-equivalent WeatherKit run proves the Apple Developer capability and provisioning profile are active for `com.guyghost.wakeve`.
