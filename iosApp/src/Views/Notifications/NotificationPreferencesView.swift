import SwiftUI
import Shared

/// Notification preferences screen for iOS.
/// Allows users to configure notification types, quiet hours, sound and vibration.
/// Parity with Android NotificationPreferencesScreen.
struct NotificationPreferencesView: View {

    @StateObject private var viewModel: NotificationPreferencesViewModel
    @Environment(\.dismiss) private var dismiss

    init(userId: String) {
        _viewModel = StateObject(wrappedValue: NotificationPreferencesViewModel(userId: userId))
    }

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
                .tint(Color.wakevePrimary)

            if viewModel.quietHoursEnabled {
                HStack {
                    Text("Début")
                    Spacer()
                    DatePicker("", selection: $viewModel.quietHoursStart, displayedComponents: .hourAndMinute)
                        .labelsHidden()
                }

                HStack {
                    Text("Fin")
                    Spacer()
                    DatePicker("", selection: $viewModel.quietHoursEnd, displayedComponents: .hourAndMinute)
                        .labelsHidden()
                }
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
                .tint(Color.wakevePrimary)
                .onChange(of: viewModel.soundEnabled) { _, _ in viewModel.save() }

            Toggle("Vibration", isOn: $viewModel.vibrationEnabled)
                .tint(Color.wakevePrimary)
                .onChange(of: viewModel.vibrationEnabled) { _, _ in viewModel.save() }
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
                .font(.title3)
                .foregroundStyle(Color.wakevePrimary)
                .frame(width: 28)

            Text(title)
                .font(.subheadline)

            Spacer()

            Toggle("", isOn: $isOn)
                .labelsHidden()
                .tint(Color.wakevePrimary)
        }
    }
}

// MARK: - Preview

#Preview {
    NavigationStack {
        NotificationPreferencesView(userId: "preview-user")
    }
}
