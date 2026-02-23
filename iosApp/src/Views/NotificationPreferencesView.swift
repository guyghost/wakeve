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
            label: "Votes",
            description: "Recevoir une notification lorsqu'un participant vote",
            icon: "chart.bar.fill",
            isEnabled: true
        ),
        NotificationPreferenceItem(
            id: "comments",
            label: "Commentaires",
            description: "Recevoir une notification pour les nouveaux commentaires",
            icon: "bubble.left.fill",
            isEnabled: true
        ),
        NotificationPreferenceItem(
            id: "status_changes",
            label: "Changements de statut",
            description: "Etre notifie lorsqu'un evenement change de statut",
            icon: "arrow.triangle.2.circlepath",
            isEnabled: true
        ),
        NotificationPreferenceItem(
            id: "reminders",
            label: "Rappels",
            description: "Rappels le jour de l'evenement",
            icon: "bell.fill",
            isEnabled: true
        ),
        NotificationPreferenceItem(
            id: "deadlines",
            label: "Deadlines",
            description: "Rappels avant la date limite des sondages",
            icon: "clock.fill",
            isEnabled: true
        ),
        NotificationPreferenceItem(
            id: "weekly_digest",
            label: "Resume hebdomadaire",
            description: "Recevoir un resume chaque lundi",
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
                    Text("Types de notifications")
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

                            Text("Son")
                                .font(.system(size: 16, weight: .medium))
                        }
                    }
                    .tint(.accentColor)
                } header: {
                    Text("General")
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

                            Text("Heures silencieuses")
                                .font(.system(size: 16, weight: .medium))
                        }
                    }
                    .tint(.accentColor)

                    if quietHoursEnabled {
                        DatePicker(
                            "Debut",
                            selection: $quietHoursStart,
                            displayedComponents: .hourAndMinute
                        )

                        DatePicker(
                            "Fin",
                            selection: $quietHoursEnd,
                            displayedComponents: .hourAndMinute
                        )
                    }
                } header: {
                    Text("Ne pas deranger")
                        .font(.system(size: 13, weight: .semibold))
                        .textCase(nil)
                } footer: {
                    if quietHoursEnabled {
                        Text("Les notifications seront mises en sourdine pendant cette periode.")
                            .font(.system(size: 12))
                    }
                }
            }
            .navigationTitle("Preferences")
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
                            Text("Enregistrer")
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
