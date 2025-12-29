import SwiftUI

/// Réusable badge component with Liquid Glass styling
/// 
/// Supports two styles:
/// - `.filled`: Colored background for selected/important states
/// - `.glass`: Material background for normal/secondary states
///
/// Example usage:
/// ```swift
/// GlassBadge(text: "Camping", icon: nil, color: .blue, style: .filled)
/// GlassBadge(text: "Assigner", icon: "person.badge.plus", color: .secondary, style: .glass)
/// ```
struct GlassBadge: View {
    let text: String
    let icon: String?
    let color: Color
    let style: BadgeStyle
    
    enum BadgeStyle {
        case filled    // État sélectionné, couleur visible
        case glass     // État normal, material
    }
    
    var body: some View {
        HStack(spacing: 4) {
            if let icon = icon {
                Image(systemName: icon)
                    .font(.caption2)
            }
            Text(text)
                .font(.caption2)
                .lineLimit(1)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background {
            if style == .filled {
                color.opacity(0.15)
            } else {
                Color.clear.background(.ultraThinMaterial)
            }
        }
        .foregroundColor(style == .filled ? color : .primary)
        .continuousCornerRadius(8)
        .shadow(
            color: .black.opacity(style == .filled ? 0.05 : 0.02),
            radius: 2,
            x: 0,
            y: 1
        )
    }
}

// MARK: - Preview

#Preview("GlassBadge Styles") {
    VStack(spacing: 16) {
        // Filled style badges
        Section("Filled Style (Selected/Important)") {
            VStack(spacing: 8) {
                GlassBadge(
                    text: "Camping",
                    icon: nil,
                    color: .blue,
                    style: .filled
                )
                
                GlassBadge(
                    text: "Assigné",
                    icon: "person.fill",
                    color: .blue,
                    style: .filled
                )
                
                GlassBadge(
                    text: "Emballé",
                    icon: "checkmark.circle.fill",
                    color: .green,
                    style: .filled
                )
                
                GlassBadge(
                    text: "Supprimer",
                    icon: "trash",
                    color: .red,
                    style: .filled
                )
            }
            .padding()
            .background(Color(.systemGray6))
            .continuousCornerRadius(12)
        }
        
        // Glass style badges
        Section("Glass Style (Normal/Secondary)") {
            VStack(spacing: 8) {
                GlassBadge(
                    text: "Assigner",
                    icon: "person.badge.plus",
                    color: .secondary,
                    style: .glass
                )
                
                GlassBadge(
                    text: "Modifier",
                    icon: "pencil",
                    color: .blue,
                    style: .glass
                )
                
                GlassBadge(
                    text: "Option",
                    icon: nil,
                    color: .secondary,
                    style: .glass
                )
            }
            .padding()
            .background(Color(.systemGray6))
            .continuousCornerRadius(12)
        }
        
        Spacer()
    }
    .padding()
}
