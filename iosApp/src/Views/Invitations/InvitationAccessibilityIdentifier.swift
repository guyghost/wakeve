import SwiftUI


/// Keeps SwiftUI accessibility semantics while exposing the same stable identifier
/// to UIKit-based automation hosts. The bridge itself is not a VoiceOver element.
struct InvitationAccessibilityIdentifierBridge: UIViewRepresentable {
    let identifier: String
    var isEnabled = true

    func makeUIView(context: Context) -> UIView {
        let view = UIView(frame: .zero)
        view.isAccessibilityElement = false
        view.isUserInteractionEnabled = false
        return view
    }

    func updateUIView(_ uiView: UIView, context: Context) {
        uiView.accessibilityIdentifier = identifier
        uiView.accessibilityTraits = isEnabled ? [] : [.notEnabled]
    }
}

extension View {
    func invitationAccessibilityIdentifier(
        _ identifier: String,
        isEnabled: Bool = true
    ) -> some View {
        accessibilityIdentifier(identifier)
            .background {
                InvitationAccessibilityIdentifierBridge(
                    identifier: identifier,
                    isEnabled: isEnabled
                )
                .frame(width: 1, height: 1)
                .allowsHitTesting(false)
                .accessibilityHidden(true)
            }
    }
}
