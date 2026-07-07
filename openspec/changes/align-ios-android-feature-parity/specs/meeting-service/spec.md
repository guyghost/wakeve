## ADDED Requirements
### Requirement: Cross-Platform Meeting Link Launch Parity
Wakeve MUST provide equivalent Android and iOS behavior for opening stored meeting links from event-scoped meeting surfaces.

Meeting creation through external providers remains governed by existing provider configuration requirements. When a meeting link already exists and the current user has access, each mobile platform MUST expose a native launch path, validate that the URL is safe for the stored provider metadata, and show a clear unavailable or warning state when the link cannot be opened safely.

#### Scenario: iOS user opens stored meeting link
- **GIVEN** an event meeting has a stored HTTPS, FaceTime, Zoom, Google Meet, Teams, or Webex URL
- **AND** the current iOS user is allowed to view the meeting
- **WHEN** the user taps the meeting join action
- **THEN** Wakeve opens the link through native iOS URL handling when safe
- **AND** does not require provider creation credentials just to launch an existing meeting link.

#### Scenario: Provider metadata and URL do not match
- **GIVEN** a stored meeting is marked as Zoom
- **AND** the stored target URL does not match the provider's expected safe URL patterns
- **WHEN** an Android or iOS user tries to open the link
- **THEN** Wakeve warns the user before opening or blocks the launch according to the link safety policy
- **AND** records the suspicious-link state for review or audit when supported.
