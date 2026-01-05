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
    let onBack: () -> Void
    
    @StateObject private var viewModel = ScenarioListViewModel()
    
    var body: some View {
        ZStack {
            Color(.systemGroupedBackground)
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header
                headerView
                
                if viewModel.isLoading {
                    loadingView
                } else if viewModel.isEmpty {
                    emptyStateView
                } else {
                    ScrollView {
                        VStack(spacing: 16) {
                            // Compare button
                            if viewModel.scenarios.count > 1 {
                                compareButton
                            }
                            
                            // Scenario cards with voting
                            ForEach(viewModel.scenarios, id: \.scenario.id) { scenarioWithVotes in
                                ScenarioCard(
                                    scenarioWithVotes: scenarioWithVotes,
                                    userVote: getUserVote(for: scenarioWithVotes),
                                    onVote: { voteType in
                                        viewModel.voteScenario(
                                            scenarioId: scenarioWithVotes.scenario.id,
                                            voteType: voteType
                                        )
                                    },
                                    onTap: { onScenarioTap(scenarioWithVotes.scenario) }
                                )
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
                Button(action: onBack) {
                    Image(systemName: "arrow.left")
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundColor(.secondary)
                        .frame(width: 36, height: 36)
                        .background(Color(.tertiarySystemFill))
                        .clipShape(Circle())
                }
                
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
    
    // MARK: - Compare Button
    
    private var compareButton: some View {
        Button(action: onCompareTap) {
            HStack {
                Image(systemName: "arrow.left.arrow.right")
                    .font(.system(size: 16, weight: .semibold))
                
                Text(NSLocalizedString("compare", comment: "Compare button text"))
                    .font(.system(size: 17, weight: .semibold))
                
                Spacer()
                
                Image(systemName: "chevron.right")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundColor(.secondary)
            }
            .foregroundColor(.blue)
            .padding(16)
            .glassCard()
        }
    }
    
    // MARK: - Loading View
    
    private var loadingView: some View {
        VStack(spacing: 20) {
            ProgressView()
                .progressViewStyle(CircularProgressViewStyle())
            
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
                    .fill(Color.blue.opacity(0.1))
                    .frame(width: 80, height: 80)
                
                Image(systemName: "list.bullet.rectangle.portrait")
                    .font(.system(size: 36))
                    .foregroundColor(.blue)
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
        }
        .frame(maxHeight: .infinity)
    }
    
    // MARK: - Helpers
    
    private func getUserVote(for scenarioWithVotes: ScenarioWithVotes) -> ScenarioVote? {
        scenarioWithVotes.votes.first { $0.participantId == participantId }
    }
}

// MARK: - Scenario Card

struct ScenarioCard: View {
    let scenarioWithVotes: ScenarioWithVotes
    let userVote: ScenarioVote?
    let onVote: (ScenarioVoteType) -> Void
    let onTap: () -> Void
    
    private var scenario: Scenario_ {
        scenarioWithVotes.scenario
    }
    
    private var votingResult: ScenarioVotingResult {
        scenarioWithVotes.votingResult
    }
    
    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            // Header with status badge
            HStack {
                Text(scenario.name)
                    .font(.system(size: 22, weight: .bold))
                    .foregroundColor(.primary)
                
                Spacer()
                
                ScenarioStatusBadge(status: scenario.status.name)
            }
            
            // Key details
            VStack(alignment: .leading, spacing: 12) {
                ScenarioInfoRow(label: NSLocalizedString("date_label", comment: "Date label"), value: scenario.dateOrPeriod, icon: "calendar")
                ScenarioInfoRow(label: NSLocalizedString("location_label", comment: "Location label"), value: scenario.location, icon: "mappin.circle")
                ScenarioInfoRow(label: NSLocalizedString("duration_label_short", comment: "Duration label"), value: "\(scenario.duration) \(NSLocalizedString("days_label", comment: "Days label"))", icon: "clock")
                ScenarioInfoRow(
                    label: NSLocalizedString("budget_label", comment: "Budget label"),
                    value: String(format: NSLocalizedString("budget_per_person_label", comment: "Budget per person format"), scenario.estimatedBudgetPerPerson),
                    icon: "dollarsign.circle"
                )
            }
            
            // Voting results
            if votingResult.totalVotes > 0 {
                VotingResultsSection(result: votingResult)
            }
            
            // Voting buttons
            VotingButtons(
                currentVote: userVote?.vote,
                onVote: onVote
            )
            
            // View details button
            Button(action: onTap) {
                HStack {
                    Text("View Details")
                        .font(.system(size: 15, weight: .medium))
                        .foregroundColor(.blue)
                    
                    Spacer()
                    
                    Image(systemName: "chevron.right")
                        .font(.system(size: 12, weight: .semibold))
                        .foregroundColor(.blue)
                }
            }
        }
        .padding(20)
        .glassCard()
    }
}

// MARK: - Status Badge

struct ScenarioStatusBadge: View {
    let status: String
    
    private var color: Color {
        switch status.uppercased() {
        case "PROPOSED": return .blue
        case "SELECTED": return .green
        case "REJECTED": return .red
        default: return .gray
        }
    }
    
    private var text: String {
        switch status.uppercased() {
        case "PROPOSED": return "Proposed"
        case "SELECTED": return "Selected"
        case "REJECTED": return "Rejected"
        default: return status
        }
    }
    
    var body: some View {
        Text(text)
            .font(.system(size: 12, weight: .semibold))
            .foregroundColor(color)
            .padding(.horizontal, 12)
            .padding(.vertical, 6)
            .background(color.opacity(0.1))
            .continuousCornerRadius(12)
    }
}

// MARK: - Voting Results Section

struct VotingResultsSection: View {
    let result: ScenarioVotingResult
    
    var body: some View {
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
                VoteCount(
                    count: Int(result.preferCount),
                    color: .green,
                    label: "Prefer"
                )
                
                VoteCount(
                    count: Int(result.neutralCount),
                    color: .orange,
                    label: "Neutral"
                )
                
                VoteCount(
                    count: Int(result.againstCount),
                    color: .red,
                    label: "Against"
                )
            }
        }
        .padding(16)
        .background(Color(.secondarySystemGroupedBackground))
        .continuousCornerRadius(12)
    }
}

// MARK: - Vote Count

struct VoteCount: View {
    let count: Int
    let color: Color
    let label: String
    
    var body: some View {
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
}

// MARK: - Voting Buttons

struct VotingButtons: View {
    let currentVote: ScenarioVoteType?
    let onVote: (ScenarioVoteType) -> Void
    
    var body: some View {
        HStack(spacing: 12) {
            ScenarioVoteButton(
                type: .prefer,
                isSelected: currentVote == .prefer,
                onTap: { onVote(.prefer) }
            )
            
            ScenarioVoteButton(
                type: .neutral,
                isSelected: currentVote == .neutral,
                onTap: { onVote(.neutral) }
            )
            
            ScenarioVoteButton(
                type: .against,
                isSelected: currentVote == .against,
                onTap: { onVote(.against) }
            )
        }
    }
}

// MARK: - Scenario Vote Button

struct ScenarioVoteButton: View {
    let type: ScenarioVoteType
    let isSelected: Bool
    let onTap: () -> Void
    
    private var color: Color {
        switch type {
        case .prefer: return .green
        case .neutral: return .orange
        case .against: return .red
        default: return .gray
        }
    }
    
    private var icon: String {
        switch type {
        case .prefer: return "hand.thumbsup.fill"
        case .neutral: return "minus.circle.fill"
        case .against: return "hand.thumbsdown.fill"
        default: return "questionmark.circle.fill"
        }
    }
    
    private var label: String {
        switch type {
        case .prefer: return "Prefer"
        case .neutral: return "Neutral"
        case .against: return "Against"
        default: return "Unknown"
        }
    }
    
    var body: some View {
        Button(action: onTap) {
            VStack(spacing: 8) {
                Image(systemName: icon)
                    .font(.system(size: 24))
                    .foregroundColor(isSelected ? .white : color)
                
                Text(label)
                    .font(.system(size: 12, weight: .medium))
                    .foregroundColor(isSelected ? .white : color)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 16)
            .background(isSelected ? color : color.opacity(0.1))
            .continuousCornerRadius(12)
        }
    }
}

// MARK: - Scenario Info Row

struct ScenarioInfoRow: View {
    let label: String
    let value: String
    let icon: String
    
    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundColor(.blue)
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
}
