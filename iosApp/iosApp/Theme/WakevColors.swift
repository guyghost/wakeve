import SwiftUI

extension Color {
    // Primary
    static let wakevPrimaryLight = Color(hex: "4A90E2")
    static let wakevPrimary = Color(hex: "2563EB")
    static let wakevPrimaryDark = Color(hex: "1E40AF")
    
    // Accent
    static let wakevAccentLight = Color(hex: "8B5CF6")
    static let wakevAccent = Color(hex: "7C3AED")
    static let wakevAccentDark = Color(hex: "6D28D9")
    
    // Success
    static let wakevSuccessLight = Color(hex: "10B981")
    static let wakevSuccess = Color(hex: "059669")
    static let wakevSuccessDark = Color(hex: "047857")
    
    // Warning
    static let wakevWarningLight = Color(hex: "F59E0B")
    static let wakevWarning = Color(hex: "D97706")
    static let wakevWarningDark = Color(hex: "B45309")
    
    // Error
    static let wakevErrorLight = Color(hex: "EF4444")
    static let wakevError = Color(hex: "DC2626")
    static let wakevErrorDark = Color(hex: "B91C1C")
    
    // Neutres
    static let wakevBackgroundLight = Color(hex: "FFFFFF")
    static let wakevBackgroundDark = Color(hex: "0F172A")
    static let wakevSurfaceLight = Color(hex: "F8FAFC")
    static let wakevSurfaceDark = Color(hex: "1E293B")
    static let wakevBorderLight = Color(hex: "E2E8F0")
    static let wakevBorderDark = Color(hex: "475569")
    static let wakevTextPrimaryLight = Color(hex: "0F172A")
    static let wakevTextPrimaryDark = Color(hex: "F1F5F9")
    static let wakevTextSecondaryLight = Color(hex: "475569")
    static let wakevTextSecondaryDark = Color(hex: "94A3B8")
}

extension Color {
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