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
    @State private var transportAISuggestion: TransportCoordinationSuggestion?
    @State private var editableTransportMessage: String = ""
    @State private var isGeneratingTransportAI = false
    @State private var appliedTransportAISuggestion = false
    @State private var ignoredTransportAISuggestion = false
    @State private var transportAIError: String?

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
        ZStack {
            WakeveTheme.ColorToken.pageBackground(for: colorScheme)
                .ignoresSafeArea()

            if canAccessDetails {
                content
            } else {
                lockedState
            }
        }
        .toolbar(.hidden, for: .tabBar)
        .safeAreaInset(edge: .top, spacing: 0) {
            topControls
        }
        .safeAreaInset(edge: .bottom, spacing: 0) {
            if canAccessDetails {
                bottomPrimaryAction
            }
        }
    }

    private var content: some View {
        ScrollView(showsIndicators: false) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                header
                routePreviewCard
                readinessCard
                transportHelperCard
                departureCard
                optimizationCard
                participantsCard
                generatedPlanCard
            }
            .padding(.horizontal, WakeveTheme.Spacing.page)
            .padding(.top, 92)
            .padding(.bottom, 124)
        }
    }

    private var header: some View {
        EventHeroCard(
            title: "Transport",
            subtitle: event.title,
            metadata: transportStatusText,
            gradient: WakeveTheme.EventGradient.work
        ) {
            HStack(spacing: WakeveTheme.Spacing.sm) {
                TransportInfoPill(systemImage: "calendar", value: confirmedDate ?? "Date bientôt confirmée")
                TransportInfoPill(systemImage: "mappin.and.ellipse", value: destinationName)
            }
        }
    }

    private var routePreviewCard: some View {
        LiquidGlassCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                HStack(alignment: .top, spacing: WakeveTheme.Spacing.md) {
                    VStack(spacing: 6) {
                        Circle()
                            .fill(WakeveTheme.ColorToken.progress(for: colorScheme))
                            .frame(width: 12, height: 12)
                        Rectangle()
                            .fill(WakeveTheme.ColorToken.separator(for: colorScheme))
                            .frame(width: 2, height: 34)
                        Circle()
                            .fill(WakeveTheme.ColorToken.eventHighlight(for: colorScheme))
                            .frame(width: 12, height: 12)
                    }
                    .padding(.top, 6)

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                        TransportRoutePoint(
                            title: "Départs participants",
                            subtitle: isReadinessComplete ? "Tous les points de départ sont prêts" : "\(displayedMissingDeparture.count) départ\(displayedMissingDeparture.count > 1 ? "s" : "") à compléter"
                        )

                        TransportRoutePoint(
                            title: "Point de rencontre",
                            subtitle: destinationName
                        )
                    }
                }

                HStack(spacing: WakeveTheme.Spacing.sm) {
                    TransportMetricTile(title: "Heure", value: confirmedDate ?? "À confirmer")
                    TransportMetricTile(title: "Mode", value: selectedOptimization.title)
                }
            }
        }
    }

    private var readinessCard: some View {
        LiquidGlassCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            HStack(spacing: WakeveTheme.Spacing.md) {
                Image(systemName: isReadinessComplete ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                    .font(.title3.weight(.bold))
                    .foregroundColor(isReadinessComplete ? WakeveTheme.ColorToken.confirmation(for: colorScheme) : WakeveTheme.ColorToken.eventHighlight(for: colorScheme))
                    .frame(width: 44, height: 44)
                    .background(controlFill)
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                    Text(isReadinessComplete ? "Préparation complète" : "Départs à compléter")
                        .font(WakeveTheme.Typography.rowTitle)
                        .foregroundColor(primaryText)

                    Text(readinessSubtitle)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(secondaryText)
                        .lineLimit(2)
                }

                Spacer(minLength: WakeveTheme.Spacing.xs)
            }
        }
    }

    private var transportHelperCard: some View {
        LiquidGlassCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack(alignment: .top, spacing: WakeveTheme.Spacing.md) {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text("Suggestion transport")
                            .font(WakeveTheme.Typography.section)
                            .foregroundColor(primaryText)

                        Text("Préparez la coordination du groupe à relire avant partage.")
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(secondaryText)
                    }

                    Spacer()

                    Button {
                        generateTransportSuggestion()
                    } label: {
                        Image(systemName: "sparkles")
                            .font(.headline.weight(.bold))
                            .foregroundColor(isGeneratingTransportAI ? secondaryText : WakeveTheme.ColorToken.accent(for: colorScheme))
                            .frame(width: 44, height: 44)
                            .background(controlFill)
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .disabled(isGeneratingTransportAI)
                    .accessibilityLabel(String(localized: "transport.prepare_suggestion_accessibility"))
                }

                if isGeneratingTransportAI {
                    HStack(spacing: WakeveTheme.Spacing.sm) {
                        ProgressView()
                        Text("Préparation en cours")
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(secondaryText)
                    }
                }

                if let transportAIError {
                    Text(transportAIError)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(WakeveTheme.ColorToken.destructive(for: colorScheme))
                }

                if let suggestion = transportAISuggestion {
                    if !suggestion.missingDetails.isEmpty {
                        TransportAIList(title: "À compléter", values: suggestion.missingDetails, colorScheme: colorScheme)
                    }

                    if !suggestion.coordinationIdeas.isEmpty {
                        TransportAIList(title: "Idées", values: suggestion.coordinationIdeas, colorScheme: colorScheme)
                    }

                    Text("Message à envoyer")
                        .font(WakeveTheme.Typography.bodySemibold)
                        .foregroundColor(primaryText)

                    TextEditor(text: $editableTransportMessage)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(primaryText)
                        .frame(minHeight: 92)
                        .padding(WakeveTheme.Spacing.xs)
                        .background(controlFill)
                        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))

                    HStack(spacing: WakeveTheme.Spacing.sm) {
                        TransportAIActionButton(title: "Modifier", systemImage: "pencil", colorScheme: colorScheme) {
                            appliedTransportAISuggestion = false
                        }
                        TransportAIActionButton(title: "Appliquer", systemImage: "checkmark", colorScheme: colorScheme) {
                            appliedTransportAISuggestion = true
                            ignoredTransportAISuggestion = false
                        }
                        TransportAIActionButton(title: "Ignorer", systemImage: "xmark", colorScheme: colorScheme) {
                            ignoredTransportAISuggestion = true
                            appliedTransportAISuggestion = false
                        }
                    }

                    if appliedTransportAISuggestion {
                        Label("Suggestion prête à être reprise dans le message de groupe", systemImage: "checkmark.circle.fill")
                            .font(WakeveTheme.Typography.caption)
                            .foregroundColor(WakeveTheme.ColorToken.confirmation(for: colorScheme))
                    } else if ignoredTransportAISuggestion {
                        Label("Suggestion ignorée", systemImage: "minus.circle.fill")
                            .font(WakeveTheme.Typography.caption)
                            .foregroundColor(secondaryText)
                    }
                }
            }
        }
    }

    private var departureCard: some View {
        LiquidGlassCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            let hasSelectedDestination = selectedDestination != nil
            let mutationDisabled = !canAccessDetails || isReadOnly || !workflowAllowsMutation(eventStatus) || !hasSelectedDestination
            let trimmedDeparture = departureInput.trimmingCharacters(in: .whitespacesAndNewlines)
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text("Départ")
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(primaryText)

                HStack(spacing: WakeveTheme.Spacing.sm) {
                    TextField("Point de départ", text: $departureInput)
                        .textInputAutocapitalization(.words)
                        .autocorrectionDisabled(false)
                        .padding(.horizontal, WakeveTheme.Spacing.md)
                        .frame(height: 52)
                        .background(controlFill)
                        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
                        .disabled(mutationDisabled)

                    Button {
                        saveDeparture(trimmedDeparture)
                    } label: {
                        Image(systemName: "mappin.and.ellipse")
                            .font(.headline.weight(.bold))
                            .foregroundColor(.white)
                            .frame(width: 52, height: 52)
                            .background((mutationDisabled || trimmedDeparture.isEmpty) ? Color.gray.opacity(0.36) : WakeveTheme.ColorToken.accent(for: colorScheme))
                            .clipShape(Circle())
                    }
                    .buttonStyle(.plain)
                    .disabled(mutationDisabled || trimmedDeparture.isEmpty)
                    .accessibilityLabel(String(localized: "transport.save_departure_accessibility"))
                }
            }
        }
    }

    private var optimizationCard: some View {
        LiquidGlassCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            let hasSelectedDestination = selectedDestination != nil
            let mutationDisabled = !isOrganizer || isReadOnly || !workflowAllowsMutation(eventStatus) || !hasSelectedDestination
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text("Mode de trajet")
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(primaryText)

                HStack(spacing: 8) {
                    ForEach(TransportPlanningOptimizationType.allCases) { option in
                        Button {
                            selectedOptimization = option
                        } label: {
                            Text(option.title)
                                .font(WakeveTheme.Typography.caption)
                                .foregroundColor(selectedOptimization == option ? selectedOptimizationText : primaryText)
                                .frame(maxWidth: .infinity)
                                .frame(height: 42)
                                .background(selectedOptimization == option ? selectedOptimizationFill : controlFill)
                                .clipShape(Capsule())
                        }
                        .buttonStyle(.plain)
                        .disabled(mutationDisabled)
                    }
                }

                if isOrganizer && !transportNotNeeded {
                    Button(action: onMarkTransportNotNeeded) {
                        Label("Transport non requis", systemImage: "xmark.circle.fill")
                            .font(WakeveTheme.Typography.callout.weight(.semibold))
                            .foregroundColor(secondaryText)
                    }
                    .buttonStyle(.plain)
                    .disabled(mutationDisabled)
                }
            }
        }
    }

    private var generatedPlanCard: some View {
        LiquidGlassCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            let hasSelectedDestination = selectedDestination != nil
            let mutationDisabled = !isOrganizer || isReadOnly || !workflowAllowsMutation(eventStatus) || !hasSelectedDestination
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text("Plan généré")
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(primaryText)

                if plans.isEmpty {
                    Text("Aucun trajet n'a encore été généré.")
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(secondaryText)
                } else {
                    ForEach(plans) { plan in
                        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.sm) {
                            HStack {
                                Text(plan.optimization.title)
                                    .font(WakeveTheme.Typography.bodySemibold)
                                    .foregroundColor(primaryText)

                                Spacer()

                                if selectedPlanId == plan.id {
                                    Text("Sélectionné")
                                        .font(WakeveTheme.Typography.tiny)
                                        .foregroundColor(WakeveTheme.ColorToken.confirmation(for: colorScheme))
                                        .padding(.horizontal, WakeveTheme.Spacing.xs)
                                        .padding(.vertical, 4)
                                        .background(WakeveTheme.ColorToken.confirmation(for: colorScheme).opacity(0.14))
                                        .clipShape(Capsule())
                                }
                            }

                            Text("Coût total : \(plan.totalCost, specifier: "%.2f") \(plan.currency)")
                                .font(WakeveTheme.Typography.callout)
                                .foregroundColor(secondaryText)

                            Button {
                                onSelectFinalPlan(plan)
                            } label: {
                                Label(selectedPlanId == plan.id ? "Plan sélectionné" : "Sélectionner le plan final", systemImage: "checkmark.seal.fill")
                                    .font(WakeveTheme.Typography.callout.weight(.semibold))
                                    .foregroundColor(selectedPlanId == plan.id ? WakeveTheme.ColorToken.confirmation(for: colorScheme) : WakeveTheme.ColorToken.accent(for: colorScheme))
                            }
                            .buttonStyle(.plain)
                            .disabled(mutationDisabled || selectedPlanId == plan.id)
                        }
                        .padding(WakeveTheme.Spacing.sm)
                        .background(controlFill)
                        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
                    }
                }

                if transportNotNeeded {
                    Label("Transport indiqué non requis", systemImage: "checkmark.circle.fill")
                        .font(WakeveTheme.Typography.callout.weight(.semibold))
                        .foregroundColor(WakeveTheme.ColorToken.confirmation(for: colorScheme))
                }
            }
        }
    }

    private var participantsCard: some View {
        LiquidGlassCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack {
                    Text("Participants")
                        .font(WakeveTheme.Typography.section)
                        .foregroundColor(primaryText)
                    Spacer()
                    Text(isReadinessComplete ? "Prêts" : "\(displayedMissingDeparture.count) à compléter")
                        .font(WakeveTheme.Typography.tiny)
                        .foregroundColor(isReadinessComplete ? WakeveTheme.ColorToken.confirmation(for: colorScheme) : WakeveTheme.ColorToken.eventHighlight(for: colorScheme))
                        .padding(.horizontal, WakeveTheme.Spacing.xs)
                        .padding(.vertical, 4)
                        .background((isReadinessComplete ? WakeveTheme.ColorToken.confirmation(for: colorScheme) : WakeveTheme.ColorToken.eventHighlight(for: colorScheme)).opacity(0.14))
                        .clipShape(Capsule())
                }

                if displayedMissingDeparture.isEmpty {
                    TransportParticipantRow(name: "Tous les participants confirmés", detail: "Point de départ disponible", isReady: true)
                } else {
                    ForEach(displayedMissingDeparture, id: \.self) { participant in
                        TransportParticipantRow(name: participant, detail: "Point de départ manquant", isReady: false)
                    }
                }
            }
        }
    }

    private var lockedState: some View {
        VStack {
            EmptyState(
                systemImage: "lock.fill",
                title: "Transport verrouillé",
                subtitle: "Seuls les organisateurs et les participants confirmés peuvent accéder aux départs et aux plans générés."
            )
            .padding(.horizontal, WakeveTheme.Spacing.page)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var topControls: some View {
        LiquidGlassToolbar(title: "Transport", subtitle: destinationName) {
            WakeveCircleButton(
                systemImage: "chevron.left",
                accessibilityLabel: String(localized: "common.back"),
                variant: .glass,
                size: 40,
                action: onBack
            )
        } trailing: {
            if pendingSync {
                HStack(spacing: WakeveTheme.Spacing.xs) {
                    Image(systemName: "icloud.slash.fill")
                        .font(.caption.weight(.bold))
                    Text("Synchronisation en attente")
                        .font(WakeveTheme.Typography.tiny)
                        .lineLimit(1)
                        .minimumScaleFactor(0.78)
                }
                .foregroundColor(WakeveTheme.ColorToken.eventHighlight(for: colorScheme))
                .padding(.horizontal, WakeveTheme.Spacing.sm)
                .frame(height: 40)
                    .background(controlFill)
                    .clipShape(Capsule())
                    .accessibilityLabel(String(localized: "sync.pending_accessibility"))
            }
        }
        .padding(.horizontal, WakeveTheme.Spacing.page)
        .padding(.top, WakeveTheme.Spacing.sm)
        .padding(.bottom, WakeveTheme.Spacing.xs)
        .background(
            LinearGradient(
                colors: [
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme),
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme).opacity(0)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .ignoresSafeArea(edges: .top)
        )
    }

    private var bottomPrimaryAction: some View {
        VStack(spacing: 0) {
            LinearGradient(
                colors: [
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme).opacity(0),
                    WakeveTheme.ColorToken.pageBackground(for: colorScheme)
                ],
                startPoint: .top,
                endPoint: .bottom
            )
            .frame(height: 32)
            .allowsHitTesting(false)

            LiquidGlassButton(
                primaryActionTitle,
                systemImage: primaryActionIcon,
                variant: .primary,
                isDisabled: primaryActionDisabled,
                action: primaryAction
            )
            .padding(.horizontal, WakeveTheme.Spacing.page)
            .padding(.bottom, WakeveTheme.Spacing.sm)
            .background(WakeveTheme.ColorToken.pageBackground(for: colorScheme))
        }
    }

    private var transportStatusText: String {
        if transportNotNeeded {
            return "Non requis"
        }
        if selectedPlanId != nil {
            return "Plan final sélectionné"
        }
        if plans.isEmpty {
            return "Plan à générer"
        }
        return "Plans disponibles"
    }

    private var readinessSubtitle: String {
        if transportNotNeeded {
            return "Transport non requis pour cet événement."
        }
        if displayedMissingDeparture.isEmpty {
            return "Tous les participants confirmés ont un point de départ."
        }
        return "\(displayedMissingDeparture.count) participant\(displayedMissingDeparture.count > 1 ? "s" : "") doi\(displayedMissingDeparture.count > 1 ? "vent" : "t") préciser son départ."
    }

    private var selectedOptimizationFill: Color {
        colorScheme == .dark ? Color.white.opacity(0.9) : WakeveTheme.ColorToken.accent(for: colorScheme)
    }

    private var selectedOptimizationText: Color {
        colorScheme == .dark ? WakeveTheme.ColorToken.midnight : .white
    }

    private var primaryActionTitle: String {
        if isReadOnly {
            return "Transport en lecture seule"
        }
        if selectedDestination == nil {
            return "Choisir une destination"
        }
        if transportNotNeeded {
            return "Transport non requis"
        }
        if !isReadinessComplete {
            return "Compléter les départs"
        }
        if plans.isEmpty {
            return "Générer le plan"
        }
        if selectedPlanId == nil {
            return "Choisir un plan final"
        }
        return "Plan final sélectionné"
    }

    private var primaryActionIcon: String {
        if isReadOnly { return "lock.fill" }
        if selectedDestination == nil { return "mappin.slash" }
        if transportNotNeeded { return "checkmark.circle.fill" }
        if !isReadinessComplete { return "person.crop.circle.badge.exclamationmark" }
        if plans.isEmpty { return "wand.and.stars" }
        if selectedPlanId == nil { return "checkmark.seal.fill" }
        return "checkmark.circle.fill"
    }

    private var primaryActionDisabled: Bool {
        let mutationDisabled = !isOrganizer || isReadOnly || !workflowAllowsMutation(eventStatus) || selectedDestination == nil
        return mutationDisabled || transportNotNeeded || !isReadinessComplete || (!plans.isEmpty && selectedPlanId != nil)
    }

    private func primaryAction() {
        if plans.isEmpty {
            onGenerate(selectedOptimization)
            return
        }

        guard selectedPlanId == nil, let firstPlan = plans.first else {
            return
        }
        onSelectFinalPlan(firstPlan)
    }

    private func saveDeparture(_ value: String) {
        let location = TransportLocation(
            name: value,
            address: nil,
            latitude: nil,
            longitude: nil,
            iataCode: nil
        )
        onSaveDepartureLocation(location)
    }

    private func generateTransportSuggestion() {
        guard !isGeneratingTransportAI else { return }
        isGeneratingTransportAI = true
        transportAIError = nil
        appliedTransportAISuggestion = false
        ignoredTransportAISuggestion = false

        let provider = TransportPlanningWakeveAIContextProvider(
            eventId: event.id,
            destinationName: destinationName,
            participantNames: event.participants,
            missingDepartureParticipants: displayedMissingDeparture,
            schedules: [confirmedDate, selectedOptimization.title].compactMap { $0 },
            proposedTrips: plans.map { "\($0.optimization.title) \(String(format: "%.0f", $0.totalCost)) \($0.currency)" }
        )
        let generator = TransportSuggestionGenerator(
            client: HeuristicWakeveAIClient(),
            contextProvider: provider
        )

        Task {
            do {
                let suggestion = try await generator.generate(eventId: event.id, localeIdentifier: "fr_FR")
                await MainActor.run {
                    transportAISuggestion = suggestion
                    editableTransportMessage = suggestion.groupMessageDraft
                    isGeneratingTransportAI = false
                }
            } catch {
                await MainActor.run {
                    transportAIError = "La suggestion n'est pas disponible pour le moment."
                    isGeneratingTransportAI = false
                }
            }
        }
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

private struct TransportPlanningWakeveAIContextProvider: WakeveAIContextProviding {
    let eventId: String
    let destinationName: String
    let participantNames: [String]
    let missingDepartureParticipants: [String]
    let schedules: [String]
    let proposedTrips: [String]

    func currentGroup() async -> WakeveAIGroupContext? {
        WakeveAIGroupContext(groupId: eventId, memberDisplayNames: participantNames)
    }

    func eventContext(eventId: String) async -> WakeveAIEventContext? {
        WakeveAIEventContext(
            eventId: eventId,
            title: "Transport",
            date: schedules.first,
            location: destinationName,
            participantNames: participantNames,
            voteSummaries: [],
            taskTitles: missingDepartureParticipants,
            recentMessages: []
        )
    }

    func participantStatuses(eventId: String) async -> WakeveAIParticipantStatuses? {
        WakeveAIParticipantStatuses(accepted: participantNames, pending: missingDepartureParticipants, declined: [])
    }

    func voteResults(eventId: String) async -> WakeveAIVoteResults? { nil }

    func transportContext(eventId: String) async -> WakeveAITransportContext? {
        WakeveAITransportContext(
            proposedTrips: proposedTrips,
            participantNames: participantNames,
            schedules: schedules,
            missingDepartureParticipants: missingDepartureParticipants
        )
    }

    func userPreferences() async -> WakeveAIUserPreferences? {
        WakeveAIUserPreferences(languageCode: "fr", localPreferences: [])
    }
}

private struct TransportAIList: View {
    let title: String
    let values: [String]
    let colorScheme: ColorScheme

    var body: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xs) {
            Text(title)
                .font(WakeveTheme.Typography.bodySemibold)
                .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))

            ForEach(values, id: \.self) { value in
                Label(value, systemImage: "checkmark.circle")
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                    .lineLimit(2)
            }
        }
    }
}

private struct TransportAIActionButton: View {
    let title: String
    let systemImage: String
    let colorScheme: ColorScheme
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Label(title, systemImage: systemImage)
                .font(WakeveTheme.Typography.tiny.weight(.semibold))
                .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                .frame(maxWidth: .infinity)
                .frame(height: 36)
                .background(WakeveTheme.ColorToken.pageBackground(for: colorScheme).opacity(0.72))
                .clipShape(Capsule())
        }
        .buttonStyle(.plain)
    }
}

private struct TransportInfoPill: View {
    let systemImage: String
    let value: String

    var body: some View {
        HStack(spacing: WakeveTheme.Spacing.xs) {
            Image(systemName: systemImage)
                .font(.caption.weight(.bold))
            Text(value)
                .font(WakeveTheme.Typography.caption)
                .lineLimit(1)
                .minimumScaleFactor(0.76)
        }
        .foregroundColor(.white.opacity(0.88))
        .padding(.horizontal, WakeveTheme.Spacing.sm)
        .padding(.vertical, WakeveTheme.Spacing.xs)
        .background(Color.black.opacity(0.24))
        .clipShape(Capsule())
    }
}

private struct TransportRoutePoint: View {
    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let subtitle: String

    var body: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
            Text(title)
                .font(WakeveTheme.Typography.bodySemibold)
                .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                .lineLimit(1)
                .minimumScaleFactor(0.78)

            Text(subtitle)
                .font(WakeveTheme.Typography.callout)
                .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                .lineLimit(2)
        }
    }
}

private struct TransportMetricTile: View {
    @Environment(\.colorScheme) private var colorScheme

    let title: String
    let value: String

    var body: some View {
        VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
            Text(title)
                .font(WakeveTheme.Typography.tiny)
                .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
                .textCase(.uppercase)

            Text(value)
                .font(WakeveTheme.Typography.caption)
                .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                .lineLimit(1)
                .minimumScaleFactor(0.76)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(WakeveTheme.Spacing.sm)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
    }
}

private struct TransportParticipantRow: View {
    @Environment(\.colorScheme) private var colorScheme

    let name: String
    let detail: String
    let isReady: Bool

    var body: some View {
        HStack(spacing: WakeveTheme.Spacing.md) {
            Image(systemName: isReady ? "checkmark.circle.fill" : "exclamationmark.circle.fill")
                .font(.body.weight(.bold))
                .foregroundColor(isReady ? WakeveTheme.ColorToken.confirmation(for: colorScheme) : WakeveTheme.ColorToken.eventHighlight(for: colorScheme))
                .frame(width: 36, height: 36)
                .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                Text(name)
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(WakeveTheme.ColorToken.primaryText(for: colorScheme))
                    .lineLimit(1)
                    .minimumScaleFactor(0.78)
                Text(detail)
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(WakeveTheme.ColorToken.secondaryText(for: colorScheme))
            }

            Spacer()
        }
        .padding(WakeveTheme.Spacing.sm)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: WakeveTheme.Radius.md, style: .continuous))
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
