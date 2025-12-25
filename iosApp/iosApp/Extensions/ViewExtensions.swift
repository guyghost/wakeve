import SwiftUI

// MARK: - Liquid Glass Extensions

extension View {
    /// Applies a glass card style following Apple's Liquid Glass guidelines
    /// - Parameters:
    ///   - cornerRadius: The corner radius (default: 20)
    ///   - material: The material to use (default: .regularMaterial)
    func glassCard(
        cornerRadius: CGFloat = 20,
        material: Material = .regularMaterial
    ) -> some View {
        self
            .background(material)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .shadow(color: Color.black.opacity(0.05), radius: 8, x: 0, y: 4)
    }

    /// Applies a thin glass style for subtle backgrounds
    func thinGlass(cornerRadius: CGFloat = 16) -> some View {
        self
            .background(.thinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }

    /// Applies an ultra thin glass style for very subtle backgrounds
    func ultraThinGlass(cornerRadius: CGFloat = 16) -> some View {
        self
            .background(.ultraThinMaterial)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
    }

    /// Applies a thick glass style for prominent cards
    func thickGlass(cornerRadius: CGFloat = 24) -> some View {
        self
            .background(.thickMaterial)
            .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
            .shadow(color: Color.black.opacity(0.08), radius: 12, x: 0, y: 6)
    }

    /// Applies Apple's recommended continuous corner radius
    func continuousCornerRadius(_ radius: CGFloat) -> some View {
        self.clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
    }
}

// MARK: - Legacy Support

extension View {
    /// Legacy backdrop method - kept for compatibility
    /// Prefer using glassCard() or specific glass methods instead
    @available(*, deprecated, message: "Use glassCard() instead for better Liquid Glass compliance")
    func backdrop(radius: CGFloat, opaque: Bool = true) -> some View {
        self.background(
            .ultraThinMaterial,
            in: RoundedRectangle(cornerRadius: radius, style: .continuous)
        )
    }
}

// MARK: - Selective Corner Radius (for detail views)

struct RoundedCorner: Shape {
    var radius: CGFloat = .infinity
    var corners: UIRectCorner = .allCorners

    func path(in rect: CGRect) -> Path {
        let path = UIBezierPath(
            roundedRect: rect,
            byRoundingCorners: corners,
            cornerRadii: CGSize(width: radius, height: radius)
        )
        return Path(path.cgPath)
    }
}

extension View {
    func cornerRadius(_ radius: CGFloat, corners: UIRectCorner) -> some View {
        clipShape(RoundedCorner(radius: radius, corners: corners))
    }
}

// MARK: - Placeholder Extension

extension View {
    func placeholder<Content: View>(
        when shouldShow: Bool,
        alignment: Alignment = .leading,
        @ViewBuilder placeholder: () -> Content) -> some View {

        ZStack(alignment: alignment) {
            placeholder().opacity(shouldShow ? 1 : 0)
            self
        }
    }
}
