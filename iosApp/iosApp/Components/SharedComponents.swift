import SwiftUI

// MARK: - Color Extension for Design System

extension Color {
    // Primary colors
    static let wakevPrimary = Color(hex: "2563EB")
    static let wakevAccent = Color(hex: "7C3AED")
    
    // Semantic colors
    static let wakevSuccess = Color(hex: "10B981")
    static let wakevWarning = Color(hex: "F59E0B")
    static let wakevError = Color(hex: "EF4444")
    
    // Backgrounds
    static let wakevBackgroundDark = Color(hex: "0F172A")
    static let wakevSurfaceDark = Color(hex: "1E293B")
    
    init(hex: String) {
        let scanner = Scanner(string: hex)
        var rgbValue: UInt64 = 0
        scanner.scanHexInt64(&rgbValue)
        
        let r = Double((rgbValue & 0xFF0000) >> 16) / 255.0
        let g = Double((rgbValue & 0x00FF00) >> 8) / 255.0
        let b = Double(rgbValue & 0x0000FF) / 255.0
        
        self.init(red: r, green: g, blue: b)
    }
}

// MARK: - Liquid Glass View Modifier

struct LiquidGlassModifier: ViewModifier {
    var cornerRadius: CGFloat = 20
    var opacity: Double = 0.8
    var intensity: Double = 1.0
    
    func body(content: Content) -> some View {
        content
            .background(
                ZStack {
                    RoundedRectangle(cornerRadius: cornerRadius)
                        .fill(
                            LinearGradient(
                                gradient: Gradient(colors: [
                                    Color.white.opacity(opacity * 0.1),
                                    Color.white.opacity(opacity * 0.05)
                                ]),
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                        )
                    RoundedRectangle(cornerRadius: cornerRadius)
                        .fill(Color.white.opacity(0.02))
                }
            )
            .overlay(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .stroke(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.white.opacity(0.3 * intensity),
                                Color.white.opacity(0.1 * intensity)
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1.5
                    )
            )
            .background(
                RoundedRectangle(cornerRadius: cornerRadius)
                    .fill(Color.blue.opacity(0.1))
                    .blur(radius: 10)
            )
    }
}

extension View {
    func liquidGlass(cornerRadius: CGFloat = 20, opacity: Double = 0.8, intensity: Double = 1.0) -> some View {
        modifier(LiquidGlassModifier(cornerRadius: cornerRadius, opacity: opacity, intensity: intensity))
    }
}

// MARK: - Liquid Glass Badge Component

enum LiquidGlassBadgeStyle {
    case `default`
    case success
    case warning
    case info
    case accent
}

struct LiquidGlassBadge: View {
    let text: String
    let icon: String?
    let style: LiquidGlassBadgeStyle
    
    init(
        text: String,
        icon: String? = nil,
        style: LiquidGlassBadgeStyle = .default
    ) {
        self.text = text
        self.icon = icon
        self.style = style
    }
    
    var body: some View {
        HStack(spacing: 4) {
            if let icon = icon {
                Image(systemName: icon)
                    .font(.caption2.weight(.medium))
            }
            Text(text)
                .font(.caption2.weight(.medium))
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .foregroundColor(badgeColor)
        .background(badgeBackground)
        .overlay(badgeOverlay)
        .accessibilityLabel(text)
    }
    
    private var badgeBackground: Color {
        badgeColor.opacity(0.15)
    }
    
    private var badgeOverlay: Color {
        badgeColor.opacity(0.3)
    }
    
    private var badgeColor: Color {
        switch style {
        case .default: return .secondary
        case .success: return .wakevSuccess
        case .warning: return .wakevWarning
        case .info: return .blue
        case .accent: return .purple
        }
    }
}

// MARK: - Liquid Glass Button Component

enum LiquidGlassButtonStyle {
    case primary
    case secondary
    case text
}

struct LiquidGlassButton: View {
    let title: String
    let style: LiquidGlassButtonStyle
    let action: () -> Void
    
    @State private var isPressed = false
    
    init(
        title: String,
        style: LiquidGlassButtonStyle = .primary,
        action: @escaping () -> Void
    ) {
        self.title = title
        self.style = style
        self.action = action
    }
    
    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                Text(title)
                    .font(.subheadline.weight(.semibold))
            }
            .frame(maxWidth: .infinity)
            .frame(height: 44)
            .foregroundColor(buttonForegroundColor)
            .background(buttonBackground)
            .overlay(buttonOverlay)
        }
        .buttonStyle(PlainButtonStyle())
        .scaleEffect(isPressed ? 0.98 : 1.0)
        .animation(.easeInOut(duration: 0.15), value: isPressed)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in isPressed = true }
                .onEnded { _ in isPressed = false }
        )
        .accessibilityLabel(title)
    }
    
    private var buttonForegroundColor: Color {
        switch style {
        case .primary: return .white
        case .secondary: return .wakevPrimary
        case .text: return .wakevPrimary
        }
    }
    
    private var buttonBackground: some View {
        Group {
            switch style {
            case .primary:
                LinearGradient(
                    gradient: Gradient(colors: [
                        Color.wakevPrimary,
                        Color.wakevAccent
                    ]),
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            case .secondary:
                Color.clear
            case .text:
                Color.clear
            }
        }
    }
    
    private var buttonOverlay: some View {
        Group {
            switch style {
            case .primary:
                RoundedRectangle(cornerRadius: 12)
                    .stroke(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.white.opacity(0.3),
                                Color.white.opacity(0.1)
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1
                    )
            case .secondary:
                RoundedRectangle(cornerRadius: 12)
                    .stroke(
                        LinearGradient(
                            gradient: Gradient(colors: [
                                Color.wakevPrimary.opacity(0.5),
                                Color.wakevAccent.opacity(0.5)
                            ]),
                            startPoint: .topLeading,
                            endPoint: .bottomTrailing
                        ),
                        lineWidth: 1.5
                    )
                    .liquidGlass(cornerRadius: 12, opacity: 0.6, intensity: 0.8)
            case .text:
                EmptyView()
            }
        }
    }
}

// MARK: - Liquid Glass Divider Component

struct LiquidGlassDivider: View {
    let style: DividerStyle
    let orientation: DividerOrientation
    
    enum DividerStyle {
        case subtle
        case `default`
        case prominent
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
        case .subtle: return 0.5
        case .default: return 1
        case .prominent: return 2
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
                    .foregroundColor(.wakevAccent)
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
    let isSelected: Bool
    let action: () -> Void
    
    init(text: String, isSelected: Bool, action: @escaping () -> Void) {
        self.text = text
        self.isSelected = isSelected
        self.action = action
    }
    
    init(title: String, isSelected: Bool, action: @escaping () -> Void) {
        self.text = title
        self.isSelected = isSelected
        self.action = action
    }
    
    init(title: String, icon: String, isSelected: Bool, action: @escaping () -> Void) {
        self.text = title
        self.isSelected = isSelected
        self.action = action
    }
    
    var body: some View {
        LiquidGlassButton(
            title: text,
            style: isSelected ? .primary : .secondary
        ) {
            action()
        }
        .frame(height: 36)
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
        case .yes: return .wakevSuccess
        case .maybe: return .wakevWarning
        case .no: return .wakevError
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
                        .foregroundColor(.wakevPrimary)
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
                .foregroundColor(.wakevAccent.opacity(0.6))
            
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
                .progressViewStyle(CircularProgressViewStyle(tint: .wakevPrimary))
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
                    .foregroundColor(.wakevPrimary)
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
                    .foregroundColor(index <= rating ? .wakevWarning : Color.gray.opacity(0.3))
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
        case .large: return .wakevPrimary
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
        .background(Color.wakevBackgroundDark)
    }
}
