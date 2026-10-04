import SwiftUI
import Shared

/// Flux de création de la refonte (couche 7, #47, spec §5.5) : quatre questions, une par écran,
/// brouillon enregistré à chaque étape, validation par étape, « Lancer le sondage » à la fin.
/// Possède l'état ; le rendu est dans `CreateEventFlowScreen`.
struct CreateEventFlow: View {
    let userId: String
    let draftEventId: String?
    let initialScenario: EventScenario?
    let onClose: () -> Void
    let onLaunched: (Event, EventCreationContext) -> Void
    /// Checklist du modèle, gardée avec le brouillon à chaque étape enregistrée.
    let checklistStore: EventChecklistStoring

    @StateObject private var controller: EventDraftFlowController
    @State private var form = CreateEventForm()
    @State private var step: CreateEventFlowStep = .what
    @State private var showErrors = false
    @State private var bannerMessage: String?
    @State private var didPrepare = false
    @State private var isLaunching = false
    /// Fermé : un enregistrement ou un lancement qui se termine ensuite n'ouvre plus rien.
    @State private var didClose = false

    init(
        userId: String,
        draftEventId: String? = nil,
        initialScenario: EventScenario? = nil,
        onClose: @escaping () -> Void,
        onLaunched: @escaping (Event, EventCreationContext) -> Void,
        checklistStore: EventChecklistStoring = UserDefaultsEventChecklistStore()
    ) {
        self.userId = userId
        self.draftEventId = draftEventId
        self.initialScenario = initialScenario
        self.onClose = onClose
        self.onLaunched = onLaunched
        self.checklistStore = checklistStore
        _controller = StateObject(wrappedValue: EventDraftFlowController(userId: userId))
    }

    var body: some View {
        CreateEventFlowScreen(
            form: $form,
            step: step,
            showErrors: showErrors,
            isDraftSaved: controller.isDraftSaved,
            isSaving: isBusy,
            bannerMessage: bannerMessage,
            actions: CreateEventFlowScreen.Actions(
                close: close,
                back: back,
                primary: primary,
                skip: primary
            )
        )
        .onAppear(perform: prepare)
        .onChange(of: step) { _, _ in
            showErrors = false
            bannerMessage = nil
        }
    }

    // MARK: - Actions

    private var isBusy: Bool { controller.isSaving || isLaunching }

    /// Reprise : brouillon relu, étape de la première validation en échec. Sinon modèle éventuel.
    private func prepare() {
        guard !didPrepare else { return }
        didPrepare = true
        if let draftEventId, var hydrated = controller.hydrate(eventId: draftEventId) {
            // La checklist du modèle n'est pas en base : relue sur l'appareil (le modèle lui-même n'est pas repris).
            hydrated.checklist = checklistStore.items(eventId: draftEventId)
                .filter { $0.source == .template }
                .map(\.title)
            form = hydrated
            step = hydrated.firstInvalidStep ?? .time
        } else if let initialScenario {
            form.apply(scenario: initialScenario)
        }
    }

    private func primary() {
        guard !isBusy, !didClose else { return }
        let errors = form.errors(for: step)
        guard errors.isEmpty else {
            showErrors = true
            announce(CreateEventFlowScreen.announcement(for: errors))
            return
        }
        Task { @MainActor in
            let result = await controller.save(step: step, form: form)
            guard !didClose else { return }
            switch result {
            case .saved:
                bannerMessage = nil
                if let eventId = controller.eventId {
                    checklistStore.seedTemplate(eventId: eventId, titles: form.checklist)
                }
                if let next = step.next {
                    step = next
                } else {
                    await launch()
                }
            case .failed(let message):
                bannerMessage = message
                announce(message)
            }
        }
    }

    private func launch() async {
        isLaunching = true
        defer { isLaunching = false }
        let result = await controller.launch()
        guard !didClose else { return }
        switch result {
        case .launched(let eventId):
            guard let event = RepositoryProvider.shared.repository.getEvent(id: eventId) else {
                onClose()
                return
            }
            onLaunched(event, EventCreationContext(form: form))
        case .failed(let message):
            bannerMessage = message
            announce(message)
        }
    }

    private func back() {
        guard !isBusy, let previous = step.previous else { return }
        step = previous
    }

    /// Le brouillon est déjà enregistré à chaque étape ; l'étape en cours l'est aussi si elle est valide,
    /// y compris une étape 1 valide pas encore continuée. Rien de saisi : fermer ne crée rien.
    private func close() {
        guard !isBusy, !didClose else { return }
        didClose = true
        guard form.savesOnClose(step: step, hasDraft: controller.eventId != nil) else {
            onClose()
            return
        }
        Task { @MainActor in
            _ = await controller.save(step: step, form: form)
            onClose()
        }
    }

    private func announce(_ text: String) {
        guard !text.isEmpty else { return }
        AccessibilityNotification.Announcement(text).post()
    }
}

// MARK: - Rendu

/// Écran d'une question : en-tête (fermer, progression, « Brouillon enregistré »), question, contenu, bas
/// (retour, action principale, « Passer » pour Où ?).
struct CreateEventFlowScreen: View {
    struct Actions {
        var close: () -> Void = {}
        var back: () -> Void = {}
        var primary: () -> Void = {}
        var skip: () -> Void = {}
    }

    @Binding var form: CreateEventForm
    let step: CreateEventFlowStep
    let showErrors: Bool
    let isDraftSaved: Bool
    let isSaving: Bool
    let bannerMessage: String?
    let actions: Actions

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    private var visibleErrors: [CreateEventField: String] {
        showErrors ? form.errors(for: step) : [:]
    }

    var body: some View {
        VStack(spacing: 0) {
            header
            ScrollView {
                VStack(alignment: .leading, spacing: WK.Space.lg) {
                    VStack(alignment: .leading, spacing: WK.Space.xs) {
                        Text(Self.question(for: step))
                            .font(WK.Typo.title)
                            .foregroundStyle(WK.Colors.textPrimary)
                            .fixedSize(horizontal: false, vertical: true)
                            .accessibilityAddTraits(.isHeader)
                            .accessibilityIdentifier("create_flow.question")
                        Text(Self.subtitle(for: step))
                            .font(WK.Typo.body)
                            .foregroundStyle(WK.Colors.textSecondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                    stepContent
                }
                .padding(.horizontal, WK.Space.screen)
                .padding(.vertical, WK.Space.md)
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .scrollDismissesKeyboard(.interactively)
            footer
        }
        .background(WK.Colors.canvas.ignoresSafeArea())
    }

    @ViewBuilder
    private var stepContent: some View {
        switch step {
        case .what: CreateFlowWhatStep(form: $form, errors: visibleErrors)
        case .who: CreateFlowWhoStep(form: $form, errors: visibleErrors)
        case .place: CreateFlowPlaceStep(form: $form, errors: visibleErrors)
        case .time: CreateFlowTimeStep(form: $form, errors: visibleErrors)
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: WK.Space.sm) {
            HStack(spacing: WK.Space.sm) {
                WKCircleButton(
                    systemImage: "xmark",
                    accessibilityLabel: String(localized: "common.close"),
                    accessibilityID: "create_flow.close",
                    action: actions.close
                )
                .disabled(isSaving)
                Spacer(minLength: WK.Space.xs)
                if isDraftSaved {
                    // Mention discrète : plafonnée pour laisser la place à la question en AX5.
                    Label(String(localized: "create_flow.draft_saved"), systemImage: "checkmark.circle")
                        .font(WK.Typo.caption)
                        .foregroundStyle(WK.Colors.textSecondary)
                        .multilineTextAlignment(.trailing)
                        .dynamicTypeSize(...DynamicTypeSize.accessibility1)
                        .accessibilityIdentifier("create_flow.draft_saved")
                }
            }
            CreateFlowProgress(step: step)
        }
        .padding(.horizontal, WK.Space.screen)
        .padding(.top, WK.Space.xs)
        .padding(.bottom, WK.Space.xxs)
        // Fond opaque : le contenu défilant ne passe pas sous la progression.
        .background(WK.Colors.canvas)
    }

    private var footer: some View {
        VStack(spacing: WK.Space.xs) {
            if let bannerMessage {
                CreateFlowFieldError(message: bannerMessage, accessibilityID: "create_flow.error.banner")
            }
            HStack(spacing: WK.Space.sm) {
                if step.previous != nil {
                    WKCircleButton(
                        systemImage: "chevron.left",
                        accessibilityLabel: String(localized: "common.back"),
                        accessibilityID: "create_flow.back",
                        action: actions.back
                    )
                    .disabled(isSaving)
                }
                WKPrimaryButton(
                    title: Self.primaryTitle(for: step),
                    // Icône seulement hors tailles d'accessibilité : le titre garde la largeur.
                    systemImage: step.isLast && !dynamicTypeSize.isAccessibilitySize ? "paperplane.fill" : nil,
                    accessibilityID: step.isLast ? "create_flow.launch" : "create_flow.continue",
                    isLoading: isSaving,
                    action: actions.primary
                )
            }
            if step.isSkippable, form.locations.isEmpty {
                Button(String(localized: "create_flow.skip"), action: actions.skip)
                    .font(WK.Typo.body.weight(.semibold))
                    .foregroundStyle(WK.Colors.accent)
                    .frame(maxWidth: .infinity, minHeight: WK.Size.minTapTarget)
                    .contentShape(Rectangle())
                    .accessibilityIdentifier("create_flow.skip")
            }
        }
        .padding(.horizontal, WK.Space.screen)
        .padding(.top, WK.Space.xs)
        .padding(.bottom, WK.Space.xs)
        .background(WK.Colors.canvas)
    }

    // MARK: Textes

    static func question(for step: CreateEventFlowStep, locale: Locale = WK.appLocale) -> String {
        WK.localizedFormat("create_flow.question.\(step.key)", locale: locale)
    }

    static func subtitle(for step: CreateEventFlowStep, locale: Locale = WK.appLocale) -> String {
        WK.localizedFormat("create_flow.subtitle.\(step.key)", locale: locale)
    }

    static func primaryTitle(for step: CreateEventFlowStep, locale: Locale = WK.appLocale) -> String {
        WK.localizedFormat(step.isLast ? "participants.start_poll.action" : "create_flow.continue", locale: locale)
    }

    static func progressLabel(for step: CreateEventFlowStep, locale: Locale = WK.appLocale) -> String {
        String(
            format: WK.localizedFormat("create_flow.a11y.progress_format", locale: locale),
            locale: locale, step.rawValue + 1, CreateEventFlowStep.allCases.count
        )
    }

    /// Texte annoncé à VoiceOver après un « Continuer » refusé : erreurs dans l'ordre des champs.
    static func announcement(for errors: [CreateEventField: String], locale: Locale = WK.appLocale) -> String {
        errors.sorted { order($0.key) < order($1.key) }
            .map { WK.localizedFormat($0.value, locale: locale) }
            .joined(separator: " ")
    }

    private static func order(_ field: CreateEventField) -> Int {
        switch field {
        case .title: return 0
        case .description: return 1
        case .customType: return 2
        case .minParticipants: return 3
        case .expectedParticipants: return 4
        case .maxParticipants: return 5
        case .location(let index): return 10 + index
        case .slots: return 1_000
        case .slot: return 1_001
        }
    }
}

/// Progression en segments ; lue « Étape n sur 4 ».
struct CreateFlowProgress: View {
    let step: CreateEventFlowStep

    var body: some View {
        HStack(spacing: WK.Space.xxs) {
            ForEach(Array(CreateEventFlowStep.segments(current: step).enumerated()), id: \.offset) { _, segment in
                Capsule()
                    .fill(segment == .upcoming ? WK.Colors.cardInset : WK.Colors.accent)
                    .frame(height: WK.Space.xxs)
                    .opacity(segment == .done ? 0.55 : 1)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(CreateEventFlowScreen.progressLabel(for: step))
        .accessibilityIdentifier("create_flow.progress")
    }
}

/// Erreur sous un champ : icône + texte (jamais la couleur seule), lue « Erreur : … ».
struct CreateFlowFieldError: View {
    let message: String
    var accessibilityID: String? = nil

    /// Indice VoiceOver du champ en erreur (clé de localisation) : l'erreur est lue avec le champ. Vide sinon.
    static func hint(_ errorKey: String?, locale: Locale = WK.appLocale) -> String {
        errorKey.map { WK.localizedFormat($0, locale: locale) } ?? ""
    }

    static func accessibilityText(_ message: String, locale: Locale = WK.appLocale) -> String {
        String(format: WK.localizedFormat("create_flow.a11y.error_format", locale: locale), locale: locale, message)
    }

    var body: some View {
        Label {
            Text(message).fixedSize(horizontal: false, vertical: true)
        } icon: {
            Image(systemName: "exclamationmark.circle.fill")
        }
        .font(WK.Typo.caption)
        .foregroundStyle(WK.Status.actionNeeded.onFill)
        .padding(.horizontal, WK.Space.sm)
        .padding(.vertical, WK.Space.xs)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(WK.Status.actionNeeded.fill, in: WK.shape(WK.Radius.sm))
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Self.accessibilityText(message))
        .wkAccessibilityID(accessibilityID)
    }
}
