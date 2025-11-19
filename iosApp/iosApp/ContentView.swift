import SwiftUI
import Shared

struct ContentView: View {
    @State private var currentView: AppView = .eventList
    @State private var selectedEvent: Event?
    @State private var repository = EventRepository()
    
    var body: some View {
        ZStack {
            // Background gradient
            LinearGradient(
                gradient: Gradient(colors: [
                    Color.blue.opacity(0.1),
                    Color.purple.opacity(0.1),
                    Color.pink.opacity(0.1)
                ]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            
            switch currentView {
            case .eventList:
                EventListView(
                    repository: repository,
                    onEventSelected: { event in
                        selectedEvent = event
                        currentView = .eventDetail
                    },
                    onCreateEvent: {
                        currentView = .eventCreation
                    }
                )
                
            case .eventCreation:
                EventCreationView(
                    repository: repository,
                    onEventCreated: { eventId in
                        if let event = repository.getEvent(id: eventId) {
                            selectedEvent = event
                            currentView = .participantManagement
                        }
                    }
                )
                
            case .eventDetail:
                if let event = selectedEvent {
                    EventDetailView(
                        event: event,
                        repository: repository,
                        onManageParticipants: {
                            currentView = .participantManagement
                        },
                        onVote: {
                            currentView = .pollVoting
                        },
                        onViewResults: {
                            currentView = .pollResults
                        },
                        onBack: {
                            currentView = .eventList
                        }
                    )
                }
                
            case .participantManagement:
                if let event = selectedEvent {
                    ParticipantManagementView(
                        event: event,
                        repository: repository,
                        onParticipantsUpdated: {
                            // Stay on the same view, just refresh
                        }
                    )
                    .overlay(
                        VStack {
                            HStack {
                                Button(action: {
                                    currentView = .eventDetail
                                }) {
                                    Image(systemName: "chevron.left")
                                        .font(.system(size: 20, weight: .semibold))
                                        .foregroundColor(.white)
                                        .padding(12)
                                        .background(Color.white.opacity(0.1))
                                        .clipShape(Circle())
                                }
                                
                                Spacer()
                            }
                            .padding(.top, 60)
                            .padding(.horizontal, 20)
                            
                            Spacer()
                        }
                    )
                }
                
            case .pollVoting:
                if let event = selectedEvent {
                    PollVotingView(
                        event: event,
                        repository: repository,
                        participantId: "participant-1", // TODO: Get from auth
                        onVoteSubmitted: {
                            currentView = .eventDetail
                        }
                    )
                    .overlay(
                        VStack {
                            HStack {
                                Button(action: {
                                    currentView = .eventDetail
                                }) {
                                    Image(systemName: "chevron.left")
                                        .font(.system(size: 20, weight: .semibold))
                                        .foregroundColor(.white)
                                        .padding(12)
                                        .background(Color.white.opacity(0.1))
                                        .clipShape(Circle())
                                }
                                
                                Spacer()
                            }
                            .padding(.top, 60)
                            .padding(.horizontal, 20)
                            
                            Spacer()
                        }
                    )
                }
                
            case .pollResults:
                if let event = selectedEvent {
                    PollResultsView(
                        event: event,
                        repository: repository,
                        onDateConfirmed: { _ in
                            currentView = .eventDetail
                        }
                    )
                    .overlay(
                        VStack {
                            HStack {
                                Button(action: {
                                    currentView = .eventDetail
                                }) {
                                    Image(systemName: "chevron.left")
                                        .font(.system(size: 20, weight: .semibold))
                                        .foregroundColor(.white)
                                        .padding(12)
                                        .background(Color.white.opacity(0.1))
                                        .clipShape(Circle())
                                }
                                
                                Spacer()
                            }
                            .padding(.top, 60)
                            .padding(.horizontal, 20)
                            
                            Spacer()
                        }
                    )
                }
            }
        }
    }
}

enum AppView {
    case eventList
    case eventCreation
    case eventDetail
    case participantManagement
    case pollVoting
    case pollResults
}

struct EventListView: View {
    let repository: EventRepository
    let onEventSelected: (Event) -> Void
    let onCreateEvent: () -> Void
    
    @State private var events: [Event] = []
    @State private var isLoading = true
    
    var body: some View {
        VStack(spacing: 0) {
            // Header
            VStack(spacing: 8) {
                Text("Wakeve")
                    .font(.system(size: 32, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
                
                Text("Collaborative Event Planning")
                    .font(.system(size: 16, weight: .medium, design: .rounded))
                    .foregroundColor(.white.opacity(0.8))
            }
            .padding(.top, 60)
            
            ScrollView {
                VStack(spacing: 24) {
                    // Create Event Card
                    VStack(spacing: 20) {
                        Button(action: onCreateEvent) {
                            VStack(spacing: 16) {
                                Image(systemName: "plus.circle.fill")
                                    .font(.system(size: 48))
                                    .foregroundColor(.blue)
                                
                                Text("Create New Event")
                                    .font(.system(size: 18, weight: .semibold, design: .rounded))
                                    .foregroundColor(.white)
                                
                                Text("Start planning your collaborative event")
                                    .font(.system(size: 14, design: .rounded))
                                    .foregroundColor(.white.opacity(0.8))
                                    .multilineTextAlignment(.center)
                            }
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 40)
                        }
                    }
                    .padding(24)
                    .liquidGlass(cornerRadius: 24, opacity: 0.9)
                    
                    // Events List
                    if isLoading {
                        VStack(spacing: 16) {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle(tint: .white))
                            
                            Text("Loading events...")
                                .font(.system(size: 14, design: .rounded))
                                .foregroundColor(.white.opacity(0.8))
                        }
                        .padding(.vertical, 40)
                    } else if events.isEmpty {
                        VStack(spacing: 16) {
                            Image(systemName: "calendar.badge.exclamationmark")
                                .font(.system(size: 48))
                                .foregroundColor(.white.opacity(0.6))
                            
                            Text("No events yet")
                                .font(.system(size: 16, weight: .medium, design: .rounded))
                                .foregroundColor(.white.opacity(0.8))
                            
                            Text("Create your first event to get started")
                                .font(.system(size: 14, design: .rounded))
                                .foregroundColor(.white.opacity(0.6))
                                .multilineTextAlignment(.center)
                        }
                        .padding(.vertical, 40)
                    } else {
                        VStack(spacing: 16) {
                            ForEach(events, id: \.id) { event in
                                EventCard(event: event, onTap: {
                                    onEventSelected(event)
                                })
                            }
                        }
                    }
                    
                    Spacer(minLength: 40)
                }
                .padding(.horizontal, 20)
            }
        }
        .onAppear {
            loadEvents()
        }
    }
    
    private func loadEvents() {
        // In a real app, this would load from a service
        // For now, just simulate loading
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            events = repository.getAllEvents()
            isLoading = false
        }
    }
}

struct EventCard: View {
    let event: Event
    let onTap: () -> Void
    
    var body: some View {
        Button(action: onTap) {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(event.title)
                            .font(.system(size: 18, weight: .semibold, design: .rounded))
                            .foregroundColor(.white)
                        
                        if !event.description.isEmpty {
                            Text(event.description)
                                .font(.system(size: 14, design: .rounded))
                                .foregroundColor(.white.opacity(0.8))
                                .lineLimit(2)
                        }
                    }
                    
                    Spacer()
                    
                    VStack(alignment: .trailing, spacing: 4) {
                        Text(statusText)
                            .font(.system(size: 12, weight: .medium, design: .rounded))
                            .foregroundColor(statusColor)
                            .padding(.horizontal, 8)
                            .padding(.vertical, 4)
                            .background(statusColor.opacity(0.2))
                            .cornerRadius(8)
                        
                        Text("\(event.participants.count) participants")
                            .font(.system(size: 12, design: .rounded))
                            .foregroundColor(.white.opacity(0.6))
                    }
                }
                
                HStack {
                    Image(systemName: "calendar")
                        .font(.system(size: 14))
                        .foregroundColor(.white.opacity(0.6))
                    
                    Text("\(event.proposedSlots.count) time slots")
                        .font(.system(size: 14, design: .rounded))
                        .foregroundColor(.white.opacity(0.8))
                    
                    Spacer()
                    
                    Image(systemName: "clock")
                        .font(.system(size: 14))
                        .foregroundColor(.white.opacity(0.6))
                    
                    Text(formatDeadline(event.deadline))
                        .font(.system(size: 14, design: .rounded))
                        .foregroundColor(.white.opacity(0.8))
                }
            }
            .padding(20)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .liquidGlass(cornerRadius: 16, opacity: 0.9)
    }
    
    private var statusText: String {
        switch event.status {
        case .draft: return "Draft"
        case .polling: return "Polling"
        case .confirmed: return "Confirmed"
        default: return "Unknown"
        }
    }
    
    private var statusColor: Color {
        switch event.status {
        case .draft: return .orange
        case .polling: return .blue
        case .confirmed: return .green
        default: return .gray
        }
    }
    
    private func formatDeadline(_ deadlineString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: deadlineString) {
            let formatter = DateFormatter()
            formatter.dateStyle = .short
            formatter.timeStyle = .none
            return formatter.string(from: date)
        }
        return deadlineString
    }
}

struct EventDetailView: View {
    let event: Event
    let repository: EventRepository
    let onManageParticipants: () -> Void
    let onVote: () -> Void
    let onViewResults: () -> Void
    let onBack: () -> Void
    
    var body: some View {
        VStack(spacing: 0) {
            // Header with back button
            HStack {
                Button(action: onBack) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.white)
                        .padding(12)
                        .background(Color.white.opacity(0.1))
                        .clipShape(Circle())
                }
                
                Spacer()
                
                Text(event.title)
                    .font(.system(size: 20, weight: .bold, design: .rounded))
                    .foregroundColor(.white)
                
                Spacer()
            }
            .padding(.top, 60)
            .padding(.horizontal, 20)
            
            ScrollView {
                VStack(spacing: 24) {
                    // Event Overview Card
                    VStack(spacing: 16) {
                        HStack {
                            Image(systemName: statusIcon)
                                .font(.system(size: 20))
                                .foregroundColor(statusColor)
                            
                            Text("Status: \(statusText)")
                                .font(.system(size: 18, weight: .semibold, design: .rounded))
                                .foregroundColor(.white)
                            
                            Spacer()
                        }
                        
                        if !event.description.isEmpty {
                            Text(event.description)
                                .font(.system(size: 14, design: .rounded))
                                .foregroundColor(.white.opacity(0.8))
                                .multilineTextAlignment(.leading)
                        }
                        
                        HStack {
                            Image(systemName: "person.2")
                                .font(.system(size: 16))
                                .foregroundColor(.white.opacity(0.6))
                            
                            Text("\(event.participants.count) participants")
                                .font(.system(size: 14, design: .rounded))
                                .foregroundColor(.white.opacity(0.8))
                            
                            Spacer()
                            
                            Image(systemName: "clock")
                                .font(.system(size: 16))
                                .foregroundColor(.white.opacity(0.6))
                            
                            Text("Deadline: \(formatDeadline(event.deadline))")
                                .font(.system(size: 14, design: .rounded))
                                .foregroundColor(.white.opacity(0.8))
                        }
                    }
                    .padding(24)
                    .liquidGlass(cornerRadius: 24, opacity: 0.9)
                    
                    // Action Buttons
                    VStack(spacing: 16) {
                        if event.status == EventStatus.draft {
                            ActionButton(
                                title: "Manage Participants",
                                subtitle: "Add or remove participants",
                                icon: "person.2.circle",
                                color: .green,
                                action: onManageParticipants
                            )
                            
                            ActionButton(
                                title: "Start Poll",
                                subtitle: "Begin voting on time slots",
                                icon: "chart.bar.xaxis",
                                color: .blue,
                                action: onManageParticipants // This will trigger poll start
                            )
                        } else if event.status == EventStatus.polling {
                            ActionButton(
                                title: "Vote on Time Slots",
                                subtitle: "Cast your vote for available times",
                                icon: "checkmark.circle",
                                color: .orange,
                                action: onVote
                            )
                            
                            ActionButton(
                                title: "View Results",
                                subtitle: "See current poll results",
                                icon: "chart.bar",
                                color: .purple,
                                action: onViewResults
                            )
                        } else if event.status == EventStatus.confirmed {
                            ActionButton(
                                title: "View Final Results",
                                subtitle: "See confirmed event details",
                                icon: "checkmark.circle.fill",
                                color: .green,
                                action: onViewResults
                            )
                        }
                    }
                    
                    Spacer(minLength: 40)
                }
                .padding(.horizontal, 20)
            }
        }
    }
    
    private var statusIcon: String {
        switch event.status {
        case .draft: return "doc"
        case .polling: return "chart.bar"
        case .confirmed: return "checkmark.circle"
        default: return "questionmark.circle"
        }
    }
    
    private var statusColor: Color {
        switch event.status {
        case .draft: return .orange
        case .polling: return .blue
        case .confirmed: return .green
        default: return .gray
        }
    }
    
    private var statusText: String {
        switch event.status {
        case .draft: return "Draft"
        case .polling: return "Polling Active"
        case .confirmed: return "Date Confirmed"
        default: return "Unknown"
        }
    }
    
    private func formatDeadline(_ deadlineString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: deadlineString) {
            let formatter = DateFormatter()
            formatter.dateStyle = .medium
            formatter.timeStyle = .short
            return formatter.string(from: date)
        }
        return deadlineString
    }
}

struct ActionButton: View {
    let title: String
    let subtitle: String
    let icon: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 16) {
                Image(systemName: icon)
                    .font(.system(size: 24))
                    .foregroundColor(color)
                    .frame(width: 40, height: 40)
                    .background(color.opacity(0.1))
                    .cornerRadius(20)
                
                VStack(alignment: .leading, spacing: 2) {
                    Text(title)
                        .font(.system(size: 16, weight: .semibold, design: .rounded))
                        .foregroundColor(.white)
                    
                    Text(subtitle)
                        .font(.system(size: 14, design: .rounded))
                        .foregroundColor(.white.opacity(0.8))
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.white.opacity(0.6))
            }
            .padding(20)
        }
        .liquidGlass(cornerRadius: 16, opacity: 0.9)
    }
}

struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
    }
}