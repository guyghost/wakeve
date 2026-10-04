import SwiftUI

/// Invitation reçue en mode immersif (couche 8, #47) : qui invite, quoi, quand, qui vient, et l'état
/// de réponse (affiché seulement). Une seule action : voter si le vote est attendu, sinon voir l'événement.
struct InvitationLandingView: View {
    static let accessibilityID = "immersive.invitation"
    static let titleAccessibilityID = "immersive.invitation.title"

    let model: InvitationLandingModel
    /// Illustration de l'invitation (`InvitationArtworkView`), nil sans illustration.
    let artwork: AnyView?
    let onPrimary: () -> Void
    let onClose: () -> Void

    static func responseSymbol(_ response: InvitationLandingFacts.Response) -> String {
        switch response {
        case .organizer: return "star"
        case .accepted: return "checkmark.circle"
        case .pending: return "clock"
        case .declined: return "xmark.circle"
        }
    }

    var body: some View {
        let mood = WK.Mood(palette: model.palette)
        WKImmersiveScaffold(
            mood: mood,
            primary: WKImmersiveAction(
                title: model.primaryTitle,
                systemImage: model.primary == .vote ? "checkmark.circle" : "arrow.right",
                action: onPrimary
            ),
            onClose: onClose
        ) {
            if let artwork {
                artwork
                    .frame(maxWidth: .infinity)
                    .frame(height: WK.Size.immersiveMedia)
                    .clipShape(WK.shape(WK.Radius.lg))
                    .accessibilityHidden(true)
            }
            Text(model.caption)
                .font(WK.Typo.headline)
                .foregroundStyle(mood.textSecondary)
            Text(model.title)
                .font(WK.Typo.display)
                .foregroundStyle(mood.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
                .wkAccessibilityID(Self.titleAccessibilityID)
            if let when = model.when {
                Text(when)
                    .font(WK.Typo.title)
                    .foregroundStyle(mood.textPrimary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            VStack(alignment: .leading, spacing: WK.Space.xs) {
                if let participants = model.participants {
                    WKImmersivePill(text: participants, systemImage: "person.2", mood: mood)
                }
                WKImmersivePill(text: model.response, systemImage: Self.responseSymbol(model.responseState), mood: mood)
            }
        }
        .wkAccessibilityID(Self.accessibilityID)
    }
}

/// Charge l'invitation reçue et aiguille ses actions ; « Voir l'événement » et « Voter » quittent l'invitation.
struct InvitationLandingContainer: View {
    @StateObject private var viewModel: InvitationLandingViewModel
    let artwork: AnyView?
    let onViewEvent: () -> Void
    let onVote: () -> Void
    let onClose: () -> Void

    init(
        eventId: String,
        userId: String,
        isLocalGuest: Bool,
        artwork: AnyView?,
        onViewEvent: @escaping () -> Void,
        onVote: @escaping () -> Void,
        onClose: @escaping () -> Void
    ) {
        _viewModel = StateObject(wrappedValue: InvitationLandingViewModel(
            eventId: eventId, viewerId: userId, isLocalGuest: isLocalGuest, source: SharedInvitationLandingSource()
        ))
        self.artwork = artwork
        self.onViewEvent = onViewEvent
        self.onVote = onVote
        self.onClose = onClose
    }

    var body: some View {
        Group {
            switch viewModel.state {
            case .loaded:
                if let model = viewModel.model {
                    InvitationLandingView(
                        model: model,
                        artwork: artwork,
                        onPrimary: { model.primary == .vote ? onVote() : onViewEvent() },
                        onClose: onClose
                    )
                }
            case .loading, .failed:
                placeholder
            }
        }
        // Mode immersif toujours sombre, comme le jour J : barre d'état claire sur le fond teinté.
        .preferredColorScheme(.dark)
        .task { await viewModel.reload() }
    }

    /// Chargement ou échec : ambiance par défaut ; en cas d'échec, l'événement reste accessible.
    private var placeholder: some View {
        let mood = WK.Mood(palette: .weekend)
        let failed = viewModel.state == .failed
        return WKImmersiveScaffold(
            mood: mood,
            primary: failed ? WKImmersiveAction(title: String(localized: "immersive.view_event"), action: onViewEvent) : nil,
            onClose: onClose
        ) {
            if failed {
                Text(String(localized: "common.error_generic"))
                    .font(WK.Typo.body)
                    .foregroundStyle(mood.textPrimary)
            } else {
                ProgressView()
                    .tint(mood.textPrimary)
                    .frame(maxWidth: .infinity)
                    .padding(.top, WK.Space.xl)
                    .accessibilityLabel(String(localized: "common.loading"))
            }
        }
    }
}
