import SwiftUI
import Shared

// MARK: - MeetingListView

struct MeetingListView: View {
    let eventId: String
    let currentUserId: String
    let isOrganizer: Bool
    let canCreateMeetings: Bool
    let isReadOnly: Bool

    @StateObject private var viewModel: MeetingListViewModel
    @State private var showCreateSheet = false
    @State private var navigateToMeetingId: String? = nil
    private let previewMeetings: [VirtualMeeting]?

    init(
        eventId: String,
        currentUserId: String,
        isOrganizer: Bool,
        canCreateMeetings: Bool,
        isReadOnly: Bool = false
    ) {
        self.eventId = eventId
        self.currentUserId = currentUserId
        self.isOrganizer = isOrganizer
        self.canCreateMeetings = canCreateMeetings
        self.isReadOnly = isReadOnly
        self._viewModel = StateObject(wrappedValue: MeetingListViewModel(eventId: eventId, currentUserId: currentUserId))
        self.previewMeetings = nil
    }

    init(eventId: String) {
        self.init(
            eventId: eventId,
            currentUserId: "anonymous-user",
            isOrganizer: false,
            canCreateMeetings: false,
            isReadOnly: true
        )
    }

#if DEBUG
    init(eventId: String, previewMeetings: [VirtualMeeting]) {
        self.eventId = eventId
        self.currentUserId = "preview-organizer"
        self.isOrganizer = true
        self.canCreateMeetings = true
        self.isReadOnly = false
        self._viewModel = StateObject(wrappedValue: MeetingListViewModel(eventId: eventId, currentUserId: "preview-organizer"))
        self.previewMeetings = previewMeetings
    }
#endif

    var body: some View {
        NavigationStack {
            ZStack {
                WakeveScreenBackground(style: .grouped)

                if shouldShowLoading {
                    loadingView
                } else if meetings.isEmpty {
                    emptyView
                } else {
                    listView
                }
            }
            .navigationTitle(String(localized: "meetings.title"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                if canCreateMeetings {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button {
                            guard canMutateMeetings else { return }
                            showCreateSheet = true
                        } label: {
                            Image(systemName: "plus")
                        }
                        .disabled(!canMutateMeetings)
                        .accessibilityLabel(String(localized: "meetings.create"))
                    }
                }
            }
            .navigationDestination(item: $navigateToMeetingId) { meetingId in
                MeetingDetailView(
                    meetingId: meetingId,
                    eventId: eventId,
                    currentUserId: currentUserId,
                    isOrganizer: isOrganizer,
                    isReadOnly: isReadOnly
                )
            }
            .sheet(isPresented: $showCreateSheet) {
                CreateMeetingSheet(eventId: eventId) { platform, title in
                    guard !isPreviewing else {
                        showCreateSheet = false
                        return
                    }
                    viewModel.createMeeting(
                        platform: platform,
                        title: title,
                        description: nil,
                        scheduledFor: Date(),
                        durationMinutes: 60
                    )
                    showCreateSheet = false
                    viewModel.loadMeetings()
                }
            }
        }
        .onAppear {
            guard !isPreviewing else { return }
            viewModel.loadMeetings()
        }
        .alert(String(localized: "common.error"), isPresented: .constant(viewModel.hasError)) {
            Button(String(localized: "common.ok")) { viewModel.clearError() }
        } message: {
            Text(viewModel.errorMessage ?? String(localized: "common.error_generic"))
        }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
                .tint(WakeveTheme.ColorToken.permissionBlue)
                .accessibilityLabel(String(localized: "common.loading"))
            Text(String(localized: "meetings.loading"))
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Empty

    private var emptyView: some View {
        ScrollView {
            VStack(spacing: WakeveTheme.Spacing.lg) {
                WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.xl) {
                    VStack(spacing: WakeveTheme.Spacing.md) {
                        Image(systemName: "video.badge.plus")
                            .font(.system(size: 42, weight: .semibold))
                            .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
                            .frame(width: 74, height: 74)
                            .background(WakeveTheme.ColorToken.permissionBlue.opacity(0.12))
                            .clipShape(Circle())

                        VStack(spacing: WakeveTheme.Spacing.xs) {
                            Text(String(localized: "meetings.empty_title"))
                                .font(WakeveTheme.Typography.title2)
                                .foregroundColor(.primary)

                            Text(String(localized: "meetings.empty_subtitle"))
                                .font(WakeveTheme.Typography.body)
                                .foregroundColor(.secondary)
                                .multilineTextAlignment(.center)
                                .fixedSize(horizontal: false, vertical: true)
                        }

                        if canCreateMeetings {
                            WakeveActionButton(
                                String(localized: "meetings.create"),
                                systemImage: "video.badge.plus",
                                variant: .primary
                            ) {
                                guard canMutateMeetings else { return }
                                showCreateSheet = true
                            }
                            .disabled(!canMutateMeetings)
                        }
                    }
                }
            }
            .padding(WakeveTheme.Spacing.page)
            .frame(maxWidth: .infinity)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - List

    private var listView: some View {
        ScrollView {
            LazyVStack(spacing: WakeveTheme.Spacing.md) {
                MeetingSyncBanner(pendingSync: viewModel.pendingSync, isOnline: viewModel.isOnline)
                MeetingOverviewCard(
                    totalCount: meetings.count,
                    readyCount: meetingsWithLinksCount,
                    liveCount: liveMeetingsCount,
                    nextActionText: meetingOverviewNextActionText
                )

                ForEach(meetings, id: \.id) { meeting in
                    Button {
                        navigateToMeetingId = meeting.id
                    } label: {
                        MeetingRowCard(meeting: meeting)
                    }
                    .buttonStyle(.plain)
                    .contextMenu {
                        if canCancelMeetings && !isPreviewing {
                            Button(role: .destructive) {
                                viewModel.cancelMeeting(
                                    meetingId: meeting.id,
                                    currentUserId: currentUserId,
                                    isOrganizer: isOrganizer,
                                    isReadOnly: isReadOnly
                                )
                            } label: {
                                Label(String(localized: "meetings.cancel"), systemImage: "xmark.circle")
                            }
                        }
                    }
                    .accessibilityHint(String(localized: "meetings.join_hint"))
                }
            }
            .padding(WakeveTheme.Spacing.page)
        }
        .refreshable {
            guard !isPreviewing else { return }
            viewModel.loadMeetings()
        }
    }

    private var isPreviewing: Bool {
        previewMeetings != nil
    }

    private var canMutateMeetings: Bool {
        isOrganizer && canCreateMeetings && !isReadOnly
    }

    private var canCancelMeetings: Bool {
        canMutateMeetings
    }

    private var shouldShowLoading: Bool {
        !isPreviewing && viewModel.isLoading
    }

    private var meetings: [VirtualMeeting] {
        previewMeetings ?? viewModel.meetings
    }

    private var meetingsWithLinksCount: Int {
        meetings.filter { !$0.meetingUrl.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }.count
    }

    private var liveMeetingsCount: Int {
        meetings.filter { $0.status == .started }.count
    }

    private var meetingOverviewNextActionText: String {
        if liveMeetingsCount > 0 {
            return String(localized: "meetings.overview.next_action_join")
        }

        if meetingsWithLinksCount < meetings.count {
            return String(localized: "meetings.overview.next_action_generate")
        }

        return String(localized: "meetings.overview.next_action_share")
    }
}

private struct MeetingSyncBanner: View {
    let pendingSync: Bool
    let isOnline: Bool

    var body: some View {
        if pendingSync || !isOnline {
            WakeveContentCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.lg, padding: WakeveTheme.Spacing.md) {
                Label(
                    pendingSync ? String(localized: "meetings.sync.pending") : String(localized: "meetings.sync.offline"),
                    systemImage: "arrow.triangle.2.circlepath"
                )
                .font(WakeveTheme.Typography.metadata.weight(.semibold))
                .foregroundColor(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

private struct MeetingOverviewCard: View {
    let totalCount: Int
    let readyCount: Int
    let liveCount: Int
    let nextActionText: String

    var body: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack(alignment: .top, spacing: WakeveTheme.Spacing.md) {
                    Image(systemName: "video.bubble.left.fill")
                        .font(.title3.weight(.bold))
                        .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
                        .frame(width: 46, height: 46)
                        .background(WakeveTheme.ColorToken.permissionBlue.opacity(0.14))
                        .clipShape(Circle())

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text(String(localized: "meetings.overview.title"))
                            .font(WakeveTheme.Typography.section)
                            .foregroundColor(.primary)

                        Text(String(localized: "meetings.overview.subtitle"))
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }

                HStack(spacing: WakeveTheme.Spacing.sm) {
                    MeetingOverviewMetric(
                        title: String(localized: "meetings.overview.metric.total"),
                        value: "\(totalCount)",
                        tint: WakeveTheme.ColorToken.permissionBlue
                    )
                    MeetingOverviewMetric(
                        title: String(localized: "meetings.overview.metric.ready"),
                        value: "\(readyCount)",
                        tint: WakeveColors.success
                    )
                    MeetingOverviewMetric(
                        title: String(localized: "meetings.overview.metric.live"),
                        value: "\(liveCount)",
                        tint: WakeveColors.warning
                    )
                }

                HStack(spacing: WakeveTheme.Spacing.sm) {
                    Text(String(localized: "meetings.overview.next_action_label"))
                        .font(WakeveTheme.Typography.tiny)
                        .foregroundColor(.secondary)
                        .textCase(.uppercase)

                    Text(nextActionText)
                        .font(WakeveTheme.Typography.callout.weight(.semibold))
                        .foregroundColor(.primary)
                        .lineLimit(2)
                        .minimumScaleFactor(0.82)

                    Spacer(minLength: 0)
                }
                .padding(.horizontal, WakeveTheme.Spacing.sm)
                .padding(.vertical, WakeveTheme.Spacing.xs)
                .background(Color.secondary.opacity(0.08), in: RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
            }
        }
    }
}

private struct MeetingOverviewMetric: View {
    let title: String
    let value: String
    let tint: Color

    var body: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
            Text(value)
                .font(WakeveTheme.Typography.rowTitle)
                .foregroundColor(tint)

            Text(title)
                .font(WakeveTheme.Typography.tiny)
                .foregroundColor(.secondary)
                .lineLimit(1)
                .minimumScaleFactor(0.74)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(WakeveTheme.Spacing.sm)
        .background(tint.opacity(0.12), in: RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
    }
}

private struct MeetingRowCard: View {
    let meeting: VirtualMeeting

    var body: some View {
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            MeetingRowView(meeting: meeting)
        }
    }
}

// MARK: - MeetingRowView

struct MeetingRowView: View {
    let meeting: VirtualMeeting

    var body: some View {
        HStack(spacing: 14) {
            // Platform icon
            ZStack {
                RoundedRectangle(cornerRadius: 10)
                    .fill(platformColor(meeting.platform).opacity(0.15))
                    .frame(width: 44, height: 44)
                Image(systemName: platformIcon(meeting.platform))
                    .font(.system(size: 18))
                    .foregroundStyle(platformColor(meeting.platform))
            }

            // Content
            VStack(alignment: .leading, spacing: 4) {
                Text(meeting.title)
                    .font(.subheadline.bold())
                    .lineLimit(1)
                    .minimumScaleFactor(0.78)
                HStack(spacing: 6) {
                    Text(platformDisplayName(meeting.platform))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                    Text("·")
                        .foregroundStyle(.secondary)
                    Text(formatScheduledFor(meeting.scheduledFor))
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            Spacer()

            // Status badge
            statusBadge(meeting.status)
        }
        .padding(.vertical, 4)
    }

    // MARK: - Helpers

    private func platformIcon(_ platform: MeetingPlatform) -> String {
        switch platform {
        case .zoom:       return "video.fill"
        case .googleMeet: return "video.circle.fill"
        case .facetime:   return "facetime"
        case .teams:      return "person.3.fill"
        case .webex:      return "antenna.radiowaves.left.and.right"
        default:          return "video"
        }
    }

    private func platformColor(_ platform: MeetingPlatform) -> Color {
        switch platform {
        case .zoom:       return .blue
        case .googleMeet: return .green
        case .facetime:   return .green
        case .teams:      return .purple
        case .webex:      return .orange
        default:          return .gray
        }
    }

    private func platformDisplayName(_ platform: MeetingPlatform) -> String {
        switch platform {
        case .zoom:       return "Zoom"
        case .googleMeet: return "Google Meet"
        case .facetime:   return "FaceTime"
        case .teams:      return "Teams"
        case .webex:      return "Webex"
        default:          return String(localized: "meetings.platform_other")
        }
    }

    private func formatScheduledFor(_ instant: Kotlinx_datetimeInstant) -> String {
        let ms = instant.toEpochMilliseconds()
        let date = Date(timeIntervalSince1970: Double(ms) / 1000)
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        formatter.locale = .current
        return formatter.string(from: date)
    }

    private func statusBadge(_ status: MeetingStatus_) -> some View {
        let (label, color): (String, Color) = {
            switch status {
            case .scheduled:  return (String(localized: "meetings.scheduled"), .blue)
            case .started:    return (String(localized: "meetings.started"), .green)
            case .ended:      return (String(localized: "meetings.ended"), .secondary)
            case .cancelled:  return (String(localized: "meetings.cancelled"), .red)
            default:          return (String(localized: "meetings.status_unknown"), .gray)
            }
        }()
        return Text(label)
            .font(.caption2.bold())
            .padding(.horizontal, 8)
            .padding(.vertical, 3)
            .background(color.opacity(0.15), in: Capsule())
            .foregroundStyle(color)
    }
}

// MARK: - Preview

#if DEBUG
#Preview("Meeting List - Light") {
    MeetingListView(eventId: "preview-event", previewMeetings: MeetingFactory.list)
        .preferredColorScheme(.light)
}

#Preview("Meeting List - Dark") {
    MeetingListView(eventId: "preview-event", previewMeetings: MeetingFactory.list)
        .preferredColorScheme(.dark)
}
#endif

// MARK: - CreateMeetingSheet

struct CreateMeetingSheet: View {
    let eventId: String
    let onSave: (MeetingPlatform, String) -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @State private var selectedPlatform: MeetingPlatform = .zoom

    private let platforms: [(MeetingPlatform, String, String)] = [
        (.zoom, "Zoom", "video.fill"),
        (.googleMeet, "Google Meet", "video.circle.fill"),
        (.facetime, "FaceTime", "facetime"),
        (.teams, "Teams", "person.3.fill"),
        (.webex, "Webex", "antenna.radiowaves.left.and.right"),
    ]

    var body: some View {
        NavigationStack {
            ZStack {
                WakeveScreenBackground(style: .grouped)

                ScrollView {
                    VStack(spacing: WakeveTheme.Spacing.lg) {
                        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
                            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                                Label(String(localized: "meetings.create_sheet_title"), systemImage: "video.badge.plus")
                                    .font(WakeveTheme.Typography.title2)
                                    .foregroundColor(.primary)

                                Text(String(localized: "meetings.create_sheet_subtitle"))
                                    .font(WakeveTheme.Typography.body)
                                    .foregroundColor(.secondary)
                                    .fixedSize(horizontal: false, vertical: true)
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }

                        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
                            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                                Text(String(localized: "meetings.field_title"))
                                    .font(WakeveTheme.Typography.bodySemibold)
                                    .foregroundColor(.primary)

                                TextField(String(localized: "meetings.title_placeholder"), text: $title)
                                    .textInputAutocapitalization(.sentences)
                                    .submitLabel(.done)
                                    .padding(.horizontal, WakeveTheme.Spacing.md)
                                    .frame(height: 52)
                                    .background(.thinMaterial, in: RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
                                    .accessibilityLabel(String(localized: "meetings.field_title"))
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }

                        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
                            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                                Text(String(localized: "meetings.platform"))
                                    .font(WakeveTheme.Typography.bodySemibold)
                                    .foregroundColor(.primary)

                                LazyVStack(spacing: WakeveTheme.Spacing.sm) {
                                    ForEach(platforms, id: \.0.name) { platform, label, icon in
                                        MeetingPlatformOptionCard(
                                            label: label,
                                            icon: icon,
                                            isSelected: selectedPlatform == platform
                                        ) {
                                            selectedPlatform = platform
                                            WakeveHaptics.selection()
                                        }
                                    }
                                }
                            }
                            .frame(maxWidth: .infinity, alignment: .leading)
                        }

                        WakeveActionButton(
                            String(localized: "meetings.generate"),
                            systemImage: "video.badge.plus",
                            variant: .primary
                        ) {
                            WakeveHaptics.success()
                            onSave(selectedPlatform, normalizedTitle)
                        }
                    }
                    .padding(WakeveTheme.Spacing.page)
                }
            }
            .navigationTitle(String(localized: "meetings.create_sheet_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(String(localized: "common.cancel")) { dismiss() }
                }
            }
        }
    }

    private var normalizedTitle: String {
        let trimmed = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return String(format: String(localized: "meetings.default_title_format"), String(eventId.prefix(4)))
        }
        return trimmed
    }
}

private struct MeetingPlatformOptionCard: View {
    let label: String
    let icon: String
    let isSelected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: WakeveTheme.Spacing.md) {
                Image(systemName: icon)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(isSelected ? WakeveTheme.ColorToken.permissionBlue : .secondary)
                    .frame(width: 38, height: 38)
                    .background(
                        isSelected ? WakeveTheme.ColorToken.permissionBlue.opacity(0.14) : Color.secondary.opacity(0.08),
                        in: Circle()
                    )

                Text(label)
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.primary)

                Spacer()

                Image(systemName: isSelected ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(isSelected ? WakeveTheme.ColorToken.permissionBlue : .secondary.opacity(0.5))
            }
            .padding(.horizontal, WakeveTheme.Spacing.md)
            .frame(height: 58)
            .background(.thinMaterial, in: RoundedRectangle(cornerRadius: WakeveTheme.Radius.lg, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: WakeveTheme.Radius.lg, style: .continuous)
                    .stroke(isSelected ? WakeveTheme.ColorToken.permissionBlue.opacity(0.55) : Color.clear, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }
}
