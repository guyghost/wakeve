import Foundation
@preconcurrency import Shared

@MainActor
final class TransportPlanningViewModel: ObservableObject {
    @Published private(set) var state = TransportPlanningPresentationState.empty

    private let database: WakeveDb
    private let transportRepository: TransportRepositoryBridge
    private var readinessByEventId: [String: TransportReadiness] = [:]

    init(
        database: WakeveDb = RepositoryProvider.shared.database,
        transportRepository: TransportRepositoryBridge? = nil
    ) {
        self.database = database
        self.transportRepository = transportRepository ?? TransportRepositoryBridge(database: database)
    }

    func load(
        event: Event,
        userId: String,
        repository: EventRepositoryInterface,
        selectedDestination: TransportLocation?
    ) {
        state = makeState(
            event: event,
            userId: userId,
            repository: repository,
            selectedDestination: selectedDestination
        )
        refreshReadiness(
            event: event,
            userId: userId,
            repository: repository,
            selectedDestination: selectedDestination
        )
    }

    func generate(
        event: Event,
        userId: String,
        repository: EventRepositoryInterface,
        selectedDestination: TransportLocation?,
        optimization: TransportPlanningOptimizationType
    ) {
        guard let selectedDestination else { return }
        let currentState = makeState(
            event: event,
            userId: userId,
            repository: repository,
            selectedDestination: selectedDestination
        )
        guard currentState.readiness?.canGeneratePlan == true else {
            state = currentState
            return
        }

        transportRepository.generatePlan(
            eventId: event.id,
            destination: selectedDestination,
            optimizationType: optimization.sharedOptimizationType,
            generatedByUserId: userId
        ) { [weak self] _, _ in
            Task { @MainActor in
                guard let self else { return }
                self.reloadState(
                    event: event,
                    userId: userId,
                    repository: repository,
                    selectedDestination: selectedDestination
                )
                WakeveHaptics.success()
            }
        }
    }

    func selectFinalPlan(
        event: Event,
        userId: String,
        repository: EventRepositoryInterface,
        selectedDestination: TransportLocation?,
        plan: TransportPlanningPlan
    ) {
        _ = transportRepository.selectFinalPlan(
            eventId: event.id,
            planId: plan.id,
            selectedByOrganizerId: userId
        )
        WakeveHaptics.success()
        reloadState(
            event: event,
            userId: userId,
            repository: repository,
            selectedDestination: selectedDestination
        )
    }

    func markTransportNotNeeded(
        event: Event,
        userId: String,
        repository: EventRepositoryInterface,
        selectedDestination: TransportLocation?
    ) {
        _ = transportRepository.markTransportNotNeeded(
            eventId: event.id,
            updatedByUserId: userId
        )
        WakeveHaptics.success()
        reloadState(
            event: event,
            userId: userId,
            repository: repository,
            selectedDestination: selectedDestination
        )
    }

    func saveDepartureLocation(
        event: Event,
        userId: String,
        repository: EventRepositoryInterface,
        selectedDestination: TransportLocation?,
        location: TransportLocation
    ) {
        let participantId = userId
        transportRepository.saveDepartureLocation(
            eventId: event.id,
            participantId: participantId,
            location: location,
            updatedByUserId: userId
        ) { [weak self] _, _ in
            Task { @MainActor in
                guard let self else { return }
                self.reloadState(
                    event: event,
                    userId: userId,
                    repository: repository,
                    selectedDestination: selectedDestination
                )
            }
        }
    }

    func makeState(
        event: Event,
        userId: String,
        repository: EventRepositoryInterface,
        selectedDestination: TransportLocation?
    ) -> TransportPlanningPresentationState {
        let confirmedParticipants = confirmedParticipantIds(event: event, repository: repository)
        let readiness = selectedDestination == nil ? nil : readinessByEventId[event.id]
        let missingDeparture = readiness?.missingDepartureParticipantIds as? [String] ?? confirmedParticipants
        let plans = transportRepository.getPlansByEvent(eventId: event.id).map { plan in
            TransportPlanningPlan(
                id: plan.id,
                optimization: TransportPlanningOptimizationType(shared: plan.optimizationType),
                totalCost: plan.totalGroupCost,
                currency: plan.participantRoutes.values.first?.currency ?? "EUR"
            )
        }

        return TransportPlanningPresentationState(
            eventId: event.id,
            readiness: readiness,
            missingDeparture: missingDeparture,
            plans: plans,
            selectedPlanId: transportRepository.getSelectedPlanId(eventId: event.id),
            pendingSync: hasReplayablePendingSync(eventId: event.id)
        )
    }

    private func confirmedParticipantIds(
        event: Event,
        repository: EventRepositoryInterface
    ) -> [String] {
        guard let participantRecords = repository.getParticipantRecords(eventId: event.id), !participantRecords.isEmpty else {
            return event.participants
        }

        let participantAccessStates = participantRecords.map { record in
            ParticipantAccessMapper.shared.fromRepositoryRecord(record: record)
        }
        return ParticipantManagementPresentationMapper.shared
            .map(participants: participantAccessStates)
            .filter { $0.canAccessOrganizationDetails }
            .map { $0.userIdOrEmail }
    }

    private func reloadState(
        event: Event,
        userId: String,
        repository: EventRepositoryInterface,
        selectedDestination: TransportLocation?
    ) {
        state = makeState(
            event: event,
            userId: userId,
            repository: repository,
            selectedDestination: selectedDestination
        )
        refreshReadiness(
            event: event,
            userId: userId,
            repository: repository,
            selectedDestination: selectedDestination
        )
    }

    private func refreshReadiness(
        event: Event,
        userId: String,
        repository: EventRepositoryInterface,
        selectedDestination: TransportLocation?
    ) {
        guard let selectedDestination else {
            readinessByEventId[event.id] = nil
            return
        }

        transportRepository.getReadiness(
            eventId: event.id,
            destination: selectedDestination
        ) { [weak self] readiness, _ in
            Task { @MainActor in
                guard let self else { return }
                self.readinessByEventId[event.id] = readiness
                self.state = self.makeState(
                    event: event,
                    userId: userId,
                    repository: repository,
                    selectedDestination: selectedDestination
                )
            }
        }
    }

    private func hasReplayablePendingSync(eventId: String) -> Bool {
        let syncMetadataReplayableTransportEntityTypes = [
            "transport_departure_location",
            "transport_plan_selection",
            "transport_event_status",
            "transport_plan"
        ]
        _ = syncMetadataReplayableTransportEntityTypes

        return transportRepository.hasPendingTransportSync(eventId: eventId)
    }
}
