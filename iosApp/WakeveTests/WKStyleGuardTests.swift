import XCTest

/// Garde-fou de la refonte (#47) : aucun style en dur dans les composants WK,
/// et les compteurs de Views/ ne peuvent que baisser (cliquet). À la couche 9, baseline = 0.
final class WKStyleGuardTests: XCTestCase {

    private struct Rule {
        let name: String
        let regex: NSRegularExpression
    }

    private let rules: [Rule] = [
        Rule(name: "Color(hex:", regex: try! NSRegularExpression(pattern: #"Color\(hex:"#)),
        Rule(name: ".font(.system(size:", regex: try! NSRegularExpression(pattern: #"\.font\(\.system\(size:"#)),
        Rule(name: "cornerRadius littéral", regex: try! NSRegularExpression(pattern: #"cornerRadius: ?[0-9]"#)),
        Rule(name: ".cornerRadius(", regex: try! NSRegularExpression(pattern: #"\.cornerRadius\("#)),
        Rule(name: "Color(red:", regex: try! NSRegularExpression(pattern: #"Color\(red:"#))
    ]

    /// Règles propres aux composants WK (les couleurs brutes vivent dans WK.swift).
    private let wkOnlyRules: [Rule] = [
        Rule(name: "littéral hexadécimal 0xRRGGBB", regex: try! NSRegularExpression(pattern: #"0x[0-9A-Fa-f]{6}"#))
    ]

    /// Baseline mesurée après la couche 0 (2026-09-28), complétée à la revue finale de la couche 1.
    /// Baisser ces valeurs quand un écran est migré.
    private let viewsBaseline: [String: Int] = [
        "Color(hex:": 65,
        ".font(.system(size:": 87,
        "cornerRadius littéral": 68,
        ".cornerRadius(": 13,
        "Color(red:": 6
    ]

    private var srcRoot: URL {
        URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()   // WakeveTests
            .deletingLastPathComponent()   // iosApp
            .appendingPathComponent("src")
    }

    private func count(_ rule: Rule, under folder: String) throws -> Int {
        let root = srcRoot.appendingPathComponent(folder)
        guard let enumerator = FileManager.default.enumerator(at: root, includingPropertiesForKeys: nil) else {
            XCTFail("Dossier introuvable : \(root.path)")
            return 0
        }
        var total = 0
        for case let url as URL in enumerator where url.pathExtension == "swift" {
            let text = try String(contentsOf: url, encoding: .utf8)
            total += rule.regex.numberOfMatches(in: text, range: NSRange(text.startIndex..., in: text))
        }
        return total
    }

    func testWKComponentsHaveNoHardCodedStyles() throws {
        for rule in rules + wkOnlyRules {
            XCTAssertEqual(try count(rule, under: "Components/WK"), 0, "Style en dur interdit dans Components/WK : \(rule.name)")
        }
    }

    func testViewsNeverGainHardCodedStyles() throws {
        for rule in rules {
            let current = try count(rule, under: "Views")
            let baseline = viewsBaseline[rule.name] ?? 0
            XCTAssertLessThanOrEqual(current, baseline, "Views/ : \(rule.name) passe de \(baseline) à \(current). Utiliser les tokens WK.")
            if current < baseline {
                print("ℹ️ WKStyleGuard : \(rule.name) est descendu à \(current) — abaisser la baseline à \(current).")
            }
        }
    }
}
