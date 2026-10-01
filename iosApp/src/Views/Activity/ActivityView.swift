import SwiftUI

/// Zone Activité de la refonte (couche 6, #47) : possède le modèle de vue (créé une seule fois par
/// montage), le recharge à chaque entrée dans la zone (`reloadToken`) et publie le badge.
/// Mêmes paramètres de shell que `InboxView` ; toujours à sa racine (aucun détail poussé).
struct ActivityView: View {
    @StateObject private var viewModel: ActivityViewModel
    /// Badge de la barre flottante : éléments « À traiter » + notifications non lues.
    @Binding var actionCount: Int
    var reloadToken: Int = 0
    var onRootStateChange: ((Bool) -> Void)? = nil
    /// Filtre demandé (lien profond `wakeve://notifications`), appliqué à chaque nouvelle demande.
    var initialFilter: ActivityFilter = .toDo
    var filterRequestID: Int = 0
    let onOpen: (ActivityTarget) -> Void

    init(
        userId: String,
        actionCount: Binding<Int>,
        reloadToken: Int = 0,
        onRootStateChange: ((Bool) -> Void)? = nil,
        initialFilter: ActivityFilter = .toDo,
        filterRequestID: Int = 0,
        onOpen: @escaping (ActivityTarget) -> Void
    ) {
        let seenStore = UserDefaultsActivitySeenStore(userId: userId)
        _viewModel = StateObject(wrappedValue: ActivityViewModel(
            viewerId: userId, source: SharedActivitySource(seenStore: seenStore), seenStore: seenStore
        ))
        _actionCount = actionCount
        self.reloadToken = reloadToken
        self.onRootStateChange = onRootStateChange
        self.initialFilter = initialFilter
        self.filterRequestID = filterRequestID
        self.onOpen = onOpen
    }

    var body: some View {
        ActivityFeedView(viewModel: viewModel, onOpen: open)
            .task {
                viewModel.filter = initialFilter
                await viewModel.reload()
            }
            .onAppear { onRootStateChange?(true) }
            .onChange(of: reloadToken) { _, _ in
                Task { await viewModel.reload() }
            }
            .onChange(of: filterRequestID) { _, _ in viewModel.filter = initialFilter }
            .onChange(of: viewModel.badgeCount, initial: true) { _, count in actionCount = count }
    }

    private func open(_ entry: ActivityEntry) {
        if let notificationId = entry.notificationId {
            viewModel.markRead(notificationId: notificationId)
        }
        guard let target = entry.target else { return }
        if case .comments(let eventId) = target {
            // La ligne « N nouveaux messages » disparaît au retour.
            Task { await viewModel.markSeen(eventId: eventId) }
        }
        onOpen(target)
    }
}

/// Rendu du fil : titre, segments « À traiter (n) » / « Tout », cartes par événement.
struct ActivityFeedView: View {
    @ObservedObject var viewModel: ActivityViewModel
    let onOpen: (ActivityEntry) -> Void

    static func filterTitle(_ filter: ActivityFilter, count: Int, locale: Locale = WK.appLocale) -> String {
        switch filter {
        case .toDo:
            return String(format: WK.localizedFormat("activity.feed.filter.todo_format", locale: locale), locale: locale, count)
        case .all:
            return WK.localizedFormat("activity.feed.filter.all", locale: locale)
        }
    }

    static func emptyText(for filter: ActivityFilter, locale: Locale = WK.appLocale) -> String {
        WK.localizedFormat(filter == .toDo ? "activity.feed.empty.todo" : "activity.feed.empty.all", locale: locale)
    }

    static func groupTitle(_ group: ActivityEventGroup, locale: Locale = WK.appLocale) -> String {
        group.title ?? WK.localizedFormat("inbox.general_conversation", locale: locale)
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: WK.Space.md) {
                Text(String(localized: "wk.nav.activity"))
                    .font(WK.Typo.display)
                    .foregroundStyle(WK.Colors.textPrimary)
                    .accessibilityAddTraits(.isHeader)

                Picker(selection: $viewModel.filter) {
                    Text(Self.filterTitle(.toDo, count: viewModel.toDoCount)).tag(ActivityFilter.toDo)
                    Text(Self.filterTitle(.all, count: viewModel.toDoCount)).tag(ActivityFilter.all)
                } label: {
                    Text(String(localized: "wk.nav.activity"))
                }
                .pickerStyle(.segmented)
                .wkAccessibilityID("activity.filter")

                content
            }
            .padding(.horizontal, WK.Space.screen)
            .padding(.vertical, WK.Space.md)
        }
        .background(WK.Colors.canvas.ignoresSafeArea())
        .refreshable { await viewModel.reload() }
    }

    @ViewBuilder
    private var content: some View {
        switch viewModel.state {
        case .loading:
            ProgressView()
                .frame(maxWidth: .infinity)
                .accessibilityLabel(String(localized: "common.loading"))
        case .failed:
            WKCard(padding: WK.Space.md, radius: WK.Radius.lg) {
                Text(String(localized: "common.error_generic"))
                    .font(WK.Typo.body)
                    .foregroundStyle(WK.Colors.textPrimary)
                WKChip(
                    title: String(localized: "common.retry"),
                    systemImage: "arrow.clockwise",
                    accessibilityID: "activity.retry",
                    action: { Task { await viewModel.reload() } }
                )
            }
        case .loaded:
            if viewModel.groups.isEmpty {
                WKCard(padding: WK.Space.md, radius: WK.Radius.lg) {
                    Text(Self.emptyText(for: viewModel.filter))
                        .font(WK.Typo.body)
                        .foregroundStyle(WK.Colors.textMuted)
                }
                .wkAccessibilityID("activity.empty")
            } else {
                ForEach(viewModel.groups) { group in
                    groupCard(group)
                }
            }
        }
    }

    private func groupCard(_ group: ActivityEventGroup) -> some View {
        WKCard(padding: WK.Space.md, radius: WK.Radius.lg) {
            Text(Self.groupTitle(group))
                .font(WK.Typo.headline)
                .foregroundStyle(WK.Colors.textPrimary)
                .accessibilityAddTraits(.isHeader)
            ForEach(group.entries) { entry in
                ActivityEntryRow(entry: entry, now: Date(), onOpen: { onOpen(entry) })
            }
        }
        .wkAccessibilityID("activity.group.\(group.id)")
    }
}

/// Ligne du fil : point rouge si une action est attendue (doublé du libellé « À traiter » pour
/// VoiceOver), libellé, détail éventuel, date relative courte.
struct ActivityEntryRow: View {
    let entry: ActivityEntry
    let now: Date
    let onOpen: () -> Void
    @ScaledMetric(relativeTo: .body) private var dotSize: CGFloat = WK.Space.xs

    static func title(for entry: ActivityEntry, locale: Locale = WK.appLocale) -> String {
        switch entry.kind {
        case .voteRequired: return WK.localizedFormat("activity.feed.vote_required", locale: locale)
        case .readyToConfirm: return WK.localizedFormat("activity.feed.ready_to_confirm", locale: locale)
        case .rsvpPending: return WK.localizedFormat("activity.feed.rsvp_pending", locale: locale)
        case .notification(let title, _, _): return title
        case .messages(let count): return HubSummaryText.plural("activity.feed.messages_count", count, locale: locale)
        }
    }

    static func detail(for entry: ActivityEntry) -> String? {
        if case .notification(_, let message, _) = entry.kind, !message.isEmpty { return message }
        return nil
    }

    /// Ouvre une destination, ou marque lue une notification générale non lue.
    static func isInteractive(_ entry: ActivityEntry) -> Bool {
        entry.target != nil || isUnread(entry)
    }

    static func isUnread(_ entry: ActivityEntry) -> Bool {
        if case .notification(_, _, let isRead) = entry.kind { return !isRead }
        return false
    }

    /// Date relative courte (« il y a 2 h »).
    static func relativeDate(_ date: Date, now: Date, locale: Locale = WK.appLocale) -> String {
        let formatter = RelativeDateTimeFormatter()
        formatter.locale = locale
        formatter.unitsStyle = .short
        return formatter.localizedString(for: date, relativeTo: now)
    }

    static func accessibilityLabel(for entry: ActivityEntry, now: Date, locale: Locale = WK.appLocale) -> String {
        var parts: [String] = []
        if entry.needsAction { parts.append(WK.localizedFormat("activity.feed.a11y.to_do", locale: locale)) }
        parts.append(title(for: entry, locale: locale))
        if let detail = detail(for: entry) { parts.append(detail) }
        if isUnread(entry) { parts.append(WK.localizedFormat("activity.feed.unread", locale: locale)) }
        if let date = entry.date { parts.append(relativeDate(date, now: now, locale: locale)) }
        return parts.joined(separator: ", ")
    }

    var body: some View {
        // Sans destination ni notification à marquer lue, la ligne reste lisible mais n'est pas un bouton.
        Group {
            if Self.isInteractive(entry) {
                Button(action: onOpen) { content }
                    .buttonStyle(.plain)
            } else {
                content
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(Self.accessibilityLabel(for: entry, now: now))
        .accessibilityAddTraits(Self.isInteractive(entry) ? .isButton : [])
        .wkAccessibilityID("activity.entry.\(entry.id)")
    }

    private var content: some View {
        HStack(alignment: .firstTextBaseline, spacing: WK.Space.xs) {
            Circle()
                .fill(entry.needsAction ? WK.Status.actionNeeded.color : Color.clear)
                .frame(width: dotSize, height: dotSize)
            VStack(alignment: .leading, spacing: WK.Space.xxxs) {
                Text(Self.title(for: entry))
                    .font(WK.Typo.body)
                    .fontWeight(Self.isUnread(entry) || entry.needsAction ? .semibold : .regular)
                    .foregroundStyle(WK.Colors.textPrimary)
                if let detail = Self.detail(for: entry) {
                    Text(detail)
                        .font(WK.Typo.caption)
                        .foregroundStyle(WK.Colors.textMuted)
                        .lineLimit(2)
                }
                if let date = entry.date {
                    Text(Self.relativeDate(date, now: now))
                        .font(WK.Typo.micro)
                        .foregroundStyle(WK.Colors.textMuted)
                }
            }
            Spacer(minLength: 0)
            if entry.target != nil {
                Image(systemName: "chevron.right")
                    .font(WK.Typo.caption)
                    .foregroundStyle(WK.Colors.textMuted)
            }
        }
        .frame(maxWidth: .infinity, minHeight: WK.Size.minTapTarget, alignment: .leading)
        .contentShape(Rectangle())
    }
}
