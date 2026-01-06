import SwiftUI
import Shared
import Foundation

// MARK: - Calendar Status Badge

/// Badge style for calendar status
enum CalendarBadgeStyle {
    case notInCalendar
    case inCalendar
    case loading
    case error
    
    var badgeText: String {
        switch self {
        case .notInCalendar:
            return NSLocalizedString("calendar_not_added", comment: "Not in calendar")
        case .inCalendar:
            return NSLocalizedString("calendar_added", comment: "Added to calendar")
        case .loading:
            return NSLocalizedString("calendar_checking", comment: "Checking calendar")
        case .error:
            return NSLocalizedString("calendar_error", comment: "Calendar error")
        }
    }
    
    var icon: String {
        switch self {
        case .notInCalendar:
            return "calendar.badge.plus"
        case .inCalendar:
            return "calendar.badge.checkmark"
        case .loading:
            return "calendar"
        case .error:
            return "calendar.badge.exclamationmark"
        }
    }
    
    var badgeStyle: LiquidGlassBadgeStyle {
        switch self {
        case .notInCalendar:
            return .info
        case .inCalendar:
            return .success
        case .loading:
            return .warning
        case .error:
            return .warning
        }
    }
    
    var iconColor: Color {
        switch self {
        case .notInCalendar:
            return .iOSSystemBlue
        case .inCalendar:
            return .iOSSystemGreen
        case .loading:
            return .iOSSecondaryLabel
        case .error:
            return .iOSSystemRed
        }
    }
}

// MARK: - Calendar Integration Card

/// Calendar Integration Card for event details
/// Displays calendar status, add to calendar, and share invitation actions
struct CalendarIntegrationCard: View {
    let event: Event
    let userId: String
    let onAddToCalendar: () -> Void
    let onShareInvitation: () -> Void
    
    @State private var isAddingToCalendar = false
    @State private var isShareInvitationLoading = false
    @State private var calendarStatus: CalendarBadgeStyle = .notInCalendar
    @State private var showError = false
    @State private var errorMessage = ""
    
    var body: some View {
        VStack(spacing: 16) {
            statusHeader
            actionsSection
        }
        .alert(NSLocalizedString("calendar_error_title", comment: "Calendar error title"), isPresented: $showError) {
            Button(NSLocalizedString("ok", comment: "OK button"), role: .cancel) { }
        } message: {
            Text(errorMessage)
        }
        .onAppear {
            checkCalendarStatus()
        }
    }
    
    // MARK: - Status Header
    
    private var statusHeader: some View {
        HStack(spacing: 12) {
            statusIcon
            statusText
            Spacer()
            loadingIndicator
        }
        .padding(16)
        .background(backgroundGlassCard)
    }
    
    private var statusIcon: some View {
        Image(systemName: calendarStatus.icon)
            .font(.system(size: 18, weight: .semibold))
            .foregroundColor(calendarStatus.iconColor)
            .frame(width: 32, height: 32)
            .background(
                Circle()
                    .fill(calendarStatus.iconColor.opacity(0.15))
            )
    }
    
    private var statusText: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(NSLocalizedString("calendar", comment: "Calendar label"))
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(.wakevTextPrimary)
            
            LiquidGlassBadge(
                text: calendarStatus.badgeText,
                icon: calendarStatus.icon,
                style: calendarStatus.badgeStyle
            )
        }
    }
    
    private var loadingIndicator: some View {
        Group {
            if calendarStatus == .loading {
                ProgressView()
                    .scaleEffect(0.9)
                    .tint(.wakevPrimary)
            }
        }
    }
    
    private var backgroundGlassCard: some View {
        RoundedRectangle(cornerRadius: 12)
            .fill(
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color.wakevBackgroundLight.opacity(0.8),
                        Color.wakevSurfaceLight.opacity(0.6)
                    ]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .overlay(
                RoundedRectangle(cornerRadius: 12)
                    .stroke(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.white.opacity(0.3),
                                Color.white.opacity(0.1)
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1
                    )
            )
    }
    
    // MARK: - Actions Section
    
    private var actionsSection: some View {
        VStack(spacing: 12) {
            LiquidGlassDivider(style: .subtle)
            
            addToCalendarButton
            shareInvitationButton
        }
        .padding(16)
        .background(
            LiquidGlassCard(cornerRadius: 16, padding: 0) {
                EmptyView()
            }
            .liquidGlass(cornerRadius: 16, opacity: 0.7, intensity: 0.9)
        )
    }
    
    private var addToCalendarButton: some View {
        LiquidGlassButton(
            title: NSLocalizedString("add_to_calendar", comment: "Add to calendar button"),
            style: .primary
        ) {
            handleAddToCalendar()
        }
        .disabled(isAddingToCalendar || isShareInvitationLoading)
        .opacity(isAddingToCalendar ? 0.7 : 1.0)
        .overlay(alignment: .trailing) {
            if isAddingToCalendar {
                ProgressView()
                    .scaleEffect(0.8)
                    .tint(.white)
                    .padding(.trailing, 16)
            }
        }
        .accessibilityLabel(NSLocalizedString("add_to_calendar", comment: "Add to calendar button"))
        .accessibilityHint(NSLocalizedString("add_to_calendar_hint", comment: "Adds event to device calendar"))
    }
    
    private var shareInvitationButton: some View {
        LiquidGlassButton(
            title: NSLocalizedString("share_invitation", comment: "Share invitation button"),
            style: .secondary
        ) {
            handleShareInvitation()
        }
        .disabled(isAddingToCalendar || isShareInvitationLoading)
        .opacity(isShareInvitationLoading ? 0.7 : 1.0)
        .overlay(alignment: .trailing) {
            if isShareInvitationLoading {
                ProgressView()
                    .scaleEffect(0.8)
                    .tint(.wakevPrimary)
                    .padding(.trailing, 16)
            }
        }
        .accessibilityLabel(NSLocalizedString("share_invitation", comment: "Share invitation button"))
        .accessibilityHint(NSLocalizedString("share_invitation_hint", comment: "Shares event invitation via ICS file"))
    }
    
    // MARK: - Private Methods
    
    private func handleAddToCalendar() {
        isAddingToCalendar = true
        
        Task {
            do {
                try await simulateCalendarIntegration()
                calendarStatus = .inCalendar
                onAddToCalendar()
            } catch {
                calendarStatus = .error
                errorMessage = NSLocalizedString("calendar_error_message", comment: "Failed to add event to calendar")
                showError = true
            }
            
            isAddingToCalendar = false
        }
    }
    
    private func handleShareInvitation() {
        isShareInvitationLoading = true
        
        Task {
            do {
                let icsContent = generateICSContent()
                let fileURL = try await createICSFile(icsContent: icsContent)
                try await presentShareSheet(fileURL: fileURL)
                onShareInvitation()
            } catch {
                errorMessage = NSLocalizedString("share_error_message", comment: "Failed to share invitation")
                showError = true
            }
            
            isShareInvitationLoading = false
        }
    }
    
    private func checkCalendarStatus() {
        calendarStatus = .notInCalendar
    }
}

// MARK: - Pure Functions (Functional Core)

/// Simulates calendar integration delay
/// This function is pure and testable without UI dependencies
private func simulateCalendarIntegration() async throws {
    try await Task.sleep(nanoseconds: 1_000_000_000)
}

/// Generates ICS content for event invitation
/// Pure function for generating calendar data
private func generateICSContent(event: Event) -> String {
    let dateFormatter = ISO8601DateFormatter()
    let now = dateFormatter.string(from: Date())
    let startDate = event.finalDate ?? now
    let endDate = event.finalDate ?? now
    
    let title = event.title.replacingOccurrences(of: "\n", with: "\\n")
    let description = (event.description ?? "").replacingOccurrences(of: "\n", with: "\\n")
    
    return """
    BEGIN:VCALENDAR
    VERSION:2.0
    PRODID:-//Wakeve//Wakeve Event//FR
    CALSCALE:GREGORIAN
    METHOD:REQUEST
    BEGIN:VEVENT
    UID:\(event.id)@wakeve.app
    DTSTAMP:\(now)
    DTSTART:\(startDate)
    DTEND:\(endDate)
    SUMMARY:\(title)
    DESCRIPTION:\(description)
    ORGANIZER:mailto:\(event.organizerId)
    END:VEVENT
    BEGIN:VALARM
    TRIGGER:-P1DT090000
    DESCRIPTION:Rappel: Événement dans 1 jour
    ACTION:DISPLAY
    END:VALARM
    END:VCALENDAR
    """
}

/// Creates a temporary ICS file with the given content
/// Returns the file URL for sharing
private func createICSFile(icsContent: String) async throws -> URL {
    let tempDirectory = FileManager.default.temporaryDirectory
    let fileName = "wakeve_invitation.ics"
    let fileURL = tempDirectory.appendingPathComponent(fileName)
    try icsContent.write(to: fileURL, atomically: true, encoding: .utf8)
    return fileURL
}

/// Presents the share sheet with the ICS file
/// Side effect: UI presentation
private func presentShareSheet(fileURL: URL) async throws {
    let activityViewController = UIActivityViewController(
        activityItems: [fileURL],
        applicationActivities: nil
    )
    
    guard let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
          let window = windowScene.windows.first,
          let rootViewController = window.rootViewController else {
        throw ShareError.presentationFailed
    }
    
    if let popover = activityViewController.popoverPresentationController {
        popover.sourceView = window
        popover.sourceRect = CGRect(
            x: window.bounds.midX,
            y: window.bounds.midY,
            width: 0,
            height: 0
        )
        popover.permittedArrowDirections = []
    }
    
    await rootViewController.present(activityViewController, animated: true)
}

/// Errors that can occur during share operation
private enum ShareError: LocalizedError {
    case presentationFailed
    
    var errorDescription: String? {
        switch self {
        case .presentationFailed:
            return NSLocalizedString("share_error_presentation", comment: "Failed to present share sheet")
        }
    }
}

// MARK: - Preview

#Preview {
    let sampleEvent = Event(
        id: "event-1",
        title: "Team Meeting",
        description: "Q4 Planning Session",
        organizerId: "user-1",
        participants: ["user-2", "user-3"],
        proposedSlots: [],
        deadline: ISO8601DateFormatter().string(from: Date().addingTimeInterval(86400)),
        status: .confirmed,
        finalDate: ISO8601DateFormatter().string(from: Date().addingTimeInterval(172800)),
        createdAt: ISO8601DateFormatter().string(from: Date()),
        updatedAt: ISO8601DateFormatter().string(from: Date()),
        eventType: .teamBuilding,
        eventTypeCustom: nil,
        minParticipants: nil,
        maxParticipants: nil,
        expectedParticipants: nil,
        heroImageUrl: nil
    )
    
    CalendarIntegrationCard(
        event: sampleEvent,
        userId: "user-1",
        onAddToCalendar: { print("Added to calendar") },
        onShareInvitation: { print("Shared invitation") }
    )
    .padding()
    .background(Color(.systemGroupedBackground))
}
