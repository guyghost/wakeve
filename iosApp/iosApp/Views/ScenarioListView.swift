import SwiftUI
import Shared

// MARK: - Local Models

enum ScenarioStatus: String {
    case proposed = "PROPOSED"
    case selected = "SELECTED"
    case rejected = "REJECTED"
}

/// Scenario List View - iOS
///
/// Displays all scenarios for an event with voting interface.
/// Uses Liquid Glass design system with Material backgrounds.
struct ScenarioListView: View {
    let event: Event
    let repository: ScenarioRepository
    let participantId: String
    let onScenarioTap: (Scenario_) -> Void
    let onCompareTap: () -> Void
    let onBack: () -> Void
    
    @State private var scenarios: [ScenarioWithVotes] = []
    @State private var userVotes: [String: ScenarioVote] = [:]
    @State private var isLoading = true
    @State private var errorMessage = ""
    @State private var showError = false
    
    var body: some View {
        ZStack {
            Color(.systemGroupedBackground)
                .ignoresSafeArea()
            
            VStack(spacing: 0) {
                // Header
                headerView
                
                if isLoading {
                    loadingView
                } else if scenarios.isEmpty {
                    emptyStateView
                } else {
                    ScrollView {
                        VStack(spacing: 16) {
                            // Compare button
                            if scenarios.count > 1 {
                                compareButton
                            }
                            
                            // Scenario cards with voting
                            ForEach(scenarios, id: \.scenario.id) { scenarioWithVotes in
                                ScenarioCard(
                                    scenarioWithVotes: scenarioWithVotes,
                                    userVote: userVotes[scenarioWithVotes.scenario.id],
                                    onVote: { voteType in
                                        Task {
                                            await submitVote(
                                                scenarioId: scenarioWithVotes.scenario.id,
                                                voteType: voteType
                                            )
                                        }
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
            loadScenarios()
        }
        .alert("Error", isPresented: $showError) {
            Button("OK", role: .cancel) {}
        } message: {
            Text(errorMessage)
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
                Text("Choose a Scenario")
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
                
                Text("Compare Scenarios")
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
            
            Text("Loading scenarios...")
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
                Text("No Scenarios Yet")
                    .font(.system(size: 24, weight: .bold))
                    .foregroundColor(.primary)
                
                Text("The organizer will add scenario options for this event.")
                    .font(.system(size: 17))
                    .foregroundColor(.secondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 40)
            }
        }
        .frame(maxHeight: .infinity)
    }
    
    // MARK: - Data Loading
    
    private func loadScenarios() {
        Task {
            let scenariosWithVotes = repository.getScenariosWithVotes(eventId: event.id)
            
            // Extract user's votes
            var votes: [String: ScenarioVote] = [:]
            for swv in scenariosWithVotes {
                if let userVote = swv.votes.first(where: { $0.participantId == participantId }) {
                    votes[swv.scenario.id] = userVote
                }
            }
            
            await MainActor.run {
                self.scenarios = scenariosWithVotes
                self.userVotes = votes
                self.isLoading = false
            }
        }
    }
    
    private func submitVote(scenarioId: String, voteType: ScenarioVoteType) async {
        do {
            let vote = ScenarioVote(
                id: UUID().uuidString,
                scenarioId: scenarioId,
                participantId: participantId,
                vote: voteType,
                createdAt: ISO8601DateFormatter().string(from: Date())
            )
            _ = try await repository.addVote(vote: vote)
            
            // Reload scenarios to refresh
            loadScenarios()
        } catch {
            await MainActor.run {
                self.errorMessage = error.localizedDescription
                self.showError = true
            }
        }
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
                
                ScenarioStatusBadge(status: ScenarioStatus(rawValue: scenario.status.name) ?? .proposed)
            }
            
            // Key details
            VStack(spacing: 12) {
                InfoRow(label: "Date", value: scenario.dateOrPeriod, icon: "calendar")
                InfoRow(label: "Location", value: scenario.location, icon: "mappin.circle")
                InfoRow(label: "Duration", value: "\(scenario.duration) days", icon: "clock")
                InfoRow(
                    label: "Budget",
                    value: String(format: "$%.0f per person", scenario.estimatedBudgetPerPerson),
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
    let status: ScenarioStatus
    
    private var color: Color {
        switch status {
        case .proposed: return .blue
        case .selected: return .green
        case .rejected: return .red
        }
    }
    
    private var text: String {
        switch status {
        case .proposed: return "Proposed"
        case .selected: return "Selected"
        case .rejected: return "Rejected"
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
                Text("Voting Results")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundColor(.primary)
                
                Spacer()
                
                Text("Score: \(result.score)")
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
