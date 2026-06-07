import Foundation

#if canImport(FoundationModels)
import FoundationModels
#endif

protocol WakeveAIAvailabilityProviding: Sendable {
    func currentAvailability() -> WakeveAIAvailability
}

struct WakeveAIAvailabilityService: WakeveAIAvailabilityProviding {
    func currentAvailability() -> WakeveAIAvailability {
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
