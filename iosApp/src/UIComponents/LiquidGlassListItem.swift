import SwiftUI

/// List item style variants for LiquidGlassListItem
enum ListItemStyle {
    case `default`      // Standard list item
    case prominent      // Larger, more emphasis
    case compact        // Smaller, tighter spacing
}

/// LiquidGlassListItem - Standardized list item component with Liquid Glass effect
///
/// Usage:
/// ```swift
/// LiquidGlassListItem(
///     title: "Event Title",
///     subtitle: "Event description",
///     icon: "calendar"
/// )
/// ```
struct LiquidGlassListItem<TrailingContent: View>: View {
    let title: String
    let subtitle: String?
    let icon: String?
    let iconColor: Color?
    let style: ListItemStyle
    let trailingContent: TrailingContent?

    init(
        title: String,
        subtitle: String? = nil,
        icon: String? = nil,
        iconColor: Color? = nil,
        style: ListItemStyle = .default,
        @ViewBuilder trailingContent: () -> TrailingContent
    ) {
        self.title = title
        self.subtitle = subtitle
        self.icon = icon
        self.iconColor = iconColor
        self.style = style
        self.trailingContent = trailingContent()
    }

    var body: some View {
        HStack(spacing: itemSpacing) {
            // Icon (if provided)
            if icon != nil {
                iconView
            }

            // Content
            VStack(alignment: .leading, spacing: subtitleSpacing) {
                Text(title)
                    .font(titleFont)
                    .foregroundColor(.primary)

                if let subtitle = subtitle {
                    Text(subtitle)
                        .font(subtitleFont)
                        .foregroundColor(.secondary)
                        .lineLimit(subtitleLineLimit)
                }
            }

            Spacer()

            // Trailing content (if provided)
            if let trailing = trailingContent {
                trailing
            }
        }
        .padding(itemPadding)
        .liquidGlass(cornerRadius: cornerRadius, opacity: 0.85, intensity: 0.9)
    }

    // MARK: - Computed Properties

    private var itemSpacing: CGFloat {
        switch style {
        case .default: return 12
        case .prominent: return 16
        case .compact: return 8
        }
    }

    private var itemPadding: CGFloat {
        switch style {
        case .default: return 16
        case .prominent: return 20
        case .compact: return 12
        }
    }

    private var cornerRadius: CGFloat {
        switch style {
        case .default: return 16
        case .prominent: return 20
        case .compact: return 12
        }
    }

    private var subtitleSpacing: CGFloat {
        switch style {
        case .default: return 4
        case .prominent: return 6
        case .compact: return 2
        }
    }

    private var subtitleLineLimit: Int {
        switch style {
        case .default: return 2
        case .prominent: return 3
        case .compact: return 1
        }
    }

    private var titleFont: Font {
        switch style {
        case .default: return .headline
        case .prominent: return .title3.weight(.semibold)
        case .compact: return .subheadline.weight(.medium)
        }
    }

    private var subtitleFont: Font {
        switch style {
        case .default: return .subheadline
        case .prominent: return .subheadline
        case .compact: return .caption
        }
    }

    private var iconView: some View {
        ZStack {
            Circle()
                .fill(
                    LinearGradient(
                        gradient: Gradient(colors: [
                            (iconColor ?? .wakevPrimary).opacity(0.2),
                            (iconColor ?? .wakevAccent).opacity(0.1)
                        ]),
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: iconSize, height: iconSize)

            Image(systemName: icon ?? "circle")
                .font(.system(size: iconFontSize, weight: .medium))
                .foregroundColor(iconColor ?? .wakevPrimary)
        }
    }

    private var iconSize: CGFloat {
        switch style {
        case .default: return 44
        case .prominent: return 52
        case .compact: return 36
        }
    }

    private var iconFontSize: CGFloat {
        switch style {
        case .default: return 18
        case .prominent: return 22
        case .compact: return 14
        }
    }
}

// MARK: - Convenience Initializer (No Trailing Content)

extension LiquidGlassListItem where TrailingContent == EmptyView {
    init(
        title: String,
        subtitle: String? = nil,
        icon: String? = nil,
        iconColor: Color? = nil,
        style: ListItemStyle = .default
    ) {
        self.title = title
        self.subtitle = subtitle
        self.icon = icon
        self.iconColor = iconColor
        self.style = style
        self.trailingContent = nil
    }
}

// MARK: - Previews

#Preview("LiquidGlassListItem - Default") {
    LiquidGlassListItem(
        title: "Event Title",
        subtitle: "Event description with some details",
        icon: "calendar",
        iconColor: .blue
    )
    .padding()
}

#Preview("LiquidGlassListItem - Prominent") {
    LiquidGlassListItem(
        title: "Prominent Event",
        subtitle: "This is a more prominent list item with larger spacing",
        icon: "star.fill",
        iconColor: .orange,
        style: .prominent
    )
    .padding()
}

#Preview("LiquidGlassListItem - Compact") {
    LiquidGlassListItem(
        title: "Compact Item",
        subtitle: "Short subtitle",
        icon: "clock",
        iconColor: .green,
        style: .compact
    )
    .padding()
}

#Preview("LiquidGlassListItem - With Trailing") {
    LiquidGlassListItem(
        title: "Event with Badge",
        subtitle: "Has trailing content",
        icon: "checkmark.circle",
        iconColor: .green
    ) {
        LiquidGlassBadge(text: "Confirmé", style: .success)
    }
    .padding()
}
