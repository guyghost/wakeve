import SwiftUI
import Shared

struct EventInformationNotificationSummary: Equatable {
    let eventPreference: String
    let accountPreference: String
    let systemAuthorization: String
    let effectiveDelivery: String

    static let unavailable = EventInformationNotificationSummary(
        eventPreference: String(localized: "invitation.state.unavailable"),
        accountPreference: String(localized: "invitation.state.unavailable"),
        systemAuthorization: String(localized: "invitation.state.unavailable"),
        effectiveDelivery: String(localized: "invitation.state.unavailable")
    )
}

@MainActor
final class EventInformationViewModel: ObservableObject {
    @Published private(set) var event: Event?
    @Published private(set) var snapshot: EventInformationSnapshot?
    @Published private(set) var systemAuthorization: SystemNotificationAuthorization = .unavailable
    @Published private(set) var organizerDisplayName = String(localized: "participants.role.organizer")

    let notificationPolicy: EventNotificationPolicy
    private let eventId: String
    private let viewerId: String
    private let database: WakeveDb
    private let informationRepository: DatabaseEventInformationRepository
    private let systemAuthorizationReader: EventInformationSystemAuthorizationReader

    init(
        eventId: String,
        viewerId: String = "",
        repository: DatabaseEventRepository = RepositoryProvider.shared.databaseRepository,
        database: WakeveDb = RepositoryProvider.shared.database,
        informationRepository: DatabaseEventInformationRepository =
            DatabaseEventInformationRepository(database: RepositoryProvider.shared.database),
        notificationPolicy: EventNotificationPolicy = EventNotificationPolicy(),
        systemAuthorizationReader: @escaping EventInformationSystemAuthorizationReader = {
            await EventInformationSystemAuthorizationAdapter().read()
        }
    ) {
        self.eventId = eventId
        self.viewerId = viewerId
        self.database = database
        self.informationRepository = informationRepository
        self.systemAuthorizationReader = systemAuthorizationReader
        self.notificationPolicy = notificationPolicy
        self.event = repository.getEvent(id: eventId)
        updateOrganizerDisplayName()
    }

    func reload() async {
        systemAuthorization = await systemAuthorizationReader()
        let now = Kotlinx_datetimeInstant.companion.fromEpochMilliseconds(
            epochMilliseconds: Int64(Date().timeIntervalSince1970 * 1_000)
        )
        do {
            let state = try await informationRepository.load(
                eventId: eventId,
                viewerId: viewerId,
                now: now
            )
            snapshot = (state as? EventInformationLoadStateReady)?.snapshot
            event = snapshot?.event
            updateOrganizerDisplayName()
        } catch {
            snapshot = nil
        }
    }

    private func updateOrganizerDisplayName() {
        organizerDisplayName = InvitationEventMetadataProjection.organizerDisplayName(
            for: event,
            database: database
        )
    }
}

struct EventInformationView: View {
    @StateObject private var viewModel: EventInformationViewModel
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @Environment(\.colorSchemeContrast) private var colorSchemeContrast
    @State private var showEventPreferenceEditor = false
    @State private var isSavingEventPreference = false
    @State private var eventPreferenceWriteFailed = false

    let onOpenNotificationOwner: ((EventNotificationPreference) async -> Bool)?
    let onOpenCalendar: (() -> Void)?
    let onOpenMaps: (() -> Void)?
    let onOpenWeather: (() -> Void)?
    let onLeave: (() -> Void)?
    let onDelete: (() -> Void)?
    let onDone: () -> Void

    init(
        eventId: String,
        viewerId: String = "",
        repository: DatabaseEventRepository = RepositoryProvider.shared.databaseRepository,
        database: WakeveDb = RepositoryProvider.shared.database,
        onOpenNotificationOwner: ((EventNotificationPreference) async -> Bool)? = nil,
        onOpenCalendar: (() -> Void)? = nil,
        onOpenMaps: (() -> Void)? = nil,
        onOpenWeather: (() -> Void)? = nil,
        onLeave: (() -> Void)? = nil,
        onDelete: (() -> Void)? = nil,
        onDone: @escaping () -> Void
    ) {
        _viewModel = StateObject(
            wrappedValue: EventInformationViewModel(
                eventId: eventId,
                viewerId: viewerId,
                repository: repository,
                database: database
            )
        )
        self.onOpenNotificationOwner = onOpenNotificationOwner
        self.onOpenCalendar = onOpenCalendar
        self.onOpenMaps = onOpenMaps
        self.onOpenWeather = onOpenWeather
        self.onLeave = onLeave
        self.onDelete = onDelete
        self.onDone = onDone
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    if let event = viewModel.event {
                        informationCard {
                            VStack(alignment: .leading, spacing: 10) {
                                Text(event.title)
                                    .font(.title2.weight(.semibold))
                                    .fixedSize(horizontal: false, vertical: true)
                                if !event.description_.isEmpty {
                                    Text(event.description_)
                                        .foregroundStyle(.secondary)
                                        .fixedSize(horizontal: false, vertical: true)
                                }
                                LabeledContent(
                                    String(localized: "event.detail.metadata.status"),
                                    value: InvitationEventMetadataProjection.localizedStatus(for: event.status)
                                )
                                LabeledContent(
                                    String(localized: "invitation.information.organizer"),
                                    value: viewModel.organizerDisplayName
                                )
                            }
                        }
                        .accessibilitySortPriority(2)
                    } else {
                        ContentUnavailableView(
                            String(localized: "invitation.information.title"),
                            systemImage: "info.circle",
                            description: Text(String(localized: "invitation.state.unavailable"))
                        )
                    }

                    informationCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Label(
                                String(localized: "invitation.information.notifications"),
                                systemImage: "bell.badge"
                            )
                            .font(.headline)

                            notificationRow(
                                String(localized: "invitation.information.notification.event"),
                                notificationSummary.eventPreference,
                                identifier: "eventInformationNotificationEventAxis"
                            )

                            Button(action: { showEventPreferenceEditor = true }) {
                                Label(
                                    String(localized: "invitation.information.notification.manage"),
                                    systemImage: isSavingEventPreference ? "arrow.triangle.2.circlepath" : "bell.badge"
                                )
                            }
                            .disabled(
                                viewModel.snapshot?.capabilities.canWriteEventPreference != true ||
                                    onOpenNotificationOwner == nil || isSavingEventPreference
                            )
                            .frame(minWidth: 44, minHeight: 44)
                            .invitationAccessibilityIdentifier("eventInformationPreferenceWrite")

                            DisclosureGroup(
                                String(localized: "invitation.information.notification.delivery_details")
                            ) {
                                VStack(alignment: .leading, spacing: 12) {
                                    notificationRow(
                                        String(localized: "invitation.information.notification.account"),
                                        notificationSummary.accountPreference,
                                        identifier: "eventInformationNotificationAccountAxis"
                                    )
                                    notificationRow(
                                        String(localized: "invitation.information.notification.system"),
                                        notificationSummary.systemAuthorization,
                                        identifier: "eventInformationNotificationSystemAxis"
                                    )
                                    notificationRow(
                                        String(localized: "invitation.information.notification.effective"),
                                        notificationSummary.effectiveDelivery,
                                        identifier: "eventInformationNotificationEffectiveAxis"
                                    )
                                }
                                .padding(.top, WakeveTheme.Spacing.sm)
                            }
                        }
                    }
                    .accessibilitySortPriority(1)

                    informationCard {
                        VStack(alignment: .leading, spacing: 12) {
                            destinationRow(
                                title: String(localized: "invitation.information.destination.calendar"),
                                systemImage: "calendar",
                                state: viewModel.snapshot?.calendar,
                                identifier: "eventInformationCalendarDestination",
                                action: onOpenCalendar
                            )
                            destinationRow(
                                title: String(localized: "invitation.information.destination.maps"),
                                systemImage: "map",
                                state: viewModel.snapshot?.maps,
                                identifier: "eventInformationMapsDestination",
                                action: onOpenMaps
                            )
                            destinationRow(
                                title: String(localized: "invitation.information.destination.weather"),
                                systemImage: "cloud.sun",
                                state: viewModel.snapshot?.weather,
                                identifier: "eventInformationWeatherDestination",
                                action: onOpenWeather
                            )
                        }
                    }

                    informationCard {
                        VStack(alignment: .leading, spacing: 12) {
                            Button(role: .destructive, action: { onLeave?() }) {
                                Label(String(localized: "common.cancel"), systemImage: "rectangle.portrait.and.arrow.right")
                            }
                            .disabled(viewModel.snapshot?.capabilities.canLeave != true || onLeave == nil)
                            .frame(minWidth: 44, minHeight: 44)
                            .invitationAccessibilityIdentifier("eventInformationLeaveConfirmation")

                            Button(role: .destructive, action: { onDelete?() }) {
                                Label(String(localized: "common.delete"), systemImage: "trash")
                            }
                            .disabled(viewModel.snapshot?.capabilities.canDelete != true || onDelete == nil)
                            .frame(minWidth: 44, minHeight: 44)
                            .invitationAccessibilityIdentifier("eventInformationDeleteConfirmation")
                        }
                    }
                }
                .padding()
            }
            .navigationTitle(String(localized: "invitation.information.title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) {
                    Button(String(localized: "common.done"), action: onDone)
                        .frame(minWidth: 44, minHeight: 44)
                }
            }
        }
        .toolbar(.hidden, for: .tabBar)
        .background {
            InvitationAccessibilityIdentifierBridge(
                identifier: "eventInformationPrimaryAction"
            )
            .frame(width: 1, height: 1)
            .allowsHitTesting(false)
            .accessibilityHidden(true)
        }
        .task { await viewModel.reload() }
        .confirmationDialog(
            String(localized: "invitation.information.notification.manage"),
            isPresented: $showEventPreferenceEditor,
            titleVisibility: .visible
        ) {
            eventPreferenceButton(.inheritAccount)
            eventPreferenceButton(.allEventUpdates)
            eventPreferenceButton(.essentialOnly)
            eventPreferenceButton(.muted)
            Button(String(localized: "common.cancel"), role: .cancel) {}
        }
        .alert(String(localized: "common.error"), isPresented: $eventPreferenceWriteFailed) {
            Button(String(localized: "common.done"), role: .cancel) {}
        } message: {
            Text(String(localized: "common.error_generic"))
        }
    }

    @ViewBuilder
    private func eventPreferenceButton(_ preference: EventNotificationPreference) -> some View {
        Button(EventInformationNotificationSummary.localizedPreference(preference)) {
            guard let onOpenNotificationOwner else { return }
            isSavingEventPreference = true
            Task {
                let saved = await onOpenNotificationOwner(preference)
                if saved {
                    await viewModel.reload()
                } else {
                    eventPreferenceWriteFailed = true
                }
                isSavingEventPreference = false
            }
        }
    }

    @ViewBuilder
    private func destinationRow(
        title: String,
        systemImage: String,
        state: (any InformationDestinationState)?,
        identifier: String,
        action: (() -> Void)?
    ) -> some View {
        let isReady = state is InformationDestinationStateReady
        Button(action: { action?() }) {
            HStack {
                Label(title, systemImage: systemImage)
                Spacer()
                Text(
                    isReady
                        ? String(localized: "event.detail.canvas.action.show_details")
                        : String(localized: "invitation.state.unavailable")
                )
                .foregroundStyle(.secondary)
            }
            .fixedSize(horizontal: false, vertical: true)
        }
        .buttonStyle(.plain)
        .disabled(!isReady || action == nil)
        .frame(minWidth: 44, minHeight: 44)
        .invitationAccessibilityIdentifier(identifier)
    }

    private func notificationRow(
        _ label: String,
        _ value: String,
        identifier: String
    ) -> some View {
        LabeledContent(label, value: value)
            .fixedSize(horizontal: false, vertical: true)
            .invitationAccessibilityIdentifier(identifier)
    }

    private func informationCard<Content: View>(
        @ViewBuilder content: () -> Content
    ) -> some View {
        content()
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(16)
            .background(
                reduceTransparency || colorSchemeContrast == .increased
                    ? Color(uiColor: .systemBackground)
                    : Color(uiColor: .secondarySystemBackground)
            )
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay {
                if colorSchemeContrast == .increased {
                    RoundedRectangle(cornerRadius: 18, style: .continuous)
                        .stroke(.primary, lineWidth: 1)
                }
            }
    }

    private var notificationSummary: EventInformationNotificationSummary {
        EventInformationNotificationSummary(
            snapshot: viewModel.snapshot,
            systemAuthorization: viewModel.systemAuthorization,
            policy: viewModel.notificationPolicy
        )
    }
}

private extension EventInformationNotificationSummary {
    init(
        snapshot: EventInformationSnapshot?,
        systemAuthorization: SystemNotificationAuthorization,
        policy: EventNotificationPolicy
    ) {
        guard let snapshot else {
            self = .unavailable
            return
        }

        let eventPreferenceRecord = snapshot.eventPreferenceRecord
        let eventPreference = eventPreferenceRecord?.preference ?? .inheritAccount
        let preferenceCopy = Self.localizedPreference(eventPreference)
        let eventCopy: String
        if eventPreferenceRecord?.pendingSync == true {
            eventCopy = String.localizedStringWithFormat(
                String(localized: "settings_sheet.notifications_summary_format"),
                preferenceCopy,
                String(localized: "invitation.state.pending_sync")
            )
        } else {
            eventCopy = preferenceCopy
        }

        let accountCopy = snapshot.accountEnabledTypes.isEmpty
            ? String(localized: "invitation.information.notification.account.none")
            : String.localizedStringWithFormat(
                String(localized: "invitation.information.notification.account.enabled_count"),
                Int64(snapshot.accountEnabledTypes.count)
            )
        let decision = policy.evaluate(
            input: EventNotificationPolicyInput(
                notificationType: .eventUpdate,
                eventPreference: eventPreference,
                accountEnabledTypes: snapshot.accountEnabledTypes,
                quietHoursActive: snapshot.quietHoursActive,
                systemAuthorization: systemAuthorization
            )
        )

        self.init(
            eventPreference: eventCopy,
            accountPreference: accountCopy,
            systemAuthorization: Self.localizedAuthorization(systemAuthorization),
            effectiveDelivery: Self.localizedDecision(decision.reason)
        )
    }

    static func localizedPreference(_ preference: EventNotificationPreference) -> String {
        switch preference {
        case .inheritAccount:
            String(localized: "invitation.information.notification.preference.inherit")
        case .allEventUpdates:
            String(localized: "invitation.information.notification.preference.all")
        case .essentialOnly:
            String(localized: "invitation.information.notification.preference.essential")
        case .muted:
            String(localized: "invitation.information.notification.preference.muted")
        default:
            String(localized: "invitation.state.unavailable")
        }
    }

    static func localizedAuthorization(_ authorization: SystemNotificationAuthorization) -> String {
        switch authorization {
        case .authorized:
            String(localized: "notifications.system_permission.authorized")
        case .provisional:
            String(localized: "notifications.system_permission.provisional")
        case .ephemeral:
            String(localized: "notifications.system_permission.ephemeral")
        case .denied:
            String(localized: "notifications.system_permission.denied")
        case .notDetermined:
            String(localized: "notifications.system_permission.not_determined")
        case .restricted:
            String(localized: "notifications.system_permission.unknown")
        default:
            String(localized: "notifications.system_permission.unknown")
        }
    }

    static func localizedDecision(_ reason: EffectiveNotificationReason) -> String {
        switch reason {
        case .blockedBySystem:
            String(localized: "invitation.information.notification.effective.blocked_system")
        case .blockedByAccount:
            String(localized: "invitation.information.notification.effective.blocked_account")
        case .blockedByEvent:
            String(localized: "invitation.information.notification.effective.blocked_event")
        case .deferredByQuietHours:
            String(localized: "invitation.information.notification.effective.deferred")
        case .eligible:
            String(localized: "invitation.information.notification.effective.eligible")
        default:
            String(localized: "invitation.state.unavailable")
        }
    }
}

enum InvitationEventMetadataProjection {
    static func localizedDate(for value: String) -> String {
        guard let date = parseRepositoryDate(value) else {
            return String(localized: "invitation.state.unavailable")
        }
        let formatter = DateFormatter()
        formatter.locale = .current
        formatter.dateStyle = .long
        formatter.timeStyle = .short
        return formatter.string(from: date)
    }

    static func localizedSettledSummary(_ values: [String]) -> String {
        let normalized = values.map {
            $0.trimmingCharacters(in: .whitespacesAndNewlines)
        }.filter { !$0.isEmpty }
        let datedValues = normalized.compactMap { value -> (value: String, date: Date)? in
            guard let date = parseRepositoryDate(value) else { return nil }
            return (value, date)
        }
        let timezone = normalized.compactMap(TimeZone.init(identifier:)).first
        var components: [String] = []

        if let start = datedValues.first?.date,
           let end = datedValues.dropFirst().first?.date {
            let formatter = DateIntervalFormatter()
            formatter.locale = .current
            formatter.timeZone = timezone ?? .current
            formatter.dateStyle = .long
            formatter.timeStyle = .short
            components.append(formatter.string(from: start, to: end))
        } else if let date = datedValues.first?.date {
            let formatter = DateFormatter()
            formatter.locale = .current
            formatter.timeZone = timezone ?? .current
            formatter.dateStyle = .long
            formatter.timeStyle = .short
            components.append(formatter.string(from: date))
        }

        if let timezone,
           timezone.identifier != TimeZone.current.identifier,
           let localizedName = timezone.localizedName(
               for: .generic,
               locale: .current
           ) {
            components.append(localizedName)
        }

        let parsedDateValues = Set(datedValues.map(\.value))
        let safeCopy = normalized.filter { value in
            !parsedDateValues.contains(value) &&
                TimeZone(identifier: value) == nil &&
                !looksLikeRepositoryTimestamp(value)
        }
        components.append(contentsOf: safeCopy)

        return components.isEmpty
            ? String(localized: "invitation.state.unavailable")
            : components.joined(separator: " · ")
    }

    private static func parseRepositoryDate(_ value: String) -> Date? {
        let parser = ISO8601DateFormatter()
        parser.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = parser.date(from: value) {
            return date
        }
        parser.formatOptions = [.withInternetDateTime]
        return parser.date(from: value)
    }

    private static func looksLikeRepositoryTimestamp(_ value: String) -> Bool {
        value.contains("T") && value.contains("-") &&
            (value.hasSuffix("Z") || value.contains("+"))
    }

    static func localizedStatus(for status: EventStatus) -> String {
        switch status {
        case .draft: String(localized: "status.draft")
        case .polling: String(localized: "status.polling")
        case .confirmed: String(localized: "status.confirmed")
        case .comparing: String(localized: "status.comparing")
        case .organizing: String(localized: "status.organizing")
        case .finalized: String(localized: "status.finalized")
        default: String(localized: "invitation.state.unavailable")
        }
    }

    static func organizerDisplayName(for event: Event?, database: WakeveDb) -> String {
        guard let event,
              let storedName = database.userQueries
                .selectUserById(id: event.organizerId)
                .executeAsOneOrNull()?
                .name
                .trimmingCharacters(in: .whitespacesAndNewlines),
              !storedName.isEmpty
        else {
            return String(localized: "participants.role.organizer")
        }
        return storedName
    }
}
