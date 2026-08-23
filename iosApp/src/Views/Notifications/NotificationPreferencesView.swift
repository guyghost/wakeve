import SwiftUI
import Shared

/// Notification preferences screen for iOS.
/// Allows users to configure notification types, quiet hours, sound and vibration.
/// Parity with Android NotificationPreferencesScreen.
struct NotificationPreferencesView: View {

    @StateObject private var viewModel: NotificationPreferencesViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    private let registrationAdapter: IosNotificationRegistrationAdapter?

    init(userId: String) {
        _viewModel = StateObject(wrappedValue: NotificationPreferencesViewModel(userId: userId))
        registrationAdapter = APNsService.shared.registrationAdapter
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
        registrationAdapter = APNsService.shared.registrationAdapter
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
        }
    }

    private var systemPermissionSection: some View {
        Section {
            if let registrationAdapter {
                IosNotificationRegistrationStatusView(adapter: registrationAdapter)
            }
        } footer: {
            Text(String(localized: "notifications.system_permission.footer"))
        }
        .listRowBackground(WakeveTheme.ColorToken.cardFill(for: colorScheme))
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

}

private struct IosNotificationRegistrationStatusView: View {
    @StateObject private var viewModel: IosNotificationRegistrationViewModel

    init(adapter: IosNotificationRegistrationAdapter) {
        _viewModel = StateObject(
            wrappedValue: IosNotificationRegistrationViewModel(adapter: adapter)
        )
    }

    private var presentation: IosNotificationRegistrationPresentation {
        viewModel.presentation
    }

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            Image(systemName: "bell.badge.fill")
                .font(.headline.weight(.semibold))
                .foregroundStyle(WakeveTheme.ColorToken.permissionBlue)
                .frame(width: 34, height: 34)
                .background(WakeveTheme.ColorToken.permissionBlue.opacity(0.12))
                .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.sm, style: .continuous))
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 4) {
                Text(localized(presentation.statusDescriptionKey))
                    .font(WakeveTheme.Typography.callout)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .accessibilityElement(children: .combine)
            .accessibilityIdentifier(presentation.statusAccessibilityIdentifier)
            .accessibilityLabel(localized(presentation.statusAccessibilityLabelKey))
            .accessibilityHint(localized(presentation.statusAccessibilityHintKey))

            Spacer(minLength: 8)

            primaryAction
        }
    }

    @ViewBuilder
    private var primaryAction: some View {
        let semantics = IosNotificationRegistrationViewModel.controlSemantics(for: presentation)
        if presentation.primaryEvent != nil,
           let titleKey = presentation.primaryActionTitleKey,
           presentation.primaryActionAccessibilityIdentifier != nil,
           presentation.primaryActionAccessibilityLabelKey != nil,
           let accessibilityIdentifier = semantics.accessibilityIdentifier,
           let accessibilityLabelKey = semantics.accessibilityLabelKey,
           let accessibilityHintKey = semantics.accessibilityHintKey {
            Button {
                viewModel.performPrimaryAction()
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "arrow.up.right.square")
                        .accessibilityHidden(semantics.decorativeIconHiddenFromVoiceOver)
                    Text(localizedPrimaryActionTitle(titleKey))
                        .font(WakeveTheme.Typography.caption)
                        .fixedSize(horizontal: false, vertical: semantics.supportsDynamicType)
                }
                .frame(minHeight: CGFloat(semantics.minimumHitTargetPoints))
                .contentShape(Rectangle())
            }
            .buttonStyle(.bordered)
            .accessibilityIdentifier(accessibilityIdentifier)
            .accessibilityLabel(localized(accessibilityLabelKey))
            .accessibilityHint(localized(accessibilityHintKey))
        }
    }

    private func localized(_ key: String) -> String {
        Bundle.main.localizedString(forKey: key, value: nil, table: nil)
    }

    private func localizedPrimaryActionTitle(_ key: String) -> String {
        let supportedKeys = [
            "notifications.system_permission.enable",
            "notifications.system_permission.open_settings"
        ]
        guard supportedKeys.contains(key) else { return "" }
        return localized(key)
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
