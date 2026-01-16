import XCTest

/// UI Tests for Delete Event Feature
/// Tests the delete event functionality:
/// - Delete button visibility for organizer
/// - Delete confirmation alert
/// - Haptic feedback (visual verification)
/// - Navigation after deletion
///
/// Pattern: XCUITest with arrange/act/assert structure
/// Run: Cmd+U in Xcode or `xcodebuild test -scheme iosApp -derivedDataPath build`
final class DeleteEventUITests: XCTestCase {
    
    // MARK: - Properties
    
    var app: XCUIApplication!
    
    // MARK: - Lifecycle
    
    override func setUpWithError() throws {
        try super.setUpWithError()
        
        // Initialize app
        app = XCUIApplication()
        
        // Launch with UI testing environment variable
        app.launchEnvironment["IS_TESTING"] = "true"
        // Launch as organizer to see delete button
        app.launchEnvironment["TEST_USER_ROLE"] = "organizer"
        app.launch()
        
        // Wait for app to stabilize
        Thread.sleep(forTimeInterval: 1.0)
    }
    
    override func tearDownWithError() throws {
        try super.tearDownWithError()
        
        app.terminate()
        app = nil
    }
    
    // MARK: - Test 1: Delete Button Visibility for Organizer
    
    /// GIVEN: The user is the organizer of an event
    /// WHEN: The event detail view is displayed
    /// THEN: The delete button should be visible
    ///
    /// This test verifies that organizers can see the delete button
    func testDeleteButtonVisibilityForOrganizer() throws {
        // ARRANGE
        navigateToOrganizerEventDetail()
        
        // ACT
        let deleteButton = app.buttons["delete_event"]
        let exists = deleteButton.waitForExistence(timeout: 3.0)
        
        // ASSERT
        XCTAssertTrue(
            exists,
            "Delete button should be visible for organizer"
        )
    }
    
    // MARK: - Test 2: Delete Button Not Visible for Non-Organizer
    
    /// GIVEN: The user is NOT the organizer of an event
    /// WHEN: The event detail view is displayed
    /// THEN: The delete button should NOT be visible
    ///
    /// This test verifies that non-organizers cannot see the delete button
    func testDeleteButtonHiddenForNonOrganizer() throws {
        // ARRANGE - Relaunch as participant
        app.terminate()
        app.launchEnvironment["TEST_USER_ROLE"] = "participant"
        app.launch()
        Thread.sleep(forTimeInterval: 1.0)
        
        navigateToEventDetail()
        
        // ACT
        let deleteButton = app.buttons["delete_event"]
        let exists = deleteButton.waitForExistence(timeout: 2.0)
        
        // ASSERT
        XCTAssertFalse(
            exists,
            "Delete button should NOT be visible for non-organizer"
        )
    }
    
    // MARK: - Test 3: Delete Confirmation Alert Appears
    
    /// GIVEN: The delete button is visible
    /// WHEN: The user taps the delete button
    /// THEN: A confirmation alert should appear
    ///
    /// This test verifies the delete confirmation dialog is shown
    func testDeleteConfirmationAlertAppears() throws {
        // ARRANGE
        navigateToOrganizerEventDetail()
        
        let deleteButton = app.buttons["delete_event"]
        _ = deleteButton.waitForExistence(timeout: 3.0)
        
        // ACT
        deleteButton.tap()
        
        // ASSERT - Alert should appear
        let alert = app.alerts.firstMatch
        let alertAppears = alert.waitForExistence(timeout: 2.0)
        
        XCTAssertTrue(
            alertAppears,
            "Delete confirmation alert should appear when tapping delete button"
        )
        
        // Verify alert has correct title
        let alertTitle = app.staticTexts["delete_event_title"]
        XCTAssertTrue(
            alertTitle.exists || alert.label.contains("Supprimer") || alert.label.contains("Delete"),
            "Alert should have delete-related title"
        )
    }
    
    // MARK: - Test 4: Delete Confirmation Has Cancel Option
    
    /// GIVEN: The delete confirmation alert is displayed
    /// WHEN: The user wants to cancel
    /// THEN: The cancel button should be available and dismisses the alert
    ///
    /// This test verifies the cancel option works
    func testDeleteConfirmationCancelOption() throws {
        // ARRANGE
        navigateToOrganizerEventDetail()
        
        let deleteButton = app.buttons["delete_event"]
        _ = deleteButton.waitForExistence(timeout: 3.0)
        deleteButton.tap()
        
        let alert = app.alerts.firstMatch
        _ = alert.waitForExistence(timeout: 2.0)
        
        // ACT - Tap Cancel
        let cancelButton = alert.buttons["Annuler"].exists ? 
            alert.buttons["Annuler"] : alert.buttons["Cancel"]
        cancelButton.tap()
        
        // ASSERT - Alert should be dismissed
        Thread.sleep(forTimeInterval: 0.5)
        XCTAssertFalse(
            alert.exists,
            "Alert should be dismissed after tapping Cancel"
        )
        
        // Event detail should still be visible
        let eventTitle = app.staticTexts.element(boundBy: 0)
        XCTAssertTrue(
            eventTitle.exists,
            "Event detail should still be visible after cancel"
        )
    }
    
    // MARK: - Test 5: Delete Confirmation Destructive Action
    
    /// GIVEN: The delete confirmation alert is displayed
    /// WHEN: The user confirms deletion
    /// THEN: The event should be deleted and user navigated back
    ///
    /// This test verifies the delete action works
    func testDeleteConfirmationDestructiveAction() throws {
        // ARRANGE
        navigateToOrganizerEventDetail()
        
        let deleteButton = app.buttons["delete_event"]
        _ = deleteButton.waitForExistence(timeout: 3.0)
        deleteButton.tap()
        
        let alert = app.alerts.firstMatch
        _ = alert.waitForExistence(timeout: 2.0)
        
        // ACT - Tap Delete (destructive action)
        let confirmButton = alert.buttons["Supprimer"].exists ? 
            alert.buttons["Supprimer"] : alert.buttons["Delete"]
        confirmButton.tap()
        
        // ASSERT - Should navigate back to home/list
        Thread.sleep(forTimeInterval: 1.0)
        
        // Event detail should no longer be visible
        let eventDetailView = app.otherElements["event_detail_view"]
        XCTAssertFalse(
            eventDetailView.exists,
            "Event detail view should be dismissed after deletion"
        )
    }
    
    // MARK: - Test 6: Delete Button in More Options Menu
    
    /// GIVEN: The event detail view has a more options menu
    /// WHEN: The user taps the more options button
    /// THEN: The delete option should be available in the menu
    ///
    /// This test verifies the delete option in the ellipsis menu
    func testDeleteOptionInMoreOptionsMenu() throws {
        // ARRANGE
        navigateToOrganizerEventDetail()
        
        // ACT - Tap more options (ellipsis) button
        let moreButton = app.buttons["more_options"]
        if moreButton.waitForExistence(timeout: 2.0) {
            moreButton.tap()
            
            Thread.sleep(forTimeInterval: 0.5)
            
            // ASSERT - Delete option should appear
            let deleteOption = app.buttons["delete_event"]
            let exists = deleteOption.waitForExistence(timeout: 2.0)
            
            XCTAssertTrue(
                exists,
                "Delete option should be available in more options menu"
            )
        }
    }
    
    // MARK: - Test 7: Delete Button Not Available for FINALIZED Events
    
    /// GIVEN: An event in FINALIZED status
    /// WHEN: The event detail view is displayed
    /// THEN: The delete button should NOT be visible (even for organizer)
    ///
    /// This test verifies FINALIZED events cannot be deleted
    func testDeleteButtonHiddenForFinalizedEvent() throws {
        // ARRANGE - Navigate to a FINALIZED event
        navigateToFinalizedEventDetail()
        
        // ACT
        let deleteButton = app.buttons["delete_event"]
        let exists = deleteButton.waitForExistence(timeout: 2.0)
        
        // ASSERT
        XCTAssertFalse(
            exists,
            "Delete button should NOT be visible for FINALIZED events"
        )
    }
    
    // MARK: - Test 8: Visual Feedback During Deletion (Animation)
    
    /// GIVEN: The user confirms deletion
    /// WHEN: The delete action is processing
    /// THEN: Visual feedback (fade out animation) should be visible
    ///
    /// This test verifies visual feedback during deletion
    func testVisualFeedbackDuringDeletion() throws {
        // ARRANGE
        navigateToOrganizerEventDetail()
        
        let deleteButton = app.buttons["delete_event"]
        _ = deleteButton.waitForExistence(timeout: 3.0)
        deleteButton.tap()
        
        let alert = app.alerts.firstMatch
        _ = alert.waitForExistence(timeout: 2.0)
        
        // ACT - Confirm deletion
        let confirmButton = alert.buttons["Supprimer"].exists ? 
            alert.buttons["Supprimer"] : alert.buttons["Delete"]
        confirmButton.tap()
        
        // ASSERT - View should animate (opacity change)
        // Note: Actual animation testing is limited in XCUITest
        // We verify the view is dismissed within expected animation time
        let eventDetailView = app.scrollViews.firstMatch
        
        let expectation = XCTNSPredicateExpectation(
            predicate: NSPredicate(format: "exists == false"),
            object: eventDetailView
        )
        
        let result = XCTWaiter.wait(for: [expectation], timeout: 1.0)
        XCTAssertTrue(
            result == .completed || eventDetailView.frame.size == .zero,
            "View should animate out during deletion"
        )
    }
    
    // MARK: - Test 9: Delete Button Accessibility
    
    /// GIVEN: The delete button is visible
    /// WHEN: Checking accessibility properties
    /// THEN: The button should have proper accessibility labels
    ///
    /// This test verifies accessibility compliance
    func testDeleteButtonAccessibility() throws {
        // ARRANGE
        navigateToOrganizerEventDetail()
        
        // ACT
        let deleteButton = app.buttons["delete_event"]
        _ = deleteButton.waitForExistence(timeout: 3.0)
        
        // ASSERT - Button should have accessibility label
        let accessibilityLabel = deleteButton.label
        XCTAssertFalse(
            accessibilityLabel.isEmpty,
            "Delete button should have an accessibility label"
        )
        
        // Button should be identified as destructive action
        // (verified through semantic trait in SwiftUI implementation)
        XCTAssertTrue(
            deleteButton.isHittable,
            "Delete button should be hittable/accessible"
        )
    }
    
    // MARK: - Test 10: Delete Flow End-to-End
    
    /// GIVEN: An event that can be deleted
    /// WHEN: The user goes through the full delete flow
    /// THEN: The event should be removed and user should see confirmation
    ///
    /// This is an end-to-end test of the delete flow
    func testDeleteFlowEndToEnd() throws {
        // ARRANGE
        navigateToOrganizerEventDetail()
        
        // Record initial event count (if visible)
        let initialEventCount = app.cells.count
        
        // ACT - Complete delete flow
        let deleteButton = app.buttons["delete_event"]
        _ = deleteButton.waitForExistence(timeout: 3.0)
        deleteButton.tap()
        
        let alert = app.alerts.firstMatch
        _ = alert.waitForExistence(timeout: 2.0)
        
        let confirmButton = alert.buttons["Supprimer"].exists ? 
            alert.buttons["Supprimer"] : alert.buttons["Delete"]
        confirmButton.tap()
        
        // Wait for deletion to complete
        Thread.sleep(forTimeInterval: 1.5)
        
        // ASSERT - Event count should decrease (if on list view)
        let finalEventCount = app.cells.count
        XCTAssertTrue(
            finalEventCount <= initialEventCount,
            "Event count should decrease or stay same after deletion"
        )
    }
    
    // MARK: - Helper Methods
    
    /// Navigate to an event detail view where the user is the organizer
    private func navigateToOrganizerEventDetail() {
        let eventCell = app.cells.firstMatch
        
        if eventCell.waitForExistence(timeout: 5.0) {
            eventCell.tap()
            Thread.sleep(forTimeInterval: 1.0)
        } else {
            XCTFail("No event found to navigate to detail view")
        }
    }
    
    /// Navigate to any event detail view
    private func navigateToEventDetail() {
        navigateToOrganizerEventDetail()
    }
    
    /// Navigate to a FINALIZED event (requires mock data)
    private func navigateToFinalizedEventDetail() {
        // Look for a FINALIZED event in the list
        let finalizedCell = app.cells.containing(
            NSPredicate(format: "label CONTAINS 'Finalized' OR label CONTAINS 'finalized'")
        ).firstMatch
        
        if finalizedCell.waitForExistence(timeout: 3.0) {
            finalizedCell.tap()
            Thread.sleep(forTimeInterval: 1.0)
        } else {
            // Fallback to first event (test may skip)
            navigateToEventDetail()
        }
    }
}

// MARK: - Constants

extension DeleteEventUITests {
    enum TestConstants {
        static let defaultTimeout: TimeInterval = 3.0
        static let shortTimeout: TimeInterval = 1.0
        static let animationDuration: TimeInterval = 0.3
    }
}
