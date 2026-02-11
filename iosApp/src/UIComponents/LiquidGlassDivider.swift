import SwiftUI

/// LiquidGlassDivider - Standardized divider component with subtle Liquid Glass effect
/// 
/// Usage:
/// ```swift
/// LiquidGlassDivider()
///
/// LiquidGlassDivider(style: .subtle)
/// LiquidGlassDivider(style: .prominent)
/// ```
struct LiquidGlassDivider: View {
    let style: DividerStyle
    let orientation: DividerOrientation
    
    enum DividerStyle {
        case subtle      // Very thin, low opacity
        case `default`   // Standard thickness
        case prominent   // Thicker, more visible
    }
    
    enum DividerOrientation {
        case horizontal
        case vertical
    }
    
    init(
        style: DividerStyle = .default,
        orientation: DividerOrientation = .horizontal
    ) {
        self.style = style
        self.orientation = orientation
    }
    
    var body: some View {
        Group {
            switch orientation {
            case .horizontal:
                horizontalDivider
            case .vertical:
                verticalDivider
            }
        }
    }
    
    private var horizontalDivider: some View {
        Rectangle()
            .fill(dividerGradient)
            .frame(height: dividerHeight)
    }
    
    private var verticalDivider: some View {
        Rectangle()
            .fill(dividerGradient)
            .frame(width: dividerHeight)
            .frame(maxHeight: .infinity)
    }
    
    private var dividerHeight: CGFloat {
        switch style {
        case .subtle:
            return 0.5
        case .default:
            return 1
        case .prominent:
            return 2
        }
    }
    
    private var dividerGradient: LinearGradient {
        LinearGradient(
            gradient: Gradient(colors: dividerColors),
            startPoint: .leading,
            endPoint: .trailing
        )
    }
    
    private var dividerColors: [Color] {
        switch style {
        case .subtle:
            return [
                Color.clear,
                Color.white.opacity(0.1),
                Color.white.opacity(0.15),
                Color.white.opacity(0.1),
                Color.clear
            ]
        case .default:
            return [
                Color.clear,
                Color.white.opacity(0.2),
                Color.white.opacity(0.3),
                Color.white.opacity(0.2),
                Color.clear
            ]
        case .prominent:
            return [
                Color.white.opacity(0.3),
                Color.white.opacity(0.5),
                Color.white.opacity(0.3)
            ]
        }
    }
}

/// Spacer variant with built-in divider
struct LiquidGlassSpacer: View {
    let height: CGFloat
    let includeDivider: Bool
    let dividerStyle: LiquidGlassDivider.DividerStyle
    
    init(
        height: CGFloat = 16,
        includeDivider: Bool = false,
        dividerStyle: LiquidGlassDivider.DividerStyle = .default
    ) {
        self.height = height
        self.includeDivider = includeDivider
        self.dividerStyle = dividerStyle
    }
    
    var body: some View {
        VStack(spacing: 0) {
            if includeDivider {
                LiquidGlassDivider(style: dividerStyle)
                    .padding(.bottom, height / 2)
            }
            
            Spacer()
                .frame(height: height)
            
            if includeDivider {
                LiquidGlassDivider(style: dividerStyle)
                    .padding(.top, height / 2)
            }
        }
    }
}

// MARK: - Previews
#Preview("LiquidGlassDivider - All Styles") {
    VStack(spacing: 20) {
        Text("Subtle Divider")
            .font(.caption)
            .foregroundColor(.secondary)
        LiquidGlassDivider(style: .subtle)
        
        Text("Default Divider")
            .font(.caption)
            .foregroundColor(.secondary)
        LiquidGlassDivider(style: .default)
        
        Text("Prominent Divider")
            .font(.caption)
            .foregroundColor(.secondary)
        LiquidGlassDivider(style: .prominent)
    }
    .padding()
}

#Preview("LiquidGlassDivider - Vertical") {
    HStack(spacing: 20) {
        Text("Left")
        LiquidGlassDivider(orientation: .vertical)
        Text("Right")
    }
    .padding()
}

#Preview("LiquidGlassSpacer") {
    VStack {
        Text("Content Above")
        LiquidGlassSpacer(height: 24, includeDivider: true)
        Text("Content Below")
    }
    .padding()
}
