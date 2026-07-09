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

    func testLiquidGlassContinuousMotionHonorsReduceMotion() throws {
        let animations = try readProjectFile("iosApp/src/Components/LiquidGlassAnimations.swift")
        let onboarding = try readProjectFile("iosApp/src/Views/Auth/OnboardingView.swift")

        XCTAssertTrue(
            animations.contains("@Environment(\\.accessibilityReduceMotion)"),
            "Liquid Glass modifiers with continuous motion must observe Reduce Motion."
        )
        XCTAssertTrue(
            animations.contains("reduceMotion ? nil :"),
            "Liquid Glass animations must disable non-essential animation when Reduce Motion is enabled."
        )
        XCTAssertFalse(
            animations.contains("value: UUID()"),
            "Animation identity must be stable; a fresh UUID causes animation on every render."
        )
        XCTAssertTrue(
            onboarding.contains("@Environment(\\.accessibilityReduceMotion)"),
            "Onboarding must observe Reduce Motion before starting its perpetual icon animation."
        )
        XCTAssertTrue(
            onboarding.contains("reduceMotion ? nil :"),
            "Onboarding repeatForever animation must be disabled under Reduce Motion."
        )
    }

    func testCompactSheetControlsExposeMinimumTouchTargets() throws {
        let eventInfo = try readProjectFile("iosApp/src/Components/EventInfoSheet.swift")
        let backgroundPicker = try readProjectFile("iosApp/src/Components/BackgroundPickerSheet.swift")
        let locationPicker = try readProjectFile("iosApp/src/Components/LocationSelectionSheet.swift")
        let minimumHitFrame = ".frame(minWidth: 44, minHeight: 44)"

        XCTAssertGreaterThanOrEqual(
            eventInfo.components(separatedBy: minimumHitFrame).count - 1,
            1,
            "EventInfoSheet confirmation control needs a minimum 44pt hit frame."
        )
        XCTAssertGreaterThanOrEqual(
            backgroundPicker.components(separatedBy: minimumHitFrame).count - 1,
            1,
            "BackgroundPickerSheet close control needs a minimum 44pt hit frame."
        )
        XCTAssertGreaterThanOrEqual(
            locationPicker.components(separatedBy: minimumHitFrame).count - 1,
            2,
            "LocationSelectionSheet confirmation and search-clear controls need minimum 44pt hit frames."
        )
    }

    func testRootErrorViewUsesSemanticSurfacesAndSharedAction() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let errorView = slice(source, from: "struct ErrorView: View", to: "// MARK: - Authenticated Content View")

        XCTAssertTrue(errorView.contains("SemanticColor."), "ErrorView colors must come from semantic brand tokens.")
        XCTAssertTrue(errorView.contains("WakeveActionButton("), "ErrorView recovery must use the shared action component.")
        XCTAssertFalse(errorView.contains("Color.blue"), "ErrorView must not hard-code a generic blue brand color.")
        XCTAssertFalse(errorView.contains("Color.purple"), "ErrorView must not hard-code a generic purple brand color.")
        XCTAssertFalse(errorView.contains("Color.pink"), "ErrorView must not hard-code a generic pink brand color.")
    }

    func testLegacyTypographyUsesSemanticDynamicTypeStyles() throws {
        let source = try readProjectFile("iosApp/src/Theme/DesignSystem.swift")
        let typography = slice(source, from: "public struct Typography", to: "// MARK:")

        XCTAssertFalse(
            typography.contains("Font.system(size:"),
            "Legacy Typography tokens must use semantic SwiftUI text styles so they scale with Dynamic Type."
        )
        for style in ["Font.largeTitle", "Font.headline", "Font.body", "Font.subheadline", "Font.caption"] {
            XCTAssertTrue(typography.contains(style), "Legacy Typography must map its roles to semantic style \(style).")
        }
    }

    func testHighTrafficScreensStayWithinFixedTypographyMigrationBudgets() throws {
        let budgets = [
            ("iosApp/src/Views/Events/CreateEventSheet.swift", 16),
            ("iosApp/src/Views/Polls/PollResultsView.swift", 5),
            ("iosApp/src/Views/Polls/PollVotingView.swift", 3)
        ]

        for (path, budget) in budgets {
            let source = try readProjectFile(path)
            let count = occurrenceCount(of: ".font(.system(size:", in: source)
            XCTAssertLessThanOrEqual(
                count,
                budget,
                "\(path) has \(count) fixed-size fonts; at most \(budget) decorative/icon occurrences may remain."
            )
        }
    }

    func testPollResultsUsesSemanticStatusColors() throws {
        let source = try readProjectFile("iosApp/src/Views/Polls/PollResultsView.swift")

        for rawColor in ["Color.red", "Color.orange", "Color.green", "Color.blue"] {
            XCTAssertFalse(source.contains(rawColor), "PollResultsView must replace \(rawColor) with a semantic status token.")
        }
        for token in ["SemanticColor.destructive", "SemanticColor.warning", "SemanticColor.confirmation", "SemanticColor.accent"] {
            XCTAssertTrue(source.contains(token), "PollResultsView must express status with \(token).")
        }
    }

    func testLegacySheetsAndCalendarActionUseSemanticAccentColors() throws {
        let paths = [
            "iosApp/src/Components/AddToCalendarButton.swift",
            "iosApp/src/Components/EventInfoSheet.swift",
            "iosApp/src/Components/LocationSelectionSheet.swift"
        ]

        for path in paths {
            let source = try readProjectFile(path)
            XCTAssertFalse(source.contains("Color.blue"), "\(path) must not hard-code Color.blue.")
            XCTAssertTrue(source.contains("SemanticColor."), "\(path) must use semantic design-system colors.")
        }
    }

    func testSharedAndCreateEventAnimationsHonorReduceMotion() throws {
        let shared = try readProjectFile("iosApp/src/Components/SharedComponents.swift")
        let createEvent = try readProjectFile("iosApp/src/Views/Events/CreateEventSheet.swift")

        for (path, source) in [
            ("SharedComponents.swift", shared),
            ("CreateEventSheet.swift", createEvent)
        ] {
            XCTAssertTrue(source.contains("@Environment(\\.accessibilityReduceMotion)"), "\(path) must observe Reduce Motion.")
            XCTAssertTrue(source.contains("reduceMotion ? nil :"), "\(path) must disable non-essential animations under Reduce Motion.")
        }
    }

    func testLiquidGlassAnimationEntryPointsAndBadgeHonorReduceMotion() throws {
        let source = try readProjectFile("iosApp/src/Components/LiquidGlassAnimations.swift")
        let helpers = slice(source, from: "struct LiquidGlassAnimations", to: "// MARK: - Animation Modifiers")
        let animatedBadge = slice(source, from: "struct AnimatedBadgeModifier", to: "extension View")

        XCTAssertTrue(helpers.contains("reduceMotion: Bool"), "LiquidGlassAnimations must expose accessibility-aware helper overloads.")
        XCTAssertTrue(helpers.contains("reduceMotion ? nil :"), "Accessibility-aware Liquid Glass helpers must return no animation for Reduce Motion.")
        XCTAssertTrue(animatedBadge.contains("@Environment(\\.accessibilityReduceMotion)"), "AnimatedBadgeModifier must observe Reduce Motion.")
        XCTAssertTrue(animatedBadge.contains("reduceMotion ? nil :"), "AnimatedBadgeModifier must suppress entrance animation under Reduce Motion.")
    }

    func testWakeveCircleButtonGuaranteesAccessibleHitTarget() throws {
        let source = try readProjectFile("iosApp/src/Components/DesignSystem/WakeveDesignSystemComponents.swift")
        let circleButton = slice(source, from: "struct WakeveCircleButton", to: "private var background")

        XCTAssertTrue(circleButton.contains(".frame(minWidth: 44, minHeight: 44)"), "WakeveCircleButton needs a minimum 44pt hit target.")
        XCTAssertTrue(circleButton.contains(".contentShape(Circle())"), "WakeveCircleButton must make its full circular hit target interactive.")
    }

    func testScenarioOrganizationUsesDynamicTypeAndSemanticAccent() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ScenarioOrganizationView.swift")
        let fixedFontCount = occurrenceCount(of: ".font(.system(size:", in: source)

        XCTAssertLessThanOrEqual(
            fixedFontCount,
            11,
            "ScenarioOrganizationView has \(fixedFontCount) fixed-size fonts; only 11 decorative or icon occurrences may remain."
        )
        for rawAccent in ["Color.blue", "? .blue", ".tint(.blue)"] {
            XCTAssertFalse(source.contains(rawAccent), "ScenarioOrganizationView must replace raw accent \(rawAccent) with a semantic token.")
        }
        XCTAssertTrue(
            source.contains("SemanticColor.accent"),
            "ScenarioOrganizationView must use the semantic accent token for selection and action emphasis."
        )
    }

    func testEventDetailExperienceUsesDynamicTypeAndAccessibleToolbarTargets() throws {
        let source = try readProjectFile("iosApp/src/Views/EventDetailExperienceView.swift")
        let fixedFontCount = occurrenceCount(of: ".font(.system(size:", in: source)
        let visualControlCount = occurrenceCount(of: ".frame(width: 36, height: 36)", in: source) - 1
        let accessibleHitTargetCount = occurrenceCount(of: ".frame(minWidth: 44, minHeight: 44)", in: source)

        XCTAssertLessThanOrEqual(
            fixedFontCount,
            7,
            "EventDetailExperienceView has \(fixedFontCount) fixed-size fonts; only 7 decorative symbol occurrences may remain."
        )
        XCTAssertEqual(
            visualControlCount,
            3,
            "The two back buttons and overflow menu are the expected custom 36pt navigation/action controls."
        )
        XCTAssertGreaterThanOrEqual(
            accessibleHitTargetCount,
            visualControlCount,
            "Every custom 36pt navigation/action control must expose a minimum 44pt hit target."
        )
    }

    func testInboxDetailMeetsTypographyMotionAndControlAccessibilityBudgets() throws {
        let source = try readProjectFile("iosApp/src/Views/Inbox/InboxDetailView.swift")
        let handoff = slice(source, from: "private var groupHandoffCard", to: "private func copyGroupHandoffMessage")
        let rsvpButton = slice(source, from: "private struct RSVPActionButton", to: "// MARK: - Update Row")
        let fixedFontCount = occurrenceCount(of: ".font(.system(size:", in: source)

        XCTAssertLessThanOrEqual(
            fixedFontCount,
            15,
            "InboxDetailView has \(fixedFontCount) fixed-size fonts; only 15 decorative or icon occurrences may remain."
        )
        XCTAssertTrue(source.contains("@Environment(\\.accessibilityReduceMotion)"), "InboxDetailView must observe Reduce Motion.")
        XCTAssertTrue(
            source.contains("reduceMotion ? nil") || source.contains("if reduceMotion"),
            "InboxDetailView must explicitly suppress or branch around copied-state motion when Reduce Motion is enabled."
        )
        XCTAssertGreaterThanOrEqual(
            occurrenceCount(of: ".frame(minWidth: 44, minHeight: 44)", in: source),
            2,
            "Both inbox moderation menus must expose minimum 44pt hit targets."
        )
        XCTAssertGreaterThanOrEqual(
            occurrenceCount(of: ".frame(minHeight: 44)", in: handoff),
            2,
            "Share and copy handoff actions must each expose an explicit minimum 44pt height."
        )
        XCTAssertTrue(rsvpButton.contains(".frame(minHeight: 44)"), "Every RSVPActionButton must expose an explicit minimum 44pt height.")

        for rawControlColor in [
            "Color(uiColor: .systemGray4)",
            "Color(uiColor: .systemGray5)",
            "Color(.systemGray4)",
            "Color(.systemGray5)"
        ] {
            XCTAssertFalse(source.contains(rawControlColor), "InboxDetailView must replace raw control color \(rawControlColor) with a semantic token.")
        }
    }

    func testContentViewMeetsLegacyTypographyColorMotionAndAITargetContracts() throws {
        let source = try readProjectFile("iosApp/src/Views/App/ContentView.swift")
        let legacyEventList = slice(source, from: "struct EventListView", to: "struct EventCard")
        let eventAIActionButton = slice(source, from: "private struct EventAIActionButton", to: "private struct EventAIList")
        let fixedFontCount = occurrenceCount(of: ".font(.system(size:", in: source)

        XCTAssertLessThanOrEqual(
            fixedFontCount,
            13,
            "ContentView.swift has \(fixedFontCount) fixed-size fonts; only 13 decorative or icon occurrences may remain."
        )
        XCTAssertFalse(legacyEventList.contains(".foregroundColor(.blue)"), "The legacy event-list section must use a semantic accent token.")
        XCTAssertFalse(source.contains("Color(.tertiaryLabel)"), "ContentView must use semantic tertiary text tokens.")
        XCTAssertFalse(source.contains(".foregroundColor(.orange)"), "ContentView must use a semantic warning token.")
        XCTAssertTrue(source.contains("@Environment(\\.accessibilityReduceMotion)"), "The root ContentView must observe Reduce Motion.")
        XCTAssertTrue(source.contains("reduceMotion ? nil :"), "Root loading/onboarding state motion must be disabled under Reduce Motion.")
        XCTAssertTrue(
            eventAIActionButton.contains(".frame(minHeight: 44)"),
            "EventAIActionButton must preserve its compact visual style while exposing a minimum 44pt hit target."
        )
    }

    func testSecondaryScreensStayWithinFixedTypographyBudgets() throws {
        let budgets = [
            ("iosApp/src/Views/Auth/LoginView.swift", 4),
            ("iosApp/src/Views/Events/HomeView.swift", 7),
            ("iosApp/src/Views/Events/ParticipantManagementView.swift", 4),
            ("iosApp/src/Views/Profile/ProfileTabView.swift", 5),
            ("iosApp/src/Views/Explore/ExploreScenarioDetailView.swift", 1),
            ("iosApp/src/Components/BackgroundPickerSheet.swift", 4)
        ]

        for (path, budget) in budgets {
            let source = try readProjectFile(path)
            XCTAssertLessThanOrEqual(
                occurrenceCount(of: ".font(.system(size:", in: source),
                budget,
                "\(path) exceeds its fixed-font decorative/icon budget of \(budget)."
            )
        }

        let explore = try readProjectFile("iosApp/src/Views/Explore/ExploreScenarioDetailView.swift")
        let backgroundPicker = try readProjectFile("iosApp/src/Components/BackgroundPickerSheet.swift")
        XCTAssertTrue(explore.contains("WakeveTheme.Typography"), "Explore scenario text must use shared typography roles.")
        XCTAssertTrue(backgroundPicker.contains("WakeveTheme.Typography"), "Background picker text must use shared typography roles.")
    }

    func testSecondaryMotionAndLegalControlsHonorAccessibilitySettings() throws {
        let login = try readProjectFile("iosApp/src/Views/Auth/LoginView.swift")
        let legal = slice(login, from: "private var privacyTermsView", to: "// MARK: - Actions")
        XCTAssertGreaterThanOrEqual(occurrenceCount(of: ".frame(minHeight: 44)", in: legal), 2, "Privacy and terms links each need a 44pt minimum target.")

        for path in [
            "iosApp/src/Views/Events/HomeView.swift",
            "iosApp/src/Views/Events/ParticipantManagementView.swift",
            "iosApp/src/Views/Explore/LeaderboardView.swift",
            "iosApp/src/Components/LocationSelectionSheet.swift"
        ] {
            let source = try readProjectFile(path)
            XCTAssertTrue(source.contains("@Environment(\\.accessibilityReduceMotion)"), "\(path) must observe Reduce Motion.")
            XCTAssertTrue(source.contains("reduceMotion ? nil"), "\(path) must disable non-essential animation under Reduce Motion.")
        }

        let premium = try readProjectFile("iosApp/src/Components/DesignSystem/PremiumLiquidGlassComponents.swift")
        let bottomSheet = slice(premium, from: "struct BottomSheet", to: "struct EmptyState")
        XCTAssertTrue(bottomSheet.contains(".animation(reduceMotion ? nil :"), "BottomSheet must fully remove animation under Reduce Motion.")
    }

    func testParticipantStatusAndSelectionColorsAreSemantic() throws {
        let source = try readProjectFile("iosApp/src/Views/Events/ParticipantManagementView.swift")

        for rawColor in [".foregroundColor(.orange)", ".foregroundColor(.blue)", ".foregroundColor(.green)", "? .orange", "? .blue", "? .green"] {
            XCTAssertFalse(source.contains(rawColor), "Participant management must replace direct state color \(rawColor).")
        }
        for token in ["SemanticColor.warning", "SemanticColor.accent", "SemanticColor.confirmation"] {
            XCTAssertTrue(source.contains(token), "Participant management must expose \(token) for status/selection meaning.")
        }
    }

    func testLiquidGlassTextFieldUsesSemanticErrorsAndAccessibleIconButtons() throws {
        let source = try readProjectFile("iosApp/src/Components/LiquidGlassTextField.swift")

        XCTAssertFalse(source.contains(".foregroundColor(.red)"), "Text-field errors must use a semantic destructive color.")
        XCTAssertTrue(source.contains("SemanticColor.destructive"), "Text-field errors must use SemanticColor.destructive.")
        XCTAssertGreaterThanOrEqual(
            occurrenceCount(of: ".frame(minWidth: 44, minHeight: 44)", in: source),
            2,
            "Left and right text-field icon buttons each need a minimum 44pt hit target."
        )
    }

    func testBudgetInteractionTargetsAndStateColorsAreSemantic() throws {
        let itemRow = try readProjectFile("iosApp/src/Views/Budget/BudgetItemRow.swift")
        let detail = try readProjectFile("iosApp/src/Views/Budget/BudgetDetailView.swift")
        let overview = try readProjectFile("iosApp/src/Views/Budget/BudgetOverviewView.swift")
        let addSheet = try readProjectFile("iosApp/src/Views/Budget/AddBudgetItemSheet.swift")
        let paidControl = slice(itemRow, from: "// Paid indicator", to: "VStack(alignment: .leading")
        let detailMenu = slice(detail, from: "private struct BudgetDetailItemRow", to: "// MARK: - BudgetSummaryRow")

        XCTAssertTrue(paidControl.contains("Button"), "The paid toggle must be a semantic Button, not an onTapGesture image.")
        XCTAssertTrue(paidControl.contains(".frame(minWidth: 44, minHeight: 44)"), "The paid toggle needs a minimum 44pt hit target.")
        XCTAssertTrue(detailMenu.contains(".frame(minWidth: 44, minHeight: 44)"), "The budget item menu needs a minimum 44pt hit target.")

        let budgetSources = itemRow + detail + overview + addSheet
        for rawColor in ["Color.blue", "Color.red", "Color.green", "Color.orange", "Color.yellow", ".foregroundStyle(.blue)", ".foregroundStyle(.red)", ".foregroundStyle(.green)", ".foregroundStyle(.orange)", ".foregroundStyle(.yellow)"] {
            XCTAssertFalse(budgetSources.contains(rawColor), "Budget state colors must replace \(rawColor) with semantic tokens.")
        }
        XCTAssertTrue(budgetSources.contains("SemanticColor."), "Budget views must use semantic state colors.")
    }

    func testMealAndMeetingStateColorsUseSemanticTokens() throws {
        let meals = try readProjectFile("iosApp/src/Views/Events/MealPlanningSheets.swift")
        let meeting = try readProjectFile("iosApp/src/Views/Meeting/MeetingDetailView.swift")

        for rawColor in [".foregroundColor(.blue)", ".foregroundColor(.red)", ".foregroundColor(.green)", "Color.blue", "Color.red", "Color.green"] {
            XCTAssertFalse(meals.contains(rawColor), "Meal planning actions/statuses must replace \(rawColor) with a semantic token.")
        }
        XCTAssertTrue(meals.contains("SemanticColor."), "Meal planning must use semantic action/status colors.")

        for rawState in [".foregroundStyle(.orange)", ".foregroundStyle(.green)"] {
            XCTAssertFalse(meeting.contains(rawState), "Meeting detail states must replace \(rawState) with semantic tokens.")
        }
        XCTAssertTrue(meeting.contains("SemanticColor."), "Meeting detail must use semantic state colors.")
    }

    func testRemainingHomeAndPollHighlightsUseSemanticColors() throws {
        let home = try readProjectFile("iosApp/src/Views/Events/HomeView.swift")
        let blockedAction = slice(home, from: "private struct HomeNextActionCard", to: "private struct HomeDraftResumeCard")
        let pollResults = try readProjectFile("iosApp/src/Views/Polls/PollResultsView.swift")

        XCTAssertFalse(blockedAction.contains("Color.orange"), "The blocked home action must not hard-code orange.")
        XCTAssertTrue(
            blockedAction.contains("SemanticColor.warning"),
            "The blocked home action must communicate its status with SemanticColor.warning."
        )

        for rawHighlight in [".foregroundColor(.yellow)", "Color.yellow"] {
            XCTAssertFalse(pollResults.contains(rawHighlight), "Poll result highlights must replace \(rawHighlight) with semantic colors.")
        }
        XCTAssertTrue(
            pollResults.contains("SemanticColor.warning") || pollResults.contains("SemanticColor.accent"),
            "Poll result highlights must use a semantic warning or accent token."
        )
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

    private func occurrenceCount(of needle: String, in source: String) -> Int {
        source.components(separatedBy: needle).count - 1
    }
}
