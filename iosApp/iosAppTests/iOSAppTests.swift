import XCTest
@testable import iosApp
import Shared

final class iOSAppTests: XCTestCase {
    
    func testEventCreationViewInitialization() {
        // Test that EventCreationView can be initialized
        let expectation = expectation(description: "Event creation")
        var createdEvent: Event?
        
        let view = EventCreationView(
            onEventCreated: { event in
                createdEvent = event
                expectation.fulfill()
            }
        )
        
        XCTAssertNotNil(view)
    }
    
    func testParticipantManagementViewInitialization() {
        // Test that ParticipantManagementView can be initialized
        let repository = EventRepository()
        let mockEvent = Event(
            id: "test-event-1",
            title: "Test Event",
            description: "A test event",
            organizerId: "org-1",
            participants: [],
            proposedSlots: [],
            deadline: "2025-12-25T18:00:00Z",
            status: EventStatus.DRAFT
        )
        
        let view = ParticipantManagementView(
            event: mockEvent,
            repository: repository,
            onParticipantsAdded: { _ in },
            onNavigateToPoll: { _ in }
        )
        
        XCTAssertNotNil(view)
    }
    
    func testPollVotingViewInitialization() {
        // Test that PollVotingView can be initialized
        let repository = EventRepository()
        let mockEvent = Event(
            id: "test-event-1",
            title: "Test Event",
            description: "A test event",
            organizerId: "org-1",
            participants: ["test@example.com"],
            proposedSlots: [
                TimeSlot(
                    id: "slot-1",
                    start: "2025-12-25T10:00:00Z",
                    end: "2025-12-25T12:00:00Z",
                    timezone: "UTC"
                )
            ],
            deadline: "2025-12-25T18:00:00Z",
            status: EventStatus.POLLING
        )
        
        let view = PollVotingView(
            event: mockEvent,
            repository: repository,
            onVoteSubmitted: { _ in }
        )
        
        XCTAssertNotNil(view)
    }
    
    func testPollResultsViewInitialization() {
        // Test that PollResultsView can be initialized
        let repository = EventRepository()
        let mockEvent = Event(
            id: "test-event-1",
            title: "Test Event",
            description: "A test event",
            organizerId: "org-1",
            participants: ["test@example.com"],
            proposedSlots: [
                TimeSlot(
                    id: "slot-1",
                    start: "2025-12-25T10:00:00Z",
                    end: "2025-12-25T12:00:00Z",
                    timezone: "UTC"
                )
            ],
            deadline: "2025-12-25T18:00:00Z",
            status: EventStatus.CONFIRMED
        )
        
        let view = PollResultsView(
            event: mockEvent,
            repository: repository,
            onDateConfirmed: { _ in }
        )
        
        XCTAssertNotNil(view)
    }
    
    func testAppViewInitialization() {
        // Test that AppView can be initialized
        let view = AppView()
        XCTAssertNotNil(view)
    }
}
