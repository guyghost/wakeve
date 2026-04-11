import SwiftUI
import Shared

// MARK: - MeetingDetailView

struct MeetingDetailView: View {
    let meetingId: String
    let eventId: String

    @StateObject private var viewModel: MeetingDetailViewModel
    @State private var showGenerateLinkSheet = false
    @State private var showDeleteConfirm = false
    @Environment(\.dismiss) private var dismiss

    init(meetingId: String, eventId: String) {
        self.meetingId = meetingId
        self.eventId = eventId
        self._viewModel = StateObject(wrappedValue: MeetingDetailViewModel(meetingId: meetingId))
    }

    var body: some View {
        Group {
            if viewModel.isLoading {
                loadingView
            } else if let meeting = viewModel.meeting {
                contentView(meeting: meeting)
            } else {
                errorView
            }
        }
        .navigationTitle("Réunion")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button {
                        showGenerateLinkSheet = true
                    } label: {
                        Label("Générer un lien", systemImage: "link")
                    }
                    Divider()
                    Button(role: .destructive) {
                        showDeleteConfirm = true
                    } label: {
                        Label("Annuler la réunion", systemImage: "xmark.circle")
                    }
                } label: {
                    Image(systemName: "ellipsis.circle")
                }
            }
        }
        .sheet(isPresented: $showGenerateLinkSheet) {
            if let meeting = viewModel.meeting {
                MeetingGenerateLinkSheet(
                    meeting: meeting,
                    onGenerate: { platform in
                        viewModel.generateMeetingLink(platform: platform)
                        showGenerateLinkSheet = false
                    },
                    onCancel: { showGenerateLinkSheet = false }
                )
            }
        }
        .confirmationDialog(
            "Annuler cette réunion ?",
            isPresented: $showDeleteConfirm,
            titleVisibility: .visible
        ) {
            Button("Annuler la réunion", role: .destructive) {
                viewModel.cancelMeeting()
                dismiss()
            }
            Button("Garder", role: .cancel) {}
        }
        .onAppear { viewModel.loadMeetings() }
    }

    // MARK: - Loading

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView().scaleEffect(1.2)
            Text("Chargement…")
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Error

    private var errorView: some View {
        VStack(spacing: 16) {
            Image(systemName: "exclamationmark.triangle.fill")
                .font(.largeTitle)
                .foregroundStyle(.orange)
            Text("Réunion introuvable")
                .font(.headline)
            Text("Cette réunion n'existe plus ou a été annulée.")
                .foregroundStyle(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding()
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    // MARK: - Content

    private func contentView(meeting: VirtualMeeting) -> some View {
        ScrollView {
            VStack(spacing: 16) {
                // Header card
                headerCard(meeting: meeting)

                // Meeting URL card
                if !meeting.meetingUrl.isEmpty {
                    meetingUrlCard(url: meeting.meetingUrl)
                }

                // Details card
                detailsCard(meeting: meeting)

                // Generated link (if any)
                // Note: link generation is handled via MeetingListViewModel

                // Action buttons
                actionButtons(meeting: meeting)
            }
            .padding()
        }
    }

    // MARK: - Cards

    private func headerCard(meeting: VirtualMeeting) -> some View {
        VStack(spacing: 12) {
            // Platform icon
            ZStack {
                Circle()
                    .fill(platformColor(meeting.platform).opacity(0.15))
                    .frame(width: 72, height: 72)
                Image(systemName: platformIcon(meeting.platform))
                    .font(.system(size: 32))
                    .foregroundStyle(platformColor(meeting.platform))
            }

            Text(meeting.title)
                .font(.title2.bold())
                .multilineTextAlignment(.center)

            // Status badge
            statusBadge(meeting.status)

            HStack(spacing: 16) {
                Label(platformDisplayName(meeting.platform), systemImage: platformIcon(meeting.platform))
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Label(formatScheduledFor(meeting.scheduledFor), systemImage: "calendar")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
        .frame(maxWidth: .infinity)
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
    }

    private func meetingUrlCard(url: String) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Image(systemName: "link")
                    .foregroundStyle(.blue)
                Text("Lien de la réunion")
                    .font(.headline)
                Spacer()
            }
            HStack {
                Text(url)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(2)
                Spacer()
                Button {
                    UIPasteboard.general.string = url
                } label: {
                    Image(systemName: "doc.on.doc")
                        .foregroundStyle(.blue)
                }
                if let meetingURL = URL(string: url) {
                    Link(destination: meetingURL) {
                        Image(systemName: "arrow.up.right.square")
                            .foregroundStyle(.blue)
                    }
                }
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
    }

    private func detailsCard(meeting: VirtualMeeting) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Image(systemName: "info.circle.fill")
                    .foregroundStyle(.blue)
                Text("Détails")
                    .font(.headline)
            }
            Divider()
            detailRow(icon: "clock", label: "Durée", value: "\(meeting.duration / 60) min")
            detailRow(icon: "globe", label: "Fuseau", value: meeting.timezone)
            if let desc = meeting.description_, !desc.isEmpty {
                detailRow(icon: "text.alignleft", label: "Description", value: desc)
            }
        }
        .padding()
        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
    }

    private func generatedLinkCard(linkResponse: MeetingLinkResponse) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(.green)
                Text("Lien généré")
                    .font(.headline)
                Spacer()
                Button("Copier") {
                    UIPasteboard.general.string = linkResponse.meetingUrl
                }
                .font(.caption)
                .buttonStyle(.bordered)
            }
            Text(linkResponse.meetingUrl)
                .font(.caption)
                .foregroundStyle(.secondary)
        }
        .padding()
        .background(Color.green.opacity(0.08), in: RoundedRectangle(cornerRadius: 16))
    }

    private func actionButtons(meeting: VirtualMeeting) -> some View {
        VStack(spacing: 12) {
            // Join meeting button
            if let url = URL(string: meeting.meetingUrl), !meeting.meetingUrl.isEmpty {
                Link(destination: url) {
                    HStack {
                        Image(systemName: "video.fill")
                        Text("Rejoindre la réunion")
                            .fontWeight(.semibold)
                    }
                    .frame(maxWidth: .infinity)
                    .padding()
                    .background(.blue, in: RoundedRectangle(cornerRadius: 12))
                    .foregroundStyle(.white)
                }
            }

            // Share button
            Button {
                shareLink(meeting.meetingUrl)
            } label: {
                HStack {
                    Image(systemName: "square.and.arrow.up")
                    Text("Partager le lien")
                }
                .frame(maxWidth: .infinity)
                .padding()
                .background(.blue.opacity(0.1), in: RoundedRectangle(cornerRadius: 12))
                .foregroundStyle(.blue)
            }
        }
    }

    // MARK: - Detail Row

    private func detailRow(icon: String, label: String, value: String) -> some View {
        HStack(alignment: .top) {
            Image(systemName: icon)
                .foregroundStyle(.secondary)
                .frame(width: 20)
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Text(value)
                    .font(.subheadline)
            }
        }
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
        formatter.dateStyle = .medium
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
            .font(.caption.bold())
            .padding(.horizontal, 12)
            .padding(.vertical, 5)
            .background(color.opacity(0.15), in: Capsule())
            .foregroundStyle(color)
    }

    private func shareLink(_ url: String) {
        guard !url.isEmpty else { return }
        let av = UIActivityViewController(activityItems: [url], applicationActivities: nil)
        if let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene,
           let root = scene.windows.first?.rootViewController {
            root.present(av, animated: true)
        }
    }
}

// MARK: - Preview

#Preview {
    NavigationStack {
        MeetingDetailView(meetingId: "preview-meeting", eventId: "preview-event")
    }
}
