import SwiftUI
import Shared

// MARK: - MeetingListView

struct MeetingListView: View {
    let eventId: String

    @StateObject private var viewModel: MeetingListViewModel
    @State private var showCreateSheet = false
    @State private var navigateToMeetingId: String? = nil

    init(eventId: String) {
        self.eventId = eventId
        self._viewModel = StateObject(wrappedValue: MeetingListViewModel(eventId: eventId))
    }

    var body: some View {
        NavigationStack {
            Group {
                if viewModel.isLoading {
                    loadingView
                } else if viewModel.isEmpty {
                    emptyView
                } else {
                    listView
                }
            }
            .navigationTitle("Réunions")
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button {
                        showCreateSheet = true
                    } label: {
                        Image(systemName: "plus")
                    }
                }
            }
            .navigationDestination(item: $navigateToMeetingId) { meetingId in
                MeetingDetailView(meetingId: meetingId, eventId: eventId)
            }
            .sheet(isPresented: $showCreateSheet) {
                CreateMeetingSheet(eventId: eventId) { platform, title in
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
        .onAppear { viewModel.loadMeetings() }
        .alert("Erreur", isPresented: .constant(viewModel.hasError)) {
            Button("OK") { viewModel.clearError() }
        } message: {
            Text(viewModel.errorMessage ?? "Une erreur est survenue")
        }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView().scaleEffect(1.2)
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
            Button {
                showCreateSheet = true
            } label: {
                Label("Créer une réunion", systemImage: "video.badge.plus")
                    .fontWeight(.semibold)
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
                    .background(.blue, in: RoundedRectangle(cornerRadius: 12))
                    .foregroundStyle(.white)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .padding()
    }

    // MARK: - List

    private var listView: some View {
        List {
            ForEach(viewModel.meetings, id: \.id) { meeting in
                MeetingRowView(meeting: meeting)
                    .contentShape(Rectangle())
                    .onTapGesture {
                        navigateToMeetingId = meeting.id
                    }
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        Button(role: .destructive) {
                            viewModel.cancelMeeting(meetingId: meeting.id)
                        } label: {
                            Label("Annuler", systemImage: "xmark.circle")
                        }
                    }
            }
        }
        .listStyle(.insetGrouped)
        .refreshable {
            viewModel.loadMeetings()
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

#Preview {
    MeetingListView(eventId: "preview-event")
}

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
