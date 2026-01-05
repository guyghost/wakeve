import SwiftUI

/**
 * MeetingListView - Virtual meetings list screen for iOS (Phase 4).
 * 
 * Displays:
 * - List of scheduled virtual meetings (Zoom, Meet, FaceTime)
 * - Meeting platform, time, participants
 * - Liquid Glass design system
 * - Matches Android MeetingListScreen functionality
 */
struct MeetingListView: View {
    let userId: String
    let onMeetingTap: (String) -> Void
    let onCreateMeeting: () -> Void
    let onBack: () -> Void
    
    @State private var meetings: [MeetingModel] = []
    @State private var isLoading = false
    
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
                
                // Content
                if isLoading {
                    loadingView
                } else if meetings.isEmpty {
                    emptyStateView
                } else {
                    meetingListView
                }
            }
            .navigationTitle("Réunions virtuelles")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(.primary)
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onCreateMeeting) {
                        Image(systemName: "plus")
                            .foregroundColor(.primary)
                    }
                }
            }
            #endif
        }
        .onAppear(perform: loadMeetings)
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.5)
            Text(NSLocalizedString("loading_label", comment: "Loading text"))
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
    }
    
    // MARK: - Empty State View
    
    private var emptyStateView: some View {
        VStack(spacing: 20) {
            Image(systemName: "video.slash")
                .font(.system(size: 64))
                .foregroundColor(.gray.opacity(0.5))
            
            Text(NSLocalizedString("no_meetings_title", comment: "No meetings title"))
                .font(.title3)
                .fontWeight(.semibold)
            
            Text(NSLocalizedString("plan_meetings", comment: "Plan meetings text"))
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
            
            Button(action: onCreateMeeting) {
                HStack {
                    Image(systemName: "plus")
                    Text(NSLocalizedString("create_meeting_button", comment: "Create meeting button"))
                }
                .font(.headline)
                .foregroundColor(.white)
                .padding(.horizontal, 24)
                .padding(.vertical, 12)
                .background(Color.blue)
                .cornerRadius(12)
            }
            .padding(.top, 8)
        }
    }
    
    // MARK: - Meeting List View
    
    private var meetingListView: some View {
        ScrollView {
            LazyVStack(spacing: 12) {
                ForEach(meetings) { meeting in
                    MeetingCard(meeting: meeting)
                        .onTapGesture {
                            onMeetingTap(meeting.id)
                        }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
    }
    
    // MARK: - Actions
    
    private func loadMeetings() {
        isLoading = true
        
        // TODO: Load from repository
        // For now, show sample data
        DispatchQueue.main.asyncAfter(deadline: .now() + 1) {
            meetings = sampleMeetings
            isLoading = false
        }
    }
}

// MARK: - Meeting Card

struct MeetingCard: View {
    let meeting: MeetingModel
    
    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            // Platform icon
            Image(systemName: meeting.platformIcon)
                .font(.system(size: 24))
                .foregroundColor(meeting.platformColor)
                .frame(width: 48, height: 48)
                .background(meeting.platformColor.opacity(0.1))
                .cornerRadius(12)
            
            // Content
            VStack(alignment: .leading, spacing: 6) {
                Text(meeting.title)
                    .font(.headline)
                
                HStack(spacing: 4) {
                    Image(systemName: "calendar")
                        .font(.caption)
                    Text(meeting.dateTime)
                        .font(.subheadline)
                }
                .foregroundColor(.secondary)
                
                HStack(spacing: 4) {
                    Image(systemName: "person.2")
                        .font(.caption)
                    Text("\(meeting.participantCount) participants")
                        .font(.subheadline)
                }
                .foregroundColor(.secondary)
                
                // Status badge
                HStack {
                    Circle()
                        .fill(meeting.statusColor)
                        .frame(width: 8, height: 8)
                    Text(meeting.statusText)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                .padding(.top, 4)
            }
            
            Spacer()
            
            Image(systemName: "chevron.right")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(16)
        .background(.ultraThinMaterial)
        .cornerRadius(16)
    }
}

// MARK: - Supporting Types

struct MeetingModel: Identifiable {
    let id: String
    let title: String
    let platform: MeetingPlatform
    let dateTime: String
    let participantCount: Int
    let status: MeetingStatus
    
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
    
    var statusColor: Color {
        switch status {
        case .upcoming: return .orange
        case .inProgress: return .green
        case .completed: return .gray
        }
    }
    
    var statusText: String {
        switch status {
        case .upcoming: return "À venir"
        case .inProgress: return "En cours"
        case .completed: return "Terminée"
        }
    }
}

enum MeetingPlatform {
    case zoom, googleMeet, facetime
}

enum MeetingStatus {
    case upcoming, inProgress, completed
}

// MARK: - Sample Data

private let sampleMeetings: [MeetingModel] = [
    MeetingModel(
        id: "1",
        title: "Réunion de planification",
        platform: .zoom,
        dateTime: "15 Jan 2024, 14:00",
        participantCount: 5,
        status: .upcoming
    ),
    MeetingModel(
        id: "2",
        title: "Discussion budget",
        platform: .googleMeet,
        dateTime: "20 Jan 2024, 10:00",
        participantCount: 3,
        status: .upcoming
    )
]
