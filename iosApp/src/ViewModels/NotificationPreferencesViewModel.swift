import Foundation
import Shared

// MARK: - Notification Type Toggle Model

struct NotificationTypeToggle: Identifiable {
    let id: String
    let rawName: String
    let displayName: String
    let iconName: String
}

// MARK: - Notification Preferences ViewModel

@MainActor
class NotificationPreferencesViewModel: ObservableObject {

    @Published var enabledTypeNames: Set<String> = []
    @Published var quietHoursEnabled = false
    @Published var quietHoursStart = Date()
    @Published var quietHoursEnd = Date()
    @Published var soundEnabled = true
    @Published var vibrationEnabled = true
    @Published var isLoading = false

    let typeToggles: [NotificationTypeToggle] = [
        NotificationTypeToggle(id: "EVENT_INVITE", rawName: "EVENT_INVITE", displayName: "Invitation événement", iconName: "envelope.fill"),
        NotificationTypeToggle(id: "VOTE_REMINDER", rawName: "VOTE_REMINDER", displayName: "Rappel de vote", iconName: "chart.bar.fill"),
        NotificationTypeToggle(id: "DATE_CONFIRMED", rawName: "DATE_CONFIRMED", displayName: "Date confirmée", iconName: "calendar.badge.checkmark"),
        NotificationTypeToggle(id: "NEW_SCENARIO", rawName: "NEW_SCENARIO", displayName: "Nouveau scénario", iconName: "doc.badge.plus"),
        NotificationTypeToggle(id: "SCENARIO_SELECTED", rawName: "SCENARIO_SELECTED", displayName: "Scénario sélectionné", iconName: "star.fill"),
        NotificationTypeToggle(id: "NEW_COMMENT", rawName: "NEW_COMMENT", displayName: "Nouveau commentaire", iconName: "bubble.left.fill"),
        NotificationTypeToggle(id: "MENTION", rawName: "MENTION", displayName: "Mention", iconName: "at"),
        NotificationTypeToggle(id: "MEETING_REMINDER", rawName: "MEETING_REMINDER", displayName: "Rappel réunion", iconName: "bell.badge.fill"),
        NotificationTypeToggle(id: "PAYMENT_DUE", rawName: "PAYMENT_DUE", displayName: "Paiement dû", iconName: "creditcard.fill"),
        NotificationTypeToggle(id: "EVENT_UPDATE", rawName: "EVENT_UPDATE", displayName: "Mise à jour événement", iconName: "arrow.triangle.2.circlepath"),
        NotificationTypeToggle(id: "VOTE_CLOSE_REMINDER", rawName: "VOTE_CLOSE_REMINDER", displayName: "Fin de vote imminente", iconName: "clock.badge"),
        NotificationTypeToggle(id: "DEADLINE_REMINDER", rawName: "DEADLINE_REMINDER", displayName: "Rappel deadline", iconName: "exclamationmark.circle"),
        NotificationTypeToggle(id: "COMMENT_REPLY", rawName: "COMMENT_REPLY", displayName: "Réponse à un commentaire", iconName: "bubble.left.and.bubble.right.fill")
    ]

    private let userId: String
    private let database: WakeveDb

    init(userId: String) {
        self.userId = userId
        self.database = RepositoryProvider.shared.database

        // Set default quiet hours (22:00 - 08:00)
        var components = DateComponents()
        components.hour = 22
        components.minute = 0
        quietHoursStart = Calendar.current.date(from: components) ?? Date()

        components.hour = 8
        components.minute = 0
        quietHoursEnd = Calendar.current.date(from: components) ?? Date()
    }

    // MARK: - Load

    func load() {
        isLoading = true
        let record = database.userQueries.selectPreferencesByUserId(user_id: userId).executeAsList().first
        if let record = record {
            // Parse enabled types from JSON
            if let typesJson = record.enabled_types,
               let data = typesJson.data(using: .utf8),
               let typeNames = try? JSONDecoder().decode([String].self, from: data) {
                enabledTypeNames = Set(typeNames)
            }

            // Parse quiet hours
            if let startStr = record.quiet_hours_start, let endStr = record.quiet_hours_end {
                quietHoursEnabled = true
                quietHoursStart = quietTimeStringToDate(startStr)
                quietHoursEnd = quietTimeStringToDate(endStr)
            }

            soundEnabled = record.sound_enabled?.int64Value != 0
            vibrationEnabled = record.vibration_enabled?.int64Value != 0
        }
        isLoading = false
    }

    // MARK: - Toggle Helpers

    func isEnabled(_ rawName: String) -> Bool {
        enabledTypeNames.contains(rawName)
    }

    func toggleType(_ rawName: String, enabled: Bool) {
        if enabled {
            enabledTypeNames.insert(rawName)
        } else {
            enabledTypeNames.remove(rawName)
        }
        save()
    }

    // MARK: - Save

    func save() {
        let typesArray = Array(enabledTypeNames)
        let typesJson = (try? JSONEncoder().encode(typesArray)).flatMap { String(data: $0, encoding: .utf8) } ?? "[]"
        let startStr = quietHoursEnabled ? dateToQuietTimeString(quietHoursStart) : nil
        let endStr = quietHoursEnabled ? dateToQuietTimeString(quietHoursEnd) : nil
        let nowMs = Int64(Date().timeIntervalSince1970 * 1000)

        let existing = database.userQueries.selectPreferencesByUserId(user_id: userId).executeAsList().first
        if existing == nil {
            database.userQueries.insertPreferences(
                user_id: userId,
                enabled_types: typesJson,
                quiet_hours_start: startStr,
                quiet_hours_end: endStr,
                sound_enabled: soundEnabled ? 1 : 0,
                vibration_enabled: vibrationEnabled ? 1 : 0,
                updated_at: nowMs
            )
        } else {
            database.userQueries.updatePreferences(
                enabled_types: typesJson,
                quiet_hours_start: startStr,
                quiet_hours_end: endStr,
                sound_enabled: soundEnabled ? 1 : 0,
                vibration_enabled: vibrationEnabled ? 1 : 0,
                updated_at: nowMs,
                user_id: userId
            )
        }
    }

    // MARK: - Conversion Helpers

    private func quietTimeStringToDate(_ timeStr: String) -> Date {
        let parts = timeStr.split(separator: ":")
        var components = DateComponents()
        components.hour = parts.count > 0 ? Int(parts[0]) ?? 0 : 0
        components.minute = parts.count > 1 ? Int(parts[1]) ?? 0 : 0
        return Calendar.current.date(from: components) ?? Date()
    }

    private func dateToQuietTimeString(_ date: Date) -> String {
        let components = Calendar.current.dateComponents([.hour, .minute], from: date)
        return String(format: "%02d:%02d", components.hour ?? 0, components.minute ?? 0)
    }
}
