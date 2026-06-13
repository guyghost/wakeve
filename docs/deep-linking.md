# Deep Linking Implementation

## Overview

Wakeve supports deep linking for direct navigation to specific screens and features. Deep links allow users to open the app from external sources (emails, SMS, websites) and navigate directly to relevant content.

## Supported Deep Links

### Release Review Scope

The first App Store release keeps deep-link behavior narrow and review-oriented. Rich notification expansion is deferred until the P0 App Store blockers are closed, because new notification categories, actions, entitlements, or background behaviors can change the review surface.

Priority links for the review build are:

| Review Flow | Custom Scheme | Universal Link | Purpose |
|-------------|---------------|----------------|---------|
| Event detail | `wakeve://event/{id}` | `https://wakeve.app/event/{id}` | Open an existing event from email, SMS, notification, or web. |
| Invite | `wakeve://invite/{token}` | `https://wakeve.app/invite/{token}` | Preserve an invitation token through launch and authentication. |
| Poll voting | `wakeve://poll/{eventId}` | `https://wakeve.app/poll/{eventId}` | Bring participants directly to the poll flow. |
| Legal and support | Public web URLs | `https://wakeve.app/privacy`, `https://wakeve.app/terms`, `https://wakeve.app/support` | Keep App Review, account deletion, privacy, and abuse-reporting surfaces reachable without requiring an installed app. |

Meeting links remain supported by the current parsers, but they are not part of the App Review-critical deep-link set. Scenario, organization, payment, and analytics links should not be promoted in store metadata or notifications until their contracts are covered by a change proposal and release evidence.

### Android & iOS (Custom Scheme)

| Deep Link | Description | Example |
|-----------|-------------|----------|
| `wakeve://event/{id}` | Navigate to event details | `wakeve://event/abc123` |
| `wakeve://poll/{eventId}` | Navigate to poll voting screen | `wakeve://poll/abc123` |
| `wakeve://meeting/{meetingId}` | Navigate to meeting details | `wakeve://meeting/xyz789` |
| `wakeve://invite/{token}` | Handle event invite | `wakeve://invite/token12345` |

### iOS (Universal Links)

| Deep Link | Description | Example |
|-----------|-------------|----------|
| `https://wakeve.app/event/{id}` | Navigate to event details | `https://wakeve.app/event/abc123` |
| `https://wakeve.app/poll/{eventId}` | Navigate to poll voting | `https://wakeve.app/poll/abc123` |
| `https://wakeve.app/meeting/{meetingId}` | Navigate to meeting | `https://wakeve.app/meeting/xyz789` |
| `https://wakeve.app/invite/{token}` | Handle event invite | `https://wakeve.app/invite/token12345` |

**Note:** iOS registers the `wakeve://` custom scheme in `Info.plist` and declares `applinks:wakeve.app` in `iosApp/src/Wakeve.entitlements`. The web app serves Apple App Site Association JSON from `/.well-known/apple-app-site-association` and `/apple-app-site-association` when a real `APPLE_TEAM_ID` or `TEAM_ID` is configured. The documented placeholder `ABCDE12345` and invalid Team IDs are rejected. Production still requires the real Apple Developer Team ID, a live `wakeve.app` deployment, and an App ID/provisioning profile with Associated Domains enabled.

## Notification Categories and Actions

The shared notification model currently defines these categories and default actions:

| Category | Identifier | Default Actions | Release Contract Notes |
|----------|------------|-----------------|------------------------|
| Event invite | `event_invite` | `accept`, `maybe`, `decline` | Existing contract. Any server-side direct accept/decline behavior must stay authenticated and idempotent. |
| Poll reminder | `poll_reminder` | `vote` | Existing contract. Review-flow routing should prefer opening the poll screen rather than writing a vote from the notification. |
| Meeting starting | `meeting_starting` | `join` | Existing contract, but not App Review-critical for the first submission. |
| Scenario vote | `scenario_vote` | `yes`, `no` | Existing contract, but not promoted for first-release App Review evidence. |
| General | `general` | none | Safe default for informational notifications. |

Do not add new notification categories, actions, background execution behavior, or direct-write notification actions without an OpenSpec proposal. For the first review build, prefer a general notification or an existing category that opens one of the priority review links above.

## Implementation Details

### Android

#### Files
- `DeepLinkHandler.kt` - Handles deep link parsing and navigation
- `DeepLinkStateManager.kt` - Manages pending deep links between MainActivity and Compose UI
- `AndroidManifest.xml` - Intent filters for deep link patterns

#### Architecture

**Functional Core (Pure Functions):**
- `parseDeepLink(uri: Uri): DeepLink?` - Parses URI into typed deep link object

**Imperative Shell:**
- `DeepLinkHandler` - Handles deep link navigation (side effects)
- `DeepLinkStateManager` - Singleton for bridging MainActivity and Compose UI

#### Flow

1. **App Launch via Deep Link:**
   ```
   User taps deep link
   → OS sends intent to MainActivity
   → MainActivity.handleDeepLinkFromIntent()
   → DeepLinkStateManager.updatePendingDeepLink()
   → App composable LaunchedEffect observes pendingDeepLink
   → DeepLinkHandler.handleDeepLink()
   → Navigate to appropriate screen
   ```

2. **App Running, Deep Link Received:**
   ```
   User taps deep link
   → OS sends intent to MainActivity.onNewIntent()
   → Handle deep link (same flow as above)
   ```

### iOS

#### Files
- `DeepLinkService.swift` - Handles deep link parsing and navigation
- `iOSApp.swift` - Integrates DeepLinkService with SwiftUI
- `Info.plist` - Registers the `wakeve` custom URL scheme
- `Wakeve.entitlements` - Declares `applinks:wakeve.app` for Universal Links
- `apps/landing/src/lib/server/apple-app-site-association.ts` - Builds the AASA response from Apple Team ID environment variables
- `apps/landing/src/routes/.well-known/apple-app-site-association/+server.ts` - Serves the primary AASA endpoint
- `apps/landing/src/routes/apple-app-site-association/+server.ts` - Serves the root AASA fallback endpoint

#### Architecture

**Functional Core (Pure Functions):**
- `parseDeepLink(_ url: URL) -> DeepLinkType?` - Parses URL into typed enum
- `parseUniversalLink(_ url: URL) -> DeepLinkType?` - Parses Universal Links

**Imperative Shell:**
- `DeepLinkService` - Handles deep link navigation (side effects)
- `@MainActor` - Ensures UI updates on main thread

#### Flow

1. **App Launch via Deep Link:**
   ```
   User taps deep link
   → iOS calls onOpenURL in iOSApp
   → handleDeepLink() → DeepLinkService.handleDeepLink()
   → DeepLinkService updates navigationPath
   → ContentView observes navigationPath
   → SwiftUI Navigation handles routing
   ```

2. **App Running, Deep Link Received:**
   ```
   User taps deep link
   → onOpenURL is called
   → Handle deep link (same flow as above)
   ```

## Usage Examples

### Sending Deep Links

#### From Backend (Email Notifications)

```typescript
// Example: Send poll link via email
const pollLink = `wakeve://poll/${eventId}`;
const emailBody = `Vote for the event date: ${pollLink}`;

// Example: Send event invite
const inviteLink = `wakeve://invite/${inviteToken}`;
const emailBody = `Join our event: ${inviteLink}`;
```

#### From Other Apps

```kotlin
// Android: Open Wakeve with deep link
val intent = Intent(Intent.ACTION_VIEW, Uri.parse("wakeve://event/abc123"))
startActivity(intent)
```

```swift
// iOS: Open Wakeve with deep link
if let url = URL(string: "wakeve://event/abc123") {
    UIApplication.shared.open(url)
}
```

## Testing Deep Links

### Android (ADB)

```bash
# Test event detail
adb shell am start -W -a android.intent.action.VIEW -d "wakeve://event/test123" com.guyghost.wakeve

# Test poll voting
adb shell am start -W -a android.intent.action.VIEW -d "wakeve://poll/event456" com.guyghost.wakeve

# Test meeting detail
adb shell am start -W -a android.intent.action.VIEW -d "wakeve://meeting/meet789" com.guyghost.wakeve

# Test invite
adb shell am start -W -a android.intent.action.VIEW -d "wakeve://invite/token12345" com.guyghost.wakeve
```

### iOS (Simulator)

```bash
# Open deep link in Simulator
xcrun simctl open booted "wakeve://event/test123"

# Or paste in Safari and tap the link
```

### iOS (Device)

1. Send deep link to yourself via email or SMS
2. Tap the link on your iOS device
3. App should open and navigate to the correct screen

## Security Considerations

1. **Deep Link Validation:**
   - Always validate deep link parameters before navigation
   - Check if event/meeting IDs exist before navigating
   - Handle invalid or expired invite tokens gracefully

2. **Authentication:**
   - Some deep links may require authentication
   - Currently, deep links navigate directly (guest mode supported)
   - Future: Prompt for authentication if required

3. **Universal Links (iOS):**
   - Require proper Apple App Site Association (AASA) JSON
   - Must be hosted on both `https://wakeve.app/.well-known/apple-app-site-association` and `https://wakeve.app/apple-app-site-association`
   - The repository provides both AASA endpoints, but production must set the real Apple Team ID and serve them over the live domain

## Future Enhancements

1. **Invite Flow:**
   - Implement invite token validation
   - Show event preview to non-authenticated users
   - Prompt for authentication before accepting invite

2. **Deep Link Analytics:**
   - Track deep link origins (email, SMS, web)
   - Measure deep link conversion rates
   - Optimize notification content

## Troubleshooting

### Android

**Deep link doesn't open app:**
- Check AndroidManifest intent filters match the deep link pattern
- Verify the scheme is exactly `wakeve` (lowercase)
- Clear app data and try again

**App opens but doesn't navigate:**
- Check DeepLinkStateManager is properly connected
- Verify LaunchedEffect is observing `pendingDeepLink`
- Check logs for deep link parsing errors

### iOS

**Deep link opens in Safari instead of app:**
- Verify custom scheme is registered in Info.plist
- For https:// links, verify `applinks:wakeve.app` is present in the signed app entitlements
- Verify `APPLE_TEAM_ID` or `TEAM_ID` is configured in production with the real Apple Developer Team ID, not the documented placeholder, so the AASA file contains the real `<Team ID>.com.guyghost.wakeve` app ID
- Verify both `https://wakeve.app/.well-known/apple-app-site-association` and `https://wakeve.app/apple-app-site-association` respond with `200` and `application/json`
- Check that onOpenURL is properly set up

**App opens but doesn't navigate:**
- Check DeepLinkService is properly initialized
- Verify navigationPath is being observed in ContentView
- Check logs for parsing errors

## References

- [Android Deep Linking Guide](https://developer.android.com/training/app-links)
- [iOS Universal Links Guide](https://developer.apple.com/documentation/xcode/allowing_apps_and_websites_to_link_to_your_content)
- [SwiftUI onOpenURL](https://developer.apple.com/documentation/swiftui/view/onopenurl(perform:))
