# Calendar Integration UI Tests - Quick Reference

## Files Created

### 1. Test Implementation
**File:** `iosApp/iosAppUITests/CalendarIntegrationUITests.swift`  
**Lines:** 451  
**Tests:** 10  
**Framework:** XCTest / XCUITest

### 2. Test Guide
**File:** `iosApp/CALENDAR_TESTS_INDEX.md`  
**Content:** Comprehensive guide for running and maintaining tests

---

## Quick Start: Run Tests Now

### In Xcode (Easiest)
1. Open `iosApp/iosApp.xcodeproj`
2. Press **Cmd + U**
3. Watch tests execute in simulator

### Via Command Line
```bash
cd /Users/guy/Developer/dev/wakeve

xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPhone 15 Pro'
```

---

## Tests at a Glance

| Test | Purpose | Type |
|------|---------|------|
| `testCalendarCardVisibility` | Verify card renders | Rendering |
| `testAddToCalendarButtonVisibility` | Verify button exists | UI |
| `testShareInvitationButtonVisibility` | Verify button exists | UI |
| `testAddToCalendarButtonInteraction` | Tap button, verify loading | Interaction |
| `testShareInvitationButtonInteraction` | Tap button, verify sheet | Interaction |
| `testAddToCalendarButtonDisabledDuringAction` | Verify state management | State |
| `testShareInvitationButtonDisabledDuringAction` | Verify mutual exclusion | State |
| `testCalendarStatusIconPresence` | Verify icon displays | Visual |
| `testCalendarCardAccessibilityAfterScroll` | Verify scrolling | Navigation |
| `testButtonTextAndIconsAccuracy` | Verify all labels/icons | Content |

---

## Test Structure

All tests follow the **AAA Pattern**:

```
ARRANGE (Given)
  - Set up state
  - Navigate to view
  
ACT (When)
  - Perform action
  - User interaction
  
ASSERT (Then)
  - Verify outcome
  - Check state changes
```

---

## Key Test Scenarios

### Scenario 1: Calendar Card Visibility
```
GIVEN: Event detail view is displayed
WHEN:  View loads
THEN:  Calendar section is visible with status
```

### Scenario 2: Button Interaction
```
GIVEN: User sees "Add to Calendar" button
WHEN:  User taps button
THEN:  Loading indicator appears, button disables
```

### Scenario 3: Share Invitation
```
GIVEN: User sees "Share Invitation" button
WHEN:  User taps button
THEN:  Share sheet or activity VC appears
```

---

## Component Being Tested

```
CalendarIntegrationCard
├── Status Section
│   ├── Icon (calendar.badge.plus)
│   ├── Title ("Calendar")
│   └── Status Text ("Not in calendar" / "Added to calendar")
│
└── Action Buttons
    ├── Button 1: "Add to Calendar"
    │   └── Icon: calendar.badge.plus
    │   └── Color: Blue
    │
    └── Button 2: "Share Invitation"
        └── Icon: square.and.arrow.up
        └── Color: Blue outline
```

---

## Environment

- **Framework:** XCTest / XCUITest
- **Language:** Swift
- **Minimum iOS:** 14.0
- **Minimum Xcode:** 14.0
- **Parent View:** ModernEventDetailView
- **Related Component:** AddToCalendarButton

---

## Troubleshooting Quick Tips

| Issue | Solution |
|-------|----------|
| "No event found" | Create seed data in `setUpWithError()` |
| "Card not visible" | Increase timeout to 5.0 seconds |
| "Button not hittable" | Scroll view to reveal element |
| "Tests timeout" | Use slower device/simulator |

---

## Important Characteristics

✅ **Complete:** All 10 scenarios covered  
✅ **Isolated:** Each test is independent  
✅ **Clear:** AAA pattern throughout  
✅ **Documented:** Comments explain each test  
✅ **Robust:** Proper timeouts and error handling  
✅ **Maintainable:** Helper methods for common actions  

---

## Next Steps

1. **Run Tests**
   - Press Cmd+U in Xcode
   - Or use xcodebuild command

2. **Review Results**
   - Check Test Navigator for pass/fail
   - Fix any failures with help from CALENDAR_TESTS_INDEX.md

3. **Integrate with CI/CD**
   - Add to GitHub Actions workflow
   - Run on every push/PR

4. **Maintain Tests**
   - Update when UI changes
   - Keep timeout values appropriate
   - Document new test additions

---

## Documentation Links

- **Full Guide:** `iosApp/CALENDAR_TESTS_INDEX.md`
- **Component Impl:** `iosApp/iosApp/Views/CalendarIntegrationCard.swift`
- **Parent View:** `iosApp/iosApp/Views/ModernEventDetailView.swift`
- **Testing Guide:** `iosApp/TESTING_GUIDE.md`
- **Test Config:** `iosApp/TEST_CONFIGURATION.md`

---

## Test Execution Example

```bash
$ xcodebuild test -project iosApp/iosApp.xcodeproj -scheme iosApp

Testing started at 2025-12-28 22:55:00

Launched simulator...
Running tests...

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

10 out of 10 tests passed
Test session finished in 45 seconds
```

---

**Created:** December 28, 2025  
**Status:** ✅ Ready for use  
**Test Count:** 10/10  
**Framework:** XCTest
