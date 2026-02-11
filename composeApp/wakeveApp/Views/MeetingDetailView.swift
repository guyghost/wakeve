import SwiftUI

/**
 * MeetingDetailView - Meeting details and join screen for iOS (Phase 4).
 *
 * Refactored to use Liquid Glass design system components:
 * - LiquidGlassCard for section containers
 * - LiquidGlassButton for actions
 * - LiquidGlassBadge for status indicators
 * - LiquidGlassDivider for separators
 * - LiquidGlassListItem for meeting information
 *
 * Displays:
 * - Meeting details (title, platform, time, participants)
 * - Join meeting button with link
 * - Cancel/reschedule options
 * - Add to calendar action
 * - Liquid Glass design system
 * - Matches Android MeetingDetailScreen functionality
 */
struct MeetingDetailView: View {
    let meetingId: String
    let userId: String
    let onBack: () -> Void
    let onJoinMeeting: (String) -> Void
    let onAddToCalendar: ((MeetingDetailModel) -> Void)?
    let onCancelMeeting: (() -> Void)?
    
    @State private var meeting: MeetingDetailModel?
    @State private var isLoading = false
    @State private var showCancelConfirmation = false
    @State private var showCalendarConfirmation = false
    
    init(
        meetingId: String,
        userId: String,
        onBack: @escaping () -> Void,
        onJoinMeeting: @escaping (String) -> Void,
        onAddToCalendar: ((MeetingDetailModel) -> Void)? = nil,
        onCancelMeeting: (() -> Void)? = nil
    ) {
        self.meetingId = meetingId
        self.userId = userId
        self.onBack = onBack
        self.onJoinMeeting = onJoinMeeting
        self.onAddToCalendar = onAddToCalendar
        self.onCancelMeeting = onCancelMeeting
    }
    
    var body: some View {
        NavigationView {
            ZStack {
                // Background gradient
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color.wakevPrimaryLight.opacity(0.1),
                        Color.wakevAccentLight.opacity(0.1)
                    ]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                .ignoresSafeArea()
                
                if isLoading {
                    loadingView
                } else if let meeting = meeting {
                    meetingDetailView(meeting: meeting)
                } else {
                    errorView
                }
            }
            .navigationTitle("Détails de la réunion")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(.primary)
                            .accessibilityLabel("Retour")
                    }
                }
            }
            #endif
        }
        .onAppear(perform: loadMeeting)
        .alert("Annuler la réunion?", isPresented: $showCancelConfirmation) {
            Button("Annuler", role: .cancel) {}
            Button("Confirmer", role: .destructive) {
                cancelMeeting()
            }
        } message: {
            Text("Cette action ne peut pas être annulée.")
        }
        .alert("Ajouter au calendrier?", isPresented: $showCalendarConfirmation) {
            Button("Annuler", role: .cancel) {}
            Button("Ajouter") {
                if let meeting = meeting {
                    onAddToCalendar?(meeting)
                }
            }
        } message: {
            Text("Cette réunion sera ajoutée à votre calendrier.")
        }
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.5)
                .tint(.wakevPrimary)
            Text("Chargement...")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .accessibilityIdentifier("loadingView")
    }
    
    // MARK: - Error View
    
    private var errorView: some View {
        LiquidGlassCard(cornerRadius: 20, padding: 24) {
            VStack(spacing: 20) {
                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 64))
                    .foregroundColor(.wakevError)
                
                Text("Réunion non trouvée")
                    .font(.title3)
                    .fontWeight(.semibold)
                
                LiquidGlassButton(title: "Retour", style: .primary) {
                    onBack()
                }
            }
        }
        .padding(.horizontal, 32)
        .accessibilityIdentifier("errorView")
    }
    
    // MARK: - Meeting Detail View
    
    private func meetingDetailView(meeting: MeetingDetailModel) -> some View {
        ScrollView {
            VStack(spacing: 20) {
                // Platform card - Hero section
                platformCard(meeting: meeting)
                
                // Meeting information - Using LiquidGlassListItem
                meetingInfoCard(meeting: meeting)
                
                // Participants section
                participantsCard(participants: meeting.participants)
                
                // Action buttons
                actionButtons(meeting: meeting)
                
                Spacer(minLength: 20)
            }
            .padding(20)
        }
        .accessibilityIdentifier("meetingDetailView")
    }
    
    // MARK: - Platform Card
    
    private func platformCard(meeting: MeetingDetailModel) -> some View {
        LiquidGlassCard(cornerRadius: 20, padding: 24) {
            VStack(spacing: 16) {
                // Platform icon with badge
                ZStack(alignment: .topTrailing) {
                    Image(systemName: meeting.platformIcon)
                        .font(.system(size: 48))
                        .foregroundColor(meeting.platformColor)
                        .frame(width: 80, height: 80)
                        .background(meeting.platformColor.opacity(0.1))
                        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                    
                    LiquidGlassBadge(
                        text: meeting.platformName,
                        style: .info
                    )
                }
                
                // Title
                Text(meeting.title)
                    .font(.title2)
                    .fontWeight(.bold)
                    .multilineTextAlignment(.center)
                    .foregroundColor(.primary)
                
                // Status badge
                if meeting.canJoin {
                    LiquidGlassBadge(
                        text: "Prêt à rejoindre",
                        icon: "checkmark.circle.fill",
                        style: .success
                    )
                }
            }
            .padding(.vertical, 8)
        }
        .accessibilityIdentifier("platformCard")
    }
    
    // MARK: - Meeting Info Card
    
    private func meetingInfoCard(meeting: MeetingDetailModel) -> some View {
        VStack(spacing: 0) {
            // Date and time
            LiquidGlassListItem(
                title: "Date et heure",
                subtitle: meeting.dateTime,
                icon: "calendar",
                iconColor: meeting.platformColor
            ) {
                EmptyView()
            }
            .accessibilityIdentifier("dateTimeRow")
            
            LiquidGlassDivider(style: .subtle)
            
            // Participants count
            LiquidGlassListItem(
                title: "Participants",
                subtitle: "\(meeting.participants.count) personnes",
                icon: "person.2",
                iconColor: .wakevAccent
            ) {
                EmptyView()
            }
            .accessibilityIdentifier("participantsRow")
            
            LiquidGlassDivider(style: .subtle)
            
            // Duration
            LiquidGlassListItem(
                title: "Durée",
                subtitle: meeting.duration,
                icon: "clock",
                iconColor: .wakevWarning
            ) {
                EmptyView()
            }
            .accessibilityIdentifier("durationRow")
        }
        .accessibilityIdentifier("meetingInfoCard")
    }
    
    // MARK: - Participants Card
    
    private func participantsCard(participants: [String]) -> some View {
        LiquidGlassCard(cornerRadius: 16, padding: 20) {
            VStack(alignment: .leading, spacing: 16) {
                // Header with badge
                HStack {
                    Text("Participants")
                        .font(.headline)
                        .foregroundColor(.primary)
                    
                    Spacer()
                    
                    LiquidGlassBadge(
                        text: "\(participants.count)",
                        style: .accent
                    )
                }
                
                // Participant list
                VStack(spacing: 12) {
                    ForEach(Array(participants.enumerated()), id: \.offset) { index, participant in
                        participantRow(
                            name: participant,
                            isOrganizer: index == 0,
                            status: participantStatus(for: index)
                        )
                    }
                }
            }
        }
        .accessibilityIdentifier("participantsCard")
    }
    
    // MARK: - Participant Row
    
    private func participantRow(name: String, isOrganizer: Bool, status: ParticipantStatus) -> some View {
        HStack(spacing: 12) {
            // Avatar with initials
            ZStack {
                Circle()
                    .fill(avatarColor(for: name))
                    .frame(width: 40, height: 40)
                
                Text(String(name.prefix(1)))
                    .font(.headline)
                    .foregroundColor(.white)
            }
            
            // Name and role
            VStack(alignment: .leading, spacing: 2) {
                HStack(spacing: 6) {
                    Text(name)
                        .font(.subheadline)
                        .fontWeight(.medium)
                        .foregroundColor(.primary)
                    
                    if isOrganizer {
                        LiquidGlassBadge(text: "Organisateur", style: .info)
                    }
                }
                
                // Status indicator
                HStack(spacing: 4) {
                    Circle()
                        .fill(statusColor(for: status))
                        .frame(width: 8, height: 8)
                    
                    Text(statusText(for: status))
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            
            Spacer()
            
            // Status badge
            statusBadge(for: status)
        }
        .padding(12)
        .background(Color.wakevSurfaceLight)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
    
    // MARK: - Participant Status Helpers
    
    private enum ParticipantStatus {
        case confirmed, pending, tentative
    }
    
    private func participantStatus(for index: Int) -> ParticipantStatus {
        switch index % 3 {
        case 0: return .confirmed
        case 1: return .pending
        default: return .tentative
        }
    }
    
    private func statusColor(for status: ParticipantStatus) -> Color {
        switch status {
        case .confirmed: return .wakevSuccess
        case .pending: return .wakevWarning
        case .tentative: return .wakevAccent
        }
    }
    
    private func statusText(for status: ParticipantStatus) -> String {
        switch status {
        case .confirmed: return "Confirmé"
        case .pending: return "En attente"
        case .tentative: return "Peut-être"
        }
    }
    
    @ViewBuilder
    private func statusBadge(for status: ParticipantStatus) -> some View {
        switch status {
        case .confirmed:
            LiquidGlassBadge(text: "", icon: "checkmark.circle.fill", style: .success)
        case .pending:
            LiquidGlassBadge(text: "", icon: "clock.fill", style: .warning)
        case .tentative:
            LiquidGlassBadge(text: "", icon: "questionmark.circle.fill", style: .accent)
        }
    }
    
    private func avatarColor(for name: String) -> Color {
        let colors: [Color] = [.blue, .purple, .green, .orange, .pink, .teal]
        let index = abs(name.hashValue) % colors.count
        return colors[index]
    }
    
    // MARK: - Action Buttons
    
    private func actionButtons(meeting: MeetingDetailModel) -> some View {
        VStack(spacing: 12) {
            // Join button (primary action)
            if meeting.canJoin {
                LiquidGlassButton(title: "Rejoindre la réunion", style: .primary) {
                    onJoinMeeting(meeting.meetingUrl)
                }
                .accessibilityLabel("Rejoindre la réunion")
                .accessibilityHint("Ouvre le lien de la réunion")
            }
            
            // Add to calendar button (if available)
            if onAddToCalendar != nil {
                LiquidGlassButton(title: "Ajouter au calendrier", style: .secondary) {
                    showCalendarConfirmation = true
                }
                .accessibilityLabel("Ajouter au calendrier")
                .accessibilityHint("Ajoute cette réunion à votre calendrier")
            }
            
            // Cancel button (destructive)
            if onCancelMeeting != nil {
                LiquidGlassButton(title: "Annuler la réunion", style: .text) {
                    showCancelConfirmation = true
                }
                .foregroundColor(.wakevError)
                .accessibilityLabel("Annuler la réunion")
                .accessibilityHint("Annule et supprime cette réunion")
            }
        }
        .accessibilityIdentifier("actionButtons")
    }
    
    // MARK: - Actions
    
    private func loadMeeting() {
        isLoading = true
        
        // TODO: Load from repository
        // For now, show sample data
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            meeting = sampleMeetingDetail
            isLoading = false
        }
    }
    
    private func cancelMeeting() {
        onCancelMeeting?()
        onBack()
    }
}

// MARK: - Supporting Types

struct MeetingDetailModel {
    let id: String
    let title: String
    let platform: DetailPlatform
    let dateTime: String
    let duration: String
    let participants: [String]
    let meetingUrl: String
    let canJoin: Bool
    let isOrganizer: Bool
    
    var platformIcon: String {
        switch platform {
        case .zoom: return "video.fill"
        case .googleMeet: return "video.badge.plus"
        case .facetime: return "video.fill"
        }
    }
    
    var platformColor: Color {
        switch platform {
        case .zoom: return .wakevPrimary
        case .googleMeet: return .wakevSuccess
        case .facetime: return .wakevAccent
        }
    }
    
    var platformName: String {
        switch platform {
        case .zoom: return "Zoom"
        case .googleMeet: return "Google Meet"
        case .facetime: return "FaceTime"
        }
    }
}

enum DetailPlatform {
    case zoom, googleMeet, facetime
}

// MARK: - Sample Data

private let sampleMeetingDetail = MeetingDetailModel(
    id: "1",
    title: "Réunion de planification - Week-end ski 2024",
    platform: .zoom,
    dateTime: "15 Jan 2024, 14:00",
    duration: "1 heure",
    participants: [
        "Alice Dupont",
        "Bob Martin",
        "Charlie Dubois",
        "Diana Laurent"
    ],
    meetingUrl: "https://zoom.us/j/123456789",
    canJoin: true,
    isOrganizer: true
)

// MARK: - Preview

struct MeetingDetailView_Previews: PreviewProvider {
    static var previews: some View {
        MeetingDetailView(
            meetingId: "1",
            userId: "user-1",
            onBack: { },
            onJoinMeeting: { _ in },
            onAddToCalendar: { _ in },
            onCancelMeeting: { }
        )
    }
}
