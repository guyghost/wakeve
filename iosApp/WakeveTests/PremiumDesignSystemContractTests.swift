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
