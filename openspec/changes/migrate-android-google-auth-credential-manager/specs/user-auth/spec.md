## MODIFIED Requirements
### Requirement: Optional Authentication Screen
The application SHALL present an optional authentication screen as the first step after app installation, before accessing the main application features.

#### Scenario: User sees auth options on first launch
- **WHEN** a user opens the application for the first time
- **THEN** the application SHALL display the authentication screen with:
  - Title and welcoming message
  - Sign in with Google button
  - Sign in with Apple button
  - Sign in with Email link/button
  - "Passer" (Skip) button positioned at top-right of the screen

#### Scenario: User skips authentication
- **WHEN** the user taps the "Passer" button
- **THEN** the application SHALL:
  - Create a guest session with limited permissions
  - Navigate to the main application interface
  - Display the user as "Invité" (Guest)
  - Allow basic event creation and participation without account

#### Scenario: User chooses Google Sign-In
- **WHEN** the user taps the Google Sign-In button
- **THEN** the application SHALL:
  - Initiate Google OAuth flow
  - On Android, use AndroidX Credential Manager with Google ID credentials rather than deprecated Google Sign-In APIs
  - Request user consent for profile and email access
  - Create user account upon successful authentication
  - Store authentication token securely
  - Navigate to main application with authenticated user state

#### Scenario: User chooses Apple Sign-In
- **WHEN** the user taps the Apple Sign-In button
- **THEN** the application SHALL:
  - Initiate Apple Sign-In flow (Sign in with Apple)
  - Request user consent for name and email (if not hidden)
  - Create user account upon successful authentication
  - Store authentication token securely
  - Navigate to main application with authenticated user state

### Requirement: Android Google Authentication Provider
The Android Google authentication implementation SHALL use Credential Manager compatible APIs and SHALL preserve the existing Wakeve auth result semantics.

#### Scenario: Android Google sign-in succeeds
- **WHEN** an Android user completes Google account selection through Credential Manager
- **THEN** the application SHALL validate the Google ID credential response
- **AND** SHALL pass the resulting Google auth token or equivalent credential to the existing shared authentication flow
- **AND** SHALL avoid direct use of deprecated `GoogleSignIn`, `GoogleSignInClient`, `GoogleSignInAccount`, or `GoogleSignInOptions` APIs.

#### Scenario: Android Google sign-in is cancelled or unavailable
- **WHEN** Credential Manager returns cancellation, no credential, provider unavailable, or invalid credential
- **THEN** the application SHALL map the result to the existing Wakeve auth error semantics
- **AND** SHALL leave the current auth state recoverable without creating a placeholder authenticated user.
