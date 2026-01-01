import SwiftUI
import Shared
import Foundation

/// Calendar Integration Card for event details
/// Displays calendar status, add to calendar, and share invitation actions
struct CalendarIntegrationCard: View {
    let event: Event
    let userId: String
    let onAddToCalendar: () -> Void
    let onShareInvitation: () -> Void
    
    @State private var isAddingToCalendar = false
    @State private var isShareInvitationLoading = false
    @State private var calendarStatus: CalendarStatus = .notInCalendar
    @State private var showError = false
    @State private var errorMessage = ""
    
    enum CalendarStatus {
        case notInCalendar
        case inCalendar
        case loading
        case error
        
        var displayText: String {
            switch self {
            case .notInCalendar:
                return "Not in calendar"
            case .inCalendar:
                return "Added to calendar"
            case .loading:
                return "Checking..."
            case .error:
                return "Error"
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
        
        var iconColor: Color {
            switch self {
            case .notInCalendar:
                return .blue
            case .inCalendar:
                return .green
            case .loading:
                return .gray
            case .error:
                return .red
            }
        }
    }
    
    var body: some View {
        VStack(spacing: 16) {
            // Status Header
            HStack(spacing: 12) {
                Image(systemName: calendarStatus.icon)
                    .font(.system(size: 18, weight: .semibold))
                    .foregroundColor(calendarStatus.iconColor)
                
                VStack(alignment: .leading, spacing: 4) {
                    Text("Calendar")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundColor(.primary)
                    
                    Text(calendarStatus.displayText)
                        .font(.system(size: 13))
                        .foregroundColor(.secondary)
                }
                
                Spacer()
                
                if calendarStatus == .loading {
                    ProgressView()
                        .scaleEffect(0.9)
                }
            }
            .padding(16)
            .glassCard(cornerRadius: 12, material: .ultraThinMaterial)
            
            // Actions
            VStack(spacing: 12) {
                // Add to Calendar Button
                Button(action: handleAddToCalendar) {
                    HStack(spacing: 12) {
                        Image(systemName: "calendar.badge.plus")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.white)
                        
                        Text("Add to Calendar")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.white)
                        
                        Spacer()
                        
                        if isAddingToCalendar {
                            ProgressView()
                                .scaleEffect(0.8)
                                .tint(.white)
                        }
                    }
                    .frame(height: 48)
                    .frame(maxWidth: .infinity)
                    .background(Color.blue)
                    .continuousCornerRadius(12)
                }
                .disabled(isAddingToCalendar || isShareInvitationLoading)
                .opacity(isAddingToCalendar ? 0.7 : 1.0)
                
                // Share Invitation Button
                Button(action: handleShareInvitation) {
                    HStack(spacing: 12) {
                        Image(systemName: "square.and.arrow.up")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.blue)
                        
                        Text("Share Invitation")
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundColor(.blue)
                        
                        Spacer()
                        
                        if isShareInvitationLoading {
                            ProgressView()
                                .scaleEffect(0.8)
                                .tint(.blue)
                        }
                    }
                    .frame(height: 48)
                    .frame(maxWidth: .infinity)
                    .background(Color.blue.opacity(0.1))
                    .continuousCornerRadius(12)
                }
                .disabled(isAddingToCalendar || isShareInvitationLoading)
                .opacity(isShareInvitationLoading ? 0.7 : 1.0)
            }
            .padding(12)
            .glassCard(cornerRadius: 12, material: .thinMaterial)
        }
        .alert("Calendar Error", isPresented: $showError) {
            Button("OK", role: .cancel) { }
        } message: {
            Text(errorMessage)
        }
        .onAppear {
            checkCalendarStatus()
        }
    }
    
    // MARK: - Private Methods
    
    private func handleAddToCalendar() {
        isAddingToCalendar = true
        
        Task {
            do {
                // Simulate calendar integration
                // In production, this would call the CalendarService via Kotlin
                try await Task.sleep(nanoseconds: 1_000_000_000) // 1 second delay
                
                calendarStatus = .inCalendar
                onAddToCalendar()
            } catch {
                calendarStatus = .error
                errorMessage = "Failed to add event to calendar"
                showError = true
            }
            
            isAddingToCalendar = false
        }
    }
    
    private func handleShareInvitation() {
        isShareInvitationLoading = true
        
        Task {
            do {
                // Generate ICS content
                let icsContent = generateICSContent()
                
                // Create temporary file
                let tempDirectory = FileManager.default.temporaryDirectory
                let fileName = "\(event.title.replacingOccurrences(of: " ", with: "_"))_invitation.ics"
                let fileURL = tempDirectory.appendingPathComponent(fileName)
                
                // Write ICS file
                try icsContent.write(to: fileURL, atomically: true, encoding: .utf8)
                
                // Prepare for sharing
                let activityViewController = UIActivityViewController(
                    activityItems: [fileURL],
                    applicationActivities: nil
                )
                
                // Determine the presenting view controller
                if let windowScene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
                   let window = windowScene.windows.first,
                   let rootViewController = window.rootViewController {
                    
                    // Configure popover for iPad
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
                    
                    rootViewController.present(activityViewController, animated: true) {
                        onShareInvitation()
                    }
                }
            } catch {
                errorMessage = "Failed to share invitation: \(error.localizedDescription)"
                showError = true
            }
            
            isShareInvitationLoading = false
        }
    }
    
    private func checkCalendarStatus() {
        // In a real implementation, this would check if the event
        // is already in the native calendar using EventKit
        // For now, we'll default to notInCalendar
        calendarStatus = .notInCalendar
    }
    
    private func generateICSContent() -> String {
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
        expectedParticipants: nil
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
