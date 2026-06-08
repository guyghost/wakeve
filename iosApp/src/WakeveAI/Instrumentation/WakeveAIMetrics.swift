import Foundation

struct WakeveAIMetrics: Equatable, Sendable {
    var promptId: String
    var availability: WakeveAIAvailability
    var startedAt: Date
    var completedAt: Date?
    var timedOut: Bool
    var cancelled: Bool
    var validationIssues: [WakeveAIValidationIssue]

    var durationMilliseconds: Int? {
        guard let completedAt else { return nil }
        return Int(completedAt.timeIntervalSince(startedAt) * 1000)
    }
}

final class WakeveAIMetricsRecorder: @unchecked Sendable {
    static let shared = WakeveAIMetricsRecorder()

    private let lock = NSLock()
    private var records: [WakeveAIMetrics] = []

    func record(_ metrics: WakeveAIMetrics) {
        lock.lock()
        records.append(metrics)
        lock.unlock()
    }

    func snapshot() -> [WakeveAIMetrics] {
        lock.lock()
        defer { lock.unlock() }
        return records
    }
}

enum WakeveAILogger {
    static func debug(_ message: @autoclosure () -> String) {
        #if DEBUG
        debugLog("[WakeveAI] \(message())")
        #endif
    }

    static func debugPersonalContext(_ message: @autoclosure () -> String) {
        #if DEBUG
        if ProcessInfo.processInfo.environment["WAKEVE_AI_LOG_PERSONAL_CONTEXT"] == "1" {
            debugLog("[WakeveAI:context] \(message())")
        }
        #endif
    }
}
