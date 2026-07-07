## ADDED Requirements
### Requirement: iOS Event Weather and Map Context Surfaces
The iOS application SHALL present event weather and map context through compact, accessible SwiftUI surfaces that follow the Wakeve iOS design system.

Weather and map context SHALL use shared design tokens, Liquid Glass surfaces with accessible fallbacks, SF Symbols or system weather imagery, and clear availability states. The surface SHALL remain secondary to the event's primary next action and SHALL NOT obscure voting, confirmation, organizing, or participant actions.

#### Scenario: Confirmed event shows weather context
- **GIVEN** an iOS user opens a confirmed event detail screen
- **AND** weather context is available for the event date and location
- **WHEN** the event header or overview renders
- **THEN** the screen shows a compact weather surface with condition, temperature range, precipitation, wind, location label, and freshness state
- **AND** the surface uses design-system typography, spacing, color, material, contrast, dynamic type, and reduced-transparency fallbacks.

#### Scenario: Weather is pending or unavailable
- **GIVEN** an iOS user opens an event or scenario with missing, stale, pending, or unavailable weather
- **WHEN** the weather surface renders
- **THEN** it shows the correct non-blocking state without layout shift
- **AND** offers a precise location action only when the current user can edit or clarify the event location.

#### Scenario: Map context is shown
- **GIVEN** an event location has coordinates or a selected MapKit item
- **WHEN** the iOS event detail or scenario comparison screen renders map context
- **THEN** it shows a compact MapKit view or map affordance with the selected place
- **AND** controls, labels, and legal/system map content remain visible and accessible.
