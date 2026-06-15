import XCTest
@testable import Wakeve

final class PremiumInvitationContractTests: XCTestCase {
    func testInvitationShareUsesBrandPreviewCardAndNativeShare() throws {
        let source = try readProjectFile("iosApp/src/Components/InvitationShareSheet.swift")
        let body = slice(source, from: "var body: some View", to: "// MARK: - Actions")

        XCTAssertTrue(body.contains("WakeveInvitationPreviewCard("))
        XCTAssertTrue(body.contains("SemanticColor.appBackground"))
        XCTAssertTrue(body.contains("BrandColor.midnightBlue"))
        XCTAssertTrue(source.contains("UIActivityViewController("), "Invitation sharing should keep the native iOS share sheet.")
        XCTAssertFalse(body.contains("Color.wakeveAccent"), "Invitation preview should not use the old purple accent stack.")
        XCTAssertFalse(body.contains("LiquidGlassCard("), "Invitation preview content should not use Liquid Glass.")
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
