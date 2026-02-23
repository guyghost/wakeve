import SwiftUI
import Shared

/**
 * MeetingEditSheet - Sheet for editing meeting details
 *
 * Allows organizers to:
 * - Edit meeting title and description
 * - Change scheduled date/time
 * - Adjust meeting duration
 * - Select meeting platform
 *
 * Uses Liquid Glass design system.
 */

struct MeetingEditSheet: View {
    let meeting: VirtualMeeting
    let onSave: (String, String?, Date, Int64) -> Void
    let onCancel: () -> Void

    @State private var title: String
    @State private var description: String
    @State private var scheduledFor: Date
    @State private var durationHours: Int
    @State private var durationMinutes: Int
    @State private var selectedPlatform: Shared.MeetingPlatform
    @State private var showDatePicker = false

    init(
        meeting: VirtualMeeting,
        onSave: @escaping (String, String?, Date, Int64) -> Void,
        onCancel: @escaping () -> Void
    ) {
        self.meeting = meeting
        self.onSave = onSave
        self.onCancel = onCancel

        // Initialize state from meeting data
        self._title = State(initialValue: meeting.title)
        self._description = State(initialValue: meeting.description)
        // Convert Kotlinx_datetimeInstant to Date using static method
        let instant = meeting.scheduledFor
        let epochSeconds = instant.epochSeconds
        self._scheduledFor = State(initialValue: Date(timeIntervalSince1970: TimeInterval(epochSeconds)))

        let durationMs = meeting.duration
        let totalMinutes = Int64(durationMs) / 60_000_000_000
        self._durationHours = State(initialValue: Int(totalMinutes / 60))
        self._durationMinutes = State(initialValue: Int(totalMinutes % 60))
        self._selectedPlatform = State(initialValue: meeting.platform)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemGroupedBackground)
                    .ignoresSafeArea()

                ScrollView {
                    VStack(spacing: 20) {
                        // Title section
                        titleSection

                        // Description section
                        descriptionSection

                        // Date and time section
                        dateTimeSection

                        // Duration section
                        durationSection

                        // Platform section
                        platformSection

                        Spacer()
                            .frame(height: 20)
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 16)
                }
            }
            .navigationTitle(String(localized: "meetings.edit"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(String(localized: "common.cancel"), action: onCancel)
                        .foregroundColor(.secondary)
                }
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(String(localized: "common.save"), action: saveMeeting)
                        .fontWeight(.semibold)
                        .disabled(title.trimmingCharacters(in: .whitespaces).isEmpty)
                }
            }
        }
        .presentationDetents([.medium, .large])
    }

    // MARK: - Title Section

    private var titleSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(String(localized: "meetings.field_title"))
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.secondary)

            LiquidGlassTextField(
                placeholder: String(localized: "meetings.title_placeholder"),
                text: $title,
                isSecure: false
            )
            .accessibilityLabel(String(localized: "meetings.title_placeholder"))
        }
    }

    // MARK: - Description Section

    private var descriptionSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(String(localized: "meetings.description_optional"))
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.secondary)

            TextEditor(text: $description)
                .font(.body)
                .foregroundColor(.primary)
                .padding(12)
                .frame(minHeight: 100)
                .background(Color.wakeveSurfaceLight)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(Color.wakeveBorderLight, lineWidth: 1)
                )
                .accessibilityLabel(String(localized: "meetings.description_label"))
        }
    }

    // MARK: - Date and Time Section

    private var dateTimeSection: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(String(localized: "meetings.date_time"))
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.secondary)

            Button(action: {
                showDatePicker.toggle()
            }) {
                HStack {
                    Image(systemName: "calendar")
                        .foregroundColor(.wakevePrimary)

                    Text(formattedDateTime)
                        .font(.body)
                        .foregroundColor(.primary)

                    Spacer()

                    Image(systemName: "chevron.down")
                        .foregroundColor(.secondary)
                        .font(.system(size: 14))
                }
                .padding(12)
                .background(Color.wakeveSurfaceLight)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: 12, style: .continuous)
                        .stroke(Color.wakeveBorderLight, lineWidth: 1)
                )
            }
            .buttonStyle(PlainButtonStyle())
            .accessibilityLabel(String(localized: "meetings.date_time_label"))
            .accessibilityHint(formattedDateTime)
            .sheet(isPresented: $showDatePicker) {
                DatePickerSheet(
                    selectedDate: $scheduledFor,
                    onConfirm: {
                        showDatePicker = false
                    },
                    onCancel: {
                        showDatePicker = false
                    }
                )
            }
        }
    }

    // MARK: - Duration Section

    private var durationSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "meetings.duration"))
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.secondary)

            HStack(spacing: 12) {
                // Hours picker
                durationPicker(
                    label: String(localized: "meetings.hours"),
                    value: $durationHours,
                    range: 0...24
                )

                Text("h")
                    .font(.title2)
                    .foregroundColor(.primary)
                    .padding(.top, 8)

                // Minutes picker
                durationPicker(
                    label: String(localized: "meetings.minutes"),
                    value: $durationMinutes,
                    range: 0...59
                )

                Text("min")
                    .font(.title2)
                    .foregroundColor(.primary)
                    .padding(.top, 8)
            }
        }
    }

    // MARK: - Duration Picker

    private func durationPicker(label: String, value: Binding<Int>, range: ClosedRange<Int>) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label)
                .font(.system(size: 13))
                .foregroundColor(.secondary)

            HStack(spacing: 0) {
                Button(action: {
                    if value.wrappedValue > range.lowerBound {
                        value.wrappedValue -= 1
                    }
                }) {
                    Image(systemName: "minus.circle.fill")
                        .font(.system(size: 32))
                        .foregroundColor(value.wrappedValue > range.lowerBound ? .wakevePrimary : .gray.opacity(0.3))
                }
                .buttonStyle(PlainButtonStyle())

                Text("\(value.wrappedValue)")
                    .font(.system(size: 32, weight: .semibold))
                    .foregroundColor(.primary)
                    .frame(width: 60)

                Button(action: {
                    if value.wrappedValue < range.upperBound {
                        value.wrappedValue += 1
                    }
                }) {
                    Image(systemName: "plus.circle.fill")
                        .font(.system(size: 32))
                        .foregroundColor(value.wrappedValue < range.upperBound ? .wakevePrimary : .gray.opacity(0.3))
                }
                .buttonStyle(PlainButtonStyle())
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
        .background(Color.wakeveSurfaceLight)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay(
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(Color.wakeveBorderLight, lineWidth: 1)
        )
    }

    // MARK: - Platform Section

    private var platformSection: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text(String(localized: "meetings.platform"))
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(.secondary)

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
            PlatformOptionContent(
                platform: platform,
                isSelected: selectedPlatform == platform
            )
        }
        .buttonStyle(PlainButtonStyle())
        .accessibilityLabel(platformName(for: platform))
        .accessibilityAddTraits(selectedPlatform == platform ? .isSelected : [])
    }

    // MARK: - Helpers

    private func saveMeeting() {
        let totalMinutes = Int64(durationHours * 60 + durationMinutes)
        onSave(title, description.isEmpty ? nil : description, scheduledFor, totalMinutes)
    }

    private var formattedDateTime: String {
        let formatter = DateFormatter()
        formatter.dateFormat = "dd MMM yyyy, HH:mm"
        formatter.locale = Locale(identifier: "fr_FR")
        return formatter.string(from: scheduledFor)
    }

    private func convertInstantToDate(_ instant: Kotlinx_datetimeInstant) -> Date {
        let epochSeconds = instant.epochSeconds
        return Date(timeIntervalSince1970: TimeInterval(epochSeconds))
    }

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
        case .zoom: return .wakevePrimary
        case .googleMeet: return .wakeveSuccess
        case .facetime: return .wakeveAccent
        case .teams: return .iOSSystemBlue
        case .webex: return .iOSSystemGreen
        default: return .wakevePrimary
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

// MARK: - Platform Option Content

private struct PlatformOptionContent: View {
    let platform: Shared.MeetingPlatform
    let isSelected: Bool
    
    var body: some View {
        VStack(spacing: 8) {
            ZStack {
                Circle()
                    .fill(platformColor.opacity(0.15))
                    .frame(width: 56, height: 56)
                
                Image(systemName: platformIcon)
                    .font(.system(size: 24))
                    .foregroundColor(platformColor)
            }
            
            Text(platformName)
                .font(.caption.weight(.medium))
                .foregroundColor(isSelected ? .wakevePrimary : .secondary)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 12)
        .background(backgroundView)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .overlay(borderOverlay)
    }
    
    @ViewBuilder
    private var backgroundView: some View {
        if isSelected {
            LinearGradient(
                gradient: Gradient(colors: [
                    Color.wakevePrimary.opacity(0.15),
                    Color.wakeveAccent.opacity(0.15)
                ]),
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
        } else {
            Color.wakeveSurfaceLight
        }
    }
    
    @ViewBuilder
    private var borderOverlay: some View {
        if isSelected {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(
                    LinearGradient(
                        gradient: Gradient(colors: [
                            Color.wakevePrimary.opacity(0.5),
                            Color.wakeveAccent.opacity(0.5)
                        ]),
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    ),
                    lineWidth: 2
                )
        } else {
            RoundedRectangle(cornerRadius: 12, style: .continuous)
                .stroke(Color.wakeveBorderLight, lineWidth: 1)
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
        case .zoom: return .wakevePrimary
        case .googleMeet: return .wakeveSuccess
        case .facetime: return .wakeveAccent
        case .teams: return .iOSSystemBlue
        case .webex: return .iOSSystemGreen
        default: return .wakevePrimary
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

// MARK: - DatePicker Sheet

struct DatePickerSheet: View {
    @Binding var selectedDate: Date
    let onConfirm: () -> Void
    let onCancel: () -> Void

    var body: some View {
        NavigationStack {
            VStack(spacing: 20) {
                DatePicker(
                    String(localized: "meetings.date_time"),
                    selection: $selectedDate,
                    displayedComponents: [.date, .hourAndMinute]
                )
                .datePickerStyle(.graphical)
                .labelsHidden()
                .padding(.horizontal, 20)
                .padding(.top, 20)

                Spacer()

                HStack(spacing: 12) {
                    LiquidGlassButton(
                        title: String(localized: "common.cancel"),
                        style: .secondary,
                        action: onCancel
                    )

                    LiquidGlassButton(
                        title: String(localized: "common.confirm"),
                        style: .primary,
                        action: onConfirm
                    )
                }
                .padding(.horizontal, 20)
                .padding(.bottom, 20)
            }
            .navigationTitle(String(localized: "meetings.select_date"))
            .navigationBarTitleDisplayMode(.inline)
        }
    }
}

// MARK: - Preview

struct MeetingEditSheet_Previews: PreviewProvider {
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

        return MeetingEditSheet(
            meeting: sampleMeeting,
            onSave: { _, _, _, _ in },
            onCancel: { }
        )
        .previewDisplayName("Meeting Edit Sheet")
    }
}
