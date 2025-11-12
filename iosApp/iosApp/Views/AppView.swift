import SwiftUI
import Shared

struct AppView: View {
    @State private var repository = EventRepository()
    @State private var events: [Event] = []
    @State private var currentScreen: NavigationScreen = .eventList
    @State private var selectedEvent: Event?
    
    enum NavigationScreen {
        case eventList
        case eventCreation
        case participantManagement
        case pollVoting
        case pollResults
    }
    
    var body: some View {
        NavigationStack {
            Group {
                switch currentScreen {
                case .eventList:
                    EventListView(
                        events: events,
                        onCreateEvent: {
                            currentScreen = .eventCreation
                        },
                        onSelectEvent: { event in
                            selectedEvent = event
                            navigateToEventScreen(event)
                        }
                    )
                
                case .eventCreation:
                    EventCreationView(
                        onEventCreated: { event in
                            events.append(event)
                            selectedEvent = event
                            currentScreen = .participantManagement
                        },
                        onNavigateToParticipants: { eventId in
                            currentScreen = .participantManagement
                        }
                    )
                
                case .participantManagement:
                    if let event = selectedEvent {
                        ParticipantManagementView(
                            event: event,
                            repository: repository,
                            onParticipantsAdded: { _ in
                                currentScreen = .eventList
                            },
                            onNavigateToPoll: { _ in
                                currentScreen = .pollVoting
                            }
                        )
                    }
                
                case .pollVoting:
                    if let event = selectedEvent {
                        PollVotingView(
                            event: event,
                            repository: repository,
                            onVoteSubmitted: { _ in
                                currentScreen = .pollResults
                            }
                        )
                    }
                
                case .pollResults:
                    if let event = selectedEvent {
                        PollResultsView(
                            event: event,
                            repository: repository,
                            onDateConfirmed: { _ in
                                currentScreen = .eventList
                            }
                        )
                    }
                }
            }
        }
    }
    
    private func navigateToEventScreen(_ event: Event) {
        switch event.status {
        case .DRAFT:
            currentScreen = .participantManagement
        case .POLLING:
            currentScreen = .pollVoting
        case .CONFIRMED:
            currentScreen = .pollResults
        @unknown default:
            currentScreen = .eventList
        }
    }
}

struct EventListView: View {
    let events: [Event]
    let onCreateEvent: () -> Void
    let onSelectEvent: (Event) -> Void
    
    var body: some View {
        VStack {
            HStack {
                Text("Wakeve")
                    .font(.system(size: 32, weight: .bold))
                
                Spacer()
                
                Button(action: onCreateEvent) {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: 24))
                }
            }
            .padding(16)
            
            if events.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "calendar.badge.plus")
                        .font(.system(size: 48))
                        .foregroundColor(.gray)
                    
                    Text("No events yet")
                        .font(.headline)
                    
                    Text("Create your first event to get started")
                        .font(.caption)
                        .foregroundColor(.gray)
                    
                    Button(action: onCreateEvent) {
                        Text("Create Event")
                            .frame(maxWidth: .infinity)
                            .padding(12)
                    }
                    .buttonStyle(.borderedProminent)
                    .padding(.horizontal)
                }
                .frame(maxHeight: .infinity)
                .padding(16)
            } else {
                List(events, id: \.id) { event in
                    VStack(alignment: .leading, spacing: 4) {
                        Text(event.title)
                            .font(.headline)
                        Text(event.description)
                            .font(.caption)
                            .foregroundColor(.gray)
                        Text("Status: \(statusString(event.status))")
                            .font(.caption2)
                            .foregroundColor(.blue)
                    }
                    .contentShape(Rectangle())
                    .onTapGesture {
                        onSelectEvent(event)
                    }
                }
            }
        }
    }
    
    private func statusString(_ status: EventStatus) -> String {
        switch status {
        case .DRAFT:
            return "Draft"
        case .POLLING:
            return "Polling"
        case .CONFIRMED:
            return "Confirmed"
        @unknown default:
            return "Unknown"
        }
    }
}

#Preview {
    AppView()
}
