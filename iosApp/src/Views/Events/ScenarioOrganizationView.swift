import SwiftUI
import Shared

struct ScenarioOrganizationView: View {
    @Environment(\.colorScheme) private var colorScheme

    let event: Event
    let participantId: String
    let repository: EventRepositoryInterface
    let onBack: () -> Void
    let onOpenMeetings: () -> Void
    let onOpenTransport: () -> Void

    @StateObject private var viewModel: ScenarioListViewModel
    @State private var canAccessOrganizationDetails = false
    @State private var selectedComparisonIds: Set<String> = []
    @State private var didLoad = false

    init(
        event: Event,
        participantId: String,
        repository: EventRepositoryInterface,
        onBack: @escaping () -> Void,
        onOpenMeetings: @escaping () -> Void,
        onOpenTransport: @escaping () -> Void
    ) {
        self.event = event
        self.participantId = participantId
        self.repository = repository
        self.onBack = onBack
        self.onOpenMeetings = onOpenMeetings
        self.onOpenTransport = onOpenTransport
        _viewModel = StateObject(wrappedValue: ScenarioListViewModel())
    }

    var body: some View {
        ZStack(alignment: .top) {
            pageBackground
                .ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(spacing: 0) {
                    heroSection

                    VStack(alignment: .leading, spacing: 18) {
                        summarySection

                        if isLocked {
                            lockedState
                        } else {
                            scenarioContent
                        }
                    }
                    .padding(.horizontal, WakeveTheme.Spacing.page)
                    .padding(.top, -24)
                    .padding(.bottom, 104)
                }
            }
            .ignoresSafeArea(edges: .top)

            topControls
        }
        .toolbar(.hidden, for: .tabBar)
        .safeAreaInset(edge: .bottom, spacing: 0) {
            bottomComparisonBar
        }
        .onAppear(perform: loadIfNeeded)
        .onChange(of: viewModel.navigationRoute) { _, route in
            guard route?.hasPrefix("meetings/") == true else { return }
            viewModel.navigationRoute = nil
            onOpenMeetings()
        }
        .alert(String(localized: "scenario.alert.title"), isPresented: toastBinding) {
            Button(String(localized: "common.ok"), role: .cancel) {
                viewModel.toastMessage = nil
            }
        } message: {
            Text(viewModel.toastMessage ?? "")
        }
    }

    private var heroSection: some View {
        ZStack(alignment: .bottomLeading) {
            LinearGradient(
                colors: heroColors,
                startPoint: .topLeading,
                endPoint: .bottomTrailing
            )
            .overlay(alignment: .trailing) {
                Image(systemName: "map.fill")
                    .font(.system(size: 118, weight: .black))
                    .foregroundColor(.white.opacity(0.18))
                    .rotationEffect(.degrees(-10))
                    .offset(x: 42, y: -6)
            }
            .overlay(alignment: .bottom) {
                LinearGradient(
                    colors: [.clear, pageBackground.opacity(colorScheme == .dark ? 0.94 : 0.98)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 146)
            }

            VStack(alignment: .leading, spacing: 10) {
                phaseBadge

                Text(String(localized: "scenario.title"))
                    .font(.system(size: 42, weight: .bold))
                    .foregroundColor(.white)
                    .lineLimit(2)
                    .minimumScaleFactor(0.74)

                Text(event.title)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundColor(.white.opacity(0.74))
                    .lineLimit(2)
            }
            .padding(.horizontal, 16)
            .padding(.bottom, 62)
        }
        .frame(height: 326)
    }

    private var topControls: some View {
        HStack {
            WakeveCircleButton(
                systemImage: "chevron.left",
                accessibilityLabel: String(localized: "common.back"),
                variant: .glass,
                size: 44,
                action: onBack
            )

            Spacer()

            Button {
                viewModel.initialize(eventId: event.id, participantId: participantId)
            } label: {
                WakeveGlassControl {
                    Image(systemName: "arrow.clockwise")
                        .font(.system(size: 17, weight: .bold))
                        .foregroundColor(.white)
                        .frame(width: 44, height: 44)
                }
            }
            .accessibilityLabel(String(localized: "scenario.refresh_accessibility"))
        }
        .padding(.horizontal, 16)
        .padding(.top, WakeveTheme.Navigation.controlTopSpacing)
    }

    private var phaseBadge: some View {
        HStack(spacing: 6) {
            Image(systemName: isLocked ? "lock.fill" : "rectangle.3.group.fill")
                .font(.system(size: 13, weight: .bold))
            Text(isLocked ? String(localized: "scenario.access_locked") : phaseText)
                .font(.system(size: 14, weight: .bold))
        }
        .foregroundColor(.white)
        .padding(.horizontal, 12)
        .padding(.vertical, 7)
        .background(Color.black.opacity(0.26))
        .clipShape(Capsule())
    }

    private var summarySection: some View {
        WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
            VStack(alignment: .leading, spacing: 14) {
                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 5) {
                        Text(String(format: String(localized: "scenario.options_count_format"), viewModel.scenarios.count))
                            .font(.system(size: 22, weight: .bold))
                            .foregroundColor(primaryText)

                        Text(summaryText)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundColor(secondaryText)
                            .lineSpacing(3)
                    }

                    Spacer()

                    Image(systemName: "chart.bar.xaxis")
                        .font(.system(size: 22, weight: .bold))
                        .foregroundColor(.blue)
                        .frame(width: 46, height: 46)
                        .background(Color.blue.opacity(0.14))
                        .clipShape(Circle())
                }

                HStack(spacing: 10) {
                    metricPill(icon: "mappin.and.ellipse", text: bestLocationText)
                    metricPill(icon: "person.2.fill", text: participantText)
                }

                if canOpenTransport {
                    WakeveActionButton(
                        "Transport",
                        systemImage: "point.topleft.down.curvedto.point.bottomright.up.fill",
                        variant: .secondary,
                        action: onOpenTransport
                    )
                }
            }
        }
    }

    @ViewBuilder
    private var scenarioContent: some View {
        if viewModel.isLoading && viewModel.scenarios.isEmpty {
            ProgressView()
                .tint(.blue)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 40)
                .accessibilityLabel(String(localized: "common.loading"))
        } else if viewModel.scenarios.isEmpty {
            emptyState
        } else {
            if let comparison = viewModel.comparison, comparison.scenarios.count >= 2 {
                comparisonSection(scenarios: comparison.scenarios)
            }

            VStack(alignment: .leading, spacing: 12) {
                Text(String(localized: "scenario.ranking"))
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(primaryText)

                ForEach(viewModel.scenariosRanked, id: \.scenario.id) { scenarioWithVotes in
                    ScenarioOrganizationCard(
                        scenarioWithVotes: scenarioWithVotes,
                        isOrganizer: isOrganizer,
                        canSelectFinal: viewModel.canSelectScenarioAsFinal,
                        isSelectedForComparison: selectedComparisonIds.contains(scenarioWithVotes.scenario.id),
                        currentParticipantVote: currentVote(in: scenarioWithVotes),
                        onToggleComparison: {
                            toggleComparison(scenarioWithVotes.scenario.id)
                        },
                        onVote: { vote in
                            viewModel.voteScenario(scenarioId: scenarioWithVotes.scenario.id, voteType: vote)
                        },
                        onSelectFinal: {
                            viewModel.selectScenarioAsFinal(
                                eventId: event.id,
                                scenarioId: scenarioWithVotes.scenario.id,
                                userId: participantId
                            )
                        }
                    )
                }
            }
        }
    }

    private func comparisonSection(scenarios: [ScenarioWithVotes]) -> some View {
        WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
            VStack(alignment: .leading, spacing: 16) {
                HStack {
                    Label(String(localized: "scenario.comparison"), systemImage: "rectangle.split.3x1.fill")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(primaryText)

                    Spacer()

                    Button(String(localized: "common.close")) {
                        viewModel.clearComparison()
                        selectedComparisonIds.removeAll()
                    }
                    .font(.system(size: 14, weight: .bold))
                    .foregroundColor(.blue)
                }

                ForEach(scenarios, id: \.scenario.id) { item in
                    VStack(alignment: .leading, spacing: 10) {
                        Text(item.scenario.name)
                            .font(.system(size: 17, weight: .bold))
                            .foregroundColor(primaryText)

                        HStack(spacing: 8) {
                            comparisonMetric(String(localized: "scenario.score"), "\(item.votingResult.score)")
                            comparisonMetric(String(localized: "scenario.budget"), formatBudget(item.scenario.estimatedBudgetPerPerson))
                            comparisonMetric(String(localized: "scenario.duration"), String(format: String(localized: "scenario.duration_days_format"), item.scenario.duration))
                        }
                    }
                    .padding(.vertical, 4)
                }
            }
        }
    }

    private var lockedState: some View {
        WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
            VStack(spacing: 16) {
                Image(systemName: "lock.fill")
                    .font(.system(size: 38, weight: .bold))
                    .foregroundColor(secondaryText)
                    .frame(width: 74, height: 74)
                    .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
                    .clipShape(Circle())

                Text(String(localized: "scenario.locked.title"))
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(primaryText)
                    .multilineTextAlignment(.center)

                Text(String(localized: "scenario.locked.subtitle"))
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(secondaryText)
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
            }
            .frame(maxWidth: .infinity)
        }
    }

    private var emptyState: some View {
        WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl) {
            VStack(spacing: 14) {
                Image(systemName: "map")
                    .font(.system(size: 40, weight: .semibold))
                    .foregroundColor(secondaryText)

                Text(String(localized: "scenario.empty.title"))
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(primaryText)

                Text(isOrganizer ? String(localized: "scenario.empty.organizer_subtitle") : String(localized: "scenario.empty.participant_subtitle"))
                    .font(.system(size: 15, weight: .medium))
                    .foregroundColor(secondaryText)
                    .multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
        }
    }

    @ViewBuilder
    private var bottomComparisonBar: some View {
        if !isLocked && selectedComparisonIds.count >= 2 {
            VStack(spacing: 0) {
                LinearGradient(
                    colors: [pageBackground.opacity(0), pageBackground.opacity(0.95)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 36)
                .allowsHitTesting(false)

                WakeveActionButton(
                    "Comparer \(selectedComparisonIds.count) scenarios",
                    systemImage: "rectangle.split.3x1.fill",
                    variant: .primary,
                    isLoading: viewModel.isLoading
                ) {
                    viewModel.compareScenarios(scenarioIds: Array(selectedComparisonIds))
                }
                .padding(.horizontal, WakeveTheme.Spacing.page)
                .padding(.top, 8)
                .padding(.bottom, 12)
                .background(pageBackground.opacity(0.95))
            }
        }
    }

    private func metricPill(icon: String, text: String) -> some View {
        HStack(spacing: 6) {
            Image(systemName: icon)
            Text(text)
                .lineLimit(1)
                .minimumScaleFactor(0.82)
        }
        .font(.system(size: 13, weight: .bold))
        .foregroundColor(primaryText)
        .padding(.horizontal, 10)
        .padding(.vertical, 7)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
        .clipShape(Capsule())
    }

    private func comparisonMetric(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(label)
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(secondaryText)
            Text(value)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(primaryText)
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(10)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func loadIfNeeded() {
        guard !didLoad else { return }
        didLoad = true
        refreshAccess()
        viewModel.initialize(eventId: event.id, participantId: participantId)
    }

    private func refreshAccess() {
        if isOrganizer {
            canAccessOrganizationDetails = true
            return
        }

        guard let participantRecords = repository.getParticipantRecords(eventId: event.id), !participantRecords.isEmpty else {
            canAccessOrganizationDetails = false
            return
        }

        let participantAccessStates = participantRecords.map { record in
            ParticipantAccessMapper.shared.fromRepositoryRecord(record: record)
        }

        let rows = ParticipantManagementPresentationMapper.shared.map(participants: participantAccessStates)
        canAccessOrganizationDetails = rows.first { $0.userIdOrEmail == participantId }?.canAccessOrganizationDetails ?? false
    }

    private func toggleComparison(_ scenarioId: String) {
        if selectedComparisonIds.contains(scenarioId) {
            selectedComparisonIds.remove(scenarioId)
        } else {
            selectedComparisonIds.insert(scenarioId)
        }
    }

    private func currentVote(in scenarioWithVotes: ScenarioWithVotes) -> ScenarioVoteType? {
        scenarioWithVotes.votes.first { $0.participantId == participantId }?.vote
    }

    private func formatBudget(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "EUR"
        formatter.maximumFractionDigits = value.rounded() == value ? 0 : 2
        return formatter.string(from: NSNumber(value: value)) ?? "\(Int(value)) EUR"
    }

    private var toastBinding: Binding<Bool> {
        Binding(
            get: { viewModel.toastMessage != nil },
            set: { isPresented in
                if !isPresented {
                    viewModel.toastMessage = nil
                }
            }
        )
    }

    private var isOrganizer: Bool {
        event.organizerId == participantId
    }

    private var isLocked: Bool {
        if let error = viewModel.errorMessage, error.localizedCaseInsensitiveContains("confirmed participant") {
            return true
        }
        return !canAccessOrganizationDetails
    }

    private var summaryText: String {
        if isLocked {
            return String(localized: "scenario.summary.locked")
        }
        return String(localized: "scenario.summary.open")
    }

    private var bestLocationText: String {
        viewModel.scenariosRanked.first?.scenario.location ?? String(localized: "scenario.destination_tbd")
    }

    private var participantText: String {
        if let expected = event.expectedParticipants?.intValue {
            return String(format: String(localized: "scenario.invited_count_format"), expected)
        }
        return String(format: String(localized: "scenario.invited_count_format"), event.participants.count)
    }

    private var phaseText: String {
        switch viewModel.eventStatus ?? event.status {
        case .confirmed:
            return String(localized: "scenario.phase.options_open")
        case .comparing:
            return String(localized: "scenario.phase.comparison")
        case .organizing:
            return String(localized: "scenario.phase.organization")
        case .finalized:
            return String(localized: "scenario.phase.finalized")
        default:
            return String(localized: "scenario.phase.preparation")
        }
    }

    private var canOpenTransport: Bool {
        switch viewModel.eventStatus ?? event.status {
        case .organizing, .finalized:
            return !isLocked
        default:
            return false
        }
    }

    private var heroColors: [Color] {
        [
            Color(hex: "0F766E"),
            Color(hex: "2563EB"),
            pageBackground
        ]
    }

    private var pageBackground: Color {
        WakeveTheme.ColorToken.pageBackground(for: colorScheme)
    }

    private var primaryText: Color {
        WakeveTheme.ColorToken.primaryText(for: colorScheme)
    }

    private var secondaryText: Color {
        WakeveTheme.ColorToken.secondaryText(for: colorScheme)
    }
}

private struct ScenarioOrganizationCard: View {
    @Environment(\.colorScheme) private var colorScheme

    let scenarioWithVotes: ScenarioWithVotes
    let isOrganizer: Bool
    let canSelectFinal: Bool
    let isSelectedForComparison: Bool
    let currentParticipantVote: ScenarioVoteType?
    let onToggleComparison: () -> Void
    let onVote: (ScenarioVoteType) -> Void
    let onSelectFinal: () -> Void

    private var scenario: Scenario_ {
        scenarioWithVotes.scenario
    }

    private var votingResult: ScenarioVotingResult {
        scenarioWithVotes.votingResult
    }

    var body: some View {
        WakeveGlassCard(cornerRadius: WakeveTheme.Radius.xl, padding: WakeveTheme.Spacing.md) {
            VStack(alignment: .leading, spacing: 16) {
                header
                logisticsGrid
                votingSummary
                actionRows
            }
        }
        .accessibilityElement(children: .contain)
    }

    private var header: some View {
        HStack(alignment: .top, spacing: 12) {
            VStack(alignment: .leading, spacing: 6) {
                HStack(spacing: 8) {
                    statusBadge

                    Text(String(format: String(localized: "scenario.score_format"), votingResult.score))
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(secondaryText)
                }

                Text(scenario.name)
                    .font(.system(size: 20, weight: .bold))
                    .foregroundColor(primaryText)
                    .lineLimit(2)

                if scenario.generationType == .matrix {
                    Text(matrixSourceLabel)
                        .font(.system(size: 12, weight: .bold))
                        .foregroundColor(.blue)
                        .lineLimit(1)
                        .minimumScaleFactor(0.78)
                }

                Text(scenario.description_)
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(secondaryText)
                    .lineLimit(3)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer()

            Button(action: onToggleComparison) {
                Image(systemName: isSelectedForComparison ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(isSelectedForComparison ? .blue : secondaryText)
                    .frame(width: 34, height: 34)
            }
            .accessibilityLabel(isSelectedForComparison ? String(localized: "scenario.remove_from_comparison_accessibility") : String(localized: "scenario.add_to_comparison_accessibility"))
        }
    }

    private var logisticsGrid: some View {
        LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
            detailTile(icon: "mappin.and.ellipse", label: String(localized: "scenario.destination"), value: scenario.location)
            detailTile(icon: "calendar", label: String(localized: "scenario.dates"), value: scenario.dateOrPeriod)
            detailTile(icon: "person.2.fill", label: String(localized: "scenario.participants"), value: "\(scenario.estimatedParticipants)")
            detailTile(icon: "eurosign.circle.fill", label: String(localized: "scenario.budget"), value: formatBudget(scenario.estimatedBudgetPerPerson))
            detailTile(icon: "moon.stars.fill", label: String(localized: "scenario.duration"), value: String(format: String(localized: "scenario.duration_nights_format"), scenario.duration))
            detailTile(icon: "house.fill", label: String(localized: "scenario.lodging"), value: lodgingLabel)
        }
    }

    private var votingSummary: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack {
                Text(String(localized: "scenario.votes"))
                    .font(.system(size: 15, weight: .bold))
                    .foregroundColor(primaryText)
                Spacer()
                Text(String(format: String(localized: "scenario.votes_total_format"), votingResult.totalVotes))
                    .font(.system(size: 13, weight: .bold))
                    .foregroundColor(secondaryText)
            }

            HStack(spacing: 8) {
                voteMetric(String(localized: "scenario.vote.prefer"), votingResult.preferCount, .green)
                voteMetric(String(localized: "scenario.vote.neutral"), votingResult.neutralCount, .blue)
                voteMetric(String(localized: "scenario.vote.against"), votingResult.againstCount, .red)
            }
        }
    }

    private var actionRows: some View {
        VStack(spacing: 10) {
            HStack(spacing: 8) {
                voteButton(title: String(localized: "scenario.vote.prefer"), icon: "hand.thumbsup.fill", vote: .prefer)
                voteButton(title: String(localized: "scenario.vote.neutral"), icon: "minus.circle.fill", vote: .neutral)
                voteButton(title: String(localized: "scenario.vote.against"), icon: "hand.thumbsdown.fill", vote: .against)
            }

            if isOrganizer && canSelectFinal && scenario.status != .selected {
                WakeveActionButton(
                    String(localized: "scenario.select_final"),
                    systemImage: "checkmark.seal.fill",
                    variant: .eventNext,
                    action: onSelectFinal
                )
            }
        }
    }

    private var statusBadge: some View {
        HStack(spacing: 5) {
            Circle()
                .fill(statusColor)
                .frame(width: 7, height: 7)
            Text(statusText)
                .font(.system(size: 12, weight: .bold))
        }
        .foregroundColor(primaryText)
        .padding(.horizontal, 9)
        .padding(.vertical, 5)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
        .clipShape(Capsule())
    }

    private func detailTile(icon: String, label: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Image(systemName: icon)
                .font(.system(size: 15, weight: .bold))
                .foregroundColor(.blue)
            Text(label)
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(secondaryText)
            Text(value)
                .font(.system(size: 14, weight: .bold))
                .foregroundColor(primaryText)
                .lineLimit(2)
                .minimumScaleFactor(0.78)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(12)
        .background(WakeveTheme.ColorToken.controlFill(for: colorScheme))
        .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    }

    private func voteMetric(_ label: String, _ count: Int32, _ color: Color) -> some View {
        VStack(spacing: 4) {
            Text("\(count)")
                .font(.system(size: 16, weight: .bold))
                .foregroundColor(primaryText)
            Text(label)
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(secondaryText)
                .lineLimit(1)
                .minimumScaleFactor(0.72)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 10)
        .background(color.opacity(0.12))
        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func voteButton(title: String, icon: String, vote: ScenarioVoteType) -> some View {
        let isActive = currentParticipantVote == vote

        return Button {
            onVote(vote)
        } label: {
            VStack(spacing: 6) {
                Image(systemName: icon)
                    .font(.system(size: 15, weight: .bold))
                Text(title)
                    .font(.system(size: 12, weight: .bold))
                    .lineLimit(1)
                    .minimumScaleFactor(0.72)
            }
            .foregroundColor(isActive ? .white : primaryText)
            .frame(maxWidth: .infinity)
            .frame(height: 54)
            .background(isActive ? Color.blue : WakeveTheme.ColorToken.controlFill(for: colorScheme))
            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
        }
        .buttonStyle(.plain)
        .disabled(scenario.status == .selected)
        .opacity(scenario.status == .selected ? 0.55 : 1)
    }

    private func formatBudget(_ value: Double) -> String {
        let formatter = NumberFormatter()
        formatter.numberStyle = .currency
        formatter.currencyCode = "EUR"
        formatter.maximumFractionDigits = value.rounded() == value ? 0 : 2
        return formatter.string(from: NSNumber(value: value)) ?? "\(Int(value)) EUR"
    }

    private var lodgingLabel: String {
        switch scenario.status {
        case .draft:
            return String(localized: "scenario.status.draft")
        case .selected:
            return String(localized: "scenario.status.retained")
        case .rejected:
            return String(localized: "scenario.status.rejected")
        default:
            return String(localized: "scenario.status.to_compare")
        }
    }

    private var statusText: String {
        switch scenario.status {
        case .draft:
            return String(localized: "scenario.status.draft")
        case .selected:
            return String(localized: "scenario.status.selected")
        case .rejected:
            return String(localized: "scenario.status.rejected")
        default:
            return String(localized: "scenario.status.proposed")
        }
    }

    private var statusColor: Color {
        switch scenario.status {
        case .draft:
            return .orange
        case .selected:
            return .green
        case .rejected:
            return .red
        default:
            return .blue
        }
    }

    private var primaryText: Color {
        WakeveTheme.ColorToken.primaryText(for: colorScheme)
    }

    private var secondaryText: Color {
        WakeveTheme.ColorToken.secondaryText(for: colorScheme)
    }

    private var matrixSourceLabel: String {
        let slot = scenario.sourceTimeSlotId ?? "-"
        let destination = scenario.sourcePotentialLocationId ?? "-"
        return String(format: String(localized: "scenario.matrix_source_format"), slot, destination)
    }
}
