## ADDED Requirements
### Requirement: Cross-Platform Notification Route Parity
Wakeve MUST route notification taps and notification preference links consistently on Android and iOS for product-scoped destinations.

Notification tap handling MUST support event detail, poll voting, scenario, budget, meeting, comments, settings, notification preferences, and unread notification list destinations where the current user has access. Private destinations MUST pass through authentication and event access gates before content is shown.

#### Scenario: iOS user opens notification for event comments
- **GIVEN** an iOS push or in-app notification targets an event comments destination
- **AND** the current user is authenticated and allowed to view that event section
- **WHEN** the user opens the notification
- **THEN** Wakeve navigates to the native iOS comments route for that event
- **AND** preserves the event-scoped context instead of falling back to a generic inbox.

#### Scenario: User opens notification preferences from a notification route
- **GIVEN** Android supports a notification preferences route
- **WHEN** an iOS user opens the equivalent settings or notification-preferences link
- **THEN** Wakeve opens the native iOS notification preferences surface
- **AND** shows both Wakeve preference state and iOS system permission state.
