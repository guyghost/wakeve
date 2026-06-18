# Change: Add event weather forecasts with MapKit and WeatherKit

## Why
Wakeve plans events around dates and places, but organizers and participants currently lack weather context when choosing or preparing for an event day. Weather context is especially useful for outdoor activities, travel, weddings, sports events, and multi-day trips.

## What Changes
- Add an Event Weather capability that resolves event locations to coordinates and retrieves weather for confirmed event dates or candidate event days.
- Use MapKit on iOS to search, disambiguate, and enrich event or scenario locations with coordinates.
- Use WeatherKit on iOS to fetch current, hourly, and daily forecast data when the event date is inside the provider-supported forecast window.
- Persist weather snapshots for offline viewing and show explicit unavailable, stale, or pending states instead of inventing forecasts for dates outside provider range.
- Add iOS event detail and planning surfaces that combine a compact map context with weather summary, precipitation, temperature, wind, and advisory states.

## Impact
- Affected specs: `event-weather` (new), `ios-design-system`
- Affected code:
  - `shared/src/commonMain/kotlin/.../weather/` for domain models, service contracts, cache policy, and repository interfaces
  - `shared/src/commonMain/sqldelight/...` for cached weather snapshots and resolved location metadata
  - `shared/src/iosMain/kotlin/...` or iOS bridging services for WeatherKit and MapKit-backed providers
  - `wakeveApp/wakeveApp/...` for WeatherKit entitlement, MapKit/WeatherKit adapters, and SwiftUI event weather surfaces
  - `wakeveApp/src/commonMain/kotlin/...` if shared presentation state needs weather status models
  - Tests in shared Kotlin, iOS XCTest, and UI snapshots/previews

## References
- Apple WeatherKit provides Swift APIs on Apple platforms and REST APIs for other platforms, with forecast data and platform availability constraints.
- Apple MapKit provides SwiftUI map presentation and local search primitives that can resolve natural language locations to map items and coordinates.
