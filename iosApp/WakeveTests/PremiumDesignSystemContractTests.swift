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
