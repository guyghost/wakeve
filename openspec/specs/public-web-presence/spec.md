# public-web-presence Specification

## Purpose
Define Wakeve's public web presence, App Store review web endpoints, and authenticated dashboard routing boundaries.

## Requirements
### Requirement: Public landing application
Wakeve SHALL provide a public web landing application that renders at the root path without requiring authentication.

#### Scenario: Visitor opens root domain
- **WHEN** a visitor opens `/`
- **THEN** the system SHALL render the Wakeve landing page
- **AND** the page SHALL include a primary call to action linking to `/app/login`
- **AND** the visitor SHALL NOT be redirected to the authenticated application.

### Requirement: Authenticated dashboard microfrontend
Wakeve SHALL expose authenticated web application routes under the `/app` path using a separate Vercel microfrontend application.

#### Scenario: User opens dashboard login
- **WHEN** a user opens `/app/login`
- **THEN** the dashboard application SHALL render the login experience.

#### Scenario: User opens protected dashboard route
- **WHEN** an unauthenticated user opens `/app/events`
- **THEN** the dashboard application SHALL require authentication before showing event data.

### Requirement: Public App Store web routes
Wakeve SHALL keep legal, support, and Apple App Site Association endpoints public on the root domain.

#### Scenario: App Store reviewer opens public routes
- **WHEN** a reviewer opens `/privacy`, `/support`, `/terms`, `/third-party-notices`, `/.well-known/apple-app-site-association`, or `/apple-app-site-association`
- **THEN** each route SHALL respond from the public landing application without requiring authentication.

### Requirement: Legacy authenticated path redirects
Wakeve SHALL redirect legacy authenticated web paths to their `/app` equivalents.

#### Scenario: Existing app link is opened
- **WHEN** a user opens `/login`, `/dashboard`, `/create`, `/explore`, `/profile`, or `/events/{id}`
- **THEN** the system SHALL redirect to the matching `/app` path.
