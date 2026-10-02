import SwiftUI
import Shared
#if canImport(UIKit)
import UIKit
#endif

// MARK: - UserDefaults Helpers

struct UserDefaultsKeys {
    static let hasCompletedOnboarding = "hasCompletedOnboarding"
    static let appearanceMode = "appearanceMode"
    static let legacyDarkMode = "darkMode"
    static let appearanceModeMigrated = "appearanceModeMigrated"
}

enum WakeveAppearancePreference: String, CaseIterable, Identifiable {
    case system
    case light
    case dark

    var id: String { rawValue }

    var colorScheme: ColorScheme? {
        switch self {
        case .system:
            return nil
        case .light:
            return .light
        case .dark:
            return .dark
        }
    }

    var localizedTitle: String {
        switch self {
        case .system:
            return String(localized: "profile.appearance.system")
        case .light:
            return String(localized: "profile.appearance.light")
        case .dark:
            return String(localized: "profile.appearance.dark")
        }
    }

    var localizedDescription: String {
        switch self {
        case .system:
            return String(localized: "profile.appearance.system_desc")
        case .light:
            return String(localized: "profile.light_mode_desc")
        case .dark:
            return String(localized: "profile.dark_mode_desc")
        }
    }
}

func hasCompletedOnboarding() -> Bool {
    return UserDefaults.standard.bool(forKey: UserDefaultsKeys.hasCompletedOnboarding)
}

func markOnboardingComplete() {
    UserDefaults.standard.set(true, forKey: UserDefaultsKeys.hasCompletedOnboarding)
}

struct ContentView: View {
    @EnvironmentObject var authStateManager: AuthStateManager
    @EnvironmentObject var authService: AuthenticationService
    @AppStorage(UserDefaultsKeys.appearanceMode) private var appearanceModeRaw = WakeveAppearancePreference.system.rawValue
    @AppStorage(UserDefaultsKeys.legacyDarkMode) private var legacyDarkMode = false
    @AppStorage(UserDefaultsKeys.appearanceModeMigrated) private var appearanceModeMigrated = false
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var hasOnboarded = false

    var body: some View {
        ZStack {
            if !authStateManager.hasCheckedAuthStatus {
                AuthLaunchLoadingView()
            } else if authStateManager.isAuthenticated {
                if let user = authStateManager.currentUser {
                    AuthenticatedView(userId: user.id)
                } else {
                    ErrorView(message: "Authentication error: no user data", onRetry: {
                        Task {
                            authStateManager.checkAuthStatus()
                        }
                    })
                }
            } else if !hasOnboarded {
                OnboardingView(onOnboardingComplete: completeOnboarding)
            } else {
                LoginView()
            }
        }
        .onAppear {
            migrateLegacyAppearancePreferenceIfNeeded()
            hasOnboarded = hasCompletedOnboarding()
        }
        .preferredColorScheme(appearancePreference.colorScheme)
    }

    private var appearancePreference: WakeveAppearancePreference {
        WakeveAppearancePreference(rawValue: appearanceModeRaw) ?? .system
    }

    private func migrateLegacyAppearancePreferenceIfNeeded() {
        guard !appearanceModeMigrated else { return }

        let defaults = UserDefaults.standard
        if defaults.object(forKey: UserDefaultsKeys.appearanceMode) == nil,
           defaults.object(forKey: UserDefaultsKeys.legacyDarkMode) != nil {
            appearanceModeRaw = legacyDarkMode
                ? WakeveAppearancePreference.dark.rawValue
                : WakeveAppearancePreference.light.rawValue
        }

        appearanceModeMigrated = true
    }

    private func completeOnboarding() {
        markOnboardingComplete()

        withAnimation(reduceMotion ? nil : .easeInOut(duration: 0.25)) {
            hasOnboarded = true
        }
    }
}

private struct AuthLaunchLoadingView: View {
    var body: some View {
        ZStack {
            WakeveScreenBackground(style: .utility)

            ProgressView()
                .tint(WakeveTheme.ColorToken.permissionBlue)
                .scaleEffect(1.2)
                .accessibilityLabel(String(localized: "common.loading"))
        }
        .ignoresSafeArea()
    }
}

// MARK: - Error View

struct ErrorView: View {
    let message: String
    let onRetry: () -> Void
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        ZStack {
            SemanticColor.appBackground(for: colorScheme)
                .ignoresSafeArea()

            VStack(spacing: 24) {
                Image(systemName: "exclamationmark.triangle")
                    .font(.system(size: 60))
                    .foregroundColor(SemanticColor.warning(for: colorScheme))

                Text(String(localized: "common.error_generic"))
                    .font(.title2)
                    .fontWeight(.semibold)
                    .foregroundColor(SemanticColor.primaryText(for: colorScheme))

                Text(message)
                    .font(.body)
                    .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 32)

                WakeveActionButton(
                    String(localized: "common.try_again"),
                    systemImage: "arrow.clockwise",
                    variant: .primary,
                    action: onRetry
                )
                .frame(maxWidth: 280)
            }
            .padding(WakeveTheme.Spacing.page)
        }
    }
}

// MARK: - Authenticated View

struct AuthenticatedView: View {
    let userId: String
    @AppStorage("iosInvitationExperienceV1") private var iosInvitationExperienceV1 = false
    @Environment(\.scenePhase) private var scenePhase
    @State private var redesignRouter = AppRouter()
    @State private var activityAtRoot = true
    @State private var activityReloadToken = 0
    /// Badge de la zone Activité de la refonte (couche 6, #47) : éléments « À traiter » (actions seulement).
    @State private var activityToDoCount = 0
    @State private var currentView: AppView = .eventList
    @State private var selectedEvent: Event?
    @State private var selectedCreationBaseRevision: Int64?
    @State private var selectedCreationArtwork: (any Artwork)?
    @State private var invitationStudioPreview: InvitationStudioPreview?
    @State private var invitationLandingEventId: String?
    @State private var pendingInformationDeleteEvent: Event?
    @State private var informationDeleteOwner: EventDetailViewModel?
    /// Rechargement de l'accueil de la refonte (couche 3, #47).
    @State private var eventsHomeReloadToken = 0
    /// Rechargement du hub d'événement de la refonte (couche 4, #47) après une transition de cycle de vie.
    @State private var eventHubReloadToken = 0
    /// Sheet de module du hub (couche 5a, #47) : module présenté et son événement, repli
    /// `(eventId, view)` appliqué après fermeture, présentation du routeur différée (`HubSheetLifecycle`).
    @State private var hubSheet = HubSheetLifecycle()
#if DEBUG
    @State private var invitationQALibraryIsSeedReady =
        !ProcessInfo.processInfo.arguments.contains(
            InvitationExperienceQALaunchSupport.seedArgument
        )
#endif
    // Use persistent database-backed repository instead of in-memory mock
    private let repository: EventRepositoryInterface = RepositoryProvider.shared.repository
    private let invitationExperienceProjectionRepository =
        DatabaseInvitationExperienceProjectionRepository(
            database: RepositoryProvider.shared.database
        )
    private let invitationExperienceRouter = InvitationExperienceRouter()
    private let invitationDeepLinkResolver = InvitationDeepLinkResolver()
    @StateObject private var directInviteProductionOwner = DirectInviteProductionOwner()
    /// Flux de création de la refonte (couche 7, #47) et brouillon à reprendre (nil : nouveau).
    @State private var showCreateEventFlow = false
    @State private var createFlowDraftId: String?
    /// Jour J de la refonte (couche 8, #47), présenté en plein écran depuis le hub ou l'accueil.
    @State private var eventDayPresentation: EventDayPresentation?
    
    // New state variables for PRD features
    @State private var showScenarioList = false
    @State private var showBudgetDetail = false
    @State private var showAccommodation = false
    @State private var showMealPlanning = false
    @State private var showEquipmentChecklist = false
    @State private var showActivityPlanning = false
    @State private var selectedMeetingId: String?
    @State private var selectedScenarioId: String?
    @State private var selectedCommentSection: CommentSectionType = .general
    @State private var selectedBudget: Budget_?
    @StateObject private var transportPlanningViewModel = TransportPlanningViewModel()

    // Get auth state from environment
    @EnvironmentObject var authStateManager: AuthStateManager
    @EnvironmentObject private var deepLinkService: DeepLinkService
    @Environment(\.openURL) private var openURL

    /// UI-only rollout control. Persisted aggregate writers, migrations and
    /// deletion fences remain installed regardless of this value.
    private var invitationExperienceRolloutEnabled: Bool {
        iosInvitationExperienceV1
    }

    var body: some View {
        // Shell de la refonte (zones Événements / Activité) ; les actions ponctuelles
        // comme la création restent contextuelles (＋, sheets).
        redesignChrome
        .fullScreenCover(isPresented: $showCreateEventFlow) {
            CreateEventFlow(
                userId: userId,
                draftEventId: createFlowDraftId,
                onClose: closeCreateEventFlow,
                onLaunched: { event, context in
                    finishCreateEventFlow(event, context: context)
                }
            )
        }
        .sheet(item: $invitationStudioPreview) { preview in
            InvitationStudioPreviewSheet(preview: preview)
        }
        .fullScreenCover(item: $eventDayPresentation) { presentation in
            EventDayContainer(
                eventId: presentation.id,
                userId: userId,
                onViewEvent: { viewEventFromEventDay(presentation.id) },
                onClose: { eventDayPresentation = nil }
            )
        }
        .confirmationDialog(
            String(localized: "common.delete"),
            isPresented: Binding(
                get: { pendingInformationDeleteEvent != nil },
                set: { isPresented in
                    if !isPresented { pendingInformationDeleteEvent = nil }
                }
            ),
            titleVisibility: .visible
        ) {
            Button(String(localized: "common.delete"), role: .destructive) {
                guard let event = pendingInformationDeleteEvent else { return }
                pendingInformationDeleteEvent = nil
                deleteInformationEventThroughOwner(event)
            }
            Button(String(localized: "common.cancel"), role: .cancel) {
                pendingInformationDeleteEvent = nil
            }
        }
        .onReceive(deepLinkService.$navigationRoute) { route in
            guard let route else { return }
            handleDeepLinkNavigation(route)
        }
#if DEBUG
        .task {
            await prepareInvitationExperienceQALaunch()
            openQAInvitationLandingIfRequested()
        }
#endif
        .onChange(of: selectedEvent?.id) { _, id in hubSheet.selectedEventChanged(to: id) }
        .onChange(of: currentView) { _, view in
            if view != .eventDetail { releaseHubSheetHost() }
        }
        // Invitation reçue (couche 8) : un marqueur que la navigation n'a pas affiché ne survit pas au détail.
        .onChange(of: currentView) { _, view in
            invitationLandingEventId = InvitationLandingRoute.marker(invitationLandingEventId, showingDetail: view == .eventDetail)
        }
    }

#if DEBUG
    private func prepareInvitationExperienceQALaunch() async {
        let arguments = ProcessInfo.processInfo.arguments
        if arguments.contains(InvitationExperienceQALaunchSupport.seedArgument) {
            // Repository-backed QA opts in explicitly; normal DEBUG launches
            // retain the same default-off behavior as production.
            iosInvitationExperienceV1 = true
        }
        let support = InvitationExperienceQALaunchSupport(
            database: RepositoryProvider.shared.database,
            eventRepository: RepositoryProvider.shared.databaseRepository
        )
        debugLog("[QALaunch] AuthenticatedView task firing userId=\(userId)")
        guard let route = await support.prepare(
            arguments: arguments,
            viewerId: userId
        ) else {
            debugLog("[QALaunch] prepare returned nil")
            return
        }
        debugLog("[QALaunch] resolved route \(route)")

        invitationQALibraryIsSeedReady = true
        invitationLandingEventId = nil
        switch route {
        case .library:
            selectedEvent = nil
            currentView = .eventList
            eventsHomeReloadToken += 1
        case .detail(let eventId):
            guard let event = repository.getEvent(id: eventId) else { return }
            routeInvitationExperience(
                InvitationExperienceRouteRequestCanvasAction(action: .showDetails),
                for: event
            )
        case .studio(let eventId):
            guard let event = repository.getEvent(id: eventId) else { return }
            selectedCreationBaseRevision = RepositoryProvider.shared.database.eventQueries
                .selectById(id: event.id)
                .executeAsOneOrNull()?
                .aggregateRevision
            selectedCreationArtwork = await invitationQAArtwork(for: event.id)
            routeInvitationExperience(
                InvitationExperienceRouteRequestCanvasAction(action: .editDraft),
                for: event
            )
        case .audience(let eventId):
            guard let event = repository.getEvent(id: eventId) else { return }
            routeInvitationExperience(
                InvitationExperienceRouteRequestParticipants.shared,
                for: event
            )
        case .information(let eventId):
            guard let event = repository.getEvent(id: eventId) else { return }
            routeInvitationExperience(
                InvitationExperienceRouteRequestEventInformation.shared,
                for: event
            )
        case .archive(let eventId):
            guard let event = repository.getEvent(id: eventId) else { return }
            routeInvitationExperience(
                InvitationExperienceRouteRequestDeepLink(
                    target: .archiveDetail,
                    intent: .read
                ),
                for: event
            )
        case .poll(let eventId):
            guard let event = repository.getEvent(id: eventId) else { return }
            routeInvitationExperience(
                InvitationExperienceRouteRequestDeepLink(
                    target: .poll,
                    intent: .mutate
                ),
                for: event
            )
        case .pollResults(let eventId):
            guard let event = repository.getEvent(id: eventId) else { return }
            routeInvitationExperience(
                InvitationExperienceRouteRequestDeepLink(
                    target: .poll,
                    intent: .read
                ),
                for: event
            )
        case .organization(let eventId):
            guard let event = repository.getEvent(id: eventId) else { return }
            routeInvitationExperience(
                InvitationExperienceRouteRequestDeepLink(
                    target: .organization,
                    intent: .read
                ),
                for: event
            )
        }
    }

    /// QA (couche 8, DEBUG seulement) : `--wakeve-qa-open-invitation-landing <eventId>` ouvre l'invitation reçue
    /// comme après `resolveInvitationDeepLink`, sans la résolution serveur du jeton (serveur local souvent injoignable).
    static let qaInvitationLandingArgument = "--wakeve-qa-open-invitation-landing"

    private func openQAInvitationLandingIfRequested() {
        let arguments = ProcessInfo.processInfo.arguments
        guard let index = arguments.firstIndex(of: Self.qaInvitationLandingArgument),
              arguments.indices.contains(index + 1),
              let event = repository.getEvent(id: arguments[index + 1]) else { return }
        selectedEvent = event
        invitationLandingEventId = event.id
        currentView = .eventDetail
    }

    private func invitationQAArtwork(for eventId: String) async -> (any Artwork)? {
        let now = Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
            epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        let repository = DatabaseInvitationExperienceProjectionRepository(
            database: RepositoryProvider.shared.database
        )
        guard let state = try? await repository.library(
            viewerId: userId,
            projection: .drafts,
            now: now
        ),
        let ready = state as? LibraryLoadStateReady<NSArray>,
        let cards = ready.snapshot as? [LibraryCardProjection]
        else {
            return nil
        }
        return cards.first(where: { $0.event.id == eventId })?.artwork
    }
#endif

    // MARK: - Shell (proposition #47)

    private var redesignChrome: some View {
        RedesignShellView(
            router: redesignRouter,
            eventsAtRoot: currentView == .eventList,
            activityAtRoot: activityAtRoot,
            activityBadge: activityToDoCount,
            userId: userId,
            userName: authStateManager.currentUser?.name,
            onCreate: beginRedesignEventCreation,
            events: {
                homeTabContent
                    // Retour des écrans sans contrôle propre (barre flottante masquée hors racine).
                    .safeAreaInset(edge: .top, spacing: 0) { redesignBackBar }
                    .environment(\.redesignBackAction, redesignToolbarBackAction)
            },
            activity: {
                ActivityView(
                    userId: userId,
                    actionCount: $activityToDoCount,
                    reloadToken: activityReloadToken,
                    onRootStateChange: { activityAtRoot = $0 },
                    initialFilter: redesignRouter.activityFilter,
                    filterRequestID: redesignRouter.activityFilterRequest,
                    onOpen: openActivityTarget
                )
            }
        )
        .onChange(of: redesignRouter.zone) { _, zone in
            dismissHubModuleSheet()
            // Les deux zones restent montées : recharger l'Activité à chaque entrée.
            if zone == .activity { activityReloadToken += 1 }
            if zone == .events { eventsHomeReloadToken += 1 }
        }
        .sheet(item: $redesignRouter.presentation) { presentation in
            // Une seule feuille possédée par le routeur : changer de présentation
            // pendant qu'une feuille est ouverte la ferme puis présente l'autre.
            switch presentation {
            case .profile:
                ProfileTabView(
                    userId: userId,
                    userName: authStateManager.currentUser?.name,
                    userEmail: authStateManager.currentUser?.email,
                    onDismiss: { redesignRouter.presentation = nil },
                    onSignOut: {
                        authStateManager.signOut()
                    }
                )
            case .settings:
                NavigationStack {
                    NotificationPreferencesView(userId: userId)
                }
            }
        }
        // Badge à jour quand un événement change ailleurs (vote depuis le hub, transition de cycle de vie),
        // au retour à la liste et au retour au premier plan.
        .onChange(of: eventsHomeReloadToken) { _, _ in activityReloadToken += 1 }
        .onChange(of: eventHubReloadToken) { _, _ in activityReloadToken += 1 }
        .onChange(of: currentView) { _, view in
            if view == .eventList { activityReloadToken += 1 }
        }
        .onChange(of: scenePhase) { _, phase in
            if phase == .active { activityReloadToken += 1 }
        }
    }

    /// Parent de l'écran courant quand il n'a pas de retour propre (`RedesignBackRoute`, couche 4).
    private var redesignBackDestination: AppView? {
        // Garde d'accès lue seulement pour les écrans qui en dépendent (budget, réunions, cagnotte).
        let organizationAccess = RedesignBackRoute.needsOrganizationAccess(currentView)
            ? selectedEvent.map { canAccessOrganizationDashboard(for: $0) } ?? true
            : true
        return RedesignBackRoute.destination(from: currentView, organizationAccess: organizationAccess)
    }

    /// Rangée de retour posée en inset : la barre de navigation propre de l'écran reste sur sa ligne.
    @ViewBuilder
    private var redesignBackBar: some View {
        if let destination = redesignBackDestination,
           RedesignBackRoute.placement(for: currentView) == .inset {
            HStack {
                WKCircleButton(
                    systemImage: "chevron.left",
                    accessibilityLabel: String(localized: "common.back"),
                    accessibilityID: "redesign.back",
                    action: { currentView = destination }
                )
                Spacer()
            }
            .padding(.horizontal, WK.Space.screen)
            .padding(.vertical, WK.Space.xxs)
        }
    }

    /// Retour lu par l'écran dans sa propre barre d'outils : un détail poussé le remplace par le retour système.
    private var redesignToolbarBackAction: (() -> Void)? {
        guard let destination = redesignBackDestination,
              RedesignBackRoute.placement(for: currentView) == .toolbar else { return nil }
        return { currentView = destination }
    }

    /// ＋ (et « Créer » de l'accueil vide, lien profond `.eventCreate`) : flux 4 questions (couche 7, #47)
    /// seulement rollout invitation éteint ; allumé, le studio reste le point d'entrée tant que le flux ne
    /// synchronise pas les événements (Swarm DAO #48).
    private func beginRedesignEventCreation() {
        redesignRouter.zone = .events
        switch CreateFlowEntry.newEventRoute(invitationRollout: invitationExperienceRolloutEnabled) {
        case .createFlow:
            openCreateEventFlow(draftEventId: nil)
        case .studio:
            selectedEvent = nil
            selectedCreationBaseRevision = nil
            selectedCreationArtwork = nil
            currentView = .eventCreation
        }
    }

    private func openCreateEventFlow(draftEventId: String?) {
        createFlowDraftId = draftEventId
        showCreateEventFlow = true
    }

    /// « Lancer le sondage » réussi : hub de l'événement en sondage.
    private func finishCreateEventFlow(_ event: Event, context: EventCreationContext) {
        persistCreationContext(context, for: event)
        showCreateEventFlow = false
        createFlowDraftId = nil
        redesignRouter.zone = .events
        selectedEvent = event
        currentView = .eventDetail
        eventsHomeReloadToken += 1
    }

    /// Fermeture : le brouillon (s'il existe) est déjà enregistré ; l'accueil le montre.
    private func closeCreateEventFlow() {
        showCreateEventFlow = false
        createFlowDraftId = nil
        eventsHomeReloadToken += 1
    }

    // MARK: - Accueil de la refonte (couche 3, #47)

    /// Ouvre un événement : routeur d'invitation si le rollout est actif, sinon directement le hub.
    private func openEventFromHome(_ id: String) {
        invitationLandingEventId = InvitationLandingRoute.marker(invitationLandingEventId, opening: id)
        guard let event = repository.getEvent(id: id) else {
            eventsHomeReloadToken += 1
            return
        }
        if invitationExperienceRolloutEnabled {
            routeInvitationExperience(
                InvitationExperienceRouteRequestCanvasAction(action: .showDetails),
                for: event
            )
        } else {
            selectedEvent = event
            currentView = .eventDetail
        }
    }

    private func handleHomeNextStep(_ step: HomeNextStep) {
        openHomeAction(step.action, eventId: step.eventId)
    }

    /// Même aiguillage que les liens profonds `.event(.pollVoting/.pollResults)` (gardes d'accès incluses),
    /// sans toucher à l'état du service de liens profonds. Partagé par l'accueil et l'Activité (couche 6).
    private func openHomeAction(_ action: HomeNextStep.Action, eventId: String) {
        invitationLandingEventId = InvitationLandingRoute.marker(invitationLandingEventId, opening: eventId)
        // Sans rollout invitation, le routeur retombe sur le détail : ouvrir directement l'écran de sondage.
        guard invitationExperienceRolloutEnabled else {
            switch action {
            case .vote: navigateInvitationDeepLink(eventId: eventId, destination: .pollVoting)
            case .pollResults: navigateInvitationDeepLink(eventId: eventId, destination: .pollResults)
            case .open: openEventFromHome(eventId)
            case .eventDay: openEventDay(eventId)
            }
            return
        }
        switch action {
        case .vote:
            navigateInvitationDeepLink(eventId: eventId, route: .poll, intent: .mutate)
        case .pollResults:
            navigateInvitationDeepLink(eventId: eventId, route: .poll, intent: .read)
        case .open:
            openEventFromHome(eventId)
        case .eventDay: openEventDay(eventId)
        }
    }

    // MARK: - Activité de la refonte (couche 6, #47)

    /// Entrée du fil d'activité → zone Événements. Le changement de zone demande la fermeture des sheets
    /// du hub (`onChange` de `redesignRouter.zone`) ; la navigation est différée dans une tâche `@MainActor`,
    /// exécutée après la mise à jour en cours (sans attendre la fin de l'animation de fermeture).
    private func openActivityTarget(_ target: ActivityTarget) {
        redesignRouter.zone = .events
        Task { @MainActor in
            switch target {
            case .hub(let eventId):
                openEventFromHome(eventId)
            case .vote(let eventId):
                openHomeAction(.vote, eventId: eventId)
            case .pollResults(let eventId):
                openHomeAction(.pollResults, eventId: eventId)
            case .comments(let eventId):
                // Même route (et mêmes gardes) que le lien profond `.event(.comments)`.
                selectedCommentSection = .general
                navigateInvitationDeepLink(eventId: eventId, destination: .comments)
            }
        }
    }

    /// Brouillon de sondage créé hors studio (aucun reçu d'invitation, `CreateFlowEntry`) : flux 4 questions (couche 7).
    /// Sinon chemin « modifier un brouillon » de la bibliothèque ; sans rollout invitation, ouvre le détail.
    private func editDraftFromHome(_ id: String) {
        let draft = repository.getEvent(id: id)
        if CreateFlowEntry.draftRoute(
               status: draft?.status,
               planningMode: draft?.planningMode,
               hasInvitationReceipt: CreateFlowEntry.hasInvitationReceipt(
                   eventId: id,
                   database: RepositoryProvider.shared.database
               )
           ) == .createFlow {
            openCreateEventFlow(draftEventId: id)
            return
        }
        guard invitationExperienceRolloutEnabled, let event = repository.getEvent(id: id) else {
            openEventFromHome(id)
            return
        }
        Task {
            selectedCreationArtwork = await homeDraftArtwork(for: id)
            routeInvitationExperience(
                InvitationExperienceRouteRequestCanvasAction(action: .editDraft),
                for: event
            )
        }
    }

    // MARK: - Hub d'événement de la refonte (couche 4, #47)

    /// Hub d'événement ; mêmes routes et gardes que l'ancien détail legacy (supprimé en couche 9).
    private func eventHubContent(for event: Event) -> some View {
        EventHubContainer(
            eventId: event.id,
            userId: userId,
            isLocalGuest: authStateManager.isCurrentSessionGuest,
            repository: repository,
            reloadToken: eventHubReloadToken,
            onBack: {
                dismissHubModuleSheet()
                invitationLandingEventId = nil
                currentView = .eventList
            },
            onOpenModule: { module in openHubModule(module, for: event) },
            onPrimary: { primary in handleHubPrimary(primary, for: event) },
            onLifecycleChanged: {
                selectedEvent = repository.getEvent(id: event.id)
                eventHubReloadToken += 1
            },
            onLoaded: { facts in refreshSelectedEvent(from: facts) },
            onRequestSignIn: {
                // Même entrée que le détail legacy : quitter le mode invité ramène à LoginView.
                authStateManager.signOut()
            },
            onOpenInfo: { currentView = .eventInformation },
            onAddParticipants: {
                performHubRoute(
                    EventHubRouting.addParticipantsRoute(invitationRollout: invitationExperienceRolloutEnabled),
                    for: event
                )
            },
            invitationRollout: invitationExperienceRolloutEnabled,
            onOpenEventDay: { openEventDay(event.id) }
        )
        // Un autre événement ouvert depuis le hub (lien profond) recrée son modèle de vue.
        .id(event.id)
        .sheet(item: hubSheetBinding, onDismiss: finishHubModuleSheet) { presented in
            hubModuleSheet(presented.module, for: event).id(event.id)
        }
    }

    /// Tuiles du hub → écrans existants ; leurs gardes d'accès s'appliquent en plus du verrou de la tuile.
    /// La tuile Date passe par `onPrimary(.vote)` quand un vote est attendu (`EventHubView.opensVoting`).
    private func openHubModule(_ module: HubModule, for event: Event) {
        let phase = SharedEventHubSource.phase(statusName: event.status.name)
        performHubRoute(
            EventHubRouting.route(for: module, phase: phase, invitationRollout: invitationExperienceRolloutEnabled),
            for: event
        )
    }

    /// Actions principales de navigation ; organiser/finaliser/se connecter sont confirmés dans le hub.
    private func handleHubPrimary(_ primary: EventHubModel.Primary, for event: Event) {
        guard let route = EventHubRouting.route(for: primary, invitationRollout: invitationExperienceRolloutEnabled) else {
            return
        }
        performHubRoute(route, for: event)
    }

    /// Seul point d'exécution des routes du hub (`EventHubRouting`, rollout invitation inclus).
    private func performHubRoute(_ route: EventHubRoute, for event: Event) {
        // Écrans de destination gardés sur `selectedEvent` : le relire avant de naviguer.
        let event = repository.getEvent(id: event.id) ?? event
        switch route {
        case .screen(let view):
            selectedEvent = event
            currentView = view
        case .editDraft:
            editDraftFromHome(event.id)
        case .invitationParticipants:
            // Même route que `onManageParticipants` du détail legacy.
            routeInvitationExperience(InvitationExperienceRouteRequestParticipants.shared, for: event)
        case .sheet(let module):
            selectedEvent = event
            // Même garde que le `case` legacy ; sans accès, son écran affiche le refus comme avant.
            let accessGranted: Bool
            switch EventHubRouting.sheetGuard(for: module) {
            case .detailedPlanning: accessGranted = canAccessDetailedPlanning(for: event)
            case .organizationDashboard: accessGranted = canAccessOrganizationDashboard(for: event)
            case .transportPlanning: accessGranted = canAccessTransportPlanning(for: event)
            case .unguarded: accessGranted = true
            }
            switch EventHubRouting.sheetRoute(for: module, accessGranted: accessGranted) {
            case .sheet?: hubSheet.present(module, eventId: event.id)
            case .screen(let view)?: currentView = view
            default: break
            }
        case .discussion:
            // Même route (et mêmes gardes) que la ligne « messages » de l'Activité et le lien `.event(.comments)`.
            selectedCommentSection = .general
            navigateInvitationDeepLink(eventId: event.id, destination: .comments)
        }
    }

    // MARK: - Mode immersif de la refonte (couche 8, #47)

    /// Invitation reçue (couche 8, #47), à la place du hub tant que `invitationLandingEventId`
    /// désigne l'événement. « Voir l'événement » efface le marqueur (le hub prend le relais) ; « Voter » suit la route
    /// du hub ; fermer revient à la liste, comme le retour du hub.
    private func invitationLandingContent(for event: Event) -> some View {
        let artwork = invitationExperienceProjectionRepository.artwork(eventId: event.id)
        return InvitationLandingContainer(
            eventId: event.id,
            userId: userId,
            isLocalGuest: authStateManager.isCurrentSessionGuest,
            artwork: artwork.flatMap { artwork in
                artwork is ArtworkNone ? nil : AnyView(InvitationArtworkView(artwork: artwork, event: event))
            },
            onViewEvent: { invitationLandingEventId = nil },
            onVote: {
                invitationLandingEventId = nil
                handleHubPrimary(.vote, for: event)
            },
            onClose: {
                invitationLandingEventId = nil
                currentView = .eventList
            }
        )
        .id(event.id)
    }

    /// Jour J en plein écran (hub « C'est aujourd'hui », accueil « Voir le jour J »).
    private func openEventDay(_ eventId: String) {
        invitationLandingEventId = InvitationLandingRoute.marker(invitationLandingEventId, opening: eventId)
        dismissHubModuleSheet()
        eventDayPresentation = EventDayPresentation(id: eventId)
    }

    /// « Voir l'événement » du jour J : ferme l'écran puis ouvre le hub, même chemin que l'accueil.
    private func viewEventFromEventDay(_ eventId: String) {
        eventDayPresentation = nil
        openEventFromHome(eventId)
    }

    /// Sheet présentée (`HubSheetLifecycle.Presented`, clé événement + module) ; la fermer passe par `close()`.
    private var hubSheetBinding: Binding<HubSheetLifecycle.Presented?> {
        Binding(
            get: { hubSheet.presented },
            set: { if $0 == nil { hubSheet.close() } }
        )
    }

    /// Sheet d'un module du hub ; les replis passent par `hubSheet.requestFallback`, appliqué après la fermeture.
    private func hubModuleSheet(_ module: HubModule, for event: Event) -> some View {
        HubModuleSheetView(
            module: module,
            eventId: event.id,
            viewerId: userId,
            source: SharedEventModuleSheetSource(),
            // Même règle que le `case` legacy : organisateur d'un événement non finalisé.
            canAddHint: event.organizerId == userId && !isFinalizedOrganizationState(event),
            // Lus à l'ouverture du formulaire seulement.
            mealParticipants: { participantModels(for: event) },
            onClose: { hubSheet.close() },
            onOpenFullScreen: {
                if let view = EventHubRouting.fullScreenFallback(for: module) {
                    hubSheet.requestFallback(view)
                } else if let route = EventHubRouting.fullScreenRoute(
                    for: module, invitationRollout: invitationExperienceRolloutEnabled
                ) {
                    // Invités : route d'ajout, sensible au flag invitations (couche 5c).
                    hubSheet.requestFallback(route: route)
                } else {
                    hubSheet.close()
                }
            },
            onOpenComments: {
                if let section = EventHubRouting.commentSection(for: module) {
                    selectedCommentSection = section
                    hubSheet.requestFallback(.comments)
                } else {
                    hubSheet.close()
                }
            },
            onOpenScreen: { view in hubSheet.requestFallback(view) }
        )
    }

    /// Navigation hors du hub (lien profond, flag, zone, retour, autre événement) : ferme la sheet sans repli.
    private func dismissHubModuleSheet() {
        hubSheet.dismiss()
    }

    /// Le hub quitte l'écran (autre écran, bascule du flag) : sa sheet ne recevra peut-être jamais `onDismiss`.
    private func releaseHubSheetHost() {
        if let presentation = hubSheet.hostRemoved() {
            redesignRouter.presentation = presentation
        }
    }

    /// Fermeture de la sheet : repli vers l'écran legacy si le hub du même événement est toujours affiché,
    /// sinon rechargement du hub ; puis présentation du routeur différée.
    private func finishHubModuleSheet() {
        for effect in hubSheet.didDismiss(currentView: currentView, selectedEventId: selectedEvent?.id) {
            switch effect {
            case .show(let view): currentView = view
            case .perform(let route): if let event = selectedEvent { performHubRoute(route, for: event) }
            case .reloadHub: eventHubReloadToken += 1
            case .presentRouter(let presentation): redesignRouter.presentation = presentation
            }
        }
    }

    /// Le hub a relu un autre statut (transition ailleurs, synchronisation) : relire l'événement sélectionné.
    private func refreshSelectedEvent(from facts: EventHubFacts) {
        guard let current = selectedEvent, current.id == facts.id,
              SharedEventHubSource.phase(statusName: current.status.name) != facts.phase,
              let fresh = repository.getEvent(id: facts.id) else { return }
        selectedEvent = fresh
    }

    /// Réutilise la confirmation de suppression existante (`pendingInformationDeleteEvent`).
    private func requestDeleteFromHome(_ id: String) {
        guard let event = repository.getEvent(id: id) else { return }
        pendingInformationDeleteEvent = event
    }

    private func homeDraftArtwork(for eventId: String) async -> (any Artwork)? {
        let now = Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
            epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        guard let state = try? await invitationExperienceProjectionRepository.library(
            viewerId: userId,
            projection: .drafts,
            now: now
        ),
        let ready = state as? LibraryLoadStateReady<NSArray>,
        let cards = ready.snapshot as? [LibraryCardProjection]
        else {
            return nil
        }
        return cards.first(where: { $0.event.id == eventId })?.artwork
    }
    
    // MARK: - Home Tab
    
    @ViewBuilder
    private var homeTabContent: some View {
        switch currentView {
        case .eventList:
#if DEBUG
            if invitationQALibraryIsSeedReady {
                invitationExperienceRootContent
            } else {
                ProgressView()
                    .accessibilityLabel(String(localized: "common.loading"))
                    .accessibilityIdentifier("invitationQALibrarySeedProgress")
            }
#else
            invitationExperienceRootContent
#endif
            
        case .eventCreation:
            if invitationExperienceRolloutEnabled {
                EventCreationStudioView(
                eventId: selectedEvent?.status == .draft ? selectedEvent?.id : nil,
                actorId: userId,
                baseRevision: selectedEvent?.status == .draft ? selectedCreationBaseRevision : nil,
                existingArtwork: selectedEvent?.status == .draft ? selectedCreationArtwork : nil,
                previewAvailable: true,
                onCancel: {
                    currentView = .eventList
                },
                onRequestPreview: { snapshot, confirmCommit in
                    invitationStudioPreview = InvitationStudioPreview(
                        snapshot: snapshot,
                        confirmCommit: {
                            let committed = await confirmCommit()
                            if committed {
                                currentView = .eventList
                            }
                            return committed
                        }
                    )
                }
                )
            } else if let event = selectedEvent {
                rolloutReadOnlyFallback(for: event)
            } else {
                creationFallbackWithoutStudio
            }
            
        case .eventDetail:
            if let event = selectedEvent {
                if InvitationLandingRoute.showsLanding(
                    marker: invitationLandingEventId, eventId: event.id, organizerId: event.organizerId, viewerId: userId
                ) {
                    invitationLandingContent(for: event)
                } else {
                    eventHubContent(for: event)
                        // L'organisateur qui ouvre son propre lien voit le hub : son marqueur est effacé.
                        .task(id: invitationLandingEventId) {
                            if invitationLandingEventId == event.id { invitationLandingEventId = nil }
                        }
                }
            }

        case .eventAudience:
            if !invitationExperienceRolloutEnabled, let event = selectedEvent {
                rolloutReadOnlyFallback(for: event)
            } else if let event = selectedEvent {
                let directInviteContext = directInviteRecipientContext(for: event)
                Group {
                    EventAudienceView(
                        eventId: event.id,
                        directInviteAvailable: directInviteContext.isReady,
                        directInviteCapability: directInviteContext.capability,
                        recipientKeyOwner: directInviteContext.recipientKeyOwner,
                        deliverySealer: directInviteContext.deliverySealer,
                        deliveryTransport: directInviteContext.deliveryTransport,
                        onInvite: {
                            selectedEvent = repository.getEvent(id: event.id)
                        }
                    )
                    .id(directInviteContext.generation)
                }
                .task(id: "\(event.id)|\(userId)|\(event.aggregateRevision)") {
                    await directInviteProductionOwner.refresh(
                        eventId: event.id,
                        actorId: userId
                    )
                }
            }

        case .eventInformation:
            if !invitationExperienceRolloutEnabled, let event = selectedEvent {
                rolloutReadOnlyFallback(for: event)
            } else if let event = selectedEvent {
                EventInformationView(
                    eventId: event.id,
                    viewerId: userId,
                    onOpenNotificationOwner: { preference in
                        await saveInformationNotificationPreference(
                            eventId: event.id,
                            preference: preference
                        )
                    },
                    onOpenCalendar: {
                        addInformationEventToCalendar(event)
                    },
                    onOpenMaps: {
                        openInformationMaps(for: event)
                    },
                    onOpenWeather: {
                        openInformationWeather(for: event)
                    },
                    onLeave: informationLeaveOwner(for: event),
                    onDelete: {
                        pendingInformationDeleteEvent = event
                    },
                    onDone: { currentView = .eventDetail }
                )
            }

        case .eventArchive:
            if !invitationExperienceRolloutEnabled, let event = selectedEvent {
                rolloutReadOnlyFallback(for: event)
            } else if let event = selectedEvent {
                EventArchiveView(
                    eventId: event.id,
                    viewerId: userId,
                    onReturn: { currentView = .eventList }
                )
            }
            
        case .participantManagement:
            if let event = selectedEvent {
                ParticipantManagementView(
                    event: event,
                    userId: userId,
                    repository: repository,
                    onParticipantsUpdated: {
                        // Refresh the event data
                        if let updatedEvent = repository.getEvent(id: event.id) {
                            selectedEvent = updatedEvent
                        }
                    },
                    onBack: {
                        currentView = .eventDetail
                    }
                )
            }
            
        case .pollVoting:
            if let event = selectedEvent {
                PollVotingView(
                    event: event,
                    repository: repository,
                    journal: RepositoryProvider.shared.ballotCommandJournal,
                    participantId: userId,
                    onVoteSubmitted: {
                        currentView = .eventDetail
                    },
                    onBack: {
                        currentView = .eventDetail
                    }
                )
            }
            
        case .pollResults:
            if let event = selectedEvent {
                PollResultsView(
                    event: event,
                    userId: userId,
                    onDateConfirmed: { _ in
                        currentView = .eventDetail
                    },
                    onBack: {
                        currentView = .eventDetail
                    }
                )
            }
            
        // MARK: - New PRD Features Navigation Cases
            
        case .scenarioList:
            if let event = selectedEvent {
                ScenarioOrganizationView(
                    event: event,
                    participantId: userId,
                    repository: repository,
                    onBack: {
                        currentView = .eventDetail
                    },
                    onOpenMeetings: {
                        // Selecting the final option emits meetings/{id}; meetings unlock
                        // only in ORGANIZING, so land on the detail and its lifecycle card first.
                        routeToMeetingsWhenUnlocked(eventId: event.id)
                    },
                    onOpenTransport: {
                        if let updatedEvent = repository.getEvent(id: event.id) {
                            selectedEvent = updatedEvent
                        }
                        currentView = .transportPlanning
                    }
                )
            } else {
                Text(String(localized: "navigation.placeholder.select_event_options"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .scenarioComparison:
            if let event = selectedEvent {
                ScenarioOrganizationView(
                    event: event,
                    participantId: userId,
                    repository: repository,
                    onBack: {
                        currentView = .eventDetail
                    },
                    onOpenMeetings: {
                        // Selecting the final option emits meetings/{id}; meetings unlock
                        // only in ORGANIZING, so land on the detail and its lifecycle card first.
                        routeToMeetingsWhenUnlocked(eventId: event.id)
                    },
                    onOpenTransport: {
                        currentView = .transportPlanning
                    }
                )
            } else {
                Text(String(localized: "navigation.placeholder.select_event_compare_options"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .budgetOverview:
            if let event = selectedEvent {
                if canAccessOrganizationDashboard(for: event) {
                    BudgetOverviewView(eventId: event.id)
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_budget")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_budget"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .accommodation:
            if let event = selectedEvent {
                if canAccessDetailedPlanning(for: event) {
                    AccommodationPlanningRouteView(
                        event: event,
                        isOrganizer: event.organizerId == userId,
                        isReadOnly: isFinalizedOrganizationState(event),
                        onBack: {
                            currentView = .eventDetail
                        },
                        onOpenComments: {
                            selectedCommentSection = .accommodation
                            currentView = .comments
                        }
                    )
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_accommodation")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_accommodation"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .mealPlanning:
            if let event = selectedEvent {
                if canAccessDetailedPlanning(for: event) {
                    MealPlanningRouteView(
                        event: event,
                        participants: participantModels(for: event),
                        isOrganizer: event.organizerId == userId,
                        isReadOnly: isFinalizedOrganizationState(event),
                        onBack: {
                            currentView = .eventDetail
                        },
                        onOpenComments: {
                            selectedCommentSection = .meal
                            currentView = .comments
                        }
                    )
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_meals")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_meals"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .equipmentChecklist:
            if let event = selectedEvent {
                if canAccessDetailedPlanning(for: event) {
                    EquipmentChecklistRouteView(
                        event: event,
                        isOrganizer: event.organizerId == userId,
                        isReadOnly: isFinalizedOrganizationState(event),
                        onBack: {
                            currentView = .eventDetail
                        },
                        onOpenComments: {
                            selectedCommentSection = .equipment
                            currentView = .comments
                        }
                    )
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_equipment")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_equipment"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .activityPlanning:
            if let event = selectedEvent {
                if canAccessDetailedPlanning(for: event) {
                    ActivityPlanningRouteView(
                        event: event,
                        isOrganizer: event.organizerId == userId,
                        isReadOnly: isFinalizedOrganizationState(event),
                        onBack: {
                            currentView = .eventDetail
                        },
                        onOpenComments: {
                            selectedCommentSection = .activity
                            currentView = .comments
                        }
                    )
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_activities")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_activities"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .scenarioDetail:
            if let event = selectedEvent {
                ScenarioDetailParityView(
                    event: event,
                    scenarioId: selectedScenarioId,
                    repository: repository,
                    userId: userId,
                    isReadOnly: isFinalizedOrganizationState(event),
                    onBack: {
                        currentView = .eventDetail
                    },
                    onOpenMeetings: {
                        currentView = .meetingList
                    },
                    onOpenTransport: {
                        currentView = .transportPlanning
                    }
                )
            } else {
                Text(String(localized: "navigation.placeholder.select_event_scenario"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .scenarioManagement:
            if let event = selectedEvent {
                ScenarioOrganizationView(
                    event: event,
                    participantId: userId,
                    repository: repository,
                    onBack: {
                        currentView = .eventDetail
                    },
                    onOpenMeetings: {
                        // Selecting the final option emits meetings/{id}; meetings unlock
                        // only in ORGANIZING, so land on the detail and its lifecycle card first.
                        routeToMeetingsWhenUnlocked(eventId: event.id)
                    },
                    onOpenTransport: {
                        if let updatedEvent = repository.getEvent(id: event.id) {
                            selectedEvent = updatedEvent
                        }
                        currentView = .transportPlanning
                    }
                )
            } else {
                Text(String(localized: "navigation.placeholder.select_event_options"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .budgetDetail:
            if let event = selectedEvent {
                if canAccessOrganizationDashboard(for: event) {
                    BudgetDetailView(eventId: event.id)
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_expenses")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_expenses"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }
            
        case .meetingList:
            if let event = selectedEvent {
                if canAccessOrganizationDashboard(for: event) {
                    MeetingListView(
                        eventId: event.id,
                        currentUserId: userId,
                        isOrganizer: event.organizerId == userId,
                        canCreateMeetings: event.organizerId == userId && event.status == .organizing,
                        isReadOnly: isFinalizedOrganizationState(event)
                    )
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_meetings")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_meetings"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .meetingDetail:
            if let meetingId = selectedMeetingId, let event = selectedEvent {
                if canAccessOrganizationDashboard(for: event) {
                    MeetingDetailView(
                        meetingId: meetingId,
                        eventId: event.id,
                        currentUserId: userId,
                        isOrganizer: event.organizerId == userId,
                        isReadOnly: isFinalizedOrganizationState(event)
                    )
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_meeting_detail")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_meeting"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .paymentPot:
            if let event = selectedEvent {
                let canAccessPhase5Organization = (event.status == .organizing || event.status == .finalized) && canAccessOrganizationDetails(for: event)
                if canAccessPhase5Organization {
                    let canManagePayment = event.status == .organizing && event.organizerId == userId
                    PaymentPotView(
                        eventId: event.id,
                        eventTitle: event.title,
                        currentUserId: userId,
                        isOrganizer: event.organizerId == userId,
                        canManagePayment: canManagePayment,
                        isReadOnly: isFinalizedOrganizationState(event)
                    )
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_payment_pot")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_payment_pot"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .tricount:
            if let event = selectedEvent {
                let canAccessPhase5Organization = (event.status == .organizing || event.status == .finalized) && canAccessOrganizationDetails(for: event)
                if canAccessPhase5Organization {
                    let canManageTricount = event.status == .organizing && event.organizerId == userId
                    TricountHandoffView(
                        eventId: event.id,
                        currentUserId: userId,
                        isOrganizer: event.organizerId == userId,
                        canManageTricount: canManageTricount,
                        isReadOnly: isFinalizedOrganizationState(event)
                    )
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_tricount")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_tricount"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .transportPlanning:
            if let event = selectedEvent {
                if canAccessTransportPlanning(for: event) {
                    let selectedDestination = transportDestination(for: event)
                    let transportPlanningAdapter: TransportPlanningViewModel = transportPlanningViewModel
                    let transportState = transportPlanningViewModel.state.eventId == event.id
                        ? transportPlanningViewModel.state
                        : makeTransportPlanningState(for: event)
                    TransportPlanningView(
                        event: event,
                        isOrganizer: event.organizerId == userId,
                        isParticipantConfirmed: isParticipantConfirmed(for: event),
                        isReadOnly: isFinalizedOrganizationState(event),
                        eventStatus: event.status,
                        confirmedDate: event.finalDate,
                        selectedDestination: selectedDestination,
                        readiness: transportState.readiness,
                        missingDeparture: transportState.missingDeparture,
                        plans: transportState.plans,
                        selectedPlanId: transportState.selectedPlanId,
                        pendingSync: transportState.pendingSync,
                        onGenerate: { optimization in
                            transportPlanningAdapter.generate(
                                event: event,
                                userId: userId,
                                repository: repository,
                                selectedDestination: selectedDestination,
                                optimization: optimization
                            )
                        },
                        onSelectFinalPlan: { plan in
                            transportPlanningAdapter.selectFinalPlan(
                                event: event,
                                userId: userId,
                                repository: repository,
                                selectedDestination: selectedDestination,
                                plan: plan
                            )
                        },
                        onMarkTransportNotNeeded: {
                            transportPlanningAdapter.markTransportNotNeeded(
                                event: event,
                                userId: userId,
                                repository: repository,
                                selectedDestination: selectedDestination
                            )
                        },
                        onSaveDepartureLocation: { location in
                            transportPlanningAdapter.saveDepartureLocation(
                                event: event,
                                userId: userId,
                                repository: repository,
                                selectedDestination: selectedDestination,
                                location: location
                            )
                        },
                        onChooseDestination: {
                            currentView = .scenarioList
                        },
                        onBack: {
                            currentView = .eventDetail
                        }
                    )
                    .onAppear {
                        transportPlanningViewModel.load(
                            event: event,
                            userId: userId,
                            repository: repository,
                            selectedDestination: selectedDestination
                        )
                    }
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_transport")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_transport"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .comments:
            if let event = selectedEvent {
                if canAccessDetailedPlanning(for: event) {
                    EventCommentsRouteView(
                        event: event,
                        section: selectedCommentSection,
                        currentUserId: userId,
                        isOrganizer: event.organizerId == userId,
                        onBack: {
                            currentView = .eventDetail
                        }
                    )
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_comments")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_comments"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .eventPhotos:
            if let event = selectedEvent {
                if canAccessDetailedPlanning(for: event) {
                    EventPhotosFollowUpRouteView(
                        event: event,
                        isReadOnly: isFinalizedOrganizationState(event),
                        onBack: {
                            currentView = .eventDetail
                        }
                    )
                } else {
                    AccessDenied(message: String(localized: "organization.access.confirm_before_photos")) {
                        currentView = .eventDetail
                    }
                }
            } else {
                Text(String(localized: "navigation.placeholder.select_event_photos"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .invitationShare:
            if let event = selectedEvent {
                InvitationShareSheet(
                    eventId: event.id,
                    eventTitle: event.title,
                    invitationCode: InvitationTokenCodec.invitationCode(forEventId: event.id),
                    onDismiss: {
                        currentView = .eventDetail
                    }
                )
            } else {
                Text(String(localized: "navigation.placeholder.select_event_invitation"))
                    .font(.title2)
                    .foregroundColor(.secondary)
            }

        case .leaderboard:
            LeaderboardView()

        case .organizerDashboard:
            OrganizerDashboardRouteView(
                events: repository.getAllEvents(),
                currentUserId: userId,
                onBack: {
                    currentView = .eventList
                }
            )
        }
    }

    private var invitationExperienceRootContent: some View {
        EventsHomeContainer(
            userId: userId,
            reloadToken: eventsHomeReloadToken,
            onOpenEvent: { id in openEventFromHome(id) },
            onNextStep: { step in handleHomeNextStep(step) },
            onCreate: { beginRedesignEventCreation() },
            onEditDraft: { id in editDraftFromHome(id) },
            onDelete: { id in requestDeleteFromHome(id) },
            invitationRollout: invitationExperienceRolloutEnabled
        )
    }

    /// Lancement du flux : la checklist du modèle reste avec l'événement (sur cet appareil) et s'affiche dans le hub.
    private func persistCreationContext(_ context: EventCreationContext, for event: Event) {
        UserDefaultsEventChecklistStore().seedTemplate(eventId: event.id, titles: context.templateChecklist)
    }

    private func directInviteRecipientContext(for event: Event) -> DirectInviteRecipientContext {
        directInviteProductionOwner.context(
            eventId: event.id,
            actorId: userId
        )
    }

    private func saveInformationNotificationPreference(
        eventId: String,
        preference: EventNotificationPreference
    ) async -> Bool {
        let owner = DatabaseEventNotificationPreferenceRepository(
            database: RepositoryProvider.shared.database
        )
        let operationKey = OperationKey(
            subject: OperationSubjectEventNotification(eventId: eventId, userId: userId),
            action: .saveEventPreference,
            target: OperationTargetUser(userId: userId),
            operationId: UUID().uuidString
        )

        do {
            _ = try await owner.save(operationKey: operationKey, preference: preference)
            let storedRecord = try await owner.get(eventId: eventId, userId: userId)
            return storedRecord?.preference == preference
        } catch {
            return false
        }
    }

    /// R1 has no typed leave-event owner yet. Keeping the callback absent makes
    /// the capability-driven control non-interactive instead of mutating SQL
    /// directly from SwiftUI.
    private func informationLeaveOwner(for _: Event) -> (() -> Void)? {
        nil
    }

    private func addInformationEventToCalendar(_ event: Event) {
        let database = RepositoryProvider.shared.database
        let participantId = userId
        Task {
            let owner = CalendarService(
                database: database,
                platformCalendarService: PlatformCalendarServiceImpl()
            )
            _ = try? await owner.addToNativeCalendar(
                eventId: event.id,
                participantId: participantId
            )
        }
    }

    private func openInformationMaps(for event: Event) {
        guard let location = RepositoryProvider.shared.database.eventWeatherQueries
            .selectResolvedLocationByEvent(eventId: event.id)
            .executeAsOneOrNull()
        else { return }

        var components = URLComponents()
        components.scheme = "https"
        components.host = "maps.apple.com"
        components.path = "/"
        components.queryItems = [
            URLQueryItem(name: "ll", value: "\(location.latitude),\(location.longitude)"),
            URLQueryItem(name: "q", value: location.label)
        ]
        if let url = components.url { openURL(url) }
    }

    private func openInformationWeather(for event: Event) {
        guard let location = RepositoryProvider.shared.database.eventWeatherQueries
            .selectResolvedLocationByEvent(eventId: event.id)
            .executeAsOneOrNull(),
              let url = URL(
                string: "weather://?latitude=\(location.latitude)&longitude=\(location.longitude)"
              )
        else { return }
        openURL(url)
    }

    private func deleteInformationEventThroughOwner(_ event: Event) {
        let owner = EventDetailViewModel(eventId: event.id, userId: userId)
        informationDeleteOwner = owner
        Task {
            let deleted = await owner.deleteEventAndWait()
            guard deleted else { return }
            // La checklist locale de l'événement (hors base) part avec lui.
            UserDefaultsEventChecklistStore().save([], eventId: event.id)
            selectedEvent = nil
            currentView = .eventList
            informationDeleteOwner = nil
            eventsHomeReloadToken += 1
        }
    }

    private func handleDeepLinkNavigation(_ route: IosRoute) {
        invitationLandingEventId = nil
        dismissHubModuleSheet()
        let shellRoute = AppRouter.preRoute(route, router: redesignRouter)
        // Profil / réglages attendent la fermeture de la sheet du hub (`finishHubModuleSheet`).
        redesignRouter.presentation = hubSheet.routerPresentation(redesignRouter.presentation)
        guard let route = shellRoute else {
            // Route entièrement traitée par le shell de la refonte.
            deepLinkService.clearPendingInvite()
            deepLinkService.clearPendingDeepLink()
            deepLinkService.resetNavigation()
            return
        }
        switch route {
        case .topLevel(.home):
            currentView = .eventList
        case .topLevel(.profile), .topLevel(.settings), .topLevel(.notificationPreferences), .topLevel(.notifications):
            // Présentés par le shell (`AppRouter.preRoute`) : jamais délégués à cet aiguillage.
            break
        case .topLevel(.leaderboard):
            currentView = .leaderboard
        case .topLevel(.organizerDashboard):
            currentView = .organizerDashboard
        case .eventCreate:
            // Même aiguillage que ＋ : flux 4 questions, ou studio sous le rollout invitation.
            beginRedesignEventCreation()
        case .event(.detail(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, action: .showDetails)
        case .event(.pollVoting(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, route: .poll, intent: .mutate)
        case .event(.pollResults(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, route: .poll, intent: .read)
        case .event(.participants(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, route: .participants, intent: .read)
        case .event(.information(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, route: .eventInformation, intent: .read)
        case .event(.archive(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, route: .archiveDetail, intent: .read)
        case .event(.scenarioList(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, destination: .scenarioList)
        case .event(.scenarioComparison(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, destination: .scenarioComparison)
        case .event(.scenarioManagement(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, destination: .scenarioManagement)
        case .event(.scenarioDetail(let eventId, let scenarioId)):
            selectedScenarioId = scenarioId
            navigateInvitationDeepLink(eventId: eventId, destination: .scenarioDetail)
        case .event(.budgetOverview(let eventId)):
            selectedBudget = nil
            navigateInvitationDeepLink(eventId: eventId, destination: .budgetOverview)
        case .event(.budgetDetail(let eventId, _)):
            selectedBudget = nil
            navigateInvitationDeepLink(eventId: eventId, destination: .budgetDetail)
        case .event(.meetingList(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, destination: .meetingList)
        case .event(.comments(let eventId)):
            selectedCommentSection = .general
            navigateInvitationDeepLink(eventId: eventId, destination: .comments)
        case .event(.invitationShare(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, destination: .invitationShare)
        case .event(.transport(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, destination: .transportPlanning)
        case .event(.accommodation(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, destination: .accommodation)
        case .event(.meals(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, destination: .mealPlanning)
        case .event(.equipment(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, destination: .equipmentChecklist)
        case .event(.activities(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, destination: .activityPlanning)
        case .event(.payment(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, destination: .paymentPot)
        case .event(.tricount(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, destination: .tricount)
        case .event(.photos(let eventId)):
            navigateInvitationDeepLink(eventId: eventId, destination: .eventPhotos)
        case .meetingDetail(let meetingId):
            navigateToMeeting(meetingId: meetingId)
        case .invite(let token):
            Task {
                await resolveInvitationDeepLink(token: token)
            }
        }

        deepLinkService.clearPendingInvite()
        deepLinkService.clearPendingDeepLink()
        deepLinkService.resetNavigation()
    }

    private func resolveInvitationDeepLink(token: String) async {
        guard let eventId = await invitationDeepLinkResolver.resolve(token: token) else {
            currentView = .eventList
            return
        }
        invitationLandingEventId = eventId
        navigateInvitationDeepLink(eventId: eventId, action: .showDetails)
    }

    private func navigateToEvent(eventId: String, destination: AppView) {
        guard let event = repository.getEvent(id: eventId) else {
            currentView = .eventList
            return
        }

        selectedEvent = event

        if destination == .pollVoting, event.status != .polling {
            currentView = .eventDetail
        } else {
            currentView = destination
        }
    }

    private func navigateInvitationDeepLink(
        eventId: String,
        route: InvitationExperienceRouteCapability,
        intent: InvitationExperienceDeepLinkIntent
    ) {
        guard let event = repository.getEvent(id: eventId) else {
            selectedEvent = nil
            currentView = .eventList
            return
        }
        routeInvitationExperience(
            InvitationExperienceRouteRequestDeepLink(target: route, intent: intent),
            for: event
        )
    }

    private func navigateInvitationDeepLink(
        eventId: String,
        action: InvitationExperienceCanvasAction
    ) {
        guard let event = repository.getEvent(id: eventId) else {
            selectedEvent = nil
            currentView = .eventList
            return
        }
        routeInvitationExperience(
            InvitationExperienceRouteRequestCanvasAction(action: action),
            for: event
        )
    }

    /// Preflights event-scoped legacy destinations through the typed invitation router.
    /// A participants READ request supplies the common membership guard, while the
    /// router redirects every PAST/FINALIZED event to Archive before any legacy route.
    private func navigateInvitationDeepLink(
        eventId: String,
        destination: AppView
    ) {
        guard let event = repository.getEvent(id: eventId) else {
            selectedEvent = nil
            currentView = .eventList
            return
        }
        guard invitationExperienceRolloutEnabled else {
            selectedEvent = event
            currentView = destination
            return
        }
        let resolution = invitationExperienceRouter.resolve(
            request: InvitationExperienceRouteRequestDeepLink(
                target: .participants,
                intent: .read
            ),
            context: invitationRouteContext(for: event)
        )
        selectedEvent = event

        if let resolved = resolution as? InvitationExperienceRouteResolutionDestination,
           resolved.route == .archiveDetail {
            currentView = .eventArchive
        } else if resolution is InvitationExperienceRouteResolutionDestination ||
                    resolution is InvitationExperienceRouteResolutionLocalDetails {
            currentView = destination
        } else {
            currentView = .eventDetail
        }
    }

    private func routeInvitationExperience(
        _ request: any InvitationExperienceRouteRequest,
        for event: Event
    ) {
        guard invitationExperienceRolloutEnabled else {
            invitationExperienceLegacyFallback(for: event)
            return
        }
        let resolution = invitationExperienceRouter.resolve(
            request: request,
            context: invitationRouteContext(for: event)
        )
        selectedEvent = event

        if let destination = resolution as? InvitationExperienceRouteResolutionDestination {
            switch destination.route {
            case .draftEditor:
                selectedCreationBaseRevision = RepositoryProvider.shared.database.eventQueries
                    .selectById(id: event.id)
                    .executeAsOneOrNull()?
                    .aggregateRevision
                currentView = .eventCreation
            case .poll:
                if let deepLink = request as? InvitationExperienceRouteRequestDeepLink {
                    currentView = deepLink.intent == .mutate ? .pollVoting : .pollResults
                } else if let canvas = request as? InvitationExperienceRouteRequestCanvasAction,
                          canvas.action == .submitVote {
                    currentView = .pollVoting
                } else {
                    currentView = .pollResults
                }
            case .participants:
                currentView = .eventAudience
            case .organization:
                currentView = .scenarioList
            case .eventInformation:
                currentView = .eventInformation
            case .archiveDetail:
                currentView = .eventArchive
            default:
                currentView = .eventDetail
            }
        } else if resolution is InvitationExperienceRouteResolutionLocalDetails {
            currentView = .eventDetail
        } else {
            currentView = .eventDetail
        }
    }

    private func routeToMeetingsWhenUnlocked(eventId: String) {
        if let updatedEvent = repository.getEvent(id: eventId) {
            selectedEvent = updatedEvent
        }
        let meetingsUnlocked = selectedEvent?.status == .organizing || selectedEvent?.status == .finalized
        currentView = meetingsUnlocked ? .meetingList : .eventDetail
    }

    private func invitationExperienceLegacyFallback(for event: Event) {
        selectedEvent = event
        currentView = .eventDetail
    }

    @ViewBuilder
    private func rolloutReadOnlyFallback(for event: Event) -> some View {
        ProgressView()
            .accessibilityLabel(String(localized: "common.loading"))
            .task(id: event.id) {
                invitationExperienceLegacyFallback(for: event)
            }
    }

    /// Studio demandé sans rollout invitation (rollout éteint en cours de route) : le flux
    /// 4 questions remplace l'ancienne feuille de création (supprimée en couche 9).
    private var creationFallbackWithoutStudio: some View {
        ProgressView()
            .accessibilityLabel(String(localized: "common.loading"))
            .task {
                currentView = .eventList
                openCreateEventFlow(draftEventId: nil)
            }
    }

    private func invitationRouteContext(for event: Event) -> InvitationExperienceRouteContext {
        let now = Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
            epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        let participantRecords = repository.getParticipantRecords(eventId: event.id) ?? []
        let currentRecord = participantRecords.first { record in
            record.userId == userId || record.id == userId
        }
        let isOrganizer = event.organizerId == userId
        let accessState = isOrganizer
            ? ParticipantAccessState.companion.organizer(userId: userId)
            : currentRecord.map {
                ParticipantAccessMapper.shared.fromRepositoryRecord(record: $0)
            } ?? ParticipantAccessState.companion.nonMember(userId: userId)
        let viewerRole: ViewerRole = switch accessState.role {
        case .organizer: .organizer
        case .member: .member
        case .nonMember: .nonMember
        default: .nonMember
        }
        let accessRow = ParticipantManagementPresentationMapper.shared
            .map(participants: [accessState])
            .first
        let hasMemberAccess = viewerRole == .organizer || viewerRole == .member
        let canUsePoll = event.status == .polling && (
            viewerRole == .organizer || (
                viewerRole == .member &&
                accessState.rsvp != .declined &&
                accessState.rsvp != .unavailable &&
                accessState.rsvp != .notApplicable
            )
        )

        return InvitationExperienceRouteContext(
            eventStatus: event.status,
            temporalClass: EventTemporalClassifier.shared.classify(event: event, now: now),
            viewerRole: viewerRole,
            access: InvitationExperienceRouteAccess(
                canEditDraft: isOrganizer && event.status == .draft,
                canUsePoll: canUsePoll,
                canReadPollResults: hasMemberAccess,
                canOpenParticipants: hasMemberAccess,
                canOpenOrganization: isOrganizer || accessRow?.canAccessOrganizationDetails == true
            ),
            installedRoutes: Set([
                .draftEditor,
                .poll,
                .participants,
                .organization,
                .eventInformation,
                .archiveDetail
            ])
        )
    }

    private func navigateToMeeting(meetingId: String) {
        guard let meeting = RepositoryProvider.shared.database.meetingQueries
            .selectById(id: meetingId)
            .executeAsOneOrNull(),
            let event = repository.getEvent(id: meeting.eventId)
        else {
            currentView = .eventList
            selectedMeetingId = nil
            return
        }

        selectedMeetingId = meetingId
        selectedEvent = event
        currentView = .meetingDetail
    }

    private func isParticipantConfirmed(for event: Event) -> Bool? {
        // Organisateur, ou participant accepté avec la date retenue validée (règle partagée avec le hub).
        if event.organizerId == userId {
            return true
        }
        return OrganizationDetailsAccess.isGranted(
            organizerId: event.organizerId,
            viewerId: userId,
            records: repository.getParticipantRecords(eventId: event.id)
        )
    }

    private func canAccessOrganizationDetails(for event: Event) -> Bool {
        event.organizerId == userId || isParticipantConfirmed(for: event) == true
    }

    private func canAccessOrganizationDashboard(for event: Event) -> Bool {
        switch event.status {
        case .organizing, .finalized:
            return canAccessOrganizationDetails(for: event)
        default:
            return false
        }
    }

    private func canAccessDetailedPlanning(for event: Event) -> Bool {
        switch event.status {
        case .confirmed, .comparing, .organizing, .finalized:
            return canAccessOrganizationDetails(for: event)
        default:
            return false
        }
    }

    private func canAccessTransportPlanning(for event: Event) -> Bool {
        switch event.status {
        case .confirmed, .organizing, .finalized:
            return canAccessOrganizationDetails(for: event)
        default:
            return false
        }
    }

    private func participantModels(for event: Event) -> [ParticipantModel] {
        if let participantRecords = repository.getParticipantRecords(eventId: event.id), !participantRecords.isEmpty {
            let participantAccessStates = participantRecords.map { record in
                ParticipantAccessMapper.shared.fromRepositoryRecord(record: record)
            }
            return ParticipantManagementPresentationMapper.shared
                .map(participants: participantAccessStates)
                .map { row in
                    ParticipantModel(
                        id: row.userIdOrEmail,
                        name: row.userIdOrEmail,
                        email: row.userIdOrEmail,
                        dietaryRestrictions: []
                    )
                }
        }

        let participantIds = repository.getParticipants(eventId: event.id) ?? []
        let fallbackParticipants = participantIds.isEmpty ? event.participants : participantIds
        return fallbackParticipants.map { participant in
            ParticipantModel(
                id: participant,
                name: participant,
                email: participant,
                dietaryRestrictions: []
            )
        }
    }

    private func makeTransportPlanningState(for event: Event) -> TransportPlanningPresentationState {
        transportPlanningViewModel.makeState(
            event: event,
            userId: userId,
            repository: repository,
            selectedDestination: transportDestination(for: event)
        )
    }

    private func transportDestination(for event: Event) -> TransportLocation? {
        guard let selectedScenario = loadSelectedScenario(for: event),
              selectedScenario.status == ScenarioStatus.selected,
              let location = selectedScenario.location.trimmingCharacters(in: .whitespacesAndNewlines).nilIfEmpty
        else {
            return nil
        }

        return TransportLocation(
            name: location,
            address: nil,
            latitude: nil,
            longitude: nil,
            iataCode: nil
        )
    }

    private func loadSelectedScenario(for event: Event) -> Scenario_? {
        let selectedScenarioRepository = ScenarioRepository(db: RepositoryProvider.shared.database)
        if let selected = selectedScenarioRepository.getSelectedScenario(eventId: event.id),
           selected.status == ScenarioStatus.selected {
            return selected
        }

        return selectedScenarioRepository
            .getScenariosByEventIdAndStatus(eventId: event.id, status: ScenarioStatus.selected)
            .first
    }
}

private extension String {
    var nilIfEmpty: String? {
        let value = trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty else {
            return nil
        }

        return value
    }
}

private func isFinalizedOrganizationState(_ event: Event) -> Bool {
    let readOnly = event.status == EventStatus.finalized
    let viewOnly = readOnly
    let mutationsDisabled = viewOnly
    return mutationsDisabled
}

// MARK: - Explore Tab View

// ProfileTabView is now in its own file: Views/Profile/ProfileTabView.swift

enum AppView {
    case eventList
    case eventCreation
    case eventDetail
    case eventAudience
    case eventInformation
    case eventArchive
    case participantManagement
    case pollVoting
    case pollResults
    // New PRD features
    case scenarioList
    case scenarioDetail
    case scenarioComparison
    case scenarioManagement
    case budgetOverview
    case budgetDetail
    case accommodation
    case mealPlanning
    case equipmentChecklist
    case activityPlanning
    case comments
    case eventPhotos
    case invitationShare
    // Phase 4 - Meetings & Communication
    case meetingList
    case meetingDetail
    case transportPlanning
    case paymentPot
    case tricount
    case leaderboard
    case organizerDashboard
}

private struct InvitationStudioPreview: Identifiable {
    let id = UUID()
    let snapshot: InvitationStudioPreviewSnapshot
    let confirmCommit: () async -> Bool
}

private struct InvitationStudioPreviewSheet: View {
    let preview: InvitationStudioPreview
    @Environment(\.dismiss) private var dismiss
    @State private var isCommitting = false
    @State private var commitFailed = false

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    VStack(alignment: .leading, spacing: 12) {
                        InvitationArtworkView(
                            artwork: preview.snapshot.artwork,
                            event: preview.snapshot.event
                        )
                        .frame(height: 220)
                        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                        .overlay(alignment: .bottomLeading) {
                            ZStack(alignment: .bottomLeading) {
                                LinearGradient(
                                    colors: [.clear, .black.opacity(0.78)],
                                    startPoint: .top,
                                    endPoint: .bottom
                                )
                                Text(preview.snapshot.event.title)
                                    .font(.largeTitle.bold())
                                    .foregroundStyle(.white)
                                    .fixedSize(horizontal: false, vertical: true)
                                    .padding(20)
                            }
                        }

                        Text(preview.snapshot.event.description_)
                            .foregroundStyle(.secondary)
                            .lineLimit(2)
                            .invitationAccessibilityIdentifier("eventStudioPreviewDescription")

                        LabeledContent(
                            String(localized: "events.status.date_confirmed"),
                            value: previewDate
                        )
                        .invitationAccessibilityIdentifier("eventStudioPreviewDate")

                        LabeledContent(
                            String(localized: "events.location"),
                            value: preview.snapshot.locationDisplayName
                        )
                        .invitationAccessibilityIdentifier("eventStudioPreviewLocation")

                        LabeledContent(
                            String(localized: "invitation.information.organizer"),
                            value: preview.snapshot.hostDisplayName
                        )
                        .invitationAccessibilityIdentifier("eventStudioPreviewHost")
                    }
                    .invitationAccessibilityIdentifier("eventStudioPreviewArtwork")
                }
                .padding()
            }
            .navigationTitle(String(localized: "invitation.studio.preview"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(localized: "common.done")) {
                        isCommitting = true
                        Task {
                            if await preview.confirmCommit() {
                                dismiss()
                            } else {
                                isCommitting = false
                                commitFailed = true
                            }
                        }
                    }
                    .disabled(isCommitting)
                }
            }
        }
        .alert(String(localized: "common.error"), isPresented: $commitFailed) {
            Button(String(localized: "common.done"), role: .cancel) {}
        } message: {
            Text(String(localized: "common.error_generic"))
        }
    }

    private var previewDate: String {
        let rawDate = preview.snapshot.event.finalDate ??
            preview.snapshot.event.proposedSlots.first?.start
        guard let rawDate else {
            return String(localized: "invitation.state.unavailable")
        }
        return InvitationEventMetadataProjection.localizedDate(for: rawDate)
    }
}

private struct AccessDenied: View {
    let message: String
    let onBack: () -> Void

    var body: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.xl) {
            VStack(spacing: WakeveTheme.Spacing.md) {
                Image(systemName: "lock.fill")
                    .font(.largeTitle)
                    .foregroundStyle(WakeveTheme.ColorToken.permissionBlue)
                Text(String(localized: "access_denied.title"))
                    .font(WakeveTheme.Typography.title2)
                Text(message)
                    .font(WakeveTheme.Typography.body)
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
                WakeveActionButton(
                    String(localized: "common.back"),
                    systemImage: "chevron.left",
                    variant: .primary,
                    action: onBack
                )
            }
        }
        .padding(WakeveTheme.Spacing.page)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(WakeveScreenBackground(style: .grouped))
    }
}

private struct Phase5SyncBanner: View {
    let pendingSync: Bool
    let isOnline: Bool

    var body: some View {
        if pendingSync || !isOnline {
            WakeveContentCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.lg, padding: WakeveTheme.Spacing.md) {
                Label(
                    pendingSync ? String(localized: "sync.pending_changes") : String(localized: "sync.offline_available"),
                    systemImage: "arrow.triangle.2.circlepath"
                )
                .font(WakeveTheme.Typography.metadata.weight(.semibold))
                .foregroundStyle(.secondary)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }
}

private struct PaymentTrustChecklistCard: View {
    let title: String
    let items: [PaymentTrustChecklistItem]

    var body: some View {
        WakeveContentCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                Label(title, systemImage: "checkmark.shield.fill")
                    .font(WakeveTheme.Typography.section)

                ForEach(items) { item in
                    HStack(alignment: .top, spacing: WakeveTheme.Spacing.sm) {
                        Image(systemName: item.systemImage)
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundStyle(.green)
                            .frame(width: 24, height: 24)

                        VStack(alignment: .leading, spacing: 2) {
                            Text(item.title)
                                .font(WakeveTheme.Typography.bodySemibold)
                            Text(item.detail)
                                .font(WakeveTheme.Typography.caption)
                                .foregroundStyle(.secondary)
                                .fixedSize(horizontal: false, vertical: true)
                        }
                    }
                }
            }
        }
    }
}

private struct PaymentTrustChecklistItem: Identifiable {
    let id: String
    let systemImage: String
    let title: String
    let detail: String
}

private func formatCurrencyAmount(_ amount: Double, currency: String) -> String {
    let formatter = NumberFormatter()
    formatter.numberStyle = .currency
    formatter.currencyCode = currency
    formatter.maximumFractionDigits = amount.rounded() == amount ? 0 : 2
    return formatter.string(from: NSNumber(value: amount)) ?? "\(currency) \(amount)"
}

private struct PaymentPotView: View {
    let eventId: String
    let eventTitle: String
    let currentUserId: String
    let isOrganizer: Bool
    let isReadOnly: Bool
    private let canManagePayment: Bool
    private let pendingSync: Bool
    @State private var activePot: PaymentPotRecord?
    @State private var goalAmountText: String
    @State private var statusText: String

    init(eventId: String, eventTitle: String, currentUserId: String, isOrganizer: Bool, canManagePayment: Bool, isReadOnly: Bool = false) {
        self.eventId = eventId
        self.eventTitle = eventTitle
        self.currentUserId = currentUserId
        self.isOrganizer = isOrganizer
        self.isReadOnly = isReadOnly
        self.canManagePayment = canManagePayment
        self.pendingSync = Phase5PendingSync.selectPending(eventId: eventId)
        let existingPot = Self.loadActivePot(eventId: eventId)
        self._activePot = State(initialValue: existingPot)
        self._goalAmountText = State(initialValue: Self.initialGoalAmountText(for: existingPot))
        self._statusText = State(initialValue: Self.statusText(for: existingPot))
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                Phase5SyncBanner(pendingSync: pendingSync, isOnline: !pendingSync)

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                    WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                            Label(String(localized: "payment.shared_title"), systemImage: "creditcard.fill")
                                .font(WakeveTheme.Typography.section)

                            Text(statusText)
                                .font(WakeveTheme.Typography.callout)
                                .foregroundStyle(.secondary)

                            if let activePot {
                                HStack {
                                    PriceDisplay(amount: activePot.currentAmount, currency: activePot.currency, style: .large)
                                    Text(String(format: String(localized: "payment.goal_ratio_format"), formatCurrencyAmount(activePot.goalAmount, currency: activePot.currency)))
                                        .font(WakeveTheme.Typography.callout)
                                        .foregroundStyle(.secondary)
                                }
                            } else {
                                Text(String(localized: "payment.no_active_body"))
                                    .font(WakeveTheme.Typography.callout)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }

                    PaymentTrustChecklistCard(
                        title: String(localized: "payment.trust.title"),
                        items: [
                            PaymentTrustChecklistItem(
                                id: "positive-goal",
                                systemImage: "number.circle.fill",
                                title: String(localized: "payment.trust.goal_title"),
                                detail: String(localized: "payment.trust.goal_detail")
                            ),
                            PaymentTrustChecklistItem(
                                id: "local-sync",
                                systemImage: "arrow.triangle.2.circlepath.circle.fill",
                                title: String(localized: "payment.trust.sync_title"),
                                detail: String(localized: "payment.trust.sync_detail")
                            ),
                            PaymentTrustChecklistItem(
                                id: "verified-links",
                                systemImage: "lock.shield.fill",
                                title: String(localized: "payment.trust.link_title"),
                                detail: String(localized: "payment.trust.link_detail")
                            )
                        ]
                    )

                    WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                            Text(String(localized: "payment.goal_section"))
                                .font(WakeveTheme.Typography.section)

                            TextField(String(localized: "payment.goal_placeholder"), text: $goalAmountText)
                                .keyboardType(.decimalPad)
                                .padding(.horizontal, WakeveTheme.Spacing.md)
                                .frame(height: 52)
                                .background(.thinMaterial, in: RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
                                .disabled(!canManagePayment || activePot != nil)
                                .accessibilityLabel(String(localized: "payment.goal_section"))

                            Text(goalAmountHelpText)
                                .font(WakeveTheme.Typography.caption)
                                .foregroundStyle(.secondary)
                        }
                    }

                    WakeveContentCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                            Text(String(localized: "payment.actions"))
                                .font(WakeveTheme.Typography.section)

                            WakeveActionButton(
                                String(localized: "payment.create_pot"),
                                systemImage: "plus.circle.fill",
                                variant: .primary,
                                isDisabled: !canCreateConfiguredPot
                            ) {
                                onCreatePaymentPot()
                            }

                            WakeveActionButton(
                                String(localized: "payment.activate_pot"),
                                systemImage: "checkmark.seal.fill",
                                variant: .secondary,
                                isDisabled: activePot == nil && !canCreateConfiguredPot
                            ) {
                                onActivatePaymentPot()
                            }

                            WakeveActionButton(
                                String(localized: "payment.open_pot"),
                                systemImage: "arrow.up.right.square",
                                variant: .neutral,
                                isDisabled: activePot == nil
                            ) {
                                onOpenPaymentPot()
                            }

                            WakeveActionButton(
                                String(localized: "payment.close_pot"),
                                systemImage: "xmark.circle",
                                variant: .destructive,
                                isDisabled: !canManagePayment || activePot == nil
                            ) {
                                onClosePaymentPot()
                            }
                        }
                    }
                }
                .padding(WakeveTheme.Spacing.md)
            }
            .background(WakeveScreenBackground(style: .grouped))
            .navigationTitle(String(localized: "payment.title"))
        }
    }

    private var parsedGoalAmount: Double? {
        let normalized = goalAmountText
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .replacingOccurrences(of: ",", with: ".")
        return Double(normalized)
    }

    private var canCreateConfiguredPot: Bool {
        canManagePayment && activePot == nil && (parsedGoalAmount ?? 0) > 0
    }

    private var goalAmountHelpText: String {
        if activePot != nil {
            return String(localized: "payment.help.locked")
        }

        if !canManagePayment {
            return isReadOnly ? String(localized: "payment.help.read_only") : String(localized: "payment.help.organizer_only")
        }

        return String(localized: "payment.help.positive_required")
    }

    private static func loadActivePot(eventId: String) -> PaymentPotRecord? {
        let repository = PaymentPotRepository(db: RepositoryProvider.shared.database)
        return repository.getActivePotForEvent(eventId: eventId)
    }

    private static func initialGoalAmountText(for pot: PaymentPotRecord?) -> String {
        guard let pot, pot.goalAmount > 0 else { return "" }
        return decimalText(pot.goalAmount)
    }

    private static func statusText(for pot: PaymentPotRecord?) -> String {
        guard let pot else { return String(localized: "payment.status.none") }
        return String(format: String(localized: "payment.status.active_format"), formatCurrencyAmount(pot.goalAmount, currency: pot.currency))
    }

    private static func decimalText(_ amount: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .decimal
        formatter.maximumFractionDigits = 2
        formatter.minimumFractionDigits = 0
        formatter.usesGroupingSeparator = false
        return formatter.string(from: NSNumber(value: amount)) ?? "\(amount)"
    }

    private func onCreatePaymentPot() {
        guard canCreateConfiguredPot, let goalAmount = parsedGoalAmount else {
            statusText = String(localized: "payment.status.create_requires_positive")
            return
        }

        let repository = PaymentPotRepository(db: RepositoryProvider.shared.database)
        let pot = repository.createPot(
            eventId: eventId,
            organizerId: currentUserId,
            goalAmount: goalAmount,
            title: String(format: String(localized: "payment.default_title_format"), eventTitle),
            currency: "EUR",
            paymentProvider: "WAKEVE_LOCAL",
            tricountGroupId: nil,
            tricountGroupUrl: nil
        )
        activePot = pot
        statusText = String(format: String(localized: "payment.status.created_pending_format"), formatCurrencyAmount(pot.goalAmount, currency: pot.currency))
    }

    private func onActivatePaymentPot() {
        if activePot == nil {
            onCreatePaymentPot()
        } else {
            statusText = String(localized: "payment.status.ready_to_share")
        }
    }

    private func onOpenPaymentPot() {
        guard let activePot else {
            statusText = String(localized: "payment.status.none_to_open")
            return
        }

        if let url = activePot.tricountGroupUrl, !url.isEmpty {
            statusText = String(localized: "payment.status.provider_verified")
        } else {
            statusText = String(localized: "payment.status.local_only")
        }
    }

    private func onClosePaymentPot() {
        guard canManagePayment else { return }
        let repository = PaymentPotRepository(db: RepositoryProvider.shared.database)
        if let pot = activePot ?? repository.getActivePotForEvent(eventId: eventId),
           repository.closePot(id: pot.id) != nil {
            activePot = nil
            goalAmountText = ""
            statusText = String(localized: "payment.status.closed_pending")
        }
    }
}

private struct TricountHandoffView: View {
    let eventId: String
    let currentUserId: String
    let isOrganizer: Bool
    let isReadOnly: Bool
    private let canManageTricount: Bool
    private let pendingSync: Bool
    @Environment(\.openURL) private var openURL
    @State private var safeLink: SafeExternalLink?
    @State private var handoffStatusText: String
    @State private var tricountURLText: String

    init(eventId: String, currentUserId: String, isOrganizer: Bool, canManageTricount: Bool, isReadOnly: Bool = false) {
        self.eventId = eventId
        self.currentUserId = currentUserId
        self.isOrganizer = isOrganizer
        self.isReadOnly = isReadOnly
        self.canManageTricount = canManageTricount
        self.pendingSync = Phase5PendingSync.selectPending(eventId: eventId)
        self._safeLink = State(initialValue: Self.loadSafeLink(eventId: eventId))
        self._handoffStatusText = State(initialValue: Self.loadStatusText(eventId: eventId))
        self._tricountURLText = State(initialValue: Self.initialTricountURLText(eventId: eventId))
    }

    private static func loadSafeLink(eventId: String) -> SafeExternalLink? {
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        let handoff = repository.getHandoff(eventId: eventId)
        return handoff?.providerUrl.flatMap { rawUrl in
            SafeExternalLink.sanitize(
                label: String(localized: "tricount.open"),
                provider: "TRICOUNT",
                rawURL: rawUrl,
                verifier: { provider, url in
                    repository.isTrustedProviderUrl(provider: provider, providerUrl: url)
                }
            )
        }
    }

    private static func loadStatusText(eventId: String) -> String {
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        let readiness = repository.getPaymentReadiness(eventId: eventId)

        if readiness.handoff?.explicitNotNeeded == true {
            return String(localized: "tricount.status.not_needed")
        }

        if readiness.complete {
            return String(localized: "tricount.status.verified")
        }

        return String(localized: "tricount.status.missing")
    }

    private static func initialTricountURLText(eventId: String) -> String {
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        return repository.getHandoff(eventId: eventId)?.providerUrl ?? ""
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                Phase5SyncBanner(pendingSync: pendingSync, isOnline: !pendingSync)

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                    WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                            Label(String(localized: "tricount.title"), systemImage: "link.circle.fill")
                                .font(WakeveTheme.Typography.section)

                            Text(handoffStatusText)
                                .font(WakeveTheme.Typography.callout)
                                .foregroundStyle(.secondary)

                            if let safeLink, safeLink.isVerified, safeLink.validatedURL != nil {
                                WakeveActionButton(
                                    safeLink.label,
                                    systemImage: "link",
                                    variant: .primary
                                ) {
                                    WakeveHaptics.selection()
                                    openSafeURL(safeLink)
                                }
                                Text(String(format: String(localized: "tricount.link_status_format"), safeLink.verificationStatus))
                                    .font(WakeveTheme.Typography.caption)
                                    .foregroundStyle(.secondary)
                            } else {
                                Label(String(localized: "tricount.no_verified_link"), systemImage: "lock.shield")
                                    .font(WakeveTheme.Typography.callout)
                                Text(String(localized: "tricount.safe_link_explanation"))
                                    .font(WakeveTheme.Typography.caption)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }

                    PaymentTrustChecklistCard(
                        title: String(localized: "tricount.trust.title"),
                        items: [
                            PaymentTrustChecklistItem(
                                id: "verified-url",
                                systemImage: "link.circle.fill",
                                title: String(localized: "tricount.trust.url_title"),
                                detail: String(localized: "tricount.trust.url_detail")
                            ),
                            PaymentTrustChecklistItem(
                                id: "explicit-decision",
                                systemImage: "checkmark.circle.fill",
                                title: String(localized: "tricount.trust.decision_title"),
                                detail: String(localized: "tricount.trust.decision_detail")
                            ),
                            PaymentTrustChecklistItem(
                                id: "readonly-final",
                                systemImage: "lock.fill",
                                title: String(localized: "tricount.trust.readonly_title"),
                                detail: String(localized: "tricount.trust.readonly_detail")
                            )
                        ]
                    )

                    WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                            Text(String(localized: "tricount.decision"))
                                .font(WakeveTheme.Typography.section)

                            TextField(String(localized: "tricount.url_placeholder"), text: $tricountURLText)
                                .keyboardType(.URL)
                                .textInputAutocapitalization(.never)
                                .autocorrectionDisabled()
                                .padding(.horizontal, WakeveTheme.Spacing.md)
                                .frame(height: 52)
                                .background(.thinMaterial, in: RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
                                .disabled(!canManageTricount)
                                .accessibilityLabel(String(localized: "tricount.url_section"))

                            Text(String(localized: "tricount.url_help"))
                                .font(WakeveTheme.Typography.caption)
                                .foregroundStyle(.secondary)

                            WakeveActionButton(
                                String(localized: "tricount.link"),
                                systemImage: "link.badge.plus",
                                variant: .primary,
                                isDisabled: !canManageTricount
                            ) {
                                onLinkTricount()
                            }

                            WakeveActionButton(
                                String(localized: "tricount.unlink"),
                                systemImage: "link.badge.minus",
                                variant: .neutral,
                                isDisabled: !canManageTricount
                            ) {
                                onUnlinkTricount()
                            }

                            WakeveActionButton(
                                String(localized: "tricount.not_needed"),
                                systemImage: "checkmark.shield",
                                variant: .secondary,
                                isDisabled: !canManageTricount
                            ) {
                                onMarkTricountNotNeeded()
                            }
                        }
                    }
                }
                .padding(WakeveTheme.Spacing.md)
            }
            .background(WakeveScreenBackground(style: .grouped))
            .navigationTitle(String(localized: "tricount.title"))
        }
    }

    private func onLinkTricount() {
        guard canManageTricount else { return }
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        let trimmedURL = tricountURLText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard repository.isTrustedProviderUrl(provider: "TRICOUNT", providerUrl: trimmedURL) else {
            safeLink = nil
            handoffStatusText = String(localized: "tricount.status.invalid_url")
            WakeveHaptics.warning()
            return
        }

        _ = repository.linkHandoff(
            eventId: eventId,
            provider: "TRICOUNT",
            providerId: "tricount-\(eventId)",
            providerUrl: trimmedURL,
            syncStatus: "LINKED"
        )
        safeLink = Self.loadSafeLink(eventId: eventId)
        handoffStatusText = Self.loadStatusText(eventId: eventId)
        WakeveHaptics.success()
    }

    private func onUnlinkTricount() {
        guard canManageTricount else { return }
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        repository.unlinkHandoff(eventId: eventId)
        safeLink = nil
        tricountURLText = ""
        handoffStatusText = Self.loadStatusText(eventId: eventId)
        WakeveHaptics.selection()
    }

    private func onMarkTricountNotNeeded() {
        guard canManageTricount else { return }
        let repository = TricountHandoffRepository(db: RepositoryProvider.shared.database)
        _ = repository.markNotNeeded(eventId: eventId, decidedBy: currentUserId)
        safeLink = nil
        tricountURLText = ""
        handoffStatusText = Self.loadStatusText(eventId: eventId)
        WakeveHaptics.success()
    }

    private func openSafeURL(_ link: SafeExternalLink) {
        SafeURLOpener.openSafeURL(link, openURL: openURL)
        _ = link.validatedURL
    }
}


private enum SafeURLOpener {
    static func openSafeURL(_ link: SafeExternalLink, openURL: OpenURLAction) {
        guard link.isVerified, let validatedURL = link.validatedURL else {
            return
        }

        openURL(validatedURL)
    }
}

private struct SafeExternalLink {
    let label: String
    let provider: String
    let validatedURL: URL?
    let verificationStatus: String
    let isVerified: Bool

    static func sanitize(
        label: String,
        provider: String,
        rawURL: String,
        verifier: (String, String) -> Bool
    ) -> SafeExternalLink? {
        let trimmed = rawURL.trimmingCharacters(in: .whitespacesAndNewlines)
        guard verifier(provider, trimmed), let url = URL(string: trimmed), url.scheme == "https" else {
            return nil
        }

        return SafeExternalLink(
            label: label,
            provider: provider,
            validatedURL: url,
            verificationStatus: String(localized: "safe_link.verified"),
            isVerified: true
        )
    }
}

private enum Phase5PendingSync {
    static func selectPending(eventId: String) -> Bool {
        RepositoryProvider.shared.database.syncMetadataQueries.selectPending().executeAsList().contains { pending in
            let phase5Types = [
                "meeting",
                "budget",
                "budget_item",
                "expense",
                "settlement",
                "payment",
                "payment_pot",
                "tricount",
                "tricount_handoff"
            ]
            return phase5Types.contains(pending.entityType) &&
                (pending.entityId == eventId || pending.entityId.hasPrefix("\(eventId):") || pending.entityId.contains(eventId))
        }
    }
}

#if DEBUG
struct ContentView_Previews: PreviewProvider {
    static var previews: some View {
        ContentView()
            .previewEnvironment(isAuthenticated: true)
            .preferredColorScheme(.light)
    }
}

#Preview("Content - Login") {
    ContentView()
        .previewEnvironment(isAuthenticated: false)
        .preferredColorScheme(.dark)
}
#endif
