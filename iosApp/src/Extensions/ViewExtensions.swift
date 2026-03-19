import SwiftUI

// MARK: - View Extensions

extension View {
    /// Applies Apple's recommended continuous corner radius
    func continuousCornerRadius(_ radius: CGFloat) -> some View {
        self.clipShape(RoundedRectangle(cornerRadius: radius, style: .continuous))
    }
}

// MARK: - Placeholder Extension

extension View {
    /// Overlay a placeholder view when a condition is true
    func placeholder<Content: View>(
        when shouldShow: Bool,
        alignment: Alignment = .leading,
        @ViewBuilder placeholder: () -> Content
    ) -> some View {
        ZStack(alignment: alignment) {
            placeholder().opacity(shouldShow ? 1 : 0)
            self
        }
    }
}
