# ios-design-system Specification

## Purpose
TBD - created by archiving change add-ios-design-system. Update Purpose after archive.
## Requirements
### Requirement: Centralized iOS Design Tokens
The iOS application SHALL expose a centralized design token system for colors, typography, spacing, radius, shadows, glass materials, and event gradients.

#### Scenario: A screen needs app colors
- **WHEN** an iOS SwiftUI screen needs a background, text, border, semantic, or action color
- **THEN** it SHALL use the centralized iOS design tokens instead of defining local hard-coded palettes.

#### Scenario: A screen needs event theming
- **WHEN** an iOS event screen renders an event preview, invitation, or poll experience
- **THEN** it SHALL use an event gradient token selected from the event type or explicit event background.

### Requirement: Reusable iOS Surface Components
The iOS application SHALL provide reusable SwiftUI components for screen backgrounds, glass cards, action buttons, circular icon buttons, search fields, list rows, avatars, section headers, and vote controls.

#### Scenario: A screen renders a card or grouped row
- **WHEN** a SwiftUI screen needs a card, grouped list row, or elevated content surface
- **THEN** it SHALL use the shared surface component with standardized radius, material, border, and shadow behavior.

#### Scenario: A screen renders primary or secondary actions
- **WHEN** a SwiftUI screen renders a primary, secondary, neutral, destructive, or event-next action
- **THEN** it SHALL use the shared action component so sizing, capsule shape, typography, disabled state, and contrast are consistent.

### Requirement: Screenshot-Inspired Experience Consistency
The iOS application SHALL align onboarding, notification, profile, friend/participant management, poll voting, and event preview experiences with the same visual system inspired by the supplied references.

#### Scenario: A user moves between high-visibility iOS screens
- **WHEN** the user navigates between onboarding, profile, friends/participants, poll voting, and event preview screens
- **THEN** the screens SHALL preserve consistent typography hierarchy, large rounded geometry, capsule actions, glass card treatment, icon button sizing, and dark-mode behavior.

#### Scenario: A utility screen uses system controls
- **WHEN** an iOS screen uses a `Form`, toggles, date pickers, or system rows
- **THEN** it SHALL still use the design system's accent, text, section, and background tokens while preserving native control behavior.

### Requirement: Accessible Liquid Glass Fallbacks
The iOS design system SHALL support native Liquid Glass where available and accessible fallbacks where Liquid Glass or transparency is unavailable or reduced.

#### Scenario: The OS supports native Liquid Glass
- **WHEN** the app runs on an iOS version with native Liquid Glass support
- **THEN** shared glass components SHALL use the native glass API with consistent shapes and interactive treatment for tappable elements.

#### Scenario: Transparency is reduced or native glass is unavailable
- **WHEN** Reduce Transparency is enabled or native Liquid Glass is unavailable
- **THEN** shared glass components SHALL render opaque or material-backed fallbacks that preserve contrast and hierarchy.

### Requirement: iOS Design System Documentation
The project SHALL document the iOS design system in the `docs/` tree.

#### Scenario: A developer builds or updates an iOS screen
- **WHEN** a developer needs to choose tokens, components, layout patterns, or migration rules for an iOS screen
- **THEN** the documentation SHALL provide token tables, component guidance, screenshot-inspired principles, accessibility rules, and migration checklists.

