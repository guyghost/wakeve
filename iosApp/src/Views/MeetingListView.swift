import SwiftUI
import Shared

/**
 * MeetingListView - Virtual meetings list screen for iOS (Phase 4, Refactored).
 *
 * Displays:
 * - List of virtual meetings (Zoom, Meet, FaceTime)
 * - Meetings grouped by status (SCHEDULED, STARTED, ENDED, CANCELLED)
 * - Pull-to-refresh functionality
 * - Full Liquid Glass design system implementation
 * - Matches Android MeetingListScreen functionality
 *
 * Architecture: Functional Core & Imperative Shell
 * - Pure functions for meeting status mapping
 * - View state managed in Imperative Shell via MeetingListViewModel
 */

// MARK: - View

struct MeetingListView: View {
    let eventId: String
    let userId: String
    let isOrganizer: Bool
    let onMeetingTap: (String) -> Void
    let onCreateMeeting: () -> Void
    let onBack: () -> Void

    @StateObject private var viewModel: MeetingListViewModel
    @State private var showingEditSheet = false
    @State private var editingMeeting: VirtualMeeting?
    @State private var showingGenerateLinkSheet = false
    @State private var generatingMeeting: VirtualMeeting?
    @State private var isRefreshing = false

    init(
        eventId: String,
        userId: String,
        isOrganizer: Bool = false,
        onMeetingTap: @escaping (String) -> Void,
        onCreateMeeting: @escaping () -> Void,
        onBack: @escaping () -> Void
    ) {
        self.eventId = eventId
        self.userId = userId
        self.isOrganizer = isOrganizer
        self.onMeetingTap = onMeetingTap
        self.onCreateMeeting = onCreateMeeting
        self.onBack = onBack
        self._viewModel = StateObject(wrappedValue: MeetingListViewModel(eventId: eventId))
    }

    var body: some View {
        ZStack {
            backgroundGradient

            NavigationStack {
                contentView
                    .navigationTitle("Réunions")
                    .navigationBarTitleDisplayMode(.large)
                    .toolbar {
                        ToolbarItem(placement: .navigationBarLeading) {
                            Button(action: onBack) {
                                Image(systemName: "chevron.left")
                                    .foregroundColor(.primary)
                                    .accessibilityLabel("Retour")
                            }
                        }
                        if isOrganizer {
                            ToolbarItem(placement: .navigationBarTrailing) {
                                Button(action: onCreateMeeting) {
                                    Image(systemName: "plus")
                                        .foregroundColor(.primary)
                                        .accessibilityLabel("Créer une réunion")
                                }
                            }
                        }
                    }
            }
        }
        .onAppear {
            viewModel.loadMeetings()
        }
        .refreshable {
            await performRefresh()
        }
        .alert("Error", isPresented: Binding(
            get: { viewModel.hasError },
            set: { if !$0 { viewModel.clearError() } }
        )) {
            Button("OK", role: .cancel) { viewModel.clearError() }
        } message: {
            Text(viewModel.errorMessage ?? "An error occurred")
        }
        .sheet(isPresented: $showingEditSheet) {
            if let meeting = editingMeeting {
                MeetingEditSheet(
                    meeting: meeting,
                    onSave: { title, description, scheduledFor, duration in
                        viewModel.updateMeeting(
                            meetingId: meeting.id,
                            title: title,
                            description: description,
                            scheduledFor: scheduledFor,
                            durationMinutes: duration
                        )
                        showingEditSheet = false
                        editingMeeting = nil
                    },
                    onCancel: {
                        showingEditSheet = false
                        editingMeeting = nil
                    }
                )
            }
        }
        .sheet(isPresented: $showingGenerateLinkSheet) {
            if let meeting = generatingMeeting {
                MeetingGenerateLinkSheet(
                    meeting: meeting,
                    onGenerate: { platform in
                        viewModel.generateMeetingLink(meetingId: meeting.id, platform: platform)
                        showingGenerateLinkSheet = false
                        generatingMeeting = nil
                    },
                    onCancel: {
                        showingGenerateLinkSheet = false
                        generatingMeeting = nil
                    }
                )
            }
        }
    }

    // MARK: - Background

    private var backgroundGradient: some View {
        LinearGradient(
            gradient: Gradient(colors: [
                Color.wakevePrimary.opacity(0.08),
                Color.wakeveAccent.opacity(0.08)
            ]),
            startPoint: .topLeading,
            endPoint: .bottomTrailing
        )
        .ignoresSafeArea()
    }

    // MARK: - Content View

    @ViewBuilder
    private var contentView: some View {
        if viewModel.isLoading && viewModel.isEmpty {
            loadingView
        } else if viewModel.isEmpty {
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
                .tint(.wakevePrimary)

            Text(NSLocalizedString("loading_label", comment: "Loading text"))
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .frame(maxHeight: .infinity)
    }

    // MARK: - Empty State View

    private var emptyStateView: some View {
        VStack(spacing: 24) {
            Spacer()

            ZStack {
                Circle()
                    .fill(Color.wakevePrimary.opacity(0.1))
                    .frame(width: 80, height: 80)

                Image(systemName: "video.slash")
                    .font(.system(size: 36))
                    .foregroundColor(.wakevePrimary)
            }

            VStack(spacing: 8) {
                Text(NSLocalizedString("no_meetings_title", comment: "No meetings title"))
                    .font(.title2.weight(.semibold))
                    .foregroundColor(.primary)

                Text(NSLocalizedString("no_meetings_subtitle", comment: "No meetings subtitle"))
                    .font(.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }

            if isOrganizer {
                LiquidGlassButton(
                    title: NSLocalizedString("create_first_meeting", comment: "Create first meeting button"),
                    icon: "plus.circle.fill",
                    style: .primary,
                    action: onCreateMeeting
                )
                .padding(.top, 8)
            }

            Spacer()
        }
        .frame(maxHeight: .infinity)
    }

    // MARK: - Meeting List View

    private var meetingListView: some View {
        ScrollView {
            LazyVStack(spacing: 16) {
                // Scheduled meetings
                if !scheduledMeetings.isEmpty {
                    Section(header: sectionHeader(title: "Planifiées", icon: "calendar")) {
                        ForEach(scheduledMeetings, id: \.id) { meeting in
                            MeetingCard(
                                meeting: meeting,
                                isOrganizer: isOrganizer,
                                onTap: { onMeetingTap(meeting.id) },
                                onEdit: {
                                    editingMeeting = meeting
                                    showingEditSheet = true
                                },
                                onGenerateLink: {
                                    generatingMeeting = meeting
                                    showingGenerateLinkSheet = true
                                }
                            )
                        }
                    }
                }

                // Started meetings
                if !startedMeetings.isEmpty {
                    Section(header: sectionHeader(title: "En cours", icon: "play.circle.fill")) {
                        ForEach(startedMeetings, id: \.id) { meeting in
                            MeetingCard(
                                meeting: meeting,
                                isOrganizer: isOrganizer,
                                onTap: { onMeetingTap(meeting.id) },
                                onEdit: {
                                    editingMeeting = meeting
                                    showingEditSheet = true
                                },
                                onGenerateLink: {
                                    generatingMeeting = meeting
                                    showingGenerateLinkSheet = true
                                }
                            )
                        }
                    }
                }

                // Ended meetings
                if !endedMeetings.isEmpty {
                    Section(header: sectionHeader(title: "Terminées", icon: "checkmark.circle.fill")) {
                        ForEach(endedMeetings, id: \.id) { meeting in
                            MeetingCard(
                                meeting: meeting,
                                isOrganizer: isOrganizer,
                                onTap: { onMeetingTap(meeting.id) },
                                onEdit: nil,
                                onGenerateLink: nil
                            )
                        }
                    }
                }

                // Cancelled meetings
                if !cancelledMeetings.isEmpty {
                    Section(header: sectionHeader(title: "Annulées", icon: "xmark.circle.fill")) {
                        ForEach(cancelledMeetings, id: \.id) { meeting in
                            MeetingCard(
                                meeting: meeting,
                                isOrganizer: isOrganizer,
                                onTap: { onMeetingTap(meeting.id) },
                                onEdit: nil,
                                onGenerateLink: nil
                            )
                        }
                    }
                }
            }
            .padding(.horizontal, 20)
            .padding(.vertical, 16)
        }
    }

    // MARK: - Section Header

    private func sectionHeader(title: String, icon: String) -> some View {
        HStack(spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.wakevePrimary)

            Text(title)
                .font(.system(size: 18, weight: .semibold))
                .foregroundColor(.primary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 4)
        .padding(.vertical, 8)
    }

    // MARK: - Meeting Groups

    private var scheduledMeetings: [VirtualMeeting] {
        viewModel.meetings.filter { $0.status == .scheduled }
    }

    private var startedMeetings: [VirtualMeeting] {
        viewModel.meetings.filter { $0.status == .started }
    }

    private var endedMeetings: [VirtualMeeting] {
        viewModel.meetings.filter { $0.status == .ended }
    }

    private var cancelledMeetings: [VirtualMeeting] {
        viewModel.meetings.filter { $0.status == .cancelled }
    }

    // MARK: - Refresh

    private func performRefresh() async {
        isRefreshing = true
        viewModel.loadMeetings()
        // Wait for the loading to complete
        try? await Task.sleep(nanoseconds: 500_000_000) // 0.5 seconds
        isRefreshing = false
    }
}

// MARK: - Meeting Card

struct MeetingCard: View {
    let meeting: VirtualMeeting
    let isOrganizer: Bool
    let onTap: () -> Void
    let onEdit: (() -> Void)?
    let onGenerateLink: (() -> Void)?

    @State private var showingShareSheet = false

    var body: some View {
        LiquidGlassCard(cornerRadius: 16, padding: 16) {
            VStack(alignment: .leading, spacing: 12) {
                // Header with platform and status
                HStack {
                    platformIcon

                    VStack(alignment: .leading, spacing: 4) {
                        Text(meeting.title)
                            .font(.headline.weight(.semibold))
                            .foregroundColor(.primary)
                            .lineLimit(1)

                        HStack(spacing: 6) {
                            platformName
                            statusBadge
                        }
                    }

                    Spacer()

                    chevronIcon
                }

                // Date and duration
                HStack(spacing: 16) {
                    HStack(spacing: 6) {
                        Image(systemName: "calendar")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Text(formattedDateTime)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }

                    HStack(spacing: 6) {
                        Image(systemName: "clock")
                            .font(.caption)
                            .foregroundColor(.secondary)

                        Text(formattedDuration)
                            .font(.subheadline)
                            .foregroundColor(.secondary)
                    }
                }

                // Meeting link (if exists)
                if !meeting.meetingUrl.isEmpty {
                    HStack(spacing: 8) {
                        Image(systemName: "link.circle.fill")
                            .font(.caption)
                            .foregroundColor(.wakevePrimary)

                        Text(meeting.meetingUrl)
                            .font(.caption)
                            .foregroundColor(.wakevePrimary)
                            .lineLimit(1)

                        Spacer()

                        Button(action: {
                            UIPasteboard.general.string = meeting.meetingUrl
                        }) {
                            Image(systemName: "doc.on.doc")
                                .font(.caption)
                                .foregroundColor(.secondary)
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 8)
                    .background(Color.wakeveSurfaceLight)
                    .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                }

                // Organizer actions
                if isOrganizer {
                    organizerActions
                }
            }
        }
        .onTapGesture {
            onTap()
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel(meetingAccessibilityLabel)
        .accessibilityHint("Tap to view meeting details")
        .sheet(isPresented: $showingShareSheet) {
            ShareSheet(items: [meeting.meetingUrl])
        }
    }

    // MARK: - Platform Icon

    private var platformIcon: some View {
        ZStack {
            Circle()
                .fill(platformColor.opacity(0.15))
                .frame(width: 48, height: 48)

            Image(systemName: platformIconName)
                .font(.system(size: 20, weight: .medium))
                .foregroundColor(platformColor)
        }
    }

    // MARK: - Platform Name

    private var platformName: some View {
        Text(platformNameText)
            .font(.caption)
            .foregroundColor(platformColor)
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(platformColor.opacity(0.1))
            .clipShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
    }

    // MARK: - Status Badge

    private var statusBadge: some View {
        LiquidGlassBadge(
            text: statusText,
            style: statusBadgeStyle
        )
    }

    // MARK: - Chevron Icon

    private var chevronIcon: some View {
        Image(systemName: "chevron.right")
            .font(.system(size: 14, weight: .medium))
            .foregroundColor(.secondary.opacity(0.6))
    }

    // MARK: - Organizer Actions

    @ViewBuilder
    private var organizerActions: some View {
        HStack(spacing: 12) {
            if let onEdit = onEdit {
                LiquidGlassButton(
                    title: "Modifier",
                    icon: "pencil",
                    style: .secondary,
                    size: .small,
                    action: onEdit
                )
            }

            if let onGenerateLink = onGenerateLink {
                LiquidGlassButton(
                    title: "Régénérer le lien",
                    icon: "link",
                    style: .primary,
                    size: .small,
                    action: onGenerateLink
                )
            }

            LiquidGlassButton(
                title: "Partager",
                icon: "square.and.arrow.up",
                style: .text,
                size: .small,
                action: {
                    showingShareSheet = true
                }
            )
        }
    }

    // MARK: - Helpers

    private var platformIconName: String {
        switch meeting.platform {
        case .zoom: return "video.fill"
        case .googleMeet: return "video.badge.plus"
        case .facetime: return "video.fill"
        case .teams: return "video.fill"
        case .webex: return "video.fill"
        default: return "video.fill"
        }
    }

    private var platformColor: Color {
        switch meeting.platform {
        case .zoom: return .wakevePrimary
        case .googleMeet: return .wakeveSuccess
        case .facetime: return .wakeveAccent
        case .teams: return .iOSSystemBlue
        case .webex: return .iOSSystemGreen
        default: return .wakevePrimary
        }
    }

    private var platformNameText: String {
        switch meeting.platform {
        case .zoom: return "Zoom"
        case .googleMeet: return "Google Meet"
        case .facetime: return "FaceTime"
        case .teams: return "Microsoft Teams"
        case .webex: return "Webex"
        default: return "Meeting"
        }
    }

    private var statusText: String {
        switch meeting.status {
        case .scheduled: return "Planifiée"
        case .started: return "En cours"
        case .ended: return "Terminée"
        case .cancelled: return "Annulée"
        default: return "Inconnue"
        }
    }

    private var statusBadgeStyle: LiquidGlassBadgeStyle {
        switch meeting.status {
        case .scheduled: return .warning
        case .started: return .success
        case .ended: return .default
        case .cancelled: return .default
        default: return .default
        }
    }

    private var formattedDateTime: String {
        let instant = meeting.scheduledFor
        let epochSeconds = instant.epochSeconds
        let date = Date(timeIntervalSince1970: TimeInterval(epochSeconds))

        let formatter = DateFormatter()
        formatter.dateFormat = "dd MMM yyyy, HH:mm"
        formatter.locale = Locale(identifier: "fr_FR")

        return formatter.string(from: date)
    }

    private var formattedDuration: String {
        let durationMs = meeting.duration
        let totalMinutes = Int64(durationMs) / 60_000_000_000
        let hours = totalMinutes / 60
        let minutes = totalMinutes % 60

        if hours > 0 {
            return "\(hours)h \(minutes)m"
        } else {
            return "\(minutes)m"
        }
    }

    private var meetingAccessibilityLabel: String {
        "\(meeting.title), \(platformNameText), \(formattedDateTime)"
    }
}

// MARK: - Preview

struct MeetingListView_Previews: PreviewProvider {
    static var previews: some View {
        MeetingListView(
            eventId: "event-123",
            userId: "user-123",
            isOrganizer: true,
            onMeetingTap: { _ in },
            onCreateMeeting: { },
            onBack: { }
        )
    }
}
