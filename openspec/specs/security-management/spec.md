# Specification: Security Management

> **Capability**: `security-management`
> **Version**: 1.0.0
> **Status**: Active
> **Last Updated**: 2026-02-08

## Overview

This specification defines the comprehensive security framework for the Wakeve platform, including authentication, authorization, session management, data protection, and audit logging. It establishes patterns that ALL other specifications MUST follow to ensure consistent security across the platform.

**Version**: 1.0.0
**Status**: Active
**Created**: 2026-02-08
**Maintainer**: Platform Team

### Core Concepts

**JWT (JSON Web Token)**: Cryptographic tokens for authenticated sessions between client and server, signed with HS256 algorithm.

**Principal**: The authenticated user identity represented by a JWT token with claims for userId, email, role, permissions, and sessionId.

**RBAC (Role-Based Access Control)**: Hierarchical permission model with roles (USER, ORGANIZER, MODERATOR, ADMIN) and fine-grained permissions.

**IDOR (Insecure Direct Object Reference)**: A vulnerability where users can access resources they don't have permission to access. This spec defines patterns to prevent IDOR.

**Fail-Closed**: Security principle where errors result in denial of access rather than allowing access.

**Guest Mode**: Local-only access mode allowing users to explore the app without account creation.

### Key Features

- **Multi-provider OAuth2 Authentication**: Support for Google and Apple OAuth2 with extensible provider architecture
- **JWT-based Session Management**: Secure, stateless tokens with configurable expiry and refresh token support
- **Role-Based Access Control (RBAC)**: Hierarchical roles (USER, ORGANIZER, MODERATOR, ADMIN) with fine-grained permissions
- **Resource-Level Authorization**: Ownership-based access control for events, budgets, and other user resources
- **Guest Mode**: Local-only access mode for trying the application without account creation
- **Audit Logging**: Structured JSON logging for authentication, authorization, and data access events
- **Security Configuration**: Environment-aware security settings with production safeguards
- **Rate Limiting**: Per-IP and per-user rate limits on all endpoints
- **Input Validation**: Standardized validation for all API inputs

### Dependencies

| Dependency | Type | Description |
|------------|------|-------------|
| `user-auth` | Spec | Core authentication models and interfaces |
| `event-organization` | Spec | Event ownership and participation model |
| `offline-sync` | Spec | Local data persistence for guest mode |
| All specs | Spec | All specs MUST include Security section referencing this spec |

## Purpose

The Security Management capability provides the foundation for protecting user data and ensuring that only authorized users can access and modify resources within the Wakeve platform. It implements defense-in-depth principles with multiple layers of security controls.

1. All API endpoints are protected by authentication
2. Authorization is verified at resource level (preventing IDOR)
3. Security events are logged for audit and compliance
4. Rate limiting prevents abuse and DoS attacks
5. Input validation prevents injection attacks
6. Environment-aware configuration prevents security misconfigurations

### Use Cases

- **User Authentication**: Users can sign in using their existing Google or Apple accounts without creating passwords
- **Secure API Access**: All API endpoints require valid JWT tokens with verified claims
- **Resource Authorization**: Users can only access events they organize or participate in
- **Session Management**: Users can view and revoke their active sessions across multiple devices
- **Guest Exploration**: New users can explore the app locally before committing to account creation
- **Security Monitoring**: Administrators can review audit logs to detect suspicious activity
- **Compliance**: Audit trails support GDPR and other data protection regulations

## Requirements

### Requirement: JWT Token Structure
**ID**: `SEC-001`

All JWT tokens issued by the Wakeve API SHALL contain the following claims:

- `iss` (issuer): API identifier from configuration
- `aud` (audience): Client identifier from configuration
- `sub` (subject): Unique user ID
- `userId`: User's unique identifier
- `email`: User's email address
- `role`: User's role (USER, ORGANIZER, MODERATOR, ADMIN)
- `permissions`: Array of permission strings
- `iat` (issued at): Token creation timestamp
- `exp` (expires at): Token expiration timestamp
- `sessionId`: Unique session identifier
- `jti` (JWT ID): Unique token identifier for revocation

**Business Rules:**
- Access tokens expire after 1 hour (3600 seconds)
- Refresh tokens expire after 30 days
- Tokens are signed using HMAC-SHA256 algorithm
- Tokens are revoked on logout or session revocation
- Tokens are added to blacklist on revocation
- Blacklist checks MUST fail closed (deny access on error)

#### Scenario: Token validation
- **GIVEN** a JWT token is presented to an authenticated endpoint
- **WHEN** the token is verified
- **THEN** the system SHALL:
  - Verify the signature is valid
  - Verify the issuer matches the configured issuer
  - Verify the audience matches the configured audience
  - Verify the expiration date is in the future
  - Verify the token is not blacklisted
  - Extract user identity (userId, sessionId, role, permissions)

#### Scenario: OAuth2 authentication flow
- **GIVEN** a user selects an OAuth provider (Google or Apple)
- **WHEN** the user completes the provider's authentication flow
- **THEN** the system SHALL:
  - Exchange the authorization code or ID token for user information
  - Create or update the user record
  - Generate a JWT token with appropriate claims based on user's role
  - Return the token to the client with refresh token

#### Scenario: Guest mode creation
- **GIVEN** a user selects guest mode
- **WHEN** guest mode is initialized
- **THEN** the system SHALL:
  - Generate a unique guest identifier
  - Store the guest ID locally on the device
  - Ensure all data is stored locally only
  - Never synchronize data to the server

### Requirement: Role-Based Access Control (RBAC)
**ID**: `SEC-002`

The system SHALL implement hierarchical roles with the following permissions:

| Role | Description | Key Permissions |
|------|-------------|-----------------|
| USER | Standard user | Create events, manage own data |
| ORGANIZER | Event organizer | All USER permissions + manage events they organize |
| MODERATOR | Content moderator | All ORGANIZER permissions + moderate content, ban users |
| ADMIN | System administrator | Full system access including metrics and settings |

**Business Rules:**
- Permissions are evaluated at both role level and resource ownership level
- Role hierarchy is cumulative (ADMIN has all MODERATOR permissions, etc.)
- JWT tokens include both role and permissions claims for efficient authorization
- Resource-level checks verify ownership or participation

#### Scenario: Permission check
- **GIVEN** a user with role ORGANIZER attempts to delete an event
- **WHEN** the user is not the event organizer
- **THEN** the system SHALL:
  - Deny the request with HTTP 403 Forbidden
  - Log an authorization failure event

### Requirement: Resource-Level Authorization
**ID**: `SEC-003`

The system SHALL verify that authenticated users have permission to access requested resources based on ownership or participation.

**Business Rules:**
- Event organizers have full access to their events
- Event participants have read/write access based on event status
- Non-participants have no access to event details
- Budget access is granted to event organizers and participants only

#### Scenario: Event access validation
- **GIVEN** a user attempts to access an event
- **WHEN** the user is neither the organizer nor a participant
- **THEN** the system SHALL:
  - Return HTTP 403 Forbidden
  - Log the authorization failure with user ID, resource ID, and action

#### Scenario: Budget access validation (IDOR prevention)
- **GIVEN** a user attempts to access `/api/events/{eventId}/budget`
- **WHEN** the user does not participate in the event
- **THEN** the system SHALL:
  - Return HTTP 403 Forbidden
  - Prevent Insecure Direct Object Reference (IDOR) attacks

### Requirement: Session Management
**ID**: `SEC-004`

The system SHALL support multi-device session management with the following capabilities:

- Users can view all active sessions
- Users can revoke specific sessions (except current)
- Users can revoke all other sessions
- Sessions track device name, device ID, IP address, and last accessed time

**Business Rules:**
- Sessions are identified by unique sessionId in JWT token
- Current session cannot be revoked (use logout instead)
- Session data includes deviceName, deviceId, ipAddress, createdAt, lastAccessed
- Session timeout varies by environment (production: 60min, staging: 120min, dev: 480min)

#### Scenario: List active sessions
- **GIVEN** an authenticated user requests their sessions
- **WHEN** the request is processed
- **THEN** the system SHALL:
  - Return all active sessions for the user
  - Mark the current session in the response

#### Scenario: Revoke session
- **GIVEN** a user attempts to revoke a session
- **WHEN** the session ID belongs to the user and is not the current session
- **THEN** the system SHALL:
  - Revoke the session
  - Add the token to the blacklist
  - Return success response

#### Scenario: Prevent current session revocation
- **GIVEN** a user attempts to revoke their current session
- **WHEN** the request is processed
- **THEN** the system SHALL:
  - Return HTTP 400 Bad Request
  - Instruct the user to use logout instead

### Requirement: Token Blacklist
**ID**: `SEC-005`

The system SHALL maintain a blacklist of revoked tokens.

**Business Rules:**
- Tokens are added to the blacklist on logout or session revocation
- Blacklist checks MUST fail closed (deny access on error)
- Blacklist entries SHALL expire with the token's original expiry time

#### Scenario: Blacklisted token rejection
- **GIVEN** a token has been revoked and added to the blacklist
- **WHEN** the token is presented for authentication
- **THEN** the system SHALL:
  - Reject the token with HTTP 401 Unauthorized

#### Scenario: Fail-closed blacklist check
- **GIVEN** the blacklist check fails due to a database error
- **WHEN** the error occurs during token validation
- **THEN** the system SHALL:
  - Deny access (fail closed)
  - Log the error for investigation

### Requirement: Audit Logging
**ID**: `SEC-006`

The system SHALL log all security-relevant events in structured JSON format.

**Business Rules:**
- Authentication failures MUST be logged
- Authorization failures MUST be logged
- Sensitive operations (create, update, delete) MUST be logged
- Logs include timestamp, userId, client IP, event type, details

#### Scenario: Log authentication failure
- **GIVEN** a user attempts to authenticate with invalid credentials
- **WHEN** authentication fails
- **THEN** the system SHALL log:
  ```json
  {
    "timestamp": "2026-02-08T14:30:00Z",
    "eventType": "AUTHENTICATION_FAILURE",
    "userId": null,
    "details": {
      "reason": "invalid_credentials",
      "email": "user@example.com",
      "sourceIp": "192.168.1.100"
    }
  }
  ```

#### Scenario: Log authorization failure
- **GIVEN** a user attempts to access a resource without permission
- **WHEN** authorization check fails
- **THEN** the system SHALL log:
  ```json
  {
    "timestamp": "2026-02-08T14:30:00Z",
    "eventType": "AUTHORIZATION_FAILURE",
    "userId": "user-123",
    "details": {
      "resource": "budget",
      "resourceId": "budget-456",
      "action": "read",
      "sourceIp": "192.168.1.100"
    }
  }
  ```

#### Scenario: Log sensitive operation
- **GIVEN** a user deletes an event
- **WHEN** the delete operation succeeds
- **THEN** the system SHALL log:
  ```json
  {
    "timestamp": "2026-02-08T14:30:00Z",
    "eventType": "SENSITIVE_OPERATION",
    "userId": "user-123",
    "details": {
      "operation": "delete_event",
      "resourceId": "event-789",
      "sourceIp": "192.168.1.100"
    }
  }
  ```

### Requirement: Rate Limiting
**ID**: `SEC-007`

The system SHALL apply rate limiting to all API endpoints to prevent abuse.

**Business Rules:**
- Auth endpoints: 10 requests per minute per IP
- API endpoints: 100 requests per minute per user
- Metrics endpoint: IP whitelist required
- Rate limit headers included in responses

#### Scenario: Rate limit exceeded on auth endpoint
- **GIVEN** a client makes 11 authentication requests within 1 minute from the same IP
- **WHEN** the 11th request is made
- **THEN** the system SHALL:
  - Return 429 Too Many Requests
  - Include `Retry-After: 60` header
  - Include `X-RateLimit-Limit: 10` header
  - Include `X-RateLimit-Remaining: 0` header
  - Include `X-RateLimit-Reset: <unix_timestamp>` header

#### Scenario: Rate limit not exceeded
- **GIVEN** a client makes 5 authentication requests within 1 minute
- **WHEN** each request is made
- **THEN** the system SHALL include:
  - `X-RateLimit-Limit: 10`
  - `X-RateLimit-Remaining: 5` (decrementing)
  - `X-RateLimit-Reset: <unix_timestamp>`

### Requirement: Security Configuration
**ID**: `SEC-008`

Security settings SHALL be environment-aware with production safeguards.

**Business Rules:**
- JWT secret MUST be set in production (application fails to start otherwise)
- Metrics endpoint SHALL be protected by IP whitelist in production
- HTTPS SHALL be required in production and staging
- Session timeout SHALL vary by environment (production: 1 hour, staging: 2 hours, development: 8 hours)
- Rate limits are configurable via environment variables

#### Scenario: Production JWT secret validation
- **GIVEN** the application starts in production mode
- **WHEN** JWT_SECRET is not configured
- **THEN** the application SHALL:
  - Fail to start
  - Throw IllegalStateException with clear error message

#### Scenario: Metrics access control
- **GIVEN** a request to /metrics endpoint
- **WHEN** the client IP is not in the whitelist
- **AND** the environment is production
- **THEN** the system SHALL:
  - Return HTTP 403 Forbidden

### Requirement: Input Validation
**ID**: `SEC-009`

The system SHALL validate all input parameters to prevent injection attacks.

**Business Rules:**
- String fields have maximum length constraints
- Numeric fields have range constraints
- Enum fields accept only valid values
- Invalid input returns 400 with error details

#### Scenario: Valid input
- **GIVEN** a user provides valid input within constraints
- **WHEN** the request is processed
- **THEN** the system SHALL process the request normally

#### Scenario: Invalid string length
- **GIVEN** a user provides a title with 201 characters (max: 200)
- **WHEN** the request is made
- **THEN** the system SHALL:
  - Return 400 Bad Request
  - Include error: "Title must not exceed 200 characters"

#### Scenario: Invalid numeric range
- **GIVEN** a user provides a duration of 1500 minutes (valid range: 1-1440)
- **WHEN** the request is made
- **THEN** the system SHALL:
  - Return 400 Bad Request
  - Include error: "Duration must be between 1 and 1440 minutes"

### Requirement: Guest Mode Data Isolation
**ID**: `SEC-010`

Guest mode SHALL operate with complete data isolation.

**Business Rules:**
- All guest data SHALL be stored locally on the device
- No guest data SHALL be synchronized to the server
- Guest sessions cannot access server-side features
- Conversion to authenticated user SHALL require explicit authentication

#### Scenario: Guest mode API access
- **GIVEN** a user is in guest mode
- **WHEN** the app attempts to access a server API endpoint
- **THEN** the request SHALL be rejected with HTTP 401 Unauthorized

#### Scenario: Guest to authenticated conversion
- **GIVEN** a guest user selects to sign in
- **WHEN** authentication completes successfully
- **THEN** the system SHALL:
  - Clear the guest session
  - Create a new authenticated session
  - Optionally migrate local data based on user consent

## Data Models

> All models are defined in language-agnostic format (JSON/Kotlin) to support multiplatform implementation.

### UserRole

```kotlin
enum class UserRole {
    USER,        // Standard user
    ORGANIZER,   // Event organizer
    MODERATOR,   // Content moderator
    ADMIN        // System administrator
}
```

### Permission

```kotlin
enum class Permission {
    // Event permissions
    EVENT_CREATE,
    EVENT_READ,
    EVENT_UPDATE_OWN,
    EVENT_UPDATE_ANY,
    EVENT_DELETE_OWN,
    EVENT_DELETE_ANY,

    // Participant permissions
    PARTICIPANT_INVITE,
    PARTICIPANT_REMOVE_OWN,
    PARTICIPANT_REMOVE_ANY,

    // Vote permissions
    VOTE_CREATE,
    VOTE_UPDATE_OWN,
    VOTE_UPDATE_ANY,
    VOTE_DELETE_OWN,
    VOTE_DELETE_ANY,

    // User management permissions
    USER_READ,
    USER_UPDATE_OWN,
    USER_UPDATE_ANY,
    USER_DELETE_OWN,
    USER_DELETE_ANY,
    USER_BAN,

    // Session management
    SESSION_READ_OWN,
    SESSION_READ_ANY,
    SESSION_REVOKE_OWN,
    SESSION_REVOKE_ANY,

    // Admin permissions
    SYSTEM_SETTINGS,
    SYSTEM_METRICS,
    SYSTEM_LOGS
}
```

### JWT Principal

```kotlin
data class JWTPrincipal(
    val userId: String,
    val sessionId: String,
    val email: String?,
    val role: UserRole,
    val permissions: List<Permission>,
    val issuedAt: Instant,
    val expiresAt: Instant
)
```

### Session

```kotlin
@Serializable
data class Session(
    val id: String,
    val userId: String,
    val deviceName: String,
    val deviceId: String,
    val ipAddress: String?,
    val createdAt: Instant,
    val lastAccessed: Instant,
    val expiresAt: Instant,
    val revoked: Boolean = false
)
```

### AuditEvent

```kotlin
@Serializable
data class AuditEvent(
    val timestamp: Instant,
    val userId: String?,
    val eventType: AuditEventType,
    val details: Map<String, String>
)

enum class AuditEventType {
    AUTHENTICATION_FAILURE,
    AUTHORIZATION_FAILURE,
    SENSITIVE_OPERATION,
    DATA_ACCESS,
    CONFIGURATION_CHANGE
}
```

### Security Configuration

```kotlin
data class SecurityConfig(
    val jwtSecret: String,
    val jwtIssuer: String,
    val jwtAudience: String,
    val jwtExpirationMinutes: Long = 60,
    val refreshExpirationDays: Int = 30,
    val requireHttps: Boolean = true,
    val enableAuditLog: Boolean = true,
    val environment: Environment
)

enum class Environment {
    PRODUCTION,
    STAGING,
    DEVELOPMENT
}
```

### Rate Limit Config

```kotlin
data class RateLimitConfig(
    val endpoint: String,
    val requestsPerWindow: Int,
    val windowMinutes: Int,
    val perUser: Boolean = true,
    val perIp: Boolean = false
)
```

## API / Interface

### Standard Security Headers

All API responses SHALL include:

| Header | Value | Purpose |
|--------|-------|---------|
| `X-Content-Type-Options` | `nosniff` | Prevent MIME sniffing |
| `X-Frame-Options` | `DENY` | Prevent clickjacking |
| `X-XSS-Protection` | `1; mode=block` | XSS protection |
| `Strict-Transport-Security` | `max-age=31536000` | Force HTTPS |

### Standard Error Response Format

```json
{
  "error": "error_code",
  "message": "Human-readable error message",
  "details": {
    "field": "Additional context"
  },
  "status": 400
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `unauthorized` | 401 | Authentication required or invalid |
| `forbidden` | 403 | Insufficient permissions |
| `token_expired` | 401 | JWT token has expired |
| `token_revoked` | 401 | JWT token was revoked |
| `validation_error` | 400 | Input validation failed |
| `rate_limit_exceeded` | 429 | Too many requests |

## Security

### Authentication Requirements

All endpoints except `/auth/*` and `/health` MUST require authentication.

Guest access is allowed only for:
- Event invitation links (with valid token)
- Public event profiles (if event is public)

### Authorization Requirements

| Resource | Create | Read | Update | Delete | Notes |
|----------|--------|------|--------|--------|-------|
| Events (own) | USER | USER | USER | USER | Full CRUD for own events |
| Events (participating) | - | USER | - | - | Read-only as participant |
| Events (other) | - | - | - | - | No access |
| Budgets | ORGANIZER | PARTICIPANT | ORGANIZER | ORGANIZER | Event-scoped access |
| Sessions | - | OWNER | - | OWNER | Users manage own sessions |
| Users | - | ADMIN | OWNER | ADMIN | Self-update or admin only |

### Data Protection

- **Passwords**: Hashed with bcrypt (10 rounds) before storage
- **JWT Secret**: Minimum 32 characters, randomly generated
- **PII**: User email, phone numbers stored encrypted at rest (optional)
- **API Keys**: Zoom/Google Meet keys stored server-side, never exposed to clients

### GDPR Compliance

- **Right to Access**: Users can export all their data via `/api/user/export`
- **Right to Deletion**: Users can delete their account via `/api/user/delete`
- **Right to Rectification**: Users can update their profile via `/api/user/profile`
- **Data Minimization**: Only collect necessary data
- **Consent**: Explicit consent for data processing

## State Machine Integration

### Security-Related Intents

```kotlin
sealed interface SecurityIntent : Intent {
    data class Login(val credentials: Credentials) : SecurityIntent
    data class Logout(val sessionId: String) : SecurityIntent
    data class RefreshToken(val refreshToken: String) : SecurityIntent
}
```

### Side Effects

| Intent | Side Effect | Description |
|--------|-------------|-------------|
| Login | NavigateTo("home") | On successful login |
| Login | ShowToast("Welcome back") | Success notification |
| Logout | ClearUserData() | Remove local data |
| Logout | NavigateTo("login") | Return to login screen |

## Database Schema

```sql
-- Users
CREATE TABLE users (
    id TEXT PRIMARY KEY,
    provider_id TEXT NOT NULL,
    provider TEXT NOT NULL CHECK(provider IN ('google', 'apple', 'email')),
    email TEXT NOT NULL UNIQUE,
    name TEXT,
    avatar_url TEXT,
    role TEXT NOT NULL DEFAULT 'USER' CHECK(role IN ('USER', 'ORGANIZER', 'MODERATOR', 'ADMIN')),
    created_at INTEGER NOT NULL,
    updated_at INTEGER
);

CREATE INDEX idx_users_provider_id ON users(provider_id, provider);
CREATE INDEX idx_users_email ON users(email);

-- OAuth Tokens
CREATE TABLE user_tokens (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    access_token TEXT NOT NULL,
    refresh_token TEXT UNIQUE NOT NULL,
    expires_at INTEGER NOT NULL,
    scope TEXT,
    created_at INTEGER NOT NULL
);

CREATE INDEX idx_user_tokens_user_id ON user_tokens(user_id);
CREATE INDEX idx_user_tokens_refresh ON user_tokens(refresh_token);

-- Sessions
CREATE TABLE sessions (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    device_name TEXT NOT NULL,
    device_id TEXT NOT NULL,
    ip_address TEXT,
    created_at INTEGER NOT NULL,
    last_accessed INTEGER NOT NULL,
    expires_at INTEGER NOT NULL,
    revoked INTEGER DEFAULT 0
);

CREATE INDEX idx_sessions_user_id ON sessions(user_id);
CREATE INDEX idx_sessions_device_id ON sessions(device_id);

-- Token Blacklist
CREATE TABLE token_blacklist (
    jti TEXT PRIMARY KEY,
    user_id TEXT NOT NULL,
    expires_at INTEGER NOT NULL,
    revoked_at INTEGER NOT NULL
);

CREATE INDEX idx_token_blacklist_user ON token_blacklist(user_id);
CREATE INDEX idx_token_blacklist_expires ON token_blacklist(expires_at);
```

## Testing Requirements

### Unit Tests

- **SecurityConfigTest** (5 tests):
  - JWT secret validation in production
  - Rate limit configuration parsing
  - Environment detection
- **AuditLoggerTest** (8 tests):
  - Authentication failure logging
  - Authorization failure logging
  - Sensitive operation logging
  - JSON output format validation

### Integration Tests

- **AuthenticationFlowTest** (4 tests):
  - Login → Receive tokens
  - Refresh token → New access token
  - Logout → Token revoked
  - Blacklisted token → 401 response

- **AuthorizationTest** (8 tests):
  - Organizer accesses own event → 200
  - Participant accesses event → 200
  - Non-participant accesses event → 403
  - Organizer accesses budget → 200
  - Participant accesses budget → 200
  - Non-participant accesses budget → 403 (IDOR prevention)
  - User with ADMIN role accesses metrics → 200
  - User with USER role attempts metrics → 403

- **SessionManagementTest** (4 tests):
  - List active sessions → Returns user's sessions
  - Revoke other session → Session revoked successfully
  - Attempt to revoke current session → 400 Bad Request
  - Revoke all other sessions → All other sessions revoked

- **RateLimitTest** (3 tests):
  - Within limit → 200
  - Exceeds limit → 429
  - Reset after window → 200

**Coverage Target**: 90%

### Security Tests

- **OWASP Top 10** coverage:
  - Injection (SQL, NoSQL, Command)
  - Broken Authentication
  - Sensitive Data Exposure
  - XML External Entities (XXE)
  - Broken Access Control
  - Security Misconfiguration
  - Cross-Site Scripting (XSS)
  - Insecure Deserialization
  - Using Components with Known Vulnerabilities
  - Insufficient Logging & Monitoring

## Platform-Specific Implementation

### Shared Layer

- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/RolesAndPermissions.kt` - RBAC definitions
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/shell/services/GuestModeService.kt` - Guest mode logic
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/SessionManager.kt` - Session management interface

### Server

- `server/src/main/kotlin/com/guyghost/wakeve/auth/AuthenticationService.kt` - OAuth2 authentication
- `server/src/main/kotlin/com/guyghost/wakeve/auth/JWTExtensions.kt` - JWT principal extensions
- `server/src/main/kotlin/com/guyghost/wakeve/routes/AuthRoutes.kt` - Authentication endpoints
- `server/src/main/kotlin/com/guyghost/wakeve/routes/SessionRoutes.kt` - Session management endpoints
- `server/src/main/kotlin/com/guyghost/wakeve/security/SecurityConfig.kt` - Security configuration
- `server/src/main/kotlin/com/guyghost/wakeve/security/AuditLogger.kt` - Audit logging

### Android

- Token storage in EncryptedSharedPreferences
- Biometric authentication option (future)

### iOS

- Token storage in Keychain
- FaceID/TouchID authentication option (future)

## Migration / Compatibility

### API Versioning

- **Current Version**: v1
- **Backward Compatibility**: Breaking changes require new version
- **Deprecation Timeline**: 6 months notice for breaking changes
- **Migration Path**: Clients must update before deadline

### Security Migration

1. **Phase 1**: Implement JWT authentication with blacklist support
2. **Phase 2**: Add RBAC with resource-level authorization
3. **Phase 3**: Implement audit logging
4. **Phase 4**: Add rate limiting
5. **Phase 5**: Production safeguards and environment-aware config

### Breaking Changes

- All endpoints now require authentication (except `/auth/*` and `/health`)
- Error response format changed to standardized ErrorResponse
- Rate limiting applied to all endpoints

## Implementation Files

### Contracts
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/shell/statemachine/AuthContract.kt`

### State Machines
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/shell/statemachine/AuthStateMachine.kt`

### Models
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/core/models/User.kt`
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/core/models/AuthToken.kt`
- `server/src/main/kotlin/com/guyghost/wakeve/models/AuthDTOs.kt`

### Repository
- `shared/src/commonMain/kotlin/com/guyghost/wakeve/auth/repository/UserRepository.kt`

### Routes (Server)
- `server/src/main/kotlin/com/guyghost/wakeve/routes/AuthRoutes.kt`
- `server/src/main/kotlin/com/guyghost/wakeve/routes/SessionRoutes.kt`

### Security
- `server/src/main/kotlin/com/guyghost/wakeve/security/SecurityConfig.kt`
- `server/src/main/kotlin/com/guyghost/wakeve/security/AuditLogger.kt`
- `server/src/main/kotlin/com/guyghost/wakeve/auth/AuthenticationService.kt`

### Tests
- `server/src/test/kotlin/com/guyghost/wakeve/AuthFlowIntegrationTest.kt`
- `shared/src/commonTest/kotlin/com/guyghost/wakeve/auth/core/logic/ValidateEmailTest.kt`

## Related Specifications

- `user-auth`: Core authentication models and interfaces
- `event-organization`: Event ownership and participation model
- `offline-sync`: Local data persistence for guest mode
- All specs: MUST include Security section referencing this spec

## Internationalization

### Security-Related Strings

| Key | English | French | Context |
|-----|---------|--------|---------|
| `auth.title` | Sign In | Connexion | Screen title |
| `auth.google` | Continue with Google | Continuer avec Google | Button label |
| `auth.apple` | Continue with Apple | Continuer avec Apple | Button label |
| `auth.guest` | Continue as Guest | Continuer en tant qu'invité | Button label |
| `auth.error.invalid_token` | Invalid authentication token | Jeton d'authentification invalide | Error message |
| `session.revoked` | Session revoked | Session révoquée | Toast message |
| `auth.rate_limited` | Too many attempts. Please try again later. | Trop de tentatives. Veuillez réessayer plus tard. | Error message |

## Performance Considerations

- JWT verification is cached per request (avoid repeated verification)
- Token blacklist check uses Redis in production (not database)
- Rate limiting uses sliding window algorithm
- Audit logging is asynchronous (non-blocking)

## Change History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-02-08 | Initial version based on security patches |

## Acceptance Criteria

- [ ] Users can authenticate via Google OAuth2
- [ ] Users can authenticate via Apple OAuth2
- [ ] JWT tokens contain all required claims (userId, email, role, permissions, sessionId)
- [ ] Expired or invalid tokens are rejected with 401 Unauthorized
- [ ] Users can only access events they organize or participate in
- [ ] Users can only access budgets for events they participate in
- [ ] Users can view their active sessions
- [ ] Users can revoke specific sessions (except current)
- [ ] Users can revoke all other sessions
- [ ] Token blacklist is checked on every authenticated request
- [ ] Blacklist check fails closed (denies access on error)
- [ ] Rate limiting is enforced on auth and API endpoints
- [ ] Metrics endpoint is protected by IP whitelist in production
- [ ] Audit logs are written for all security-relevant events
- [ ] Guest mode operates with complete data isolation
- [ ] Production environment fails to start without JWT_SECRET

## Success Metrics

- Authentication Success Rate: > 99% of valid authentication attempts succeed
- Authorization Failure Rate: < 0.1% of legitimate requests fail authorization
- Token Verification Latency: P50 < 10ms, P99 < 50ms
- Rate Limiting Effectiveness: 100% of abusive requests are throttled
- Audit Log Completeness: 100% of security events are logged
- Session Revocation Latency: < 5 seconds from API call to token rejection
- IDOR Vulnerabilities: 0 confirmed IDOR vulnerabilities

---

**Spec Version**: 1.0.0
**Last Updated**: 2026-02-08
**Status**: Active
**Maintainer**: Platform Team
