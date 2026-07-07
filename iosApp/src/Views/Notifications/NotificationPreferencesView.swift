import SwiftUI
import Shared
import UserNotifications
#if canImport(UIKit)
import UIKit
#endif

/// Notification preferences screen for iOS.
/// Allows users to configure notification types, quiet hours, sound and vibration.
/// Parity with Android NotificationPreferencesScreen.
struct NotificationPreferencesView: View {

    @StateObject private var viewModel: NotificationPreferencesViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.scenePhase) private var scenePhase
    @State private var systemPermissionStatus: UNAuthorizationStatus = .notDetermined

    init(userId: String) {
        _viewModel = StateObject(wrappedValue: NotificationPreferencesViewModel(userId: userId))
    }

#if DEBUG
    init(
        previewUserId: String,
        enabledTypeNames: Set<String>,
        quietHoursEnabled: Bool,
        soundEnabled: Bool,
        vibrationEnabled: Bool
    ) {
        _viewModel = StateObject(
            wrappedValue: NotificationPreferencesViewModel(
                userId: previewUserId,
                enabledTypeNames: enabledTypeNames,
                quietHoursEnabled: quietHoursEnabled,
                soundEnabled: soundEnabled,
                vibrationEnabled: vibrationEnabled
            )
        )
    }
#endif

    var body: some View {
        Form {
            systemPermissionSection

            // Section: Notification Types
            notificationTypesSection

            // Section: Quiet Hours
            quietHoursSection

            // Section: Sound & Vibration
            soundVibrationSection
        }
        .navigationTitle(String(localized: "notifications.settings.title"))
        .navigationBarTitleDisplayMode(.inline)
        .scrollContentBackground(.hidden)
        .background(WakeveScreenBackground(style: .grouped))
        .tint(WakeveTheme.ColorToken.permissionBlue)
        .onAppear {
            viewModel.load()
            refreshSystemPermissionStatus()
        }
        .onChange(of: scenePhase) { _, phase in
            guard phase == .active else { return }
            refreshSystemPermissionStatus()
        }
    }

    private var systemPermissionSection: some View {
        Section {
            HStack(alignment: .top, spacing: 12) {
                Image(systemName: systemPermissionIcon)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(systemPermissionColor)
                    .frame(width: 34, height: 34)
                    .background(systemPermissionColor.opacity(0.12))
                    .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.sm, style: .continuous))

                VStack(alignment: .leading, spacing: 4) {
                    Text(String(localized: "notifications.system_permission.title"))
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundStyle(.primary)

                    Text(systemPermissionDescription)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }

                Spacer(minLength: 8)

                if shouldShowOpenSettings {
                    Button(String(localized: "notifications.system_permission.open_settings")) {
                        openAppSettings()
                    }
                    .font(WakeveTheme.Typography.caption)
                    .buttonStyle(.bordered)
                }
            }
            .listRowBackground(WakeveTheme.ColorToken.cardFill(for: colorScheme))
        } footer: {
            Text(String(localized: "notifications.system_permission.footer"))
        }
    }

    // MARK: - Notification Types Section

    private var notificationTypesSection: some View {
        Section {
            ForEach(viewModel.typeToggles) { toggle in
                ToggleRow(
                    title: toggle.displayName,
                    icon: toggle.iconName,
                    isOn: binding(for: toggle)
                )
                .listRowInsets(EdgeInsets(top: 8, leading: 16, bottom: 8, trailing: 16))
                .listRowBackground(WakeveTheme.ColorToken.cardFill(for: colorScheme))
            }
        } header: {
            Text(String(localized: "notifications.settings.types"))
        } footer: {
            Text(String(localized: "notifications.settings.types_footer"))
        }
    }

    // MARK: - Quiet Hours Section

    private var quietHoursSection: some View {
        Section {
            Toggle(String(localized: "notifications.settings.quiet_hours.enable"), isOn: $viewModel.quietHoursEnabled)
                .tint(WakeveTheme.ColorToken.permissionBlue)
                .listRowBackground(WakeveTheme.ColorToken.cardFill(for: colorScheme))

            if viewModel.quietHoursEnabled {
                HStack {
                    Text(String(localized: "notifications.settings.quiet_hours.start"))
                    Spacer()
                    DatePicker("", selection: $viewModel.quietHoursStart, displayedComponents: .hourAndMinute)
                        .labelsHidden()
                }
                .listRowBackground(WakeveTheme.ColorToken.cardFill(for: colorScheme))

                HStack {
                    Text(String(localized: "notifications.settings.quiet_hours.end"))
                    Spacer()
                    DatePicker("", selection: $viewModel.quietHoursEnd, displayedComponents: .hourAndMinute)
                        .labelsHidden()
                }
                .listRowBackground(WakeveTheme.ColorToken.cardFill(for: colorScheme))
            }
        } header: {
            Text(String(localized: "notifications.settings.quiet_hours.title"))
        } footer: {
            Text(String(localized: "notifications.settings.quiet_hours.footer"))
        }
        .onChange(of: viewModel.quietHoursEnabled) { _, _ in viewModel.save() }
        .onChange(of: viewModel.quietHoursStart) { _, _ in viewModel.save() }
        .onChange(of: viewModel.quietHoursEnd) { _, _ in viewModel.save() }
    }

    // MARK: - Sound & Vibration

    private var soundVibrationSection: some View {
        Section {
            Toggle(String(localized: "notifications.settings.sound"), isOn: $viewModel.soundEnabled)
                .tint(WakeveTheme.ColorToken.permissionBlue)
                .onChange(of: viewModel.soundEnabled) { _, _ in viewModel.save() }
                .listRowBackground(WakeveTheme.ColorToken.cardFill(for: colorScheme))

            Toggle(String(localized: "notifications.settings.vibration"), isOn: $viewModel.vibrationEnabled)
                .tint(WakeveTheme.ColorToken.permissionBlue)
                .onChange(of: viewModel.vibrationEnabled) { _, _ in viewModel.save() }
                .listRowBackground(WakeveTheme.ColorToken.cardFill(for: colorScheme))
        } header: {
            Text(String(localized: "notifications.settings.sound_vibration"))
        }
    }

    // MARK: - Binding Helper

    private func binding(for toggle: NotificationTypeToggle) -> Binding<Bool> {
        Binding<Bool>(
            get: {
                viewModel.isEnabled(toggle.rawName)
            },
            set: { newValue in
                viewModel.toggleType(toggle.rawName, enabled: newValue)
            }
        )
    }

    private var systemPermissionIcon: String {
        switch systemPermissionStatus {
        case .authorized, .provisional, .ephemeral:
            return "checkmark.circle.fill"
        case .denied:
            return "exclamationmark.triangle.fill"
        case .notDetermined:
            return "bell.badge"
        @unknown default:
            return "questionmark.circle.fill"
        }
    }

    private var systemPermissionColor: Color {
        switch systemPermissionStatus {
        case .authorized, .provisional, .ephemeral:
            return WakeveColors.success
        case .denied:
            return WakeveColors.error
        case .notDetermined:
            return WakeveTheme.ColorToken.permissionBlue
        @unknown default:
            return WakeveColors.warning
        }
    }

    private var systemPermissionDescription: String {
        switch systemPermissionStatus {
        case .authorized:
            return String(localized: "notifications.system_permission.authorized")
        case .provisional:
            return String(localized: "notifications.system_permission.provisional")
        case .ephemeral:
            return String(localized: "notifications.system_permission.ephemeral")
        case .denied:
            return String(localized: "notifications.system_permission.denied")
        case .notDetermined:
            return String(localized: "notifications.system_permission.not_determined")
        @unknown default:
            return String(localized: "notifications.system_permission.unknown")
        }
    }

    private var shouldShowOpenSettings: Bool {
        systemPermissionStatus == .denied
    }

    private func refreshSystemPermissionStatus() {
        APNsService.shared.checkAuthorizationStatus { status in
            systemPermissionStatus = status
        }
    }

    private func openAppSettings() {
        #if canImport(UIKit)
        guard let url = URL(string: UIApplication.openSettingsURLString) else { return }
        UIApplication.shared.open(url)
        #endif
    }
}

// MARK: - Toggle Row

private struct ToggleRow: View {
    let title: String
    let icon: String
    @Binding var isOn: Bool

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 17, weight: .semibold))
                .foregroundStyle(WakeveTheme.ColorToken.permissionBlue)
                .frame(width: 34, height: 34)
                .background(WakeveTheme.ColorToken.permissionBlue.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.sm, style: .continuous))

            Text(title)
                .font(WakeveTheme.Typography.body)
                .foregroundStyle(.primary)
                .lineLimit(2)
                .minimumScaleFactor(0.88)

            Spacer()

            Toggle("", isOn: $isOn)
                .labelsHidden()
                .tint(WakeveTheme.ColorToken.permissionBlue)
        }
        .frame(minHeight: 48)
    }
}

// MARK: - Preview

#if DEBUG
#Preview("Notification Preferences - Light") {
    NavigationStack {
        NotificationPreferencesView(
            previewUserId: "preview-user",
            enabledTypeNames: [
                "EVENT_INVITE",
                "VOTE_REMINDER",
                "DATE_CONFIRMED",
                "NEW_COMMENT",
                "MENTION",
                "MEETING_REMINDER"
            ],
            quietHoursEnabled: true,
            soundEnabled: true,
            vibrationEnabled: true
        )
    }
    .preferredColorScheme(.light)
}

#Preview("Notification Preferences - Dark") {
    NavigationStack {
        NotificationPreferencesView(
            previewUserId: "preview-user",
            enabledTypeNames: [
                "EVENT_INVITE",
                "DATE_CONFIRMED",
                "MEETING_REMINDER"
            ],
            quietHoursEnabled: true,
            soundEnabled: false,
            vibrationEnabled: true
        )
    }
    .preferredColorScheme(.dark)
}
#endif
