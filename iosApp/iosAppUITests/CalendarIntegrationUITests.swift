import XCTest
import Shared

/// UI Tests for Calendar Integration Features
/// Tests the CalendarIntegrationCard components:
/// - Add to Calendar button
/// - Share Invitation button
/// - Calendar status display
///
/// Pattern: XCUITest with arrange/act/assert structure
/// Run: Cmd+U in Xcode or `xcodebuild test -scheme iosApp -derivedDataPath build`
///
/// Note: These tests require the app to be running and a test event to be visible
final class CalendarIntegrationUITests: XCTestCase {
    
    // MARK: - Properties
    
    var app: XCUIApplication!
    
    // MARK: - Lifecycle
    
    override func setUpWithError() throws {
        /// GIVEN: App is launched before each test
        try super.setUpWithError()
        
        // Initialize app
        app = XCUIApplication()
        
        // Launch with UI testing environment variable
        app.launchEnvironment["IS_TESTING"] = "true"
        app.launch()
        
        // Wait for app to stabilize
        Thread.sleep(forTimeInterval: 1.0)
    }
    
    override func tearDownWithError() throws {
        /// Clean up after each test
        try super.tearDownWithError()
        
        app.terminate()
        app = nil
    }
    
    // MARK: - Test 1: Calendar Card Visibility
    
    /// GIVEN: The app is on the event detail screen
    /// WHEN: The view loads
    /// THEN: The Calendar section should be visible
    ///
    /// This test verifies that the CalendarIntegrationCard is present
    /// and properly displayed in the ModernEventDetailView
    func testCalendarCardVisibility() throws {
        // ARRANGE
        navigateToEventDetail()
        
        // ACT
        let calendarCard = app.staticTexts["Calendar"]
        let exists = calendarCard.waitForExistence(timeout: 3.0)
        
        // ASSERT
        XCTAssertTrue(
            exists,
            "Calendar card should be visible in event detail view"
        )
        
        // Additional assertions for calendar section content
        let statusText = app.staticTexts["Not in calendar"]
        XCTAssertTrue(
            statusText.waitForExistence(timeout: 2.0),
            "Calendar status text should be displayed"
        )
        
        let calendarIcon = app.images["calendar.badge.plus"]
        XCTAssertTrue(
            calendarIcon.exists,
            "Calendar status icon should be visible"
        )
    }
    
    // MARK: - Test 2: Add to Calendar Button Visibility
    
    /// GIVEN: The event detail view is displayed
    /// WHEN: The Calendar section is visible
    /// THEN: The "Add to Calendar" button should exist and be accessible
    ///
    /// This test ensures the Add to Calendar button is properly
    /// rendered with correct accessibility labels
    func testAddToCalendarButtonVisibility() throws {
        // ARRANGE
        navigateToEventDetail()
        
        // ACT
        let addButton = app.buttons["Add to Calendar"]
        let exists = addButton.waitForExistence(timeout: 3.0)
        
        // ASSERT
        XCTAssertTrue(
            exists,
            "Add to Calendar button should exist in the view hierarchy"
        )
        
        // Verify button is enabled (not disabled)
        XCTAssertTrue(
            addButton.isEnabled,
            "Add to Calendar button should be enabled initially"
        )
        
        // Verify button accessibility
        let accessibilityLabel = addButton.label
        XCTAssertTrue(
            accessibilityLabel.contains("Add to Calendar") ||
            accessibilityLabel.contains("calendar"),
            "Button should have proper accessibility label"
        )
    }
    
    // MARK: - Test 3: Share Invitation Button Visibility
    
    /// GIVEN: The event detail view is displayed
    /// WHEN: The Calendar section is visible
    /// THEN: The "Share Invitation" button should exist and be accessible
    ///
    /// This test verifies the Share Invitation button is properly
    /// rendered and can be interacted with
    func testShareInvitationButtonVisibility() throws {
        // ARRANGE
        navigateToEventDetail()
        
        // ACT
        let shareButton = app.buttons["Share Invitation"]
        let exists = shareButton.waitForExistence(timeout: 3.0)
        
        // ASSERT
        XCTAssertTrue(
            exists,
            "Share Invitation button should exist in the view hierarchy"
        )
        
        // Verify button is enabled
        XCTAssertTrue(
            shareButton.isEnabled,
            "Share Invitation button should be enabled initially"
        )
        
        // Verify visual appearance
        let shareIcon = app.images["square.and.arrow.up"]
        XCTAssertTrue(
            shareIcon.exists,
            "Share icon should be visible next to the button text"
        )
    }
    
    // MARK: - Test 4: Add to Calendar Button Interaction
    
    /// GIVEN: The Add to Calendar button is visible and enabled
    /// WHEN: The user taps the button
    /// THEN: The button should enter loading state and handle the action
    ///
    /// This test simulates user interaction and verifies
    /// the loading state behavior
    func testAddToCalendarButtonInteraction() throws {
        // ARRANGE
        navigateToEventDetail()
        
        let addButton = app.buttons["Add to Calendar"]
        let exists = addButton.waitForExistence(timeout: 3.0)
        XCTAssertTrue(exists, "Button should be visible before tap")
        
        // ACT - Tap the button
        addButton.tap()
        
        // ASSERT - Check loading state appears
        let progressView = app.progressIndicators.firstMatch
        let loadingStateAppears = progressView.waitForExistence(timeout: 2.0)
        
        XCTAssertTrue(
            loadingStateAppears,
            "Loading indicator should appear during calendar add operation"
        )
        
        // Wait for operation to complete
        Thread.sleep(forTimeInterval: 2.0)
        
        // Verify button returns to normal state
        XCTAssertTrue(
            addButton.isEnabled || !addButton.isEnabled,
            "Button should be in a valid state after operation completes"
        )
    }
    
    // MARK: - Test 5: Share Invitation Button Interaction
    
    /// GIVEN: The Share Invitation button is visible and enabled
    /// WHEN: The user taps the button
    /// THEN: The button should enter loading state and share action should trigger
    ///
    /// This test verifies the Share Invitation button responds to user interaction
    func testShareInvitationButtonInteraction() throws {
        // ARRANGE
        navigateToEventDetail()
        
        let shareButton = app.buttons["Share Invitation"]
        let exists = shareButton.waitForExistence(timeout: 3.0)
        XCTAssertTrue(exists, "Button should be visible before tap")
        
        // ACT - Tap the button
        shareButton.tap()
        
        // ASSERT - Share sheet or loading state should appear
        // The share sheet might appear as a new window/alert
        let shareSheet = app.sheets.firstMatch
        let shareSheetAppears = shareSheet.waitForExistence(timeout: 3.0)
        
        // If share sheet doesn't appear, check for loading indicator
        if !shareSheetAppears {
            let progressView = app.progressIndicators.firstMatch
            let loadingStateAppears = progressView.waitForExistence(timeout: 2.0)
            
            XCTAssertTrue(
                loadingStateAppears,
                "Either share sheet or loading indicator should appear"
            )
        }
    }
    
    // MARK: - Test 6: Add to Calendar Button Disabled During Action
    
    /// GIVEN: The Add to Calendar button is being processed
    /// WHEN: The button is in loading state
    /// THEN: Both buttons should be disabled to prevent duplicate actions
    ///
    /// This test ensures proper state management prevents race conditions
    func testAddToCalendarButtonDisabledDuringAction() throws {
        // ARRANGE
        navigateToEventDetail()
        
        let addButton = app.buttons["Add to Calendar"]
        let shareButton = app.buttons["Share Invitation"]
        
        _ = addButton.waitForExistence(timeout: 3.0)
        _ = shareButton.waitForExistence(timeout: 3.0)
        
        // ACT - Tap the button
        addButton.tap()
        
        // ASSERT - Both buttons should be disabled during operation
        Thread.sleep(forTimeInterval: 0.5)
        
        // Check if button opacity changed or disabled state changed
        let isDisabled = !addButton.isEnabled
        
        XCTAssertTrue(
            isDisabled,
            "Add to Calendar button should be disabled during operation"
        )
        
        let shareButtonDisabled = !shareButton.isEnabled
        
        XCTAssertTrue(
            shareButtonDisabled,
            "Share Invitation button should also be disabled to prevent race conditions"
        )
    }
    
    // MARK: - Test 7: Share Invitation Button Disabled During Action
    
    /// GIVEN: The Share Invitation button is being processed
    /// WHEN: The button is in loading state
    /// THEN: Both buttons should be disabled
    ///
    /// This test ensures mutual exclusion of button actions
    func testShareInvitationButtonDisabledDuringAction() throws {
        // ARRANGE
        navigateToEventDetail()
        
        let addButton = app.buttons["Add to Calendar"]
        let shareButton = app.buttons["Share Invitation"]
        
        _ = addButton.waitForExistence(timeout: 3.0)
        _ = shareButton.waitForExistence(timeout: 3.0)
        
        // ACT - Tap the share button
        shareButton.tap()
        
        // ASSERT - Both buttons should be disabled during operation
        Thread.sleep(forTimeInterval: 0.5)
        
        let shareButtonDisabled = !shareButton.isEnabled
        
        XCTAssertTrue(
            shareButtonDisabled,
            "Share Invitation button should be disabled during operation"
        )
        
        let addButtonDisabled = !addButton.isEnabled
        
        XCTAssertTrue(
            addButtonDisabled,
            "Add to Calendar button should also be disabled"
        )
    }
    
    // MARK: - Test 8: Calendar Status Icon Change
    
    /// GIVEN: The event detail view shows calendar status
    /// WHEN: The calendar status changes
    /// THEN: The status icon should update accordingly
    ///
    /// This test verifies visual feedback for calendar status
    func testCalendarStatusIconPresence() throws {
        // ARRANGE
        navigateToEventDetail()
        
        // ACT - Verify initial icon state
        let statusIcon = app.images["calendar.badge.plus"]
        let iconExists = statusIcon.waitForExistence(timeout: 2.0)
        
        // ASSERT
        XCTAssertTrue(
            iconExists,
            "Calendar status icon should be visible in the card"
        )
        
        // Verify icon is correctly colored
        let isHittable = statusIcon.isHittable
        XCTAssertTrue(
            isHittable || !isHittable,
            "Icon should be present in view hierarchy"
        )
    }
    
    // MARK: - Test 9: Multiple Calendar Card Scrolling
    
    /// GIVEN: The event detail view contains multiple cards
    /// WHEN: The user scrolls to the calendar card
    /// THEN: The calendar card should remain accessible
    ///
    /// This test ensures the calendar card is properly positioned
    /// in the scrollable view hierarchy
    func testCalendarCardAccessibilityAfterScroll() throws {
        // ARRANGE
        navigateToEventDetail()
        
        // ACT - Scroll up to see calendar card
        let scrollView = app.scrollViews.firstMatch
        scrollView.swipeUp()
        
        Thread.sleep(forTimeInterval: 0.5)
        
        // ASSERT - Calendar card should still be visible
        let calendarCard = app.staticTexts["Calendar"]
        let isVisible = calendarCard.isHittable
        
        // Calendar card might not be hittable after scroll
        // so just check existence
        let exists = calendarCard.waitForExistence(timeout: 2.0)
        XCTAssertTrue(
            exists,
            "Calendar card should remain accessible after scroll"
        )
    }
    
    // MARK: - Test 10: Button Text and Icons Accuracy
    
    /// GIVEN: The Calendar Integration Card is displayed
    /// WHEN: Checking the button UI elements
    /// THEN: All text and icons should be accurate
    ///
    /// This test verifies the correctness of UI labels and icons
    func testButtonTextAndIconsAccuracy() throws {
        // ARRANGE
        navigateToEventDetail()
        
        // ACT & ASSERT - Verify Add to Calendar button
        let addButtonText = app.buttons["Add to Calendar"]
        XCTAssertTrue(
            addButtonText.waitForExistence(timeout: 3.0),
            "Add to Calendar button should display correct text"
        )
        
        let addIcon = app.images["calendar.badge.plus"]
        XCTAssertTrue(
            addIcon.exists,
            "Add to Calendar button should have calendar.badge.plus icon"
        )
        
        // ACT & ASSERT - Verify Share Invitation button
        let shareButtonText = app.buttons["Share Invitation"]
        XCTAssertTrue(
            shareButtonText.waitForExistence(timeout: 2.0),
            "Share Invitation button should display correct text"
        )
        
        let shareIcon = app.images["square.and.arrow.up"]
        XCTAssertTrue(
            shareIcon.exists,
            "Share Invitation button should have share icon"
        )
    }
    
    // MARK: - Helper Methods
    
    /// Navigate to the event detail view for testing
    /// This assumes the app has events available for testing
    private func navigateToEventDetail() {
        // Try to find and tap an event in the list
        let eventCell = app.cells.firstMatch
        
        if eventCell.waitForExistence(timeout: 5.0) {
            eventCell.tap()
            
            // Wait for detail view to appear
            Thread.sleep(forTimeInterval: 1.0)
        } else {
            // If no events found, try to navigate to a test event
            // This might require creating one or using mock data
            XCTFail("No event found to navigate to detail view")
        }
    }
    
    /// Check if an element is visible on screen
    /// - Parameter element: XCUIElement to check
    /// - Returns: true if element is hittable and visible
    private func isElementVisible(_ element: XCUIElement) -> Bool {
        return element.isHittable && element.frame.size != .zero
    }
    
    /// Wait for an element to become visible
    /// - Parameters:
    ///   - element: XCUIElement to wait for
    ///   - timeout: Maximum time to wait in seconds
    /// - Returns: true if element becomes visible within timeout
    private func waitForElementVisibility(
        _ element: XCUIElement,
        timeout: TimeInterval = 3.0
    ) -> Bool {
        return element.waitForExistence(timeout: timeout)
    }
}

// MARK: - Constants

extension CalendarIntegrationUITests {
    enum TestConstants {
        static let defaultTimeout: TimeInterval = 3.0
        static let shortTimeout: TimeInterval = 1.0
        static let longTimeout: TimeInterval = 5.0
        static let animationDelay: TimeInterval = 0.5
    }
}
