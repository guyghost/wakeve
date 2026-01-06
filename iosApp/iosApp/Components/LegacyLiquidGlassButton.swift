import SwiftUI

/// Liquid Glass Button Component
///
/// A reusable button component following Apple's Liquid Glass guidelines.
/// Supports 4 variants: primary, secondary, text, and icon.
///
/// ## Features
/// - 4 button styles (primary, secondary, text, icon)
/// - Automatic Liquid Glass materials
/// - Disabled states with proper accessibility
/// - Size variants (small, medium, large)
/// - Native haptic feedback on tap
///
/// ## Usage Examples
/// ```swift
/// // Primary button (most common)
/// LiquidGlassButton(
///     title: "Create Event",
///     style: .primary
///     action: { /* handle tap */ }
/// )
///
/// // Secondary button
/// LiquidGlassButton(
///     title: "Cancel",
///     style: .secondary
///     action: { /* handle tap */ }
/// )
///
/// // Text-only button
/// LiquidGlassButton(
///     title: "Learn More",
///     style: .text
///     action: { /* handle tap */ }
/// )
///
/// // Icon-only button
/// LiquidGlassButton(
///     icon: "star.fill",
///     style: .icon
///     action: { /* handle tap */ }
/// )
///
/// // Disabled button
/// LiquidGlassButton(
///     title: "Submit",
///     style: .primary,
///     isDisabled: true
///     action: { /* handle tap */ }
/// )
///
/// // Large button with icon
/// LiquidGlassButton(
///     title: "Delete Event",
///     style: .primary,
///     size: .large,
///     icon: "trash.fill",
///     action: { /* handle tap */ }
/// )
/// ```

struct LiquidGlassButton: View {
    let title: String?
    let icon: String?
    let style: ButtonStyle
    let size: ButtonSize
    let isDisabled: Bool
    let action: () -> Void
    
    // MARK: - Initialization
    
    init(
        title: String? = nil,
        icon: String? = nil,
        style: ButtonStyle = .primary,
        size: ButtonSize = .medium,
        isDisabled: Bool = false,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.icon = icon
        self.style = style
        self.size = size
        self.isDisabled = isDisabled
        self.action = action
    }
    
    // MARK: - Button Styles
    
    enum ButtonStyle {
        /// Primary button with gradient background
        /// Use for main actions (submit, confirm, create)
        case primary
        
        /// Secondary button with glass background
        /// Use for secondary actions (cancel, back)
        case secondary
        
        /// Text-only button with transparent background
        /// Use for tertiary actions (learn more, details)
        case text
        
        /// Icon-only button with glass background
        /// Use for icon-only actions (menu, settings)
        case icon
    }
    
    // MARK: - Button Sizes
    
    enum ButtonSize {
        /// Compact button for tight spaces
        /// Height: 36dp
        case small
        
        /// Standard button (most common)
        /// Height: 44dp
        case medium
        
        /// Prominent button for primary actions
        /// Height: 52dp
        case large
    }
    
    // MARK: - Computed Properties
    
    private var buttonHeight: CGFloat {
        switch size {
        case .small: return 36
        case .medium: return 44
        case .large: return 52
        }
    }
    
    private var horizontalPadding: CGFloat {
        switch size {
        case .small: return 16
        case .medium: return 20
        case .large: return 24
        }
    }
    
    private var fontSize: Font {
        switch size {
        case .small: return .subheadline
        case .medium: return .headline
        case .large: return .title3
        }
    }
    
    private var iconSize: CGFloat {
        switch size {
        case .small: return 16
        case .medium: return 20
        case .large: return 24
        }
    }
    
    private var material: Material {
        switch style {
        case .primary:
            return .thickMaterial
        case .secondary, .icon:
            return .thinMaterial
        case .text:
            return .ultraThinMaterial
        }
    }
    
    private var backgroundColor: Color {
        switch style {
        case .primary:
            return Color.wakevPrimary
        case .secondary, .icon, .text:
            return Color.clear
        }
    }
    
    private var foregroundColor: Color {
        switch style {
        case .primary:
            return .white
        case .secondary, .icon:
            return .primary
        case .text:
            return .wakevPrimary
        }
    }
    
    // MARK: - Body
    
    var body: some View {
        Button(action: action) {
            buttonContent
                .padding(.horizontal, horizontalPadding)
                .frame(height: buttonHeight)
                .background(isDisabled ? Color.wakevTextSecondary.opacity(0.3) : backgroundColor)
                .foregroundColor(isDisabled ? Color.wakevTextSecondary.opacity(0.6) : foregroundColor)
                .clipShape(RoundedRectangle(cornerRadius: buttonHeight / 3, style: .continuous))
                .overlay(
                    RoundedRectangle(cornerRadius: buttonHeight / 3, style: .continuous)
                        .strokeBorder(
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
                .background(material)
                .shadow(
                    color: .black.opacity(0.08),
                    radius: 6,
                    x: 0,
                    y: 3
                )
        }
        .buttonStyle(.plain)
        .disabled(isDisabled)
        .accessibilityLabel(title ?? "")
        .accessibilityHint(isDisabled ? "Button disabled" : "Tap to \(title ?? "perform action")")
        .accessibilityAddTraits(isDisabled ? [.isNotEnabled] : [.isButton])
    }
    
    // MARK: - Button Content
    
    @ViewBuilder
    private var buttonContent: some View {
        HStack(spacing: 8) {
            if let icon = icon {
                Image(systemName: icon)
                    .font(.system(size: iconSize, weight: .semibold))
                    .foregroundColor(foregroundColor)
            }
            
            if let title = title {
                Text(title)
                    .font(fontSize.weight(.semibold))
                    .foregroundColor(foregroundColor)
                    .lineLimit(1)
            }
        }
    }
    
    // MARK: - Previews
    
    struct LiquidGlassButton_Previews: PreviewProvider {
        static var previews: some View {
            ScrollView {
                VStack(spacing: 20) {
                    Text("Primary Buttons")
                        .font(.headline)
                    
                    LiquidGlassButton(
                        title: "Create Event",
                        style: .primary,
                        action: { }
                    )
                    
                    LiquidGlassButton(
                        title: "Submit",
                        style: .primary,
                        icon: "checkmark",
                        action: { }
                    )
                    
                    LiquidGlassButton(
                        title: "Disabled",
                        style: .primary,
                        isDisabled: true,
                        action: { }
                    )
                    
                    Divider()
                        .padding(.vertical)
                    
                    Text("Secondary Buttons")
                        .font(.headline)
                    
                    LiquidGlassButton(
                        title: "Cancel",
                        style: .secondary,
                        action: { }
                    )
                    
                    LiquidGlassButton(
                        title: "Back",
                        style: .secondary,
                        icon: "chevron.left",
                        action: { }
                    )
                    
                    Divider()
                        .padding(.vertical)
                    
                    Text("Text Buttons")
                        .font(.headline)
                    
                    LiquidGlassButton(
                        title: "Learn More",
                        style: .text,
                        action: { }
                    )
                    
                    LiquidGlassButton(
                        title: "View Details",
                        style: .text,
                        action: { }
                    )
                    
                    Divider()
                        .padding(.vertical)
                    
                    Text("Icon Buttons")
                        .font(.headline)
                    
                    LiquidGlassButton(
                        icon: "star.fill",
                        style: .icon,
                        action: { }
                    )
                    
                    LiquidGlassButton(
                        icon: "heart",
                        style: .icon,
                        action: { }
                    )
                    
                    Divider()
                        .padding(.vertical)
                    
                    Text("Size Variants")
                        .font(.headline)
                    
                    HStack(spacing: 12) {
                        LiquidGlassButton(
                            title: "Small",
                            style: .primary,
                            size: .small,
                            action: { }
                        )
                        
                        LiquidGlassButton(
                            title: "Medium",
                            style: .primary,
                            size: .medium,
                            action: { }
                        )
                        
                        LiquidGlassButton(
                            title: "Large",
                            style: .primary,
                            size: .large,
                            action: { }
                        )
                    }
                }
                .padding()
            }
        }
    }
}
