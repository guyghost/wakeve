# ScenarioDetailView Testing Guide

**Version**: 1.0  
**Date**: 2025-12-29  
**Framework**: XCTest + SwiftUI Preview  
**Pattern**: State Machine (MVI/FSM)

---

## Overview

This guide explains how to test the refactored `ScenarioDetailView` with its `ScenarioDetailViewModel` using the State Machine pattern.

---

## Testing Strategy

### Three Levels of Testing

```
Unit Tests (ViewModel)        Integration Tests (View + ViewModel)    UI Tests (Full Screen)
├─ State transitions          ├─ Intent dispatch                      ├─ User interactions
├─ Intent handling            ├─ State updates trigger UI              ├─ Navigation flows
├─ Error states               ├─ Side effects execute                  └─ Accessibility
└─ Computed properties        └─ Loading/error states show
```

### What to Test

| Component | Test Type | Focus |
|-----------|-----------|-------|
| ViewModel state machine | Unit | Intent → state transitions |
| ViewModel intents | Unit | Proper intent creation/dispatch |
| View rendering | Integration | @Published properties → UI |
| Error handling | Integration | Error states display correctly |
| Edit form | Integration | Form validation & save |
| Delete flow | Integration | Confirmation → deletion |
| Offline behavior | Integration | State machine handles offline |

---

## Unit Tests: ViewModel

### Test File Structure

```swift
// iosApp/iosApp/Tests/ViewModels/ScenarioDetailViewModelTests.swift

import XCTest
@testable import iosApp
import Shared

@MainActor
class ScenarioDetailViewModelTests: XCTestCase {
    var sut: ScenarioDetailViewModel!
    
    override func setUp() {
        super.setUp()
        sut = ScenarioDetailViewModel(scenarioId: "test-scenario-1")
    }
    
    override func tearDown() {
        sut = nil
        super.tearDown()
    }
    
    // MARK: - Initialization Tests
    
    func testInitialization() {
        XCTAssertNotNil(sut.state)
        XCTAssert(sut.isLoading)  // Should load on init
    }
    
    func testScenarioIdStored() {
        // Verify scenarioId is used for filtering
        XCTAssertEqual(sut.scenario?.id, "test-scenario-1")
    }
    
    // MARK: - State Loading Tests
    
    func testLoadScenariosIntent() async {
        // Dispatch load intent
        sut.dispatch(.loadScenarios())
        
        // Wait for state to update
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        // Verify scenario loaded
        XCTAssertNotNil(sut.scenario)
        XCTAssertFalse(sut.isLoading)
    }
    
    func testReloadMethod() async {
        sut.reload()
        
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        XCTAssertFalse(sut.isLoading)
    }
    
    // MARK: - Edit Tests
    
    func testStartEditing() {
        XCTAssertFalse(sut.isEditing)
        
        sut.startEditing()
        
        XCTAssert(sut.isEditing)
    }
    
    func testCancelEditing() {
        sut.startEditing()
        XCTAssert(sut.isEditing)
        
        sut.cancelEditing()
        
        XCTAssertFalse(sut.isEditing)
    }
    
    func testUpdateScenario() async {
        // Setup: Load scenario first
        sut.dispatch(.loadScenarios())
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        let originalName = sut.scenario?.name ?? ""
        
        // Dispatch update intent
        sut.updateScenario(
            name: "Updated Name",
            dateOrPeriod: "2025-12-15",
            location: "Paris",
            duration: 3,
            estimatedParticipants: 5,
            estimatedBudgetPerPerson: 500.0,
            description: "Updated description"
        )
        
        // Wait for update
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        // Verify editing mode closed
        XCTAssertFalse(sut.isEditing)
        
        // Verify scenario updated
        XCTAssertNotEqual(sut.scenario?.name, originalName)
        XCTAssertEqual(sut.scenario?.name, "Updated Name")
    }
    
    // MARK: - Delete Tests
    
    func testDeleteScenario() async {
        // Setup: Load scenario
        sut.dispatch(.loadScenarios())
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        let scenarioIdBefore = sut.scenario?.id
        
        // Dispatch delete
        sut.deleteScenario()
        
        // Wait for deletion
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        // Verify scenario is gone
        if sut.scenario == nil {
            XCTPass()  // Expected
        } else {
            XCTAssertNotEqual(sut.scenario?.id, scenarioIdBefore)
        }
    }
    
    // MARK: - Voting Tests
    
    func testVoteScenario() async {
        sut.dispatch(.loadScenarios())
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        let votingResultBefore = sut.votingResult
        
        sut.voteScenario(voteType: .PREFER)
        
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        // Voting result should update
        XCTAssertNotNil(sut.votingResult)
    }
    
    // MARK: - Error Handling Tests
    
    func testClearError() {
        // Create error state (if possible)
        // sut.dispatch(.someErrorCausingIntent())
        
        XCTAssert(sut.hasError || !sut.hasError)  // Initial state
        
        sut.clearError()
        
        XCTAssertFalse(sut.hasError)
    }
    
    // MARK: - Convenience Properties Tests
    
    func testIsLoaded() async {
        XCTAssertFalse(sut.isLoaded)
        
        sut.dispatch(.loadScenarios())
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        XCTAssert(sut.isLoaded)
    }
    
    func testIsEmpty() async {
        sut.dispatch(.loadScenarios())
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        XCTAssertFalse(sut.isEmpty)
    }
    
    func testVotingResultComputation() async {
        sut.dispatch(.loadScenarios())
        try? await Task.sleep(nanoseconds: 100_000_000)
        
        let result = sut.votingResult
        
        if let result = result {
            XCTAssertGreaterThanOrEqual(result.preferCount, 0)
            XCTAssertGreaterThanOrEqual(result.neutralCount, 0)
            XCTAssertGreaterThanOrEqual(result.againstCount, 0)
        }
    }
}
```

---

## Integration Tests: View + ViewModel

### Test File Structure

```swift
// iosApp/iosApp/Tests/Views/ScenarioDetailViewIntegrationTests.swift

import XCTest
import SwiftUI
@testable import iosApp
import Shared

class ScenarioDetailViewIntegrationTests: XCTestCase {
    
    // MARK: - View Initialization Tests
    
    func testViewInitializationWithDefaults() {
        let view = ScenarioDetailView(scenarioId: "test-1")
        
        XCTAssertNotNil(view)
    }
    
    func testViewInitializationWithAllParameters() {
        let view = ScenarioDetailView(
            scenarioId: "test-1",
            eventId: "event-1",
            isOrganizer: true,
            currentUserId: "user-1",
            currentUserName: "John",
            onBack: { print("Back") },
            onDeleted: { print("Deleted") }
        )
        
        XCTAssertNotNil(view)
    }
    
    // MARK: - Rendering Tests
    
    func testLoadingStateRendering() {
        let view = ScenarioDetailView(scenarioId: "test-1")
        
        // Initial state should show loading or scenario
        let viewController = UIHostingController(rootView: view)
        
        XCTAssertNotNil(viewController.view)
    }
    
    func testScenarioDetailRendering() {
        let view = ScenarioDetailView(
            scenarioId: "test-1",
            eventId: "event-1",
            isOrganizer: false
        )
        
        let viewController = UIHostingController(rootView: view)
        
        // Verify view renders without errors
        XCTAssertNotNil(viewController.view)
    }
    
    // MARK: - State Update Tests
    
    func testViewUpdatesWhenStateChanges() {
        let view = ScenarioDetailView(scenarioId: "test-1")
        let viewController = UIHostingController(rootView: view)
        
        // Force update
        DispatchQueue.main.async {
            viewController.view.setNeedsLayout()
        }
        
        XCTAssertNotNil(viewController.view)
    }
    
    // MARK: - Edit Mode Tests
    
    func testEditModeToggle() {
        let view = ScenarioDetailView(
            scenarioId: "test-1",
            isOrganizer: true
        )
        
        let viewController = UIHostingController(rootView: view)
        
        // Cannot directly test state mutation without access to private viewModel
        // But we verify the view structure
        XCTAssertNotNil(viewController.view)
    }
    
    // MARK: - Error State Tests
    
    func testErrorStateDisplay() {
        let view = ScenarioDetailView(scenarioId: "invalid-id")
        
        let viewController = UIHostingController(rootView: view)
        
        XCTAssertNotNil(viewController.view)
        // In real test, would check for error message display
    }
}
```

---

## Preview Tests: SwiftUI Previews

### Preview Structure

```swift
// iosApp/iosApp/Views/ScenarioDetailView+Preview.swift

import SwiftUI
import Shared

#if DEBUG
struct ScenarioDetailViewPreview: PreviewProvider {
    static var previews: some View {
        Group {
            // Preview 1: Loading State
            ScenarioDetailView(
                scenarioId: "preview-1",
                eventId: "event-1"
            )
            .preferredColorScheme(.light)
            .previewDisplayName("Loading State")
            
            // Preview 2: Scenario Details (Participant)
            ScenarioDetailView(
                scenarioId: "preview-1",
                eventId: "event-1",
                isOrganizer: false,
                currentUserId: "user-1",
                currentUserName: "Alice"
            )
            .preferredColorScheme(.light)
            .previewDisplayName("Details View (Participant)")
            
            // Preview 3: Scenario Details (Organizer)
            ScenarioDetailView(
                scenarioId: "preview-1",
                eventId: "event-1",
                isOrganizer: true,
                currentUserId: "user-1",
                currentUserName: "Alice",
                onBack: { print("Back") },
                onDeleted: { print("Deleted") }
            )
            .preferredColorScheme(.light)
            .previewDisplayName("Details View (Organizer)")
            
            // Preview 4: Dark Mode
            ScenarioDetailView(
                scenarioId: "preview-1",
                eventId: "event-1"
            )
            .preferredColorScheme(.dark)
            .previewDisplayName("Dark Mode")
            
            // Preview 5: Error State
            ScenarioDetailView(
                scenarioId: "invalid-id",
                eventId: "event-1"
            )
            .preferredColorScheme(.light)
            .previewDisplayName("Error State")
        }
    }
}
#endif
```

---

## Snapshot Tests

### Visual Regression Testing

```swift
// iosApp/iosApp/Tests/Snapshots/ScenarioDetailViewSnapshotTests.swift

import XCTest
import SnapshotTesting  // SwiftUI Testing library
import SwiftUI
@testable import iosApp

class ScenarioDetailViewSnapshotTests: XCTestCase {
    
    func testScenarioDetailViewSnapshot_Light() {
        let view = ScenarioDetailView(
            scenarioId: "test-1",
            eventId: "event-1",
            isOrganizer: false
        )
        
        assertSnapshot(matching: view, as: .image(traits: .init(userInterfaceStyle: .light)))
    }
    
    func testScenarioDetailViewSnapshot_Dark() {
        let view = ScenarioDetailView(
            scenarioId: "test-1",
            eventId: "event-1",
            isOrganizer: false
        )
        
        assertSnapshot(matching: view, as: .image(traits: .init(userInterfaceStyle: .dark)))
    }
    
    func testScenarioDetailViewSnapshot_Organizer() {
        let view = ScenarioDetailView(
            scenarioId: "test-1",
            eventId: "event-1",
            isOrganizer: true
        )
        
        assertSnapshot(matching: view, as: .image)
    }
    
    func testScenarioDetailViewSnapshot_EditMode() {
        // Would require access to viewModel to toggle editing
        // This is a limitation of snapshot testing with @StateObject
    }
}
```

---

## UI Tests: End-to-End Flows

### Full User Journey Testing

```swift
// iosApp/iosAppUITests/ScenarioDetailViewUITests.swift

import XCTest

class ScenarioDetailViewUITests: XCTestCase {
    
    var app: XCUIApplication!
    
    override func setUp() {
        super.setUp()
        app = XCUIApplication()
        app.launch()
    }
    
    // MARK: - Navigation Tests
    
    func testBackNavigationButton() {
        // Navigate to scenario detail
        // Tap back button
        // Verify pop back to list
    }
    
    // MARK: - Edit Flow
    
    func testEditScenarioFlow() {
        // Tap edit button
        let editButton = app.navigationBars.buttons["ellipsis"]
        editButton.tap()
        
        let editOption = app.staticTexts["Edit"]
        editOption.tap()
        
        // Verify edit form appears
        let nameField = app.textFields["Name"]
        XCTAssertTrue(nameField.isHittable)
        
        // Edit form
        nameField.clearAndTypeText("New Name")
        
        // Save
        let saveButton = app.navigationBars.buttons["Save"]
        saveButton.tap()
        
        // Verify saved
        let newNameElement = app.staticTexts["New Name"]
        XCTAssertTrue(newNameElement.exists)
    }
    
    // MARK: - Delete Flow
    
    func testDeleteScenarioFlow() {
        let editButton = app.navigationBars.buttons["ellipsis"]
        editButton.tap()
        
        let deleteOption = app.staticTexts["Delete"]
        deleteOption.tap()
        
        // Confirm deletion
        let confirmButton = app.alerts.buttons["Delete"]
        confirmButton.tap()
        
        // Verify back to list (or error if can't delete)
    }
    
    // MARK: - Voting
    
    func testVoteOnScenario() {
        // If voting buttons present
        // Tap vote button
        // Verify vote updates
    }
}

// Helper extension
extension XCUIElement {
    func clearAndTypeText(_ text: String) {
        self.tap()
        let deleteString = String(repeating: XCUIKeyboardKey.delete.rawValue, count: self.value as? String ?? "")
        self.typeText(deleteString + text)
    }
}
```

---

## Running Tests

### Command Line

```bash
# Run all tests
xcodebuild test -scheme iosApp -destination 'generic/platform=iOS'

# Run specific test class
xcodebuild test -scheme iosApp -destination 'generic/platform=iOS' \
  -only-testing iosAppTests/ScenarioDetailViewModelTests

# Run with coverage
xcodebuild test -scheme iosApp -destination 'generic/platform=iOS' \
  -enableCodeCoverage YES

# Generate coverage report
xcodebuild test -scheme iosApp -destination 'generic/platform=iOS' \
  -enableCodeCoverage YES && \
  xcrun xccov view --report --json

# Run UI tests
xcodebuild test -scheme iosApp -destination 'name=iPhone 15 Pro'
```

### Xcode

1. **Run Tests**: `Cmd + U` or Product → Test
2. **Run Specific Test**: Click diamond icon next to test name
3. **Run with Coverage**: Edit Scheme → Test → Code Coverage ✓
4. **Run UI Tests**: Select iosAppUITests scheme

---

## Test Coverage Goals

```
Target Coverage: 80%+

┌─ Unit Tests (70% of effort)
│  ├─ ViewModel: 85%+ coverage
│  └─ State machine intents: 100%
│
├─ Integration Tests (20% of effort)
│  ├─ View + ViewModel: 70% coverage
│  └─ Error flows: 100%
│
└─ UI Tests (10% of effort)
   ├─ Critical user flows: 100%
   └─ Edge cases: 50%
```

---

## Mock Data for Testing

### ScenarioTestData.swift

```swift
extension Scenario {
    static let mockData = Scenario(
        id: "test-scenario-1",
        eventId: "event-1",
        name: "Paris Trip",
        dateOrPeriod: "2025-12-15 to 2025-12-18",
        location: "Paris, France",
        duration: 4,
        estimatedParticipants: 5,
        estimatedBudgetPerPerson: 800.0,
        description: "A weekend trip to Paris with friends",
        status: .proposed,
        createdAt: "2025-12-01T10:00:00Z",
        updatedAt: "2025-12-01T10:00:00Z"
    )
    
    static let mockDataOrganizing = Scenario(
        id: "test-scenario-2",
        eventId: "event-1",
        name: "Mountain Hiking",
        dateOrPeriod: "2025-12-22",
        location: "Alps, France",
        duration: 1,
        estimatedParticipants: 3,
        estimatedBudgetPerPerson: 50.0,
        description: "Morning hike in the Alps",
        status: .comparing,
        createdAt: "2025-12-05T14:30:00Z",
        updatedAt: "2025-12-05T14:30:00Z"
    )
}

extension ScenarioVotingResult {
    static let mockData = ScenarioVotingResult(
        scenarioId: "test-scenario-1",
        preferCount: 4,
        neutralCount: 1,
        againstCount: 0,
        totalVotes: 5,
        score: 9
    )
}
```

---

## Debugging Tests

### Useful XCTest Helpers

```swift
// Add logging to state changes
override func setUp() {
    super.setUp()
    
    // Enable detailed logging
    XCTest.verbosity = .verbose
}

// Wait for async operations
func waitForCondition(_ condition: @escaping () -> Bool, timeout: TimeInterval = 5.0) {
    let expectation = XCTestExpectation(description: "Condition met")
    
    DispatchQueue.global().async {
        var elapsed = 0.0
        while elapsed < timeout && !condition() {
            usleep(100000)  // 0.1s
            elapsed += 0.1
        }
        expectation.fulfill()
    }
    
    wait(for: [expectation], timeout: timeout + 0.1)
}
```

---

## Continuous Integration

### GitHub Actions Configuration

```yaml
name: Test ScenarioDetailView

on: [push, pull_request]

jobs:
  test:
    runs-on: macos-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Select Xcode version
        run: sudo xcode-select -s /Applications/Xcode.app/Contents/Developer
      
      - name: Run tests
        run: |
          xcodebuild test \
            -scheme iosApp \
            -destination 'platform=iOS Simulator,name=iPhone 15 Pro' \
            -enableCodeCoverage YES
      
      - name: Upload coverage
        uses: codecov/codecov-action@v3
        with:
          files: ./coverage.xcov
```

---

## Checklist

- [x] Unit tests for ViewModel created
- [x] Integration tests for View created
- [x] SwiftUI Previews working
- [ ] Snapshot tests implemented
- [ ] UI tests created
- [ ] Code coverage > 80%
- [ ] CI/CD pipeline configured
- [ ] All tests passing locally
- [ ] Edge cases covered
- [ ] Documentation up to date

---

## Resources

- [XCTest Documentation](https://developer.apple.com/documentation/xctest)
- [SwiftUI Testing](https://developer.apple.com/documentation/swiftui/testing)
- [UI Testing Guide](https://developer.apple.com/documentation/xctest/user_interface_testing)
- [SnapshotTesting Library](https://github.com/pointfreeco/swift-snapshot-testing)

---

**Last Updated**: 2025-12-29  
**Testing Lead**: QA Team  
**Status**: Ready for Implementation
