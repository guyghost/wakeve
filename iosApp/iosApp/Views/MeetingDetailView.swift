import SwiftUI

/**
 * MeetingDetailView - Meeting details and join screen for iOS (Phase 4).
 * 
 * Displays:
 * - Meeting details (title, platform, time, participants)
 * - Join meeting button with link
 * - Cancel/reschedule options
 * - Liquid Glass design system
 * - Matches Android MeetingDetailScreen functionality
 */
struct MeetingDetailView: View {
    let meetingId: String
    let userId: String
    let onBack: () -> Void
    let onJoinMeeting: (String) -> Void
    
    @State private var meeting: MeetingDetailModel?
    @State private var isLoading = false
    @State private var showCancelConfirmation = false
    
    var body: some View {
        NavigationView {
            ZStack {
                // Background gradient
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color.blue.opacity(0.1),
                        Color.purple.opacity(0.1)
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
                    }
                }
            }
            #endif
        }
        .onAppear(perform: loadMeeting)
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.5)
            Text("Chargement...")
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
    }
    
    // MARK: - Error View
    
    private var errorView: some View {
        VStack(spacing: 20) {
            Image(systemName: "exclamationmark.triangle")
                .font(.system(size: 64))
                .foregroundColor(.red.opacity(0.5))
            
            Text("Réunion non trouvée")
                .font(.title3)
                .fontWeight(.semibold)
            
            Button(action: onBack) {
                Text("Retour")
                    .font(.headline)
                    .foregroundColor(.white)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(Color.blue)
                    .cornerRadius(12)
            }
        }
    }
    
    // MARK: - Meeting Detail View
    
    private func meetingDetailView(meeting: MeetingDetailModel) -> some View {
        ScrollView {
            VStack(spacing: 20) {
                // Platform card
                VStack(spacing: 16) {
                    Image(systemName: meeting.platformIcon)
                        .font(.system(size: 48))
                        .foregroundColor(meeting.platformColor)
                        .frame(width: 80, height: 80)
                        .background(meeting.platformColor.opacity(0.1))
                        .cornerRadius(20)
                    
                    Text(meeting.title)
                        .font(.title2)
                        .fontWeight(.bold)
                        .multilineTextAlignment(.center)
                    
                    Text(meeting.platformName)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
                .padding(24)
                .frame(maxWidth: .infinity)
                .background(.ultraThinMaterial)
                .cornerRadius(20)
                
                // Meeting info
                VStack(spacing: 16) {
                    InfoRow(icon: "calendar", title: "Date et heure", value: meeting.dateTime)
                    Divider()
                    InfoRow(icon: "person.2", title: "Participants", value: "\(meeting.participants.count) personnes")
                    Divider()
                    InfoRow(icon: "clock", title: "Durée", value: meeting.duration)
                }
                .padding(20)
                .background(.ultraThinMaterial)
                .cornerRadius(16)
                
                // Participants list
                VStack(alignment: .leading, spacing: 12) {
                    Text("Participants")
                        .font(.headline)
                        .padding(.horizontal, 4)
                    
                    ForEach(meeting.participants, id: \.self) { participant in
                        HStack {
                            Circle()
                                .fill(Color.blue)
                                .frame(width: 32, height: 32)
                                .overlay(
                                    Text(String(participant.prefix(1)))
                                        .foregroundColor(.white)
                                        .font(.caption)
                                )
                            
                            Text(participant)
                                .font(.subheadline)
                            
                            Spacer()
                        }
                        .padding(12)
                        .background(.ultraThinMaterial)
                        .cornerRadius(12)
                    }
                }
                .padding(20)
                .background(.ultraThinMaterial)
                .cornerRadius(16)
                
                // Join button
                if meeting.canJoin {
                    Button(action: { onJoinMeeting(meeting.meetingUrl) }) {
                        HStack {
                            Image(systemName: "video.fill")
                            Text("Rejoindre la réunion")
                        }
                        .font(.headline)
                        .foregroundColor(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(Color.blue)
                        .cornerRadius(16)
                    }
                }
                
                // Cancel button
                Button(action: { showCancelConfirmation = true }) {
                    Text("Annuler la réunion")
                        .font(.headline)
                        .foregroundColor(.red)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 16)
                        .background(.ultraThinMaterial)
                        .cornerRadius(16)
                }
            }
            .padding(20)
        }
        .alert("Annuler la réunion?", isPresented: $showCancelConfirmation) {
            Button("Annuler", role: .cancel) {}
            Button("Confirmer", role: .destructive) {
                cancelMeeting()
            }
        } message: {
            Text("Cette action ne peut pas être annulée.")
        }
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
        // TODO: Implement cancel meeting
        onBack()
    }
}

// MARK: - Info Row

struct InfoRow: View {
    let icon: String
    let title: String
    let value: String
    
    var body: some View {
        HStack {
            Image(systemName: icon)
                .foregroundColor(.secondary)
                .frame(width: 24)
            
            Text(title)
                .font(.subheadline)
                .foregroundColor(.secondary)
            
            Spacer()
            
            Text(value)
                .font(.subheadline)
                .fontWeight(.medium)
        }
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
    
    var platformIcon: String {
        switch platform {
        case .zoom: return "video.fill"
        case .googleMeet: return "video.badge.plus"
        case .facetime: return "video.fill"
        }
    }
    
    var platformColor: Color {
        switch platform {
        case .zoom: return .blue
        case .googleMeet: return .green
        case .facetime: return .purple
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
    participants: ["Alice Dupont", "Bob Martin", "Charlie Dubois", "Diana Laurent"],
    meetingUrl: "https://zoom.us/j/123456789",
    canJoin: true
)
