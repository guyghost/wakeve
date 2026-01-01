# DraftEventWizardTests Documentation

## 📋 Overview

Comprehensive XCTest suite for Enhanced DRAFT Phase components on iOS.

**File Location**: `iosApp/iosApp/Tests/DraftEventWizardTests.swift`  
**Total Tests**: 14  
**Frameworks**: XCTest, SwiftUI  
**Target**: iOS 16+

---

## 🎯 Test Coverage

### Component Breakdown

| Component | Tests | Coverage |
|-----------|-------|----------|
| **EventTypePicker** | 3 | Type selection, custom type, validation |
| **ParticipantsEstimationCard** | 3 | Input, validation, range checking |
| **PotentialLocationsList** | 3 | Add, remove, empty state |
| **DraftEventWizardView** | 5 | Navigation, workflow, auto-save, persistence |
| **Total** | **14** | Multi-step wizard interaction |

---

## 🧪 Test Details

### EventTypePicker Tests (3)

#### Test 1: Select Preset Type
```
GIVEN:  EventTypePicker with no selection
WHEN:   User selects BIRTHDAY
THEN:   selectedType = BIRTHDAY
AND:    No custom text field shown
```

**What it validates:**
- Type selection binding works
- Menu selection updates state
- Predefined types don't require custom text

**User Journey:**
1. User taps Event Type menu
2. User selects "Birthday Party"
3. Menu closes with selection

---

#### Test 2: Select Custom Type with Text
```
GIVEN:  EventTypePicker with CUSTOM selected
WHEN:   User enters "Hackathon"
THEN:   selectedType = CUSTOM
AND:    customTypeValue = "Hackathon"
AND:    Custom TextField is visible
```

**What it validates:**
- Custom type selection
- TextField appears conditionally
- Custom text binding works
- State changes propagate

**User Journey:**
1. User taps Event Type menu
2. User selects "Custom"
3. TextField appears
4. User types "Hackathon"
5. TextField captures text

---

#### Test 3: Custom Type Without Text Validation
```
GIVEN:  EventTypePicker with CUSTOM selected
WHEN:   User leaves customText empty
THEN:   Error message displays
AND:    "Custom event type is required"
AND:    Red border on TextField
```

**What it validates:**
- Validation logic for custom type
- Error message display
- Visual error feedback (border color)
- Prevents invalid submission

**User Journey:**
1. User selects CUSTOM
2. TextField appears empty
3. Error message shows immediately
4. Red border indicates error

---

### ParticipantsEstimationCard Tests (3)

#### Test 4: Update All Three Fields
```
GIVEN:  ParticipantsEstimationCard (empty)
WHEN:   User enters min=15, max=25, expected=20
THEN:   All fields updated
AND:    Helper text shows "~20 people"
AND:    No validation errors
```

**What it validates:**
- All three fields update independently
- Binding works bidirectionally
- Helper text updates dynamically
- Valid ranges are accepted

**User Journey:**
1. User taps Minimum field
2. User enters "15"
3. User taps Maximum field
4. User enters "25"
5. User taps Expected field
6. User enters "20"
7. Helper text updates

---

#### Test 5: Validation Max < Min
```
GIVEN:  ParticipantsEstimationCard
WHEN:   User enters min=30, max=20
THEN:   Error message displays
AND:    "Maximum must be ≥ minimum"
AND:    Max field has red border
```

**What it validates:**
- Range validation logic
- Error message specificity
- Real-time validation feedback
- Invalid state prevention

**User Journey:**
1. User enters min=30
2. User enters max=20
3. Error appears immediately
4. Red border on max field
5. Cannot proceed with form

---

#### Test 6: Only Expected Field
```
GIVEN:  ParticipantsEstimationCard (empty)
WHEN:   User enters only expected=10
THEN:   expected=10
AND:    min and max remain nil
AND:    No range errors
```

**What it validates:**
- Optional field support
- Independent field updates
- No validation between independent fields
- Partial form completion

**User Journey:**
1. User skips Minimum field
2. User skips Maximum field
3. User enters Expected=10
4. Form validates successfully

---

### PotentialLocationsList Tests (3)

#### Test 7: Add Location
```
GIVEN:  PotentialLocationsList (empty)
WHEN:   User taps "Add" and enters "Paris" (CITY)
THEN:   Location appended to list
AND:    Count badge shows "1"
AND:    Empty state hidden
```

**What it validates:**
- Add button functionality
- Location append to array
- Badge count updates
- Empty state transition

**User Journey:**
1. User sees empty state message
2. User taps "Add" button
3. Location input sheet appears
4. User enters "Paris"
5. User selects "City" type
6. User confirms
7. Location appears in list

---

#### Test 8: Remove Location
```
GIVEN:  PotentialLocationsList with 2 locations
WHEN:   User taps delete on first location
THEN:   Location removed
AND:    Count badge updates to "1"
AND:    List animates removal
```

**What it validates:**
- Delete button functionality
- Location removal from array
- Badge count updates
- Animation triggers

**User Journey:**
1. User sees 2 locations (Paris, London)
2. User taps delete on Paris
3. Location animates out
4. Badge updates from "2" to "1"

---

#### Test 9: Empty List Shows Message
```
GIVEN:  PotentialLocationsList (empty)
THEN:   Empty state message visible
AND:    Empty state icon visible
AND:    "Add" button visible and enabled
AND:    List section hidden
```

**What it validates:**
- Empty state rendering
- Message content
- CTA button visibility
- Conditional layout

**User Journey:**
1. User opens locations section (empty)
2. User sees "No locations yet" message
3. User sees empty state icon
4. User sees enabled "Add" button

---

### DraftEventWizardView Tests (5)

#### Test 10: Step 1 to Step 2 Navigation
```
GIVEN:  DraftEventWizardView at Step 1
WHEN:   User fills title, description, type and taps "Next"
THEN:   Navigation to Step 2
AND:    All Step 1 data saved
AND:    Progress bar shows 2/4
AND:    Step indicator shows "Step 2 of 4"
```

**What it validates:**
- Step validation logic
- Navigation on valid step
- Progress bar updates
- Step indicator updates
- Data persistence

**User Journey:**
1. User is at Step 1 (Basic Info)
2. User enters title="Team Building"
3. User enters description="Annual event"
4. User selects type=TEAM_BUILDING
5. User taps "Next"
6. Progress bar updates to 50%
7. Step indicator changes to "Step 2 of 4: Participants"

---

#### Test 11: Step 4 Validation (No Time Slots)
```
GIVEN:  DraftEventWizardView at Step 4
WHEN:   User taps "Create Event" without time slots
THEN:   Validation error shows
AND:    "At least one time slot is required"
AND:    "Create Event" button disabled
AND:    No navigation
```

**What it validates:**
- Step 4 validation
- Empty time slots handling
- Error message display
- Button disabled state

**User Journey:**
1. User is at Step 4 (Time Slots)
2. User skips adding time slots
3. User taps "Create Event"
4. Error message appears
5. Button remains disabled

---

#### Test 12: Complete Workflow
```
GIVEN:  DraftEventWizardView at Step 1
WHEN:   User completes all 4 steps:
        1. title="Test", desc="Test", type=TEAM_BUILDING
        2. expected=20
        3. Add "Paris" (CITY)
        4. Add time slot
THEN:   Event created successfully
AND:    onComplete callback called
AND:    Event contains all data
```

**What it validates:**
- Full multi-step workflow
- All data preservation
- Event creation
- Callback invocation
- Data integrity end-to-end

**User Journey:**
1. Step 1: Fill basic info
2. Step 2: Set expected participants
3. Step 3: Add location
4. Step 4: Add time slot
5. Confirm
6. Event created with all data

---

#### Test 13: Auto-Save on Step Change
```
GIVEN:  DraftEventWizardView at Step 1 with title="Test"
WHEN:   User navigates to Step 2
THEN:   Draft auto-saved
AND:    title="Test" preserved
AND:    onSaveStep called
AND:    No data lost
```

**What it validates:**
- Auto-save functionality
- Data persistence
- Callback invocation timing
- State integrity

**User Journey:**
1. User at Step 1, title="Test Event"
2. User taps "Next"
3. Behind scenes: onSaveStep called
4. Draft saved to repository
5. Navigate to Step 2
6. Data preserved across steps

---

#### Test 14: Back Navigation Preserves Data
```
GIVEN:  DraftEventWizardView at Step 3 with location="Paris"
WHEN:   User taps "Previous" to Step 2
THEN:   location="Paris" preserved
AND:    Step 2 data also preserved
AND:    Can navigate "Next" back to Step 3
AND:    All data consistent
```

**What it validates:**
- Back navigation
- Data preservation (backward)
- Bidirectional navigation
- State consistency

**User Journey:**
1. User at Step 3 with "Paris" location
2. User taps "Previous"
3. Navigate to Step 2
4. Step 2 data intact
5. User taps "Next"
6. Back to Step 3
7. "Paris" location still there

---

## 🏃 Running the Tests

### In Xcode

```bash
# Run all tests
Cmd + U

# Run specific test class
Cmd + Shift + U (select test class)

# Run single test
Click diamond icon next to test method name
```

### From Command Line

```bash
# Run all DraftEventWizardTests
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -testPlan DraftEventWizardTests

# Run specific test
xcodebuild test \
  -project iosApp/iosApp.xcodeproj \
  -testName DraftEventWizardTests/testEventTypePicker_SelectPresetType
```

---

## 📊 Test Patterns

### AAA Pattern (Arrange/Act/Assert)

All tests follow the standard AAA pattern:

```swift
func testExample() {
    // ARRANGE: Set up initial state
    @State var selectedType: Shared.EventType = .other
    
    // ACT: Perform the action
    selectedType = .birthday
    
    // ASSERT: Verify the result
    XCTAssertEqual(selectedType, .birthday)
}
```

### Using XCTContext.runActivity

Tests use `XCTContext.runActivity` for better organization:

```swift
XCTContext.runActivity(named: "Action description") { _ in
    // ARRANGE
    // ACT
    // ASSERT
}
```

This provides better test reporting and grouping in Xcode.

---

## 🔧 Helper Methods

### createMockLocation

Creates a test `PotentialLocation_` with specified parameters:

```swift
let location = createMockLocation(
    name: "Paris",
    type: .city,
    address: "France"
)
```

### createMockTimeSlot

Creates a test `TimeSlot` for tomorrow:

```swift
let timeSlot = createMockTimeSlot()
// Returns TimeSlot starting tomorrow at all day
```

### EventType.displayName Extension

Provides display names for all event types (mirrors Android):

```swift
eventType.displayName // "Birthday Party", "Team Building", etc.
```

---

## ✅ Validation Rules

### EventTypePicker
- ✅ Predefined types don't require custom text
- ❌ Custom type requires non-empty text
- ✅ Type changes are animated
- ✅ Menu selection updates binding

### ParticipantsEstimationCard
- ✅ All fields are optional
- ✅ Max must be ≥ Min (if both set)
- ✅ All values must be > 0 (if set)
- ✅ Expected can be outside range (warning only)

### PotentialLocationsList
- ✅ Empty state shows when 0 locations
- ✅ Add button appends to list
- ✅ Delete button removes from list
- ✅ Count badge updates on add/remove
- ✅ List animates changes

### DraftEventWizardView
- ✅ Step 1: title + description + (type OR custom type)
- ✅ Step 2: optional (min, max, expected)
- ✅ Step 3: optional (locations)
- ✅ Step 4: required (min 1 time slot)
- ✅ Back navigation preserves all data
- ✅ Auto-save on forward navigation

---

## 🐛 Known Limitations

1. **SwiftUI Testing**: Tests use state bindings directly rather than UI interaction simulation
   - Real UI tests would use `XCUIApplication`
   - These tests validate state logic and bindings

2. **Shared Module**: Relies on Kotlin multiplatform module
   - Requires full project build context
   - Cannot run tests in isolation

3. **Animation Testing**: Cannot verify animation durations/curves
   - Only verify that state changes occur

4. **Sheet Presentation**: Cannot test `@State var showLocationSheet`
   - Only validate the trigger conditions

---

## 📝 Adding New Tests

### Template

```swift
/// Test description in natural language
///
/// GIVEN: Initial state
/// WHEN: Action taken
/// THEN: Expected result
/// AND: Additional assertion
///
/// Validates:
/// - What the test checks
/// - Feature aspect
func testComponentName_ScenarioDescription() {
    XCTContext.runActivity(named: "Human-readable activity name") { _ in
        // ARRANGE
        
        // ACT
        
        // ASSERT
        XCTAssertEqual(actual, expected)
    }
}
```

### Checklist

- [ ] Clear test name (verb_subject_scenario)
- [ ] Documentation with GIVEN/WHEN/THEN
- [ ] AAA pattern (Arrange/Act/Assert)
- [ ] XCTContext.runActivity for organization
- [ ] Multiple assertions for comprehensive validation
- [ ] Comment explaining what's validated

---

## 🎓 Learning Resources

### XCTest Documentation
- [Apple XCTest Documentation](https://developer.apple.com/documentation/xctest)
- [Writing Tests in Swift](https://developer.apple.com/swift/)

### SwiftUI Testing
- [Testing SwiftUI Apps](https://developer.apple.com/documentation/swiftui)
- [ViewInspector Library](https://github.com/nalexn/ViewInspector) for advanced UI testing

### Conventions
- [Conventional Commits](https://www.conventionalcommits.org/)
- [AAA Pattern](https://www.thinktocode.com/2018/06/13/the-aaa-pattern/)

---

## 📞 Support

For questions about:
- **Test implementation**: See inline comments
- **Component functionality**: Check `.swift` files in `iosApp/Components/`
- **iOS architecture**: Read `ARCHITECTURE.md`
- **OpenSpec details**: Check `openspec/changes/enhance-draft-phase/`

---

**Last Updated**: 2025-01-01  
**Total Test Cases**: 14  
**Expected Pass Rate**: 100% (if components are implemented)
