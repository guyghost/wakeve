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
            Group {
                if shouldShowLoading {
                    loadingView
                } else if meetings.isEmpty {
                    emptyView
                } else {
                    listView
                }
            }
            .navigationTitle("Réunions")
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
        .alert("Erreur", isPresented: .constant(viewModel.hasError)) {
            Button("OK") { viewModel.clearError() }
        } message: {
            Text(viewModel.errorMessage ?? "Une erreur est survenue")
        }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .scaleEffect(1.2)
                .accessibilityLabel(String(localized: "common.loading"))
            Text("Chargement des réunions…")
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Empty

    private var emptyView: some View {
        VStack(spacing: 20) {
            Image(systemName: "video.badge.plus")
                .font(.system(size: 56))
                .foregroundStyle(.secondary.opacity(0.5))
            Text("Aucune réunion")
                .font(.title3.bold())
            Text("Créez une réunion virtuelle pour\ncoordiner les participants.")
                .multilineTextAlignment(.center)
                .foregroundStyle(.secondary)
            if canCreateMeetings {
                Button {
                    guard canMutateMeetings else { return }
                    showCreateSheet = true
                } label: {
                    Label("Créer une réunion", systemImage: "video.badge.plus")
                        .fontWeight(.semibold)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 12)
                        .background(.blue, in: RoundedRectangle(cornerRadius: 12))
                        .foregroundStyle(.white)
                }
                .disabled(!canMutateMeetings)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }

    // MARK: - List

    private var listView: some View {
        List {
            MeetingSyncBanner(pendingSync: viewModel.pendingSync, isOnline: viewModel.isOnline)

            ForEach(meetings, id: \.id) { meeting in
                MeetingRowView(meeting: meeting)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        navigateToMeetingId = meeting.id
                    }
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        if canCancelMeetings {
                            Button(role: .destructive) {
                                guard !isPreviewing else { return }
                                viewModel.cancelMeeting(
                                    meetingId: meeting.id,
                                    currentUserId: currentUserId,
                                    isOrganizer: isOrganizer,
                                    isReadOnly: isReadOnly
                                )
                            } label: {
                                Label("Annuler", systemImage: "xmark.circle")
                            }
                        }
                    }
            }
        }
        .listStyle(.insetGrouped)
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
}

private struct MeetingSyncBanner: View {
    let pendingSync: Bool
    let isOnline: Bool

    var body: some View {
        if pendingSync || !isOnline {
            Label(
                pendingSync ? "Modifications locales en attente d'envoi" : "Données locales disponibles hors ligne",
                systemImage: "arrow.triangle.2.circlepath"
            )
            .font(.footnote.weight(.semibold))
            .padding(.vertical, 4)
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
        default:          return "Autre"
        }
    }

    private func formatScheduledFor(_ instant: Kotlinx_datetimeInstant) -> String {
        let ms = instant.toEpochMilliseconds()
        let date = Date(timeIntervalSince1970: Double(ms) / 1000)
        let formatter = DateFormatter()
        formatter.dateStyle = .short
        formatter.timeStyle = .short
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: date)
    }

    private func statusBadge(_ status: MeetingStatus_) -> some View {
        let (label, color): (String, Color) = {
            switch status {
            case .scheduled:  return ("Planifiée", .blue)
            case .started:    return ("En cours", .green)
            case .ended:      return ("Terminée", .secondary)
            case .cancelled:  return ("Annulée", .red)
            default:          return ("—", .gray)
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
        NavigationView {
            Form {
                Section("Titre") {
                    TextField("Nom de la réunion", text: $title)
                }
                Section("Plateforme") {
                    ForEach(platforms, id: \.0.name) { platform, label, icon in
                        Button {
                            selectedPlatform = platform
                        } label: {
                            HStack {
                                Label(label, systemImage: icon)
                                    .foregroundStyle(.primary)
                                Spacer()
                                if selectedPlatform == platform {
                                    Image(systemName: "checkmark")
                                        .foregroundStyle(.blue)
                                }
                            }
                        }
                    }
                }
            }
            .navigationTitle("Nouvelle réunion")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Annuler") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Créer") {
                        let meetingTitle = title.isEmpty ? "Réunion \(eventId.prefix(4))" : title
                        onSave(selectedPlatform, meetingTitle)
                    }
                    .fontWeight(.semibold)
                }
            }
        }
    }
}
