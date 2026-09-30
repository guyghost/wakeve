import Foundation

/// Une ligne d'une sheet de module (couche 5a, #47), déjà localisée.
struct HubModuleSheetItem: Equatable, Identifiable {
    let id: String
    let title: String
    /// Ex. « sam. 3 oct. · 19:30 · 8 personnes ».
    let detail: String?
    /// Ex. repas prêt → `.confirmed`, à assigner → `.pending` ; nil = sans statut (annulé, activité).
    let status: WK.Status?
    /// Texte de la pastille propre au module (« Prêt », « À trouver », « Réservé »…).
    let statusText: String?
    /// Avatars : responsables, porteur ou inscrits.
    let assigneeNames: [String]
    /// Libellé VoiceOver de la carte : titre, détail, statut, personnes avec leur rôle.
    let accessibilityLabel: String
}

/// Entrées Swift simples lues par la source (aucun objet Kotlin) ; `statusName` = `enum.name` Kotlin.
enum HubModuleSheetRaw: Equatable {
    struct Meal: Equatable {
        let id: String
        let name: String
        /// `yyyy-MM-dd` (`Meal.date`).
        let date: String
        /// `HH:mm` (`Meal.time`).
        let time: String
        let servings: Int
        /// `MealStatus` : PLANNED, ASSIGNED, IN_PROGRESS, COMPLETED, CANCELLED.
        let statusName: String
        /// Noms des `responsibleParticipantIds` (identifiant si le nom est inconnu).
        let responsibleNames: [String]
    }

    struct EquipmentItem: Equatable {
        let id: String
        let name: String
        let quantity: Int
        /// `ItemStatus` : NEEDED, ASSIGNED, CONFIRMED, PACKED, CANCELLED.
        let statusName: String
        /// Nom de `assignedTo`, nil si personne.
        let assigneeName: String?
    }

    struct Activity: Equatable {
        let id: String
        let name: String
        let date: String?
        let time: String?
        let location: String?
        /// Noms des `registeredParticipantIds`.
        let registeredNames: [String]
    }

    struct Accommodation: Equatable {
        let id: String
        let name: String
        /// `pricePerNight`, en centimes (0 = inconnu).
        let pricePerNightCents: Int64
        let capacity: Int
        /// `BookingStatus` : SEARCHING, RESERVED, CONFIRMED, CANCELLED.
        let bookingStatusName: String
    }

    case meals([Meal])
    case equipment([EquipmentItem])
    case activities([Activity])
    case accommodation([Accommodation])
    case photos

    var module: HubModule {
        switch self {
        case .meals: return .meals
        case .equipment: return .equipment
        case .activities: return .activities
        case .accommodation: return .accommodation
        case .photos: return .photos
        }
    }
}

/// Présentation pure d'une sheet de module : lignes, pastille, phrase « ce qui manque », action d'ajout.
struct HubModuleSheetData: Equatable {
    struct Pill: Equatable {
        let text: String
        let status: WK.Status
    }

    let module: HubModule
    let items: [HubModuleSheetItem]
    /// Pastille d'en-tête (repas : « x/y prêts »).
    let status: Pill?
    /// Phrase « ce qui manque », localisée.
    let missing: String?
    /// Action principale « Ajouter » : organisateur, événement modifiable, module doté d'un formulaire (repas).
    let canAdd: Bool
    let pendingSync: Bool

    /// Entrées conservées pour recalculer après un ajout local (`appendingMeal`).
    let raw: HubModuleSheetRaw
    let isOrganizer: Bool
    let isReadOnly: Bool

    static func make(
        raw: HubModuleSheetRaw,
        isOrganizer: Bool,
        isReadOnly: Bool,
        pendingSync: Bool,
        locale: Locale = WK.appLocale,
        calendar: Calendar = .current
    ) -> HubModuleSheetData {
        let text = SheetText(locale: locale, calendar: calendar)
        let items: [HubModuleSheetItem]
        var pill: Pill?
        let missing: String?
        var canAdd = false

        switch raw {
        case .meals(let meals):
            items = meals.map { meal in
                let status = mealStatus(meal.statusName)
                return text.item(
                    module: .meals,
                    id: meal.id,
                    title: meal.name,
                    detail: text.join([
                        text.when(date: meal.date, time: meal.time),
                        meal.servings > 0 ? text.plural("hub.sheet.meals.people_count", meal.servings) : nil
                    ]),
                    status: status,
                    statusText: status.map { text.format($0 == .confirmed ? "hub.sheet.status.meal_ready" : "hub.sheet.status.meal_todo") },
                    names: meal.responsibleNames
                )
            }
            // Même décompte que la tuile du hub : prêts / repas non annulés.
            let progress = HubSummaryText.mealProgress(statusNames: meals.map(\.statusName))
            if progress.total > 0 {
                pill = Pill(
                    text: String(format: text.format("hub.sheet.meals.progress_format"), locale: locale, progress.ready, progress.total),
                    status: progress.ready == progress.total ? .confirmed : .pending
                )
            }
            let unassigned = meals.filter { $0.statusName != "CANCELLED" && $0.responsibleNames.isEmpty }.count
            missing = meals.isEmpty
                ? text.format("hub.sheet.meals.empty")
                : (unassigned > 0 ? text.plural("hub.sheet.meals.unassigned_count", unassigned) : nil)
            canAdd = isOrganizer && !isReadOnly

        case .equipment(let equipment):
            items = equipment.map { item in
                let status = equipmentStatus(item)
                return text.item(
                    module: .equipment,
                    id: item.id,
                    title: item.name,
                    detail: String(format: text.format("hub.sheet.equipment.quantity_format"), locale: locale, item.quantity),
                    status: status,
                    statusText: status.map {
                        text.format($0 == .confirmed ? "hub.sheet.status.equipment_covered" : "hub.sheet.status.equipment_needed")
                    },
                    names: item.assigneeName.map { [$0] } ?? []
                )
            }
            // `equipmentStatus` ne renvoie jamais `.pending` pour un objet annulé.
            let unassigned = equipment.filter { equipmentStatus($0) == .pending }.count
            missing = equipment.isEmpty
                ? text.format("hub.sheet.equipment.empty")
                : (unassigned > 0 ? text.plural("hub.sheet.equipment.unassigned_count", unassigned) : nil)

        case .activities(let activities):
            items = activities.map { activity in
                text.item(
                    module: .activities,
                    id: activity.id,
                    title: activity.name,
                    detail: text.join([
                        activity.date.flatMap { text.when(date: $0, time: activity.time ?? "") },
                        activity.location.flatMap { $0.isEmpty ? nil : $0 },
                        activity.registeredNames.isEmpty
                            ? nil
                            : text.plural("hub.sheet.activities.registered_count", activity.registeredNames.count)
                    ]),
                    status: nil,
                    statusText: nil,
                    names: activity.registeredNames
                )
            }
            missing = activities.isEmpty ? text.format("hub.sheet.activities.empty") : nil

        case .accommodation(let options):
            items = options.map { option in
                let status = accommodationStatus(option.bookingStatusName)
                return text.item(
                    module: .accommodation,
                    id: option.id,
                    title: option.name,
                    detail: text.join([
                        option.pricePerNightCents > 0
                            ? String(
                                format: text.format("hub.sheet.accommodation.price_per_night_format"),
                                HubSummaryText.currency(Double(option.pricePerNightCents) / 100, code: "EUR", locale: locale)
                            )
                            : nil,
                        option.capacity > 0
                            ? String(format: text.format("accommodation.capacity_format"), locale: locale, option.capacity)
                            : nil
                    ]),
                    status: status,
                    statusText: status.map {
                        text.format($0 == .confirmed
                                    ? "hub.sheet.status.accommodation_confirmed"
                                    : "hub.sheet.status.accommodation_reserved")
                    },
                    names: []
                )
            }
            let retained = options.contains { accommodationStatus($0.bookingStatusName) != nil }
            if options.isEmpty {
                missing = text.format("hub.sheet.accommodation.empty")
            } else {
                missing = retained ? nil : text.format("hub.sheet.accommodation.none_selected")
            }

        case .photos:
            items = []
            missing = text.format("hub.tile.photos_hint")
        }

        return HubModuleSheetData(
            module: raw.module,
            items: items,
            status: pill,
            missing: missing,
            canAdd: canAdd,
            pendingSync: pendingSync,
            raw: raw,
            isOrganizer: isOrganizer,
            isReadOnly: isReadOnly
        )
    }

    /// Repas ajouté par le formulaire existant (`MealFormSheet`, qui n'écrit pas en base) : même comportement
    /// que l'écran legacy, le repas s'ajoute à la liste affichée.
    func appendingMeal(
        _ meal: HubModuleSheetRaw.Meal,
        locale: Locale = WK.appLocale,
        calendar: Calendar = .current
    ) -> HubModuleSheetData {
        guard case .meals(let meals) = raw else { return self }
        return Self.make(
            raw: .meals(meals + [meal]), isOrganizer: isOrganizer, isReadOnly: isReadOnly,
            pendingSync: pendingSync, locale: locale, calendar: calendar
        )
    }

    // MARK: - Statuts

    static func mealStatus(_ name: String) -> WK.Status? {
        switch name {
        case "COMPLETED": return .confirmed
        case "CANCELLED": return nil
        default: return .pending
        }
    }

    /// Apporté, confirmé ou attribué à quelqu'un → couvert ; à trouver → en attente.
    static func equipmentStatus(_ item: HubModuleSheetRaw.EquipmentItem) -> WK.Status? {
        switch item.statusName {
        case "CANCELLED": return nil
        case "ASSIGNED", "CONFIRMED", "PACKED": return .confirmed
        default: return item.assigneeName == nil ? .pending : .confirmed
        }
    }

    /// Option retenue : confirmée, ou réservée (en attente de confirmation).
    static func accommodationStatus(_ name: String) -> WK.Status? {
        switch name {
        case "CONFIRMED": return .confirmed
        case "RESERVED": return .pending
        default: return nil
        }
    }

    // MARK: - Accessibilité

    /// Personnes d'une carte avec leur rôle (« Responsables : Léa et Tom ») ; nil sans personne ou sans rôle.
    static func peopleLabel(for module: HubModule, names: [String], locale: Locale = WK.appLocale) -> String? {
        guard !names.isEmpty else { return nil }
        let key: String
        switch module {
        case .meals: key = "hub.sheet.a11y.meal_responsibles_format"
        case .activities: key = "hub.sheet.a11y.activity_registered_format"
        case .equipment: key = "hub.sheet.a11y.equipment_brought_by_format"
        default: return nil
        }
        let formatter = ListFormatter()
        formatter.locale = locale
        let list = formatter.string(from: names) ?? names.joined(separator: ", ")
        return String(format: WK.localizedFormat(key, locale: locale), locale: locale, list)
    }

    // MARK: - Texte

    /// Formats localisés pour une locale donnée (testable indépendamment de la langue de l'app).
    private struct SheetText {
        let locale: Locale
        let calendar: Calendar
        /// Un seul analyseur de date par construction de sheet.
        private let dayFormatter: DateFormatter

        init(locale: Locale, calendar: Calendar) {
            self.locale = locale
            self.calendar = calendar
            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: "en_US_POSIX")
            formatter.calendar = calendar
            formatter.timeZone = calendar.timeZone
            formatter.dateFormat = "yyyy-MM-dd"
            dayFormatter = formatter
        }

        func item(
            module: HubModule, id: String, title: String, detail: String?,
            status: WK.Status?, statusText: String?, names: [String]
        ) -> HubModuleSheetItem {
            let label = [title, detail, statusText, HubModuleSheetData.peopleLabel(for: module, names: names, locale: locale)]
                .compactMap { $0 }
                .filter { !$0.isEmpty }
                .joined(separator: ", ")
            return HubModuleSheetItem(
                id: id, title: title, detail: detail, status: status, statusText: statusText,
                assigneeNames: names, accessibilityLabel: label
            )
        }

        func format(_ key: String) -> String { WK.localizedFormat(key, locale: locale) }

        func plural(_ key: String, _ count: Int) -> String { HubSummaryText.plural(key, count, locale: locale) }

        /// « a · b · c » (format existant `hub.summary.format`) ; nil si aucune partie.
        func join(_ parts: [String?]) -> String? {
            let present = parts.compactMap { $0 }.filter { !$0.isEmpty }
            guard var result = present.first else { return nil }
            for part in present.dropFirst() {
                result = String(format: format("hub.summary.format"), result, part)
            }
            return result
        }

        /// « sam. 3 oct. · 19:30 » ; date illisible → valeur brute.
        func when(date: String, time: String) -> String? {
            let day = dayFormatter.date(from: date).map { HomeDateText.short($0, locale: locale, calendar: calendar) }
                ?? (date.isEmpty ? nil : date)
            return join([day, time.isEmpty ? nil : time])
        }
    }
}
