import Shared
import SwiftUI

/// Hub d'un événement (couche 4, #47) : hero teinté, tuiles de modules
/// filtrées par statut, vote rapide et action principale. Les tuiles ouvrent les écrans existants.
struct EventHubView: View {
    enum MenuAction: Equatable { case info, addParticipants, report, support }

    @ObservedObject var viewModel: EventHubViewModel
    /// Échec de la dernière transition de cycle de vie, affiché sous l'action principale.
    let lifecycleError: String?
    let lifecycleInFlight: Bool
    let onBack: () -> Void
    let onOpenModule: (HubModule) -> Void
    /// Actions principales de navigation (vote, résultats, date, ajout de dates).
    let onPrimary: (EventHubModel.Primary) -> Void
    /// Transition confirmée par l'organisateur (confirmé → organisation, organisation → finalisé).
    let onLifecycle: (EventLifecycleTransitionController.Target) -> Void
    let onRequestSignIn: () -> Void
    let onOpenInfo: () -> Void
    let onAddParticipants: () -> Void
    /// Rollout invitation (`iosInvitationExperienceV1`) : sans lui, les infos de l'événement n'existent pas.
    let invitationRollout: Bool
    /// Appelé avant chaque rechargement (tirer pour actualiser, réessayer) : efface l'erreur de transition.
    var onWillReload: () -> Void = {}
    /// Bannière « C'est aujourd'hui » (couche 8) : ouvre le jour J.
    var onOpenEventDay: () -> Void = {}

    @Environment(\.openURL) private var openURL
    @State private var showsMenu = false
    @State private var confirmationTarget: EventLifecycleTransitionController.Target?
    @State private var showsConfirmation = false
    @State private var showsGuestSignIn = false
    @State private var moderationTarget: ModerationActionTarget?

    /// Même adresse que le menu du détail legacy (`canvasMenuActions`).
    static let supportURL = URL(string: "mailto:support@wakeve.app?subject=Wakeve%20abuse%20report")

    // MARK: - Règles de présentation (testées)

    static func columnCount(for size: DynamicTypeSize) -> Int { size.isAccessibilitySize ? 1 : 2 }

    static func systemImage(for module: HubModule) -> String {
        switch module {
        case .date: return "calendar"
        case .location: return "mappin.and.ellipse"
        case .participants: return "person.2"
        case .budget: return "eurosign.circle"
        case .scenarios: return "square.stack"
        case .transport: return "car"
        case .accommodation: return "bed.double"
        case .meals: return "fork.knife"
        case .equipment: return "backpack"
        case .activities: return "figure.hiking"
        case .meetings: return "video"
        case .recap: return "checkmark.seal"
        case .photos: return "photo.on.rectangle"
        case .payments: return "creditcard"
        }
    }

    static func moduleTitle(_ module: HubModule, locale: Locale = WK.appLocale) -> String {
        WK.localizedFormat("hub.module.\(module.rawValue)", locale: locale)
    }

    /// Tuile verrouillée : « À confirmer d'abord » ; sinon le résumé de la source, ou l'indice statique.
    static func tileSummary(_ tile: EventHubModel.Tile, facts: EventHubFacts, locale: Locale = WK.appLocale) -> String {
        if tile.isLocked { return WK.localizedFormat("hub.tile.locked", locale: locale) }
        return facts.summaries[tile.module] ?? WK.localizedFormat(hintKey(for: tile.module), locale: locale)
    }

    /// Indice statique d'une tuile sans résumé ; l'après-événement n'a plus rien « à préparer ».
    static func hintKey(for module: HubModule) -> String {
        switch module {
        case .recap: return "hub.tile.recap_hint"
        case .photos: return "hub.tile.photos_hint"
        default: return "hub.tile.hint"
        }
    }

    /// « 3 créneaux · 4 invités », ou la date retenue une fois le sondage terminé.
    static func heroSummary(for facts: EventHubFacts, locale: Locale = WK.appLocale) -> String? {
        let when: String?
        if let finalDate = facts.finalDate, facts.phase != .draft, facts.phase != .polling {
            when = HomeDateText.short(finalDate, locale: locale)
        } else {
            when = facts.slotCount > 0 ? HubSummaryText.slots(facts.slotCount, locale: locale) : nil
        }
        let guestCount = facts.confirmedCount + facts.pendingCount
        let guests = guestCount > 0 ? HubSummaryText.plural("hub.guests_count", guestCount, locale: locale) : nil
        switch (when, guests) {
        case let (when?, guests?):
            return String(format: WK.localizedFormat("hub.summary.format", locale: locale), when, guests)
        case let (when?, nil): return when
        case let (nil, guests?): return guests
        case (nil, nil): return nil
        }
    }

    static func primaryTitle(for primary: EventHubModel.Primary, facts: EventHubFacts, locale: Locale = WK.appLocale) -> String? {
        func text(_ key: String) -> String { WK.localizedFormat(key, locale: locale) }
        switch primary {
        case .vote: return text("hub.primary.vote")
        case .pollResults: return text("hub.primary.results")
        case .confirmDate:
            guard let slot = facts.leadingSlotStart else { return text("hub.primary.confirm_date") }
            return String(format: text("hub.primary.confirm_date_format"), HomeDateText.short(slot, locale: locale))
        case .organize: return text("hub.primary.organize")
        case .finalize: return text("hub.primary.finalize")
        case .signInToFinalize: return text("hub.primary.sign_in")
        case .addDates: return text("hub.primary.add_dates")
        case .none: return nil
        }
    }

    /// La tuile Date mène au vote quand il est attendu ; sinon l'écran du module (résultats, brouillon).
    static func opensVoting(_ module: HubModule, facts: EventHubFacts) -> Bool {
        module == .date && facts.voteRequired
    }

    /// Les transitions de cycle de vie passent toujours par une confirmation.
    static func lifecycleTarget(for primary: EventHubModel.Primary) -> EventLifecycleTransitionController.Target? {
        switch primary {
        case .organize: return .organizing
        case .finalize: return .finalized
        default: return nil
        }
    }

    /// Titre de la confirmation ; la cible reste posée après la fermeture pour que le titre ne bascule pas.
    static func confirmationTitleKey(for target: EventLifecycleTransitionController.Target?) -> String {
        target == .finalized ? "event.lifecycle.finalize.title" : "event.lifecycle.organizing.title"
    }

    /// Menu « … » : infos (qui portent quitter/supprimer, rollout invitation seulement : sinon l'écran
    /// retombe sur le hub), ajout de participants pour l'organisateur tant que l'événement n'est pas finalisé
    /// (règle du détail legacy), signalement pour les autres, support.
    static func menuActions(for facts: EventHubFacts, invitationRollout: Bool) -> [MenuAction] {
        var actions: [MenuAction] = invitationRollout ? [.info] : []
        if facts.isOrganizer && facts.phase != .finalized { actions.append(.addParticipants) }
        if !facts.isOrganizer { actions.append(.report) }
        actions.append(.support)
        return actions
    }

    // MARK: - Corps

    var body: some View {
        ScrollView {
            Group {
                switch viewModel.state {
                case .loading:
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .padding(.top, WK.Space.xl)
                        .accessibilityLabel(String(localized: "common.loading"))
                case .failed:
                    failedState
                case .loaded:
                    if let facts = viewModel.facts, let model = viewModel.model {
                        // Jour J recalculé chaque minute : la bannière apparaît à minuit (fuseau du créneau) et
                        // disparaît le lendemain sans recharger le hub, y compris au retour au premier plan.
                        TimelineView(.everyMinute) { context in
                            EventHubContent(
                                facts: facts,
                                model: model,
                                onOpenModule: { module in
                                    if Self.opensVoting(module, facts: facts) {
                                        onPrimary(.vote)
                                    } else {
                                        onOpenModule(module)
                                    }
                                },
                                onQuickVote: { onPrimary(.vote) },
                                isEventDay: facts.isEventDay(now: context.date, invitationRollout: invitationRollout),
                                onOpenEventDay: onOpenEventDay
                            )
                        }
                    }
                }
            }
            .padding(.horizontal, WK.Space.screen)
            .padding(.vertical, WK.Space.md)
        }
        .background(WK.Colors.canvas.ignoresSafeArea())
        .safeAreaInset(edge: .top, spacing: 0) { topBar }
        .safeAreaInset(edge: .bottom, spacing: 0) { primaryBar }
        .refreshable { await reload() }
        .task { await reload() }
        .confirmationDialog(
            String(localized: "hub.menu.more"),
            isPresented: $showsMenu,
            titleVisibility: .hidden
        ) {
            if let facts = viewModel.facts {
                ForEach(Self.menuActions(for: facts, invitationRollout: invitationRollout), id: \.self) { action in
                    menuButton(action, facts: facts)
                }
            }
            Button(String(localized: "common.cancel"), role: .cancel) {}
        }
        .confirmationDialog(
            String(localized: String.LocalizationValue(Self.confirmationTitleKey(for: confirmationTarget))),
            isPresented: $showsConfirmation,
            titleVisibility: .visible,
            presenting: confirmationTarget
        ) { target in
            Button(String(localized: target == .finalized
                ? "event.lifecycle.finalize.action"
                : "event.lifecycle.organizing.action")) {
                onLifecycle(target)
            }
            Button(String(localized: "common.cancel"), role: .cancel) {}
        } message: { target in
            Text(String(localized: target == .finalized
                ? "event.lifecycle.finalize.confirm_message"
                : "event.lifecycle.organizing.confirm_message"))
        }
        .confirmationDialog(
            String(localized: "event.lifecycle.guest.title"),
            isPresented: $showsGuestSignIn,
            titleVisibility: .visible
        ) {
            Button(String(localized: "event.lifecycle.guest.action"), action: onRequestSignIn)
            Button(String(localized: "common.cancel"), role: .cancel) {}
        } message: {
            Text(String(localized: "event.lifecycle.guest.confirm_message"))
        }
        .sheet(item: $moderationTarget) { target in
            ModerationActionSheet(target: target)
        }
    }

    private func reload() async {
        onWillReload()
        await viewModel.reload()
    }

    private var topBar: some View {
        HStack {
            WKCircleButton(
                systemImage: "chevron.left",
                accessibilityLabel: String(localized: "common.back"),
                accessibilityID: "hub.back",
                action: onBack
            )
            Spacer()
            if viewModel.facts != nil {
                WKCircleButton(
                    systemImage: "ellipsis",
                    accessibilityLabel: String(localized: "hub.menu.more"),
                    accessibilityID: "hub.more",
                    action: { showsMenu = true }
                )
            }
        }
        .padding(.horizontal, WK.Space.screen)
        .padding(.vertical, WK.Space.xxs)
        // Le contenu défile sous la barre : fond opaque, comme `EventHubPrimaryBar`.
        .background(WK.Colors.canvas.ignoresSafeArea(edges: .top))
    }

    @ViewBuilder
    private var primaryBar: some View {
        if viewModel.state == .loaded,
           let facts = viewModel.facts,
           let model = viewModel.model,
           let title = Self.primaryTitle(for: model.primary, facts: facts) {
            EventHubPrimaryBar(
                title: title,
                errorMessage: lifecycleError,
                isBusy: lifecycleInFlight,
                action: { handlePrimary(model.primary) }
            )
        }
    }

    private var failedState: some View {
        WKCard(padding: WK.Space.md, radius: WK.Radius.lg) {
            Text(String(localized: "common.error_generic"))
                .font(WK.Typo.body)
                .foregroundStyle(WK.Colors.textPrimary)
            WKChip(
                title: String(localized: "common.retry"),
                systemImage: "arrow.clockwise",
                accessibilityID: "hub.retry",
                action: { Task { await reload() } }
            )
        }
    }

    private func handlePrimary(_ primary: EventHubModel.Primary) {
        if let target = Self.lifecycleTarget(for: primary) {
            confirmationTarget = target
            showsConfirmation = true
        } else if primary == .signInToFinalize {
            showsGuestSignIn = true
        } else {
            onPrimary(primary)
        }
    }

    @ViewBuilder
    private func menuButton(_ action: MenuAction, facts: EventHubFacts) -> some View {
        switch action {
        case .info:
            Button(String(localized: "hub.menu.info"), action: onOpenInfo)
        case .addParticipants:
            Button(String(localized: "event.detail.menu.add_participants"), action: onAddParticipants)
        case .report:
            Button(String(localized: "moderation.report_event")) {
                moderationTarget = ModerationActionTarget(
                    type: .event,
                    targetId: facts.id,
                    eventId: facts.id,
                    authorId: facts.organizerId,
                    displayName: String(localized: "moderation.report_event_context"),
                    allowsBlock: false
                )
            }
        case .support:
            Button(String(localized: "moderation.contact_support")) {
                if let url = Self.supportURL { openURL(url) }
            }
        }
    }
}

/// Contenu défilant du hub (hero, vote rapide, tuiles), sans défilement propre pour être mesurable.
struct EventHubContent: View {
    let facts: EventHubFacts
    let model: EventHubModel
    let onOpenModule: (HubModule) -> Void
    let onQuickVote: () -> Void
    /// Jour J (`EventHubFacts.isEventDay`) : bannière « C'est aujourd'hui » dans le hero.
    var isEventDay: Bool = false
    var onOpenEventDay: () -> Void = {}

    static let eventDayAccessibilityID = "hub.eventDay"

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private var columns: [GridItem] {
        Array(
            repeating: GridItem(.flexible(), spacing: WK.Space.sm, alignment: .top),
            count: EventHubView.columnCount(for: dynamicTypeSize)
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: WK.Space.md) {
            hero
            if model.showsQuickVote, let slot = facts.leadingSlotStart {
                quickVote(slot)
            }
            LazyVGrid(columns: columns, spacing: WK.Space.sm) {
                ForEach(model.tiles, id: \.module) { tile in
                    tileView(tile)
                }
            }
        }
    }

    private var hero: some View {
        WKCard(
            padding: WK.Space.md,
            radius: WK.Radius.lg,
            tint: EventMoodPalette.palette(for: facts.eventTypeName).primary(for: colorScheme)
        ) {
            WKStatusPill(text: String(localized: String.LocalizationValue(model.statusKey)), status: model.status)
            Text(facts.title)
                .font(WK.Typo.title)
                .foregroundStyle(WK.Colors.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
                .accessibilityAddTraits(.isHeader)
            if let summary = EventHubView.heroSummary(for: facts) {
                // Carte teintée : texte principal pour garder le contraste.
                Text(summary)
                    .font(WK.Typo.body)
                    .foregroundStyle(WK.Colors.textPrimary)
            }
            let avatars = facts.participantNames.enumerated().map { index, name in
                WKAvatar(id: "\(facts.id)-\(index)", name: name)
            }
            if !avatars.isEmpty {
                WKAvatarStack(avatars: avatars)
            }
            if isEventDay {
                WKChip(
                    title: String(localized: "immersive.event_day.banner"),
                    systemImage: "sun.max.fill",
                    style: .prominent,
                    accessibilityID: Self.eventDayAccessibilityID,
                    action: onOpenEventDay
                )
                .accessibilityHint(String(localized: "immersive.event_day.banner_hint"))
            }
        }
        .wkAccessibilityID("hub.hero")
    }

    private func quickVote(_ slot: Date) -> some View {
        WKCard(style: .inset, padding: WK.Space.md) {
            Text(String(format: String(localized: "hub.quick_vote.title_format"), HomeDateText.short(slot)))
                .font(WK.Typo.headline)
                .foregroundStyle(WK.Colors.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
            EventHubQuickVoteAnswers(onVote: onQuickVote)
        }
        .wkAccessibilityID("hub.quickVote")
    }

    @ViewBuilder
    private func tileView(_ tile: EventHubModel.Tile) -> some View {
        let base = WKModuleTile(
            systemImage: EventHubView.systemImage(for: tile.module),
            title: EventHubView.moduleTitle(tile.module),
            summary: EventHubView.tileSummary(tile, facts: facts),
            status: tile.isLocked ? nil : tile.status,
            isHighlighted: tile.isHighlighted,
            accessibilityID: "hub.tile.\(tile.module.rawValue)",
            action: {
                guard !tile.isLocked else { return }
                onOpenModule(tile.module)
            }
        )
        .disabled(tile.isLocked)
        if tile.isLocked {
            // Non actionnable : lue « Budget, À confirmer d'abord », sans trait de bouton.
            base.accessibilityRemoveTraits(.isButton)
        } else {
            base
        }
    }
}

/// Réponses du vote rapide : en ligne, empilées aux tailles d'accessibilité.
/// Les trois réponses ouvrent l'écran de vote : un bulletin se remplit en entier là-bas.
struct EventHubQuickVoteAnswers: View {
    let onVote: () -> Void

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    var body: some View {
        let layout = dynamicTypeSize.isAccessibilitySize
            ? AnyLayout(VStackLayout(alignment: .leading, spacing: WK.Space.xs))
            : AnyLayout(HStackLayout(spacing: WK.Space.xs))
        layout {
            WKChip(title: String(localized: "poll.yes"), accessibilityID: "hub.quickVote.yes", action: onVote)
                .accessibilityHint(String(localized: "hub.quick_vote.hint"))
            WKChip(title: String(localized: "poll.maybe"), accessibilityID: "hub.quickVote.maybe", action: onVote)
                .accessibilityHint(String(localized: "hub.quick_vote.hint"))
            WKChip(title: String(localized: "poll.no"), accessibilityID: "hub.quickVote.no", action: onVote)
                .accessibilityHint(String(localized: "hub.quick_vote.hint"))
        }
    }
}

/// Action principale collée en bas, avec l'éventuelle erreur de transition au-dessus.
struct EventHubPrimaryBar: View {
    let title: String
    let errorMessage: String?
    let isBusy: Bool
    let action: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: WK.Space.xs) {
            if let errorMessage {
                // Icône d'avertissement colorée ; texte principal pour garder le contraste.
                Label {
                    Text(errorMessage)
                        .foregroundStyle(WK.Colors.textPrimary)
                        .fixedSize(horizontal: false, vertical: true)
                } icon: {
                    Image(systemName: "exclamationmark.triangle.fill")
                        .foregroundStyle(WK.Status.actionNeeded.color)
                        .accessibilityHidden(true)
                }
                .font(WK.Typo.caption)
                .wkAccessibilityID("eventLifecycleError")
            }
            WKPrimaryButton(title: title, accessibilityID: "hub.primary", isLoading: isBusy, action: action)
                .disabled(isBusy)
        }
        .padding(.horizontal, WK.Space.screen)
        .padding(.vertical, WK.Space.xs)
        .background(WK.Colors.canvas.ignoresSafeArea(edges: .bottom))
        .onChange(of: errorMessage) { _, message in
            // L'erreur apparaît hors du focus VoiceOver : l'annoncer.
            if let message { AccessibilityNotification.Announcement(message).post() }
        }
    }
}

/// Possède le modèle de vue du hub et le contrôleur de cycle de vie ; recharge quand `reloadToken` change.
struct EventHubContainer: View {
    @StateObject private var viewModel: EventHubViewModel
    @State private var lifecycleController: EventLifecycleTransitionController?
    @State private var lifecycleError: String?
    @State private var lifecycleInFlight = false

    private let eventId: String
    private let userId: String
    private let repository: EventRepositoryInterface
    let reloadToken: Int
    let onBack: () -> Void
    let onOpenModule: (HubModule) -> Void
    let onPrimary: (EventHubModel.Primary) -> Void
    /// Appelé après une transition réussie (l'appelant relit l'événement et fait avancer `reloadToken`).
    let onLifecycleChanged: () -> Void
    /// Chaque chargement réussi : l'appelant garde son événement sélectionné à jour.
    let onLoaded: (EventHubFacts) -> Void
    let onRequestSignIn: () -> Void
    let onOpenInfo: () -> Void
    let onAddParticipants: () -> Void
    let invitationRollout: Bool
    let onOpenEventDay: () -> Void

    init(
        eventId: String,
        userId: String,
        isLocalGuest: Bool,
        repository: EventRepositoryInterface,
        reloadToken: Int,
        onBack: @escaping () -> Void,
        onOpenModule: @escaping (HubModule) -> Void,
        onPrimary: @escaping (EventHubModel.Primary) -> Void,
        onLifecycleChanged: @escaping () -> Void,
        onLoaded: @escaping (EventHubFacts) -> Void,
        onRequestSignIn: @escaping () -> Void,
        onOpenInfo: @escaping () -> Void,
        onAddParticipants: @escaping () -> Void,
        invitationRollout: Bool,
        onOpenEventDay: @escaping () -> Void = {}
    ) {
        _viewModel = StateObject(wrappedValue: EventHubViewModel(
            eventId: eventId, viewerId: userId, isLocalGuest: isLocalGuest, source: SharedEventHubSource()
        ))
        self.eventId = eventId
        self.userId = userId
        self.repository = repository
        self.reloadToken = reloadToken
        self.onBack = onBack
        self.onOpenModule = onOpenModule
        self.onPrimary = onPrimary
        self.onLifecycleChanged = onLifecycleChanged
        self.onLoaded = onLoaded
        self.onRequestSignIn = onRequestSignIn
        self.onOpenInfo = onOpenInfo
        self.onAddParticipants = onAddParticipants
        self.invitationRollout = invitationRollout
        self.onOpenEventDay = onOpenEventDay
    }

    var body: some View {
        EventHubView(
            viewModel: viewModel,
            lifecycleError: lifecycleError,
            lifecycleInFlight: lifecycleInFlight,
            onBack: onBack,
            onOpenModule: onOpenModule,
            onPrimary: onPrimary,
            onLifecycle: performLifecycleTransition,
            onRequestSignIn: onRequestSignIn,
            onOpenInfo: onOpenInfo,
            onAddParticipants: onAddParticipants,
            invitationRollout: invitationRollout,
            onWillReload: { lifecycleError = nil },
            onOpenEventDay: onOpenEventDay
        )
        .onChange(of: reloadToken) { _, _ in
            lifecycleError = nil
            Task { await viewModel.reload() }
        }
        .onChange(of: viewModel.facts) { _, facts in
            if let facts { onLoaded(facts) }
        }
    }

    /// Même chemin que `EventDetailView.performLifecycleTransition` : la machine à états reste seule
    /// propriétaire de l'écriture du statut.
    private func performLifecycleTransition(to target: EventLifecycleTransitionController.Target) {
        guard !lifecycleInFlight else { return }
        lifecycleError = nil
        lifecycleInFlight = true
        let controller = lifecycleController ?? EventLifecycleTransitionController(
            eventId: eventId,
            userId: userId,
            repository: repository
        )
        lifecycleController = controller
        controller.transition(to: target) { outcome in
            lifecycleInFlight = false
            switch outcome {
            case .transitioned:
                WakeveHaptics.success()
                onLifecycleChanged()
            case .failed(let message):
                lifecycleError = message
            }
        }
    }
}
