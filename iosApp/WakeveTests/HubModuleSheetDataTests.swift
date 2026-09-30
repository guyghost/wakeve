import XCTest
@testable import Wakeve

/// Règles pures des sheets de modules (couche 5a, #47).
final class HubModuleSheetDataTests: XCTestCase {
    private let fr = Locale(identifier: "fr_FR")
    private var utc: Calendar {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(identifier: "UTC")!
        return calendar
    }

    private func make(
        _ raw: HubModuleSheetRaw,
        isOrganizer: Bool = true,
        isReadOnly: Bool = false,
        pendingSync: Bool = false
    ) -> HubModuleSheetData {
        HubModuleSheetData.make(
            raw: raw, isOrganizer: isOrganizer, isReadOnly: isReadOnly, pendingSync: pendingSync,
            locale: fr, calendar: utc
        )
    }

    private func meal(_ id: String, status: String = "PLANNED", responsibles: [String] = ["Léa"], servings: Int = 8) -> HubModuleSheetRaw.Meal {
        .init(id: id, name: "Barbecue \(id)", date: "2026-10-03", time: "19:30", servings: servings,
              statusName: status, responsibleNames: responsibles)
    }

    private func day(_ iso: String) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.calendar = utc
        formatter.timeZone = utc.timeZone
        formatter.dateFormat = "yyyy-MM-dd"
        return HomeDateText.short(formatter.date(from: iso)!, locale: fr, calendar: utc)
    }

    private func euros(_ amount: Double) -> String { HubSummaryText.currency(amount, code: "EUR", locale: fr) }

    func testExpectedDayTextIsTheFrenchShortDate() {
        XCTAssertTrue(day("2026-10-03").hasPrefix("sam."), day("2026-10-03"))
    }

    // MARK: - Repas

    func testMealsListEveryMealWithWhenPeopleStatusAndResponsibles() {
        let data = make(.meals([meal("a", status: "COMPLETED"), meal("b", responsibles: []), meal("c", status: "CANCELLED", responsibles: [])]))
        XCTAssertEqual(data.module, .meals)
        XCTAssertEqual(data.items.map(\.id), ["a", "b", "c"])
        XCTAssertEqual(data.items[0].title, "Barbecue a")
        XCTAssertEqual(data.items[0].detail, "\(day("2026-10-03")) · 19:30 · 8 personnes")
        XCTAssertEqual(data.items.map(\.status), [.confirmed, .pending, nil])
        XCTAssertEqual(data.items[0].assigneeNames, ["Léa"])
        XCTAssertEqual(data.items[1].assigneeNames, [])
    }

    func testMealsPillCountsReadyMealsLikeTheHubTile() {
        let partial = make(.meals([meal("a", status: "COMPLETED"), meal("b")]))
        XCTAssertEqual(partial.status, .init(text: "1/2 prêts", status: .pending))
        let done = make(.meals([meal("a", status: "COMPLETED"), meal("b", status: "COMPLETED")]))
        XCTAssertEqual(done.status, .init(text: "2/2 prêts", status: .confirmed))
    }

    func testMealsMissingCountsMealsWithoutResponsibleExceptCancelled() {
        let one = make(.meals([meal("a"), meal("b", responsibles: []), meal("c", status: "CANCELLED", responsibles: [])]))
        XCTAssertEqual(one.missing, "1 repas sans responsable")
        let three = make(.meals([meal("a", responsibles: []), meal("b", responsibles: []), meal("c", responsibles: [])]))
        XCTAssertEqual(three.missing, "3 repas sans responsable")
        XCTAssertNil(make(.meals([meal("a")])).missing)
    }

    func testOnlyAnOrganizerOfAnEditableEventCanAddAMeal() {
        XCTAssertTrue(make(.meals([]), isOrganizer: true, isReadOnly: false).canAdd)
        XCTAssertFalse(make(.meals([]), isOrganizer: false, isReadOnly: false).canAdd)
        XCTAssertFalse(make(.meals([]), isOrganizer: true, isReadOnly: true).canAdd)
    }

    func testEmptyMealsSayWhatIsMissingWithoutPill() {
        let data = make(.meals([]))
        XCTAssertEqual(data.items, [])
        XCTAssertNil(data.status)
        XCTAssertEqual(data.missing, "Aucun repas prévu pour l'instant.")
    }

    func testMealAddedLocallyIsListedAndCounted() {
        let data = make(.meals([meal("a", status: "COMPLETED")]))
        let added = data.appendingMeal(meal("b", responsibles: []), locale: fr, calendar: utc)
        XCTAssertEqual(added.items.map(\.id), ["a", "b"])
        XCTAssertEqual(added.status, .init(text: "1/2 prêts", status: .pending))
        XCTAssertEqual(added.missing, "1 repas sans responsable")
        XCTAssertEqual(added.canAdd, data.canAdd)
    }

    func testUnreadableMealDateFallsBackToTheRawValue() {
        let odd = HubModuleSheetRaw.Meal(id: "x", name: "Pique-nique", date: "demain", time: "", servings: 1,
                                         statusName: "PLANNED", responsibleNames: ["Tom"])
        XCTAssertEqual(make(.meals([odd])).items[0].detail, "demain · 1 personne")
    }

    func testCancelledMealsAreLeftOutOfTheProgressLikeTheHubTile() {
        let data = make(.meals([meal("a", status: "COMPLETED"), meal("b", status: "CANCELLED")]))
        XCTAssertEqual(data.status, .init(text: "1/1 prêts", status: .confirmed))
        XCTAssertNil(make(.meals([meal("a", status: "CANCELLED")])).status, "Aucun repas actif : pas de pastille.")
        XCTAssertEqual(HubSummaryText.mealProgress(statusNames: ["COMPLETED", "PLANNED", "CANCELLED", "ASSIGNED"]),
                       .init(ready: 1, total: 3))
        XCTAssertEqual(HubSummaryText.mealProgress(statusNames: []), .init(ready: 0, total: 0))
    }

    func testHubTileAndSheetShareTheMealProgress() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        let hub = try String(contentsOf: root.appendingPathComponent("src/Services/SharedEventHubSource.swift"), encoding: .utf8)
        let sheet = try String(contentsOf: root.appendingPathComponent("src/Models/Hub/HubModuleSheetData.swift"), encoding: .utf8)
        XCTAssertTrue(hub.contains("HubSummaryText.mealProgress(statusNames:"))
        XCTAssertFalse(hub.contains("getMealPlanningSummary"), "Le résumé Kotlin compte les repas annulés.")
        XCTAssertTrue(sheet.contains("HubSummaryText.mealProgress(statusNames:"))
    }

    func testMealStatusTextSaysReadyOrToPrepare() {
        let data = make(.meals([meal("a", status: "COMPLETED"), meal("b", status: "ASSIGNED"), meal("c", status: "CANCELLED")]))
        XCTAssertEqual(data.items.map(\.statusText), ["Prêt", "À préparer", nil])
    }

    // MARK: - Matériel

    func testEquipmentItemsShowQuantityStatusAndAssignee() {
        let data = make(.equipment([
            .init(id: "t", name: "Tente 4 places", quantity: 2, statusName: "NEEDED", assigneeName: nil),
            .init(id: "g", name: "Barbecue", quantity: 1, statusName: "PACKED", assigneeName: "Tom"),
            .init(id: "c", name: "Glacière", quantity: 1, statusName: "CANCELLED", assigneeName: nil)
        ]), isOrganizer: true)
        XCTAssertEqual(data.module, .equipment)
        XCTAssertEqual(data.items.map(\.title), ["Tente 4 places", "Barbecue", "Glacière"])
        XCTAssertEqual(data.items[0].detail, "Quantité : 2")
        XCTAssertEqual(data.items.map(\.status), [.pending, .confirmed, nil])
        XCTAssertEqual(data.items[1].assigneeNames, ["Tom"])
        XCTAssertEqual(data.missing, "1 objet sans personne")
        XCTAssertNil(data.status)
        XCTAssertFalse(data.canAdd, "Pas de formulaire hors écran plein en 5a.")
    }

    func testAssignedButNeededEquipmentCountsAsCovered() {
        let data = make(.equipment([.init(id: "t", name: "Tente", quantity: 1, statusName: "NEEDED", assigneeName: "Léa")]))
        XCTAssertEqual(data.items[0].status, .confirmed)
        XCTAssertNil(data.missing)
    }

    func testEquipmentStatusTextSaysCoveredOrToFind() {
        let data = make(.equipment([
            .init(id: "t", name: "Tente", quantity: 1, statusName: "NEEDED", assigneeName: nil),
            .init(id: "g", name: "Barbecue", quantity: 1, statusName: "NEEDED", assigneeName: "Tom"),
            .init(id: "c", name: "Glacière", quantity: 1, statusName: "CANCELLED", assigneeName: nil)
        ]))
        XCTAssertEqual(data.items.map(\.statusText), ["À trouver", "Pris en charge", nil])
    }

    func testEmptyEquipment() {
        XCTAssertEqual(make(.equipment([])).missing, "Aucun matériel listé pour l'instant.")
    }

    // MARK: - Activités

    func testActivitiesShowWhenWhereAndRegistrations() {
        let data = make(.activities([
            .init(id: "h", name: "Randonnée au lac", date: "2026-10-04", time: "09:00", location: "Semnoz",
                  registeredNames: ["Léa", "Tom"]),
            .init(id: "k", name: "Kayak", date: nil, time: nil, location: nil, registeredNames: [])
        ]))
        XCTAssertEqual(data.module, .activities)
        XCTAssertEqual(data.items[0].detail, "\(day("2026-10-04")) · 09:00 · Semnoz · 2 inscrits")
        XCTAssertNil(data.items[1].detail)
        XCTAssertEqual(data.items[0].assigneeNames, ["Léa", "Tom"])
        XCTAssertNil(data.items[0].status)
        XCTAssertNil(data.missing)
        XCTAssertFalse(data.canAdd)
    }

    func testEmptyActivities() {
        XCTAssertEqual(make(.activities([])).missing, "Aucune activité prévue pour l'instant.")
    }

    // MARK: - Hébergement

    func testAccommodationShowsPriceCapacityAndSelection() {
        let data = make(.accommodation([
            .init(id: "c", name: "Chalet des Aravis", pricePerNightCents: 12_000, capacity: 8, bookingStatusName: "CONFIRMED"),
            .init(id: "h", name: "Hôtel du Lac", pricePerNightCents: 0, capacity: 0, bookingStatusName: "SEARCHING")
        ]))
        XCTAssertEqual(data.module, .accommodation)
        XCTAssertEqual(data.items[0].detail, "\(euros(120)) / nuit · 8 couchages")
        XCTAssertNil(data.items[1].detail)
        XCTAssertEqual(data.items.map(\.status), [.confirmed, nil])
        XCTAssertNil(data.missing)
        XCTAssertFalse(data.canAdd)
    }

    func testReservedAccommodationIsRetainedButPending() {
        let data = make(.accommodation([.init(id: "c", name: "Chalet", pricePerNightCents: 0, capacity: 4, bookingStatusName: "RESERVED")]))
        XCTAssertEqual(data.items[0].status, .pending)
        XCTAssertNil(data.missing)
    }

    func testAccommodationStatusTextFollowsTheBookingStatus() {
        let data = make(.accommodation([
            .init(id: "r", name: "Gîte", pricePerNightCents: 0, capacity: 0, bookingStatusName: "RESERVED"),
            .init(id: "c", name: "Chalet", pricePerNightCents: 0, capacity: 0, bookingStatusName: "CONFIRMED"),
            .init(id: "s", name: "Hôtel", pricePerNightCents: 0, capacity: 0, bookingStatusName: "SEARCHING")
        ]))
        XCTAssertEqual(data.items.map(\.status), [.pending, .confirmed, nil])
        XCTAssertEqual(data.items.map(\.statusText), ["Réservé", "Confirmé", nil])
    }

    func testAccommodationWithoutRetainedOptionSaysSo() {
        let data = make(.accommodation([.init(id: "h", name: "Hôtel", pricePerNightCents: 9_950, capacity: 2, bookingStatusName: "SEARCHING")]))
        XCTAssertEqual(data.items[0].detail, "\(euros(99.5)) / nuit · 2 couchages")
        XCTAssertEqual(data.missing, "Aucun hébergement retenu pour l'instant.")
        XCTAssertEqual(make(.accommodation([])).missing, "Aucun hébergement proposé pour l'instant.")
    }

    // MARK: - Accessibilité (libellé de carte)

    func testCardLabelNamesThePeopleWithTheirRole() {
        let meals = make(.meals([meal("a", status: "COMPLETED", responsibles: ["Léa", "Tom"])]))
        XCTAssertEqual(meals.items[0].accessibilityLabel,
                       "Barbecue a, \(day("2026-10-03")) · 19:30 · 8 personnes, Prêt, Responsables : Léa et Tom")
        let equipment = make(.equipment([.init(id: "g", name: "Barbecue", quantity: 1, statusName: "PACKED", assigneeName: "Tom")]))
        XCTAssertEqual(equipment.items[0].accessibilityLabel, "Barbecue, Quantité : 1, Pris en charge, Apporté par Tom")
        let activities = make(.activities([.init(id: "k", name: "Kayak", date: nil, time: nil, location: nil, registeredNames: ["Léa"])]))
        XCTAssertEqual(activities.items[0].accessibilityLabel, "Kayak, 1 inscrit, Inscrits : Léa")
        let nobody = make(.meals([meal("b", responsibles: [])]))
        XCTAssertFalse(nobody.items[0].accessibilityLabel.contains("Responsables"), nobody.items[0].accessibilityLabel)
    }

    func testCardLabelRolesAreLocalized() {
        let en = Locale(identifier: "en_US")
        XCTAssertEqual(HubModuleSheetData.peopleLabel(for: .meals, names: ["Léa"], locale: en), "Hosts: Léa")
        XCTAssertEqual(HubModuleSheetData.peopleLabel(for: .activities, names: ["Léa", "Tom"], locale: en), "Registered: Léa and Tom")
        XCTAssertEqual(HubModuleSheetData.peopleLabel(for: .equipment, names: ["Tom"], locale: en), "Brought by Tom")
        XCTAssertNil(HubModuleSheetData.peopleLabel(for: .equipment, names: [], locale: en))
        XCTAssertNil(HubModuleSheetData.peopleLabel(for: .accommodation, names: ["Tom"], locale: en))
    }

    // MARK: - Photos et commun

    func testPhotosHaveNoItemsAndReuseTheTileHint() {
        let data = make(.photos)
        XCTAssertEqual(data.module, .photos)
        XCTAssertEqual(data.items, [])
        XCTAssertNil(data.status)
        XCTAssertEqual(data.missing, "Partage tes photos")
        XCTAssertFalse(data.canAdd)
    }

    func testPhotosAreServedWithoutReadingTheDatabase() {
        XCTAssertEqual(SharedEventModuleSheetSource.immediateData(for: .photos, locale: fr),
                       HubModuleSheetData.make(raw: .photos, isOrganizer: false, isReadOnly: false, pendingSync: false, locale: fr))
        for module in [HubModule.meals, .equipment, .activities, .accommodation] {
            XCTAssertNil(SharedEventModuleSheetSource.immediateData(for: module, locale: fr), "\(module)")
        }
    }

    func testPendingSyncIsCarried() {
        XCTAssertTrue(make(.activities([]), pendingSync: true).pendingSync)
        XCTAssertFalse(make(.activities([])).pendingSync)
    }

    // MARK: - Conversion de la source (règles pures)

    func testOnlyAFinalizedEventIsReadOnlyLikeTheLegacyScreens() {
        XCTAssertTrue(SharedEventModuleSheetSource.isReadOnly(statusName: "FINALIZED"))
        for status in ["DRAFT", "POLLING", "COMPARING", "CONFIRMED", "ORGANIZING"] {
            XCTAssertFalse(SharedEventModuleSheetSource.isReadOnly(statusName: status), status)
        }
    }

    func testNamesAreResolvedOncePerLoadAndFallBackToTheIdentifier() {
        var lookups: [String] = []
        var names = SharedEventModuleSheetSource.NameCache { id in
            lookups.append(id)
            return ["u1": "Léa Martin", "u2": "  "][id]
        }
        XCTAssertEqual(names.names(for: ["u1", "u2", "u3", "u1"]), ["Léa Martin", "u2", "u3", "Léa Martin"])
        XCTAssertEqual(names.name(for: nil), nil)
        XCTAssertEqual(names.name(for: ""), nil)
        XCTAssertEqual(names.name(for: "u1"), "Léa Martin")
        XCTAssertEqual(lookups, ["u1", "u2", "u3"], "Chaque identifiant n'est lu qu'une fois.")
    }

    // MARK: - Localisation

    static let sheetStringKeys = [
        "hub.sheet.meals.progress_format", "hub.sheet.meals.empty", "hub.sheet.meals.add",
        "hub.sheet.equipment.empty", "hub.sheet.equipment.quantity_format",
        "hub.sheet.activities.empty",
        "hub.sheet.accommodation.empty", "hub.sheet.accommodation.none_selected",
        "hub.sheet.accommodation.price_per_night_format",
        "hub.sheet.open_full", "hub.sheet.comments",
        "hub.sheet.status.meal_ready", "hub.sheet.status.meal_todo",
        "hub.sheet.status.equipment_covered", "hub.sheet.status.equipment_needed",
        "hub.sheet.status.accommodation_reserved", "hub.sheet.status.accommodation_confirmed",
        "hub.sheet.a11y.meal_responsibles_format", "hub.sheet.a11y.activity_registered_format",
        "hub.sheet.a11y.equipment_brought_by_format"
    ]
    static let sheetPluralKeys = [
        "hub.sheet.meals.unassigned_count", "hub.sheet.meals.people_count",
        "hub.sheet.equipment.unassigned_count", "hub.sheet.activities.registered_count"
    ]
    /// Clés existantes réutilisées par les sheets.
    static let reusedKeys = [
        "common.close", "common.retry", "common.error_generic", "common.loading",
        "hub.tile.photos_hint", "hub.summary.format", "accommodation.capacity_format",
        "organization.state.pending_sync", "wk.status.confirmed", "wk.status.pending"
    ]

    func testEverySheetKeyExistsInEveryLanguage() throws {
        let root = URL(fileURLWithPath: #filePath).deletingLastPathComponent().deletingLastPathComponent()
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try String(contentsOf: root.appendingPathComponent("src/Resources/\(locale).lproj/Localizable.strings"), encoding: .utf8)
            for key in Self.sheetStringKeys + Self.reusedKeys {
                XCTAssertTrue(strings.contains("\"\(key)\" ="), "\(key) manquante (\(locale))")
            }
            let dict = try String(contentsOf: root.appendingPathComponent("src/Resources/\(locale).lproj/Localizable.stringsdict"), encoding: .utf8)
            for key in Self.sheetPluralKeys {
                XCTAssertTrue(dict.contains("<key>\(key)</key>"), "pluriel \(key) manquant (\(locale))")
            }
        }
    }

    func testEnglishPluralsResolve() {
        let en = Locale(identifier: "en_US")
        XCTAssertEqual(String(format: WK.localizedFormat("hub.sheet.meals.unassigned_count", locale: en), locale: en, 1),
                       "1 meal without a host")
        XCTAssertEqual(String(format: WK.localizedFormat("hub.sheet.equipment.unassigned_count", locale: en), locale: en, 2),
                       "2 items nobody is bringing")
    }
}
