# Authentication API Endpoints

This document describes the REST API endpoints for authentication in Wakeve.

## Base URL

```
https://api.wakeve.com/api/auth
```

## Endpoints

### OAuth Authentication

#### POST /api/auth/google
Authenticate user with Google Sign-In.

**Request:**
```json
{
  "idToken": "string",
  "email": "user@gmail.com",
  "name": "John Doe"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| idToken | String | Yes | Google ID token from mobile SDK |
| email | String | Yes | User's email address |
| name | String | No | User's display name |

**Response (200 OK):**
```json
{
  "user": {
    "id": "user_123",
    "email": "user@gmail.com",
    "name": "John Doe",
    "isGuest": false,
    "authMethod": "GOOGLE"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresInSeconds": 3600
}
```

**Errors:**
- 400 Bad Request: Invalid token format
- 401 Unauthorized: Invalid or expired token

---

#### POST /api/auth/apple
Authenticate user with Apple Sign-In.

**Request:**
```json
{
  "idToken": "string",
  "email": "user@icloud.com",
  "name": "John Doe"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| idToken | String | Yes | Apple identity token |
| email | String | No | Email (may be null with private relay) |
| name | String | No | Real name (only first sign-in) |

**Response (200 OK):** Same as Google endpoint

**Errors:** Same as Google endpoint

---

### Email Authentication

#### POST /api/auth/email/request
Send OTP (One-Time Password) to email address.

**Request:**
```json
{
  "email": "user@example.com"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| email | String | Yes | Valid email address |

**Response (200 OK):**
```json
{
  "success": true,
  "message": "OTP sent successfully",
  "expiresInSeconds": 300
}
```

**Errors:**
- 400 Bad Request: Invalid email format
- 500 Internal Server Error: Failed to send OTP

---

#### POST /api/auth/email/verify
Verify OTP and authenticate user.

**Request:**
```json
{
  "email": "user@example.com",
  "otp": "123456"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| email | String | Yes | Email address |
| otp | String | Yes | 6-digit OTP code |

**Response (200 OK):**
```json
{
  "user": {
    "id": "email_user_123",
    "email": "user@example.com",
    "name": null,
    "isGuest": false,
    "authMethod": "EMAIL"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresInSeconds": 3600
}
```

**Errors:**
- 400 Bad Request: Invalid OTP format
- 401 Unauthorized: Invalid or expired OTP
- 429 Too Many Requests: Too many attempts

---

### Guest Session

#### POST /api/auth/guest
Create a guest session for offline use.

**Request:**
```json
{
  "deviceId": "guest_device_123"
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| deviceId | String | No | Unique device identifier |

**Response (200 OK):**
```json
{
  "user": {
    "id": "guest_123",
    "email": null,
    "name": null,
    "isGuest": true,
    "authMethod": "GUEST"
  },
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresInSeconds": 604800
}
```

**Notes:**
- Guest tokens have 7-day expiry
- Guest users cannot sync data to backend
- All data is stored locally only

---

### Token Management

#### POST /api/auth/refresh
Refresh an expired access token.

**Request:**
```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

| Field | Type | Required | Description |
|-------|------|----------|-------------|
| refreshToken | String | Yes | Valid refresh token |

**Response (200 OK):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
  "expiresInSeconds": 3600
}
```

**Errors:**
- 400 Bad Request: Invalid request format
- 401 Unauthorized: Invalid or expired refresh token

---

### Utility Endpoints

#### GET /api/auth/google/url
Get Google OAuth authorization URL.

**Response (200 OK):**
```json
{
  "url": "https://accounts.google.com/o/oauth2/v2/auth?..."
}
```

---

#### GET /api/auth/apple/url
Get Apple Sign-In authorization URL.

**Response (200 OK):**
```json
{
  "url": "https://appleid.apple.com/auth/authorize?..."
}
```

---

## Error Response Format

All errors return JSON with the following format:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
  "details": "Additional details (optional)"
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| INVALID_TOKEN | 401 | OAuth token validation failed |
| INVALID_EMAIL | 400 | Email format is invalid |
| INVALID_OTP | 401 | OTP code is incorrect |
| OTP_EXPIRED | 401 | OTP has expired |
| TOO_MANY_ATTEMPTS | 429 | Too many verification attempts |
| AUTH_FAILED | 401 | Authentication failed |
| INTERNAL_ERROR | 500 | Server error |

---

## Security Considerations

1. **Token Storage:**
   - Access tokens: Short-lived (1 hour)
   - Refresh tokens: Medium-lived (7 days)
   - Guest tokens: Long-lived (7 days), local-only

2. **GDPR Compliance:**
   - Only necessary data is stored
   - Users can request data deletion
   - Guest data never leaves the device

3. **Rate Limiting:**
   - OTP requests: 5 per hour per email
   - OTP verification: 10 per hour per email

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2025-01-08 | Initial release |
