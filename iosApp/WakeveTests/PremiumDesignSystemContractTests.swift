import XCTest
@testable import Wakeve

final class PremiumDesignSystemContractTests: XCTestCase {
    func testPremiumLiquidGlassComponentsExist() throws {
        let source = try readProjectFile("iosApp/src/Components/DesignSystem/PremiumLiquidGlassComponents.swift")
        let requiredComponents = [
            "struct LiquidGlassCard",
            "struct LiquidGlassButton",
            "struct LiquidGlassToolbar",
            "struct LiquidGlassTabBar",
            "struct EventHeroCard",
            "struct EventListRow",
            "struct ParticipantAvatarStack",
            "struct VoteOptionCard",
            "struct BottomSheet",
            "struct EmptyState",
            "struct LoadingSkeleton"
        ]

        for component in requiredComponents {
            XCTAssertTrue(source.contains(component), "Missing premium component: \(component)")
        }
    }

    func testPremiumTokensCoverSemanticVisualSystem() throws {
        let source = try readProjectFile("iosApp/src/Theme/DesignSystem.swift")
        let requiredTokens = [
            "public static let graphite",
            "public static let midnight",
            "public static let softIvory",
            "public static let mutedLavender",
            "public static let paleBlue",
            "public static let warmAmber",
            "public static func accent",
            "public static func progress",
            "public static func confirmation",
            "public static func destructive",
            "public static func glassTint",
            "public enum Blur",
            "public enum Opacity",
            "public enum Motion",
            "public enum Glass"
        ]

        for token in requiredTokens {
            XCTAssertTrue(source.contains(token), "Missing design token: \(token)")
        }
    }

    func testBrandIdentityTokenFilesExist() throws {
        let brand = try readProjectFile("iosApp/src/Theme/BrandColor.swift")
        let semantic = try readProjectFile("iosApp/src/Theme/SemanticColor.swift")
        let mood = try readProjectFile("iosApp/src/Theme/EventMoodPalette.swift")
        let typography = try readProjectFile("iosApp/src/Theme/TypographyTokens.swift")
        let iconography = try readProjectFile("iosApp/src/Theme/IconographyGuidelines.swift")

        for token in ["midnightBlue", "graphite", "softIvory", "warmPeach", "mutedLavender", "subtleAmber", "blueGrey"] {
            XCTAssertTrue(brand.contains(token), "Missing brand token: \(token)")
        }

        for token in ["nativeChromeSurface", "contentSurface", "selectedState", "callToAction", "progress", "confirmation"] {
            XCTAssertTrue(semantic.contains(token), "Missing semantic token: \(token)")
        }

        for moodName in ["evening", "travel", "birthday", "family", "dinner", "beach", "weekend"] {
            XCTAssertTrue(mood.contains("case \(moodName)") || mood.contains("let \(moodName)"), "Missing mood palette: \(moodName)")
        }

        XCTAssertTrue(typography.contains("SF Pro"))
        XCTAssertTrue(typography.contains("DynamicTypeSize"))
        XCTAssertTrue(iconography.contains("standardActionSymbols"))
        XCTAssertTrue(iconography.contains("wakeveConceptSymbols"))
    }

    func testSharedFrameworkSupportsGenericSimulatorArchitectures() throws {
        let sharedBuild = try readProjectFile("shared/build.gradle.kts")

        XCTAssertTrue(
            sharedBuild.contains("iosSimulatorArm64()"),
            "Apple Silicon simulator builds need the KMP arm64 simulator framework slice."
        )
        XCTAssertTrue(
            sharedBuild.contains("iosX64()"),
            "Generic iOS simulator builds compile x86_64 too; Shared.framework must expose the matching KMP target."
        )
    }

    func testContentCardComponentExistsForNonNavigationSurfaces() throws {
        let source = try readProjectFile("iosApp/src/Components/DesignSystem/WakeveDesignSystemComponents.swift")
        let contentCard = slice(source, from: "struct WakeveContentCard", to: "struct WakeveGlassCard")

        XCTAssertTrue(contentCard.contains("SemanticColor.contentSurface"))
        XCTAssertTrue(contentCard.contains("SemanticColor.border"))
        XCTAssertFalse(contentCard.contains(".liquidGlass("), "WakeveContentCard should stay solid/material-backed content, not Liquid Glass.")
    }

    func testBrandedContentPreviewComponentsExist() throws {
        let source = try readProjectFile("iosApp/src/Components/DesignSystem/BrandedContentPreviewComponents.swift")

        XCTAssertTrue(source.contains("struct WakeveInvitationPreviewCard"))
        XCTAssertTrue(source.contains("struct WakeveGroupCard"))
        XCTAssertTrue(source.contains("struct WakeveWidgetPreviewCard"))
        XCTAssertTrue(source.contains("struct WakeveWidgetPreviewGallery"))
        XCTAssertTrue(source.contains("EventMoodPalette"))
        XCTAssertTrue(source.contains("SemanticColor.contentSurface"))
        XCTAssertTrue(source.contains("case nextEvent"))
        XCTAssertTrue(source.contains("case activeVote"))
        XCTAssertTrue(source.contains("case guestList"))
        XCTAssertTrue(source.contains("case notification"))
        XCTAssertTrue(source.contains("case liveActivity"))
        XCTAssertFalse(source.contains(".liquidGlass("), "Branded content previews are content surfaces and should not use Liquid Glass.")
    }

    func testAddToCalendarButtonUsesLocalizedVisibleCopy() throws {
        let source = try readProjectFile("iosApp/src/Components/AddToCalendarButton.swift")

        XCTAssertTrue(source.contains("String(localized: \"meetings.add_to_calendar\")"))
        XCTAssertTrue(source.contains("calendar.add_event_accessibility"))
        XCTAssertTrue(source.contains("calendar.add_event_hint"))
        XCTAssertFalse(source.contains("Text(\"Add to Calendar\")"))
    }

    func testBrandDocumentationDeliverablesExist() throws {
        let requiredDocs = [
            "docs/design/wakeve-brand-on-ios-audit.md",
            "docs/design/wakeve-brand-guidelines.md",
            "docs/design/wakeve-voice-and-tone.md",
            "docs/design/wakeve-motion-guidelines.md"
        ]

        for path in requiredDocs {
            let doc = try readProjectFile(path)
            XCTAssertTrue(doc.contains("Wakeve"), "Brand doc should name Wakeve: \(path)")
            XCTAssertTrue(doc.contains("native") || doc.contains("iOS"), "Brand doc should identify native iOS constraints: \(path)")
        }
    }

    func testInvitationGradientNoLongerUsesDominantPurpleStack() throws {
        let source = try readProjectFile("iosApp/src/Theme/DesignSystem.swift")
        let invitation = slice(source, from: "public static let invitation", to: "public static let profile")

        XCTAssertFalse(invitation.contains("A71AA0"))
        XCTAssertFalse(invitation.contains("6E13D8"))
        XCTAssertFalse(invitation.contains("11137E"))
        XCTAssertTrue(invitation.contains("midnight"))
    }

    func testLiquidGlassFallbacksRemainAvailable() throws {
        let source = try readProjectFile("iosApp/src/Theme/LiquidGlassModifier.swift")

        XCTAssertTrue(source.contains("#available(iOS 26.0, *)"))
        XCTAssertTrue(source.contains("accessibilityReduceTransparency"))
        XCTAssertTrue(source.contains("regularMaterial"))
    }

    func testContentSurfacesUseBrandTokensWithoutGlassOnEmptyState() throws {
        let components = try readProjectFile("iosApp/src/Components/DesignSystem/PremiumLiquidGlassComponents.swift")
        let shared = try readProjectFile("iosApp/src/Components/DesignSystem/WakeveDesignSystemComponents.swift")
        let emptyState = slice(components, from: "struct EmptyState", to: "struct LoadingSkeleton")
        let eventHero = slice(components, from: "struct EventHeroCard", to: "struct EventListRow")

        XCTAssertTrue(eventHero.contains("EventMoodPalette"))
        XCTAssertTrue(components.contains("SemanticColor.contentSurface"))
        XCTAssertTrue(components.contains("BrandColor.calmAccent"))
        XCTAssertTrue(shared.contains("SemanticColor.appBackground"))
        XCTAssertFalse(emptyState.contains(".liquidGlass("), "EmptyState is content-layer brand expression and should not use Liquid Glass.")
    }

    func testParticipantAvatarStackUsesSingularAccessibilityLabel() throws {
        let source = try readProjectFile("iosApp/src/Components/DesignSystem/PremiumLiquidGlassComponents.swift")

        XCTAssertTrue(source.contains("private var participantAccessibilityLabel"))
        XCTAssertTrue(source.contains(#"participant\(initials.count > 1 ? "s" : "")"#))
        XCTAssertFalse(source.contains(#".accessibilityLabel("\(initials.count) participants")"#))
    }

    func testDecisionMomentsUseSharedHapticFeedback() throws {
        let shared = try readProjectFile("iosApp/src/Components/SharedComponents.swift")
        let vote = try readProjectFile("iosApp/src/Views/Polls/PollVotingView.swift")
        let results = try readProjectFile("iosApp/src/Views/Polls/PollResultsView.swift")
        let scenarios = try readProjectFile("iosApp/src/Views/Events/ScenarioOrganizationView.swift")
        let transportView = try readProjectFile("iosApp/src/Views/Events/TransportPlanningView.swift")
        let transportModel = try readProjectFile("iosApp/src/ViewModels/TransportPlanningViewModel.swift")
        let budget = try readProjectFile("iosApp/src/Views/Budget/BudgetDetailView.swift")

        XCTAssertTrue(shared.contains("enum WakeveHaptics"))
        XCTAssertTrue(shared.contains("UINotificationFeedbackGenerator"))
        XCTAssertTrue(shared.contains("UISelectionFeedbackGenerator"))
        XCTAssertTrue(vote.contains("WakeveHaptics.success()"))
        XCTAssertTrue(vote.contains("private func selectVote(_ vote: PollVote)"))
        XCTAssertTrue(vote.contains("WakeveHaptics.selection()"))
        XCTAssertTrue(results.contains("WakeveHaptics.success()"))
        XCTAssertTrue(results.contains(".simultaneousGesture(TapGesture().onEnded"))
        XCTAssertTrue(scenarios.contains("WakeveHaptics.success()"))
        XCTAssertTrue(scenarios.contains("WakeveHaptics.selection()"))
        XCTAssertTrue(transportView.contains("WakeveHaptics.selection()"))
        XCTAssertTrue(transportView.contains("WakeveHaptics.warning()"))
        XCTAssertTrue(transportModel.contains("WakeveHaptics.success()"))
        XCTAssertTrue(budget.contains("WakeveHaptics.warning()"))
    }

    func testBudgetDetailUsesBrandedContentSurfacesInsteadOfNativeList() throws {
        let overview = try readProjectFile("iosApp/src/Views/Budget/BudgetOverviewView.swift")
        let source = try readProjectFile("iosApp/src/Views/Budget/BudgetDetailView.swift")
        let overviewContent = slice(overview, from: "private var contentView", to: "private func formatAmount")
        let listView = slice(source, from: "private var listView", to: "private var isPreviewing")
        let emptyView = slice(source, from: "private var emptyView", to: "// MARK: - List")
        let addSheet = try readProjectFile("iosApp/src/Views/Budget/AddBudgetItemSheet.swift")
        let budgetRow = try readProjectFile("iosApp/src/Views/Budget/BudgetItemRow.swift")

        XCTAssertTrue(overviewContent.contains("WakeveScreenBackground(style: .grouped)"))
        XCTAssertTrue(overviewContent.contains("WakeveContentCard("))
        XCTAssertTrue(overview.contains("budget.overview.summary"))
        XCTAssertTrue(overview.contains("budget.overview.view_all_expenses"))
        XCTAssertTrue(overview.contains("sync.pending_changes"))
        XCTAssertFalse(overview.contains("Résumé"), "Budget overview copy should be localized.")
        XCTAssertFalse(overview.contains("Modifications locales en attente d'envoi"), "Budget overview sync copy should use localized keys.")
        XCTAssertFalse(overview.contains("List {"), "Budget overview should avoid native List chrome.")
        XCTAssertTrue(listView.contains("ScrollView"))
        XCTAssertTrue(listView.contains("WakeveScreenBackground(style: .grouped)"))
        XCTAssertTrue(listView.contains("WakeveContentCard("))
        XCTAssertTrue(listView.contains("BudgetSectionHeader"))
        XCTAssertFalse(listView.contains("List {"), "Budget detail should stay visually aligned with premium content surfaces, not a native utility list.")
        XCTAssertTrue(addSheet.contains("NavigationStack"))
        XCTAssertTrue(addSheet.contains("ScrollView(showsIndicators: false)"))
        XCTAssertTrue(addSheet.contains("WakeveContentCard("))
        XCTAssertTrue(addSheet.contains("BudgetCategoryOptionCard"))
        XCTAssertTrue(addSheet.contains("WakeveActionButton("))
        XCTAssertTrue(addSheet.contains("budget.add_sheet.title"))
        XCTAssertTrue(addSheet.contains("budget.add_sheet.validation.amount_required"))
        XCTAssertFalse(addSheet.contains("Nouvelle dépense"), "Add expense copy should be localized.")
        XCTAssertFalse(addSheet.contains("Ajoutez un coût clair"), "Add expense explanatory copy should be localized.")
        XCTAssertFalse(addSheet.contains("Form {"), "Add expense should avoid native Form chrome.")
        XCTAssertFalse(addSheet.contains("NavigationView"), "Add expense should use modern NavigationStack.")
        XCTAssertTrue(source.contains("budget.expenses_title"))
        XCTAssertTrue(source.contains("budget.delete_confirmation.title"))
        XCTAssertTrue(source.contains("budget.empty_expenses_title"))
        XCTAssertTrue(emptyView.contains("WakeveScreenBackground(style: .grouped)"))
        XCTAssertTrue(emptyView.contains("WakeveContentCard(prominence: .prominent"))
        XCTAssertTrue(emptyView.contains("WakeveActionButton("))
        XCTAssertTrue(emptyView.contains("WakeveHaptics.selection()"))
        XCTAssertFalse(emptyView.contains(".background(.blue"), "Budget empty state should use WakeveActionButton styling, not a generic blue button.")
        XCTAssertFalse(source.contains("Aucune dépense"), "Budget detail empty state should be localized.")
        XCTAssertTrue(budgetRow.contains("WakeveContentCard("))
        XCTAssertTrue(budgetRow.contains("WakeveScreenBackground(style: .grouped)"))
        XCTAssertTrue(budgetRow.contains("budget.item.paid"))
        XCTAssertTrue(budgetRow.contains("budget.item.estimated"))
        XCTAssertTrue(budgetRow.contains("budget.item.shared_cost_format"))
        XCTAssertFalse(budgetRow.contains("Text(\"payé\")"), "Budget row paid state should be localized.")
        XCTAssertFalse(budgetRow.contains("Text(\"estimé\")"), "Budget row estimated state should be localized.")
        XCTAssertFalse(budgetRow.contains("List {"), "Budget row previews should not reintroduce native List chrome.")
    }

    private func readProjectFile(_ relativePath: String) throws -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let testsDir = fileURL.deletingLastPathComponent()
        let iosAppDir = testsDir.deletingLastPathComponent()
        let projectRoot = iosAppDir.deletingLastPathComponent()
        let targetURL = projectRoot.appendingPathComponent(relativePath)
        return try String(contentsOf: targetURL, encoding: .utf8)
    }

    private func slice(_ source: String, from startMarker: String, to endMarker: String) -> String {
        guard let start = source.range(of: startMarker)?.lowerBound else {
            return source
        }

        let tail = source[start...]
        guard let end = tail.range(of: endMarker)?.lowerBound else {
            return String(tail)
        }

        return String(tail[..<end])
    }
}
