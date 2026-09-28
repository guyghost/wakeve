import Foundation

/// Flags de déploiement de la refonte iOS (proposition #47).
/// Activable en debug via l'argument de lancement `-iosRedesign2026 YES`
/// (domaine d'arguments de UserDefaults).
enum FeatureFlags {
    static let redesign2026Key = "iosRedesign2026"

    static func isRedesign2026Enabled(in defaults: UserDefaults = .standard) -> Bool {
        defaults.bool(forKey: redesign2026Key)
    }
}
