# Calendar Integration Tests - Quick Reference

## Test File Location
```
composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/ui/event/CalendarIntegrationTest.kt
```

## Quick Start

### Run All Tests
```bash
./gradlew composeApp:connectedAndroidTest --tests "CalendarIntegrationTest"
```

### Run Single Test
```bash
./gradlew composeApp:connectedAndroidTest \
  --tests "CalendarIntegrationTest.addToCalendarButton_triggersCallback_onClick"
```

### Run in Android Studio
1. Right-click test file → "Run CalendarIntegrationTest"
2. Or right-click specific test method
3. Ensure emulator is running (API 24+)

## Test List (13 Total)

| # | Test Name | Purpose |
|---|-----------|---------|
| 1 | `calendarIntegrationCard_isDisplayed_whenEventIsConfirmed` | Card visible when event loaded |
| 2 | `eventDate_isDisplayed_inCardContent` | Event date displays correctly |
| 3 | `addToCalendarButton_triggersCallback_onClick` | "Ajouter" button works |
| 4 | `shareInviteButton_triggersCallback_onClick` | "Inviter" button works |
| 5 | `bothButtons_arePresent_andClickable` | Both buttons present |
| 6 | `addToCalendarButton_triggersCallback_onMultipleClicks` | Multiple clicks work |
| 7 | `calendarIntegrationCard_rendersGracefully_withoutFinalDate` | Null date handling |
| 8 | `cardIcons_areDisplayed_inCard` | Icons display correctly |
| 9 | `cardLayout_hasProperStructure_allElementsVisible` | Layout integrity |
| 10 | `modernEventDetailView_displaysCalendarActions_inConfirmedMode` | CONFIRMED integration |
| 11 | `modernEventDetailView_displaysCalendarActions_inFinalizedMode` | FINALIZED integration |
| 12 | `modernEventDetailView_doesNotDisplayCalendarCard_inDraftMode` | DRAFT exclusion |
| 13 | `buttonCallbacks_areIndependent_noUnintendedSideEffects` | No side effects |

## Component Structure

```
ModernEventDetailView (event, callbacks)
├── ConfirmedModeActions
│   └── CalendarIntegrationCard ← Tested component
│       ├── Title: "Calendrier & Invitations"
│       ├── Date Display: "Date prévue : ..."
│       ├── Button: "Ajouter" → onAddToCalendar
│       └── Button: "Inviter" → onShareInvite
└── FinalizedModeActions
    └── CalendarIntegrationCard ← Tested component
```

## Test Pattern

All tests follow Arrange-Act-Assert:

```kotlin
@Test
fun testExample() {
    // ARRANGE: Setup
    composeTestRule.setContent { /* component */ }
    
    // ACT: User interaction
    composeTestRule.onNodeWithText("Button").performClick()
    
    // ASSERT: Verify
    assert(condition) { "error message" }
}
```

## Dependencies

Added to `composeApp/build.gradle.kts`:
```gradle
implementation("androidx.compose.ui:ui-test-junit4:1.6.0")
```

## Test Data

Default test event:
- **ID:** test-event-001
- **Title:** Team Building Event
- **Status:** CONFIRMED
- **Date:** 2025-03-15T10:00:00Z
- **Timezone:** Europe/Paris

## Expected Results

```
CalendarIntegrationTest:
  ✓ 13 tests passed
  ✓ ~45 seconds total
  ✓ All assertions passed
```

## Common Issues & Solutions

| Issue | Solution |
|-------|----------|
| Tests not found | Run `./gradlew clean` first |
| Emulator crash | Increase RAM to 4GB+ |
| "Button not found" | Check exact text matching (case-sensitive) |
| Callback not invoked | Verify onClick handler in Composable |
| Tests timeout | Use `-x lint` flag to skip linting |

## Key Assertions Used

```kotlin
// Button/Text finding
composeTestRule.onNodeWithText("Ajouter").assertExists()

// Content description
composeTestRule.onNodeWithContentDescription("").assertExists()

// Callbacks
assert(callbackInvoked) { "Message" }

// Non-existence
composeTestRule.onNodeWithText("Text").assertDoesNotExist()
```

## Implementation Notes

### Callback Testing Approach
- Tests focus on **callback invocation**, not full implementation
- App.kt should implement actual calendar/share logic
- Tests verify the UI → callback contract
- Example implementation:
  ```kotlin
  onAddToCalendar = {
      // Request WRITE_CALENDAR permission
      // Call CalendarService.addToNativeCalendar(event)
  }
  ```

### Why Component-Level Testing?
✅ Fast execution  
✅ No permission dialogs  
✅ Isolated component behavior  
✅ Easier CI/CD integration  
✅ Non-flaky (no async issues)

## Files Modified

1. **Created:** `CalendarIntegrationTest.kt` (21KB, 13 tests)
2. **Created:** `README.md` (9.5KB, detailed documentation)
3. **Updated:** `composeApp/build.gradle.kts` (added test dependency)

## Running Specific Tests

```bash
# Test visibility
./gradlew composeApp:connectedAndroidTest \
  --tests "*isDisplayed*"

# Test callbacks
./gradlew composeApp:connectedAndroidTest \
  --tests "*Callback*"

# Test integration
./gradlew composeApp:connectedAndroidTest \
  --tests "*modernEventDetailView*"
```

## Documentation References

- 📖 Full Details: `composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/ui/event/README.md`
- 📊 Summary: `CALENDAR_INTEGRATION_TESTS_SUMMARY.md` (root)
- 🔧 Phase 4.6 Spec: `openspec/changes/cleanup-complete-calendar-management/specs/`

## CI/CD Integration

For continuous integration:
```bash
./gradlew composeApp:connectedAndroidTest \
  --tests "CalendarIntegrationTest" \
  -Pno-daemon \
  -x lint \
  --info
```

## Contact / Support

For test failures or questions:
1. Check emulator is running (API 24+)
2. Review test documentation in README.md
3. Check component source code alignment
4. Run with `-v` flag for verbose output

---

**Status:** ✅ Ready to run  
**Tests:** 13/13 implemented  
**Coverage:** Component + Integration  
**Framework:** Jetpack Compose UI Testing
