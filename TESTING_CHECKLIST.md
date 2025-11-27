# Wakeve OAuth Authentication - Manual Testing Checklist

This checklist covers all critical paths for the OAuth authentication system implementation.

## ✅ Success Criteria

Before marking a section as complete, ensure all items pass.

---

## 📱 Platform: Android

### OAuth Login Flow
- [ ] **Google Sign-In button appears** on login screen
- [ ] **Tapping Google button** launches Google account picker
- [ ] **Selecting account** returns to app with authenticated state
- [ ] **User profile** displays correct name and email
- [ ] **Canceling OAuth flow** returns to login screen with no error
- [ ] **Network error** during login shows user-friendly error message
- [ ] **Invalid credentials** show appropriate error

### Token Management
- [ ] **Access token** is stored securely in EncryptedSharedPreferences
- [ ] **Token expiry** is correctly calculated and stored
- [ ] **Refresh token** is stored separately and securely
- [ ] **App restart** with valid token maintains authenticated state
- [ ] **App restart** with expired token triggers re-authentication

### Session Management
- [ ] **Settings screen** shows current session with "Current" badge
- [ ] **Multiple devices** appear in session list with correct details
- [ ] **Device names** are human-readable (e.g., "Pixel 6", "Samsung Galaxy S23")
- [ ] **IP addresses** are displayed correctly
- [ ] **Last accessed** timestamps update appropriately
- [ ] **Revoking a session** removes it from list immediately
- [ ] **"Revoke All Others"** keeps only current session
- [ ] **Cannot revoke current session** shows appropriate message

### Background Token Refresh
- [ ] **WorkManager job** is scheduled on login
- [ ] **Token refresh** happens automatically before expiry (check logs)
- [ ] **App in background** still refreshes tokens (test with 15+ min wait)
- [ ] **Network unavailable** retries refresh with exponential backoff
- [ ] **Refresh failure** logs out user and shows login screen

### Logout Flow
- [ ] **Logout button** clears all tokens from storage
- [ ] **After logout** returns to login screen
- [ ] **After logout** WorkManager job is cancelled
- [ ] **After logout** re-opening app shows login screen
- [ ] **After logout** old tokens are blacklisted (cannot be reused)

### UI/UX
- [ ] **Loading states** show circular progress indicator
- [ ] **Error messages** are clear and actionable
- [ ] **Network errors** have retry buttons
- [ ] **Animations** are smooth during transitions
- [ ] **Back button** from main app returns to login (after logout)

---

## 🍎 Platform: iOS

### OAuth Login Flow
- [ ] **Apple Sign-In button** appears on login screen
- [ ] **Tapping button** launches native Apple Sign-In sheet
- [ ] **Face ID/Touch ID** authentication works
- [ ] **Selecting account** returns to app authenticated
- [ ] **"Hide My Email"** option works correctly
- [ ] **Canceling flow** returns to login with no error
- [ ] **Network error** shows user-friendly message

### Token Management
- [ ] **Tokens stored** in iOS Keychain
- [ ] **Keychain items** have correct access control (kSecAttrAccessibleAfterFirstUnlock)
- [ ] **App restart** with valid token maintains auth
- [ ] **App uninstall/reinstall** clears tokens properly
- [ ] **iCloud Keychain sync** does NOT sync auth tokens (security)

### Session Management
- [ ] **Settings screen** shows sessions correctly
- [ ] **SwiftUI UI** matches design (Material Design 3 equivalent)
- [ ] **Device names** show iOS device model correctly
- [ ] **Revoke session** works smoothly
- [ ] **Pull to refresh** updates session list

### Background Token Refresh
- [ ] **BGTaskScheduler** is registered correctly (check console logs)
- [ ] **Background task** runs when app is terminated
- [ ] **Simulated background refresh** works (use Xcode debugger)
- [ ] **Failed refresh** doesn't crash app
- [ ] **Multiple refresh attempts** use exponential backoff

### Logout Flow
- [ ] **Logout** clears Keychain items
- [ ] **Background tasks** are cancelled
- [ ] **App returns** to login screen
- [ ] **Tokens blacklisted** on server

### UI/UX
- [ ] **Loading indicators** use native iOS style
- [ ] **Error alerts** use native iOS alerts
- [ ] **Haptic feedback** on important actions (if implemented)
- [ ] **Dark mode** support works correctly
- [ ] **Safe area insets** are respected

---

## 💻 Platform: JVM/Desktop

### OAuth Login Flow
- [ ] **Google Sign-In button** appears
- [ ] **Browser OAuth flow** launches in default browser
- [ ] **OAuth callback** returns to app correctly
- [ ] **Localhost callback** server starts/stops cleanly
- [ ] **Port conflicts** are handled gracefully
- [ ] **Multiple OAuth attempts** don't leave zombie processes

### Token Management
- [ ] **Tokens stored** in OS-appropriate secure storage
- [ ] **macOS Keychain** integration works (if on macOS)
- [ ] **Linux secret service** works (if on Linux)
- [ ] **Windows Credential Manager** works (if on Windows)
- [ ] **App restart** maintains authentication

### Session Management
- [ ] **Settings window** shows session list
- [ ] **Desktop window controls** work (minimize, maximize, close)
- [ ] **Multi-window support** (if applicable) maintains state
- [ ] **Keyboard shortcuts** work (Cmd+W to close, etc.)

### Logout Flow
- [ ] **Logout clears** secure storage
- [ ] **Window closes** and returns to login
- [ ] **Re-opening app** shows login screen

### UI/UX
- [ ] **Compose Desktop UI** renders correctly
- [ ] **Window sizing** is appropriate
- [ ] **Fonts and icons** render clearly
- [ ] **Loading states** are visible
- [ ] **Error dialogs** are modal and clear

---

## 🌐 Server API

### Authentication Endpoints

#### POST /auth/google
- [ ] **Valid auth code** returns JWT token
- [ ] **Invalid auth code** returns 401 Unauthorized
- [ ] **Network error** to Google returns 503 Service Unavailable
- [ ] **Duplicate request** (same code twice) returns error
- [ ] **Response includes** accessToken, refreshToken, expiresIn, user profile

#### POST /auth/apple
- [ ] **Valid identity token** returns JWT
- [ ] **Invalid token** returns 401
- [ ] **Token signature verification** works correctly
- [ ] **User profile** parsed from token correctly

#### POST /auth/refresh
- [ ] **Valid refresh token** returns new access token
- [ ] **Expired refresh token** returns 401
- [ ] **Blacklisted token** returns 401
- [ ] **New tokens have** updated expiry time
- [ ] **Old access token** is blacklisted after refresh

#### POST /auth/logout
- [ ] **Valid token** logs out successfully
- [ ] **Token added** to blacklist immediately
- [ ] **Subsequent requests** with old token fail with 401
- [ ] **All user sessions** can be revoked with flag

### Session Management Endpoints

#### GET /api/sessions
- [ ] **Authenticated request** returns user's sessions
- [ ] **No auth token** returns 401
- [ ] **Blacklisted token** returns 401
- [ ] **Response includes** all session details (device, IP, timestamps)
- [ ] **Current session** is marked with `isCurrent: true`

#### DELETE /api/sessions/{sessionId}
- [ ] **Valid session ID** revokes session
- [ ] **Current session** cannot be revoked (returns 400)
- [ ] **Non-existent session** returns 404
- [ ] **Session from another user** returns 404
- [ ] **Token blacklisted** after revocation

#### POST /api/sessions/revoke-all-others
- [ ] **All other sessions** are revoked
- [ ] **Current session** remains active
- [ ] **Response includes** count of revoked sessions
- [ ] **All tokens blacklisted** except current

#### GET /api/sessions/current
- [ ] **Returns current** session details
- [ ] **Includes device** fingerprint info
- [ ] **Last accessed** timestamp is recent

### JWT Middleware
- [ ] **Valid JWT** allows API access
- [ ] **Expired JWT** returns 401 with clear message
- [ ] **Malformed JWT** returns 401
- [ ] **Missing JWT** returns 401
- [ ] **Blacklisted JWT** returns 401 with "token_revoked" error
- [ ] **JWT claims** (userId, sessionId) are correctly extracted

### Rate Limiting
- [ ] **Auth endpoints** are rate limited (10 requests/minute)
- [ ] **Excessive requests** return 429 Too Many Requests
- [ ] **Rate limit headers** are present (X-RateLimit-Remaining, etc.)
- [ ] **Different users** have separate rate limits

### Metrics & Monitoring
- [ ] **GET /metrics** returns Prometheus-format metrics
- [ ] **auth.login.success** counter increments on successful login
- [ ] **auth.login.failure** counter increments on failed login
- [ ] **auth.session.active** gauge shows correct session count
- [ ] **auth.token.refresh.success** increments on token refresh
- [ ] **auth.token.blacklisted** increments when tokens are revoked
- [ ] **JVM metrics** (memory, threads) are exposed

---

## 🔒 Security Testing

### Token Security
- [ ] **JWT secret** is NOT in source code (uses environment variable)
- [ ] **Tokens use HS256** algorithm (or stronger)
- [ ] **JWT includes** expiry claim
- [ ] **Refresh tokens** are single-use (or rotated)
- [ ] **Token hashes** stored in database (not plaintext)

### Session Security
- [ ] **Session hijacking prevention**: IP address validation
- [ ] **Device fingerprinting** detects device changes
- [ ] **Concurrent session limit** enforced (if configured)
- [ ] **Inactive sessions** expire after period
- [ ] **Old sessions** auto-cleanup (30+ days)

### API Security
- [ ] **HTTPS enforced** in production (HTTP redirects to HTTPS)
- [ ] **CORS configured** correctly (not wide-open)
- [ ] **SQL injection** protected (parameterized queries)
- [ ] **XSS protection** (proper content-type headers)
- [ ] **CSRF protection** (state parameter in OAuth)

### Data Privacy
- [ ] **User passwords** are NEVER stored (OAuth only)
- [ ] **Refresh tokens** encrypted at rest
- [ ] **Sensitive logs** don't include tokens
- [ ] **Database backups** are encrypted
- [ ] **GDPR compliance**: User can delete all data

---

## 🔄 End-to-End Flows

### First-Time User
1. [ ] Opens app → sees login screen
2. [ ] Taps OAuth button → browser/system auth appears
3. [ ] Completes auth → returns to app
4. [ ] Sees main app screen immediately
5. [ ] User profile displays correctly
6. [ ] Can access all features

### Returning User (Valid Token)
1. [ ] Opens app → sees loading screen briefly
2. [ ] Automatically logged in
3. [ ] Lands on main app screen
4. [ ] No re-authentication required

### Returning User (Expired Token)
1. [ ] Opens app → sees loading screen
2. [ ] Token refresh happens automatically
3. [ ] Lands on main app screen
4. [ ] OR if refresh fails → sees login screen

### Multi-Device Login
1. [ ] Logs in on Device A → session created
2. [ ] Logs in on Device B → second session created
3. [ ] Both devices work independently
4. [ ] Device A sees Device B in session list
5. [ ] Device A revokes Device B → Device B logs out
6. [ ] Device B shows "Session expired" message

### Token Expiry During Use
1. [ ] User is actively using app
2. [ ] Token expires during session
3. [ ] Next API call triggers auto-refresh
4. [ ] User doesn't notice interruption
5. [ ] If refresh fails → graceful logout

### Network Interruption
1. [ ] App is offline → network indicator shows
2. [ ] User attempts login → error message appears
3. [ ] Network restored → retry succeeds
4. [ ] Token refresh during offline → retries when online

---

## 📊 Performance Testing

### Load Testing
- [ ] **100 concurrent logins** complete successfully
- [ ] **1000 session list requests** return in <500ms
- [ ] **Token refresh** under load doesn't fail
- [ ] **Database** handles 10,000+ sessions efficiently
- [ ] **Metrics endpoint** responds in <100ms

### Memory Testing
- [ ] **Android app** doesn't leak memory on login/logout cycles
- [ ] **iOS app** memory usage is stable
- [ ] **JVM app** heap usage is reasonable
- [ ] **Server** memory doesn't grow unbounded
- [ ] **Background workers** don't accumulate

### Battery Testing (Mobile)
- [ ] **Background token refresh** doesn't drain battery excessively
- [ ] **WorkManager** respects battery optimization
- [ ] **iOS background tasks** don't cause battery drain

---

## 🐛 Error Scenarios

### Network Errors
- [ ] **No internet** → clear error message
- [ ] **Slow network** → loading indicator persists
- [ ] **Timeout** → retry option appears
- [ ] **Server down** → user-friendly message

### Invalid States
- [ ] **Corrupted token** → logout and re-login
- [ ] **Missing database** → app recreates schema
- [ ] **Conflicting sessions** → newer wins
- [ ] **Race conditions** → proper locking/transactions

### Edge Cases
- [ ] **System time change** doesn't break token validation
- [ ] **App killed during login** → restarts cleanly
- [ ] **Quick app switching** maintains state
- [ ] **Low storage** → error handling
- [ ] **Revoked Google/Apple account** → graceful logout

---

## 📝 Final Verification

### Code Quality
- [ ] **Unit tests pass** (all platforms)
- [ ] **Integration tests pass** (server)
- [ ] **No compiler warnings** (Kotlin, Swift)
- [ ] **Code review completed**
- [ ] **Documentation updated**

### Deployment Readiness
- [ ] **Environment variables** documented
- [ ] **Database migrations** tested
- [ ] **Rollback plan** exists
- [ ] **Monitoring configured** (Prometheus/Grafana)
- [ ] **Logging configured** (structured logs)

### User Experience
- [ ] **Error messages** are user-friendly (no stack traces)
- [ ] **Loading times** are acceptable (<2s for login)
- [ ] **UI is responsive** on all platforms
- [ ] **Accessibility** features work (screen readers, etc.)
- [ ] **Help documentation** exists for users

---

## ✅ Sign-Off

Once ALL items are checked and verified:

- [ ] **Android Lead** approves
- [ ] **iOS Lead** approves
- [ ] **Backend Lead** approves
- [ ] **QA Lead** approves
- [ ] **Security Review** passed
- [ ] **Product Owner** approves

**Release Version:** _________

**Date:** _________

**Tested By:** _________

---

## 📞 Support Contacts

If issues found during testing:

- **Backend Issues:** Backend team channel
- **Android Issues:** Android team channel
- **iOS Issues:** iOS team channel
- **Critical Security Issues:** Security team (private channel)

---

**Note:** This checklist should be executed in a staging environment first, then repeated in production with a canary deployment.
