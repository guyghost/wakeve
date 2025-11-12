import SwiftUI

/// Système de design Liquid Glass conforme aux directives Apple
/// Basé sur les principes d'Apple pour iOS 17+
struct LiquidGlassDesign {
    
    // MARK: - Colors (Material Design)
    
    /// Couleur de fond primaire avec support du mode sombre
    static var backgroundColor: Color {
        Color(UIColor { traitCollection in
            traitCollection.userInterfaceStyle == .dark
                ? UIColor(red: 0.11, green: 0.11, blue: 0.12, alpha: 1.0)  // #1C1C1E
                : UIColor(red: 0.98, green: 0.98, blue: 1.0, alpha: 1.0)   // #FAFAFE
        })
    }
    
    /// Couleur secondaire (surfaces)
    static var surfaceColor: Color {
        Color(UIColor { traitCollection in
            traitCollection.userInterfaceStyle == .dark
                ? UIColor(red: 0.17, green: 0.17, blue: 0.18, alpha: 1.0)  // #2C2C2E
                : UIColor(red: 1.0, green: 1.0, blue: 1.0, alpha: 1.0)     // #FFFFFF
        })
    }
    
    /// Couleur du verre avec opacité et blur
    static var glassColor: Color {
        Color(UIColor { traitCollection in
            traitCollection.userInterfaceStyle == .dark
                ? UIColor(red: 0.25, green: 0.25, blue: 0.27, alpha: 0.4)  // Dark glass
                : UIColor(red: 1.0, green: 1.0, blue: 1.0, alpha: 0.6)     // Light glass
        })
    }
    
    /// Couleur tertiaire (éléments secondaires)
    static var tertiaryColor: Color {
        Color(UIColor { traitCollection in
            traitCollection.userInterfaceStyle == .dark
                ? UIColor(red: 0.23, green: 0.23, blue: 0.24, alpha: 1.0)  // #3A3A3C
                : UIColor(red: 0.95, green: 0.95, blue: 0.97, alpha: 1.0)  // #F2F2F7
        })
    }
    
    // MARK: - Accents
    
    /// Bleu primaire Apple (accent)
    static let accentBlue = Color(red: 0.0, green: 0.48, blue: 1.0)  // #007AFF
    
    /// Vert succès
    static let successGreen = Color(red: 0.21, green: 0.84, blue: 0.39)  // #34C759
    
    /// Orange avertissement
    static let warningOrange = Color(red: 1.0, green: 0.59, blue: 0.0)  // #FF9500
    
    /// Rouge erreur
    static let errorRed = Color(red: 1.0, green: 0.23, blue: 0.19)  // #FF3B30
    
    // MARK: - Typography
    
    /// Titre XL (32pt, bold)
    static let titleXL = Font.system(size: 32, weight: .bold, design: .default)
    
    /// Titre L (28pt, bold)
    static let titleL = Font.system(size: 28, weight: .bold, design: .default)
    
    /// Titre M (22pt, semibold)
    static let titleM = Font.system(size: 22, weight: .semibold, design: .default)
    
    /// Titre S (17pt, semibold)
    static let titleS = Font.system(size: 17, weight: .semibold, design: .default)
    
    /// Body régulier
    static let bodyRegular = Font.system(size: 17, weight: .regular, design: .default)
    
    /// Body petit
    static let bodySmall = Font.system(size: 13, weight: .regular, design: .default)
    
    /// Caption
    static let caption = Font.system(size: 12, weight: .regular, design: .default)
    
    // MARK: - Spacing
    
    /// Espacement extra petit (4pt)
    static let spacingXS: CGFloat = 4
    
    /// Espacement petit (8pt)
    static let spacingS: CGFloat = 8
    
    /// Espacement moyen (12pt)
    static let spacingM: CGFloat = 12
    
    /// Espacement normal (16pt)
    static let spacingL: CGFloat = 16
    
    /// Espacement grand (24pt)
    static let spacingXL: CGFloat = 24
    
    /// Espacement extra grand (32pt)
    static let spacingXXL: CGFloat = 32
    
    // MARK: - Border Radius
    
    /// Radius petit (6pt)
    static let radiusS: CGFloat = 6
    
    /// Radius moyen (12pt)
    static let radiusM: CGFloat = 12
    
    /// Radius grand (18pt)
    static let radiusL: CGFloat = 18
    
    /// Radius extra grand (28pt)
    static let radiusXL: CGFloat = 28
    
    // MARK: - Shadow
    
    /// Ombre légère pour surfaces
    static let shadowLight = Shadow(
        color: Color.black.opacity(0.08),
        radius: 8,
        x: 0,
        y: 2
    )
    
    /// Ombre moyenne pour éléments flottants
    static let shadowMedium = Shadow(
        color: Color.black.opacity(0.12),
        radius: 12,
        x: 0,
        y: 4
    )
    
    /// Ombre large pour modales
    static let shadowLarge = Shadow(
        color: Color.black.opacity(0.16),
        radius: 24,
        x: 0,
        y: 8
    )
}

// MARK: - Blur View (Glass Morphism)

/// Vue avec effet de verre (frosted glass effect)
struct GlassView<Content: View>: View {
    let content: Content
    var cornerRadius: CGFloat = LiquidGlassDesign.radiusL
    var opacity: Double = 0.6
    
    init(cornerRadius: CGFloat = LiquidGlassDesign.radiusL, opacity: Double = 0.6, @ViewBuilder content: () -> Content) {
        self.cornerRadius = cornerRadius
        self.opacity = opacity
        self.content = content()
    }
    
    var body: some View {
        content
            .background(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(LiquidGlassDesign.glassColor)
                    .blur(radius: 20)
            )
            .background(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .stroke(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.white.opacity(0.3),
                                Color.white.opacity(0.1)
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1.5
                    )
            )
            .cornerRadius(cornerRadius)
            .shadow(color: Color.black.opacity(0.1), radius: 12, x: 0, y: 4)
    }
}

// MARK: - Button Styles

/// Style de bouton primaire avec Liquid Glass
struct LiquidGlassButtonStyle: ButtonStyle {
    var isEnabled: Bool = true
    
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(LiquidGlassDesign.bodyRegular)
            .foregroundColor(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .background(
                RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusL)
                    .fill(LiquidGlassDesign.accentBlue)
            )
            .opacity(isEnabled ? (configuration.isPressed ? 0.8 : 1.0) : 0.5)
            .scaleEffect(configuration.isPressed ? 0.98 : 1.0)
    }
}

/// Style de bouton secondaire avec Liquid Glass
struct LiquidGlassSecondaryButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .font(LiquidGlassDesign.bodyRegular)
            .foregroundColor(LiquidGlassDesign.accentBlue)
            .frame(maxWidth: .infinity)
            .frame(height: 48)
            .background(
                RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusL)
                    .fill(LiquidGlassDesign.glassColor)
            )
            .overlay(
                RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusL)
                    .stroke(LiquidGlassDesign.accentBlue.opacity(0.3), lineWidth: 1)
            )
            .scaleEffect(configuration.isPressed ? 0.98 : 1.0)
    }
}

// MARK: - Card Styles

/// Card avec effet Liquid Glass
struct LiquidGlassCard<Content: View>: View {
    let content: Content
    var cornerRadius: CGFloat = LiquidGlassDesign.radiusL
    var padding: CGFloat = LiquidGlassDesign.spacingL
    
    init(cornerRadius: CGFloat = LiquidGlassDesign.radiusL, padding: CGFloat = LiquidGlassDesign.spacingL, @ViewBuilder content: () -> Content) {
        self.cornerRadius = cornerRadius
        self.padding = padding
        self.content = content()
    }
    
    var body: some View {
        GlassView(cornerRadius: cornerRadius) {
            content
                .padding(padding)
        }
    }
}

// MARK: - Text Field Style

/// TextField avec design Liquid Glass
struct LiquidGlassTextFieldStyle: TextFieldStyle {
    func _body(configuration: TextField<Self.Body>) -> some View {
        configuration
            .padding(LiquidGlassDesign.spacingM)
            .background(
                RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusM)
                    .fill(LiquidGlassDesign.glassColor)
            )
            .overlay(
                RoundedRectangle(cornerRadius: LiquidGlassDesign.radiusM)
                    .stroke(Color.white.opacity(0.2), lineWidth: 1)
            )
            .font(LiquidGlassDesign.bodyRegular)
    }
}

// MARK: - Extension Helper

extension View {
    /// Applique le style Liquid Glass Card
    func liquidGlassCard(cornerRadius: CGFloat = LiquidGlassDesign.radiusL) -> some View {
        self
            .padding(LiquidGlassDesign.spacingL)
            .background(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(LiquidGlassDesign.glassColor)
            )
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .stroke(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.white.opacity(0.3),
                                Color.white.opacity(0.1)
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1.5
                    )
            )
            .cornerRadius(cornerRadius)
            .shadow(color: Color.black.opacity(0.1), radius: 12, x: 0, y: 4)
    }
}
