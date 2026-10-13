import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

// MARK: - Shared UI Components

enum WakeveHaptics {
    static func selection() {
        #if canImport(UIKit)
        UISelectionFeedbackGenerator().selectionChanged()
        #endif
    }

    static func success() {
        #if canImport(UIKit)
        UINotificationFeedbackGenerator().notificationOccurred(.success)
        #endif
    }

    static func warning() {
        #if canImport(UIKit)
        UINotificationFeedbackGenerator().notificationOccurred(.warning)
        #endif
    }
}

// MARK: - Vote Enum

enum PollVote: String, Codable {
    case yes = "YES"
    case maybe = "MAYBE"
    case no = "NO"
}

// MARK: - Price Display

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
