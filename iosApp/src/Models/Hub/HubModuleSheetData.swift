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

    /// Budget lu sans création (`BudgetRepository.getBudgetByEventId`), en euros comme `BudgetOverviewView`.
    struct Budget: Equatable {
        struct Category: Equatable {
            /// Suffixe de `budget.category.*` : transport, accommodation, meals, activities, equipment, other.
            let key: String
            let estimated: Double
            let actual: Double
        }

        let totalEstimated: Double
        let totalActual: Double
        /// Dans l'ordre d'affichage.
        let categories: [Category]
    }

    /// Dernière cagnotte de l'événement, ouverte ou clôturée (`potQueries.selectByEvent`).
    struct PaymentPot: Equatable {
        let title: String
        let goalAmount: Double
        /// Code ISO (`EUR`, `CHF`…).
        let currency: String
        /// ACTIVE, CLOSED.
        let statusName: String
    }

    /// État Tricount (`TricountHandoffRepository.getPaymentReadiness`), voir `HubModuleSheetData.tricountState`.
    enum Tricount: Equatable {
        case notRequired, linkVerified, linkToCheck, undecided
    }

    struct Meeting: Equatable {
        let id: String
        let title: String
        /// ISO 8601 UTC (`meeting.startTime`).
        let startTime: String
        /// ZOOM, GOOGLE_MEET, FACETIME, TEAMS, WEBEX, OTHER.
        let platformName: String
        /// SCHEDULED, STARTED, ENDED, CANCELLED.
        let statusName: String
        /// Lien de réunion généré (`meetingLink` non vide).
        let hasLink: Bool
    }

    case meals([Meal])
    case equipment([EquipmentItem])
    case activities([Activity])
    case accommodation([Accommodation])
    case photos
    /// nil : aucun budget enregistré.
    case budget(Budget?)
    case payments(pot: PaymentPot?, tricount: Tricount)
    case meetings([Meeting])

    var module: HubModule {
        switch self {
        case .meals: return .meals
        case .equipment: return .equipment
        case .activities: return .activities
        case .accommodation: return .accommodation
        case .photos: return .photos
        case .budget: return .budget
        case .payments: return .payments
        case .meetings: return .meetings
        }
    }
}

/// Présentation pure d'une sheet de module : lignes, pastille, phrase « ce qui manque », action d'ajout.
struct HubModuleSheetData: Equatable {
    struct Pill: Equatable {
        let text: String
        let status: WK.Status
    }

    /// Action principale de la sheet ; les écritures restent dans les écrans existants.
    enum Primary: Equatable {
        /// Formulaire `MealFormSheet`, présenté depuis la sheet.
        case addMeal
        /// Consultation de l'écran budget, ouverte à tous (même finalisé).
        case viewExpenses
        /// Écran cagnotte.
        case managePot
        /// Écran réunions (création par « + »).
        case planMeeting
    }

    let module: HubModule
    let items: [HubModuleSheetItem]
    /// Pastille d'en-tête (repas : « x/y prêts »).
    let status: Pill?
    /// Phrase « ce qui manque », localisée.
    let missing: String?
    /// Action principale (`primaryAction(for:isOrganizer:isReadOnly:)`).
    let primary: Primary?
    let pendingSync: Bool

    /// Entrées conservées pour recalculer après un ajout local (`appendingMeal`).
    let raw: HubModuleSheetRaw
    let isOrganizer: Bool
    let isReadOnly: Bool

    /// Ajout de repas depuis la sheet (organisateur, événement modifiable).
    var canAdd: Bool { primary == .addMeal }

    /// Action principale par module, mêmes règles que les écrans legacy :
    /// repas, cagnotte (`canManagePayment`) et réunions (`canCreateMeetings`) pour l'organisateur d'un événement
    /// non finalisé — les gardes des sheets 5b limitent déjà la phase à organisation/finalisé ;
    /// « Voir les dépenses » est une consultation, ouverte à tous.
    static func primaryAction(for module: HubModule, isOrganizer: Bool, isReadOnly: Bool) -> Primary? {
        let canWrite = isOrganizer && !isReadOnly
        switch module {
        case .meals: return canWrite ? .addMeal : nil
        case .budget: return .viewExpenses
        case .payments: return canWrite ? .managePot : nil
        case .meetings: return canWrite ? .planMeeting : nil
        default: return nil
        }
    }

    static func make(
        raw: HubModuleSheetRaw,
        isOrganizer: Bool,
        isReadOnly: Bool,
        pendingSync: Bool,
        locale: Locale = WK.appLocale,
        calendar: Calendar = .current,
        now: Date = Date()
    ) -> HubModuleSheetData {
        let text = SheetText(locale: locale, calendar: calendar)
        let items: [HubModuleSheetItem]
        var pill: Pill?
        var missing: String?

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

        case .budget(let budget):
            let money = { HubSummaryText.euros($0, locale: locale) }
            let amounts = { (actual: Double, estimated: Double) in
                String(format: text.format("hub.sheet.budget.amounts_format"), money(actual), money(estimated))
            }
            guard let budget, budget.totalEstimated > 0 || budget.totalActual > 0 else {
                items = []
                missing = text.format("hub.sheet.budget.empty")
                break
            }
            items = budget.categories
                .filter { $0.estimated > 0 || $0.actual > 0 }
                .map { category in
                    let over = isOverspent(actual: category.actual, estimated: category.estimated)
                    return text.item(
                        module: .budget,
                        id: category.key,
                        title: text.format("budget.category.\(category.key)"),
                        detail: amounts(category.actual, category.estimated),
                        status: over ? .pending : nil,
                        statusText: over ? text.format("hub.sheet.status.budget_over") : nil,
                        names: []
                    )
                }
            let over = isOverspent(actual: budget.totalActual, estimated: budget.totalEstimated)
            pill = Pill(text: amounts(budget.totalActual, budget.totalEstimated), status: over ? .pending : .confirmed)
            let overCents = cents(budget.totalActual) - cents(budget.totalEstimated)
            missing = over
                ? String(format: text.format("hub.sheet.budget.over_format"), money(Double(overCents) / 100))
                : nil

        case .payments(let pot, let tricount):
            var cards: [HubModuleSheetItem] = []
            if let pot {
                let status = potStatus(pot.statusName)
                pill = status.map { Pill(text: text.format($0.key), status: $0.status) }
                let title = pot.title.trimmingCharacters(in: .whitespacesAndNewlines)
                cards.append(text.item(
                    module: .payments,
                    id: "pot",
                    title: title.isEmpty ? text.format("event.detail.organization.payment_pot_label") : title,
                    // Même texte que la tuile du hub.
                    detail: HubSummaryText.paymentPot(goalAmount: pot.goalAmount, currency: pot.currency, locale: locale),
                    status: status?.status,
                    statusText: status.map { text.format($0.key) },
                    names: []
                ))
            } else {
                missing = text.format("hub.sheet.payments.no_pot")
            }
            let ready = tricount == .notRequired || tricount == .linkVerified
            cards.append(text.item(
                module: .payments,
                id: "tricount",
                title: text.format("tricount.title"),
                detail: text.format(tricountKey(tricount)),
                status: ready ? .confirmed : .pending,
                statusText: text.format(ready ? "hub.sheet.status.tricount_ready" : "hub.sheet.status.tricount_todo"),
                names: []
            ))
            items = cards

        case .meetings(let meetings):
            // À venir d'abord, terminées ensuite ; dans chaque groupe par date, dates illisibles en dernier,
            // ordre d'origine conservé.
            let dated = meetings
                .filter { $0.statusName != "CANCELLED" }
                .enumerated()
                .map { entry in
                    (
                        offset: entry.offset,
                        meeting: entry.element,
                        date: HomeDateText.parseISO(entry.element.startTime),
                        upcoming: HubSummaryText.isUpcomingMeeting(
                            statusName: entry.element.statusName, startTime: entry.element.startTime, now: now
                        )
                    )
                }
                .sorted { lhs, rhs in
                    if lhs.upcoming != rhs.upcoming { return lhs.upcoming }
                    switch (lhs.date, rhs.date) {
                    case let (l?, r?): return l == r ? lhs.offset < rhs.offset : l < r
                    case (.some, nil): return true
                    case (nil, .some): return false
                    case (nil, nil): return lhs.offset < rhs.offset
                    }
                }
            items = dated.map { entry in
                let meeting = entry.meeting
                let platform = meetingPlatformName(meeting.platformName, locale: locale)
                let title = meeting.title.trimmingCharacters(in: .whitespacesAndNewlines)
                let status: WK.Status? = entry.upcoming ? (meeting.hasLink ? .confirmed : .pending) : nil
                let statusKey = !entry.upcoming
                    ? "meetings.ended"
                    : (meeting.hasLink ? "hub.sheet.status.meeting_link_ready" : "hub.sheet.status.meeting_no_link")
                return text.item(
                    module: .meetings,
                    id: meeting.id,
                    title: title.isEmpty ? platform : meeting.title,
                    detail: text.join([
                        entry.date.map { text.when(instant: $0) } ?? (meeting.startTime.isEmpty ? nil : meeting.startTime),
                        platform
                    ]),
                    status: status,
                    statusText: text.format(statusKey),
                    names: []
                )
            }
            // Même décompte que la tuile du hub (réunions non annulées) ; « à venir » = ni annulée ni terminée,
            // une réunion programmée dont l'heure est passée comptant comme terminée.
            let counts = HubSummaryText.meetingCounts(statusNames: meetings.map(\.statusName))
            let upcoming = dated.filter(\.upcoming).map(\.meeting)
            let withoutLink = upcoming.filter { !$0.hasLink }.count
            if !upcoming.isEmpty {
                pill = Pill(
                    text: text.plural("hub.sheet.meetings.upcoming_count", upcoming.count),
                    status: withoutLink == 0 ? .confirmed : .pending
                )
            }
            if counts.active == 0 {
                missing = text.format("hub.sheet.meetings.empty")
            } else if withoutLink > 0 {
                missing = text.plural("hub.sheet.meetings.without_link_count", withoutLink)
            }
        }

        return HubModuleSheetData(
            module: raw.module,
            items: items,
            status: pill,
            missing: missing,
            primary: primaryAction(for: raw.module, isOrganizer: isOrganizer, isReadOnly: isReadOnly),
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

    /// Montant en centimes : les comparaisons ignorent les résidus des `Double` (0,1 + 0,2).
    static func cents(_ amount: Double) -> Int64 {
        Int64((amount * 100).rounded())
    }

    /// Dépassement au centime près ; une dépense sans estimation (estimé 0) est un dépassement.
    static func isOverspent(actual: Double, estimated: Double) -> Bool {
        cents(actual) > cents(estimated)
    }

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

    /// Cagnotte ouverte ou clôturée ; statut inconnu → pas de pastille.
    static func potStatus(_ name: String) -> (key: String, status: WK.Status)? {
        switch name {
        case "ACTIVE": return ("hub.sheet.status.pot_open", .confirmed)
        case "CLOSED": return ("hub.sheet.status.pot_closed", .confirmed)
        default: return nil
        }
    }

    /// Même ordre que `tricountSummaryValue` (ContentView) : non requis, lien vérifié, lien à vérifier, à décider.
    static func tricountState(explicitNotNeeded: Bool?, complete: Bool, hasHandoff: Bool) -> HubModuleSheetRaw.Tricount {
        if explicitNotNeeded == true { return .notRequired }
        if complete { return .linkVerified }
        if hasHandoff { return .linkToCheck }
        return .undecided
    }

    /// Textes existants du résumé Tricount (sans le suffixe « détails finalisés » : la sheet a sa pastille).
    static func tricountKey(_ state: HubModuleSheetRaw.Tricount) -> String {
        switch state {
        case .notRequired: return "event.detail.tricount.not_required"
        case .linkVerified: return "event.detail.tricount.link_verified"
        case .linkToCheck: return "event.detail.tricount.link_to_check"
        case .undecided: return "event.detail.tricount.decide_before_expenses"
        }
    }

    /// Mêmes noms que la ligne legacy (`MeetingRowView.platformDisplayName`).
    static func meetingPlatformName(_ name: String, locale: Locale = WK.appLocale) -> String {
        switch name {
        case "ZOOM": return "Zoom"
        case "GOOGLE_MEET": return "Google Meet"
        case "FACETIME": return "FaceTime"
        case "TEAMS": return "Teams"
        case "WEBEX": return "Webex"
        default: return WK.localizedFormat("meetings.platform_other", locale: locale)
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
        /// Heure locale courte (« 18:00 », « 6:00 PM »).
        private let timeFormatter: DateFormatter

        init(locale: Locale, calendar: Calendar) {
            self.locale = locale
            self.calendar = calendar
            let formatter = DateFormatter()
            formatter.locale = Locale(identifier: "en_US_POSIX")
            formatter.calendar = calendar
            formatter.timeZone = calendar.timeZone
            formatter.dateFormat = "yyyy-MM-dd"
            dayFormatter = formatter
            let time = DateFormatter()
            time.locale = locale
            time.calendar = calendar
            time.timeZone = calendar.timeZone
            time.setLocalizedDateFormatFromTemplate("jmm")
            timeFormatter = time
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

        /// Instant (réunion) : « sam. 3 oct. · 18:00 » dans le fuseau du calendrier.
        func when(instant: Date) -> String {
            join([HomeDateText.short(instant, locale: locale, calendar: calendar), timeFormatter.string(from: instant)])
                ?? HomeDateText.short(instant, locale: locale, calendar: calendar)
        }
    }
}
