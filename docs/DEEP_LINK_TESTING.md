# Deep Link Testing Guide

Quick reference for testing deep link implementation in Wakeve.

## Android Testing

### Using ADB Command Line

```bash
# Test event detail deep link
adb shell am start -W -a android.intent.action.VIEW -d "wakeve://event/test123" com.guyghost.wakeve

# Test poll voting deep link
adb shell am start -W -a android.intent.action.VIEW -d "wakeve://poll/event456" com.guyghost.wakeve

# Test meeting detail deep link
adb shell am start -W -a android.intent.action.VIEW -d "wakeve://meeting/meet789" com.guyghost.wakeve

# Test invite deep link
adb shell am start -W -a android.intent.action.VIEW -d "wakeve://invite/token12345" com.guyghost.wakeve
```

### From Web Browser

1. Create an HTML file with deep link:
   ```html
   <a href="wakeve://event/test123">Test Event Deep Link</a>
   <a href="wakeve://poll/event456">Test Poll Deep Link</a>
   <a href="wakeve://meeting/meet789">Test Meeting Deep Link</a>
   <a href="wakeve://invite/token12345">Test Invite Deep Link</a>
   ```

2. Open the HTML file in Chrome browser
3. Tap the links to test deep linking

### From Email Client

1. Compose an email with deep link in body
2. Send to yourself
3. Tap the link from email app
4. App should open and navigate to correct screen

## iOS Testing

### Using Simulator Command Line

```bash
# Find your simulator UUID
xcrun simctl list devices

# Test event detail deep link
xcrun simctl open booted "wakeve://event/test123"

# Test poll voting deep link
xcrun simctl open booted "wakeve://poll/event456"

# Test meeting detail deep link
xcrun simctl open booted "wakeve://meeting/meet789"

# Test invite deep link
xcrun simctl open booted "wakeve://invite/token12345"
```

### From Safari (Simulator or Device)

1. Type deep link directly into Safari address bar:
   - `wakeve://event/test123`
   - `wakeve://poll/event456`
   - `wakeve://meeting/meet789`
   - `wakeve://invite/token12345`

2. Tap "Go" or press Enter
3. Tap the suggested "Open this page in 'Wakeve'" (or similar)

### From Notes App (iOS)

1. Create a new note
2. Type or paste deep link
3. Long-press the link
4. Tap "Open in Wakeve" from the context menu

## Verification Checklist

### Android

- [ ] App opens from deep link
- [ ] Navigates to correct screen (event details, poll, meeting, or invite)
- [ ] No error messages shown
- [ ] Back navigation works correctly
- [ ] Works when app is closed
- [ ] Works when app is running in background
- [ ] Works when app is running in foreground

### iOS

- [ ] App opens from deep link
- [ ] Navigates to correct screen (event details, poll, meeting, or invite)
- [ ] No error messages shown
- [ ] Back navigation works correctly
- [ ] Works when app is closed
- [ ] Works when app is running in background
- [ ] Works when app is running in foreground

## Expected Behavior

### Event Detail Deep Link
- Deep Link: `wakeve://event/{id}`
- Expected: Navigate to EventDetailScreen with eventId
- Screen: Shows event title, description, participants, and action buttons

### Poll Voting Deep Link
- Deep Link: `wakeve://poll/{eventId}`
- Expected: Navigate to PollVotingScreen with eventId
- Screen: Shows poll options, allows voting

### Meeting Detail Deep Link
- Deep Link: `wakeve://meeting/{meetingId}`
- Expected: Navigate to MeetingDetailScreen with meetingId
- Screen: Shows meeting details (platform, link, participants)

### Invite Deep Link
- Deep Link: `wakeve://invite/{token}`
- Expected: Show invite preview or prompt for authentication
- Note: This flow is not yet fully implemented

## Troubleshooting

### Android

**Deep link opens in browser instead of app:**
```bash
# Verify intent filters are correct
adb shell dumpsys package com.guyghost.wakeve | grep -A 20 "IntentFilter"
```

**App crashes on deep link:**
```bash
# Check logcat for errors
adb logcat | grep -i "deeplink\|deep.*link"
```

### iOS

**Deep link opens in Safari instead of app:**
- Verify that custom scheme is working (wakeve:// doesn't require Info.plist changes)
- For https:// links, Universal Links must be configured (not yet implemented)

**App doesn't receive deep link:**
- Check console output for "[DeepLinkService]" messages
- Verify that onOpenURL is properly configured

## Additional Resources

- Full documentation: `docs/deep-linking.md`
- Android developer guide: https://developer.android.com/training/app-links
- iOS developer guide: https://developer.apple.com/documentation/xcode/allowing_apps_and_websites_to_link_to_your_content
