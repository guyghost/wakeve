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
        ZStack {
            // Fond dégradé
            LinearGradient(
                gradient: Gradient(colors: [
                    LiquidGlassDesign.backgroundColor,
                    LiquidGlassDesign.backgroundColor.opacity(0.95)
                ]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .ignoresSafeArea()
            
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
        ScrollView {
            VStack(spacing: LiquidGlassDesign.spacingL) {
                // Header
                HStack {
                    VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingXS) {
                        Text("Wakeve")
                            .font(LiquidGlassDesign.titleL)
                        Text("Planifiez vos événements ensemble")
                            .font(LiquidGlassDesign.bodySmall)
                            .foregroundColor(.secondary)
                    }
                    
                    Spacer()
                    
                    Button(action: onCreateEvent) {
                        Image(systemName: "plus.circle.fill")
                            .font(.system(size: 32))
                            .foregroundColor(LiquidGlassDesign.accentBlue)
                    }
                }
                .padding(LiquidGlassDesign.spacingL)
                
                if events.isEmpty {
                    VStack(spacing: LiquidGlassDesign.spacingL) {
                        Image(systemName: "calendar.badge.plus")
                            .font(.system(size: 64))
                            .foregroundColor(LiquidGlassDesign.accentBlue.opacity(0.5))
                        
                        VStack(spacing: LiquidGlassDesign.spacingS) {
                            Text("Aucun événement")
                                .font(LiquidGlassDesign.titleM)
                            
                            Text("Créez votre premier événement pour commencer")
                                .font(LiquidGlassDesign.bodySmall)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                        }
                        
                        Button(action: onCreateEvent) {
                            Text("Créer un événement")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(LiquidGlassButtonStyle())
                        .padding(.horizontal, LiquidGlassDesign.spacingL)
                    }
                    .frame(maxHeight: .infinity)
                    .padding(.vertical, LiquidGlassDesign.spacingXXL)
                    .padding(.horizontal, LiquidGlassDesign.spacingL)
                } else {
                    VStack(spacing: LiquidGlassDesign.spacingM) {
                        ForEach(events, id: \.id) { event in
                            EventRow(
                                event: event,
                                onSelect: { onSelectEvent(event) }
                            )
                        }
                    }
                    .padding(.horizontal, LiquidGlassDesign.spacingL)
                }
            }
            .padding(.vertical, LiquidGlassDesign.spacingL)
        }
    }
}

struct EventRow: View {
    let event: Event
    let onSelect: () -> Void
    
    var body: some View {
        Button(action: onSelect) {
            HStack(spacing: LiquidGlassDesign.spacingM) {
                // Icon
                Image(systemName: statusIcon(event.status))
                    .font(.system(size: 20))
                    .foregroundColor(statusColor(event.status))
                    .frame(width: 40, height: 40)
                    .background(
                        Circle()
                            .fill(statusColor(event.status).opacity(0.1))
                    )
                
                // Content
                VStack(alignment: .leading, spacing: LiquidGlassDesign.spacingXS) {
                    Text(event.title)
                        .font(LiquidGlassDesign.titleS)
                        .foregroundColor(.primary)
                    
                    if !event.description.isEmpty {
                        Text(event.description)
                            .font(LiquidGlassDesign.bodySmall)
                            .foregroundColor(.secondary)
                            .lineLimit(1)
                    }
                    
                    HStack(spacing: LiquidGlassDesign.spacingS) {
                        Text(statusString(event.status))
                            .font(LiquidGlassDesign.caption)
                            .foregroundColor(statusColor(event.status))
                        
                        Spacer()
                        
                        Text("\(event.participants.count) participants")
                            .font(LiquidGlassDesign.caption)
                            .foregroundColor(.secondary)
                    }
                }
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.secondary)
            }
            .liquidGlassCard()
        }
        .buttonStyle(.plain)
    }
    
    private func statusIcon(_ status: EventStatus) -> String {
        switch status {
        case .DRAFT:
            return "pencil.circle.fill"
        case .POLLING:
            return "checkmark.circle.fill"
        case .CONFIRMED:
            return "calendar.circle.fill"
        @unknown default:
            return "circle.fill"
        }
    }
    
    private func statusColor(_ status: EventStatus) -> Color {
        switch status {
        case .DRAFT:
            return LiquidGlassDesign.warningOrange
        case .POLLING:
            return LiquidGlassDesign.accentBlue
        case .CONFIRMED:
            return LiquidGlassDesign.successGreen
        @unknown default:
            return .gray
        }
    }
    
    private func statusString(_ status: EventStatus) -> String {
        switch status {
        case .DRAFT:
            return "Brouillon"
        case .POLLING:
            return "Vote en cours"
        case .CONFIRMED:
            return "Confirmé"
        @unknown default:
            return "Inconnu"
        }
    }
}

#Preview {
    AppView()
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
