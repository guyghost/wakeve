import SwiftUI

// MARK: - Wakeve Color Palette
// Design System colors for iOS with Liquid Glass aesthetic

extension Color {
    // MARK: - Primary Colors
    static let wakevePrimaryLight = Color(hex: "4A90E2")
    static let wakevePrimary = Color(hex: "2563EB")
    static let wakevePrimaryDark = Color(hex: "1E40AF")

    // MARK: - Accent Colors
    static let wakeveAccentLight = Color(hex: "8B5CF6")
    static let wakeveAccent = Color(hex: "7C3AED")
    static let wakeveAccentDark = Color(hex: "6D28D9")
    
    // MARK: - Success Colors
    static let wakeveSuccessLight = Color(hex: "10B981")
    static let wakeveSuccess = Color(hex: "059669")
    static let wakeveSuccessDark = Color(hex: "047857")

    // MARK: - Warning Colors
    static let wakeveWarningLight = Color(hex: "F59E0B")
    static let wakeveWarning = Color(hex: "D97706")
    static let wakeveWarningDark = Color(hex: "B45309")

    // MARK: - Error Colors
    static let wakeveErrorLight = Color(hex: "EF4444")
    static let wakeveError = Color(hex: "DC2626")
    static let wakeveErrorDark = Color(hex: "B91C1C")
    
    // MARK: - Neutral Colors (Light Mode)
    static let wakeveBackgroundLight = Color(hex: "FFFFFF")
    static let wakeveSurfaceLight = Color(hex: "F8FAFC")
    static let wakeveBorderLight = Color(hex: "E2E8F0")
    static let wakeveTextPrimaryLight = Color(hex: "0F172A")
    static let wakeveTextSecondaryLight = Color(hex: "475569")

    // MARK: - Neutral Colors (Dark Mode)
    static let wakeveBackgroundDark = Color(hex: "0F172A")
    static let wakeveSurfaceDark = Color(hex: "1E293B")
    static let wakeveBorderDark = Color(hex: "475569")
    static let wakeveTextPrimaryDark = Color(hex: "F1F5F9")
    static let wakeveTextSecondaryDark = Color(hex: "94A3B8")
    
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
    
    // MARK: - App Surface Colors
    // Light Mode
    static let appBackgroundLight = Color(hex: "FFFFFF")
    static let appSurfaceLight = Color(hex: "F6F8FA")
    static let appBorderLight = Color(hex: "D0D7DE")
    static let appTextPrimaryLight = Color(hex: "24292F")
    static let appTextSecondaryLight = Color(hex: "57606A")
    static let appTabBarLight = Color(hex: "F5F5F5")
    
    // Dark Mode
    static let appBackgroundDark = Color(hex: "0D1117")
    static let appSurfaceDark = Color(hex: "161B22")
    static let appBorderDark = Color(hex: "30363D")
    static let appTextPrimaryDark = Color(hex: "C9D1D9")
    static let appTextSecondaryDark = Color(hex: "8B949E")
    static let appTabBarDark = Color(hex: "161B22")
    
    // Accent Colors
    static let appAccent = Color(hex: "0969DA")
    static let appAccentLight = Color(hex: "58A6FF")

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
    public static let secondaryContainer = Color.wakeveAccent.opacity(0.15)
    public static let onSecondaryContainer = Color.wakeveAccentDark

    // Surface
    public static let surface = Color.wakeveSurfaceDark
    public static let onSurface = Color.wakeveTextPrimaryDark
    public static let onSurfaceVariant = Color.wakeveTextSecondaryDark

    // Background
    public static let background = Color.wakeveBackgroundDark
    public static let onBackground = Color.wakeveTextPrimaryDark

    // Outline
    public static let outline = Color.wakeveBorderDark

    // Error
    public static let error = Color.wakeveError
    public static let onError = Color.white

    // Success
    public static let success = Color.wakeveSuccess
    public static let onSuccess = Color.white

    // Warning
    public static let warning = Color.wakeveWarning
    public static let onWarning = Color.white
}

// MARK: - Gradient Presets

extension LinearGradient {
    /// Primary gradient for main buttons and accents
    static let wakevePrimaryGradient = LinearGradient(
        gradient: Gradient(colors: [.wakevePrimary, .wakeveAccent]),
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
    
    /// Success gradient for positive actions
    static let wakeveSuccessGradient = LinearGradient(
        gradient: Gradient(colors: [.wakeveSuccess, .wakeveSuccessLight]),
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
    
    /// Liquid Glass border gradient
    static let wakeveLiquidGlassBorder = LinearGradient(
        gradient: Gradient(colors: [
            Color.white.opacity(0.3),
            Color.white.opacity(0.1)
        ]),
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )
}
