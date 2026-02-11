# Deep Linking Implementation

## Overview

Wakeve supports deep linking for direct navigation to specific screens and features. Deep links allow users to open the app from external sources (emails, SMS, websites) and navigate directly to relevant content.

## Supported Deep Links

### Android & iOS (Custom Scheme)

| Deep Link | Description | Example |
|-----------|-------------|----------|
| `wakeve://event/{id}` | Navigate to event details | `wakeve://event/abc123` |
| `wakeve://poll/{eventId}` | Navigate to poll voting screen | `wakeve://poll/abc123` |
| `wakeve://meeting/{meetingId}` | Navigate to meeting details | `wakeve://meeting/xyz789` |
| `wakeve://invite/{token}` | Handle event invite | `wakeve://invite/token12345` |

### iOS (Universal Links - Planned)

| Deep Link | Description | Example |
|-----------|-------------|----------|
| `https://wakeve.app/event/{id}` | Navigate to event details | `https://wakeve.app/event/abc123` |
| `https://wakeve.app/poll/{eventId}` | Navigate to poll voting | `https://wakeve.app/poll/abc123` |
| `https://wakeve.app/meeting/{meetingId}` | Navigate to meeting | `https://wakeve.app/meeting/xyz789` |
| `https://wakeve.app/invite/{token}` | Handle event invite | `https://wakeve.app/invite/token12345` |

**Note:** Universal Links are planned for future implementation. Currently, only the `wakeve://` custom scheme is supported on iOS.

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
   - Require proper Apple App Site Association (AASA) file
   - Must be hosted on `https://wakeve.app/.well-known/apple-app-site-association`
   - Not yet implemented

## Future Enhancements

1. **Universal Links (iOS):**
   - Implement AASA file on server
   - Configure Associated Domains in Xcode
   - Support https://wakeve.app/* URLs

2. **Invite Flow:**
   - Implement invite token validation
   - Show event preview to non-authenticated users
   - Prompt for authentication before accepting invite

3. **Deep Link Analytics:**
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
- Verify custom scheme is registered in Info.plist (not needed for wakeve://)
- For https:// links, Universal Links must be configured
- Check that onOpenURL is properly set up

**App opens but doesn't navigate:**
- Check DeepLinkService is properly initialized
- Verify navigationPath is being observed in ContentView
- Check logs for parsing errors

## References

- [Android Deep Linking Guide](https://developer.android.com/training/app-links)
- [iOS Universal Links Guide](https://developer.apple.com/documentation/xcode/allowing_apps_and_websites_to_link_to_your_content)
- [SwiftUI onOpenURL](https://developer.apple.com/documentation/swiftui/view/onopenurl(perform:))
