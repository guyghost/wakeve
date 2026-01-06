import SwiftUI

/// Liquid Glass Divider Component
///
/// A reusable divider component following Apple's Liquid Glass guidelines.
/// Supports horizontal and vertical orientations with 3 line styles.
///
/// ## Features
/// - 3 divider styles (thin, medium, thick)
/// - Automatic glass gradient background
/// - Optional custom color support
/// - Native iOS materials integration
/// - Horizontal and vertical orientation
///
/// ## Usage Examples
/// ```swift
/// // Thin divider (most common)
/// LiquidGlassDivider()
///
/// // Medium divider
/// LiquidGlassDivider(style: .medium)
///
/// // Thick divider
/// LiquidGlassDivider(style: .thick)
///
/// // Vertical divider
/// LiquidGlassDivider(orientation: .vertical)
///
/// // Divider with custom color
/// LiquidGlassDivider(color: .blue)
///
/// // Divider with custom opacity
/// LiquidGlassDivider(opacity: 0.3)
/// ```
///

struct LiquidGlassDivider: View {
    var style: DividerStyle = .medium
    var opacity: Double = 0.15
    var color: Color? = nil
    var orientation: DividerOrientation = .horizontal
    
    enum DividerStyle {
        /// Thin divider - 0.5pt line with 15% opacity
        case thin
        
        /// Medium divider - 1.0pt line with 20% opacity
        case medium
        
        /// Thick divider - 1.5pt line with 25% opacity
        case thick
    }
    
    enum DividerOrientation {
        case horizontal
        case vertical
    }
    
    var body: some View {
        if orientation == .horizontal {
            horizontalDivider
        } else {
            verticalDivider
        }
    }
    
    // MARK: - Divider Views
    
    private var horizontalDivider: some View {
        Rectangle()
            .fill(Color.clear)
            .frame(height: dividerHeight)
            .frame(maxWidth: .infinity)
            .background(
                LinearGradient(
                    gradient: Gradient(colors: [
                        dividerColor ?? .secondary.opacity(0.15),
                        dividerColor ?? .secondary.opacity(0)
                    ]),
                    startPoint: .leading,
                    endPoint: .trailing
                )
            )
            .padding(.horizontal, 12)
    }
    
    private var verticalDivider: some View {
        Rectangle()
            .fill(Color.clear)
            .frame(width: dividerHeight)
            .frame(maxHeight: .infinity)
            .background(
                LinearGradient(
                    gradient: Gradient(colors: [
                        dividerColor ?? .secondary.opacity(0.15),
                        dividerColor ?? .secondary.opacity(0)
                    ]),
                    startPoint: .top,
                    endPoint: .bottom
                )
            )
            .padding(.vertical, 12)
    }
    
    // MARK: - Computed Properties
    
    private var dividerHeight: CGFloat {
        switch style {
        case .thin: return 0.5
        case .medium: return 1.0
        case .thick: return 1.5
        }
    }
}

// MARK: - Preview

struct LiquidGlassDivider_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                Text("Horizontal Dividers")
                    .font(.headline)
                    .padding(.bottom)
                
                LiquidGlassDivider(style: .thin)
                
                LiquidGlassDivider(style: .medium)
                
                LiquidGlassDivider(style: .thick)
                
                LiquidGlassDivider(opacity: 0.3)
                
                LiquidGlassDivider(color: .blue)
                
                Divider()
                    .padding(.vertical)
                
                Text("Vertical Dividers")
                    .font(.headline)
                    .padding(.bottom)
                
                HStack(spacing: 20) {
                    LiquidGlassDivider(style: .thin, orientation: .vertical)
                        .frame(height: 100)
                    
                    LiquidGlassDivider(style: .medium, orientation: .vertical)
                        .frame(height: 100)
                    
                    LiquidGlassDivider(style: .thick, orientation: .vertical)
                        .frame(height: 100)
                }
            }
            .padding()
            .background(Color(red: 0.97, green: 0.97, blue: 0.98))
        }
    }
}
