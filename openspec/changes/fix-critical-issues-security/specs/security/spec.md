## ADDED Requirements

### Requirement: JWT Secret Configuration
The system MUST require JWT_SECRET environment variable without fallback.

#### Scenario: Missing JWT secret
- **GIVEN** the server starts without JWT_SECRET environment variable
- **WHEN** the application initializes JWT configuration
- **THEN** an IllegalStateException is thrown with message "JWT_SECRET environment variable required"

#### Scenario: Valid JWT secret
- **GIVEN** JWT_SECRET environment variable is set to a secure random string
- **WHEN** the server starts
- **THEN** JWT tokens can be generated and verified successfully

### Requirement: Metrics Endpoint Security
The system MUST restrict metrics endpoint access to authorized IPs only.

#### Scenario: Blocked external IP
- **GIVEN** a request to /metrics from IP 192.168.1.100 (not in whitelist)
- **WHEN** the request is processed
- **THEN** return 403 Forbidden

#### Scenario: Allowed internal IP
- **GIVEN** a request to /metrics from IP 127.0.0.1 (in whitelist)
- **WHEN** the request is processed
- **THEN** return 200 OK with metrics

### Requirement: Non-blocking JWT Validation
The system MUST validate JWT tokens without blocking coroutine threads.

#### Scenario: Concurrent token validation
- **GIVEN** 100 concurrent requests with JWT tokens
- **WHEN** tokens are validated
- **THEN** all validations complete without thread blocking
- **AND** response time < 50ms per validation

### Requirement: JWT Blacklist Cache
The system MUST cache JWT blacklist checks for performance.

#### Scenario: Cached blacklist check
- **GIVEN** a token is checked against blacklist
- **WHEN** the same token is checked again within 5 minutes
- **THEN** the cached result is returned without database query

## MODIFIED Requirements

### Requirement: User Creation
The system SHALL create users with explicit timestamp parameters.

#### Scenario: Create guest user
- **GIVEN** a request to create a guest user
- **WHEN** User.createGuest is called with currentTime parameter
- **THEN** a guest user is created with createdAt and lastLoginAt set to currentTime

#### Scenario: Create authenticated user
- **GIVEN** a request to create an authenticated user
- **WHEN** User.createAuthenticated is called with id, email, and currentTime
- **THEN** an authenticated user is created with specified timestamps

### Requirement: OTP Generation
The system SHALL generate OTPs through the Shell layer.

#### Scenario: Generate OTP
- **GIVEN** EmailAuthService with injected Random
- **WHEN** generateOTP is called
- **THEN** a 6-digit OTP is generated using the injected Random

## REMOVED Requirements

### Requirement: Core-level Non-deterministic Functions
**Reason**: Violates FC&IS architecture principles
**Migration**: Move to Shell layer with dependency injection
