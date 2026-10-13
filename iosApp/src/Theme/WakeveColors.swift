import SwiftUI

// MARK: - Wakeve Color Palette
// Design System colors for iOS with Liquid Glass aesthetic

extension Color {
    // MARK: - Primary Colors
    static let wakevePrimary = Color(hex: "2563EB")
    static let wakevePrimaryDark = Color(hex: "1E40AF")

    // MARK: - Accent Colors
    static let wakeveAccent = Color(hex: "7C3AED")
    
    // MARK: - Success Colors
    static let wakeveSuccess = Color(hex: "059669")

    // MARK: - Warning Colors
    static let wakeveWarning = Color(hex: "D97706")

    // MARK: - Error Colors
    static let wakeveError = Color(hex: "DC2626")
    
    // MARK: - Neutral Colors (Light Mode)
    /// Fond ivoire chaud standard de l'app — proposition Swarm DAO #27.
    /// Valeur mesurée au pixel sur l'écran Résultats
    /// (qa-screenshots/cycle-2026-09-04/28-poll-results.png).
    static let wakeveWarmIvory = Color(hex: "F6F1EA")

    // MARK: - Neutral Colors (Dark Mode)
    
    // MARK: - iOS System Colors (for native-style UI)
    
    // MARK: - iOS Dark Mode Form Colors
    
    // MARK: - App Surface Colors
    // Light Mode
    
    // Dark Mode
    
    // Accent Colors

    // MARK: - Hex Initializer
    init(hex: String) {
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)
        
        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0
        
        self.init(red: r, green: g, blue: b)
    }
}

// MARK: - WakeveColors Struct
/// Material You inspired color container for the Wakeve app
public struct WakeveColors {
    // Primary
    public static let primary = Color.wakevePrimary
    public static let onPrimary = Color.white
    public static let primaryContainer = Color.wakevePrimary.opacity(0.15)
    public static let onPrimaryContainer = Color.wakevePrimaryDark

    // Secondary / Accent
    public static let secondary = Color.wakeveAccent
    public static let onSecondary = Color.white

    // Surface
    public static let surface = Color(uiColor: .secondarySystemBackground)
    public static let onSurface = Color(uiColor: .label)
    public static let onSurfaceVariant = Color(uiColor: .secondaryLabel)

    // Background
    public static let background = Color(uiColor: .systemBackground)

    // Outline
    public static let outline = Color(uiColor: .separator)

    // Error
    public static let error = Color.wakeveError

    // Success
    public static let success = Color.wakeveSuccess

    // Warning
    public static let warning = Color.wakeveWarning
}

