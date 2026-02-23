import SwiftUI

// MARK: - Data Models

struct DashboardOverview {
    let totalEvents: Int
    let totalParticipants: Int
    let averageParticipants: Double
    let totalVotes: Int
    let totalComments: Int
    let eventsByStatus: [String: Int]
}

struct DashboardEventItem: Identifiable {
    let id: String
    let title: String
    let status: String
    let eventType: String?
    let createdAt: String
    let deadline: String
    let participantCount: Int
    let voteCount: Int
    let commentCount: Int
    let responseRate: Double
}

struct EventDetailedAnalytics {
    let eventId: String
    let title: String
    let status: String
    let voteTimeline: [TimelineEntry]
    let participantTimeline: [TimelineEntry]
    let popularTimeSlots: [PopularTimeSlotItem]
    let pollCompletionRate: Double
    let totalParticipants: Int
    let votedParticipants: Int
}

struct TimelineEntry: Identifiable {
    let id = UUID()
    let date: String
    let count: Int
}

struct PopularTimeSlotItem: Identifiable {
    let id: String
    let startTime: String?
    let endTime: String?
    let timeOfDay: String?
    let yesVotes: Int
    let maybeVotes: Int
    let noVotes: Int
    let totalVotes: Int
}

// MARK: - Organizer Dashboard View

struct OrganizerDashboardView: View {
    @State private var overview = DashboardOverview(
        totalEvents: 12,
        totalParticipants: 87,
        averageParticipants: 7.25,
        totalVotes: 234,
        totalComments: 56,
        eventsByStatus: [
            "DRAFT": 3,
            "POLLING": 2,
            "CONFIRMED": 4,
            "FINALIZED": 3
        ]
    )

    @State private var events: [DashboardEventItem] = [
        DashboardEventItem(id: "1", title: "Anniversaire Marie", status: "CONFIRMED", eventType: "BIRTHDAY", createdAt: "2025-10-01", deadline: "2025-11-15", participantCount: 15, voteCount: 42, commentCount: 8, responseRate: 87.5),
        DashboardEventItem(id: "2", title: "Team Building Q4", status: "POLLING", eventType: "TEAM_BUILDING", createdAt: "2025-10-10", deadline: "2025-11-20", participantCount: 22, voteCount: 38, commentCount: 12, responseRate: 65.0),
        DashboardEventItem(id: "3", title: "Soiree de Noel", status: "DRAFT", eventType: "PARTY", createdAt: "2025-10-15", deadline: "2025-12-20", participantCount: 8, voteCount: 0, commentCount: 3, responseRate: 0.0),
        DashboardEventItem(id: "4", title: "Reunion Projet Alpha", status: "FINALIZED", eventType: "CONFERENCE", createdAt: "2025-09-01", deadline: "2025-10-05", participantCount: 10, voteCount: 28, commentCount: 5, responseRate: 93.0),
        DashboardEventItem(id: "5", title: "Brunch Dominical", status: "CONFIRMED", eventType: "FOOD_TASTING", createdAt: "2025-10-20", deadline: "2025-11-03", participantCount: 6, voteCount: 18, commentCount: 4, responseRate: 100.0)
    ]

    @State private var selectedEvent: DashboardEventItem? = nil
    @State private var showEventDetail = false

    var body: some View {
        ScrollView {
            VStack(spacing: 20) {
                // Summary Cards
                SummaryCardsSection(overview: overview)

                // Events by Status Chart
                StatusBreakdownChart(eventsByStatus: overview.eventsByStatus)

                // Events List with Analytics
                EventsAnalyticsListSection(
                    events: events,
                    onEventTap: { event in
                        selectedEvent = event
                        showEventDetail = true
                    }
                )
            }
            .padding()
        }
        .navigationTitle(String(localized: "dashboard.title"))
        .sheet(isPresented: $showEventDetail) {
            if let event = selectedEvent {
                EventDetailedAnalyticsView(event: event)
            }
        }
    }
}

// MARK: - Summary Cards Section

struct SummaryCardsSection: View {
    let overview: DashboardOverview

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "dashboard.overview"))
                .font(.headline)
                .foregroundColor(.primary)

            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible()),
                GridItem(.flexible())
            ], spacing: 12) {
                SummaryCard(
                    icon: "calendar.badge.plus",
                    title: String(localized: "dashboard.events"),
                    value: "\(overview.totalEvents)",
                    color: .blue
                )
                SummaryCard(
                    icon: "person.3.fill",
                    title: String(localized: "dashboard.participants"),
                    value: "\(overview.totalParticipants)",
                    color: .green
                )
                SummaryCard(
                    icon: "chart.bar.fill",
                    title: String(localized: "dashboard.avg_per_event"),
                    value: String(format: "%.1f", overview.averageParticipants),
                    color: .purple
                )
            }

            LazyVGrid(columns: [
                GridItem(.flexible()),
                GridItem(.flexible()),
                GridItem(.flexible())
            ], spacing: 12) {
                SummaryCard(
                    icon: "hand.thumbsup.fill",
                    title: String(localized: "dashboard.votes"),
                    value: "\(overview.totalVotes)",
                    color: .orange
                )
                SummaryCard(
                    icon: "bubble.left.fill",
                    title: String(localized: "dashboard.comments"),
                    value: "\(overview.totalComments)",
                    color: .teal
                )
                SummaryCard(
                    icon: "percent",
                    title: String(localized: "dashboard.response_rate"),
                    value: overview.totalEvents > 0
                        ? String(format: "%.0f%%", overview.averageParticipants > 0 ? min(Double(overview.totalVotes) / Double(overview.totalParticipants) * 100.0 / max(overview.averageParticipants, 1.0), 100.0) : 0.0)
                        : "0%",
                    color: .red
                )
            }
        }
    }
}

struct SummaryCard: View {
    let icon: String
    let title: String
    let value: String
    let color: Color

    var body: some View {
        VStack(spacing: 8) {
            Image(systemName: icon)
                .font(.title2)
                .foregroundColor(color)

            Text(value)
                .font(.system(size: 22, weight: .bold, design: .rounded))
                .foregroundColor(.primary)

            Text(title)
                .font(.caption2)
                .foregroundColor(.secondary)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 14)
        .padding(.horizontal, 6)
        .background(
            RoundedRectangle(cornerRadius: 14, style: .continuous)
                .fill(color.opacity(0.1))
        )
    }
}

// MARK: - Status Breakdown Chart

struct StatusBreakdownChart: View {
    let eventsByStatus: [String: Int]

    private let statusColors: [String: Color] = [
        "DRAFT": .gray,
        "POLLING": .blue,
        "COMPARING": .indigo,
        "CONFIRMED": .green,
        "ORGANIZING": .orange,
        "FINALIZED": .purple
    ]

    private var statusLabels: [String: String] {
        [
            "DRAFT": String(localized: "dashboard.status.draft"),
            "POLLING": String(localized: "dashboard.status.polling"),
            "COMPARING": String(localized: "dashboard.status.comparing"),
            "CONFIRMED": String(localized: "dashboard.status.confirmed"),
            "ORGANIZING": String(localized: "dashboard.status.organizing"),
            "FINALIZED": String(localized: "dashboard.status.finalized")
        ]
    }

    private var total: Int {
        eventsByStatus.values.reduce(0, +)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "dashboard.events_by_status"))
                .font(.headline)
                .foregroundColor(.primary)

            ProfileCard {
                VStack(spacing: 12) {
                    ForEach(Array(eventsByStatus.sorted(by: { $0.value > $1.value })), id: \.key) { status, count in
                        HStack(spacing: 12) {
                            Text(statusLabels[status] ?? status)
                                .font(.subheadline)
                                .foregroundColor(.primary)
                                .frame(width: 100, alignment: .leading)

                            GeometryReader { geometry in
                                ZStack(alignment: .leading) {
                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(Color.gray.opacity(0.15))
                                        .frame(height: 20)

                                    RoundedRectangle(cornerRadius: 4)
                                        .fill(statusColors[status] ?? .gray)
                                        .frame(
                                            width: total > 0
                                                ? geometry.size.width * CGFloat(count) / CGFloat(total)
                                                : 0,
                                            height: 20
                                        )
                                }
                            }
                            .frame(height: 20)

                            Text("\(count)")
                                .font(.subheadline)
                                .fontWeight(.semibold)
                                .foregroundColor(statusColors[status] ?? .gray)
                                .frame(width: 30, alignment: .trailing)
                        }
                    }
                }
            }
        }
    }
}

// MARK: - Events Analytics List Section

struct EventsAnalyticsListSection: View {
    let events: [DashboardEventItem]
    let onEventTap: (DashboardEventItem) -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "dashboard.my_events"))
                .font(.headline)
                .foregroundColor(.primary)

            ForEach(events) { event in
                Button(action: { onEventTap(event) }) {
                    EventAnalyticsRow(event: event)
                }
                .buttonStyle(.plain)
            }
        }
    }
}

struct EventAnalyticsRow: View {
    let event: DashboardEventItem

    private var statusColor: Color {
        switch event.status {
        case "DRAFT": return .gray
        case "POLLING": return .blue
        case "COMPARING": return .indigo
        case "CONFIRMED": return .green
        case "ORGANIZING": return .orange
        case "FINALIZED": return .purple
        default: return .gray
        }
    }

    private var statusLabel: String {
        switch event.status {
        case "DRAFT": return String(localized: "dashboard.status.draft")
        case "POLLING": return String(localized: "dashboard.status.polling")
        case "COMPARING": return String(localized: "dashboard.status.comparing")
        case "CONFIRMED": return String(localized: "dashboard.status.confirmed")
        case "ORGANIZING": return String(localized: "dashboard.status.organizing")
        case "FINALIZED": return String(localized: "dashboard.status.finalized")
        default: return event.status
        }
    }

    var body: some View {
        ProfileCard {
            VStack(alignment: .leading, spacing: 10) {
                // Title + Status
                HStack {
                    Text(event.title)
                        .font(.subheadline)
                        .fontWeight(.semibold)
                        .foregroundColor(.primary)

                    Spacer()

                    Text(statusLabel)
                        .font(.caption2)
                        .fontWeight(.medium)
                        .foregroundColor(statusColor)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(
                            Capsule()
                                .fill(statusColor.opacity(0.15))
                        )
                }

                // Mini analytics row
                HStack(spacing: 16) {
                    MiniStatView(icon: "person.2.fill", value: "\(event.participantCount)", label: "Participants")
                    MiniStatView(icon: "hand.thumbsup.fill", value: "\(event.voteCount)", label: "Votes")
                    MiniStatView(icon: "bubble.left.fill", value: "\(event.commentCount)", label: "Comm.")
                    MiniStatView(icon: "percent", value: String(format: "%.0f%%", event.responseRate), label: "Rep.")
                }

                // Chevron hint
                HStack {
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(.secondary)
                }
            }
        }
    }
}

struct MiniStatView: View {
    let icon: String
    let value: String
    let label: String

    var body: some View {
        VStack(spacing: 3) {
            HStack(spacing: 4) {
                Image(systemName: icon)
                    .font(.system(size: 10))
                    .foregroundColor(.secondary)
                Text(value)
                    .font(.caption)
                    .fontWeight(.semibold)
                    .foregroundColor(.primary)
            }
            Text(label)
                .font(.system(size: 9))
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - Event Detailed Analytics View (Sheet)

struct EventDetailedAnalyticsView: View {
    let event: DashboardEventItem
    @Environment(\.dismiss) private var dismiss

    // Mock detailed analytics
    @State private var analytics = EventDetailedAnalytics(
        eventId: "",
        title: "",
        status: "",
        voteTimeline: [
            TimelineEntry(date: "2025-10-01", count: 3),
            TimelineEntry(date: "2025-10-02", count: 7),
            TimelineEntry(date: "2025-10-03", count: 12),
            TimelineEntry(date: "2025-10-04", count: 8),
            TimelineEntry(date: "2025-10-05", count: 15),
            TimelineEntry(date: "2025-10-06", count: 5),
            TimelineEntry(date: "2025-10-07", count: 2)
        ],
        participantTimeline: [
            TimelineEntry(date: "2025-10-01", count: 2),
            TimelineEntry(date: "2025-10-02", count: 4),
            TimelineEntry(date: "2025-10-03", count: 3),
            TimelineEntry(date: "2025-10-04", count: 5),
            TimelineEntry(date: "2025-10-05", count: 1)
        ],
        popularTimeSlots: [
            PopularTimeSlotItem(id: "slot1", startTime: "2025-11-15T14:00:00Z", endTime: "2025-11-15T18:00:00Z", timeOfDay: "AFTERNOON", yesVotes: 12, maybeVotes: 3, noVotes: 2, totalVotes: 17),
            PopularTimeSlotItem(id: "slot2", startTime: "2025-11-16T10:00:00Z", endTime: "2025-11-16T14:00:00Z", timeOfDay: "MORNING", yesVotes: 8, maybeVotes: 5, noVotes: 4, totalVotes: 17),
            PopularTimeSlotItem(id: "slot3", startTime: "2025-11-17T19:00:00Z", endTime: "2025-11-17T23:00:00Z", timeOfDay: "EVENING", yesVotes: 6, maybeVotes: 7, noVotes: 4, totalVotes: 17)
        ],
        pollCompletionRate: 85.0,
        totalParticipants: 15,
        votedParticipants: 13
    )

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(spacing: 20) {
                    // Poll Completion
                    PollCompletionSection(
                        completionRate: analytics.pollCompletionRate,
                        votedParticipants: analytics.votedParticipants,
                        totalParticipants: analytics.totalParticipants
                    )

                    // Vote Timeline Chart
                    TimelineChartSection(
                        title: String(localized: "dashboard.vote_timeline"),
                        icon: "hand.thumbsup.fill",
                        color: .blue,
                        entries: analytics.voteTimeline
                    )

                    // Participant Timeline Chart
                    TimelineChartSection(
                        title: String(localized: "dashboard.registration_timeline"),
                        icon: "person.badge.plus",
                        color: .green,
                        entries: analytics.participantTimeline
                    )

                    // Popular Time Slots
                    PopularTimeSlotsSection(slots: analytics.popularTimeSlots)
                }
                .padding()
            }
            .navigationTitle(event.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button(String(localized: "dashboard.close")) {
                        dismiss()
                    }
                }
            }
        }
    }
}

// MARK: - Poll Completion Section

struct PollCompletionSection: View {
    let completionRate: Double
    let votedParticipants: Int
    let totalParticipants: Int

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "dashboard.poll_participation"))
                .font(.headline)
                .foregroundColor(.primary)

            ProfileCard {
                VStack(spacing: 14) {
                    // Circular progress
                    ZStack {
                        Circle()
                            .stroke(Color.gray.opacity(0.15), lineWidth: 10)
                            .frame(width: 100, height: 100)

                        Circle()
                            .trim(from: 0, to: completionRate / 100.0)
                            .stroke(
                                completionRate > 75 ? Color.green :
                                    completionRate > 50 ? Color.orange : Color.red,
                                style: StrokeStyle(lineWidth: 10, lineCap: .round)
                            )
                            .frame(width: 100, height: 100)
                            .rotationEffect(.degrees(-90))

                        Text(String(format: "%.0f%%", completionRate))
                            .font(.system(size: 24, weight: .bold, design: .rounded))
                            .foregroundColor(.primary)
                    }

                    Text(String(format: String(localized: "dashboard.participants_voted"), votedParticipants, totalParticipants))
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                }
            }
        }
    }
}

// MARK: - Timeline Chart Section

struct TimelineChartSection: View {
    let title: String
    let icon: String
    let color: Color
    let entries: [TimelineEntry]

    private var maxCount: Int {
        entries.map(\.count).max() ?? 1
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 6) {
                Image(systemName: icon)
                    .foregroundColor(color)
                Text(title)
                    .font(.headline)
                    .foregroundColor(.primary)
            }

            ProfileCard {
                VStack(spacing: 8) {
                    // Bar chart
                    HStack(alignment: .bottom, spacing: 6) {
                        ForEach(entries) { entry in
                            VStack(spacing: 4) {
                                Text("\(entry.count)")
                                    .font(.system(size: 9, weight: .semibold))
                                    .foregroundColor(.secondary)

                                RoundedRectangle(cornerRadius: 4)
                                    .fill(
                                        LinearGradient(
                                            gradient: Gradient(colors: [color.opacity(0.6), color]),
                                            startPoint: .bottom,
                                            endPoint: .top
                                        )
                                    )
                                    .frame(
                                        height: maxCount > 0
                                            ? max(CGFloat(entry.count) / CGFloat(maxCount) * 80, 4)
                                            : 4
                                    )

                                Text(formatShortDate(entry.date))
                                    .font(.system(size: 8))
                                    .foregroundColor(.secondary)
                                    .lineLimit(1)
                            }
                            .frame(maxWidth: .infinity)
                        }
                    }
                    .frame(height: 120)
                }
            }
        }
    }

    private func formatShortDate(_ dateString: String) -> String {
        let parts = dateString.split(separator: "-")
        if parts.count >= 3 {
            return "\(parts[2])/\(parts[1])"
        }
        return dateString
    }
}

// MARK: - Popular Time Slots Section

struct PopularTimeSlotsSection: View {
    let slots: [PopularTimeSlotItem]

    private var timeOfDayLabels: [String: String] {
        [
            "MORNING": String(localized: "dashboard.time.morning"),
            "AFTERNOON": String(localized: "dashboard.time.afternoon"),
            "EVENING": String(localized: "dashboard.time.evening"),
            "SPECIFIC": String(localized: "dashboard.time.specific"),
            "ALL_DAY": String(localized: "dashboard.time.all_day")
        ]
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack(spacing: 6) {
                Image(systemName: "clock.fill")
                    .foregroundColor(.purple)
                Text(String(localized: "dashboard.popular_slots"))
                    .font(.headline)
                    .foregroundColor(.primary)
            }

            ForEach(Array(slots.enumerated()), id: \.element.id) { index, slot in
                ProfileCard {
                    VStack(alignment: .leading, spacing: 10) {
                        HStack {
                            Text("#\(index + 1)")
                                .font(.caption)
                                .fontWeight(.bold)
                                .foregroundColor(.white)
                                .frame(width: 26, height: 26)
                                .background(
                                    Circle()
                                        .fill(index == 0 ? Color.purple : Color.gray)
                                )

                            VStack(alignment: .leading, spacing: 2) {
                                Text(timeOfDayLabels[slot.timeOfDay ?? ""] ?? slot.timeOfDay ?? "")
                                    .font(.subheadline)
                                    .fontWeight(.semibold)

                                if let start = slot.startTime {
                                    Text(formatDateTime(start))
                                        .font(.caption)
                                        .foregroundColor(.secondary)
                                }
                            }

                            Spacer()

                            Text(String(format: String(localized: "dashboard.votes_count"), slot.totalVotes))
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }

                        // Stacked bar for votes
                        GeometryReader { geometry in
                            let total = max(slot.totalVotes, 1)
                            let yesWidth = geometry.size.width * CGFloat(slot.yesVotes) / CGFloat(total)
                            let maybeWidth = geometry.size.width * CGFloat(slot.maybeVotes) / CGFloat(total)
                            let noWidth = geometry.size.width * CGFloat(slot.noVotes) / CGFloat(total)

                            HStack(spacing: 2) {
                                RoundedRectangle(cornerRadius: 3)
                                    .fill(Color.green)
                                    .frame(width: max(yesWidth, 2), height: 14)
                                RoundedRectangle(cornerRadius: 3)
                                    .fill(Color.orange)
                                    .frame(width: max(maybeWidth, 2), height: 14)
                                RoundedRectangle(cornerRadius: 3)
                                    .fill(Color.red)
                                    .frame(width: max(noWidth, 2), height: 14)
                                Spacer()
                            }
                        }
                        .frame(height: 14)

                        // Legend
                        HStack(spacing: 16) {
                            VoteLegendItem(color: .green, label: String(localized: "dashboard.vote.yes"), count: slot.yesVotes)
                            VoteLegendItem(color: .orange, label: String(localized: "dashboard.vote.maybe"), count: slot.maybeVotes)
                            VoteLegendItem(color: .red, label: String(localized: "dashboard.vote.no"), count: slot.noVotes)
                        }
                    }
                }
            }
        }
    }

    private func formatDateTime(_ isoString: String) -> String {
        let parts = isoString.replacingOccurrences(of: "T", with: " ").replacingOccurrences(of: "Z", with: "")
        let components = parts.split(separator: " ")
        if components.count >= 2 {
            let dateParts = components[0].split(separator: "-")
            let timeParts = components[1].split(separator: ":")
            if dateParts.count >= 3 && timeParts.count >= 2 {
                return "\(dateParts[2])/\(dateParts[1])/\(dateParts[0]) \(timeParts[0])h\(timeParts[1])"
            }
        }
        return isoString
    }
}

struct VoteLegendItem: View {
    let color: Color
    let label: String
    let count: Int

    var body: some View {
        HStack(spacing: 4) {
            Circle()
                .fill(color)
                .frame(width: 8, height: 8)
            Text("\(label): \(count)")
                .font(.caption2)
                .foregroundColor(.secondary)
        }
    }
}

// MARK: - Preview

struct OrganizerDashboardView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            OrganizerDashboardView()
        }
    }
}
