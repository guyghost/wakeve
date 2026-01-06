# iOS Calendar Integration UI Tests

## 📋 Overview

This guide explains how to run UI tests for the **Calendar Integration** features on iOS. These tests verify the `CalendarIntegrationCard` component and its interactive buttons using **XCTest UITests framework**.

### Test File Location
```
iosApp/iosAppUITests/CalendarIntegrationUITests.swift
```

### Total Tests: 10 ✅

---

## 🎯 Test Coverage

| # | Test Name | Category | Purpose |
|---|-----------|----------|---------|
| 1 | `testCalendarCardVisibility` | Rendering | Verify Calendar card appears in event detail |
| 2 | `testAddToCalendarButtonVisibility` | UI Components | Verify "Add to Calendar" button exists and is enabled |
| 3 | `testShareInvitationButtonVisibility` | UI Components | Verify "Share Invitation" button exists and is enabled |
| 4 | `testAddToCalendarButtonInteraction` | Interaction | Simulate user tap and verify loading state |
| 5 | `testShareInvitationButtonInteraction` | Interaction | Simulate user tap and verify share sheet/loading |
| 6 | `testAddToCalendarButtonDisabledDuringAction` | State Management | Verify button disabled during operation |
| 7 | `testShareInvitationButtonDisabledDuringAction` | State Management | Verify mutual exclusion of button actions |
| 8 | `testCalendarStatusIconPresence` | Visual Feedback | Verify status icon updates correctly |
| 9 | `testCalendarCardAccessibilityAfterScroll` | Navigation | Verify card accessibility after scrolling |
| 10 | `testButtonTextAndIconsAccuracy` | Content Verification | Verify all UI labels and icons are correct |

---

## 🚀 Running Tests in Xcode

### Method 1: Run All Tests (Recommended)

1. **Open Xcode**
   ```bash
   open iosApp/iosApp.xcodeproj
   ```

2. **Select the Test Target**
   - Top-left: Product Scheme → Select `iosApp`
   - Select destination: **iPhone 15 Pro Simulator** (or any available device)

3. **Run Tests**
   - Press **`Cmd + U`** to run all tests
   - Or: **Product** → **Test** from menu

4. **Monitor Progress**
   - Xcode opens Test Navigator on the left
   - Watch the simulator run through each test
   - Green checkmarks = PASS, Red X = FAIL

### Method 2: Run Specific Test

1. **Open Test Navigator**
   - **Cmd + 6** or View → Navigators → Show Test Navigator

2. **Find Test**
   - Expand `CalendarIntegrationUITests`
   - Click on specific test (e.g., `testCalendarCardVisibility`)

3. **Run Test**
   - Click the **▶️ Run** button next to test name
   - Or press **Ctrl+Click** → Run

### Method 3: Run from Command Line

```bash
# Navigate to project root
cd /Users/guy/Developer/dev/wakeve

# Run all UI tests
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro'

# Run specific test
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
  -testOnly CalendarIntegrationUITests/testCalendarCardVisibility
```

---

## 📊 Test Execution Flow

```
1. App Launch
   ↓
2. Navigate to Event Detail
   ↓
3. Verify Calendar Card Visible ✓
   ↓
4. Test Add to Calendar Button
   ├─ Verify visibility
   ├─ Verify enabled state
   ├─ Simulate tap
   ├─ Verify loading state
   └─ Verify disabled during operation
   ↓
5. Test Share Invitation Button
   ├─ Verify visibility
   ├─ Verify enabled state
   ├─ Simulate tap
   ├─ Verify share sheet or loading
   └─ Verify disabled during operation
   ↓
6. Verify UI Elements
   ├─ Icons accuracy
   ├─ Text labels
   └─ Status updates
   ↓
7. App Cleanup
```

---

## 🧪 Test Scenarios

### Scenario 1: Calendar Card Visibility
**GIVEN:** Event detail view is displayed  
**WHEN:** View loads with calendar card  
**THEN:** Calendar section shows status and buttons

```swift
func testCalendarCardVisibility() throws {
    // ARRANGE: Navigate to event
    navigateToEventDetail()
    
    // ACT: Look for calendar card
    let calendarCard = app.staticTexts["Calendar"]
    let exists = calendarCard.waitForExistence(timeout: 3.0)
    
    // ASSERT: Card must exist
    XCTAssertTrue(exists, "Calendar card should be visible")
}
```

### Scenario 2: Add to Calendar Interaction
**GIVEN:** "Add to Calendar" button is visible  
**WHEN:** User taps the button  
**THEN:** Loading state appears, button becomes disabled

```swift
func testAddToCalendarButtonInteraction() throws {
    // ARRANGE: Navigate and find button
    navigateToEventDetail()
    let addButton = app.buttons["Add to Calendar"]
    
    // ACT: Tap button
    addButton.tap()
    
    // ASSERT: Verify loading state
    let progressView = app.progressIndicators.firstMatch
    XCTAssertTrue(progressView.waitForExistence(timeout: 2.0))
}
```

### Scenario 3: Share Invitation Interaction
**GIVEN:** "Share Invitation" button is visible  
**WHEN:** User taps the button  
**THEN:** Share sheet appears or loading indicator shows

```swift
func testShareInvitationButtonInteraction() throws {
    // ARRANGE
    navigateToEventDetail()
    let shareButton = app.buttons["Share Invitation"]
    
    // ACT: Tap button
    shareButton.tap()
    
    // ASSERT: Share sheet or loading appears
    let shareSheet = app.sheets.firstMatch
    XCTAssertTrue(shareSheet.waitForExistence(timeout: 3.0))
}
```

---

## ✅ Expected Results

When all tests pass, you should see:

```
CalendarIntegrationUITests.swift
✅ testCalendarCardVisibility
✅ testAddToCalendarButtonVisibility
✅ testShareInvitationButtonVisibility
✅ testAddToCalendarButtonInteraction
✅ testShareInvitationButtonInteraction
✅ testAddToCalendarButtonDisabledDuringAction
✅ testShareInvitationButtonDisabledDuringAction
✅ testCalendarStatusIconPresence
✅ testCalendarCardAccessibilityAfterScroll
✅ testButtonTextAndIconsAccuracy

10 of 10 tests passed
```

---

## 🐛 Troubleshooting

### Issue: "No event found to navigate to detail view"

**Cause:** No events available in the test environment  
**Solution:**
1. Seed test data before running tests
2. Create a mock event in `setUpWithError()`
3. Use app launch arguments to pre-populate data

```swift
override func setUpWithError() throws {
    try super.setUpWithError()
    app = XCUIApplication()
    app.launchArguments = ["--test-mode", "--seed-events"]
    app.launch()
}
```

### Issue: "Calendar card not visible after timeout"

**Cause:** View hierarchy not properly structured  
**Solution:**
1. Check ModernEventDetailView includes CalendarIntegrationCard
2. Increase timeout: `waitForExistence(timeout: 5.0)`
3. Scroll view to reveal hidden content

```swift
let scrollView = app.scrollViews.firstMatch
scrollView.swipeUp() // Scroll to reveal card
```

### Issue: "Button tap doesn't trigger action"

**Cause:** Element not hittable or view not responsive  
**Solution:**
1. Verify button `isHittable` before tapping
2. Use `tap()` instead of other interaction methods
3. Add delay between tap and assertion

```swift
if addButton.isHittable {
    addButton.tap()
    Thread.sleep(forTimeInterval: 0.5)
}
```

### Issue: Tests timeout on slower devices

**Solution:** Increase timeout values for older simulators
```swift
let exists = calendarCard.waitForExistence(timeout: 5.0) // Instead of 3.0
```

---

## 📱 Device Support

### Recommended Simulators
- ✅ iPhone 15 Pro (Latest)
- ✅ iPhone 14 Pro
- ✅ iPhone 13 Pro
- ⚠️ iPhone SE (may need longer timeouts)

### Physical Devices
- ✅ iOS 14.0+
- ✅ iPhone 12 and later recommended

---

## 🏗️ Test Architecture

### AAA Pattern (Arrange/Act/Assert)

All tests follow the **AAA pattern** for clarity:

```swift
func testExample() {
    // ARRANGE (Given)
    // - Set up initial state
    // - Navigate to view
    // - Find UI elements
    
    // ACT (When)
    // - Perform user action
    // - Simulate interaction
    // - Trigger async operations
    
    // ASSERT (Then)
    // - Verify expected outcome
    // - Check state changes
    // - Validate UI updates
}
```

### Helper Methods

- `navigateToEventDetail()` - Navigate to event detail view
- `isElementVisible(_ element:)` - Check if element is visible
- `waitForElementVisibility(_ element:timeout:)` - Wait for visibility

---

## 🔍 Debugging Tests

### View the App During Test

1. **Slow Down Execution**
   ```swift
   // In setUpWithError()
   app.launchEnvironment["com.apple.CoreAnimation.FPS"] = "1"
   ```

2. **Print Debug Info**
   ```swift
   print(app.debugDescription) // Print entire hierarchy
   ```

3. **Take Screenshots**
   ```swift
   let screenshot = XCUIScreen.main.screenshot()
   // Check Xcode's attachment panel for images
   ```

### XCTest Debugging Commands

```bash
# List all elements in view hierarchy
po app.debugDescription

# Find specific element
po app.buttons["Add to Calendar"].debugDescription

# Check element properties
po app.buttons["Add to Calendar"].isEnabled
po app.buttons["Add to Calendar"].isHittable
```

---

## 📚 Reference: CalendarIntegrationCard Structure

The component being tested:

```swift
struct CalendarIntegrationCard: View {
    let event: Event
    let userId: String
    let onAddToCalendar: () -> Void
    let onShareInvitation: () -> Void
    
    // Status Section
    // - Icon: calendar.badge.plus
    // - Title: "Calendar"
    // - Status: "Not in calendar" / "Added to calendar" / "Loading" / "Error"
    
    // Action Buttons
    // - Button 1: "Add to Calendar" (Blue background)
    // - Button 2: "Share Invitation" (Blue outline)
    
    // State Management
    @State private var calendarStatus: CalendarStatus
    @State private var isAddingToCalendar: Bool
    @State private var isShareInvitationLoading: Bool
}
```

---

## 🎓 Learning Resources

### XCTest Documentation
- [Apple XCTest Framework](https://developer.apple.com/documentation/xctest)
- [XCUITest Best Practices](https://developer.apple.com/videos/play/wwdc2020/10220/)
- [Accessibility Testing](https://developer.apple.com/videos/play/wwdc2021/10119/)

### Related Files
- `CalendarIntegrationCard.swift` - View being tested
- `AddToCalendarButton.swift` - Component used in card
- `ModernEventDetailView.swift` - Parent view
- `TEST_DOCUMENTATION.md` - General testing guide

---

## ⚙️ Configuration

### Xcode Test Settings

**Location:** Product → Scheme → Edit Scheme → Test

- **Language & Region:** English (US)
- **Execution Time Allowance:** 120 seconds per test
- **Code Coverage:** Enable for test target

### Environment Variables

```swift
app.launchEnvironment = [
    "IS_TESTING": "true",
    "MOCK_DATA": "true",
    "ANIMATION_DISABLED": "false"
]
```

---

## 📋 Checklist: Before Running Tests

- [ ] Xcode is up to date (14.0+)
- [ ] iOS deployment target is 14.0+
- [ ] Simulator is selected and ready
- [ ] Test target includes UITests
- [ ] CalendarIntegrationCard is in ModernEventDetailView
- [ ] No pending code changes that break compilation
- [ ] App can be launched without errors

---

## 🚢 CI/CD Integration

### GitHub Actions Example

```yaml
name: UI Tests

on: [push, pull_request]

jobs:
  ui-tests:
    runs-on: macos-latest
    steps:
      - uses: actions/checkout@v2
      
      - name: Run UI Tests
        run: |
          xcodebuild test \
            -project iosApp/iosApp.xcodeproj \
            -scheme iosApp \
            -destination 'platform=iOS Simulator,name=iPhone 15 Pro'
```

---

## 📞 Support & Issues

If tests fail:

1. **Check Test Output**
   - Xcode Navigator → Test Results
   - Look for specific assertion failures

2. **Review Logs**
   - Console output shows XCTest activity
   - Search for "FAILED" or "Error"

3. **Verify Setup**
   - Ensure app launches successfully
   - Check simulator is available
   - Verify event data exists

4. **Create Debug Build**
   ```bash
   xcodebuild -scheme iosApp -configuration Debug
   ```

---

## 📝 Test Maintenance

### Updating Tests

When CalendarIntegrationCard changes:

1. **Update Element IDs**
   ```swift
   // If button text changes from "Add to Calendar" to "Save to Calendar"
   let button = app.buttons["Save to Calendar"] // Updated
   ```

2. **Update Accessibility Labels**
   ```swift
   // In CalendarIntegrationCard.swift
   Button(action: handleAddToCalendar) {
       // ...
   }
   .accessibilityLabel("Add event to calendar") // Ensure consistency
   ```

3. **Verify Timeout Values**
   - Increase if animations are slower
   - Decrease for faster devices

---

## 🎉 Success Criteria

All tests pass if:

✅ Calendar card is visible in event detail  
✅ Add to Calendar button is present and clickable  
✅ Share Invitation button is present and clickable  
✅ Loading states appear correctly  
✅ Buttons disable during operations  
✅ Icons and text display correctly  
✅ App handles user interaction properly  
✅ No crashes or exceptions occur  

---

**Last Updated:** December 28, 2025  
**Framework:** XCTest / XCUITest  
**Minimum iOS:** 14.0  
**Xcode Version:** 14.0+

For more information, see:
- `TESTING_GUIDE.md` - General iOS testing guide
- `CALENDAR_INTEGRATION_GUIDE.md` - Calendar integration feature guide
- `TEST_CONFIGURATION.md` - Test environment setup
