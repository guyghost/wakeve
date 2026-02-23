import SwiftUI

// MARK: - Preference Toggle Model

struct NotificationPreferenceItem: Identifiable {
    let id: String
    let label: String
    let description: String
    let icon: String
    var isEnabled: Bool
}

// MARK: - Notification Preferences View

struct NotificationPreferencesView: View {
    let onDismiss: () -> Void

    @State private var preferences: [NotificationPreferenceItem] = [
        NotificationPreferenceItem(
            id: "votes",
            label: String(localized: "notifications.preferences.votes"),
            description: String(localized: "notifications.preferences.votes_desc"),
            icon: "chart.bar.fill",
            isEnabled: true
        ),
        NotificationPreferenceItem(
            id: "comments",
            label: String(localized: "notifications.preferences.comments"),
            description: String(localized: "notifications.preferences.comments_desc"),
            icon: "bubble.left.fill",
            isEnabled: true
        ),
        NotificationPreferenceItem(
            id: "status_changes",
            label: String(localized: "notifications.preferences.status_changes"),
            description: String(localized: "notifications.preferences.status_changes_desc"),
            icon: "arrow.triangle.2.circlepath",
            isEnabled: true
        ),
        NotificationPreferenceItem(
            id: "reminders",
            label: String(localized: "notifications.preferences.reminders"),
            description: String(localized: "notifications.preferences.reminders_desc"),
            icon: "bell.fill",
            isEnabled: true
        ),
        NotificationPreferenceItem(
            id: "deadlines",
            label: String(localized: "notifications.preferences.deadlines"),
            description: String(localized: "notifications.preferences.deadlines_desc"),
            icon: "clock.fill",
            isEnabled: true
        ),
        NotificationPreferenceItem(
            id: "weekly_digest",
            label: String(localized: "notifications.preferences.weekly_digest"),
            description: String(localized: "notifications.preferences.weekly_digest_desc"),
            icon: "newspaper.fill",
            isEnabled: false
        )
    ]

    @State private var soundEnabled = true
    @State private var quietHoursEnabled = false
    @State private var quietHoursStart = createTime(hour: 22, minute: 0)
    @State private var quietHoursEnd = createTime(hour: 7, minute: 0)
    @State private var isSaving = false

    var body: some View {
        NavigationStack {
            List {
                // Section types de notification
                Section {
                    ForEach($preferences) { $item in
                        HStack(spacing: 14) {
                            Image(systemName: item.icon)
                                .font(.system(size: 18))
                                .foregroundColor(item.isEnabled ? .accentColor : .secondary)
                                .frame(width: 24)

                            VStack(alignment: .leading, spacing: 2) {
                                Text(item.label)
                                    .font(.system(size: 16, weight: .medium))

                                Text(item.description)
                                    .font(.system(size: 13))
                                    .foregroundColor(.secondary)
                                    .lineLimit(2)
                            }

                            Spacer()

                            Toggle("", isOn: $item.isEnabled)
                                .labelsHidden()
                                .tint(.accentColor)
                        }
                        .padding(.vertical, 4)
                    }
                } header: {
                    Text(String(localized: "notifications.preferences.types_header"))
                        .font(.system(size: 13, weight: .semibold))
                        .textCase(nil)
                }

                // Section son
                Section {
                    Toggle(isOn: $soundEnabled) {
                        HStack(spacing: 14) {
                            Image(systemName: soundEnabled ? "speaker.wave.2.fill" : "speaker.slash.fill")
                                .font(.system(size: 18))
                                .foregroundColor(soundEnabled ? .accentColor : .secondary)
                                .frame(width: 24)

                            Text(String(localized: "notifications.preferences.sound"))
                                .font(.system(size: 16, weight: .medium))
                        }
                    }
                    .tint(.accentColor)
                } header: {
                    Text(String(localized: "notifications.preferences.general"))
                        .font(.system(size: 13, weight: .semibold))
                        .textCase(nil)
                }

                // Section heures silencieuses
                Section {
                    Toggle(isOn: $quietHoursEnabled) {
                        HStack(spacing: 14) {
                            Image(systemName: "moon.fill")
                                .font(.system(size: 18))
                                .foregroundColor(quietHoursEnabled ? .purple : .secondary)
                                .frame(width: 24)

                            Text(String(localized: "notifications.preferences.quiet_hours"))
                                .font(.system(size: 16, weight: .medium))
                        }
                    }
                    .tint(.accentColor)

                    if quietHoursEnabled {
                        DatePicker(
                            String(localized: "notifications.preferences.start"),
                            selection: $quietHoursStart,
                            displayedComponents: .hourAndMinute
                        )

                        DatePicker(
                            String(localized: "notifications.preferences.end"),
                            selection: $quietHoursEnd,
                            displayedComponents: .hourAndMinute
                        )
                    }
                } header: {
                    Text(String(localized: "notifications.preferences.dnd"))
                        .font(.system(size: 13, weight: .semibold))
                        .textCase(nil)
                } footer: {
                    if quietHoursEnabled {
                        Text(String(localized: "notifications.preferences.muted_footer"))
                            .font(.system(size: 12))
                    }
                }
            }
            .navigationTitle(String(localized: "notifications.preferences.title"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: onDismiss) {
                        Image(systemName: "chevron.left")
                            .foregroundColor(.accentColor)
                    }
                }

                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: savePreferences) {
                        if isSaving {
                            ProgressView()
                                .progressViewStyle(CircularProgressViewStyle())
                        } else {
                            Text(String(localized: "notifications.preferences.save"))
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundColor(.accentColor)
                        }
                    }
                    .disabled(isSaving)
                }
            }
        }
    }

    // MARK: - Actions

    private func savePreferences() {
        isSaving = true
        // TODO: Appeler l'API PUT /api/notifications/preferences
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.5) {
            isSaving = false
        }
    }
}

// MARK: - Helper

private func createTime(hour: Int, minute: Int) -> Date {
    let calendar = Calendar.current
    var components = calendar.dateComponents([.year, .month, .day], from: Date())
    components.hour = hour
    components.minute = minute
    return calendar.date(from: components) ?? Date()
}

// MARK: - Preview

#Preview("NotificationPreferencesView") {
    NotificationPreferencesView {
        print("Dismiss")
    }
}
