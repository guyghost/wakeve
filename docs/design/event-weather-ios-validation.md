# Event Weather iOS Design Validation

Date: 2026-06-20
Scope: `EventWeatherMapCard` and compact scenario-comparison weather context.

## Result

The event weather surfaces pass the local design validation for the current implementation.

## Checks

- Liquid Glass / material hierarchy: weather uses the existing `WakeveGlassCard` shell for the event-detail card and token-backed solid fills for nested metrics. The scenario comparison weather row uses `WakeveTheme.ColorToken.controlFill` and does not add another glass layer inside the comparison card.
- Accessibility: the MapKit preview has `weather.map_accessibility_format`, state rows use localized visible labels, scenario comparison weather rows combine their icon/text children for VoiceOver, and status text is advisory rather than blocking.
- Dynamic Type: event weather text uses `WakeveTheme.Typography` styles with line limits and minimum scale factors for compact metrics. A dedicated `Weather Available Dynamic Type` preview renders the card at `.accessibility3`.
- Reduce Transparency: the weather card is previewed with `accessibilityReduceTransparency=true`; the shared `WakeveGlassCard`/Liquid Glass fallback layer owns the material fallback behavior.
- Dark/light mode: weather colors are sourced through `WakeveTheme.ColorToken` and `SemanticColor`; dedicated light and dark previews cover the available state.

## Remaining Manual Evidence

This is source and simulator-build validation. It does not replace physical-device WeatherKit validation, signed TestFlight inspection, or App Store accessibility-label evidence for the final review build.
