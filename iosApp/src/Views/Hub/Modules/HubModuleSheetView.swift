import SwiftUI

/// Charge le contenu d'une sheet de module (couche 5a, #47) ; seul le dernier chargement publie.
@MainActor
final class HubModuleSheetViewModel: ObservableObject {
    enum State: Equatable { case loading, loaded, failed }

    @Published private(set) var state: State = .loading
    @Published private(set) var data: HubModuleSheetData?

    let module: HubModule
    private let eventId: String
    private let viewerId: String
    private let source: EventModuleSheetSource
    private var generation = 0
    /// Repas ajoutés par `MealFormSheet`, qui n'écrit pas en base : ils restent affichés comme dans l'écran legacy.
    private var localMeals: [HubModuleSheetRaw.Meal] = []

    init(module: HubModule, eventId: String, viewerId: String, source: EventModuleSheetSource) {
        self.module = module
        self.eventId = eventId
        self.viewerId = viewerId
        self.source = source
    }

    func reload() async {
        generation += 1
        let token = generation
        do {
            let loaded = try await source.load(module: module, eventId: eventId, viewerId: viewerId)
            guard token == generation else { return }
            data = localMeals.reduce(loaded) { $0.appendingMeal($1) }
            state = .loaded
        } catch {
            // Chargement annulé (sheet fermée) : on garde l'état courant.
            guard token == generation, !(error is CancellationError) else { return }
            // Données déjà affichées : on les garde plutôt que d'afficher un échec.
            state = data == nil ? .failed : .loaded
        }
    }

    func addMeal(_ meal: HubModuleSheetRaw.Meal) {
        localMeals.append(meal)
        data = data?.appendingMeal(meal)
    }
}

/// Sheet d'un module du hub : possède le modèle de vue et le formulaire d'ajout de repas existant.
struct HubModuleSheetView: View {
    enum SecondaryAction: Equatable { case fullScreen, comments, tricount }

    @StateObject private var viewModel: HubModuleSheetViewModel
    @State private var showsMealForm = false
    /// Participants lus à l'ouverture du formulaire, pas à chaque rendu.
    @State private var formParticipants: [ParticipantModel] = []

    private let eventId: String
    /// Organisateur d'un événement non finalisé : action principale d'écriture affichée dès la première image.
    private let canAddHint: Bool
    /// Mêmes participants que le formulaire legacy (`participantModels(for:)`), lus à la demande.
    private let mealParticipants: () -> [ParticipantModel]
    let onClose: () -> Void
    let onOpenFullScreen: () -> Void
    let onOpenComments: () -> Void
    /// Écran legacy demandé par une action de la sheet (« Voir les dépenses », « Tricount »…).
    let onOpenScreen: (AppView) -> Void

    init(
        module: HubModule,
        eventId: String,
        viewerId: String,
        source: EventModuleSheetSource,
        canAddHint: Bool,
        mealParticipants: @escaping () -> [ParticipantModel],
        onClose: @escaping () -> Void,
        onOpenFullScreen: @escaping () -> Void,
        onOpenComments: @escaping () -> Void,
        onOpenScreen: @escaping (AppView) -> Void
    ) {
        _viewModel = StateObject(wrappedValue: HubModuleSheetViewModel(
            module: module, eventId: eventId, viewerId: viewerId, source: source
        ))
        self.eventId = eventId
        self.canAddHint = canAddHint
        self.mealParticipants = mealParticipants
        self.onClose = onClose
        self.onOpenFullScreen = onOpenFullScreen
        self.onOpenComments = onOpenComments
        self.onOpenScreen = onOpenScreen
    }

    // MARK: - Règles de présentation (testées)

    /// « Plein écran » partout, sauf le budget dont l'action principale ouvre déjà l'écran plein ;
    /// « Commentaires » là où l'écran legacy en a ; « Tricount » pour la cagnotte.
    static func secondaryActions(for module: HubModule) -> [SecondaryAction] {
        secondaryActions(for: module, primary: nil)
    }

    /// Invités : « Plein écran » seulement sans « Inviter », qui ouvre déjà le même écran.
    static func secondaryActions(for module: HubModule, primary: HubModuleSheetData.Primary?) -> [SecondaryAction] {
        if module == .participants { return primary == nil ? [.fullScreen] : [] }
        if module == .payments { return [.tricount, .fullScreen] }
        // Budget et transport : l'action principale ouvre déjà l'écran plein.
        if module == .budget { return [] }
        if module == .transport { return [.comments] }
        return EventHubRouting.commentSection(for: module) == nil ? [.fullScreen] : [.fullScreen, .comments]
    }

    /// Écran legacy d'une action secondaire propre ; « Plein écran » et « Commentaires » ont leur propre repli.
    static func fallback(for secondary: SecondaryAction) -> AppView? {
        secondary == .tricount ? .tricount : nil
    }

    static func title(for primary: HubModuleSheetData.Primary, locale: Locale = WK.appLocale) -> String {
        let key: String
        switch primary {
        case .addMeal: key = "hub.sheet.meals.add"
        case .viewExpenses: key = "hub.sheet.budget.view"
        case .managePot: key = "hub.sheet.payments.manage_pot"
        case .planMeeting: key = "hub.sheet.meetings.plan"
        case .organizeTransport: key = "hub.sheet.transport.organize"
        case .invite: key = "hub.sheet.participants.invite"
        }
        return WK.localizedFormat(key, locale: locale)
    }

    /// Action principale des données chargées (`HubModuleSheetData.primaryAction`).
    static func primaryTitle(for data: HubModuleSheetData?, locale: Locale = WK.appLocale) -> String? {
        data?.primary.map { title(for: $0, locale: locale) }
    }

    /// Pendant le chargement, l'indice de l'appelant (organisateur d'un événement non finalisé) tient lieu de droits ;
    /// une fois chargées, les données font foi ; en échec, seule la relance est proposée.
    static func primaryAction(
        module: HubModule,
        state: HubModuleSheetViewModel.State,
        data: HubModuleSheetData?,
        canAddHint: Bool
    ) -> HubModuleSheetData.Primary? {
        switch state {
        case .loaded: return data?.primary
        case .failed: return nil
        case .loading:
            return HubModuleSheetData.primaryAction(for: module, isOrganizer: canAddHint, isReadOnly: !canAddHint)
        }
    }

    static func primaryTitle(
        module: HubModule,
        state: HubModuleSheetViewModel.State,
        data: HubModuleSheetData?,
        canAddHint: Bool,
        locale: Locale = WK.appLocale
    ) -> String? {
        primaryAction(module: module, state: state, data: data, canAddHint: canAddHint).map { title(for: $0, locale: locale) }
    }

    static func rawMeal(from meal: MealModel) -> HubModuleSheetRaw.Meal {
        HubModuleSheetRaw.Meal(
            id: meal.id,
            name: meal.name,
            date: meal.date,
            time: meal.time,
            servings: meal.servings,
            statusName: meal.status.rawValue,
            // Le formulaire identifie les responsables comme l'écran legacy (`userIdOrEmail`).
            responsibleNames: meal.responsibleParticipantIds
        )
    }

    // MARK: - Corps

    var body: some View {
        HubModuleSheetBody(
            viewModel: viewModel,
            canAddHint: canAddHint,
            onClose: onClose,
            onPrimary: { primary in
                if primary == .addMeal {
                    formParticipants = mealParticipants()
                    showsMealForm = true
                } else if primary == .invite {
                    // Même route que « Plein écran » des invités (sensible au flag invitations).
                    onOpenFullScreen()
                } else if let view = EventHubRouting.fallback(for: primary) {
                    onOpenScreen(view)
                }
            },
            onSecondary: { secondary in
                switch secondary {
                case .fullScreen: onOpenFullScreen()
                case .comments: onOpenComments()
                case .tricount: Self.fallback(for: .tricount).map(onOpenScreen)
                }
            }
        )
        .task(id: eventId) { await viewModel.reload() }
        .sheet(isPresented: $showsMealForm, onDismiss: {
            // Si le formulaire écrit un jour en base, la liste se met à jour ; les ajouts locaux restent.
            Task { await viewModel.reload() }
        }) {
            MealFormSheet(eventId: eventId, meal: nil, participants: formParticipants) { meal in
                viewModel.addMeal(Self.rawMeal(from: meal))
                showsMealForm = false
            }
        }
    }
}

/// Rendu de la sheet pour un état du modèle de vue (mesurable sans chargement).
struct HubModuleSheetBody: View {
    @ObservedObject var viewModel: HubModuleSheetViewModel
    let canAddHint: Bool
    let onClose: () -> Void
    let onPrimary: (HubModuleSheetData.Primary) -> Void
    let onSecondary: (HubModuleSheetView.SecondaryAction) -> Void

    private var module: HubModule { viewModel.module }

    var body: some View {
        let data = viewModel.state == .loaded ? viewModel.data : nil
        let primaryAction = HubModuleSheetView.primaryAction(
            module: module, state: viewModel.state, data: data, canAddHint: canAddHint
        )
        WKModuleSheet(
            title: EventHubView.moduleTitle(module),
            status: data?.status.map { .init(text: $0.text, status: $0.status) },
            missing: data?.missing,
            primary: primaryAction.map { primary in (title: HubModuleSheetView.title(for: primary), action: { onPrimary(primary) }) },
            secondary: secondaryItems(primary: primaryAction),
            onClose: onClose
        ) {
            switch viewModel.state {
            case .loading:
                ProgressView()
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, WK.Space.lg)
                    .accessibilityLabel(String(localized: "common.loading"))
            case .failed:
                failedState
            case .loaded:
                if let data {
                    ForEach(Array(data.items.enumerated()), id: \.element.id) { index, item in
                        if let section = item.sectionTitle, index == 0 || data.items[index - 1].sectionTitle != section {
                            Text(section)
                                .font(WK.Typo.headline)
                                .foregroundStyle(WK.Colors.textMuted)
                                .accessibilityAddTraits(.isHeader)
                                .padding(.top, index == 0 ? 0 : WK.Space.xs)
                                .frame(maxWidth: .infinity, alignment: .leading)
                        }
                        HubModuleSheetItemCard(item: item)
                    }
                    if data.pendingSync {
                        Label(String(localized: "organization.state.pending_sync"), systemImage: "arrow.triangle.2.circlepath")
                            .font(WK.Typo.caption)
                            .foregroundStyle(WK.Colors.textMuted)
                            .wkAccessibilityID("hub.sheet.pendingSync")
                    }
                }
            }
        }
        .accessibilityElement(children: .contain)
        .wkAccessibilityID("hub.sheet.\(module.rawValue)")
    }

    private func secondaryItems(primary: HubModuleSheetData.Primary?) -> [WKActionBar.Item] {
        HubModuleSheetView.secondaryActions(for: module, primary: primary).map { action in
            switch action {
            case .fullScreen:
                return WKActionBar.Item(
                    systemImage: "arrow.up.left.and.arrow.down.right",
                    label: String(localized: "hub.sheet.open_full"),
                    accessibilityID: "hub.sheet.openFull",
                    action: { onSecondary(.fullScreen) }
                )
            case .comments:
                return WKActionBar.Item(
                    systemImage: "bubble.left",
                    label: String(localized: "hub.sheet.comments"),
                    accessibilityID: "hub.sheet.comments",
                    action: { onSecondary(.comments) }
                )
            case .tricount:
                return WKActionBar.Item(
                    systemImage: "divide.circle",
                    label: String(localized: "tricount.title"),
                    accessibilityID: "hub.sheet.tricount",
                    action: { onSecondary(.tricount) }
                )
            }
        }
    }

    private var failedState: some View {
        WKCard(style: .inset, padding: WK.Space.md) {
            Text(String(localized: "common.error_generic"))
                .font(WK.Typo.body)
                .foregroundStyle(WK.Colors.textPrimary)
            WKChip(
                title: String(localized: "common.retry"),
                systemImage: "arrow.clockwise",
                accessibilityID: "hub.sheet.retry",
                action: { Task { await viewModel.reload() } }
            )
        }
    }
}

/// Une ligne du module : titre, détail, statut et personnes concernées.
struct HubModuleSheetItemCard: View {
    let item: HubModuleSheetItem

    var body: some View {
        WKCard(style: .inset) {
            Text(item.title)
                .font(WK.Typo.headline)
                .foregroundStyle(WK.Colors.textPrimary)
                .fixedSize(horizontal: false, vertical: true)
            if let detail = item.detail {
                Text(detail)
                    .font(WK.Typo.caption)
                    .foregroundStyle(WK.Colors.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if item.status == nil, let statusText = item.statusText {
                // État sans pastille (réunion terminée) : texte discret, jamais la couleur seule.
                Text(statusText)
                    .font(WK.Typo.caption)
                    .foregroundStyle(WK.Colors.textMuted)
                    .fixedSize(horizontal: false, vertical: true)
            }
            if item.status != nil || !item.assigneeNames.isEmpty {
                HStack(spacing: WK.Space.xs) {
                    if let status = item.status {
                        WKStatusPill(text: item.statusText ?? status.localizedName, status: status)
                    }
                    if !item.assigneeNames.isEmpty {
                        WKAvatarStack(avatars: item.assigneeNames.enumerated().map { index, name in
                            WKAvatar(id: "\(item.id)-\(index)-\(name)", name: name)
                        })
                    }
                }
            }
        }
        // Libellé explicite : les avatars seuls ne disent pas le rôle des personnes.
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(item.accessibilityLabel)
    }
}
