import SwiftUI
import Shared

struct TransportPlanningView: View {
    @Environment(\.colorScheme) private var colorScheme

    let event: Event
    let isOrganizer: Bool
    let isParticipantConfirmed: Bool?
    let isReadOnly: Bool
    let eventStatus: EventStatus
    let confirmedDate: String?
    let selectedDestination: TransportLocation?
    let readiness: TransportReadiness?
    let missingDeparture: [String]
    let plans: [TransportPlanningPlan]
    let selectedPlanId: String?
    let pendingSync: Bool
    let onGenerate: (TransportPlanningOptimizationType) -> Void
    let onSelectFinalPlan: (TransportPlanningPlan) -> Void
    let onMarkTransportNotNeeded: () -> Void
    let onSaveDepartureLocation: (TransportLocation) -> Void
    let onBack: () -> Void

    @State private var selectedOptimization: TransportPlanningOptimizationType = .BALANCED
    @State private var departureInput: String = ""

    private var canAccessDetails: Bool {
        isOrganizer || isParticipantConfirmed == true
    }

    private var isReadinessComplete: Bool {
        readiness?.isComplete ?? missingDeparture.isEmpty
    }

    private var transportNotNeeded: Bool {
        readiness?.transportNotNeeded ?? false
    }

    private var destinationName: String {
        selectedDestination?.name ?? "Destination non sélectionnée"
    }

    private var displayedMissingDeparture: [String] {
        let readinessNames = readiness?.missingDepartureParticipantNames as? [String] ?? []
        let readinessIds = readiness?.missingDepartureParticipantIds as? [String] ?? []
        if !readinessNames.isEmpty {
            return readinessNames
        }
        if !readinessIds.isEmpty {
            return readinessIds
        }
        return missingDeparture
    }

    var body: some View {
        ZStack(alignment: .top) {
            WakeveTheme.ColorToken.pageBackground(for: colorScheme)
                .ignoresSafeArea()

            if canAccessDetails {
                content
            } else {
                lockedState
            }

            topControls
        }
        .toolbar(.hidden, for: .tabBar)
    }

    private var content: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: 18) {
                header
                readinessCard
                departureCard
                optimizationCard
                generatedPlanCard
            }
            .padding(.horizontal, WakeveTheme.Spacing.page)
            .padding(.top, 112)
            .padding(.bottom, 80)
        }
    }

    private var header: some View {
        WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
            VStack(alignment: .leading, spacing: 12) {
                Label("Transport", systemImage: "point.topleft.down.curvedto.point.bottomright.up.fill")
                    .font(.system(size: 28, weight: .bold))
                    .foregroundColor(primaryText)

                Text(event.title)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(secondaryText)

                VStack(alignment: .leading, spacing: 8) {
                    Text("Date confirmée : \(confirmedDate ?? "Date bientôt confirmée")")
                    Text("Destination: \(destinationName)")
                }
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(primaryText)

                if pendingSync {
                    Label("Synchronisation en attente. Modifications locales en attente d'envoi", systemImage: "icloud.slash.fill")
                        .font(.system(size: 13, weight: .bold))
                        .foregroundColor(.orange)
                }
            }
        }
    }

    private var readinessCard: some View {
        WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
            VStack(alignment: .leading, spacing: 14) {
                Label(
                    isReadinessComplete ? "Préparation complète" : "Départ manquant",
                    systemImage: isReadinessComplete ? "checkmark.circle.fill" : "exclamationmark.triangle.fill"
                )
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(primaryText)

                if transportNotNeeded {
                    Text("Transport non requis pour cet événement.")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(secondaryText)
                } else if displayedMissingDeparture.isEmpty {
                    Text("Tous les participants confirmés ont un point de départ.")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(secondaryText)
                } else {
                    ForEach(displayedMissingDeparture, id: \.self) { participant in
                        Text("• \(participant)")
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(secondaryText)
                    }
                }
            }
        }
    }

    private var departureCard: some View {
        WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
            let hasSelectedDestination = selectedDestination != nil
            let mutationDisabled = !canAccessDetails || isReadOnly || !workflowAllowsMutation(eventStatus) || !hasSelectedDestination
            let trimmedDeparture = departureInput.trimmingCharacters(in: .whitespacesAndNewlines)
            VStack(alignment: .leading, spacing: 14) {
                Text("Départ")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(primaryText)

                TextField("Point de départ", text: $departureInput)
                    .textInputAutocapitalization(.words)
                    .autocorrectionDisabled(false)
                    .padding(.horizontal, 14)
                    .padding(.vertical, 12)
                    .background(controlFill)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .disabled(mutationDisabled)

                WakeveActionButton(
                    "Enregistrer le départ",
                    systemImage: "mappin.and.ellipse",
                    variant: .secondary,
                    isDisabled: mutationDisabled || trimmedDeparture.isEmpty
                ) {
                    let location = TransportLocation(
                        name: trimmedDeparture,
                        address: nil,
                        latitude: nil,
                        longitude: nil,
                        iataCode: nil
                    )
                    onSaveDepartureLocation(location)
                }
            }
        }
    }

    private var optimizationCard: some View {
        WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
            let hasSelectedDestination = selectedDestination != nil
            let mutationDisabled = !isOrganizer || isReadOnly || !workflowAllowsMutation(eventStatus) || !hasSelectedDestination
            VStack(alignment: .leading, spacing: 14) {
                Text("Optimisation")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(primaryText)

                HStack(spacing: 8) {
                    ForEach(TransportPlanningOptimizationType.allCases) { option in
                        Button {
                            selectedOptimization = option
                        } label: {
                            Text(option.title)
                                .font(.system(size: 13, weight: .bold))
                                .foregroundColor(selectedOptimization == option ? .white : primaryText)
                                .padding(.horizontal, 10)
                                .padding(.vertical, 8)
                                .background(selectedOptimization == option ? Color.blue : controlFill)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                    }
                }

                WakeveActionButton(
                    "Générer le plan",
                    systemImage: "wand.and.stars",
                    variant: .primary,
                    isDisabled: mutationDisabled || !isReadinessComplete || transportNotNeeded
                ) {
                    onGenerate(selectedOptimization)
                }

                if isOrganizer && !transportNotNeeded {
                    WakeveActionButton(
                        "Transport non requis",
                        systemImage: "xmark.circle.fill",
                        variant: .secondary,
                        isDisabled: mutationDisabled,
                        action: onMarkTransportNotNeeded
                    )
                }
            }
        }
    }

    private var generatedPlanCard: some View {
        WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
            let hasSelectedDestination = selectedDestination != nil
            let mutationDisabled = !isOrganizer || isReadOnly || !workflowAllowsMutation(eventStatus) || !hasSelectedDestination
            VStack(alignment: .leading, spacing: 14) {
                Text("Plan généré")
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(primaryText)

                if plans.isEmpty {
                    Text("Aucun trajet n'a encore été généré.")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(secondaryText)
                } else {
                    ForEach(plans) { plan in
                        VStack(alignment: .leading, spacing: 8) {
                            HStack {
                                Text(plan.optimization.title)
                                    .font(.system(size: 16, weight: .bold))
                                    .foregroundColor(primaryText)

                                Spacer()

                                if selectedPlanId == plan.id {
                                    Text("Sélectionné")
                                        .font(.system(size: 12, weight: .bold))
                                        .foregroundColor(.green)
                                }
                            }

                            Text("Coût total : \(plan.totalCost, specifier: "%.2f") \(plan.currency)")
                                .font(.system(size: 14, weight: .medium))
                                .foregroundColor(secondaryText)

                            WakeveActionButton(
                                selectedPlanId == plan.id ? "Plan sélectionné" : "Sélectionner le plan final",
                                systemImage: "checkmark.seal.fill",
                                variant: .eventNext,
                                isDisabled: mutationDisabled || selectedPlanId == plan.id
                            ) {
                                onSelectFinalPlan(plan)
                            }
                        }
                        .padding(.vertical, 8)
                    }
                }

                if transportNotNeeded {
                    WakeveActionButton(
                        "Transport indiqué non requis",
                        systemImage: "checkmark.circle.fill",
                        variant: .secondary,
                        isDisabled: true,
                        action: {}
                    )
                }
            }
        }
    }

    private var lockedState: some View {
        VStack(spacing: 18) {
            Image(systemName: "lock.fill")
                .font(.system(size: 42, weight: .bold))
                .foregroundColor(secondaryText)
            Text("Transport verrouillé")
                .font(.system(size: 24, weight: .bold))
                .foregroundColor(primaryText)
            Text("Seuls les organisateurs et les participants confirmés peuvent accéder aux départs et aux plans générés.")
                .font(.system(size: 15, weight: .medium))
                .foregroundColor(secondaryText)
                .multilineTextAlignment(.center)
        }
        .padding(.horizontal, WakeveTheme.Spacing.page)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var topControls: some View {
        HStack {
            WakeveCircleButton(
                systemImage: "chevron.left",
                accessibilityLabel: "Retour",
                variant: .glass,
                size: 44,
                action: onBack
            )
            Spacer()
        }
        .padding(.horizontal, 16)
        .padding(.top, WakeveTheme.Navigation.controlTopSpacing)
    }

    private var primaryText: Color {
        WakeveTheme.ColorToken.primaryText(for: colorScheme)
    }

    private var secondaryText: Color {
        WakeveTheme.ColorToken.secondaryText(for: colorScheme)
    }

    private var controlFill: Color {
        WakeveTheme.ColorToken.controlFill(for: colorScheme)
    }

    private func workflowAllowsMutation(_ eventStatus: EventStatus) -> Bool {
        eventStatus == EventStatus.confirmed || eventStatus == EventStatus.comparing || eventStatus == EventStatus.organizing
    }
}

struct TransportPlanningPlan: Identifiable, Equatable, Codable {
    let id: String
    let optimization: TransportPlanningOptimizationType
    let totalCost: Double
    let currency: String
}

struct TransportPlanningPresentationState {
    let eventId: String?
    let readiness: TransportReadiness?
    let missingDeparture: [String]
    let plans: [TransportPlanningPlan]
    let selectedPlanId: String?
    let pendingSync: Bool

    static let empty = TransportPlanningPresentationState(
        eventId: nil,
        readiness: nil,
        missingDeparture: [],
        plans: [],
        selectedPlanId: nil,
        pendingSync: Bool()
    )
}

enum TransportPlanningOptimizationType: String, CaseIterable, Identifiable, Codable {
    case COST_MINIMIZE
    case TIME_MINIMIZE
    case BALANCED

    var id: String { rawValue }

    var title: String {
        switch self {
        case .COST_MINIMIZE:
            return "Coût"
        case .TIME_MINIMIZE:
            return "Temps"
        case .BALANCED:
            return "Équilibré"
        }
    }

    var sharedOptimizationType: OptimizationType {
        switch self {
        case .COST_MINIMIZE:
            return OptimizationType.costMinimize
        case .TIME_MINIMIZE:
            return OptimizationType.timeMinimize
        case .BALANCED:
            return OptimizationType.balanced
        }
    }

    init(shared: OptimizationType) {
        if shared == OptimizationType.costMinimize {
            self = .COST_MINIMIZE
        } else if shared == OptimizationType.timeMinimize {
            self = .TIME_MINIMIZE
        } else {
            self = .BALANCED
        }
    }
}
