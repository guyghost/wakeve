# User Authentication Specification

## Version
**Version**: 1.0.0
**Status**: ✅ Implémenté
**Date**: 3 février 2026

## Purpose

Provides a flexible authentication system supporting multiple sign-in methods (Google OAuth, Apple Sign-In, Email/OTP) and a guest mode with limited functionality. The system ensures secure token storage, RGPD compliance, and offline support for both authenticated and guest users.
## Requirements
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

### Requirement: Email Authentication with OTP
The system SHALL provide email-based authentication using One-Time Password (OTP) for users who prefer not to use social login providers.

#### Scenario: User initiates email authentication
- **WHEN** the user taps the "Sign in with Email" button
- **THEN** the application SHALL display:
  - Email input field
  - "Send OTP" button
  - Instructions explaining the OTP process

#### Scenario: System sends OTP email
- **WHEN** a valid email address is provided and user taps "Send OTP"
- **THEN** the system SHALL:
  - Validate the email format
  - Generate a 6-digit OTP code
  - Send the OTP code to the provided email address
  - Display a message confirming OTP was sent
  - Show OTP input field for verification

#### Scenario: User verifies OTP
- **WHEN** the user enters the correct OTP code and submits
- **THEN** the system SHALL:
  - Verify the OTP code
  - Check if OTP is still valid (5-minute expiry)
  - Create user account if email not already registered
  - Create session for registered user if email exists
  - Store authentication token securely
  - Navigate to main application with authenticated user state

#### Scenario: Invalid OTP entered
- **WHEN** the user enters an incorrect OTP code
- **THEN** the system SHALL:
  - Display error message indicating invalid OTP
  - Allow user to retry (max 3 attempts)
  - After 3 failed attempts, require sending new OTP

### Requirement: Guest Mode Limitations
The application SHALL provide a guest mode with limited functionality for users who skip authentication.

#### Scenario: Guest mode feature restrictions
- **WHEN** a user is in guest mode (has not authenticated)
- **THEN** the application SHALL:
  - Allow creating events locally
  - Allow participating in events as a guest
  - Allow viewing events locally
  - Prevent cloud synchronization of events
  - Prevent receiving push notifications
  - Prevent accessing premium features (collaborative albums, chat)
  - Display "Mode Invité" indicator in the UI

#### Scenario: Guest mode data persistence
- **WHEN** a user is in guest mode
- **THEN** the application SHALL:
  - Store all data locally in SQLite
  - Allow data export/import for backup
  - Provide option to authenticate later to migrate guest data

### Requirement: Authentication State Management
The application SHALL maintain authentication state across app launches and integrate with the global application state.

#### Scenario: Authenticated user returns to app
- **WHEN** an authenticated user reopens the application
- **THEN** the application SHALL:
  - Validate stored authentication token
  - If token is valid, automatically log user in
  - Skip authentication screen
  - Navigate to main application

#### Scenario: Guest user returns to app
- **WHEN** a guest user reopens the application
- **THEN** the application SHALL:
  - Restore guest session
  - Skip authentication screen
  - Navigate to main application
  - Maintain "Mode Invité" indicator

#### Scenario: Session expires
- **WHEN** the authentication token has expired
- **THEN** the application SHALL:
  - Display authentication screen
  - Allow user to re-authenticate
  - Preserve local data during re-authentication

### Requirement: Token Security
The application SHALL store authentication tokens securely using platform-specific secure storage mechanisms.

#### Scenario: Storing authentication tokens
- **WHEN** a user successfully authenticates
- **THEN** the application SHALL:
  - On Android: Store tokens in Android Keystore
  - On iOS: Store tokens in iOS Keychain
  - Never store tokens in SharedPreferences or UserDefaults (plain text)

#### Scenario: Retrieving authentication tokens
- **WHEN** the application needs to access stored tokens
- **THEN** the application SHALL:
  - Retrieve tokens from secure storage only
  - Handle secure storage access errors gracefully
  - Fall back to requiring re-authentication if tokens are inaccessible

### Requirement: Privacy and RGPD Compliance
The authentication system SHALL comply with RGPD principles of data minimization and user consent.

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
  - Allow complete data deletion on request

#### Scenario: Data deletion on request
- **WHEN** a user requests account deletion
- **THEN** the system SHALL:
  - Delete all user data from backend
  - Delete authentication tokens
  - Delete local data
  - Provide confirmation of deletion

### Requirement: Offline Support
The authentication system SHALL support offline access for both authenticated and guest users.

#### Scenario: Authenticated user offline
- **WHEN** an authenticated user uses the app offline
- **THEN** the application SHALL:
  - Allow access to previously synced data
  - Allow local data modifications
  - Queue changes for synchronization when online
  - Display offline indicator in UI

#### Scenario: Guest user offline
- **WHEN** a guest user uses the app offline
- **THEN** the application SHALL:
  - Continue full functionality (all data is local anyway)
  - Not display offline indicator (guest mode is inherently local-first)

### Requirement: Authentication Error Handling
The application SHALL provide clear error messages and recovery options for authentication failures.

#### Scenario: Network error during authentication
- **WHEN** a network error occurs during authentication
- **THEN** the application SHALL:
  - Display user-friendly error message
  - Allow retry operation
  - Do not crash or show technical errors

#### Scenario: OAuth provider error
- **WHEN** Google or Apple authentication fails
- **THEN** the application SHALL:
  - Display specific error message from provider
  - Allow user to retry
  - Offer alternative authentication methods (Email, other provider)

#### Scenario: Invalid credentials
- **WHEN** authentication fails due to invalid credentials
- **THEN** the application SHALL:
  - Display clear error message
  - Highlight the problematic field (if applicable)
  - Allow user to correct and retry

