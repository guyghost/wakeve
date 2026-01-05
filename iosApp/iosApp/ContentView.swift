import SwiftUI
import Shared

// MARK: - UserDefaults Helpers

struct UserDefaultsKeys {
    static let hasCompletedOnboarding = "hasCompletedOnboarding"
}

func hasCompletedOnboarding() -> Bool {
    return UserDefaults.standard.bool(forKey: UserDefaultsKeys.hasCompletedOnboarding)
}

func markOnboardingComplete() {
    UserDefaults.standard.set(true, forKey: UserDefaultsKeys.hasCompletedOnboarding)
}

struct ContentView: View {
    @EnvironmentObject var authStateManager: AuthStateManager
    @EnvironmentObject var authService: AuthenticationService
    @State private var hasSeenGetStarted = false
    @State private var hasOnboarded = false

    var body: some View {
        ZStack {
            if authStateManager.isAuthenticated {
                if let user = authStateManager.currentUser {
                    if hasOnboarded {
                        AuthenticatedView(userId: user.id)
                    } else {
                        OnboardingView(onOnboardingComplete: {
                            markOnboardingComplete()
                            hasOnboarded = true
                        })
                    }
                } else {
                    ErrorView(message: "Authentication error: no user data", onRetry: {
                        Task {
                            authStateManager.checkAuthStatus()
                        }
                    })
                }
            } else {
                if hasSeenGetStarted {
                    LoginView()
                } else {
                    ModernGetStartedView(onGetStarted: {
                        withAnimation(.easeInOut(duration: 0.3)) {
                            hasSeenGetStarted = true
                        }
                    })
                    .transition(.opacity)
                }
            }
        }
        .onAppear {
            hasOnboarded = hasCompletedOnboarding()
        }
    }
}

// MARK: - Error View

struct ErrorView: View {
    let message: String
    let onRetry: () -> Void

    var body: some View {
        ZStack {
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

            VStack(spacing: 24) {
                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 60))
                    .foregroundColor(.red.opacity(0.8))

                Text("Something went wrong")
                    .font(.title2)
                    .fontWeight(.semibold)
                    .foregroundColor(.white)

                Text(message)
                    .font(.body)
                    .foregroundColor(.white.opacity(0.8))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)

                Button(action: onRetry) {
                    Text("Try Again")
                        .font(.headline)
                        .foregroundColor(.white)
                        .padding(.horizontal, 32)
                        .padding(.vertical, 16)
                        .background(Color.blue)
                        .cornerRadius(12)
                }
            }
        }
    }
}

// MARK: - Authenticated View

struct AuthenticatedView: View {
    let userId: String
    @State private var selectedTab: WakevTab = .home
    @State private var currentView: AppView = .eventList
    @State private var selectedEvent: Event?
    // Use persistent database-backed repository instead of in-memory mock
    private let repository: EventRepositoryInterface = RepositoryProvider.shared.repository
    @State private var showEventCreationSheet = false
    
    // New state variables for PRD features
    @State private var showScenarioList = false
    @State private var showBudgetDetail = false
    @State private var showAccommodation = false
    @State private var showMealPlanning = false
    @State private var showEquipmentChecklist = false
    @State private var showActivityPlanning = false
    @State private var selectedScenario: Scenario_?

    var body: some View {
        // Using native iOS TabView which automatically adopts Liquid Glass on iOS 26+
        WakevTabBarContainer(
            selectedTab: $selectedTab,
            home: { homeTabContent },
            inbox: { inboxTabContent },
            explore: { exploreTabContent },
            profile: { profileTabContent }
        )
        .sheet(isPresented: $showEventCreationSheet) {
            CreateEventView(
                userId: userId,
                repository: repository,
                onEventCreated: { eventId in
                    // Navigate to participant management after creation
                    if let event = repository.getEvent(id: eventId) {
                        selectedEvent = event
                        currentView = .participantManagement
                    }
                }
            )
        }
    }
    
    // MARK: - Home Tab
    
    @ViewBuilder
    private var homeTabContent: some View {
        switch currentView {
        case .eventList:
            ModernHomeView(
                userId: userId,
                repository: repository,
                onEventSelected: { event in
                    selectedEvent = event
                    currentView = .eventDetail
                },
                onCreateEvent: {
                    // Show bottom sheet instead of navigating
                    showEventCreationSheet = true
                }
            )
            
        case .eventCreation:
            // Legacy full-screen creation (keep for backwards compatibility)
            // Using DraftEventWizardView wrapped in CreateEventView
            CreateEventView(
                userId: userId,
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
                ModernEventDetailView(
                    event: event,
                    userId: userId,
                    repository: repository,
                    onBack: {
                        currentView = .eventList
                    },
                    onVote: {
                        currentView = .pollVoting
                    },
                    onManageParticipants: {
                        currentView = .participantManagement
                    }
                )
            }
            
        case .participantManagement:
            if let event = selectedEvent {
                ModernParticipantManagementView(
                    event: event,
                    repository: repository,
                    onParticipantsUpdated: {
                        // Refresh the event data
                        if let updatedEvent = repository.getEvent(id: event.id) {
                            selectedEvent = updatedEvent
                        }
                    },
                    onBack: {
                        currentView = .eventDetail
                    }
                )
            }
            
        case .pollVoting:
            if let event = selectedEvent {
                ModernPollVotingView(
                    event: event,
                    repository: repository,
                    participantId: userId,
                    onVoteSubmitted: {
                        currentView = .eventDetail
                    },
                    onBack: {
                        currentView = .eventDetail
                    }
                )
            }
            
        case .pollResults:
            if let event = selectedEvent {
                ModernPollResultsView(
                    event: event,
                    repository: repository,
                    userId: userId,
                    onDateConfirmed: { _ in
                        currentView = .eventDetail
                    },
                    onBack: {
                        currentView = .eventDetail
                    }
                )
            }
            
        // MARK: - New PRD Features Navigation Cases
            
        case .scenarioList:
            if let event = selectedEvent {
                ScenarioListView(
                    event: event,
                    participantId: userId,
                    onScenarioTap: { scenario in
                        selectedScenario = scenario
                        // TODO: Navigate to scenario detail
                    },
                    onCompareTap: {
                        // TODO: Navigate to scenario comparison
                    },
                    onBack: {
                        currentView = .eventDetail
                    }
                )
            }
            
        case .scenarioComparison:
            if let event = selectedEvent {
                ScenarioComparisonView(
                    event: event,
                    repository: ScenarioRepository(db: DatabaseProvider.shared.getDatabase(factory: IosDatabaseFactory())),
                    onBack: {
                        currentView = .scenarioList
                    }
                )
            }
            
        case .budgetOverview:
            if let event = selectedEvent {
                BudgetOverviewView(
                    event: event,
                    repository: BudgetRepository(db: DatabaseProvider.shared.getDatabase(factory: IosDatabaseFactory())),
                    onBack: {
                        currentView = .eventDetail
                    },
                    onViewDetails: {
                        // TODO: Navigate to budget detail
                    }
                )
            }
            
        case .accommodation:
            if let event = selectedEvent {
                AccommodationView(
                    eventId: event.id,
                    currentUserId: userId,
                    currentUserName: "Current User" // TODO: Get actual user name
                )
                .navigationBarTitleDisplayMode(.inline)
            }
            
        case .mealPlanning:
            if let event = selectedEvent {
                // TODO: Re-enable MealPlanningView when Shared types are properly integrated
                Text("Meal Planning - Coming Soon")
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .equipmentChecklist:
            if let event = selectedEvent {
                EquipmentChecklistView(
                    eventId: event.id,
                    currentUserId: userId,
                    currentUserName: "Current User" // TODO: Get actual user name
                )
            }
            
        case .activityPlanning:
            if let event = selectedEvent {
                ActivityPlanningView(
                    eventId: event.id,
                    currentUserId: userId,
                    currentUserName: "Current User" // TODO: Get actual user name
                )
            }
            
        default:
            // Fallback to event list
            ModernHomeView(
                userId: userId,
                repository: repository,
                onEventSelected: { event in
                    selectedEvent = event
                    currentView = .eventDetail
                },
                onCreateEvent: {
                    showEventCreationSheet = true
                }
            )
        }
    }
    
    // MARK: - Inbox Tab
    
    @ViewBuilder
    private var inboxTabContent: some View {
        InboxView(
            userId: userId,
            onBack: { /* Inbox is main tab, no back action needed */ }
        )
    }
    
    // MARK: - Explore Tab
    
    @ViewBuilder
    private var exploreTabContent: some View {
        ExploreTabView()
    }
    
    // MARK: - Profile Tab
    
    @ViewBuilder
    private var profileTabContent: some View {
        ProfileTabView(userId: userId)
    }
}



// MARK: - Explore Tab View

// ProfileTabView is now in its own file: Views/ProfileTabView.swift

enum AppView {
    case eventList
    case eventCreation
    case eventDetail
    case participantManagement
    case pollVoting
    case pollResults
    // New PRD features
    case scenarioList
    case scenarioDetail
    case scenarioComparison
    case budgetOverview
    case budgetDetail
    case accommodation
    case mealPlanning
    case equipmentChecklist
    case activityPlanning
    // Phase 4 - Meetings & Communication
    case inbox
    case meetingList
    case meetingDetail
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
                        if event.status.name == "DRAFT" {
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
                        } else if event.status.name == "POLLING" {
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
                        } else if event.status.name == "CONFIRMED" {
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
