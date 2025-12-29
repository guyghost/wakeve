# Calendar Integration UI Tests

## Overview

This document describes the UI test suite for the Calendar Integration feature on Android, implemented as part of the **Phase 4.6** requirements of the `cleanup-complete-calendar-management` change.

## Test File

**Location:** `composeApp/src/androidInstrumentedTest/kotlin/com/guyghost/wakeve/ui/event/CalendarIntegrationTest.kt`

**Test Framework:** `androidx.compose.ui:ui-test-junit4` (Jetpack Compose UI Testing)

## Test Coverage

The test suite includes **13 comprehensive test cases** covering:

### 1. Component Visibility & Display
- **Test 1:** Calendar Integration Card displays when event is loaded
- **Test 8:** Card icons are displayed correctly
- **Test 9:** Card layout respects Material Design spacing with all elements visible

### 2. Button Interactions (Primary Flows)
- **Test 3:** "Ajouter" (Add to Calendar) button triggers `onAddToCalendar` callback
- **Test 4:** "Inviter" (Share Invite) button triggers `onShareInvite` callback
- **Test 5:** Both buttons are present and independently clickable
- **Test 6:** Multiple rapid clicks on "Ajouter" trigger multiple callbacks

### 3. Data Display
- **Test 2:** Event date is displayed correctly in readable format
- **Test 7:** Card renders gracefully even without a `finalDate` (null case)

### 4. Callback Behavior
- **Test 13:** Button callbacks are independent with no unintended side effects

### 5. Integration with ModernEventDetailView
- **Test 10:** Calendar actions display in CONFIRMED status mode
- **Test 11:** Calendar actions display in FINALIZED status mode
- **Test 12:** Calendar actions do NOT display in non-appropriate statuses (e.g., DRAFT)

## Running the Tests

### Prerequisites

Ensure you have:
- Android SDK 36 installed
- An Android emulator running (API 24+)
- Gradle 8.13.2 or later

### Command Line Execution

```bash
# Run all calendar integration tests
./gradlew composeApp:connectedAndroidTest --tests "CalendarIntegrationTest"

# Run a specific test
./gradlew composeApp:connectedAndroidTest --tests "CalendarIntegrationTest.calendarIntegrationCard_isDisplayed_whenEventIsConfirmed"

# Run with verbose output
./gradlew composeApp:connectedAndroidTest --tests "CalendarIntegrationTest" -v

# Run with debug mode
./gradlew composeApp:connectedAndroidTest --tests "CalendarIntegrationTest" -d
```

### IDE Execution (Android Studio)

1. Open `CalendarIntegrationTest.kt` in Android Studio
2. Right-click on the test class or individual test method
3. Select **Run 'CalendarIntegrationTest'** or **Run with Coverage**
4. Ensure an emulator is running before execution

## Test Structure

Each test follows the **Arrange-Act-Assert (AAA)** pattern:

```kotlin
@Test
fun exampleTest() {
    // ARRANGE: Setup test data and compose the UI
    val mockCallback = { /* ... */ }
    composeTestRule.setContent {
        CalendarIntegrationCard(
            event = testEvent,
            onAddToCalendar = mockCallback,
            onShareInvite = {}
        )
    }
    
    // ACT: Perform the action (e.g., click button)
    composeTestRule.onNodeWithText("Ajouter")
        .performClick()
    
    // ASSERT: Verify the result
    assert(callbackInvoked) { "Callback should be invoked" }
}
```

## Components Tested

### Primary Component: CalendarIntegrationCard

**Location:** `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/CalendarIntegrationCard.kt`

**Purpose:** Displays calendar integration options when an event reaches CONFIRMED or FINALIZED status.

**Key Features:**
- Displays event date in readable format
- Provides "Ajouter" (Add to Calendar) button
- Provides "Inviter" (Share Invite) button
- Displays calendar and share icons
- Uses Material Design styling

### Integration: ModernEventDetailView

**Location:** `composeApp/src/commonMain/kotlin/com/guyghost/wakeve/ui/event/ModernEventDetailView.kt`

**Purpose:** Main event detail screen that composes CalendarIntegrationCard when appropriate.

**Integration Points:**
- `ConfirmedModeActions`: Shows calendar buttons when event status is CONFIRMED
- `FinalizedModeActions`: Shows calendar buttons when event status is FINALIZED
- Passes `onAddToCalendar` and `onShareInvite` callbacks through the hierarchy

## Test Dependencies

Added to `composeApp/build.gradle.kts`:

```gradle
androidInstrumentedTest.dependencies {
    // ... existing dependencies ...
    implementation("androidx.compose.ui:ui-test-junit4:1.6.0")
}
```

## Key Test Scenarios

### Happy Path: User Adds Event to Calendar
```
Given: Event is in CONFIRMED status
When:  User clicks "Ajouter" button
Then:  onAddToCalendar callback is invoked
       (Caller can trigger CalendarService.addToNativeCalendar)
```

### Happy Path: User Shares Invitation
```
Given: Event is in CONFIRMED status
When:  User clicks "Inviter" button
Then:  onShareInvite callback is invoked
       (Caller can generate and share ICS file)
```

### Edge Case: No Final Date
```
Given: Event has finalDate = null
When:  CalendarIntegrationCard is composed
Then:  Card displays without crash
       Buttons remain functional
```

### Integration: Wrong Status
```
Given: Event is in DRAFT status
When:  ModernEventDetailView is composed
Then:  Calendar Integration Card should NOT be visible
```

## Callback Testing Approach

Rather than testing the full permission and file generation logic (which requires UI Automator and system intents), the tests focus on **component-level callback invocation**:

1. **Callback Setup:** Each test defines a mock callback that tracks invocation
2. **Button Interaction:** Simulates user clicking the button
3. **Callback Verification:** Asserts the callback was invoked exactly once

**Example:**
```kotlin
var addToCalendarInvoked = false
val mockCallback = { addToCalendarInvoked = true }

composeTestRule.setContent {
    CalendarIntegrationCard(
        event = testEvent,
        onAddToCalendar = mockCallback,
        onShareInvite = {}
    )
}

composeTestRule.onNodeWithText("Ajouter").performClick()
assert(addToCalendarInvoked) { "Callback should be invoked" }
```

**Why This Approach?**
- ✅ Isolates UI component behavior from platform-specific logic
- ✅ Tests the contract between composables
- ✅ Runs reliably without Android permission dialogs
- ✅ Fast execution (no system calls needed)
- ✅ Caller (App.kt) can provide the actual implementation

## Related Implementation Files

### Callback Implementers (in App.kt or EventDetailScreen)
- `onAddToCalendar` → Should call `CalendarService.addToNativeCalendar(event)`
- `onShareInvite` → Should generate ICS and share via `ACTION_SEND`

### Supporting Services (to be called from callbacks)
- `shared/src/androidMain/kotlin/platform/AndroidCalendarService.kt` - Calendar integration
- `shared/src/commonMain/kotlin/services/IcsGenerator.kt` - ICS file generation

## Test Results

Expected output when all tests pass:

```
com.guyghost.wakeve.ui.event.CalendarIntegrationTest > 
  calendarIntegrationCard_isDisplayed_whenEventIsConfirmed PASSED
  eventDate_isDisplayed_inCardContent PASSED
  addToCalendarButton_triggersCallback_onClick PASSED
  shareInviteButton_triggersCallback_onClick PASSED
  bothButtons_arePresent_andClickable PASSED
  addToCalendarButton_triggersCallback_onMultipleClicks PASSED
  calendarIntegrationCard_rendersGracefully_withoutFinalDate PASSED
  cardIcons_areDisplayed_inCard PASSED
  cardLayout_hasProperStructure_allElementsVisible PASSED
  modernEventDetailView_displaysCalendarActions_inConfirmedMode PASSED
  modernEventDetailView_displaysCalendarActions_inFinalizedMode PASSED
  modernEventDetailView_doesNotDisplayCalendarCard_inDraftMode PASSED
  buttonCallbacks_areIndependent_noUnintendedSideEffects PASSED

13 tests PASSED in 45.3s
```

## Debugging Tips

### If tests fail to find buttons:
1. Verify the button text exactly matches (case-sensitive)
2. Check if the Composable hierarchy has changed
3. Use `composeTestRule.onRoot().printToLog()` to see the UI tree

### If callbacks aren't invoked:
1. Verify the onClick handler is correctly passed to the Button
2. Check if state mutations are preventing callback execution
3. Ensure `performClick()` is called before assertions

### If emulator crashes:
1. Increase emulator RAM allocation (min 4GB)
2. Clear emulator data: `emulator -wipe-data`
3. Check Android Studio logcat for detailed error messages

## CI/CD Integration

To integrate with CI/CD pipelines:

```bash
# Run tests headlessly
./gradlew composeApp:connectedAndroidTest \
  --tests "CalendarIntegrationTest" \
  -Pno-daemon \
  -x lint
```

## Notes

- Tests use **`kotlin.test`** assertions for consistency with other tests in the project
- Component testing approach (vs. end-to-end) keeps tests fast and maintainable
- All tests are **deterministic** - no flakiness from async operations
- Tests follow **Phase 4.6** requirements for Calendar Integration

## Future Extensions

Potential enhancements to this test suite:

1. **Screenshot Testing** - Add UI snapshot tests to verify exact layout
2. **Accessibility Testing** - Add a11y checks using AccessibilityEventChecker
3. **Performance Testing** - Monitor compose recomposition count
4. **Integration Tests** - Full end-to-end with mock CalendarService
5. **Network Tests** - ICS generation and share intents (requires test harness)

## References

- [Jetpack Compose Testing Documentation](https://developer.android.com/jetpack/compose/testing)
- [UI Testing Best Practices](https://developer.android.com/training/testing/ui-testing)
- [Kotlin Test Framework](https://kotlinlang.org/docs/testing.html)
- [Calendar Integration Phase 4.6 Spec](../../../openspec/changes/cleanup-complete-calendar-management/specs/)
