import SwiftUI
import Shared

/// Notification preferences screen for iOS.
/// Allows users to configure notification types, quiet hours, sound and vibration.
/// Parity with Android NotificationPreferencesScreen.
struct NotificationPreferencesView: View {

    @StateObject private var viewModel: NotificationPreferencesViewModel
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme

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
            // Section: Notification Types
            notificationTypesSection

            // Section: Quiet Hours
            quietHoursSection

            // Section: Sound & Vibration
            soundVibrationSection
        }
        .navigationTitle("Notifications")
        .navigationBarTitleDisplayMode(.inline)
        .scrollContentBackground(.hidden)
        .background(WakeveScreenBackground(style: .grouped))
        .tint(WakeveTheme.ColorToken.permissionBlue)
        .onAppear { viewModel.load() }
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
            Text("Types de notification")
        } footer: {
            Text("Désactivez les types de notification que vous ne souhaitez pas recevoir.")
        }
    }

    // MARK: - Quiet Hours Section

    private var quietHoursSection: some View {
        Section {
            Toggle("Activer les heures silencieuses", isOn: $viewModel.quietHoursEnabled)
                .tint(WakeveTheme.ColorToken.permissionBlue)
                .listRowBackground(WakeveTheme.ColorToken.cardFill(for: colorScheme))

            if viewModel.quietHoursEnabled {
                HStack {
                    Text("Début")
                    Spacer()
                    DatePicker("", selection: $viewModel.quietHoursStart, displayedComponents: .hourAndMinute)
                        .labelsHidden()
                }
                .listRowBackground(WakeveTheme.ColorToken.cardFill(for: colorScheme))

                HStack {
                    Text("Fin")
                    Spacer()
                    DatePicker("", selection: $viewModel.quietHoursEnd, displayedComponents: .hourAndMinute)
                        .labelsHidden()
                }
                .listRowBackground(WakeveTheme.ColorToken.cardFill(for: colorScheme))
            }
        } header: {
            Text("Heures silencieuses")
        } footer: {
            Text("Les notifications non urgentes seront mises en pause pendant ces heures. Les rappels de réunion restent actifs.")
        }
        .onChange(of: viewModel.quietHoursEnabled) { _, _ in viewModel.save() }
        .onChange(of: viewModel.quietHoursStart) { _, _ in viewModel.save() }
        .onChange(of: viewModel.quietHoursEnd) { _, _ in viewModel.save() }
    }

    // MARK: - Sound & Vibration

    private var soundVibrationSection: some View {
        Section {
            Toggle("Son", isOn: $viewModel.soundEnabled)
                .tint(WakeveTheme.ColorToken.permissionBlue)
                .onChange(of: viewModel.soundEnabled) { _, _ in viewModel.save() }
                .listRowBackground(WakeveTheme.ColorToken.cardFill(for: colorScheme))

            Toggle("Vibration", isOn: $viewModel.vibrationEnabled)
                .tint(WakeveTheme.ColorToken.permissionBlue)
                .onChange(of: viewModel.vibrationEnabled) { _, _ in viewModel.save() }
                .listRowBackground(WakeveTheme.ColorToken.cardFill(for: colorScheme))
        } header: {
            Text("Son et vibration")
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
