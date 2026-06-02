## ADDED Requirements
### Requirement: Account Deletion Security Enforcement
The system SHALL enforce secure, authenticated, auditable account deletion for registered users.

#### Scenario: Authenticated deletion request
- **GIVEN** a registered user has a valid session
- **WHEN** the user sends `DELETE /api/user/delete`
- **THEN** the system SHALL:
  - Authenticate the bearer token
  - Delete or anonymize the authenticated user's personal data
  - Revoke every active session for the user
  - Reject future use of the deleted user's access and refresh tokens
  - Return a stable success response
  - Write an audit log containing the deleted user ID, request ID, timestamp, and outcome

#### Scenario: Unauthenticated deletion request
- **GIVEN** no valid bearer token is provided
- **WHEN** a deletion request is sent to `DELETE /api/user/delete`
- **THEN** the system SHALL:
  - Deny the request with HTTP 401 Unauthorized
  - Avoid deleting any account data
  - Log the failed attempt without exposing sensitive token details

#### Scenario: Repeated deletion request
- **GIVEN** a user deletion request has already completed
- **WHEN** the same authenticated deletion operation is replayed or retried
- **THEN** the system SHALL:
  - Treat the operation as idempotent for user-visible behavior
  - Not restore or recreate deleted account data
  - Return a success or gone response that the client can handle as complete

#### Scenario: Sign in with Apple revocation evidence
- **GIVEN** the deleted account was created with Sign in with Apple
- **WHEN** account deletion is processed
- **THEN** the system SHALL:
  - Attempt Apple credential revocation when required authorization material is available
  - Record whether Apple revocation succeeded, failed, or was unavailable
  - Complete Wakeve-side account deletion even if provider revocation fails transiently
