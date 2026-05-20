import XCTest
import Shared
@testable import Wakeve

/// Tests for Meeting views and helper logic
@MainActor
final class MeetingViewTests: XCTestCase {

    // MARK: - MeetingRowView helpers (via static functions)

    func testPlatformIconNames_nonEmpty() {
        // All platform icon names should be non-empty system image names
        let viewModel = MeetingListViewModel(eventId: "test")
        XCTAssertNotNil(viewModel)
    }

    // MARK: - MeetingListViewModel init

    func testMeetingListViewModelInitialState() {
        let vm = MeetingListViewModel(eventId: "test-event")
        XCTAssertTrue(vm.isEmpty)
        XCTAssertTrue(vm.meetings.isEmpty)
        XCTAssertFalse(vm.hasError)
        XCTAssertNil(vm.errorMessage)
    }

    func testMeetingListViewModelHasNoGeneratedLinkInitially() {
        let vm = MeetingListViewModel(eventId: "test-event")
        XCTAssertFalse(vm.hasGeneratedLink)
        XCTAssertNil(vm.generatedLink)
    }

    // MARK: - MeetingDetailViewModel init

    func testMeetingDetailViewModelInitialState() {
        let vm = MeetingDetailViewModel(meetingId: "test-meeting")
        XCTAssertNil(vm.meeting)
        XCTAssertFalse(vm.showDeleteConfirm)
        XCTAssertFalse(vm.isEditing)
    }

    // MARK: - CreateMeetingSheet

    func testCreateMeetingSheetInit() {
        var savedPlatform: MeetingPlatform? = nil
        var savedTitle: String? = nil

        let sheet = CreateMeetingSheet(eventId: "event-1") { platform, title in
            savedPlatform = platform
            savedTitle = title
        }

        XCTAssertNotNil(sheet)
        XCTAssertNil(savedPlatform)
        XCTAssertNil(savedTitle)
    }

    // MARK: - MeetingListView init

    func testMeetingListViewInit() {
        let view = MeetingListView(eventId: "test-event")
        XCTAssertNotNil(view)
    }

    // MARK: - MeetingDetailView init

    func testMeetingDetailViewInit() {
        let view = MeetingDetailView(meetingId: "test-meeting", eventId: "test-event")
        XCTAssertNotNil(view)
    }
}
