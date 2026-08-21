import SwiftUI
import Shared

@MainActor
final class EventArchiveViewModel: ObservableObject {
    @Published private(set) var event: Event?
    @Published private(set) var snapshot: ArchiveSnapshot?
    @Published private(set) var freshness: (any Freshness)?
    @Published private(set) var isReloading = false
    @Published private(set) var organizerDisplayName = String(localized: "participants.role.organizer")

    let router: InvitationExperienceRouter
    private let eventId: String
    private let viewerId: String
    private let repository: DatabaseEventRepository
    private let database: WakeveDb
    private let projectionRepository: DatabaseInvitationExperienceProjectionRepository

    init(
        eventId: String,
        viewerId: String = "",
        repository: DatabaseEventRepository = RepositoryProvider.shared.databaseRepository,
        database: WakeveDb = RepositoryProvider.shared.database,
        projectionRepository: DatabaseInvitationExperienceProjectionRepository =
            DatabaseInvitationExperienceProjectionRepository(
                database: RepositoryProvider.shared.database
            ),
        router: InvitationExperienceRouter = InvitationExperienceRouter()
    ) {
        self.eventId = eventId
        self.repository = repository
        self.database = database
        self.projectionRepository = projectionRepository
        self.router = router
        let initialEvent = Self.archiveEvent(repository.getEvent(id: eventId))
        self.event = initialEvent
        self.organizerDisplayName = InvitationEventMetadataProjection.organizerDisplayName(
            for: initialEvent,
            database: database
        )
        self.viewerId = viewerId.isEmpty ? initialEvent?.organizerId ?? "" : viewerId
    }

    func reload() async {
        isReloading = true
        defer { isReloading = false }
        let now = Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
            epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        do {
            let state = try await projectionRepository.archive(
                eventId: eventId,
                viewerId: viewerId,
                now: now
            )
            if let ready = state as? ArchiveLoadStateReady {
                snapshot = ready.snapshot
                freshness = ready.freshness
                event = ready.snapshot.event
                organizerDisplayName = InvitationEventMetadataProjection.organizerDisplayName(
                    for: ready.snapshot.event,
                    database: database
                )
            } else {
                snapshot = nil
                freshness = nil
                event = nil
            }
        } catch {
            snapshot = nil
            freshness = nil
            event = nil
        }
    }

    private static func archiveEvent(_ event: Event?) -> Event? {
        guard let event else { return nil }
        if event.status == .finalized { return event }
        let now = Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
            epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        return EventTemporalClassifier.shared.classify(event: event, now: now) == .past
            ? event
            : nil
    }
}

struct EventArchiveView: View {
    @StateObject private var viewModel: EventArchiveViewModel
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize

    init(
        eventId: String,
        viewerId: String = "",
        repository: DatabaseEventRepository = RepositoryProvider.shared.databaseRepository,
        database: WakeveDb = RepositoryProvider.shared.database
    ) {
        _viewModel = StateObject(
            wrappedValue: EventArchiveViewModel(
                eventId: eventId,
                viewerId: viewerId,
                repository: repository,
                database: database
            )
        )
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: dynamicTypeSize.isAccessibilitySize ? 20 : 14) {
                    if let event = viewModel.event {
                        let palette = EventMoodPalette.palette(for: event.eventType.name)

                        VStack(alignment: .leading, spacing: 16) {
                            if let snapshot = viewModel.snapshot {
                                InvitationArtworkView(
                                    artwork: snapshot.artwork,
                                    event: snapshot.event
                                )
                                .frame(maxWidth: .infinity, minHeight: 180, maxHeight: 240)
                                .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                            } else {
                                Image(systemName: palette.symbolName)
                                    .font(.largeTitle)
                                    .accessibilityHidden(true)
                            }
                            Text(event.title)
                                .font(.largeTitle.bold())
                                .fixedSize(horizontal: false, vertical: true)
                            Label(
                                String(localized: "invitation.archive.read_only"),
                                systemImage: "lock.fill"
                            )
                            .font(.headline)
                            .fixedSize(horizontal: false, vertical: true)
                        }
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(20)
                        .background {
                            ZStack {
                                palette.gradient(for: colorScheme)
                                Color.black.opacity(0.32)
                            }
                        }
                        .clipShape(RoundedRectangle(cornerRadius: 24, style: .continuous))
                        .accessibilitySortPriority(2)

                        VStack(alignment: .leading, spacing: 12) {
                            LabeledContent(
                                String(localized: "event.detail.metadata.status"),
                                value: InvitationEventMetadataProjection.localizedStatus(for: event.status)
                            )
                            if let finalDate = event.finalDate, !finalDate.isEmpty {
                                LabeledContent(
                                    String(localized: "events.status.date_confirmed"),
                                    value: InvitationEventMetadataProjection.localizedDate(for: finalDate)
                                )
                            }
                            LabeledContent(
                                String(localized: "invitation.information.organizer"),
                                value: viewModel.organizerDisplayName
                            )
                        }
                        .padding(16)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .background(Color(uiColor: .secondarySystemBackground))
                        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
                        .accessibilitySortPriority(1)

                        archiveStateRows(isAvailable: true)
                    } else {
                        ContentUnavailableView(
                            String(localized: "invitation.archive.title"),
                            systemImage: "archivebox",
                            description: Text(String(localized: "invitation.state.unavailable"))
                        )
                        archiveStateRows(isAvailable: false)
                    }
                }
                .padding()
            }
            .navigationTitle(String(localized: "invitation.archive.title"))
            .toolbar {
                ToolbarItem(placement: .primaryAction) {
                    Button {
                        Task { await viewModel.reload() }
                    } label: {
                        Label(
                            String(localized: "invitation.action.reload_projection"),
                            systemImage: "arrow.clockwise"
                        )
                        .frame(minWidth: 44, minHeight: 44)
                    }
                    .disabled(viewModel.isReloading)
                    .invitationAccessibilityIdentifier("eventArchivePrimaryAction")
                }
            }
        }
        .toolbar(.hidden, for: .tabBar)
        .task { await viewModel.reload() }
    }

    private func archiveStateRows(isAvailable: Bool) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            Label(
                isAvailable && viewModel.freshness is FreshnessCurrent
                    ? String(localized: "invitation.archive.freshness.current")
                    : String(localized: "invitation.state.unavailable"),
                systemImage: isAvailable ? "clock.badge.checkmark" : "clock.badge.exclamationmark"
            )
            .invitationAccessibilityIdentifier("eventArchiveFreshness")

            Label(
                isAvailable && viewModel.snapshot?.warning == nil
                    ? String(localized: "invitation.archive.sync.current")
                    : String(localized: "invitation.state.stale"),
                systemImage: isAvailable ? "checkmark.icloud" : "exclamationmark.icloud"
            )
            .invitationAccessibilityIdentifier("eventArchiveSyncWarning")

            Label(
                isAvailable && viewModel.snapshot?.settledSummary.isEmpty == false
                    ? InvitationEventMetadataProjection.localizedSettledSummary(
                        viewModel.snapshot?.settledSummary ?? []
                    )
                    : String(localized: "invitation.state.unavailable"),
                systemImage: "checklist.checked"
            )
            .invitationAccessibilityIdentifier("eventArchiveSettledSummary")
        }
        .font(.subheadline)
        .foregroundStyle(.secondary)
        .padding(16)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color(uiColor: .secondarySystemBackground))
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}
