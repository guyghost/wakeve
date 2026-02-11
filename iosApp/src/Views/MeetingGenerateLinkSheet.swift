import SwiftUI
import Shared

/**
 * MeetingGenerateLinkSheet - Sheet for generating meeting links
 *
 * Allows organizers to select a platform and generate a new meeting link.
 * Uses Liquid Glass design system.
 */

struct MeetingGenerateLinkSheet: View {
    let meeting: VirtualMeeting
    let onGenerate: (Shared.MeetingPlatform) -> Void
    let onCancel: () -> Void

    @State private var selectedPlatform: Shared.MeetingPlatform
    @State private var isGenerating = false

    init(
        meeting: VirtualMeeting,
        onGenerate: @escaping (Shared.MeetingPlatform) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.meeting = meeting
        self.onGenerate = onGenerate
        self.onCancel = onCancel
        self._selectedPlatform = State(initialValue: meeting.platform)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 24) {
                        // Header
                        headerSection

                        // Platform selection
                        platformSelectionSection

                        // Info text
                        infoSection

                        Spacer()
                            .frame(height: 20)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                }
            }
            .navigationTitle("Régénérer le lien")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button("Annuler", action: onCancel)
                        .foregroundColor(.secondary)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button("Générer") {
                        isGenerating = true
                        onGenerate(selectedPlatform)
                    }
                    .fontWeight(.semibold)
                    .disabled(isGenerating)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    // MARK: - Header Section

    private var headerSection: some View {
        VStack(spacing: 12) {
            Image(systemName: "link.circle.fill")
                .font(.system(size: 48))
                .foregroundColor(.wakevPrimary)

            Text("Sélectionnez une plateforme")
                .font(.title2.weight(.semibold))
                .foregroundColor(.primary)
                .multilineTextAlignment(.center)

            Text("Un nouveau lien de réunion sera généré pour la plateforme sélectionnée.")
                .font(.body)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
        }
        .padding(.vertical, 8)
    }

    // MARK: - Platform Selection Section

    private var platformSelectionSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            Text("Plateforme")
                .font(.system(size: 15, weight: .semibold))
                .foregroundColor(.primary)

            LazyVGrid(columns: [
                GridItem(.flexible(), spacing: 12),
                GridItem(.flexible(), spacing: 12)
            ], spacing: 12) {
                ForEach([Shared.MeetingPlatform.zoom,
                          Shared.MeetingPlatform.googleMeet,
                          Shared.MeetingPlatform.facetime,
                          Shared.MeetingPlatform.teams,
                          Shared.MeetingPlatform.webex], id: \.self) { platform in
                    platformOption(platform: platform)
                }
            }
        }
    }

    // MARK: - Platform Option

    private func platformOption(platform: Shared.MeetingPlatform) -> some View {
        Button(action: {
            selectedPlatform = platform
        }) {
            GenerateLinkPlatformOption(
                platform: platform,
                isSelected: selectedPlatform == platform
            )
        }
        .buttonStyle(PlainButtonStyle())
        .accessibilityLabel(platformName(for: platform))
        .accessibilityAddTraits(selectedPlatform == platform ? .isSelected : [])
        .accessibilityHint(selectedPlatform == platform ? "Sélectionné" : "Double tap to select")
    }

    // MARK: - Info Section

    private var infoSection: some View {
        HStack(spacing: 12) {
            Image(systemName: "info.circle.fill")
                .foregroundColor(.wakevAccent)
                .font(.system(size: 20))

            Text("Le lien existant sera remplacé par le nouveau lien généré.")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.wakevAccent.opacity(0.1))
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }

    // MARK: - Helpers

    private func platformIcon(for platform: Shared.MeetingPlatform) -> String {
        switch platform {
        case .zoom: return "video.fill"
        case .googleMeet: return "video.badge.plus"
        case .facetime: return "video.fill"
        case .teams: return "video.fill"
        case .webex: return "video.fill"
        default: return "video.fill"
        }
    }

    private func platformColor(for platform: Shared.MeetingPlatform) -> Color {
        switch platform {
        case .zoom: return .wakevPrimary
        case .googleMeet: return .wakevSuccess
        case .facetime: return .wakevAccent
        case .teams: return .iOSSystemBlue
        case .webex: return .iOSSystemGreen
        default: return .wakevPrimary
        }
    }

    private func platformName(for platform: Shared.MeetingPlatform) -> String {
        switch platform {
        case .zoom: return "Zoom"
        case .googleMeet: return "Google Meet"
        case .facetime: return "FaceTime"
        case .teams: return "Microsoft Teams"
        case .webex: return "Webex"
        default: return "Meeting"
        }
    }
}

// MARK: - Generate Link Platform Option

private struct GenerateLinkPlatformOption: View {
    let platform: Shared.MeetingPlatform
    let isSelected: Bool
    
    var body: some View {
        VStack(spacing: 12) {
            ZStack {
                Circle()
                    .fill(platformColor.opacity(0.15))
                    .frame(width: 64, height: 64)
                
                Image(systemName: platformIcon)
                    .font(.system(size: 28))
                    .foregroundColor(platformColor)
            }
            
            VStack(spacing: 4) {
                Text(platformName)
                    .font(.subheadline.weight(.medium))
                    .foregroundColor(isSelected ? .wakevPrimary : .primary)
                
                if isSelected {
                    Text("Sélectionné")
                        .font(.caption)
                        .foregroundColor(.wakevPrimary)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(backgroundView)
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        .overlay(borderOverlay)
    }
    
    @ViewBuilder
    private var backgroundView: some View {
        if isSelected {
            LinearGradient(
                gradient: Gradient(colors: [
                    Color.wakevPrimary.opacity(0.15),
                    Color.wakevAccent.opacity(0.15)
                ]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        } else {
            Color.wakevSurfaceLight
        }
    }
    
    @ViewBuilder
    private var borderOverlay: some View {
        if isSelected {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(
                    LinearGradient(
                        gradient: Gradient(colors: [
                            Color.wakevPrimary.opacity(0.5),
                            Color.wakevAccent.opacity(0.5)
                        ]),
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 2
                )
        } else {
            RoundedRectangle(cornerRadius: 16, style: .continuous)
                .stroke(Color.wakevBorderLight, lineWidth: 1)
        }
    }
    
    private var platformIcon: String {
        switch platform {
        case .zoom: return "video.fill"
        case .googleMeet: return "video.badge.plus"
        case .facetime: return "video.fill"
        case .teams: return "video.fill"
        case .webex: return "video.fill"
        default: return "video.fill"
        }
    }
    
    private var platformColor: Color {
        switch platform {
        case .zoom: return .wakevPrimary
        case .googleMeet: return .wakevSuccess
        case .facetime: return .wakevAccent
        case .teams: return .iOSSystemBlue
        case .webex: return .iOSSystemGreen
        default: return .wakevPrimary
        }
    }
    
    private var platformName: String {
        switch platform {
        case .zoom: return "Zoom"
        case .googleMeet: return "Google Meet"
        case .facetime: return "FaceTime"
        case .teams: return "Microsoft Teams"
        case .webex: return "Webex"
        default: return "Meeting"
        }
    }
}

// MARK: - Preview

struct MeetingGenerateLinkSheet_Previews: PreviewProvider {
    static var previews: some View {
        // Create a sample meeting
        let sampleMeeting = VirtualMeeting(
            id: "meeting-1",
            eventId: "event-1",
            organizerId: "user-1",
            platform: .zoom,
            meetingId: "123456789",
            meetingPassword: nil,
            meetingUrl: "https://zoom.us/j/123456789",
            dialInNumber: nil,
            dialInPassword: nil,
            title: "Réunion de planification",
            description: "Discussion sur les détails de l'événement",
            scheduledFor: Kotlinx_datetimeInstant.companion.fromEpochSeconds(epochSeconds: Int64(Date().timeIntervalSince1970), nanosecondAdjustment: 0),
            duration: 3600000000000, // 1 hour
            timezone: TimeZone.current.identifier,
            participantLimit: nil,
            requirePassword: true,
            waitingRoom: true,
            hostKey: nil,
            createdAt: Kotlinx_datetimeInstant.companion.fromEpochSeconds(epochSeconds: Int64(Date().timeIntervalSince1970), nanosecondAdjustment: 0),
            status: .scheduled
        )

        return MeetingGenerateLinkSheet(
            meeting: sampleMeeting,
            onGenerate: { _ in },
            onCancel: { }
        )
        .previewDisplayName("Meeting Generate Link Sheet")
    }
}
