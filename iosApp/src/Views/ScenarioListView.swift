import SwiftUI
import Shared

/// Scenario List View - iOS
///
/// Displays all scenarios for an event with voting interface.
/// Uses Liquid Glass design system with Material backgrounds.
/// Uses State Machine via ViewModel for state management.
struct ScenarioListView: View {
    let event: Event
    let participantId: String
    let onScenarioTap: (Scenario_) -> Void
    let onCompareTap: () -> Void
    let onCreateScenarioTap: () -> Void
    let onBack: () -> Void
    
    @StateObject private var viewModel = ScenarioListViewModel()
    
    var body: some View {
        ZStack {
            Color(.systemGroupedBackground)
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                headerView
                
                if viewModel.isLoading {
                    loadingView
                } else if viewModel.isEmpty {
                    emptyStateView
                } else {
                    ScrollView {
                        VStack(spacing: 16) {
                            createScenarioButton
                            
                            if viewModel.scenarios.count > 1 {
                                compareButton
                            }
                            
                            LiquidGlassDivider(style: .default)
                                                        ForEach(viewModel.scenarios, id: \.scenario.id) { scenarioWithVotes in
                                scenarioCard(scenarioWithVotes: scenarioWithVotes)
                            }
                            
                            Spacer()
                                .frame(height: 40)
                        }
                        .padding(.horizontal, 20)
                        .padding(.top, 16)
                    }
                }
            }
        }
        .onAppear {
            viewModel.initialize(eventId: event.id, participantId: participantId)
        }
        .alert("Error", isPresented: Binding(
            get: { viewModel.hasError },
            set: { if !$0 { viewModel.clearError() } }
        )) {
            Button("OK", role: .cancel) { viewModel.clearError() }
        } message: {
            Text(viewModel.errorMessage ?? "An error occurred")
        }
    }
    
    // MARK: - Header View
    
    private var headerView: some View {
        VStack(spacing: 16) {
            HStack {
                LiquidGlassButton(
                    icon: "arrow.left",
                    style: .icon,
                    size: .small,
                    action: onBack
                )
                
                Spacer()
            }
            .padding(.horizontal, 20)
            .padding(.top, 60)
            
            VStack(spacing: 8) {
                Text(NSLocalizedString("choose_scenario_title", comment: "Title for choosing a scenario"))
                    .font(.system(size: 34, weight: .bold))
                    .foregroundColor(.primary)
                    .frame(maxWidth: .infinity, alignment: .leading)
                
                Text(event.title)
                    .font(.system(size: 20, weight: .medium))
                    .foregroundColor(.secondary)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 20)
        }
        .background(Color(.systemGroupedBackground))
    }
    
    // MARK: - Create Scenario Button
    
    private var createScenarioButton: some View {
        LiquidGlassButton(
            title: NSLocalizedString("create_scenario", comment: "Create scenario button text"),
            icon: "plus.circle.fill",
            style: .primary,
            size: .medium,
            action: onCreateScenarioTap
        )
        .accessibilityLabel("Create a new scenario")
        .accessibilityHint("Tap to create a new scenario for this event")
    }
    
    // MARK: - Compare Button
    
    private var compareButton: some View {
        LiquidGlassButton(
            title: NSLocalizedString("compare", comment: "Compare button text"),
            icon: "arrow.left.arrow.right",
            style: .secondary,
            size: .medium,
            action: onCompareTap
        )
        .accessibilityLabel("Compare scenarios")
        .accessibilityHint("Tap to compare all scenarios side by side")
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
                .scaleEffect(1.2)
            
            Text(NSLocalizedString("loading_scenarios", comment: "Loading scenarios text"))
                .font(.system(size: 17))
                .foregroundColor(.secondary)
        }
        .frame(maxHeight: .infinity)
    }
    
    // MARK: - Empty State
    
    private var emptyStateView: some View {
        VStack(spacing: 24) {
            ZStack {
                Circle()
                    .fill(Color.wakevePrimary.opacity(0.1))
                    .frame(width: 80, height: 80)
                
                Image(systemName: "list.bullet.rectangle.portrait")
                    .font(.system(size: 36))
                    .foregroundColor(.wakevePrimary)
            }
            
            VStack(spacing: 8) {
                Text(NSLocalizedString("no_scenarios_yet_title", comment: "No scenarios yet title"))
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.primary)
                
                Text(NSLocalizedString("organizer_will_add", comment: "Organizer will add scenarios text"))
                    .font(.system(size: 17))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
            
            LiquidGlassButton(
                title: NSLocalizedString("create_first_scenario", comment: "Create first scenario button text"),
                icon: "plus.circle.fill",
                style: .primary,
                size: .medium,
                action: onCreateScenarioTap
            )
            .padding(.top, 8)
        }
        .frame(maxHeight: .infinity)
    }
    
    // MARK: - Scenario Card
    
    private func scenarioCard(scenarioWithVotes: ScenarioWithVotes) -> some View {
        let scenario = scenarioWithVotes.scenario
        let userVote = getUserVote(for: scenarioWithVotes)
        let votingResult = scenarioWithVotes.votingResult
        
        return LiquidGlassCard(cornerRadius: 16, padding: 20) {
            VStack(alignment: .leading, spacing: 16) {
                scenarioHeader(scenario: scenario)
                
                LiquidGlassDivider(style: .subtle)
                
                scenarioInfoSection(scenario: scenario)
                
                if votingResult.totalVotes > 0 {
                    votingResultsSection(result: votingResult)
                }
                
                votingButtons(currentVote: userVote?.vote) { voteType in
                    viewModel.voteScenario(
                        scenarioId: scenarioWithVotes.scenario.id,
                        voteType: voteType
                    )
                }
                
                viewDetailsButton(onTap: { onScenarioTap(scenarioWithVotes.scenario) })
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(scenario.name) scenario")
        .accessibilityHint("Tap to view details of \(scenario.name)")
    }
    
    // MARK: - Scenario Header
    
    private func scenarioHeader(scenario: Scenario_) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Text(scenario.name)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(.primary)
                
                Text(scenario.dateOrPeriod)
                    .font(.system(size: 14))
                    .foregroundColor(.secondary)
            }
            
            Spacer()
            
            statusBadge(for: scenario.status.name)
        }
    }
    
    // MARK: - Status Badge
    
    private func statusBadge(for status: String) -> some View {
        let badgeType: LiquidGlassBadgeStyle
        let badgeText: String
        
        switch status.uppercased() {
        case "PROPOSED":
            badgeType = .success
            badgeText = NSLocalizedString("status_proposed", comment: "Proposed status")
        case "SELECTED":
            badgeType = .success
            badgeText = NSLocalizedString("status_selected", comment: "Selected status")
        case "REJECTED":
            badgeType = .warning
            badgeText = NSLocalizedString("status_rejected", comment: "Rejected status")
        default:
            badgeType = .success
            badgeText = status
        }
        
        return LiquidGlassBadge(
            text: badgeText,
            style: badgeType
        )
    }
    
    // MARK: - Scenario Info Section
    
    private func scenarioInfoSection(scenario: Scenario_) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            scenarioInfoRow(
                label: NSLocalizedString("location_label", comment: "Location label"),
                value: scenario.location,
                icon: "mappin.circle.fill"
            )
            
            scenarioInfoRow(
                label: NSLocalizedString("duration_label_short", comment: "Duration label"),
                value: "\(scenario.duration) \(NSLocalizedString("days_label", comment: "Days label"))",
                icon: "clock.fill"
            )
            
            scenarioInfoRow(
                label: NSLocalizedString("budget_label", comment: "Budget label"),
                value: String(format: NSLocalizedString("budget_per_person_label", comment: "Budget per person format"), scenario.estimatedBudgetPerPerson),
                icon: "dollarsign.circle.fill"
            )
        }
    }
    
    // MARK: - Scenario Info Row
    
    private func scenarioInfoRow(label: String, value: String, icon: String) -> some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.wakevePrimary)
                .frame(width: 24)
            
            VStack(alignment: .leading, spacing: 2) {
                Text(label)
                    .font(.system(size: 13, weight: .medium))
                    .foregroundColor(.secondary)
                
                Text(value)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.primary)
            }
            
            Spacer()
        }
    }
    
    // MARK: - Voting Results Section
    
    private func votingResultsSection(result: ScenarioVotingResult) -> some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text(NSLocalizedString("voting_results_label", comment: "Voting results label"))
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.primary)
                
                Spacer()
                
                Text("\(NSLocalizedString("score_label_short", comment: "Score label")): \(result.score)")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundColor(.secondary)
            }
            
            HStack(spacing: 12) {
                voteCount(
                    count: Int(result.preferCount),
                    color: .wakeveSuccess,
                    label: NSLocalizedString("prefer_label", comment: "Prefer label")
                )
                
                voteCount(
                    count: Int(result.neutralCount),
                    color: .wakeveWarning,
                    label: NSLocalizedString("neutral_label", comment: "Neutral label")
                )
                
                voteCount(
                    count: Int(result.againstCount),
                    color: .wakeveError,
                    label: NSLocalizedString("against_label", comment: "Against label")
                )
            }
        }
        .padding(16)
        .background(Color.wakeveSurfaceLight)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
    }
    
    // MARK: - Vote Count
    
    private func voteCount(count: Int, color: Color, label: String) -> some View {
        VStack(spacing: 4) {
            Text("\(count)")
                .font(.system(size: 20, weight: .bold))
                .foregroundColor(color)
            
            Text(label)
                .font(.system(size: 12))
                .foregroundColor(.secondary)
        }
        .frame(maxWidth: .infinity)
    }
    
    // MARK: - Voting Buttons
    
    private func votingButtons(currentVote: ScenarioVoteType?, onVote: @escaping (ScenarioVoteType) -> Void) -> some View {
        HStack(spacing: 12) {
            voteButton(
                type: .prefer,
                isSelected: currentVote == .prefer,
                onTap: { onVote(.prefer) }
            )
            
            voteButton(
                type: .neutral,
                isSelected: currentVote == .neutral,
                onTap: { onVote(.neutral) }
            )
            
            voteButton(
                type: .against,
                isSelected: currentVote == .against,
                onTap: { onVote(.against) }
            )
        }
    }
    
    // MARK: - Vote Button
    
    private func voteButton(type: ScenarioVoteType, isSelected: Bool, onTap: @escaping () -> Void) -> some View {
        let icon: String
        let label: String
        
        switch type {
        case .prefer:
            icon = "hand.thumbsup.fill"
            label = NSLocalizedString("prefer_label", comment: "Prefer label")
        case .neutral:
            icon = "minus.circle.fill"
            label = NSLocalizedString("neutral_label", comment: "Neutral label")
        case .against:
            icon = "hand.thumbsdown.fill"
            label = NSLocalizedString("against_label", comment: "Against label")
        default:
            icon = "questionmark.circle.fill"
            label = NSLocalizedString("unknown_label", comment: "Unknown label")
        }
        
        return LiquidGlassButton(
            title: label,
            icon: icon,
            style: isSelected ? .primary : .secondary,
            size: .small,
            action: onTap
        )
        .accessibilityLabel("\(label) vote")
        .accessibilityHint(isSelected ? "Vote recorded. Tap to change." : "Tap to vote \(label.lowercased())")
    }
    
    // MARK: - View Details Button
    
    private func viewDetailsButton(onTap: @escaping () -> Void) -> some View {
        LiquidGlassButton(
            title: NSLocalizedString("view_details", comment: "View details button text"),
            icon: "chevron.right",
            style: .text,
            size: .small,
            action: onTap
        )
        .accessibilityLabel("View scenario details")
        .accessibilityHint("Tap to see full details of this scenario")
    }
    
    // MARK: - Helpers
    
    private func getUserVote(for scenarioWithVotes: ScenarioWithVotes) -> ScenarioVote? {
        scenarioWithVotes.votes.first { $0.participantId == participantId }
    }
}

// MARK: - Preview

struct ScenarioListView_Previews: PreviewProvider {
    static var previews: some View {
        // Note: This preview is disabled because the Event type is from the Shared module
        // and requires proper initialization with all required parameters.
        // Uncomment and update when Shared module is properly linked.
        /*
        ScenarioListView(
            event: Event(
                id: "preview-event-id",
                title: "Team Building Event",
                description: "Annual team building activity",
                status: EventStatus.confirmed,
                organizerId: "organizer-1",
                participants: ["participant-1"],
                proposedSlots: [],
                deadline: ISO8601DateFormatter().string(from: Date()),
                createdAt: ISO8601DateFormatter().string(from: Date()),
                updatedAt: ISO8601DateFormatter().string(from: Date()),
                eventType: .teamBuilding,
                eventTypeCustom: nil,
                minParticipants: nil,
                maxParticipants: nil,
                expectedParticipants: nil,
                finalDate: nil,
                heroImageUrl: nil
            ),
            participantId: "participant-1",
            onScenarioTap: { _ in },
            onCompareTap: { },
            onCreateScenarioTap: { },
            onBack: { }
        )
        */
        EmptyView()
    }
}
