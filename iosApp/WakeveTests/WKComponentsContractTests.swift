import XCTest
@testable import Wakeve

final class WKComponentsContractTests: XCTestCase {

    static let requiredKeys = [
        "wk.avatars.others_format",
        "wk.nav.events",
        "wk.nav.create",
        "wk.nav.activity",
        "wk.status.confirmed",
        "wk.status.pending",
        "wk.status.action_needed",
        "wk.status.draft",
        "wk.module.highlight"
    ]

    /// Clés pluralisées : vivent uniquement dans Localizable.stringsdict.
    static let requiredPluralKeys = [
        "wk.nav.activity.badge_format"
    ]

    func testWKKeysAreLocalizedInSupportedLocales() throws {
        for locale in ["en", "fr", "es", "it", "pt"] {
            let strings = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.strings")
            for key in Self.requiredKeys {
                XCTAssertTrue(strings.contains("\"\(key)\""), "Clé \(key) manquante pour \(locale).")
            }
            let dict = try readProjectFile("iosApp/src/Resources/\(locale).lproj/Localizable.stringsdict")
            for key in Self.requiredPluralKeys {
                XCTAssertTrue(dict.contains("<key>\(key)</key>"), "Clé pluralisée \(key) manquante pour \(locale).")
                XCTAssertFalse(strings.contains("\"\(key)\""), "Clé \(key) morte dans Localizable.strings (\(locale)).")
            }
        }
    }

    private func localized(_ key: String, _ language: String) -> String {
        let bundle = Bundle.main.path(forResource: language, ofType: "lproj").flatMap(Bundle.init(path:)) ?? .main
        return bundle.localizedString(forKey: key, value: nil, table: nil)
    }

    func testStatusNamesAreTranslatedPerLocale() {
        let expected: [String: [String]] = [
            "en": ["Confirmed", "Pending", "Action needed", "Draft"],
            "fr": ["Confirmé", "En attente", "À toi d'agir", "Brouillon"],
            "es": ["Confirmado", "Pendiente", "Requiere acción", "Borrador"],
            "it": ["Confermato", "In attesa", "Azione richiesta", "Bozza"],
            "pt": ["Confirmado", "Pendente", "Ação necessária", "Rascunho"]
        ]
        let keys = ["wk.status.confirmed", "wk.status.pending", "wk.status.action_needed", "wk.status.draft"]
        for (language, names) in expected {
            XCTAssertEqual(keys.map { localized($0, language) }, names, language)
        }
        let highlight = ["en": "Next step", "fr": "Prochaine étape", "es": "Próximo paso", "it": "Prossimo passo", "pt": "Próximo passo"]
        for (language, value) in highlight {
            XCTAssertEqual(localized("wk.module.highlight", language), value, language)
        }
    }

    func testEveryStatusHasADistinctLocalizedName() {
        let names = WK.Status.allCases.map(\.localizedName)
        XCTAssertEqual(Set(names).count, WK.Status.allCases.count)
        for name in names {
            XCTAssertFalse(name.hasPrefix("wk."), "Nom de statut non traduit : \(name)")
        }
    }

    func testCardUsesContinuousTokenRadii() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKCard.swift")
        XCTAssertTrue(source.contains("WK.shape("), "WKCard doit utiliser WK.shape (coins continus).")
        XCTAssertTrue(source.contains("case standard, inset, selected"))
    }

    func testStatusPillExposesTextToVoiceOver() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKStatusPill.swift")
        XCTAssertTrue(source.contains(".accessibilityLabel(text)"), "Le statut ne doit jamais être porté par la couleur seule.")
    }

    static let wkComponentFiles = [
        "WKActionBar", "WKAvatarStack", "WKButtons", "WKCard",
        "WKFloatingNavBar", "WKHeroMetric", "WKModuleTile", "WKStatusPill"
    ]

    func testComponentsUseTokensNotLiterals() throws {
        let forbidden = [
            "Color(hex:", ".font(.system(size:", "Color.white", "lineWidth: 1", "lineWidth: 2",
            "offset(x: 1", "offset(x: 2", "offset(x: 3", "offset(x: 4", "+ 1)", "+ 2)", "spacing: 2)"
        ]
        for name in Self.wkComponentFiles {
            let source = try readProjectFile("iosApp/src/Components/WK/\(name).swift")
            for token in forbidden {
                XCTAssertFalse(source.contains(token), "\(name) contient le littéral « \(token) ».")
            }
            XCTAssertNil(source.range(of: #"cornerRadius: *[0-9]"#, options: .regularExpression), name)
        }
    }

    func testStrokeAndHairlineSpacingTokens() {
        XCTAssertEqual(WK.Space.xxxs, 2)
        XCTAssertEqual(WK.Stroke.hairline, 1)
        XCTAssertEqual(WK.Stroke.emphasis, 1.5)
    }

    func testFixedSizeGlyphButtonsCapDynamicTypeAndOfferLargeContentViewer() throws {
        for name in ["WKButtons", "WKActionBar", "WKFloatingNavBar"] {
            let source = try readProjectFile("iosApp/src/Components/WK/\(name).swift")
            XCTAssertTrue(source.contains(".accessibilityShowsLargeContentViewer"), name)
            XCTAssertTrue(source.contains(".dynamicTypeSize(...DynamicTypeSize.accessibility1)"), name)
            XCTAssertFalse(source.contains("frame(width: WK.Size.minTapTarget"), "\(name) : cadre fixe autour d'un glyphe.")
        }
    }

    func testChromeUsesHierarchicalForegroundStyles() throws {
        let bar = try readProjectFile("iosApp/src/Components/WK/WKFloatingNavBar.swift")
        XCTAssertFalse(bar.contains("WK.Colors.textPrimary"))
        XCTAssertFalse(bar.contains("WK.Colors.textSecondary"))
        XCTAssertTrue(bar.contains(".opaqueSeparator"), "Contour Reduce Transparency visible.")
        let buttons = try readProjectFile("iosApp/src/Components/WK/WKButtons.swift")
        let circle = buttons.components(separatedBy: "struct WKCircleButton").last ?? ""
        XCTAssertFalse(circle.contains("WK.Colors.textPrimary"))
    }

    func testActionBarItemIdentityIsStable() {
        let a = WKActionBar.Item(systemImage: "trash", label: "Supprimer") {}
        let b = WKActionBar.Item(systemImage: "trash", label: "Delete") {}
        // L'identité ne dépend pas du libellé (donc pas de la langue).
        XCTAssertEqual(a.id, "trash")
        XCTAssertEqual(a.id, b.id)
        let c = WKActionBar.Item(systemImage: "trash", label: "Supprimer", accessibilityID: "wk.test.delete") {}
        let d = WKActionBar.Item(systemImage: "trash", label: "Eliminar", accessibilityID: "wk.test.delete") {}
        XCTAssertEqual(c.id, "wk.test.delete")
        XCTAssertEqual(c.id, d.id)
    }

    func testActionBarAppliesStableAccessibilityIdentifiers() throws {
        _ = WKActionBar(primaryTitle: "OK", primaryAction: {}, secondary: [], primaryAccessibilityID: "wk.test.bar.primary")
        let source = try readProjectFile("iosApp/src/Components/WK/WKActionBar.swift")
        XCTAssertTrue(source.contains(".wkAccessibilityID(item.accessibilityID)"))
        XCTAssertTrue(source.contains(".wkAccessibilityID(primaryAccessibilityID)"))
        XCTAssertFalse(source.contains("+ label"), "L'identité ne doit pas inclure le libellé.")
    }

    func testComponentsUseContinuousCapsules() throws {
        for name in Self.wkComponentFiles + ["WKGallery"] {
            let source = try readProjectFile("iosApp/src/Components/WK/\(name).swift")
            XCTAssertFalse(source.contains("Capsule()"), "\(name) : utiliser Capsule(style: .continuous).")
        }
    }

    func testNavBarBadgeUsesBadgeSizeToken() throws {
        XCTAssertEqual(WK.Size.badge, 16)
        let source = try readProjectFile("iosApp/src/Components/WK/WKFloatingNavBar.swift")
        XCTAssertTrue(source.contains(".frame(minHeight: WK.Size.badge)"))
        XCTAssertFalse(source.contains(".frame(minHeight: WK.Space.md)"))
    }

    func testWrappingButtonTitlesAreCentered() throws {
        let buttons = try readProjectFile("iosApp/src/Components/WK/WKButtons.swift")
        let chip = buttons.components(separatedBy: "struct WKChip").last?
            .components(separatedBy: "struct WKCircleButton").first ?? ""
        XCTAssertTrue(chip.contains(".multilineTextAlignment(.center)"), "WKChip")
        let bar = try readProjectFile("iosApp/src/Components/WK/WKActionBar.swift")
        XCTAssertTrue(bar.contains(".multilineTextAlignment(.center)"), "WKActionBar")
    }

    func testComponentsAcceptStableAccessibilityIdentifiers() {
        // Compile-time contract : chaque composant interactif accepte un identifiant stable (spec §7).
        _ = WKPrimaryButton(title: "OK", accessibilityID: "wk.test.primary") {}
        _ = WKChip(title: "OK", accessibilityID: "wk.test.chip") {}
        _ = WKCircleButton(systemImage: "xmark", accessibilityLabel: "Fermer", accessibilityID: "wk.test.close") {}
        _ = WKModuleTile(systemImage: "car", title: "Transport", summary: "", accessibilityID: "wk.test.tile") {}
    }

    func testLocalizedFormatResolvesByLanguageWhateverTheRegion() {
        func format(_ id: String) -> String { WK.localizedFormat("wk.avatars.others_format", locale: Locale(identifier: id)) }
        XCTAssertEqual(format("pt_BR"), "mais %d")
        XCTAssertEqual(format("en_GB"), "%d others")
        XCTAssertEqual(format("fr_CA"), "%d autres")
        XCTAssertEqual(format("fr"), "%d autres")
        XCTAssertEqual(format("it_CH"), "altri %d")
    }

    func testAppLocaleUsesAppLanguageAndKeepsRegion() {
        XCTAssertEqual(WK.appLocale.language.languageCode?.identifier, Bundle.main.preferredLocalizations.first)
        XCTAssertEqual(WK.appLocale.region, Locale.current.region)
    }

    func testDefaultLocaleArgumentsMatchAppLocale() {
        XCTAssertEqual(
            WKFloatingNavBar.badgeAccessibilityValue(for: 3),
            WKFloatingNavBar.badgeAccessibilityValue(for: 3, locale: WK.appLocale)
        )
        let avatars = ["Léa", "Tom", "Max", "Sam"].map { WKAvatar(id: $0, name: $0) }
        XCTAssertEqual(
            WKAvatarStack.accessibilityLabel(for: avatars),
            WKAvatarStack.accessibilityLabel(for: avatars, locale: WK.appLocale)
        )
    }

    func testCircleButtonRequiresAccessibilityLabelAndHonorsReduceTransparency() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKButtons.swift")
        let circle = source.components(separatedBy: "struct WKCircleButton").last ?? ""
        XCTAssertTrue(circle.contains("let accessibilityLabel: String"))
        XCTAssertTrue(circle.contains("accessibilityReduceTransparency"))
        XCTAssertTrue(circle.contains("#available(iOS 26.0, *)"))
    }

    func testHeroMetricCombinesValueAndCaptionForVoiceOver() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKHeroMetric.swift")
        XCTAssertTrue(source.contains("static func accessibilitySummary"))
        XCTAssertTrue(source.contains("WK.Typo.display"))
    }

    func testHeroMetricSummaryReadsNaturally() {
        XCTAssertEqual(
            WKHeroMetric.accessibilitySummary(caption: "Week-end Annecy", value: "5", unit: "/8", subtitle: "votes reçus"),
            "Week-end Annecy, 5/8, votes reçus"
        )
        XCTAssertEqual(
            WKHeroMetric.accessibilitySummary(caption: "Rendez-vous", value: "19:30", unit: nil, subtitle: nil),
            "Rendez-vous, 19:30"
        )
    }

    func testActionBarSecondaryItemsAreLabelledIconButtons() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKActionBar.swift")
        XCTAssertTrue(source.contains("struct Item: Identifiable"))
        XCTAssertTrue(source.contains(".accessibilityLabel(item.label)"))
        XCTAssertTrue(source.contains("WK.Size.minTapTarget"))
    }

    func testModuleTileSummaryIsReadWithTitleAndStatusName() {
        XCTAssertEqual(
            WKModuleTile.accessibilityLabel(title: "Transport", summary: "2 sans place", status: nil),
            "Transport, 2 sans place"
        )
        XCTAssertEqual(
            WKModuleTile.accessibilityLabel(title: "Transport", summary: "2 sans place", status: .pending),
            "Transport, 2 sans place, \(WK.Status.pending.localizedName)"
        )
    }

    func testModuleTileHighlightIsAnnouncedAsValueNotSelection() throws {
        XCTAssertEqual(WKModuleTile.accessibilityValue(isHighlighted: false), "")
        XCTAssertEqual(WKModuleTile.accessibilityValue(isHighlighted: true), String(localized: "wk.module.highlight"))
        XCTAssertFalse(WKModuleTile.accessibilityValue(isHighlighted: true).isEmpty)
        let source = try readProjectFile("iosApp/src/Components/WK/WKModuleTile.swift")
        XCTAssertFalse(source.contains(".isSelected"), "Le surlignage n'est pas une sélection.")
        XCTAssertFalse(source.contains("children: .ignore"), "Le Button doit garder son trait natif.")
    }

    func testModuleTileNeverUsesStatusColorForText() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKModuleTile.swift")
        XCTAssertFalse(source.contains("status?.color ??"), "status.color est réservé aux indices non textuels.")
        XCTAssertTrue(source.contains("circle.fill"))
    }

    func testHeroMetricActionUsesProminentChipNotSelection() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKHeroMetric.swift")
        XCTAssertTrue(source.contains("style: .prominent"))
        XCTAssertFalse(source.contains("isSelected: true"))
        XCTAssertEqual(WKChip(title: "x", action: {}).style, .standard)
    }

    func testAppZoneHasExactlyEventsAndActivity() {
        XCTAssertEqual(AppZone.allCases, [.events, .activity])
        XCTAssertEqual(AppZone.events.systemImage, "calendar")
        XCTAssertEqual(AppZone.activity.systemImage, "bell")
    }

    func testActivityBadgeLabelIsHiddenWhenZero() {
        XCTAssertNil(WKFloatingNavBar.badgeText(for: 0))
        XCTAssertEqual(WKFloatingNavBar.badgeText(for: 3), "3")
        XCTAssertEqual(WKFloatingNavBar.badgeText(for: 120), "99+")
    }

    func testFloatingNavBarUsesGlassWithFallbacks() throws {
        let source = try readProjectFile("iosApp/src/Components/WK/WKFloatingNavBar.swift")
        XCTAssertTrue(source.contains("#available(iOS 26.0, *)"))
        XCTAssertTrue(source.contains(".regularMaterial"))
        XCTAssertTrue(source.contains("accessibilityReduceTransparency"))
        XCTAssertTrue(source.contains("String(localized: \"wk.nav.create\")"))
    }

    func testActivityBadgeAccessibilityValueUsesPluralsPerLocale() {
        XCTAssertEqual(
            WKFloatingNavBar.badgeAccessibilityValue(for: 1, locale: Locale(identifier: "es")),
            "1 pendiente"
        )
        XCTAssertEqual(
            WKFloatingNavBar.badgeAccessibilityValue(for: 3, locale: Locale(identifier: "es")),
            "3 pendientes"
        )
        XCTAssertEqual(
            WKFloatingNavBar.badgeAccessibilityValue(for: 1, locale: Locale(identifier: "pt")),
            "1 pendente"
        )
        XCTAssertEqual(
            WKFloatingNavBar.badgeAccessibilityValue(for: 0, locale: Locale(identifier: "en")),
            ""
        )
    }

    func readProjectFile(_ relativePath: String) throws -> String {
        let projectRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()   // WakeveTests
            .deletingLastPathComponent()   // iosApp
            .deletingLastPathComponent()   // racine
        return try String(contentsOf: projectRoot.appendingPathComponent(relativePath), encoding: .utf8)
    }
}
