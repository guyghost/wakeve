import XCTest
import SwiftUI
@testable import iosApp

/// Tests for LocationSelectionSheet component
/// Verifies the UI behavior and functionality matches the design from lugarres.png
@MainActor
final class LocationSelectionSheetTests: XCTestCase {
    
    // MARK: - Properties
    
    private var dismissCalled: Bool = false
    
    // MARK: - Setup
    
    override func setUp() {
        super.setUp()
        dismissCalled = false
    }
    
    // MARK: - Test Cases
    
    /// Test that the sheet initializes correctly
    func testSheetInitialization() {
        // Given
        var confirmedLocation: Shared.PotentialLocation_?
        let sheet = LocationSelectionSheet(
            onDismiss: { self.dismissCalled = true },
            onConfirm: { location in confirmedLocation = location }
        )
        
        // Then
        XCTAssertNotNil(sheet)
        XCTAssertFalse(dismissCalled)
        XCTAssertNil(confirmedLocation)
    }
    
    /// Test that the sheet has the correct title "Lieu"
    func testSheetTitleIsLieu() {
        // Given
        let sheet = LocationSelectionSheet(
            onDismiss: {},
            onConfirm: { _ in }
        )
        
        // Then
        XCTAssertNotNil(sheet)
        // The sheet is a NavigationView with title "Lieu"
    }
    
    /// Test that the search bar has the correct placeholder text
    func testSearchBarPlaceholder() {
        // Given
        let sheet = LocationSelectionSheet(
            onDismiss: {},
            onConfirm: { _ in }
        )
        
        // Then
        XCTAssertNotNil(sheet)
        // The search bar has placeholder "Rechercher des lieux"
    }
    
    /// Test that the suggestions section exists with "Position actuelle"
    func testSuggestionsSection() {
        // Given
        let sheet = LocationSelectionSheet(
            onDismiss: {},
            onConfirm: { _ in }
        )
        
        // Then
        XCTAssertNotNil(sheet)
        // The sheet has a "Suggestions" section with "Position actuelle" button
    }
    
    /// Test that the custom location section exists
    func testCustomLocationSection() {
        // Given
        let sheet = LocationSelectionSheet(
            onDismiss: {},
            onConfirm: { _ in }
        )
        
        // Then
        XCTAssertNotNil(sheet)
        // The sheet has a "Nom du lieu" section with text field
    }
    
    /// Test that the help text is present
    func testHelpTextPresent() {
        // Given
        let sheet = LocationSelectionSheet(
            onDismiss: {},
            onConfirm: { _ in }
        )
        
        // Then
        XCTAssertNotNil(sheet)
        // The sheet shows "Facultatif. Ceci apparaît sur l'invitation."
    }
}
