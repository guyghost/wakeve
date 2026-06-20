## MODIFIED Requirements
### Requirement: Privacy and RGPD Compliance
The authentication system SHALL comply with RGPD principles of data minimization, user consent, and user-initiated data deletion.

#### Scenario: Minimal data collection
- **WHEN** a user authenticates (via Google, Apple, or Email)
- **THEN** the system SHALL:
  - Collect only necessary data: email, name (if provided), auth provider
  - Do NOT collect unnecessary personal information
  - Display clear consent message during authentication

#### Scenario: Guest mode privacy
- **WHEN** a user uses guest mode
- **THEN** the system SHALL:
  - Store all data locally on device
  - Not send any data to backend servers
  - Provide clear indication of local-only storage
  - Allow complete local data deletion on request

#### Scenario: Authenticated account deletion from iOS Profile
- **WHEN** an authenticated user opens Profile settings and chooses Delete Account
- **THEN** the application SHALL:
  - Present a destructive confirmation that states deletion is permanent
  - Initiate account deletion directly in-app without requiring email support or a web-only workflow
  - Call the authenticated backend deletion endpoint when the user confirms
  - Show a completion message when deletion succeeds
  - Clear authentication tokens, cached profile data, synced local user data, and analytics identifiers
  - Return the user to the unauthenticated onboarding or login state

#### Scenario: Backend account deletion scope
- **WHEN** the authenticated backend deletion endpoint accepts a deletion request
- **THEN** the system SHALL:
  - Delete the user's account record and personal profile fields
  - Revoke active sessions and authentication tokens
  - Remove registered push notification tokens
  - Delete or anonymize user-owned content according to collaborative event integrity rules
  - Record a security audit event for the deletion request and outcome

#### Scenario: Guest local data deletion
- **WHEN** a guest user chooses Delete Guest Data
- **THEN** the application SHALL:
  - Clear local guest identifiers and local-only event data
  - Avoid calling backend deletion APIs
  - Return the user to the unauthenticated onboarding or login state

#### Scenario: Offline authenticated deletion is not accepted
- **WHEN** an authenticated user confirms account deletion while the backend cannot be reached
- **THEN** the application SHALL:
  - Not claim that the account has been deleted
  - Keep credentials available for retry unless the user explicitly signs out
  - Display a clear retryable error message
