import SwiftUI

struct WKStatusPill: View {
    let text: String
    let status: WK.Status

    var body: some View {
        Text(text)
            .font(WK.Typo.micro.weight(.medium))
            .foregroundStyle(status.onFill)
            .padding(.horizontal, WK.Space.xs)
            .padding(.vertical, WK.Space.xxs)
            .background(status.fill, in: Capsule(style: .continuous))
            .accessibilityLabel(text)
    }
}
