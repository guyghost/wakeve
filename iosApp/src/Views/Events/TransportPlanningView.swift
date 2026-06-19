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
    let onChooseDestination: () -> Void
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
        selectedDestination?.name ?? String(localized: "transport.destination_missing")
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
            SemanticColor.appBackground(for: colorScheme)
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
            moodPalette: .travel
        ) {
            HStack(spacing: WakeveTheme.Spacing.sm) {
                TransportInfoPill(systemImage: "calendar", value: confirmedDate ?? String(localized: "transport.date_pending"))
                TransportInfoPill(systemImage: "mappin.and.ellipse", value: destinationName)
            }
        }
    }

    private var routePreviewCard: some View {
        WakeveContentCard(prominence: .prominent, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.lg) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.lg) {
                HStack(alignment: .top, spacing: WakeveTheme.Spacing.md) {
                    VStack(spacing: 6) {
                        Circle()
                            .fill(SemanticColor.progress(for: colorScheme))
                            .frame(width: 12, height: 12)
                        Rectangle()
                            .fill(SemanticColor.separator(for: colorScheme))
                            .frame(width: 2, height: 34)
                        Circle()
                            .fill(SemanticColor.warning(for: colorScheme))
                            .frame(width: 12, height: 12)
                    }
                    .padding(.top, 6)

                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                        TransportRoutePoint(
                            title: String(localized: "transport.route.departures_title"),
                            subtitle: departureReadinessRouteSubtitle
                        )

                        TransportRoutePoint(
                            title: String(localized: "transport.route.meeting_point_title"),
                            subtitle: destinationName
                        )
                    }
                }

                HStack(spacing: WakeveTheme.Spacing.sm) {
                    TransportMetricTile(title: String(localized: "transport.metric.time"), value: confirmedDate ?? String(localized: "transport.to_confirm"))
                    TransportMetricTile(title: String(localized: "transport.metric.mode"), value: selectedOptimization.title)
                }
            }
        }
    }

    private var readinessCard: some View {
        WakeveContentCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            HStack(spacing: WakeveTheme.Spacing.md) {
                Image(systemName: isReadinessComplete ? "checkmark.circle.fill" : "exclamationmark.triangle.fill")
                    .font(.title3.weight(.bold))
                    .foregroundColor(isReadinessComplete ? SemanticColor.confirmation(for: colorScheme) : SemanticColor.warning(for: colorScheme))
                    .frame(width: 44, height: 44)
                    .background(controlFill)
                    .clipShape(Circle())

                VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                    Text(isReadinessComplete ? String(localized: "transport.readiness.complete_title") : String(localized: "transport.readiness.incomplete_title"))
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
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack(alignment: .top, spacing: WakeveTheme.Spacing.md) {
                    VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                        Text(String(localized: "transport.ai.title"))
                            .font(WakeveTheme.Typography.section)
                            .foregroundColor(primaryText)

                        Text(String(localized: "transport.ai.subtitle"))
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(secondaryText)
                    }

                    Spacer()

                    Button {
                        generateTransportSuggestion()
                    } label: {
                        Image(systemName: "sparkles")
                            .font(.headline.weight(.bold))
                            .foregroundColor(isGeneratingTransportAI ? secondaryText : SemanticColor.selectedState(for: colorScheme))
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
                            .accessibilityLabel(String(localized: "common.loading"))
                        Text(String(localized: "ai.preparing"))
                            .font(WakeveTheme.Typography.callout)
                            .foregroundColor(secondaryText)
                    }
                }

                if let transportAIError {
                    Text(transportAIError)
                        .font(WakeveTheme.Typography.callout)
                        .foregroundColor(SemanticColor.destructive(for: colorScheme))
                }

                if let suggestion = transportAISuggestion {
                    if !suggestion.missingDetails.isEmpty {
                        TransportAIList(title: String(localized: "transport.ai.missing_details"), values: suggestion.missingDetails, colorScheme: colorScheme)
                    }

                    if !suggestion.coordinationIdeas.isEmpty {
                        TransportAIList(title: String(localized: "transport.ai.ideas"), values: suggestion.coordinationIdeas, colorScheme: colorScheme)
                    }

                    Text(String(localized: "transport.ai.message_to_send"))
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
                        TransportAIActionButton(title: String(localized: "common.edit"), systemImage: "pencil", colorScheme: colorScheme) {
                            WakeveHaptics.selection()
                            appliedTransportAISuggestion = false
                        }
                        TransportAIActionButton(title: String(localized: "common.apply"), systemImage: "checkmark", colorScheme: colorScheme) {
                            WakeveHaptics.success()
                            appliedTransportAISuggestion = true
                            ignoredTransportAISuggestion = false
                        }
                        TransportAIActionButton(title: String(localized: "transport.ai.ignore_action"), systemImage: "xmark", colorScheme: colorScheme) {
                            WakeveHaptics.warning()
                            ignoredTransportAISuggestion = true
                            appliedTransportAISuggestion = false
                        }
                    }

                    if appliedTransportAISuggestion {
                        Label(String(localized: "transport.ai.applied"), systemImage: "checkmark.circle.fill")
                            .font(WakeveTheme.Typography.caption)
                            .foregroundColor(SemanticColor.confirmation(for: colorScheme))
                    } else if ignoredTransportAISuggestion {
                        Label(String(localized: "transport.ai.ignored"), systemImage: "minus.circle.fill")
                            .font(WakeveTheme.Typography.caption)
                            .foregroundColor(secondaryText)
                    }
                }
            }
        }
    }

    private var departureCard: some View {
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            let hasSelectedDestination = selectedDestination != nil
            let mutationDisabled = !canAccessDetails || isReadOnly || !workflowAllowsMutation(eventStatus) || !hasSelectedDestination
            let trimmedDeparture = departureInput.trimmingCharacters(in: .whitespacesAndNewlines)
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text(String(localized: "transport.departure.title"))
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(primaryText)

                HStack(spacing: WakeveTheme.Spacing.sm) {
                    TextField(String(localized: "transport.departure.placeholder"), text: $departureInput)
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
                            .background((mutationDisabled || trimmedDeparture.isEmpty) ? Color.gray.opacity(0.36) : SemanticColor.selectedState(for: colorScheme))
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
        WakeveContentCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            let hasSelectedDestination = selectedDestination != nil
            let mutationDisabled = !isOrganizer || isReadOnly || !workflowAllowsMutation(eventStatus) || !hasSelectedDestination
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text(String(localized: "transport.optimization.title"))
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(primaryText)

                HStack(spacing: 8) {
                    ForEach(TransportPlanningOptimizationType.allCases) { option in
                        Button {
                            WakeveHaptics.selection()
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
                    Button {
                        WakeveHaptics.warning()
                        onMarkTransportNotNeeded()
                    } label: {
                        Label(String(localized: "transport.not_required"), systemImage: "xmark.circle.fill")
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
        WakeveContentCard(prominence: .regular, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            let hasSelectedDestination = selectedDestination != nil
            let mutationDisabled = !isOrganizer || isReadOnly || !workflowAllowsMutation(eventStatus) || !hasSelectedDestination
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                Text(String(localized: "transport.generated_plan.title"))
                    .font(WakeveTheme.Typography.section)
                    .foregroundColor(primaryText)

                if plans.isEmpty {
                    Text(String(localized: "transport.generated_plan.empty"))
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
                                    Text(String(localized: "common.selected"))
                                        .font(WakeveTheme.Typography.tiny)
                                        .foregroundColor(SemanticColor.confirmation(for: colorScheme))
                                        .padding(.horizontal, WakeveTheme.Spacing.xs)
                                        .padding(.vertical, 4)
                                        .background(SemanticColor.confirmation(for: colorScheme).opacity(0.14))
                                        .clipShape(Capsule())
                                }
                            }

                            Text(String(format: String(localized: "transport.plan.total_cost_format"), plan.totalCost, plan.currency))
                                .font(WakeveTheme.Typography.callout)
                                .foregroundColor(secondaryText)

                            Button {
                                WakeveHaptics.success()
                                onSelectFinalPlan(plan)
                            } label: {
                                Label(selectedPlanId == plan.id ? String(localized: "transport.plan.selected") : String(localized: "transport.plan.select_final"), systemImage: "checkmark.seal.fill")
                                    .font(WakeveTheme.Typography.callout.weight(.semibold))
                                    .foregroundColor(selectedPlanId == plan.id ? SemanticColor.confirmation(for: colorScheme) : SemanticColor.selectedState(for: colorScheme))
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
                    Label(String(localized: "transport.not_required_set"), systemImage: "checkmark.circle.fill")
                        .font(WakeveTheme.Typography.callout.weight(.semibold))
                        .foregroundColor(SemanticColor.confirmation(for: colorScheme))
                }
            }
        }
    }

    private var participantsCard: some View {
        WakeveContentCard(prominence: .subtle, cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.md) {
                HStack {
                    Text(String(localized: "transport.participants.title"))
                        .font(WakeveTheme.Typography.section)
                        .foregroundColor(primaryText)
                    Spacer()
                    Text(isReadinessComplete ? String(localized: "transport.participants.ready") : missingDepartureBadgeText)
                        .font(WakeveTheme.Typography.tiny)
                        .foregroundColor(isReadinessComplete ? SemanticColor.confirmation(for: colorScheme) : SemanticColor.warning(for: colorScheme))
                        .padding(.horizontal, WakeveTheme.Spacing.xs)
                        .padding(.vertical, 4)
                        .background((isReadinessComplete ? SemanticColor.confirmation(for: colorScheme) : SemanticColor.warning(for: colorScheme)).opacity(0.14))
                        .clipShape(Capsule())
                }

                if displayedMissingDeparture.isEmpty {
                    TransportParticipantRow(name: String(localized: "transport.participants.all_confirmed"), detail: String(localized: "transport.departure.available"), isReady: true)
                } else {
                    ForEach(displayedMissingDeparture, id: \.self) { participant in
                        TransportParticipantRow(name: participant, detail: String(localized: "transport.departure.missing"), isReady: false)
                    }
                }
            }
        }
    }

    private var lockedState: some View {
        VStack {
            EmptyState(
                systemImage: "lock.fill",
                title: String(localized: "transport.locked.title"),
                subtitle: String(localized: "transport.locked.subtitle")
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
                    Text(String(localized: "sync.pending_changes"))
                        .font(WakeveTheme.Typography.tiny)
                        .lineLimit(1)
                        .minimumScaleFactor(0.78)
                }
                .foregroundColor(SemanticColor.warning(for: colorScheme))
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
                    SemanticColor.appBackground(for: colorScheme),
                    SemanticColor.appBackground(for: colorScheme).opacity(0)
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
                    SemanticColor.appBackground(for: colorScheme).opacity(0),
                    SemanticColor.appBackground(for: colorScheme)
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
            .background(SemanticColor.appBackground(for: colorScheme))
        }
    }

    private var transportStatusText: String {
        if transportNotNeeded {
            return String(localized: "transport.status.not_required")
        }
        if selectedPlanId != nil {
            return String(localized: "transport.status.final_selected")
        }
        if plans.isEmpty {
            return String(localized: "transport.status.to_generate")
        }
        return String(localized: "transport.status.available")
    }

    private var readinessSubtitle: String {
        if transportNotNeeded {
            return String(localized: "transport.readiness.not_required")
        }
        if displayedMissingDeparture.isEmpty {
            return String(localized: "transport.readiness.all_departures_ready")
        }
        return displayedMissingDeparture.count == 1
            ? String(format: String(localized: "transport.readiness.missing_departure_singular_format"), displayedMissingDeparture.count)
            : String(format: String(localized: "transport.readiness.missing_departure_plural_format"), displayedMissingDeparture.count)
    }

    private var selectedOptimizationFill: Color {
        colorScheme == .dark ? Color.white.opacity(0.9) : SemanticColor.selectedState(for: colorScheme)
    }

    private var selectedOptimizationText: Color {
        colorScheme == .dark ? WakeveTheme.ColorToken.midnight : .white
    }

    private var primaryActionTitle: String {
        if isReadOnly {
            return String(localized: "transport.action.read_only")
        }
        if selectedDestination == nil {
            return String(localized: "transport.action.choose_destination")
        }
        if transportNotNeeded {
            return String(localized: "transport.not_required")
        }
        if !isReadinessComplete {
            return String(localized: "transport.action.complete_departures")
        }
        if plans.isEmpty {
            return String(localized: "transport.action.prepare_plan")
        }
        if selectedPlanId == nil {
            return String(localized: "transport.action.choose_final_plan")
        }
        return String(localized: "transport.status.final_selected")
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
        let mutationDisabled = !isOrganizer || isReadOnly || !workflowAllowsMutation(eventStatus)
        if selectedDestination == nil {
            return mutationDisabled
        }
        return mutationDisabled || transportNotNeeded || !isReadinessComplete || (!plans.isEmpty && selectedPlanId != nil)
    }

    private func primaryAction() {
        if selectedDestination == nil {
            WakeveHaptics.selection()
            onChooseDestination()
            return
        }

        if plans.isEmpty {
            WakeveHaptics.selection()
            onGenerate(selectedOptimization)
            return
        }

        guard selectedPlanId == nil, let firstPlan = plans.first else {
            return
        }
        WakeveHaptics.success()
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
                let suggestion = try await generator.generate(eventId: event.id, localeIdentifier: Locale.autoupdatingCurrent.identifier)
                await MainActor.run {
                    transportAISuggestion = suggestion
                    editableTransportMessage = suggestion.groupMessageDraft
                    isGeneratingTransportAI = false
                    WakeveHaptics.success()
                }
            } catch {
                await MainActor.run {
                    transportAIError = String(localized: "transport.ai.error_unavailable")
                    isGeneratingTransportAI = false
                    WakeveHaptics.warning()
                }
            }
        }
    }

    private var primaryText: Color {
        SemanticColor.primaryText(for: colorScheme)
    }

    private var secondaryText: Color {
        SemanticColor.secondaryText(for: colorScheme)
    }

    private var controlFill: Color {
        SemanticColor.badge(for: colorScheme)
    }

    private func workflowAllowsMutation(_ eventStatus: EventStatus) -> Bool {
        eventStatus == EventStatus.confirmed || eventStatus == EventStatus.comparing || eventStatus == EventStatus.organizing
    }

    private var departureReadinessRouteSubtitle: String {
        if isReadinessComplete {
            return String(localized: "transport.route.departures_ready")
        }

        return displayedMissingDeparture.count == 1
            ? String(format: String(localized: "transport.route.departure_missing_singular_format"), displayedMissingDeparture.count)
            : String(format: String(localized: "transport.route.departure_missing_plural_format"), displayedMissingDeparture.count)
    }

    private var missingDepartureBadgeText: String {
        displayedMissingDeparture.count == 1
            ? String(format: String(localized: "transport.participants.to_complete_singular_format"), displayedMissingDeparture.count)
            : String(format: String(localized: "transport.participants.to_complete_plural_format"), displayedMissingDeparture.count)
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
        let languageCode = Locale.autoupdatingCurrent.language.languageCode?.identifier ?? "en"
        return WakeveAIUserPreferences(languageCode: languageCode, localPreferences: [])
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
                .foregroundColor(SemanticColor.primaryText(for: colorScheme))

            ForEach(values, id: \.self) { value in
                Label(value, systemImage: "checkmark.circle")
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
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
                .foregroundColor(SemanticColor.primaryText(for: colorScheme))
                .frame(maxWidth: .infinity)
                .frame(height: 36)
                .background(SemanticColor.badge(for: colorScheme))
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
                .foregroundColor(SemanticColor.primaryText(for: colorScheme))
                .lineLimit(1)
                .minimumScaleFactor(0.78)

            Text(subtitle)
                .font(WakeveTheme.Typography.callout)
                .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
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
                .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
                .textCase(.uppercase)

            Text(value)
                .font(WakeveTheme.Typography.caption)
                .foregroundColor(SemanticColor.primaryText(for: colorScheme))
                .lineLimit(1)
                .minimumScaleFactor(0.76)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(WakeveTheme.Spacing.sm)
        .background(SemanticColor.badge(for: colorScheme))
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
                .foregroundColor(isReady ? SemanticColor.confirmation(for: colorScheme) : SemanticColor.warning(for: colorScheme))
                .frame(width: 36, height: 36)
                .background(SemanticColor.badge(for: colorScheme))
                .clipShape(Circle())

            VStack(alignment: .leading, spacing: WakeveTheme.Spacing.xxs) {
                Text(name)
                    .font(WakeveTheme.Typography.bodySemibold)
                    .foregroundColor(SemanticColor.primaryText(for: colorScheme))
                    .lineLimit(1)
                    .minimumScaleFactor(0.78)
                Text(detail)
                    .font(WakeveTheme.Typography.callout)
                    .foregroundColor(SemanticColor.secondaryText(for: colorScheme))
            }

            Spacer()
        }
        .padding(WakeveTheme.Spacing.sm)
        .background(SemanticColor.badge(for: colorScheme))
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
            return String(localized: "transport.optimization.cost")
        case .TIME_MINIMIZE:
            return String(localized: "transport.optimization.time")
        case .BALANCED:
            return String(localized: "transport.optimization.balanced")
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
