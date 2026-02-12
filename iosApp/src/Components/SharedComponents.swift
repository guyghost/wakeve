import SwiftUI

// Import the common UI components
// LiquidGlassBadge, LiquidGlassButton, LiquidGlassDivider are defined in UIComponents/

// MARK: - Vote Enum

enum PollVote: String, Codable {
    case yes = "YES"
    case maybe = "MAYBE"
    case no = "NO"
}

// MARK: - Shared Components

/// Info row with label and value, optional icon - Liquid Glass enhanced
struct InfoRow: View {
    let label: String
    let value: String
    let icon: String?

    init(label: String, value: String, icon: String? = nil) {
        self.label = label
        self.value = value
        self.icon = icon
    }

    var body: some View {
        HStack(spacing: 12) {
            if let icon = icon {
                Image(systemName: icon)
                    .font(.body)
                    .foregroundColor(.wakeveAccent)
                    .frame(width: 20)
            }

            Text(label)
                .font(.subheadline)
                .foregroundColor(.secondary)

            Spacer()

            Text(value)
                .font(.subheadline)
                .foregroundColor(.primary)
                .fontWeight(.medium)
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("\(label), \(value)")
    }
}

/// Status badge with color coding - Uses LiquidGlassBadge
struct StatusBadge: View {
    let status: String

    var body: some View {
        LiquidGlassBadge(
            text: statusText,
            style: statusBadgeStyle
        )
        .accessibilityLabel(statusText)
    }

    private var statusText: String {
        switch status {
        case "PLANNED": return "Planifié"
        case "ASSIGNED": return "Assigné"
        case "IN_PROGRESS": return "En cours"
        case "COMPLETED": return "Terminé"
        case "CANCELLED": return "Annulé"
        default: return status
        }
    }

    private var statusBadgeStyle: LiquidGlassBadgeStyle {
        switch status {
        case "PLANNED": return .info
        case "ASSIGNED": return .accent
        case "IN_PROGRESS": return .warning
        case "COMPLETED": return .success
        case "CANCELLED": return .default
        default: return .default
        }
    }
}

/// Filter chip with selection state and action - Liquid Glass enhanced
struct FilterChip: View {
    let text: String
    let icon: String?
    let count: Int?
    let isSelected: Bool
    let action: () -> Void

    init(text: String, isSelected: Bool, action: @escaping () -> Void) {
        self.text = text
        self.icon = nil
        self.count = nil
        self.isSelected = isSelected
        self.action = action
    }

    init(title: String, isSelected: Bool, action: @escaping () -> Void) {
        self.text = title
        self.icon = nil
        self.count = nil
        self.isSelected = isSelected
        self.action = action
    }

    init(title: String, icon: String, isSelected: Bool, action: @escaping () -> Void) {
        self.text = title
        self.icon = icon
        self.count = nil
        self.isSelected = isSelected
        self.action = action
    }

    init(title: String, count: Int, isSelected: Bool, action: @escaping () -> Void) {
        self.text = title
        self.icon = nil
        self.count = count
        self.isSelected = isSelected
        self.action = action
    }

    init(title: String, icon: String?, isSelected: Bool, action: @escaping () -> Void) {
        self.text = title
        self.icon = icon
        self.count = nil
        self.isSelected = isSelected
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if let icon = icon {
                    Image(systemName: icon)
                        .font(.system(size: 12))
                }
                Text(text)
                    .font(.subheadline)
                    .fontWeight(isSelected ? .semibold : .regular)

                if let count = count, count > 0 {
                    Text("\(count)")
                        .font(.caption.weight(.semibold))
                        .padding(.horizontal, 8)
                        .padding(.vertical, 4)
                        .background(
                            RoundedRectangle(cornerRadius: 10)
                                .fill(isSelected ? Color.white.opacity(0.25) : Color.wakeveBorderLight.opacity(0.3))
                        )
                }
            }
            .foregroundColor(isSelected ? .white : .primary)
            .padding(.horizontal, 16)
            .padding(.vertical, 8)
            .background(
                Group {
                    if isSelected {
                        LinearGradient(
                            gradient: Gradient(colors: [.wakevePrimary, .wakeveAccent]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                    } else {
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.white.opacity(0.7),
                                Color.white.opacity(0.5)
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        )
                        .overlay(
                            RoundedRectangle(cornerRadius: 20)
                                .strokeBorder(Color.wakeveBorderLight.opacity(0.5), lineWidth: 1)
                        )
                    }
                }
            )
            .clipShape(Capsule())
            .shadow(color: isSelected ? Color.wakevePrimary.opacity(0.3) : Color.clear, radius: 4, x: 0, y: 2)
        }
        .buttonStyle(.plain)
        .accessibilityLabel(text)
        .accessibilityHint(isSelected ? "Selected" : "Tap to select")
    }
}

/// Vote button for poll voting (Yes/Maybe/No) - Liquid Glass enhanced
struct VoteButton: View {
    let vote: PollVote
    let isSelected: Bool
    let action: () -> Void

    @State private var isPressed = false

    var body: some View {
        Button(action: action) {
            VStack(spacing: 8) {
                ZStack {
                    Circle()
                        .fill(
                            isSelected
                                ? LinearGradient(
                                    gradient: Gradient(colors: [
                                        voteColor(for: vote),
                                        voteColor(for: vote).opacity(0.7)
                                    ]),
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                                : LinearGradient(
                                    gradient: Gradient(colors: [
                                        Color.white.opacity(0.2),
                                        Color.white.opacity(0.2)
                                    ]),
                                    startPoint: .topLeading,
                                    endPoint: .bottomTrailing
                                )
                        )
                        .frame(width: 50, height: 50)
                        .overlay(
                            Circle()
                                .stroke(
                                    isSelected
                                        ? LinearGradient(
                                            gradient: Gradient(colors: [
                                                voteColor(for: vote).opacity(0.5),
                                                voteColor(for: vote).opacity(0.3)
                                            ]),
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        )
                                        : LinearGradient(
                                            gradient: Gradient(colors: [
                                                Color.white.opacity(0.3),
                                                Color.white.opacity(0.3)
                                            ]),
                                            startPoint: .topLeading,
                                            endPoint: .bottomTrailing
                                        ),
                                    lineWidth: 2
                                )
                        )

                    Image(systemName: voteSymbol(for: vote))
                        .font(.system(size: 20, weight: .bold, design: .rounded))
                        .foregroundColor(isSelected ? .white : .white.opacity(0.8))
                }

                Text(voteLabel(for: vote))
                    .font(.system(size: 12, weight: .medium, design: .rounded))
                    .foregroundColor(isSelected ? voteColor(for: vote) : .white.opacity(0.8))
            }
        }
        .scaleEffect(isPressed ? 0.95 : 1.0)
        .animation(.easeInOut(duration: 0.15), value: isPressed)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in isPressed = true }
                .onEnded { _ in isPressed = false }
        )
        .accessibilityLabel(voteLabel(for: vote))
    }

    private func voteColor(for vote: PollVote) -> Color {
        switch vote {
        case .yes: return .wakeveSuccess
        case .maybe: return .wakeveWarning
        case .no: return .wakeveError
        }
    }

    private func voteSymbol(for vote: PollVote) -> String {
        switch vote {
        case .yes: return "checkmark"
        case .maybe: return "minus"
        case .no: return "xmark"
        }
    }

    private func voteLabel(for vote: PollVote) -> String {
        switch vote {
        case .yes: return "Oui"
        case .maybe: return "Peut-être"
        case .no: return "Non"
        }
    }
}

// MARK: - Additional Liquid Glass Shared Components

/// Section header with optional action button
struct SectionHeader: View {
    let title: String
    let actionTitle: String?
    let action: (() -> Void)?

    init(
        _ title: String,
        actionTitle: String? = nil,
        action: (() -> Void)? = nil
    ) {
        self.title = title
        self.actionTitle = actionTitle
        self.action = action
    }

    var body: some View {
        HStack {
            Text(title)
                .font(.headline)
                .foregroundColor(.primary)

            Spacer()

            if let actionTitle = actionTitle, let action = action {
                Button(action: action) {
                    Text(actionTitle)
                        .font(.subheadline)
                        .foregroundColor(.wakevePrimary)
                }
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }
}

/// Empty state view with icon, title, and optional action
struct EmptyStateView: View {
    let icon: String
    let title: String
    let message: String
    let actionTitle: String?
    let action: (() -> Void)?

    init(
        icon: String,
        title: String,
        message: String,
        actionTitle: String? = nil,
        action: (() -> Void)? = nil
    ) {
        self.icon = icon
        self.title = title
        self.message = message
        self.actionTitle = actionTitle
        self.action = action
    }

    var body: some View {
        VStack(spacing: 16) {
            Image(systemName: icon)
                .font(.system(size: 48))
                .foregroundColor(.wakeveAccent.opacity(0.6))

            Text(title)
                .font(.title3.weight(.semibold))
                .foregroundColor(.primary)

            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            if let actionTitle = actionTitle, let action = action {
                LiquidGlassButton(title: actionTitle, style: .primary) {
                    action()
                }
                .frame(width: 200)
                .padding(.top, 8)
            }
        }
        .padding()
    }
}

/// Loading view with spinner
struct LoadingView: View {
    let message: String

    var body: some View {
        VStack(spacing: 16) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle(tint: .wakevePrimary))
                .scaleEffect(1.2)

            Text(message)
                .font(.subheadline)
                .foregroundColor(.secondary)
        }
        .padding()
    }
}

/// Action row with icon, title, and chevron
struct ActionRow: View {
    let icon: String
    let title: String
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.body)
                    .foregroundColor(.wakevePrimary)
                    .frame(width: 24)

                Text(title)
                    .font(.subheadline)
                    .foregroundColor(.primary)

                Spacer()

                Image(systemName: "chevron.right")
                    .font(.caption)
                    .foregroundColor(.secondary)
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 12)
            .liquidGlass(cornerRadius: 12, opacity: 0.7, intensity: 0.8)
        }
        .buttonStyle(.plain)
    }
}

/// Rating display component
struct RatingDisplay: View {
    let rating: Int
    let maxRating: Int
    let icon: String

    init(rating: Int, maxRating: Int = 5, icon: String = "star.fill") {
        self.rating = rating
        self.maxRating = maxRating
        self.icon = icon
    }

    var body: some View {
        HStack(spacing: 4) {
            ForEach(1...maxRating, id: \.self) { index in
                Image(systemName: icon)
                    .font(.caption)
                    .foregroundColor(index <= rating ? .wakeveWarning : Color.gray.opacity(0.3))
            }
        }
    }
}

/// Price display component
struct PriceDisplay: View {
    let amount: Double
    let currency: String
    let style: PriceStyle

    enum PriceStyle {
        case normal
        case large
        case subtle
    }

    var body: some View {
        Text(formattedPrice)
            .font(priceFont)
            .foregroundColor(priceColor)
    }

    private var formattedPrice: String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = currency
        return formatter.string(from: NSNumber(value: amount)) ?? "\(currency) \(amount)"
    }

    private var priceFont: Font {
        switch style {
        case .normal: return .subheadline.weight(.medium)
        case .large: return .title2.weight(.bold)
        case .subtle: return .caption
        }
    }

    private var priceColor: Color {
        switch style {
        case .normal: return .primary
        case .large: return .wakevePrimary
        case .subtle: return .secondary
        }
    }
}

// MARK: - Previews

struct SharedComponents_Previews: PreviewProvider {
    static var previews: some View {
        ScrollView {
            VStack(spacing: 24) {
                // InfoRow
                VStack(alignment: .leading, spacing: 8) {
                    Text("InfoRow")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    InfoRow(label: "Date", value: "25 Dec", icon: "calendar")
                    InfoRow(label: "Participants", value: "8 personnes")
                }
                .padding()
                .liquidGlass(cornerRadius: 16)

                // StatusBadge
                VStack(alignment: .leading, spacing: 8) {
                    Text("StatusBadge")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    HStack(spacing: 12) {
                        StatusBadge(status: "PLANNED")
                        StatusBadge(status: "IN_PROGRESS")
                        StatusBadge(status: "COMPLETED")
                    }
                }
                .padding()
                .liquidGlass(cornerRadius: 16)

                // FilterChip
                VStack(alignment: .leading, spacing: 8) {
                    Text("FilterChip")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    HStack(spacing: 12) {
                        FilterChip(text: "Tous", isSelected: true, action: {})
                        FilterChip(text: "Actifs", isSelected: false, action: {})
                        FilterChip(text: "Terminés", isSelected: false, action: {})
                    }
                }
                .padding()
                .liquidGlass(cornerRadius: 16)

                // VoteButton
                VStack(alignment: .leading, spacing: 8) {
                    Text("VoteButton")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    HStack(spacing: 20) {
                        VoteButton(vote: .yes, isSelected: true, action: {})
                        VoteButton(vote: .maybe, isSelected: false, action: {})
                        VoteButton(vote: .no, isSelected: false, action: {})
                    }
                }
                .padding()
                .liquidGlass(cornerRadius: 16)

                // SectionHeader
                VStack(alignment: .leading, spacing: 8) {
                    Text("SectionHeader")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    SectionHeader("Mes Événements", actionTitle: "Voir tout") {
                        print("Voir tout tapped")
                    }
                }
                .padding()
                .liquidGlass(cornerRadius: 16)

                // ActionRow
                VStack(alignment: .leading, spacing: 8) {
                    Text("ActionRow")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    ActionRow(icon: "calendar.badge.plus", title: "Créer un événement") {
                        print("Create event tapped")
                    }

                    ActionRow(icon: "gearshape", title: "Paramètres") {
                        print("Settings tapped")
                    }
                }
                .padding()
                .liquidGlass(cornerRadius: 16)

                // Rating & Price
                VStack(alignment: .leading, spacing: 8) {
                    Text("Rating & Price")
                        .font(.caption)
                        .foregroundColor(.secondary)

                    HStack {
                        RatingDisplay(rating: 4)
                        Spacer()
                        PriceDisplay(amount: 99.99, currency: "EUR", style: .large)
                    }
                }
                .padding()
                .liquidGlass(cornerRadius: 16)
            }
            .padding()
        }
        .background(Color.wakeveBackgroundDark)
    }
}
