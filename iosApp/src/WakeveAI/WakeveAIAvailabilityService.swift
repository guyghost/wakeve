import Foundation

#if canImport(FoundationModels)
import FoundationModels
#endif

protocol WakeveAIAvailabilityProviding: Sendable {
    func currentAvailability() -> WakeveAIAvailability
}

struct WakeveAIAvailabilityService: WakeveAIAvailabilityProviding {
    func currentAvailability() -> WakeveAIAvailability {
        #if DEBUG
        if let override = Self.debugAvailabilityOverride() {
            WakeveAILogger.debug("Using DEBUG WakeveAI availability override: \(override)")
            return override
        }
        #endif

        #if canImport(FoundationModels)
        if #available(iOS 26.0, *) {
            switch SystemLanguageModel.default.availability {
            case .available:
                return .available
            case .unavailable(let reason):
                return Self.mapUnavailableReason(String(describing: reason))
            @unknown default:
                return .unknownUnavailable("unknown")
            }
        }
        #endif

        return .unsupportedDevice
    }

    #if DEBUG
    static func debugAvailabilityOverride(
        arguments: [String] = ProcessInfo.processInfo.arguments,
        environment: [String: String] = ProcessInfo.processInfo.environment
    ) -> WakeveAIAvailability? {
        let rawValue = environment["WAKEVE_AI_AVAILABILITY_OVERRIDE"]
            ?? arguments.first { $0.hasPrefix("--wakeve-ai-availability=") }?.split(separator: "=", maxSplits: 1).last.map(String.init)

        guard let rawValue else { return nil }
        return availabilityOverrideValue(rawValue)
    }

    private static func availabilityOverrideValue(_ rawValue: String) -> WakeveAIAvailability? {
        switch rawValue.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "available":
            return .available
        case "disabled", "appleintelligencedisabled", "apple_intelligence_disabled":
            return .appleIntelligenceDisabled
        case "notready", "not_ready":
            return .notReady
        case "unsupported", "unsupporteddevice", "unsupported_device":
            return .unsupportedDevice
        case "unknown", "unknownunavailable", "unknown_unavailable":
            return .unknownUnavailable("debug_override")
        default:
            return nil
        }
    }
    #endif

    static func mapUnavailableReason(_ reason: String) -> WakeveAIAvailability {
        let normalized = reason.lowercased()
        if normalized.contains("notenabled") || normalized.contains("not enabled") || normalized.contains("disabled") {
            return .appleIntelligenceDisabled
        }
        if normalized.contains("notsupported") || normalized.contains("unsupported") {
            return .unsupportedDevice
        }
        if normalized.contains("notready") || normalized.contains("assets") || normalized.contains("download") {
            return .notReady
        }
        return .unknownUnavailable(reason)
    }
}

struct FixedWakeveAIAvailabilityService: WakeveAIAvailabilityProviding {
    var availability: WakeveAIAvailability

    func currentAvailability() -> WakeveAIAvailability {
        availability
    }
}
