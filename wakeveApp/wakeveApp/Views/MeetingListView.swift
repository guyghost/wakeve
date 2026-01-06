import SwiftUI

/**
 * MeetingListView - Virtual meetings list screen for iOS (Phase 4, Refactored).
 *
 * Displays:
 * - List of scheduled virtual meetings (Zoom, Meet, FaceTime)
 * - Meeting platform, time, participants
 * - Full Liquid Glass design system implementation
 * - Matches Android MeetingListScreen functionality
 *
 * Architecture: Functional Core & Imperative Shell
 * - Pure functions for meeting status mapping
 * - View state managed in Imperative Shell
 */

// MARK: - View

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
                backgroundGradient

                contentView
            }
            .navigationTitle("Réunions virtuelles")
            #if os(iOS)
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onBack) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(.primary)
                            .accessibilityLabel("Retour")
                    }
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onCreateMeeting) {
                        Image(systemName: "plus")
                            .foregroundColor(.primary)
                            .accessibilityLabel("Créer une réunion")
                    }
                }
            }
            #endif
        }
        .onAppear(perform: loadMeetings)
    }

    // MARK: - Background

    private var backgroundGradient: some View {
        LinearGradient(
            gradient: Gradient(colors: [
                Color.wakevPrimary.opacity(0.08),
                Color.wakevAccent.opacity(0.08)
            ]),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        .ignoresSafeArea()
    }

    // MARK: - Content View

    @ViewBuilder
    private var contentView: some View {
        if isLoading {
            loadingView
        } else if meetings.isEmpty {
            emptyStateView
        } else {
            meetingListView
        }
    }

    // MARK: - Loading View

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.5)
                .tint(.wakevPrimary)

            Text(NSLocalizedString("loading_label", comment: "Loading text"))
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
    }

    // MARK: - Empty State View

    private var emptyStateView: some View {
        VStack(spacing: 24) {
            Spacer()

            Image(systemName: "video.slash")
                .font(.system(size: 72))
                .foregroundColor(.secondary.opacity(0.4))

            VStack(spacing: 8) {
                Text(NSLocalizedString("no_meetings_title", comment: "No meetings title"))
                    .font(.title2.weight(.semibold))
                    .foregroundColor(.primary)

                Text(NSLocalizedString("plan_meetings", comment: "Plan meetings text"))
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }

            LiquidGlassButton(
                title: NSLocalizedString("create_meeting_button", comment: "Create meeting button"),
                style: .primary,
                action: onCreateMeeting
            )
            .padding(.top, 8)

            Spacer()
        }
    }

    // MARK: - Meeting List View

    private var meetingListView: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                createMeetingButton

                ForEach(meetings) { meeting in
                    MeetingListItem(meeting: meeting)
                        .onTapGesture {
                            onMeetingTap(meeting.id)
                        }
                        .accessibilityLabel(meetingAccessibilityLabel(meeting))
                        .accessibilityHint("Appuyez pour voir les détails de la réunion")
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
    }

    // MARK: - Create Meeting Button

    private var createMeetingButton: some View {
        LiquidGlassButton(
            title: "Créer une réunion",
            style: .primary,
            action: onCreateMeeting
        )
        .padding(.bottom, 8)
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

    // MARK: - Accessibility Helpers

    private func meetingAccessibilityLabel(_ meeting: MeetingModel) -> String {
        "\(meeting.title), \(meeting.statusText), \(meeting.participantCount) participants via \(meeting.platformName)"
    }
}

// MARK: - Meeting List Item

struct MeetingListItem: View {
    let meeting: MeetingModel

    var body: some View {
        LiquidGlassCard(cornerRadius: 16, padding: 16) {
            HStack(alignment: .top, spacing: 14) {
                platformIcon

                meetingContent

                Spacer()

                chevronIcon
            }
        }
        .accessibilityElement(children: .combine)
    }

    // MARK: - Platform Icon

    private var platformIcon: some View {
        ZStack {
            Circle()
                .fill(meeting.platformColor.opacity(0.15))
                .frame(width: 48, height: 48)

            Image(systemName: meeting.platformIcon)
                .font(.system(size: 20, weight: .medium))
                .foregroundColor(meeting.platformColor)
        }
    }

    // MARK: - Meeting Content

    private var meetingContent: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(meeting.title)
                .font(.headline.weight(.semibold))
                .foregroundColor(.primary)
                .lineLimit(1)

            HStack(spacing: 6) {
                Image(systemName: "calendar")
                    .font(.caption)
                    .foregroundColor(.secondary)

                Text(meeting.dateTime)
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }

            HStack(spacing: 6) {
                Image(systemName: "person.2")
                    .font(.caption)
                    .foregroundColor(.secondary)

                Text("\(meeting.participantCount) participants")
                    .font(.subheadline)
                    .foregroundColor(.secondary)
            }
            .padding(.top, 2)

            meetingStatusBadge
        }
    }

    // MARK: - Meeting Status Badge

    private var meetingStatusBadge: some View {
        HStack(spacing: 6) {
            LiquidGlassBadge(
                text: meeting.statusText,
                style: meeting.statusBadgeStyle
            )
        }
        .padding(.top, 6)
    }

    // MARK: - Chevron Icon

    private var chevronIcon: some View {
        Image(systemName: "chevron.right")
            .font(.system(size: 14, weight: .medium))
            .foregroundColor(.secondary.opacity(0.6))
            .padding(.top, 4)
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

    var statusColor: Color {
        switch status {
        case .upcoming: return .wakevWarning
        case .inProgress: return .wakevSuccess
        case .completed: return .secondary
        }
    }

    var statusText: String {
        switch status {
        case .upcoming: return "À venir"
        case .inProgress: return "En cours"
        case .completed: return "Terminée"
        }
    }

    var statusBadgeStyle: LiquidGlassBadgeStyle {
        switch status {
        case .upcoming: return .warning
        case .inProgress: return .success
        case .completed: return .default
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
    ),
    MeetingModel(
        id: "3",
        title: "Point hebdomadaire",
        platform: .zoom,
        dateTime: "22 Jan 2024, 09:00",
        participantCount: 8,
        status: .inProgress
    ),
    MeetingModel(
        id: "4",
        title: "Formation équipe",
        platform: .facetime,
        dateTime: "10 Jan 2024, 15:00",
        participantCount: 4,
        status: .completed
    )
]

// MARK: - Preview

#Preview {
    MeetingListView(
        userId: "user-123",
        onMeetingTap: { _ in },
        onCreateMeeting: { },
        onBack: { }
    )
}
