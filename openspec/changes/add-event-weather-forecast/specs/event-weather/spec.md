## ADDED Requirements
### Requirement: Event Weather Forecasts
Wakeve MUST provide weather context for confirmed event dates and eligible candidate event days when an event has a resolvable location.

Weather summaries MUST include the event date or date range, source location, temperature range, precipitation probability, wind summary, weather condition, provider, freshness metadata, and an availability state. Weather MUST be advisory context only and MUST NOT block event creation, voting, date confirmation, scenario selection, organizing, or finalization.

#### Scenario: Confirmed event has WeatherKit forecast
- **GIVEN** an event has status `CONFIRMED`
- **AND** the final event date is inside the provider-supported forecast window
- **AND** the final event location has stored coordinates
- **WHEN** the event weather service loads weather context
- **THEN** Wakeve retrieves a WeatherKit forecast for the event coordinates and date
- **AND** stores a weather snapshot with provider, source coordinates, date range, `fetchedAt`, and `expiresAt`
- **AND** returns an `available` weather summary for the event detail surface.

#### Scenario: Event date is outside forecast window
- **GIVEN** an event has a known date and location
- **AND** the event date is outside the provider-supported forecast window
- **WHEN** the event weather service loads weather context
- **THEN** Wakeve returns `pendingForecastWindow`
- **AND** does not invent forecast values
- **AND** includes the earliest refresh date when the provider can reasonably be queried.

#### Scenario: Multi-day event has daily summaries
- **GIVEN** an event spans multiple days
- **AND** each day is inside the provider-supported forecast window
- **WHEN** weather context is requested
- **THEN** Wakeve returns one summary per event day
- **AND** preserves date ordering in the event timezone.

### Requirement: Event Location Resolution
Wakeve MUST resolve event, scenario, and potential-location text into coordinates before requesting weather when coordinates are missing.

Location resolution MUST prefer existing stored coordinates. When coordinates are absent, iOS MUST use MapKit search or geocoding to propose matching locations. Ambiguous results MUST require user confirmation or a confidence threshold before coordinates are persisted for weather use.

#### Scenario: Potential location lacks coordinates
- **GIVEN** an event has a potential location named "Biarritz"
- **AND** the potential location has no coordinates
- **WHEN** the organizer requests weather or map context
- **THEN** Wakeve uses MapKit to find matching map locations
- **AND** persists the selected coordinates and provider metadata after confirmation
- **AND** uses those coordinates for subsequent weather requests.

#### Scenario: Location cannot be resolved
- **GIVEN** an event has a free-text location that MapKit cannot resolve confidently
- **WHEN** weather context is requested
- **THEN** Wakeve returns `missingLocation`
- **AND** prompts for a more precise location without blocking the event workflow.

### Requirement: Weather Offline Cache
Wakeve MUST keep weather context usable offline when a fresh or stale cached weather snapshot exists.

Cached weather MUST be keyed by event, source location, date range, and provider. Cached weather shown after `expiresAt` MUST be labelled stale. If no cached weather exists while offline, Wakeve MUST show an offline unavailable state.

#### Scenario: Cached weather is available offline
- **GIVEN** weather was previously fetched for an event date and location
- **AND** the device is offline
- **WHEN** a participant opens the event detail
- **THEN** Wakeve displays the cached weather summary
- **AND** marks it as fresh or stale based on `expiresAt`
- **AND** does not attempt a provider network request.

#### Scenario: No cached weather exists offline
- **GIVEN** an event has a confirmed date and location
- **AND** no weather snapshot exists for that date and location
- **AND** the device is offline
- **WHEN** the event weather service loads weather context
- **THEN** Wakeve returns `offlineUnavailable`
- **AND** leaves the event workflow fully usable.

### Requirement: Weather Privacy and Access Control
Wakeve MUST apply existing event access-control rules and minimize data sent to weather and map providers.

Weather requests MUST use event or selected-place coordinates only. Wakeve MUST NOT send participant identities, participant departure locations, chat content, private notes, or unrelated event metadata to WeatherKit or MapKit for event weather. Weather details for restricted event locations MUST follow the same visibility rules as the underlying event or scenario location.

#### Scenario: Participant lacks access to final location
- **GIVEN** an event has restricted final-location details
- **AND** a participant has not confirmed attendance or lacks access under existing event rules
- **WHEN** they open event planning content
- **THEN** Wakeve does not reveal precise weather tied to the restricted final location
- **AND** may show only allowed coarse context or an access-controlled unavailable state.

#### Scenario: Weather request is made
- **GIVEN** an event has permitted location coordinates
- **WHEN** Wakeve requests WeatherKit data
- **THEN** the request contains only the coordinates and required date/weather query parameters
- **AND** no participant identity or private collaboration content is included.
