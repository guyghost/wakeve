import SwiftUI

/// Accueil de la zone Événements sous `iosRedesign2026` (couche 3, #47) :
/// « Prochaine étape », grille d'événements, section « Passés » repliée, état vide et synchro discrète.
struct EventsHomeView: View {
    @ObservedObject var viewModel: EventsHomeViewModel
    let onOpenEvent: (String) -> Void
    let onNextStep: (HomeNextStep) -> Void
    let onCreate: () -> Void
    let onEditDraft: (String) -> Void
    let onDelete: (String) -> Void

    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @State private var showsPast = false

    static func columnCount(for size: DynamicTypeSize) -> Int { size.isAccessibilitySize ? 1 : 2 }

    /// Sous-titre de la carte « Prochaine étape », avec l'échéance quand elle est connue.
    static func subtitle(for step: HomeNextStep, locale: Locale = WK.appLocale) -> String {
        func text(_ key: String) -> String { WK.localizedFormat(key, locale: locale) }
        func withDeadline(_ base: String) -> String {
            guard let days = step.daysLeft else { return base }
            return base + " · " + HomeDateText.closesIn(days: days, locale: locale)
        }
        switch step.kind {
        case .voteRequired: return withDeadline(text("home.v2.next_step.vote.subtitle"))
        case .readyToConfirm: return text("home.v2.next_step.ready.subtitle")
        case .pollInProgress: return withDeadline(text("home.v2.next_step.polling.subtitle"))
        case .organizing: return text("home.v2.next_step.organizing.subtitle")
        }
    }

    static func actionTitle(for step: HomeNextStep) -> String {
        switch step.kind {
        case .voteRequired: return String(localized: "home.v2.next_step.action.vote")
        case .readyToConfirm: return String(localized: "home.v2.next_step.action.choose_date")
        case .pollInProgress: return String(localized: "home.v2.next_step.action.results")
        case .organizing: return String(localized: "home.v2.next_step.action.organize")
        }
    }

    private var columns: [GridItem] {
        Array(
            repeating: GridItem(.flexible(), spacing: WK.Space.sm, alignment: .top),
            count: Self.columnCount(for: dynamicTypeSize)
        )
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: WK.Space.md) {
                if viewModel.pendingSyncCount > 0 {
                    syncBanner
                }

                if let step = viewModel.nextStep {
                    WKHeroMetric(
                        caption: String(
                            format: String(localized: "home.v2.next_step.caption_format"),
                            step.title
                        ),
                        value: step.value,
                        unit: step.unit,
                        subtitle: Self.subtitle(for: step),
                        actionTitle: Self.actionTitle(for: step),
                        action: { onNextStep(step) }
                    )
                    .wkAccessibilityID("home.nextStep")
                }

                switch viewModel.state {
                case .loading:
                    ProgressView()
                        .frame(maxWidth: .infinity)
                        .accessibilityLabel(String(localized: "common.loading"))
                case .empty:
                    emptyState
                case .failed:
                    failedState
                case .loaded:
                    grid(viewModel.active)
                }

                if !viewModel.past.isEmpty {
                    DisclosureGroup(isExpanded: $showsPast) {
                        grid(viewModel.past)
                            .padding(.top, WK.Space.xs)
                    } label: {
                        Text(String(localized: "home.v2.section.past"))
                            .font(WK.Typo.headline)
                            .foregroundStyle(WK.Colors.textPrimary)
                    }
                    .tint(WK.Colors.textMuted)
                    .wkAccessibilityID("home.section.past")
                }
            }
            .padding(.horizontal, WK.Space.screen)
            .padding(.vertical, WK.Space.md)
        }
        .background(WK.Colors.canvas.ignoresSafeArea())
        .refreshable { await viewModel.reload() }
        .task { await viewModel.reload() }
    }

    private var syncBanner: some View {
        Label(String(localized: "home.v2.sync.pending"), systemImage: "arrow.triangle.2.circlepath")
            .font(WK.Typo.caption)
            .foregroundStyle(WK.Colors.textMuted)
            .frame(maxWidth: .infinity, alignment: .leading)
            .wkAccessibilityID("home.sync.pending")
    }

    private var emptyState: some View {
        WKCard(padding: WK.Space.md, radius: WK.Radius.lg) {
            Text(String(localized: "home.v2.empty.title"))
                .font(WK.Typo.title)
                .foregroundStyle(WK.Colors.textPrimary)
            Text(String(localized: "home.v2.empty.body"))
                .font(WK.Typo.body)
                .foregroundStyle(WK.Colors.textMuted)
                .padding(.bottom, WK.Space.xs)
            WKPrimaryButton(
                title: String(localized: "home.v2.empty.action"),
                accessibilityID: "home.empty.create",
                action: onCreate
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
                accessibilityID: "home.retry",
                action: { Task { await viewModel.reload() } }
            )
        }
    }

    private func grid(_ items: [HomeEventSummary]) -> some View {
        LazyVGrid(columns: columns, spacing: WK.Space.sm) {
            ForEach(items) { summary in
                HomeEventCard(summary: summary, onOpen: { onOpenEvent(summary.id) })
                    .contextMenu { menu(for: summary) }
            }
        }
    }

    @ViewBuilder
    private func menu(for summary: HomeEventSummary) -> some View {
        Button {
            onOpenEvent(summary.id)
        } label: {
            Label(String(localized: "home.v2.menu.open"), systemImage: "arrow.up.forward.app")
        }
        if summary.facts.phase == .draft && summary.facts.role == .organizer {
            Button {
                onEditDraft(summary.id)
            } label: {
                Label(String(localized: "home.v2.menu.edit"), systemImage: "pencil")
            }
        }
        if summary.canDelete {
            Button(role: .destructive) {
                onDelete(summary.id)
            } label: {
                Label(String(localized: "home.v2.menu.delete"), systemImage: "trash")
            }
        }
    }
}

/// Possède le modèle de vue de l'accueil (créé une seule fois par montage) et le recharge
/// quand `reloadToken` change (retour dans la zone Événements, suppression).
struct EventsHomeContainer: View {
    @StateObject private var viewModel: EventsHomeViewModel
    let reloadToken: Int
    let onOpenEvent: (String) -> Void
    let onNextStep: (HomeNextStep) -> Void
    let onCreate: () -> Void
    let onEditDraft: (String) -> Void
    let onDelete: (String) -> Void

    init(
        userId: String,
        reloadToken: Int,
        onOpenEvent: @escaping (String) -> Void,
        onNextStep: @escaping (HomeNextStep) -> Void,
        onCreate: @escaping () -> Void,
        onEditDraft: @escaping (String) -> Void,
        onDelete: @escaping (String) -> Void
    ) {
        _viewModel = StateObject(
            wrappedValue: EventsHomeViewModel(viewerId: userId, source: SharedEventsHomeSource())
        )
        self.reloadToken = reloadToken
        self.onOpenEvent = onOpenEvent
        self.onNextStep = onNextStep
        self.onCreate = onCreate
        self.onEditDraft = onEditDraft
        self.onDelete = onDelete
    }

    var body: some View {
        EventsHomeView(
            viewModel: viewModel,
            onOpenEvent: onOpenEvent,
            onNextStep: onNextStep,
            onCreate: onCreate,
            onEditDraft: onEditDraft,
            onDelete: onDelete
        )
        .onChange(of: reloadToken) { _, _ in
            Task { await viewModel.reload() }
        }
    }
}
