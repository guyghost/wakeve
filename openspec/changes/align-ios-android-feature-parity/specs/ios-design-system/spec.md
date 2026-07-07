## ADDED Requirements
### Requirement: Native iOS Parity Surfaces
The iOS application MUST close Android feature parity gaps with native SwiftUI surfaces rather than copied Android layouts.

Parity surfaces MUST use Wakeve iOS tokens, SF Symbols for standard actions, Liquid Glass only on navigation/control layers, accessible content surfaces, Dynamic Type, reduced-transparency and reduced-motion fallbacks, and compact mobile-first layouts. They MUST NOT use generic placeholder screens for Android-equivalent event organization features.

#### Scenario: iOS replaces a placeholder route
- **GIVEN** an Android-equivalent iOS route previously rendered placeholder copy
- **WHEN** the route is implemented
- **THEN** it renders a native SwiftUI event-scoped surface with a clear title, current state, primary action, and back/navigation affordance
- **AND** it uses existing Wakeve iOS design tokens and components
- **AND** it supports VoiceOver labels for primary controls.

#### Scenario: iOS route is unavailable for a valid reason
- **GIVEN** an iOS route cannot match Android behavior due to platform or provider limitations
- **WHEN** the user reaches that route
- **THEN** the UI shows a specific event-scoped unavailable state
- **AND** it offers the next useful action such as returning to event detail, opening settings, or continuing with manual planning
- **AND** it does not present the unavailable state as a generic construction placeholder.

### Requirement: iOS Secondary Route Navigation
The iOS application MUST provide predictable native navigation for secondary event routes such as scenario detail, logistics, comments, photos/follow-up, organizer dashboard, leaderboard, notifications, settings, invitation share, meetings, budget, payment, and Tricount.

Secondary routes MUST be reachable from event detail, profile/home utilities, notification taps, or deep links as appropriate. The main tab bar MUST remain destination-only and MUST NOT add one-off event actions as permanent tabs.

#### Scenario: User opens an event secondary route
- **GIVEN** an iOS user is viewing an event detail screen
- **WHEN** they choose a secondary event action such as meals, equipment, comments, budget, payment, meetings, or transport
- **THEN** Wakeve opens the corresponding native route without switching the tab model
- **AND** the tab bar visibility and navigation back behavior remain predictable
- **AND** the route preserves the selected event after repository refreshes.

#### Scenario: Notification opens a private route
- **GIVEN** an iOS notification or universal link targets a private event route
- **WHEN** the user opens it
- **THEN** Wakeve routes through authentication and access checks before rendering content
- **AND** the destination follows the same visual hierarchy and read-only/access-denied patterns as manual navigation.
