import SwiftUI

/// Liquid Glass List Item Component
///
/// A reusable list item component following Apple's Liquid Glass guidelines.
/// Displays items with optional icons, trailing content, and tap actions.
///
/// ## Features
/// - Clean, glass-style backgrounds
/// - Title and optional subtitle
/// - Left and right icon support
/// - Trailing content view
/// - Tap action support with haptic feedback
/// - Accessibility support
///
/// ## Usage Examples
/// ```swift
/// // Basic item
/// LiquidGlassListItem(
///     title: "Team Meeting",
///     subtitle: "Weekly planning session"
/// )
///
/// // Item with icon
/// LiquidGlassListItem(
///     title: "Event Details",
///     icon: "info.circle.fill",
///     subtitle: "View event information"
/// )
///
/// // Item with tap action
/// LiquidGlassListItem(
///     title: "Confirm Attendance",
///     icon: "checkmark.circle.fill",
///     onTap: { /* handle tap */ }
/// )
///
/// // Item with trailing content
/// LiquidGlassListItem(
///     title: "Team Meeting",
///     trailingContent: Text("12 participants")
/// )
///
/// // Item with all features
/// LiquidGlassListItem(
///     title: "Team Meeting",
///     subtitle: "Weekly planning session",
///     icon: "calendar",
///     trailingContent: Button("Join"),
///     onTap: { /* navigate */ }
/// )
/// ```

struct LiquidGlassListItem: View {
    let title: String
    let subtitle: String?
    let icon: String?
    let trailingContent: AnyView?
    let onTap: (() -> Void)?
    
    @State private var isPressed = false
    
    var body: some View {
        Button(action: {
            handleTap()
        withAnimation(.spring(response: 0.3, dampingFraction: 0.7)) {
                isPressed = true
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.15) {
                    isPressed = false
                }
            }
        }) {
            buttonContent
                .buttonStyle(.plain)
                .accessibilityLabel(title)
                .accessibilityHint(subtitle ?? "")
        }
    }
    
    // MARK: - Button Content
    
    @ViewBuilder
    private var buttonContent: some View {
        HStack(spacing: 12) {
            // Left icon
            if let icon = icon {
                Image(systemName: icon)
                    .font(.system(size: 16, weight: .medium))
                    .foregroundColor(.secondary)
                    .frame(width: 20)
            }
            
            // Text content
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.headline.weight(.semibold))
                    .foregroundColor(.primary)
                    .lineLimit(1)
                
                if let subtitle = subtitle {
                    Text(subtitle)
                        .font(.subheadline)
                        .foregroundColor(.secondary)
                        .lineLimit(2)
                }
            }
            
            Spacer()
            
            // Trailing content
            if let trailing = trailingContent {
                trailing
            }
        }
        .padding(16)
        .background(.ultraThinMaterial)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
    
    // MARK: - Helper Method
    
    private func handleTap() {
        // Haptic feedback
        let impactor = UIImpactFeedbackGenerator(style: .light)
        impactor.impactOccurred()
        
        onTap?()
    }
}

// MARK: - Preview

struct LiquidGlassListItem_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                Text("Basic Items")
                    .font(.headline)
                    .padding(.bottom)
                
                LiquidGlassListItem(
                    title: "Team Meeting",
                    subtitle: "Weekly planning session"
                )
                
                LiquidGlassListItem(
                    title: "Product Launch",
                    subtitle: "Q4 planning and roadmap discussion"
                )
                
                LiquidGlassListItem(
                    title: "One-on-One Meeting",
                    subtitle: "Monthly catchup with manager"
                )
                
                Divider()
                    .padding(.vertical)
                
                Text("Items with Icons")
                    .font(.headline)
                    .padding(.bottom)
                
                LiquidGlassListItem(
                    title: "Calendar Event",
                    icon: "calendar",
                    subtitle: "Team sync meeting"
                )
                
                LiquidGlassListItem(
                    title: "Location",
                    icon: "mappin",
                    subtitle: "Office downtown"
                )
                
                LiquidGlassListItem(
                    title: "Time",
                    icon: "clock",
                    subtitle: "3:00 PM - 4:00 PM"
                )
                
                Divider()
                    .padding(.vertical)
                
                Text("Items with Tap Actions")
                    .font(.headline)
                    .padding(.bottom)
                
                LiquidGlassListItem(
                    title: "Confirm Attendance",
                    icon: "checkmark.circle.fill",
                    onTap: { }
                )
                
                LiquidGlassListItem(
                    title: "View Details",
                    icon: "info.circle.fill",
                    onTap: { }
                )
                
                LiquidGlassListItem(
                    title: "Share Event",
                    icon: "square.and.arrow.up",
                    onTap: { }
                )
                
                Divider()
                    .padding(.vertical)
                
                Text("Items with Trailing Content")
                    .font(.headline)
                    .padding(.bottom)
                
                LiquidGlassListItem(
                    title: "Team Meeting",
                    trailingContent: Text("12 participants")
                )
                
                LiquidGlassListItem(
                    title: "Product Launch",
                    trailingContent: Text("5 participants")
                )
                
                LiquidGlassListItem(
                    title: "One-on-One",
                    trailingContent: HStack {
                        Text("Manager")
                        Text("•")
                        Text("Today at 3PM")
                    })
                )
                
                LiquidGlassListItem(
                    title: "Complete",
                    trailingContent: Text("✓")
                )
            }
            .padding()
            .background(Color(red: 0.97, green: 0.97, blue: 0.98))
        }
    }
}
