import SwiftUI
import Shared

/**
 * MeetingGenerateLinkSheet - Sheet for generating meeting links
 *
 * Allows organizers to select a platform and generate a new meeting link.
 * Uses the Wakeve design system.
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
                WakeveScreenBackground(style: .grouped)

                ScrollView {
                    VStack(spacing: WakeveTheme.Spacing.lg) {
                        headerSection
                        platformSelectionSection
                        infoSection

                        WakeveActionButton(
                            String(localized: "meetings.generate"),
                            systemImage: "link",
                            variant: .primary,
                            isLoading: isGenerating
                        ) {
                            isGenerating = true
                            WakeveHaptics.success()
                            onGenerate(selectedPlatform)
                        }
                    }
                    .padding(WakeveTheme.Spacing.page)
                }
            }
            .navigationTitle(String(localized: "meetings.regenerate_link"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(String(localized: "common.cancel"), action: onCancel)
                        .foregroundColor(.secondary)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    // MARK: - Header Section

    private var headerSection: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
            VStack(spacing: WakeveTheme.Spacing.md) {
                Image(systemName: "link.circle.fill")
                    .font(.system(size: 44, weight: .semibold))
                    .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
                    .frame(width: 74, height: 74)
                    .background(WakeveTheme.ColorToken.permissionBlue.opacity(0.12))
                    .clipShape(Circle())

                Text(String(localized: "meetings.select_platform"))
                    .font(WakeveTheme.Typography.title2)
                    .foregroundColor(.primary)
                    .multilineTextAlignment(.center)

                Text(String(localized: "meetings.generate_link_description"))
                    .font(WakeveTheme.Typography.body)
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity)
        }
    }

    // MARK: - Platform Selection Section

    private var platformSelectionSection: some View {
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text(String(localized: "meetings.platform"))
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(.primary)

                LazyVGrid(columns: [
                    GridItem(.flexible(), spacing: WakeveTheme.Spacing.sm),
                    GridItem(.flexible(), spacing: WakeveTheme.Spacing.sm)
                ], spacing: WakeveTheme.Spacing.sm) {
                    ForEach([Shared.MeetingPlatform.zoom,
                              Shared.MeetingPlatform.googleMeet,
                              Shared.MeetingPlatform.facetime,
                              Shared.MeetingPlatform.teams,
                              Shared.MeetingPlatform.webex], id: \.self) { platform in
                        platformOption(platform: platform)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    // MARK: - Platform Option

    private func platformOption(platform: Shared.MeetingPlatform) -> some View {
        Button(action: {
            selectedPlatform = platform
            WakeveHaptics.selection()
        }) {
            GenerateLinkPlatformOption(
                platform: platform,
                isSelected: selectedPlatform == platform
            )
        }
        .buttonStyle(PlainButtonStyle())
        .accessibilityLabel(platformName(for: platform))
        .accessibilityAddTraits(selectedPlatform == platform ? .isSelected : [])
        .accessibilityHint(selectedPlatform == platform ? String(localized: "common.selected") : String(localized: "common.double_tap_to_select"))
    }

    // MARK: - Info Section

    private var infoSection: some View {
        WakeveContentCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.lg, padding: WakeveTheme.Spacing.md) {
            HStack(spacing: WakeveTheme.Spacing.sm) {
                Image(systemName: "info.circle.fill")
                    .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
                    .font(.system(size: 20))

                Text(String(localized: "meetings.generate_link_info"))
                    .font(WakeveTheme.Typography.metadata)
                    .foregroundColor(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    // MARK: - Helpers

    private func platformName(for platform: Shared.MeetingPlatform) -> String {
        switch platform {
        case .zoom: return "Zoom"
        case .googleMeet: return "Google Meet"
        case .facetime: return "FaceTime"
        case .teams: return "Microsoft Teams"
        case .webex: return "Webex"
        default: return String(localized: "meetings.platform_other")
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
                    .font(WakeveTheme.Typography.metadata.weight(.semibold))
                    .foregroundColor(isSelected ? WakeveTheme.ColorToken.permissionBlue : .primary)

                if isSelected {
                    Text(String(localized: "common.selected"))
                        .font(WakeveTheme.Typography.caption)
                        .foregroundColor(WakeveTheme.ColorToken.permissionBlue)
                }
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 16)
        .background(
            isSelected ? WakeveTheme.ColorToken.permissionBlue.opacity(0.10) : Color.secondary.opacity(0.08),
            in: RoundedRectangle(cornerRadius: WakeveTheme.Radius.lg, style: .continuous)
        )
        .overlay(
            RoundedRectangle(cornerRadius: WakeveTheme.Radius.lg, style: .continuous)
                .stroke(
                    isSelected ? WakeveTheme.ColorToken.permissionBlue.opacity(0.55) : Color.secondary.opacity(0.16),
                    lineWidth: isSelected ? 1.5 : 1
                )
        )
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
        case .zoom: return WakeveTheme.ColorToken.permissionBlue
        case .googleMeet: return .green
        case .facetime: return WakeveTheme.ColorToken.permissionBlue
        case .teams: return .purple
        case .webex: return .orange
        default: return WakeveTheme.ColorToken.permissionBlue
        }
    }

    private var platformName: String {
        switch platform {
        case .zoom: return "Zoom"
        case .googleMeet: return "Google Meet"
        case .facetime: return "FaceTime"
        case .teams: return "Microsoft Teams"
        case .webex: return "Webex"
        default: return String(localized: "meetings.platform_other")
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
            title: "Planning meeting",
            description: "Discussion about event details",
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

        return Group {
            MeetingGenerateLinkSheet(
                meeting: sampleMeeting,
                onGenerate: { _ in },
                onCancel: { }
            )
            .preferredColorScheme(.light)

            MeetingGenerateLinkSheet(
                meeting: sampleMeeting,
                onGenerate: { _ in },
                onCancel: { }
            )
            .preferredColorScheme(.dark)
        }
    }
}
