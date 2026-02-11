import SwiftUI

/// AI Badge View - A liquid glass styled badge component for AI suggestions
///
/// Displays AI-generated badges with glassmorphism effects, tooltips, and accessibility.
///
/// Features:
/// - Liquid Glass styling with ultraThinMaterial
/// - Dynamic Type support
/// - VoiceOver accessibility with hints
/// - Tap gesture for tooltip display
/// - Smooth animations
///
/// Example usage:
/// ```swift
/// let badge = AIBadge.from(type: .highConfidence)
/// AIBadgeView(badge: badge) {
///     showTooltip(for: badge)
/// }
/// ```
struct AIBadgeView: View {
    let badge: AIBadge
    var onTap: (() -> Void)?
    var showTooltip: Bool = false
    
    @State private var isPressed = false
    @State private var showDetail = false
    
    private let animation = Animation.spring(response: 0.3, dampingFraction: 0.7)
    
    var body: some View {
        badgeContent
            .scaleEffect(isPressed ? 0.95 : 1.0)
            .animation(animation, value: isPressed)
            .onTapGesture {
                handleTap()
            }
            .sheet(isPresented: $showDetail) {
                tooltipSheet
            }
    }
    
    // MARK: - Badge Content
    
    private var badgeContent: some View {
        HStack(spacing: 4) {
            Text(badge.icon)
                .font(.caption)
                .fontWeight(.medium)
            
            Text(badge.displayName)
                .font(.caption)
                .fontWeight(.medium)
                .lineLimit(1)
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(.ultraThinMaterial)
        )
        .overlay(
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .stroke(
                    badge.color.opacity(0.3),
                    lineWidth: 1
                )
        )
        .shadow(
            color: .black.opacity(0.05),
            radius: 4,
            x: 0,
            y: 2
        )
        .accessibilityElement(children: .combine)
        .accessibilityLabel(badge.displayName)
        .accessibilityHint(badge.tooltip ?? "Tap to see more details about this AI suggestion")
        .accessibilityAddTraits(.isButton)
    }
    
    // MARK: - Tooltip Sheet
    
    private var tooltipSheet: some View {
        VStack(spacing: 16) {
            HStack {
                Text(badge.icon)
                    .font(.system(size: 32))
                
                VStack(alignment: .leading, spacing: 4) {
                    Text(badge.displayName)
                        .font(.headline)
                    
                    Text(badge.type.rawValue)
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                Spacer()
            }
            
            Divider()
            
            if let tooltip = badge.tooltip {
                Text(tooltip)
                    .font(.body)
                    .multilineTextAlignment(.leading)
            }
            
            VStack(alignment: .leading, spacing: 8) {
                Text("Confidence Details")
                    .font(.subheadline)
                    .fontWeight(.medium)
                
                HStack {
                    Circle()
                        .fill(badge.color)
                        .frame(width: 12, height: 12)
                    
                    Text("Color: \(badge.colorHex)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
                
                HStack {
                    Image(systemName: "info.circle")
                        .foregroundColor(.secondary)
                    
                    Text("Type: \(badge.type.rawValue)")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding()
            .background(Color(.tertiarySystemFill))
            .continuousCornerRadius(12)
            
            Spacer()
            
            Button("Close") {
                showDetail = false
            }
            .buttonStyle(.borderedProminent)
        }
        .padding(24)
        .thickGlass(cornerRadius: 24)
        .padding()
        .presentationDetents([.medium])
        .presentationDragIndicator(.visible)
    }
    
    // MARK: - Actions
    
    private func handleTap() {
        withAnimation(animation) {
            isPressed = true
        }
        
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.1) {
            withAnimation(animation) {
                isPressed = false
            }
        }
        
        if showTooltip {
            showDetail = true
        }
        
        onTap?()
    }
}

// MARK: - Convenience Initializer

extension AIBadgeView {
    /// Initialize from badge type with default values
    init(type: AIBadgeType, onTap: (() -> Void)? = nil, showTooltip: Bool = false) {
        self.badge = AIBadge.from(type: type)
        self.onTap = onTap
        self.showTooltip = showTooltip
    }
}

// MARK: - Previews

#Preview("AIBadgeView - All Types") {
    ScrollView {
        VStack(spacing: 16) {
            Section("AI Badge Types") {
                ForEach(AIBadgeType.allCases, id: \.self) { type in
                    HStack {
                        AIBadgeView(type: type, showTooltip: true)
                        Spacer()
                    }
                    .padding(.horizontal)
                }
            }
            
            Section("With Custom Tap Handler") {
                AIBadgeView(
                    type: .highConfidence,
                    onTap: {
                        print("Badge tapped!")
                    }
                )
                .padding(.horizontal)
            }
            
            Section("In Context") {
                HStack(spacing: 12) {
                    AIBadgeView(type: .highConfidence)
                    AIBadgeView(type: .personalized)
                    Spacer()
                }
                .padding()
                .glassCard()
                .padding(.horizontal)
            }
        }
        .padding(.vertical)
    }
    .background(Color(red: 0.97, green: 0.97, blue: 0.98))
}

#Preview("AIBadgeView - Tooltip Sheet") {
    Button("Show Badge Detail") {
        // Preview sheet presentation
    }
}

#Preview("AIBadgeView - Dynamic Type") {
    VStack(spacing: 20) {
        AIBadgeView(type: .aiSuggestion)
        
        AIBadgeView(type: .highConfidence)
        
        AIBadgeView(type: .personalized)
    }
    .padding()
    .glassCard()
    .padding()
}
