## 1. Specification and Planning
- [x] 1.1 Review and approve the Event Weather proposal and open questions.
- [ ] 1.2 Confirm WeatherKit entitlement availability for the iOS bundle and Apple Developer team.
- [x] 1.3 Decide first-release Android behavior: hidden, cached-only, or backend/REST proxy.

## 2. Tests First
- [x] 2.1 @tests Add shared unit tests for date range mapping, provider forecast-window states, stale cache handling, and missing-location errors.
- [x] 2.2 @tests Add repository tests for weather snapshot persistence keyed by event, location, date range, and provider.
- [x] 2.3 @tests Add iOS provider tests or fakes that map WeatherKit success, entitlement failure, provider outage, and unsupported date states.
- [x] 2.4 @tests Add iOS UI preview/snapshot tests for loading, available, stale, pending, and unavailable weather states.

## 3. Shared Domain and Storage
- [x] 3.1 @codegen Add shared weather domain models, availability states, provider interfaces, and cache freshness policy.
- [x] 3.2 @codegen Add SQLDelight tables and queries for weather snapshots and resolved event-location metadata.
- [x] 3.3 @codegen Add an EventWeatherService that chooses final event date, scenario dates, or candidate time slots and returns typed weather summaries.
- [x] 3.4 @codegen Ensure offline reads serve cached weather with explicit freshness metadata and queue no destructive sync operation.

## 4. iOS MapKit and WeatherKit Integration
- [x] 4.1 @codegen Add MapKit-based location search/resolution for event, scenario, and potential-location text.
- [x] 4.2 @codegen Add WeatherKit provider integration using resolved coordinates and date ranges.
- [x] 4.3 @codegen Add WeatherKit entitlement/configuration checks and map provider errors into shared availability states.
- [x] 4.4 @codegen Add request debouncing and cache reuse to avoid repeated provider calls on screen reload.

## 5. iOS UI
- [x] 5.1 @codegen Add an event weather card to iOS event detail for confirmed events.
- [ ] 5.2 @codegen Add optional weather context in scenario/date comparison when location and date are known.
- [x] 5.3 @codegen Add compact MapKit place context when the event location has coordinates or a selected map item.
- [ ] 5.4 @designer Validate Liquid Glass, accessibility, dynamic type, reduce transparency, and dark/light mode states.

## 6. Review and Verification
- [ ] 6.1 @review Review privacy, access control, App Store entitlement risk, and fallback behavior.
- [ ] 6.2 @tests Run shared tests, iOS unit tests, and iOS UI validation on simulator where possible and physical device where WeatherKit requires entitlement validation.
- [x] 6.3 @docs Document WeatherKit entitlement setup, MapKit location resolution behavior, cache policy, and provider limitations.

## Notes
- 3.3 is implemented for confirmed dates, selected/proposed scenario dates, and candidate time slots.
- 2.3 is covered by `EventWeatherProviderTests`, which exercises fakeable WeatherKit success, entitlement, outage, and unsupported-date mapping.
- 2.4 is covered by named SwiftUI previews plus `EventWeatherMapCardContractTests` for loading, available, stale, pending, and unavailable states.
- 4.1 is implemented for potential event locations and scenario location text. Event-level location text currently resolves through potential-location records because `Event` has no standalone location field.
- 4.4 is covered by `CachedEventWeatherProvider`, which reuses fresh same-place/same-date results and coalesces concurrent duplicate requests before hitting WeatherKit.
- 6.2 is partially covered by shared JVM tests, Android compilation, iOS simulator build, and targeted iOS XCTest; physical-device WeatherKit validation remains pending.
