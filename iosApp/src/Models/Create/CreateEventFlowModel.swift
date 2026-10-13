import Foundation
import Shared

// MARK: - Étapes

/// Les quatre questions du flux de création de la refonte (couche 7, #47) : Quoi ? · Qui ? · Où ? · Quand ?
/// (`place` / `time` : `where` et `when` ne sont pas des identifiants Swift utilisables ici).
enum CreateEventFlowStep: Int, CaseIterable, Identifiable {
    case what, who, place, time

    enum Segment: Equatable { case done, current, upcoming }

    var id: Int { rawValue }

    var next: CreateEventFlowStep? { Self(rawValue: rawValue + 1) }
    var previous: CreateEventFlowStep? { Self(rawValue: rawValue - 1) }
    var isLast: Bool { next == nil }
    /// « Où ? » est optionnel (bouton « Passer »).
    var isSkippable: Bool { self == .place }

    /// Nom stable (identifiants d'accessibilité et clés de texte).
    var key: String {
        switch self {
        case .what: return "what"
        case .who: return "who"
        case .place: return "place"
        case .time: return "time"
        }
    }

    /// Progression en segments : étapes passées, courante, à venir.
    static func segments(current: CreateEventFlowStep) -> [Segment] {
        allCases.map { step in
            if step.rawValue < current.rawValue { return .done }
            return step == current ? .current : .upcoming
        }
    }
}

/// Champ porteur d'une erreur, pour l'afficher sous le champ concerné.
enum CreateEventField: Hashable {
    case title, description, customType
    case minParticipants, expectedParticipants, maxParticipants
    case location(Int)
    case slots
    case slot(String)
}

// MARK: - Créneaux

/// Moment proposé pour un créneau (`TimeOfDay` partagé).
enum CreateEventMoment: String, CaseIterable, Identifiable {
    case allDay, morning, afternoon, evening, specific

    var id: String { rawValue }

    var timeOfDay: Shared.TimeOfDay {
        switch self {
        case .allDay: return .allDay
        case .morning: return .morning
        case .afternoon: return .afternoon
        case .evening: return .evening
        case .specific: return .specific
        }
    }

    init(timeOfDay: Shared.TimeOfDay) {
        switch timeOfDay {
        case .allDay: self = .allDay
        case .morning: self = .morning
        case .afternoon: self = .afternoon
        case .evening: self = .evening
        default: self = .specific
        }
    }

    /// Plage indicative (heures locales) d'un moment flou ; mêmes repères que le partagé (9 h, 14 h, 19 h).
    var hours: (start: Int, end: Int)? {
        switch self {
        case .morning: return (9, 12)
        case .afternoon: return (14, 18)
        case .evening: return (19, 23)
        case .allDay, .specific: return nil
        }
    }

    var titleKey: String {
        switch self {
        case .allDay: return "create_flow.moment.all_day"
        case .morning: return "create_flow.moment.morning"
        case .afternoon: return "create_flow.moment.afternoon"
        case .evening: return "create_flow.moment.evening"
        case .specific: return "create_flow.moment.specific"
        }
    }
}

/// Créneau saisi, avec un identifiant logique stable d'un enregistrement à l'autre.
struct CreateEventSlot: Identifiable, Equatable {
    let id: String
    /// Début vide : créneau repris sans date (moment flou, ou heure précise à choisir).
    var input: EventTimeSlotInput
    /// Fuseau d'origine d'un créneau repris ; nil pour un créneau ajouté (fuseau de l'appareil).
    var timezone: String? = nil

    var moment: CreateEventMoment { CreateEventMoment(timeOfDay: input.timeOfDay) }

    /// Repris sans date : le début est vide (`TimeSlot.start` nil).
    var isDateless: Bool { input.start.isEmpty }

    /// Début strictement avant la fin (exigé pour une heure précise).
    var hasValidRange: Bool {
        let formatter = ISO8601DateFormatter()
        guard let start = formatter.date(from: input.start),
              let endValue = input.end,
              let end = formatter.date(from: endValue) else { return false }
        return start < end
    }
}

enum CreateEventSlotBuilder {
    static func newID() -> String {
        "slot-\(UUID().uuidString.prefix(8).lowercased())"
    }

    /// Jour entier : comme `EventSlotInputBuilder` (début du jour → jour suivant). Moment flou : plage
    /// indicative du jour. Heure précise : début et fin tels que saisis (aucun report au lendemain,
    /// la validation exige début < fin).
    static func slot(
        id: String = newID(),
        day: Date,
        moment: CreateEventMoment,
        start: Date,
        end: Date,
        formatter: ISO8601DateFormatter = ISO8601DateFormatter(),
        calendar: Calendar = .current
    ) -> CreateEventSlot {
        switch moment {
        case .allDay:
            let base = EventSlotInputBuilder.input(
                startDate: day, startTime: day, isAllDay: true, hasEndTime: false, endTime: day,
                formatter: formatter, calendar: calendar
            )
            return CreateEventSlot(id: id, input: base)
        case .specific:
            return CreateEventSlot(id: id, input: EventTimeSlotInput(
                start: formatter.string(from: start),
                end: formatter.string(from: end),
                timeOfDay: .specific
            ))
        case .morning, .afternoon, .evening:
            let hours = moment.hours ?? (9, 12)
            let startDate = calendar.date(bySettingHour: hours.start, minute: 0, second: 0, of: day) ?? day
            let endDate = calendar.date(bySettingHour: hours.end, minute: 0, second: 0, of: day) ?? day
            return CreateEventSlot(id: id, input: EventTimeSlotInput(
                start: formatter.string(from: startDate),
                end: formatter.string(from: endDate),
                timeOfDay: moment.timeOfDay
            ))
        }
    }
}

// MARK: - Formulaire

/// Saisie du flux. Les messages d'erreur sont des clés de localisation.
struct CreateEventForm: Equatable {
    static let otherTypeName = "OTHER"
    static let customTypeName = "CUSTOM"
    /// Types proposés en pastilles (« Autre » = type personnalisé avec libellé, en plus).
    static let commonEventTypeNames = [
        "PARTY", "BIRTHDAY", "FOOD_TASTING", "OUTDOOR_ACTIVITY", "SPORTS_EVENT",
        "FAMILY_GATHERING", "CULTURAL_EVENT", "WELLNESS_EVENT", "TEAM_BUILDING"
    ]

    var title = ""
    var description = ""
    var eventTypeName = CreateEventForm.otherTypeName
    var eventTypeCustom = ""
    var minParticipants: Int?
    var expectedParticipants: Int?
    var maxParticipants: Int?
    var locations: [String] = []
    var slots: [CreateEventSlot] = []
    var scenarioId: String?
    /// Checklist du modèle choisi : gardée sur l'appareil avec le brouillon (`EventChecklistStoring`), puis montrée dans le hub.
    var checklist: [String] = []

    var isCustomType: Bool { eventTypeName == Self.customTypeName }

    var eventType: Shared.EventType { Self.eventType(named: eventTypeName) }

    /// Libellé à enregistrer : seulement pour un type personnalisé.
    var persistedEventTypeCustom: String? {
        guard isCustomType else { return nil }
        let label = Self.trimmed(eventTypeCustom)
        return label.isEmpty ? nil : label
    }

    /// Rien de saisi : fermer ne crée aucun brouillon.
    var isBlank: Bool {
        Self.trimmed(title).isEmpty && Self.trimmed(description).isEmpty
            && eventTypeName == Self.otherTypeName && Self.trimmed(eventTypeCustom).isEmpty
            && minParticipants == nil && expectedParticipants == nil && maxParticipants == nil
            && locations.isEmpty && slots.isEmpty
    }

    /// Règles DRAFT d'AGENTS.md, étape par étape.
    func errors(for step: CreateEventFlowStep) -> [CreateEventField: String] {
        var errors: [CreateEventField: String] = [:]
        switch step {
        case .what:
            if Self.trimmed(title).isEmpty { errors[.title] = "create_event.validation.title_required" }
            if Self.trimmed(description).isEmpty { errors[.description] = "create_flow.error.description_required" }
            if isCustomType, Self.trimmed(eventTypeCustom).isEmpty {
                errors[.customType] = "create_flow.error.custom_type_required"
            }
        case .who:
            let counts: [(CreateEventField, Int?)] = [
                (.minParticipants, minParticipants),
                (.expectedParticipants, expectedParticipants),
                (.maxParticipants, maxParticipants)
            ]
            for (field, value) in counts {
                if let value, value < 1 { errors[field] = "create_flow.error.participants_positive" }
            }
            if let min = minParticipants, let max = maxParticipants, max < min, errors[.maxParticipants] == nil {
                errors[.maxParticipants] = "create_flow.error.max_less_than_min"
            }
        case .place:
            var seen = Set<String>()
            for (index, name) in locations.enumerated() {
                let key = Self.locationKey(name)
                if key.isEmpty {
                    errors[.location(index)] = "create_flow.error.location_empty"
                } else if !seen.insert(key).inserted {
                    errors[.location(index)] = "create_flow.error.location_duplicate"
                }
            }
        case .time:
            if slots.isEmpty { errors[.slots] = "create_event.validation.slot_required" }
            for slot in slots where slot.moment == .specific {
                if slot.isDateless {
                    errors[.slot(slot.id)] = "create_flow.error.slot_time_required"
                } else if !slot.hasValidRange {
                    errors[.slot(slot.id)] = "create_flow.error.slot_end_before_start"
                }
            }
        }
        return errors
    }

    func isValid(_ step: CreateEventFlowStep) -> Bool { errors(for: step).isEmpty }

    /// Fermer enregistre l'étape courante si elle est valide : brouillon existant, ou étape 1 valide pas
    /// encore continuée (elle crée le brouillon). Rien de saisi : rien n'est créé.
    func savesOnClose(step: CreateEventFlowStep, hasDraft: Bool) -> Bool {
        guard !isBlank, isValid(step) else { return false }
        return hasDraft || isValid(.what)
    }

    /// Étape où reprendre un brouillon : la première dont la validation échoue (nil : tout est valide).
    var firstInvalidStep: CreateEventFlowStep? {
        CreateEventFlowStep.allCases.first { !isValid($0) }
    }

    /// Erreur du champ d'ajout d'un lieu (nil : ajout possible).
    func locationAdditionError(_ name: String) -> String? {
        let key = Self.locationKey(name)
        if key.isEmpty { return "create_flow.error.location_empty" }
        if locations.contains(where: { Self.locationKey($0) == key }) { return "create_flow.error.location_duplicate" }
        return nil
    }

    // MARK: Modèles (ex-Explorer)

    static func scenarioID(_ scenario: EventScenario) -> String { scenario.title }

    static func scenario(withID id: String) -> EventScenario? {
        EventScenario.allScenarios.first { scenarioID($0) == id }
    }

    /// Préremplit Quoi ? : un texte saisi n'est jamais écrasé, seul le préremplissage du modèle précédent l'est.
    mutating func apply(scenario: EventScenario) {
        let previous = scenarioId.flatMap(Self.scenario(withID:))
        if Self.trimmed(title).isEmpty || title == previous?.suggestedTitle {
            title = scenario.suggestedTitle
        }
        if Self.trimmed(description).isEmpty || description == previous?.description {
            description = scenario.description
        }
        eventTypeName = scenario.eventType
        eventTypeCustom = ""
        scenarioId = Self.scenarioID(scenario)
        checklist = scenario.checklistItems
    }

    mutating func clearScenario() {
        scenarioId = nil
        checklist = []
    }

    // MARK: Outils

    static func eventType(named name: String) -> Shared.EventType {
        Shared.EventType.entries.first { $0.name == name } ?? .other
    }

    static func trimmed(_ value: String) -> String {
        value.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    static func locationKey(_ name: String) -> String {
        trimmed(name).folding(options: [.caseInsensitive], locale: nil)
    }
}
