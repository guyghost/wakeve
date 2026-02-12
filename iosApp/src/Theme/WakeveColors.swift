import SwiftUI

// MARK: - Wakeve Color Palette
// Design System colors for iOS with Liquid Glass aesthetic

extension Color {
    // MARK: - Primary Colors
    static let wakevPrimaryLight = Color(hex: "4A90E2")
    static let wakevPrimary = Color(hex: "2563EB")
    static let wakevPrimaryDark = Color(hex: "1E40AF")
    
    // MARK: - Accent Colors
    static let wakevAccentLight = Color(hex: "8B5CF6")
    static let wakevAccent = Color(hex: "7C3AED")
    static let wakevAccentDark = Color(hex: "6D28D9")
    
    // MARK: - Success Colors
    static let wakevSuccessLight = Color(hex: "10B981")
    static let wakevSuccess = Color(hex: "059669")
    static let wakevSuccessDark = Color(hex: "047857")
    
    // MARK: - Warning Colors
    static let wakevWarningLight = Color(hex: "F59E0B")
    static let wakevWarning = Color(hex: "D97706")
    static let wakevWarningDark = Color(hex: "B45309")
    
    // MARK: - Error Colors
    static let wakevErrorLight = Color(hex: "EF4444")
    static let wakevError = Color(hex: "DC2626")
    static let wakevErrorDark = Color(hex: "B91C1C")
    
    // MARK: - Neutral Colors (Light Mode)
    static let wakevBackgroundLight = Color(hex: "FFFFFF")
    static let wakevSurfaceLight = Color(hex: "F8FAFC")
    static let wakevBorderLight = Color(hex: "E2E8F0")
    static let wakevTextPrimaryLight = Color(hex: "0F172A")
    static let wakevTextSecondaryLight = Color(hex: "475569")
    
    // MARK: - Neutral Colors (Dark Mode)
    static let wakevBackgroundDark = Color(hex: "0F172A")
    static let wakevSurfaceDark = Color(hex: "1E293B")
    static let wakevBorderDark = Color(hex: "475569")
    static let wakevTextPrimaryDark = Color(hex: "F1F5F9")
    static let wakevTextSecondaryDark = Color(hex: "94A3B8")
    
    // MARK: - iOS System Colors (for native-style UI)
    static let iOSSystemBlue = Color(hex: "007AFF")
    static let iOSSystemGreen = Color(hex: "34C759")
    static let iOSSystemRed = Color(hex: "FF3B30")
    static let iOSSystemOrange = Color(hex: "FF9500")
    
    // MARK: - iOS Dark Mode Form Colors
    static let iOSDarkBackground = Color(hex: "000000")
    static let iOSDarkSurface = Color(hex: "1C1C1E")
    static let iOSDarkSurfaceSecondary = Color(hex: "2C2C2E")
    static let iOSDarkSeparator = Color(hex: "38383A")
    static let iOSSecondaryLabel = Color(hex: "8E8E93")
    static let iOSTertiaryLabel = Color(hex: "48484A")

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
    public static let primary = Color.wakevPrimary
    public static let onPrimary = Color.white
    public static let primaryContainer = Color.wakevPrimary.opacity(0.15)
    public static let onPrimaryContainer = Color.wakevPrimaryDark
    
    // Secondary / Accent
    public static let secondary = Color.wakevAccent
    public static let onSecondary = Color.white
    public static let secondaryContainer = Color.wakevAccent.opacity(0.15)
    public static let onSecondaryContainer = Color.wakevAccentDark
    
    // Surface
    public static let surface = Color.wakevSurfaceDark
    public static let onSurface = Color.wakevTextPrimaryDark
    public static let onSurfaceVariant = Color.wakevTextSecondaryDark
    
    // Background
    public static let background = Color.wakevBackgroundDark
    public static let onBackground = Color.wakevTextPrimaryDark
    
    // Outline
    public static let outline = Color.wakevBorderDark
    
    // Error
    public static let error = Color.wakevError
    public static let onError = Color.white
    
    // Success
    public static let success = Color.wakevSuccess
    public static let onSuccess = Color.white
    
    // Warning
    public static let warning = Color.wakevWarning
    public static let onWarning = Color.white
}

// MARK: - Gradient Presets

extension LinearGradient {
    /// Primary gradient for main buttons and accents
    static let wakevPrimaryGradient = LinearGradient(
        gradient: Gradient(colors: [.wakevPrimary, .wakevAccent]),
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
    
    /// Success gradient for positive actions
    static let wakevSuccessGradient = LinearGradient(
        gradient: Gradient(colors: [.wakevSuccess, .wakevSuccessLight]),
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
    
    /// Liquid Glass border gradient
    static let liquidGlassBorder = LinearGradient(
        gradient: Gradient(colors: [
            Color.white.opacity(0.3),
            Color.white.opacity(0.1)
        ]),
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
}
