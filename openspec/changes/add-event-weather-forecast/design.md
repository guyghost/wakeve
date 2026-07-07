## Context
Wakeve already stores potential locations with optional coordinates and supports scenario planning from `TimeSlot x PotentialLocation`. Weather should be attached to the event planning context rather than treated as a separate user-location feature.

Apple WeatherKit requires Apple Developer Program access, the WeatherKit capability/entitlement, and an iOS 16+ runtime for the native Swift API. Apple documents additional WeatherKit data details available on iOS 18+, so the implementation must tolerate OS and provider capability differences.

## Goals / Non-Goals
- Goals:
  - Show useful weather context for confirmed event days and candidate event days.
  - Resolve ambiguous event locations through MapKit and store stable coordinates for later weather fetches.
  - Keep weather visible offline from cached snapshots with explicit freshness metadata.
  - Preserve Wakeve's access-control and privacy model by using event/place coordinates, not participant tracking.
- Non-Goals:
  - Do not provide long-range deterministic forecasts beyond WeatherKit's supported forecast window.
  - Do not make weather required to create, vote, confirm, or finalize an event.
  - Do not add a non-Apple weather provider in this change unless needed for Android parity later.

## Decisions
- Decision: Add a shared `event-weather` domain capability with platform providers.
  - Why: Shared models keep event, scenario, cache, and offline behavior consistent while allowing iOS to use native WeatherKit and MapKit.
  - Alternative considered: SwiftUI-only WeatherKit calls in the event detail screen. Rejected because it would bypass offline cache, tests, and shared event workflow state.

- Decision: Use existing coordinates first, then MapKit location resolution.
  - Why: `PotentialLocation.coordinates` already exists. MapKit should enrich missing or ambiguous locations, not overwrite organizer intent silently.
  - Alternative considered: Always search by text each time weather loads. Rejected because it is slower, less deterministic, and harder to cache.

- Decision: Return typed weather availability states.
  - Why: Events can be too far in the future, offline, missing a location, outside provider coverage, or blocked by entitlement/runtime issues.
  - Expected states: `available`, `stale`, `pendingForecastWindow`, `missingLocation`, `permissionOrEntitlementRequired`, `providerUnavailable`, `offlineUnavailable`.

- Decision: Cache weather snapshots by event, location, date range, and provider.
  - Why: This supports offline-first behavior and prevents repeated provider calls for the same event context.
  - Cache metadata MUST include `fetchedAt`, `expiresAt`, `provider`, `sourceCoordinates`, and `dateRange`.

## Risks / Trade-offs
- WeatherKit forecast windows are limited for future planning.
  - Mitigation: Show `pendingForecastWindow` and optionally schedule refresh when the event enters the provider-supported window.
- MapKit search can return multiple plausible locations.
  - Mitigation: Require user selection or confidence threshold before persisting coordinates for weather.
- WeatherKit entitlement or account configuration can fail on device.
  - Mitigation: Add provider diagnostics, XCTest coverage for failure mapping, and a non-blocking UI state.
- Provider quotas can be consumed by repeated loads.
  - Mitigation: Cache by event/date/location, debounce refreshes, and avoid background polling until explicitly planned.

## Migration Plan
1. Add weather snapshot and resolved-location metadata tables without changing existing event creation requirements.
2. Backfill nothing for existing events; weather is lazily resolved when event detail or scenario comparison requests it.
3. Preserve existing `PotentialLocation.coordinates` where present.
4. If WeatherKit is unavailable, keep event workflows functional and show a clear unavailable state.

## Open Questions
- Should Android initially show cached/server-provided weather only, or remain hidden until a non-Apple provider is selected?
- Should weather influence scenario scoring automatically, or only appear as context for organizer decisions in the first release?
- Should the app expose severe weather alerts as notifications, or only in the event detail surface for this change?
