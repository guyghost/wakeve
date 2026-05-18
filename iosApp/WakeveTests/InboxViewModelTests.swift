import XCTest
@testable import Wakeve

/// Tests for InboxViewModel
/// Verifies notification loading, read/unread state, deletion, and type mapping.
@MainActor
final class InboxViewModelTests: XCTestCase {

    // MARK: - Initial State

    func testInitialState() {
        let vm = InboxViewModel(userId: "test-user")
        XCTAssertTrue(vm.items.isEmpty, "Items should be empty initially")
        XCTAssertTrue(vm.isLoading == false, "Should not be loading initially")
        XCTAssertNil(vm.errorMessage, "No error initially")
        XCTAssertEqual(vm.unreadCount, 0, "Unread count should be 0")
    }

    // MARK: - Read / Unread Logic

    func testMarkAsRead_updatesUnreadCount() {
        let vm = InboxViewModel(userId: "test-user")

        // Manually set items to simulate loaded state
        vm.items = [
            InboxItemModel(id: "1", title: "Test", message: "Msg", timeAgo: "5m",
                           type: .invitation, isRead: false, commentCount: 0,
                           isFocused: true, eventName: "Event", eventId: "e1"),
            InboxItemModel(id: "2", title: "Test 2", message: "Msg", timeAgo: "1h",
                           type: .eventUpdate, isRead: false, commentCount: 0,
                           isFocused: false, eventName: nil, eventId: nil)
        ]
        vm.unreadCount = 2

        vm.markAsRead("1")

        // After marking as read, the item should be updated and count decremented
        XCTAssertEqual(vm.unreadCount, 1, "Unread count should be 1 after reading one item")
        XCTAssertTrue(vm.items[0].isRead, "First item should be marked as read")
    }

    func testMarkAllAsRead_marksAllItems() {
        let vm = InboxViewModel(userId: "test-user")

        vm.items = [
            InboxItemModel(id: "1", title: "A", message: "M", timeAgo: "5m",
                           type: .invitation, isRead: false, commentCount: 0,
                           isFocused: true, eventName: nil, eventId: nil),
            InboxItemModel(id: "2", title: "B", message: "M", timeAgo: "1h",
                           type: .pollUpdate, isRead: false, commentCount: 0,
                           isFocused: false, eventName: nil, eventId: nil),
            InboxItemModel(id: "3", title: "C", message: "M", timeAgo: "2h",
                           type: .comment, isRead: true, commentCount: 0,
                           isFocused: false, eventName: nil, eventId: nil)
        ]
        vm.unreadCount = 2

        vm.markAllAsRead()

        XCTAssertTrue(vm.items.allSatisfy(\.isRead), "All items should be read")
        XCTAssertEqual(vm.unreadCount, 0, "Unread count should be 0")
    }

    // MARK: - Delete

    func testDeleteNotification_removesFromList() {
        let vm = InboxViewModel(userId: "test-user")

        vm.items = [
            InboxItemModel(id: "1", title: "Keep", message: "M", timeAgo: "5m",
                           type: .invitation, isRead: false, commentCount: 0,
                           isFocused: true, eventName: nil, eventId: nil),
            InboxItemModel(id: "2", title: "Delete", message: "M", timeAgo: "1h",
                           type: .eventUpdate, isRead: true, commentCount: 0,
                           isFocused: false, eventName: nil, eventId: nil)
        ]
        vm.unreadCount = 1

        vm.deleteNotification("2")

        XCTAssertEqual(vm.items.count, 1, "Should have 1 item after deletion")
        XCTAssertEqual(vm.items[0].id, "1", "Remaining item should be id 1")
        XCTAssertEqual(vm.unreadCount, 1, "Unread count unchanged (deleted item was read)")
    }

    func testDeleteNotification_updatesUnreadCount() {
        let vm = InboxViewModel(userId: "test-user")

        vm.items = [
            InboxItemModel(id: "1", title: "A", message: "M", timeAgo: "5m",
                           type: .invitation, isRead: false, commentCount: 0,
                           isFocused: true, eventName: nil, eventId: nil),
            InboxItemModel(id: "2", title: "B", message: "M", timeAgo: "1h",
                           type: .comment, isRead: false, commentCount: 0,
                           isFocused: false, eventName: nil, eventId: nil)
        ]
        vm.unreadCount = 2

        vm.deleteNotification("1")

        XCTAssertEqual(vm.items.count, 1)
        XCTAssertEqual(vm.unreadCount, 1, "Unread count should decrease when deleting unread item")
    }

    // MARK: - InboxItemModel Tests

    func testInboxItemModel_requiresAction() {
        let invitation = InboxItemModel(id: "1", title: "", message: "", timeAgo: "",
                                         type: .invitation, isRead: false, commentCount: 0,
                                         isFocused: false, eventName: nil, eventId: nil)
        XCTAssertTrue(invitation.requiresAction)

        let comment = InboxItemModel(id: "2", title: "", message: "", timeAgo: "",
                                      type: .comment, isRead: false, commentCount: 0,
                                      isFocused: false, eventName: nil, eventId: nil)
        XCTAssertFalse(comment.requiresAction)
    }

    func testInboxItemModel_icons() {
        let invitation = InboxItemModel(id: "1", title: "", message: "", timeAgo: "",
                                         type: .invitation, isRead: false, commentCount: 0,
                                         isFocused: false, eventName: nil, eventId: nil)
        XCTAssertEqual(invitation.icon, "envelope.fill")

        let poll = InboxItemModel(id: "2", title: "", message: "", timeAgo: "",
                                   type: .pollUpdate, isRead: false, commentCount: 0,
                                   isFocused: false, eventName: nil, eventId: nil)
        XCTAssertEqual(poll.icon, "chart.bar.fill")

        let eventUpd = InboxItemModel(id: "3", title: "", message: "", timeAgo: "",
                                       type: .eventUpdate, isRead: false, commentCount: 0,
                                       isFocused: false, eventName: nil, eventId: nil)
        XCTAssertEqual(eventUpd.icon, "calendar")
    }

    func testInboxItemModel_hashableAndEquatable() {
        let item1 = InboxItemModel(id: "same", title: "A", message: "M", timeAgo: "5m",
                                    type: .invitation, isRead: false, commentCount: 0,
                                    isFocused: false, eventName: nil, eventId: nil)
        let item2 = InboxItemModel(id: "same", title: "A", message: "M", timeAgo: "5m",
                                    type: .invitation, isRead: false, commentCount: 0,
                                    isFocused: false, eventName: nil, eventId: nil)
        XCTAssertEqual(item1, item2)
        XCTAssertEqual(item1.hashValue, item2.hashValue)
    }
}
