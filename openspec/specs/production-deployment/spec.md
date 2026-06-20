# production-deployment Specification

## Purpose
Define production deployment requirements for Wakeve public backend, App Store live endpoint evidence, and infrastructure safety gates.

## Requirements
### Requirement: Public Cloudflare backend deployment
Wakeve SHALL deploy the Ktor backend behind a public Cloudflare HTTPS endpoint at `api.wakeve.app`.

#### Scenario: App Review checks backend health
- **GIVEN** the production backend has been deployed to Cloudflare Containers
- **AND** public DNS for `api.wakeve.app` resolves from an external network
- **WHEN** an App Review checker requests `https://api.wakeve.app/health`
- **THEN** the response SHALL be served over valid HTTPS
- **AND** the response status SHALL be `200 OK`
- **AND** the response body SHALL indicate the backend is healthy without exposing secrets or private data.

#### Scenario: DNS is not publicly configured
- **GIVEN** `api.wakeve.app` does not resolve publicly
- **WHEN** the live URL/AASA audit runs
- **THEN** App Store AS-14 evidence SHALL remain incomplete
- **AND** the deployment SHALL NOT be marked submission-ready.

### Requirement: Cloudflare Container runtime configuration
Wakeve SHALL configure the Cloudflare Container runtime without committing production secrets.

#### Scenario: Production secret is missing
- **GIVEN** the backend container starts in production
- **WHEN** `JWT_SECRET` is not configured as a Cloudflare secret or environment value
- **THEN** the backend SHALL fail startup
- **AND** the deployment SHALL be treated as failed.

#### Scenario: OAuth redirect values are configured
- **GIVEN** Google or Apple OAuth is enabled for the production backend
- **WHEN** the backend is deployed
- **THEN** provider redirect URIs SHALL use public `https://api.wakeve.app` callback URLs
- **AND** no localhost callback SHALL be used by the production environment.

### Requirement: Backend deployment safety gates
Wakeve SHALL verify the public backend before closing App Store live URL evidence.

#### Scenario: Public smoke test runs after deployment
- **GIVEN** the Cloudflare deployment is complete
- **WHEN** the release smoke test runs from a public network
- **THEN** it SHALL verify public DNS resolution for `api.wakeve.app`
- **AND** verify `GET https://api.wakeve.app/health`
- **AND** verify protected API endpoints reject unauthenticated requests
- **AND** verify production metrics are not publicly readable without an allowed source.

#### Scenario: Evidence is updated
- **GIVEN** all public live URL, AASA, dashboard, redirect, and backend checks pass
- **WHEN** `docs/APP_STORE_LIVE_URL_AASA_EVIDENCE.md` is updated
- **THEN** the evidence SHALL include DNS output, HTTPS/backend health output, deployment identifier, rollback instructions, reviewer/date, and command output.

### Requirement: Container persistence limits are explicit
Wakeve SHALL document and enforce the persistence limitations of Cloudflare Container deployment.

#### Scenario: Backend uses container-local SQLite
- **GIVEN** the backend is deployed with container-local SQLite storage
- **WHEN** the deployment is used for App Review or smoke testing
- **THEN** the deployment SHALL be documented as non-durable for production user data
- **AND** the backend SHALL NOT be scaled beyond a topology that preserves expected review behavior unless durable external persistence is configured.

#### Scenario: Durable production use is required
- **GIVEN** Wakeve needs durable production backend state
- **WHEN** the backend is promoted beyond review/smoke usage
- **THEN** the deployment SHALL use an external durable datastore or an explicitly approved persistence design
- **AND** the production runbook SHALL describe backup, restore, migration, and rollback behavior.
