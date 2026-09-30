import Foundation

/// Cycle de vie pur des sheets de modules du hub (couche 5a, #47).
///
/// - La sheet est liée à l'événement dont le hub l'a ouverte.
/// - Le repli « Plein écran » / « Commentaires » n'est appliqué qu'à la fermeture effective, et seulement
///   si le hub de ce même événement est toujours affiché.
/// - Toute navigation ailleurs (lien profond, bascule du flag, changement de zone, autre événement, retour)
///   ferme la sheet sans repli.
/// - Une présentation du routeur (profil, réglages) demandée pendant la fermeture attend la fin de celle-ci :
///   la présenter pendant que la sheet du hub se ferme échouerait.
struct HubSheetLifecycle: Equatable {
    struct Presented: Equatable, Identifiable {
        let module: HubModule
        let eventId: String
        /// Clé de `.sheet(item:)` : un autre événement ou un autre module recrée la sheet.
        var id: String { "\(eventId)/\(module.rawValue)" }
    }

    struct Fallback: Equatable {
        let eventId: String
        let view: AppView
    }

    enum Effect: Equatable {
        /// Écran legacy demandé depuis la sheet.
        case show(AppView)
        /// Fermeture simple : le hub relit ses résumés.
        case reloadHub
        /// Présentation du routeur différée jusqu'à la fermeture.
        case presentRouter(AppRouter.Presentation)
    }

    private(set) var presented: Presented?
    private(set) var pendingFallback: Fallback?
    private(set) var deferredPresentation: AppRouter.Presentation?
    /// Sheet retirée mais `onDismiss` pas encore reçu (animation de fermeture).
    private(set) var isClosing = false

    mutating func present(_ module: HubModule, eventId: String) {
        presented = Presented(module: module, eventId: eventId)
        pendingFallback = nil
        deferredPresentation = nil
        isClosing = false
    }

    /// « Fermer », glissement vers le bas.
    mutating func close() {
        guard presented != nil else { return }
        presented = nil
        isClosing = true
    }

    /// « Plein écran » / « Commentaires » : repli mémorisé avec l'événement de la sheet, appliqué après fermeture.
    mutating func requestFallback(_ view: AppView) {
        guard let presented else { return }
        pendingFallback = Fallback(eventId: presented.eventId, view: view)
        close()
    }

    /// Navigation ailleurs : ferme la sheet et oublie tout repli.
    /// - Returns: vrai si une sheet était affichée ou en cours de fermeture.
    @discardableResult
    mutating func dismiss() -> Bool {
        let wasShown = presented != nil || isClosing
        pendingFallback = nil
        close()
        return wasShown
    }

    /// L'événement sélectionné a changé : la sheet d'un autre événement se ferme sans repli.
    @discardableResult
    mutating func selectedEventChanged(to eventId: String?) -> Bool {
        let owner = presented?.eventId ?? pendingFallback?.eventId
        guard let owner, owner != eventId else { return false }
        return dismiss()
    }

    /// Présentation du routeur demandée : immédiate, ou différée si une sheet se ferme.
    /// - Returns: la présentation à appliquer maintenant.
    mutating func routerPresentation(_ requested: AppRouter.Presentation?) -> AppRouter.Presentation? {
        guard isClosing, let requested else { return requested }
        deferredPresentation = requested
        return nil
    }

    /// Le hub qui porte la sheet quitte l'écran : `onDismiss` peut ne jamais arriver, on repart d'un état vide.
    /// - Returns: la présentation du routeur qui attendait la fermeture, à appliquer maintenant.
    mutating func hostRemoved() -> AppRouter.Presentation? {
        let deferred = deferredPresentation
        self = HubSheetLifecycle()
        return deferred
    }

    /// `onDismiss` de la sheet.
    mutating func didDismiss(currentView: AppView, selectedEventId: String?) -> [Effect] {
        var effects: [Effect] = []
        if let fallback = pendingFallback {
            if currentView == .eventDetail, selectedEventId == fallback.eventId {
                effects.append(.show(fallback.view))
            }
        } else if currentView == .eventDetail {
            effects.append(.reloadHub)
        }
        if let deferredPresentation {
            effects.append(.presentRouter(deferredPresentation))
        }
        pendingFallback = nil
        deferredPresentation = nil
        isClosing = false
        return effects
    }
}
