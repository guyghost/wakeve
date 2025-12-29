import SwiftUI
import Shared

/// Modern event detail view inspired by Apple Invites
/// Features: Large hero image, Going/Not Going/Maybe buttons, participant list
struct ModernEventDetailView: View {
    let event: Event
    let userId: String
    let repository: EventRepositoryInterface
    let onBack: () -> Void
    let onVote: () -> Void
    let onManageParticipants: () -> Void
    
    // PRD feature navigation callbacks
    var onScenarioPlanning: (() -> Void)?
    var onBudgetOverview: (() -> Void)?
    var onAccommodation: (() -> Void)?
    var onMealPlanning: (() -> Void)?
    var onEquipmentChecklist: (() -> Void)?
    var onActivityPlanning: (() -> Void)?
    
    @State private var userResponse: RSVPResponse = .maybe
    @State private var showingHostOptions = false
    
    var body: some View {
        ZStack {
            ScrollView {
                VStack(spacing: 0) {
                    // Hero Image Section
                    HeroImageSection(event: event)
                    
                    // Event Details Card
                    LiquidGlassCard(cornerRadius: 30, padding: 20) {
                        VStack(alignment: .leading, spacing: 24) {
                        // Title and Date
                        VStack(alignment: .leading, spacing: 8) {
                            Text(event.title)
                                .font(.title.weight(.bold))
                                .foregroundColor(.primary)
                            
                            if let finalDate = event.finalDate {
                                HStack(spacing: 8) {
                                    Image(systemName: "calendar")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(.secondary)
                                    
                                    Text(formatEventDate(finalDate))
                                        .font(.subheadline.weight(.medium))
                                        .foregroundColor(.primary)
                                }
                            } else {
                                HStack(spacing: 8) {
                                    Image(systemName: "clock")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(.secondary)
                                    
                                    Text("Vote by \(formatDeadline(event.deadline))")
                                        .font(.subheadline.weight(.medium))
                                        .foregroundColor(.secondary)
                                }
                            }
                            
                            // Location (if available)
                            if !event.description.isEmpty {
                                HStack(spacing: 8) {
                                    Image(systemName: "mappin.circle")
                                        .font(.system(size: 16, weight: .medium))
                                        .foregroundColor(.secondary)
                                    
                                    Text(event.description)
                                        .font(.body)
                                        .foregroundColor(.primary)
                                }
                                .padding(.top, 4)
                            }
                        }
                        
                        // RSVP Buttons (if not host and polling)
                        if event.status == .polling && event.organizerId != userId {
                            RSVPButtonsSection(
                                userResponse: $userResponse,
                                onVote: onVote
                            )
                        }
                        
                        // Host Options
                        if event.organizerId == userId {
                            HostActionsSection(
                                event: event,
                                onManageParticipants: onManageParticipants,
                                onViewResults: onVote
                            )
                        }
                        
                        // PRD Feature Buttons based on event status
                        PRDFeatureButtonsSection(
                            event: event,
                            onScenarioPlanning: onScenarioPlanning,
                            onBudgetOverview: onBudgetOverview,
                            onAccommodation: onAccommodation,
                            onMealPlanning: onMealPlanning,
                            onEquipmentChecklist: onEquipmentChecklist,
                            onActivityPlanning: onActivityPlanning
                        )
                        
                        // Calendar Integration Card
                        CalendarIntegrationCard(
                            event: event,
                            userId: userId,
                            onAddToCalendar: {
                                // Handle add to calendar
                            },
                            onShareInvitation: {
                                // Handle share invitation
                            }
                        )
                        
                        // Hosted by Section
                        HostedBySection(hostId: event.organizerId)
                        
                        Divider()
                            .padding(.vertical, 8)
                        
                        // Description Section
                        if !event.description.isEmpty {
                            VStack(alignment: .leading, spacing: 8) {
                                Text("About")
                                    .font(.system(size: 20, weight: .semibold))
                                    .foregroundColor(.primary)
                                
                                Text(event.description)
                                    .font(.system(size: 17))
                                    .foregroundColor(.secondary)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                            .padding(.vertical, 8)
                        }
                        
                        // Participants Section
                        ParticipantsSection(
                            participants: event.participants,
                            goingCount: event.participants.count
                        )
                        
                        Spacer()
                            .frame(height: 40)
                        }
                    }
                    .offset(y: -30)
                }
            }
            .ignoresSafeArea(edges: .top)
            
            // Back Button Overlay
            VStack {
                HStack(spacing: 12) {
                    Button(action: onBack) {
                        Image(systemName: "xmark")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.primary)
                            .frame(width: 36, height: 36)
                            .background(.thinMaterial)
                            .clipShape(Circle())
                    }
                    .accessibilityLabel("Fermer")
                    .padding(.leading, 16)
                    .padding(.top, 12)
                    
                    Spacer()
                    
                    // More options button
                    Button(action: { showingHostOptions.toggle() }) {
                        Image(systemName: "ellipsis")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.primary)
                            .frame(width: 36, height: 36)
                            .background(.thinMaterial)
                            .clipShape(Circle())
                    }
                    .accessibilityLabel("Plus d'options")
                    .padding(.trailing, 16)
                    .padding(.top, 12)
                }
                
                Spacer()
            }
        }
        .background(Color(.systemGroupedBackground))
    }
    
    private func formatEventDate(_ dateString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: dateString) {
            let formatter = DateFormatter()
            formatter.dateFormat = "EEEE, MMMM d, h:mm a"
            return formatter.string(from: date)
        }
        return dateString
    }
    
    private func formatDeadline(_ deadlineString: String) -> String {
        if let date = ISO8601DateFormatter().date(from: deadlineString) {
            let formatter = DateFormatter()
            formatter.dateFormat = "MMM d, h:mm a"
            return formatter.string(from: date)
        }
        return deadlineString
    }
}

// MARK: - Hero Image Section

struct HeroImageSection: View {
    let event: Event
    
    var body: some View {
        ZStack(alignment: .bottomLeading) {
            // Background Gradient
            LinearGradient(
                colors: gradientColors,
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .frame(height: 350)
            
            // Pattern Overlay
            GeometryReader { geometry in
                Image(systemName: "calendar")
                    .font(.system(size: 250))
                    .foregroundColor(.white.opacity(0.1))
                    .offset(x: geometry.size.width * 0.5, y: 50)
            }
            
            // Status Badge
            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    StatusBadgeLarge(status: event.status)
                }
                .padding(20)
                
                Spacer()
            }
        }
        .frame(height: 350)
    }
    
    private var gradientColors: [Color] {
        switch event.status {
        case .draft:
            return [Color.orange, Color.red]
        case .polling:
            return [Color.blue, Color.purple]
        case .confirmed:
            return [Color.green, Color.teal]
        default:
            return [Color.gray, Color.gray.opacity(0.7)]
        }
    }
}

struct StatusBadgeLarge: View {
    let status: EventStatus
    
    var body: some View {
        HStack(spacing: 8) {
            Image(systemName: statusIcon)
                .font(.system(size: 14, weight: .semibold))
            
            Text(statusText)
                .font(.system(size: 15, weight: .semibold))
        }
        .foregroundColor(.white)
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
        .background(.ultraThinMaterial)
        .continuousCornerRadius(22)
    }
    
    private var statusText: String {
        switch status {
        case .draft: return "Hosting"
        case .polling: return "Polling"
        case .confirmed: return "Confirmed"
        default: return ""
        }
    }
    
    private var statusIcon: String {
        switch status {
        case .draft: return "crown.fill"
        case .polling: return "chart.bar.fill"
        case .confirmed: return "checkmark.circle.fill"
        default: return "questionmark"
        }
    }
}

// MARK: - RSVP Buttons Section

enum RSVPResponse {
    case going
    case notGoing
    case maybe
}

struct RSVPButtonsSection: View {
    @Binding var userResponse: RSVPResponse
    let onVote: () -> Void
    
    var body: some View {
        VStack(spacing: 12) {
            HStack(spacing: 12) {
                RSVPButton(
                    title: "Going",
                    icon: "checkmark",
                    isSelected: userResponse == .going,
                    color: .green,
                    action: {
                        userResponse = .going
                        onVote()
                    }
                )
                
                RSVPButton(
                    title: "Not Going",
                    icon: "xmark",
                    isSelected: userResponse == .notGoing,
                    color: .red,
                    action: {
                        userResponse = .notGoing
                    }
                )
                
                RSVPButton(
                    title: "Maybe",
                    icon: "questionmark",
                    isSelected: userResponse == .maybe,
                    color: .orange,
                    action: {
                        userResponse = .maybe
                    }
                )
            }
        }
        .padding(.vertical, 8)
    }
}

struct RSVPButton: View {
    let title: String
    let icon: String
    let isSelected: Bool
    let color: Color
    let action: () -> Void
    
    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                ZStack {
                    Circle()
                        .fill(isSelected ? color : Color(.tertiarySystemFill))
                        .frame(width: 48, height: 48)
                    
                    Image(systemName: icon)
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(isSelected ? .white : .secondary)
                }
                
                Text(title)
                    .font(.system(size: 13, weight: isSelected ? .semibold : .regular))
                    .foregroundColor(isSelected ? color : .secondary)
            }
            .frame(maxWidth: .infinity)
        }
    }
}

// MARK: - Host Actions Section

struct HostActionsSection: View {
    let event: Event
    let onManageParticipants: () -> Void
    let onViewResults: () -> Void
    
    var body: some View {
        VStack(spacing: 12) {
            HostActionButton(
                title: "Manage Participants",
                icon: "person.2.fill",
                color: .blue,
                action: onManageParticipants
            )
            
            if event.status == .polling {
                HostActionButton(
                    title: "View Results",
                    icon: "chart.bar.fill",
                    color: .purple,
                    action: onViewResults
                )
            }
        }
        .padding(.vertical, 8)
    }
}

struct HostActionButton: View {
    let title: String
    let icon: String
    let color: Color
    let action: () -> Void
    
    var body: some View {
        liquidBody
    }
}

// MARK: - Modified HostActionButton with LiquidGlassCard.thin
extension HostActionButton {
    var liquidBody: some View {
        Button(action: action) {
            LiquidGlassCard.thin(cornerRadius: 12, padding: 0) {
                HStack {
                    Image(systemName: icon)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(color)
                        .frame(width: 28)
                    
                    Text(title)
                        .font(.system(size: 17, weight: .medium))
                        .foregroundColor(.primary)
                    
                    Spacer()
                    
                    Image(systemName: "chevron.right")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(.secondary)
                }
                .padding(16)
            }
        }
    }
}

// MARK: - PRD Feature Buttons Section

struct PRDFeatureButtonsSection: View {
    let event: Event
    var onScenarioPlanning: (() -> Void)?
    var onBudgetOverview: (() -> Void)?
    var onAccommodation: (() -> Void)?
    var onMealPlanning: (() -> Void)?
    var onEquipmentChecklist: (() -> Void)?
    var onActivityPlanning: (() -> Void)?
    
    var body: some View {
        VStack(spacing: 12) {
            // Scenario Planning - Available in COMPARING and CONFIRMED states
            if event.status == .comparing || event.status == .confirmed {
                HostActionButton(
                    title: "Scenario Planning",
                    icon: "list.bullet.rectangle.portrait",
                    color: .blue,
                    action: {
                        onScenarioPlanning?()
                    }
                )
            }
            
            // Budget Overview - Available in CONFIRMED and ORGANIZING states
            if event.status == .confirmed || event.status == .organizing {
                HostActionButton(
                    title: "Budget Overview",
                    icon: "dollarsign.circle",
                    color: .green,
                    action: {
                        onBudgetOverview?()
                    }
                )
            }
            
            // Accommodation - Available in ORGANIZING state
            if event.status == .organizing {
                HostActionButton(
                    title: "Accommodation",
                    icon: "house.fill",
                    color: .purple,
                    action: {
                        onAccommodation?()
                    }
                )
                
                HostActionButton(
                    title: "Meal Planning",
                    icon: "fork.knife",
                    color: .orange,
                    action: {
                        onMealPlanning?()
                    }
                )
                
                HostActionButton(
                    title: "Equipment Checklist",
                    icon: "bag.fill",
                    color: .pink,
                    action: {
                        onEquipmentChecklist?()
                    }
                )
                
                HostActionButton(
                    title: "Activity Planning",
                    icon: "figure.walk",
                    color: .red,
                    action: {
                        onActivityPlanning?()
                    }
                )
            }
        }
        .padding(.vertical, 8)
    }
}

// MARK: - Hosted By Section

struct HostedBySection: View {
    let hostId: String
    
    private var hostInitial: String {
        String(hostId.prefix(1).uppercased())
    }
    
    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(
                    LinearGradient(
                        colors: [Color.blue, Color.purple],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 48, height: 48)
                .overlay(
                    Text(hostInitial)
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundColor(.white)
                )
            
            VStack(alignment: .leading, spacing: 2) {
                Text("Hosted by \(hostId)")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(.primary)
                
                Text("Organizer")
                    .font(.system(size: 15))
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
        .padding(.vertical, 8)
    }
}

// MARK: - Participants Section

struct ParticipantsSection: View {
    let participants: [String]
    let goingCount: Int
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("\(goingCount) Going")
                .font(.system(size: 20, weight: .semibold))
                .foregroundColor(.primary)
            
            if participants.isEmpty {
                Text("No participants yet")
                    .font(.system(size: 17))
                    .foregroundColor(.secondary)
                    .padding(.vertical, 20)
            } else {
                VStack(spacing: 12) {
                    ForEach(participants.prefix(10), id: \.self) { participant in
                        ModernParticipantRow(participantId: participant)
                    }
                    
                    if participants.count > 10 {
                        HStack {
                            Text("+ \(participants.count - 10) more")
                                .font(.system(size: 17, weight: .medium))
                                .foregroundColor(.blue)
                            
                            Spacer()
                            
                            Image(systemName: "chevron.right")
                                .font(.system(size: 14, weight: .semibold))
                                .foregroundColor(.secondary)
                        }
                        .padding(.vertical, 12)
                    }
                }
            }
        }
        .padding(.vertical, 8)
    }
}

struct ModernParticipantRow: View {
    let participantId: String
    
    private var participantInitial: String {
        String(participantId.prefix(1).uppercased())
    }
    
    private var avatarColor: Color {
        let colors: [Color] = [.red, .orange, .yellow, .green, .blue, .purple, .pink]
        let hash = abs(participantId.hashValue)
        return colors[hash % colors.count]
    }
    
    var body: some View {
        HStack(spacing: 12) {
            Circle()
                .fill(avatarColor)
                .frame(width: 44, height: 44)
                .overlay(
                    Text(participantInitial)
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundColor(.white)
                )
            
            VStack(alignment: .leading, spacing: 2) {
                Text(participantId)
                    .font(.system(size: 17, weight: .medium))
                    .foregroundColor(.primary)
                
                Text("Going")
                    .font(.system(size: 15))
                    .foregroundColor(.secondary)
            }
            
            Spacer()
        }
    }
}