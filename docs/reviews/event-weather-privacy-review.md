# Event Weather Privacy and Fallback Review

Date: 2026-06-20
Scope: OpenSpec change `add-event-weather-forecast`, task 6.1.

## Summary

The event weather feature is approved for the current local implementation after adding an explicit event-detail access guard. Weather remains advisory and does not block event creation, voting, scenario selection, organizing, or finalization.

## Reviewed Areas

- Privacy minimization: `WeatherKitEventForecastProvider` constructs `CLLocation` from `EventWeatherPlace.coordinate` and requests `WeatherService.weather(for:)`. It does not pass participant identifiers, participant departure locations, votes, chat content, notes, budgets, or event metadata to WeatherKit.
- MapKit minimization: event detail resolution searches only potential-location name/address text or selected scenario location text. Scenario comparison searches only the scenario location string when `dateOrPeriod` contains an ISO-like date.
- Access control: event-detail weather is shown and loaded only when `canShowWeatherContext` is true, which currently maps to `canAccessOrganizationDetails`. Participants without access have the view model reset to `.hidden`. Scenario comparison weather is only inside `scenarioContent`, which is not rendered while `ScenarioOrganizationView.isLocked` is true.
- Entitlement risk: WeatherKit source entitlement wiring is guarded by `EventWeatherMapCardContractTests.testWeatherKitEntitlementIsWiredToAppTarget` and `scripts/lint-store-metadata.sh`. Final Apple Developer portal/App ID/provisioning-profile proof remains outside local source control.
- Fallback behavior: unsupported future dates return pending states, provider/entitlement failures show localized unavailable or hidden advisory UI, and scenario-comparison provider errors are silent because comparison weather is optional context.

## Findings

No blocking local privacy, access-control, or fallback issues remain for the reviewed source state.

## Residual Risk

- Physical-device WeatherKit validation remains required because the simulator/source tree cannot prove Apple Developer account capability, signed entitlements, or provisioning profile state.
- Event-detail iOS weather currently fetches directly through WeatherKit instead of the shared SQLDelight cache path, so full offline behavior remains covered by shared/domain tests rather than this SwiftUI card.
