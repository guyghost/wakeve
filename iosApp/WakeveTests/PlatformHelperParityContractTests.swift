import XCTest

final class PlatformHelperParityContractTests: XCTestCase {
    func testIosMeetingProviderLaunchesSafeStoredLinksWithoutClaimingCreationSupport() throws {
        let source = try readProjectFile("shared/src/iosMain/kotlin/com/guyghost/wakeve/meeting/IosMeetingProvider.kt")

        XCTAssertTrue(source.contains("NoConfiguredMeetingProvider.createMeeting"))
        XCTAssertTrue(source.contains("UIApplication.sharedApplication.openURL(url)"))
        XCTAssertTrue(source.contains("isSafeMeetingUrl"))
        XCTAssertTrue(source.contains("\"https\""))
        XCTAssertTrue(source.contains("\"facetime\""))
        XCTAssertTrue(source.contains("\"zoommtg\""))
        XCTAssertTrue(source.contains("url.user != null || url.password != null || url.fragment != null"))
        XCTAssertFalse(source.contains("NoConfiguredMeetingProvider.launchMeeting"))
    }

    func testIosTextToSpeechUsesAVSpeechSynthesizer() throws {
        let source = try readProjectFile("shared/src/iosMain/kotlin/com/guyghost/wakeve/services/IosTextToSpeechService.kt")

        XCTAssertTrue(source.contains("AVSpeechSynthesizer"))
        XCTAssertTrue(source.contains("AVSpeechUtterance.speechUtteranceWithString"))
        XCTAssertTrue(source.contains("AVSpeechSynthesisVoice.voiceWithLanguage"))
        XCTAssertTrue(source.contains("QueueMode.FLUSH") && source.contains("QueueMode.INTERRUPT"))
        XCTAssertTrue(source.contains("Language.EN -> \"en-US\""))
        XCTAssertTrue(source.contains("Language.FR -> \"fr-FR\""))
        XCTAssertFalse(source.contains("NoConfiguredTextToSpeechService"))
    }

    func testIosBadgeNotificationServiceUsesUNUserNotificationCenter() throws {
        let source = try readProjectFile("shared/src/iosMain/kotlin/com/guyghost/wakeve/gamification/IosBadgeNotificationService.kt")

        XCTAssertTrue(source.contains("UNUserNotificationCenter.currentNotificationCenter()"))
        XCTAssertTrue(source.contains("requestAuthorizationWithOptions"))
        XCTAssertTrue(source.contains("UNAuthorizationOptionAlert"))
        XCTAssertTrue(source.contains("UNAuthorizationOptionBadge"))
        XCTAssertTrue(source.contains("UNNotificationRequest.requestWithIdentifier"))
        XCTAssertTrue(source.contains("UIApplication.sharedApplication.applicationIconBadgeNumber"))
        XCTAssertFalse(source.contains("NoConfiguredBadgeNotificationService"))
    }

    private func readProjectFile(_ relativePath: String) throws -> String {
        let fileURL = URL(fileURLWithPath: #filePath)
        let testsDir = fileURL.deletingLastPathComponent()
        let iosAppDir = testsDir.deletingLastPathComponent()
        let projectRoot = iosAppDir.deletingLastPathComponent()
        let targetURL = projectRoot.appendingPathComponent(relativePath)
        return try String(contentsOf: targetURL, encoding: .utf8)
    }
}
