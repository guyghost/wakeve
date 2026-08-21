import SwiftUI
import Shared

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

fileprivate enum EventLibraryFilter: String, CaseIterable, Identifiable {
    case drafts
    case hosting
    case attending
    case upcoming
    case past

    var id: String { rawValue }

    var projection: LibraryProjection {
        switch self {
        case .drafts: .drafts
        case .hosting: .hosting
        case .attending: .attending
        case .upcoming: .upcoming
        case .past: .past
        }
    }

    var title: String {
        switch self {
        case .drafts:
            String(localized: "invitation.library.filter.drafts")
        case .hosting:
            String(localized: "invitation.library.filter.hosting")
        case .attending:
            String(localized: "invitation.library.filter.attending")
        case .upcoming:
            String(localized: "invitation.library.filter.upcoming")
        case .past:
            String(localized: "invitation.library.filter.past")
        }
    }

    var accessibilityIdentifier: String {
        "eventLibraryFilter\(rawValue.prefix(1).uppercased())\(rawValue.dropFirst())"
    }
}

@MainActor
final class EventLibraryViewModel: ObservableObject {
    @Published private(set) var cards: [LibraryCardProjection] = []
    @Published fileprivate var selectedFilter: EventLibraryFilter = .upcoming
    @Published private(set) var isLoading = false
    @Published private(set) var loadState: Any = LibraryLoadStateIdle.shared

    let projector = EventLibraryProjector()
    private let viewerId: String
    private let projectionRepository: DatabaseInvitationExperienceProjectionRepository
    private var loadTask: Task<Void, Never>?
    private var previousStableLoadState: Any = LibraryLoadStateIdle.shared
    private var loadGeneration = UUID()

    init(
        viewerId: String = "",
        projectionRepository: DatabaseInvitationExperienceProjectionRepository =
            DatabaseInvitationExperienceProjectionRepository(
                database: RepositoryProvider.shared.database
            )
    ) {
        self.viewerId = viewerId
        self.projectionRepository = projectionRepository
    }

    var visibleCards: [LibraryCardProjection] {
        projector.filter(cards: cards, projection: selectedFilter.projection)
    }

    var hasLoadFailure: Bool {
        loadState is LibraryLoadStateFailed<NSArray>
    }

    func reload() async {
        let generation = UUID()
        loadGeneration = generation
        previousStableLoadState = loadState
        isLoading = true
        defer {
            if loadGeneration == generation { isLoading = false }
        }
        let now = Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
            epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        do {
            let state = try await projectionRepository.library(
                viewerId: viewerId,
                projection: selectedFilter.projection,
                now: now
            )
            guard !Task.isCancelled, loadGeneration == generation else { return }
            loadState = state
            if let ready = state as? LibraryLoadStateReady<NSArray>,
               let projectedCards = ready.snapshot as? [LibraryCardProjection] {
                cards = projectedCards
            } else if state is LibraryLoadStateEmpty {
                cards = []
            }
        } catch {
            guard !Task.isCancelled, loadGeneration == generation else { return }
            // Repository failures preserve the previous stable snapshot. The next
            // explicit retry asks the repository for a fresh typed LibraryLoadState.
            loadState = previousStableLoadState
        }
    }

    fileprivate func select(_ filter: EventLibraryFilter) {
        guard filter != selectedFilter else { return }
        cancelLoad()
        selectedFilter = filter
        loadTask = Task { await reload() }
    }

    func cancelLoad() {
        loadTask?.cancel()
        loadTask = nil
        loadGeneration = UUID()
        isLoading = false
        loadState = previousStableLoadState
    }

    func retry() {
        cancelLoad()
        loadTask = Task { await reload() }
    }
}

@MainActor
struct EventLibraryView: View {
    @StateObject private var viewModel: EventLibraryViewModel
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast

    let onOpenEvent: (Event) -> Void
    let onOpenCard: ((LibraryCardProjection) -> Void)?
    let onCreateEvent: () -> Void

    init(
        viewerId: String = "",
        viewModel: EventLibraryViewModel? = nil,
        onOpenEvent: @escaping (Event) -> Void,
        onOpenCard: ((LibraryCardProjection) -> Void)? = nil,
        onCreateEvent: @escaping () -> Void
    ) {
        _viewModel = StateObject(
            wrappedValue: viewModel ?? EventLibraryViewModel(viewerId: viewerId)
        )
        self.onOpenEvent = onOpenEvent
        self.onOpenCard = onOpenCard
        self.onCreateEvent = onCreateEvent
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: dynamicTypeSize.isAccessibilitySize ? 18 : 12) {
                    filterBar

                    if viewModel.isLoading {
                        VStack(spacing: 12) {
                            ProgressView()
                                .accessibilityLabel(String(localized: "common.loading"))
                            Button(String(localized: "common.cancel")) {
                                viewModel.cancelLoad()
                            }
                            .frame(minWidth: 44, minHeight: 44)
                        }
                        .frame(maxWidth: .infinity, minHeight: 120)
                    } else if viewModel.hasLoadFailure {
                        ContentUnavailableView {
                            Label(String(localized: "common.error"), systemImage: "exclamationmark.icloud")
                        } description: {
                            Text(String(localized: "common.error_generic"))
                        } actions: {
                            Button(String(localized: "common.retry")) {
                                viewModel.retry()
                            }
                            .frame(minWidth: 44, minHeight: 44)
                            .invitationAccessibilityIdentifier("eventLibraryRetryAction")
                        }
                    } else if viewModel.visibleCards.isEmpty {
                        ContentUnavailableView(
                            String(localized: "events.empty.title"),
                            systemImage: "calendar",
                            description: Text(String(localized: "events.empty.subtitle"))
                        )
                    } else {
                        ForEach(viewModel.visibleCards, id: \.event.id) { card in
                            Button {
                                if let onOpenCard {
                                    onOpenCard(card)
                                } else {
                                    onOpenEvent(card.event)
                                }
                            } label: {
                                HStack(alignment: .top, spacing: 12) {
                                    InvitationArtworkView(
                                        artwork: card.artwork,
                                        event: card.event
                                    )
                                    .frame(width: 58, height: 72)
                                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                                    VStack(alignment: .leading, spacing: 6) {
                                        Text(card.event.title)
                                            .font(.headline)
                                            .foregroundStyle(.primary)
                                            .fixedSize(horizontal: false, vertical: true)
                                        Text(nextActionTitle(card.nextAction))
                                            .font(.subheadline.weight(.medium))
                                            .foregroundStyle(.tint)
                                            .fixedSize(horizontal: false, vertical: true)
                                        if card.warning != nil {
                                            Label(
                                                String(localized: "invitation.state.stale"),
                                                systemImage: "exclamationmark.icloud"
                                            )
                                            .font(.caption)
                                            .foregroundStyle(.secondary)
                                        }
                                    }

                                    Spacer(minLength: 8)
                                    Image(systemName: "chevron.right")
                                        .foregroundStyle(.tertiary)
                                }
                                .padding(16)
                                .frame(maxWidth: .infinity, alignment: .leading)
                                .background(cardBackground)
                                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                            }
                            .buttonStyle(.plain)
                            .frame(minWidth: 44, minHeight: 44)
                            .accessibilitySortPriority(1)
                        }
                    }
                }
                .padding()
            }
            .navigationTitle(String(localized: "invitation.library.title"))
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button(action: onCreateEvent) {
                        Label(String(localized: "create_event.title"), systemImage: "plus")
                            .foregroundStyle(libraryPrimaryForeground)
                            .frame(minWidth: 44, minHeight: 44)
                    }
                    .buttonStyle(.borderedProminent)
                    .tint(libraryPrimaryTint)
                    .invitationAccessibilityIdentifier("eventLibraryPrimaryAction")
                }
            }
        }
        .task { await viewModel.reload() }
    }

    private var filterBar: some View {
        LazyVGrid(columns: filterColumns, alignment: .leading, spacing: 8) {
            ForEach(EventLibraryFilter.allCases) { filter in
                Button(filter.title) {
                    viewModel.select(filter)
                }
                .buttonStyle(.bordered)
                .tint(viewModel.selectedFilter == filter ? .accentColor : .secondary)
                .frame(maxWidth: .infinity, minHeight: 44, alignment: .leading)
                .id(filter.id)
                .invitationAccessibilityIdentifier(filter.accessibilityIdentifier)
                .accessibilityLabel(filter.title)
                .accessibilityAddTraits(viewModel.selectedFilter == filter ? .isSelected : [])
            }
        }
        .padding(.horizontal, WakeveTheme.Spacing.page)
        .accessibilitySortPriority(2)
    }

    private var filterColumns: [GridItem] {
        [
            GridItem(
                .adaptive(minimum: dynamicTypeSize.isAccessibilitySize ? 180 : 112),
                spacing: 8,
                alignment: .leading
            )
        ]
    }

    private var libraryPrimaryForeground: Color {
        colorSchemeContrast == .increased
            ? (colorScheme == .dark ? .black : .white)
            : .white
    }

    private var libraryPrimaryTint: Color {
        colorSchemeContrast == .increased
            ? (colorScheme == .dark ? .white : .black)
            : .wakevePrimary
    }

    private var cardBackground: Color {
        reduceTransparency || colorSchemeContrast == .increased
            ? Color(uiColor: .systemBackground)
            : Color(uiColor: .secondarySystemBackground)
    }

    private func nextActionTitle(_ action: LibraryNextAction) -> String {
        switch action {
        case .continueDraft:
            String(localized: "event.detail.canvas.action.edit_draft")
        case .submitVote:
            String(localized: "event.detail.canvas.action.submit_vote")
        case .viewPollResults:
            String(localized: "event.detail.canvas.action.view_results")
        case .compareOptions:
            String(localized: "event.detail.canvas.action.compare_options")
        case .continueOrganization:
            String(localized: "event.detail.canvas.action.continue_organization")
        case .viewArchive:
            String(localized: "invitation.action.view_archive")
        default:
            String(localized: "event.detail.canvas.action.show_details")
        }
    }
}

/// Total artwork renderer shared by Library and Archive. Missing or unavailable
/// remote imagery falls back to the event mood without rewriting persisted state.
struct InvitationArtworkView: View {
    let artwork: any Artwork
    let event: Event

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Group {
            if let legacy = artwork as? ArtworkLegacyRemote,
               let url = URL(string: legacy.validatedHttpsUrl) {
                remote(url: url, crop: .fill, focalPoint: nil)
            } else if let structured = artwork as? ArtworkStructured,
                      let server = structured.ref.source as? ArtworkSourceServerAsset,
                      let url = URL(string: server.canonicalHttpsUrl) {
                remote(
                    url: url,
                    crop: structured.ref.crop,
                    focalPoint: structured.ref.focalPoint
                )
            } else if let structured = artwork as? ArtworkStructured,
                      let preset = structured.ref.source as? ArtworkSourcePreset {
                presetArtwork(
                    presetId: preset.presetId,
                    crop: structured.ref.crop,
                    focalPoint: structured.ref.focalPoint
                )
            } else {
                fallback
            }
        }
        .accessibilityHidden(true)
    }

    @ViewBuilder
    private func remote(
        url: URL,
        crop: ArtworkCrop,
        focalPoint: ArtworkFocalPoint?
    ) -> some View {
        AsyncImage(url: url) { phase in
            if let image = phase.image {
                if crop == .fit {
                    image
                        .resizable()
                        .scaledToFit()
                        .frame(
                            maxWidth: .infinity,
                            maxHeight: .infinity,
                            alignment: focalAlignment(focalPoint)
                        )
                } else {
                    image
                        .resizable()
                        .scaledToFill()
                        .frame(
                            maxWidth: .infinity,
                            maxHeight: .infinity,
                            alignment: focalAlignment(focalPoint)
                        )
                        .clipped()
                }
            } else {
                fallback
            }
        }
    }

    @ViewBuilder
    private func presetArtwork(
        presetId: String,
        crop: ArtworkCrop,
        focalPoint: ArtworkFocalPoint
    ) -> some View {
        if crop == .fit {
            presetImage(presetId: presetId)
                .scaledToFit()
                .frame(
                    maxWidth: .infinity,
                    maxHeight: .infinity,
                    alignment: focalAlignment(focalPoint)
                )
        } else {
            presetImage(presetId: presetId)
                .scaledToFill()
                .frame(
                    maxWidth: .infinity,
                    maxHeight: .infinity,
                    alignment: focalAlignment(focalPoint)
                )
                .clipped()
        }
    }

    @ViewBuilder
    private func presetImage(presetId: String) -> some View {
        switch presetId {
        case "wakeve-lake":
            Image("InvitationPresetLake")
                .resizable()
        case "wakeve-sunset":
            Image("InvitationPresetSunset")
                .resizable()
        case "wakeve-celebration":
            Image("InvitationPresetCelebration")
                .resizable()
        default:
            fallback
        }
    }

    private func focalAlignment(_ focalPoint: ArtworkFocalPoint?) -> Alignment {
        guard let focalPoint else { return .center }
        let horizontal: HorizontalAlignment = focalPoint.x < 0.34
            ? .leading
            : (focalPoint.x > 0.66 ? .trailing : .center)
        let vertical: VerticalAlignment = focalPoint.y < 0.34
            ? .top
            : (focalPoint.y > 0.66 ? .bottom : .center)
        return Alignment(horizontal: horizontal, vertical: vertical)
    }

    private var fallback: some View {
        let palette = EventMoodPalette.palette(for: event.eventType.name)
        return ZStack {
            palette.gradient(for: colorScheme)
            Image(systemName: palette.symbolName)
                .font(.title2)
                .foregroundStyle(.white)
        }
    }
}
