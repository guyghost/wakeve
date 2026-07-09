## ADDED Requirements

### Requirement: Invitation URLs SHALL Use Verified Universal Links
Production invitation URLs SHALL use HTTPS Universal Links on controlled domains. Each domain SHALL serve a valid AASA document without redirect or authentication, with the correct content type, production app identifier, and invitation-path restriction. The web fallback SHALL expose no private event content and SHALL provide a safe next action.

#### Scenario: iOS opens a production invitation
- **GIVEN** the signed application has the matching associated-domain entitlement
- **AND** the production host serves a matching AASA document
- **WHEN** a user taps a supported invitation URL
- **THEN** iOS SHALL route it to the modeled invitation flow when the app is installed
- **AND** the web fallback SHALL remain safe when the app is absent
- **AND** sensitive redemption SHALL not downgrade to a custom URL scheme.

#### Scenario: Release gate verifies live AASA over HTTP
- **GIVEN** a candidate signed archive and its production associated domains
- **WHEN** the production release gate performs a fresh public HTTP request to each `https://<host>/.well-known/apple-app-site-association`
- **THEN** every response SHALL be direct HTTP `200` with no redirect or authentication
- **AND** SHALL have an accepted JSON content type and a body matching the archive Team ID, bundle ID, and invitation path components
- **AND** any DNS, TLS, status, redirect, content-type, parse, or appID/path mismatch SHALL block TestFlight/App Store readiness.
